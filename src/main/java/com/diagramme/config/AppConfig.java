package com.diagramme.config;

import javafx.fxml.FXMLLoader;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.env.Environment;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.boot.autoconfigure.domain.EntityScan;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
@EnableJpaRepositories("com.diagramme.repository")
@EntityScan("com.diagramme.model")
public class AppConfig {

    private final Environment env;

    public AppConfig(Environment env) {
        this.env = env;
        initializeAppDirectories();
    }

    @Bean
    @Lazy
    public FXMLLoader fxmlLoader() {
        return new FXMLLoader();
    }

    /**
     * Crée les répertoires nécessaires au fonctionnement de l'application
     */
    private void initializeAppDirectories() {
        // Créer le répertoire pour les données
        createDirectory("./data");

        // Créer le répertoire pour les diagrammes
        String diagramPath = env.getProperty("app.diagram.storage.path", "./diagrams");
        createDirectory(diagramPath);

        // Créer le répertoire pour les logs
        createDirectory("./logs");
    }

    private void createDirectory(String path) {
        try {
            Path dirPath = Paths.get(path);
            if (!Files.exists(dirPath)) {
                Files.createDirectories(dirPath);
            }
        } catch (Exception e) {
            throw new RuntimeException("Impossible de créer le répertoire: " + path, e);
        }
    }

    @Bean
    public String applicationName() {
        return env.getProperty("app.name", "Éditeur de Diagrammes de Classe");
    }

    @Bean
    public String applicationVersion() {
        return env.getProperty("app.version", "1.0.0");
    }
}