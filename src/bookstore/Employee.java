package bookstore;

public class Employee implements Identifiable {
    private final String id;
    private String name;
    public String getId() { return id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = InputHelper.text(name, "Tên"); }
    private String position;
    private String phone = "";

    public Employee(String id, String name, String position) {
        this.id = InputHelper.id(id);
        this.name = InputHelper.text(name, "Tên");
        this.position = InputHelper.text(position, "Chức vụ");
    }

    public Employee(String id, String name, String phone, String position) {
        this(id, name, position);
        String value = InputHelper.text(phone, "Điện thoại");
        if (!value.matches("0[0-9]{9}")) throw new IllegalArgumentException("Điện thoại nhân viên phải có đúng 10 chữ số và bắt đầu bằng 0.");
        this.phone = value;
    }
    public String getPhone() { return phone; }
    public String getPosition() { return position; }

    public String getRole() { return "Employee"; }

    @Override
    public String toString() {
        return getId() + " - " + getName() + " - " + (phone.isEmpty() ? "Chưa có SĐT" : phone) + " - " + position;
    }
}
