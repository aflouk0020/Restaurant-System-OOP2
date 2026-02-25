package ie.tus.oop2.restaurant.reporting;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

public interface ManagerReportsService {

    /**
     * Daily total sales from receipts.
     * Uses Streams + groupingBy(date).
     *
     * @return LinkedHashMap sorted by date ascending (or consistent ordering).
     */
    LinkedHashMap<LocalDate, BigDecimal> dailySalesTotals();

    /**
     * Top selling items by quantity (from paid orders only).
     * Uses Streams + toMap + sorted.
     *
     * @param limit max items to return
     * @return LinkedHashMap itemName -> qtySold sorted desc by qty
     */
    LinkedHashMap<String, Long> topSellingItems(int limit);

    /**
     * Partition sales into vegetarian vs non-vegetarian.
     * Uses Streams + partitioningBy.
     *
     * @return Map(true/false) -> total sales amount
     */
    Map<Boolean, BigDecimal> partitionSalesByVegetarian();
}