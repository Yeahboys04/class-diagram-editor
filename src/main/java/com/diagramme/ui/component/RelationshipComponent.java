package com.diagramme.ui.component;

import com.diagramme.model.RelationshipElement;
import com.diagramme.model.enums.RelationshipType;
import com.diagramme.ui.DiagramEditorController;
import javafx.geometry.Point2D;
import javafx.scene.Group;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.input.MouseButton;
import javafx.scene.paint.Color;
import javafx.scene.shape.*;
import javafx.scene.text.Text;
import lombok.Getter;

/**
 * Composant visuel représentant une relation entre deux classes
 */
public class RelationshipComponent extends Group {

    @Getter
    private final RelationshipElement relationship;
    private final ClassNodeComponent sourceNode;
    private final ClassNodeComponent targetNode;
    private final DiagramEditorController editorController;

    private Line mainLine;
    private Path arrowHead;
    private Text sourceRoleText;
    private Text targetRoleText;
    private Text sourceMultiplicityText;
    private Text targetMultiplicityText;
    private Path selectionPath;

    public RelationshipComponent(
            RelationshipElement relationship,
            ClassNodeComponent sourceNode,
            ClassNodeComponent targetNode,
            DiagramEditorController editorController) {
        this.relationship = relationship;
        this.sourceNode = sourceNode;
        this.targetNode = targetNode;
        this.editorController = editorController;

        // Créer les éléments visuels
        createVisuals();

        // Configurer les gestionnaires d'événements
        setupEventHandlers();
        setupContextMenu();

        // Mettre à jour la vue
        updateView();
    }

    /**
     * Crée les éléments visuels de la relation
     */
    private void createVisuals() {
        // Ligne principale
        mainLine = new Line();
        mainLine.setStrokeWidth(relationship.getLineWidth());
        mainLine.setStroke(Color.web(relationship.getLineColor()));

        // Définir le style de ligne
        if ("DASHED".equals(relationship.getLineStyle())) {
            mainLine.getStrokeDashArray().addAll(5.0, 5.0);
        } else if ("DOTTED".equals(relationship.getLineStyle())) {
            mainLine.getStrokeDashArray().addAll(2.0, 2.0);
        }

        // Flèche
        arrowHead = new Path();
        arrowHead.setFill(Color.web(relationship.getLineColor()));
        arrowHead.setStroke(Color.web(relationship.getLineColor()));

        // Textes
        sourceRoleText = new Text();
        targetRoleText = new Text();
        sourceMultiplicityText = new Text();
        targetMultiplicityText = new Text();

        // Chemin de sélection (invisible, mais utilisé pour la détection de clics)
        selectionPath = new Path();
        selectionPath.setStroke(Color.BLUE);
        selectionPath.setStrokeWidth(2);
        selectionPath.setFill(null);
        selectionPath.setVisible(false);
        selectionPath.setMouseTransparent(false);

        // Ajouter les éléments au groupe
        getChildren().addAll(
                mainLine,
                arrowHead,
                sourceRoleText,
                targetRoleText,
                sourceMultiplicityText,
                targetMultiplicityText,
                selectionPath
        );
    }

    /**
     * Met à jour l'affichage de la relation
     */
    public void updateView() {
        // Calculer les points de départ et d'arrivée
        Point2D sourcePoint = calculateConnectionPoint(sourceNode, targetNode);
        Point2D targetPoint = calculateConnectionPoint(targetNode, sourceNode);

        // Mettre à jour la ligne
        mainLine.setStartX(sourcePoint.getX());
        mainLine.setStartY(sourcePoint.getY());
        mainLine.setEndX(targetPoint.getX());
        mainLine.setEndY(targetPoint.getY());

        // Mettre à jour la flèche
        updateArrowHead(sourcePoint, targetPoint);

        // Mettre à jour les textes
        updateTexts(sourcePoint, targetPoint);

        // Mettre à jour le chemin de sélection
        updateSelectionPath(sourcePoint, targetPoint);

        // Mettre à jour l'apparence selon le type de relation
        updateRelationshipAppearance();
    }

    /**
     * Calcule le point de connexion entre deux nœuds
     */
    private Point2D calculateConnectionPoint(ClassNodeComponent source, ClassNodeComponent target) {
        // Récupérer les coordonnées et dimensions
        double sourceX = source.getLayoutX() + source.getWidth() / 2;
        double sourceY = source.getLayoutY() + source.getHeight() / 2;
        double targetX = target.getLayoutX() + target.getWidth() / 2;
        double targetY = target.getLayoutY() + target.getHeight() / 2;

        // Calculer les vecteurs
        double dx = targetX - sourceX;
        double dy = targetY - sourceY;
        double angle = Math.atan2(dy, dx);

        // Calculer les intersections avec le bord du rectangle
        double sourceWidth = source.getWidth() / 2;
        double sourceHeight = source.getHeight() / 2;

        // Déterminer quelle bordure intersecte
        double intersectX, intersectY;

        if (Math.abs(Math.tan(angle)) < sourceHeight / sourceWidth) {
            // Intersection avec les bords gauche ou droit
            intersectX = sourceWidth * Math.signum(dx);
            intersectY = intersectX * Math.tan(angle);
        } else {
            // Intersection avec les bords haut ou bas
            intersectY = sourceHeight * Math.signum(dy);
            intersectX = intersectY / Math.tan(angle);
        }

        // Coordonnées du point d'intersection
        return new Point2D(
                sourceX + intersectX,
                sourceY + intersectY
        );
    }

    /**
     * Met à jour la flèche selon le type de relation
     */
    private void updateArrowHead(Point2D sourcePoint, Point2D targetPoint) {
        // Calculer l'angle
        double dx = targetPoint.getX() - sourcePoint.getX();
        double dy = targetPoint.getY() - sourcePoint.getY();
        double angle = Math.atan2(dy, dx);

        // Créer la flèche selon le type de relation
        arrowHead.getElements().clear();

        double arrowSize = 10.0;

        switch (relationship.getType()) {
            case INHERITANCE:
            case IMPLEMENTATION:
                // Flèche ouverte (triangle vide)
                double x1 = targetPoint.getX() - arrowSize * Math.cos(angle - Math.PI / 6);
                double y1 = targetPoint.getY() - arrowSize * Math.sin(angle - Math.PI / 6);
                double x2 = targetPoint.getX() - arrowSize * Math.cos(angle + Math.PI / 6);
                double y2 = targetPoint.getY() - arrowSize * Math.sin(angle + Math.PI / 6);

                arrowHead.getElements().add(new MoveTo(targetPoint.getX(), targetPoint.getY()));
                arrowHead.getElements().add(new LineTo(x1, y1));
                arrowHead.getElements().add(new LineTo(x2, y2));
                arrowHead.getElements().add(new LineTo(targetPoint.getX(), targetPoint.getY()));

                arrowHead.setFill(null);
                break;

            case ASSOCIATION:
                // Flèche simple
                x1 = targetPoint.getX() - arrowSize * Math.cos(angle - Math.PI / 6);
                y1 = targetPoint.getY() - arrowSize * Math.sin(angle - Math.PI / 6);
                x2 = targetPoint.getX() - arrowSize * Math.cos(angle + Math.PI / 6);
                y2 = targetPoint.getY() - arrowSize * Math.sin(angle + Math.PI / 6);

                arrowHead.getElements().add(new MoveTo(targetPoint.getX(), targetPoint.getY()));
                arrowHead.getElements().add(new LineTo(x1, y1));
                arrowHead.getElements().add(new MoveTo(targetPoint.getX(), targetPoint.getY()));
                arrowHead.getElements().add(new LineTo(x2, y2));

                arrowHead.setFill(null);
                break;

            case AGGREGATION:
                // Diamant vide
                double x0 = targetPoint.getX() - 2 * arrowSize * Math.cos(angle);
                double y0 = targetPoint.getY() - 2 * arrowSize * Math.sin(angle);
                x1 = x0 - arrowSize * Math.cos(angle - Math.PI / 2);
                y1 = y0 - arrowSize * Math.sin(angle - Math.PI / 2);
                x2 = x0 - arrowSize * Math.cos(angle + Math.PI / 2);
                y2 = y0 - arrowSize * Math.sin(angle + Math.PI / 2);

                arrowHead.getElements().add(new MoveTo(targetPoint.getX(), targetPoint.getY()));
                arrowHead.getElements().add(new LineTo(x1, y1));
                arrowHead.getElements().add(new LineTo(x0, y0));
                arrowHead.getElements().add(new LineTo(x2, y2));
                arrowHead.getElements().add(new LineTo(targetPoint.getX(), targetPoint.getY()));

                arrowHead.setFill(null);
                break;

            case COMPOSITION:
                // Diamant plein
                x0 = targetPoint.getX() - 2 * arrowSize * Math.cos(angle);
                y0 = targetPoint.getY() - 2 * arrowSize * Math.sin(angle);
                x1 = x0 - arrowSize * Math.cos(angle - Math.PI / 2);
                y1 = y0 - arrowSize * Math.sin(angle - Math.PI / 2);
                x2 = x0 - arrowSize * Math.cos(angle + Math.PI / 2);
                y2 = y0 - arrowSize * Math.sin(angle + Math.PI / 2);

                arrowHead.getElements().add(new MoveTo(targetPoint.getX(), targetPoint.getY()));
                arrowHead.getElements().add(new LineTo(x1, y1));
                arrowHead.getElements().add(new LineTo(x0, y0));
                arrowHead.getElements().add(new LineTo(x2, y2));
                arrowHead.getElements().add(new LineTo(targetPoint.getX(), targetPoint.getY()));

                arrowHead.setFill(Color.web(relationship.getLineColor()));
                break;

            case DEPENDENCY:
                // Flèche en pointillés
                x1 = targetPoint.getX() - arrowSize * Math.cos(angle - Math.PI / 6);
                y1 = targetPoint.getY() - arrowSize * Math.sin(angle - Math.PI / 6);
                x2 = targetPoint.getX() - arrowSize * Math.cos(angle + Math.PI / 6);
                y2 = targetPoint.getY() - arrowSize * Math.sin(angle + Math.PI / 6);

                arrowHead.getElements().add(new MoveTo(targetPoint.getX(), targetPoint.getY()));
                arrowHead.getElements().add(new LineTo(x1, y1));
                arrowHead.getElements().add(new MoveTo(targetPoint.getX(), targetPoint.getY()));
                arrowHead.getElements().add(new LineTo(x2, y2));

                arrowHead.setFill(null);
                break;
        }
    }

    /**
     * Met à jour les textes de la relation
     */
    private void updateTexts(Point2D sourcePoint, Point2D targetPoint) {
        // Calculer le point médian
        double midX = (sourcePoint.getX() + targetPoint.getX()) / 2;
        double midY = (sourcePoint.getY() + targetPoint.getY()) / 2;

        // Calculer la direction perpendiculaire pour le décalage
        double dx = targetPoint.getX() - sourcePoint.getX();
        double dy = targetPoint.getY() - sourcePoint.getY();
        double length = Math.sqrt(dx * dx + dy * dy);

        // Éviter la division par zéro
        if (length < 1e-6) {
            return;
        }

        // Normaliser
        double nx = dx / length;
        double ny = dy / length;

        // Vecteur perpendiculaire
        double px = -ny;
        double py = nx;

        // Décalage pour les étiquettes
        double offset = 15;

        // Mettre à jour les rôles
        if (relationship.getSourceRole() != null && !relationship.getSourceRole().isEmpty()) {
            sourceRoleText.setText(relationship.getSourceRole());
            sourceRoleText.setX(sourcePoint.getX() + px * offset);
            sourceRoleText.setY(sourcePoint.getY() + py * offset);
        } else {
            sourceRoleText.setText("");
        }

        if (relationship.getTargetRole() != null && !relationship.getTargetRole().isEmpty()) {
            targetRoleText.setText(relationship.getTargetRole());
            targetRoleText.setX(targetPoint.getX() + px * offset);
            targetRoleText.setY(targetPoint.getY() + py * offset);
        } else {
            targetRoleText.setText("");
        }

        // Mettre à jour les multiplicités
        if (relationship.getSourceMultiplicity() != null && !relationship.getSourceMultiplicity().isEmpty()) {
            sourceMultiplicityText.setText(relationship.getSourceMultiplicity());
            sourceMultiplicityText.setX(sourcePoint.getX() - px * offset);
            sourceMultiplicityText.setY(sourcePoint.getY() - py * offset);
        } else {
            sourceMultiplicityText.setText("");
        }

        if (relationship.getTargetMultiplicity() != null && !relationship.getTargetMultiplicity().isEmpty()) {
            targetMultiplicityText.setText(relationship.getTargetMultiplicity());
            targetMultiplicityText.setX(targetPoint.getX() - px * offset);
            targetMultiplicityText.setY(targetPoint.getY() - py * offset);
        } else {
            targetMultiplicityText.setText("");
        }
    }

    /**
     * Met à jour le chemin de sélection
     */
    private void updateSelectionPath(Point2D sourcePoint, Point2D targetPoint) {
        selectionPath.getElements().clear();

        selectionPath.getElements().add(new MoveTo(sourcePoint.getX(), sourcePoint.getY()));
        selectionPath.getElements().add(new LineTo(targetPoint.getX(), targetPoint.getY()));

        // Élargir le chemin pour faciliter la sélection
        selectionPath.setStrokeWidth(10);
    }

    /**
     * Met à jour l'apparence de la relation selon son type
     */
    private void updateRelationshipAppearance() {
        if (relationship.getType() == RelationshipType.IMPLEMENTATION ||
                relationship.getType() == RelationshipType.DEPENDENCY) {
            mainLine.getStrokeDashArray().setAll(5.0, 5.0);
        } else {
            mainLine.getStrokeDashArray().clear();
        }

        // Mettre à jour les couleurs
        Color lineColor = Color.web(relationship.getLineColor());
        mainLine.setStroke(lineColor);
        mainLine.setStrokeWidth(relationship.getLineWidth());
        arrowHead.setStroke(lineColor);

        if (relationship.getType() == RelationshipType.COMPOSITION) {
            arrowHead.setFill(lineColor);
        } else {
            arrowHead.setFill(null);
        }
    }

    /**
     * Configure les gestionnaires d'événements
     */
    private void setupEventHandlers() {
        // Clic de souris sur le chemin de sélection
        selectionPath.setOnMouseClicked(event -> {
            if (event.getButton() == MouseButton.PRIMARY) {
                // Sélectionner cette relation
                if (event.isControlDown()) {
                    // Ajouter à la sélection avec Ctrl
                    editorController.selectElement(relationship);
                } else {
                    // Sinon, remplacer la sélection
                    editorController.clearSelection();
                    editorController.selectElement(relationship);
                }

                event.consume();

            } else if (event.getButton() == MouseButton.SECONDARY) {
                // Géré par le menu contextuel
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
        editItem.setOnAction(event -> editorController.editRelationship(relationship));

        // Option de suppression
        MenuItem deleteItem = new MenuItem("Supprimer");
        deleteItem.setOnAction(event -> editorController.deleteRelationship(relationship));

        // Ajouter les options au menu
        contextMenu.getItems().addAll(editItem, deleteItem);

        // Définir le menu contextuel sur le groupe entier
        this.setOnContextMenuRequested(event -> {
            contextMenu.show(this, event.getScreenX(), event.getScreenY());
            event.consume();
        });
    }

    /**
     * Définit l'état de sélection de la relation
     */
    public void setSelected(boolean selected) {
        selectionPath.setVisible(selected);
    }
}