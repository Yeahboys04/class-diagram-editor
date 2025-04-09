package com.diagramme.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * Classe abstraite représentant un élément dans un diagramme de classe
 */
@Entity
@Table(name = "diagram_elements")
@Inheritance(strategy = InheritanceType.JOINED)
@Data
@NoArgsConstructor
public abstract class DiagramElement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    // Position et taille dans le diagramme
    private double x;
    private double y;
    private double width;
    private double height;

    // Relation avec le diagramme parent
    @ManyToOne
    @JoinColumn(name = "diagram_id")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private ClassDiagram diagram;

    public DiagramElement(String name) {
        this.name = name;
    }
}