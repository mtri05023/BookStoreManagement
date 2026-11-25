package bookstore;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class AccountList {
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
        AccountList checked = new AccountList();
        for (Account value : values) checked.add(value);
        accounts = checked.accounts; size = checked.size;
    }
    public AccountList() { }
    private StoreFile file;
    private EmployeeList employees;
    void configure(StoreFile file, EmployeeList employees) {
        this.file = file;
        this.employees = employees;
    }
    public AccountList repository() {
        AccountList.require(AccountList.Permission.MANAGE_ACCOUNTS);
        return this;
    }
    /** Remove a linked employee account and roll it back if the employee change fails. */
    public void deleteEmployeeWithAccount(String employeeId, Runnable deleteEmployee) throws IOException {
        AccountList.require(AccountList.Permission.MANAGE_EMPLOYEES);
        var linked = this.getAll().stream()
                .filter(a -> a.getEmployeeId().equalsIgnoreCase(employeeId)).toList();
        if (linked.stream().anyMatch(a -> a.getRole() == Account.Role.ADMIN))
            throw new IllegalStateException("Không thể xóa nhân viên đang liên kết tài khoản Admin.");
        if (!linked.isEmpty()) {
            for (Account account : linked) this.deleteById(account.getId());
            try { file.save(this); }
            catch (IOException | RuntimeException error) {
                for (Account account : linked) this.add(account);
                throw error;
            }
        }
        try { deleteEmployee.run(); }
        catch (RuntimeException error) {
            if (!linked.isEmpty()) {
                for (Account account : linked) this.add(account);
                try { file.save(this); }
                catch (IOException restoreError) { error.addSuppressed(restoreError); }
            }
            throw error;
        }
    }
    public void bootstrapAdmin(String username, char[] password) throws IOException {
        bootstrapAdmin(username, password, "");
    }
    public void bootstrapAdmin(String username, char[] password, String managerEmployeeId) throws IOException {
        if (!this.getAll().isEmpty()) throw new IllegalStateException("Tài khoản đã được khởi tạo.");
        Account admin = new Account("A001", username, Account.hash(password), Account.Role.ADMIN,
                managerEmployeeId, true, false);
        this.add(admin);
        try { file.save(this); } catch (IOException | RuntimeException e) { this.deleteById(admin.getId()); throw e; }
    }
    public Account createEmployee(String username, String employeeId, Employee employee, char[] password) throws IOException {
        AccountList.require(AccountList.Permission.MANAGE_ACCOUNTS);
        if (employee == null || !employee.getId().equalsIgnoreCase(employeeId)) throw new IllegalArgumentException("Nhân viên liên kết không hợp lệ.");
        if (this.getAll().stream().anyMatch(a -> a.getEmployeeId().equalsIgnoreCase(employeeId)))
            throw new IllegalArgumentException("Nhân viên đã có tài khoản.");
        String id = String.format(Locale.ROOT, "A%03d", this.getAll().size() + 1);
        while (this.findById(id) != null) id = String.format(Locale.ROOT, "A%03d", Integer.parseInt(id.substring(1)) + 1);
        Account account = new Account(id, username, Account.hash(password), Account.Role.EMPLOYEE,
                employeeId, true, false);
        this.add(account);
        try { file.save(this); } catch (IOException | RuntimeException e) { this.deleteById(id); throw e; }
        return account;
    }
    public void setActive(String id, boolean active) throws IOException {
        AccountList.require(AccountList.Permission.MANAGE_ACCOUNTS);
        Account account = requireAccount(id);
        if (!active && account == AccountList.currentAccount())
            throw new IllegalStateException("Hãy đăng xuất trước khi Admin tự khóa tài khoản của mình.");
        if (!active && account.getRole() == Account.Role.ADMIN && this.getAll().stream()
                .filter(a -> a.getRole() == Account.Role.ADMIN && a.isActive()).count() <= 1)
            throw new IllegalStateException("Không thể khóa Admin đang hoạt động cuối cùng.");
        boolean before = account.isActive(); account.setActive(active);
        try { file.save(this); } catch (IOException | RuntimeException e) { account.setActive(before); throw e; }
    }
    public void resetEmployeePassword(String id, char[] password) throws IOException {
        AccountList.require(AccountList.Permission.MANAGE_ACCOUNTS);
        Account account = requireAccount(id);
        if (account.getRole() != Account.Role.EMPLOYEE) throw new IllegalArgumentException("Chỉ Admin mới được đặt lại mật khẩu Admin.");
        rotateWithPassword(account, password, false);
    }
    public void updateAccount(String id, String username, Account.Role role, String employeeId, Employee linkedEmployee) throws IOException {
        AccountList.require(AccountList.Permission.MANAGE_ACCOUNTS);
        Account current = requireAccount(id);
        if (current == AccountList.currentAccount()) throw new IllegalStateException("Không thể sửa vai trò/tên đăng nhập của phiên đang dùng.");
        if (role == Account.Role.EMPLOYEE && (linkedEmployee == null || !linkedEmployee.getId().equalsIgnoreCase(employeeId)))
            throw new IllegalArgumentException("Cần chọn nhân viên có trong danh sách.");
        if (employeeId != null && !employeeId.isBlank() && this.getAll().stream().anyMatch(a -> !a.getId().equalsIgnoreCase(id)
                && a.getEmployeeId().equalsIgnoreCase(employeeId))) throw new IllegalArgumentException("Nhân viên đã liên kết tài khoản khác.");
        if (current.getRole() == Account.Role.ADMIN && role != Account.Role.ADMIN && current.isActive()
                && this.getAll().stream().filter(a -> a.getRole() == Account.Role.ADMIN && a.isActive()).count() <= 1)
            throw new IllegalStateException("Không thể hạ quyền Admin đang hoạt động cuối cùng.");
        Account replacement = new Account(current.getId(), username, current.getPasswordHash(), role,
                role == Account.Role.EMPLOYEE ? employeeId : current.getEmployeeId(), current.isActive(), current.mustChangePassword());
        this.replace(replacement);
        try { file.save(this); } catch (IOException | RuntimeException e) { this.replace(current); throw e; }
    }
    public void updateOwnPassword(char[] oldPassword, char[] newPassword, AccountList session) throws IOException {
        AccountList.require(AccountList.Permission.CHANGE_PASSWORD);
        Account account = session.loggedInAccount();
        if (account == null || account != AccountList.currentAccount()
                || !Account.verify(oldPassword, account.getPasswordHash())) throw new IllegalArgumentException("Mật khẩu hiện tại không đúng.");
        rotateWithPassword(account, newPassword, false);
    }
    private void rotateWithPassword(Account account, char[] password, boolean force) throws IOException {
        String oldHash = account.getPasswordHash(); boolean oldForce = account.mustChangePassword();
        account.setPasswordHash(Account.hash(password), force);
        try { file.save(this); } catch (IOException | RuntimeException e) { account.setPasswordHash(oldHash, oldForce); throw e; }
    }
    private Account requireAccount(String id) {
        Account account = this.findById(id);
        if (account == null) throw new IllegalArgumentException("Không tìm thấy tài khoản.");
        return account;
    }
    private Account currentAccount;
    public void login(Account account) {
        if (account == null || !account.isActive()) throw new IllegalArgumentException("Tài khoản không hoạt động.");
        currentAccount = account;
    }
    public Account loggedInAccount() { return currentAccount; }
    public boolean isAuthenticated() { return currentAccount != null && currentAccount.isActive(); }
    public void logout() { currentAccount = null; }
    public enum Permission { READ_BOOKS, MANAGE_BOOKS, READ_CUSTOMERS, MANAGE_CUSTOMERS, DELETE_CUSTOMERS,
        MANAGE_EMPLOYEES, CREATE_INVOICE, VIEW_OWN_INVOICES, VIEW_ALL_INVOICES, VIEW_INVENTORY,
        MANAGE_INVENTORY, MANAGE_SUPPLIERS, VIEW_REPORTS, MANAGE_ACCOUNTS, CHANGE_PASSWORD }
    private static AccountList session;
    public static void enable(AccountList manager) { session = java.util.Objects.requireNonNull(manager); }
    public static Account currentAccount() { return session == null ? null : session.loggedInAccount(); }
    public static boolean isAdmin() { return currentAccount() != null && currentAccount().getRole() == Account.Role.ADMIN; }
    public static void require(Permission permission) {
        Account account = currentAccount();
        if (account == null || !account.isActive()) throw new SecurityException("Vui lòng đăng nhập để sử dụng chức năng.");
        boolean allowed = switch (permission) {
            case READ_BOOKS, READ_CUSTOMERS, VIEW_INVENTORY, CREATE_INVOICE, VIEW_OWN_INVOICES, CHANGE_PASSWORD -> true;
            case MANAGE_CUSTOMERS -> true;
            case MANAGE_BOOKS, DELETE_CUSTOMERS, MANAGE_EMPLOYEES, VIEW_ALL_INVOICES,
                    MANAGE_INVENTORY, MANAGE_SUPPLIERS, VIEW_REPORTS, MANAGE_ACCOUNTS -> account.getRole() == Account.Role.ADMIN;
        };
        if (!allowed) throw new SecurityException("Tài khoản " + account.getRole() + " không có quyền thực hiện thao tác này.");
    }
    public Account login(String username, char[] password) {
        Account account = findByUsername(username == null ? "" : username.trim().toLowerCase(Locale.ROOT));
        if (account == null || !account.isActive() || !Account.verify(password, account.getPasswordHash())
                || (account.getRole() == Account.Role.EMPLOYEE && employees != null
                    && employees.findById(account.getEmployeeId()) == null))
            throw new IllegalArgumentException("Tên đăng nhập hoặc mật khẩu không đúng, hoặc tài khoản đã bị khóa.");
        login(account);
        return account;
    }
}
