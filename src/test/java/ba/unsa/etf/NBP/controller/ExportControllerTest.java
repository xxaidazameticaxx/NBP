package ba.unsa.etf.NBP.controller;

import ba.unsa.etf.NBP.service.XmlExportService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExportControllerTest {

    @Mock
    private XmlExportService xmlExportService;

    private ExportController controller;

    @BeforeEach
    void setUp() {
        controller = new ExportController(xmlExportService);
    }

    @Test
    void exportXml_returnsXmlAttachment() {
        byte[] xml = "<export/>".getBytes(StandardCharsets.UTF_8);
        when(xmlExportService.exportTables(List.of("NBP_STUDENT"))).thenReturn(xml);

        ResponseEntity<byte[]> response = controller.exportXml(List.of("NBP_STUDENT"));

        assertEquals(200, response.getStatusCode().value());
        assertEquals(MediaType.APPLICATION_XML, response.getHeaders().getContentType());

        String contentDisposition = response.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION);
        assertNotNull(contentDisposition);
        assertTrue(contentDisposition.contains("attachment"));
        assertTrue(contentDisposition.contains(".xml"));

        assertArrayEquals(xml, response.getBody());
    }

    @Test
    void exportXml_invalidInput_mapsToBadRequest() {
        when(xmlExportService.exportTables(List.of("X;DROP")))
                .thenThrow(new IllegalArgumentException("Invalid table name: X;DROP"));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> controller.exportXml(List.of("X;DROP")));

        assertEquals(400, ex.getStatusCode().value());
    }
}
