package model;

public class Employee extends Person {
    private String position;

    public Employee(String id, String name, String position) {
        super(id, name);
        this.position = ModelChecks.text(position, "Chức vụ");
    }

    public String getPosition() { return position; }

    @Override
    public String getRole() { return "Employee"; }

    @Override
    public String toString() {
        return getId() + " - " + getName() + " - " + position;
    }
}
