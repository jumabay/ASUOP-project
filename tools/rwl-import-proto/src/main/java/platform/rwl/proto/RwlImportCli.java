package platform.rwl.proto;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import platform.rwl.proto.config.EnvFileLoader;
import platform.rwl.proto.config.RwlConfig;
import platform.rwl.proto.db.ConnectionIdentity;
import platform.rwl.proto.db.DbSupport;
import platform.rwl.proto.importing.ImportService;
import platform.rwl.proto.importing.ImportService.ImportReport;
import platform.rwl.proto.model.ImportMode;

/**
 * One-shot CLI for RWL dislocation import prototype.
 *
 * <p>Environment variables are read from the process environment. A {@code .env} file is NOT
 * loaded automatically. Pass {@code --env-file=path} to load KEY=VALUE into an overlay used by
 * this process only.
 *
 * <p>Target database for staging is the new Oracle <b>RAILDB</b>. Legacy RAC/DBTC archive is refused
 * by default.
 */
public final class RwlImportCli {

    public static void main(String[] args) throws Exception {
        ImportMode mode = null;
        Path envFile = null;
        Path fixture = null;
        Path saveDir = null;
        boolean forceWriteFailure = false;
        boolean initSchema = false;
        boolean allowArchiveOracle = false;
        String db = "auto";

        for (String arg : args) {
            if ("--dry-run".equals(arg)) {
                mode = ImportMode.DRY_RUN;
            } else if ("--import".equals(arg)) {
                mode = ImportMode.IMPORT;
            } else if (arg.startsWith("--env-file=")) {
                envFile = Path.of(arg.substring("--env-file=".length()));
            } else if (arg.startsWith("--fixture=")) {
                fixture = Path.of(arg.substring("--fixture=".length()));
            } else if (arg.startsWith("--save-dir=")) {
                saveDir = Path.of(arg.substring("--save-dir=".length()));
            } else if ("--force-write-failure".equals(arg)) {
                forceWriteFailure = true;
            } else if ("--init-schema".equals(arg)) {
                initSchema = true;
            } else if ("--allow-archive-oracle".equals(arg)) {
                allowArchiveOracle = true;
            } else if (arg.startsWith("--db=")) {
                db = arg.substring("--db=".length()).trim().toLowerCase(Locale.ROOT);
            } else if ("--help".equals(arg) || "-h".equals(arg)) {
                printHelp();
                return;
            } else {
                System.err.println("Unknown argument: " + arg);
                printHelp();
                System.exit(2);
                return;
            }
        }

        if (mode == null) {
            System.err.println("Specify --dry-run or --import");
            printHelp();
            System.exit(2);
            return;
        }

        Map<String, String> env = System.getenv();
        if (envFile != null) {
            System.out.println("Loading env overlay from " + envFile.toAbsolutePath());
            env = EnvFileLoader.mergeWithProcessEnv(EnvFileLoader.load(envFile));
        } else {
            System.out.println(
                    "Using process environment only (no automatic .env load). Optional: --env-file=...");
        }

        RwlConfig config =
                RwlConfig.from(env, mode, fixture, saveDir, forceWriteFailure, allowArchiveOracle);
        System.out.println("Target DB policy: new Oracle RAILDB (legacy RAC/DBTC refused by default)");
        System.out.println("Live RWL ready: " + config.canCallLiveRwl());
        if (!config.missingForLiveRwl().isEmpty() && fixture == null) {
            System.out.println("Missing for live RWL: " + String.join(", ", config.missingForLiveRwl()));
        }
        System.out.println("Oracle RAILDB staging ready (env): " + config.canUseOracle());
        if (!config.missingForOracle().isEmpty()) {
            System.out.println("Missing for Oracle: " + String.join(", ", config.missingForOracle()));
        }
        if (config.canUseOracle()) {
            System.out.println("JDBC URL (redacted): " + config.jdbcUrlSummary());
            System.out.println("Configured user: " + config.oracleUser());
        }

        Connection connection = null;
        DbSupport.Dialect dialect = null;
        try {
            if (mode == ImportMode.IMPORT || initSchema) {
                ResolvedDb resolved = resolveDb(db, config);
                if (resolved == null) {
                    if (mode == ImportMode.IMPORT) {
                        System.err.println(
                                "IMPORT requires DB. Configure RAILDB_JDBC_URL/RAILDB_USER/RAILDB_PASSWORD "
                                        + "(or ORACLE_*) for new RAILDB, or use --db=h2 for local only.");
                        System.exit(3);
                        return;
                    }
                } else {
                    connection = resolved.connection();
                    dialect = resolved.dialect();
                    if (dialect == DbSupport.Dialect.ORACLE) {
                        ConnectionIdentity identity = ConnectionIdentity.probeOracle(connection);
                        identity.print();
                        Optional<String> refusal = identity.validateAgainstExpected(
                                config.expectedOracleService(),
                                config.expectedOracleDbName(),
                                config.allowArchiveOracle());
                        if (refusal.isPresent()) {
                            System.err.println("Refused before DDL/DML: " + refusal.get());
                            System.exit(7);
                            return;
                        }
                    }
                    if (initSchema || !DbSupport.stagingTablesExist(connection)) {
                        System.out.println("Applying staging DDL (" + dialect + ")");
                        DbSupport.applyDdl(connection, dialect);
                    }
                    System.out.println("Dialect: " + dialect);
                }
            }

            if (mode == ImportMode.DRY_RUN && fixture == null && !config.canCallLiveRwl()) {
                System.out.println(
                        "Dry-run skipped live call: missing RWL settings. Use --fixture=... for local parse.");
                System.exit(4);
                return;
            }

            ImportService service = new ImportService(config);
            ImportReport report = service.run(connection, dialect);
            maybeSaveResponses(config, report);
            report.print();

            int code = switch (report.dbStatus()) {
                case SUCCESS, EMPTY -> 0;
                case API_ERROR, PARSE_ERROR -> 5;
                case FAILED -> 6;
            };
            System.exit(code);
        } finally {
            if (connection != null) {
                connection.close();
            }
        }
    }

    private static void maybeSaveResponses(RwlConfig config, ImportReport report) throws Exception {
        if (config.saveDir() == null || report.rawBodies().isEmpty()) {
            return;
        }
        Path dir = config.saveDir();
        Files.createDirectories(dir);
        String stamp = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'")
                .withZone(ZoneOffset.UTC)
                .format(Instant.now());
        int i = 0;
        for (String body : report.rawBodies()) {
            Path file = dir.resolve("rwl-view_vagon-" + stamp + "-" + (++i) + ".xml");
            Files.writeString(file, body);
            System.out.println("Saved response (outside Git recommended): " + file.toAbsolutePath());
        }
    }

    private static ResolvedDb resolveDb(String db, RwlConfig config) throws Exception {
        if ("h2".equals(db)) {
            String url = "jdbc:h2:mem:rwlproto;DB_CLOSE_DELAY=-1";
            return new ResolvedDb(DbSupport.openH2(url), DbSupport.Dialect.H2);
        }
        if ("oracle".equals(db) || "auto".equals(db)) {
            if (!config.canUseOracle()) {
                return null;
            }
            return new ResolvedDb(DbSupport.openOracle(config), DbSupport.Dialect.ORACLE);
        }
        throw new IllegalArgumentException("Unknown --db value: " + db);
    }

    private static void printHelp() {
        System.out.println(
                """
                RWL dislocation import prototype (one-shot CLI)

                Usage (PowerShell):
                  .\\gradlew.bat run --args="--dry-run --fixture=src/test/resources/fixtures/success-two-wagons.xml"
                  .\\gradlew.bat run --args="--import --db=h2 --fixture=..."
                  .\\gradlew.bat run --args="--import --init-schema --env-file=$env:USERPROFILE\\raildb-rwl.env"
                  .\\gradlew.bat run --args="--dry-run --env-file=$env:USERPROFILE\\raildb-rwl.env --save-dir=$env:USERPROFILE\\rwl-captures"

                Flags:
                  --dry-run                 Fetch/parse only (or fixture); no staging writes
                  --import                  Write staging tables
                  --fixture=path            Use saved XML instead of live RWL
                  --save-dir=path           Save raw RWL XML responses (keep outside Git)
                  --env-file=path           Explicit KEY=VALUE overlay (not auto-loaded)
                  --db=auto|oracle|h2       auto uses Oracle when RAILDB_/ORACLE_* set
                  --init-schema             Create staging tables/sequences
                  --force-write-failure     Simulate DB failure after parse (FAILED status)
                  --allow-archive-oracle    Allow legacy RAC/DBTC (default: refuse)

                Live RWL requires:
                  RWL_NAME, RWL_PASSWORD, RWL_VAGON_NOS (max 5, semicolon-separated)
                  Method: view_vagon per number, documented XML

                Oracle staging target: new RAILDB (not RAC.WORLD archive)
                  RAILDB_JDBC_URL or ORACLE_JDBC_URL (full JDBC URL)
                  RAILDB_USER or ORACLE_USER (actual schema user from RAILDB config)
                  RAILDB_PASSWORD or ORACLE_PASSWORD
                  Optional pin: EXPECTED_ORACLE_SERVICE / EXPECTED_ORACLE_DB_NAME
                """);
    }

    private record ResolvedDb(Connection connection, DbSupport.Dialect dialect) {
    }
}
