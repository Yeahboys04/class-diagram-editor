package com.diagramme.model;

import com.diagramme.model.enums.Visibility;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class ClassElementTest {

    private ClassElement classElement;

    @BeforeEach
    public void setUp() {
        classElement = new ClassElement("TestClass");
        classElement.setPackageName("com.test");
        classElement.setType(ClassElement.ClassType.CLASS);
    }

    @Test
    public void testConstructorAndGetters() {
        assertEquals("TestClass", classElement.getName());
        assertEquals("com.test", classElement.getPackageName());
        assertEquals(ClassElement.ClassType.CLASS, classElement.getType());
        assertFalse(classElement.isAbstract());
    }

    @Test
    public void testSetters() {
        // Modification des propriétés
        classElement.setName("ModifiedClass");
        classElement.setPackageName("com.modified");
        classElement.setType(ClassElement.ClassType.INTERFACE);
        classElement.setAbstract(true);

        // Vérification des modifications
        assertEquals("ModifiedClass", classElement.getName());
        assertEquals("com.modified", classElement.getPackageName());
        assertEquals(ClassElement.ClassType.INTERFACE, classElement.getType());
        assertTrue(classElement.isAbstract());
    }

    @Test
    public void testAddAttribute() {
        // Créer et ajouter un attribut
        Attribute attribute = new Attribute("testAttr", "String");
        classElement.addAttribute(attribute);

        // Vérifier que l'attribut a été ajouté
        assertEquals(1, classElement.getAttributes().size());
        assertTrue(classElement.getAttributes().contains(attribute));

        // Vérifier que l'attribut est correctement associé à la classe
        assertEquals(classElement, attribute.getClassElement());
    }

    @Test
    public void testRemoveAttribute() {
        // Créer et ajouter un attribut
        Attribute attribute = new Attribute("testAttr", "String");
        classElement.addAttribute(attribute);

        // Supprimer l'attribut
        classElement.removeAttribute(attribute);

        // Vérifier que l'attribut a été supprimé
        assertEquals(0, classElement.getAttributes().size());
        assertFalse(classElement.getAttributes().contains(attribute));

        // Vérifier que l'attribut n'est plus associé à la classe
        assertNull(attribute.getClassElement());
    }

    @Test
    public void testAddMethod() {
        // Créer et ajouter une méthode
        Method method = new Method("testMethod", "void");
        classElement.addMethod(method);

        // Vérifier que la méthode a été ajoutée
        assertEquals(1, classElement.getMethods().size());
        assertTrue(classElement.getMethods().contains(method));

        // Vérifier que la méthode est correctement associée à la classe
        assertEquals(classElement, method.getClassElement());
    }

    @Test
    public void testRemoveMethod() {
        // Créer et ajouter une méthode
        Method method = new Method("testMethod", "void");
        classElement.addMethod(method);

        // Supprimer la méthode
        classElement.removeMethod(method);

        // Vérifier que la méthode a été supprimée
        assertEquals(0, classElement.getMethods().size());
        assertFalse(classElement.getMethods().contains(method));

        // Vérifier que la méthode n'est plus associée à la classe
        assertNull(method.getClassElement());
    }
}
