package ba.unsa.etf.NBP.model;

public class Course {

    private Long id;
    private String name;
    private String code;
    private Long professorId;
    private Long departmentId;
    private String academicYear;
    private Long semester;
    private Long credits;

    public Course() {}

    public Course(Long id, String name, String code, Long professorId, Long departmentId,
                  String academicYear, Long semester, Long credits) {
        this.id = id;
        this.name = name;
        this.code = code;
        this.professorId = professorId;
        this.departmentId = departmentId;
        this.academicYear = academicYear;
        this.semester = semester;
        this.credits = credits;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public Long getProfessorId() { return professorId; }
    public void setProfessorId(Long professorId) { this.professorId = professorId; }

    public Long getDepartmentId() { return departmentId; }
    public void setDepartmentId(Long departmentId) { this.departmentId = departmentId; }

    public String getAcademicYear() { return academicYear; }
    public void setAcademicYear(String academicYear) { this.academicYear = academicYear; }

    public Long getSemester() { return semester; }
    public void setSemester(Long semester) { this.semester = semester; }

    public Long getCredits() { return credits; }
    public void setCredits(Long credits) { this.credits = credits; }
}
