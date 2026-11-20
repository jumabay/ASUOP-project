package platform.oracle.migration.l0.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record StockRecord(
        Long stockId,
        Long warehouseId,
        Long partTypeId,
        Long objectWagonId,
        int qty,
        String scenario
) {
}
