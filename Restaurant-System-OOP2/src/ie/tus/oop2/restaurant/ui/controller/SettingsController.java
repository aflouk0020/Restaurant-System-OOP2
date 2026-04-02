package ie.tus.oop2.restaurant.ui.controller;

import ie.tus.oop2.restaurant.service.SettingsService;
import ie.tus.oop2.restaurant.service.SettingsServiceImpl;
import ie.tus.oop2.restaurant.ui.model.AppSettings;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

import java.math.BigDecimal;

public class SettingsController {

    private final SettingsService settingsService = new SettingsServiceImpl();

    @FXML
    private TextField restaurantNameField;

    @FXML
    private ComboBox<String> currencySelector;

    @FXML
    private TextField taxRateField;

    @FXML
    private ComboBox<String> languageSelector;

    @FXML
    private TextField exportFolderField;

    @FXML
    private Label statusLabel;

    @FXML
    public void initialize() {
        currencySelector.setItems(FXCollections.observableArrayList("EUR", "USD", "GBP"));
        languageSelector.setItems(FXCollections.observableArrayList("en", "fr"));

        loadSettings();
    }

    @FXML
    private void saveSettings() {
        try {
            String restaurantName = restaurantNameField.getText();
            String currency = currencySelector.getValue();
            String language = languageSelector.getValue();
            String exportFolder = exportFolderField.getText();

            BigDecimal taxRate = new BigDecimal(taxRateField.getText().trim());

            if (restaurantName == null || restaurantName.isBlank()) {
                statusLabel.setText("Restaurant name is required");
                return;
            }

            if (currency == null || currency.isBlank()) {
                statusLabel.setText("Select a currency");
                return;
            }

            if (language == null || language.isBlank()) {
                statusLabel.setText("Select a language");
                return;
            }

            if (exportFolder == null || exportFolder.isBlank()) {
                statusLabel.setText("Export folder is required");
                return;
            }

            if (taxRate.compareTo(BigDecimal.ZERO) < 0) {
                statusLabel.setText("Tax rate cannot be negative");
                return;
            }

            AppSettings settings = new AppSettings(
                    restaurantName.trim(),
                    currency,
                    taxRate,
                    language,
                    exportFolder.trim()
            );

            settingsService.save(settings);
            statusLabel.setText("Settings saved successfully");

        } catch (NumberFormatException e) {
            statusLabel.setText("Tax rate must be a valid decimal number");
        } catch (Exception e) {
            statusLabel.setText("Error: " + e.getMessage());
        }
    }

    @FXML
    private void resetDefaults() {
        try {
            AppSettings defaults = settingsService.defaults();
            settingsService.save(defaults);
            applyToFields(defaults);
            statusLabel.setText("Settings reset to defaults");
        } catch (Exception e) {
            statusLabel.setText("Error: " + e.getMessage());
        }
    }

    private void loadSettings() {
        try {
            AppSettings settings = settingsService.load();
            applyToFields(settings);
            statusLabel.setText("Settings loaded");
        } catch (Exception e) {
            statusLabel.setText("Error: " + e.getMessage());
        }
    }

    private void applyToFields(AppSettings settings) {
        restaurantNameField.setText(settings.restaurantName());
        currencySelector.setValue(settings.defaultCurrency());
        taxRateField.setText(settings.taxRate().toPlainString());
        languageSelector.setValue(settings.language());
        exportFolderField.setText(settings.receiptExportFolder());
    }
}