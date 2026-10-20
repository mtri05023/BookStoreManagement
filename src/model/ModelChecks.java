package model;

import java.util.Locale;

/** Shared model invariants; independent of menus, services and storage. */
final class ModelChecks {
    private ModelChecks() { }

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
}
