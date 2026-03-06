package de.conciso.ragingester;

import de.conciso.ragingester.model.PoisoningStage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies that RAGChecker labels are generated correctly for each poisoning stage.
 *
 * <p>Label schema: {@code <runGroup>_<labelSuffix>}
 */
class LabelGenerationTest {

    private static final String RUN_GROUP = "param_run_001";

    @ParameterizedTest(name = "{0} → label={1}")
    @CsvSource({
            "P01PCT,  param_run_001_p01pct",
            "P02PCT,  param_run_001_p02pct",
            "P04PCT,  param_run_001_p04pct",
            "P06PCT,  param_run_001_p06pct",
            "P08PCT,  param_run_001_p08pct",
            "P10PCT,  param_run_001_p10pct",
            "P17PCT,  param_run_001_p17pct",
            "P25PCT,  param_run_001_p25pct",
            "P50PCT,  param_run_001_p50pct",
            "P75PCT,  param_run_001_p75pct",
            "P100PCT, param_run_001_p100pct",
            "P150PCT, param_run_001_p150pct",
            "P200PCT, param_run_001_p200pct",
    })
    void label_containsRunGroupAndSuffix(String stageName, String expectedLabel) {
        PoisoningStage stage = PoisoningStage.valueOf(stageName);
        assertThat(stage.label(RUN_GROUP)).isEqualTo(expectedLabel.strip());
    }

    @Test
    void cleanLabel_hasCorrectFormat() {
        String cleanLabel = RUN_GROUP + "_clean";
        assertThat(cleanLabel).isEqualTo("param_run_001_clean");
    }

    @Test
    void label_startsWithRunGroup() {
        for (PoisoningStage stage : PoisoningStage.values()) {
            assertThat(stage.label(RUN_GROUP)).startsWith(RUN_GROUP + "_");
        }
    }

    @Test
    void labelSuffix_startsWithP() {
        for (PoisoningStage stage : PoisoningStage.values()) {
            assertThat(stage.labelSuffix()).startsWith("p");
        }
    }
}
