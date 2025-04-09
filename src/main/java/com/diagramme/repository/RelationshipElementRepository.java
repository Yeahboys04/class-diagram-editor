package com.diagramme.repository;

import com.diagramme.model.ClassElement;
import com.diagramme.model.RelationshipElement;
import com.diagramme.model.enums.RelationshipType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository pour gérer les relations entre classes
 */
@Repository
public interface RelationshipElementRepository extends JpaRepository<RelationshipElement, Long> {

    /**
     * Trouve toutes les relations dans un diagramme
     */
    List<RelationshipElement> findByDiagramId(Long diagramId);

    /**
     * Trouve toutes les relations d'un certain type
     */
    List<RelationshipElement> findByDiagramIdAndType(Long diagramId, RelationshipType type);

    /**
     * Trouve toutes les relations impliquant une classe spécifique (source ou cible)
     */
    List<RelationshipElement> findBySourceElementIdOrTargetElementId(Long sourceId, Long targetId);

    /**
     * Trouve une relation entre deux classes spécifiques
     */
    List<RelationshipElement> findBySourceElementIdAndTargetElementId(Long sourceId, Long targetId);
}