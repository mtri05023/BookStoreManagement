package business;

import data.IRepository;
import model.Book;
import model.Customer;
import model.Employee;
import model.Invoice;
import model.InvoiceDetail;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/** Single-threaded checkout. MainMenu saves the complete session as one text snapshot. */
public class InvoiceService {
    private final IRepository<Invoice> invoices;
    private final IRepository<Book> books;
    private final IRepository<Customer> customers;
    private final IRepository<Employee> employees;

    public InvoiceService(IRepository<Invoice> invoices, IRepository<Book> books,
                          IRepository<Customer> customers, IRepository<Employee> employees) {
        this.invoices = Objects.requireNonNull(invoices);
        this.books = Objects.requireNonNull(books);
        this.customers = Objects.requireNonNull(customers);
        this.employees = Objects.requireNonNull(employees);
    }

    public Invoice createInvoice(String id, String customerId, Employee employee,
                                 Map<String, Integer> quantities) {
        PermissionService.require(PermissionService.Permission.CREATE_INVOICE);
        Invoice invoice = prepareInvoice(id, customerId, employee, quantities);
        return commitInvoice(invoice);
    }

    /** Return the authoritative employee only when their current position may create invoices. */
    public Employee requireInvoiceEmployee(Employee candidate) {
        PermissionService.require(PermissionService.Permission.CREATE_INVOICE);
        if (candidate == null) throw new IllegalArgumentException("Chưa chọn nhân viên hợp lệ.");
        Employee current = employees.findById(candidate.getId());
        var account = PermissionService.currentAccount();
        if (account != null && account.getRole() == model.Role.EMPLOYEE
                && !account.getEmployeeId().equalsIgnoreCase(candidate.getId()))
            throw new SecurityException("Nhân viên chỉ được lập hóa đơn bằng tài khoản được liên kết với chính mình.");
        if (current == null) throw new IllegalArgumentException("Nhân viên không có trong danh sách.");
        if (account == null
                && !current.getPosition().equalsIgnoreCase("Thu ngân")
                && !current.getPosition().equalsIgnoreCase("Quản lý")) {
            throw new IllegalArgumentException("Chỉ nhân viên Thu ngân hoặc Quản lý được lập hóa đơn.");
        }
        return current;
    }

    /** Revalidate a displayed quote and reject stale prices instead of charging a different amount. */
    public Invoice payInvoice(Invoice quote) {
        PermissionService.require(PermissionService.Permission.CREATE_INVOICE);
        if (quote == null || quote.getStatus() != Invoice.Status.DRAFT)
            throw new IllegalArgumentException("Chỉ được thanh toán hóa đơn đang lập.");
        Map<String, Integer> quantities = new LinkedHashMap<>();
        for (InvoiceDetail d : quote.getDetails()) {
            if (quantities.putIfAbsent(d.getBookId(), d.getQuantity()) != null)
                throw new IllegalArgumentException("Trùng dòng sách trong hóa đơn.");
        }
        Invoice current = prepareInvoice(quote.getId(), quote.getCustomer().getId(), quote.getEmployee(), quantities);
        for (int i = 0; i < current.getDetails().size(); i++) {
            if (Double.compare(current.getDetails().get(i).getPrice(), quote.getDetails().get(i).getPrice()) != 0)
                throw new IllegalStateException("Giá sách đã thay đổi. Vui lòng xem lại hóa đơn trước khi xác nhận.");
        }
        if (Double.compare(current.getTotal(), quote.getTotal()) != 0)
            throw new IllegalStateException("Tổng tiền đã thay đổi. Vui lòng xác nhận lại.");
        Invoice invoice = new Invoice(current.getId(), current.getCustomer(), current.getEmployee(), quote.getCreatedAt());
        for (InvoiceDetail detail : current.getDetails()) invoice.addDetail(detail);
        return commitInvoice(invoice);
    }

    private Invoice commitInvoice(Invoice invoice) {
        Map<Book, Integer> remainingStock = new LinkedHashMap<>();
        for (InvoiceDetail detail : invoice.getDetails()) {
            Book book = books.findById(detail.getBookId());
            remainingStock.put(book, book.getQuantity() - detail.getQuantity());
        }
        invoice.complete();
        // Repository rejection occurs before stock is touched. The application wraps this
        // command in StoreSession.change to commit invoice + stock + text file together.
        invoices.add(invoice);
        remainingStock.forEach(Book::setQuantity);
        return invoice;
    }

    /** Validate all current references and totals without recording sales or changing stock. */
    public Invoice prepareInvoice(String id, String customerId, Employee employee,
                                  Map<String, Integer> quantities) {
        PermissionService.require(PermissionService.Permission.CREATE_INVOICE);
        if (id == null || id.isBlank()) throw new IllegalArgumentException("Mã hóa đơn không được trống.");
        if (invoices.findById(id) != null) throw new IllegalArgumentException("Mã hóa đơn đã tồn tại.");
        Customer customer = customers.findById(customerId);
        if (customer == null) throw new IllegalArgumentException("Khách hàng không còn tồn tại: " + customerId);
        employee = requireInvoiceEmployee(employee);
        if (quantities == null || quantities.isEmpty()) throw new IllegalArgumentException("Giỏ hàng rỗng.");

        Map<String, Integer> cart = new LinkedHashMap<>();
        for (Map.Entry<String, Integer> entry : quantities.entrySet()) {
            String bookId = entry.getKey();
            Integer quantity = entry.getValue();
            if (bookId == null || bookId.isBlank() || quantity == null || quantity <= 0) {
                throw new IllegalArgumentException("Dòng giỏ hàng không hợp lệ.");
            }
            String normalizedId = bookId.trim().toUpperCase(Locale.ROOT);
            try {
                cart.merge(normalizedId, quantity, Math::addExact);
            } catch (ArithmeticException e) {
                throw new IllegalArgumentException("Số lượng vượt giới hạn.", e);
            }
        }

        Invoice invoice = new Invoice(id, customer, employee);
        for (Map.Entry<String, Integer> entry : cart.entrySet()) {
            Book book = books.findById(entry.getKey());
            if (book == null) throw new IllegalArgumentException("Không tìm thấy sách: " + entry.getKey());
            if (entry.getValue() > book.getQuantity()) {
                throw new IllegalStateException("Không đủ tồn kho: " + book.getId());
            }
            invoice.addDetail(new InvoiceDetail(book, entry.getValue()));
        }
        if (!Double.isFinite(invoice.getTotal()) || invoice.getTotal() <= 0)
            throw new IllegalArgumentException("Tổng tiền thanh toán không hợp lệ.");
        return invoice;
    }

    public Invoice[] filter(java.time.LocalDate from, java.time.LocalDate to, String customerId) {
        PermissionService.require(PermissionService.Permission.VIEW_OWN_INVOICES);
        if (from == null || to == null || from.isAfter(to)) throw new IllegalArgumentException("Khoảng ngày không hợp lệ.");
        var stream = invoices.getAll().stream().filter(Invoice::isCompleted);
        var account = PermissionService.currentAccount();
        if (account != null && account.getRole() == model.Role.EMPLOYEE)
            stream = stream.filter(i -> i.getEmployee().getId().equalsIgnoreCase(account.getEmployeeId()));
        return stream
                .filter(i -> !i.getCreatedDate().isBefore(from) && !i.getCreatedDate().isAfter(to))
                .filter(i -> customerId == null || i.getCustomer().getId().equalsIgnoreCase(customerId.trim()))
                .sorted(java.util.Comparator.comparing(Invoice::getCreatedDate).thenComparing(Invoice::getId))
                .toArray(Invoice[]::new);
    }
    public Invoice[] history(String customerId) {
        Invoice[] result = filter(java.time.LocalDate.MIN, java.time.LocalDate.MAX, customerId);
        java.util.Arrays.sort(result, java.util.Comparator.comparing(Invoice::getPaymentDate).thenComparing(Invoice::getCreatedAt));
        return result;
    }
    public List<Invoice> getInvoices() {
        PermissionService.require(PermissionService.Permission.VIEW_OWN_INVOICES);
        var account = PermissionService.currentAccount();
        if (account == null || account.getRole() == model.Role.ADMIN) return invoices.getAll();
        return invoices.getAll().stream().filter(i -> i.getEmployee().getId().equalsIgnoreCase(account.getEmployeeId())).toList();
    }
    public Invoice findInvoice(String id) {
        PermissionService.require(PermissionService.Permission.VIEW_OWN_INVOICES);
        Invoice invoice = invoices.findById(id);
        var account = PermissionService.currentAccount();
        if (invoice != null && account != null && account.getRole() == model.Role.EMPLOYEE
                && !invoice.getEmployee().getId().equalsIgnoreCase(account.getEmployeeId()))
            throw new SecurityException("Nhân viên chỉ được xem hóa đơn do mình lập.");
        return invoice;
    }
}
