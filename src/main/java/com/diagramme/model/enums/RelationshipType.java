package com.diagramme.model.enums;

import lombok.Getter;

/**
 * Types de relations entre classes dans un diagramme UML
 */
@Getter
public enum RelationshipType {
    INHERITANCE("Héritage", "Une classe hérite d'une autre"),
    IMPLEMENTATION("Implémentation", "Une classe implémente une interface"),
    ASSOCIATION("Association", "Une relation générique entre deux classes"),
    AGGREGATION("Agrégation", "Une relation tout-partie où la partie peut exister indépendamment du tout"),
    COMPOSITION("Composition", "Une relation tout-partie où la partie ne peut pas exister sans le tout"),
    DEPENDENCY("Dépendance", "Une classe dépend d'une autre classe");

    private final String label;
    private final String description;

    RelationshipType(String label, String description) {
        this.label = label;
        this.description = description;
    }

}
