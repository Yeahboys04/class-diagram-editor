package com.diagramme.ui.dialog;

import com.diagramme.model.ClassDiagram;
import com.diagramme.service.ExportService;
import com.diagramme.util.AlertUtils;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import lombok.Getter;

import java.io.File;
import java.io.IOException;

/**
 * Boîte de dialogue pour l'export de diagrammes
 */
@Getter
public class ExportDialog extends Dialog<Void> {

    private boolean exportSuccessful = false;

    public ExportDialog(Window owner, ClassDiagram diagram, ExportService exportService) {
        // Configurer la boîte de dialogue
        initOwner(owner);
        setTitle("Exporter le diagramme");
        setHeaderText("Exporter \"" + diagram.getName() + "\" vers un fichier");

        // Créer le contenu
        VBox content = new VBox(10);
        content.setPadding(new Insets(10));

        // Format d'export
        Label formatLabel = new Label("Format:");
        ComboBox<String> formatComboBox = new ComboBox<>(FXCollections.observableArrayList(
                "PNG", "SVG", "PDF", "JSON", "XMI"
        ));
        formatComboBox.setValue("PNG");

        // Description des formats
        TextArea descriptionArea = new TextArea();
        descriptionArea.setEditable(false);
        descriptionArea.setWrapText(true);
        descriptionArea.setPrefRowCount(3);
        updateFormatDescription(descriptionArea, "PNG");

        // Mettre à jour la description lorsque le format change
        formatComboBox.setOnAction(event -> updateFormatDescription(descriptionArea, formatComboBox.getValue()));

        // Ajouter les composants
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(5);

        grid.add(formatLabel, 0, 0);
        grid.add(formatComboBox, 1, 0);

        content.getChildren().addAll(grid, descriptionArea);

        getDialogPane().setContent(content);

        // Ajouter les boutons
        ButtonType exportButtonType = new ButtonType("Exporter", ButtonBar.ButtonData.OK_DONE);
        getDialogPane().getButtonTypes().addAll(exportButtonType, ButtonType.CANCEL);

        // Configurer le bouton d'export
        Button exportButton = (Button) getDialogPane().lookupButton(exportButtonType);
        exportButton.setOnAction(event -> {
            String format = formatComboBox.getValue();

            // Ouvrir une boîte de dialogue pour choisir le fichier
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Enregistrer le diagramme comme " + format);

            // Configurer les filtres selon le format
            switch (format) {
                case "PNG":
                    fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Images PNG", "*.png"));
                    break;
                case "SVG":
                    fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Images SVG", "*.svg"));
                    break;
                case "PDF":
                    fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Documents PDF", "*.pdf"));
                    break;
                case "JSON":
                    fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Fichiers JSON", "*.json"));
                    break;
                case "XMI":
                    fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Fichiers XMI", "*.xmi"));
                    break;
            }

            // Proposer un nom de fichier par défaut
            fileChooser.setInitialFileName(diagram.getName() + getExtensionForFormat(format));

            // Afficher la boîte de dialogue
            File file = fileChooser.showSaveDialog(getDialogPane().getScene().getWindow());
            if (file != null) {
                try {
                    // Exporter le diagramme
                    exportService.exportDiagram(diagram, format, file);

                    // Marquer comme réussi
                    exportSuccessful = true;

                    // Fermer la boîte de dialogue
                    close();

                    // Afficher un message de succès
                    AlertUtils.showInfoDialog("Export réussi",
                            "Diagramme exporté avec succès",
                            "Le diagramme a été exporté avec succès vers le fichier:\n" + file.getAbsolutePath());

                } catch (IOException e) {
                    AlertUtils.showErrorDialog("Erreur d'export",
                            "Erreur lors de l'export du diagramme",
                            e.getMessage());
                }
            }

            // Ne pas fermer la boîte de dialogue si l'utilisateur annule le choix de fichier
            event.consume();
        });

        // Ne pas utiliser le convertisseur de résultat par défaut
        setResultConverter(buttonType -> null);
    }

    /**
     * Met à jour la description du format
     */
    private void updateFormatDescription(TextArea descriptionArea, String format) {
        switch (format) {
            case "PNG":
                descriptionArea.setText("Image bitmap au format PNG. Idéal pour l'inclusion dans des documents ou sur le web. " +
                        "Les images PNG prennent en charge la transparence et offrent une bonne qualité.");
                break;
            case "SVG":
                descriptionArea.setText("Image vectorielle au format SVG. Parfait pour une utilisation dans des documents qui " +
                        "nécessitent une mise à l'échelle sans perte de qualité. Les SVG sont également facilement éditables.");
                break;
            case "PDF":
                descriptionArea.setText("Document PDF. Idéal pour l'impression ou pour le partage de documents officiels. " +
                        "Les PDF préservent la mise en page exacte quel que soit le système utilisé pour les visualiser.");
                break;
            case "JSON":
                descriptionArea.setText("Format JSON pour l'échange de données. Utile pour sauvegarder le diagramme et le " +
                        "recharger ultérieurement, ou pour l'intégration avec d'autres outils qui prennent en charge JSON.");
                break;
            case "XMI":
                descriptionArea.setText("Format XMI (XML Metadata Interchange). Standard d'échange de modèles UML, " +
                        "compatible avec la plupart des outils de modélisation UML.");
                break;
        }
    }

    /**
     * Retourne l'extension de fichier pour un format donné
     */
    private String getExtensionForFormat(String format) {
        return switch (format) {
            case "PNG" -> ".png";
            case "SVG" -> ".svg";
            case "PDF" -> ".pdf";
            case "JSON" -> ".json";
            case "XMI" -> ".xmi";
            default -> "";
        };
    }
}
