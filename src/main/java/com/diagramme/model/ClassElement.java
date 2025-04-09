package com.diagramme.model;

import com.diagramme.model.enums.Visibility;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.ArrayList;
import java.util.List;

/**
 * Représente une classe dans un diagramme UML
 */
@Entity
@Table(name = "class_elements")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@NoArgsConstructor
public class ClassElement extends DiagramElement {

    private String packageName;

    @Enumerated(EnumType.STRING)
    @Column(name = "element_type", nullable = false)
    private ClassType type = ClassType.CLASS;

    private boolean isAbstract;

    @OneToMany(mappedBy = "classElement", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Attribute> attributes = new ArrayList<>();

    @OneToMany(mappedBy = "classElement", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Method> methods = new ArrayList<>();

    // Style
    private String backgroundColor = "#FFFFFF";
    private String borderColor = "#000000";
    private double borderWidth = 1.0;

    public ClassElement(String name) {
        super(name);
    }

    /**
     * Types d'éléments de classe
     */
    public enum ClassType {
        CLASS,
        INTERFACE,
        ENUM,
        ABSTRACT_CLASS
    }

    /**
     * Ajoute un attribut à la classe
     */
    public void addAttribute(Attribute attribute) {
        attribute.setClassElement(this);
        attributes.add(attribute);
    }

    /**
     * Supprime un attribut de la classe
     */
    public void removeAttribute(Attribute attribute) {
        attributes.remove(attribute);
        attribute.setClassElement(null);
    }

    /**
     * Ajoute une méthode à la classe
     */
    public void addMethod(Method method) {
        method.setClassElement(this);
        methods.add(method);
    }

    /**
     * Supprime une méthode de la classe
     */
    public void removeMethod(Method method) {
        methods.remove(method);
        method.setClassElement(null);
    }
}