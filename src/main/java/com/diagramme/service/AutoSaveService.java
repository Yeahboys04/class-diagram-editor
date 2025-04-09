package com.diagramme.service;

import com.diagramme.model.ClassDiagram;
import com.diagramme.ui.DiagramEditorController;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import javafx.application.Platform;


/**
 * Service pour la sauvegarde automatique des diagrammes
 */
@Service
@Slf4j
public class AutoSaveService {

    private final DiagramService diagramService;
    private final PreferenceService preferenceService;

    private final Map<Long, Timer> autoSaveTimers = new ConcurrentHashMap<>();

    @Autowired
    public AutoSaveService(DiagramService diagramService, PreferenceService preferenceService) {
        this.diagramService = diagramService;
        this.preferenceService = preferenceService;
    }

    /**
     * Démarre la sauvegarde automatique pour un diagramme
     */
    public void startAutoSave(DiagramEditorController editorController) {
        if (!preferenceService.isAutoSaveEnabled()) {
            return;
        }

        ClassDiagram diagram = editorController.getDiagram();
        if (diagram == null || diagram.getId() == null) {
            return;
        }

        // Annuler le timer existant s'il y en a un
        stopAutoSave(diagram.getId());

        // Créer un nouveau timer
        Timer timer = new Timer(true);
        long interval = preferenceService.getAutoSaveInterval();

        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                if (editorController.hasUnsavedChanges()) {
                    try {
                        log.debug("Sauvegarde automatique du diagramme: {}", diagram.getName());
                        ClassDiagram savedDiagram = diagramService.saveDiagram(diagram);

                        // Utiliser Platform.runLater au lieu de getUiThreadExecutor
                        Platform.runLater(() -> {
                            editorController.setDiagram(savedDiagram);
                            editorController.setUnsavedChanges(false);
                            editorController.updateStatusMessage("Sauvegarde automatique effectuée");
                        });

                    } catch (Exception e) {
                        log.error("Erreur lors de la sauvegarde automatique", e);
                    }
                }
            }
        }, interval, interval);

        autoSaveTimers.put(diagram.getId(), timer);
        log.debug("Sauvegarde automatique démarrée pour le diagramme: {}", diagram.getName());
    }

    /**
     * Arrête la sauvegarde automatique pour un diagramme
     */
    public void stopAutoSave(Long diagramId) {
        Timer timer = autoSaveTimers.remove(diagramId);
        if (timer != null) {
            timer.cancel();
            log.debug("Sauvegarde automatique arrêtée pour le diagramme: {}", diagramId);
        }
    }

    /**
     * Arrête toutes les sauvegardes automatiques
     */
    public void stopAllAutoSaves() {
        for (Map.Entry<Long, Timer> entry : autoSaveTimers.entrySet()) {
            entry.getValue().cancel();
            log.debug("Sauvegarde automatique arrêtée pour le diagramme: {}", entry.getKey());
        }
        autoSaveTimers.clear();
    }
}
