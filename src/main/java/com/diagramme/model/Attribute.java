package com.diagramme.model;

import com.diagramme.model.enums.Visibility;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * Représente un attribut d'une classe
 */
@Entity
@Table(name = "attributes")
@Data
@NoArgsConstructor
public class Attribute {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String type;

    private String defaultValue;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Visibility visibility = Visibility.PRIVATE;

    private boolean isStatic;
    private boolean isFinal;

    @ManyToOne
    @JoinColumn(name = "class_id")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private ClassElement classElement;

    public Attribute(String name, String type) {
        this.name = name;
        this.type = type;
    }

    public Attribute(String name, String type, Visibility visibility) {
        this.name = name;
        this.type = type;
        this.visibility = visibility;
    }
}
