package platform.oracle.dict;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/** Credentials and paths from environment only. Never logs password. */
public final class InspectorConfig {

    public static final String DEFAULT_JDBC_URL = "jdbc:oracle:thin:@//tc-asuvp-03:1521/RAC.WORLD";
    public static final String EXPECTED_DB_NAME = "RAC";
    public static final String EXPECTED_SERVICE = "RAC.WORLD";

    private static final Set<String> FORBIDDEN_USERS = Set.of(
            "RAIL_APP", "DBTC", "SYS", "SYSTEM", "SYSDBA", "SYSOPER");

    private final String jdbcUrl;
    private final String user;
    private final String password;
    private final Path sqlDir;
    private final Path outDir;
    private final List<String> problems;

    private InspectorConfig(
            String jdbcUrl, String user, String password, Path sqlDir, Path outDir, List<String> problems) {
        this.jdbcUrl = jdbcUrl;
        this.user = user;
        this.password = password;
        this.sqlDir = sqlDir;
        this.outDir = outDir;
        this.problems = List.copyOf(problems);
    }

    public static InspectorConfig fromEnv(Map<String, String> env, Path repoRoot) {
        List<String> problems = new ArrayList<>();
        String url = first(env, "P3B_JDBC_URL", "ORACLE_JDBC_URL");
        if (isBlank(url)) {
            url = DEFAULT_JDBC_URL;
        }
        String user = first(env, "P3B_ORACLE_USER", "ORACLE_USER", "DBTC_DICT_USER");
        String password = first(env, "P3B_ORACLE_PASSWORD", "ORACLE_PASSWORD", "DBTC_DICT_PASSWORD");

        if (isBlank(user)) {
            problems.add("Missing read-only user env: set P3B_ORACLE_USER or ORACLE_USER");
        } else if (FORBIDDEN_USERS.contains(user.trim().toUpperCase(Locale.ROOT))) {
            problems.add("Forbidden user for P3B dictionary: " + user
                    + " (use DBA-approved read-only account, not RAIL_APP/DBTC/SYS)");
        }
        if (isBlank(password)) {
            problems.add("Missing password env: set P3B_ORACLE_PASSWORD or ORACLE_PASSWORD");
        }
        if (!url.contains("tc-asuvp-03") || !url.toLowerCase(Locale.ROOT).contains("rac.world")) {
            problems.add("JDBC URL must target tc-asuvp-03 / RAC.WORLD for this P3B authorization");
        }

        Path sqlDir = repoRoot.resolve("docs/architecture/sql/dba/p3b");
        Path outDir = repoRoot.resolve("docs/architecture/sql/dba/p3b_results");
        return new InspectorConfig(url, user, password, sqlDir, outDir, problems);
    }

    public boolean ok() {
        return problems.isEmpty();
    }

    public List<String> problems() {
        return problems;
    }

    public String jdbcUrl() {
        return jdbcUrl;
    }

    public String user() {
        return user;
    }

    public String password() {
        return password;
    }

    public Path sqlDir() {
        return sqlDir;
    }

    public Path outDir() {
        return outDir;
    }

    public String redactedUrl() {
        return jdbcUrl.replaceAll("(?i)^(jdbc:oracle:thin:)[^@/]+@", "$1****@");
    }

    private static String first(Map<String, String> env, String... keys) {
        for (String k : keys) {
            String v = env.get(k);
            if (!isBlank(v)) {
                return v.trim();
            }
        }
        return null;
    }

    private static boolean isBlank(String v) {
        return v == null || v.isBlank();
    }
}
