package com.ocsms.util;

import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TextField;
import java.util.Optional;

/**
 * AlertUtil — reusable alert dialogs for the OCSMS GUI.
 * Provides error dialogs, confirmation dialogs, and field highlighting.
 */
public class AlertUtil {

    public static void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    public static void showInfo(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    public static void showWarning(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    public static boolean confirm(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        Optional<ButtonType> result = alert.showAndWait();
        return result.isPresent() && result.get() == ButtonType.OK;
    }

    public static void highlightError(TextField field) {
        field.setStyle("-fx-border-color: #ef4444; -fx-border-width: 1.5px; -fx-border-radius: 4px;");
        field.textProperty().addListener(e -> field.setStyle(""));
    }

    public static void clearHighlight(TextField field) {
        field.setStyle("");
    }
}
