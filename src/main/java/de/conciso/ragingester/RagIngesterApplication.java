package de.conciso.ragingester;

import de.conciso.ragingester.config.LightRagProperties;
import de.conciso.ragingester.config.RagIngesterProperties;
import de.conciso.ragingester.service.IngestionOrchestrator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

/**
 * Entry point for RAGIngester.
 *
 * <p>Implements {@link ApplicationRunner} so that {@link IngestionOrchestrator#run()}
 * is invoked after the Spring context is ready. Any uncaught exception causes Spring Boot
 * to exit with code 1.
 */
@SpringBootApplication
@EnableConfigurationProperties({LightRagProperties.class, RagIngesterProperties.class})
public class RagIngesterApplication implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(RagIngesterApplication.class);

    private final IngestionOrchestrator orchestrator;

    public RagIngesterApplication(IngestionOrchestrator orchestrator) {
        this.orchestrator = orchestrator;
    }

    public static void main(String[] args) {
        SpringApplication.run(RagIngesterApplication.class, args);
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        try {
            orchestrator.run();
        } catch (Exception e) {
            log.error("RAGIngester run failed: {}", e.getMessage(), e);
            throw e; // re-throw so Spring Boot exits with code 1
        }
    }
}
