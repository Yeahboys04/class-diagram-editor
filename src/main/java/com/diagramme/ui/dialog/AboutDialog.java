package com.diagramme.ui.dialog;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.stage.Window;

import java.util.Objects;

/**
 * Boîte de dialogue "À propos"
 */
public class AboutDialog extends Dialog<Void> {

    public AboutDialog(Window owner) {
        // Configurer la boîte de dialogue
        initOwner(owner);
        setTitle("À propos");

        // Créer le contenu
        VBox content = new VBox(10);
        content.setAlignment(Pos.CENTER);
        content.setPadding(new Insets(20));

        // Logo
        try {
            Image logo = new Image(Objects.requireNonNull(getClass().getResourceAsStream("/images/app_logo.png")));
            ImageView logoView = new ImageView(logo);
            logoView.setFitHeight(100);
            logoView.setFitWidth(100);
            content.getChildren().add(logoView);
        } catch (Exception e) {
            // Ignorer si l'image n'est pas trouvée
        }

        // Nom de l'application
        Label nameLabel = new Label("Éditeur de Diagrammes de Classe");
        nameLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");

        // Version
        Label versionLabel = new Label("Version 1.0.0");

        // Copyright
        Label copyrightLabel = new Label("© 2023 - Tous droits réservés");

        // Description
        Label descriptionLabel = new Label(
                "Un éditeur de diagrammes de classe UML simple et intuitif pour Java."
        );
        descriptionLabel.setWrapText(true);
        descriptionLabel.setMaxWidth(400);
        descriptionLabel.setAlignment(Pos.CENTER);

        // Ajouter les composants
        content.getChildren().addAll(
                nameLabel,
                versionLabel,
                new javafx.scene.control.Separator(),
                descriptionLabel,
                new javafx.scene.control.Separator(),
                copyrightLabel
        );

        getDialogPane().setContent(content);

        // Ajouter un bouton Fermer
        getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
    }
}