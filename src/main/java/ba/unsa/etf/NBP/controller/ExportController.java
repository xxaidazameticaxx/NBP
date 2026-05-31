package ba.unsa.etf.NBP.controller;

import ba.unsa.etf.NBP.service.XmlExportService;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Endpoints for exporting database content.
 */
@RestController
@RequestMapping("/export")
public class ExportController {

    private final XmlExportService xmlExportService;

    public ExportController(XmlExportService xmlExportService) {
        this.xmlExportService = xmlExportService;
    }

    /**
     * Exports one or more tables into a single XML file.
     * <p>
     * Example: {@code GET /export/xml?tables=NBP_STUDENT,NBP_COURSE}
     */
    @GetMapping(value = "/xml", produces = MediaType.APPLICATION_XML_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<byte[]> exportXml(@RequestParam(name = "tables") List<String> tables) {
        byte[] xml;
        try {
            xml = xmlExportService.exportTables(tables);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }

        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
        String filename = "export-" + timestamp + ".xml";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_XML);
        headers.setContentDisposition(ContentDisposition.attachment().filename(filename).build());
        headers.setCacheControl("no-store");

        return ResponseEntity.ok()
                .headers(headers)
                .body(xml);
    }
}
