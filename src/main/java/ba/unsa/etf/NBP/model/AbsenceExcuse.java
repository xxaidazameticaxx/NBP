package ba.unsa.etf.NBP.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.time.LocalDateTime;

public class AbsenceExcuse {

    private Long id;
    private Long studentId;
    private Long courseSessionId;
    private String reason;
    private LocalDateTime submittedAt;
    private String status;
    private Long reviewedBy;
    @JsonIgnore
    private byte[] document;
    private String documentName;

    public AbsenceExcuse() {}

    public AbsenceExcuse(Long id, Long studentId, Long courseSessionId, String reason,
                         LocalDateTime submittedAt, String status, Long reviewedBy,
                         byte[] document, String documentName) {
        this.id = id;
        this.studentId = studentId;
        this.courseSessionId = courseSessionId;
        this.reason = reason;
        this.submittedAt = submittedAt;
        this.status = status;
        this.reviewedBy = reviewedBy;
        this.document = document;
        this.documentName = documentName;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getStudentId() { return studentId; }
    public void setStudentId(Long studentId) { this.studentId = studentId; }

    public Long getCourseSessionId() { return courseSessionId; }
    public void setCourseSessionId(Long courseSessionId) { this.courseSessionId = courseSessionId; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public LocalDateTime getSubmittedAt() { return submittedAt; }
    public void setSubmittedAt(LocalDateTime submittedAt) { this.submittedAt = submittedAt; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Long getReviewedBy() { return reviewedBy; }
    public void setReviewedBy(Long reviewedBy) { this.reviewedBy = reviewedBy; }

    public byte[] getDocument() { return document; }
    public void setDocument(byte[] document) { this.document = document; }

    public String getDocumentName() { return documentName; }
    public void setDocumentName(String documentName) { this.documentName = documentName; }
}
