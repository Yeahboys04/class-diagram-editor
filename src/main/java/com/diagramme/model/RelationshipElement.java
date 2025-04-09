package com.diagramme.model;

import com.diagramme.model.enums.RelationshipType;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.ArrayList;
import java.util.List;

/**
 * Représente une relation entre deux classes dans un diagramme UML
 */
@Entity
@Table(name = "relationship_elements")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@NoArgsConstructor
public class RelationshipElement extends DiagramElement {

    @ManyToOne
    @JoinColumn(name = "source_id", nullable = false)
    private ClassElement sourceElement;

    @ManyToOne
    @JoinColumn(name = "target_id", nullable = false)
    private ClassElement targetElement;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RelationshipType type;

    private String sourceRole;
    private String targetRole;

    private String sourceMultiplicity;
    private String targetMultiplicity;

    private String sourceTooltip;
    private String targetTooltip;

    // Style
    private String lineColor = "#000000";
    private double lineWidth = 1.0;
    private String lineStyle = "SOLID"; // SOLID, DASHED, DOTTED

    // Points de contrôle pour les lignes courbes
    @ElementCollection
    @OrderColumn
    @CollectionTable(name = "relationship_control_points")
    private List<Point> controlPoints = new ArrayList<>();

    public RelationshipElement(String name, ClassElement source, ClassElement target, RelationshipType type) {
        super(name);
        this.sourceElement = source;
        this.targetElement = target;
        this.type = type;
    }

    /**
     * Classe utilitaire pour les points de contrôle
     */
    @Embeddable
    @Data
    @NoArgsConstructor
    public static class Point {
        private double x;
        private double y;

        public Point(double x, double y) {
            this.x = x;
            this.y = y;
        }
    }
}