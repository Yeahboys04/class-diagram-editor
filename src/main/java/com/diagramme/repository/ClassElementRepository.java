package com.diagramme.repository;

import com.diagramme.model.ClassElement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository pour gérer les éléments de classe
 */
@Repository
public interface ClassElementRepository extends JpaRepository<ClassElement, Long> {

    /**
     * Trouve tous les éléments appartenant à un diagramme
     */
    List<ClassElement> findByDiagramId(Long diagramId);

    /**
     * Trouve tous les éléments d'un certain type dans un diagramme
     */
    List<ClassElement> findByDiagramIdAndType(Long diagramId, ClassElement.ClassType type);

    /**
     * Trouve tous les éléments avec un nom spécifique (pour vérifier les doublons)
     */
    List<ClassElement> findByDiagramIdAndNameContainingIgnoreCase(Long diagramId, String name);
}

