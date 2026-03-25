package ie.tus.oop2.restaurant.reporting;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

import ie.tus.oop2.restaurant.model.PaymentType;

public interface ManagerReportsService {

    LinkedHashMap<LocalDate, BigDecimal> dailySalesTotals();

    LinkedHashMap<String, Long> topSellingItems(int limit);

    Map<Boolean, BigDecimal> partitionSalesByVegetarian();

    LinkedHashMap<PaymentType, BigDecimal> revenueByPaymentType();

    LinkedHashMap<Integer, BigDecimal> revenueByHour();   // hour -> total revenue

    int peakSalesHour();

    LinkedHashMap<LocalDate, BigDecimal> topRevenueDays(int limit);
    BigDecimal averageTableSpend();
    // ✅ NEW (Revenue by staff member who created the order)
    LinkedHashMap<String, BigDecimal> revenueByStaff();
    RevenueStats overallRevenueStats();
}