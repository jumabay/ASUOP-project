package platform.rwl.proto;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import platform.rwl.proto.db.ConnectionIdentity;

class ConnectionIdentityTest {

    @Test
    void refusesLegacyRacByDefault() {
        ConnectionIdentity id =
                new ConnectionIdentity("SOME_USER", "SOME_USER", "RAC", "RAC.WORLD", "RAC", "Oracle Database");
        assertTrue(id.validateAgainstExpected(null, null, false).isPresent());
    }

    @Test
    void acceptsPinnedRaildb() {
        ConnectionIdentity id =
                new ConnectionIdentity("APP_USER", "APP_USER", "RAILDB", "RAILDB.WORLD", "RAILDB", "Oracle Database");
        assertTrue(id.validateAgainstExpected("RAILDB.WORLD", "RAILDB", false).isEmpty());
    }
}
