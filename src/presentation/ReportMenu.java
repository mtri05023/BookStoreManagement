package presentation;

import business.ReportService;
import data.StoreState;
import model.Invoice;
import utility.InputHelper;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.HashSet;
import java.math.BigDecimal;

public final class ReportMenu {
    private final ReportService reports;
    private final InputHelper input;
    private final StoreState state;
    public ReportMenu(ReportService reports, InputHelper input) { this(reports, input, null); }
    public ReportMenu(ReportService reports, InputHelper input, StoreState state) {
        this.reports = reports; this.input = input; this.state = state;
    }
    public void show() {
        while (true) {
            ConsoleView.options("THỐNG KÊ DOANH THU", "Doanh thu hôm nay", "Doanh thu theo ngày", "Doanh thu theo tháng",
                    "Doanh thu theo năm", "Doanh thu theo khoảng thời gian", "Số lượng sách bán ra", "Doanh thu từng sách",
                    "Top sách bán chạy", "Thống kê hóa đơn", "Doanh thu theo khách hàng", "Xếp hạng nhà cung cấp theo sách bán");
            int choice = input.readInt("Chọn: ");
            try {
                if (choice == 0) return;
                if (choice < 1 || choice > 11) { System.out.println("Lựa chọn không hợp lệ."); continue; }
                LocalDate[] range;
                if (choice <= 5) range = ConsoleView.range(input, choice);
                else {
                    int mode = input.readInt("Thời gian: 1. Hôm nay, 2. Ngày, 3. Tháng, 4. Năm, 5. Khoảng ngày, 6. Toàn bộ: ");
                    range = mode == 6 ? new LocalDate[] { LocalDate.MIN, LocalDate.MAX } : ConsoleView.range(input, mode);
                }
                summary(range[0], range[1], choice);
            } catch (IllegalArgumentException | IllegalStateException | java.time.DateTimeException e) { System.out.println(e.getMessage()); }
        }
    }
    public void summary(LocalDate from, LocalDate to, int mode) {
        Invoice[] invoices = reports.filter(from, to);
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
        System.out.println("\nTHỐNG KÊ DOANH THU: " + (from.equals(LocalDate.MIN) ? "Toàn bộ thời gian" : from.format(ConsoleView.DATE) + " - " + to.format(ConsoleView.DATE)));
        System.out.printf("Tổng doanh thu: %,.0f VNĐ%nSố hóa đơn: %d%nSố lượng sách bán ra: %d%nSố khách hàng: %d%n",
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
            for (Invoice i : invoices) System.out.printf("%s | %s | %s | %,.0f%n", i.getId(), i.getPaymentDate().format(ConsoleView.DATE), i.getCustomer().getId(), i.getTotal());
        }
        if (mode == 10) {
            System.out.println("DOANH THU THEO KHÁCH HÀNG: Mã | Tổng tiền (VNĐ)");
            reports.revenueByCustomer(from, to).entrySet().stream()
                    .sorted(java.util.Map.Entry.<String, Double>comparingByValue().reversed())
                    .forEach(e -> System.out.printf("%s | %,.0f%n", e.getKey(), e.getValue()));
            if (reports.revenueByCustomer(from, to).isEmpty()) System.out.println("Không có khách hàng phát sinh doanh thu trong thời gian này.");
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
        class Sales { long quantity; BigDecimal revenue = BigDecimal.ZERO; }
        var totals = new LinkedHashMap<String, Sales>();
        for (Invoice invoice : invoices) {
            for (var detail : invoice.getDetails()) {
                String supplier = supplierAtSaleDate(detail.getBookId(), invoice.getPaymentDate());
                Sales sales = totals.computeIfAbsent(supplier, ignored -> new Sales());
                sales.quantity += detail.getQuantity();
                sales.revenue = sales.revenue.add(BigDecimal.valueOf(detail.getAmount()));
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
}
