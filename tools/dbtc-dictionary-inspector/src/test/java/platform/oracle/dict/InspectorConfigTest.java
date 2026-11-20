package platform.oracle.dict;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.Map;
import org.junit.jupiter.api.Test;

class InspectorConfigTest {

    @Test
    void missingCredentialsFail() {
        InspectorConfig cfg = InspectorConfig.fromEnv(Map.of(), Path.of("."));
        assertFalse(cfg.ok());
        assertTrue(cfg.problems().stream().anyMatch(p -> p.contains("user")));
        assertTrue(cfg.problems().stream().anyMatch(p -> p.contains("password")));
    }

    @Test
    void forbiddenUserRejected() {
        InspectorConfig cfg = InspectorConfig.fromEnv(
                Map.of(
                        "ORACLE_USER", "RAIL_APP",
                        "ORACLE_PASSWORD", "x",
                        "ORACLE_JDBC_URL", InspectorConfig.DEFAULT_JDBC_URL),
                Path.of("."));
        assertFalse(cfg.ok());
        assertTrue(cfg.problems().stream().anyMatch(p -> p.contains("Forbidden")));
    }

    @Test
    void approvedShapeAccepted() {
        InspectorConfig cfg = InspectorConfig.fromEnv(
                Map.of(
                        "ORACLE_USER", "RAILWAY_READONLY",
                        "ORACLE_PASSWORD", "secret-not-logged",
                        "ORACLE_JDBC_URL", InspectorConfig.DEFAULT_JDBC_URL),
                Path.of("."));
        assertTrue(cfg.ok());
    }
}
