package platform.oracle.jdbc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import platform.dislocation.StateReportLine;
import platform.fleet.WagonCard;

class DbtcMappingFactoriesTest {

    @Test
    void passportCoreOnly_mapsCodeAndInventory_leavesRefsEmpty() {
        WagonCard card = DbtcWagonCardFactory.coreOnly("54787361", 1);
        assertEquals("54787361", card.wagonCode());
        assertTrue(card.inventoryPark());
        assertEquals("", card.model());
        assertEquals("", card.wagonKind());
        assertEquals("", card.ownerName());
        assertEquals("", card.homeStationCode());
        assertEquals("", card.homeStationName());
        assertNull(card.currentState());
    }

    @Test
    void inventoryPark_nullAndZeroAreFalse() {
        assertFalse(DbtcWagonCardFactory.toInventoryBoolean(null));
        assertFalse(DbtcWagonCardFactory.toInventoryBoolean(0));
        assertTrue(DbtcWagonCardFactory.toInventoryBoolean(1));
    }

    @Test
    void dayLine_usesGettingDate_notLocationIdsAsCodes() {
        LocalDateTime getting = LocalDateTime.of(2026, 11, 24, 2, 0);
        StateReportLine line = DbtcStateReportLineFactory.coreOnly(588503L, "54787361", getting);
        assertEquals(588503L, line.reportId());
        assertEquals("54787361", line.wagonCode());
        assertEquals(getting, line.dislocationTime());
        assertEquals("", line.stationCode());
        assertEquals("", line.operationCode());
    }

    @Test
    void dayLine_withResolvedCodes_stillUsesGettingAxis() {
        LocalDateTime getting = LocalDateTime.of(2026, 11, 24, 11, 0);
        StateReportLine line = DbtcStateReportLineFactory.fromGettingAxis(
                588541L, "54787361", getting, "6900", "04");
        assertEquals("6900", line.stationCode());
        assertEquals("04", line.operationCode());
        assertEquals(getting, line.dislocationTime());
    }

    @Test
    void sqlDrafts_mentionGettingDateAxisAndRefApprovalJoins() {
        assertTrue(DbtcA1A2Sql.STATE_REPORT_DAY_CORE.contains("getting_date"));
        assertTrue(DbtcA1A2Sql.STATE_REPORT_DAY_WITH_CODES.contains("dbtc.stations"));
        assertTrue(DbtcA1A2Sql.STATE_REPORT_DAY_WITH_CODES.contains("dbtc.operations"));
        assertTrue(DbtcA1A2Sql.WAGON_PASSPORT_WITH_HOME_STATION.contains("dbtc.stations"));
        assertTrue(DbtcPassportMapping.FK_OWNER.contains("OWNERS"));
        assertTrue(DbtcPassportMapping.SRR_LOCATION_STATION_FK.contains("LOCATION_STATION_ID"));
    }
}
