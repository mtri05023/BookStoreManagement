import business.BookService;
import business.InvoiceService;
import business.ReportService;
import data.BookRepository;
import data.CustomerRepository;
import data.EmployeeRepository;
import data.InvoiceRepository;
import data.IRepository;
import model.Book;
import model.Customer;
import model.Employee;
import model.Invoice;
import model.InvoiceDetail;
import model.Person;
import business.CustomerService;
import presentation.InvoiceMenu;
import utility.FileManager;
import utility.InputHelper;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Scanner;

/** Integration checks without external dependencies. Throws AssertionError on failure. */
public class RegressionChecks {
    private static int checks;

    private static void check(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message);
    }

    private static void rejects(Class<? extends Throwable> type, Runnable action) {
        checks++;
        try {
            action.run();
        } catch (Throwable error) {
            if (type.isInstance(error)) return;
            throw new AssertionError("Unexpected exception", error);
        }
        throw new AssertionError("Expected " + type.getName());
    }

    public static void main(String[] args) throws Exception {
        BookRepository books = new BookRepository();
        CustomerRepository customers = new CustomerRepository();
        EmployeeRepository employees = new EmployeeRepository();
        InvoiceRepository invoices = new InvoiceRepository();
        Book book = new Book(" s001 ", "Java", "Author", 150000, 20);
        Book second = new Book("S002", "OOP", "Author", 100000, 1);
        books.add(book);
        books.add(second);
        Customer customer = new Customer("kh001", "Customer", "0900000000");
        Employee employee = new Employee("NV001", "Employee", "Cashier");
        employees.add(employee);
        customers.add(customer);
        InvoiceService sales = new InvoiceService(invoices, books, customers, employees);
        BookService bookService = new BookService(books);
        check(books.findById(" s001 ") == book, "Normalized lookup");
        rejects(IllegalArgumentException.class,
                () -> books.add(new Book("s001", "Duplicate", "Author", 1, 1)));
        rejects(IllegalArgumentException.class, () -> books.add(null));
        books.getAll().clear();
        check(books.getAll().size() == 2, "Repository list must be protected");
        rejects(IllegalArgumentException.class, () -> new Book(" ", "B", "A", 1, 0));
        rejects(IllegalArgumentException.class, () -> book.setPrice(Double.NaN));
        rejects(IllegalArgumentException.class, () -> book.setPrice(Double.POSITIVE_INFINITY));
        check(!bookService.updateBook("S001", "Changed", " ", 1, 2), "Reject invalid update");
        check(book.getTitle().equals("Java") && book.getQuantity() == 20,
                "Failed update must not mutate fields");
        check(!bookService.addBook(null), "Null service input");
        Person person = customer;
        check(person.getRole().equals("Customer") && employee.getRole().equals("Employee"),
                "Person polymorphism");

        rejects(IllegalStateException.class, () -> sales.createInvoice("HD1", "KH001", employee,
                Map.of("S001", 2, "S002", 2)));
        check(book.getQuantity() == 20 && second.getQuantity() == 1 && invoices.getAll().isEmpty(),
                "Insufficient cart must not partially mutate state");
        Map<String, Integer> duplicateLines = new LinkedHashMap<>();
        duplicateLines.put("S001", 12);
        duplicateLines.put(" s001 ", 10);
        rejects(IllegalStateException.class,
                () -> sales.createInvoice("HD1", "KH001", employee, duplicateLines));
        rejects(IllegalArgumentException.class,
                () -> sales.createInvoice("HD1", "KH001", employee, Map.of()));
        rejects(IllegalArgumentException.class,
                () -> sales.createInvoice("HD1", "KH001", employee, Map.of("S001", 0)));
        rejects(IllegalArgumentException.class,
                () -> sales.createInvoice("HD1", "missing", employee, Map.of("S001", 1)));
        rejects(IllegalArgumentException.class,
                () -> sales.createInvoice("HD1", "KH001", employee, Map.of("missing", 1)));
        Invoice invoice = sales.createInvoice("HD1", "KH001", employee, Map.of("S001", 2));
        check(invoice.isCompleted() && invoice.getTotal() == 300000 && book.getQuantity() == 18,
                "Successful checkout");
        rejects(IllegalArgumentException.class,
                () -> sales.createInvoice(" hd1 ", "KH001", employee, Map.of("S001", 2)));
        check(book.getQuantity() == 18 && invoices.getAll().size() == 1, "No double checkout");
        rejects(UnsupportedOperationException.class, () -> invoice.getDetails().clear());
        rejects(IllegalStateException.class, () -> invoice.addDetail(new InvoiceDetail(book, 1)));
        book.setPrice(200000);
        book.setTitle("Changed");
        customer.setName("Changed");
        check(invoice.getTotal() == 300000 && invoice.getDetails().get(0).getBookTitle().equals("Java")
                && invoice.getCustomerName().equals("Customer"), "Historical snapshot");
        ReportService reports = new ReportService(invoices);
        check(reports.getRevenue() == 300000 && reports.getTotalQuantitySold() == 2
                && reports.getQuantitySoldByBook().get("S001") == 2, "Report consistency");
        IRepository<Invoice> failingRepository = new IRepository<Invoice>() {
            public void add(Invoice value) { throw new IllegalStateException("Simulated save failure"); }
            public List<Invoice> getAll() { return List.of(); }
            public Invoice findById(String id) { return null; }
        };
        rejects(IllegalStateException.class, () -> new InvoiceService(failingRepository, books, customers, employees)
                .createInvoice("FAIL", "KH001", employee, Map.of("S001", 1)));
        check(book.getQuantity() == 18, "Repository rejection must not deduct stock");
        try (Scanner scanner = new Scanner("CANCEL\nKH001\nS001\n1\n-1\n")) {
            new InvoiceMenu(bookService, new CustomerService(customers), sales, employee,
                    new InputHelper(scanner)).createInvoice();
            check(book.getQuantity() == 18 && invoices.getAll().size() == 1,
                    "Cancel after adding a cart line must not deduct stock");
        }
        try (Scanner scanner = new Scanner("EOF\nKH001\nS001\n1\n")) {
            InvoiceMenu menu = new InvoiceMenu(bookService, new CustomerService(customers), sales,
                    employee, new InputHelper(scanner));
            rejects(NoSuchElementException.class, menu::createInvoice);
            check(book.getQuantity() == 18 && invoices.getAll().size() == 1,
                    "EOF before confirmation must not deduct stock");
        }
        try (Scanner scanner = new Scanner("text\n3\nNaN\n5.5\n2026-02-30\n2026-10-20\n")) {
            InputHelper input = new InputHelper(scanner);
            check(input.readInt("") == 3 && input.readDouble("") == 5.5
                    && input.readDate("").toString().equals("2026-10-20"), "Input retry");
            rejects(NoSuchElementException.class, () -> input.readInt(""));
        }
        Path file = Path.of(args[0], "utf8-check.txt");
        List<String> lines = List.of("Sách tiếng Việt", "Khách hàng");
        FileManager.writeLines(file, lines);
        check(FileManager.readLines(file).equals(lines), "UTF-8 round trip");
        check(FileManager.readLines(file.resolveSibling("missing.txt")).isEmpty(), "Missing file");
        System.out.println("PASS: " + checks + " checks");
    }
}
