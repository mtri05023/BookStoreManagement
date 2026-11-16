package model;

public class Employee extends Person {
    private String position;
    private String phone = "";

    public Employee(String id, String name, String position) {
        super(id, name);
        this.position = ModelChecks.text(position, "Chức vụ");
    }

    public Employee(String id, String name, String phone, String position) {
        this(id, name, position);
        this.phone = ModelChecks.text(phone, "Điện thoại");
    }
    public String getPhone() { return phone; }
    public String getPosition() { return position; }

    @Override
    public String getRole() { return "Employee"; }

    @Override
    public String toString() {
        return getId() + " - " + getName() + " - " + (phone.isEmpty() ? "Chưa có SĐT" : phone) + " - " + position;
    }
}
