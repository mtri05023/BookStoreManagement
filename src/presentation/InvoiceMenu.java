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
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public class InvoiceMenu {
    // Do rong cot ten sach khi in.
    private static final int TITLE_WIDTH = 25;

    private final BookService bookService;
    private final CustomerService customerService;
    private final InvoiceService invoiceService;
    private final Supplier<Employee> employee; // nhan vien dang lap hoa don (lay moi lan can)
    private final InputHelper input;

    // Constructor tien loi: truyen thang mot nhan vien co dinh
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
        // Chan ma rong ngay tu dau.
        if (invoiceId == null || invoiceId.isBlank()) {
            System.out.println("Ma hoa don khong duoc rong.");
            return;
        }
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
            String bookId = input.read("Ma sach (0 de thanh toan, -1 de huy): ");
            if (bookId.equals("-1")) return;
            if (bookId.equals("0")) {
                // Khong cho thanh toan khi gio rong
                if (cart.isEmpty()) {
                    System.out.println("Gio hang dang rong, hay them it nhat mot cuon sach (hoac nhap -1 de huy).");
                    continue;
                }
                break;
            }
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

            // Kiem tra ton kho NGAY khi them vao gio.
            long inCart = cart.getOrDefault(book.getId(), 0);
            if (inCart + quantity > book.getQuantity()) {
                long canAdd = Math.max(0, book.getQuantity() - inCart);
                System.out.println("Khong du ton kho cho \"" + book.getTitle() + "\": ton "
                        + book.getQuantity() + ", trong gio da co " + inCart
                        + ", con them duoc toi da " + canAdd + ".");
                continue;
            }
            try {
                cart.merge(book.getId(), quantity, Math::addExact);
            } catch (ArithmeticException e) {
                System.out.println("So luong vuot gioi han.");
            }
        }

        // Boc loi goi service bang try/catch.
        // Bat loi tai day de bao ro la LAP HOA DON that bai, va khong de exception di len MainMenu / Main.
        try {
            Invoice invoice = invoiceService.createInvoice(invoiceId, customer.getId(), employee.get(), cart);
            printInvoice(invoice);
        } catch (IllegalArgumentException | IllegalStateException e) {
            System.out.println("Lap hoa don that bai: " + e.getMessage());
        }
    }

    public void showInvoices() {
        // Gio goi mot lan, luu vao bien cuc bo roi dung lai.
        List<Invoice> all = invoiceService.getInvoices();
        if (all.isEmpty()) {
            System.out.println("Chua co hoa don.");
            return;
        }

        for (Invoice invoice : all)
            printInvoice(invoice);
    }

    public void printInvoice(Invoice invoice) {
        System.out.println("\n============== HOA DON ==============");
        System.out.println("Ma HD: " + invoice.getId());
        System.out.println("Ngay lap: " + invoice.getCreatedDate());
        System.out.println("Khach hang: " + invoice.getCustomerName());
        System.out.println("Nhan vien: " + invoice.getEmployeeName());
        System.out.println("--------------------------------------");

        for (InvoiceDetail d : invoice.getDetails()) {
            // Cat ten sach qua dai de cot so luong va thanh tien luon thang hang
            System.out.printf("%-" + TITLE_WIDTH + "s x%-3d %10.0f%n",
                    shorten(d.getBookTitle(), TITLE_WIDTH), d.getQuantity(), d.getAmount());
        }

        System.out.println("--------------------------------------");
        System.out.printf("TONG TIEN: %,.0f VND%n", invoice.getTotal());
        System.out.println("======================================");
    }

    /* Cat chuoi dai hon maxLength va them "..." o cuoi */
    private static String shorten(String text, int maxLength) {
        if (text == null) return "";
        if (text.length() <= maxLength) return text;
        return text.substring(0, maxLength - 3) + "...";
    }
}