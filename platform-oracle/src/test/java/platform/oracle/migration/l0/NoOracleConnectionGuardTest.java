package platform.oracle.migration.l0;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import org.junit.jupiter.api.Test;

/**
 * Safety guard: L0 package must not reference javax.sql / oracle JDBC types.
 */
class NoOracleConnectionGuardTest {

    @Test
    void selectionEngineHasNoDataSourceFields() {
        for (Field field : SelectionEngine.class.getDeclaredFields()) {
            String type = field.getType().getName();
            assertFalse(type.contains("DataSource"), field.getName());
            assertFalse(type.contains("Connection"), field.getName());
            assertFalse(type.startsWith("oracle."), field.getName());
        }
        for (Method method : SelectionEngine.class.getDeclaredMethods()) {
            if (!Modifier.isPublic(method.getModifiers()) && !Modifier.isProtected(method.getModifiers())) {
                continue;
            }
            Arrays.stream(method.getParameterTypes()).forEach(t -> {
                assertFalse(t.getName().contains("DataSource"));
                assertFalse(t.getName().startsWith("oracle."));
            });
        }
    }

    @Test
    void fixtureLoadDoesNotThrow() {
        assertDoesNotThrow(() -> MigrationFixturePack.load(FixturePaths.requireV1()));
    }
}
