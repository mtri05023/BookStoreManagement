package model;

public class Customer extends Person {
    private String phone;

    public Customer(String id, String name, String phone) {
        super(id, name);
        setPhone(phone);
    }

    public String getPhone() { return phone; }

    public void setPhone(String phone) {
        String value = ModelChecks.text(phone, "Điện thoại");
        if (!value.matches("0[0-9]{9}")) throw new IllegalArgumentException("Điện thoại khách hàng phải có đúng 10 chữ số và bắt đầu bằng 0.");
        this.phone = value;
    }

    @Override
    public String getRole() { return "Customer"; }

    @Override
    public String toString() {
        return String.format("%-8s %-25s %-15s", getId(), getName(), phone);
    }
}
