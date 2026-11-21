package business;

import model.Account;

public final class SessionManager {
    private Account currentAccount;
    public void login(Account account) {
        if (account == null || !account.isActive()) throw new IllegalArgumentException("Tài khoản không hoạt động.");
        currentAccount = account;
    }
    public Account currentAccount() { return currentAccount; }
    public boolean isAuthenticated() { return currentAccount != null && currentAccount.isActive(); }
    public void logout() { currentAccount = null; }
}
