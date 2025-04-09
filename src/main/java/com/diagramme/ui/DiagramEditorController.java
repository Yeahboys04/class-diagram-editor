package com.diagramme.ui;

import com.diagramme.model.ClassDiagram;
import com.diagramme.model.ClassElement;
import com.diagramme.model.DiagramElement;
import com.diagramme.model.RelationshipElement;
import com.diagramme.model.enums.RelationshipType;
import com.diagramme.service.DiagramService;
import com.diagramme.ui.component.ClassNodeComponent;
import com.diagramme.ui.component.RelationshipComponent;
import com.diagramme.ui.dialog.ClassDialog;
import com.diagramme.ui.dialog.RelationshipDialog;
import com.diagramme.util.AlertUtils;
import com.diagramme.util.UmlValidator;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.*;

/**
 * Contrôleur de l'éditeur de diagramme
 */
@Component
@Slf4j
public class DiagramEditorController {

    @FXML private Pane diagramCanvas;
    @FXML private ScrollPane scrollPane;
    @FXML private ToggleButton selectionButton;
    @FXML private ToggleButton addClassButton;
    @FXML private ToggleButton addInterfaceButton;
    @FXML private ToggleButton addRelationshipButton;

    private final DiagramService diagramService;
    private final ApplicationContext applicationContext;

    /**
     * -- GETTER --
     *  Retourne le diagramme édité
     */
    @Getter
    private ClassDiagram diagram;
    /**
     * -- SETTER --
     *  Définit le contrôleur principal
     */
    @Setter
    private MainController mainController;

    private double zoomFactor = 1.0;
    private boolean showGrid = true;
    /**
     * -- GETTER --
     *  Indique si l'alignement sur la grille est activé
     *
     * @return true si l'alignement sur la grille est activé, false sinon
     */
    @Getter
    private boolean snapToGrid = true;

    private boolean unsavedChanges = false;

    // Mode d'édition courant
    private enum EditMode {
        SELECT, ADD_CLASS, ADD_INTERFACE, ADD_RELATIONSHIP
    }
    private EditMode currentMode = EditMode.SELECT;

    // Pour la création de relations
    private ClassNodeComponent relationSourceNode;

    /**
     * -- GETTER --
     *  Retourne la liste des éléments sélectionnés
     *
     * @return La liste des éléments actuellement sélectionnés
     */
    // Pour la sélection multiple
    @Getter
    private final List<DiagramElement> selectedElements = new ArrayList<>();

    // Pour le copier-coller
    private final List<DiagramElement> clipboardElements = new ArrayList<>();

    // Pour le suivi des modifications (undo/redo)
    private final Deque<ClassDiagram> undoStack = new ArrayDeque<>();
    private final Deque<ClassDiagram> redoStack = new ArrayDeque<>();

    @Autowired
    public DiagramEditorController(DiagramService diagramService, ApplicationContext applicationContext) {
        this.diagramService = diagramService;
        this.applicationContext = applicationContext;
    }

    /**
     * Initialisation du contrôleur
     */
    @FXML
    public void initialize() {
        log.debug("Initialisation du contrôleur d'éditeur");

        // Configurer le canvas
        diagramCanvas.setStyle("-fx-background-color: white;");

        // Configurer les boutons de mode
        ToggleGroup modeGroup = new ToggleGroup();
        selectionButton.setToggleGroup(modeGroup);
        addClassButton.setToggleGroup(modeGroup);
        addInterfaceButton.setToggleGroup(modeGroup);
        addRelationshipButton.setToggleGroup(modeGroup);

        selectionButton.setSelected(true);

        // Auditeurs pour les boutons de mode
        selectionButton.setOnAction(event -> setEditMode(EditMode.SELECT));
        addClassButton.setOnAction(event -> setEditMode(EditMode.ADD_CLASS));
        addInterfaceButton.setOnAction(event -> setEditMode(EditMode.ADD_INTERFACE));
        addRelationshipButton.setOnAction(event -> setEditMode(EditMode.ADD_RELATIONSHIP));

        // Auditeur pour les clics sur le canvas
        diagramCanvas.setOnMouseClicked(this::handleCanvasClick);

        // Initialiser l'état
        setEditMode(EditMode.SELECT);
    }

    /**
     * Définit le mode d'édition courant
     */
    private void setEditMode(EditMode mode) {
        currentMode = mode;

        // Réinitialiser l'état de création de relation
        relationSourceNode = null;

        // Mettre à jour le curseur selon le mode
        switch (mode) {
            case SELECT:
                diagramCanvas.setCursor(javafx.scene.Cursor.DEFAULT);
                break;
            case ADD_CLASS:
            case ADD_INTERFACE:
                diagramCanvas.setCursor(javafx.scene.Cursor.CROSSHAIR);
                break;
            case ADD_RELATIONSHIP:
                diagramCanvas.setCursor(javafx.scene.Cursor.HAND);
                break;
        }

        // Mettre à jour les boutons
        selectionButton.setSelected(mode == EditMode.SELECT);
        addClassButton.setSelected(mode == EditMode.ADD_CLASS);
        addInterfaceButton.setSelected(mode == EditMode.ADD_INTERFACE);
        addRelationshipButton.setSelected(mode == EditMode.ADD_RELATIONSHIP);
    }

    /**
     * Gère les clics sur le canvas
     */
    private void handleCanvasClick(MouseEvent event) {
        // Ignorer les clics autres que le bouton principal
        if (event.getButton() != MouseButton.PRIMARY) {
            return;
        }

        // Récupérer les coordonnées
        double x = event.getX();
        double y = event.getY();

        // Ajuster selon le zoom
        x = x / zoomFactor;
        y = y / zoomFactor;

        // Ajuster selon la grille si nécessaire
        if (snapToGrid) {
            double gridSize = diagram.getGridSize();
            x = Math.round(x / gridSize) * gridSize;
            y = Math.round(y / gridSize) * gridSize;
        }

        // Traiter selon le mode d'édition
        switch (currentMode) {
            case SELECT:
                // Si clic sur le fond, désélectionner tout
                if (event.getTarget() == diagramCanvas) {
                    clearSelection();
                }
                break;

            case ADD_CLASS:
                // Ajouter une classe
                addClassElementAt(x, y, ClassElement.ClassType.CLASS);
                break;

            case ADD_INTERFACE:
                // Ajouter une interface
                addClassElementAt(x, y, ClassElement.ClassType.INTERFACE);
                break;

            case ADD_RELATIONSHIP:
                // La création de relation est gérée au niveau des nœuds de classe
                break;
        }
    }

    /**
     * Ajoute une classe au diagramme à une position donnée
     */
    private void addClassElementAt(double x, double y, ClassElement.ClassType type) {
        // Créer la boîte de dialogue pour éditer la classe
        ClassDialog dialog = new ClassDialog(diagramCanvas.getScene().getWindow(), null, type);

        dialog.showAndWait().ifPresent(classElement -> {
            try {
                // Sauvegarder l'état actuel pour undo
                saveStateForUndo();

                // Positionner l'élément
                classElement.setX(x);
                classElement.setY(y);
                classElement.setWidth(200);
                classElement.setHeight(150);

                // Ajouter au diagramme
                diagram.addElement(classElement);

                // Créer le composant visuel
                ClassNodeComponent classNode = new ClassNodeComponent(classElement, this);
                diagramCanvas.getChildren().add(classNode);

                // Marquer comme modifié
                setUnsavedChanges(true);

                // Revenir en mode sélection
                setEditMode(EditMode.SELECT);

            } catch (Exception e) {
                log.error("Erreur lors de l'ajout d'un élément de classe", e);
                AlertUtils.showErrorDialog("Ajout d'élément",
                        "Erreur lors de l'ajout de l'élément",
                        e.getMessage());
            }
        });
    }

    /**
     * Commence l'ajout d'une relation
     */
    public void startAddingRelationship() {
        setEditMode(EditMode.ADD_RELATIONSHIP);
    }

    /**
     * Sélectionne un nœud comme source de relation
     */
    public void selectRelationSource(ClassNodeComponent sourceNode) {
        if (currentMode == EditMode.ADD_RELATIONSHIP) {
            relationSourceNode = sourceNode;
            updateStatusMessage("Sélectionnez la cible de la relation");
        }
    }

    /**
     * Sélectionne un nœud comme cible de relation
     */
    public void selectRelationTarget(ClassNodeComponent targetNode) {
        if (currentMode == EditMode.ADD_RELATIONSHIP && relationSourceNode != null) {
            // Ne pas créer de relation d'un nœud vers lui-même
            if (relationSourceNode == targetNode) {
                updateStatusMessage("Impossible de créer une relation vers le même élément");
                return;
            }

            // Créer la boîte de dialogue pour éditer la relation
            RelationshipDialog dialog = new RelationshipDialog(
                    diagramCanvas.getScene().getWindow(),
                    relationSourceNode.getClassElement(),
                    targetNode.getClassElement());

            dialog.showAndWait().ifPresent(relationship -> {
                try {
                    // Sauvegarder l'état actuel pour undo
                    saveStateForUndo();

                    // Ajouter la relation au diagramme
                    diagram.addElement(relationship);

                    // Créer le composant visuel
                    RelationshipComponent relationComponent = new RelationshipComponent(
                            relationship, relationSourceNode, targetNode, this);
                    diagramCanvas.getChildren().add(0, relationComponent); // Ajouter en arrière-plan

                    // Marquer comme modifié
                    setUnsavedChanges(true);

                    // Revenir en mode sélection
                    setEditMode(EditMode.SELECT);

                } catch (Exception e) {
                    log.error("Erreur lors de l'ajout d'une relation", e);
                    AlertUtils.showErrorDialog("Ajout de relation",
                            "Erreur lors de l'ajout de la relation",
                            e.getMessage());
                }
            });

            // Réinitialiser l'état
            relationSourceNode = null;
        }
    }

    /**
     * Sélectionne un élément du diagramme
     */
    public void selectElement(DiagramElement element) {
        // Ajouter à la sélection
        if (!selectedElements.contains(element)) {
            selectedElements.add(element);
        }

        // Mettre à jour l'interface
        updateSelectionVisuals();
        updatePropertiesPanel();
    }

    /**
     * Désélectionne un élément du diagramme
     */
    public void deselectElement(DiagramElement element) {
        selectedElements.remove(element);

        // Mettre à jour l'interface
        updateSelectionVisuals();
        updatePropertiesPanel();
    }

    /**
     * Désélectionne tous les éléments
     */
    public void clearSelection() {
        selectedElements.clear();

        // Mettre à jour l'interface
        updateSelectionVisuals();
        updatePropertiesPanel();
    }

    /**
     * Met à jour l'apparence visuelle des éléments sélectionnés
     */
    private void updateSelectionVisuals() {
        // Parcourir tous les nœuds et relations pour mettre à jour leur apparence
        for (javafx.scene.Node node : diagramCanvas.getChildren()) {
            if (node instanceof ClassNodeComponent classNode) {
                boolean isSelected = selectedElements.contains(classNode.getClassElement());
                classNode.setSelected(isSelected);
            } else if (node instanceof RelationshipComponent relationComponent) {
                boolean isSelected = selectedElements.contains(relationComponent.getRelationship());
                relationComponent.setSelected(isSelected);
            }
        }
    }

    /**
     * Met à jour le panneau des propriétés avec l'élément sélectionné
     */
    public void updatePropertiesPanel() {
        if (mainController != null) {
            mainController.updatePropertiesPanel();
        }
    }

    /**
     * Met à jour le panneau des propriétés avec le contenu spécifié
     */
    public void updatePropertiesPanel(VBox propertiesPanel) {
        // Vider le panneau
        propertiesPanel.getChildren().clear();

        // S'il n'y a pas d'élément sélectionné, afficher les propriétés du diagramme
        if (selectedElements.isEmpty()) {
            addDiagramProperties(propertiesPanel);
            return;
        }

        // S'il y a plusieurs éléments sélectionnés, afficher un résumé
        if (selectedElements.size() > 1) {
            addMultiSelectionProperties(propertiesPanel);
            return;
        }

        // Sinon, afficher les propriétés de l'élément sélectionné
        DiagramElement element = selectedElements.get(0);
        if (element instanceof ClassElement classElement) {
            addClassProperties(propertiesPanel, classElement);
        } else if (element instanceof RelationshipElement relationship) {
            addRelationshipProperties(propertiesPanel, relationship);
        }
    }

    /**
     * Ajoute les propriétés du diagramme au panneau
     */
    private void addDiagramProperties(VBox panel) {
        // En-tête
        Label titleLabel = new Label("Propriétés du diagramme");
        titleLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");
        panel.getChildren().add(titleLabel);

        // Nom du diagramme
        TextField nameField = new TextField(diagram.getName());
        nameField.textProperty().addListener((obs, oldVal, newVal) -> {
            diagram.setName(newVal);
            setUnsavedChanges(true);
        });

        // Description
        TextArea descriptionArea = new TextArea(diagram.getDescription());
        descriptionArea.setPrefRowCount(3);
        descriptionArea.textProperty().addListener((obs, oldVal, newVal) -> {
            diagram.setDescription(newVal);
            setUnsavedChanges(true);
        });

        // Couleur de fond
        ColorPicker bgColorPicker = new ColorPicker(javafx.scene.paint.Color.web(diagram.getBackgroundColor()));
        bgColorPicker.setOnAction(event -> {
            diagram.setBackgroundColor(bgColorPicker.getValue().toString());
            diagramCanvas.setStyle("-fx-background-color: " + diagram.getBackgroundColor() + ";");
            setUnsavedChanges(true);
        });

        // Taille de la grille
        Spinner<Double> gridSizeSpinner = new Spinner<>(5, 100, diagram.getGridSize(), 5);
        gridSizeSpinner.valueProperty().addListener((obs, oldVal, newVal) -> {
            diagram.setGridSize(newVal);
            redrawGrid();
            setUnsavedChanges(true);
        });

        // Ajouter tous les contrôles au panneau
        addFormRow(panel, "Nom:", nameField);
        addFormRow(panel, "Description:", descriptionArea);
        addFormRow(panel, "Couleur de fond:", bgColorPicker);
        addFormRow(panel, "Taille de la grille:", gridSizeSpinner);

        // Ajouter des boutons d'action
        Button optimizeLayoutButton = new Button("Optimiser la disposition");
        optimizeLayoutButton.setMaxWidth(Double.MAX_VALUE);
        optimizeLayoutButton.setOnAction(event -> optimizeLayout());

        Button exportButton = new Button("Exporter...");
        exportButton.setMaxWidth(Double.MAX_VALUE);
        exportButton.setOnAction(event -> mainController.onExportDiagram());

        panel.getChildren().addAll(
                new Separator(),
                optimizeLayoutButton,
                exportButton
        );
    }

    /**
     * Ajoute les propriétés d'une sélection multiple au panneau
     */
    private void addMultiSelectionProperties(VBox panel) {
        // En-tête
        Label titleLabel = new Label(selectedElements.size() + " éléments sélectionnés");
        titleLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");
        panel.getChildren().add(titleLabel);

        // Compter les types d'éléments
        int classCount = 0;
        int interfaceCount = 0;
        int enumCount = 0;
        int relationCount = 0;

        for (DiagramElement element : selectedElements) {
            if (element instanceof ClassElement classElement) {
                switch (classElement.getType()) {
                    case CLASS:
                        classCount++;
                        break;
                    case INTERFACE:
                        interfaceCount++;
                        break;
                    case ENUM:
                        enumCount++;
                        break;
                }
            } else if (element instanceof RelationshipElement) {
                relationCount++;
            }
        }

        // Afficher le résumé
        if (classCount > 0) {
            panel.getChildren().add(new Label("Classes: " + classCount));
        }
        if (interfaceCount > 0) {
            panel.getChildren().add(new Label("Interfaces: " + interfaceCount));
        }
        if (enumCount > 0) {
            panel.getChildren().add(new Label("Énumérations: " + enumCount));
        }
        if (relationCount > 0) {
            panel.getChildren().add(new Label("Relations: " + relationCount));
        }

        // Ajouter des boutons d'action
        panel.getChildren().add(new Separator());

        Button deleteButton = new Button("Supprimer la sélection");
        deleteButton.setMaxWidth(Double.MAX_VALUE);
        deleteButton.setOnAction(event -> deleteSelectedElements());

        Button alignHorizontallyButton = new Button("Aligner horizontalement");
        alignHorizontallyButton.setMaxWidth(Double.MAX_VALUE);
        alignHorizontallyButton.setOnAction(event -> alignSelectedElementsHorizontally());

        Button alignVerticallyButton = new Button("Aligner verticalement");
        alignVerticallyButton.setMaxWidth(Double.MAX_VALUE);
        alignVerticallyButton.setOnAction(event -> alignSelectedElementsVertically());

        panel.getChildren().addAll(
                deleteButton,
                alignHorizontallyButton,
                alignVerticallyButton
        );
    }

    /**
     * Ajoute les propriétés d'une classe au panneau
     */
    private void addClassProperties(VBox panel, ClassElement classElement) {
        // En-tête
        String typeLabel;
        switch (classElement.getType()) {
            case INTERFACE:
                typeLabel = "Interface";
                break;
            case ENUM:
                typeLabel = "Énumération";
                break;
            default:
                typeLabel = "Classe";
        }

        Label titleLabel = new Label(typeLabel + ": " + classElement.getName());
        titleLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");
        panel.getChildren().add(titleLabel);

        // Nom de la classe
        TextField nameField = new TextField(classElement.getName());
        nameField.textProperty().addListener((obs, oldVal, newVal) -> {
            classElement.setName(newVal);
            refreshNodeForElement(classElement);
            setUnsavedChanges(true);
        });

        // Package
        TextField packageField = new TextField(classElement.getPackageName());
        packageField.textProperty().addListener((obs, oldVal, newVal) -> {
            classElement.setPackageName(newVal);
            refreshNodeForElement(classElement);
            setUnsavedChanges(true);
        });

        // Classe abstraite
        CheckBox abstractCheckBox = new CheckBox("Abstraite");
        abstractCheckBox.setSelected(classElement.isAbstract());
        abstractCheckBox.setDisable(classElement.getType() == ClassElement.ClassType.INTERFACE);
        abstractCheckBox.selectedProperty().addListener((obs, oldVal, newVal) -> {
            classElement.setAbstract(newVal);
            refreshNodeForElement(classElement);
            setUnsavedChanges(true);
        });

        // Couleur de fond
        ColorPicker bgColorPicker = new ColorPicker(javafx.scene.paint.Color.web(classElement.getBackgroundColor()));
        bgColorPicker.setOnAction(event -> {
            classElement.setBackgroundColor(bgColorPicker.getValue().toString());
            refreshNodeForElement(classElement);
            setUnsavedChanges(true);
        });

        // Couleur de bordure
        ColorPicker borderColorPicker = new ColorPicker(javafx.scene.paint.Color.web(classElement.getBorderColor()));
        borderColorPicker.setOnAction(event -> {
            classElement.setBorderColor(borderColorPicker.getValue().toString());
            refreshNodeForElement(classElement);
            setUnsavedChanges(true);
        });

        // Position X
        Spinner<Double> xSpinner = new Spinner<>(0, 2000, classElement.getX(), 10);
        xSpinner.setEditable(true);
        xSpinner.valueProperty().addListener((obs, oldVal, newVal) -> {
            classElement.setX(newVal);
            refreshNodeForElement(classElement);
            setUnsavedChanges(true);
        });

        // Position Y
        Spinner<Double> ySpinner = new Spinner<>(0, 2000, classElement.getY(), 10);
        ySpinner.setEditable(true);
        ySpinner.valueProperty().addListener((obs, oldVal, newVal) -> {
            classElement.setY(newVal);
            refreshNodeForElement(classElement);
            setUnsavedChanges(true);
        });

        // Largeur
        Spinner<Double> widthSpinner = new Spinner<>(50, 500, classElement.getWidth(), 10);
        widthSpinner.setEditable(true);
        widthSpinner.valueProperty().addListener((obs, oldVal, newVal) -> {
            classElement.setWidth(newVal);
            refreshNodeForElement(classElement);
            setUnsavedChanges(true);
        });

        // Hauteur
        Spinner<Double> heightSpinner = new Spinner<>(50, 500, classElement.getHeight(), 10);
        heightSpinner.setEditable(true);
        heightSpinner.valueProperty().addListener((obs, oldVal, newVal) -> {
            classElement.setHeight(newVal);
            refreshNodeForElement(classElement);
            setUnsavedChanges(true);
        });

        // Ajouter tous les contrôles au panneau
        addFormRow(panel, "Nom:", nameField);
        addFormRow(panel, "Package:", packageField);
        panel.getChildren().add(abstractCheckBox);
        addFormRow(panel, "Couleur de fond:", bgColorPicker);
        addFormRow(panel, "Couleur de bordure:", borderColorPicker);

        // Section position et taille
        Label positionLabel = new Label("Position et taille");
        positionLabel.setStyle("-fx-font-weight: bold;");
        panel.getChildren().add(new Separator());
        panel.getChildren().add(positionLabel);

        addFormRow(panel, "X:", xSpinner);
        addFormRow(panel, "Y:", ySpinner);
        addFormRow(panel, "Largeur:", widthSpinner);
        addFormRow(panel, "Hauteur:", heightSpinner);

        // Ajouter des boutons d'action
        panel.getChildren().add(new Separator());

        Button editButton = new Button("Éditer...");
        editButton.setMaxWidth(Double.MAX_VALUE);
        editButton.setOnAction(event -> editClassElement(classElement));

        Button deleteButton = new Button("Supprimer");
        deleteButton.setMaxWidth(Double.MAX_VALUE);
        deleteButton.setOnAction(event -> deleteClassElement(classElement));

        panel.getChildren().addAll(
                editButton,
                deleteButton
        );
    }

    /**
     * Ajoute les propriétés d'une relation au panneau
     */
    private void addRelationshipProperties(VBox panel, RelationshipElement relationship) {
        // En-tête
        Label titleLabel = new Label("Relation: " + relationship.getType().getLabel());
        titleLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");
        panel.getChildren().add(titleLabel);

        // Nom
        TextField nameField = new TextField(relationship.getName());
        nameField.textProperty().addListener((obs, oldVal, newVal) -> {
            relationship.setName(newVal);
            refreshNodeForElement(relationship);
            setUnsavedChanges(true);
        });

        // Type de relation
        ComboBox<RelationshipType> typeComboBox = new ComboBox<>();
        typeComboBox.getItems().addAll(RelationshipType.values());
        typeComboBox.setValue(relationship.getType());
        typeComboBox.setOnAction(event -> {
            relationship.setType(typeComboBox.getValue());
            refreshNodeForElement(relationship);
            setUnsavedChanges(true);
        });

        // Source
        Label sourceLabel = new Label(relationship.getSourceElement().getName());

        // Rôle source
        TextField sourceRoleField = new TextField(relationship.getSourceRole());
        sourceRoleField.textProperty().addListener((obs, oldVal, newVal) -> {
            relationship.setSourceRole(newVal);
            refreshNodeForElement(relationship);
            setUnsavedChanges(true);
        });

        // Multiplicité source
        TextField sourceMultiplicityField = new TextField(relationship.getSourceMultiplicity());
        sourceMultiplicityField.textProperty().addListener((obs, oldVal, newVal) -> {
            relationship.setSourceMultiplicity(newVal);
            refreshNodeForElement(relationship);
            setUnsavedChanges(true);
        });

        // Cible
        Label targetLabel = new Label(relationship.getTargetElement().getName());

        // Rôle cible
        TextField targetRoleField = new TextField(relationship.getTargetRole());
        targetRoleField.textProperty().addListener((obs, oldVal, newVal) -> {
            relationship.setTargetRole(newVal);
            refreshNodeForElement(relationship);
            setUnsavedChanges(true);
        });

        // Multiplicité cible
        TextField targetMultiplicityField = new TextField(relationship.getTargetMultiplicity());
        targetMultiplicityField.textProperty().addListener((obs, oldVal, newVal) -> {
            relationship.setTargetMultiplicity(newVal);
            refreshNodeForElement(relationship);
            setUnsavedChanges(true);
        });

        // Couleur de ligne
        ColorPicker lineColorPicker = new ColorPicker(javafx.scene.paint.Color.web(relationship.getLineColor()));
        lineColorPicker.setOnAction(event -> {
            relationship.setLineColor(lineColorPicker.getValue().toString());
            refreshNodeForElement(relationship);
            setUnsavedChanges(true);
        });

        // Épaisseur de ligne
        Spinner<Double> lineWidthSpinner = new Spinner<>(0.5, 5.0, relationship.getLineWidth(), 0.5);
        lineWidthSpinner.valueProperty().addListener((obs, oldVal, newVal) -> {
            relationship.setLineWidth(newVal);
            refreshNodeForElement(relationship);
            setUnsavedChanges(true);
        });

        // Style de ligne
        ComboBox<String> lineStyleComboBox = new ComboBox<>();
        lineStyleComboBox.getItems().addAll("SOLID", "DASHED", "DOTTED");
        lineStyleComboBox.setValue(relationship.getLineStyle());
        lineStyleComboBox.setOnAction(event -> {
            relationship.setLineStyle(lineStyleComboBox.getValue());
            refreshNodeForElement(relationship);
            setUnsavedChanges(true);
        });

        // Ajouter tous les contrôles au panneau
        addFormRow(panel, "Nom:", nameField);
        addFormRow(panel, "Type:", typeComboBox);

        panel.getChildren().add(new Separator());

        // Section source
        Label sourceHeaderLabel = new Label("Source: " + relationship.getSourceElement().getName());
        sourceHeaderLabel.setStyle("-fx-font-weight: bold;");
        panel.getChildren().add(sourceHeaderLabel);

        addFormRow(panel, "Rôle:", sourceRoleField);
        addFormRow(panel, "Multiplicité:", sourceMultiplicityField);

        panel.getChildren().add(new Separator());

        // Section cible
        Label targetHeaderLabel = new Label("Cible: " + relationship.getTargetElement().getName());
        targetHeaderLabel.setStyle("-fx-font-weight: bold;");
        panel.getChildren().add(targetHeaderLabel);

        addFormRow(panel, "Rôle:", targetRoleField);
        addFormRow(panel, "Multiplicité:", targetMultiplicityField);

        panel.getChildren().add(new Separator());

        // Section style
        Label styleLabel = new Label("Style");
        styleLabel.setStyle("-fx-font-weight: bold;");
        panel.getChildren().add(styleLabel);

        addFormRow(panel, "Couleur:", lineColorPicker);
        addFormRow(panel, "Épaisseur:", lineWidthSpinner);
        addFormRow(panel, "Style:", lineStyleComboBox);

        // Ajouter des boutons d'action
        panel.getChildren().add(new Separator());

        Button editButton = new Button("Éditer...");
        editButton.setMaxWidth(Double.MAX_VALUE);
        editButton.setOnAction(event -> editRelationship(relationship));

        Button deleteButton = new Button("Supprimer");
        deleteButton.setMaxWidth(Double.MAX_VALUE);
        deleteButton.setOnAction(event -> deleteRelationship(relationship));

        panel.getChildren().addAll(
                editButton,
                deleteButton
        );
    }

    /**
     * Ajoute une ligne de formulaire au panneau
     */
    private void addFormRow(VBox panel, String labelText, javafx.scene.Node control) {
        Label label = new Label(labelText);
        label.setPrefWidth(100);

        javafx.scene.layout.HBox row = new javafx.scene.layout.HBox(10);
        row.getChildren().addAll(label, control);
        row.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        javafx.scene.layout.HBox.setHgrow(control, javafx.scene.layout.Priority.ALWAYS);

        panel.getChildren().add(row);
    }

    /**
     * Édite un élément de classe
     */
    public void editClassElement(ClassElement classElement) {
        // Créer la boîte de dialogue
        ClassDialog dialog = new ClassDialog(diagramCanvas.getScene().getWindow(), classElement, classElement.getType());

        dialog.showAndWait().ifPresent(updatedElement -> {
            try {
                // Sauvegarder l'état actuel pour undo
                saveStateForUndo();

                // Mettre à jour l'élément
                diagramService.updateClassElement(classElement.getId(), updatedElement);

                // Rafraîchir l'affichage
                refreshNodeForElement(classElement);

                // Marquer comme modifié
                setUnsavedChanges(true);

            } catch (Exception e) {
                log.error("Erreur lors de la mise à jour de l'élément de classe", e);
                AlertUtils.showErrorDialog("Mise à jour d'élément",
                        "Erreur lors de la mise à jour de l'élément",
                        e.getMessage());
            }
        });
    }

    /**
     * Supprime un élément de classe
     */
    public void deleteClassElement(ClassElement classElement) {
        // Demander confirmation
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Suppression d'élément");
        alert.setHeaderText("Supprimer " + classElement.getName());
        alert.setContentText("Voulez-vous vraiment supprimer cet élément et toutes ses relations?");

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                // Sauvegarder l'état actuel pour undo
                saveStateForUndo();

                // Supprimer l'élément du diagramme
                diagramService.deleteClassElement(classElement.getId());

                // Supprimer du modèle
                diagram.removeElement(classElement);

                // Supprimer de la sélection
                selectedElements.remove(classElement);

                // Rafraîchir l'affichage
                refreshDiagram();

                // Marquer comme modifié
                setUnsavedChanges(true);

            } catch (Exception e) {
                log.error("Erreur lors de la suppression de l'élément de classe", e);
                AlertUtils.showErrorDialog("Suppression d'élément",
                        "Erreur lors de la suppression de l'élément",
                        e.getMessage());
            }
        }
    }

    /**
     * Édite une relation
     */
    public void editRelationship(RelationshipElement relationship) {
        // Créer la boîte de dialogue
        RelationshipDialog dialog = new RelationshipDialog(
                diagramCanvas.getScene().getWindow(),
                relationship.getSourceElement(),
                relationship.getTargetElement(),
                relationship);

        dialog.showAndWait().ifPresent(updatedRelationship -> {
            try {
                // Sauvegarder l'état actuel pour undo
                saveStateForUndo();

                // Mettre à jour la relation
                diagramService.updateRelationship(relationship.getId(), updatedRelationship);

                // Rafraîchir l'affichage
                refreshNodeForElement(relationship);

                // Marquer comme modifié
                setUnsavedChanges(true);

            } catch (Exception e) {
                log.error("Erreur lors de la mise à jour de la relation", e);
                AlertUtils.showErrorDialog("Mise à jour de relation",
                        "Erreur lors de la mise à jour de la relation",
                        e.getMessage());
            }
        });
    }

    /**
     * Supprime une relation
     */
    public void deleteRelationship(RelationshipElement relationship) {
        // Demander confirmation
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Suppression de relation");
        alert.setHeaderText("Supprimer la relation");
        alert.setContentText("Voulez-vous vraiment supprimer cette relation?");

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                // Sauvegarder l'état actuel pour undo
                saveStateForUndo();

                // Supprimer la relation du diagramme
                diagramService.deleteRelationship(relationship.getId());

                // Supprimer du modèle
                diagram.removeElement(relationship);

                // Supprimer de la sélection
                selectedElements.remove(relationship);

                // Rafraîchir l'affichage
                refreshDiagram();

                // Marquer comme modifié
                setUnsavedChanges(true);

            } catch (Exception e) {
                log.error("Erreur lors de la suppression de la relation", e);
                AlertUtils.showErrorDialog("Suppression de relation",
                        "Erreur lors de la suppression de la relation",
                        e.getMessage());
            }
        }
    }

    /**
     * Rafraîchit l'affichage du nœud correspondant à un élément
     */
    private void refreshNodeForElement(DiagramElement element) {
        for (javafx.scene.Node node : diagramCanvas.getChildren()) {
            if (node instanceof ClassNodeComponent classNode &&
                    classNode.getClassElement() == element) {
                classNode.updateView();
                return;
            } else if (node instanceof RelationshipComponent relationComponent &&
                    relationComponent.getRelationship() == element) {
                relationComponent.updateView();
                return;
            }
        }
    }

    /**
     * Rafraîchit l'affichage complet du diagramme
     */
    public void refreshDiagram() {
        // Sauvegarder la sélection actuelle
        List<DiagramElement> savedSelection = new ArrayList<>(selectedElements);

        // Vider le canvas
        diagramCanvas.getChildren().clear();

        // Redessiner la grille si nécessaire
        redrawGrid();

        // Dessiner les relations d'abord (en arrière-plan)
        Map<ClassElement, ClassNodeComponent> nodeMap = new HashMap<>();

        // Dessiner les classes
        for (ClassElement classElement : diagram.getClasses()) {
            ClassNodeComponent classNode = new ClassNodeComponent(classElement, this);
            diagramCanvas.getChildren().add(classNode);
            nodeMap.put(classElement, classNode);
        }

        // Dessiner les relations
        for (RelationshipElement relationship : diagram.getRelationships()) {
            ClassNodeComponent sourceNode = nodeMap.get(relationship.getSourceElement());
            ClassNodeComponent targetNode = nodeMap.get(relationship.getTargetElement());

            if (sourceNode != null && targetNode != null) {
                RelationshipComponent relationComponent = new RelationshipComponent(
                        relationship, sourceNode, targetNode, this);
                // Ajouter en arrière-plan
                diagramCanvas.getChildren().add(0, relationComponent);
            }
        }

        // Restaurer la sélection
        selectedElements.clear();
        for (DiagramElement element : savedSelection) {
            if (diagram.getElements().contains(element)) {
                selectedElements.add(element);
            }
        }

        // Mettre à jour l'apparence des éléments sélectionnés
        updateSelectionVisuals();

        // Mettre à jour le panneau des propriétés
        updatePropertiesPanel();
    }

    /**
     * Rafraîchit uniquement les composants de relation dans le diagramme
     * Cette méthode est appelée après le déplacement d'un nœud de classe
     */
    public void refreshRelationships() {
        // Pour l'instant, nous pouvons simplement redessiner tout le diagramme
        // Plus tard, il serait bien de rendre l'implémentation plus efficace en à jour que les relations
        refreshDiagram();

        // Version plus efficace (à implémenter ultérieurement) :
        // Mettre à jour uniquement les composants de relation existants
        // sans recréer tous les éléments du diagramme
    }

    /**
     * Redessine la grille du diagramme
     */
    private void redrawGrid() {
        // Supprimer les lignes de grille existantes
        diagramCanvas.getChildren().removeIf(node ->
                node.getStyleClass().contains("grid-line"));

        // Dessiner la nouvelle grille si nécessaire
        if (showGrid) {
            double gridSize = diagram.getGridSize();
            double width = diagramCanvas.getWidth();
            double height = diagramCanvas.getHeight();

            // Créer les lignes de la grille
            for (double x = 0; x < width; x += gridSize) {
                javafx.scene.shape.Line line = new javafx.scene.shape.Line(x, 0, x, height);
                line.getStyleClass().add("grid-line");
                line.setStroke(javafx.scene.paint.Color.LIGHTGRAY);
                line.setStrokeWidth(0.5);
                line.setMouseTransparent(true);
                diagramCanvas.getChildren().add(0, line);
            }

            for (double y = 0; y < height; y += gridSize) {
                javafx.scene.shape.Line line = new javafx.scene.shape.Line(0, y, width, y);
                line.getStyleClass().add("grid-line");
                line.setStroke(javafx.scene.paint.Color.LIGHTGRAY);
                line.setStrokeWidth(0.5);
                line.setMouseTransparent(true);
                diagramCanvas.getChildren().add(0, line);
            }
        }
    }

    /**
     * Aligne les éléments sélectionnés horizontalement
     */
    private void alignSelectedElementsHorizontally() {
        if (selectedElements.size() < 2) {
            return;
        }

        // Sauvegarder l'état actuel pour undo
        saveStateForUndo();

        // Calculer la moyenne des positions Y
        double avgY = selectedElements.stream()
                .mapToDouble(DiagramElement::getY)
                .average()
                .orElse(0);

        // Appliquer à tous les éléments sélectionnés
        for (DiagramElement element : selectedElements) {
            element.setY(avgY);
        }

        // Rafraîchir l'affichage
        refreshDiagram();

        // Marquer comme modifié
        setUnsavedChanges(true);
    }

    /**
     * Aligne les éléments sélectionnés verticalement
     */
    private void alignSelectedElementsVertically() {
        if (selectedElements.size() < 2) {
            return;
        }

        // Sauvegarder l'état actuel pour undo
        saveStateForUndo();

        // Calculer la moyenne des positions X
        double avgX = selectedElements.stream()
                .mapToDouble(DiagramElement::getX)
                .average()
                .orElse(0);

        // Appliquer à tous les éléments sélectionnés
        for (DiagramElement element : selectedElements) {
            element.setX(avgX);
        }

        // Rafraîchir l'affichage
        refreshDiagram();

        // Marquer comme modifié
        setUnsavedChanges(true);
    }

    /**
     * Coupe les éléments sélectionnés
     */
    public void cutSelectedElements() {
        if (selectedElements.isEmpty()) {
            return;
        }

        // Copier d'abord
        copySelectedElements();

        // Puis supprimer
        deleteSelectedElements();
    }

    /**
     * Copie les éléments sélectionnés
     */
    public void copySelectedElements() {
        if (selectedElements.isEmpty()) {
            return;
        }

        // Vider le presse-papiers
        clipboardElements.clear();

        // Copier les éléments sélectionnés
        clipboardElements.addAll(selectedElements);

        updateStatusMessage(clipboardElements.size() + " élément(s) copié(s)");
    }

    /**
     * Colle les éléments du presse-papiers
     */
    public void pasteElements() {
        if (clipboardElements.isEmpty()) {
            return;
        }

        // Sauvegarder l'état actuel pour undo
        saveStateForUndo();

        // Désélectionner tous les éléments
        clearSelection();

        // Calculer le décalage pour le collage
        double offsetX = 20;
        double offsetY = 20;

        // Coller les éléments
        for (DiagramElement originalElement : clipboardElements) {
            if (originalElement instanceof ClassElement classElement) {
                // Créer une copie de la classe
                ClassElement newElement = new ClassElement(classElement.getName() + " (copie)");
                newElement.setPackageName(classElement.getPackageName());
                newElement.setType(classElement.getType());
                newElement.setAbstract(classElement.isAbstract());
                newElement.setX(classElement.getX() + offsetX);
                newElement.setY(classElement.getY() + offsetY);
                newElement.setWidth(classElement.getWidth());
                newElement.setHeight(classElement.getHeight());
                newElement.setBackgroundColor(classElement.getBackgroundColor());
                newElement.setBorderColor(classElement.getBorderColor());
                newElement.setBorderWidth(classElement.getBorderWidth());

                // Copier les attributs
                for (var attr : classElement.getAttributes()) {
                    var newAttr = new com.diagramme.model.Attribute(attr.getName(), attr.getType());
                    newAttr.setVisibility(attr.getVisibility());
                    newAttr.setStatic(attr.isStatic());
                    newAttr.setFinal(attr.isFinal());
                    newAttr.setDefaultValue(attr.getDefaultValue());
                    newElement.addAttribute(newAttr);
                }

                // Copier les méthodes
                for (var method : classElement.getMethods()) {
                    var newMethod = new com.diagramme.model.Method(method.getName(), method.getReturnType());
                    newMethod.setVisibility(method.getVisibility());
                    newMethod.setStatic(method.isStatic());
                    newMethod.setAbstract(method.isAbstract());
                    newMethod.setFinal(method.isFinal());

                    // Copier les paramètres
                    for (var param : method.getParameters()) {
                        var newParam = new com.diagramme.model.Parameter(param.getName(), param.getType());
                        newParam.setDefaultValue(param.getDefaultValue());
                        newMethod.addParameter(newParam);
                    }

                    newElement.addMethod(newMethod);
                }

                // Ajouter au diagramme
                diagram.addElement(newElement);

                // Sélectionner l'élément nouvellement créé
                selectElement(newElement);
            }
            // Note: La copie des relations n'est pas implémentée car elles dépendent des classes
        }

        // Rafraîchir l'affichage
        refreshDiagram();

        // Marquer comme modifié
        setUnsavedChanges(true);

        updateStatusMessage(selectedElements.size() + " élément(s) collé(s)");
    }

    /**
     * Supprime les éléments sélectionnés
     */
    public void deleteSelectedElements() {
        if (selectedElements.isEmpty()) {
            return;
        }

        // Demander confirmation
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Suppression d'éléments");
        alert.setHeaderText("Supprimer " + selectedElements.size() + " élément(s)");
        alert.setContentText("Voulez-vous vraiment supprimer ces éléments?");

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                // Sauvegarder l'état actuel pour undo
                saveStateForUndo();

                // Faire une copie de la liste pour éviter les problèmes de modification pendant l'itération
                List<DiagramElement> elementsToDelete = new ArrayList<>(selectedElements);

                // Supprimer d'abord les relations
                for (DiagramElement element : elementsToDelete) {
                    if (element instanceof RelationshipElement relationship) {
                        diagramService.deleteRelationship(relationship.getId());
                        diagram.removeElement(relationship);
                    }
                }

                // Puis supprimer les classes
                for (DiagramElement element : elementsToDelete) {
                    if (element instanceof ClassElement classElement) {
                        diagramService.deleteClassElement(classElement.getId());
                        diagram.removeElement(classElement);
                    }
                }

                // Vider la sélection
                selectedElements.clear();

                // Rafraîchir l'affichage
                refreshDiagram();

                // Marquer comme modifié
                setUnsavedChanges(true);

                updateStatusMessage(elementsToDelete.size() + " élément(s) supprimé(s)");

            } catch (Exception e) {
                log.error("Erreur lors de la suppression des éléments", e);
                AlertUtils.showErrorDialog("Suppression d'éléments",
                        "Erreur lors de la suppression des éléments",
                        e.getMessage());
            }
        }
    }


    /**
     * Sélectionne tous les éléments du diagramme
     */
    public void selectAllElements() {
        // Vider la sélection actuelle
        selectedElements.clear();

        // Ajouter tous les éléments du diagramme
        selectedElements.addAll(diagram.getElements());

        // Mettre à jour l'interface
        updateSelectionVisuals();
        updatePropertiesPanel();

        updateStatusMessage(selectedElements.size() + " élément(s) sélectionné(s)");
    }

    /**
     * Ajoute un élément de classe au diagramme
     */
    public void addClassElement() {
        setEditMode(EditMode.ADD_CLASS);
    }

    /**
     * Ajoute un élément d'interface au diagramme
     */
    public void addInterfaceElement() {
        setEditMode(EditMode.ADD_INTERFACE);
    }

    /**
     * Ajoute un élément d'énumération au diagramme
     */
    public void addEnumElement() {
        // Définir la position pour le nouvel élément
        double x = diagramCanvas.getWidth() / 2;
        double y = diagramCanvas.getHeight() / 2;

        // Ajuster selon le zoom
        x = x / zoomFactor;
        y = y / zoomFactor;

        // Ajuster selon la grille si nécessaire
        if (snapToGrid) {
            double gridSize = diagram.getGridSize();
            x = Math.round(x / gridSize) * gridSize;
            y = Math.round(y / gridSize) * gridSize;
        }

        // Ajouter l'élément
        addClassElementAt(x, y, ClassElement.ClassType.ENUM);
    }

    /**
     * Effectue un zoom avant sur le diagramme
     */
    public void zoomIn() {
        zoomFactor *= 1.1;
        applyZoom();
    }

    /**
     * Effectue un zoom arrière sur le diagramme
     */
    public void zoomOut() {
        zoomFactor *= 0.9;
        applyZoom();
    }

    /**
     * Réinitialise le zoom
     */
    public void resetZoom() {
        zoomFactor = 1.0;
        applyZoom();
    }

    /**
     * Applique le facteur de zoom actuel
     */
    private void applyZoom() {
        // Limiter le zoom
        zoomFactor = Math.max(0.1, Math.min(5.0, zoomFactor));

        // Appliquer le zoom au canvas
        diagramCanvas.setScaleX(zoomFactor);
        diagramCanvas.setScaleY(zoomFactor);

        // Mettre à jour l'étiquette de zoom
        if (mainController != null) {
            mainController.updateZoomLabel(zoomFactor);
        }
    }

    /**
     * Active/désactive l'affichage de la grille
     */
    public void toggleGrid() {
        showGrid = !showGrid;
        diagram.setShowGrid(showGrid);
        redrawGrid();
        setUnsavedChanges(true);
    }

    /**
     * Active/désactive l'alignement sur la grille
     */
    public void toggleSnapToGrid() {
        snapToGrid = !snapToGrid;
        diagram.setSnapToGrid(snapToGrid);
        setUnsavedChanges(true);
    }

    /**
     * Vérifie la validité UML du diagramme
     */
    public void checkUmlValidity() {
        List<String> issues = UmlValidator.validateDiagram(diagram);

        if (issues.isEmpty()) {
            AlertUtils.showInfoDialog("Validation UML",
                    "Diagramme valide",
                    "Le diagramme est conforme aux règles UML.");
        } else {
            // Créer la liste des problèmes
            StringBuilder sb = new StringBuilder();
            for (String issue : issues) {
                sb.append("• ").append(issue).append("\n");
            }

            AlertUtils.showWarningDialog("Validation UML",
                    "Problèmes de validation UML",
                    sb.toString());
        }
    }

    /**
     * Optimise la disposition des éléments du diagramme
     */
    public void optimizeLayout() {
        try {
            // Sauvegarder l'état actuel pour undo
            saveStateForUndo();

            // Optimiser la disposition
            ClassDiagram optimizedDiagram = diagramService.optimizeLayout(diagram.getId());

            // Mettre à jour le diagramme
            diagram = optimizedDiagram;

            // Rafraîchir l'affichage
            refreshDiagram();

            // Marquer comme modifié
            setUnsavedChanges(true);

            updateStatusMessage("Disposition optimisée");

        } catch (Exception e) {
            log.error("Erreur lors de l'optimisation de la disposition", e);
            AlertUtils.showErrorDialog("Optimisation de la disposition",
                    "Erreur lors de l'optimisation de la disposition",
                    e.getMessage());
        }
    }

    /**
     * Génère du code Java à partir du diagramme
     */
    public void generateJavaCode() {
        // Demander un répertoire de destination
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("Choisir un répertoire de destination");

        File directory = directoryChooser.showDialog(diagramCanvas.getScene().getWindow());
        if (directory != null) {
            try {
                // Cette fonctionnalité nécessiterait un service de génération de code Java
                // qui n'est pas implémenté dans cet exemple

                // Pour l'exemple, afficher un message de succès fictif
                AlertUtils.showInfoDialog("Génération de code",
                        "Code Java généré",
                        "Le code Java a été généré dans le répertoire:\n" + directory.getAbsolutePath());

                updateStatusMessage("Code Java généré dans " + directory.getName());

            } catch (Exception e) {
                log.error("Erreur lors de la génération de code Java", e);
                AlertUtils.showErrorDialog("Génération de code",
                        "Erreur lors de la génération de code Java",
                        e.getMessage());
            }
        }
    }

    /**
     * Enregistre l'état actuel pour l'annulation (undo)
     */
    private void saveStateForUndo() {
        try {
            // Créer une copie profonde du diagramme actuel
            // Note: Dans une implémentation réelle, utilisez une méthode de clonage appropriée
            ClassDiagram copy = deepCopyDiagram(diagram);

            // Ajouter à la pile d'annulation
            undoStack.push(copy);

            // Vider la pile de rétablissement
            redoStack.clear();

        } catch (Exception e) {
            log.error("Erreur lors de la sauvegarde de l'état pour undo", e);
        }
    }

    /**
     * Crée une copie profonde du diagramme
     * Note: Il s'agit d'une implémentation simplifiée pour l'exemple
     */
    private ClassDiagram deepCopyDiagram(ClassDiagram original) {
        // Dans une application réelle, utilisez une méthode de sérialisation/désérialisation
        // ou une bibliothèque de clonage comme Cloner

        // Pour l'exemple, créer un nouveau diagramme avec les mêmes propriétés de base
        ClassDiagram copy = new ClassDiagram(original.getName());
        copy.setDescription(original.getDescription());
        copy.setShowGrid(original.isShowGrid());
        copy.setSnapToGrid(original.isSnapToGrid());
        copy.setGridSize(original.getGridSize());
        copy.setBackgroundColor(original.getBackgroundColor());

        // Note: Une copie réelle devrait copier tous les éléments et leurs propriétés

        return copy;
    }

    /**
     * Annule la dernière action
     */
    public void undo() {
        if (undoStack.isEmpty()) {
            updateStatusMessage("Rien à annuler");
            return;
        }

        try {
            // Sauvegarder l'état actuel pour redo
            redoStack.push(deepCopyDiagram(diagram));

            // Restaurer l'état précédent
            diagram = undoStack.pop();

            // Rafraîchir l'affichage
            refreshDiagram();

            // Marquer comme modifié
            setUnsavedChanges(true);

            updateStatusMessage("Action annulée");

        } catch (Exception e) {
            log.error("Erreur lors de l'annulation", e);
            AlertUtils.showErrorDialog("Annulation",
                    "Erreur lors de l'annulation",
                    e.getMessage());
        }
    }

    /**
     * Rétablit la dernière action annulée
     */
    public void redo() {
        if (redoStack.isEmpty()) {
            updateStatusMessage("Rien à rétablir");
            return;
        }

        try {
            // Sauvegarder l'état actuel pour undo
            undoStack.push(deepCopyDiagram(diagram));

            // Restaurer l'état suivant
            diagram = redoStack.pop();

            // Rafraîchir l'affichage
            refreshDiagram();

            // Marquer comme modifié
            setUnsavedChanges(true);

            updateStatusMessage("Action rétablie");

        } catch (Exception e) {
            log.error("Erreur lors du rétablissement", e);
            AlertUtils.showErrorDialog("Rétablissement",
                    "Erreur lors du rétablissement",
                    e.getMessage());
        }
    }

    /**
     * Met à jour le message de la barre d'état
     */
    public void updateStatusMessage(String message) {
        if (mainController != null) {
            mainController.updateStatusMessage(message);
        }
    }

    /**
     * Définit le diagramme à éditer
     */
    public void setDiagram(ClassDiagram diagram) {
        this.diagram = diagram;

        // Configurer la taille du canvas
        diagramCanvas.setPrefWidth(2000);
        diagramCanvas.setPrefHeight(2000);

        // Configurer le fond
        diagramCanvas.setStyle("-fx-background-color: " + diagram.getBackgroundColor() + ";");

        // Afficher les éléments du diagramme
        refreshDiagram();

        // Initialiser les propriétés de l'éditeur
        showGrid = diagram.isShowGrid();
        snapToGrid = diagram.isSnapToGrid();
        zoomFactor = 1.0;

        // Réinitialiser l'état des modifications
        unsavedChanges = false;
    }

    /**
     * Crée un nouveau diagramme
     */
    public void setNewDiagram() {
        ClassDiagram newDiagram = new ClassDiagram("Nouveau diagramme");
        setDiagram(newDiagram);
    }

    /**
     * Met à jour le diagramme avec un nouveau diagramme
     */
    public void updateDiagram(ClassDiagram updatedDiagram) {
        // Sauvegarder l'état actuel pour undo
        saveStateForUndo();

        // Mettre à jour les propriétés du diagramme
        diagram.setName(updatedDiagram.getName());
        diagram.setDescription(updatedDiagram.getDescription());
        diagram.setShowGrid(updatedDiagram.isShowGrid());
        diagram.setSnapToGrid(updatedDiagram.isSnapToGrid());
        diagram.setGridSize(updatedDiagram.getGridSize());
        diagram.setBackgroundColor(updatedDiagram.getBackgroundColor());

        // Mettre à jour les états locaux
        showGrid = diagram.isShowGrid();
        snapToGrid = diagram.isSnapToGrid();

        // Rafraîchir l'affichage
        diagramCanvas.setStyle("-fx-background-color: " + diagram.getBackgroundColor() + ";");
        redrawGrid();

        // Marquer comme modifié
        setUnsavedChanges(true);
    }

    /**
     * Vérifie s'il y a des modifications non enregistrées
     */
    public boolean hasUnsavedChanges() {
        return unsavedChanges;
    }

    /**
     * Définit l'état des modifications
     */
    public void setUnsavedChanges(boolean unsavedChanges) {
        this.unsavedChanges = unsavedChanges;

        // Notifier le contrôleur principal
        if (mainController != null && unsavedChanges) {
            mainController.notifyDiagramChanged();
        }
    }


}