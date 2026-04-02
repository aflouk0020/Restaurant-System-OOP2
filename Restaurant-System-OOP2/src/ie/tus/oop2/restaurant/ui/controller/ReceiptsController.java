package ie.tus.oop2.restaurant.ui.controller;

import ie.tus.oop2.restaurant.model.Order;
import ie.tus.oop2.restaurant.model.OrderLine;
import ie.tus.oop2.restaurant.model.OrderLineStatus;
import ie.tus.oop2.restaurant.model.Receipt;
import ie.tus.oop2.restaurant.service.OrderLineService;
import ie.tus.oop2.restaurant.service.OrderLineServiceImpl;
import ie.tus.oop2.restaurant.service.OrderService;
import ie.tus.oop2.restaurant.service.OrderServiceImpl;
import ie.tus.oop2.restaurant.service.ReceiptService;
import ie.tus.oop2.restaurant.service.ReceiptServiceImpl;
import ie.tus.oop2.restaurant.ui.model.OrderOption;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

public class ReceiptsController {

    private static final Path EXPORT_DIR = Path.of("exports", "receipts");

    private final OrderService orderService = new OrderServiceImpl();
    private final OrderLineService orderLineService = new OrderLineServiceImpl();
    private final ReceiptService receiptService = new ReceiptServiceImpl();

    private final NumberFormat euroFormat = NumberFormat.getCurrencyInstance(new Locale("en", "IE"));

    @FXML
    private ComboBox<OrderOption> orderSelector;

    @FXML
    private Label statusLabel;

    @FXML
    private Label receiptIdLabel;

    @FXML
    private Label orderIdLabel;

    @FXML
    private Label totalLabel;

    @FXML
    private Label generatedAtLabel;

    @FXML
    private TextArea receiptPreviewArea;

    @FXML
    public void initialize() {
        loadOrders();
        clearReceiptSummary();
    }

    @FXML
    private void refreshOrders() {
        loadOrders();
        statusLabel.setText("Orders refreshed");
    }

    @FXML
    private void generateReceipt() {
        try {
            OrderOption selected = orderSelector.getSelectionModel().getSelectedItem();
            if (selected == null) {
                statusLabel.setText("Select an order first");
                return;
            }

            Receipt receipt = receiptService.generateReceiptForOrder(selected.orderId(), null);
            showReceipt(receipt);
            statusLabel.setText("Receipt generated successfully");

        } catch (Exception e) {
            statusLabel.setText("Error: " + e.getMessage());
        }
    }

    @FXML
    private void generateAndExportReceipt() {
        try {
            OrderOption selected = orderSelector.getSelectionModel().getSelectedItem();
            if (selected == null) {
                statusLabel.setText("Select an order first");
                return;
            }

            Receipt receipt = receiptService.generateReceiptForOrder(selected.orderId(), EXPORT_DIR);
            showReceipt(receipt);
            statusLabel.setText("Receipt generated and exported successfully");

        } catch (Exception e) {
            statusLabel.setText("Error: " + e.getMessage());
        }
    }

    private void loadOrders() {
    	List<OrderOption> orders = orderService.listOrders().stream()
    			.filter(o -> o.status() == ie.tus.oop2.restaurant.model.OrderStatus.PAID)
    	        .sorted((a, b) -> b.createdAt().compareTo(a.createdAt()))
    	        .map(order -> new OrderOption(order.orderId(), order.sessionId(), order.status()))
    	        .toList();

        orderSelector.setItems(FXCollections.observableArrayList(orders));

        if (!orderSelector.getItems().isEmpty()) {
            orderSelector.getSelectionModel().selectFirst();
        }
    }

    private void showReceipt(Receipt receipt) {
        receiptIdLabel.setText(String.valueOf(receipt.receiptId()));
        orderIdLabel.setText(String.valueOf(receipt.orderId()));
        totalLabel.setText(euroFormat.format(receipt.total()));
        generatedAtLabel.setText(String.valueOf(receipt.generatedAt()));

        receiptPreviewArea.setText(buildPreview(receipt));
    }

    private String buildPreview(Receipt receipt) {
        List<OrderLine> lines = orderLineService.listLinesForOrder(receipt.orderId()).stream()
                .filter(line -> line.lineStatus() != OrderLineStatus.CANCELLED)
                .toList();

        StringBuilder sb = new StringBuilder();
        sb.append("====================================\n");
        sb.append("        RESTAURANT RECEIPT\n");
        sb.append("====================================\n");
        sb.append("Receipt ID: ").append(receipt.receiptId()).append("\n");
        sb.append("Order ID:   ").append(receipt.orderId()).append("\n");
        sb.append("Generated:  ").append(receipt.generatedAt()).append("\n");
        sb.append("====================================\n");
        sb.append("Items\n");
        sb.append("====================================\n");

        for (OrderLine line : lines) {
            BigDecimal lineTotal = line.unitPriceSnapshot()
                    .multiply(BigDecimal.valueOf(line.quantity()));

            sb.append(line.itemNameSnapshot())
              .append(" x")
              .append(line.quantity())
              .append(" @ ")
              .append(euroFormat.format(line.unitPriceSnapshot()))
              .append(" = ")
              .append(euroFormat.format(lineTotal))
              .append("\n");
        }

        sb.append("====================================\n");
        sb.append("Subtotal: ").append(euroFormat.format(receipt.subtotal())).append("\n");
        sb.append("Tax:      ").append(euroFormat.format(receipt.tax())).append("\n");
        sb.append("Total:    ").append(euroFormat.format(receipt.total())).append("\n");
        sb.append("====================================\n");

        return sb.toString();
    }

    private void clearReceiptSummary() {
        receiptIdLabel.setText("-");
        orderIdLabel.setText("-");
        totalLabel.setText("€0.00");
        generatedAtLabel.setText("-");
        receiptPreviewArea.clear();
    }
}