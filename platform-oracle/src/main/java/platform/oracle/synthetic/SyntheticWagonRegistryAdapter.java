package platform.oracle.synthetic;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import platform.fleet.WagonCardPort;
import platform.fleet.WagonRegistryCriteria;
import platform.fleet.WagonRegistryItem;
import platform.fleet.WagonRegistryPage;
import platform.fleet.WagonRegistryPort;
import platform.fleet.WagonRegistrySort;

@Component
@Profile("synthetic")
public class SyntheticWagonRegistryAdapter implements WagonRegistryPort {

    private final SyntheticCatalog catalog;
    private final WagonCardPort wagonCardPort;

    @Autowired
    public SyntheticWagonRegistryAdapter(SyntheticCatalog catalog, WagonCardPort wagonCardPort) {
        this.catalog = catalog;
        this.wagonCardPort = wagonCardPort;
    }

    /** Test / non-Spring helper: builds T0 classpath catalog. */
    SyntheticWagonRegistryAdapter(ObjectMapper mapper) {
        this(SyntheticCatalog.classpath(mapper), new SyntheticWagonCardAdapter(mapper));
    }

    /** Scale / catalog-only helper: reuses stub passport via card adapter. */
    SyntheticWagonRegistryAdapter(SyntheticCatalog catalog) {
        this(catalog, new SyntheticWagonCardAdapter(new ObjectMapper(), catalog));
    }

    @Override
    public WagonRegistryPage findPage(WagonRegistryCriteria criteria) {
        String search = criteria.search();
        String owner = criteria.owner();
        String kind = criteria.kind();
        Comparator<String> codeOrder = criteria.sort() == WagonRegistrySort.CODE_DESC
                ? Comparator.reverseOrder()
                : Comparator.naturalOrder();

        // Passport filters require mapping before page; otherwise keep pre-G code-only path.
        if (owner != null || kind != null) {
            Stream<WagonRegistryItem> items = catalog.wagonCodes().stream()
                    .filter(code -> search == null || code.contains(search))
                    .map(this::toItem);
            if (owner != null) {
                items = items.filter(item ->
                        item.ownerName() != null && item.ownerName().contains(owner));
            }
            if (kind != null) {
                items = items.filter(item ->
                        item.wagonKind() != null && item.wagonKind().equals(kind));
            }
            List<WagonRegistryItem> filtered = items
                    .sorted(Comparator.comparing(WagonRegistryItem::wagonCode, codeOrder))
                    .toList();
            return pageItems(filtered, criteria);
        }

        Stream<String> stream = catalog.wagonCodes().stream();
        if (search != null) {
            stream = stream.filter(code -> code.contains(search));
        }
        List<String> filtered = stream.sorted(codeOrder).toList();

        long total = filtered.size();
        int from = criteria.page() * criteria.size();
        List<WagonRegistryItem> content;
        if (from >= filtered.size()) {
            content = List.of();
        } else {
            int to = Math.min(from + criteria.size(), filtered.size());
            content = filtered.subList(from, to).stream().map(this::toItem).toList();
        }

        return new WagonRegistryPage(
                content,
                criteria.page(),
                criteria.size(),
                total,
                WagonRegistryPage.computeTotalPages(total, criteria.size()));
    }

    private static WagonRegistryPage pageItems(
            List<WagonRegistryItem> filtered, WagonRegistryCriteria criteria) {
        long total = filtered.size();
        int from = criteria.page() * criteria.size();
        List<WagonRegistryItem> content;
        if (from >= filtered.size()) {
            content = List.of();
        } else {
            int to = Math.min(from + criteria.size(), filtered.size());
            content = filtered.subList(from, to);
        }
        return new WagonRegistryPage(
                content,
                criteria.page(),
                criteria.size(),
                total,
                WagonRegistryPage.computeTotalPages(total, criteria.size()));
    }

    private WagonRegistryItem toItem(String wagonCode) {
        return wagonCardPort.findByCode(wagonCode)
                .map(WagonRegistryItem::fromCard)
                .orElseThrow(() -> new IllegalStateException(
                        "Registry wagon missing passport data: " + wagonCode));
    }
}
