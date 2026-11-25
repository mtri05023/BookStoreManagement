package bookstore;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class StoreFile {
    private final Path path;

    public StoreFile(Path path) { this.path = Objects.requireNonNull(path); }

    public Path getPath() { return path; }

    public BookStore load() throws IOException {
        return StoreFile.decode(StoreFile.readLines(path));
    }

    public void save(BookStore state) throws IOException {
        if (java.nio.file.Files.exists(path)) {
            var existing = StoreFile.readLines(path);
            // Validate before replacing: never seed or overwrite an empty/corrupt snapshot.
            StoreFile.decode(existing);
            if (!existing.get(0).equals("VERSION|" + StoreFile.FILE_VERSION)) {
                String version = existing.get(0).substring("VERSION|".length());
                Path backup = path.resolveSibling(path.getFileName() + ".v" + version + ".bak");
                if (java.nio.file.Files.exists(backup) && !java.util.Arrays.equals(
                        java.nio.file.Files.readAllBytes(path), java.nio.file.Files.readAllBytes(backup))) {
                    backup = path.resolveSibling(path.getFileName() + ".v" + version + "-" + java.util.UUID.randomUUID() + ".bak");
                }
                if (!java.nio.file.Files.exists(backup)) java.nio.file.Files.copy(path, backup);
            }
        }
        StoreFile.writeLinesAtomic(path, StoreFile.encode(Objects.requireNonNull(state)));
    }
    public static final String DEFAULT_EMPLOYEE_ID = "NV001";
    public static final String STORE_NAME = "CỬA HÀNG SÁCH";
    public static final String FILE_VERSION = "4";
    public static final Charset TEXT_CHARSET = StandardCharsets.UTF_8;
    public static final Path DATA_DIRECTORY = Path.of("data");
    public static final Path STORE_FILE = DATA_DIRECTORY.resolve("store.txt");

    public static Path resolveStoreFile(String[] args) {
        if (args.length == 0) return STORE_FILE;
        if (args.length == 2 && args[0].equals("--data")) return Path.of(args[1]);
        throw new IllegalArgumentException("Cách chạy: Main [--data <file.txt>]");
    }
    public static Path resolveAccountFile(Path storeFile) {
        return storeFile.resolveSibling(storeFile.getFileName().toString() + ".accounts");
    }
    public static List<String> readLines(Path path) throws IOException {
        if (Files.notExists(path)) return List.of();
        return Files.readAllLines(path, StandardCharsets.UTF_8);
    }

    public static void writeLines(Path path, List<String> lines) throws IOException {
        Path parent = path.toAbsolutePath().getParent();
        if (parent != null) Files.createDirectories(parent);
        Files.write(path, lines, StandardCharsets.UTF_8);
    }

    /** Fail safely when the filesystem cannot atomically replace the snapshot. */
    public static void writeLinesAtomic(Path path, List<String> lines) throws IOException {
        Objects.requireNonNull(lines, "Lines must not be null.");
        for (String line : lines) Objects.requireNonNull(line, "A line must not be null.");
        Path target = path.toAbsolutePath();
        Path parent = target.getParent();
        Files.createDirectories(parent);
        Path temporary = Files.createTempFile(parent, ".bookstore-", ".tmp");
        try {
            Files.write(temporary, lines, StoreFile.TEXT_CHARSET);
            Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } finally {
            Files.deleteIfExists(temporary);
        }
    }
    public static List<String> encode(BookStore state) {
        List<String> lines = new ArrayList<>();
        lines.add("VERSION|" + StoreFile.FILE_VERSION);
        lines.add("SETTING|LOW_STOCK|" + state.getLowStockThreshold());
        for (Book book : state.getBooks().getAll()) {
            lines.add(String.join("|", "BOOK", book.getId(), book.getTitle(), book.getAuthor(),
                    Double.toString(book.getPrice()), Integer.toString(book.getQuantity()),
                    book.getCategory(), Double.toString(book.getPurchasePrice())));
        }
        for (Customer customer : state.getCustomers().getAll()) {
            lines.add(String.join("|", "CUSTOMER", customer.getId(), customer.getName(), customer.getPhone()));
        }
        for (Employee employee : state.getEmployees().getAll()) {
            lines.add(String.join("|", "EMPLOYEE", employee.getId(), employee.getName(), employee.getPosition(), employee.getPhone()));
        }
        for (Supplier supplier : state.getSuppliers().getAll())
            lines.add(String.join("|", "SUPPLIER", supplier.getId(), supplier.getName(), supplier.getPhone()));
        int details = 0;
        for (Invoice invoice : state.getInvoices().getAll()) {
            if (!invoice.isCompleted()) throw new IllegalStateException("File bán hàng chỉ lưu hóa đơn đã thanh toán.");
            lines.add(String.join("|", "INVOICE", invoice.getId(), invoice.getCustomer().getId(),
                    invoice.getCustomerName(), invoice.getCustomerPhone(), invoice.getEmployee().getId(),
                    invoice.getEmployeeName(), invoice.getEmployeePosition(), invoice.getCreatedDate().toString(),
                    invoice.getCreatedAt().toString(), invoice.getPaidAt() == null ? "" : invoice.getPaidAt().toString(),
                    invoice.getStatus().name()));
            for (InvoiceDetail detail : invoice.getDetails()) {
                lines.add(String.join("|", "DETAIL", invoice.getId(), detail.getBookId(), detail.getBookTitle(),
                        Integer.toString(detail.getQuantity()), Double.toString(detail.getPrice())));
                details++;
            }
        }
        java.util.Collections.addAll(lines, state.getStockEvents());
        lines.add(String.join("|", "END", Integer.toString(state.getBooks().getAll().size()),
                Integer.toString(state.getCustomers().getAll().size()),
                Integer.toString(state.getEmployees().getAll().size()),
                Integer.toString(state.getInvoices().getAll().size()), Integer.toString(details), Integer.toString(state.getStockEvents().length),
                Integer.toString(state.getSuppliers().getAll().size())));
        return lines;
    }

    public static BookStore decode(List<String> lines) throws IOException {
        if (lines.size() < 2 || !(lines.get(0).equals("VERSION|1") || lines.get(0).equals("VERSION|2")
                || lines.get(0).equals("VERSION|3") || lines.get(0).equals("VERSION|4"))) {
            throw new IOException("File rỗng, thiếu VERSION hoặc phiên bản không được hỗ trợ.");
        }
        boolean legacy = lines.get(0).equals("VERSION|1");
        boolean version4 = lines.get(0).equals("VERSION|4");
        boolean timestamps = lines.get(0).equals("VERSION|3") || version4;
        List<Row> rows = new ArrayList<>();
        for (int i = 1; i < lines.size() - 1; i++) {
            String[] fields = lines.get(i).split("\\|", -1);
            int expected = switch (fields[0]) {
                case "BOOK" -> legacy ? 6 : 8;
                case "DETAIL", "STOCK" -> 6;
                case "RECEIPT" -> 8;
                case "SOURCE" -> 4;
                case "CUSTOMER" -> 4;
                case "EMPLOYEE" -> legacy ? 4 : 5;
                case "SUPPLIER" -> 4;
                case "INVOICE" -> timestamps ? 12 : 9;
                case "SETTING" -> 3;
                default -> -1;
            };
            if (fields.length != expected) throw new IOException("Sai loại bản ghi/số cột ở dòng " + (i + 1));
            rows.add(new Row(i + 1, fields));
        }
        BookStore state = new BookStore();
        Map<String, Invoice> drafts = new LinkedHashMap<>();
        Map<String, LocalDateTime> paidTimes = new java.util.HashMap<>();
        boolean hasThreshold = false;
        for (Row row : rows) {
            String[] f = row.fields;
            try {
                switch (f[0]) {
                    case "SETTING" -> {
                        if (legacy || hasThreshold || !f[1].equals("LOW_STOCK"))
                            throw new IllegalArgumentException("Cấu hình trùng hoặc không hợp lệ.");
                        state.setLowStockThreshold(Integer.parseInt(f[2]));
                        hasThreshold = true;
                    }
                    case "BOOK" -> {
                        Book book = Book.restore(f[1], f[2], f[3], legacy ? "Chưa phân loại" : f[6], Double.parseDouble(f[4]), Integer.parseInt(f[5]));
                        if (!legacy) { book.restorePurchasePrice(Double.parseDouble(f[7])); }
                        state.getBooks().add(book);
                    }
                    case "CUSTOMER" -> {
                        if (state.getEmployees().findById(f[1]) != null)
                            throw new IllegalArgumentException("Mã người đã tồn tại: " + f[1]);
                        state.getCustomers().add(new Customer(f[1], f[2], f[3]));
                    }
                    case "EMPLOYEE" -> {
                        if (state.getCustomers().findById(f[1]) != null)
                            throw new IllegalArgumentException("Mã người đã tồn tại: " + f[1]);
                        state.getEmployees().add(legacy || f[4].isEmpty()
                            ? new Employee(f[1], f[2], f[3]) : new Employee(f[1], f[2], f[4], f[3]));
                    }
                    case "SUPPLIER" -> state.getSuppliers().add(new Supplier(f[1], f[2], f[3]));
                    case "SOURCE" -> {
                        if (state.getBooks().findById(f[1]) == null || state.getSuppliers().findById(f[2]) == null || f[3].isBlank())
                            throw new IllegalArgumentException("Nguồn cung sách không hợp lệ.");
                        state.addStockEvent(String.join("|", f));
                    }
                    case "STOCK" -> {
                        if (legacy) throw new IllegalArgumentException("STOCK chỉ có ở phiên bản 2.");
                        LocalDate.parse(f[1]);
                        if (f[2].isBlank() || Integer.parseInt(f[3]) < 0 || Integer.parseInt(f[4]) < 0 || f[5].isBlank())
                            throw new IllegalArgumentException("Nhật ký kho không hợp lệ.");
                        state.addStockEvent(String.join("|", f));
                    }
                    case "RECEIPT" -> {
                        if (!version4) throw new IllegalArgumentException("RECEIPT chỉ có ở phiên bản 4.");
                        LocalDate.parse(f[1]);
                        if (f[2].isBlank() || Integer.parseInt(f[3]) < 0 || Integer.parseInt(f[4]) <= Integer.parseInt(f[3])
                                || f[5].isBlank() || f[6].isBlank() || f[7].isBlank())
                            throw new IllegalArgumentException("Nhật ký nhập hàng không hợp lệ.");
                        state.addStockEvent(String.join("|", f));
                    }
                    case "INVOICE" -> {
                        // Independent historical people retain names/phone even after catalog edits/deletion.
                        LocalDate date = LocalDate.parse(f[8]);
                        LocalDateTime created = timestamps ? LocalDateTime.parse(f[9]) : date.atStartOfDay();
                        if (!created.toLocalDate().equals(date) || (timestamps && !f[11].equals("PAID")))
                            throw new IllegalArgumentException("Ngày lập hoặc trạng thái hóa đơn không hợp lệ.");
                        Invoice invoice = new Invoice(f[1], new Customer(f[2], f[3], f[4]),
                                new Employee(f[5], f[6], f[7]), created);
                        paidTimes.put(invoice.getId(), timestamps && !f[10].isEmpty() ? LocalDateTime.parse(f[10]) : null);
                        if (drafts.putIfAbsent(invoice.getId(), invoice) != null) {
                            throw new IllegalArgumentException("Trùng mã hóa đơn.");
                        }
                    }
                    default -> { /* Details are attached after every book/invoice is known. */ }
                }
            } catch (RuntimeException e) {
                throw invalidRow(row, e);
            }
        }
        Set<String> detailKeys = new HashSet<>();
        int detailCount = 0;
        for (Row row : rows) {
            String[] f = row.fields;
            if (!f[0].equals("DETAIL")) continue;
            try {
                String invoiceId = normalizeId(f[1]);
                Invoice invoice = drafts.get(invoiceId);
                if (invoice == null) throw new IllegalArgumentException("Chi tiết không có hóa đơn.");
                String bookId = normalizeId(f[2]);
                if (!detailKeys.add(invoiceId + "|" + bookId)) {
                    throw new IllegalArgumentException("Trùng sách trong hóa đơn.");
                }
                int quantity = Integer.parseInt(f[4]);
                double price = Double.parseDouble(f[5]);
                Book book = state.getBooks().findById(bookId);
                if (book == null) book = Book.create(bookId, f[3], "Lưu trữ", price, 0);
                invoice.addDetail(new InvoiceDetail(book, quantity, price, f[3]));
                detailCount++;
            } catch (RuntimeException e) {
                throw invalidRow(row, e);
            }
        }
        try {
            if (!version4 && state.getSuppliers().getAll().isEmpty()) BookStore.addDefaultSuppliers(state);
            for (Invoice invoice : drafts.values()) {
                invoice.restorePaidAt(paidTimes.get(invoice.getId()));
                state.getInvoices().add(invoice);
            }
            String[] end = lines.get(lines.size() - 1).split("\\|", -1);
            if ((!legacy && !hasThreshold) || end.length != (version4 ? 8 : legacy ? 6 : 7) || !end[0].equals("END")
                    || Integer.parseInt(end[1]) != state.getBooks().getAll().size()
                    || Integer.parseInt(end[2]) != state.getCustomers().getAll().size()
                    || Integer.parseInt(end[3]) != state.getEmployees().getAll().size()
                    || Integer.parseInt(end[4]) != state.getInvoices().getAll().size()
                    || Integer.parseInt(end[5]) != detailCount
                    || (!legacy && Integer.parseInt(end[6]) != state.getStockEvents().length)
                    || (version4 && Integer.parseInt(end[7]) != state.getSuppliers().getAll().size())) {
                throw new IllegalArgumentException("Thiếu END hoặc số bản ghi không khớp.");
            }
        } catch (RuntimeException e) {
            throw new IOException("File chưa đầy đủ hoặc hóa đơn rỗng: " + e.getMessage(), e);
        }
        return state;
    }

    private static String normalizeId(String id) { return id.trim().toUpperCase(Locale.ROOT); }

    private static IOException invalidRow(Row row, RuntimeException cause) {
        return new IOException("Dữ liệu lỗi ở dòng " + row.number + ": " + cause.getMessage(), cause);
    }

    private record Row(int number, String[] fields) { }

    public boolean exists() { return Files.exists(path); }
    public AccountList loadAccounts() throws IOException {
        var lines = StoreFile.readLines(path);
        AccountList repository = decodeAccounts(lines);
        boolean hasLegacyRecoveryField = lines.stream().skip(1).limit(Math.max(0, lines.size() - 2L))
                .anyMatch(line -> line.startsWith("ACCOUNT|") && line.split("\\|", -1).length == 9);
        if (hasLegacyRecoveryField) save(repository);
        return repository;
    }
    public void save(AccountList repository) throws IOException {
        if (Files.exists(path)) decodeAccounts(StoreFile.readLines(path));
        StoreFile.writeLinesAtomic(path, encodeAccounts(repository));
    }

    public static List<String> encodeAccounts(AccountList repository) {
        List<String> lines = new ArrayList<>();
        lines.add("ACCOUNTS|1");
        for (Account a : repository.getAll()) lines.add(String.join("|", "ACCOUNT", a.getId(), a.getUsername(),
                a.getPasswordHash(), a.getRole().name(), a.getEmployeeId(), Boolean.toString(a.isActive()),
                Boolean.toString(a.mustChangePassword())));
        lines.add("END|" + repository.getAll().size());
        return lines;
    }
    public static AccountList decodeAccounts(List<String> lines) throws IOException {
        if (lines.size() < 2 || !"ACCOUNTS|1".equals(lines.get(0))) throw new IOException("File tài khoản sai header hoặc phiên bản.");
        AccountList result = new AccountList();
        try {
            for (int i = 1; i < lines.size() - 1; i++) {
                String[] f = lines.get(i).split("\\|", -1);
                if ((f.length != 8 && f.length != 9) || !f[0].equals("ACCOUNT")) throw new IllegalArgumentException("Sai cấu trúc tài khoản.");
                if (!(f[6].equals("true") || f[6].equals("false")) || !(f[7].equals("true") || f[7].equals("false")))
                    throw new IllegalArgumentException("Trạng thái tài khoản không hợp lệ.");
                if (!validHash(f[3])) throw new IllegalArgumentException("Mã băm mật khẩu bị lỗi.");
                result.add(new Account(f[1], f[2], f[3], Account.Role.valueOf(f[4]), f[5], Boolean.parseBoolean(f[6]),
                        Boolean.parseBoolean(f[7])));
            }
            String[] end = lines.get(lines.size() - 1).split("\\|", -1);
            if (end.length != 2 || !end[0].equals("END") || Integer.parseInt(end[1]) != result.getAll().size())
                throw new IllegalArgumentException("END hoặc số tài khoản không khớp.");
        } catch (RuntimeException e) { throw new IOException("File tài khoản không hợp lệ: " + e.getMessage(), e); }
        if (result.getAll().stream().noneMatch(a -> a.getRole() == Account.Role.ADMIN && a.isActive()))
            throw new IOException("File phải có ít nhất một Admin đang hoạt động.");
        return result;
    }
    private static boolean validHash(String value) {
        try {
            String[] parts = value.split("\\$", -1);
            if (parts.length != 3) return false;
            int rounds = Integer.parseInt(parts[0]);
            byte[] salt = java.util.Base64.getUrlDecoder().decode(parts[1]);
            byte[] hash = java.util.Base64.getUrlDecoder().decode(parts[2]);
            return rounds >= 100_000 && rounds <= 2_000_000 && salt.length >= 16 && hash.length == 32;
        } catch (RuntimeException e) { return false; }
    }
}
