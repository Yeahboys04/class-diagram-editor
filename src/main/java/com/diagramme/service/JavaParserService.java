package com.diagramme.service;

import com.diagramme.model.*;
import com.diagramme.model.enums.RelationshipType;
import com.diagramme.model.enums.Visibility;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Service pour l'analyse du code source Java et la génération de diagrammes
 * Note: Dans l'application finale, il faudra utiliser une bibliothèque comme JavaParser
 * l'implémentation est simplifiée et ne prend en charge que les cas basiques
 */
@Service
@Slf4j
public class JavaParserService {

    /**
     * Analyse un fichier Java et génère un diagramme
     */
    public ClassDiagram parseJavaFile(File file) {
        log.debug("Analyse du fichier Java: {}", file.getName());
        ClassDiagram diagram = new ClassDiagram(file.getName().replace(".java", ""));

        try {
            String content = Files.readString(file.toPath());
            parseJavaContent(content, diagram);
        } catch (IOException e) {
            log.error("Erreur lors de la lecture du fichier", e);
        }

        return diagram;
    }

    /**
     * Analyse un répertoire contenant des fichiers Java et génère un diagramme
     */
    public ClassDiagram parseJavaDirectory(File directory) {
        log.debug("Analyse du répertoire Java: {}", directory.getName());
        ClassDiagram diagram = new ClassDiagram(directory.getName());

        if (!directory.isDirectory()) {
            log.error("Le chemin spécifié n'est pas un répertoire: {}", directory.getAbsolutePath());
            return diagram;
        }

        // Collecter tous les fichiers Java
        List<File> javaFiles = collectJavaFiles(directory);
        log.debug("Trouvé {} fichiers Java à analyser", javaFiles.size());

        // Analyse chaque fichier et ajoute ses éléments au diagramme
        for (File javaFile : javaFiles) {
            try {
                String content = Files.readString(javaFile.toPath());
                parseJavaContent(content, diagram);
            } catch (IOException e) {
                log.error("Erreur lors de la lecture du fichier: {}", javaFile.getName(), e);
            }
        }

        // Analyser les relations entre les classes
        analyzeRelationships(diagram);

        return diagram;
    }

    /**
     * Collecte tous les fichiers Java dans un répertoire et ses sous-répertoires
     */
    private List<File> collectJavaFiles(File directory) {
        List<File> javaFiles = new ArrayList<>();
        File[] files = directory.listFiles();

        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    javaFiles.addAll(collectJavaFiles(file));
                } else if (file.getName().endsWith(".java")) {
                    javaFiles.add(file);
                }
            }
        }

        return javaFiles;
    }

    /**
     * Analyse le contenu d'un fichier Java et ajoute les éléments trouvés au diagramme
     */
    private void parseJavaContent(String content, ClassDiagram diagram) {
        // Extraire le nom du package
        String packageName = extractPackage(content);

        // Extraire les informations de la classe
        List<ClassInfo> classInfos = extractClassInfo(content, packageName);

        // Créer les éléments de classe
        for (ClassInfo info : classInfos) {
            ClassElement classElement = createClassElement(info);
            diagram.addElement(classElement);
        }
    }

    /**
     * Extrait le nom du package à partir du contenu Java
     */
    private String extractPackage(String content) {
        Pattern pattern = Pattern.compile("package\\s+([\\w.]+)\\s*;");
        Matcher matcher = pattern.matcher(content);

        if (matcher.find()) {
            return matcher.group(1);
        }

        return "";
    }

    /**
     * Extrait les informations de classe à partir du contenu Java
     */
    private List<ClassInfo> extractClassInfo(String content, String packageName) {
        List<ClassInfo> classInfos = new ArrayList<>();

        // Patterns pour détecter les classes, interfaces, etc.
        Pattern classPattern = Pattern.compile(
                "(?:public|private|protected)?\\s*(?:abstract)?\\s*(class|interface|enum)\\s+(\\w+)(?:\\s+extends\\s+(\\w+))?(?:\\s+implements\\s+([\\w,\\s]+))?\\s*\\{");

        Matcher classMatcher = classPattern.matcher(content);

        while (classMatcher.find()) {
            String type = classMatcher.group(1);
            String name = classMatcher.group(2);
            String extendsClass = classMatcher.group(3);
            String implementsInterfaces = classMatcher.group(4);

            ClassInfo classInfo = new ClassInfo();
            classInfo.setName(name);
            classInfo.setPackageName(packageName);

            switch (type) {
                case "class":
                    classInfo.setType(ClassElement.ClassType.CLASS);
                    break;
                case "interface":
                    classInfo.setType(ClassElement.ClassType.INTERFACE);
                    break;
                case "enum":
                    classInfo.setType(ClassElement.ClassType.ENUM);
                    break;
            }

            // Détecter si c'est une classe abstraite
            if (content.contains("abstract class " + name)) {
                classInfo.setAbstract(true);
            }

            // Enregistrer les relations d'héritage
            if (extendsClass != null) {
                classInfo.setExtendsClass(extendsClass);
            }

            // Enregistrer les interfaces implémentées
            if (implementsInterfaces != null) {
                classInfo.setImplementsInterfaces(Arrays.stream(implementsInterfaces.split(","))
                        .map(String::trim)
                        .collect(Collectors.toList()));
            }

            // Extraire les attributs
            extractAttributes(content, classInfo);

            // Extraire les méthodes
            extractMethods(content, classInfo);

            classInfos.add(classInfo);
        }

        return classInfos;
    }

    /**
     * Extrait les attributs de la classe
     */
    private void extractAttributes(String content, ClassInfo classInfo) {
        // Recherche d'attributs avec motif: visibilité [static] [final] type nom [= valeur];
        Pattern attrPattern = Pattern.compile(
                "(private|public|protected)\\s+(static\\s+)?(final\\s+)?(\\w+(?:<[\\w<>,\\s]*>)?)\\s+(\\w+)(?:\\s*=\\s*([^;]+))?;");

        Matcher attrMatcher = attrPattern.matcher(content);

        while (attrMatcher.find()) {
            String visibility = attrMatcher.group(1);
            boolean isStatic = attrMatcher.group(2) != null;
            boolean isFinal = attrMatcher.group(3) != null;
            String type = attrMatcher.group(4);
            String name = attrMatcher.group(5);
            String defaultValue = attrMatcher.group(6);

            AttributeInfo attrInfo = new AttributeInfo();
            attrInfo.setName(name);
            attrInfo.setType(type);
            attrInfo.setDefaultValue(defaultValue);
            attrInfo.setStatic(isStatic);
            attrInfo.setFinal(isFinal);

            switch (visibility) {
                case "private":
                    attrInfo.setVisibility(Visibility.PRIVATE);
                    break;
                case "public":
                    attrInfo.setVisibility(Visibility.PUBLIC);
                    break;
                case "protected":
                    attrInfo.setVisibility(Visibility.PROTECTED);
                    break;
                default:
                    attrInfo.setVisibility(Visibility.PACKAGE);
            }

            classInfo.getAttributes().add(attrInfo);
        }
    }

    /**
     * Extrait les méthodes de la classe
     */
    private void extractMethods(String content, ClassInfo classInfo) {
        // Recherche de méthodes avec motif: visibilité [static] [final] type nom(paramètres) { ... }
        Pattern methodPattern = Pattern.compile(
                "(private|public|protected)\\s+(static\\s+)?(final\\s+)?(\\w+(?:<[\\w<>,\\s]*>)?)\\s+(\\w+)\\s*\\(([^)]*)\\)\\s*(?:\\{|;)");

        Matcher methodMatcher = methodPattern.matcher(content);

        while (methodMatcher.find()) {
            String visibility = methodMatcher.group(1);
            boolean isStatic = methodMatcher.group(2) != null;
            boolean isFinal = methodMatcher.group(3) != null;
            String returnType = methodMatcher.group(4);
            String name = methodMatcher.group(5);
            String paramsString = methodMatcher.group(6);

            // Ignorer les constructeurs
            if (name.equals(classInfo.getName())) {
                continue;
            }

            MethodInfo methodInfo = new MethodInfo();
            methodInfo.setName(name);
            methodInfo.setReturnType(returnType);
            methodInfo.setStatic(isStatic);
            methodInfo.setFinal(isFinal);

            switch (visibility) {
                case "private":
                    methodInfo.setVisibility(Visibility.PRIVATE);
                    break;
                case "public":
                    methodInfo.setVisibility(Visibility.PUBLIC);
                    break;
                case "protected":
                    methodInfo.setVisibility(Visibility.PROTECTED);
                    break;
                default:
                    methodInfo.setVisibility(Visibility.PACKAGE);
            }

            // Extraire les paramètres de la méthode
            if (!paramsString.trim().isEmpty()) {
                String[] params = paramsString.split(",");
                for (String param : params) {
                    param = param.trim();
                    String[] parts = param.split("\\s+");
                    if (parts.length >= 2) {
                        String paramType = parts[0];
                        String paramName = parts[1];

                        // Supprimer les annotations éventuelles
                        if (paramType.startsWith("@")) {
                            if (parts.length < 3) continue;
                            paramType = parts[1];
                            paramName = parts[2];
                        }

                        ParameterInfo paramInfo = new ParameterInfo();
                        paramInfo.setType(paramType);
                        paramInfo.setName(paramName);
                        methodInfo.getParameters().add(paramInfo);
                    }
                }
            }

            classInfo.getMethods().add(methodInfo);
        }
    }

    /**
     * Crée un élément de classe à partir des informations extraites
     */
    private ClassElement createClassElement(ClassInfo classInfo) {
        ClassElement element = new ClassElement(classInfo.getName());
        element.setPackageName(classInfo.getPackageName());
        element.setType(classInfo.getType());
        element.setAbstract(classInfo.isAbstract());

        // Définir une position et taille par défaut
        element.setX(100 + Math.random() * 400);
        element.setY(100 + Math.random() * 400);
        element.setWidth(200);
        element.setHeight(150);

        // Ajouter les attributs
        for (AttributeInfo attrInfo : classInfo.getAttributes()) {
            Attribute attribute = new Attribute();
            attribute.setName(attrInfo.getName());
            attribute.setType(attrInfo.getType());
            attribute.setDefaultValue(attrInfo.getDefaultValue());
            attribute.setVisibility(attrInfo.getVisibility());
            attribute.setStatic(attrInfo.isStatic());
            attribute.setFinal(attrInfo.isFinal());

            element.addAttribute(attribute);
        }

        // Ajouter les méthodes
        for (MethodInfo methodInfo : classInfo.getMethods()) {
            Method method = new Method();
            method.setName(methodInfo.getName());
            method.setReturnType(methodInfo.getReturnType());
            method.setVisibility(methodInfo.getVisibility());
            method.setStatic(methodInfo.isStatic());
            method.setFinal(methodInfo.isFinal());
            method.setAbstract(methodInfo.isAbstract());

            // Ajouter les paramètres
            for (ParameterInfo paramInfo : methodInfo.getParameters()) {
                Parameter parameter = new Parameter();
                parameter.setName(paramInfo.getName());
                parameter.setType(paramInfo.getType());
                method.addParameter(parameter);
            }

            element.addMethod(method);
        }

        return element;
    }

    /**
     * Analyse les relations entre les classes du diagramme
     */
    private void analyzeRelationships(ClassDiagram diagram) {
        Map<String, ClassElement> classMap = new HashMap<>();

        // Créer une carte des classes par nom
        for (ClassElement element : diagram.getClasses()) {
            classMap.put(element.getName(), element);
            // Inclure aussi avec le package
            if (element.getPackageName() != null && !element.getPackageName().isEmpty()) {
                classMap.put(element.getPackageName() + "." + element.getName(), element);
            }
        }

        // Analyser les relations d'héritage et d'implémentation
        for (ClassElement element : diagram.getClasses()) {
            // Parcourir tous les attributs pour trouver des associations
            for (Attribute attribute : element.getAttributes()) {
                String type = attribute.getType();

                // Vérifier si le type est une classe du diagramme
                ClassElement targetElement = classMap.get(type);
                if (targetElement != null && targetElement != element) {
                    // Créer une relation d'association
                    RelationshipElement relationship = new RelationshipElement(
                            element.getName() + "->" + targetElement.getName(),
                            element,
                            targetElement,
                            RelationshipType.ASSOCIATION
                    );

                    // Si l'attribut est final, c'est plutôt une composition
                    if (attribute.isFinal()) {
                        relationship.setType(RelationshipType.COMPOSITION);
                    }

                    diagram.addElement(relationship);
                }
            }

            // Parcourir les méthodes pour trouver des dépendances
            for (Method method : element.getMethods()) {
                // Vérifier le type de retour
                String returnType = method.getReturnType();
                ClassElement returnElement = classMap.get(returnType);
                if (returnElement != null && returnElement != element) {
                    RelationshipElement relationship = new RelationshipElement(
                            element.getName() + "->" + returnElement.getName(),
                            element,
                            returnElement,
                            RelationshipType.DEPENDENCY
                    );
                    diagram.addElement(relationship);
                }

                // Vérifier les types des paramètres
                for (Parameter parameter : method.getParameters()) {
                    String paramType = parameter.getType();
                    ClassElement paramElement = classMap.get(paramType);
                    if (paramElement != null && paramElement != element) {
                        RelationshipElement relationship = new RelationshipElement(
                                element.getName() + "->" + paramElement.getName(),
                                element,
                                paramElement,
                                RelationshipType.DEPENDENCY
                        );
                        diagram.addElement(relationship);
                    }
                }
            }
        }
    }

    /**
     * Classe interne pour stocker temporairement les informations d'une classe
     */
    private static class ClassInfo {
        // Getters et setters
        @Setter
        @Getter
        private String name;
        @Setter
        @Getter
        private String packageName;
        @Getter
        @Setter
        private ClassElement.ClassType type = ClassElement.ClassType.CLASS;
        private boolean isAbstract;
        @Setter
        @Getter
        private String extendsClass;
        @Setter
        @Getter
        private List<String> implementsInterfaces = new ArrayList<>();
        @Setter
        @Getter
        private List<AttributeInfo> attributes = new ArrayList<>();
        @Setter
        @Getter
        private List<MethodInfo> methods = new ArrayList<>();

        public boolean isAbstract() { return isAbstract; }
        public void setAbstract(boolean isAbstract) { this.isAbstract = isAbstract; }

    }

    /**
     * Classe interne pour stocker temporairement les informations d'un attribut
     */
    private static class AttributeInfo {
        // Getters et setters
        @Setter
        @Getter
        private String name;
        @Setter
        @Getter
        private String type;
        @Setter
        @Getter
        private String defaultValue;
        @Setter
        @Getter
        private Visibility visibility = Visibility.PRIVATE;
        private boolean isStatic;
        private boolean isFinal;

        public boolean isStatic() { return isStatic; }
        public void setStatic(boolean isStatic) { this.isStatic = isStatic; }

        public boolean isFinal() { return isFinal; }
        public void setFinal(boolean isFinal) { this.isFinal = isFinal; }
    }

    /**
     * Classe interne pour stocker temporairement les informations d'une méthode
     */
    private static class MethodInfo {
        // Getters et setters
        @Setter
        @Getter
        private String name;
        @Setter
        @Getter
        private String returnType;
        @Setter
        @Getter
        private Visibility visibility = Visibility.PUBLIC;
        private boolean isStatic;
        private boolean isAbstract;
        private boolean isFinal;
        @Setter
        @Getter
        private List<ParameterInfo> parameters = new ArrayList<>();

        public boolean isStatic() { return isStatic; }
        public void setStatic(boolean isStatic) { this.isStatic = isStatic; }

        public boolean isAbstract() { return isAbstract; }
        public void setAbstract(boolean isAbstract) { this.isAbstract = isAbstract; }

        public boolean isFinal() { return isFinal; }
        public void setFinal(boolean isFinal) { this.isFinal = isFinal; }

    }

    /**
     * Classe interne pour stocker temporairement les informations d'un paramètre
     */
    @Setter
    @Getter
    private static class ParameterInfo {
        // Getters et setters
        private String name;
        private String type;

    }
}