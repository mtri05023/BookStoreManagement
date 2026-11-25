package bookstore;

import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.util.Locale;
import java.util.Objects;

public class Account implements Identifiable {
    private final String id;
    private final String username;
    private String passwordHash;
    private final Account.Role role;
    private final String employeeId;
    private boolean active;
    private boolean mustChangePassword;

    public Account(String id, String username, String passwordHash, Account.Role role, String employeeId,
                   boolean active, boolean mustChangePassword) {
        this.id = required(id, "Mã tài khoản").toUpperCase(Locale.ROOT);
        this.username = required(username, "Tên đăng nhập").toLowerCase(Locale.ROOT);
        if (!this.username.matches("[a-z0-9._-]{3,32}"))
            throw new IllegalArgumentException("Tên đăng nhập phải dài 3-32 ký tự, chỉ gồm chữ, số, dấu chấm, gạch dưới hoặc gạch ngang.");
        this.passwordHash = required(passwordHash, "Mã băm mật khẩu");
        this.role = Objects.requireNonNull(role);
        this.employeeId = employeeId == null ? "" : employeeId.trim().toUpperCase(Locale.ROOT);
        if (role == Account.Role.EMPLOYEE && this.employeeId.isBlank())
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
    public Account.Role getRole() { return role; }
    public String getEmployeeId() { return employeeId; }
    public boolean isActive() { return active; }
    public boolean mustChangePassword() { return mustChangePassword; }
    public void setPasswordHash(String value, boolean forceChange) { passwordHash = required(value, "Mã băm mật khẩu"); mustChangePassword = forceChange; }
    public void setActive(boolean value) { active = value; }
    private static final int ITERATIONS = 210_000;
    private static final int KEY_BITS = 256;
    private static final SecureRandom RANDOM = new SecureRandom();
    public static String hash(char[] secret) {
        if (secret == null || secret.length < 3 || secret.length > 128)
            throw new IllegalArgumentException("Mật khẩu phải dài từ 3 đến 128 ký tự.");
        return encode(secret);
    }
    private static String encode(char[] secret) {
        byte[] salt = new byte[16]; RANDOM.nextBytes(salt);
        byte[] derived = derive(secret, salt, ITERATIONS);
        return ITERATIONS + "$" + Base64.getUrlEncoder().withoutPadding().encodeToString(salt)
                + "$" + Base64.getUrlEncoder().withoutPadding().encodeToString(derived);
    }
    public static boolean verify(char[] secret, String encoded) {
        if (secret == null || encoded == null) return false;
        try {
            String[] parts = encoded.split("\\$", -1);
            if (parts.length != 3) return false;
            int rounds = Integer.parseInt(parts[0]);
            if (rounds < 100_000 || rounds > 2_000_000) return false;
            byte[] salt = Base64.getUrlDecoder().decode(parts[1]);
            byte[] expected = Base64.getUrlDecoder().decode(parts[2]);
            return MessageDigest.isEqual(expected, derive(secret, salt, rounds));
        } catch (RuntimeException e) { return false; }
    }
    private static byte[] derive(char[] secret, byte[] salt, int rounds) {
        PBEKeySpec spec = new PBEKeySpec(secret, salt, rounds, KEY_BITS);
        try { return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded(); }
        catch (java.security.GeneralSecurityException e) { throw new IllegalStateException("Không hỗ trợ PBKDF2.", e); }
        finally { spec.clearPassword(); }
    }
    public enum Role { ADMIN, EMPLOYEE }
}
