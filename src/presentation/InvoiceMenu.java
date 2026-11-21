package presentation;

import business.BookService;
import business.CustomerService;
import business.InvoiceService;
import model.Book;
import model.Customer;
import model.Employee;
import model.Invoice;
import model.InvoiceDetail;
import utility.InputHelper;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

public class InvoiceMenu {
    private final BookService bookService;
    private final CustomerService customerService;
    private final InvoiceService invoiceService;
    private final Supplier<Employee> employee;
    private final InputHelper input;
    public InvoiceMenu(BookService bookService, CustomerService customerService, InvoiceService invoiceService, Employee employee, InputHelper input) {
        this(bookService, customerService, invoiceService, () -> employee, input);
    }

    public InvoiceMenu(BookService bookService, CustomerService customerService, InvoiceService invoiceService,
                       Supplier<Employee> employee, InputHelper input) {
        this.bookService = bookService;
        this.customerService = customerService;
        this.invoiceService = invoiceService;
        this.employee = employee;
        this.input = input;
    }
    private business.StoreSession session;
    public InvoiceMenu(business.StoreSession session, InputHelper input, InvoiceService invoices) {
        this(session, input, invoices, () -> null);
    }
    public InvoiceMenu(business.StoreSession session, InputHelper input, InvoiceService invoices,
                       Supplier<Employee> employee) {
        this(new BookService(session.state().getBooks()), new CustomerService(session.state().getCustomers()),
                invoices, employee, input);
        this.session = session;
    }
    public void createInvoice() {
        if (session != null) { checkout(); return; }
        Employee invoiceEmployee;
        try { invoiceEmployee = invoiceService.requireInvoiceEmployee(employee.get()); }
        catch (IllegalArgumentException e) { System.out.println(e.getMessage()); return; }
        String invoiceId = input.read("Mã hóa đơn: ");
        if (invoiceService.findInvoice(invoiceId) != null) {
            System.out.println("Mã hóa đơn đã tồn tại.");
            return;
        }

        Customer customer = customerService.findCustomer(input.read("Mã khách hàng: "));
        if (customer == null) {
            System.out.println("Không tìm thấy khách hàng.");
            return;
        }

        Map<String, Integer> cart = new LinkedHashMap<>();
        while (true) {
            String bookId = input.read("Mã sách (0 để thanh toán, -1 để hủy): ");
            if (bookId.equals("-1")) return;
            if (bookId.equals("0")) break;
            Book book = bookService.findBook(bookId);
            if (book == null) {
                System.out.println("Không tìm thấy sách.");
                continue;
            }
            int quantity = input.readInt("Số lượng: ");
            if (quantity <= 0) {
                System.out.println("Số lượng phải lớn hơn 0.");
                continue;
            }
            try {
                cart.merge(book.getId(), quantity, Math::addExact);
            } catch (ArithmeticException e) {
                System.out.println("Số lượng vượt giới hạn.");
            }
        }
        Invoice invoice = invoiceService.createInvoice(invoiceId, customer.getId(), invoiceEmployee, cart);
        printInvoice(invoice);
    }

    private void checkout() {
        String id = input.readNewCode("HD", "Mã hóa đơn: ");
        if (id.isBlank() || id.contains("|")) throw new IllegalArgumentException("Mã hóa đơn không hợp lệ.");
        if (invoiceService.findInvoice(id) != null) throw new IllegalArgumentException("Mã hóa đơn đã tồn tại: " + id);
        Customer customer;
        while (true) {
            String key = input.readExistingCode("KH", "Mã khách hàng: ");
            customer = customerService.findCustomer(key);
            if (customer != null) break;
            System.out.println("Không tìm thấy khách hàng có mã " + key + ". Vui lòng nhập lại.");
        }
        Employee cashier;
        model.Account active = business.PermissionService.currentAccount();
        if (active != null && active.getRole() == model.Role.EMPLOYEE) {
            cashier = session.state().getEmployees().findById(active.getEmployeeId());
            if (cashier == null) throw new SecurityException("Tài khoản chưa liên kết nhân viên hợp lệ.");
        } else {
        while (true) {
            String key = input.readExistingCode("NV", "Mã nhân viên: ");
            Employee candidate = session.state().getEmployees().findById(key);
            if (candidate == null) {
                System.out.println("Không tìm thấy nhân viên có mã " + key + ". Vui lòng nhập lại.");
                continue;
            }
            try {
                cashier = invoiceService.requireInvoiceEmployee(candidate);
                break;
            } catch (IllegalArgumentException e) {
                System.out.println(e.getMessage() + " Vui lòng nhập mã nhân viên khác.");
            }
        }
        }
        Map<String, Integer> cart = new LinkedHashMap<>();
        System.out.println("Danh sách sách có thể mua:");
        ConsoleView.books(bookService.getBooks().stream().filter(book -> book.getQuantity() > 0).toArray(Book[]::new));
        boolean finishedAddingBooks = false;
        while (!finishedAddingBooks) {
            String bookId = input.readExistingCode("S", "Mã sách cần lập hóa đơn: ");
            Book book = bookService.findBook(bookId);
            if (book == null) {
                System.out.println("Không tìm thấy sách có mã " + bookId + ". Vui lòng nhập lại.");
                continue;
            }
            if (book.getQuantity() == 0) {
                System.out.println("Sách đã hết hàng. Vui lòng chọn sách khác.");
                continue;
            }
            int quantity = input.readInt("Số lượng mua: ");
            if (quantity <= 0) {
                System.out.println("Số lượng phải lớn hơn 0.");
                continue;
            }
            long combinedQuantity = (long) cart.getOrDefault(book.getId(), 0) + quantity;
            if (combinedQuantity > book.getQuantity()) {
                System.out.println("Không đủ tồn kho cho tổng số lượng. Tồn hiện tại: " + book.getQuantity());
                continue;
            }
            cart.put(book.getId(), (int) combinedQuantity);
            printCart(cart);
            while (true) {
                String more = input.read("Có mua thêm sách không? (C/K): ");
                if (more.equalsIgnoreCase("C")) break;
                if (more.equalsIgnoreCase("K")) {
                    finishedAddingBooks = true;
                    break;
                }
                System.out.println("Vui lòng nhập C để thêm sách hoặc K để hoàn tất hóa đơn.");
            }
        }

        Invoice quote = invoiceService.prepareInvoice(id, customer.getId(), cashier, cart);
        ConsoleView.invoice(quote);
        String answer;
        while (true) {
            answer = input.read("Nhập OK để đồng ý thanh toán hoặc HỦY để hủy hóa đơn: ").trim();
            if (answer.equalsIgnoreCase("OK") || answer.equalsIgnoreCase("HUY") || answer.equalsIgnoreCase("HỦY")) break;
            System.out.println("Lựa chọn không hợp lệ. Vui lòng nhập OK hoặc HỦY.");
        }
        if (answer.equalsIgnoreCase("HUY") || answer.equalsIgnoreCase("HỦY")) {
            quote.cancel();
            System.out.println("Đã hủy hóa đơn. Tồn kho và doanh thu không thay đổi.");
            return;
        }
        try {
            session.payInvoice(invoiceService, quote);
        } catch (IllegalArgumentException | IllegalStateException error) {
            throw new IllegalStateException("Thanh toán không thành công: " + error.getMessage(), error);
        }
        Invoice paid = invoiceService.findInvoice(id);
        System.out.println("THANH TOÁN THÀNH CÔNG!");
        ConsoleView.invoice(paid);
        System.out.println("Tồn kho còn lại:");
        for (InvoiceDetail detail : paid.getDetails()) {
            Book remaining = bookService.findBook(detail.getBookId());
            System.out.println(remaining.getId() + " - " + remaining.getTitle() + ": " + remaining.getQuantity());
        }
    }
    private void printCart(Map<String, Integer> cart) {
        System.out.printf("%-10s %-28s %8s %12s %14s%n", "Mã sách", "Tên sách", "Số lượng", "Đơn giá", "Thành tiền");
        double total = 0;
        for (var entry : cart.entrySet()) {
            Book book = bookService.findBook(entry.getKey());
            double amount = book.getPrice() * entry.getValue(); total += amount;
            System.out.printf("%-10s %-28s %8d %12.0f %14.0f%n", book.getId(), book.getTitle(), entry.getValue(), book.getPrice(), amount);
        }
        System.out.printf("Tổng tiền tạm tính / phải thanh toán: %,.0f VNĐ%n", total);
    }
    public void manage() {
        business.PermissionService.require(business.PermissionService.Permission.VIEW_ALL_INVOICES);
        while (true) {
            ConsoleView.options("QUẢN LÝ HÓA ĐƠN", "Xem tất cả", "Tìm theo mã hóa đơn", "Tìm theo mã khách hàng",
                    "Xem chi tiết hóa đơn", "Lọc theo ngày", "Lọc theo tháng", "Lọc theo năm", "Lọc theo khoảng thời gian",
                    "Sắp xếp theo ngày lập", "In hóa đơn theo mã");
            int choice = input.readInt("Chọn: ");
            try {
                if (choice == 0) return;
                model.Invoice[] result;
                switch (choice) {
                    case 1, 9 -> result = invoiceService.filter(java.time.LocalDate.MIN, java.time.LocalDate.MAX, null);
                    case 2, 4, 10 -> {
                        String id = input.readExistingCode("HD", "Mã hóa đơn: ");
                        Invoice invoice = invoiceService.findInvoice(id);
                        if (invoice == null) throw new IllegalArgumentException("Không tìm thấy hóa đơn có mã " + id);
                        result = new Invoice[] { invoice };
                    }
                    case 3 -> result = invoiceService.history(input.readExistingCode("KH", "Mã khách hàng: "));
                    case 5, 6, 7, 8 -> {
                        var range = ConsoleView.range(input, choice - 3);
                        result = invoiceService.filter(range[0], range[1], null);
                    }
                    default -> { System.out.println("Lựa chọn không hợp lệ."); continue; }
                }
                if (result.length == 0) System.out.println("Không có hóa đơn phù hợp.");
                for (Invoice invoice : result) ConsoleView.invoice(invoice);
                System.out.println(result.length == 0 ? "Tìm/lọc hóa đơn không có kết quả."
                        : "Tìm/lọc hóa đơn thành công. Số hóa đơn: " + result.length);
            } catch (IllegalArgumentException | IllegalStateException | java.time.DateTimeException e) { System.out.println(e.getMessage()); }
        }
    }
    public void showInvoices() {
        if (invoiceService.getInvoices().isEmpty()) {
            System.out.println("Chưa có hóa đơn.");
            return;
        }

        for (Invoice invoice : invoiceService.getInvoices())
            printInvoice(invoice);
        System.out.println("Đã hiển thị " + invoiceService.getInvoices().size() + " hóa đơn.");
    }
    public void showOwnInvoices() {
        for (Invoice invoice : invoiceService.getInvoices()) printInvoice(invoice);
        if (invoiceService.getInvoices().isEmpty()) System.out.println("Bạn chưa lập hóa đơn nào.");
    }

    public void printInvoice(Invoice invoice) {
        System.out.println("\n============== HÓA ĐƠN ==============");
        System.out.println("Mã HĐ: " + invoice.getId());
        System.out.println("Khách hàng: " + invoice.getCustomerName());
        System.out.println("Nhân viên: " + invoice.getEmployeeName());
        System.out.println("--------------------------------------");

        System.out.println("Ngày lập: " + invoice.getCreatedDate());
        for (InvoiceDetail d : invoice.getDetails()) {
            System.out.printf("%-25s x%-3d %10.0f%n",
                    d.getBookTitle(), d.getQuantity(), d.getAmount());
        }

        System.out.println("--------------------------------------");
        System.out.printf("TỔNG TIỀN: %,.0f VNĐ%n", invoice.getTotal());
        System.out.println("======================================");
    }

}
