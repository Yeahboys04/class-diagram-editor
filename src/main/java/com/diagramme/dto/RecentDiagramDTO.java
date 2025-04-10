package com.diagramme.dto;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * DTO pour représenter un diagramme de classe récent dans l'interface utilisateur
 * sans avoir à charger toutes les relations lazy
 */
public class RecentDiagramDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;
    private String uuid;
    private String name;
    private String description;
    private String author;
    private String version;
    private transient LocalDateTime createdAt;  // transient car LocalDateTime n'est pas Serializable par défaut
    private transient LocalDateTime modifiedAt; // transient car LocalDateTime n'est pas Serializable par défaut
    private int elementCount;
    private boolean showGrid;
    private boolean snapToGrid;
    private double gridSize;
    private String backgroundColor;

    // Constructeur par défaut
    public RecentDiagramDTO() {}

    // Méthodes de sérialisation personnalisées pour gérer LocalDateTime
    private void writeObject(ObjectOutputStream out) throws IOException {
        out.defaultWriteObject();
        // Écrire les dates en tant que chaînes
        out.writeObject(createdAt != null ? createdAt.toString() : null);
        out.writeObject(modifiedAt != null ? modifiedAt.toString() : null);
    }

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        // Lire et reconstruire les dates
        String createdAtStr = (String) in.readObject();
        String modifiedAtStr = (String) in.readObject();

        this.createdAt = createdAtStr != null ? LocalDateTime.parse(createdAtStr) : null;
        this.modifiedAt = modifiedAtStr != null ? LocalDateTime.parse(modifiedAtStr) : null;
    }

    // Getters et Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getModifiedAt() {
        return modifiedAt;
    }

    public void setModifiedAt(LocalDateTime modifiedAt) {
        this.modifiedAt = modifiedAt;
    }

    public int getElementCount() {
        return elementCount;
    }

    public void setElementCount(int elementCount) {
        this.elementCount = elementCount;
    }

    public boolean isShowGrid() {
        return showGrid;
    }

    public void setShowGrid(boolean showGrid) {
        this.showGrid = showGrid;
    }

    public boolean isSnapToGrid() {
        return snapToGrid;
    }

    public void setSnapToGrid(boolean snapToGrid) {
        this.snapToGrid = snapToGrid;
    }

    public double getGridSize() {
        return gridSize;
    }

    public void setGridSize(double gridSize) {
        this.gridSize = gridSize;
    }

    public String getBackgroundColor() {
        return backgroundColor;
    }

    public void setBackgroundColor(String backgroundColor) {
        this.backgroundColor = backgroundColor;
    }
}