package data;

import model.Book;
import model.Customer;
import model.Employee;
import model.Invoice;
import model.InvoiceDetail;
import utility.StoreConfig;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/** Versioned, human-readable snapshot. Parse into a new state, never half-load live data. */
public final class StoreTextCodec {
    private StoreTextCodec() { }

    public static List<String> encode(StoreState state) {
        List<String> lines = new ArrayList<>();
        lines.add("VERSION|" + StoreConfig.FILE_VERSION);
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
                Integer.toString(state.getInvoices().getAll().size()), Integer.toString(details), Integer.toString(state.getStockEvents().length)));
        return lines;
    }

    public static StoreState decode(List<String> lines) throws IOException {
        if (lines.size() < 2 || !(lines.get(0).equals("VERSION|1") || lines.get(0).equals("VERSION|2") || lines.get(0).equals("VERSION|3"))) {
            throw new IOException("File rỗng, thiếu VERSION hoặc phiên bản không được hỗ trợ.");
        }
        boolean legacy = lines.get(0).equals("VERSION|1");
        boolean timestamps = lines.get(0).equals("VERSION|3");
        List<Row> rows = new ArrayList<>();
        for (int i = 1; i < lines.size() - 1; i++) {
            String[] fields = lines.get(i).split("\\|", -1);
            int expected = switch (fields[0]) {
                case "BOOK" -> legacy ? 6 : 8;
                case "DETAIL", "STOCK" -> 6;
                case "CUSTOMER" -> 4;
                case "EMPLOYEE" -> legacy ? 4 : 5;
                case "INVOICE" -> timestamps ? 12 : 9;
                case "SETTING" -> 3;
                default -> -1;
            };
            if (fields.length != expected) throw new IOException("Sai loại bản ghi/số cột ở dòng " + (i + 1));
            rows.add(new Row(i + 1, fields));
        }
        StoreState state = new StoreState();
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
                        Book book = new Book(f[1], f[2], f[3], Double.parseDouble(f[4]), Integer.parseInt(f[5]));
                        if (!legacy) { book.setCategory(f[6]); book.setPurchasePrice(Double.parseDouble(f[7])); }
                        state.getBooks().add(book);
                    }
                    case "CUSTOMER" -> state.getCustomers().add(new Customer(f[1], f[2], f[3]));
                    case "EMPLOYEE" -> state.getEmployees().add(legacy || f[4].isEmpty()
                            ? new Employee(f[1], f[2], f[3]) : new Employee(f[1], f[2], f[4], f[3]));
                    case "STOCK" -> {
                        if (legacy) throw new IllegalArgumentException("STOCK chỉ có ở phiên bản 2.");
                        LocalDate.parse(f[1]);
                        if (f[2].isBlank() || Integer.parseInt(f[3]) < 0 || Integer.parseInt(f[4]) < 0 || f[5].isBlank())
                            throw new IllegalArgumentException("Nhật ký kho không hợp lệ.");
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
                if (book == null) book = new Book(bookId, f[3], "Lưu trữ", price, 0);
                invoice.addDetail(new InvoiceDetail(book, quantity, price, f[3]));
                detailCount++;
            } catch (RuntimeException e) {
                throw invalidRow(row, e);
            }
        }
        try {
            for (Invoice invoice : drafts.values()) {
                invoice.restorePaidAt(paidTimes.get(invoice.getId()));
                state.getInvoices().add(invoice);
            }
            String[] end = lines.get(lines.size() - 1).split("\\|", -1);
            if ((!legacy && !hasThreshold) || end.length != (legacy ? 6 : 7) || !end[0].equals("END")
                    || Integer.parseInt(end[1]) != state.getBooks().getAll().size()
                    || Integer.parseInt(end[2]) != state.getCustomers().getAll().size()
                    || Integer.parseInt(end[3]) != state.getEmployees().getAll().size()
                    || Integer.parseInt(end[4]) != state.getInvoices().getAll().size()
                    || Integer.parseInt(end[5]) != detailCount
                    || (!legacy && Integer.parseInt(end[6]) != state.getStockEvents().length)) {
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

    private static final class Row {
        private final int number;
        private final String[] fields;

        private Row(int number, String[] fields) {
            this.number = number;
            this.fields = fields;
        }
    }
}
