package platform.oracle.synthetic;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import platform.fleet.WagonRegistryCriteria;
import platform.fleet.WagonRegistryItem;
import platform.fleet.WagonRegistryPage;
import platform.fleet.WagonRegistryPort;
import platform.fleet.WagonRegistrySort;

@Component
@Profile("synthetic")
public class SyntheticWagonRegistryAdapter implements WagonRegistryPort {

    private final SyntheticCatalog catalog;

    public SyntheticWagonRegistryAdapter(ObjectMapper mapper) {
        this.catalog = new SyntheticCatalog(mapper);
    }

    @Override
    public WagonRegistryPage findPage(WagonRegistryCriteria criteria) {
        Stream<String> stream = catalog.wagonCodes().stream();
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
        List<WagonRegistryItem> content;
        if (from >= filtered.size()) {
            content = List.of();
        } else {
            int to = Math.min(from + criteria.size(), filtered.size());
            content = filtered.subList(from, to).stream().map(WagonRegistryItem::new).toList();
        }

        return new WagonRegistryPage(
                content,
                criteria.page(),
                criteria.size(),
                total,
                WagonRegistryPage.computeTotalPages(total, criteria.size()));
    }
}
