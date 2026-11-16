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
    private business.StoreSession session;
    private business.PersonService people;
    private business.InvoiceService sales;
    public CustomerMenu(business.StoreSession session, InputHelper input, business.InvoiceService sales) {
        this(new CustomerService(session.state().getCustomers()), input);
        this.session = session;
        this.people = new business.PersonService(session.state().getPeople(), session.state().getInvoices());
        this.sales = sales;
    }
    public void show() {
        if (session != null) { showManaged(); return; }
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

    private void showManaged() {
        while (true) {
            ConsoleView.options("QUẢN LÝ KHÁCH HÀNG", "Thêm khách hàng", "Hiển thị danh sách", "Tìm theo mã",
                    "Tìm theo tên hoặc từ khóa", "Tìm theo số điện thoại", "Sửa thông tin", "Xóa khách hàng",
                    "Lịch sử mua hàng", "Tổng số hóa đơn", "Tổng chi tiêu", "Lần mua gần nhất", "Thống kê theo tổng chi tiêu");
            int choice = input.readInt("Chọn: ");
            try {
                switch (choice) {
                    case 0 -> { return; }
                    case 1 -> {
                        Customer c = readCustomer(input.readNewCode("KH", "Mã khách hàng: "));
                        session.change(() -> people.add(c));
                        System.out.println("Thêm khách hàng thành công! Đã lưu dữ liệu.");
                    }
                    case 2 -> showCustomers();
                    case 3 -> findCustomer();
                    case 4, 5 -> {
                        String key = input.read(choice == 5 ? "Số điện thoại: " : "Tên hoặc từ khóa: ");
                        model.Person[] matches = people.search(key, "Customer", false);
                        int count = 0;
                        for (model.Person p : matches) {
                            Customer c = (Customer) p;
                            if (choice != 5 || c.getPhone().contains(key)) { System.out.println(c); count++; }
                        }
                        System.out.println("Số khách hàng tìm thấy: " + count);
                    }
                    case 6 -> {
                        Customer c = requireCustomer();
                        Customer replacement = readCustomer(c.getId());
                        session.change(() -> { if (!people.updateById(c.getId(), replacement)) throw new IllegalStateException("Không tìm thấy khách hàng."); });
                        System.out.println("Cập nhật khách hàng thành công! Đã lưu dữ liệu.");
                    }
                    case 7 -> {
                        Customer c = requireCustomer();
                        session.change(() -> { if (!people.delete(c.getId())) throw new IllegalStateException("Không tìm thấy khách hàng."); });
                        System.out.println("Xóa khách hàng thành công! Đã lưu dữ liệu.");
                    }
                    case 8, 9, 10, 11 -> history(choice);
                    case 12 -> {
                        Customer[] customers = customerService.getCustomers().toArray(new Customer[0]);
                        java.util.Arrays.sort(customers, java.util.Comparator.comparingDouble((Customer c) -> total(c.getId())).reversed());
                        System.out.println("Mã | Họ tên | Số hóa đơn | Tổng chi tiêu (VNĐ)");
                        for (Customer c : customers) System.out.printf("%s | %s | %d | %,.0f%n", c.getId(), c.getName(), sales.history(c.getId()).length, total(c.getId()));
                    }
                    default -> System.out.println("Lựa chọn không hợp lệ.");
                }
            } catch (IllegalArgumentException | IllegalStateException e) { System.out.println(e.getMessage()); }
        }
    }
    private Customer readCustomer(String id) { return new Customer(id, input.read("Họ tên: "), input.read("Điện thoại: ")); }
    private Customer requireCustomer() {
        String id = input.readExistingCode("KH", "Mã khách hàng: ");
        Customer c = customerService.findCustomer(id);
        if (c == null) throw new IllegalArgumentException("Không tìm thấy khách hàng có mã " + id);
        return c;
    }
    private double total(String id) { return java.util.Arrays.stream(sales.history(id)).mapToDouble(model.Invoice::getTotal).sum(); }
    private void history(int mode) {
        Customer c = requireCustomer();
        model.Invoice[] invoices = sales.history(c.getId());
        System.out.println(c.getId() + " - " + c.getName() + " | SĐT: " + c.getPhone());
        if (invoices.length == 0) { System.out.println("Khách hàng chưa có giao dịch nào."); return; }
        if (mode == 8) for (model.Invoice i : invoices) ConsoleView.invoice(i);
        if (mode == 8 || mode == 9) System.out.println("Tổng số hóa đơn: " + invoices.length);
        if (mode == 8 || mode == 10) System.out.printf("Tổng chi tiêu: %,.0f VNĐ%n", total(c.getId()));
        if (mode == 8 || mode == 11) System.out.println("Lần mua gần nhất: " + invoices[invoices.length - 1].getPaymentDate().format(ConsoleView.DATE));
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
        System.out.println("Số khách hàng: " + customerService.getCustomers().size());
    }

    public void findCustomer() {
        String id = session == null ? input.read("Mã KH: ") : input.readExistingCode("KH", "Mã khách hàng: ");
        Customer c = customerService.findCustomer(id);
        System.out.println(c == null ? "Không tìm thấy khách hàng có mã " + id : c);
    }

}
