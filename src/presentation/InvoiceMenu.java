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
    public void createInvoice() {
        String invoiceId = input.read("Ma hoa don: ");
        if (invoiceService.findInvoice(invoiceId) != null) {
            System.out.println("Ma hoa don da ton tai.");
            return;
        }

        Customer customer = customerService.findCustomer(input.read("Ma khach hang: "));
        if (customer == null) {
            System.out.println("Khong tim thay khach hang.");
            return;
        }

        Map<String, Integer> cart = new LinkedHashMap<>();
        while (true) {
            String bookId = input.read("Ma sach(0: thanh toan, -1: huy): ");
            if (bookId.equals("-1")) return;
            if (bookId.equals("0")) break;
            Book book = bookService.findBook(bookId);
            if (book == null) {
                System.out.println("Khong tim thay sach.");
                continue;
            }
            int quantity = input.readInt("So luong: ");
            if (quantity <= 0) {
                System.out.println("So luong phai lon hon 0.");
                continue;
            }
            try {
                cart.merge(book.getId(), quantity, Math::addExact);
            } catch (ArithmeticException e) {
                System.out.println("So luong vuot gioi han.");
            }
        }
        Invoice invoice = invoiceService.createInvoice(invoiceId, customer.getId(), employee.get(), cart);
        printInvoice(invoice);
    }

    public void showInvoices() {
        if (invoiceService.getInvoices().isEmpty()) {
            System.out.println("Chua co hoa don.");
            return;
        }

        for (Invoice invoice : invoiceService.getInvoices())
            printInvoice(invoice);
    }

    public void printInvoice(Invoice invoice) {
        System.out.println("\n============== HOA DON ==============");
        System.out.println("Ma HD: " + invoice.getId());
        System.out.println("Khach hang: " + invoice.getCustomerName());
        System.out.println("Nhan vien: " + invoice.getEmployeeName());
        System.out.println("--------------------------------------");

        System.out.println("Ngay lap hoa don: " + invoice.getCreatedDate());
        for (InvoiceDetail d : invoice.getDetails()) {
            System.out.printf("%-25s x%-3d %10.0f%n",
                    d.getBookTitle(), d.getQuantity(), d.getAmount());
        }

        System.out.println("--------------------------------------");
        System.out.printf("TONG TIEN: %,.0f VNĐ%n", invoice.getTotal());
        System.out.println("======================================");
    }

}
