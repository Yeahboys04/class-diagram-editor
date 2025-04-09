package com.diagramme.ui.dialog;

import com.diagramme.model.Attribute;
import com.diagramme.model.ClassElement;
import com.diagramme.model.Method;
import com.diagramme.model.Parameter;
import com.diagramme.model.enums.Visibility;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Window;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Boîte de dialogue pour éditer une classe
 */
public class ClassDialog extends Dialog<ClassElement> {

    private final ClassElement classElement;
    private final ClassElement.ClassType classType;

    private TextField nameField;
    private TextField packageField;
    private CheckBox abstractCheckBox;

    private TableView<Attribute> attributesTable;
    private ObservableList<Attribute> attributes;

    private TableView<Method> methodsTable;
    private ObservableList<Method> methods;

    public ClassDialog(Window owner, ClassElement classElement, ClassElement.ClassType classType) {
        this.classElement = classElement;
        this.classType = classType;

        // Configurer la boîte de dialogue
        initOwner(owner);
        setTitle(getDialogTitle());
        setHeaderText(getDialogHeader());

        // Créer le contenu
        VBox content = createContent();
        getDialogPane().setContent(content);

        // Configurer les boutons
        getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        // Validation
        Button okButton = (Button) getDialogPane().lookupButton(ButtonType.OK);
        okButton.disableProperty().bind(nameField.textProperty().isEmpty());

        // Convertisseur de résultat
        setResultConverter(buttonType -> {
            if (buttonType == ButtonType.OK) {
                return createClassElement();
            }
            return null;
        });
    }

    /**
     * Crée le contenu de la boîte de dialogue
     */
    private VBox createContent() {
        VBox content = new VBox(10);
        content.setPadding(new Insets(10));
        content.setPrefWidth(600);
        content.setPrefHeight(500);

        // Champs de base
        content.getChildren().add(createBasicFields());

        // Attributs
        content.getChildren().add(createAttributesSection());

        // Méthodes
        content.getChildren().add(createMethodsSection());

        return content;
    }

    /**
     * Crée la section des champs de base
     */
    private VBox createBasicFields() {
        VBox section = new VBox(5);
        section.setPadding(new Insets(0, 0, 10, 0));

        // Nom
        nameField = new TextField();
        nameField.setPromptText("Nom de la classe");
        if (classElement != null) {
            nameField.setText(classElement.getName());
        }

        // Package
        packageField = new TextField();
        packageField.setPromptText("Package");
        if (classElement != null && classElement.getPackageName() != null) {
            packageField.setText(classElement.getPackageName());
        }

        // Case à cocher pour classe abstraite (désactivée pour les interfaces)
        abstractCheckBox = new CheckBox("Classe abstraite");
        abstractCheckBox.setDisable(classType == ClassElement.ClassType.INTERFACE);
        if (classElement != null) {
            abstractCheckBox.setSelected(classElement.isAbstract());
        } else {
            abstractCheckBox.setSelected(false);
        }

        // Ajouter à la section
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(5);

        grid.add(new Label("Nom:"), 0, 0);
        grid.add(nameField, 1, 0);
        GridPane.setHgrow(nameField, Priority.ALWAYS);

        grid.add(new Label("Package:"), 0, 1);
        grid.add(packageField, 1, 1);
        GridPane.setHgrow(packageField, Priority.ALWAYS);

        section.getChildren().addAll(grid, abstractCheckBox);

        return section;
    }

    /**
     * Crée la section des attributs
     */
    private VBox createAttributesSection() {
        VBox section = new VBox(5);
        section.setPadding(new Insets(0, 0, 10, 0));

        // En-tête
        Label titleLabel = new Label("Attributs");
        titleLabel.setStyle("-fx-font-weight: bold;");

        // Initialiser la liste d'attributs
        initializeAttributes();

        // Tableau d'attributs
        attributesTable = new TableView<>(attributes);
        attributesTable.setPrefHeight(150);

        // Colonnes
        TableColumn<Attribute, String> visibilityColumn = new TableColumn<>("Visibilité");
        visibilityColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getVisibility().getSymbol()));
        visibilityColumn.setPrefWidth(80);

        TableColumn<Attribute, String> nameColumn = new TableColumn<>("Nom");
        nameColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getName()));
        nameColumn.setPrefWidth(150);

        TableColumn<Attribute, String> typeColumn = new TableColumn<>("Type");
        typeColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getType()));
        typeColumn.setPrefWidth(150);

        TableColumn<Attribute, String> modifiersColumn = new TableColumn<>("Modificateurs");
        modifiersColumn.setCellValueFactory(data -> {
            StringBuilder sb = new StringBuilder();
            if (data.getValue().isStatic()) {
                sb.append("static ");
            }
            if (data.getValue().isFinal()) {
                sb.append("final");
            }
            return new SimpleStringProperty(sb.toString());
        });

        attributesTable.getColumns().addAll(visibilityColumn, nameColumn, typeColumn, modifiersColumn);

        // Boutons
        Button addButton = new Button("Ajouter");
        addButton.setOnAction(event -> openAttributeDialog(null));

        Button editButton = new Button("Éditer");
        editButton.setOnAction(event -> {
            Attribute selected = attributesTable.getSelectionModel().getSelectedItem();
            if (selected != null) {
                openAttributeDialog(selected);
            }
        });
        editButton.disableProperty().bind(attributesTable.getSelectionModel().selectedItemProperty().isNull());

        Button deleteButton = new Button("Supprimer");
        deleteButton.setOnAction(event -> {
            Attribute selected = attributesTable.getSelectionModel().getSelectedItem();
            if (selected != null) {
                attributes.remove(selected);
            }
        });
        deleteButton.disableProperty().bind(attributesTable.getSelectionModel().selectedItemProperty().isNull());

        HBox buttonsBox = new HBox(5, addButton, editButton, deleteButton);

        // Assembler la section
        section.getChildren().addAll(titleLabel, attributesTable, buttonsBox);

        return section;
    }

    /**
     * Crée la section des méthodes
     */
    private VBox createMethodsSection() {
        VBox section = new VBox(5);
        section.setPadding(new Insets(0));

        // En-tête
        Label titleLabel = new Label("Méthodes");
        titleLabel.setStyle("-fx-font-weight: bold;");

        // Initialiser la liste de méthodes
        initializeMethods();

        // Tableau de méthodes
        methodsTable = new TableView<>(methods);
        methodsTable.setPrefHeight(150);

        // Colonnes
        TableColumn<Method, String> visibilityColumn = new TableColumn<>("Visibilité");
        visibilityColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getVisibility().getSymbol()));
        visibilityColumn.setPrefWidth(80);

        TableColumn<Method, String> nameColumn = new TableColumn<>("Nom");
        nameColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getName()));
        nameColumn.setPrefWidth(150);

        TableColumn<Method, String> parametersColumn = new TableColumn<>("Paramètres");
        parametersColumn.setCellValueFactory(data -> {
            StringBuilder sb = new StringBuilder();
            List<Parameter> params = data.getValue().getParameters();
            for (int i = 0; i < params.size(); i++) {
                Parameter param = params.get(i);
                sb.append(param.getName()).append(": ").append(param.getType());
                if (i < params.size() - 1) {
                    sb.append(", ");
                }
            }
            return new SimpleStringProperty(sb.toString());
        });
        parametersColumn.setPrefWidth(150);

        TableColumn<Method, String> returnTypeColumn = new TableColumn<>("Retour");
        returnTypeColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getReturnType()));
        returnTypeColumn.setPrefWidth(100);

        TableColumn<Method, String> modifiersColumn = new TableColumn<>("Modificateurs");
        modifiersColumn.setCellValueFactory(data -> {
            StringBuilder sb = new StringBuilder();
            if (data.getValue().isStatic()) {
                sb.append("static ");
            }
            if (data.getValue().isAbstract()) {
                sb.append("abstract ");
            }
            if (data.getValue().isFinal()) {
                sb.append("final");
            }
            return new SimpleStringProperty(sb.toString());
        });

        methodsTable.getColumns().addAll(visibilityColumn, nameColumn, parametersColumn, returnTypeColumn, modifiersColumn);

        // Boutons
        Button addButton = new Button("Ajouter");
        addButton.setOnAction(event -> openMethodDialog(null));

        Button editButton = new Button("Éditer");
        editButton.setOnAction(event -> {
            Method selected = methodsTable.getSelectionModel().getSelectedItem();
            if (selected != null) {
                openMethodDialog(selected);
            }
        });
        editButton.disableProperty().bind(methodsTable.getSelectionModel().selectedItemProperty().isNull());

        Button deleteButton = new Button("Supprimer");
        deleteButton.setOnAction(event -> {
            Method selected = methodsTable.getSelectionModel().getSelectedItem();
            if (selected != null) {
                methods.remove(selected);
            }
        });
        deleteButton.disableProperty().bind(methodsTable.getSelectionModel().selectedItemProperty().isNull());

        HBox buttonsBox = new HBox(5, addButton, editButton, deleteButton);

        // Assembler la section
        section.getChildren().addAll(titleLabel, methodsTable, buttonsBox);

        return section;
    }

    /**
     * Initialise la liste d'attributs
     */
    private void initializeAttributes() {
        attributes = FXCollections.observableArrayList();

        if (classElement != null && classElement.getAttributes() != null) {
            attributes.addAll(classElement.getAttributes());
        }
    }

    /**
     * Initialise la liste de méthodes
     */
    private void initializeMethods() {
        methods = FXCollections.observableArrayList();

        if (classElement != null && classElement.getMethods() != null) {
            methods.addAll(classElement.getMethods());
        }
    }

    /**
     * Ouvre la boîte de dialogue d'édition d'attribut
     */
    private void openAttributeDialog(Attribute attribute) {
        AttributeDialog dialog = new AttributeDialog(getDialogPane().getScene().getWindow(), attribute);
        Optional<Attribute> result = dialog.showAndWait();

        result.ifPresent(newAttribute -> {
            if (attribute != null) {
                // Mise à jour d'un attribut existant
                int index = attributes.indexOf(attribute);
                if (index >= 0) {
                    attributes.set(index, newAttribute);
                }
            } else {
                // Ajout d'un nouvel attribut
                attributes.add(newAttribute);
            }
        });
    }

    /**
     * Ouvre la boîte de dialogue d'édition de méthode
     */
    private void openMethodDialog(Method method) {
        MethodDialog dialog = new MethodDialog(getDialogPane().getScene().getWindow(), method);
        Optional<Method> result = dialog.showAndWait();

        result.ifPresent(newMethod -> {
            if (method != null) {
                // Mise à jour d'une méthode existante
                int index = methods.indexOf(method);
                if (index >= 0) {
                    methods.set(index, newMethod);
                }
            } else {
                // Ajout d'une nouvelle méthode
                methods.add(newMethod);
            }
        });
    }

    /**
     * Crée un élément de classe à partir des données saisies
     */
    private ClassElement createClassElement() {
        ClassElement element;

        if (classElement != null) {
            // Mise à jour d'un élément existant
            element = classElement;
        } else {
            // Création d'un nouvel élément
            element = new ClassElement(nameField.getText());
            element.setType(classType);
        }

        // Mettre à jour les propriétés
        element.setName(nameField.getText());
        element.setPackageName(packageField.getText());
        element.setAbstract(abstractCheckBox.isSelected());

        // Mettre à jour les attributs et méthodes
        element.getAttributes().clear();
        element.getMethods().clear();

        for (Attribute attribute : attributes) {
            // Créer une copie de l'attribut
            Attribute newAttribute = new Attribute(attribute.getName(), attribute.getType());
            newAttribute.setVisibility(attribute.getVisibility());
            newAttribute.setStatic(attribute.isStatic());
            newAttribute.setFinal(attribute.isFinal());
            newAttribute.setDefaultValue(attribute.getDefaultValue());

            element.addAttribute(newAttribute);
        }

        for (Method method : methods) {
            // Créer une copie de la méthode
            Method newMethod = new Method(method.getName(), method.getReturnType());
            newMethod.setVisibility(method.getVisibility());
            newMethod.setStatic(method.isStatic());
            newMethod.setAbstract(method.isAbstract());
            newMethod.setFinal(method.isFinal());

            // Copier les paramètres
            for (Parameter parameter : method.getParameters()) {
                Parameter newParameter = new Parameter(parameter.getName(), parameter.getType());
                newParameter.setDefaultValue(parameter.getDefaultValue());
                newMethod.addParameter(newParameter);
            }

            element.addMethod(newMethod);
        }

        return element;
    }

    /**
     * Retourne le titre de la boîte de dialogue
     */
    private String getDialogTitle() {
        if (classElement != null) {
            return "Éditer " + getClassTypeLabel();
        } else {
            return "Ajouter " + getClassTypeLabel();
        }
    }

    /**
     * Retourne l'en-tête de la boîte de dialogue
     */
    private String getDialogHeader() {
        if (classElement != null) {
            return "Éditer les propriétés de " + getClassTypeLabel().toLowerCase();
        } else {
            return "Définir les propriétés de " + getClassTypeLabel().toLowerCase();
        }
    }

    /**
     * Retourne le libellé du type de classe
     */
    private String getClassTypeLabel() {
        switch (classType) {
            case INTERFACE:
                return "l'interface";
            case ENUM:
                return "l'énumération";
            default:
                return "la classe";
        }
    }

    /**
     * Boîte de dialogue pour éditer un attribut
     */
    private static class AttributeDialog extends Dialog<Attribute> {

        public AttributeDialog(Window owner, Attribute attribute) {
            initOwner(owner);
            setTitle(attribute != null ? "Éditer l'attribut" : "Ajouter un attribut");

            // Champs
            TextField nameField = new TextField();
            nameField.setPromptText("Nom");
            if (attribute != null) {
                nameField.setText(attribute.getName());
            }

            TextField typeField = new TextField();
            typeField.setPromptText("Type");
            if (attribute != null) {
                typeField.setText(attribute.getType());
            }

            TextField defaultValueField = new TextField();
            defaultValueField.setPromptText("Valeur par défaut (optionnelle)");
            if (attribute != null && attribute.getDefaultValue() != null) {
                defaultValueField.setText(attribute.getDefaultValue());
            }

            ComboBox<Visibility> visibilityComboBox = new ComboBox<>();
            visibilityComboBox.getItems().addAll(Visibility.values());
            visibilityComboBox.setValue(attribute != null ? attribute.getVisibility() : Visibility.PRIVATE);

            CheckBox staticCheckBox = new CheckBox("static");
            if (attribute != null) {
                staticCheckBox.setSelected(attribute.isStatic());
            }

            CheckBox finalCheckBox = new CheckBox("final");
            if (attribute != null) {
                finalCheckBox.setSelected(attribute.isFinal());
            }

            // Disposition
            GridPane grid = new GridPane();
            grid.setHgap(10);
            grid.setVgap(5);
            grid.setPadding(new Insets(10));

            grid.add(new Label("Nom:"), 0, 0);
            grid.add(nameField, 1, 0);

            grid.add(new Label("Type:"), 0, 1);
            grid.add(typeField, 1, 1);

            grid.add(new Label("Valeur par défaut:"), 0, 2);
            grid.add(defaultValueField, 1, 2);

            grid.add(new Label("Visibilité:"), 0, 3);
            grid.add(visibilityComboBox, 1, 3);

            HBox modifiersBox = new HBox(10, staticCheckBox, finalCheckBox);
            grid.add(modifiersBox, 1, 4);

            getDialogPane().setContent(grid);

            // Boutons
            getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

            // Validation
            Button okButton = (Button) getDialogPane().lookupButton(ButtonType.OK);
            okButton.disableProperty().bind(nameField.textProperty().isEmpty()
                    .or(typeField.textProperty().isEmpty()));

            // Convertisseur de résultat
            setResultConverter(buttonType -> {
                if (buttonType == ButtonType.OK) {
                    Attribute result = new Attribute(
                            nameField.getText(),
                            typeField.getText(),
                            visibilityComboBox.getValue()
                    );

                    String defaultValue = defaultValueField.getText().trim();
                    if (!defaultValue.isEmpty()) {
                        result.setDefaultValue(defaultValue);
                    }

                    result.setStatic(staticCheckBox.isSelected());
                    result.setFinal(finalCheckBox.isSelected());

                    return result;
                }
                return null;
            });
        }
    }

    /**
     * Boîte de dialogue pour éditer une méthode
     */
    private static class MethodDialog extends Dialog<Method> {

        private final ObservableList<Parameter> parameters = FXCollections.observableArrayList();

        public MethodDialog(Window owner, Method method) {
            initOwner(owner);
            setTitle(method != null ? "Éditer la méthode" : "Ajouter une méthode");

            // Initialiser les paramètres
            if (method != null && method.getParameters() != null) {
                parameters.addAll(method.getParameters());
            }

            // Champs
            TextField nameField = new TextField();
            nameField.setPromptText("Nom");
            if (method != null) {
                nameField.setText(method.getName());
            }

            TextField returnTypeField = new TextField();
            returnTypeField.setPromptText("Type de retour (void si vide)");
            if (method != null && method.getReturnType() != null) {
                returnTypeField.setText(method.getReturnType());
            }

            ComboBox<Visibility> visibilityComboBox = new ComboBox<>();
            visibilityComboBox.getItems().addAll(Visibility.values());
            visibilityComboBox.setValue(method != null ? method.getVisibility() : Visibility.PUBLIC);

            CheckBox staticCheckBox = new CheckBox("static");
            if (method != null) {
                staticCheckBox.setSelected(method.isStatic());
            }

            CheckBox abstractCheckBox = new CheckBox("abstract");
            if (method != null) {
                abstractCheckBox.setSelected(method.isAbstract());
            }

            CheckBox finalCheckBox = new CheckBox("final");
            if (method != null) {
                finalCheckBox.setSelected(method.isFinal());
            }

            // Tableau de paramètres
            TableView<Parameter> parametersTable = new TableView<>(parameters);
            parametersTable.setPrefHeight(150);

            TableColumn<Parameter, String> nameColumn = new TableColumn<>("Nom");
            nameColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getName()));
            nameColumn.setPrefWidth(150);

            TableColumn<Parameter, String> typeColumn = new TableColumn<>("Type");
            typeColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getType()));
            typeColumn.setPrefWidth(150);

            parametersTable.getColumns().addAll(nameColumn, typeColumn);

            // Boutons pour les paramètres
            Button addParamButton = new Button("Ajouter");
            addParamButton.setOnAction(event -> openParameterDialog(null));

            Button editParamButton = new Button("Éditer");
            editParamButton.setOnAction(event -> {
                Parameter selected = parametersTable.getSelectionModel().getSelectedItem();
                if (selected != null) {
                    openParameterDialog(selected);
                }
            });
            editParamButton.disableProperty().bind(parametersTable.getSelectionModel().selectedItemProperty().isNull());

            Button deleteParamButton = new Button("Supprimer");
            deleteParamButton.setOnAction(event -> {
                Parameter selected = parametersTable.getSelectionModel().getSelectedItem();
                if (selected != null) {
                    parameters.remove(selected);
                }
            });
            deleteParamButton.disableProperty().bind(parametersTable.getSelectionModel().selectedItemProperty().isNull());

            HBox paramButtonsBox = new HBox(5, addParamButton, editParamButton, deleteParamButton);

            // Disposition
            VBox content = new VBox(10);
            content.setPadding(new Insets(10));

            GridPane basicGrid = new GridPane();
            basicGrid.setHgap(10);
            basicGrid.setVgap(5);

            basicGrid.add(new Label("Nom:"), 0, 0);
            basicGrid.add(nameField, 1, 0);

            basicGrid.add(new Label("Type de retour:"), 0, 1);
            basicGrid.add(returnTypeField, 1, 1);

            basicGrid.add(new Label("Visibilité:"), 0, 2);
            basicGrid.add(visibilityComboBox, 1, 2);

            HBox modifiersBox = new HBox(10, staticCheckBox, abstractCheckBox, finalCheckBox);

            Label paramsLabel = new Label("Paramètres");
            paramsLabel.setStyle("-fx-font-weight: bold;");

            content.getChildren().addAll(
                    basicGrid,
                    modifiersBox,
                    new Separator(),
                    paramsLabel,
                    parametersTable,
                    paramButtonsBox
            );

            getDialogPane().setContent(content);
            getDialogPane().setPrefWidth(500);
            getDialogPane().setPrefHeight(500);

            // Boutons
            getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

            // Validation
            Button okButton = (Button) getDialogPane().lookupButton(ButtonType.OK);
            okButton.disableProperty().bind(nameField.textProperty().isEmpty());

            // Convertisseur de résultat
            setResultConverter(buttonType -> {
                if (buttonType == ButtonType.OK) {
                    Method result = new Method(nameField.getText());

                    String returnType = returnTypeField.getText().trim();
                    if (!returnType.isEmpty()) {
                        result.setReturnType(returnType);
                    } else {
                        result.setReturnType("void");
                    }

                    result.setVisibility(visibilityComboBox.getValue());
                    result.setStatic(staticCheckBox.isSelected());
                    result.setAbstract(abstractCheckBox.isSelected());
                    result.setFinal(finalCheckBox.isSelected());

                    // Ajouter les paramètres
                    for (Parameter parameter : parameters) {
                        Parameter newParam = new Parameter(parameter.getName(), parameter.getType());
                        newParam.setDefaultValue(parameter.getDefaultValue());
                        result.addParameter(newParam);
                    }

                    return result;
                }
                return null;
            });
        }

        /**
         * Ouvre la boîte de dialogue d'édition de paramètre
         */
        private void openParameterDialog(Parameter parameter) {
            ParameterDialog dialog = new ParameterDialog(getDialogPane().getScene().getWindow(), parameter);
            Optional<Parameter> result = dialog.showAndWait();

            result.ifPresent(newParameter -> {
                if (parameter != null) {
                    // Mise à jour d'un paramètre existant
                    int index = parameters.indexOf(parameter);
                    if (index >= 0) {
                        parameters.set(index, newParameter);
                    }
                } else {
                    // Ajout d'un nouveau paramètre
                    parameters.add(newParameter);
                }
            });
        }
    }

    /**
     * Boîte de dialogue pour éditer un paramètre
     */
    private static class ParameterDialog extends Dialog<Parameter> {

        public ParameterDialog(Window owner, Parameter parameter) {
            initOwner(owner);
            setTitle(parameter != null ? "Éditer le paramètre" : "Ajouter un paramètre");

            // Champs
            TextField nameField = new TextField();
            nameField.setPromptText("Nom");
            if (parameter != null) {
                nameField.setText(parameter.getName());
            }

            TextField typeField = new TextField();
            typeField.setPromptText("Type");
            if (parameter != null) {
                typeField.setText(parameter.getType());
            }

            TextField defaultValueField = new TextField();
            defaultValueField.setPromptText("Valeur par défaut (optionnelle)");
            if (parameter != null && parameter.getDefaultValue() != null) {
                defaultValueField.setText(parameter.getDefaultValue());
            }

            // Disposition
            GridPane grid = new GridPane();
            grid.setHgap(10);
            grid.setVgap(5);
            grid.setPadding(new Insets(10));

            grid.add(new Label("Nom:"), 0, 0);
            grid.add(nameField, 1, 0);

            grid.add(new Label("Type:"), 0, 1);
            grid.add(typeField, 1, 1);

            grid.add(new Label("Valeur par défaut:"), 0, 2);
            grid.add(defaultValueField, 1, 2);

            getDialogPane().setContent(grid);

            // Boutons
            getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

            // Validation
            Button okButton = (Button) getDialogPane().lookupButton(ButtonType.OK);
            okButton.disableProperty().bind(nameField.textProperty().isEmpty()
                    .or(typeField.textProperty().isEmpty()));

            // Convertisseur de résultat
            setResultConverter(buttonType -> {
                if (buttonType == ButtonType.OK) {
                    Parameter result = new Parameter(nameField.getText(), typeField.getText());

                    String defaultValue = defaultValueField.getText().trim();
                    if (!defaultValue.isEmpty()) {
                        result.setDefaultValue(defaultValue);
                    }

                    return result;
                }
                return null;
            });
        }
    }
}
