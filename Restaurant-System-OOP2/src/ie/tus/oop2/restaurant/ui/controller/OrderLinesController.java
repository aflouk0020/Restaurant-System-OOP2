package ie.tus.oop2.restaurant.ui.controller;

import ie.tus.oop2.restaurant.model.MenuItem;
import ie.tus.oop2.restaurant.model.Order;
import ie.tus.oop2.restaurant.model.OrderLine;
import ie.tus.oop2.restaurant.model.OrderLineStatus;
import ie.tus.oop2.restaurant.service.OrderLineService;
import ie.tus.oop2.restaurant.service.OrderLineServiceImpl;
import ie.tus.oop2.restaurant.service.OrderService;
import ie.tus.oop2.restaurant.service.OrderServiceImpl;
import ie.tus.oop2.restaurant.ui.model.MenuItemOption;
import ie.tus.oop2.restaurant.ui.model.OrderOption;

import javafx.beans.property.SimpleLongProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.util.List;

public class OrderLinesController {

    private final OrderLineService orderLineService = new OrderLineServiceImpl();
    private final OrderService orderService = new OrderServiceImpl();
    private final ObservableList<OrderLine> lineData = FXCollections.observableArrayList();

    @FXML
    private ComboBox<OrderOption> orderSelector;

    @FXML
    private ComboBox<MenuItemOption> menuItemSelector;

    @FXML
    private TextField quantityField;

    @FXML
    private ComboBox<OrderLineStatus> statusSelector;

    @FXML
    private Label statusLabel;

    @FXML
    private TableView<OrderLine> orderLinesTable;

    @FXML
    private TableColumn<OrderLine, Number> lineIdColumn;

    @FXML
    private TableColumn<OrderLine, Number> orderIdColumn;

    @FXML
    private TableColumn<OrderLine, String> menuItemColumn;

    @FXML
    private TableColumn<OrderLine, String> priceColumn;

    @FXML
    private TableColumn<OrderLine, Number> quantityColumn;

    @FXML
    private TableColumn<OrderLine, String> statusColumn;

    @FXML
    public void initialize() {
        lineIdColumn.setCellValueFactory(cell -> new SimpleLongProperty(cell.getValue().orderLineId()));
        orderIdColumn.setCellValueFactory(cell -> new SimpleLongProperty(cell.getValue().orderId()));
        menuItemColumn.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().itemNameSnapshot()));
        priceColumn.setCellValueFactory(cell -> new SimpleStringProperty(String.valueOf(cell.getValue().unitPriceSnapshot())));
        quantityColumn.setCellValueFactory(cell -> new SimpleLongProperty(cell.getValue().quantity()));
        statusColumn.setCellValueFactory(cell -> new SimpleStringProperty(String.valueOf(cell.getValue().lineStatus())));

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
                    case "NEW" -> setStyle("-fx-text-fill: #2563eb; -fx-font-weight: bold;");
                    case "IN_KITCHEN" -> setStyle("-fx-text-fill: #f59e0b; -fx-font-weight: bold;");
                    case "READY" -> setStyle("-fx-text-fill: #16a34a; -fx-font-weight: bold;");
                    case "SERVED" -> setStyle("-fx-text-fill: #7c3aed; -fx-font-weight: bold;");
                    case "CANCELLED" -> setStyle("-fx-text-fill: #dc2626; -fx-font-weight: bold;");
                    default -> setStyle("-fx-text-fill: #374151; -fx-font-weight: bold;");
                }
            }
        });

        orderLinesTable.setItems(lineData);
        statusSelector.setItems(FXCollections.observableArrayList(OrderLineStatus.values()));

        loadOrders();
        loadMenuItems();
        refreshLines();
    }

    @FXML
    private void refreshLines() {
        OrderOption selectedOrder = orderSelector.getSelectionModel().getSelectedItem();

        if (selectedOrder != null) {
            lineData.setAll(
                    orderLineService.listLinesForOrder(selectedOrder.orderId()).stream()
                            .sorted((a, b) -> Long.compare(b.orderLineId(), a.orderLineId()))
                            .toList()
            );
        } else {
            lineData.setAll(
                    orderLineService.listAllLines().stream()
                            .sorted((a, b) -> Long.compare(b.orderLineId(), a.orderLineId()))
                            .toList()
            );
        }

        loadOrders();
        loadMenuItems();
        statusLabel.setText("Order lines refreshed");
    }

    @FXML
    private void addLine() {
        try {
            OrderOption selectedOrder = orderSelector.getSelectionModel().getSelectedItem();
            MenuItemOption selectedMenuItem = menuItemSelector.getSelectionModel().getSelectedItem();

            if (selectedOrder == null) {
                statusLabel.setText("Select an order first");
                return;
            }

            if (selectedMenuItem == null) {
                statusLabel.setText("Select a menu item first");
                return;
            }

            int quantity = Integer.parseInt(quantityField.getText().trim());

            orderLineService.addLine(
                    selectedOrder.orderId(),
                    selectedMenuItem.menuItemId(),
                    quantity
            );

            refreshLines();
            quantityField.clear();
            statusLabel.setText("Order line added successfully");

        } catch (NumberFormatException e) {
            statusLabel.setText("Quantity must be a valid number");
        } catch (Exception e) {
            statusLabel.setText("Error: " + e.getMessage());
        }
    }

    @FXML
    private void updateSelectedStatus() {
        try {
            OrderLine selectedLine = orderLinesTable.getSelectionModel().getSelectedItem();
            OrderLineStatus newStatus = statusSelector.getSelectionModel().getSelectedItem();

            if (selectedLine == null) {
                statusLabel.setText("Select an order line first");
                return;
            }

            if (newStatus == null) {
                statusLabel.setText("Select a new status");
                return;
            }

            orderLineService.updateLineStatus(selectedLine.orderLineId(), newStatus);
            refreshLines();
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
            refreshLines();
            statusLabel.setText("Order line cancelled");

        } catch (Exception e) {
            statusLabel.setText("Error: " + e.getMessage());
        }
    }

    private void loadOrders() {
        List<Order> orders = orderService.listOrders();

        orderSelector.getItems().setAll(
                orders.stream()
                        .map(o -> new OrderOption(o.orderId(), o.sessionId(), o.status()))
                        .toList()
        );

        if (!orderSelector.getItems().isEmpty() && orderSelector.getSelectionModel().getSelectedItem() == null) {
            orderSelector.getSelectionModel().selectFirst();
        }
    }

    private void loadMenuItems() {
        List<MenuItem> items = orderLineService.listAvailableMenuItems();

        menuItemSelector.getItems().setAll(
                items.stream()
                        .map(i -> new MenuItemOption(i.menuItemId(), i.name(), i.price()))
                        .toList()
        );

        if (!menuItemSelector.getItems().isEmpty() && menuItemSelector.getSelectionModel().getSelectedItem() == null) {
            menuItemSelector.getSelectionModel().selectFirst();
        }
    }
}
