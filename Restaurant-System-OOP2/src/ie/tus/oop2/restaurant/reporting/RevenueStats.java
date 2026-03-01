package ie.tus.oop2.restaurant.reporting;

import java.math.BigDecimal;

/**
 * Immutable aggregated revenue metrics for manager reporting.
 */
public record RevenueStats(
        BigDecimal totalRevenue,
        BigDecimal averageSpend,
        int totalReceipts
) { }