package com.diagramme.service;

import com.diagramme.model.ClassDiagram;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;

/**
 * Service pour exporter un diagramme au format PDF
 * Note: Dans une l'application finale, il faudra utiliser une bibliothèque comme iText ou PDFBox
 * Cette implémentation est une version simplifiée qui génère un fichier HTML pouvant être imprimé en PDF
 */
@Service
@Slf4j
public class PdfExportService {

    /**
     * Exporte un diagramme au format PDF (simulé avec HTML)
     */
    public File exportDiagramToPdf(ClassDiagram diagram, File outputFile) throws IOException {
        log.debug("Exportation du diagramme {} au format PDF", diagram.getName());

        // Générer le contenu HTML
        String htmlContent = generateHtmlContent(diagram);

        // Écrire dans le fichier temporaire HTML
        File htmlFile = new File(outputFile.getParentFile(), outputFile.getName().replace(".pdf", ".html"));
        try (OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(htmlFile), StandardCharsets.UTF_8)) {
            writer.write(htmlContent);
        }

        log.info("Fichier HTML généré: {}. Vous pouvez l'ouvrir dans un navigateur et l'imprimer en PDF", htmlFile.getAbsolutePath());

        // Dans une application réelle, utilisez ici une bibliothèque pour convertir HTML en PDF
        // Par exemple, avec Flying Saucer + iText:
        // ITextRenderer renderer = new ITextRenderer();
        // renderer.setDocument(htmlFile);
        // renderer.layout();
        // try (OutputStream os = new FileOutputStream(outputFile)) {
        //     renderer.createPDF(os);
        // }

        return htmlFile;
    }

    /**
     * Génère le contenu HTML pour le diagramme
     */
    private String generateHtmlContent(ClassDiagram diagram) {
        StringBuilder sb = new StringBuilder();

        // En-tête HTML
        sb.append("<!DOCTYPE html>\n");
        sb.append("<html lang=\"fr\">\n");
        sb.append("<head>\n");
        sb.append("  <meta charset=\"UTF-8\">\n");
        sb.append("  <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n");
        sb.append("  <title>").append(diagram.getName()).append("</title>\n");
        sb.append("  <style>\n");
        sb.append("    body { font-family: Arial, sans-serif; margin: 20px; }\n");
        sb.append("    h1 { color: #2c3e50; }\n");
        sb.append("    .info { margin-bottom: 20px; }\n");
        sb.append("    .info p { margin: 5px 0; }\n");
        sb.append("    .class-list { margin-top: 30px; }\n");
        sb.append("    .class-item { border: 1px solid #ddd; margin-bottom: 20px; page-break-inside: avoid; }\n");
        sb.append("    .class-header { background-color: #2c3e50; color: white; padding: 10px; font-weight: bold; }\n");
        sb.append("    .interface-header { background-color: #3498db; }\n");
        sb.append("    .enum-header { background-color: #e74c3c; }\n");
        sb.append("    .abstract-name { font-style: italic; }\n");
        sb.append("    .class-content { padding: 10px; }\n");
        sb.append("    .class-section { margin-bottom: 10px; }\n");
        sb.append("    .class-section h3 { margin: 0 0 5px 0; font-size: 14px; border-bottom: 1px solid #eee; }\n");
        sb.append("    .class-section ul { list-style-type: none; padding-left: 10px; margin: 0; }\n");
        sb.append("    .class-section li { font-family: monospace; margin: 3px 0; }\n");
        sb.append("    .relationship-list { margin-top: 30px; }\n");
        sb.append("    .relationship-item { margin-bottom: 5px; }\n");
        sb.append("    @media print {\n");
        sb.append("      body { font-size: 12px; }\n");
        sb.append("      h1 { font-size: 18px; }\n");
        sb.append("      h2 { font-size: 16px; }\n");
        sb.append("      h3 { font-size: 14px; }\n");
        sb.append("    }\n");
        sb.append("  </style>\n");
        sb.append("</head>\n");
        sb.append("<body>\n");

        // Titre et informations du diagramme
        sb.append("  <h1>Diagramme de classe: ").append(diagram.getName()).append("</h1>\n");
        sb.append("  <div class=\"info\">\n");
        if (diagram.getDescription() != null && !diagram.getDescription().isEmpty()) {
            sb.append("    <p><strong>Description:</strong> ").append(diagram.getDescription()).append("</p>\n");
        }
        if (diagram.getAuthor() != null && !diagram.getAuthor().isEmpty()) {
            sb.append("    <p><strong>Auteur:</strong> ").append(diagram.getAuthor()).append("</p>\n");
        }
        if (diagram.getVersion() != null && !diagram.getVersion().isEmpty()) {
            sb.append("    <p><strong>Version:</strong> ").append(diagram.getVersion()).append("</p>\n");
        }
        sb.append("    <p><strong>Date:</strong> ").append(diagram.getModifiedAt()).append("</p>\n");
        sb.append("  </div>\n");

        // Liste des classes
        sb.append("  <h2>Classes</h2>\n");
        sb.append("  <div class=\"class-list\">\n");

        for (var classElement : diagram.getClasses()) {
            sb.append("    <div class=\"class-item\">\n");

            // En-tête de classe
            String headerClass = "class-header";
            if (classElement.getType() == com.diagramme.model.ClassElement.ClassType.INTERFACE) {
                headerClass += " interface-header";
            } else if (classElement.getType() == com.diagramme.model.ClassElement.ClassType.ENUM) {
                headerClass += " enum-header";
            }

            sb.append("      <div class=\"").append(headerClass).append("\">");

            // Type et nom
            switch (classElement.getType()) {
                case INTERFACE:
                    sb.append("Interface ");
                    break;
                case ENUM:
                    sb.append("Énumération ");
                    break;
                default:
                    sb.append("Classe ");
            }

            if (classElement.isAbstract()) {
                sb.append("<span class=\"abstract-name\">").append(classElement.getName()).append("</span>");
            } else {
                sb.append(classElement.getName());
            }

            // Package
            if (classElement.getPackageName() != null && !classElement.getPackageName().isEmpty()) {
                sb.append(" <small>(").append(classElement.getPackageName()).append(")</small>");
            }

            sb.append("</div>\n");

            // Contenu de la classe
            sb.append("      <div class=\"class-content\">\n");

            // Attributs
            sb.append("        <div class=\"class-section\">\n");
            sb.append("          <h3>Attributs</h3>\n");
            sb.append("          <ul>\n");

            if (classElement.getAttributes().isEmpty()) {
                sb.append("            <li><em>Aucun attribut</em></li>\n");
            } else {
                for (var attribute : classElement.getAttributes()) {
                    sb.append("            <li>");

                    // Visibilité
                    sb.append(attribute.getVisibility().getSymbol()).append(" ");

                    // Nom et type
                    sb.append(attribute.getName()).append(" : ").append(attribute.getType());

                    // Modificateurs
                    if (attribute.isStatic()) {
                        sb.append(" {static}");
                    }
                    if (attribute.isFinal()) {
                        sb.append(" {readOnly}");
                    }

                    sb.append("</li>\n");
                }
            }

            sb.append("          </ul>\n");
            sb.append("        </div>\n");

            // Méthodes
            sb.append("        <div class=\"class-section\">\n");
            sb.append("          <h3>Méthodes</h3>\n");
            sb.append("          <ul>\n");

            if (classElement.getMethods().isEmpty()) {
                sb.append("            <li><em>Aucune méthode</em></li>\n");
            } else {
                for (var method : classElement.getMethods()) {
                    sb.append("            <li>");

                    // Visibilité
                    sb.append(method.getVisibility().getSymbol()).append(" ");

                    // Nom
                    sb.append(method.getName()).append("(");

                    // Paramètres
                    for (int i = 0; i < method.getParameters().size(); i++) {
                        var param = method.getParameters().get(i);
                        sb.append(param.getName()).append(" : ").append(param.getType());
                        if (i < method.getParameters().size() - 1) {
                            sb.append(", ");
                        }
                    }

                    sb.append(")");

                    // Type de retour
                    if (method.getReturnType() != null && !method.getReturnType().isEmpty()) {
                        sb.append(" : ").append(method.getReturnType());
                    }

                    // Modificateurs
                    if (method.isStatic()) {
                        sb.append(" {static}");
                    }
                    if (method.isAbstract()) {
                        sb.append(" {abstract}");
                    }
                    if (method.isFinal()) {
                        sb.append(" {final}");
                    }

                    sb.append("</li>\n");
                }
            }

            sb.append("          </ul>\n");
            sb.append("        </div>\n");

            sb.append("      </div>\n");
            sb.append("    </div>\n");
        }

        sb.append("  </div>\n");

        // Liste des relations
        sb.append("  <h2>Relations</h2>\n");
        sb.append("  <div class=\"relationship-list\">\n");

        if (diagram.getRelationships().isEmpty()) {
            sb.append("    <p><em>Aucune relation</em></p>\n");
        } else {
            for (var relationship : diagram.getRelationships()) {
                sb.append("    <div class=\"relationship-item\">\n");

                // Type de relation
                sb.append("      <strong>").append(relationship.getType().getLabel()).append(":</strong> ");

                // Source et cible
                sb.append(relationship.getSourceElement().getName()).append(" → ").append(relationship.getTargetElement().getName());

                // Rôles et multiplicités
                boolean hasDetails = false;
                StringBuilder details = new StringBuilder();

                if (relationship.getSourceRole() != null && !relationship.getSourceRole().isEmpty()) {
                    details.append(" [Source - Rôle: ").append(relationship.getSourceRole()).append("]");
                    hasDetails = true;
                }

                if (relationship.getTargetRole() != null && !relationship.getTargetRole().isEmpty()) {
                    details.append(" [Cible - Rôle: ").append(relationship.getTargetRole()).append("]");
                    hasDetails = true;
                }

                if (relationship.getSourceMultiplicity() != null && !relationship.getSourceMultiplicity().isEmpty()) {
                    details.append(" [Source - Multiplicité: ").append(relationship.getSourceMultiplicity()).append("]");
                    hasDetails = true;
                }

                if (relationship.getTargetMultiplicity() != null && !relationship.getTargetMultiplicity().isEmpty()) {
                    details.append(" [Cible - Multiplicité: ").append(relationship.getTargetMultiplicity()).append("]");
                    hasDetails = true;
                }

                if (hasDetails) {
                    sb.append("<br><small>").append(details).append("</small>");
                }

                sb.append("\n    </div>\n");
            }
        }

        sb.append("  </div>\n");

        // Pied de page
        sb.append("  <div style=\"margin-top: 30px; text-align: center; font-size: 12px; color: #777;\">\n");
        sb.append("    Généré par l'Éditeur de Diagrammes de Classe\n");
        sb.append("  </div>\n");

        // Fermeture du document HTML
        sb.append("</body>\n");
        sb.append("</html>\n");

        return sb.toString();
    }
}
