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
import java.util.Base64;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class Main {
    private Main() { }

    public static void main(String[] args) {
        configureConsole();
        try (Scanner scanner = new Scanner(System.in, StandardCharsets.UTF_8)) {
            int exitCode = runInteractive(args, scanner);
            if (exitCode != 0) System.exit(exitCode);
        }
    }

    private static void configureConsole() {
        if (System.getProperty("os.name", "").startsWith("Windows")) {
            try {
                // Java launches child processes without a console on Windows.
                // Attach to this JVM's console before changing its input/output code pages.
                String setup = "Add-Type -TypeDefinition 'using System.Runtime.InteropServices; "
                        + "public static class BookStoreConsole { "
                        + "[DllImport(\"kernel32.dll\")] public static extern bool FreeConsole(); "
                        + "[DllImport(\"kernel32.dll\")] public static extern bool AttachConsole(uint id); "
                        + "[DllImport(\"kernel32.dll\")] public static extern bool SetConsoleCP(uint cp); "
                        + "[DllImport(\"kernel32.dll\")] public static extern bool SetConsoleOutputCP(uint cp); }'; "
                        + "[BookStoreConsole]::FreeConsole() | Out-Null; "
                        + "[BookStoreConsole]::AttachConsole([uint32]::MaxValue) | Out-Null; "
                        + "[BookStoreConsole]::SetConsoleCP(65001) | Out-Null; "
                        + "[BookStoreConsole]::SetConsoleOutputCP(65001) | Out-Null";
                String encodedSetup = Base64.getEncoder().encodeToString(
                        setup.getBytes(StandardCharsets.UTF_16LE));
                new ProcessBuilder("powershell.exe", "-NoProfile", "-NonInteractive",
                        "-EncodedCommand", encodedSetup)
                        .redirectOutput(ProcessBuilder.Redirect.DISCARD)
                        .redirectError(ProcessBuilder.Redirect.DISCARD)
                        .start().waitFor();
            } catch (IOException e) {
                // Redirected streams and non-Windows consoles still use UTF-8 below.
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        System.setOut(new PrintStream(System.out, true, StandardCharsets.UTF_8));
        System.setErr(new PrintStream(System.err, true, StandardCharsets.UTF_8));
    }

    /** Reusable entry point for integration tests; the caller owns the Scanner. */
    public static int run(String[] args, Scanner scanner) {
        return run(args, scanner, false);
    }

    /** Current application flow. The original run overload retains the scripted integration API. */
    public static int runInteractive(String[] args, Scanner scanner) {
        return run(args, scanner, true);
    }

    private static int run(String[] args, Scanner scanner, boolean managed) {
        try {
            Path path = StoreConfig.resolveStoreFile(args);
            StoreFile storeFile = new StoreFile(path);
            boolean firstRun = Files.notExists(path);
            StoreState state = firstRun ? SampleData.create() : storeFile.load();
            if (firstRun) storeFile.save(state);
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
            MainMenu menu = managed ? new MainMenu(new business.StoreSession(state, storeFile), input)
                    : new MainMenu(bookMenu, customerMenu, invoiceMenu, reports, input, personMenu, state, storeFile);
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
