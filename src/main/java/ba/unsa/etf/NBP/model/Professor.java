package ba.unsa.etf.NBP.model;

public class Professor {

    private Long id;
    private Long userId;
    private String title;
    private Long departmentId;
    private String officeLocation;

    public Professor() {}

    public Professor(Long id, Long userId, String title, Long departmentId, String officeLocation) {
        this.id = id;
        this.userId = userId;
        this.title = title;
        this.departmentId = departmentId;
        this.officeLocation = officeLocation;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public Long getDepartmentId() { return departmentId; }
    public void setDepartmentId(Long departmentId) { this.departmentId = departmentId; }

    public String getOfficeLocation() { return officeLocation; }
    public void setOfficeLocation(String officeLocation) { this.officeLocation = officeLocation; }
}
