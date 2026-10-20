package model;

public abstract class Person {
    private final String id;
    private String name;
    protected Person(String id, String name) {
        this.id = ModelChecks.id(id);
        this.name = ModelChecks.text(name, "Tên");
    }
    public String getId() { return id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = ModelChecks.text(name, "Tên"); }
    public abstract String getRole();
}
