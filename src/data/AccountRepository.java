package data;

import model.Account;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/** Array-backed account storage, following the project's repository convention. */
public final class AccountRepository {
    private Account[] accounts = new Account[4];
    private int size;
    public void add(Account account) {
        if (account == null) throw new IllegalArgumentException("Tài khoản không được null.");
        if (findById(account.getId()) != null) throw new IllegalArgumentException("Mã tài khoản đã tồn tại.");
        if (findByUsername(account.getUsername()) != null) throw new IllegalArgumentException("Tên đăng nhập đã tồn tại.");
        if (!account.getEmployeeId().isBlank() && isEmployeeLinked(account.getEmployeeId(), null))
            throw new IllegalArgumentException("Nhân viên đã được liên kết với tài khoản khác.");
        if (size == accounts.length) accounts = Arrays.copyOf(accounts, accounts.length * 2);
        accounts[size++] = account;
    }
    public Account findById(String id) {
        if (id == null) return null;
        for (int i = 0; i < size; i++) if (accounts[i].getId().equalsIgnoreCase(id.trim())) return accounts[i];
        return null;
    }
    public Account findByUsername(String username) {
        if (username == null) return null;
        String key = username.trim().toLowerCase(Locale.ROOT);
        for (int i = 0; i < size; i++) if (accounts[i].getUsername().equals(key)) return accounts[i];
        return null;
    }
    public List<Account> getAll() { return List.copyOf(Arrays.asList(Arrays.copyOf(accounts, size))); }
    public boolean deleteById(String id) {
        Account value = findById(id);
        if (value == null) return false;
        int index = 0;
        while (accounts[index] != value) index++;
        System.arraycopy(accounts, index + 1, accounts, index, size - index - 1);
        accounts[--size] = null;
        return true;
    }
    public void replace(Account replacement) {
        Account current = findById(replacement.getId());
        if (current == null) throw new IllegalArgumentException("Không tìm thấy tài khoản cần cập nhật.");
        Account duplicate = findByUsername(replacement.getUsername());
        if (duplicate != null && duplicate != current) throw new IllegalArgumentException("Tên đăng nhập đã tồn tại.");
        if (!replacement.getEmployeeId().isBlank() && isEmployeeLinked(replacement.getEmployeeId(), current))
            throw new IllegalArgumentException("Nhân viên đã được liên kết với tài khoản khác.");
        for (int i = 0; i < size; i++) if (accounts[i] == current) { accounts[i] = replacement; return; }
    }
    private boolean isEmployeeLinked(String employeeId, Account ignored) {
        for (int i = 0; i < size; i++)
            if (accounts[i] != ignored && accounts[i].getEmployeeId().equalsIgnoreCase(employeeId)) return true;
        return false;
    }
    public void replaceAll(Account[] values) {
        AccountRepository checked = new AccountRepository();
        for (Account value : values) checked.add(value);
        accounts = checked.accounts; size = checked.size;
    }
}
