package com.diagramme.ui.dialog;

import com.diagramme.model.ClassDiagram;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.paint.Color;
import javafx.stage.Window;

import java.util.Optional;

/**
 * Boîte de dialogue pour éditer les propriétés d'un diagramme
 */
public class DiagramPropertiesDialog extends Dialog<ClassDiagram> {

    private final ClassDiagram diagram;

    public DiagramPropertiesDialog(Window owner, ClassDiagram diagram) {
        this.diagram = diagram;

        // Configurer la boîte de dialogue
        initOwner(owner);
        setTitle("Propriétés du diagramme");
        setHeaderText("Éditer les propriétés du diagramme");

        // Créer le contenu
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        // Nom du diagramme
        TextField nameField = new TextField(diagram.getName());
        nameField.setPromptText("Nom du diagramme");
        grid.add(new Label("Nom:"), 0, 0);
        grid.add(nameField, 1, 0);

        // Description
        TextArea descriptionArea = new TextArea(diagram.getDescription());
        descriptionArea.setPromptText("Description du diagramme");
        descriptionArea.setPrefRowCount(3);
        grid.add(new Label("Description:"), 0, 1);
        grid.add(descriptionArea, 1, 1);

        // Auteur
        TextField authorField = new TextField(diagram.getAuthor());
        authorField.setPromptText("Auteur du diagramme");
        grid.add(new Label("Auteur:"), 0, 2);
        grid.add(authorField, 1, 2);

        // Version
        TextField versionField = new TextField(diagram.getVersion());
        versionField.setPromptText("Version du diagramme");
        grid.add(new Label("Version:"), 0, 3);
        grid.add(versionField, 1, 3);

        // Afficher la grille
        CheckBox showGridCheckBox = new CheckBox("Afficher la grille");
        showGridCheckBox.setSelected(diagram.isShowGrid());
        grid.add(showGridCheckBox, 0, 4, 2, 1);

        // Aligner sur la grille
        CheckBox snapToGridCheckBox = new CheckBox("Aligner sur la grille");
        snapToGridCheckBox.setSelected(diagram.isSnapToGrid());
        grid.add(snapToGridCheckBox, 0, 5, 2, 1);

        // Taille de la grille
        Spinner<Double> gridSizeSpinner = new Spinner<>(5, 100, diagram.getGridSize(), 5);
        gridSizeSpinner.setEditable(true);
        grid.add(new Label("Taille de la grille:"), 0, 6);
        grid.add(gridSizeSpinner, 1, 6);

        // Couleur de fond
        ColorPicker backgroundColorPicker = new ColorPicker(Color.web(diagram.getBackgroundColor()));
        grid.add(new Label("Couleur de fond:"), 0, 7);
        grid.add(backgroundColorPicker, 1, 7);

        getDialogPane().setContent(grid);

        // Ajouter les boutons
        getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        // Validation
        Button okButton = (Button) getDialogPane().lookupButton(ButtonType.OK);
        okButton.disableProperty().bind(nameField.textProperty().isEmpty());

        // Convertisseur de résultat
        setResultConverter(buttonType -> {
            if (buttonType == ButtonType.OK) {
                // Création d'une copie modifiée du diagramme
                ClassDiagram result = new ClassDiagram(nameField.getText());
                result.setId(diagram.getId());
                result.setUuid(diagram.getUuid());
                result.setDescription(descriptionArea.getText());
                result.setAuthor(authorField.getText());
                result.setVersion(versionField.getText());
                result.setShowGrid(showGridCheckBox.isSelected());
                result.setSnapToGrid(snapToGridCheckBox.isSelected());
                result.setGridSize(gridSizeSpinner.getValue());
                result.setBackgroundColor(backgroundColorPicker.getValue().toString());

                // Conserver les éléments du diagramme original
                for (var element : diagram.getElements()) {
                    result.addElement(element);
                }

                // Conserver les timestamps
                result.setCreatedAt(diagram.getCreatedAt());

                return result;
            }
            return null;
        });
    }
}