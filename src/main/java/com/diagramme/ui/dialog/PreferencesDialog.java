package com.diagramme.ui.dialog;

import com.diagramme.service.PreferenceService;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.stage.Window;

/**
 * Boîte de dialogue des préférences
 */
public class PreferencesDialog extends Dialog<Void> {

    private final PreferenceService preferenceService;

    public PreferencesDialog(Window owner, PreferenceService preferenceService) {
        this.preferenceService = preferenceService;

        // Configurer la boîte de dialogue
        initOwner(owner);
        setTitle("Préférences");
        setHeaderText("Configurer les préférences de l'application");

        // Créer le contenu
        TabPane tabPane = new TabPane();
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);

        // Onglet Général
        Tab generalTab = new Tab("Général");
        generalTab.setContent(createGeneralTab());

        // Onglet Apparence
        Tab appearanceTab = new Tab("Apparence");
        appearanceTab.setContent(createAppearanceTab());

        // Onglet Sauvegarde
        Tab saveTab = new Tab("Sauvegarde");
        saveTab.setContent(createSaveTab());

        // Ajouter les onglets
        tabPane.getTabs().addAll(generalTab, appearanceTab, saveTab);

        getDialogPane().setContent(tabPane);

        // Ajouter les boutons
        getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        // Convertisseur de résultat
        setResultConverter(buttonType -> {
            if (buttonType == ButtonType.OK) {
                savePreferences();
            }
            return null;
        });
    }

    /**
     * Crée l'onglet des préférences générales
     */
    private GridPane createGeneralTab() {
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));

        // Nombre de projets récents
        Label recentProjectsLabel = new Label("Nombre de projets récents:");
        Spinner<Integer> recentProjectsSpinner = new Spinner<>(1, 20, preferenceService.getRecentProjectsMax());

        // Ajouter les contrôles
        grid.add(recentProjectsLabel, 0, 0);
        grid.add(recentProjectsSpinner, 1, 0);

        // Sauvegarder les valeurs
        recentProjectsSpinner.valueProperty().addListener((obs, oldVal, newVal) -> {
            preferenceService.setRecentProjectsMax(newVal);
        });

        return grid;
    }

    /**
     * Crée l'onglet des préférences d'apparence
     */
    private GridPane createAppearanceTab() {
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));

        // Thème
        Label themeLabel = new Label("Thème:");
        ToggleGroup themeGroup = new ToggleGroup();

        RadioButton lightThemeRadio = new RadioButton("Clair");
        lightThemeRadio.setToggleGroup(themeGroup);
        lightThemeRadio.setUserData("light");

        RadioButton darkThemeRadio = new RadioButton("Sombre");
        darkThemeRadio.setToggleGroup(themeGroup);
        darkThemeRadio.setUserData("dark");

        // Définir la sélection initiale
        if ("dark".equals(preferenceService.getTheme())) {
            darkThemeRadio.setSelected(true);
        } else {
            lightThemeRadio.setSelected(true);
        }

        // Ajouter les contrôles
        grid.add(themeLabel, 0, 0);
        grid.add(lightThemeRadio, 1, 0);
        grid.add(darkThemeRadio, 2, 0);

        // Sauvegarder les valeurs
        themeGroup.selectedToggleProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                preferenceService.setTheme((String) newVal.getUserData());
            }
        });

        return grid;
    }

    /**
     * Crée l'onglet des préférences de sauvegarde
     */
    private GridPane createSaveTab() {
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));

        // Sauvegarde automatique
        CheckBox autoSaveCheckBox = new CheckBox("Activer la sauvegarde automatique");
        autoSaveCheckBox.setSelected(preferenceService.isAutoSaveEnabled());

        // Intervalle de sauvegarde
        Label intervalLabel = new Label("Intervalle de sauvegarde (minutes):");
        Spinner<Integer> intervalSpinner = new Spinner<>(1, 60, preferenceService.getAutoSaveInterval() / 60000);
        intervalSpinner.disableProperty().bind(autoSaveCheckBox.selectedProperty().not());

        // Ajouter les contrôles
        grid.add(autoSaveCheckBox, 0, 0, 2, 1);
        grid.add(intervalLabel, 0, 1);
        grid.add(intervalSpinner, 1, 1);

        // Sauvegarder les valeurs
        autoSaveCheckBox.selectedProperty().addListener((obs, oldVal, newVal) -> {
            preferenceService.setAutoSaveEnabled(newVal);
        });

        intervalSpinner.valueProperty().addListener((obs, oldVal, newVal) -> {
            preferenceService.setAutoSaveInterval(newVal * 60000); // Convertir en millisecondes
        });

        return grid;
    }

    /**
     * Sauvegarde les préférences
     */
    private void savePreferences() {
        // Toutes les modifications sont déjà enregistrées via les auditeurs
    }
}