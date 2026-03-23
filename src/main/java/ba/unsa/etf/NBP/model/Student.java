package ba.unsa.etf.NBP.model;

public class Student {

    private Long id;
    private Long userId;
    private String indexNumber;
    private Long studyProgramId;
    private Long enrollmentYear;

    public Student() {}

    public Student(Long id, Long userId, String indexNumber, Long studyProgramId, Long enrollmentYear) {
        this.id = id;
        this.userId = userId;
        this.indexNumber = indexNumber;
        this.studyProgramId = studyProgramId;
        this.enrollmentYear = enrollmentYear;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getIndexNumber() { return indexNumber; }
    public void setIndexNumber(String indexNumber) { this.indexNumber = indexNumber; }

    public Long getStudyProgramId() { return studyProgramId; }
    public void setStudyProgramId(Long studyProgramId) { this.studyProgramId = studyProgramId; }

    public Long getEnrollmentYear() { return enrollmentYear; }
    public void setEnrollmentYear(Long enrollmentYear) { this.enrollmentYear = enrollmentYear; }
}
