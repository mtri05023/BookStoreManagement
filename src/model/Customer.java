package model;

public class Customer extends Person {
    private String phone;

    public Customer(String id, String name, String phone) {
        super(id, name);
        this.phone = ModelChecks.text(phone, "Điện thoại");
    }

    public String getPhone() { return phone; }

    public void setPhone(String phone) { this.phone = ModelChecks.text(phone, "Điện thoại"); }

    @Override
    public String getRole() { return "Customer"; }

    @Override
    public String toString() {
        return String.format("%-8s %-25s %-15s", getId(), getName(), phone);
    }
}
