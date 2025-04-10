package com.diagramme.service;

import com.diagramme.dto.RecentDiagramDTO;
import com.diagramme.model.ClassDiagram;
import com.diagramme.model.ClassElement;
import com.diagramme.model.RelationshipElement;
import com.diagramme.model.enums.RelationshipType;
import com.diagramme.repository.ClassDiagramRepository;
import com.diagramme.repository.ClassElementRepository;
import com.diagramme.repository.RelationshipElementRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class DiagramServiceTest {

    @Mock
    private ClassDiagramRepository diagramRepository;

    @Mock
    private ClassElementRepository classElementRepository;

    @Mock
    private RelationshipElementRepository relationshipRepository;

    @Mock
    private JavaParserService javaParserService;

    @Mock
    private RecentProjectsService recentProjectsService;

    @InjectMocks
    private DiagramServiceImpl diagramService;

    private ClassDiagram testDiagram;
    private ClassElement testClass;
    private RelationshipElement testRelationship;

    @BeforeEach
    public void setUp() {
        // Créer des objets de test
        testDiagram = new ClassDiagram("Test Diagram");
        testDiagram.setId(1L);

        testClass = new ClassElement("TestClass");
        testClass.setId(1L);

        ClassElement targetClass = new ClassElement("TargetClass");
        targetClass.setId(2L);

        testRelationship = new RelationshipElement("TestRelation", testClass, targetClass, RelationshipType.ASSOCIATION);
        testRelationship.setId(1L);
    }

    @Test
    public void testSaveDiagram() {
        // Configuration du mock
        when(diagramRepository.save(any(ClassDiagram.class))).thenReturn(testDiagram);

        // Appel de la méthode à tester
        ClassDiagram savedDiagram = diagramService.saveDiagram(testDiagram);

        // Vérifications
        assertNotNull(savedDiagram);
        assertEquals(testDiagram.getId(), savedDiagram.getId());
        assertEquals(testDiagram.getName(), savedDiagram.getName());

        // Vérifier que le repository a été appelé une fois
        verify(diagramRepository, times(1)).save(testDiagram);
    }

    @Test
    public void testGetDiagramById() {
        // Configuration du mock
        when(diagramRepository.findById(1L)).thenReturn(Optional.of(testDiagram));

        // Appel de la méthode à tester
        Optional<ClassDiagram> result = diagramService.getDiagramById(1L);

        // Vérifications
        assertTrue(result.isPresent());
        assertEquals(testDiagram, result.get());

        // Vérifier que le repository a été appelé une fois
        verify(diagramRepository, times(1)).findById(1L);
    }

    @Test
    public void testGetDiagramByIdNotFound() {
        // Configuration du mock
        when(diagramRepository.findById(999L)).thenReturn(Optional.empty());

        // Appel de la méthode à tester
        Optional<ClassDiagram> result = diagramService.getDiagramById(999L);

        // Vérifications
        assertFalse(result.isPresent());

        // Vérifier que le repository a été appelé une fois
        verify(diagramRepository, times(1)).findById(999L);
    }

    @Test
    public void testGetAllDiagrams() {
        // Configuration du mock
        when(diagramRepository.findAll()).thenReturn(Arrays.asList(testDiagram));

        // Appel de la méthode à tester
        List<ClassDiagram> diagrams = diagramService.getAllDiagrams();

        // Vérifications
        assertNotNull(diagrams);
        assertEquals(1, diagrams.size());
        assertEquals(testDiagram, diagrams.get(0));

        // Vérifier que le repository a été appelé une fois
        verify(diagramRepository, times(1)).findAll();
    }

    @Test
    public void testDeleteDiagram() {
        // Appel de la méthode à tester
        diagramService.deleteDiagram(1L);

        // Vérifier que le repository a été appelé une fois
        verify(diagramRepository, times(1)).deleteById(1L);
    }

    @Test
    public void testCreateNewDiagram() {
        // Configuration du mock
        when(diagramRepository.save(any(ClassDiagram.class))).thenAnswer(invocation -> {
            ClassDiagram diagram = invocation.getArgument(0);
            diagram.setId(1L);
            return diagram;
        });

        // Appel de la méthode à tester
        ClassDiagram newDiagram = diagramService.createNewDiagram("New Diagram");

        // Vérifications
        assertNotNull(newDiagram);
        assertEquals("New Diagram", newDiagram.getName());
        assertEquals(1L, newDiagram.getId());

        // Vérifier que le repository a été appelé une fois
        verify(diagramRepository, times(1)).save(any(ClassDiagram.class));
    }

    @Test
    public void testConvertToDTO() {
        // Préparation du test
        ClassDiagram diagram = new ClassDiagram("Test Diagram");
        diagram.setId(1L);
        diagram.setUuid("test-uuid");
        diagram.setDescription("Test Description");
        diagram.setAuthor("Test Author");
        diagram.setVersion("1.0");
        diagram.setCreatedAt(LocalDateTime.now());
        diagram.setModifiedAt(LocalDateTime.now());
        diagram.setShowGrid(true);
        diagram.setSnapToGrid(true);
        diagram.setGridSize(20.0);
        diagram.setBackgroundColor("#FFFFFF");

        // Création de quelques éléments pour le test
        ClassElement classElement = new ClassElement("TestClass");
        diagram.addElement(classElement);

        // Appel de la méthode à tester
        RecentDiagramDTO dto = diagramService.convertToDTO(diagram);

        // Vérifications
        assertNotNull(dto);
        assertEquals(diagram.getId(), dto.getId());
        assertEquals(diagram.getUuid(), dto.getUuid());
        assertEquals(diagram.getName(), dto.getName());
        assertEquals(diagram.getDescription(), dto.getDescription());
        assertEquals(diagram.getAuthor(), dto.getAuthor());
        assertEquals(diagram.getVersion(), dto.getVersion());
        assertEquals(diagram.getCreatedAt(), dto.getCreatedAt());
        assertEquals(diagram.getModifiedAt(), dto.getModifiedAt());
        assertEquals(diagram.isShowGrid(), dto.isShowGrid());
        assertEquals(diagram.isSnapToGrid(), dto.isSnapToGrid());
        assertEquals(diagram.getGridSize(), dto.getGridSize());
        assertEquals(diagram.getBackgroundColor(), dto.getBackgroundColor());
        assertEquals(1, dto.getElementCount());
    }
}