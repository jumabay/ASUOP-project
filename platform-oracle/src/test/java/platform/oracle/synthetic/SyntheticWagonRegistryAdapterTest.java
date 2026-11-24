package platform.oracle.synthetic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import platform.fleet.WagonRegistryCriteria;
import platform.fleet.WagonRegistryPage;
import platform.fleet.WagonRegistrySort;

class SyntheticWagonRegistryAdapterTest {

    private SyntheticWagonRegistryAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new SyntheticWagonRegistryAdapter(new ObjectMapper());
    }

    @Test
    void catalogHas250UniqueCodes() {
        WagonRegistryPage page = adapter.findPage(
                new WagonRegistryCriteria(null, null, null, 0, 200, WagonRegistrySort.CODE_ASC));
        assertEquals(250, page.totalElements());
        assertEquals(2, page.totalPages());
        assertEquals(200, page.content().size());
    }

    @Test
    void searchIsCaseSensitiveContains() {
        WagonRegistryPage lower = adapter.findPage(
                new WagonRegistryCriteria("syn-", null, null, 0, 50, WagonRegistrySort.CODE_ASC));
        assertEquals(0, lower.totalElements());
        assertEquals(0, lower.totalPages());

        WagonRegistryPage upper = adapter.findPage(
                new WagonRegistryCriteria("SYN-0000", null, null, 0, 50, WagonRegistrySort.CODE_ASC));
        assertTrue(upper.totalElements() > 0);
        assertTrue(upper.content().stream().allMatch(i -> i.wagonCode().contains("SYN-0000")));
    }

    @Test
    void sortDescAndNoOverlapBetweenPages() {
        WagonRegistryPage page0 = adapter.findPage(
                new WagonRegistryCriteria(null, null, null, 0, 50, WagonRegistrySort.CODE_DESC));
        WagonRegistryPage page1 = adapter.findPage(
                new WagonRegistryCriteria(null, null, null, 1, 50, WagonRegistrySort.CODE_DESC));
        assertEquals("SYN-00250", page0.content().getFirst().wagonCode());
        Set<String> a = new HashSet<>();
        page0.content().forEach(i -> a.add(i.wagonCode()));
        page1.content().forEach(i -> assertFalse(a.contains(i.wagonCode())));
        assertEquals(50, page0.content().size());
        assertEquals(50, page1.content().size());
    }

    @Test
    void pageBeyondEndIsEmptyWithTotals() {
        WagonRegistryPage page = adapter.findPage(
                new WagonRegistryCriteria(null, null, null, 99, 50, WagonRegistrySort.CODE_ASC));
        assertTrue(page.content().isEmpty());
        assertEquals(250, page.totalElements());
        assertEquals(5, page.totalPages());
    }

    @Test
    void emptyFiltersMatchPreGFullCatalog() {
        WagonRegistryPage page = adapter.findPage(
                new WagonRegistryCriteria(null, null, null, 0, 50, WagonRegistrySort.CODE_ASC));
        assertEquals(250, page.totalElements());
        assertEquals(50, page.content().size());
        assertEquals("SYN-00001", page.content().getFirst().wagonCode());
    }

    @Test
    void ownerContainsAndKindEqualsAndTogether() {
        WagonRegistryPage andHit = adapter.findPage(
                new WagonRegistryCriteria("SYN-0000", "SYN-OWN", "КРЫТЫЙ", 0, 50, WagonRegistrySort.CODE_ASC));
        assertTrue(andHit.totalElements() > 0);
        assertTrue(andHit.content().stream().allMatch(i ->
                i.wagonCode().contains("SYN-0000")
                        && i.ownerName().contains("SYN-OWN")
                        && "КРЫТЫЙ".equals(i.wagonKind())));

        WagonRegistryPage ownerMiss = adapter.findPage(
                new WagonRegistryCriteria(null, "NO-SUCH-OWNER", null, 0, 50, WagonRegistrySort.CODE_ASC));
        assertEquals(0, ownerMiss.totalElements());
        assertTrue(ownerMiss.content().isEmpty());

        WagonRegistryPage kindCaseMiss = adapter.findPage(
                new WagonRegistryCriteria(null, null, "крытый", 0, 50, WagonRegistrySort.CODE_ASC));
        assertEquals(0, kindCaseMiss.totalElements());

        WagonRegistryPage kindEquals = adapter.findPage(
                new WagonRegistryCriteria(null, null, "КРЫТЫЙ", 0, 50, WagonRegistrySort.CODE_ASC));
        assertEquals(250, kindEquals.totalElements());
    }

    @Test
    void ownerFilterIsCaseSensitiveContains() {
        WagonRegistryPage lower = adapter.findPage(
                new WagonRegistryCriteria(null, "syn-owner", null, 0, 50, WagonRegistrySort.CODE_ASC));
        assertEquals(0, lower.totalElements());

        WagonRegistryPage partial = adapter.findPage(
                new WagonRegistryCriteria(null, "OWNER", null, 0, 50, WagonRegistrySort.CODE_ASC));
        assertEquals(250, partial.totalElements());
    }
}
