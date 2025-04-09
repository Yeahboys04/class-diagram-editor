package com.diagramme.service;

import com.diagramme.model.ClassDiagram;
import com.diagramme.model.ClassElement;
import com.diagramme.model.RelationshipElement;
import com.diagramme.repository.ClassDiagramRepository;
import com.diagramme.repository.ClassElementRepository;
import com.diagramme.repository.RelationshipElementRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.util.List;
import java.util.Optional;

/**
 * Implémentation du service de gestion des diagrammes
 */
@Service
@Slf4j
public class DiagramServiceImpl implements DiagramService {

    private final ClassDiagramRepository diagramRepository;
    private final ClassElementRepository classElementRepository;
    private final RelationshipElementRepository relationshipRepository;
    private final JavaParserService javaParserService;

    @Autowired
    public DiagramServiceImpl(
            ClassDiagramRepository diagramRepository,
            ClassElementRepository classElementRepository,
            RelationshipElementRepository relationshipRepository,
            JavaParserService javaParserService) {
        this.diagramRepository = diagramRepository;
        this.classElementRepository = classElementRepository;
        this.relationshipRepository = relationshipRepository;
        this.javaParserService = javaParserService;
    }

    @Override
    @Transactional
    public ClassDiagram saveDiagram(ClassDiagram diagram) {
        log.debug("Sauvegarde du diagramme: {}", diagram.getName());
        return diagramRepository.save(diagram);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ClassDiagram> getDiagramById(Long id) {
        log.debug("Récupération du diagramme par ID: {}", id);
        return diagramRepository.findById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ClassDiagram> getDiagramByUuid(String uuid) {
        log.debug("Récupération du diagramme par UUID: {}", uuid);
        return diagramRepository.findByUuid(uuid);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ClassDiagram> getAllDiagrams() {
        log.debug("Récupération de tous les diagrammes");
        return diagramRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ClassDiagram> getRecentDiagrams(int limit) {
        log.debug("Récupération des {} diagrammes récents", limit);
        if (limit <= 0) {
            return List.of();
        }

        // Si limit est 10 ou moins, utiliser la méthode prédéfinie
        if (limit <= 10) {
            return diagramRepository.findTop10ByOrderByModifiedAtDesc();
        }

        // Sinon, récupérer tous les diagrammes triés et limiter manuellement
        return diagramRepository.findAllByOrderByModifiedAtDesc().stream()
                .limit(limit)
                .toList();
    }

    @Override
    @Transactional
    public void deleteDiagram(Long id) {
        log.debug("Suppression du diagramme: {}", id);
        diagramRepository.deleteById(id);
    }

    @Override
    @Transactional
    public ClassDiagram createNewDiagram(String name) {
        log.debug("Création d'un nouveau diagramme: {}", name);
        ClassDiagram diagram = new ClassDiagram(name);
        return diagramRepository.save(diagram);
    }

    @Override
    @Transactional
    public ClassDiagram generateDiagramFromJavaSource(File sourceFile) {
        log.debug("Génération d'un diagramme à partir du fichier: {}", sourceFile.getName());
        ClassDiagram diagram = javaParserService.parseJavaFile(sourceFile);
        return diagramRepository.save(diagram);
    }

    @Override
    @Transactional
    public ClassDiagram generateDiagramFromJavaDirectory(File directory) {
        log.debug("Génération d'un diagramme à partir du répertoire: {}", directory.getName());
        ClassDiagram diagram = javaParserService.parseJavaDirectory(directory);
        return diagramRepository.save(diagram);
    }

    @Override
    @Transactional
    public ClassElement addClassElement(Long diagramId, ClassElement classElement) {
        log.debug("Ajout d'un élément de classe au diagramme: {}", diagramId);
        ClassDiagram diagram = diagramRepository.findById(diagramId)
                .orElseThrow(() -> new EntityNotFoundException("Diagramme non trouvé avec l'ID: " + diagramId));

        diagram.addElement(classElement);
        diagramRepository.save(diagram);
        return classElement;
    }

    @Override
    @Transactional
    public ClassElement updateClassElement(Long elementId, ClassElement updatedElement) {
        log.debug("Mise à jour de l'élément de classe: {}", elementId);
        ClassElement existingElement = classElementRepository.findById(elementId)
                .orElseThrow(() -> new EntityNotFoundException("Élément de classe non trouvé avec l'ID: " + elementId));

        // Mise à jour des propriétés
        existingElement.setName(updatedElement.getName());
        existingElement.setPackageName(updatedElement.getPackageName());
        existingElement.setType(updatedElement.getType());
        existingElement.setAbstract(updatedElement.isAbstract());
        existingElement.setX(updatedElement.getX());
        existingElement.setY(updatedElement.getY());
        existingElement.setWidth(updatedElement.getWidth());
        existingElement.setHeight(updatedElement.getHeight());
        existingElement.setBackgroundColor(updatedElement.getBackgroundColor());
        existingElement.setBorderColor(updatedElement.getBorderColor());
        existingElement.setBorderWidth(updatedElement.getBorderWidth());

        // Mise à jour des attributs et méthodes si nécessaire
        // Note: Dans l'application finale, il faudrait une logique plus complexe
        // pour gérer l'ajout/suppression/mise à jour des attributs et méthodes

        return classElementRepository.save(existingElement);
    }

    @Override
    @Transactional
    public void deleteClassElement(Long elementId) {
        log.debug("Suppression de l'élément de classe: {}", elementId);

        // Supprimer d'abord les relations impliquant cet élément
        List<RelationshipElement> relationships = relationshipRepository
                .findBySourceElementIdOrTargetElementId(elementId, elementId);
        relationshipRepository.deleteAll(relationships);

        // Ensuite supprimer l'élément de classe
        classElementRepository.deleteById(elementId);
    }

    @Override
    @Transactional
    public RelationshipElement addRelationship(Long diagramId, RelationshipElement relationship) {
        log.debug("Ajout d'une relation au diagramme: {}", diagramId);
        ClassDiagram diagram = diagramRepository.findById(diagramId)
                .orElseThrow(() -> new EntityNotFoundException("Diagramme non trouvé avec l'ID: " + diagramId));

        diagram.addElement(relationship);
        diagramRepository.save(diagram);
        return relationship;
    }

    @Override
    @Transactional
    public RelationshipElement updateRelationship(Long relationshipId, RelationshipElement updatedRelationship) {
        log.debug("Mise à jour de la relation: {}", relationshipId);
        RelationshipElement existingRelationship = relationshipRepository.findById(relationshipId)
                .orElseThrow(() -> new EntityNotFoundException("Relation non trouvée avec l'ID: " + relationshipId));

        // Mise à jour des propriétés
        existingRelationship.setName(updatedRelationship.getName());
        existingRelationship.setType(updatedRelationship.getType());
        existingRelationship.setSourceRole(updatedRelationship.getSourceRole());
        existingRelationship.setTargetRole(updatedRelationship.getTargetRole());
        existingRelationship.setSourceMultiplicity(updatedRelationship.getSourceMultiplicity());
        existingRelationship.setTargetMultiplicity(updatedRelationship.getTargetMultiplicity());
        existingRelationship.setSourceTooltip(updatedRelationship.getSourceTooltip());
        existingRelationship.setTargetTooltip(updatedRelationship.getTargetTooltip());
        existingRelationship.setLineColor(updatedRelationship.getLineColor());
        existingRelationship.setLineWidth(updatedRelationship.getLineWidth());
        existingRelationship.setLineStyle(updatedRelationship.getLineStyle());

        // Si les points de contrôle sont modifiés
        if (updatedRelationship.getControlPoints() != null) {
            existingRelationship.setControlPoints(updatedRelationship.getControlPoints());
        }

        return relationshipRepository.save(existingRelationship);
    }

    @Override
    @Transactional
    public void deleteRelationship(Long relationshipId) {
        log.debug("Suppression de la relation: {}", relationshipId);
        relationshipRepository.deleteById(relationshipId);
    }

    @Override
    @Transactional
    public ClassDiagram optimizeLayout(Long diagramId) {
        log.debug("Optimisation de la disposition du diagramme: {}", diagramId);
        ClassDiagram diagram = diagramRepository.findById(diagramId)
                .orElseThrow(() -> new EntityNotFoundException("Diagramme non trouvé avec l'ID: " + diagramId));

        // Algorithme simple d'optimisation de la disposition
        // Dans une application réelle, utilisez un algorithme plus sophistiqué
        double x = 50;
        double y = 50;
        double maxHeight = 0;
        double horizontalGap = 50;
        double verticalGap = 50;

        for (ClassElement element : diagram.getClasses()) {
            // Si l'élément dépasse la largeur disponible, passer à la ligne suivante
            if (x > 800) {
                x = 50;
                y += maxHeight + verticalGap;
                maxHeight = 0;
            }

            // Positionner l'élément
            element.setX(x);
            element.setY(y);

            // Mettre à jour les variables pour le prochain élément
            x += element.getWidth() + horizontalGap;
            maxHeight = Math.max(maxHeight, element.getHeight());
        }

        return diagramRepository.save(diagram);
    }
}
