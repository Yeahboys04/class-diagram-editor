package com.diagramme.repository;

import com.diagramme.model.ClassDiagram;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository pour gérer les diagrammes de classe en base de données
 */
@Repository
public interface ClassDiagramRepository extends JpaRepository<ClassDiagram, Long> {

    /**
     * Trouve un diagramme par son UUID
     */
    Optional<ClassDiagram> findByUuid(String uuid);

    /**
     * Cherche des diagrammes par nom (recherche approximative)
     */
    List<ClassDiagram> findByNameContainingIgnoreCase(String name);

    /**
     * Récupère tous les diagrammes triés par date de modification
     */
    List<ClassDiagram> findAllByOrderByModifiedAtDesc();

    /**
     * Récupère les N diagrammes les plus récemment modifiés
     */
    List<ClassDiagram> findTop10ByOrderByModifiedAtDesc();
}
