package utility;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
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
}
