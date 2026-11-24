package platform.oracle.synthetic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import platform.fleet.WagonCard;
import platform.fleet.WagonRegistryCriteria;
import platform.fleet.WagonRegistryPage;
import platform.fleet.WagonRegistrySort;

/**
 * T1 scale suite for synthetic registry (P4-1 A).
 *
 * <p>Run opt-in only:
 * <pre>
 *   .\scripts\gradle-jdk21.ps1 :platform-oracle:testScale
 *   .\scripts\gradle-jdk21.ps1 testScale
 * </pre>
 * Default {@code gradlew test} excludes {@code @Tag("scale")} so T0 CI stays fast.
 *
 * <p><strong>Synthetic in-memory timings ≠ Oracle load proof.</strong>
 */
@Tag("scale")
class SyntheticCatalogScaleTest {

    /** Soft wall-time budgets for in-memory page0 (record-only; not contractual p95). */
    private static final long SOFT_PAGE0_BUDGET_18K_MS = 5_000L;
    private static final long SOFT_PAGE0_BUDGET_30K_MS = 10_000L;

    @Test
    void t1_registryPagingSearchTotalsAt18334() {
        SyntheticCatalog catalog = SyntheticCatalog.generated(SyntheticWagonCodeGenerator.SIZE_T1);
        SyntheticWagonRegistryAdapter registry = new SyntheticWagonRegistryAdapter(catalog);
        SyntheticWagonCardAdapter cards = new SyntheticWagonCardAdapter(
                new com.fasterxml.jackson.databind.ObjectMapper(), catalog);

        assertEquals(SyntheticWagonCodeGenerator.SIZE_T1, catalog.size());
        assertTrue(catalog.contains("SYN-00001"));
        assertTrue(catalog.contains("SYN-18334"));

        WagonRegistryPage page0 = registry.findPage(
                new WagonRegistryCriteria(null, null, null, 0, 200, WagonRegistrySort.CODE_ASC));
        assertEquals(SyntheticWagonCodeGenerator.SIZE_T1, page0.totalElements());
        assertEquals(92, page0.totalPages()); // ceil(18334/200)=92
        assertEquals(200, page0.content().size());
        assertEquals("SYN-00001", page0.content().getFirst().wagonCode());

        int lastPageIndex = page0.totalPages() - 1;
        WagonRegistryPage last = registry.findPage(
                new WagonRegistryCriteria(null, null, null, lastPageIndex, 200, WagonRegistrySort.CODE_ASC));
        int expectedLastSize = SyntheticWagonCodeGenerator.SIZE_T1 % 200; // 134
        assertEquals(expectedLastSize, last.content().size());
        assertEquals(SyntheticWagonCodeGenerator.SIZE_T1, last.totalElements());
        assertFalse(last.content().isEmpty());

        WagonRegistryPage beyond = registry.findPage(
                new WagonRegistryCriteria(null, null, null, lastPageIndex + 1, 200, WagonRegistrySort.CODE_ASC));
        assertTrue(beyond.content().isEmpty());
        assertEquals(SyntheticWagonCodeGenerator.SIZE_T1, beyond.totalElements());

        WagonRegistryPage search = registry.findPage(
                new WagonRegistryCriteria("SYN-1833", null, null, 0, 50, WagonRegistrySort.CODE_ASC));
        assertTrue(search.totalElements() >= 1);
        assertTrue(search.content().stream().allMatch(i -> i.wagonCode().contains("SYN-1833")));

        WagonRegistryPage desc = registry.findPage(
                new WagonRegistryCriteria(null, null, null, 0, 50, WagonRegistrySort.CODE_DESC));
        assertEquals("SYN-18334", desc.content().getFirst().wagonCode());

        Set<String> page0Codes = new HashSet<>();
        page0.content().forEach(i -> page0Codes.add(i.wagonCode()));
        WagonRegistryPage page1 = registry.findPage(
                new WagonRegistryCriteria(null, null, null, 1, 200, WagonRegistrySort.CODE_ASC));
        page1.content().forEach(i -> assertFalse(page0Codes.contains(i.wagonCode())));

        WagonCard stub = cards.findByCode("SYN-18334").orElseThrow();
        assertEquals("SYN-18334", stub.wagonCode());
        assertEquals("12-132", stub.model());
        assertTrue(cards.findByCode("SYN-99999").isEmpty());
    }

    @Test
    void softTimingAndMemory_page0_at18kAnd30k() {
        recordMeasurement(SyntheticWagonCodeGenerator.SIZE_18K, SOFT_PAGE0_BUDGET_18K_MS);
        recordMeasurement(SyntheticWagonCodeGenerator.SIZE_30K, SOFT_PAGE0_BUDGET_30K_MS);
    }

    private static void recordMeasurement(int size, long softBudgetMs) {
        Runtime rt = Runtime.getRuntime();
        System.gc();
        long heapBefore = rt.totalMemory() - rt.freeMemory();

        long genStart = System.nanoTime();
        SyntheticCatalog catalog = SyntheticCatalog.generated(size);
        SyntheticWagonRegistryAdapter registry = new SyntheticWagonRegistryAdapter(catalog);
        long genMs = (System.nanoTime() - genStart) / 1_000_000L;

        long pageStart = System.nanoTime();
        WagonRegistryPage page0 = registry.findPage(
                new WagonRegistryCriteria(null, null, null, 0, 200, WagonRegistrySort.CODE_ASC));
        long pageMs = (System.nanoTime() - pageStart) / 1_000_000L;

        long heapAfter = rt.totalMemory() - rt.freeMemory();
        long heapDeltaMb = Math.max(0L, (heapAfter - heapBefore) / (1024L * 1024L));

        assertEquals(size, page0.totalElements());
        assertEquals(200, page0.content().size());

        System.out.printf(
                "SCALE_MEASURE size=%d genMs=%d page0Ms=%d heapDeltaApproxMb=%d softBudgetMs=%d%n",
                size,
                genMs,
                pageMs,
                heapDeltaMb,
                softBudgetMs);

        // Soft budget only — fail loudly if pathological; not a contractual p95.
        assertTrue(
                pageMs <= softBudgetMs,
                "page0 wall ms " + pageMs + " exceeded soft budget " + softBudgetMs
                        + " for size=" + size
                        + " (synthetic only; not Oracle proof)");
    }
}
