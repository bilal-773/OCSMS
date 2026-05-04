package com.ocsms;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * Main — JavaFX Application entry point.
 * Launches the OCSMS Login screen.
 */
public class Main extends Application {

    private static Stage primaryStage;

    @Override
    public void start(Stage stage) throws Exception {
        primaryStage = stage;
        stage.setTitle("OCSMS — Online College Society Management System | FAST-NUCES Peshawar");
        stage.setMinWidth(900);
        stage.setMinHeight(650);

        showLogin();
        stage.show();
    }

    public static void showLogin() throws Exception {
        FXMLLoader loader = new FXMLLoader(
            Main.class.getResource("/fxml/login.fxml"));
        Parent root = loader.load();
        Scene scene = new Scene(root);
        scene.getStylesheets().add(
            Main.class.getResource("/css/styles.css").toExternalForm());
        primaryStage.setScene(scene);
        primaryStage.centerOnScreen();
    }

    public static void showDashboard() throws Exception {
        FXMLLoader loader = new FXMLLoader(
            Main.class.getResource("/fxml/dashboard.fxml"));
        Parent root = loader.load();
        Scene scene = new Scene(root, 1200, 750);
        scene.getStylesheets().add(
            Main.class.getResource("/css/styles.css").toExternalForm());
        primaryStage.setScene(scene);
        primaryStage.setMaximized(true);
    }

    public static Stage getPrimaryStage() {
        return primaryStage;
    }

    public static void main(String[] args) {
        launch(args);
    }
}
