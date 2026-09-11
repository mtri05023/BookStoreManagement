package model;

public class Employee {
    private String id;
    private String name;
    private String position;

    public Employee(String id, String name, String position) {
        this.id = id;
        this.name = name;
        this.position = position;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getPosition() { return position; }

    @Override
    public String toString() {
        return id + " - " + name + " - " + position;
    }
}
