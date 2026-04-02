package ie.tus.oop2.restaurant.ui.controller;

import ie.tus.oop2.restaurant.model.DiningTable;
import ie.tus.oop2.restaurant.model.TableSession;
import ie.tus.oop2.restaurant.service.DiningTableService;
import ie.tus.oop2.restaurant.service.DiningTableServiceImpl;
import ie.tus.oop2.restaurant.service.TableSessionService;
import ie.tus.oop2.restaurant.service.TableSessionServiceImpl;

import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleLongProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;

public class TablesSessionsController {

    private final DiningTableService diningTableService = new DiningTableServiceImpl();
    private final TableSessionService tableSessionService = new TableSessionServiceImpl();

    private final ObservableList<DiningTable> tableData = FXCollections.observableArrayList();
    private final ObservableList<TableSession> sessionData = FXCollections.observableArrayList();

    @FXML
    private ComboBox<DiningTable> tableSelector;

    @FXML
    private TextField staffIdField;

    @FXML
    private Label statusLabel;

    @FXML
    private TableView<DiningTable> tablesTable;

    @FXML
    private TableColumn<DiningTable, Number> tableIdColumn;

    @FXML
    private TableColumn<DiningTable, String> tableLabelColumn;

    @FXML
    private TableColumn<DiningTable, Number> capacityColumn;

    @FXML
    private TableColumn<DiningTable, Boolean> activeColumn;

    @FXML
    private TableView<TableSession> sessionsTable;

    @FXML
    private TableColumn<TableSession, Number> sessionIdColumn;

    @FXML
    private TableColumn<TableSession, Number> sessionTableIdColumn;

    @FXML
    private TableColumn<TableSession, String> openedAtColumn;

    @FXML
    private TableColumn<TableSession, String> closedAtColumn;

    @FXML
    private TableColumn<TableSession, String> openedByColumn;

    @FXML
    private TableColumn<TableSession, String> sessionStatusColumn;

    @FXML
    public void initialize() {
        tableIdColumn.setCellValueFactory(cell -> new SimpleIntegerProperty(cell.getValue().tableId()));
        tableLabelColumn.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().label()));
        capacityColumn.setCellValueFactory(cell -> new SimpleIntegerProperty(cell.getValue().capacity()));
        activeColumn.setCellValueFactory(cell -> new SimpleBooleanProperty(cell.getValue().active()));

        sessionIdColumn.setCellValueFactory(cell -> new SimpleLongProperty(cell.getValue().sessionId()));
        sessionTableIdColumn.setCellValueFactory(cell -> new SimpleIntegerProperty(cell.getValue().tableId()));
        openedAtColumn.setCellValueFactory(cell -> new SimpleStringProperty(String.valueOf(cell.getValue().openedAt())));
        closedAtColumn.setCellValueFactory(cell -> new SimpleStringProperty(
                cell.getValue().closedAt() == null ? "-" : String.valueOf(cell.getValue().closedAt())
        ));
        openedByColumn.setCellValueFactory(cell -> new SimpleStringProperty(
                cell.getValue().openedByStaffId() == null ? "-" : String.valueOf(cell.getValue().openedByStaffId())
        ));
        sessionStatusColumn.setCellValueFactory(cell -> new SimpleStringProperty(String.valueOf(cell.getValue().status())));

        sessionStatusColumn.setCellFactory(column -> new TableCell<>() {
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
                    case "OPEN" -> setStyle("-fx-text-fill: #16a34a; -fx-font-weight: bold;");
                    case "CLOSED" -> setStyle("-fx-text-fill: #6b7280; -fx-font-weight: bold;");
                    default -> setStyle("-fx-text-fill: #374151; -fx-font-weight: bold;");
                }
            }
        });
        tablesTable.setItems(tableData);
        sessionsTable.setItems(sessionData);

        tableSelector.setCellFactory(param -> new ListCell<>() {
            @Override
            protected void updateItem(DiningTable item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : "Table " + item.tableId() + " - " + item.label());
            }
        });

        tableSelector.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(DiningTable item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : "Table " + item.tableId() + " - " + item.label());
            }
        });

        refreshData();
    }

    @FXML
    private void refreshData() {
        tableData.setAll(
                diningTableService.listTables().stream()
                        .sorted((a, b) -> Integer.compare(a.tableId(), b.tableId()))
                        .toList()
        );

        sessionData.setAll(
                tableSessionService.listAllSessions().stream()
                .sorted((a, b) -> {
                    boolean aOpen = a.status().name().equals("OPEN");
                    boolean bOpen = b.status().name().equals("OPEN");

                    if (aOpen && !bOpen) return -1;
                    if (!aOpen && bOpen) return 1;

                    return b.openedAt().compareTo(a.openedAt());
                })
                        .toList()
        );

        tableSelector.getItems().setAll(tableData);

        if (!tableSelector.getItems().isEmpty()) {
            tableSelector.getSelectionModel().selectFirst();
        }

        statusLabel.setText("Tables and sessions refreshed");
    }

    @FXML
    private void openSession() {
        DiningTable selectedTable = tableSelector.getSelectionModel().getSelectedItem();
        if (selectedTable == null) {
            statusLabel.setText("Select a table first");
            return;
        }

        Long staffId = null;
        try {
            if (staffIdField.getText() != null && !staffIdField.getText().isBlank()) {
                staffId = Long.parseLong(staffIdField.getText().trim());
            }

            tableSessionService.openSession(selectedTable.tableId(), staffId);
            refreshData();
            statusLabel.setText("Session opened successfully");

        } catch (NumberFormatException e) {
            statusLabel.setText("Staff ID must be a valid number");
        } catch (Exception e) {
            statusLabel.setText("Error: " + e.getMessage());
        }
    }

    @FXML
    private void closeSelectedSession() {
        TableSession selected = sessionsTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            statusLabel.setText("Select a session first");
            return;
        }

        try {
            tableSessionService.closeSession(selected.sessionId());
            refreshData();
            statusLabel.setText("Session closed successfully");
        } catch (Exception e) {
            statusLabel.setText("Error: " + e.getMessage());
        }
    }
}