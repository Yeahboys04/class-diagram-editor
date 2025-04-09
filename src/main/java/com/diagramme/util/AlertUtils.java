package com.diagramme.util;

import javafx.scene.control.Alert;
import javafx.scene.control.TextArea;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;

/**
 * Utilitaires pour les boîtes de dialogue d'alerte
 */
public class AlertUtils {

    /**
     * Affiche une boîte de dialogue d'information
     */
    public static void showInfoDialog(String title, String header, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
    }

    /**
     * Affiche une boîte de dialogue d'avertissement
     */
    public static void showWarningDialog(String title, String header, String content) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
    }

    /**
     * Affiche une boîte de dialogue d'erreur
     */
    public static void showErrorDialog(String title, String header, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
    }

    /**
     * Affiche une boîte de dialogue d'erreur avec une trace d'exception
     */
    public static void showExceptionDialog(String title, String header, Throwable exception) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(exception.getMessage());

        // Créer une zone de texte pour la trace d'exception
        TextArea textArea = new TextArea();
        textArea.setEditable(false);
        textArea.setWrapText(true);

        // Générer la trace d'exception
        StringBuilder stackTrace = new StringBuilder();
        stackTrace.append(exception.toString()).append("\n");
        for (StackTraceElement element : exception.getStackTrace()) {
            stackTrace.append("\tat ").append(element).append("\n");
        }
        textArea.setText(stackTrace.toString());

        // Configurer la disposition
        GridPane.setVgrow(textArea, Priority.ALWAYS);
        GridPane.setHgrow(textArea, Priority.ALWAYS);

        GridPane expContent = new GridPane();
        expContent.setMaxWidth(Double.MAX_VALUE);
        expContent.add(textArea, 0, 0);

        // Définir la zone de contenu extensible
        alert.getDialogPane().setExpandableContent(expContent);
        alert.showAndWait();
    }
}