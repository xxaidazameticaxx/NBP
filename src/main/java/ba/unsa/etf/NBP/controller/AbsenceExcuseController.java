package ba.unsa.etf.NBP.controller;

import ba.unsa.etf.NBP.model.AbsenceExcuse;
import ba.unsa.etf.NBP.model.User;
import ba.unsa.etf.NBP.service.AbsenceExcuseService;
import ba.unsa.etf.NBP.service.AuthService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.List;

/**
 * Absence-excuse endpoints under {@code /absence-excuses}.
 * <p>
 * Students submit an excuse (a reason plus a PDF document) for a missed session.
 * Professors then approve or reject each excuse, and can download the PDF.
 */
@RestController
@RequestMapping("/absence-excuses")
public class AbsenceExcuseController {

    private final AbsenceExcuseService absenceExcuseService;
    private final AuthService authService;

    public AbsenceExcuseController(AbsenceExcuseService absenceExcuseService, AuthService authService) {
        this.absenceExcuseService = absenceExcuseService;
        this.authService = authService;
    }

    /**
     * Submits an absence excuse with a PDF attachment for a missed session.
     *
     * @param courseSessionId session the student was absent from
     * @param reason          written explanation
     * @param document        PDF file supporting the excuse
     * @return {@code 201 Created} with the saved excuse
     * @throws IOException             if the uploaded file cannot be read
     * @throws ResponseStatusException 400 if the document is missing or not a PDF
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<AbsenceExcuse> submitExcuse(
            @RequestParam("courseSessionId") Long courseSessionId,
            @RequestParam("reason") String reason,
            @RequestPart("document") MultipartFile document) throws IOException {

        if (document.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "PDF document is required");
        }
        String contentType = document.getContentType();
        if (contentType == null || !contentType.equalsIgnoreCase(MediaType.APPLICATION_PDF_VALUE)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only PDF files are accepted");
        }

        User currentUser = getAuthenticatedUser();
        AbsenceExcuse excuse = absenceExcuseService.submitExcuse(
                courseSessionId, reason,
                document.getBytes(), document.getOriginalFilename(),
                currentUser);
        return ResponseEntity.status(HttpStatus.CREATED).body(excuse);
    }

    /**
     * Downloads the PDF attached to an absence excuse.
     *
     * @param id excuse ID
     * @return the PDF bytes with {@code Content-Disposition: attachment}
     * @throws ResponseStatusException 404 if the excuse or document is missing
     */
    @GetMapping("/{id}/document")
    public ResponseEntity<byte[]> downloadDocument(@PathVariable Long id) {
        AbsenceExcuse excuse = absenceExcuseService.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Excuse not found"));

        byte[] document = excuse.getDocument();
        if (document == null || document.length == 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No document attached to this excuse");
        }

        String filename = excuse.getDocumentName() != null ? excuse.getDocumentName() : "document.pdf";
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .body(document);
    }

    /**
     * Returns a single absence excuse by ID.
     *
     * @param id excuse ID
     * @return the excuse
     * @throws ResponseStatusException 404 if not found
     */
    @GetMapping("/{id}")
    public ResponseEntity<AbsenceExcuse> findById(@PathVariable Long id) {
        AbsenceExcuse excuse = absenceExcuseService.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Excuse not found"));
        return ResponseEntity.ok(excuse);
    }

    /**
     * Approves an absence excuse. Intended for the course's professor.
     *
     * @param id excuse ID
     * @return {@code 200 OK}
     */
    @PutMapping("/{id}/approve")
    public ResponseEntity<Void> approveExcuse(@PathVariable Long id) {
        User currentUser = getAuthenticatedUser();
        absenceExcuseService.approveExcuse(id, currentUser);
        return ResponseEntity.ok().build();
    }

    /**
     * Rejects an absence excuse. Intended for the course's professor.
     *
     * @param id excuse ID
     * @return {@code 200 OK}
     */
    @PutMapping("/{id}/reject")
    public ResponseEntity<Void> rejectExcuse(@PathVariable Long id) {
        User currentUser = getAuthenticatedUser();
        absenceExcuseService.rejectExcuse(id, currentUser);
        return ResponseEntity.ok().build();
    }

    /**
     * Lists every excuse submitted by a student.
     *
     * @param studentId student ID
     * @return the student's excuses
     */
    @GetMapping("/student/{studentId}")
    public ResponseEntity<List<AbsenceExcuse>> findByStudentId(@PathVariable Long studentId) {
        return ResponseEntity.ok(absenceExcuseService.findByStudentId(studentId));
    }

    /**
     * Lists every excuse submitted for a specific course session.
     *
     * @param sessionId session ID
     * @return excuses for that session
     */
    @GetMapping("/session/{sessionId}")
    public ResponseEntity<List<AbsenceExcuse>> findBySessionId(@PathVariable Long sessionId) {
        return ResponseEntity.ok(absenceExcuseService.findByCourseSessionId(sessionId));
    }

    /**
     * Lists pending excuses that the calling professor is responsible for reviewing.
     *
     * @return pending excuses for the caller's courses
     */
    @GetMapping("/pending")
    public ResponseEntity<List<AbsenceExcuse>> findPending() {
        User currentUser = getAuthenticatedUser();
        return ResponseEntity.ok(absenceExcuseService.findPendingByProfessor(currentUser));
    }

    /**
     * Returns the authenticated user from the security context, or 401 if none.
     *
     * @return the authenticated {@link User}
     */
    private User getAuthenticatedUser() {
        return authService.getAuthenticatedUserFromContext()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized"));
    }
}
