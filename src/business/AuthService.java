package business;

import data.AccountRepository;
import model.Account;
import java.util.Locale;

public final class AuthService {
    private final AccountRepository accounts;
    private final SessionManager session;
    public AuthService(AccountRepository accounts, SessionManager session) { this.accounts = accounts; this.session = session; }
    public Account login(String username, char[] password) {
        Account account = accounts.findByUsername(username == null ? "" : username.trim().toLowerCase(Locale.ROOT));
        if (account == null || !account.isActive() || !PasswordUtil.verify(password, account.getPasswordHash()))
            throw new IllegalArgumentException("Tên đăng nhập hoặc mật khẩu không đúng, hoặc tài khoản đã bị khóa.");
        session.login(account);
        return account;
    }
    public void logout() { session.logout(); }
    public boolean mustChangePassword() {
        return session.currentAccount() != null && session.currentAccount().mustChangePassword();
    }
}
