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

public class InvoiceMenu {
    private final BookService bookService;
    private final CustomerService customerService;
    private final InvoiceService invoiceService;
    private final Employee employee;
    private final InputHelper input;
    public InvoiceMenu(BookService bookService, CustomerService customerService, InvoiceService invoiceService, Employee employee, InputHelper input) {
        this.bookService = bookService;
        this.customerService = customerService;
        this.invoiceService = invoiceService;
        this.employee = employee;
        this.input = input;
    }
    public void createInvoice() {
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
        Invoice invoice = invoiceService.createInvoice(invoiceId, customer.getId(), employee, cart);
        printInvoice(invoice);
    }

    public void showInvoices() {
        if (invoiceService.getInvoices().isEmpty()) {
            System.out.println("Chưa có hóa đơn.");
            return;
        }

        for (Invoice invoice : invoiceService.getInvoices())
            printInvoice(invoice);
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
