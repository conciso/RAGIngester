package de.conciso.ragingester.service;

import de.conciso.ragingester.config.RagIngesterProperties;
import de.conciso.ragingester.model.PoisoningStage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Drives the complete ingestion and poisoning workflow.
 *
 * <ol>
 *   <li>Upload 64 clean documents → wait for indexing → run RAGChecker (clean baseline)</li>
 *   <li>For each poisoning stage: upload delta → wait for indexing → run RAGChecker</li>
 * </ol>
 */
@Service
public class IngestionOrchestrator {

    private static final Logger log = LoggerFactory.getLogger(IngestionOrchestrator.class);

    private final RagIngesterProperties props;
    private final LightRagClient lightRagClient;
    private final PollingService pollingService;
    private final DockerService dockerService;

    public IngestionOrchestrator(RagIngesterProperties props,
                                 LightRagClient lightRagClient,
                                 PollingService pollingService,
                                 DockerService dockerService) {
        this.props = props;
        this.lightRagClient = lightRagClient;
        this.pollingService = pollingService;
        this.dockerService = dockerService;
    }

    /**
     * Executes the full ingestion run.
     *
     * @throws Exception on unrecoverable errors (polling timeout, Docker failure)
     */
    public void run() throws Exception {
        log.info("=== RAGIngester starting — run group: {} | dry-run: {} ===",
                props.runGroup(), props.dryRun());

        if (!props.dryRun()) {
            dockerService.checkDockerAvailable();
        }

        // ---- 1. Upload clean documents ----
        Path cleanPath = Path.of(props.cleanDocsPath());
        List<Path> cleanDocs = listPdfFiles(cleanPath);
        log.info("Uploading {} clean document(s) from {}", cleanDocs.size(), cleanPath);
        uploadDocuments(cleanDocs);

        int expectedTotal = cleanDocs.size();
        waitForIndexing(expectedTotal);

        // ---- 2. RAGChecker — clean baseline ----
        runCheckerIfNotDryRun(buildLabel("_clean"));

        // ---- 3. Progressive poisoning stages ----
        Set<String> uploadedPoisonedNames = new HashSet<>();

        List<PoisoningStage> stagesToRun = (props.stages() == null || props.stages().isEmpty())
                ? List.of(PoisoningStage.values())
                : props.stages();

        for (PoisoningStage stage : stagesToRun) {
            Path stageDir = Path.of(props.poisonedDocsPath(), stage.directoryName());

            if (!Files.isDirectory(stageDir)) {
                log.warn("Stage directory not found, skipping stage {}: {}", stage.directoryName(), stageDir);
                continue;
            }

            List<Path> allDocsInStage = listPdfFiles(stageDir);

            // Upload only the delta (files not yet uploaded from a previous stage)
            List<Path> newDocs = allDocsInStage.stream()
                    .filter(p -> !uploadedPoisonedNames.contains(p.getFileName().toString()))
                    .toList();

            log.info("Stage {}: uploading {} new document(s) ({} cumulative in dir)",
                    stage.directoryName(), newDocs.size(), allDocsInStage.size());
            uploadDocuments(newDocs);
            newDocs.forEach(p -> uploadedPoisonedNames.add(p.getFileName().toString()));

            // Expected total = clean docs + cumulative poisoned docs for this stage
            expectedTotal = cleanDocs.size() + allDocsInStage.size();
            waitForIndexing(expectedTotal);

            runCheckerIfNotDryRun(buildLabel("_" + stage.labelSuffix()));
        }

        log.info("=== RAGIngester run completed successfully ===");
    }

    // ---- helpers ----

    private void uploadDocuments(List<Path> docs) {
        for (Path doc : docs) {
            if (props.dryRun()) {
                log.info("[DRY-RUN] Would upload: {}", doc.getFileName());
                continue;
            }
            try {
                lightRagClient.uploadDocument(doc);
                log.debug("Uploaded: {}", doc.getFileName());
            } catch (RestClientException e) {
                log.warn("Failed to upload {} — skipping: {}", doc.getFileName(), e.getMessage());
            }
        }
    }

    private void waitForIndexing(int expectedCount) throws InterruptedException {
        if (props.dryRun()) {
            log.info("[DRY-RUN] Would poll LightRAG for {} documents", expectedCount);
            return;
        }
        log.info("Waiting for LightRAG to index {} document(s) ...", expectedCount);
        pollingService.waitForDocumentCount(expectedCount);
    }

    private void runCheckerIfNotDryRun(String label) throws IOException, InterruptedException {
        if (props.dryRun()) {
            log.info("[DRY-RUN] Would run RAGChecker with label={}", label);
            return;
        }
        dockerService.runRagChecker(props.runGroup(), label);
    }

    private String buildLabel(String suffix) {
        String base = (props.runLabel() != null && !props.runLabel().isBlank())
                ? props.runGroup() + "_" + props.runLabel()
                : props.runGroup();
        String label = base + suffix;
        return (props.ragcheckerRunLabel() != null && !props.ragcheckerRunLabel().isBlank())
                ? label + "_" + props.ragcheckerRunLabel()
                : label;
    }

    private List<Path> listPdfFiles(Path directory) throws IOException {
        if (!Files.isDirectory(directory)) {
            throw new IllegalArgumentException("Not a directory: " + directory);
        }
        try (Stream<Path> stream = Files.walk(directory)) {
            return stream
                    .filter(Files::isRegularFile)
                    .filter(p -> p.getFileName().toString().toLowerCase().endsWith(".pdf"))
                    .sorted()
                    .toList();
        }
    }
}
