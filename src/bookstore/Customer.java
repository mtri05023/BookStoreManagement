package bookstore;

public class Customer implements Identifiable {
    private final String id;
    private String name;
    public String getId() { return id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = InputHelper.text(name, "Tên"); }
    private String phone;

    public Customer(String id, String name, String phone) {
        this.id = InputHelper.id(id);
        this.name = InputHelper.text(name, "Tên");
        setPhone(phone);
    }

    public String getPhone() { return phone; }

    public void setPhone(String phone) {
        String value = InputHelper.text(phone, "Điện thoại");
        if (!value.matches("0[0-9]{9}")) throw new IllegalArgumentException("Điện thoại khách hàng phải có đúng 10 chữ số và bắt đầu bằng 0.");
        this.phone = value;
    }

    public String getRole() { return "Customer"; }

    @Override
    public String toString() {
        return String.format("%-8s %-25s %-15s", getId(), getName(), phone);
    }
}
