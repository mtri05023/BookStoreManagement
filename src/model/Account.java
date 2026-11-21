package model;

import java.util.Locale;
import java.util.Objects;

/** Login identity with a salted PBKDF2 password hash. */
public final class Account {
    private final String id;
    private final String username;
    private String passwordHash;
    private final Role role;
    private final String employeeId;
    private boolean active;
    private boolean mustChangePassword;

    public Account(String id, String username, String passwordHash, Role role, String employeeId,
                   boolean active, boolean mustChangePassword) {
        this.id = required(id, "Mã tài khoản").toUpperCase(Locale.ROOT);
        this.username = required(username, "Tên đăng nhập").toLowerCase(Locale.ROOT);
        if (!this.username.matches("[a-z0-9._-]{3,32}"))
            throw new IllegalArgumentException("Tên đăng nhập phải dài 3-32 ký tự, chỉ gồm chữ, số, dấu chấm, gạch dưới hoặc gạch ngang.");
        this.passwordHash = required(passwordHash, "Mã băm mật khẩu");
        this.role = Objects.requireNonNull(role);
        this.employeeId = employeeId == null ? "" : employeeId.trim().toUpperCase(Locale.ROOT);
        if (role == Role.EMPLOYEE && this.employeeId.isBlank())
            throw new IllegalArgumentException("Tài khoản nhân viên phải liên kết mã nhân viên.");
        this.active = active;
        this.mustChangePassword = mustChangePassword;
    }

    private static String required(String value, String label) {
        if (value == null || value.isBlank() || value.contains("|") || value.contains("\n") || value.contains("\r"))
            throw new IllegalArgumentException(label + " không hợp lệ.");
        return value.trim();
    }
    public String getId() { return id; }
    public String getUsername() { return username; }
    public String getPasswordHash() { return passwordHash; }
    public Role getRole() { return role; }
    public String getEmployeeId() { return employeeId; }
    public boolean isActive() { return active; }
    public boolean mustChangePassword() { return mustChangePassword; }
    public void setPasswordHash(String value, boolean forceChange) { passwordHash = required(value, "Mã băm mật khẩu"); mustChangePassword = forceChange; }
    public void setActive(boolean value) { active = value; }
}
