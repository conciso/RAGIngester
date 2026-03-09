package de.conciso.ragingester.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Maps all RAGINGESTER_* environment variables.
 *
 * <p>Defaults for optional properties are supplied in {@code application.properties}.
 * Only {@code runGroup} (RAGINGESTER_RUN_GROUP) is required with no default.
 */
@ConfigurationProperties(prefix = "ragingester")
public record RagIngesterProperties(

        /** RAGINGESTER_RUN_GROUP – e.g. "param_run_001" (required) */
        String runGroup,

        /** RAGINGESTER_RUN_LABEL – preset label forwarded from env-tester (optional) */
        String runLabel,

        /** RAGINGESTER_CLEAN_DOCS_PATH (default: /data/clean) */
        String cleanDocsPath,

        /** RAGINGESTER_POISONED_DOCS_PATH (default: /data/poisoned) */
        String poisonedDocsPath,

        /** RAGINGESTER_TESTCASES_PATH (default: /app/testcases) */
        String testcasesPath,

        /** RAGINGESTER_REPORTS_PATH (default: /app/reports) */
        String reportsPath,

        /** RAGINGESTER_OVERRIDE_ENV_PATH (default: /config/override.env) */
        String overrideEnvPath,

        /** RAGINGESTER_RAGCHECKER_IMAGE (default: ragchecker:latest) */
        String ragcheckerImage,

        /** RAGINGESTER_RAGCHECKER_ENV_FILE (default: /config/ragchecker.env) */
        String ragcheckerEnvFile,

        /** RAGINGESTER_POLLING_TIMEOUT_MINUTES (default: 10) */
        int pollingTimeoutMinutes,

        /** RAGINGESTER_DRY_RUN (default: false) – log only, no uploads/docker calls */
        boolean dryRun,

        /**
         * RAGINGESTER_STAGES – comma-separated list of stage names to run,
         * e.g. "P25PCT,P50PCT". Empty list = run all stages (default).
         */
        java.util.List<de.conciso.ragingester.model.PoisoningStage> stages,

        /**
         * RAGINGESTER_RAGCHECKER_RUN_LABEL – query-preset label appended to every
         * RAGCHECKER_RUN_LABEL for result attribution, e.g. "sweep-chunktopk-5".
         * Optional; omitted when blank.
         */
        String ragcheckerRunLabel
) {
}
