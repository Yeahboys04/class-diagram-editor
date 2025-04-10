package com.diagramme.service;

import com.diagramme.dto.RecentDiagramDTO;
import com.diagramme.model.ClassDiagram;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service pour gérer les projets récents
 */
@Service
@Slf4j
public class RecentProjectsService {

    private final PreferenceService preferenceService;
    private final DiagramService diagramService;
    private final List<RecentDiagramDTO> recentProjects = new ArrayList<>();
    private final String recentProjectsFilePath;

    @Autowired
    public RecentProjectsService(PreferenceService preferenceService, @Lazy DiagramService diagramService) {
        this.preferenceService = preferenceService;
        this.diagramService = diagramService;

        // Chemin du fichier des projets récents
        String appDataPath = System.getProperty("user.home") + File.separator + ".class-diagram-editor";
        recentProjectsFilePath = appDataPath + File.separator + "recent_projects.ser";

        // Créer le répertoire si nécessaire
        File appDataDir = new File(appDataPath);
        if (!appDataDir.exists()) {
            appDataDir.mkdirs();
        }

        // Charger les projets récents
        loadRecentProjects();
    }

    /**
     * Charge la liste des projets récents
     */
    @SuppressWarnings("unchecked")
    private void loadRecentProjects() {
        File file = new File(recentProjectsFilePath);
        if (file.exists()) {
            try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file))) {
                List<RecentDiagramDTO> loadedProjects = (List<RecentDiagramDTO>) ois.readObject();
                recentProjects.clear();
                recentProjects.addAll(loadedProjects);
                log.debug("Projets récents chargés: {}", recentProjects.size());
            } catch (Exception e) {
                log.error("Erreur lors du chargement des projets récents", e);
            }
        }
    }

    /**
     * Enregistre la liste des projets récents
     */
    private void saveRecentProjects() {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(recentProjectsFilePath))) {
            oos.writeObject(recentProjects);
            log.debug("Projets récents enregistrés: {}", recentProjects.size());
        } catch (IOException e) {
            log.error("Erreur lors de l'enregistrement des projets récents", e);
        }
    }

    /**
     * Ajoute un projet à la liste des projets récents
     */
    @Transactional(readOnly = true)
    public void addRecentProject(ClassDiagram diagram) {
        // Convertir le diagramme en DTO
        RecentDiagramDTO dto = diagramService.convertToDTO(diagram);

        // Supprimer le projet s'il existe déjà
        recentProjects.removeIf(p -> p.getId().equals(dto.getId()));

        // Ajouter le projet en tête de liste
        recentProjects.add(0, dto);

        // Limiter le nombre de projets récents
        int maxRecentProjects = preferenceService.getRecentProjectsMax();
        while (recentProjects.size() > maxRecentProjects) {
            recentProjects.remove(recentProjects.size() - 1);
        }

        // Enregistrer la liste
        saveRecentProjects();
    }

    /**
     * Récupère la liste des projets récents
     */
    public List<RecentDiagramDTO> getRecentProjects() {
        return new ArrayList<>(recentProjects);
    }

    /**
     * Récupère la liste des projets récents (limitée)
     */
    public List<RecentDiagramDTO> getRecentProjects(int limit) {
        return recentProjects.stream()
                .limit(limit)
                .collect(Collectors.toList());
    }

    /**
     * Efface la liste des projets récents
     */
    public void clearRecentProjects() {
        recentProjects.clear();
        saveRecentProjects();
    }

    /**
     * Supprime un projet de la liste des projets récents
     */
    public void removeRecentProject(Long diagramId) {
        recentProjects.removeIf(p -> p.getId().equals(diagramId));
        saveRecentProjects();
    }
}