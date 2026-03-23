package ba.unsa.etf.NBP.model;

public class StudyProgram {

    private Long id;
    private String name;
    private String code;
    private Long departmentId;
    private Long durationYears;
    private String degreeType;

    public StudyProgram() {}

    public StudyProgram(Long id, String name, String code, Long departmentId,
                        Long durationYears, String degreeType) {
        this.id = id;
        this.name = name;
        this.code = code;
        this.departmentId = departmentId;
        this.durationYears = durationYears;
        this.degreeType = degreeType;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public Long getDepartmentId() { return departmentId; }
    public void setDepartmentId(Long departmentId) { this.departmentId = departmentId; }

    public Long getDurationYears() { return durationYears; }
    public void setDurationYears(Long durationYears) { this.durationYears = durationYears; }

    public String getDegreeType() { return degreeType; }
    public void setDegreeType(String degreeType) { this.degreeType = degreeType; }
}
