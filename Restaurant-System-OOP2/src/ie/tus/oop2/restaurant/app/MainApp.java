package ie.tus.oop2.restaurant.app;

import ie.tus.oop2.restaurant.service.SettingsService;
import ie.tus.oop2.restaurant.service.SettingsServiceImpl;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class MainApp extends Application {

    @Override
    public void start(Stage stage) throws Exception {
        FXMLLoader loader = new FXMLLoader(
                MainApp.class.getResource("/ie/tus/oop2/restaurant/ui/view/main_layout.fxml")
        );

        Scene scene = new Scene(loader.load(), 1280, 800);
        scene.getStylesheets().add(
                MainApp.class.getResource("/ie/tus/oop2/restaurant/ui/view/app.css").toExternalForm()
        );

        SettingsService settingsService = new SettingsServiceImpl();
        stage.setTitle(settingsService.load().restaurantName());

        stage.setWidth(1280);
        stage.setHeight(800);
        stage.setMinWidth(1100);
        stage.setMinHeight(700);
        stage.centerOnScreen();
        stage.setScene(scene);
        stage.show();
        stage.toFront();
        stage.requestFocus();

        System.out.println("Stage showing = " + stage.isShowing());
        System.out.println("X = " + stage.getX() + ", Y = " + stage.getY());
        System.out.println("Width = " + stage.getWidth() + ", Height = " + stage.getHeight());
    }

    public static void main(String[] args) {
        launch(args);
    }
}