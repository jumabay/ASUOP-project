package platform.oracle.dict;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

/**
 * Same gate semantics as tools/oracle-jdbc-smoke PrivilegeClassifier.
 * Copied to keep the inspector a standalone process without cross-module compile deps.
 */
public final class PrivilegeClassifier {

    public enum Kind {
        ALLOWED_READ,
        BROAD_READ,
        NOTE,
        WRITE_OR_ADMIN,
        UNKNOWN_TREAT_AS_RISK
    }

    private static final Set<String> ALLOWED_READ = Set.of(
            "CREATE SESSION", "READ ANY TABLE", "SELECT", "READ");

    private static final Set<String> BROAD_READ = Set.of("SELECT ANY TABLE");

    private static final Set<String> NOTE_ONLY = Set.of("INHERIT PRIVILEGES");

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
            "ALTER DATABASE");

    private static final Set<String> WRITE_ROLE_MARKERS = Set.of(
            "DBA",
            "RESOURCE",
            "IMP_FULL_DATABASE",
            "EXP_FULL_DATABASE",
            "DATAPUMP_EXP_FULL_DATABASE",
            "DATAPUMP_IMP_FULL_DATABASE",
            "SCHEDULER_ADMIN");

    private PrivilegeClassifier() {}

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
        if (upper.startsWith("READ ") || upper.equals("READ")) {
            return Kind.ALLOWED_READ;
        }
        if (upper.equals("SELECT ANY TABLE") || upper.startsWith("SELECT ")) {
            if (upper.startsWith("SELECT ANY ")) {
                return Kind.BROAD_READ;
            }
            return Kind.ALLOWED_READ;
        }
        if ("INHERIT PRIVILEGES".equals(upper)) {
            return Kind.NOTE;
        }
        if (upper.startsWith("INHERIT ANY")) {
            return Kind.WRITE_OR_ADMIN;
        }
        if (upper.startsWith("INSERT")
                || upper.startsWith("UPDATE")
                || upper.startsWith("DELETE")
                || upper.startsWith("MERGE")
                || upper.startsWith("DROP")
                || upper.startsWith("ALTER")
                || upper.startsWith("CREATE")
                || upper.startsWith("EXECUTE")
                || upper.startsWith("GRANT")
                || upper.startsWith("FLASHBACK")
                || upper.startsWith("DEBUG")
                || upper.startsWith("WRITE")
                || upper.startsWith("BECOME")
                || upper.startsWith("ADMINISTER")) {
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

    public static Kind classifyObjectPrivilegeKind(String privilegeKind) {
        if (privilegeKind == null || privilegeKind.isBlank()) {
            return Kind.UNKNOWN_TREAT_AS_RISK;
        }
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
                case WRITE_OR_ADMIN, UNKNOWN_TREAT_AS_RISK ->
                        dangerous.add("PRIV:" + label + " (" + kind + ")");
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
                case ALLOWED_READ -> { }
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
            List<String> allowedRead, List<String> broadReadNotes, List<String> writeOrAdmin) {
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
