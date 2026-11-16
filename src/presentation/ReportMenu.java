package presentation;

import business.ReportService;
import model.Invoice;
import utility.InputHelper;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.HashSet;
import java.math.BigDecimal;

public final class ReportMenu {
    private final ReportService reports;
    private final InputHelper input;
    public ReportMenu(ReportService reports, InputHelper input) { this.reports = reports; this.input = input; }
    public void show() {
        while (true) {
            ConsoleView.options("THỐNG KÊ DOANH THU", "Doanh thu hôm nay", "Doanh thu theo ngày", "Doanh thu theo tháng",
                    "Doanh thu theo năm", "Doanh thu theo khoảng thời gian", "Số lượng sách bán ra", "Doanh thu từng sách",
                    "Top sách bán chạy", "Thống kê hóa đơn", "Doanh thu theo khách hàng");
            int choice = input.readInt("Chọn: ");
            try {
                if (choice == 0) return;
                if (choice < 1 || choice > 10) { System.out.println("Lựa chọn không hợp lệ."); continue; }
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
        System.out.println("SÁCH BÁN CHẠY VÀ DOANH THU TỪNG SÁCH (tên tại lần bán cuối trong kết quả)");
        System.out.printf("%-10s %-28s %12s %16s%n", "Mã sách", "Tên sách", "Số lượng", "Doanh thu");
        quantities.entrySet().stream().sorted(java.util.Map.Entry.<String, Long>comparingByValue().reversed()
                .thenComparing(java.util.Map.Entry.comparingByKey())).forEach(e ->
                System.out.printf("%-10s %-28s %12d %16.0f%n", e.getKey(), titles.get(e.getKey()), e.getValue(), revenues.get(e.getKey())));
        if (mode == 9) {
            System.out.println("Mã hóa đơn | Ngày thanh toán | Mã khách hàng | Thực thanh toán");
            for (Invoice i : invoices) System.out.printf("%s | %s | %s | %,.0f%n", i.getId(), i.getPaymentDate().format(ConsoleView.DATE), i.getCustomer().getId(), i.getTotal());
        }
        if (mode == 10) {
            System.out.println("DOANH THU THEO KHÁCH HÀNG: Mã | Tổng tiền (VNĐ)");
            reports.revenueByCustomer(from, to).entrySet().stream()
                    .sorted(java.util.Map.Entry.<String, Double>comparingByValue().reversed())
                    .forEach(e -> System.out.printf("%s | %,.0f%n", e.getKey(), e.getValue()));
        }
    }
}
