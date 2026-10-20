package presentation;

import business.CustomerService;
import model.Customer;
import utility.InputHelper;

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
            System.out.println("\n======= QUẢN LÝ KHÁCH HÀNG =======");
            System.out.println("1. Thêm khách hàng");
            System.out.println("2. Danh sách khách hàng");
            System.out.println("3. Tìm khách hàng");
            System.out.println("0. Quay lại");

            choice = input.readInt("Chọn: ");

            try {
                switch (choice) {
                    case 1 -> addCustomer();
                    case 2 -> showCustomers();
                    case 3 -> findCustomer();
                    case 0 -> { }
                    default -> System.out.println("Lựa chọn không hợp lệ.");
                }
            } catch (IllegalArgumentException | IllegalStateException e) {
                System.out.println(e.getMessage());
            }
        } while (choice != 0);
    }

    public void addCustomer() {
        String id = input.read("Mã KH: ");
        String name = input.read("Tên KH: ");
        String phone = input.read("SĐT: ");

        System.out.println(customerService.addCustomer(
                new Customer(id, name, phone))
                ? "Thêm thành công." : "Mã khách hàng đã tồn tại.");
    }

    public void showCustomers() {
        System.out.printf("%-8s %-25s %-15s%n", "Mã", "Tên", "SĐT");
        for (Customer c : customerService.getCustomers())
            System.out.println(c);
    }

    public void findCustomer() {
        Customer c = customerService.findCustomer(input.read("Mã KH: "));
        System.out.println(c == null ? "Không tìm thấy." : c);
    }

}
