package ba.unsa.etf.NBP.model;

public class Room {

    private Long id;
    private String name;
    private String building;

    public Room() {}

    public Room(Long id, String name, String building) {
        this.id = id;
        this.name = name;
        this.building = building;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getBuilding() { return building; }
    public void setBuilding(String building) { this.building = building; }
}
