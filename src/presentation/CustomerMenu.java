package presentation;

import business.CustomerService;
import model.Customer;
import utility.InputHelper;

import java.util.List;
import java.util.function.UnaryOperator;

public class CustomerMenu {
    private final CustomerService customerService;
    private final InputHelper input;

    public CustomerMenu(CustomerService customerService, InputHelper input) {
        this.customerService = customerService;
        this.input = input;
    }

    public void show() {
        int choice;
        do {
            System.out.println("\n======= QUAN LY KHACH HANG =======");
            System.out.println("1. Them khach hang");
            System.out.println("2. Danh sach khach hang");
            System.out.println("3. Tim khach hang theo ma");
            System.out.println("4. Tim khach hang theo ten");
            System.out.println("5. Sua khach hang");
            System.out.println("6. Xoa khach hang");
            System.out.println("0. Quay lai");

            choice = input.readInt("Moi nhap so: ");

            try {
                switch (choice) {
                    case 1 -> addCustomer();
                    case 2 -> showCustomers();
                    case 3 -> findCustomer();
                    case 4 -> searchCustomers();
                    case 5 -> updateCustomer();
                    case 6 -> deleteCustomer();
                    case 0 -> { }
                    default -> System.out.println("Lua chon khong hop le.");
                }
            } catch (IllegalArgumentException | IllegalStateException e) {
                System.out.println(e.getMessage());
            }
        } while (choice != 0);
    }

    public void addCustomer() {
        String id;
        while (true) {
            id = readUntilValid("Ma KH (vd KH001): ", Customer::checkId);
            if (customerService.findCustomer(id) == null) break;
            System.out.println("Ma " + id + " da ton tai. Vui long nhap ma khac.");
        }
        String name = readUntilValid("Ten KH: ", Customer::checkName);
        String phone = readUntilValid("SDT (co 10 so, bat dau tu so 0): ", Customer::checkPhone);

        System.out.println(customerService.addCustomer(new Customer(id, name, phone))
                ? "Them thanh công." : "Khong them duoc khach hang.");
    }

    public void showCustomers() {
        printTable(customerService.getCustomers());
    }

    public void findCustomer() {
        Customer c = customerService.findCustomer(input.read("Ma KH: "));
        if (c == null) {
            System.out.println("Khong tim thay.");
            return;
        }
        printTable(List.of(c));
    }

    public void searchCustomers() {
        printTable(customerService.searchByName(input.read("Tu khoa ten: ")));
    }

    public void updateCustomer() {
        Customer c = customerService.findCustomer(input.read("Ma KH can sua: "));
        if (c == null) {
            System.out.println("Khong tim thay.");
            return;
        }
        printTable(List.of(c));
        System.out.println("(Bo trong de giu gia tri cu)");
        String name = readOptional("Ten moi: ", c.getName(), Customer::checkName);
        String phone = readOptional("SDT moi: ", c.getPhone(), Customer::checkPhone);

        System.out.println(customerService.updateCustomer(c.getId(), name, phone)
                ? "Cap nhat thanh cong." : "Khong cap nhat duoc khach hang.");
    }

    public void deleteCustomer() {
        Customer c = customerService.findCustomer(input.read("Ma KH can xoa: "));
        if (c == null) {
            System.out.println("Khong tim thay.");
            return;
        }
        printTable(List.of(c));
        String answer = input.read("Xac nhan xoa? (C/K): ").trim();
        if (!answer.equalsIgnoreCase("C")) {
            System.out.println("Da huy xoa.");
            return;
        }
        // deleteCustomer ném IllegalStateException nếu khách đã có hóa đơn; show() bắt và in thông báo.
        System.out.println(customerService.deleteCustomer(c.getId())
                ? "Xoa thanh cong." : "Khong tim thay.");
    }

    private void printTable(List<Customer> customers) {
        if (customers.isEmpty()) {
            System.out.println("Khong co khach hang nao.");
            return;
        }
        System.out.printf("%-8s %-25s %-15s%n", "Ma", "Ten", "SDT");
        for (Customer c : customers) {
            System.out.println(c);
        }
    }

    // Nhập lại đến khi hợp lệ. Hết dữ liệu nhập (EOF) thì input.read ném lỗi ra ngoài, không lặp vô hạn.
    private String readUntilValid(String prompt, UnaryOperator<String> check) {
        while (true) {
            String raw = input.read(prompt);
            try {
                return check.apply(raw);
            } catch (IllegalArgumentException e) {
                System.out.println("Lỗi: " + e.getMessage() + " Vui long nhap lai.");
            }
        }
    }

    // Như readUntilValid nhưng cho phép bỏ trống để giữ giá trị cũ.
    private String readOptional(String prompt, String oldValue, UnaryOperator<String> check) {
        while (true) {
            String raw = input.read(prompt);
            if (raw == null || raw.isBlank()) return oldValue;
            try {
                return check.apply(raw);
            } catch (IllegalArgumentException e) {
                System.out.println("Loi: " + e.getMessage() + " Vui long nhap lai.");
            }
        }
    }
}
