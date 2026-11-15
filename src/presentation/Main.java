package presentation;

import business.BookService;
import business.CustomerService;
import business.InvoiceService;
import business.ReportService;
import business.PersonService;
import data.SampleData;
import data.StoreState;
import data.StoreFile;
import utility.InputHelper;
import utility.StoreConfig;
import java.util.NoSuchElementException;
import java.util.Scanner;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class Main {
    private Main() { }

    public static void main(String[] args) {
        try (Scanner scanner = new Scanner(System.in)) {
            int exitCode = run(args, scanner);
            if (exitCode != 0) System.exit(exitCode);
        }
    }

    /**
     * Safely loads an existing store snapshot or seeds initial sample data on first run.
     * Rejects missing read permissions, directories, empty files, and corrupt data without overwriting.
     */
    public static StoreState loadOrSeed(StoreFile storeFile) throws IOException {
        Path path = storeFile.getPath();
        boolean firstRun = Files.notExists(path);
        if (!firstRun) {
            if (Files.isDirectory(path)) {
                throw new IOException("Đường dẫn là thư mục, không phải file: " + path.toAbsolutePath());
            }
            if (!Files.isReadable(path)) {
                throw new IOException("Không có quyền đọc file: " + path.toAbsolutePath());
            }
            if (Files.size(path) == 0) {
                throw new IOException("File rỗng: " + path.toAbsolutePath());
            }
        }
        StoreState state = firstRun ? SampleData.create() : storeFile.load();
        if (firstRun) storeFile.save(state);
        return state;
    }

    /** Reusable entry point for integration tests; the caller owns the Scanner. */
    public static int run(String[] args, Scanner scanner) {
        try {
            Path path = StoreConfig.resolveStoreFile(args);
            StoreFile storeFile = new StoreFile(path);
            StoreState state = loadOrSeed(storeFile);
            System.out.println("Dữ liệu: " + path.toAbsolutePath());
            BookService bookService = new BookService(state.getBooks());
            CustomerService customerService = new CustomerService(state.getCustomers());
            InvoiceService invoiceService = new InvoiceService(state.getInvoices(), state.getBooks(),
                    state.getCustomers(), state.getEmployees());
            ReportService reports = new ReportService(state.getInvoices());
            InputHelper input = new InputHelper(scanner);
            BookMenu bookMenu = new BookMenu(bookService, input);
            CustomerMenu customerMenu = new CustomerMenu(customerService, input);
            InvoiceMenu invoiceMenu = new InvoiceMenu(bookService, customerService, invoiceService,
                    () -> state.getEmployees().findById(StoreConfig.DEFAULT_EMPLOYEE_ID), input);
            PersonService personService = new PersonService(state.getPeople(), state.getInvoices());
            PersonMenu personMenu = new PersonMenu(personService, input);
            MainMenu menu = new MainMenu(bookMenu, customerMenu, invoiceMenu, reports, input,
                    personMenu, state, storeFile);
            try {
                menu.show();
            } catch (NoSuchElementException e) {
                System.out.println("Đã kết thúc nhập liệu; đang lưu dữ liệu đã hoàn tất.");
                menu.save();
            }
            return 0;
        } catch (IOException | IllegalArgumentException e) {
            System.err.println("Không thể mở/lưu dữ liệu: " + e.getMessage());
            System.err.println("Không tự tạo mẫu đè lên file lỗi. Kiểm tra file hoặc dùng --data <file-khác.txt>.");
            return 1;
        }
    }
}
