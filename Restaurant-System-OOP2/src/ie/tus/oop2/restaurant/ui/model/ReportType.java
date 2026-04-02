package ie.tus.oop2.restaurant.ui.model;

public enum ReportType {
    DAILY_SALES_TOTALS("Daily Sales Totals"),
    TOP_SELLING_ITEMS("Top Selling Items"),
    VEG_PARTITION("Vegetarian vs Non-Vegetarian Sales"),
    REVENUE_BY_PAYMENT("Revenue by Payment Type"),
    REVENUE_BY_HOUR("Revenue by Hour"),
    PEAK_SALES_HOUR("Peak Sales Hour"),
    TOP_REVENUE_DAYS("Top Revenue Days"),
    REVENUE_BY_STAFF("Revenue by Staff"),
    AVERAGE_TABLE_SPEND("Average Table Spend"),
    OVERALL_REVENUE_STATS("Overall Revenue Stats");

    private final String label;

    ReportType(String label) {
        this.label = label;
    }

    @Override
    public String toString() {
        return label;
    }
}	