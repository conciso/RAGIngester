package de.conciso.ragingester;

import de.conciso.ragingester.model.PoisoningStage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies the expected poisoned-document count per stage for 64 clean documents.
 *
 * <p>Formula: {@code ceil(64 * percentage / 100)}
 */
class PoisoningStageCalculationTest {

    private static final int CLEAN_COUNT = 64;

    @ParameterizedTest(name = "{0} → expectedDocCount={2}")
    @CsvSource({
            "P01PCT,   1.0,   1",
            "P02PCT,   2.0,   2",
            "P04PCT,   4.0,   3",
            "P06PCT,   6.0,   4",
            "P08PCT,   8.0,   6",
            "P10PCT,  10.0,   7",
            "P17PCT,  17.5,  12",
            "P25PCT,  25.0,  16",
            "P50PCT,  50.0,  32",
            "P75PCT,  75.0,  48",
            "P100PCT,100.0,  64",
            "P150PCT,150.0,  96",
            "P200PCT,200.0, 128",
    })
    void expectedDocCount_matchesCeilCalculation(String stageName, double pct, int expectedCount) {
        PoisoningStage stage = PoisoningStage.valueOf(stageName);
        assertThat(stage.percentage()).isEqualTo(pct);
        assertThat(stage.expectedDocCount(CLEAN_COUNT)).isEqualTo(expectedCount);
    }

    @Test
    void allStagesArePresent() {
        assertThat(PoisoningStage.values()).hasSize(13);
    }

    @Test
    void expectedDocCountIncreasesMonotonically() {
        PoisoningStage[] stages = PoisoningStage.values();
        for (int i = 1; i < stages.length; i++) {
            assertThat(stages[i].expectedDocCount(CLEAN_COUNT))
                    .as("stage %s should have more docs than %s", stages[i], stages[i - 1])
                    .isGreaterThanOrEqualTo(stages[i - 1].expectedDocCount(CLEAN_COUNT));
        }
    }
}
