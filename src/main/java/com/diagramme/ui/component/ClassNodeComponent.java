package com.diagramme.ui.component;

import com.diagramme.model.Attribute;
import com.diagramme.model.ClassElement;
import com.diagramme.model.Method;
import com.diagramme.model.enums.Visibility;
import com.diagramme.ui.DiagramEditorController;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.Separator;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import lombok.Getter;

/**
 * Composant visuel représentant une classe dans le diagramme
 */
public class ClassNodeComponent extends StackPane {

    @Getter
    private final ClassElement classElement;
    private final DiagramEditorController editorController;

    private final VBox classContent;
    private final Label titleLabel;
    private final VBox attributesBox;
    private final VBox methodsBox;
    private final Rectangle selectionRectangle;

    private double startDragX;
    private double startDragY;
    private boolean isDragging = false;

    public ClassNodeComponent(ClassElement classElement, DiagramEditorController editorController) {
        this.classElement = classElement;
        this.editorController = editorController;

        // Configuration visuelle
        setPrefSize(classElement.getWidth(), classElement.getHeight());
        setLayoutX(classElement.getX());
        setLayoutY(classElement.getY());

        // Rectangle de sélection
        selectionRectangle = new Rectangle();
        selectionRectangle.setStroke(Color.BLUE);
        selectionRectangle.setStrokeWidth(2);
        selectionRectangle.setFill(Color.TRANSPARENT);
        selectionRectangle.setVisible(false);
        selectionRectangle.setMouseTransparent(true);

        // Rectangle de fond
        Rectangle background = new Rectangle();
        background.setFill(Color.web(classElement.getBackgroundColor()));
        background.setStroke(Color.web(classElement.getBorderColor()));
        background.setStrokeWidth(classElement.getBorderWidth());

        // Contenu de la classe
        classContent = new VBox();
        classContent.setAlignment(Pos.TOP_CENTER);
        classContent.setSpacing(5);
        classContent.setPadding(new Insets(0, 0, 5, 0));

        // Titre de la classe
        titleLabel = new Label();
        titleLabel.getStyleClass().add("class-title");
        titleLabel.setAlignment(Pos.CENTER);
        titleLabel.setPadding(new Insets(5));
        titleLabel.setMaxWidth(Double.MAX_VALUE);

        // Section des attributs
        attributesBox = new VBox();
        attributesBox.setSpacing(2);
        attributesBox.setPadding(new Insets(5, 10, 5, 10));

        // Section des méthodes
        methodsBox = new VBox();
        methodsBox.setSpacing(2);
        methodsBox.setPadding(new Insets(5, 10, 5, 10));

        // Assembler le composant
        classContent.getChildren().addAll(
                titleLabel,
                new Separator(),
                attributesBox,
                new Separator(),
                methodsBox
        );

        getChildren().addAll(background, classContent, selectionRectangle);

        // Mettre à jour la vue
        updateView();

        // Configurer les gestionnaires d'événements
        setupEventHandlers();
        setupContextMenu();

        // Lier la taille du rectangle de fond et de sélection à celle du composant
        background.widthProperty().bind(widthProperty());
        background.heightProperty().bind(heightProperty());
        selectionRectangle.widthProperty().bind(widthProperty());
        selectionRectangle.heightProperty().bind(heightProperty());
    }

    /**
     * Met à jour l'affichage du composant
     */
    public void updateView() {
        // Mettre à jour le titre
        updateTitle();

        // Mettre à jour les attributs
        updateAttributes();

        // Mettre à jour les méthodes
        updateMethods();

        // Mettre à jour la position et la taille
        setPrefSize(classElement.getWidth(), classElement.getHeight());
        setLayoutX(classElement.getX());
        setLayoutY(classElement.getY());

        // Mettre à jour les couleurs
        for (javafx.scene.Node child : getChildren()) {
            if (child instanceof Rectangle rect && rect != selectionRectangle) {
                rect.setFill(Color.web(classElement.getBackgroundColor()));
                rect.setStroke(Color.web(classElement.getBorderColor()));
                rect.setStrokeWidth(classElement.getBorderWidth());
            }
        }
    }

    /**
     * Met à jour le titre de la classe
     */
    private void updateTitle() {
        StringBuilder title = new StringBuilder();

        // Ajouter des stéréotypes selon le type
        switch (classElement.getType()) {
            case INTERFACE:
                title.append("«interface»\n");
                break;
            case ENUM:
                title.append("«enumeration»\n");
                break;
            case ABSTRACT_CLASS:
                title.append("«abstract»\n");
                break;
        }

        // Ajouter le nom de la classe
        title.append(classElement.getName());

        titleLabel.setText(title.toString());

        // Définir le style selon le type
        if (classElement.isAbstract() && classElement.getType() == ClassElement.ClassType.CLASS) {
            titleLabel.setStyle("-fx-font-style: italic; -fx-font-weight: bold;");
        } else {
            titleLabel.setStyle("-fx-font-weight: bold;");
        }
    }

    /**
     * Met à jour la liste des attributs
     */
    private void updateAttributes() {
        attributesBox.getChildren().clear();

        for (Attribute attribute : classElement.getAttributes()) {
            Label label = new Label(formatAttribute(attribute));
            label.setMaxWidth(Double.MAX_VALUE);
            attributesBox.getChildren().add(label);
        }
    }

    /**
     * Met à jour la liste des méthodes
     */
    private void updateMethods() {
        methodsBox.getChildren().clear();

        for (Method method : classElement.getMethods()) {
            Label label = new Label(formatMethod(method));
            label.setMaxWidth(Double.MAX_VALUE);
            methodsBox.getChildren().add(label);
        }
    }

    /**
     * Formate un attribut pour l'affichage
     */
    private String formatAttribute(Attribute attribute) {
        StringBuilder sb = new StringBuilder();

        // Visibilité
        sb.append(getVisibilitySymbol(attribute.getVisibility())).append(" ");

        // Nom
        sb.append(attribute.getName());

        // Type
        sb.append(": ").append(attribute.getType());

        // Modificateurs
        if (attribute.isStatic()) {
            sb.append(" {static}");
        }

        if (attribute.isFinal()) {
            sb.append(" {readOnly}");
        }

        return sb.toString();
    }

    /**
     * Formate une méthode pour l'affichage
     */
    private String formatMethod(Method method) {
        StringBuilder sb = new StringBuilder();

        // Visibilité
        sb.append(getVisibilitySymbol(method.getVisibility())).append(" ");

        // Nom
        sb.append(method.getName());

        // Paramètres
        sb.append("(");
        if (!method.getParameters().isEmpty()) {
            for (int i = 0; i < method.getParameters().size(); i++) {
                var param = method.getParameters().get(i);
                sb.append(param.getName()).append(": ").append(param.getType());
                if (i < method.getParameters().size() - 1) {
                    sb.append(", ");
                }
            }
        }
        sb.append(")");

        // Type de retour
        if (method.getReturnType() != null && !method.getReturnType().equals("void")) {
            sb.append(": ").append(method.getReturnType());
        }

        // Modificateurs
        if (method.isStatic()) {
            sb.append(" {static}");
        }

        if (method.isAbstract()) {
            sb.append(" {abstract}");
        }

        return sb.toString();
    }

    /**
     * Retourne le symbole UML pour une visibilité
     */
    private String getVisibilitySymbol(Visibility visibility) {
        return visibility.getSymbol();
    }

    /**
     * Configure les gestionnaires d'événements
     */
    private void setupEventHandlers() {
        // Clic de souris
        setOnMouseClicked(event -> {
            if (event.getButton() == MouseButton.PRIMARY) {
                // Sélectionner cet élément
                if (event.isControlDown()) {
                    // Ajouter à la sélection avec Ctrl
                    editorController.selectElement(classElement);
                } else {
                    // Sinon, remplacer la sélection
                    editorController.clearSelection();
                    editorController.selectElement(classElement);
                }

                event.consume();

            } else if (event.getButton() == MouseButton.SECONDARY) {
                // Géré par le menu contextuel
                event.consume();
            }
        });

        // Début de glisser
        setOnMousePressed(event -> {
            if (event.getButton() == MouseButton.PRIMARY) {
                startDragX = event.getSceneX();
                startDragY = event.getSceneY();

                // Sélectionner si ce n'est pas déjà fait
                if (!editorController.getSelectedElements().contains(classElement)) {
                    if (!event.isControlDown()) {
                        editorController.clearSelection();
                    }
                    editorController.selectElement(classElement);
                }

                event.consume();
            }
        });

        // Glissement
        setOnMouseDragged(event -> {
            if (event.getButton() == MouseButton.PRIMARY) {
                double deltaX = event.getSceneX() - startDragX;
                double deltaY = event.getSceneY() - startDragY;

                // Si le déplacement est significatif, le considérer comme un glissement
                if (Math.abs(deltaX) > 5 || Math.abs(deltaY) > 5) {
                    isDragging = true;
                }

                if (isDragging) {
                    // Mettre à jour la position
                    double newX = getLayoutX() + deltaX;
                    double newY = getLayoutY() + deltaY;

                    // Mise à jour du modèle
                    classElement.setX(newX);
                    classElement.setY(newY);

                    // Mise à jour visuelle
                    setLayoutX(newX);
                    setLayoutY(newY);

                    // Mettre à jour le point de départ pour le prochain événement
                    startDragX = event.getSceneX();
                    startDragY = event.getSceneY();

                    // Marquer comme modifié
                    editorController.setUnsavedChanges(true);
                }

                event.consume();
            }
        });

        // Fin de glissement
        setOnMouseReleased(event -> {
            if (event.getButton() == MouseButton.PRIMARY) {
                if (isDragging) {
                    // Vérifier s'il faut aligner sur la grille
                    if (editorController.isSnapToGrid()) {
                        double gridSize = editorController.getDiagram().getGridSize();
                        double newX = Math.round(classElement.getX() / gridSize) * gridSize;
                        double newY = Math.round(classElement.getY() / gridSize) * gridSize;

                        // Mise à jour du modèle
                        classElement.setX(newX);
                        classElement.setY(newY);

                        // Mise à jour visuelle
                        setLayoutX(newX);
                        setLayoutY(newY);
                    }

                    // Rafraîchir les composants de relation
                    editorController.refreshRelationships();
                }

                isDragging = false;
                event.consume();
            }
        });
    }

    /**
     * Configure le menu contextuel
     */
    private void setupContextMenu() {
        ContextMenu contextMenu = new ContextMenu();

        // Option d'édition
        MenuItem editItem = new MenuItem("Éditer...");
        editItem.setOnAction(event -> editorController.editClassElement(classElement));

        // Option de suppression
        MenuItem deleteItem = new MenuItem("Supprimer");
        deleteItem.setOnAction(event -> editorController.deleteClassElement(classElement));

        // Options d'ajout de relation
        MenuItem addInheritanceItem = new MenuItem("Ajouter une relation d'héritage");
        addInheritanceItem.setOnAction(event -> startAddingRelationship());

        MenuItem addAssociationItem = new MenuItem("Ajouter une relation d'association");
        addAssociationItem.setOnAction(event -> startAddingRelationship());

        // Ajouter les options au menu
        contextMenu.getItems().addAll(
                editItem,
                deleteItem,
                new javafx.scene.control.SeparatorMenuItem(),
                addInheritanceItem,
                addAssociationItem
        );

        // Définir le menu contextuel
        setOnContextMenuRequested(event -> {
            contextMenu.show(this, event.getScreenX(), event.getScreenY());
            event.consume();
        });
    }

    /**
     * Commence l'ajout d'une relation à partir de ce nœud
     */
    private void startAddingRelationship() {
        editorController.startAddingRelationship();
        editorController.selectRelationSource(this);
    }

    /**
     * Définit l'état de sélection du nœud
     */
    public void setSelected(boolean selected) {
        selectionRectangle.setVisible(selected);
    }


}
