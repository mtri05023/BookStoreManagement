package presentation;

import model.Book;
import model.Invoice;
import utility.InputHelper;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;

/** Shared console formatting and strict date-range selection. */
public final class ConsoleView {
    public static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private ConsoleView() { }
    public static void options(String title, String... labels) {
        optionsWithZero(title, "Quay lại", labels);
    }
    public static void optionsWithZero(String title, String zero, String... labels) {
        System.out.println("\n=== " + title + " ===");
        for (int i = 0; i < labels.length; i++) System.out.println((i + 1) + ". " + labels[i]);
        System.out.println("0. " + zero);
    }
    public static void books(Book[] books) {
        System.out.printf("%-10s %-28s %-24s %12s %12s %8s%n", "Mã", "Tên sách", "Tác giả", "Giá nhập", "Giá bán", "Tồn kho");
        for (Book b : books) System.out.printf("%-10s %-28s %-24s %12s %12.0f %8d%n",
                b.getId(), b.getTitle(), b.getAuthor(), b.getPurchasePrice() == 0 ? "Chưa có" : String.format("%.0f", b.getPurchasePrice()), b.getPrice(), b.getQuantity());
        if (books.length == 0) System.out.println("Không tìm thấy sách phù hợp.");
        else System.out.println("Số sách hiển thị: " + books.length);
    }
    public static void invoice(Invoice i) {
        System.out.println("\nHÓA ĐƠN " + i.getId() + " | " + i.getCreatedDate().format(DATE));
        System.out.println("Trạng thái: " + switch (i.getStatus()) {
            case DRAFT -> "Đang lập";
            case PAID -> "Đã thanh toán";
            case CANCELLED -> "Đã hủy";
        });
        System.out.println("Ngày giờ lập: " + i.getCreatedAt().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")));
        if (i.isCompleted()) System.out.println("Thời gian thanh toán: " + (i.getPaidAt() == null
                ? "Không có trong dữ liệu cũ" : i.getPaidAt().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"))));
        System.out.println("Khách hàng: " + i.getCustomer().getId() + " - " + i.getCustomerName() + " | SĐT: " + i.getCustomerPhone());
        System.out.println("Nhân viên: " + i.getEmployee().getId() + " - " + i.getEmployeeName() + " | Chức vụ: " + i.getEmployeePosition());
        System.out.printf("%-10s %-28s %8s %12s %14s%n", "Mã sách", "Tên sách", "Số lượng", "Đơn giá", "Thành tiền");
        for (var d : i.getDetails()) System.out.printf("%-10s %-28s %8d %12.0f %14.0f%n",
                d.getBookId(), d.getBookTitle(), d.getQuantity(), d.getPrice(), d.getAmount());
        System.out.printf("Tổng tiền: %,.0f VNĐ%nGiảm giá: 0 VNĐ%n%s: %,.0f VNĐ%n", i.getTotal(),
                i.isCompleted() ? "Tổng tiền đã thanh toán" : "Số tiền phải thanh toán", i.getTotal());
    }
    public static LocalDate[] range(InputHelper in, int mode) {
        return switch (mode) {
            case 1 -> new LocalDate[] { LocalDate.now(), LocalDate.now() };
            case 2 -> { LocalDate d = in.readDate("Ngày (dd/MM/yyyy): "); yield new LocalDate[] { d, d }; }
            case 3 -> {
                int month = in.readInt("Tháng (1..12): ");
                int year = year(in);
                YearMonth ym = YearMonth.of(year, month);
                yield new LocalDate[] { ym.atDay(1), ym.atEndOfMonth() };
            }
            case 4 -> { int y = year(in); yield new LocalDate[] { LocalDate.of(y, 1, 1), LocalDate.of(y, 12, 31) }; }
            case 5 -> {
                LocalDate a = in.readDate("Từ ngày (dd/MM/yyyy): ");
                LocalDate b = in.readDate("Đến ngày (dd/MM/yyyy): ");
                if (a.isAfter(b)) throw new IllegalArgumentException("Ngày bắt đầu phải không sau ngày kết thúc.");
                yield new LocalDate[] { a, b };
            }
            default -> throw new IllegalArgumentException("Bộ lọc không hợp lệ.");
        };
    }
    private static int year(InputHelper in) {
        int year = in.readInt("Năm (1..9999): ");
        if (year < 1 || year > 9999) throw new IllegalArgumentException("Năm phải trong 1..9999.");
        return year;
    }
}
