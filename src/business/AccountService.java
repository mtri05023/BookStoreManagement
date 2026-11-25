package business;

import data.AccountFile;
import data.AccountRepository;
import model.Account;
import model.Employee;
import model.Role;
import java.io.IOException;
import java.util.Locale;

public final class AccountService {
    private final AccountRepository accounts;
    private final AccountFile file;
    public AccountService(AccountRepository accounts, AccountFile file) { this.accounts = accounts; this.file = file; }
    public AccountRepository repository() {
        PermissionService.require(PermissionService.Permission.MANAGE_ACCOUNTS);
        return accounts;
    }
    /** Remove a linked employee account and roll it back if the employee change fails. */
    public void deleteEmployeeWithAccount(String employeeId, Runnable deleteEmployee) throws IOException {
        PermissionService.require(PermissionService.Permission.MANAGE_EMPLOYEES);
        var linked = accounts.getAll().stream()
                .filter(a -> a.getEmployeeId().equalsIgnoreCase(employeeId)).toList();
        if (linked.stream().anyMatch(a -> a.getRole() == Role.ADMIN))
            throw new IllegalStateException("Không thể xóa nhân viên đang liên kết tài khoản Admin.");
        if (!linked.isEmpty()) {
            for (Account account : linked) accounts.deleteById(account.getId());
            try { file.save(accounts); }
            catch (IOException | RuntimeException error) {
                for (Account account : linked) accounts.add(account);
                throw error;
            }
        }
        try { deleteEmployee.run(); }
        catch (RuntimeException error) {
            if (!linked.isEmpty()) {
                for (Account account : linked) accounts.add(account);
                try { file.save(accounts); }
                catch (IOException restoreError) { error.addSuppressed(restoreError); }
            }
            throw error;
        }
    }
    public void bootstrapAdmin(String username, char[] password) throws IOException {
        bootstrapAdmin(username, password, "");
    }
    public void bootstrapAdmin(String username, char[] password, String managerEmployeeId) throws IOException {
        if (!accounts.getAll().isEmpty()) throw new IllegalStateException("Tài khoản đã được khởi tạo.");
        Account admin = new Account("A001", username, PasswordUtil.hash(password), Role.ADMIN,
                managerEmployeeId, true, false);
        accounts.add(admin);
        try { file.save(accounts); } catch (IOException | RuntimeException e) { accounts.deleteById(admin.getId()); throw e; }
    }
    public Account createEmployee(String username, String employeeId, Employee employee, char[] password) throws IOException {
        PermissionService.require(PermissionService.Permission.MANAGE_ACCOUNTS);
        if (employee == null || !employee.getId().equalsIgnoreCase(employeeId)) throw new IllegalArgumentException("Nhân viên liên kết không hợp lệ.");
        if (accounts.getAll().stream().anyMatch(a -> a.getEmployeeId().equalsIgnoreCase(employeeId)))
            throw new IllegalArgumentException("Nhân viên đã có tài khoản.");
        String id = String.format(Locale.ROOT, "A%03d", accounts.getAll().size() + 1);
        while (accounts.findById(id) != null) id = String.format(Locale.ROOT, "A%03d", Integer.parseInt(id.substring(1)) + 1);
        Account account = new Account(id, username, PasswordUtil.hash(password), Role.EMPLOYEE,
                employeeId, true, false);
        accounts.add(account);
        try { file.save(accounts); } catch (IOException | RuntimeException e) { accounts.deleteById(id); throw e; }
        return account;
    }
    public void setActive(String id, boolean active) throws IOException {
        PermissionService.require(PermissionService.Permission.MANAGE_ACCOUNTS);
        Account account = requireAccount(id);
        if (!active && account == PermissionService.currentAccount())
            throw new IllegalStateException("Hãy đăng xuất trước khi Admin tự khóa tài khoản của mình.");
        if (!active && account.getRole() == Role.ADMIN && accounts.getAll().stream()
                .filter(a -> a.getRole() == Role.ADMIN && a.isActive()).count() <= 1)
            throw new IllegalStateException("Không thể khóa Admin đang hoạt động cuối cùng.");
        boolean before = account.isActive(); account.setActive(active);
        try { file.save(accounts); } catch (IOException | RuntimeException e) { account.setActive(before); throw e; }
    }
    public void resetEmployeePassword(String id, char[] password) throws IOException {
        PermissionService.require(PermissionService.Permission.MANAGE_ACCOUNTS);
        Account account = requireAccount(id);
        if (account.getRole() != Role.EMPLOYEE) throw new IllegalArgumentException("Chỉ Admin mới được đặt lại mật khẩu Admin.");
        rotateWithPassword(account, password, false);
    }
    public void updateAccount(String id, String username, Role role, String employeeId, Employee linkedEmployee) throws IOException {
        PermissionService.require(PermissionService.Permission.MANAGE_ACCOUNTS);
        Account current = requireAccount(id);
        if (current == PermissionService.currentAccount()) throw new IllegalStateException("Không thể sửa vai trò/tên đăng nhập của phiên đang dùng.");
        if (role == Role.EMPLOYEE && (linkedEmployee == null || !linkedEmployee.getId().equalsIgnoreCase(employeeId)))
            throw new IllegalArgumentException("Cần chọn nhân viên có trong danh sách.");
        if (employeeId != null && !employeeId.isBlank() && accounts.getAll().stream().anyMatch(a -> !a.getId().equalsIgnoreCase(id)
                && a.getEmployeeId().equalsIgnoreCase(employeeId))) throw new IllegalArgumentException("Nhân viên đã liên kết tài khoản khác.");
        if (current.getRole() == Role.ADMIN && role != Role.ADMIN && current.isActive()
                && accounts.getAll().stream().filter(a -> a.getRole() == Role.ADMIN && a.isActive()).count() <= 1)
            throw new IllegalStateException("Không thể hạ quyền Admin đang hoạt động cuối cùng.");
        Account replacement = new Account(current.getId(), username, current.getPasswordHash(), role,
                role == Role.EMPLOYEE ? employeeId : current.getEmployeeId(), current.isActive(), current.mustChangePassword());
        accounts.replace(replacement);
        try { file.save(accounts); } catch (IOException | RuntimeException e) { accounts.replace(current); throw e; }
    }
    public void updateOwnPassword(char[] oldPassword, char[] newPassword, SessionManager session) throws IOException {
        PermissionService.require(PermissionService.Permission.CHANGE_PASSWORD);
        Account account = session.currentAccount();
        if (account == null || account != PermissionService.currentAccount()
                || !PasswordUtil.verify(oldPassword, account.getPasswordHash())) throw new IllegalArgumentException("Mật khẩu hiện tại không đúng.");
        rotateWithPassword(account, newPassword, false);
    }
    private void rotateWithPassword(Account account, char[] password, boolean force) throws IOException {
        String oldHash = account.getPasswordHash(); boolean oldForce = account.mustChangePassword();
        account.setPasswordHash(PasswordUtil.hash(password), force);
        try { file.save(accounts); } catch (IOException | RuntimeException e) { account.setPasswordHash(oldHash, oldForce); throw e; }
    }
    private Account requireAccount(String id) {
        Account account = accounts.findById(id);
        if (account == null) throw new IllegalArgumentException("Không tìm thấy tài khoản.");
        return account;
    }
}
