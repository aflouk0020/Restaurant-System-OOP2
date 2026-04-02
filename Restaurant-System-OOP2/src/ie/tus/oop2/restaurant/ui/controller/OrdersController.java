package ie.tus.oop2.restaurant.ui.controller;

import java.text.NumberFormat;
import java.util.Locale;
import ie.tus.oop2.restaurant.model.MenuItem;
import ie.tus.oop2.restaurant.model.Order;
import ie.tus.oop2.restaurant.model.OrderLine;
import ie.tus.oop2.restaurant.model.OrderLineStatus;
import ie.tus.oop2.restaurant.service.OrderLineService;
import ie.tus.oop2.restaurant.service.OrderLineServiceImpl;
import ie.tus.oop2.restaurant.service.OrderService;
import ie.tus.oop2.restaurant.service.OrderServiceImpl;
import ie.tus.oop2.restaurant.service.TableSessionService;
import ie.tus.oop2.restaurant.service.TableSessionServiceImpl;
import ie.tus.oop2.restaurant.ui.model.MenuItemOption;
import ie.tus.oop2.restaurant.ui.model.SessionOption;

import javafx.beans.property.SimpleLongProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;

public class OrdersController {

    private final OrderService orderService = new OrderServiceImpl();
    private final TableSessionService tableSessionService = new TableSessionServiceImpl();
    private final OrderLineService orderLineService = new OrderLineServiceImpl();

    private final ObservableList<Order> orderData = FXCollections.observableArrayList();
    private final ObservableList<OrderLine> lineData = FXCollections.observableArrayList();
    private final NumberFormat euroFormat = NumberFormat.getCurrencyInstance(new Locale("en", "IE"));
    
    @FXML
    private Label selectedOrderTotalLabel;
    
    @FXML
    private TableColumn<Order, String> totalColumn;
    
    @FXML
    private ComboBox<SessionOption> sessionSelector;

    @FXML
    private TextField staffIdField;

    @FXML
    private TextField notesField;

    @FXML
    private Label statusLabel;

    @FXML
    private TableView<Order> ordersTable;

    @FXML
    private TableColumn<Order, Number> orderIdColumn;

    @FXML
    private TableColumn<Order, Number> sessionIdColumn;

    @FXML
    private TableColumn<Order, String> createdAtColumn;

    @FXML
    private TableColumn<Order, String> statusColumn;

    @FXML
    private TableColumn<Order, String> staffIdColumn;

    @FXML
    private TableColumn<Order, String> notesColumn;

    @FXML
    private ComboBox<MenuItemOption> menuItemSelector;

    @FXML
    private TextField quantityField;

    @FXML
    private ComboBox<OrderLineStatus> lineStatusSelector;

    @FXML
    private TableView<OrderLine> orderLinesTable;

    @FXML
    private TableColumn<OrderLine, Number> lineIdColumn;

    @FXML
    private TableColumn<OrderLine, Number> lineOrderIdColumn;

    @FXML
    private TableColumn<OrderLine, String> menuItemColumn;

    @FXML
    private TableColumn<OrderLine, String> priceColumn;

    @FXML
    private TableColumn<OrderLine, Number> quantityColumn;

    @FXML
    private TableColumn<OrderLine, String> lineStatusColumn;

    @FXML
    public void initialize() {
        setupOrdersTable();
        setupOrderLinesTable();

        ordersTable.setItems(orderData);
        orderLinesTable.setItems(lineData);

        lineStatusSelector.setItems(FXCollections.observableArrayList(OrderLineStatus.values()));

        loadOpenSessions();
        loadMenuItems();
        refreshOrders();

        ordersTable.getSelectionModel().selectedItemProperty().addListener((obs, oldOrder, newOrder) -> {
            loadLinesForSelectedOrder(newOrder);
            refreshSelectedOrderTotal(newOrder);
        });
    }

    private void setupOrdersTable() {
        orderIdColumn.setCellValueFactory(cell ->
                new SimpleLongProperty(cell.getValue().orderId()));

        sessionIdColumn.setCellValueFactory(cell ->
                new SimpleLongProperty(cell.getValue().sessionId()));

        createdAtColumn.setCellValueFactory(cell ->
                new SimpleStringProperty(String.valueOf(cell.getValue().createdAt())));

        statusColumn.setCellValueFactory(cell ->
                new SimpleStringProperty(String.valueOf(cell.getValue().status())));

        statusColumn.setCellFactory(column -> new TableCell<>() {
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
                    case "CREATED" -> setStyle("-fx-text-fill: #2563eb; -fx-font-weight: bold;");
                    case "SUBMITTED" -> setStyle("-fx-text-fill: #16a34a; -fx-font-weight: bold;");
                    case "CANCELLED" -> setStyle("-fx-text-fill: #dc2626; -fx-font-weight: bold;");
                    case "PAID" -> setStyle("-fx-text-fill: #7c3aed; -fx-font-weight: bold;");
                    default -> setStyle("-fx-text-fill: #374151; -fx-font-weight: bold;");
                }
            }
        });

        staffIdColumn.setCellValueFactory(cell -> {
            Long value = cell.getValue().createdByStaffId();
            return new SimpleStringProperty(value == null ? "-" : String.valueOf(value));
        });

        notesColumn.setCellValueFactory(cell -> {
            String notes = cell.getValue().notes();
            return new SimpleStringProperty(notes == null ? "" : notes);
        });
        totalColumn.setCellValueFactory(cell -> {
            java.math.BigDecimal total = orderLineService.calculateOrderTotal(cell.getValue().orderId());
            return new SimpleStringProperty(euroFormat.format(total));
        });
    }
    
    private void refreshSelectedOrderTotal(Order order) {
        if (order == null) {
            selectedOrderTotalLabel.setText("€0.00");
            return;
        }

        java.math.BigDecimal total = orderLineService.calculateOrderTotal(order.orderId());
        selectedOrderTotalLabel.setText(euroFormat.format(total));
    }

    private void setupOrderLinesTable() {
        lineIdColumn.setCellValueFactory(cell ->
                new SimpleLongProperty(cell.getValue().orderLineId()));

        lineOrderIdColumn.setCellValueFactory(cell ->
                new SimpleLongProperty(cell.getValue().orderId()));

        menuItemColumn.setCellValueFactory(cell ->
                new SimpleStringProperty(cell.getValue().itemNameSnapshot()));

        priceColumn.setCellValueFactory(cell ->
                new SimpleStringProperty(String.valueOf(cell.getValue().unitPriceSnapshot())));

        quantityColumn.setCellValueFactory(cell ->
                new SimpleLongProperty(cell.getValue().quantity()));

        lineStatusColumn.setCellValueFactory(cell ->
                new SimpleStringProperty(String.valueOf(cell.getValue().lineStatus())));

        lineStatusColumn.setCellFactory(column -> new TableCell<>() {
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
                    case "NEW" -> setStyle("-fx-text-fill: #2563eb; -fx-font-weight: bold;");
                    case "IN_KITCHEN" -> setStyle("-fx-text-fill: #f59e0b; -fx-font-weight: bold;");
                    case "READY" -> setStyle("-fx-text-fill: #16a34a; -fx-font-weight: bold;");
                    case "SERVED" -> setStyle("-fx-text-fill: #7c3aed; -fx-font-weight: bold;");
                    case "CANCELLED" -> setStyle("-fx-text-fill: #dc2626; -fx-font-weight: bold;");
                    default -> setStyle("-fx-text-fill: #374151; -fx-font-weight: bold;");
                }
            }
        });
    }

    @FXML
    private void refreshOrders() {
        var orders = orderService.listOrders();
        System.out.println("Orders found: " + orders.size());

        orderData.setAll(
                orders.stream()
                        .sorted((a, b) -> b.createdAt().compareTo(a.createdAt()))
                        .toList()
        );
        ordersTable.refresh();

        loadOpenSessions();
        loadMenuItems();

        Order selected = ordersTable.getSelectionModel().getSelectedItem();
        loadLinesForSelectedOrder(selected);
        refreshSelectedOrderTotal(selected);
        statusLabel.setText("Orders refreshed");
    }

    @FXML
    private void createOrder() {
        try {
            SessionOption selectedSession = sessionSelector.getSelectionModel().getSelectedItem();
            if (selectedSession == null) {
                statusLabel.setText("No active session available");
                return;
            }

            long sessionId = selectedSession.sessionId();

            Long staffId = null;
            if (staffIdField.getText() != null && !staffIdField.getText().isBlank()) {
                staffId = Long.parseLong(staffIdField.getText().trim());
            }

            String notes = notesField.getText();
            if (notes != null && notes.isBlank()) {
                notes = null;
            }

            Order created = orderService.createOrder(sessionId, staffId, notes);
            refreshOrders();
            ordersTable.getSelectionModel().select(created);
            loadLinesForSelectedOrder(created);
            statusLabel.setText("Order created successfully");

            staffIdField.clear();
            notesField.clear();

        } catch (NumberFormatException e) {
            statusLabel.setText("Staff ID must be a valid number");
        } catch (Exception e) {
            statusLabel.setText("Error: " + e.getMessage());
        }
    }

    @FXML
    private void submitSelectedOrder() {
        Order selected = ordersTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            statusLabel.setText("Select an order first");
            return;
        }

        try {
            Order updated = orderService.submitOrder(selected.orderId());
            refreshOrders();
            ordersTable.getSelectionModel().select(updated);
            loadLinesForSelectedOrder(updated);
            statusLabel.setText("Order submitted successfully");
        } catch (Exception e) {
            statusLabel.setText("Error: " + e.getMessage());
        }
    }

    @FXML
    private void cancelSelectedOrder() {
        Order selected = ordersTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            statusLabel.setText("Select an order first");
            return;
        }

        try {
            Order updated = orderService.cancelOrder(selected.orderId());
            refreshOrders();
            ordersTable.getSelectionModel().select(updated);
            loadLinesForSelectedOrder(updated);
            statusLabel.setText("Order cancelled successfully");
        } catch (Exception e) {
            statusLabel.setText("Error: " + e.getMessage());
        }
    }

    @FXML
    private void addLineToSelectedOrder() {
        try {
            Order selectedOrder = ordersTable.getSelectionModel().getSelectedItem();
            if (selectedOrder == null) {
                statusLabel.setText("Select an order first");
                return;
            }

            MenuItemOption selectedItem = menuItemSelector.getSelectionModel().getSelectedItem();
            if (selectedItem == null) {
                statusLabel.setText("Select a menu item first");
                return;
            }

            int qty = Integer.parseInt(quantityField.getText().trim());

            orderLineService.addLine(selectedOrder.orderId(), selectedItem.menuItemId(), qty);
            loadLinesForSelectedOrder(selectedOrder);
            ordersTable.refresh();
            quantityField.clear();
            statusLabel.setText("Order line added successfully");

        } catch (NumberFormatException e) {
            statusLabel.setText("Quantity must be a valid number");
        } catch (Exception e) {
            statusLabel.setText("Error: " + e.getMessage());
        }
    }

    @FXML
    private void updateSelectedLineStatus() {
        try {
            OrderLine selectedLine = orderLinesTable.getSelectionModel().getSelectedItem();
            if (selectedLine == null) {
                statusLabel.setText("Select an order line first");
                return;
            }

            OrderLineStatus newStatus = lineStatusSelector.getSelectionModel().getSelectedItem();
            if (newStatus == null) {
                statusLabel.setText("Select a line status");
                return;
            }

            orderLineService.updateLineStatus(selectedLine.orderLineId(), newStatus);
            Order selectedOrder = ordersTable.getSelectionModel().getSelectedItem();
            loadLinesForSelectedOrder(selectedOrder);
            ordersTable.refresh();
            statusLabel.setText("Order line status updated");

        } catch (Exception e) {
            statusLabel.setText("Error: " + e.getMessage());
        }
    }

    @FXML
    private void cancelSelectedLine() {
        try {
            OrderLine selectedLine = orderLinesTable.getSelectionModel().getSelectedItem();
            if (selectedLine == null) {
                statusLabel.setText("Select an order line first");
                return;
            }

            orderLineService.cancelLine(selectedLine.orderLineId());
            Order selectedOrder = ordersTable.getSelectionModel().getSelectedItem();
            loadLinesForSelectedOrder(selectedOrder);
            ordersTable.refresh();
            statusLabel.setText("Order line cancelled");

        } catch (Exception e) {
            statusLabel.setText("Error: " + e.getMessage());
        }
    }

    private void loadOpenSessions() {
        sessionSelector.getItems().setAll(
                tableSessionService.listOpenSessions().stream()
                        .map(s -> new SessionOption(s.sessionId(), s.tableId()))
                        .toList()
        );

        if (!sessionSelector.getItems().isEmpty() && sessionSelector.getSelectionModel().getSelectedItem() == null) {
            sessionSelector.getSelectionModel().selectFirst();
        }
    }

    private void loadMenuItems() {
        java.util.List<MenuItem> items = orderLineService.listAvailableMenuItems();
        System.out.println("Available menu items: " + items.size());
        menuItemSelector.getItems().setAll(
                items.stream()
                        .map(i -> new MenuItemOption(i.menuItemId(), i.name(), i.price()))
                        .toList()
        );

        if (!menuItemSelector.getItems().isEmpty() && menuItemSelector.getSelectionModel().getSelectedItem() == null) {
            menuItemSelector.getSelectionModel().selectFirst();
        }
    }

    private void loadLinesForSelectedOrder(Order order) {
        if (order == null) {
            lineData.clear();
            selectedOrderTotalLabel.setText("€0.00");
            return;
        }

        lineData.setAll(
                orderLineService.listLinesForOrder(order.orderId()).stream()
                        .sorted((a, b) -> Long.compare(b.orderLineId(), a.orderLineId()))
                        .toList()
        );

        orderLinesTable.refresh();
        refreshSelectedOrderTotal(order);
    }
    
    
}