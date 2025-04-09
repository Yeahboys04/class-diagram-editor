package com.diagramme.util;

import com.diagramme.model.ClassDiagram;
import com.diagramme.model.ClassElement;
import com.diagramme.model.RelationshipElement;
import com.diagramme.model.enums.RelationshipType;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Validateur de diagrammes UML
 */
public class UmlValidator {

    /**
     * Valide un diagramme UML et retourne la liste des problèmes
     */
    public static List<String> validateDiagram(ClassDiagram diagram) {
        List<String> issues = new ArrayList<>();

        // Vérifier les noms en double
        checkDuplicateNames(diagram, issues);

        // Vérifier les cycles d'héritage
        checkInheritanceCycles(diagram, issues);

        // Vérifier les relations d'héritage multiples (non autorisé en Java)
        checkMultipleInheritance(diagram, issues);

        // Vérifier les relations incohérentes
        checkInconsistentRelationships(diagram, issues);

        return issues;
    }

    /**
     * Vérifie les noms en double
     */
    private static void checkDuplicateNames(ClassDiagram diagram, List<String> issues) {
        Set<String> classNames = new HashSet<>();

        for (ClassElement element : diagram.getClasses()) {
            String fullName = element.getPackageName() != null && !element.getPackageName().isEmpty()
                    ? element.getPackageName() + "." + element.getName()
                    : element.getName();

            if (classNames.contains(fullName)) {
                issues.add("Nom de classe en double: " + fullName);
            } else {
                classNames.add(fullName);
            }
        }
    }

    /**
     * Vérifie les cycles d'héritage
     */
    private static void checkInheritanceCycles(ClassDiagram diagram, List<String> issues) {
        // Pour chaque classe, vérifier si elle est impliquée dans un cycle d'héritage
        for (ClassElement element : diagram.getClasses()) {
            Set<ClassElement> visited = new HashSet<>();
            visited.add(element);

            // Parcourir les parents
            for (RelationshipElement relationship : diagram.getRelationships()) {
                if (relationship.getType() == RelationshipType.INHERITANCE &&
                        relationship.getSourceElement() == element) {

                    ClassElement parent = relationship.getTargetElement();
                    if (hasPathToClass(diagram, parent, element, visited)) {
                        issues.add("Cycle d'héritage détecté impliquant " + element.getName());
                        break;
                    }
                }
            }
        }
    }

    /**
     * Vérifie s'il existe un chemin d'héritage de source à target
     */
    private static boolean hasPathToClass(ClassDiagram diagram, ClassElement source, ClassElement target, Set<ClassElement> visited) {
        if (source == target) {
            return true;
        }

        visited.add(source);

        for (RelationshipElement relationship : diagram.getRelationships()) {
            if (relationship.getType() == RelationshipType.INHERITANCE &&
                    relationship.getSourceElement() == source) {

                ClassElement parent = relationship.getTargetElement();
                if (!visited.contains(parent) && hasPathToClass(diagram, parent, target, visited)) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Vérifie les relations d'héritage multiples (non autorisé en Java)
     */
    private static void checkMultipleInheritance(ClassDiagram diagram, List<String> issues) {
        for (ClassElement element : diagram.getClasses()) {
            if (element.getType() == ClassElement.ClassType.CLASS ||
                    element.getType() == ClassElement.ClassType.ENUM) {

                int inheritanceCount = 0;
                for (RelationshipElement relationship : diagram.getRelationships()) {
                    if (relationship.getType() == RelationshipType.INHERITANCE &&
                            relationship.getSourceElement() == element) {
                        inheritanceCount++;
                    }
                }

                if (inheritanceCount > 1) {
                    issues.add("Héritage multiple détecté pour " + element.getName() + " (non autorisé en Java)");
                }
            }
        }
    }

    /**
     * Vérifie les relations incohérentes
     */
    private static void checkInconsistentRelationships(ClassDiagram diagram, List<String> issues) {
        for (RelationshipElement relationship : diagram.getRelationships()) {
            // Vérifier qu'une interface ne peut pas implémenter une classe
            if (relationship.getType() == RelationshipType.IMPLEMENTATION) {
                ClassElement source = relationship.getSourceElement();
                ClassElement target = relationship.getTargetElement();

                if (target.getType() != ClassElement.ClassType.INTERFACE) {
                    issues.add("La relation d'implémentation de " + source.getName() +
                            " vers " + target.getName() + " est incorrecte: la cible doit être une interface");
                }
            }

            // Vérifier que les associations ont des rôles et multiplicités cohérents
            if (relationship.getType() == RelationshipType.ASSOCIATION ||
                    relationship.getType() == RelationshipType.AGGREGATION ||
                    relationship.getType() == RelationshipType.COMPOSITION) {

                String sourceMultiplicity = relationship.getSourceMultiplicity();
                String targetMultiplicity = relationship.getTargetMultiplicity();

                if (sourceMultiplicity != null && !sourceMultiplicity.isEmpty() && !isValidMultiplicity(sourceMultiplicity)) {
                    issues.add("Multiplicité de source invalide '" + sourceMultiplicity +
                            "' dans la relation entre " + relationship.getSourceElement().getName() +
                            " et " + relationship.getTargetElement().getName());
                }

                if (targetMultiplicity != null && !targetMultiplicity.isEmpty() && !isValidMultiplicity(targetMultiplicity)) {
                    issues.add("Multiplicité de cible invalide '" + targetMultiplicity +
                            "' dans la relation entre " + relationship.getSourceElement().getName() +
                            " et " + relationship.getTargetElement().getName());
                }
            }
        }
    }

    /**
     * Vérifie si une multiplicité est valide
     */
    private static boolean isValidMultiplicity(String multiplicity) {
        if (multiplicity == null || multiplicity.isEmpty()) {
            return true;
        }

        // Expressions valides: "0", "1", "*", "0..1", "0..*", "1..*", "n", "m..n"
        String pattern = "(\\d+|\\*)|(\\d+)\\.\\.(\\d+|\\*)";
        return multiplicity.matches(pattern);
    }
}
