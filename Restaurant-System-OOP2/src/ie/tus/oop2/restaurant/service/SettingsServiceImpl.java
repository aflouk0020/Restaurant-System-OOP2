package ie.tus.oop2.restaurant.service;

import ie.tus.oop2.restaurant.ui.model.AppSettings;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public class SettingsServiceImpl implements SettingsService {

    private static final Path CONFIG_DIR = Path.of("exports", "config");
    private static final Path CONFIG_FILE = CONFIG_DIR.resolve("app-settings.properties");

    @Override
    public AppSettings load() {
        if (!Files.exists(CONFIG_FILE)) {
            AppSettings defaults = defaults();
            save(defaults);
            return defaults;
        }

        Properties props = new Properties();

        try (InputStream in = Files.newInputStream(CONFIG_FILE)) {
            props.load(in);

            return new AppSettings(
                    props.getProperty("restaurant.name", "Restaurant Management System"),
                    props.getProperty("currency.default", "EUR"),
                    new BigDecimal(props.getProperty("tax.rate", "0.13")),
                    props.getProperty("language", "en"),
                    props.getProperty("receipt.export.folder", "exports/receipts")
            );

        } catch (Exception e) {
            throw new RuntimeException("Failed to load settings", e);
        }
    }

    @Override
    public void save(AppSettings settings) {
        try {
            Files.createDirectories(CONFIG_DIR);

            Properties props = new Properties();
            props.setProperty("restaurant.name", settings.restaurantName());
            props.setProperty("currency.default", settings.defaultCurrency());
            props.setProperty("tax.rate", settings.taxRate().toPlainString());
            props.setProperty("language", settings.language());
            props.setProperty("receipt.export.folder", settings.receiptExportFolder());

            try (OutputStream out = Files.newOutputStream(CONFIG_FILE)) {
                props.store(out, "Restaurant System Settings");
            }

        } catch (IOException e) {
            throw new RuntimeException("Failed to save settings", e);
        }
    }

    @Override
    public AppSettings defaults() {
        return new AppSettings(
                "Restaurant Management System",
                "EUR",
                new BigDecimal("0.13"),
                "en",
                "exports/receipts"
        );
    }
}