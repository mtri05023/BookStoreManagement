package bookstore;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Locale;
import java.util.Scanner;

public class InputHelper {
    private final Scanner scanner;

    public InputHelper(Scanner scanner) {
        this.scanner = scanner;
    }

    public String read(String message) {
        System.out.print(message);
        return scanner.nextLine().trim();
    }
    public String readPhone(String message) {
        while (true) {
            String phone = read(message);
            if (phone.matches("0[0-9]{9}")) return phone;
            System.out.println("Số điện thoại phải có đúng 10 chữ số và bắt đầu bằng 0. Vui lòng nhập lại.");
        }
    }
    public String readPhoneKeyword(String message) {
        while (true) {
            String keyword = read(message);
            if (keyword.matches("[0-9]+")) return keyword;
            System.out.println("Từ khóa số điện thoại chỉ được chứa chữ số. Vui lòng nhập lại.");
        }
    }

    public char[] readPassword(String message) {
        java.io.Console console = System.console();
        if (console != null) {
            System.out.print(message);
            System.out.flush();
            char[] value = console.readPassword();
            if (value == null) throw new java.util.NoSuchElementException("Đã kết thúc nhập liệu.");
            return value;
        }
        System.out.print(message + " (ký tự có thể hiển thị khi chạy trong IDE): ");
        if (!scanner.hasNextLine()) throw new java.util.NoSuchElementException("Đã kết thúc nhập liệu.");
        return scanner.nextLine().toCharArray();
    }

    public static String normalizeCode(String prefix, String value) {
        if (!java.util.Set.of("S", "NV", "KH", "HD", "NCC").contains(prefix))
            throw new IllegalArgumentException("Tiền tố mã không hợp lệ.");
        String digits = value.trim().toUpperCase(java.util.Locale.ROOT);
        if (digits.startsWith(prefix)) digits = digits.substring(prefix.length());
        if (!digits.matches("[0-9]{1,9}") || Integer.parseInt(digits) == 0)
            throw new IllegalArgumentException("Chỉ nhập số nguyên dương (tối đa 9 chữ số), ví dụ 1 thành " + prefix + "001.");
        return prefix + String.format(java.util.Locale.ROOT, "%03d", Integer.parseInt(digits));
    }

    public String readNewCode(String prefix, String message, String... commands) {
        return readCode(prefix, message, false, commands);
    }

    public String readExistingCode(String prefix, String message, String... commands) {
        return readCode(prefix, message, true, commands);
    }

    private String readCode(String prefix, String message, boolean legacy, String... commands) {
        while (true) {
            String value = read(message + prefix);
            for (String command : commands) if (value.equalsIgnoreCase(command)) return command;
            // Explicit escape keeps historical IDs such as "01" accessible without renaming records.
            if (legacy && value.startsWith("=") && value.length() > 1) return value.substring(1).trim();
            try { return normalizeCode(prefix, value); }
            catch (IllegalArgumentException error) { System.out.println("Sai mã. " + error.getMessage() + " Vui lòng nhập lại."); }
        }
    }

    public int readInt(String message) {
        while (true) {
            try {
                return Integer.parseInt(read(message));
            } catch (NumberFormatException e) {
                System.out.println("Vui lòng nhập số nguyên hợp lệ.");
            }
        }
    }

    public double readDouble(String message) {
        while (true) {
            try {
                double value = Double.parseDouble(read(message));
                if (Double.isFinite(value)) return value;
            } catch (NumberFormatException e) {
                // Retry invalid numeric input; EOF propagates to Main.
            }
            System.out.println("Vui lòng nhập số hữu hạn hợp lệ.");
        }
    }

    public LocalDate readDate(String message) {
        while (true) {
            try {
                String text = read(message);
                return text.contains("/") ? LocalDate.parse(text,
                        java.time.format.DateTimeFormatter.ofPattern("dd/MM/uuuu")
                                .withResolverStyle(java.time.format.ResolverStyle.STRICT)) : LocalDate.parse(text);
            } catch (DateTimeParseException e) {
                System.out.println("Vui lòng nhập ngày dd/MM/yyyy hợp lệ.");
            }
        }
    }
    static String text(String value, String field) {
        if (value == null || value.isBlank() || value.contains("|")
                || value.contains("\r") || value.contains("\n")) {
            throw new IllegalArgumentException(field + " không hợp lệ.");
        }
        return value.trim();
    }

    static String id(String value) {
        return text(value, "Mã").toUpperCase(Locale.ROOT);
    }

    static double price(double value) {
        if (!Double.isFinite(value) || value <= 0) {
            throw new IllegalArgumentException("Giá phải là số hữu hạn lớn hơn 0.");
        }
        return value;
    }
    public static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    /** Pinned so grouped money totals read the same regardless of the running machine's default locale. */
    public static final Locale CURRENCY_LOCALE = Locale.forLanguageTag("vi-VN");
    public static void options(String title, String... labels) {
        optionsWithZero(title, "Quay lại", labels);
    }
    public static void optionsWithZero(String title, String zero, String... labels) {
        System.out.println("\n=== " + title + " ===");
        for (int i = 0; i < labels.length; i++) System.out.println((i + 1) + ". " + labels[i]);
        System.out.println("0. " + zero);
    }
    public static void books(Book[] books) {
        String header = String.format("%-10s %-28s %-24s %12s %12s %8s", "Mã", "Tên sách", "Tác giả", "Giá nhập", "Giá bán", "Tồn kho");
        String border = "-".repeat(header.length());
        System.out.println(border);
        System.out.println(header);
        System.out.println(border);
        for (Book b : books) System.out.printf("%-10s %-28s %-24s %12s %12.0f %8d%n",
                b.getId(), b.getTitle(), b.getAuthor(), b.getPurchasePrice() == 0 ? "Chưa có" : String.format("%.0f", b.getPurchasePrice()), b.getPrice(), b.getQuantity());
        System.out.println(border);
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
        System.out.printf(CURRENCY_LOCALE, "Tổng tiền: %,.0f VNĐ%nGiảm giá: 0 VNĐ%n%s: %,.0f VNĐ%n", i.getTotal(),
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
