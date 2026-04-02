package ie.tus.oop2.restaurant.ui.controller;

import ie.tus.oop2.restaurant.reporting.ManagerReportsService;
import ie.tus.oop2.restaurant.reporting.ManagerReportsServiceImpl;
import ie.tus.oop2.restaurant.reporting.RevenueStats;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

public class DashboardController {

    private final ManagerReportsService reportsService = new ManagerReportsServiceImpl();
    private final NumberFormat euroFormat = NumberFormat.getCurrencyInstance(new Locale("en", "IE"));

    @FXML
    private Label totalRevenueLabel;

    @FXML
    private Label totalReceiptsLabel; // keep fx:id, but change visible text in FXML to "Total Payments"

    @FXML
    private Label averageSpendLabel;

    @FXML
    private Label peakHourLabel;

    @FXML
    private Label topItem1Label;

    @FXML
    private Label topItem2Label;

    @FXML
    private Label topItem3Label;

    @FXML
    private LineChart<String, Number> dailyRevenueChart;

    @FXML
    private BarChart<String, Number> topItemsChart;

    @FXML
    public void initialize() {
        loadDashboard();
    }

    private void loadDashboard() {
        try {
            loadSummaryCards();
            loadTopItemsPreview();
            loadDailyRevenueChart();
            loadTopItemsChart();
        } catch (Exception e) {
            e.printStackTrace();
            showFallbackState();
        }
    }

    private void loadSummaryCards() {
        RevenueStats stats = reportsService.overallRevenueStats();

        totalRevenueLabel.setText(euroFormat.format(stats.totalRevenue()));
        totalReceiptsLabel.setText(String.valueOf(stats.totalReceipts()));
        averageSpendLabel.setText(euroFormat.format(stats.averageSpend()));

        int peakHour = reportsService.peakSalesHour();
        peakHourLabel.setText(peakHour < 0 ? "--:--" : String.format("%02d:00", peakHour));
    }

    private void loadTopItemsPreview() {
        LinkedHashMap<String, Long> topItems = reportsService.topSellingItems(3);

        Label[] labels = {topItem1Label, topItem2Label, topItem3Label};
        String[] defaults = {"1. -", "2. -", "3. -"};

        int i = 0;
        for (Map.Entry<String, Long> entry : topItems.entrySet()) {
            if (i >= 3) break;
            labels[i].setText((i + 1) + ". " + entry.getKey() + " (" + entry.getValue() + ")");
            i++;
        }

        while (i < 3) {
            labels[i].setText(defaults[i]);
            i++;
        }
    }

    private void loadDailyRevenueChart() {
        dailyRevenueChart.getData().clear();

        LinkedHashMap<LocalDate, BigDecimal> revenueMap = reportsService.dailySalesTotals();

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Revenue");

        revenueMap.forEach((date, value) ->
                series.getData().add(new XYChart.Data<>(date.toString(), value))
        );

        dailyRevenueChart.getData().add(series);

        Platform.runLater(() -> series.getData().forEach(data ->
                Tooltip.install(
                        data.getNode(),
                        new Tooltip(
                                euroFormat.format(new BigDecimal(data.getYValue().toString()))
                                        + "\n" + data.getXValue()
                        )
                )
        ));
    }

    private void loadTopItemsChart() {
        topItemsChart.getData().clear();

        LinkedHashMap<String, Long> topItems = reportsService.topSellingItems(5);

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Items");

        topItems.forEach((item, qty) ->
                series.getData().add(new XYChart.Data<>(item, qty))
        );

        topItemsChart.getData().add(series);

        Platform.runLater(() -> series.getData().forEach(data ->
                Tooltip.install(
                        data.getNode(),
                        new Tooltip(data.getXValue() + ": " + data.getYValue())
                )
        ));
    }

    private void showFallbackState() {
        totalRevenueLabel.setText("€0.00");
        totalReceiptsLabel.setText("0");
        averageSpendLabel.setText("€0.00");
        peakHourLabel.setText("--:--");

        topItem1Label.setText("1. -");
        topItem2Label.setText("2. -");
        topItem3Label.setText("3. -");

        dailyRevenueChart.getData().clear();
        topItemsChart.getData().clear();
    }
}