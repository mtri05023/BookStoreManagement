package data;

import model.Account;
import model.Role;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public final class AccountTextCodec {
    private AccountTextCodec() { }
    public static List<String> encode(AccountRepository repository) {
        List<String> lines = new ArrayList<>();
        lines.add("ACCOUNTS|1");
        for (Account a : repository.getAll()) lines.add(String.join("|", "ACCOUNT", a.getId(), a.getUsername(),
                a.getPasswordHash(), a.getRole().name(), a.getEmployeeId(), Boolean.toString(a.isActive()),
                Boolean.toString(a.mustChangePassword())));
        lines.add("END|" + repository.getAll().size());
        return lines;
    }
    public static AccountRepository decode(List<String> lines) throws IOException {
        if (lines.size() < 2 || !"ACCOUNTS|1".equals(lines.get(0))) throw new IOException("File tài khoản sai header hoặc phiên bản.");
        AccountRepository result = new AccountRepository();
        try {
            for (int i = 1; i < lines.size() - 1; i++) {
                String[] f = lines.get(i).split("\\|", -1);
                if ((f.length != 8 && f.length != 9) || !f[0].equals("ACCOUNT")) throw new IllegalArgumentException("Sai cấu trúc tài khoản.");
                if (!(f[6].equals("true") || f[6].equals("false")) || !(f[7].equals("true") || f[7].equals("false")))
                    throw new IllegalArgumentException("Trạng thái tài khoản không hợp lệ.");
                if (!validHash(f[3])) throw new IllegalArgumentException("Mã băm mật khẩu bị lỗi.");
                result.add(new Account(f[1], f[2], f[3], Role.valueOf(f[4]), f[5], Boolean.parseBoolean(f[6]),
                        Boolean.parseBoolean(f[7])));
            }
            String[] end = lines.get(lines.size() - 1).split("\\|", -1);
            if (end.length != 2 || !end[0].equals("END") || Integer.parseInt(end[1]) != result.getAll().size())
                throw new IllegalArgumentException("END hoặc số tài khoản không khớp.");
        } catch (RuntimeException e) { throw new IOException("File tài khoản không hợp lệ: " + e.getMessage(), e); }
        if (result.getAll().stream().noneMatch(a -> a.getRole() == Role.ADMIN && a.isActive()))
            throw new IOException("File phải có ít nhất một Admin đang hoạt động.");
        return result;
    }
    private static boolean validHash(String value) {
        try {
            String[] parts = value.split("\\$", -1);
            if (parts.length != 3) return false;
            int rounds = Integer.parseInt(parts[0]);
            byte[] salt = java.util.Base64.getUrlDecoder().decode(parts[1]);
            byte[] hash = java.util.Base64.getUrlDecoder().decode(parts[2]);
            return rounds >= 100_000 && rounds <= 2_000_000 && salt.length >= 16 && hash.length == 32;
        } catch (RuntimeException e) { return false; }
    }
}
