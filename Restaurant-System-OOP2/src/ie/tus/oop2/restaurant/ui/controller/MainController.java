package ie.tus.oop2.restaurant.ui.controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;

public class MainController {

    @FXML
    private Label sectionTitleLabel;

    @FXML
    private StackPane contentArea;

    private Button activeButton;

    @FXML
    public void initialize() {
        sectionTitleLabel.setText("Dashboard");

        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/ie/tus/oop2/restaurant/ui/view/dashboard.fxml")
            );
            contentArea.getChildren().clear();
            contentArea.getChildren().add(loader.load());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void setActive(Button button) {
        if (activeButton != null) {
            activeButton.getStyleClass().remove("nav-button-active");
        }

        activeButton = button;

        if (!activeButton.getStyleClass().contains("nav-button-active")) {
            activeButton.getStyleClass().add("nav-button-active");
        }
    }

    @FXML
    private void showDashboard(ActionEvent event) {
        setActive((Button) event.getSource());
        sectionTitleLabel.setText("Dashboard");

        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/ie/tus/oop2/restaurant/ui/view/dashboard.fxml")
            );

            contentArea.getChildren().clear();
            contentArea.getChildren().add(loader.load());

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    @FXML
    private void showTablesSessions(ActionEvent event) {
        setActive((Button) event.getSource());
        sectionTitleLabel.setText("Tables & Sessions");

        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/ie/tus/oop2/restaurant/ui/view/tables_sessions.fxml")
            );

            contentArea.getChildren().clear();
            contentArea.getChildren().add(loader.load());

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void showOrders(ActionEvent event) {
        setActive((Button) event.getSource());
        sectionTitleLabel.setText("Orders");

        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/ie/tus/oop2/restaurant/ui/view/orders.fxml")
            );

            contentArea.getChildren().clear();
            contentArea.getChildren().add(loader.load());

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void showReceipts(javafx.event.ActionEvent event) {
        setActive((javafx.scene.control.Button) event.getSource());
        sectionTitleLabel.setText("Receipts");

        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(
                    getClass().getResource("/ie/tus/oop2/restaurant/ui/view/receipts.fxml")
            );

            contentArea.getChildren().clear();
            contentArea.getChildren().add(loader.load());

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void showManagerReports(ActionEvent event) {
        setActive((Button) event.getSource());
        sectionTitleLabel.setText("Manager Reports");

        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/ie/tus/oop2/restaurant/ui/view/manager_reports.fxml")
            );

            contentArea.getChildren().clear();
            contentArea.getChildren().add(loader.load());

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void showSettings(ActionEvent event) {
        setActive((Button) event.getSource());
        sectionTitleLabel.setText("Settings");
        contentArea.getChildren().clear();
    }
    
    @FXML
    private void showPayments(javafx.event.ActionEvent event) {
        setActive((javafx.scene.control.Button) event.getSource());
        sectionTitleLabel.setText("Payments");

        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(
                    getClass().getResource("/ie/tus/oop2/restaurant/ui/view/payments.fxml")
            );

            contentArea.getChildren().clear();
            contentArea.getChildren().add(loader.load());

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}