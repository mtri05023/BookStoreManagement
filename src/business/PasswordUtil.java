package business;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

/** PBKDF2-HMAC-SHA256 password hashing with a per-value random salt. */
public final class PasswordUtil {
    private static final int ITERATIONS = 210_000;
    private static final int KEY_BITS = 256;
    private static final SecureRandom RANDOM = new SecureRandom();
    private PasswordUtil() { }
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
}
