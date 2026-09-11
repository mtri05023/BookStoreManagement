package presentation;

import business.BookService;
import business.CustomerService;
import business.InvoiceService;
import data.BookRepository;
import data.CustomerRepository;
import data.InvoiceRepository;
import model.*;

import java.util.Scanner;

public class Main {
    static Scanner sc = new Scanner(System.in);

    static BookService bookService =
            new BookService(new BookRepository());
    static CustomerService customerService =
            new CustomerService(new CustomerRepository());
    static InvoiceService invoiceService =
            new InvoiceService(new InvoiceRepository());

    static Employee employee =
            new Employee("NV001", "Admin", "Quản lý");

    public static void main(String[] args) {
        seedData();

        int choice;
        do {
            System.out.println("\n======================================");
            System.out.println("       QUẢN LÝ CỬA HÀNG SÁCH");
            System.out.println("======================================");
            System.out.println("1. Quản lý sách");
            System.out.println("2. Quản lý khách hàng");
            System.out.println("3. Lập hóa đơn");
            System.out.println("4. Xem hóa đơn");
            System.out.println("5. Thống kê doanh thu");
            System.out.println("0. Thoát");

            choice = readInt("Chọn: ");

            switch (choice) {
                case 1 -> bookMenu();
                case 2 -> customerMenu();
                case 3 -> createInvoice();
                case 4 -> showInvoices();
                case 5 -> System.out.printf("Doanh thu: %,.0f VNĐ%n",
                        invoiceService.getRevenue());
                case 0 -> System.out.println("Đã thoát chương trình.");
                default -> System.out.println("Lựa chọn không hợp lệ.");
            }
        } while (choice != 0);
    }

    static void bookMenu() {
        int choice;
        do {
            System.out.println("\n========== QUẢN LÝ SÁCH ==========");
            System.out.println("1. Thêm sách");
            System.out.println("2. Danh sách sách");
            System.out.println("3. Tìm sách");
            System.out.println("4. Cập nhật sách");
            System.out.println("5. Xóa sách");
            System.out.println("0. Quay lại");

            choice = readInt("Chọn: ");

            switch (choice) {
                case 1 -> addBook();
                case 2 -> showBooks();
                case 3 -> findBook();
                case 4 -> updateBook();
                case 5 -> deleteBook();
            }
        } while (choice != 0);
    }

    static void addBook() {
        String id = read("Mã sách: ");
        String title = read("Tên sách: ");
        String author = read("Tác giả: ");
        double price = readDouble("Giá: ");
        int quantity = readInt("Số lượng: ");

        if (bookService.addBook(new Book(id, title, author, price, quantity)))
            System.out.println("Thêm sách thành công.");
        else
            System.out.println("Không thể thêm sách. Kiểm tra mã hoặc dữ liệu.");
    }

    static void showBooks() {
        System.out.printf("%-8s %-25s %-20s %10s %6s%n",
                "Mã", "Tên sách", "Tác giả", "Giá", "SL");
        for (Book book : bookService.getBooks())
            System.out.println(book);
    }

    static void findBook() {
        Book book = bookService.findBook(read("Nhập mã sách: "));
        System.out.println(book == null ? "Không tìm thấy." : book);
    }

    static void updateBook() {
        String id = read("Mã sách cần sửa: ");
        if (bookService.findBook(id) == null) {
            System.out.println("Không tìm thấy sách.");
            return;
        }
        String title = read("Tên mới: ");
        String author = read("Tác giả mới: ");
        double price = readDouble("Giá mới: ");
        int quantity = readInt("Số lượng mới: ");

        System.out.println(bookService.updateBook(id, title, author, price, quantity)
                ? "Cập nhật thành công." : "Cập nhật thất bại.");
    }

    static void deleteBook() {
        String id = read("Mã sách cần xóa: ");
        System.out.println(bookService.deleteBook(id)
                ? "Xóa thành công." : "Không tìm thấy sách.");
    }

    static void customerMenu() {
        int choice;
        do {
            System.out.println("\n======= QUẢN LÝ KHÁCH HÀNG =======");
            System.out.println("1. Thêm khách hàng");
            System.out.println("2. Danh sách khách hàng");
            System.out.println("3. Tìm khách hàng");
            System.out.println("0. Quay lại");

            choice = readInt("Chọn: ");

            switch (choice) {
                case 1 -> addCustomer();
                case 2 -> showCustomers();
                case 3 -> findCustomer();
            }
        } while (choice != 0);
    }

    static void addCustomer() {
        String id = read("Mã KH: ");
        String name = read("Tên KH: ");
        String phone = read("SĐT: ");

        System.out.println(customerService.addCustomer(
                new Customer(id, name, phone))
                ? "Thêm thành công." : "Mã khách hàng đã tồn tại.");
    }

    static void showCustomers() {
        System.out.printf("%-8s %-25s %-15s%n", "Mã", "Tên", "SĐT");
        for (Customer c : customerService.getCustomers())
            System.out.println(c);
    }

    static void findCustomer() {
        Customer c = customerService.findCustomer(read("Mã KH: "));
        System.out.println(c == null ? "Không tìm thấy." : c);
    }

    static void createInvoice() {
        String invoiceId = read("Mã hóa đơn: ");
        if (invoiceService.findInvoice(invoiceId) != null) {
            System.out.println("Mã hóa đơn đã tồn tại.");
            return;
        }

        Customer customer = customerService.findCustomer(read("Mã khách hàng: "));
        if (customer == null) {
            System.out.println("Không tìm thấy khách hàng.");
            return;
        }

        Invoice invoice = new Invoice(invoiceId, customer, employee);

        while (true) {
            String bookId = read("Mã sách (0 để thanh toán): ");
            if (bookId.equals("0")) break;

            Book book = bookService.findBook(bookId);
            if (book == null) {
                System.out.println("Không tìm thấy sách.");
                continue;
            }

            int quantity = readInt("Số lượng: ");
            if (!bookService.sellBook(bookId, quantity)) {
                System.out.println("Số lượng không hợp lệ hoặc không đủ tồn kho.");
                continue;
            }

            invoice.addDetail(new InvoiceDetail(book, quantity));
            System.out.println("Đã thêm vào hóa đơn.");
        }

        if (invoice.getDetails().isEmpty()) {
            System.out.println("Hóa đơn rỗng, không lưu.");
            return;
        }

        invoiceService.addInvoice(invoice);
        printInvoice(invoice);
    }

    static void showInvoices() {
        if (invoiceService.getInvoices().isEmpty()) {
            System.out.println("Chưa có hóa đơn.");
            return;
        }

        for (Invoice invoice : invoiceService.getInvoices())
            printInvoice(invoice);
    }

    static void printInvoice(Invoice invoice) {
        System.out.println("\n============== HÓA ĐƠN ==============");
        System.out.println("Mã HĐ: " + invoice.getId());
        System.out.println("Khách hàng: " + invoice.getCustomer().getName());
        System.out.println("Nhân viên: " + invoice.getEmployee().getName());
        System.out.println("--------------------------------------");

        for (InvoiceDetail d : invoice.getDetails()) {
            System.out.printf("%-25s x%-3d %10.0f%n",
                    d.getBook().getTitle(), d.getQuantity(), d.getAmount());
        }

        System.out.println("--------------------------------------");
        System.out.printf("TỔNG TIỀN: %,.0f VNĐ%n", invoice.getTotal());
        System.out.println("======================================");
    }

    static void seedData() {
        bookService.addBook(new Book("S001", "Java Core", "Nguyen Van A", 150000, 20));
        bookService.addBook(new Book("S002", "Clean Code", "Robert C. Martin", 250000, 10));
        customerService.addCustomer(new Customer("KH001", "Nguyen Van Minh", "0901234567"));
        customerService.addCustomer(new Customer("KH002", "Tran Thi Lan", "0912345678"));
    }

    static String read(String message) {
        System.out.print(message);
        return sc.nextLine().trim();
    }

    static int readInt(String message) {
        while (true) {
            try {
                return Integer.parseInt(read(message));
            } catch (Exception e) {
                System.out.println("Vui lòng nhập số nguyên.");
            }
        }
    }

    static double readDouble(String message) {
        while (true) {
            try {
                return Double.parseDouble(read(message));
            } catch (Exception e) {
                System.out.println("Vui lòng nhập số.");
            }
        }
    }
}
