package platform.rwl.proto.config;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import platform.rwl.proto.model.ImportMode;

public final class RwlConfig {

    public static final String DEFAULT_BASE_URL =
            "https://www.railwagonlocation.com/xml/export.php";
    public static final int MAX_WAGONS = 5;

    private final ImportMode mode;
    private final String baseUrl;
    private final String rwlName;
    private final String rwlPassword;
    private final List<String> vagonNos;
    private final String oracleJdbcUrl;
    private final String oracleUser;
    private final String oraclePassword;
    private final String expectedOracleService;
    private final String expectedOracleDbName;
    private final Path fixturePath;
    private final Path saveDir;
    private final boolean forceWriteFailure;
    private final boolean allowArchiveOracle;
    private final int connectTimeoutMs;
    private final int readTimeoutMs;
    private final List<String> missingForLiveRwl;
    private final List<String> missingForOracle;

    private RwlConfig(
            ImportMode mode,
            String baseUrl,
            String rwlName,
            String rwlPassword,
            List<String> vagonNos,
            String oracleJdbcUrl,
            String oracleUser,
            String oraclePassword,
            String expectedOracleService,
            String expectedOracleDbName,
            Path fixturePath,
            Path saveDir,
            boolean forceWriteFailure,
            boolean allowArchiveOracle,
            int connectTimeoutMs,
            int readTimeoutMs,
            List<String> missingForLiveRwl,
            List<String> missingForOracle) {
        this.mode = mode;
        this.baseUrl = baseUrl;
        this.rwlName = rwlName;
        this.rwlPassword = rwlPassword;
        this.vagonNos = List.copyOf(vagonNos);
        this.oracleJdbcUrl = oracleJdbcUrl;
        this.oracleUser = oracleUser;
        this.oraclePassword = oraclePassword;
        this.expectedOracleService = expectedOracleService;
        this.expectedOracleDbName = expectedOracleDbName;
        this.fixturePath = fixturePath;
        this.saveDir = saveDir;
        this.forceWriteFailure = forceWriteFailure;
        this.allowArchiveOracle = allowArchiveOracle;
        this.connectTimeoutMs = connectTimeoutMs;
        this.readTimeoutMs = readTimeoutMs;
        this.missingForLiveRwl = List.copyOf(missingForLiveRwl);
        this.missingForOracle = List.copyOf(missingForOracle);
    }

    public static RwlConfig from(
            Map<String, String> env,
            ImportMode mode,
            Path fixturePath,
            Path saveDir,
            boolean forceWriteFailure,
            boolean allowArchiveOracle) {
        String baseUrl = firstNonBlank(env.get("RWL_BASE_URL"), DEFAULT_BASE_URL);
        String name = trimToNull(env.get("RWL_NAME"));
        String password = trimToNull(env.get("RWL_PASSWORD"));
        List<String> vagons = parseVagonNos(env.get("RWL_VAGON_NOS"));

        // Prefer RAILDB_* aliases for the new primary database; fall back to ORACLE_* / staging aliases.
        String jdbcUrl = firstNonBlank(
                trimToNull(env.get("RAILDB_JDBC_URL")),
                trimToNull(env.get("ORACLE_JDBC_URL")),
                trimToNull(env.get("RWL_STAGING_JDBC_URL")));
        String user = firstNonBlank(
                trimToNull(env.get("RAILDB_USER")),
                trimToNull(env.get("ORACLE_USER")),
                trimToNull(env.get("RWL_STAGING_USER")));
        String dbPassword = firstNonBlank(
                trimToNull(env.get("RAILDB_PASSWORD")),
                trimToNull(env.get("ORACLE_PASSWORD")),
                trimToNull(env.get("RWL_STAGING_PASSWORD")));

        String expectedService = firstNonBlank(
                trimToNull(env.get("EXPECTED_ORACLE_SERVICE")),
                trimToNull(env.get("EXPECTED_RAILDB_SERVICE")));
        String expectedDbName = firstNonBlank(
                trimToNull(env.get("EXPECTED_ORACLE_DB_NAME")),
                trimToNull(env.get("EXPECTED_RAILDB_DB_NAME")));

        List<String> missingRwl = new ArrayList<>();
        if (fixturePath == null) {
            if (name == null) {
                missingRwl.add("RWL_NAME");
            }
            if (password == null) {
                missingRwl.add("RWL_PASSWORD");
            }
            if (vagons.isEmpty()) {
                missingRwl.add("RWL_VAGON_NOS (semicolon-separated, max " + MAX_WAGONS + ")");
            }
        }

        List<String> missingOracle = new ArrayList<>();
        if (jdbcUrl == null) {
            missingOracle.add("RAILDB_JDBC_URL or ORACLE_JDBC_URL (full JDBC URL to new RAILDB)");
        }
        if (user == null) {
            missingOracle.add("RAILDB_USER or ORACLE_USER (schema user of new RAILDB)");
        }
        if (dbPassword == null) {
            missingOracle.add("RAILDB_PASSWORD or ORACLE_PASSWORD");
        }

        int connectMs = parseInt(env.get("RWL_CONNECT_TIMEOUT_MS"), 15_000);
        int readMs = parseInt(env.get("RWL_READ_TIMEOUT_MS"), 30_000);

        return new RwlConfig(
                mode,
                baseUrl,
                name,
                password,
                vagons,
                jdbcUrl,
                user,
                dbPassword,
                expectedService,
                expectedDbName,
                fixturePath,
                saveDir,
                forceWriteFailure,
                allowArchiveOracle,
                connectMs,
                readMs,
                missingRwl,
                missingOracle);
    }

    public boolean canCallLiveRwl() {
        return fixturePath == null && missingForLiveRwl.isEmpty();
    }

    public boolean canUseOracle() {
        return missingForOracle.isEmpty();
    }

    public ImportMode mode() {
        return mode;
    }

    public String baseUrl() {
        return baseUrl;
    }

    public String rwlName() {
        return rwlName;
    }

    public String rwlPassword() {
        return rwlPassword;
    }

    public List<String> vagonNos() {
        return vagonNos;
    }

    public String oracleJdbcUrl() {
        return oracleJdbcUrl;
    }

    public String oracleUser() {
        return oracleUser;
    }

    public String oraclePassword() {
        return oraclePassword;
    }

    public String expectedOracleService() {
        return expectedOracleService;
    }

    public String expectedOracleDbName() {
        return expectedOracleDbName;
    }

    public Path fixturePath() {
        return fixturePath;
    }

    public Path saveDir() {
        return saveDir;
    }

    public boolean forceWriteFailure() {
        return forceWriteFailure;
    }

    public boolean allowArchiveOracle() {
        return allowArchiveOracle;
    }

    public int connectTimeoutMs() {
        return connectTimeoutMs;
    }

    public int readTimeoutMs() {
        return readTimeoutMs;
    }

    public List<String> missingForLiveRwl() {
        return missingForLiveRwl;
    }

    public List<String> missingForOracle() {
        return missingForOracle;
    }

    public String requestSummary() {
        if (fixturePath != null) {
            return "fixture=" + fixturePath;
        }
        return "view_vagon x" + vagonNos.size() + " wagons=" + String.join(";", vagonNos);
    }

    /** Redacted JDBC summary for logs (host/service markers only, no password). */
    public String jdbcUrlSummary() {
        if (oracleJdbcUrl == null) {
            return "-";
        }
        String u = oracleJdbcUrl;
        // Strip user/password only in forms like thin:user/pass@//host...
        u = u.replaceAll("(?i)(jdbc:oracle:thin:)([^/@]+)(/)([^@]*)@", "$1***$3***@");
        return u;
    }

    private static List<String> parseVagonNos(String raw) {
        if (raw == null || raw.isBlank()) {
            return List.of();
        }
        List<String> out = new ArrayList<>();
        for (String part : raw.split("[;,\n]")) {
            String n = part.trim();
            if (!n.isEmpty()) {
                out.add(n);
            }
        }
        if (out.size() > MAX_WAGONS) {
            throw new IllegalArgumentException(
                    "RWL_VAGON_NOS has " + out.size() + " numbers; max is " + MAX_WAGONS);
        }
        return out;
    }

    private static String trimToNull(String v) {
        if (v == null) {
            return null;
        }
        String t = v.trim();
        return t.isEmpty() ? null : t;
    }

    private static String firstNonBlank(String... values) {
        return Arrays.stream(values).filter(Objects::nonNull).filter(s -> !s.isBlank()).findFirst().orElse(null);
    }

    private static int parseInt(String raw, int defaultValue) {
        if (raw == null || raw.isBlank()) {
            return defaultValue;
        }
        return Integer.parseInt(raw.trim());
    }
}
