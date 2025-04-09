package com.diagramme.ui;

import com.diagramme.model.ClassDiagram;
import com.diagramme.service.DiagramService;
import com.diagramme.service.ExportService;
import com.diagramme.service.ImportService;
import com.diagramme.service.PreferenceService;
import com.diagramme.ui.dialog.AboutDialog;
import com.diagramme.ui.dialog.DiagramPropertiesDialog;
import com.diagramme.ui.dialog.ExportDialog;
import com.diagramme.ui.dialog.PreferencesDialog;
import com.diagramme.util.AlertUtils;
import com.diagramme.util.FileUtils;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Optional;

import javafx.scene.Scene;

/**
 * Contrôleur principal de l'application
 */
@Component
@Slf4j
public class MainController {

    @FXML private MenuBar menuBar;
    @FXML private TabPane diagramTabPane;
    @FXML private VBox propertiesPanel;
    @FXML private VBox propertiesContent;
    @FXML private TreeView<String> diagramExplorer;
    @FXML private Label statusLabel;
    @FXML private Label zoomLabel;
    @FXML private Menu recentProjectsMenu;

    @FXML private MenuItem undoMenuItem;
    @FXML private MenuItem redoMenuItem;
    @FXML private MenuItem saveMenuItem;
    @FXML private MenuItem saveAsMenuItem;
    @FXML private MenuItem exportMenuItem;
    @FXML private MenuItem closeMenuItem;

    @FXML private Button newDiagramButton;
    @FXML private Button openDiagramButton;
    @FXML private Button saveDiagramButton;
    @FXML private Button undoButton;
    @FXML private Button redoButton;

    @FXML private ToggleGroup themeToggleGroup;

    private final ApplicationContext applicationContext;
    private final DiagramService diagramService;
    private final ImportService importService;
    private final ExportService exportService;
    private final PreferenceService preferenceService;

    // Propriétés de l'application
    private final BooleanProperty hasOpenDiagram = new SimpleBooleanProperty(false);
    private final BooleanProperty hasUnsavedChanges = new SimpleBooleanProperty(false);
    /**
     * -- SETTER --
     *  Définit la scène principale
     */
    @Setter
    private Stage primaryStage;

    @Autowired
    public MainController(
            ApplicationContext applicationContext,
            DiagramService diagramService,
            ImportService importService,
            ExportService exportService,
            PreferenceService preferenceService) {
        this.applicationContext = applicationContext;
        this.diagramService = diagramService;
        this.importService = importService;
        this.exportService = exportService;
        this.preferenceService = preferenceService;
    }

    /**
     * Initialisation du contrôleur
     */
    @FXML
    public void initialize() {
        log.debug("Initialisation du contrôleur principal");

        // Initialiser l'explorateur de diagrammes
        initializeExplorer();

        // Initialiser les projets récents
        loadRecentProjects();

        // Configurer les états des contrôles
        setupControlBindings();

        // Configurer les auditeurs d'événements
        diagramTabPane.getSelectionModel().selectedItemProperty().addListener((obs, oldTab, newTab) -> {
            updatePropertiesPanel();
            hasOpenDiagram.set(newTab != null);
        });

        // Définir l'état initial
        hasOpenDiagram.set(false);
        hasUnsavedChanges.set(false);

        // État initial de la barre de statut
        statusLabel.setText("Prêt");
    }

    /**
     * Configurer les liaisons des propriétés avec les contrôles
     */
    private void setupControlBindings() {
        // Activer/désactiver les menus selon l'état
        undoMenuItem.disableProperty().bind(hasOpenDiagram.not());
        redoMenuItem.disableProperty().bind(hasOpenDiagram.not());
        saveMenuItem.disableProperty().bind(hasOpenDiagram.not().or(hasUnsavedChanges.not()));
        saveAsMenuItem.disableProperty().bind(hasOpenDiagram.not());
        exportMenuItem.disableProperty().bind(hasOpenDiagram.not());
        closeMenuItem.disableProperty().bind(hasOpenDiagram.not());

        // Activer/désactiver les boutons selon l'état
        undoButton.disableProperty().bind(hasOpenDiagram.not());
        redoButton.disableProperty().bind(hasOpenDiagram.not());
        saveDiagramButton.disableProperty().bind(hasOpenDiagram.not().or(hasUnsavedChanges.not()));
    }

    /**
     * Initialise l'explorateur de diagrammes
     */
    private void initializeExplorer() {
        TreeItem<String> rootItem = new TreeItem<>("Diagrammes");
        rootItem.setExpanded(true);

        // Charger les diagrammes existants
        List<ClassDiagram> diagrams = diagramService.getAllDiagrams();
        for (ClassDiagram diagram : diagrams) {
            TreeItem<String> diagramItem = new TreeItem<>(diagram.getName());
            diagramItem.setExpanded(false);
            rootItem.getChildren().add(diagramItem);
        }

        diagramExplorer.setRoot(rootItem);
        diagramExplorer.setShowRoot(true);

        // Ajouter un gestionnaire de clic double pour ouvrir un diagramme
        diagramExplorer.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                TreeItem<String> selectedItem = diagramExplorer.getSelectionModel().getSelectedItem();
                if (selectedItem != null && selectedItem != rootItem) {
                    String diagramName = selectedItem.getValue();
                    List<ClassDiagram> selectedDiagrams = diagramService.getAllDiagrams().stream()
                            .filter(d -> d.getName().equals(diagramName))
                            .toList();

                    if (!selectedDiagrams.isEmpty()) {
                        openDiagramInTab(selectedDiagrams.get(0));
                    }
                }
            }
        });
    }

    /**
     * Charge les projets récents dans le menu
     */
    private void loadRecentProjects() {
        // Vider le menu des projets récents
        recentProjectsMenu.getItems().clear();

        // Charger les diagrammes récents
        List<ClassDiagram> recentDiagrams = diagramService.getRecentDiagrams(10);

        if (recentDiagrams.isEmpty()) {
            MenuItem emptyItem = new MenuItem("Aucun projet récent");
            emptyItem.setDisable(true);
            recentProjectsMenu.getItems().add(emptyItem);
        } else {
            for (ClassDiagram diagram : recentDiagrams) {
                MenuItem item = new MenuItem(diagram.getName());
                item.setOnAction(event -> openDiagramInTab(diagram));
                recentProjectsMenu.getItems().add(item);
            }

            recentProjectsMenu.getItems().add(new SeparatorMenuItem());
            MenuItem clearItem = new MenuItem("Effacer la liste");
            clearItem.setOnAction(event -> clearRecentProjects());
            recentProjectsMenu.getItems().add(clearItem);
        }
    }

    /**
     * Efface la liste des projets récents
     */
    private void clearRecentProjects() {
        // Cette fonctionnalité dépend de l'implémentation
        // On se contente ici de vider le menu
        recentProjectsMenu.getItems().clear();
        MenuItem emptyItem = new MenuItem("Aucun projet récent");
        emptyItem.setDisable(true);
        recentProjectsMenu.getItems().add(emptyItem);
    }

    /**
     * Met à jour le panneau des propriétés selon l'élément sélectionné
     */
    void updatePropertiesPanel() {
        // Vider le panneau des propriétés
        propertiesContent.getChildren().clear();

        // Obtenir l'onglet actif
        Tab selectedTab = diagramTabPane.getSelectionModel().getSelectedItem();
        if (selectedTab == null) {
            return;
        }

        // Obtenir le contrôleur de l'éditeur
        DiagramEditorController editorController = (DiagramEditorController) selectedTab.getUserData();
        if (editorController == null) {
            return;
        }

        // Demander au contrôleur d'éditeur de mettre à jour le panneau des propriétés
        editorController.updatePropertiesPanel(propertiesContent);
    }

    /**
     * Ouvre un diagramme dans un nouvel onglet
     */
    private void openDiagramInTab(ClassDiagram diagram) {
        try {
            // Vérifier si le diagramme est déjà ouvert
            for (Tab tab : diagramTabPane.getTabs()) {
                DiagramEditorController controller = (DiagramEditorController) tab.getUserData();
                if (controller != null && controller.getDiagram().getId().equals(diagram.getId())) {
                    // Sélectionner l'onglet existant
                    diagramTabPane.getSelectionModel().select(tab);
                    return;
                }
            }

            // Charger l'éditeur de diagramme
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/diagram_editor.fxml"));
            loader.setControllerFactory(applicationContext::getBean);

            // Créer un nouvel onglet
            Tab tab = new Tab(diagram.getName());
            tab.setContent(loader.load());

            // Configurer le contrôleur
            DiagramEditorController controller = loader.getController();
            controller.setDiagram(diagram);
            controller.setMainController(this);

            // Stocker une référence au contrôleur dans l'onglet
            tab.setUserData(controller);

            // Gérer la fermeture de l'onglet
            tab.setOnCloseRequest(event -> {
                if (controller.hasUnsavedChanges()) {
                    Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
                    alert.setTitle("Fermeture de l'onglet");
                    alert.setHeaderText("Modifications non enregistrées");
                    alert.setContentText("Voulez-vous enregistrer les modifications avant de fermer?");

                    ButtonType buttonSave = new ButtonType("Enregistrer");
                    ButtonType buttonDontSave = new ButtonType("Ne pas enregistrer");
                    ButtonType buttonCancel = new ButtonType("Annuler", ButtonBar.ButtonData.CANCEL_CLOSE);

                    alert.getButtonTypes().setAll(buttonSave, buttonDontSave, buttonCancel);

                    Optional<ButtonType> result = alert.showAndWait();
                    if (result.isPresent()) {
                        if (result.get() == buttonSave) {
                            saveDiagram(controller);
                        } else if (result.get() == buttonCancel) {
                            event.consume();
                        }
                    }
                }
            });

            // Ajouter l'onglet et le sélectionner
            diagramTabPane.getTabs().add(tab);
            diagramTabPane.getSelectionModel().select(tab);

            // Mettre à jour les propriétés de l'application
            hasOpenDiagram.set(true);
            updateStatusMessage("Diagramme " + diagram.getName() + " ouvert");

        } catch (IOException e) {
            log.error("Erreur lors de l'ouverture du diagramme", e);
            AlertUtils.showErrorDialog("Ouverture du diagramme",
                    "Erreur lors de l'ouverture du diagramme",
                    e.getMessage());
        }
    }

    /**
     * Crée un nouveau diagramme
     */
    @FXML
    private void onNewDiagram() {
        TextInputDialog dialog = new TextInputDialog("Nouveau diagramme");
        dialog.setTitle("Nouveau diagramme");
        dialog.setHeaderText("Créer un nouveau diagramme de classe");
        dialog.setContentText("Nom du diagramme:");

        Optional<String> result = dialog.showAndWait();
        result.ifPresent(name -> {
            if (!name.trim().isEmpty()) {
                ClassDiagram diagram = diagramService.createNewDiagram(name);
                openDiagramInTab(diagram);

                // Mettre à jour l'explorateur
                TreeItem<String> rootItem = diagramExplorer.getRoot();
                TreeItem<String> diagramItem = new TreeItem<>(diagram.getName());
                rootItem.getChildren().add(diagramItem);
            }
        });
    }

    /**
     * Ouvre un diagramme existant
     */
    @FXML
    private void onOpenDiagram() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Ouvrir un diagramme");

        // Configurer les filtres
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Diagrammes", "*.json", "*.xmi", "*.cld"),
                new FileChooser.ExtensionFilter("Tous les fichiers", "*.*")
        );

        // Définir le répertoire initial
        String lastOpenDir = preferenceService.getLastOpenDirectory();
        if (lastOpenDir != null && !lastOpenDir.isEmpty()) {
            File dir = new File(lastOpenDir);
            if (dir.exists() && dir.isDirectory()) {
                fileChooser.setInitialDirectory(dir);
            }
        }

        // Afficher la boîte de dialogue
        File file = fileChooser.showOpenDialog(primaryStage);
        if (file != null) {
            try {
                // Mémoriser le répertoire
                preferenceService.setLastOpenDirectory(file.getParent());

                // Importer le diagramme
                ClassDiagram diagram = importService.importDiagram(file);
                if (diagram != null) {
                    openDiagramInTab(diagram);
                    updateStatusMessage("Diagramme importé depuis " + file.getName());
                }

            } catch (IOException e) {
                log.error("Erreur lors de l'ouverture du fichier", e);
                AlertUtils.showErrorDialog("Ouverture du fichier",
                        "Erreur lors de l'ouverture du fichier",
                        e.getMessage());
            }
        }
    }

    /**
     * Enregistre le diagramme actif
     */
    @FXML
    private void onSaveDiagram() {
        DiagramEditorController controller = getActiveEditorController();
        if (controller != null) {
            saveDiagram(controller);
        }
    }

    /**
     * Enregistre le diagramme actif sous un nouveau nom
     */
    @FXML
    private void onSaveAsDiagram() {
        DiagramEditorController controller = getActiveEditorController();
        if (controller != null) {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Enregistrer le diagramme sous");

            // Configurer les filtres
            fileChooser.getExtensionFilters().addAll(
                    new FileChooser.ExtensionFilter("Diagramme JSON", "*.json"),
                    new FileChooser.ExtensionFilter("Diagramme XMI", "*.xmi"),
                    new FileChooser.ExtensionFilter("Diagramme personnalisé", "*.cld")
            );

            // Définir le répertoire initial
            String lastSaveDir = preferenceService.getLastSaveDirectory();
            if (lastSaveDir != null && !lastSaveDir.isEmpty()) {
                File dir = new File(lastSaveDir);
                if (dir.exists() && dir.isDirectory()) {
                    fileChooser.setInitialDirectory(dir);
                }
            }

            // Proposer un nom de fichier basé sur le nom du diagramme
            fileChooser.setInitialFileName(controller.getDiagram().getName() + ".json");

            // Afficher la boîte de dialogue
            File file = fileChooser.showSaveDialog(primaryStage);
            if (file != null) {
                try {
                    // Mémoriser le répertoire
                    preferenceService.setLastSaveDirectory(file.getParent());

                    // Déterminer le format en fonction de l'extension
                    String fileName = file.getName().toLowerCase();
                    String format;
                    if (fileName.endsWith(".json")) {
                        format = "JSON";
                    } else if (fileName.endsWith(".xmi")) {
                        format = "XMI";
                    } else if (fileName.endsWith(".cld")) {
                        format = "CLD";
                    } else {
                        // Ajouter l'extension .json par défaut
                        file = new File(file.getAbsolutePath() + ".json");
                        format = "JSON";
                    }

                    // Exporter le diagramme
                    File outputFile = exportService.exportDiagram(controller.getDiagram(), format, file);

                    // Mettre à jour l'état
                    controller.setUnsavedChanges(false);
                    hasUnsavedChanges.set(false);

                    updateStatusMessage("Diagramme enregistré sous " + outputFile.getName());

                } catch (IOException e) {
                    log.error("Erreur lors de l'enregistrement du fichier", e);
                    AlertUtils.showErrorDialog("Enregistrement du fichier",
                            "Erreur lors de l'enregistrement du fichier",
                            e.getMessage());
                }
            }
        }
    }

    /**
     * Exporte le diagramme actif
     */
    @FXML
    void onExportDiagram() {
        DiagramEditorController controller = getActiveEditorController();
        if (controller != null) {
            ExportDialog dialog = new ExportDialog(primaryStage, controller.getDiagram(), exportService);
            dialog.showAndWait();

            if (dialog.isExportSuccessful()) {
                updateStatusMessage("Diagramme exporté avec succès");
            }
        }
    }

    /**
     * Importe un diagramme
     */
    @FXML
    private void onImportDiagram() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Importer un diagramme");

        // Configurer les filtres
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Fichiers JSON", "*.json"),
                new FileChooser.ExtensionFilter("Fichiers XMI", "*.xmi"),
                new FileChooser.ExtensionFilter("Diagrammes personnalisés", "*.cld"),
                new FileChooser.ExtensionFilter("Tous les fichiers", "*.*")
        );

        // Afficher la boîte de dialogue
        File file = fileChooser.showOpenDialog(primaryStage);
        if (file != null) {
            try {
                // Importer le diagramme
                ClassDiagram diagram = importService.importDiagram(file);
                if (diagram != null) {
                    openDiagramInTab(diagram);
                    updateStatusMessage("Diagramme importé depuis " + file.getName());
                }

            } catch (IOException e) {
                log.error("Erreur lors de l'importation du fichier", e);
                AlertUtils.showErrorDialog("Importation du fichier",
                        "Erreur lors de l'importation du fichier",
                        e.getMessage());
            }
        }
    }

    /**
     * Génère un diagramme à partir de code source Java
     */
    @FXML
    private void onGenerateFromCode() {
        // Demander à l'utilisateur de choisir un fichier Java ou un répertoire
        Alert choiceAlert = new Alert(Alert.AlertType.CONFIRMATION);
        choiceAlert.setTitle("Génération de diagramme");
        choiceAlert.setHeaderText("Choisir la source du code Java");
        choiceAlert.setContentText("Voulez-vous générer le diagramme à partir d'un fichier unique ou d'un répertoire?");

        ButtonType buttonFile = new ButtonType("Fichier unique");
        ButtonType buttonDirectory = new ButtonType("Répertoire");
        ButtonType buttonCancel = new ButtonType("Annuler", ButtonBar.ButtonData.CANCEL_CLOSE);

        choiceAlert.getButtonTypes().setAll(buttonFile, buttonDirectory, buttonCancel);

        Optional<ButtonType> result = choiceAlert.showAndWait();
        if (result.isPresent()) {
            if (result.get() == buttonFile) {
                generateFromFile();
            } else if (result.get() == buttonDirectory) {
                generateFromDirectory();
            }
        }
    }

    /**
     * Génère un diagramme à partir d'un fichier Java
     */
    private void generateFromFile() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Choisir un fichier Java");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Fichiers Java", "*.java"));

        File file = fileChooser.showOpenDialog(primaryStage);
        if (file != null) {
            try {
                // Générer le diagramme
                ClassDiagram diagram = diagramService.generateDiagramFromJavaSource(file);

                // Ouvrir le diagramme
                openDiagramInTab(diagram);
                updateStatusMessage("Diagramme généré à partir de " + file.getName());

            } catch (Exception e) {
                log.error("Erreur lors de la génération du diagramme", e);
                AlertUtils.showErrorDialog("Génération de diagramme",
                        "Erreur lors de la génération du diagramme",
                        e.getMessage());
            }
        }
    }

    /**
     * Génère un diagramme à partir d'un répertoire de code Java
     */
    private void generateFromDirectory() {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("Choisir un répertoire de code Java");

        File directory = directoryChooser.showDialog(primaryStage);
        if (directory != null) {
            try {
                // Vérifier que le répertoire contient des fichiers Java
                if (!FileUtils.containsJavaFiles(directory)) {
                    AlertUtils.showWarningDialog("Génération de diagramme",
                            "Aucun fichier Java trouvé",
                            "Le répertoire sélectionné ne contient aucun fichier Java.");
                    return;
                }

                // Générer le diagramme
                ClassDiagram diagram = diagramService.generateDiagramFromJavaDirectory(directory);

                // Ouvrir le diagramme
                openDiagramInTab(diagram);
                updateStatusMessage("Diagramme généré à partir du répertoire " + directory.getName());

            } catch (Exception e) {
                log.error("Erreur lors de la génération du diagramme", e);
                AlertUtils.showErrorDialog("Génération de diagramme",
                        "Erreur lors de la génération du diagramme",
                        e.getMessage());
            }
        }
    }

    /**
     * Ouvre la boîte de dialogue des propriétés du diagramme
     */
    @FXML
    private void onDiagramProperties() {
        DiagramEditorController controller = getActiveEditorController();
        if (controller != null) {
            DiagramPropertiesDialog dialog = new DiagramPropertiesDialog(primaryStage, controller.getDiagram());
            dialog.showAndWait().ifPresent(diagram -> {
                // Mettre à jour le diagramme
                controller.updateDiagram(diagram);
                // Mettre à jour l'onglet
                Tab selectedTab = diagramTabPane.getSelectionModel().getSelectedItem();
                if (selectedTab != null) {
                    selectedTab.setText(diagram.getName());
                }
            });
        }
    }

    /**
     * Quitte l'application
     */
    @FXML
    private void onQuitApplication() {
        boolean canClose = checkUnsavedChanges();
        if (canClose) {
            Platform.exit();
        }
    }

    /**
     * Vérifie s'il y a des modifications non enregistrées
     * @return true si l'application peut être fermée, false sinon
     */
    public boolean checkUnsavedChanges() {
        boolean hasUnsaved = false;

        // Vérifier chaque onglet
        for (Tab tab : diagramTabPane.getTabs()) {
            DiagramEditorController controller = (DiagramEditorController) tab.getUserData();
            if (controller != null && controller.hasUnsavedChanges()) {
                hasUnsaved = true;
                break;
            }
        }

        if (hasUnsaved) {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Quitter l'application");
            alert.setHeaderText("Modifications non enregistrées");
            alert.setContentText("Certains diagrammes ont des modifications non enregistrées. Voulez-vous quitter quand même?");

            ButtonType buttonSaveAll = new ButtonType("Tout enregistrer");
            ButtonType buttonQuit = new ButtonType("Quitter sans enregistrer");
            ButtonType buttonCancel = new ButtonType("Annuler", ButtonBar.ButtonData.CANCEL_CLOSE);

            alert.getButtonTypes().setAll(buttonSaveAll, buttonQuit, buttonCancel);

            Optional<ButtonType> result = alert.showAndWait();
            if (result.isPresent()) {
                if (result.get() == buttonSaveAll) {
                    // Enregistrer tous les diagrammes
                    for (Tab tab : diagramTabPane.getTabs()) {
                        DiagramEditorController controller = (DiagramEditorController) tab.getUserData();
                        if (controller != null && controller.hasUnsavedChanges()) {
                            saveDiagram(controller);
                        }
                    }
                    return true;
                } else if (result.get() == buttonQuit) {
                    return true;
                } else {
                    return false;
                }
            }
            return false;
        }

        return true;
    }

    /**
     * Annule la dernière action
     */
    @FXML
    private void onUndo() {
        DiagramEditorController controller = getActiveEditorController();
        if (controller != null) {
            controller.undo();
        }
    }

    /**
     * Rétablit la dernière action annulée
     */
    @FXML
    private void onRedo() {
        DiagramEditorController controller = getActiveEditorController();
        if (controller != null) {
            controller.redo();
        }
    }

    /**
     * Coupe les éléments sélectionnés
     */
    @FXML
    private void onCut() {
        DiagramEditorController controller = getActiveEditorController();
        if (controller != null) {
            controller.cutSelectedElements();
        }
    }

    /**
     * Copie les éléments sélectionnés
     */
    @FXML
    private void onCopy() {
        DiagramEditorController controller = getActiveEditorController();
        if (controller != null) {
            controller.copySelectedElements();
        }
    }

    /**
     * Colle les éléments du presse-papiers
     */
    @FXML
    private void onPaste() {
        DiagramEditorController controller = getActiveEditorController();
        if (controller != null) {
            controller.pasteElements();
        }
    }

    /**
     * Supprime les éléments sélectionnés
     */
    @FXML
    private void onDelete() {
        DiagramEditorController controller = getActiveEditorController();
        if (controller != null) {
            controller.deleteSelectedElements();
        }
    }

    /**
     * Sélectionne tous les éléments
     */
    @FXML
    private void onSelectAll() {
        DiagramEditorController controller = getActiveEditorController();
        if (controller != null) {
            controller.selectAllElements();
        }
    }

    /**
     * Ajoute une classe au diagramme
     */
    @FXML
    private void onAddClass() {
        DiagramEditorController controller = getActiveEditorController();
        if (controller != null) {
            controller.addClassElement();
        }
    }

    /**
     * Ajoute une interface au diagramme
     */
    @FXML
    private void onAddInterface() {
        DiagramEditorController controller = getActiveEditorController();
        if (controller != null) {
            controller.addInterfaceElement();
        }
    }

    /**
     * Ajoute une énumération au diagramme
     */
    @FXML
    private void onAddEnum() {
        DiagramEditorController controller = getActiveEditorController();
        if (controller != null) {
            controller.addEnumElement();
        }
    }

    /**
     * Ajoute une relation au diagramme
     */
    @FXML
    private void onAddRelationship() {
        DiagramEditorController controller = getActiveEditorController();
        if (controller != null) {
            controller.startAddingRelationship();
        }
    }

    /**
     * Effectue un zoom avant sur le diagramme
     */
    @FXML
    private void onZoomIn() {
        DiagramEditorController controller = getActiveEditorController();
        if (controller != null) {
            controller.zoomIn();
        }
    }

    /**
     * Effectue un zoom arrière sur le diagramme
     */
    @FXML
    private void onZoomOut() {
        DiagramEditorController controller = getActiveEditorController();
        if (controller != null) {
            controller.zoomOut();
        }
    }

    /**
     * Réinitialise le zoom
     */
    @FXML
    private void onZoomReset() {
        DiagramEditorController controller = getActiveEditorController();
        if (controller != null) {
            controller.resetZoom();
        }
    }

    /**
     * Active/désactive l'affichage de la grille
     */
    @FXML
    private void onToggleGrid() {
        DiagramEditorController controller = getActiveEditorController();
        if (controller != null) {
            controller.toggleGrid();
        }
    }

    /**
     * Active/désactive l'alignement sur la grille
     */
    @FXML
    private void onToggleSnapToGrid() {
        DiagramEditorController controller = getActiveEditorController();
        if (controller != null) {
            controller.toggleSnapToGrid();
        }
    }

    /**
     * Change le thème de l'application
     */
    @FXML
    private void onChangeTheme() {
        ToggleGroup group = themeToggleGroup;
        if (group != null) {
            RadioMenuItem selectedTheme = (RadioMenuItem) group.getSelectedToggle();
            if (selectedTheme != null) {
                String theme = (String) selectedTheme.getUserData();
                applyTheme(theme);
            }
        }
    }

    /**
     * Applique un thème à l'application
     */
    private void applyTheme(String theme) {
        // Supprimer les anciennes feuilles de style
        Scene scene = menuBar.getScene();
        if (scene != null) {
            scene.getStylesheets().clear();

            // Ajouter la feuille de style de base
            scene.getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());

            // Ajouter la feuille de style du thème
            if ("dark".equals(theme)) {
                scene.getStylesheets().add(getClass().getResource("/css/dark-theme.css").toExternalForm());
                preferenceService.setTheme("dark");
            } else {
                scene.getStylesheets().add(getClass().getResource("/css/light-theme.css").toExternalForm());
                preferenceService.setTheme("light");
            }
        }
    }

    /**
     * Vérifie la validité UML du diagramme
     */
    @FXML
    private void onCheckUmlValidity() {
        DiagramEditorController controller = getActiveEditorController();
        if (controller != null) {
            controller.checkUmlValidity();
        }
    }

    /**
     * Optimise la disposition des éléments du diagramme
     */
    @FXML
    private void onOptimizeLayout() {
        DiagramEditorController controller = getActiveEditorController();
        if (controller != null) {
            controller.optimizeLayout();
        }
    }

    /**
     * Génère du code Java à partir du diagramme
     */
    @FXML
    private void onGenerateJavaCode() {
        DiagramEditorController controller = getActiveEditorController();
        if (controller != null) {
            controller.generateJavaCode();
        }
    }

    /**
     * Ouvre la boîte de dialogue des préférences
     */
    @FXML
    private void onOpenPreferences() {
        PreferencesDialog dialog = new PreferencesDialog(primaryStage, preferenceService);
        dialog.showAndWait();

        // Appliquer les préférences
        applyPreferences();
    }

    /**
     * Affiche la documentation
     */
    @FXML
    private void onShowDocumentation() {
        // Implémenter l'affichage de la documentation (par exemple, ouvrir un fichier PDF ou une page HTML)
        updateStatusMessage("Documentation non disponible");
    }

    /**
     * Affiche les raccourcis clavier
     */
    @FXML
    private void onShowKeyboardShortcuts() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Raccourcis clavier");
        alert.setHeaderText("Raccourcis clavier disponibles");

        // Créer une liste des raccourcis
        VBox content = new VBox(10);
        content.getChildren().addAll(
                new Label("Fichier:"),
                createShortcutLabel("Ctrl+N", "Nouveau diagramme"),
                createShortcutLabel("Ctrl+O", "Ouvrir un diagramme"),
                createShortcutLabel("Ctrl+S", "Enregistrer"),
                createShortcutLabel("Ctrl+Shift+S", "Enregistrer sous"),
                createShortcutLabel("Ctrl+Q", "Quitter"),

                new Label("Édition:"),
                createShortcutLabel("Ctrl+Z", "Annuler"),
                createShortcutLabel("Ctrl+Y", "Rétablir"),
                createShortcutLabel("Ctrl+X", "Couper"),
                createShortcutLabel("Ctrl+C", "Copier"),
                createShortcutLabel("Ctrl+V", "Coller"),
                createShortcutLabel("Suppr", "Supprimer"),
                createShortcutLabel("Ctrl+A", "Sélectionner tout"),

                new Label("Vue:"),
                createShortcutLabel("Ctrl++", "Zoom avant"),
                createShortcutLabel("Ctrl+-", "Zoom arrière"),
                createShortcutLabel("Ctrl+0", "Réinitialiser le zoom")
        );

        ScrollPane scrollPane = new ScrollPane(content);
        scrollPane.setFitToWidth(true);
        scrollPane.setPrefHeight(400);

        alert.getDialogPane().setContent(scrollPane);
        alert.showAndWait();
    }

    /**
     * Crée un label pour un raccourci clavier
     */
    private HBox createShortcutLabel(String shortcut, String description) {
        HBox hbox = new HBox(20);

        Label shortcutLabel = new Label(shortcut);
        shortcutLabel.setStyle("-fx-font-family: monospace; -fx-font-weight: bold;");
        shortcutLabel.setPrefWidth(100);

        Label descLabel = new Label(description);

        hbox.getChildren().addAll(shortcutLabel, descLabel);
        return hbox;
    }

    /**
     * Affiche la boîte de dialogue "À propos"
     */
    @FXML
    private void onShowAbout() {
        AboutDialog dialog = new AboutDialog(primaryStage);
        dialog.showAndWait();
    }

    /**
     * Applique les préférences de l'application
     */
    private void applyPreferences() {
        // Appliquer le thème
        String theme = preferenceService.getTheme();
        applyTheme(theme);

        // Mettre à jour le groupe de boutons radio du thème
        ToggleGroup group = themeToggleGroup;
        if (group != null) {
            for (Toggle toggle : group.getToggles()) {
                RadioMenuItem item = (RadioMenuItem) toggle;
                if (theme.equals(item.getUserData())) {
                    item.setSelected(true);
                    break;
                }
            }
        }
    }

    /**
     * Enregistre un diagramme
     */
    private void saveDiagram(DiagramEditorController controller) {
        try {
            // Enregistrer le diagramme dans la base de données
            ClassDiagram savedDiagram = diagramService.saveDiagram(controller.getDiagram());

            // Mettre à jour le contrôleur avec le diagramme sauvegardé
            controller.setDiagram(savedDiagram);
            controller.setUnsavedChanges(false);

            // Mettre à jour les propriétés de l'application
            hasUnsavedChanges.set(false);

            // Mettre à jour l'onglet
            Tab selectedTab = diagramTabPane.getSelectionModel().getSelectedItem();
            if (selectedTab != null) {
                selectedTab.setText(savedDiagram.getName());
            }

            updateStatusMessage("Diagramme " + savedDiagram.getName() + " enregistré");

        } catch (Exception e) {
            log.error("Erreur lors de l'enregistrement du diagramme", e);
            AlertUtils.showErrorDialog("Enregistrement du diagramme",
                    "Erreur lors de l'enregistrement du diagramme",
                    e.getMessage());
        }
    }

    /**
     * Récupère le contrôleur de l'éditeur actif
     */
    private DiagramEditorController getActiveEditorController() {
        Tab selectedTab = diagramTabPane.getSelectionModel().getSelectedItem();
        if (selectedTab != null) {
            return (DiagramEditorController) selectedTab.getUserData();
        }
        return null;
    }

    /**
     * Met à jour le message de la barre d'état
     */
    public void updateStatusMessage(String message) {
        statusLabel.setText(message);
    }

    /**
     * Met à jour l'étiquette de zoom
     */
    public void updateZoomLabel(double zoom) {
        int zoomPercent = (int) (zoom * 100);
        zoomLabel.setText("Zoom: " + zoomPercent + "%");
    }

    /**
     * Notification qu'un diagramme a des changements non enregistrés
     */
    public void notifyDiagramChanged() {
        hasUnsavedChanges.set(true);
    }
}
