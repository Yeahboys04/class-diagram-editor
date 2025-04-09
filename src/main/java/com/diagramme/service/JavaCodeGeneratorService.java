package com.diagramme.service;

import com.diagramme.model.*;
import com.diagramme.model.enums.RelationshipType;
import com.diagramme.model.enums.Visibility;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service pour générer du code Java à partir d'un diagramme de classe
 */
@Service
@Slf4j
public class JavaCodeGeneratorService {

    /**
     * Génère des fichiers Java à partir d'un diagramme de classe
     *
     * @param diagram Le diagramme de classe
     * @param outputDir Le répertoire de sortie
     * @return Le nombre de fichiers générés
     */
    public int generateJavaCode(ClassDiagram diagram, File outputDir) throws IOException {
        log.debug("Génération de code Java pour le diagramme: {}", diagram.getName());

        // Vérifier que le répertoire existe
        if (!outputDir.exists()) {
            outputDir.mkdirs();
        }

        if (!outputDir.isDirectory()) {
            throw new IllegalArgumentException("Le chemin spécifié n'est pas un répertoire valide: " + outputDir.getAbsolutePath());
        }

        // Créer une carte des éléments de classe par nom
        Map<String, ClassElement> classMap = new HashMap<>();
        for (ClassElement element : diagram.getClasses()) {
            classMap.put(element.getName(), element);
        }

        // Générer le code pour chaque classe
        int fileCount = 0;
        for (ClassElement element : diagram.getClasses()) {
            String javaCode = generateClassCode(element, diagram.getRelationships(), classMap);

            // Créer les répertoires pour le package si nécessaire
            String packagePath = "";
            if (element.getPackageName() != null && !element.getPackageName().isEmpty()) {
                packagePath = element.getPackageName().replace('.', File.separatorChar);
                Files.createDirectories(Path.of(outputDir.getAbsolutePath(), packagePath));
            }

            // Écrire le fichier Java
            File javaFile = new File(outputDir, packagePath + File.separator + element.getName() + ".java");
            try (FileWriter writer = new FileWriter(javaFile)) {
                writer.write(javaCode);
                fileCount++;
                log.debug("Fichier généré: {}", javaFile.getAbsolutePath());
            }
        }

        return fileCount;
    }

    /**
     * Génère le code Java pour une classe
     */
    private String generateClassCode(ClassElement element, List<RelationshipElement> relationships, Map<String, ClassElement> classMap) {
        StringBuilder sb = new StringBuilder();

        // Package
        if (element.getPackageName() != null && !element.getPackageName().isEmpty()) {
            sb.append("package ").append(element.getPackageName()).append(";\n\n");
        }

        // Imports
        sb.append(generateImports(element, relationships, classMap)).append("\n");

        // Javadoc de classe
        sb.append("/**\n");
        sb.append(" * ").append(element.getName()).append("\n");
        sb.append(" */\n");

        // Définition de classe
        switch (element.getType()) {
            case INTERFACE:
                sb.append("public interface ").append(element.getName());

                // Extensions
                List<String> extendedInterfaces = findExtendedInterfaces(element, relationships);
                if (!extendedInterfaces.isEmpty()) {
                    sb.append(" extends ");
                    for (int i = 0; i < extendedInterfaces.size(); i++) {
                        sb.append(extendedInterfaces.get(i));
                        if (i < extendedInterfaces.size() - 1) {
                            sb.append(", ");
                        }
                    }
                }

                sb.append(" {\n\n");
                break;

            case ENUM:
                sb.append("public enum ").append(element.getName()).append(" {\n\n");
                sb.append("\t// TODO: Ajouter les valeurs de l'énumération\n\n");
                break;

            default: // CLASS ou ABSTRACT_CLASS
                if (element.isAbstract()) {
                    sb.append("public abstract class ");
                } else {
                    sb.append("public class ");
                }

                sb.append(element.getName());

                // Héritage
                String parentClass = findParentClass(element, relationships);
                if (parentClass != null) {
                    sb.append(" extends ").append(parentClass);
                }

                // Implémentation d'interfaces
                List<String> implementedInterfaces = findImplementedInterfaces(element, relationships);
                if (!implementedInterfaces.isEmpty()) {
                    sb.append(" implements ");
                    for (int i = 0; i < implementedInterfaces.size(); i++) {
                        sb.append(implementedInterfaces.get(i));
                        if (i < implementedInterfaces.size() - 1) {
                            sb.append(", ");
                        }
                    }
                }

                sb.append(" {\n\n");
        }

        // Attributs
        for (Attribute attribute : element.getAttributes()) {
            // Javadoc de l'attribut
            sb.append("\t/**\n");
            sb.append("\t * ").append(attribute.getName()).append("\n");
            sb.append("\t */\n");

            // Déclaration de l'attribut
            sb.append("\t");

            // Visibilité
            sb.append(getVisibilityKeyword(attribute.getVisibility())).append(" ");

            // Modificateurs
            if (attribute.isStatic()) {
                sb.append("static ");
            }
            if (attribute.isFinal()) {
                sb.append("final ");
            }

            // Type et nom
            sb.append(attribute.getType()).append(" ").append(attribute.getName());

            // Valeur par défaut
            if (attribute.getDefaultValue() != null && !attribute.getDefaultValue().isEmpty()) {
                sb.append(" = ").append(attribute.getDefaultValue());
            }

            sb.append(";\n\n");
        }

        // Méthodes
        for (Method method : element.getMethods()) {
            // Javadoc de la méthode
            sb.append("\t/**\n");
            sb.append("\t * ").append(method.getName()).append("\n");

            // Paramètres dans le JavaDoc
            for (Parameter parameter : method.getParameters()) {
                sb.append("\t * @param ").append(parameter.getName()).append(" Description du paramètre\n");
            }

            // Retour dans le JavaDoc
            if (method.getReturnType() != null && !method.getReturnType().equals("void")) {
                sb.append("\t * @return Description de la valeur de retour\n");
            }

            sb.append("\t */\n");

            // Déclaration de la méthode
            sb.append("\t");

            // Visibilité
            sb.append(getVisibilityKeyword(method.getVisibility())).append(" ");

            // Modificateurs
            if (method.isStatic()) {
                sb.append("static ");
            }
            if (method.isAbstract()) {
                sb.append("abstract ");
            }
            if (method.isFinal()) {
                sb.append("final ");
            }

            // Type de retour
            String returnType = method.getReturnType() != null && !method.getReturnType().isEmpty() ?
                    method.getReturnType() : "void";
            sb.append(returnType).append(" ");

            // Nom et paramètres
            sb.append(method.getName()).append("(");

            // Liste des paramètres
            List<Parameter> parameters = method.getParameters();
            for (int i = 0; i < parameters.size(); i++) {
                Parameter parameter = parameters.get(i);
                sb.append(parameter.getType()).append(" ").append(parameter.getName());

                if (parameter.getDefaultValue() != null && !parameter.getDefaultValue().isEmpty()) {
                    sb.append(" /* = ").append(parameter.getDefaultValue()).append(" */");
                }

                if (i < parameters.size() - 1) {
                    sb.append(", ");
                }
            }

            sb.append(") ");

            // Corps de la méthode ou point-virgule pour les interfaces et méthodes abstraites
            if (element.getType() == ClassElement.ClassType.INTERFACE || method.isAbstract()) {
                sb.append(";\n\n");
            } else {
                sb.append("{\n");

                // Corps par défaut
                sb.append("\t\t// TODO: Implémentation de la méthode\n");

                // Retour par défaut
                if (!returnType.equals("void")) {
                    sb.append("\t\treturn ");

                    // Valeur par défaut selon le type
                    if (returnType.equals("boolean")) {
                        sb.append("false");
                    } else if (returnType.equals("int") || returnType.equals("long") ||
                            returnType.equals("short") || returnType.equals("byte") ||
                            returnType.equals("float") || returnType.equals("double")) {
                        sb.append("0");
                    } else if (returnType.equals("char")) {
                        sb.append("'\\0'");
                    } else {
                        sb.append("null");
                    }

                    sb.append(";\n");
                }

                sb.append("\t}\n\n");
            }
        }

        // Fermeture de la classe
        sb.append("}\n");

        return sb.toString();
    }

    /**
     * Génère les imports nécessaires
     */
    private String generateImports(ClassElement element, List<RelationshipElement> relationships, Map<String, ClassElement> classMap) {
        StringBuilder sb = new StringBuilder();

        // Map pour éviter les doublons d'imports
        Map<String, String> imports = new HashMap<>();

        // Parcourir les attributs
        for (Attribute attribute : element.getAttributes()) {
            addImportForType(attribute.getType(), element.getPackageName(), imports, classMap);
        }

        // Parcourir les méthodes
        for (Method method : element.getMethods()) {
            // Type de retour
            if (method.getReturnType() != null && !method.getReturnType().isEmpty() && !method.getReturnType().equals("void")) {
                addImportForType(method.getReturnType(), element.getPackageName(), imports, classMap);
            }

            // Types des paramètres
            for (Parameter parameter : method.getParameters()) {
                addImportForType(parameter.getType(), element.getPackageName(), imports, classMap);
            }
        }

        // Ajouter les types utilisés dans les relations
        for (RelationshipElement relationship : relationships) {
            // Vérifier si c'est la source ou la cible
            if (relationship.getSourceElement() == element) {
                ClassElement targetElement = relationship.getTargetElement();
                String targetPackage = targetElement.getPackageName();
                String targetName = targetElement.getName();

                if (targetPackage != null && !targetPackage.isEmpty() &&
                        !targetPackage.equals(element.getPackageName())) {
                    imports.put(targetName, "import " + targetPackage + "." + targetName + ";\n");
                }
            }
        }

        // Ajouter les imports au code
        for (String importLine : imports.values()) {
            sb.append(importLine);
        }

        return sb.toString();
    }

    /**
     * Ajoute un import pour un type si nécessaire
     */
    private void addImportForType(String type, String currentPackage, Map<String, String> imports, Map<String, ClassElement> classMap) {
        // Ignorer les types primitifs et les types dans le package java.lang
        if (isPrimitiveType(type) || type.startsWith("java.lang.")) {
            return;
        }

        // Vérifier si le type est un type paramétré (générique)
        if (type.contains("<")) {
            // Extraire le type principal
            String mainType = type.substring(0, type.indexOf("<"));
            addImportForType(mainType, currentPackage, imports, classMap);

            // Extraire les types de paramètres
            String paramTypes = type.substring(type.indexOf("<") + 1, type.lastIndexOf(">"));
            String[] params = paramTypes.split(",");
            for (String param : params) {
                addImportForType(param.trim(), currentPackage, imports, classMap);
            }

            return;
        }

        // Vérifier si c'est un tableau
        if (type.endsWith("[]")) {
            addImportForType(type.substring(0, type.length() - 2), currentPackage, imports, classMap);
            return;
        }

        // Vérifier si le type est une classe du projet
        ClassElement classElement = classMap.get(type);
        if (classElement != null) {
            String packageName = classElement.getPackageName();

            // Ajouter un import seulement si le package est différent
            if (packageName != null && !packageName.isEmpty() && !packageName.equals(currentPackage)) {
                imports.put(type, "import " + packageName + "." + type + ";\n");
            }
        } else if (type.contains(".")) {
            // Type complet (avec package)
            imports.put(type, "import " + type + ";\n");
        } else {
            // Type simple, supposé être dans java.util
            imports.put(type, "import java.util." + type + ";\n");
        }
    }

    /**
     * Vérifie si un type est un type primitif Java
     */
    private boolean isPrimitiveType(String type) {
        return type.equals("boolean") || type.equals("byte") || type.equals("char") ||
                type.equals("short") || type.equals("int") || type.equals("long") ||
                type.equals("float") || type.equals("double") || type.equals("void");
    }

    /**
     * Trouve la classe parente d'un élément
     */
    private String findParentClass(ClassElement element, List<RelationshipElement> relationships) {
        for (RelationshipElement relationship : relationships) {
            if (relationship.getSourceElement() == element &&
                    relationship.getType() == RelationshipType.INHERITANCE) {
                return relationship.getTargetElement().getName();
            }
        }
        return null;
    }

    /**
     * Trouve les interfaces étendues par une interface
     */
    private List<String> findExtendedInterfaces(ClassElement element, List<RelationshipElement> relationships) {
        return relationships.stream()
                .filter(r -> r.getSourceElement() == element &&
                        (r.getType() == RelationshipType.INHERITANCE || r.getType() == RelationshipType.IMPLEMENTATION))
                .map(r -> r.getTargetElement().getName())
                .toList();
    }

    /**
     * Trouve les interfaces implémentées par une classe
     */
    private List<String> findImplementedInterfaces(ClassElement element, List<RelationshipElement> relationships) {
        return relationships.stream()
                .filter(r -> r.getSourceElement() == element && r.getType() == RelationshipType.IMPLEMENTATION)
                .map(r -> r.getTargetElement().getName())
                .toList();
    }

    /**
     * Retourne le mot-clé Java pour une visibilité
     */
    private String getVisibilityKeyword(Visibility visibility) {
        return switch (visibility) {
            case PUBLIC -> "public";
            case PRIVATE -> "private";
            case PROTECTED -> "protected";
            case PACKAGE -> "";
        };
    }
}