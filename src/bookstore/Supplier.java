package bookstore;

public class Supplier implements Identifiable {
    private final String id;
    private String name;
    private String phone;

    public Supplier(String id, String name, String phone) {
        if (id == null || !id.trim().toUpperCase(java.util.Locale.ROOT).matches("NCC[0-9]{3,9}"))
            throw new IllegalArgumentException("Mã nhà cung cấp không hợp lệ.");
        this.id = id.trim().toUpperCase(java.util.Locale.ROOT);
        setName(name);
        setPhone(phone);
    }
    public String getId() { return id; }
    public String getName() { return name; }
    public String getPhone() { return phone; }
    public void setName(String value) { name = InputHelper.text(value, "Tên nhà cung cấp"); }
    public void setPhone(String value) {
        if (value == null || !value.matches("0[0-9]{9}"))
            throw new IllegalArgumentException("Số điện thoại nhà cung cấp phải có 10 chữ số và bắt đầu bằng 0.");
        phone = value;
    }
    @Override public String toString() { return id + " - " + name + " - " + phone; }
}
