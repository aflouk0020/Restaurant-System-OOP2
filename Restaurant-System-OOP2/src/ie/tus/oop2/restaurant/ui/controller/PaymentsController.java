package ie.tus.oop2.restaurant.ui.controller;

import ie.tus.oop2.restaurant.model.Order;
import ie.tus.oop2.restaurant.model.Payment;
import ie.tus.oop2.restaurant.model.PaymentType;
import ie.tus.oop2.restaurant.service.OrderLineService;
import ie.tus.oop2.restaurant.service.OrderLineServiceImpl;
import ie.tus.oop2.restaurant.service.OrderService;
import ie.tus.oop2.restaurant.service.OrderServiceImpl;
import ie.tus.oop2.restaurant.service.PaymentService;
import ie.tus.oop2.restaurant.service.PaymentServiceImpl;
import ie.tus.oop2.restaurant.ui.model.OrderOption;

import javafx.beans.property.SimpleLongProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

public class PaymentsController {

    private final OrderService orderService = new OrderServiceImpl();
    private final OrderLineService orderLineService = new OrderLineServiceImpl();
    private final PaymentService paymentService = new PaymentServiceImpl();

    private final ObservableList<Payment> paymentData = FXCollections.observableArrayList();
    private final NumberFormat euroFormat = NumberFormat.getCurrencyInstance(new Locale("en", "IE"));

    @FXML
    private ComboBox<OrderOption> orderSelector;

    @FXML
    private ComboBox<PaymentType> paymentTypeSelector;

    @FXML
    private TextField amountField;

    @FXML
    private TextField currencyField;

    @FXML
    private TextField cardLast4Field;

    @FXML
    private TextField voucherCodeField;

    @FXML
    private HBox cardBox;

    @FXML
    private HBox voucherBox;

    @FXML
    private Label orderTotalLabel;

    @FXML
    private Label paymentStateLabel;

    @FXML
    private Label selectedTypeLabel;

    @FXML
    private Label statusLabel;

    @FXML
    private Button payButton;

    @FXML
    private TableView<Payment> paymentsTable;

    @FXML
    private TableColumn<Payment, Number> paymentIdColumn;

    @FXML
    private TableColumn<Payment, Number> orderIdColumn;

    @FXML
    private TableColumn<Payment, String> paidAtColumn;

    @FXML
    private TableColumn<Payment, String> paymentTypeColumn;

    @FXML
    private TableColumn<Payment, String> amountColumn;

    @FXML
    private TableColumn<Payment, String> currencyColumn;

    @FXML
    public void initialize() {
        setupPaymentsTable();

        paymentsTable.setItems(paymentData);

        paymentTypeSelector.setItems(FXCollections.observableArrayList(PaymentType.values()));
        paymentTypeSelector.getSelectionModel().select(PaymentType.CASH);

        currencyField.setText("EUR");

        loadOrders();
        refreshPayments();
        updatePaymentTypeFields();
        updateSelectedOrderDetails();

        paymentTypeSelector.setOnAction(e -> {
            updatePaymentTypeFields();
            updateSelectedTypeLabel();
        });

        orderSelector.setOnAction(e -> updateSelectedOrderDetails());
    }

    private void setupPaymentsTable() {
        paymentIdColumn.setCellValueFactory(cell ->
                new SimpleLongProperty(cell.getValue().paymentId()));

        orderIdColumn.setCellValueFactory(cell ->
                new SimpleLongProperty(cell.getValue().orderId()));

        paidAtColumn.setCellValueFactory(cell ->
                new SimpleStringProperty(String.valueOf(cell.getValue().paidAt())));

        paymentTypeColumn.setCellValueFactory(cell ->
                new SimpleStringProperty(cell.getValue().paymentType().name()));

        paymentTypeColumn.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);

                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                    return;
                }

                setText(item);

                switch (item) {
                    case "CASH" -> setStyle("-fx-text-fill: #16a34a; -fx-font-weight: bold;");
                    case "CARD" -> setStyle("-fx-text-fill: #2563eb; -fx-font-weight: bold;");
                    case "VOUCHER" -> setStyle("-fx-text-fill: #7c3aed; -fx-font-weight: bold;");
                    default -> setStyle("-fx-text-fill: #374151; -fx-font-weight: bold;");
                }
            }
        });

        amountColumn.setCellValueFactory(cell ->
                new SimpleStringProperty(euroFormat.format(cell.getValue().amount())));

        currencyColumn.setCellValueFactory(cell ->
                new SimpleStringProperty(cell.getValue().currency()));
    }

    @FXML
    private void refreshPayments() {
        List<Payment> payments = paymentService.listPayments().stream()
                .sorted((a, b) -> b.paidAt().compareTo(a.paidAt()))
                .toList();

        paymentData.setAll(payments);
        paymentsTable.refresh();

        loadOrders();
        updateSelectedOrderDetails();
        statusLabel.setText("Payments refreshed");
    }

    @FXML
    private void paySelectedOrder() {
        try {
            OrderOption selectedOrder = orderSelector.getSelectionModel().getSelectedItem();
            if (selectedOrder == null) {
                statusLabel.setText("Select an order first");
                return;
            }

            PaymentType type = paymentTypeSelector.getSelectionModel().getSelectedItem();
            if (type == null) {
                statusLabel.setText("Select a payment type");
                return;
            }

            BigDecimal amount = parseAmount();
            String currency = currencyField.getText();
            String cardLast4 = cardLast4Field.getText();
            String voucherCode = voucherCodeField.getText();

            Payment payment = paymentService.pay(
                    selectedOrder.orderId(),
                    type,
                    amount,
                    currency,
                    cardLast4,
                    voucherCode
            );

            refreshPayments();
            statusLabel.setText("Payment created successfully (Payment #" + payment.paymentId() + ")");
            clearOptionalFields();
            updateSelectedOrderDetails();

        } catch (NumberFormatException e) {
            statusLabel.setText("Amount must be a valid number");
        } catch (Exception e) {
            statusLabel.setText("Error: " + e.getMessage());
        }
    }

    private void loadOrders() {
    	List<OrderOption> orders = orderService.listOrders().stream()
    			.filter(o -> o.status() == ie.tus.oop2.restaurant.model.OrderStatus.SUBMITTED)
    	        .sorted((a, b) -> b.createdAt().compareTo(a.createdAt()))
    	        .map(order -> new OrderOption(order.orderId(), order.sessionId(), order.status()))
    	        .toList();

        OrderOption current = orderSelector.getSelectionModel().getSelectedItem();

        orderSelector.setItems(FXCollections.observableArrayList(orders));

        if (current != null) {
            orderSelector.getSelectionModel().select(
                    orderSelector.getItems().stream()
                            .filter(o -> o.orderId() == current.orderId())
                            .findFirst()
                            .orElse(null)
            );
        }

        if (orderSelector.getSelectionModel().getSelectedItem() == null && !orderSelector.getItems().isEmpty()) {
            orderSelector.getSelectionModel().selectFirst();
        }
    }

    private void updatePaymentTypeFields() {
        PaymentType type = paymentTypeSelector.getSelectionModel().getSelectedItem();

        boolean card = type == PaymentType.CARD;
        boolean voucher = type == PaymentType.VOUCHER;

        cardBox.setManaged(card);
        cardBox.setVisible(card);

        voucherBox.setManaged(voucher);
        voucherBox.setVisible(voucher);
    }

    private void updateSelectedTypeLabel() {
        PaymentType type = paymentTypeSelector.getSelectionModel().getSelectedItem();
        selectedTypeLabel.setText(type == null ? "-" : type.name());
    }

    private void updateSelectedOrderDetails() {
        OrderOption selected = orderSelector.getSelectionModel().getSelectedItem();

        if (selected == null) {
            orderTotalLabel.setText("€0.00");
            paymentStateLabel.setText("NO ORDER");
            selectedTypeLabel.setText("-");
            payButton.setDisable(true);
            amountField.clear();
            return;
        }

        BigDecimal total = orderLineService.calculateOrderTotal(selected.orderId());
        orderTotalLabel.setText(euroFormat.format(total));
        amountField.setText(total.toPlainString());

        Payment existingPayment = paymentService.findByOrderId(selected.orderId());

        if (existingPayment != null) {
            paymentStateLabel.setText("PAID");
            payButton.setDisable(true);
        } else {
            paymentStateLabel.setText("UNPAID");
            payButton.setDisable(false);
        }

        updateSelectedTypeLabel();
    }

    private BigDecimal parseAmount() {
        String text = amountField.getText();
        if (text == null || text.isBlank()) {
            throw new NumberFormatException("Amount is empty");
        }
        return new BigDecimal(text.trim());
    }

    private void clearOptionalFields() {
        cardLast4Field.clear();
        voucherCodeField.clear();
    }
}