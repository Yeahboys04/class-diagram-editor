package com.diagramme.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * Représente un paramètre de méthode
 */
@Entity
@Table(name = "parameters")
@Data
@NoArgsConstructor
public class Parameter {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String type;

    private String defaultValue;

    @ManyToOne
    @JoinColumn(name = "method_id")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Method method;

    public Parameter(String name, String type) {
        this.name = name;
        this.type = type;
    }
}