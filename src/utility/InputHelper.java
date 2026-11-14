package utility;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Objects;
import java.util.Scanner;

public class InputHelper {
    private final Scanner scanner;

    public InputHelper(Scanner scanner) {
        this.scanner = Objects.requireNonNull(scanner, "Scanner must not be null.");
    }

    public String read(String message) {
        System.out.print(message);
        return scanner.nextLine().trim();
    }

    public String readRequiredString(String message) {
        return readRequiredString(message, "Vui lòng không để trống.");
    }

    public String readRequiredString(String message, String errorMessage) {
        while (true) {
            String value = read(message);
            if (!value.isBlank()) return value;
            System.out.println(errorMessage);
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

    public int readInt(String message, int min, int max) {
        if (min > max) {
            throw new IllegalArgumentException("min (" + min + ") phải nhỏ hơn hoặc bằng max (" + max + ").");
        }
        while (true) {
            int value = readInt(message);
            if (value >= min && value <= max) {
                return value;
            }
            System.out.println("Vui lòng nhập số trong khoảng [" + min + ", " + max + "].");
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
                return LocalDate.parse(read(message));
            } catch (DateTimeParseException e) {
                System.out.println("Vui lòng nhập ngày yyyy-MM-dd hợp lệ.");
            }
        }
    }
}
