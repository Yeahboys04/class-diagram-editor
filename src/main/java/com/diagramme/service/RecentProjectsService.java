package com.diagramme.service;

import com.diagramme.model.ClassDiagram;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

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
    private final List<ClassDiagram> recentProjects = new ArrayList<>();
    private final String recentProjectsFilePath;

    @Autowired
    public RecentProjectsService(PreferenceService preferenceService) {
        this.preferenceService = preferenceService;

        // Chemin du fichier des projets récents
        String appDataPath = System.getProperty("user.home") + File.separator + ".class-diagram-editor";
        recentProjectsFilePath = appDataPath + File.separator + "recent_projects.ser";

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
                List<ClassDiagram> loadedProjects = (List<ClassDiagram>) ois.readObject();
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
    public void addRecentProject(ClassDiagram diagram) {
        // Supprimer le projet s'il existe déjà
        recentProjects.removeIf(p -> p.getId().equals(diagram.getId()));

        // Ajouter le projet en tête de liste
        recentProjects.add(0, diagram);

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
    public List<ClassDiagram> getRecentProjects() {
        return new ArrayList<>(recentProjects);
    }

    /**
     * Récupère la liste des projets récents (limitée)
     */
    public List<ClassDiagram> getRecentProjects(int limit) {
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