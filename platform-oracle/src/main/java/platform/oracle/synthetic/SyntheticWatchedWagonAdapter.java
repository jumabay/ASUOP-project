package platform.oracle.synthetic;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;
import org.springframework.stereotype.Component;
import platform.fleet.WagonCardPort;
import platform.fleet.WagonRegistryCriteria;
import platform.fleet.WagonRegistrySort;
import platform.fleet.WatchedWagonItem;
import platform.fleet.WatchedWagonPage;
import platform.fleet.WatchedWagonPort;

@Component
public class SyntheticWatchedWagonAdapter implements WatchedWagonPort {

    private final SyntheticWatchedList watched;
    private final WagonCardPort wagonCardPort;

    public SyntheticWatchedWagonAdapter(ObjectMapper mapper, WagonCardPort wagonCardPort) {
        SyntheticCatalog catalog = new SyntheticCatalog(mapper);
        this.watched = new SyntheticWatchedList(mapper, catalog);
        this.wagonCardPort = wagonCardPort;
    }

    @Override
    public WatchedWagonPage findPage(WagonRegistryCriteria criteria) {
        Stream<String> stream = watched.wagonCodes().stream();
        String search = criteria.search();
        if (search != null) {
            stream = stream.filter(code -> code.contains(search));
        }

        Comparator<String> order = criteria.sort() == WagonRegistrySort.CODE_DESC
                ? Comparator.reverseOrder()
                : Comparator.naturalOrder();
        List<String> filtered = stream.sorted(order).toList();

        long total = filtered.size();
        int from = criteria.page() * criteria.size();
        List<WatchedWagonItem> content;
        if (from >= filtered.size()) {
            content = List.of();
        } else {
            int to = Math.min(from + criteria.size(), filtered.size());
            content = filtered.subList(from, to).stream()
                    .map(this::toItem)
                    .toList();
        }

        return new WatchedWagonPage(
                content,
                criteria.page(),
                criteria.size(),
                total,
                WatchedWagonPage.computeTotalPages(total, criteria.size()));
    }

    private WatchedWagonItem toItem(String wagonCode) {
        return wagonCardPort.findByCode(wagonCode)
                .map(WatchedWagonItem::fromCard)
                .orElseThrow(() -> new IllegalStateException(
                        "Watched wagon missing passport data: " + wagonCode));
    }
}
