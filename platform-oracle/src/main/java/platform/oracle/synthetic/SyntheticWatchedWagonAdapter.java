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
import platform.fleet.WagonRegistrySort;
import platform.fleet.WatchedMembershipSource;
import platform.fleet.WatchedWagonItem;
import platform.fleet.WatchedWagonPage;
import platform.fleet.WatchedWagonPort;

/**
 * Synthetic watched adapter: returns classpath fixture rows labeled
 * {@link WatchedMembershipSource#FIXTURE} (demo only; not company Active / G31).
 */
@Component
@Profile("synthetic")
public class SyntheticWatchedWagonAdapter implements WatchedWagonPort {

    private final SyntheticWatchedList watched;
    private final WagonCardPort wagonCardPort;

    @Autowired
    public SyntheticWatchedWagonAdapter(
            ObjectMapper mapper, SyntheticCatalog catalog, WagonCardPort wagonCardPort) {
        this.watched = new SyntheticWatchedList(mapper, catalog);
        this.wagonCardPort = wagonCardPort;
    }

    /** Test / non-Spring helper: builds T0 classpath catalog. */
    SyntheticWatchedWagonAdapter(ObjectMapper mapper, WagonCardPort wagonCardPort) {
        this(mapper, SyntheticCatalog.classpath(mapper), wagonCardPort);
    }

    @Override
    public WatchedWagonPage findPage(WagonRegistryCriteria criteria) {
        String search = criteria.search();
        String owner = criteria.owner();
        String kind = criteria.kind();
        Comparator<String> codeOrder = criteria.sort() == WagonRegistrySort.CODE_DESC
                ? Comparator.reverseOrder()
                : Comparator.naturalOrder();

        if (owner != null || kind != null) {
            Stream<WatchedWagonItem> items = watched.wagonCodes().stream()
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
            List<WatchedWagonItem> filtered = items
                    .sorted(Comparator.comparing(WatchedWagonItem::wagonCode, codeOrder))
                    .toList();
            return pageItems(filtered, criteria);
        }

        Stream<String> stream = watched.wagonCodes().stream();
        if (search != null) {
            stream = stream.filter(code -> code.contains(search));
        }
        List<String> filtered = stream.sorted(codeOrder).toList();

        long total = filtered.size();
        int from = criteria.page() * criteria.size();
        List<WatchedWagonItem> content;
        if (from >= filtered.size()) {
            content = List.of();
        } else {
            int to = Math.min(from + criteria.size(), filtered.size());
            content = filtered.subList(from, to).stream().map(this::toItem).toList();
        }

        return WatchedWagonPage.of(
                content,
                criteria.page(),
                criteria.size(),
                total,
                WatchedWagonPage.computeTotalPages(total, criteria.size()),
                WatchedMembershipSource.FIXTURE);
    }

    private static WatchedWagonPage pageItems(
            List<WatchedWagonItem> filtered, WagonRegistryCriteria criteria) {
        long total = filtered.size();
        int from = criteria.page() * criteria.size();
        List<WatchedWagonItem> content;
        if (from >= filtered.size()) {
            content = List.of();
        } else {
            int to = Math.min(from + criteria.size(), filtered.size());
            content = filtered.subList(from, to);
        }
        return WatchedWagonPage.of(
                content,
                criteria.page(),
                criteria.size(),
                total,
                WatchedWagonPage.computeTotalPages(total, criteria.size()),
                WatchedMembershipSource.FIXTURE);
    }

    private WatchedWagonItem toItem(String wagonCode) {
        return wagonCardPort.findByCode(wagonCode)
                .map(WatchedWagonItem::fromCard)
                .orElseThrow(() -> new IllegalStateException(
                        "Watched wagon missing passport data: " + wagonCode));
    }
}
