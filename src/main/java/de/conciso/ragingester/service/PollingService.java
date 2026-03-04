package de.conciso.ragingester.service;

import de.conciso.ragingester.config.RagIngesterProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.function.Supplier;

/**
 * Polls LightRAG until the indexed document count reaches an expected threshold or times out.
 */
@Service
public class PollingService {

    private static final Logger log = LoggerFactory.getLogger(PollingService.class);
    static final long DEFAULT_POLL_INTERVAL_MS = 10_000L;

    private final LightRagClient lightRagClient;
    private final int timeoutMinutes;
    private final long pollIntervalMs;
    private final Supplier<Instant> clock;

    /** Production constructor – wired by Spring. */
    public PollingService(LightRagClient lightRagClient, RagIngesterProperties props) {
        this(lightRagClient, props.pollingTimeoutMinutes(), DEFAULT_POLL_INTERVAL_MS, Instant::now);
    }

    /** Package-private constructor for unit tests. */
    PollingService(LightRagClient lightRagClient, int timeoutMinutes, long pollIntervalMs,
                   Supplier<Instant> clock) {
        this.lightRagClient = lightRagClient;
        this.timeoutMinutes = timeoutMinutes;
        this.pollIntervalMs = pollIntervalMs;
        this.clock = clock;
    }

    /**
     * Blocks until {@code total} indexed documents >= {@code expectedCount}, or throws on timeout.
     *
     * @param expectedCount minimum document count to wait for
     * @throws PollingTimeoutException  if the timeout expires before the count is reached
     * @throws InterruptedException     if the thread is interrupted during a sleep
     */
    public void waitForDocumentCount(int expectedCount) throws InterruptedException {
        Instant deadline = clock.get().plusSeconds((long) timeoutMinutes * 60);

        while (clock.get().isBefore(deadline)) {
            int total = lightRagClient.getTotalDocumentCount();
            if (total >= expectedCount) {
                log.info("Indexing complete: {}/{} documents", total, expectedCount);
                return;
            }
            log.info("Waiting for indexing: {}/{} documents indexed ...", total, expectedCount);
            Thread.sleep(pollIntervalMs);
        }

        throw new PollingTimeoutException(
                "Timeout after %d minutes: expected %d indexed documents".formatted(timeoutMinutes, expectedCount));
    }

    // ---- exception ----

    public static class PollingTimeoutException extends RuntimeException {
        public PollingTimeoutException(String message) {
            super(message);
        }
    }
}
