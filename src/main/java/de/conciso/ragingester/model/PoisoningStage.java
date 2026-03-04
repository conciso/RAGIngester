package de.conciso.ragingester.model;

/**
 * Ordered list of all poisoning stages.
 *
 * <p>Each stage knows its:
 * <ul>
 *   <li>{@code directoryName} – sub-directory under the poisoned-docs root (e.g. "01pct")</li>
 *   <li>{@code labelSuffix}   – suffix appended to the run-group for the RAGChecker label (e.g. "p01pct")</li>
 *   <li>{@code percentage}    – poisoning percentage relative to the 64 clean documents</li>
 * </ul>
 *
 * <p>The directory contains the <em>cumulative</em> poisoned documents for that stage
 * (not just the delta compared to the previous stage).
 */
public enum PoisoningStage {

    P01PCT  ("01pct",  "p01pct",    1.0),
    P02PCT  ("02pct",  "p02pct",    2.0),
    P04PCT  ("04pct",  "p04pct",    4.0),
    P06PCT  ("06pct",  "p06pct",    6.0),
    P08PCT  ("08pct",  "p08pct",    8.0),
    P10PCT  ("10pct",  "p10pct",   10.0),
    P175PCT ("17pct",  "p175pct",  17.5),
    P25PCT  ("25pct",  "p25pct",   25.0),
    P50PCT  ("50pct",  "p50pct",   50.0),
    P75PCT  ("75pct",  "p75pct",   75.0),
    P100PCT ("100pct", "p100pct", 100.0),
    P150PCT ("150pct", "p150pct", 150.0),
    P200PCT ("200pct", "p200pct", 200.0);

    private final String directoryName;
    private final String labelSuffix;
    private final double percentage;

    PoisoningStage(String directoryName, String labelSuffix, double percentage) {
        this.directoryName = directoryName;
        this.labelSuffix   = labelSuffix;
        this.percentage    = percentage;
    }

    public String directoryName() { return directoryName; }

    public String labelSuffix() { return labelSuffix; }

    public double percentage() { return percentage; }

    /** Full RAGChecker label: {@code <runGroup>_<labelSuffix>}. */
    public String label(String runGroup) {
        return runGroup + "_" + labelSuffix;
    }

    /**
     * Expected cumulative document count for this stage.
     *
     * <p>Calculated as {@code ceil(cleanCount * percentage / 100)}.
     * Matches the number of documents that should be present in the stage directory.
     *
     * @param cleanCount number of clean baseline documents (typically 64)
     * @return expected document count for this poisoning stage
     */
    public int expectedDocCount(int cleanCount) {
        return (int) Math.ceil(cleanCount * percentage / 100.0);
    }
}
