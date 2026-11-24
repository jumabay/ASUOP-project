package platform.oracle.jdbc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import platform.dislocation.StateReportLine;
import platform.fleet.WagonCard;
import platform.fleet.WagonCurrentState;

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
    void currentState_fromMaster_mapsStationOpLocationDate_trainAlwaysNull() {
        Timestamp loc = Timestamp.valueOf(LocalDateTime.of(2026, 11, 21, 1, 25, 0));
        WagonCurrentState current = DbtcWagonCurrentStateFactory.fromMasterRow(
                "69260", "04", "4", loc);
        assertEquals("69260", current.stationCode());
        assertEquals("04", current.operationCode());
        assertEquals("2026-11-21T01:25", current.dislocationTime());
        assertNull(current.trainIndex());
    }

    @Test
    void currentState_orphanSoftRefsAndNullDate_yieldNullBlock() {
        assertNull(DbtcWagonCurrentStateFactory.fromMasterRow(null, null, null, null));
        assertNull(DbtcWagonCurrentStateFactory.fromMasterRow("  ", "", "  ", null));
    }

    @Test
    void currentState_nullLocationDate_staysNull_noFallback() {
        WagonCurrentState current = DbtcWagonCurrentStateFactory.fromMasterRow(
                "69260", "81", null, null);
        assertEquals("69260", current.stationCode());
        assertEquals("81", current.operationCode());
        assertNull(current.dislocationTime());
        assertNull(current.trainIndex());
    }

    @Test
    void currentState_partialOrphanStation_keepsOtherFields() {
        Timestamp loc = Timestamp.valueOf(LocalDateTime.of(2026, 11, 21, 1, 25, 30));
        WagonCurrentState current = DbtcWagonCurrentStateFactory.fromMasterRow(
                null, "04", "4", loc);
        assertNull(current.stationCode());
        assertEquals("04", current.operationCode());
        assertEquals("2026-11-21T01:25:30", current.dislocationTime());
    }

    @Test
    void passportFactory_passesCurrentThrough() {
        WagonCurrentState current = DbtcWagonCurrentStateFactory.fromMasterRow(
                "7100", "05", null, Timestamp.valueOf("2024-01-16 06:00:00"));
        WagonCard card = DbtcWagonCardFactory.fromCoreRow(
                "54787361", "68390", "HOME", 1, "m", "k", "o", current);
        assertEquals(current, card.currentState());
    }

    @Test
    void dayLine_usesGettingDate_notLocationIdsAsCodes() {
        LocalDateTime getting = LocalDateTime.of(2026, 11, 24, 2, 0);
        StateReportLine line = DbtcStateReportLineFactory.coreOnly(588503L, "54787361", getting);
        assertEquals(588503L, line.reportId());
        assertEquals("54787361", line.wagonCode());
        assertEquals(getting, line.snapshotTime());
        assertEquals(line.snapshotTime(), line.dislocationTime());
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
        assertEquals(getting, line.snapshotTime());
        assertEquals(line.snapshotTime(), line.dislocationTime());
    }

    @Test
    void sqlDrafts_mentionGettingDateAxisAndEcurMaster() {
        assertTrue(DbtcA1A2Sql.STATE_REPORT_DAY_CORE.contains("getting_date"));
        assertTrue(DbtcA1A2Sql.STATE_REPORT_DAY_WITH_CODES.contains("dbtc.stations"));
        assertTrue(DbtcA1A2Sql.STATE_REPORT_DAY_WITH_CODES.contains("dbtc.operations"));
        assertTrue(DbtcA1A2Sql.WAGON_PASSPORT_WITH_HOME_STATION.contains("dbtc.stations"));
        assertTrue(DbtcA1A2Sql.WAGON_CURRENT_STATE_MASTER.contains("locationdate"));
        assertTrue(DbtcA1A2Sql.WAGON_CURRENT_STATE_MASTER.contains("locationstation_id"));
        assertTrue(DbtcA1A2Sql.WAGON_TRAIN_COLUMN_METADATA.contains("%TRAIN%"));
        assertTrue(DbtcPassportMapping.FK_OWNER.contains("OWNERS"));
        assertTrue(DbtcPassportMapping.SRR_LOCATION_STATION_FK.contains("LOCATION_STATION_ID"));
    }
}
