package de.conciso.ragingester.service;

import de.conciso.ragingester.config.RagIngesterProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Runs Docker containers for RAGChecker and validates Docker availability at startup.
 */
@Service
public class DockerService {

    private static final Logger log = LoggerFactory.getLogger(DockerService.class);

    private final RagIngesterProperties props;

    public DockerService(RagIngesterProperties props) {
        this.props = props;
    }

    /**
     * Checks that the Docker daemon is reachable (via {@code docker info}).
     * Throws {@link IllegalStateException} with a clear message if not.
     */
    public void checkDockerAvailable() throws IOException, InterruptedException {
        log.debug("Checking Docker availability ...");
        Process process = new ProcessBuilder("docker", "info")
                .redirectErrorStream(true)
                .start();
        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new IllegalStateException(
                    "Docker daemon is not reachable. " +
                    "Make sure /var/run/docker.sock is mounted and Docker is running.");
        }
        log.debug("Docker is available.");
    }

    /**
     * Runs the RAGChecker container synchronously.
     *
     * @param runGroup  value for RAGCHECKER_RUN_GROUP
     * @param label     value for RAGCHECKER_RUN_LABEL
     * @throws DockerExecutionException if the container exits with a non-zero code
     */
    public void runRagChecker(String runGroup, String label) throws IOException, InterruptedException {
        List<String> command = new ArrayList<>(List.of(
                "docker", "run", "--rm",
                "--network", "aibox_network",
                "--env-file", props.ragcheckerEnvFile(),
                "-e", "RAGCHECKER_RUN_GROUP=" + runGroup,
                "-e", "RAGCHECKER_RUN_LABEL=" + label,
                "-v", props.testcasesPath() + ":/app/testcases:ro",
                "-v", props.reportsPath() + ":/app/reports",
                props.ragcheckerImage()
        ));

        log.info("Starting RAGChecker: label={}", label);
        log.debug("Docker command: {}", String.join(" ", command));

        ProcessBuilder pb = new ProcessBuilder(command);
        pb.inheritIO();
        Process process = pb.start();
        int exitCode = process.waitFor();

        if (exitCode != 0) {
            throw new DockerExecutionException(
                    "RAGChecker exited with code %d (label=%s)".formatted(exitCode, label));
        }
        log.info("RAGChecker finished successfully: label={}", label);
    }

    // ---- exception ----

    public static class DockerExecutionException extends RuntimeException {
        public DockerExecutionException(String message) {
            super(message);
        }
    }
}
