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

    public Invoice createInvoice(String id, String customerId, Employee employee, Map<String, Integer> quantities) {
        // kiem tra du lieu dau vao
        if (id == null || id.isBlank()) throw new IllegalArgumentException("Ma hoa don khong duoc trong.");
        if (invoices.findById(id) != null) throw new IllegalArgumentException("Ma hoa don da ton tai.");
        Customer customer = customers.findById(customerId);
        if (customer == null || employee == null) throw new IllegalArgumentException("Thieu khach hang hoac nhan vien.");
        employee = employees.findById(employee.getId());
        if (employee == null) throw new IllegalArgumentException("Nhan vien khong co trong danh sach.");
        if (quantities == null || quantities.isEmpty()) throw new IllegalArgumentException("Gio hang rong.");

        // chuan hoa gio hang (ma sach viet HOA, gop cac dong trung ma, chong tran so)
        Map<String, Integer> cart = new LinkedHashMap<>();
        for (Map.Entry<String, Integer> entry : quantities.entrySet()) {
            String bookId = entry.getKey();
            Integer quantity = entry.getValue();
            if (bookId == null || bookId.isBlank() || quantity == null || quantity <= 0) {
                throw new IllegalArgumentException("Dong gio hang khong hop le.");
            }
            String normalizedId = bookId.trim().toUpperCase(Locale.ROOT);
            try {
                cart.merge(normalizedId, quantity, Math::addExact);
            } catch (ArithmeticException e) {
                throw new IllegalArgumentException("So luong vuot gioi han.", e);
            }
        }

        Invoice invoice = new Invoice(id, customer, employee);

            // dung ma sach (String) lam key vi String luon co equals/hashCode chuan.
            //   - booksToUpdate : ma sach -> doi tuong Book can cap nhat ton kho
            //   - remainingStock: ma sach -> so luong ton SAU khi ban
        Map<String, Book> booksToUpdate = new LinkedHashMap<>();
        Map<String, Integer> remainingStock = new LinkedHashMap<>();

        // kiem tra ton kho va tao cac dong chi tiet (chua tru kho that)
        for (Map.Entry<String, Integer> entry : cart.entrySet()) {
            Book book = books.findById(entry.getKey());
            if (book == null) throw new IllegalArgumentException("Khong tim thay sach: " + entry.getKey());
            if (entry.getValue() > book.getQuantity()) {
                throw new IllegalStateException("Khong du ton kho: " + book.getId());
            }
            invoice.addDetail(new InvoiceDetail(book, entry.getValue()));
            booksToUpdate.put(book.getId(), book);
            remainingStock.put(book.getId(), book.getQuantity() - entry.getValue());
        }
        invoice.complete();

        // luu hoa don truoc. Kho hien tai nam trong bo nho, nen neu add that bai thi no nem loi truoc khi thay doi du lieu
        invoices.add(invoice);

        // Chi tru kho SAU KHI invoices.add thanh cong
        remainingStock.forEach((bookId, newQuantity) -> booksToUpdate.get(bookId).setQuantity(newQuantity));
        return invoice;
    }

    public List<Invoice> getInvoices() { return invoices.getAll(); }
    public Invoice findInvoice(String id) { return invoices.findById(id); }
}