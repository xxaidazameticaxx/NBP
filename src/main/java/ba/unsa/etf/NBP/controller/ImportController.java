package ba.unsa.etf.NBP.controller;

import ba.unsa.etf.NBP.service.XmlImportService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/import")
public class ImportController {

    private final XmlImportService xmlImportService;

    public ImportController(XmlImportService xmlImportService) {
        this.xmlImportService = xmlImportService;
    }

    /**
     * Accepts the XML document either as a multipart file upload (type=file in Postman / browser
     * file inputs) or as a plain form-data text field (type=text in Postman / curl -F).
     *
     * <p>Uses {@link MultipartHttpServletRequest} directly to avoid Spring's
     * {@code RequestParamMethodArgumentResolver} attempting to convert a {@code MultipartFile}
     * to {@code String} when both a file part and a text part share the same field name.
     */
    @PostMapping(value = "/xml", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.TEXT_PLAIN_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> importXml(MultipartHttpServletRequest request) {
        byte[] bytes = resolveBytes(request);
        try {
            return ResponseEntity.ok(xmlImportService.importXml(bytes));
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    private byte[] resolveBytes(MultipartHttpServletRequest request) {
        MultipartFile file = request.getFile("file");
        if (file != null && !file.isEmpty()) {
            try {
                return file.getBytes();
            } catch (IOException e) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Failed to read uploaded file");
            }
        }
        String fileText = request.getParameter("file");
        if (fileText != null && !fileText.isEmpty()) {
            return fileText.getBytes(StandardCharsets.UTF_8);
        }
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Request part 'file' is empty or missing");
    }
}
