package com.diagramme.util;

import com.diagramme.model.ClassDiagram;
import com.diagramme.model.ClassElement;
import com.diagramme.model.RelationshipElement;
import com.diagramme.model.enums.RelationshipType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class UmlValidatorTest {

    private ClassDiagram diagram;
    private ClassElement classA;
    private ClassElement classB;
    private ClassElement interfaceC;

    @BeforeEach
    public void setUp() {
        diagram = new ClassDiagram("Test Diagram");

        // Créer des éléments de classe
        classA = new ClassElement("ClassA");
        classA.setPackageName("com.test");

        classB = new ClassElement("ClassB");
        classB.setPackageName("com.test");

        interfaceC = new ClassElement("InterfaceC");
        interfaceC.setPackageName("com.test");
        interfaceC.setType(ClassElement.ClassType.INTERFACE);

        // Ajouter les éléments au diagramme
        diagram.addElement(classA);
        diagram.addElement(classB);
        diagram.addElement(interfaceC);
    }

    @Test
    public void testValidDiagram() {
        // Ajouter une relation valide
        RelationshipElement relationship = new RelationshipElement(
                "ClassA_implements_InterfaceC",
                classA,
                interfaceC,
                RelationshipType.IMPLEMENTATION
        );
        diagram.addElement(relationship);

        // Valider le diagramme
        List<String> issues = UmlValidator.validateDiagram(diagram);

        // Vérifier qu'il n'y a pas de problèmes
        assertTrue(issues.isEmpty());
    }

    @Test
    public void testDuplicateNames() {
        // Créer une classe avec le même nom
        ClassElement duplicateClass = new ClassElement("ClassA");
        duplicateClass.setPackageName("com.test");
        diagram.addElement(duplicateClass);

        // Valider le diagramme
        List<String> issues = UmlValidator.validateDiagram(diagram);

        // Vérifier qu'il y a un problème de nom en double
        assertFalse(issues.isEmpty());
        assertTrue(issues.get(0).contains("Nom de classe en double"));
    }

    @Test
    public void testInheritanceCycle() {
        // Créer un cycle d'héritage: A extends B, B extends A
        RelationshipElement relationshipAB = new RelationshipElement(
                "ClassA_extends_ClassB",
                classA,
                classB,
                RelationshipType.INHERITANCE
        );

        RelationshipElement relationshipBA = new RelationshipElement(
                "ClassB_extends_ClassA",
                classB,
                classA,
                RelationshipType.INHERITANCE
        );

        diagram.addElement(relationshipAB);
        diagram.addElement(relationshipBA);

        // Valider le diagramme
        List<String> issues = UmlValidator.validateDiagram(diagram);

        // Vérifier qu'il y a un problème de cycle d'héritage
        assertFalse(issues.isEmpty());
        assertTrue(issues.get(0).contains("Cycle d'héritage"));
    }

    @Test
    public void testMultipleInheritance() {
        // Créer une situation d'héritage multiple: A extends B, A extends C
        ClassElement classC = new ClassElement("ClassC");
        classC.setPackageName("com.test");
        diagram.addElement(classC);

        RelationshipElement relationshipAB = new RelationshipElement(
                "ClassA_extends_ClassB",
                classA,
                classB,
                RelationshipType.INHERITANCE
        );

        RelationshipElement relationshipAC = new RelationshipElement(
                "ClassA_extends_ClassC",
                classA,
                classC,
                RelationshipType.INHERITANCE
        );

        diagram.addElement(relationshipAB);
        diagram.addElement(relationshipAC);

        // Valider le diagramme
        List<String> issues = UmlValidator.validateDiagram(diagram);

        // Vérifier qu'il y a un problème d'héritage multiple
        assertFalse(issues.isEmpty());
        assertTrue(issues.get(0).contains("Héritage multiple"));
    }

    @Test
    public void testIncorrectImplementationRelation() {
        // Créer une relation d'implémentation incorrecte: A implements B (B n'est pas une interface)
        RelationshipElement relationship = new RelationshipElement(
                "ClassA_implements_ClassB",
                classA,
                classB,
                RelationshipType.IMPLEMENTATION
        );
        diagram.addElement(relationship);

        // Valider le diagramme
        List<String> issues = UmlValidator.validateDiagram(diagram);

        // Vérifier qu'il y a un problème de relation incorrecte
        assertFalse(issues.isEmpty());
        assertTrue(issues.get(0).contains("relation d'implémentation"));
    }

    @Test
    public void testInvalidMultiplicity() {
        // Créer une relation avec une multiplicité invalide
        RelationshipElement relationship = new RelationshipElement(
                "ClassA_to_ClassB",
                classA,
                classB,
                RelationshipType.ASSOCIATION
        );
        relationship.setTargetMultiplicity("invalid");
        diagram.addElement(relationship);

        // Valider le diagramme
        List<String> issues = UmlValidator.validateDiagram(diagram);

        // Vérifier qu'il y a un problème de multiplicité invalide
        assertFalse(issues.isEmpty());
        assertTrue(issues.get(0).contains("Multiplicité"));
    }
}