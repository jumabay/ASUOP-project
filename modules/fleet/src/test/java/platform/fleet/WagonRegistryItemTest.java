package platform.fleet;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class WagonRegistryItemTest {

    @Test
    void fromCardMapsPassportOnly() {
        WagonCard card = new WagonCard(
                "SYN-00001",
                "12-132",
                "КРЫТЫЙ",
                "SYN-OWNER",
                "6900",
                "SYN-STATION",
                true,
                new WagonCurrentState("7100", "05", "2024-01-16T06:00:00", "TRN-DIFF"));

        WagonRegistryItem item = WagonRegistryItem.fromCard(card);
        assertEquals("SYN-00001", item.wagonCode());
        assertEquals("12-132", item.model());
        assertEquals("КРЫТЫЙ", item.wagonKind());
        assertEquals("SYN-OWNER", item.ownerName());
        assertEquals("6900", item.homeStationCode());
        assertEquals("SYN-STATION", item.homeStationName());
        assertEquals(true, item.inventoryPark());
    }

    @Test
    void watchedFromCardIgnoresCurrentState() {
        WagonCurrentState current =
                new WagonCurrentState("7100", "05", "2024-01-16T06:00:00", "TRN-DIFF");
        WagonCard card = new WagonCard(
                "SYN-00001", "12-132", "КРЫТЫЙ", "SYN-OWNER", "6900", "SYN-STATION", true, current);

        WatchedWagonItem watched = WatchedWagonItem.fromCard(card);
        assertEquals("SYN-00001", watched.wagonCode());
        assertEquals("12-132", watched.model());
        assertEquals("КРЫТЫЙ", watched.wagonKind());
        assertEquals(current, card.currentState());
    }
}
