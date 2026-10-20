package presentation;

import business.BookService;
import business.CustomerService;
import business.InvoiceService;
import business.ReportService;
import data.BookRepository;
import data.CustomerRepository;
import data.InvoiceRepository;
import model.Book;
import model.Customer;
import model.Employee;
import utility.InputHelper;
import utility.StoreConfig;
import java.util.NoSuchElementException;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        BookRepository books = new BookRepository();
        CustomerRepository customers = new CustomerRepository();
        InvoiceRepository invoices = new InvoiceRepository();
        BookService bookService = new BookService(books);
        CustomerService customerService = new CustomerService(customers);
        InvoiceService invoiceService = new InvoiceService(invoices, books, customers);
        ReportService reports = new ReportService(invoices);
        Employee employee = new Employee(StoreConfig.DEFAULT_EMPLOYEE_ID, "Admin", "Quản lý");
        seedData(bookService, customerService);
        try (Scanner scanner = new Scanner(System.in)) {
            InputHelper input = new InputHelper(scanner);
            BookMenu bookMenu = new BookMenu(bookService, input);
            CustomerMenu customerMenu = new CustomerMenu(customerService, input);
            InvoiceMenu invoiceMenu = new InvoiceMenu(bookService, customerService, invoiceService, employee, input);
            try {
                new MainMenu(bookMenu, customerMenu, invoiceMenu, reports, input).show();
            } catch (NoSuchElementException e) {
                System.out.println("Đã kết thúc nhập liệu.");
            }
        }
    }
    private static void seedData(BookService bookService, CustomerService customerService) {
        bookService.addBook(new Book("S001", "Java Core", "Nguyen Van A", 150000, 20));
        bookService.addBook(new Book("S002", "Clean Code", "Robert C. Martin", 250000, 10));
        customerService.addCustomer(new Customer("KH001", "Nguyen Van Minh", "0901234567"));
        customerService.addCustomer(new Customer("KH002", "Tran Thi Lan", "0912345678"));
    }

}
