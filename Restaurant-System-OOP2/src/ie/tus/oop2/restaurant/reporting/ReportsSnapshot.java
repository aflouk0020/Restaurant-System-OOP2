package ie.tus.oop2.restaurant.reporting;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Immutable snapshot of all manager reports.
 * Perfect for demo + concurrency explanation.
 */
public record ReportsSnapshot(
        LinkedHashMap<LocalDate, BigDecimal> dailySales,
        LinkedHashMap<String, Long> topSelling,
        Map<Boolean, BigDecimal> vegetarianPartition
) {}