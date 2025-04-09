package com.diagramme.ui.dialog;

import com.diagramme.model.ClassElement;
import com.diagramme.model.RelationshipElement;
import com.diagramme.model.enums.RelationshipType;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Window;

/**
 * Boîte de dialogue pour éditer une relation
 */
public class RelationshipDialog extends Dialog<RelationshipElement> {

    private final ClassElement sourceElement;
    private final ClassElement targetElement;
    private final RelationshipElement relationship;

    private TextField nameField;
    private ComboBox<RelationshipType> typeComboBox;
    private TextField sourceRoleField;
    private TextField targetRoleField;
    private TextField sourceMultiplicityField;
    private TextField targetMultiplicityField;
    private ColorPicker lineColorPicker;
    private Spinner<Double> lineWidthSpinner;
    private ComboBox<String> lineStyleComboBox;

    public RelationshipDialog(Window owner, ClassElement sourceElement, ClassElement targetElement) {
        this(owner, sourceElement, targetElement, null);
    }

    public RelationshipDialog(Window owner, ClassElement sourceElement, ClassElement targetElement, RelationshipElement relationship) {
        this.sourceElement = sourceElement;
        this.targetElement = targetElement;
        this.relationship = relationship;

        // Configurer la boîte de dialogue
        initOwner(owner);
        setTitle(relationship != null ? "Éditer la relation" : "Ajouter une relation");
        setHeaderText(createHeaderText());

        // Créer le contenu
        VBox content = createContent();
        getDialogPane().setContent(content);

        // Configurer les boutons
        getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        // Validation
        Button okButton = (Button) getDialogPane().lookupButton(ButtonType.OK);
        okButton.disableProperty().bind(nameField.textProperty().isEmpty());

        // Convertisseur de résultat
        setResultConverter(buttonType -> {
            if (buttonType == ButtonType.OK) {
                return createRelationship();
            }
            return null;
        });
    }

    /**
     * Crée le contenu de la boîte de dialogue
     */
    private VBox createContent() {
        VBox content = new VBox(10);
        content.setPadding(new Insets(10));

        // Section de base
        content.getChildren().add(createBasicSection());

        // Section source
        content.getChildren().add(createSourceSection());

        // Section cible
        content.getChildren().add(createTargetSection());

        // Section style
        content.getChildren().add(createStyleSection());

        return content;
    }

    /**
     * Crée la section des propriétés de base
     */
    private VBox createBasicSection() {
        VBox section = new VBox(5);

        // Nom
        nameField = new TextField();
        nameField.setPromptText("Nom de la relation");
        if (relationship != null) {
            nameField.setText(relationship.getName());
        } else {
            nameField.setText(sourceElement.getName() + " -> " + targetElement.getName());
        }

        // Type de relation
        typeComboBox = new ComboBox<>(FXCollections.observableArrayList(RelationshipType.values()));
        typeComboBox.setConverter(new javafx.util.StringConverter<RelationshipType>() {
            @Override
            public String toString(RelationshipType type) {
                return type.getLabel();
            }

            @Override
            public RelationshipType fromString(String string) {
                return null;
            }
        });

        if (relationship != null) {
            typeComboBox.setValue(relationship.getType());
        } else {
            typeComboBox.setValue(RelationshipType.ASSOCIATION);
        }

        // Description du type
        Label descriptionLabel = new Label();
        if (relationship != null) {
            descriptionLabel.setText(relationship.getType().getDescription());
        } else {
            descriptionLabel.setText(RelationshipType.ASSOCIATION.getDescription());
        }

        // Mettre à jour la description lorsque le type change
        typeComboBox.setOnAction(event -> {
            RelationshipType selectedType = typeComboBox.getValue();
            if (selectedType != null) {
                descriptionLabel.setText(selectedType.getDescription());
            }
        });

        // Ajouter à la section
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(5);

        grid.add(new Label("Nom:"), 0, 0);
        grid.add(nameField, 1, 0);

        grid.add(new Label("Type:"), 0, 1);
        grid.add(typeComboBox, 1, 1);

        section.getChildren().addAll(grid, descriptionLabel);

        return section;
    }

    /**
     * Crée la section des propriétés de la source
     */
    private VBox createSourceSection() {
        VBox section = new VBox(5);
        section.setPadding(new Insets(10, 0, 0, 0));

        // En-tête
        Label titleLabel = new Label("Source: " + sourceElement.getName());
        titleLabel.setStyle("-fx-font-weight: bold;");

        // Rôle
        sourceRoleField = new TextField();
        sourceRoleField.setPromptText("Rôle de la source (optionnel)");
        if (relationship != null && relationship.getSourceRole() != null) {
            sourceRoleField.setText(relationship.getSourceRole());
        }

        // Multiplicité
        sourceMultiplicityField = new TextField();
        sourceMultiplicityField.setPromptText("Multiplicité de la source (optionnel)");
        if (relationship != null && relationship.getSourceMultiplicity() != null) {
            sourceMultiplicityField.setText(relationship.getSourceMultiplicity());
        }

        // Ajouter à la section
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(5);

        grid.add(new Label("Rôle:"), 0, 0);
        grid.add(sourceRoleField, 1, 0);

        grid.add(new Label("Multiplicité:"), 0, 1);
        grid.add(sourceMultiplicityField, 1, 1);

        section.getChildren().addAll(titleLabel, grid);

        return section;
    }

    /**
     * Crée la section des propriétés de la cible
     */
    private VBox createTargetSection() {
        VBox section = new VBox(5);
        section.setPadding(new Insets(10, 0, 0, 0));

        // En-tête
        Label titleLabel = new Label("Cible: " + targetElement.getName());
        titleLabel.setStyle("-fx-font-weight: bold;");

        // Rôle
        targetRoleField = new TextField();
        targetRoleField.setPromptText("Rôle de la cible (optionnel)");
        if (relationship != null && relationship.getTargetRole() != null) {
            targetRoleField.setText(relationship.getTargetRole());
        }

        // Multiplicité
        targetMultiplicityField = new TextField();
        targetMultiplicityField.setPromptText("Multiplicité de la cible (optionnel)");
        if (relationship != null && relationship.getTargetMultiplicity() != null) {
            targetMultiplicityField.setText(relationship.getTargetMultiplicity());
        }

        // Ajouter à la section
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(5);

        grid.add(new Label("Rôle:"), 0, 0);
        grid.add(targetRoleField, 1, 0);

        grid.add(new Label("Multiplicité:"), 0, 1);
        grid.add(targetMultiplicityField, 1, 1);

        section.getChildren().addAll(titleLabel, grid);

        return section;
    }

    /**
     * Crée la section des propriétés de style
     */
    private VBox createStyleSection() {
        VBox section = new VBox(5);
        section.setPadding(new Insets(10, 0, 0, 0));

        // En-tête
        Label titleLabel = new Label("Style");
        titleLabel.setStyle("-fx-font-weight: bold;");

        // Couleur de ligne
        lineColorPicker = new ColorPicker();
        if (relationship != null) {
            lineColorPicker.setValue(javafx.scene.paint.Color.web(relationship.getLineColor()));
        } else {
            lineColorPicker.setValue(javafx.scene.paint.Color.BLACK);
        }

        // Épaisseur de ligne
        lineWidthSpinner = new Spinner<>(0.5, 5.0, 1.0, 0.5);
        if (relationship != null) {
            lineWidthSpinner.getValueFactory().setValue(relationship.getLineWidth());
        }

        // Style de ligne
        lineStyleComboBox = new ComboBox<>(FXCollections.observableArrayList("SOLID", "DASHED", "DOTTED"));
        if (relationship != null) {
            lineStyleComboBox.setValue(relationship.getLineStyle());
        } else {
            lineStyleComboBox.setValue("SOLID");
        }

        // Ajouter à la section
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(5);

        grid.add(new Label("Couleur:"), 0, 0);
        grid.add(lineColorPicker, 1, 0);

        grid.add(new Label("Épaisseur:"), 0, 1);
        grid.add(lineWidthSpinner, 1, 1);

        grid.add(new Label("Style:"), 0, 2);
        grid.add(lineStyleComboBox, 1, 2);

        section.getChildren().addAll(titleLabel, grid);

        return section;
    }

    /**
     * Crée une relation à partir des données saisies
     */
    private RelationshipElement createRelationship() {
        RelationshipElement result;

        if (relationship != null) {
            // Mise à jour d'une relation existante
            result = relationship;
        } else {
            // Création d'une nouvelle relation
            result = new RelationshipElement(nameField.getText(), sourceElement, targetElement, typeComboBox.getValue());
        }

        // Mettre à jour les propriétés
        result.setName(nameField.getText());
        result.setType(typeComboBox.getValue());

        result.setSourceRole(sourceRoleField.getText());
        result.setTargetRole(targetRoleField.getText());

        result.setSourceMultiplicity(sourceMultiplicityField.getText());
        result.setTargetMultiplicity(targetMultiplicityField.getText());

        result.setLineColor(lineColorPicker.getValue().toString());
        result.setLineWidth(lineWidthSpinner.getValue());
        result.setLineStyle(lineStyleComboBox.getValue());

        return result;
    }

    /**
     * Retourne l'en-tête de la boîte de dialogue
     */
    private String createHeaderText() {
        if (relationship != null) {
            return "Éditer la relation entre " + sourceElement.getName() + " et " + targetElement.getName();
        } else {
            return "Créer une relation entre " + sourceElement.getName() + " et " + targetElement.getName();
        }
    }
}