package com.diagramme.util;

import javafx.stage.FileChooser;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Utilitaires pour la gestion des fichiers
 */
public class FileUtils {

    /**
     * Vérifie si un répertoire contient des fichiers Java
     */
    public static boolean containsJavaFiles(File directory) {
        if (!directory.isDirectory()) {
            return false;
        }

        File[] files = directory.listFiles();
        if (files == null) {
            return false;
        }

        for (File file : files) {
            if (file.isFile() && file.getName().endsWith(".java")) {
                return true;
            } else if (file.isDirectory() && containsJavaFiles(file)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Collecte tous les fichiers Java dans un répertoire et ses sous-répertoires
     */
    public static List<File> collectJavaFiles(File directory) {
        List<File> javaFiles = new ArrayList<>();

        if (!directory.isDirectory()) {
            return javaFiles;
        }

        File[] files = directory.listFiles();
        if (files == null) {
            return javaFiles;
        }

        for (File file : files) {
            if (file.isFile() && file.getName().endsWith(".java")) {
                javaFiles.add(file);
            } else if (file.isDirectory()) {
                javaFiles.addAll(collectJavaFiles(file));
            }
        }

        return javaFiles;
    }

    /**
     * Configure les filtres pour les fichiers de diagramme
     */
    public static void setupDiagramFileFilters(FileChooser fileChooser) {
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Tous les diagrammes", "*.json", "*.xmi", "*.cld"),
                new FileChooser.ExtensionFilter("Diagrammes JSON", "*.json"),
                new FileChooser.ExtensionFilter("Diagrammes XMI", "*.xmi"),
                new FileChooser.ExtensionFilter("Diagrammes personnalisés", "*.cld"),
                new FileChooser.ExtensionFilter("Tous les fichiers", "*.*")
        );
    }

    /**
     * Vérifie si une extension de fichier est celle d'un diagramme
     */
    public static boolean isDiagramFileExtension(String filename) {
        String lowercase = filename.toLowerCase();
        return lowercase.endsWith(".json") || lowercase.endsWith(".xmi") || lowercase.endsWith(".cld");
    }

    /**
     * Ajoute l'extension appropriée à un nom de fichier si nécessaire
     */
    public static String ensureFileExtension(String filename, String defaultExtension) {
        if (!isDiagramFileExtension(filename)) {
            return filename + "." + defaultExtension;
        }
        return filename;
    }
}