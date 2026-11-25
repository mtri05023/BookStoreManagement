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
    private business.InvoiceService sales;
    public CustomerMenu(business.StoreSession session, InputHelper input, business.InvoiceService sales) {
        this(new CustomerService(session.state().getCustomers()), input);
        this.session = session;
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

    /** Restricted employee workflow: no customer deletion, bulk replacement, or global sales statistics. */
    public void showForEmployee() {
        while (true) {
            ConsoleView.options("KHÁCH HÀNG", "Danh sách", "Tìm theo mã", "Tìm theo tên", "Thêm khách hàng", "Cập nhật khách hàng");
            int choice = input.readInt("Chọn: ");
            try {
                switch (choice) {
                    case 0 -> { return; }
                    case 1 -> showCustomers();
                    case 2 -> findCustomer();
                    case 3 -> findByName();
                    case 4 -> session.addCustomer(readCustomer(input.readNewCode("KH", "Mã khách hàng: ")));
                    case 5 -> {
                        updateCustomerFields();
                    }
                    default -> System.out.println("Lựa chọn không hợp lệ.");
                }
            } catch (IllegalArgumentException | IllegalStateException e) { System.out.println(e.getMessage()); }
        }
    }

    private void showManaged() {
        while (true) {
            ConsoleView.options("QUẢN LÝ KHÁCH HÀNG", "Thêm khách hàng", "Hiển thị danh sách", "Tìm theo mã",
                    "Tìm theo tên hoặc từ khóa", "Tìm theo số điện thoại", "Sửa thông tin", "Xóa khách hàng",
                    "Lịch sử mua hàng", "Tổng số hóa đơn", "Tổng chi tiêu", "Lần mua gần nhất", "Thống kê theo tổng chi tiêu",
                    "Thêm nhiều khách hàng");
            int choice = input.readInt("Chọn: ");
            try {
                switch (choice) {
                    case 0 -> { return; }
                    case 1 -> {
                        Customer c = readCustomer(input.readNewCode("KH", "Mã khách hàng: "));
                        session.addCustomer(c);
                        System.out.println("Thêm khách hàng thành công! Đã lưu dữ liệu.");
                    }
                    case 2 -> showCustomers();
                    case 3 -> findCustomer();
                    case 4, 5 -> {
                        while (true) {
                            String key = choice == 5 ? input.readPhoneKeyword("Số điện thoại (0 để quay lại): ")
                                    : input.read("Tên hoặc từ khóa (0 để quay lại): ");
                            if (key.equals("0")) break;
                            if (key.isBlank()) { System.out.println("Vui lòng nhập từ khóa tìm kiếm."); continue; }
                            int count = 0;
                            for (Customer c : customerService.getCustomers()) {
                                String value = choice == 5 ? c.getPhone() : c.getName();
                                if (value.toLowerCase(java.util.Locale.ROOT).contains(key.toLowerCase(java.util.Locale.ROOT))) {
                                    System.out.println(c);
                                    count++;
                                }
                            }
                            if (count > 0) { System.out.println("Số khách hàng tìm thấy: " + count); break; }
                            System.out.println("Không tìm thấy khách hàng phù hợp. Vui lòng nhập lại.");
                        }
                    }
                    case 6 -> {
                        if (updateCustomerFields()) System.out.println("Cập nhật khách hàng thành công! Đã lưu dữ liệu.");
                    }
                    case 7 -> {
                        Customer c = requireCustomer();
                        session.deleteCustomer(c.getId());
                        System.out.println("Xóa khách hàng thành công! Đã lưu dữ liệu.");
                    }
                    case 8, 9, 10, 11 -> history(choice);
                    case 12 -> {
                        Customer[] customers = customerService.getCustomers().toArray(new Customer[0]);
                        java.util.Arrays.sort(customers, java.util.Comparator.comparingDouble((Customer c) -> total(c.getId())).reversed());
                        System.out.println("Mã | Họ tên | Số hóa đơn | Tổng chi tiêu (VNĐ)");
                        for (Customer c : customers) System.out.printf("%s | %s | %d | %,.0f%n", c.getId(), c.getName(), sales.history(c.getId()).length, total(c.getId()));
                        System.out.println(customers.length == 0 ? "Chưa có khách hàng để thống kê." : "Đã thống kê chi tiêu của " + customers.length + " khách hàng.");
                    }
                    case 13 -> {
                        int n = input.readInt("Số lượng khách hàng cần thêm (1..1000): ");
                        if (n < 1 || n > 1000) throw new IllegalArgumentException("Số lượng phải trong 1..1000.");
                        model.Person[] batch = new model.Person[n];
                        for (int i = 0; i < n; i++) {
                            System.out.println("Khách hàng thứ " + (i + 1) + "/" + n);
                            batch[i] = readCustomer(input.readNewCode("KH", "Mã khách hàng: "));
                        }
                        Customer[] customers = java.util.Arrays.stream(batch).map(Customer.class::cast).toArray(Customer[]::new);
                        session.addCustomers(customers);
                        System.out.println("Đã thêm " + n + " khách hàng vào danh sách. Đã lưu dữ liệu.");
                    }
                    default -> System.out.println("Lựa chọn không hợp lệ.");
                }
            } catch (IllegalArgumentException | IllegalStateException e) { System.out.println(e.getMessage()); }
        }
    }
    private Customer readCustomer(String id) { return new Customer(id, input.read("Họ tên: "), input.readPhone("Điện thoại: ")); }
    private void findByName() {
        while (true) {
            String key = input.read("Tên hoặc từ khóa (0 để quay lại): ").toLowerCase(java.util.Locale.ROOT);
            if (key.equals("0")) return;
            if (key.isBlank()) { System.out.println("Vui lòng nhập tên hoặc từ khóa."); continue; }
            int count = 0;
            for (Customer customer : customerService.getCustomers()) {
                if (customer.getName().toLowerCase(java.util.Locale.ROOT).contains(key)) {
                    System.out.println(customer);
                    count++;
                }
            }
            if (count > 0) { System.out.println("Số khách hàng tìm thấy: " + count); return; }
            System.out.println("Không tìm thấy khách hàng phù hợp. Vui lòng nhập lại.");
        }
    }
    private boolean updateCustomerFields() {
        Customer current = requireCustomer();
        System.out.println("Hiện tại: " + current);
        ConsoleView.options("SỬA KHÁCH HÀNG - CHỌN THÔNG TIN MUỐN SỬA", "Họ tên", "Số điện thoại", "Mã khách hàng");
        int field = input.readInt("Chọn thông tin cần sửa: ");
        switch (field) {
            case 0 -> { System.out.println("Đã hủy sửa thông tin."); return false; }
            case 1 -> session.updateCustomer(new Customer(current.getId(), input.read("Họ tên mới: "), current.getPhone()));
            case 2 -> {
                while (true) {
                    String phone = input.readPhone("Số điện thoại mới: ");
                    boolean duplicate = customerService.getCustomers().stream().anyMatch(c ->
                            !c.getId().equalsIgnoreCase(current.getId()) && c.getPhone().equals(phone));
                    if (duplicate) { System.out.println("Số điện thoại khách hàng đã tồn tại. Vui lòng nhập lại."); continue; }
                    session.updateCustomer(new Customer(current.getId(), current.getName(), phone));
                    break;
                }
            }
            case 3 -> {
                String id = input.readNewCode("KH", "Mã khách hàng mới: ");
                session.renameCustomer(current.getId(), new Customer(id, current.getName(), current.getPhone()));
            }
            default -> throw new IllegalArgumentException("Lựa chọn không hợp lệ.");
        }
        return true;
    }
    private Customer requireCustomer() {
        String id = input.readExistingCode("KH", "Mã khách hàng: ");
        Customer c = customerService.findCustomer(id);
        if (c != null) System.out.println("Tìm khách hàng thành công.");
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
        String phone = input.readPhone("SĐT: ");

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
        while (true) {
            String id = input.readExistingCode("KH", "Mã khách hàng (0 để quay lại): ", "0");
            if (id.equals("0")) return;
            Customer c = customerService.findCustomer(id);
            if (c != null) { System.out.println(c); return; }
            System.out.println("Sai mã khách hàng: " + id + ". Vui lòng nhập lại.");
        }
    }

}
