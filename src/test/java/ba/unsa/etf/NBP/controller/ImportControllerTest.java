package ba.unsa.etf.NBP.controller;

import ba.unsa.etf.NBP.service.XmlImportService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.mock.web.MockMultipartHttpServletRequest;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ImportControllerTest {

    @Mock
    private XmlImportService xmlImportService;

    private ImportController controller;

    @BeforeEach
    void setUp() {
        controller = new ImportController(xmlImportService);
    }

    // --- helpers ---

    private static MockMultipartHttpServletRequest requestWithFile(byte[] content) {
        MockMultipartHttpServletRequest request = new MockMultipartHttpServletRequest();
        request.addFile(new MockMultipartFile("file", "import.xml", "application/xml", content));
        return request;
    }

    private static MockMultipartHttpServletRequest requestWithText(String xml) {
        MockMultipartHttpServletRequest request = new MockMultipartHttpServletRequest();
        request.setParameter("file", xml);
        return request;
    }

    // --- tests ---

    @Test
    void importXml_filePartUpload_returnsOkWithSummary() {
        byte[] xml = "<export><table name=\"NBP_ROOM\"><ROWSET/></table></export>".getBytes(StandardCharsets.UTF_8);
        when(xmlImportService.importXml(any())).thenReturn("NBP_ROOM: merged 0 row(s)\nTotal rows merged: 0\nTables processed: 1");

        ResponseEntity<String> response = controller.importXml(requestWithFile(xml));

        assertEquals(200, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals("NBP_ROOM: merged 0 row(s)\nTotal rows merged: 0\nTables processed: 1", response.getBody());
    }

    @Test
    void importXml_textParam_returnsOkWithSummary() {
        when(xmlImportService.importXml(any())).thenReturn("NBP_ROOM: merged 0 row(s)\nTotal rows merged: 0\nTables processed: 1");

        // Simulates Postman form-data type=text (no filename in Content-Disposition)
        ResponseEntity<String> response = controller.importXml(
                requestWithText("<export><table name=\"NBP_ROOM\"><ROWSET/></table></export>"));

        assertEquals(200, response.getStatusCode().value());
        assertNotNull(response.getBody());
    }

    @Test
    void importXml_emptyFilePart_returnsBadRequest() {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> controller.importXml(requestWithFile(new byte[0])));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    @Test
    void importXml_neitherPartNorText_returnsBadRequest() {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> controller.importXml(new MockMultipartHttpServletRequest()));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    @Test
    void importXml_serviceThrowsIllegalArgument_returnsBadRequest() {
        when(xmlImportService.importXml(any())).thenThrow(new IllegalArgumentException("Root element must be <export>"));

        byte[] xml = "<bad/>".getBytes(StandardCharsets.UTF_8);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> controller.importXml(requestWithFile(xml)));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        assertEquals("Root element must be <export>", ex.getReason());
    }
}
