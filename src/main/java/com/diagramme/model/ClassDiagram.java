package com.diagramme.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Représente un diagramme de classe UML
 */
@Entity
@Table(name = "class_diagrams")
@Data
@NoArgsConstructor
public class ClassDiagram {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    private String description;

    @Column(nullable = false, unique = true)
    private String uuid = UUID.randomUUID().toString();

    @OneToMany(mappedBy = "diagram", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DiagramElement> elements = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "modified_at")
    private LocalDateTime modifiedAt;

    // Paramètres du diagramme
    private boolean showGrid = true;
    private boolean snapToGrid = true;
    private double gridSize = 20.0;
    private String backgroundColor = "#FFFFFF";

    // Métadonnées
    private String author;
    private String version;

    public ClassDiagram(String name) {
        this.name = name;
    }

    /**
     * Ajoute un élément au diagramme
     */
    public void addElement(DiagramElement element) {
        element.setDiagram(this);
        elements.add(element);
    }

    /**
     * Supprime un élément du diagramme
     */
    public void removeElement(DiagramElement element) {
        elements.remove(element);
        element.setDiagram(null);
    }

    /**
     * Récupère toutes les classes du diagramme
     */
    public List<ClassElement> getClasses() {
        return elements.stream()
                .filter(element -> element instanceof ClassElement)
                .map(element -> (ClassElement) element)
                .toList();
    }

    /**
     * Récupère toutes les relations du diagramme
     */
    public List<RelationshipElement> getRelationships() {
        return elements.stream()
                .filter(element -> element instanceof RelationshipElement)
                .map(element -> (RelationshipElement) element)
                .toList();
    }
}