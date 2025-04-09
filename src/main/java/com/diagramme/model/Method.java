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
 * Représente une méthode d'une classe
 */
@Entity
@Table(name = "methods")
@Data
@NoArgsConstructor
public class Method {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    private String returnType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Visibility visibility = Visibility.PUBLIC;

    private boolean isStatic;
    private boolean isAbstract;
    private boolean isFinal;

    @OneToMany(mappedBy = "method", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Parameter> parameters = new ArrayList<>();

    @ManyToOne
    @JoinColumn(name = "class_id")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private ClassElement classElement;

    public Method(String name) {
        this.name = name;
    }

    public Method(String name, String returnType) {
        this.name = name;
        this.returnType = returnType;
    }

    /**
     * Ajoute un paramètre à la méthode
     */
    public void addParameter(Parameter parameter) {
        parameter.setMethod(this);
        parameters.add(parameter);
    }

    /**
     * Supprime un paramètre de la méthode
     */
    public void removeParameter(Parameter parameter) {
        parameters.remove(parameter);
        parameter.setMethod(null);
    }
}
