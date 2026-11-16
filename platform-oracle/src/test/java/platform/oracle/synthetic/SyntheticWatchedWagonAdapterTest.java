package platform.oracle.synthetic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import platform.fleet.WagonRegistryCriteria;
import platform.fleet.WagonRegistrySort;
import platform.fleet.WatchedWagonPage;

class SyntheticWatchedWagonAdapterTest {

    private SyntheticWatchedWagonAdapter adapter;
    private SyntheticCatalog catalog;

    @BeforeEach
    void setUp() {
        ObjectMapper mapper = new ObjectMapper();
        catalog = new SyntheticCatalog(mapper);
        adapter = new SyntheticWatchedWagonAdapter(mapper, new SyntheticWagonCardAdapter(mapper));
    }

    @Test
    void watchedHas60UniqueCodesSubsetOfCatalog() {
        WatchedWagonPage page = adapter.findPage(
                new WagonRegistryCriteria(null, 0, 200, WagonRegistrySort.CODE_ASC));
        assertEquals(60, page.totalElements());
        assertEquals(1, page.totalPages());
        assertEquals(60, page.content().size());
        assertTrue(page.content().stream().allMatch(i -> catalog.contains(i.wagonCode())));
        assertTrue(catalog.contains("SYN-00250"));
        assertTrue(page.content().stream().noneMatch(i -> "SYN-00250".equals(i.wagonCode())));
    }

    @Test
    void pages50And10HaveNoOverlap() {
        WatchedWagonPage page0 = adapter.findPage(
                new WagonRegistryCriteria(null, 0, 50, WagonRegistrySort.CODE_ASC));
        WatchedWagonPage page1 = adapter.findPage(
                new WagonRegistryCriteria(null, 1, 50, WagonRegistrySort.CODE_ASC));
        assertEquals(50, page0.content().size());
        assertEquals(10, page1.content().size());
        assertEquals(60, page0.totalElements());
        assertEquals(2, page0.totalPages());
        Set<String> seen = new HashSet<>();
        page0.content().forEach(i -> seen.add(i.wagonCode()));
        page1.content().forEach(i -> assertFalse(seen.contains(i.wagonCode())));
        assertEquals("SYN-00001", page0.content().getFirst().wagonCode());
        assertEquals("SYN-00051", page1.content().getFirst().wagonCode());
        assertEquals("SYN-00060", page1.content().getLast().wagonCode());
    }

    @Test
    void sortDescAndSearch() {
        WatchedWagonPage desc = adapter.findPage(
                new WagonRegistryCriteria(null, 0, 50, WagonRegistrySort.CODE_DESC));
        assertEquals("SYN-00060", desc.content().getFirst().wagonCode());

        WatchedWagonPage hit = adapter.findPage(
                new WagonRegistryCriteria("SYN-0000", 0, 50, WagonRegistrySort.CODE_ASC));
        assertTrue(hit.totalElements() > 0);
        assertTrue(hit.content().stream().allMatch(i -> i.wagonCode().contains("SYN-0000")));

        WatchedWagonPage miss = adapter.findPage(
                new WagonRegistryCriteria("NOMATCH", 0, 50, WagonRegistrySort.CODE_ASC));
        assertEquals(0, miss.totalElements());
        assertEquals(0, miss.totalPages());
        assertTrue(miss.content().isEmpty());
    }

    @Test
    void passportFieldsPresent() {
        WatchedWagonPage page = adapter.findPage(
                new WagonRegistryCriteria(null, 0, 1, WagonRegistrySort.CODE_ASC));
        var item = page.content().getFirst();
        assertEquals("SYN-00001", item.wagonCode());
        assertFalse(item.model().isBlank());
        assertFalse(item.wagonKind().isBlank());
        assertFalse(item.ownerName().isBlank());
    }
}
