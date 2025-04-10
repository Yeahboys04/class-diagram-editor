package com.diagramme.service;

import com.diagramme.dto.RecentDiagramDTO;
import com.diagramme.model.ClassDiagram;
import com.diagramme.model.ClassElement;
import com.diagramme.model.RelationshipElement;

import java.io.File;
import java.util.List;
import java.util.Optional;

/**
 * Interface pour le service de gestion des diagrammes
 */
public interface DiagramService {

    /**
     * Sauvegarde un diagramme
     */
    ClassDiagram saveDiagram(ClassDiagram diagram);

    /**
     * Récupère un diagramme par son ID
     */
    Optional<ClassDiagram> getDiagramById(Long id);

    /**
     * Récupère un diagramme par son UUID
     */
    Optional<ClassDiagram> getDiagramByUuid(String uuid);

    /**
     * Récupère tous les diagrammes
     */
    List<ClassDiagram> getAllDiagrams();

    /**
     * Récupère les diagrammes récemment modifiés
     */
    List<ClassDiagram> getRecentDiagrams(int limit);

    /**
     * Récupère les diagrammes récents sous forme de DTOs
     */
    List<RecentDiagramDTO> getRecentDiagramDTOs(int limit);

    /**
     * Supprime un diagramme
     */
    void deleteDiagram(Long id);

    /**
     * Crée un nouveau diagramme vide
     */
    ClassDiagram createNewDiagram(String name);

    /**
     * Génère un diagramme à partir de code source Java
     */
    ClassDiagram generateDiagramFromJavaSource(File sourceFile);

    /**
     * Génère un diagramme à partir d'un répertoire de code source Java
     */
    ClassDiagram generateDiagramFromJavaDirectory(File directory);

    /**
     * Ajoute un élément de classe au diagramme
     */
    ClassElement addClassElement(Long diagramId, ClassElement classElement);

    /**
     * Met à jour un élément de classe
     */
    ClassElement updateClassElement(Long elementId, ClassElement classElement);

    /**
     * Supprime un élément de classe
     */
    void deleteClassElement(Long elementId);

    /**
     * Ajoute une relation au diagramme
     */
    RelationshipElement addRelationship(Long diagramId, RelationshipElement relationship);

    /**
     * Met à jour une relation
     */
    RelationshipElement updateRelationship(Long relationshipId, RelationshipElement relationship);

    /**
     * Supprime une relation
     */
    void deleteRelationship(Long relationshipId);

    /**
     * Optimise la disposition des éléments dans le diagramme
     */
    ClassDiagram optimizeLayout(Long diagramId);

    /**
     * Convertit un diagramme en DTO
     */
    RecentDiagramDTO convertToDTO(ClassDiagram diagram);

    /**
     * Récupère un diagramme avec tous ses éléments chargés
     * @param id L'identifiant du diagramme
     * @return Le diagramme complet avec toutes ses associations
     */
    Optional<ClassDiagram> getDiagramWithElementsById(Long id);
}