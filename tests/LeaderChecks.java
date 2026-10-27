import business.InvoiceService;
import business.ReportService;
import business.PersonService;
import data.BookRepository;
import data.PersonDirectory;
import data.SampleData;
import data.StoreFile;
import data.StoreState;
import data.StoreTextCodec;
import model.Book;
import model.Customer;
import model.Employee;
import model.Invoice;
import model.Person;
import presentation.Main;
import presentation.MainMenu;
import utility.FileManager;
import utility.InputHelper;
import utility.StoreConfig;

import java.io.IOException;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

/** Acceptance tests for the Leader foundation and the teacher's array/file requirements. */
public class LeaderChecks {
    private static int checks;

    private LeaderChecks() { }

    private interface Action { void run() throws Exception; }

    private static void check(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message);
    }

    private static void rejects(Class<? extends Throwable> type, Action action) {
        checks++;
        try {
            action.run();
        } catch (Throwable error) {
            if (type.isInstance(error)) return;
            throw new AssertionError("Wrong exception", error);
        }
        throw new AssertionError("Expected " + type.getName());
    }

    private static InvoiceService sales(StoreState state) {
        return new InvoiceService(state.getInvoices(), state.getBooks(), state.getCustomers(), state.getEmployees());
    }

    public static void main(String[] args) throws Exception {
        Path root = Path.of(args[0]);
        PersonDirectory people = new PersonDirectory();
        Customer customer = new Customer("KH1", "Khách Ánh", "0900000000");
        Employee employee = new Employee("NV1", "Nhân viên Bình", "Thu ngân");
        people.initialize(new Person[] { customer, employee });
        check(people.size() == 2, "Initialize n people");
        for (int i = 2; i <= 10; i++) people.add(new Customer("KH" + i, "Khách " + i, "0900000000"));
        check(people.size() == 11 && people.findById("kh10") != null, "Array grows beyond initial capacity");
        check(people.countByRole().get("Customer") == 10 && people.countByRole().get("Employee") == 1,
                "Runtime polymorphic role counts");
        Person[] exported = people.toArray();
        exported[0] = null;
        check(people.findById("KH1") == customer, "Defensive array copy");
        check(people.search("ánh", "Customer", false).length == 1, "Approximate Unicode name search");
        check(people.search("KH1", "Customer", true).length == 1, "Exact ID search");
        check(people.search("Thu", "Employee", false).length == 1, "Position and role search");
        check(people.search("KH1", "Employee", true).length == 0, "Role and keyword are ANDed");
        check(people.search("0900000000", "Customer", true).length == 10, "Phone search");
        people.addAll(new Person[] { new Customer("KH11", "Batch A", "0900000000"),
                new Employee("NV2", "Batch B", "Quản lý") });
        check(people.size() == 13, "Append k people");
        rejects(IllegalArgumentException.class, () -> people.addAll(new Person[] {
                new Customer("NEW", "New", "0900000000"), customer }));
        check(people.size() == 13 && people.findById("NEW") == null, "Batch failure is atomic");
        rejects(IllegalArgumentException.class, () -> people.initialize(new Person[] { customer, customer }));
        check(people.size() == 13, "Failed initialization keeps existing array");
        check(people.updateById("KH1", new Customer("KH1", "Changed", "0910000000")), "Update by ID");
        check(people.findById("KH1").getName().equals("Changed"), "Replacement visible");
        rejects(IllegalArgumentException.class, () -> people.updateById("KH1", employee));
        check(people.delete("NV1") && people.delete("KH1") && people.delete("KH11"), "Delete and shift");
        check(people.size() == 10 && people.findById("NV2") != null, "Shift preserves remaining records");
        check(!people.delete("missing"), "Missing delete");

        StoreState shared = SampleData.create();
        shared.getPeople().add(new Customer("KH3", "From array", "0900000000"));
        check(shared.getCustomers().findById("KH3") != null, "Customer view shares central Person[]");
        shared.getCustomers().add(new Customer("KH4", "From repository", "0900000000"));
        check(shared.getPeople().findById("KH4") != null, "Repository additions update central array");
        rejects(IllegalArgumentException.class, () -> shared.getEmployees().add(
                new Employee("KH4", "Conflicting ID", "Role")));
        check(shared.getEmployees().findById("KH4") == null, "Typed view cannot return wrong subtype");
        check(!shared.getEmployees().delete("KH4"), "Typed delete cannot delete another subtype");
        BookRepository manyBooks = new BookRepository();
        for (int i = 0; i < 12; i++) manyBooks.add(new Book("B" + i, "Book " + i, "A", 100, 5));
        check(manyBooks.getAll().size() == 12, "Book[] capacity growth");
        check(manyBooks.delete("B0") && manyBooks.delete("B5") && manyBooks.delete("B11"), "Book[] shift");
        check(manyBooks.getAll().size() == 9 && manyBooks.findById("B10") != null, "Book[] contents after removal");

        StoreState state = SampleData.create();
        InvoiceService service = sales(state);
        Employee cashier = state.getEmployees().findById(StoreConfig.DEFAULT_EMPLOYEE_ID);
        rejects(IllegalArgumentException.class, () -> service.createInvoice("MISSING_EMP", "KH001",
                new Employee("UNKNOWN", "Fake", "Role"), Map.of("S001", 1)));
        Invoice invoice = service.createInvoice("HD001", "KH001", cashier, Map.of("S001", 2));
        PersonService personService = new PersonService(state.getPeople(), state.getInvoices());
        rejects(IllegalStateException.class, () -> personService.delete("NV001"));
        rejects(IllegalStateException.class, () -> personService.delete("KH001"));
        rejects(IllegalStateException.class, () -> personService.initialize(new Person[] { cashier }));
        check(personService.delete("KH002"), "Unreferenced person may be removed");
        state.getCustomers().add(new Customer("KH002", "Khách hàng thứ hai", "0910000000"));
        String savedName = invoice.getCustomerName();
        String savedBookTitle = invoice.getDetails().get(0).getBookTitle();
        state.getBooks().findById("S001").setTitle("New title");
        state.getBooks().findById("S001").setPrice(250000);
        state.getCustomers().findById("KH001").setName("New customer");
        state.getCustomers().findById("KH001").setPhone("0999999999");
        for (int i = 2; i <= 7; i++) service.createInvoice("HD00" + i, "KH001", cashier, Map.of("S002", 1));
        check(state.getInvoices().getAll().size() == 7, "Invoice[] capacity growth");
        Path storePath = root.resolve("leader-store.txt");
        StoreFile file = new StoreFile(storePath);
        file.save(state);
        StoreState loaded = file.load();
        Invoice restored = loaded.getInvoices().findById("HD001");
        check(loaded.getBooks().findById("S001").getQuantity() == 18, "Stock survives restart");
        check(restored.getTotal() == 300000 && restored.getCreatedDate().equals(LocalDate.now()),
                "Historical price and date survive restart");
        check(restored.getCustomerName().equals(savedName) && restored.getCustomerPhone().equals("0900000000")
                && restored.getDetails().get(0).getBookTitle().equals(savedBookTitle), "Historical snapshot round trip");
        check(loaded.getCustomers().findById("KH001").getName().equals("New customer"), "Live and historical names differ");
        check(new ReportService(loaded.getInvoices()).getRevenue() == 900000, "Revenue after restart");
        check(StoreTextCodec.encode(loaded).equals(StoreTextCodec.encode(state)), "Stable full snapshot round trip");
        loaded.getBooks().delete("S001");
        loaded.getCustomers().delete("KH001");
        StoreState archived = StoreTextCodec.decode(StoreTextCodec.encode(loaded));
        check(archived.getInvoices().findById("HD001").getTotal() == 300000,
                "Historical invoices load even if catalog records were removed");

        List<String> valid = StoreTextCodec.encode(state);
        rejects(IOException.class, () -> StoreTextCodec.decode(List.of()));
        rejects(IOException.class, () -> StoreTextCodec.decode(List.of("VERSION|999", "END|0|0|0|0|0")));
        rejects(IOException.class, () -> StoreTextCodec.decode(valid.subList(0, valid.size() - 1)));
        List<String> duplicate = new ArrayList<>(valid);
        duplicate.add(2, valid.get(1));
        rejects(IOException.class, () -> StoreTextCodec.decode(duplicate));
        List<String> corruptPrice = new ArrayList<>(valid);
        corruptPrice.set(1, "BOOK|S001|B|A|NaN|20");
        rejects(IOException.class, () -> StoreTextCodec.decode(corruptPrice));
        List<String> corruptDetail = new ArrayList<>(valid);
        int detailIndex = -1;
        for (int i = 0; i < valid.size(); i++) if (valid.get(i).startsWith("DETAIL|")) { detailIndex = i; break; }
        corruptDetail.set(detailIndex, "DETAIL|MISSING|S001|B|1|100");
        rejects(IOException.class, () -> StoreTextCodec.decode(corruptDetail));
        List<String> corruptCount = new ArrayList<>(valid);
        corruptCount.set(corruptCount.size() - 1, "END|2|2|1|999|7");
        rejects(IOException.class, () -> StoreTextCodec.decode(corruptCount));
        List<String> corruptDate = new ArrayList<>(valid);
        for (int i = 0; i < corruptDate.size(); i++) {
            if (corruptDate.get(i).startsWith("INVOICE|")) {
                String[] fields = corruptDate.get(i).split("\\|", -1);
                fields[8] = "2026-02-30";
                corruptDate.set(i, String.join("|", fields));
                break;
            }
        }
        rejects(IOException.class, () -> StoreTextCodec.decode(corruptDate));
        byte[] original = Files.readAllBytes(storePath);
        rejects(NullPointerException.class, () -> FileManager.writeLinesAtomic(storePath, Arrays.asList("partial", null)));
        check(Arrays.equals(original, Files.readAllBytes(storePath)), "Invalid write input preserves previous snapshot");
        Path directoryTarget = root.resolve("blocked-target");
        Files.createDirectories(directoryTarget);
        Files.writeString(directoryTarget.resolve("keep.txt"), "keep");
        rejects(IOException.class, () -> new StoreFile(directoryTarget).save(state));
        check(Files.readString(directoryTarget.resolve("keep.txt")).equals("keep"), "Save failure preserves existing target");
        try (Scanner scanner = new Scanner("0\n")) {
            MainMenu menu = new MainMenu(null, null, null, null, new InputHelper(scanner), null, state,
                    new StoreFile(directoryTarget));
            // A failed save on option 0 must loop for input instead of silently exiting.
            rejects(java.util.NoSuchElementException.class, menu::show);
        }

        Path appPath = root.resolve("app-session.txt");
        try (Scanner scanner = new Scanner("3\nAPP001\nKH001\nS001\n2\n0\n0\n")) {
            check(Main.run(new String[] { "--data", appPath.toString() }, scanner) == 0, "Main integration checkout/save");
        }
        StoreState appState = new StoreFile(appPath).load();
        check(appState.getBooks().findById("S001").getQuantity() == 18
                && appState.getInvoices().findById("APP001").getTotal() == 300000, "Main writes consistent stock and invoice");
        try (Scanner scanner = new Scanner("")) {
            check(Main.run(new String[] { "--data", appPath.toString() }, scanner) == 0, "EOF saves completed work");
        }
        check(new StoreFile(appPath).load().getInvoices().getAll().size() == 1, "Restart does not duplicate seed data");
        Path peopleAppPath = root.resolve("people-app-session.txt");
        String peopleInput = String.join("\n", "6", "4", "2", "1", "KHNEW", "New customer", "0900000000",
                "2", "NV2", "Second employee", "Sales", "5", "KHNEW", "Renamed", "0911111111",
                "7", "Renamed", "Customer", "1", "8", "6", "KHNEW", "5", "NV001", "New cashier", "Manager",
                "0", "3", "PEOPLE001", "KH001", "S001", "1", "0", "0") + "\n";
        try (Scanner scanner = new Scanner(peopleInput)) {
            check(Main.run(new String[] { "--data", peopleAppPath.toString() }, scanner) == 0,
                    "PersonMenu batch/update/search/stats/delete and checkout integration");
        }
        StoreState peopleApp = new StoreFile(peopleAppPath).load();
        check(peopleApp.getCustomers().findById("KHNEW") == null && peopleApp.getEmployees().findById("NV2") != null
                && peopleApp.getInvoices().findById("PEOPLE001").getEmployeeName().equals("New cashier"),
                "Shared array edits, typed views, current cashier and file persistence agree");
        Path broken = root.resolve("broken-store.txt");
        Files.writeString(broken, "invalid file");
        ByteArrayOutputStream errors = new ByteArrayOutputStream();
        PrintStream previousErr = System.err;
        try (Scanner scanner = new Scanner("0\n");
             PrintStream errorSink = new PrintStream(errors, true, StandardCharsets.UTF_8)) {
            try {
                System.setErr(errorSink);
                check(Main.run(new String[] { "--data", broken.toString() }, scanner) == 1, "Invalid file rejects startup");
            } finally {
                System.setErr(previousErr);
            }
        }
        check(errors.toString(StandardCharsets.UTF_8).contains("Không tự tạo mẫu"), "Startup explains refusal to overwrite");
        check(Files.readString(broken).equals("invalid file"), "Startup does not overwrite corrupt data");
        rejects(IllegalArgumentException.class, () -> StoreConfig.resolveStoreFile(new String[] { "bad" }));
        System.out.println("PASS: " + checks + " Leader checks");
    }
}
