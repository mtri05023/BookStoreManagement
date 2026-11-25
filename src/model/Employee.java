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
        String value = ModelChecks.text(phone, "Điện thoại");
        if (!value.matches("0[0-9]{9}")) throw new IllegalArgumentException("Điện thoại nhân viên phải có đúng 10 chữ số và bắt đầu bằng 0.");
        this.phone = value;
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
