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

/** Single-threaded in-memory checkout. Persistence must be integrated separately. */
public class InvoiceService {
    private final IRepository<Invoice> invoices;
    private final IRepository<Book> books;
    private final IRepository<Customer> customers;

    public InvoiceService(IRepository<Invoice> invoices, IRepository<Book> books,
                          IRepository<Customer> customers) {
        this.invoices = Objects.requireNonNull(invoices);
        this.books = Objects.requireNonNull(books);
        this.customers = Objects.requireNonNull(customers);
    }

    public Invoice createInvoice(String id, String customerId, Employee employee,
                                 Map<String, Integer> quantities) {
        if (id == null || id.isBlank()) throw new IllegalArgumentException("Mã hóa đơn không được trống.");
        if (invoices.findById(id) != null) throw new IllegalArgumentException("Mã hóa đơn đã tồn tại.");
        Customer customer = customers.findById(customerId);
        if (customer == null || employee == null) throw new IllegalArgumentException("Thiếu khách hàng hoặc nhân viên.");
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
        Map<Book, Integer> remainingStock = new LinkedHashMap<>();
        for (Map.Entry<String, Integer> entry : cart.entrySet()) {
            Book book = books.findById(entry.getKey());
            if (book == null) throw new IllegalArgumentException("Không tìm thấy sách: " + entry.getKey());
            if (entry.getValue() > book.getQuantity()) {
                throw new IllegalStateException("Không đủ tồn kho: " + book.getId());
            }
            invoice.addDetail(new InvoiceDetail(book, entry.getValue()));
            remainingStock.put(book, book.getQuantity() - entry.getValue());
        }
        invoice.complete();
        // Current repositories are in memory. add must fail before changing repository state.
        // After successful add these non-negative stock setters cannot fail on normal Book objects.
        invoices.add(invoice);
        remainingStock.forEach(Book::setQuantity);
        return invoice;
    }

    public List<Invoice> getInvoices() { return invoices.getAll(); }
    public Invoice findInvoice(String id) { return invoices.findById(id); }
}
