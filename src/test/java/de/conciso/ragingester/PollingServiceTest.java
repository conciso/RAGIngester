package de.conciso.ragingester;

import de.conciso.ragingester.service.LightRagClient;
import de.conciso.ragingester.service.PollingService;
import de.conciso.ragingester.service.PollingService.PollingTimeoutException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link PollingService} using a mock {@link LightRagClient}
 * and a controllable clock to avoid real I/O or long sleeps.
 */
@ExtendWith(MockitoExtension.class)
class PollingServiceTest {

    @Mock
    LightRagClient lightRagClient;

    /**
     * Builds a PollingService with a 5-minute logical timeout and 0 ms poll interval.
     * The clock supplier uses the given counter: each invocation advances 1 minute.
     */
    private PollingService serviceWithTickingClock(AtomicInteger tickMinutes) {
        Instant base = Instant.parse("2024-01-01T00:00:00Z");
        return new PollingService(
                lightRagClient,
                5,            // timeout: 5 minutes
                0L,           // poll interval: 0 ms (no real sleeping in tests)
                () -> base.plusSeconds(tickMinutes.getAndIncrement() * 60L)
        );
    }

    // ---- success cases ----

    @Test
    void returnsImmediately_whenFirstPollSatisfiesExpectedCount() throws InterruptedException {
        when(lightRagClient.getTotalDocumentCount()).thenReturn(64);
        AtomicInteger tick = new AtomicInteger(0);
        PollingService service = serviceWithTickingClock(tick);

        assertThatCode(() -> service.waitForDocumentCount(64)).doesNotThrowAnyException();
        verify(lightRagClient, times(1)).getTotalDocumentCount();
    }

    @Test
    void returnsAfterMultiplePolls_whenCountGrowsGradually() throws InterruptedException {
        // Returns 10, 40, 64 on successive calls
        when(lightRagClient.getTotalDocumentCount())
                .thenReturn(10)
                .thenReturn(40)
                .thenReturn(64);

        AtomicInteger tick = new AtomicInteger(0);
        PollingService service = serviceWithTickingClock(tick);

        assertThatCode(() -> service.waitForDocumentCount(64)).doesNotThrowAnyException();
        verify(lightRagClient, times(3)).getTotalDocumentCount();
    }

    @Test
    void acceptsCountHigherThanExpected() throws InterruptedException {
        when(lightRagClient.getTotalDocumentCount()).thenReturn(70); // more than 64
        AtomicInteger tick = new AtomicInteger(0);
        PollingService service = serviceWithTickingClock(tick);

        assertThatCode(() -> service.waitForDocumentCount(64)).doesNotThrowAnyException();
    }

    // ---- timeout case ----

    @Test
    void throwsPollingTimeoutException_whenDeadlineExceeded() {
        // Clock always returns a time already past the deadline:
        // deadline = base + 0 * 60 = base; first check: base.isBefore(base) = false
        Instant base = Instant.parse("2024-01-01T12:00:00Z");
        PollingService service = new PollingService(
                lightRagClient,
                0,   // timeout: 0 minutes → deadline == base
                0L,
                () -> base
        );

        assertThatThrownBy(() -> service.waitForDocumentCount(64))
                .isInstanceOf(PollingTimeoutException.class)
                .hasMessageContaining("Timeout");

        // No poll should have happened (loop never entered)
        verifyNoInteractions(lightRagClient);
    }

    @Test
    void throwsPollingTimeoutException_afterRunningOutOfTime() {
        when(lightRagClient.getTotalDocumentCount()).thenReturn(0);

        // Clock ticks 1 minute per call; timeout = 2 minutes → expires on the 3rd clock read
        AtomicInteger tick = new AtomicInteger(0);
        Instant base = Instant.parse("2024-01-01T00:00:00Z");
        PollingService service = new PollingService(
                lightRagClient,
                2,   // timeout: 2 minutes
                0L,
                () -> base.plusSeconds(tick.getAndIncrement() * 60L)
        );

        assertThatThrownBy(() -> service.waitForDocumentCount(64))
                .isInstanceOf(PollingTimeoutException.class);
    }
}
