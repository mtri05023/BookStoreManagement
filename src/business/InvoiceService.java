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
        if (id == null || id.isBlank()) throw new IllegalArgumentException("Ma hoa don khong duoc de trong.");
        if (invoices.findById(id) != null) throw new IllegalArgumentException("Ma hoa don da ton tai.");
        Customer customer = customers.findById(customerId);
        if (customer == null || employee == null) throw new IllegalArgumentException("Khong co khach hang hoac nhan vien.");
        employee = employees.findById(employee.getId());
        if (employee == null) throw new IllegalArgumentException("Nhan vien khong co trong danh sach.");
        if (quantities == null || quantities.isEmpty()) throw new IllegalArgumentException("Gio hang rong.");

        Map<String, Integer> cart = new LinkedHashMap<>();
        for (Map.Entry<String, Integer> entry : quantities.entrySet()) {
            String bookId = entry.getKey();
            Integer quantity = entry.getValue();
            if (bookId == null || bookId.isBlank() || quantity == null || quantity <= 0) {
                throw new IllegalArgumentException("Gio hang khong hop le.");
            }
            String normalizedId = bookId.trim().toUpperCase(Locale.ROOT);
            try {
                cart.merge(normalizedId, quantity, Math::addExact);
            } catch (ArithmeticException e) {
                throw new IllegalArgumentException("So luong vuot gioi han.", e);
            }
        }

        Invoice invoice = new Invoice(id, customer, employee);
        Map<Book, Integer> remainingStock = new LinkedHashMap<>();
        for (Map.Entry<String, Integer> entry : cart.entrySet()) {
            Book book = books.findById(entry.getKey());
            if (book == null) throw new IllegalArgumentException("Khong tim thay sach: " + entry.getKey());
            if (entry.getValue() > book.getQuantity()) {
                throw new IllegalStateException("Khong du ton kho: " + book.getId());
            }
            invoice.addDetail(new InvoiceDetail(book, entry.getValue()));
            remainingStock.put(book, book.getQuantity() - entry.getValue());
        }
        invoice.complete();
        invoices.add(invoice);
        remainingStock.forEach(Book::setQuantity);
        return invoice;
    }

    public List<Invoice> getInvoices() { return invoices.getAll(); }
    public Invoice findInvoice(String id) { return invoices.findById(id); }
}
