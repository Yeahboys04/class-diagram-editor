package com.diagramme.service;

import com.diagramme.model.ClassDiagram;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Service pour l'export de diagrammes dans différents formats
 */
@Service
@Slf4j
public class ExportService {

    private final Environment env;

    @Autowired
    public ExportService(Environment env) {
        this.env = env;
    }

    /**
     * Exporte un diagramme vers un fichier
     *
     * @param diagram Le diagramme à exporter
     * @param format Le format d'export ("PNG", "SVG", "PDF", "JSON", "XMI")
     * @param outputFile Le fichier de sortie
     * @return Le fichier créé
     */
    public File exportDiagram(ClassDiagram diagram, String format, File outputFile) throws IOException {
        log.debug("Export du diagramme {} au format {}", diagram.getName(), format);

        switch (format.toUpperCase()) {
            case "PNG":
                return exportToPNG(diagram, outputFile);
            case "SVG":
                return exportToSVG(diagram, outputFile);
            case "PDF":
                return exportToPDF(diagram, outputFile);
            case "JSON":
                return exportToJSON(diagram, outputFile);
            case "XMI":
                return exportToXMI(diagram, outputFile);
            default:
                throw new IllegalArgumentException("Format d'export non supporté: " + format);
        }
    }

    /**
     * Exporte un diagramme vers un fichier PNG
     */
    private File exportToPNG(ClassDiagram diagram, File outputFile) throws IOException {
        // Créer une image
        int width = 1000;
        int height = 800;
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = image.createGraphics();

        // Configurer le rendu
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        // Dessiner le fond
        g2d.setColor(Color.WHITE);
        g2d.fillRect(0, 0, width, height);

        // Dessiner une grille si nécessaire
        if (diagram.isShowGrid()) {
            g2d.setColor(new Color(230, 230, 230));
            for (int x = 0; x < width; x += diagram.getGridSize()) {
                g2d.drawLine(x, 0, x, height);
            }
            for (int y = 0; y < height; y += diagram.getGridSize()) {
                g2d.drawLine(0, y, width, y);
            }
        }

        // Dessiner les classes et relations
        // Note: Implémentation simplifiée, vous devrez adapter cela à votre modèle

        g2d.dispose();

        // Enregistrer l'image
        ImageIO.write(image, "PNG", outputFile);

        return outputFile;
    }

    /**
     * Exporte un diagramme vers un fichier SVG
     */
    private File exportToSVG(ClassDiagram diagram, File outputFile) throws IOException {
        // Créer le contenu SVG
        StringBuilder svg = new StringBuilder();
        svg.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        svg.append("<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"1000\" height=\"800\">\n");

        // Fond
        svg.append("  <rect width=\"1000\" height=\"800\" fill=\"white\" />\n");

        // Grille si nécessaire
        if (diagram.isShowGrid()) {
            svg.append("  <g stroke=\"#e6e6e6\" stroke-width=\"1\">\n");
            double gridSize = diagram.getGridSize();
            for (int x = 0; x < 1000; x += gridSize) {
                svg.append("    <line x1=\"").append(x).append("\" y1=\"0\" x2=\"").append(x).append("\" y2=\"800\" />\n");
            }
            for (int y = 0; y < 800; y += gridSize) {
                svg.append("    <line x1=\"0\" y1=\"").append(y).append("\" x2=\"1000\" y2=\"").append(y).append("\" />\n");
            }
            svg.append("  </g>\n");
        }

        // Dessiner les classes et relations
        // Note: Implémentation simplifiée, vous devrez adapter cela à votre modèle

        svg.append("</svg>");

        // Écrire dans le fichier
        try (FileWriter writer = new FileWriter(outputFile)) {
            writer.write(svg.toString());
        }

        return outputFile;
    }

    /**
     * Exporte un diagramme vers un fichier PDF
     */
    private File exportToPDF(ClassDiagram diagram, File outputFile) throws IOException {
        // Dans une application réelle, utilisez une bibliothèque comme iText ou PDFBox
        log.warn("Export PDF non implémenté complètement");

        // Écrire un contenu fictif pour l'exemple
        String content = "Contenu PDF du diagramme " + diagram.getName();
        Files.write(outputFile.toPath(), content.getBytes());

        return outputFile;
    }

    /**
     * Exporte un diagramme vers un fichier JSON
     */
    private File exportToJSON(ClassDiagram diagram, File outputFile) throws IOException {
        // Dans une application réelle, utilisez une bibliothèque comme Jackson ou Gson
        log.warn("Export JSON non implémenté complètement");

        // Écrire un JSON simplifié pour l'exemple
        StringBuilder json = new StringBuilder();
        json.append("{\n");
        json.append("  \"name\": \"").append(diagram.getName()).append("\",\n");
        json.append("  \"description\": \"").append(diagram.getDescription()).append("\",\n");
        json.append("  \"classes\": [\n");

        // Ajouter les classes
        // Note: Implémentation simplifiée, vous devrez adapter cela à votre modèle

        json.append("  ],\n");
        json.append("  \"relationships\": [\n");

        // Ajouter les relations
        // Note: Implémentation simplifiée, vous devrez adapter cela à votre modèle

        json.append("  ]\n");
        json.append("}");

        // Écrire dans le fichier
        try (FileWriter writer = new FileWriter(outputFile)) {
            writer.write(json.toString());
        }

        return outputFile;
    }

    /**
     * Exporte un diagramme vers un fichier XMI
     */
    private File exportToXMI(ClassDiagram diagram, File outputFile) throws IOException {
        // Dans une application réelle, utilisez une bibliothèque XML ou JAXB
        log.warn("Export XMI non implémenté complètement");

        // Écrire un XMI simplifié pour l'exemple
        StringBuilder xmi = new StringBuilder();
        xmi.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        xmi.append("<xmi:XMI xmlns:xmi=\"http://www.omg.org/XMI\" xmlns:uml=\"http://www.eclipse.org/uml2/5.0.0/UML\">\n");
        xmi.append("  <uml:Model name=\"").append(diagram.getName()).append("\">\n");

        // Ajouter les classes
        // Note: Implémentation simplifiée, vous devrez adapter cela à votre modèle

        xmi.append("  </uml:Model>\n");
        xmi.append("</xmi:XMI>");

        // Écrire dans le fichier
        try (FileWriter writer = new FileWriter(outputFile)) {
            writer.write(xmi.toString());
        }

        return outputFile;
    }

    /**
     * Crée un répertoire temporaire pour l'export
     */
    public String createTempExportDirectory() {
        String tempDir = env.getProperty("app.export.temp.dir", System.getProperty("java.io.tmpdir") + "/diagram-exports");
        Path tempPath = Paths.get(tempDir);

        try {
            if (!Files.exists(tempPath)) {
                Files.createDirectories(tempPath);
                log.debug("Répertoire temporaire d'export créé: {}", tempPath);
            }
        } catch (IOException e) {
            log.error("Erreur lors de la création du répertoire temporaire: {}", tempPath, e);
            // Fallback au répertoire temporaire du système
            tempDir = System.getProperty("java.io.tmpdir");
        }

        return tempDir;
    }
}
