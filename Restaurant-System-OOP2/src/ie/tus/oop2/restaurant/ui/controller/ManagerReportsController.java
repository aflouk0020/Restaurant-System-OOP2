package ie.tus.oop2.restaurant.ui.controller;

import ie.tus.oop2.restaurant.reporting.ManagerReportsService;
import ie.tus.oop2.restaurant.reporting.ManagerReportsServiceImpl;
import ie.tus.oop2.restaurant.reporting.RevenueStats;
import ie.tus.oop2.restaurant.ui.model.ReportRow;
import ie.tus.oop2.restaurant.ui.model.ReportType;


import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;
import java.text.NumberFormat;
import java.util.Locale;

public class ManagerReportsController {

    private final ManagerReportsService reportsService = new ManagerReportsServiceImpl();
    private final ObservableList<ReportRow> tableData = FXCollections.observableArrayList();

    @FXML
    private ComboBox<ReportType> reportSelector;

    @FXML
    private TextField topNField;

    @FXML
    private TableView<ReportRow> reportTable;

    @FXML
    private TableColumn<ReportRow, String> keyColumn;

    @FXML
    private TableColumn<ReportRow, String> valueColumn;

    @FXML
    private Label totalRevenueLabel;

    @FXML
    private Label ordersCountLabel;

    @FXML
    private Label avgOrderValueLabel;
    
    private final NumberFormat euroFormat = NumberFormat.getCurrencyInstance(new Locale("en", "IE"));

    @FXML
    public void initialize() {
        reportSelector.setItems(FXCollections.observableArrayList(ReportType.values()));
        reportSelector.getSelectionModel().select(ReportType.OVERALL_REVENUE_STATS);

        keyColumn.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().key()));
        valueColumn.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().value()));
        reportTable.setItems(tableData);

        topNField.setDisable(true);

        reportSelector.setOnAction(e -> {
            ReportType selected = reportSelector.getValue();
            boolean needsTopN = requiresTopN(selected);
            topNField.setDisable(!needsTopN);
            if (!needsTopN) {
                topNField.clear();
            }
            runReport();
        });

        topNField.setOnAction(e -> {
            if (requiresTopN(reportSelector.getValue())) {
                runReport();
            }
        });

        refreshSummaryCards();
        runReport();
    }

    @FXML
    private void runReport() {
        tableData.clear();

        ReportType selected = reportSelector.getValue();
        int topN = parseTopN();

        if (selected == null) {
            return;
        }

        switch (selected) {
	        case DAILY_SALES_TOTALS -> {
	            LinkedHashMap<LocalDate, java.math.BigDecimal> map = reportsService.dailySalesTotals();
	            map.forEach((k, v) -> tableData.add(new ReportRow(k.toString(), formatMoney(v))));
	        }

            case TOP_SELLING_ITEMS -> {
                LinkedHashMap<String, Long> map = reportsService.topSellingItems(topN);
                map.forEach((k, v) -> tableData.add(new ReportRow(k, String.valueOf(v))));
            }

            case VEG_PARTITION -> {
                Map<Boolean, java.math.BigDecimal> map = reportsService.partitionSalesByVegetarian();

                java.math.BigDecimal veg = map.getOrDefault(true, java.math.BigDecimal.ZERO);
                java.math.BigDecimal nonVeg = map.getOrDefault(false, java.math.BigDecimal.ZERO);

                tableData.add(new ReportRow("Vegetarian", formatMoney(veg)));
                tableData.add(new ReportRow("Non-Vegetarian", formatMoney(nonVeg)));
            }

            case REVENUE_BY_PAYMENT -> {
                LinkedHashMap<?, java.math.BigDecimal> map = reportsService.revenueByPaymentType();
                map.forEach((k, v) -> tableData.add(new ReportRow(String.valueOf(k), formatMoney(v))));
            }

            case REVENUE_BY_HOUR -> {
                LinkedHashMap<Integer, java.math.BigDecimal> map = reportsService.revenueByHour();
                map.forEach((k, v) -> tableData.add(new ReportRow(k + ":00", formatMoney(v))));
            }

            case TOP_REVENUE_DAYS -> {
                LinkedHashMap<LocalDate, java.math.BigDecimal> map = reportsService.topRevenueDays(topN);
                map.forEach((k, v) -> tableData.add(new ReportRow(k.toString(), formatMoney(v))));
            }

            case REVENUE_BY_STAFF -> {
                LinkedHashMap<String, java.math.BigDecimal> map = reportsService.revenueByStaff();
                map.forEach((k, v) -> tableData.add(new ReportRow(k, formatMoney(v))));
            }

            case AVERAGE_TABLE_SPEND -> {
                tableData.add(new ReportRow("Average Table Spend", formatMoney(reportsService.averageTableSpend())));
            }

            case OVERALL_REVENUE_STATS -> {
                RevenueStats stats = reportsService.overallRevenueStats();
                tableData.add(new ReportRow("Total Revenue", formatMoney(stats.totalRevenue())));
                tableData.add(new ReportRow("Average Spend", formatMoney(stats.averageSpend())));
                tableData.add(new ReportRow("Total Payments", String.valueOf(stats.totalReceipts())));
            }
        }

        refreshSummaryCards();
    }

    @FXML
    private void exportReport() {
        try {
            Path exportDir = Path.of("exports", "reports");
            Files.createDirectories(exportDir);

            ReportType selected = reportSelector.getValue();
            String reportName = (selected == null ? "report" : selected.name().toLowerCase());
            Path file = exportDir.resolve(reportName + ".csv");

            StringBuilder sb = new StringBuilder();
            sb.append("Key,Value\n");

            for (ReportRow row : tableData) {
                sb.append(escapeCsv(row.key()))
                  .append(",")
                  .append(escapeCsv(row.value()))
                  .append("\n");
            }

            Files.writeString(file, sb.toString());

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void refreshSummaryCards() {
        RevenueStats stats = reportsService.overallRevenueStats();
        totalRevenueLabel.setText(euroFormat.format(stats.totalRevenue()));
        ordersCountLabel.setText(String.valueOf(stats.totalReceipts()));
        avgOrderValueLabel.setText(euroFormat.format(stats.averageSpend()));
    }
    
    private boolean requiresTopN(ReportType type) {
        return type == ReportType.TOP_SELLING_ITEMS
                || type == ReportType.TOP_REVENUE_DAYS;
    }

    private int parseTopN() {
        try {
            String text = topNField.getText();
            if (text == null || text.isBlank()) return 5;
            int value = Integer.parseInt(text.trim());
            return value > 0 ? value : 5;
        } catch (NumberFormatException e) {
            return 5;
        }
    }
    
    private String formatMoney(java.math.BigDecimal value) {
        return euroFormat.format(value);
    }

    private String escapeCsv(String input) {
        if (input == null) return "";
        String escaped = input.replace("\"", "\"\"");
        return "\"" + escaped + "\"";
    }
}