package bookstore;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;

public class SalesReport {
    private final InputHelper input;
    private final BookStore state;
    public SalesReport(InputHelper input, BookStore state) {
        this.input = input;
        this.state = state;
    }
    public void show() {
        while (true) {
            InputHelper.options("THỐNG KÊ DOANH THU", "Doanh thu hôm nay", "Doanh thu theo ngày", "Doanh thu theo tháng",
                    "Doanh thu theo năm", "Doanh thu theo khoảng thời gian", "Số lượng sách bán ra", "Doanh thu từng sách",
                    "Top sách bán chạy", "Thống kê hóa đơn", "Doanh thu theo khách hàng", "Xếp hạng nhà cung cấp theo sách bán");
            int choice = input.readInt("Chọn: ");
            try {
                if (choice == 0) return;
                if (choice < 1 || choice > 11) { System.out.println("Lựa chọn không hợp lệ."); continue; }
                LocalDate[] range;
                if (choice <= 5) range = InputHelper.range(input, choice);
                else {
                    int mode = input.readInt("Thời gian: 1. Hôm nay, 2. Ngày, 3. Tháng, 4. Năm, 5. Khoảng ngày, 6. Toàn bộ: ");
                    range = mode == 6 ? new LocalDate[] { LocalDate.MIN, LocalDate.MAX } : InputHelper.range(input, mode);
                }
                summary(range[0], range[1], choice);
            } catch (IllegalArgumentException | IllegalStateException | java.time.DateTimeException e) { System.out.println(e.getMessage()); }
        }
    }
    public void summary(LocalDate from, LocalDate to, int mode) {
        Invoice[] invoices = filter(from, to);
        var quantities = new LinkedHashMap<String, Long>();
        var revenues = new LinkedHashMap<String, BigDecimal>();
        var titles = new LinkedHashMap<String, String>();
        var supplierByBook = latestSupplierByBook();
        var customers = new HashSet<String>();
        BigDecimal total = BigDecimal.ZERO;
        long units = 0;
        for (Invoice i : invoices) {
            total = total.add(BigDecimal.valueOf(i.getTotal())); customers.add(i.getCustomer().getId());
            for (var d : i.getDetails()) {
                units += d.getQuantity(); quantities.merge(d.getBookId(), (long) d.getQuantity(), Long::sum);
                revenues.merge(d.getBookId(), BigDecimal.valueOf(d.getAmount()), BigDecimal::add);
                titles.put(d.getBookId(), d.getBookTitle());
            }
        }
        System.out.println("\nTHỐNG KÊ DOANH THU: " + (from.equals(LocalDate.MIN) ? "Toàn bộ thời gian" : from.format(InputHelper.DATE) + " - " + to.format(InputHelper.DATE)));
        System.out.printf(InputHelper.CURRENCY_LOCALE, "Tổng doanh thu: %,.0f VNĐ%nSố hóa đơn: %d%nSố lượng sách bán ra: %d%nSố khách hàng: %d%n",
                total, invoices.length, units, customers.size());
        if (invoices.length == 0) System.out.println("Chưa có giao dịch trong thời gian đã chọn.");
        else System.out.println("Đã lập thống kê thành công từ " + invoices.length + " hóa đơn.");
        System.out.println("SÁCH BÁN CHẠY VÀ DOANH THU TỪNG SÁCH (tên tại lần bán cuối trong kết quả)");
        System.out.printf("%-10s %-28s %-30s %12s %16s%n", "Mã sách", "Tên sách", "Nhà cung cấp", "Số lượng", "Doanh thu");
        quantities.entrySet().stream().sorted(java.util.Map.Entry.<String, Long>comparingByValue().reversed()
                .thenComparing(java.util.Map.Entry.comparingByKey())).forEach(e ->
                System.out.printf("%-10s %-28s %-30s %12d %16.0f%n", e.getKey(), titles.get(e.getKey()),
                        supplierByBook.getOrDefault(e.getKey(), "Chưa ghi nhận"), e.getValue(), revenues.get(e.getKey())));
        if (mode == 9) {
            System.out.println("Mã hóa đơn | Ngày thanh toán | Mã khách hàng | Thực thanh toán");
            for (Invoice i : invoices) System.out.printf(InputHelper.CURRENCY_LOCALE, "%s | %s | %s | %,.0f%n", i.getId(), i.getPaymentDate().format(InputHelper.DATE), i.getCustomer().getId(), i.getTotal());
        }
        if (mode == 10) {
            System.out.println("DOANH THU THEO KHÁCH HÀNG: Mã | Tổng tiền (VNĐ)");
            revenueByCustomer(from, to).entrySet().stream()
                    .sorted(java.util.Map.Entry.<String, Double>comparingByValue().reversed())
                    .forEach(e -> System.out.printf(InputHelper.CURRENCY_LOCALE, "%s | %,.0f%n", e.getKey(), e.getValue()));
            if (revenueByCustomer(from, to).isEmpty()) System.out.println("Không có khách hàng phát sinh doanh thu trong thời gian này.");
        }
        if (mode == 11) printSupplierRanking(invoices);
    }

    private java.util.Map<String, String> latestSupplierByBook() {
        var result = new LinkedHashMap<String, String>();
        if (state == null) return result;
        for (String row : state.getStockEvents()) {
            String[] fields = row.split("\\|", -1);
            if (fields.length == 4 && fields[0].equals("SOURCE")) result.put(fields[1], fields[3]);
            else if (fields.length == 8 && fields[0].equals("RECEIPT")) result.put(fields[2], fields[6]);
        }
        return result;
    }

    private void printSupplierRanking(Invoice[] invoices) {
        record Sales(long quantity, BigDecimal revenue) { }
        var totals = new LinkedHashMap<String, Sales>();
        for (Invoice invoice : invoices) {
            for (var detail : invoice.getDetails()) {
                String supplier = supplierAtSaleDate(detail.getBookId(), invoice.getPaymentDate());
                Sales sales = totals.getOrDefault(supplier, new Sales(0, BigDecimal.ZERO));

                totals.put(supplier, new Sales(sales.quantity + detail.getQuantity(),
                        sales.revenue.add(BigDecimal.valueOf(detail.getAmount()))));
            }
        }
        System.out.println("XẾP HẠNG NHÀ CUNG CẤP (theo lần nhập gần nhất; dùng nguồn mặc định nếu chưa có nhật ký nhập)");
        System.out.printf("%-5s %-32s %14s %18s%n", "Hạng", "Nhà cung cấp", "Số lượng bán", "Doanh thu (VNĐ)");
        int[] rank = {1};
        totals.entrySet().stream().sorted(java.util.Map.Entry.<String, Sales>comparingByValue(
                        java.util.Comparator.comparingLong((Sales sales) -> sales.quantity).reversed()
                                .thenComparing((Sales sales) -> sales.revenue, java.util.Comparator.reverseOrder()))
                .thenComparing(java.util.Map.Entry.comparingByKey()))
                .forEach(entry -> System.out.printf("%-5d %-32s %14d %18.0f%n", rank[0]++, entry.getKey(),
                        entry.getValue().quantity, entry.getValue().revenue));
        if (totals.isEmpty()) System.out.println("Chưa có sách bán ra trong khoảng thời gian này.");
    }

    private String supplierAtSaleDate(String bookId, LocalDate saleDate) {
        if (state == null) return "Chưa ghi nhận";
        LocalDate latestDate = null;
        String supplier = defaultSupplierByBook().getOrDefault(bookId, "Chưa ghi nhận");
        for (String row : state.getStockEvents()) {
            String[] fields = row.split("\\|", -1);
            if (fields.length != 8 || !fields[0].equals("RECEIPT") || !fields[2].equalsIgnoreCase(bookId)) continue;
            LocalDate receiptDate = LocalDate.parse(fields[1]);
            if (!receiptDate.isAfter(saleDate) && (latestDate == null || !receiptDate.isBefore(latestDate))) {
                latestDate = receiptDate;
                supplier = fields[6];
            }
        }
        return supplier;
    }

    private java.util.Map<String, String> defaultSupplierByBook() {
        var result = new LinkedHashMap<String, String>();
        if (state == null) return result;
        for (String row : state.getStockEvents()) {
            String[] fields = row.split("\\|", -1);
            if (fields.length == 4 && fields[0].equals("SOURCE")) result.put(fields[1], fields[3]);
        }
        return result;
    }
    public Invoice[] filter(java.time.LocalDate from, java.time.LocalDate to) {
        AccountList.require(AccountList.Permission.VIEW_REPORTS);
        if (from == null || to == null || from.isAfter(to)) throw new IllegalArgumentException("Khoảng ngày không hợp lệ.");
        return state.getInvoices().getAll().stream().filter(Invoice::isCompleted)
                .filter(i -> !i.getPaymentDate().isBefore(from) && !i.getPaymentDate().isAfter(to)).toArray(Invoice[]::new);
    }
    public double getRevenue(java.time.LocalDate from, java.time.LocalDate to) {
        AccountList.require(AccountList.Permission.VIEW_REPORTS);
        return java.util.Arrays.stream(filter(from, to)).mapToDouble(Invoice::getTotal).sum();
    }
    public java.util.Map<String, Double> revenueByCustomer(java.time.LocalDate from, java.time.LocalDate to) {
        AccountList.require(AccountList.Permission.VIEW_REPORTS);
        java.util.Map<String, Double> result = new LinkedHashMap<>();
        for (Invoice i : filter(from, to)) result.merge(i.getCustomer().getId(), i.getTotal(), Double::sum);
        return result;
    }
    public double getRevenue() {
        AccountList.require(AccountList.Permission.VIEW_REPORTS);
        return state.getInvoices().getAll().stream().filter(Invoice::isCompleted).mapToDouble(Invoice::getTotal).sum();
    }

    public long getTotalQuantitySold() {
        AccountList.require(AccountList.Permission.VIEW_REPORTS);
        return state.getInvoices().getAll().stream()
                .filter(Invoice::isCompleted)
                .flatMap(invoice -> invoice.getDetails().stream())
                .mapToLong(InvoiceDetail::getQuantity).sum();
    }

    public Map<String, Long> getQuantitySoldByBook() {
        AccountList.require(AccountList.Permission.VIEW_REPORTS);
        Map<String, Long> result = new LinkedHashMap<>();
        for (Invoice invoice : state.getInvoices().getAll()) {
            if (!invoice.isCompleted()) continue;
            for (InvoiceDetail detail : invoice.getDetails()) {
                result.merge(detail.getBookId(), (long) detail.getQuantity(), Long::sum);
            }
        }
        return result;
    }
}
