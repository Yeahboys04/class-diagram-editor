package com.diagramme;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

import java.io.IOException;
import java.util.Objects;

@Slf4j
@SpringBootApplication
public class DiagrammeDesktopApplication extends Application {

    private ConfigurableApplicationContext springContext;
    private Parent rootNode;

    public static void main(String[] args) {
        // Lancement de l'application JavaFX
        launch(args);
    }

    @Override
    public void init() throws Exception {
        log.info("Initialisation de l'application...");

        // Démarrer le contexte Spring
        springContext = SpringApplication.run(DiagrammeDesktopApplication.class);

        // Charger l'interface FXML principale
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/fxml/main.fxml"));
        fxmlLoader.setControllerFactory(springContext::getBean);

        try {
            rootNode = fxmlLoader.load();
            log.info("Interface principale chargée avec succès");
        } catch (IOException e) {
            log.error("Erreur lors du chargement de l'interface principale", e);
            throw e;
        }
    }

    @Override
    public void start(Stage primaryStage) {
        try {
            log.info("Démarrage de l'interface utilisateur...");

            // Configurer la scène principale
            Scene scene = new Scene(rootNode, 1280, 800);
            scene.getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());

            // Configurer la fenêtre principale
            primaryStage.setTitle("Éditeur de Diagrammes de Classe");
            primaryStage.setScene(scene);

            // Charger l'icône de l'application
            try {
                Image appIcon = new Image(Objects.requireNonNull(
                        getClass().getResourceAsStream("/images/app_icon.png")));
                primaryStage.getIcons().add(appIcon);
            } catch (Exception e) {
                log.warn("Impossible de charger l'icône de l'application", e);
            }

            // Gérer la fermeture de l'application
            primaryStage.setOnCloseRequest(event -> {
                log.info("Fermeture de l'application demandée par l'utilisateur");
                stop();
                Platform.exit();
            });

            // Afficher la fenêtre
            primaryStage.show();
            log.info("Interface utilisateur démarrée avec succès");

        } catch (Exception e) {
            log.error("Erreur lors du démarrage de l'interface utilisateur", e);
        }
    }

    @Override
    public void stop() {
        log.info("Fermeture de l'application...");
        if (springContext != null) {
            springContext.close();
            log.info("Contexte Spring fermé");
        }
    }
}
