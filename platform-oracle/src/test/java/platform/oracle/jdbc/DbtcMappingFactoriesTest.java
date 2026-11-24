package platform.oracle.jdbc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import platform.dislocation.StateReportLine;
import platform.fleet.WagonCard;

class DbtcMappingFactoriesTest {

    @Test
    void passportCoreOnly_mapsCodeAndInventoryOne_leavesRefsEmpty() {
        WagonCard card = DbtcWagonCardFactory.coreOnly("54787361", 1);
        assertEquals("54787361", card.wagonCode());
        assertEquals(Boolean.TRUE, card.inventoryPark());
        assertEquals("", card.model());
        assertEquals("", card.wagonKind());
        assertEquals("", card.ownerName());
        assertEquals("", card.homeStationCode());
        assertEquals("", card.homeStationName());
        assertNull(card.currentState());
    }

    @Test
    void inventoryPark_onlyOneIsTrue_nullAndOthersUnknown() {
        assertNull(DbtcWagonCardFactory.toInventoryBoolean(null));
        assertNull(DbtcWagonCardFactory.toInventoryBoolean(0));
        assertNull(DbtcWagonCardFactory.toInventoryBoolean(2));
        assertNull(DbtcWagonCardFactory.toInventoryBoolean(BigDecimal.ZERO));
        assertNull(DbtcWagonCardFactory.toInventoryBoolean(new BigDecimal("1.5")));
        assertEquals(Boolean.TRUE, DbtcWagonCardFactory.toInventoryBoolean(1));
        assertEquals(Boolean.TRUE, DbtcWagonCardFactory.toInventoryBoolean(BigDecimal.ONE));
        assertEquals(Boolean.TRUE, DbtcWagonCardFactory.toInventoryBoolean(1L));
    }

    @Test
    void operationCode_prefersIdCode_asString_keepsLeadingZeros() {
        assertEquals("04", DbtcOperationCode.resolve("04", "4"));
        assertEquals("04", DbtcOperationCode.resolve("04", null));
        assertEquals("81", DbtcOperationCode.resolve("81", "очи"));
        assertEquals("ABC", DbtcOperationCode.resolve(null, "ABC"));
        assertEquals("ABC", DbtcOperationCode.resolve("  ", " ABC "));
        assertEquals("", DbtcOperationCode.resolve(null, null));
        assertEquals("", DbtcOperationCode.resolve("", "  "));
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
