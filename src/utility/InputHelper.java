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
                return LocalDate.parse(read(message));
            } catch (DateTimeParseException e) {
                System.out.println("Vui lòng nhập ngày yyyy-MM-dd hợp lệ.");
            }
        }
    }
}
