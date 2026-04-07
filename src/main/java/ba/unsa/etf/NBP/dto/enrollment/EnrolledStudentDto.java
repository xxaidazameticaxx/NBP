package ba.unsa.etf.NBP.dto.enrollment;

import java.time.LocalDate;

public class EnrolledStudentDto {

    private Long studentId;
    private Long userId;
    private String indexNumber;
    private Long enrollmentYear;
    private LocalDate enrollmentDate;
    private Long studyProgramId;
    private String studyProgramName;
    private String studyProgramCode;

    public EnrolledStudentDto() {}

    public Long getStudentId() { return studentId; }
    public void setStudentId(Long studentId) { this.studentId = studentId; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getIndexNumber() { return indexNumber; }
    public void setIndexNumber(String indexNumber) { this.indexNumber = indexNumber; }

    public Long getEnrollmentYear() { return enrollmentYear; }
    public void setEnrollmentYear(Long enrollmentYear) { this.enrollmentYear = enrollmentYear; }

    public LocalDate getEnrollmentDate() { return enrollmentDate; }
    public void setEnrollmentDate(LocalDate enrollmentDate) { this.enrollmentDate = enrollmentDate; }

    public Long getStudyProgramId() { return studyProgramId; }
    public void setStudyProgramId(Long studyProgramId) { this.studyProgramId = studyProgramId; }

    public String getStudyProgramName() { return studyProgramName; }
    public void setStudyProgramName(String studyProgramName) { this.studyProgramName = studyProgramName; }

    public String getStudyProgramCode() { return studyProgramCode; }
    public void setStudyProgramCode(String studyProgramCode) { this.studyProgramCode = studyProgramCode; }
}