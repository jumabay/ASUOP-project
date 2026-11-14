package platform.oracle.smoke;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

/**
 * Classifies Oracle session privileges/roles for the JDBC smoke gate.
 *
 * <p>{@code READ ANY TABLE} is read-only. {@code SELECT ANY TABLE} is a broad read grant
 * (NOTE, not STOP). Object privilege {@code INHERIT PRIVILEGES} (Oracle 12c+ IR control on a
 * USER) is also NOTE — it is not table DML. System privilege {@code INHERIT ANY PRIVILEGES}
 * remains STOP. Unknown and write/admin privileges remain blocked.
 */
public final class PrivilegeClassifier {

    public enum Kind {
        /** Expected for a read-only login. */
        ALLOWED_READ,
        /** Broad read (e.g. SELECT ANY TABLE) — note, do not treat as write. */
        BROAD_READ,
        /**
         * Recognized non-write privilege that warrants a NOTE (e.g. object INHERIT PRIVILEGES).
         * Not an automatic STOP.
         */
        NOTE,
        /** Insert/update/delete/DDL/execute/admin — stop the smoke. */
        WRITE_OR_ADMIN,
        /** Unknown privilege — treat cautiously as write/admin for the smoke gate. */
        UNKNOWN_TREAT_AS_RISK
    }

    private static final Set<String> ALLOWED_READ = Set.of(
            "CREATE SESSION",
            "READ ANY TABLE",
            "SELECT",
            "READ"
    );

    private static final Set<String> BROAD_READ = Set.of(
            "SELECT ANY TABLE"
    );

    /**
     * Object privilege on USER (12.1+): allows an IR unit owner to run with that invoker's
     * privileges. Does not grant INSERT/UPDATE/DELETE on tables. Default installs often grant
     * {@code INHERIT PRIVILEGES ON USER <name> TO PUBLIC}.
     */
    private static final Set<String> NOTE_ONLY = Set.of(
            "INHERIT PRIVILEGES"
    );

    private static final Set<String> WRITE_OR_ADMIN = Set.of(
            "INSERT ANY TABLE",
            "UPDATE ANY TABLE",
            "DELETE ANY TABLE",
            "MERGE ANY VIEW",
            "ALTER ANY TABLE",
            "DROP ANY TABLE",
            "CREATE ANY TABLE",
            "CREATE ANY PROCEDURE",
            "CREATE ANY FUNCTION",
            "CREATE ANY PACKAGE",
            "CREATE ANY TRIGGER",
            "EXECUTE ANY PROCEDURE",
            "EXECUTE ANY FUNCTION",
            "EXECUTE ANY PROGRAM",
            "GRANT ANY PRIVILEGE",
            "GRANT ANY ROLE",
            "GRANT ANY OBJECT PRIVILEGE",
            // System privilege: inherit from ANY invoker — treat as elevated / stop.
            "INHERIT ANY PRIVILEGES",
            "UNLIMITED TABLESPACE",
            "DBA",
            "SYSDBA",
            "SYSOPER",
            "SYSASM",
            "SYSBACKUP",
            "SYSDG",
            "SYSKM",
            "ALTER SYSTEM",
            "ALTER DATABASE"
    );

    private static final Set<String> WRITE_ROLE_MARKERS = Set.of(
            "DBA",
            "RESOURCE",
            "IMP_FULL_DATABASE",
            "EXP_FULL_DATABASE",
            "DATAPUMP_EXP_FULL_DATABASE",
            "DATAPUMP_IMP_FULL_DATABASE",
            "SCHEDULER_ADMIN"
    );

    private PrivilegeClassifier() {
    }

    public static Kind classifySessionPrivilege(String privilege) {
        if (privilege == null || privilege.isBlank()) {
            return Kind.UNKNOWN_TREAT_AS_RISK;
        }
        String upper = privilege.trim().toUpperCase(Locale.ROOT);

        if (ALLOWED_READ.contains(upper)) {
            return Kind.ALLOWED_READ;
        }
        if (BROAD_READ.contains(upper)) {
            return Kind.BROAD_READ;
        }
        if (NOTE_ONLY.contains(upper)) {
            return Kind.NOTE;
        }
        if (WRITE_OR_ADMIN.contains(upper)) {
            return Kind.WRITE_OR_ADMIN;
        }

        // Prefix heuristics for object-level and ANY privileges not listed above.
        if (upper.startsWith("READ ") || upper.equals("READ")) {
            return Kind.ALLOWED_READ;
        }
        if (upper.equals("SELECT ANY TABLE") || upper.startsWith("SELECT ")) {
            // SELECT <object> is read; SELECT ANY TABLE already handled; other SELECT ANY * → broad.
            if (upper.startsWith("SELECT ANY ")) {
                return Kind.BROAD_READ;
            }
            return Kind.ALLOWED_READ;
        }
        // Exact object privilege only — do not treat "INHERIT ANY PRIVILEGES" as NOTE via prefix.
        if ("INHERIT PRIVILEGES".equals(upper)) {
            return Kind.NOTE;
        }
        if (upper.startsWith("INHERIT ANY")) {
            return Kind.WRITE_OR_ADMIN;
        }
        if (upper.startsWith("INSERT") || upper.startsWith("UPDATE")
                || upper.startsWith("DELETE") || upper.startsWith("MERGE")
                || upper.startsWith("DROP") || upper.startsWith("ALTER")
                || upper.startsWith("CREATE") || upper.startsWith("EXECUTE")
                || upper.startsWith("GRANT") || upper.startsWith("FLASHBACK")
                || upper.startsWith("DEBUG") || upper.startsWith("WRITE")
                || upper.startsWith("BECOME") || upper.startsWith("ADMINISTER")) {
            return Kind.WRITE_OR_ADMIN;
        }

        return Kind.UNKNOWN_TREAT_AS_RISK;
    }

    public static boolean isDangerousRole(String role) {
        if (role == null || role.isBlank()) {
            return false;
        }
        return WRITE_ROLE_MARKERS.contains(role.trim().toUpperCase(Locale.ROOT));
    }

    /**
     * Classify a USER_TAB_PRIVS privilege kind (name only, without object).
     */
    public static Kind classifyObjectPrivilegeKind(String privilegeKind) {
        if (privilegeKind == null || privilegeKind.isBlank()) {
            return Kind.UNKNOWN_TREAT_AS_RISK;
        }
        // May arrive as "SELECT=12" from smoke aggregation — strip count suffix.
        String name = privilegeKind.trim().toUpperCase(Locale.ROOT);
        int eq = name.indexOf('=');
        if (eq > 0) {
            name = name.substring(0, eq).trim();
        }
        return classifySessionPrivilege(name);
    }

    public static Assessment assess(
            Collection<String> sessionPrivileges,
            Collection<String> sessionRoles,
            Collection<String> objectPrivilegeKinds) {
        List<String> dangerous = new ArrayList<>();
        List<String> broadReadNotes = new ArrayList<>();
        List<String> allowed = new ArrayList<>();

        for (String priv : nullToEmpty(sessionPrivileges)) {
            Kind kind = classifySessionPrivilege(priv);
            String label = priv == null ? "<null>" : priv.trim().toUpperCase(Locale.ROOT);
            switch (kind) {
                case ALLOWED_READ -> allowed.add("PRIV:" + label);
                case BROAD_READ, NOTE -> broadReadNotes.add("PRIV:" + label);
                case WRITE_OR_ADMIN, UNKNOWN_TREAT_AS_RISK -> dangerous.add("PRIV:" + label + " (" + kind + ")");
            }
        }

        for (String role : nullToEmpty(sessionRoles)) {
            if (isDangerousRole(role)) {
                dangerous.add("ROLE:" + role.trim().toUpperCase(Locale.ROOT));
            }
        }

        for (String kindRaw : nullToEmpty(objectPrivilegeKinds)) {
            Kind kind = classifyObjectPrivilegeKind(kindRaw);
            String label = kindRaw == null ? "<null>" : kindRaw.trim();
            switch (kind) {
                case ALLOWED_READ -> { /* object-level read is fine */ }
                case BROAD_READ, NOTE -> broadReadNotes.add("TABKIND:" + label);
                case WRITE_OR_ADMIN, UNKNOWN_TREAT_AS_RISK ->
                        dangerous.add("TABKIND:" + label + " (" + kind + ")");
            }
        }

        return new Assessment(List.copyOf(allowed), List.copyOf(broadReadNotes), List.copyOf(dangerous));
    }

    private static Collection<String> nullToEmpty(Collection<String> values) {
        return values == null ? List.of() : values;
    }

    public record Assessment(
            List<String> allowedRead,
            List<String> broadReadNotes,
            List<String> writeOrAdmin
    ) {
        public Assessment {
            Objects.requireNonNull(allowedRead);
            Objects.requireNonNull(broadReadNotes);
            Objects.requireNonNull(writeOrAdmin);
        }

        public boolean isSafeToContinue() {
            return writeOrAdmin.isEmpty();
        }
    }
}
