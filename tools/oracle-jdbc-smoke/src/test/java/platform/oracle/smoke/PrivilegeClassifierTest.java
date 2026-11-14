package platform.oracle.smoke;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;
import platform.oracle.smoke.PrivilegeClassifier.Kind;

class PrivilegeClassifierTest {

    @Test
    void readAnyTableIsAllowedReadNotWrite() {
        assertEquals(Kind.ALLOWED_READ, PrivilegeClassifier.classifySessionPrivilege("READ ANY TABLE"));
        assertEquals(Kind.ALLOWED_READ, PrivilegeClassifier.classifySessionPrivilege("read any table"));
    }

    @Test
    void createSessionIsAllowedRead() {
        assertEquals(Kind.ALLOWED_READ, PrivilegeClassifier.classifySessionPrivilege("CREATE SESSION"));
    }

    @Test
    void selectAnyTableIsBroadReadNotAutomaticWriteStop() {
        assertEquals(Kind.BROAD_READ, PrivilegeClassifier.classifySessionPrivilege("SELECT ANY TABLE"));

        var assessment = PrivilegeClassifier.assess(
                List.of("CREATE SESSION", "SELECT ANY TABLE"),
                List.of(),
                List.of());
        assertTrue(assessment.isSafeToContinue());
        assertTrue(assessment.broadReadNotes().stream().anyMatch(s -> s.contains("SELECT ANY TABLE")));
        assertTrue(assessment.writeOrAdmin().isEmpty());
    }

    @Test
    void railwayReadonlyObservedPrivsPass() {
        var assessment = PrivilegeClassifier.assess(
                List.of("CREATE SESSION", "READ ANY TABLE"),
                List.of(),
                List.of("INHERIT PRIVILEGES=1"));
        assertTrue(assessment.isSafeToContinue());
        assertTrue(assessment.writeOrAdmin().isEmpty());
        assertTrue(assessment.broadReadNotes().stream().anyMatch(s -> s.contains("INHERIT PRIVILEGES")));
        assertEquals(2, assessment.allowedRead().size());
    }

    @Test
    void inheritPrivilegesObjectPrivilegeIsNoteNotStop() {
        assertEquals(Kind.NOTE, PrivilegeClassifier.classifySessionPrivilege("INHERIT PRIVILEGES"));
        assertEquals(Kind.NOTE, PrivilegeClassifier.classifyObjectPrivilegeKind("INHERIT PRIVILEGES=1"));

        var assessment = PrivilegeClassifier.assess(
                List.of("CREATE SESSION", "READ ANY TABLE"),
                List.of(),
                List.of("INHERIT PRIVILEGES=1"));
        assertTrue(assessment.isSafeToContinue());
        assertTrue(assessment.writeOrAdmin().isEmpty());
        assertTrue(assessment.broadReadNotes().stream().anyMatch(s -> s.contains("INHERIT PRIVILEGES")));
    }

    @Test
    void inheritAnyPrivilegesRemainsStop() {
        assertEquals(Kind.WRITE_OR_ADMIN,
                PrivilegeClassifier.classifySessionPrivilege("INHERIT ANY PRIVILEGES"));

        var assessment = PrivilegeClassifier.assess(
                List.of("CREATE SESSION", "READ ANY TABLE", "INHERIT ANY PRIVILEGES"),
                List.of(),
                List.of());
        assertFalse(assessment.isSafeToContinue());
        assertTrue(assessment.writeOrAdmin().stream().anyMatch(s -> s.contains("INHERIT ANY PRIVILEGES")));
    }

    @Test
    void insertUpdateDeleteAlterDropExecuteRemainBlocked() {
        assertEquals(Kind.WRITE_OR_ADMIN, PrivilegeClassifier.classifySessionPrivilege("INSERT ANY TABLE"));
        assertEquals(Kind.WRITE_OR_ADMIN, PrivilegeClassifier.classifySessionPrivilege("UPDATE ANY TABLE"));
        assertEquals(Kind.WRITE_OR_ADMIN, PrivilegeClassifier.classifySessionPrivilege("DELETE ANY TABLE"));
        assertEquals(Kind.WRITE_OR_ADMIN, PrivilegeClassifier.classifySessionPrivilege("ALTER ANY TABLE"));
        assertEquals(Kind.WRITE_OR_ADMIN, PrivilegeClassifier.classifySessionPrivilege("DROP ANY TABLE"));
        assertEquals(Kind.WRITE_OR_ADMIN, PrivilegeClassifier.classifySessionPrivilege("CREATE ANY PROCEDURE"));
        assertEquals(Kind.WRITE_OR_ADMIN, PrivilegeClassifier.classifySessionPrivilege("EXECUTE ANY PROCEDURE"));
        assertEquals(Kind.WRITE_OR_ADMIN, PrivilegeClassifier.classifySessionPrivilege("GRANT ANY PRIVILEGE"));
        assertEquals(Kind.WRITE_OR_ADMIN, PrivilegeClassifier.classifySessionPrivilege("ALTER SYSTEM"));
    }

    @Test
    void dangerousRolesStillBlocked() {
        assertTrue(PrivilegeClassifier.isDangerousRole("DBA"));
        assertTrue(PrivilegeClassifier.isDangerousRole("RESOURCE"));
        assertFalse(PrivilegeClassifier.isDangerousRole("CONNECT"));

        var assessment = PrivilegeClassifier.assess(
                List.of("CREATE SESSION", "READ ANY TABLE"),
                List.of("DBA"),
                List.of());
        assertFalse(assessment.isSafeToContinue());
        assertTrue(assessment.writeOrAdmin().stream().anyMatch(s -> s.contains("ROLE:DBA")));
    }

    @Test
    void objectPrivilegeKindsClassifyCorrectly() {
        assertEquals(Kind.ALLOWED_READ, PrivilegeClassifier.classifyObjectPrivilegeKind("SELECT=12"));
        assertEquals(Kind.ALLOWED_READ, PrivilegeClassifier.classifyObjectPrivilegeKind("READ"));
        assertEquals(Kind.WRITE_OR_ADMIN, PrivilegeClassifier.classifyObjectPrivilegeKind("INSERT=3"));
        assertEquals(Kind.WRITE_OR_ADMIN, PrivilegeClassifier.classifyObjectPrivilegeKind("EXECUTE=1"));
    }

    @Test
    void mixedWriteAmongReadStopsAssessment() {
        var assessment = PrivilegeClassifier.assess(
                List.of("CREATE SESSION", "READ ANY TABLE", "INSERT ANY TABLE"),
                List.of(),
                List.of());
        assertFalse(assessment.isSafeToContinue());
        assertTrue(assessment.writeOrAdmin().stream().anyMatch(s -> s.contains("INSERT ANY TABLE")));
    }
}
