package com.diagramme.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

/**
 * Service pour gérer les préférences de l'application
 */
@Service
@Slf4j
public class PreferenceService {

    private final Properties preferences = new Properties();
    private final String preferencesFilePath;

    @Autowired
    public PreferenceService(Environment env) {
        // Chemin du fichier de préférences
        String appDataPath = System.getProperty("user.home") + File.separator + ".class-diagram-editor";
        preferencesFilePath = appDataPath + File.separator + "preferences.properties";

        // Créer le répertoire si nécessaire
        try {
            Path appDataDir = Paths.get(appDataPath);
            if (!Files.exists(appDataDir)) {
                Files.createDirectories(appDataDir);
            }
        } catch (IOException e) {
            log.error("Erreur lors de la création du répertoire des préférences", e);
        }

        // Charger les préférences
        loadPreferences();

        // Définir les valeurs par défaut si nécessaires
        setDefaultIfMissing("theme", "light");
        setDefaultIfMissing("autosave.enabled", "true");
        setDefaultIfMissing("autosave.interval", "300000");
        setDefaultIfMissing("recent.max", "10");
    }

    /**
     * Charge les préférences depuis le fichier
     */
    private void loadPreferences() {
        File file = new File(preferencesFilePath);
        if (file.exists()) {
            try (FileInputStream fis = new FileInputStream(file)) {
                preferences.load(fis);
                log.debug("Préférences chargées depuis: {}", preferencesFilePath);
            } catch (IOException e) {
                log.error("Erreur lors du chargement des préférences", e);
            }
        } else {
            log.debug("Fichier de préférences non trouvé, création d'un nouveau fichier");
            savePreferences();
        }
    }

    /**
     * Enregistre les préférences dans le fichier
     */
    private void savePreferences() {
        try (FileOutputStream fos = new FileOutputStream(preferencesFilePath)) {
            preferences.store(fos, "Préférences de l'éditeur de diagrammes de classe");
            log.debug("Préférences enregistrées dans: {}", preferencesFilePath);
        } catch (IOException e) {
            log.error("Erreur lors de l'enregistrement des préférences", e);
        }
    }

    /**
     * Définit une valeur par défaut si la préférence n'existe pas
     */
    private void setDefaultIfMissing(String key, String defaultValue) {
        if (!preferences.containsKey(key)) {
            preferences.setProperty(key, defaultValue);
            savePreferences();
        }
    }

    /**
     * Récupère une préférence sous forme de chaîne de caractères
     */
    public String getString(String key, String defaultValue) {
        return preferences.getProperty(key, defaultValue);
    }

    /**
     * Définit une préférence sous forme de chaîne de caractères
     */
    public void setString(String key, String value) {
        preferences.setProperty(key, value);
        savePreferences();
    }

    /**
     * Récupère une préférence sous forme de booléen
     */
    public boolean getBoolean(String key, boolean defaultValue) {
        String value = preferences.getProperty(key);
        return value != null ? Boolean.parseBoolean(value) : defaultValue;
    }

    /**
     * Définit une préférence sous forme de booléen
     */
    public void setBoolean(String key, boolean value) {
        preferences.setProperty(key, String.valueOf(value));
        savePreferences();
    }

    /**
     * Récupère une préférence sous forme d'entier
     */
    public int getInt(String key, int defaultValue) {
        String value = preferences.getProperty(key);
        if (value != null) {
            try {
                return Integer.parseInt(value);
            } catch (NumberFormatException e) {
                log.warn("Valeur incorrecte pour la préférence {}: {}", key, value);
            }
        }
        return defaultValue;
    }

    /**
     * Définit une préférence sous forme d'entier
     */
    public void setInt(String key, int value) {
        preferences.setProperty(key, String.valueOf(value));
        savePreferences();
    }

    /**
     * Récupère le thème
     */
    public String getTheme() {
        return getString("theme", "light");
    }

    /**
     * Définit le thème
     */
    public void setTheme(String theme) {
        setString("theme", theme);
    }

    /**
     * Récupère le dernier répertoire d'ouverture
     */
    public String getLastOpenDirectory() {
        return getString("last.open.directory", "");
    }

    /**
     * Définit le dernier répertoire d'ouverture
     */
    public void setLastOpenDirectory(String directory) {
        setString("last.open.directory", directory);
    }

    /**
     * Récupère le dernier répertoire d'enregistrement
     */
    public String getLastSaveDirectory() {
        return getString("last.save.directory", "");
    }

    /**
     * Définit le dernier répertoire d'enregistrement
     */
    public void setLastSaveDirectory(String directory) {
        setString("last.save.directory", directory);
    }

    /**
     * Vérifie si la sauvegarde automatique est activée
     */
    public boolean isAutoSaveEnabled() {
        return getBoolean("autosave.enabled", true);
    }

    /**
     * Définit si la sauvegarde automatique est activée
     */
    public void setAutoSaveEnabled(boolean enabled) {
        setBoolean("autosave.enabled", enabled);
    }

    /**
     * Récupère l'intervalle de sauvegarde automatique (en millisecondes)
     */
    public int getAutoSaveInterval() {
        return getInt("autosave.interval", 300000); // 5 minutes par défaut
    }

    /**
     * Définit l'intervalle de sauvegarde automatique (en millisecondes)
     */
    public void setAutoSaveInterval(int interval) {
        setInt("autosave.interval", interval);
    }

    /**
     * Récupère le nombre maximum de projets récents
     */
    public int getRecentProjectsMax() {
        return getInt("recent.max", 10);
    }

    /**
     * Définit le nombre maximum de projets récents
     */
    public void setRecentProjectsMax(int max) {
        setInt("recent.max", max);
    }
}
