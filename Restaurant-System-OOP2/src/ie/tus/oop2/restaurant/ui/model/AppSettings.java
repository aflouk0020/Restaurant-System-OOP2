package ie.tus.oop2.restaurant.ui.model;

import java.math.BigDecimal;

public record AppSettings(
        String restaurantName,
        String defaultCurrency,
        BigDecimal taxRate,
        String language,
        String receiptExportFolder
) {}