package com.diagramme.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class ClassDiagramTest {

    private ClassDiagram diagram;
    private ClassElement classElement;
    private RelationshipElement relationshipElement;

    @BeforeEach
    public void setUp() {
        diagram = new ClassDiagram("Test Diagram");

        // Créer un élément de classe
        classElement = new ClassElement("TestClass");

        // Créer un deuxième élément de classe pour la relation
        ClassElement targetClass = new ClassElement("TargetClass");

        // Créer une relation
        relationshipElement = new RelationshipElement(
                "TestRelation",
                classElement,
                targetClass,
                com.diagramme.model.enums.RelationshipType.ASSOCIATION
        );
    }

    @Test
    public void testAddElement() {
        // Ajouter les éléments au diagramme
        diagram.addElement(classElement);
        diagram.addElement(relationshipElement);

        // Vérifier que les éléments ont été ajoutés
        assertEquals(2, diagram.getElements().size());
        assertTrue(diagram.getElements().contains(classElement));
        assertTrue(diagram.getElements().contains(relationshipElement));

        // Vérifier que les éléments sont correctement associés au diagramme
        assertEquals(diagram, classElement.getDiagram());
        assertEquals(diagram, relationshipElement.getDiagram());
    }

    @Test
    public void testRemoveElement() {
        // Ajouter les éléments au diagramme
        diagram.addElement(classElement);
        diagram.addElement(relationshipElement);

        // Supprimer un élément
        diagram.removeElement(classElement);

        // Vérifier que l'élément a été supprimé
        assertEquals(1, diagram.getElements().size());
        assertFalse(diagram.getElements().contains(classElement));
        assertTrue(diagram.getElements().contains(relationshipElement));

        // Vérifier que l'élément n'est plus associé au diagramme
        assertNull(classElement.getDiagram());
        assertEquals(diagram, relationshipElement.getDiagram());
    }

    @Test
    public void testGetClasses() {
        // Ajouter les éléments au diagramme
        diagram.addElement(classElement);
        diagram.addElement(relationshipElement);

        // Vérifier que getClasses ne retourne que les éléments de classe
        assertEquals(1, diagram.getClasses().size());
        assertTrue(diagram.getClasses().contains(classElement));
    }

    @Test
    public void testGetRelationships() {
        // Ajouter les éléments au diagramme
        diagram.addElement(classElement);
        diagram.addElement(relationshipElement);

        // Vérifier que getRelationships ne retourne que les éléments de relation
        assertEquals(1, diagram.getRelationships().size());
        assertTrue(diagram.getRelationships().contains(relationshipElement));
    }
}
