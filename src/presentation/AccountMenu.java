package presentation;

import business.AccountService;
import data.StoreState;
import model.Account;
import model.Employee;
import utility.InputHelper;

/** Admin-only account administration menu. */
public final class AccountMenu {
    private final AccountService accounts;
    private final StoreState state;
    private final InputHelper input;
    public AccountMenu(AccountService accounts, StoreState state, InputHelper input) {
        this.accounts = accounts; this.state = state; this.input = input;
    }
    public void show() {
        business.PermissionService.require(business.PermissionService.Permission.MANAGE_ACCOUNTS);
        while (true) {
            ConsoleView.options("QUẢN LÝ TÀI KHOẢN", "Danh sách tài khoản", "Tạo tài khoản nhân viên",
                    "Khóa tài khoản", "Mở khóa tài khoản", "Đặt lại mật khẩu nhân viên", "Sửa tên đăng nhập/vai trò");
            int choice = input.readInt("Chọn: ");
            try {
                switch (choice) {
                    case 0 -> { return; }
                    case 1 -> list();
                    case 2 -> createEmployee();
                    case 3 -> { accounts.setActive(input.read("Mã tài khoản cần khóa: "), false); System.out.println("Đã khóa tài khoản."); }
                    case 4 -> { accounts.setActive(input.read("Mã tài khoản cần mở khóa: "), true); System.out.println("Đã mở khóa tài khoản."); }
                    case 5 -> {
                        String id = input.read("Mã tài khoản nhân viên: ");
                        char[] password = readConfirmedPassword();
                        try {
                            accounts.resetEmployeePassword(id, password);
                            System.out.println("Đã đặt mật khẩu mới cho tài khoản.");
                        } finally { java.util.Arrays.fill(password, '\0'); }
                    }
                    case 6 -> updateAccount();
                    default -> System.out.println("Lựa chọn không hợp lệ.");
                }
            } catch (IllegalArgumentException | IllegalStateException | java.io.IOException e) {
                System.out.println("Không thể hoàn tất: " + e.getMessage());
            }
        }
    }
    private void list() {
        for (Account account : accounts.repository().getAll())
            System.out.printf("%s | %-20s | %-8s | nhân viên: %-8s | %s | %s%n", account.getId(), account.getUsername(),
                    account.getRole(), account.getEmployeeId().isBlank() ? "-" : account.getEmployeeId(),
                    account.isActive() ? "Hoạt động" : "Đã khóa", account.mustChangePassword() ? "Cần đổi mật khẩu" : "");
    }
    private void createEmployee() throws java.io.IOException {
        String employeeId = input.readExistingCode("NV", "Mã nhân viên: ");
        Employee employee = state.getEmployees().findById(employeeId);
        if (employee == null) throw new IllegalArgumentException("Không tìm thấy nhân viên.");
        String username = input.read("Tên đăng nhập mới: ");
        char[] password = readConfirmedPassword();
        try {
            Account created = accounts.createEmployee(username, employeeId, employee, password);
            System.out.println("Tạo tài khoản thành công: " + created.getId());
        } finally { java.util.Arrays.fill(password, '\0'); }
    }
    private char[] readConfirmedPassword() {
        while (true) {
            char[] password = input.readPassword("Mật khẩu (3-128 ký tự): ");
            char[] confirmation = input.readPassword("Nhập lại mật khẩu: ");
            if (java.util.Arrays.equals(password, confirmation)) {
                java.util.Arrays.fill(confirmation, '\0');
                return password;
            }
            java.util.Arrays.fill(password, '\0');
            java.util.Arrays.fill(confirmation, '\0');
            System.out.println("Mật khẩu xác nhận không khớp. Vui lòng nhập lại.");
        }
    }
    private void updateAccount() throws java.io.IOException {
        String id = input.read("Mã tài khoản cần sửa: ");
        Account current = accounts.repository().findById(id);
        if (current == null) throw new IllegalArgumentException("Không tìm thấy tài khoản.");
        String username = input.read("Tên đăng nhập mới: ");
        System.out.println("1. ADMIN\n2. EMPLOYEE");
        int choice = input.readInt("Vai trò mới: ");
        model.Role role = switch (choice) { case 1 -> model.Role.ADMIN; case 2 -> model.Role.EMPLOYEE;
            default -> throw new IllegalArgumentException("Vai trò không hợp lệ."); };
        String employeeId = current.getEmployeeId(); Employee linked = null;
        if (role == model.Role.EMPLOYEE) {
            employeeId = input.readExistingCode("NV", "Mã nhân viên liên kết: ");
            linked = state.getEmployees().findById(employeeId);
        }
        accounts.updateAccount(id, username, role, employeeId, linked);
        System.out.println("Đã cập nhật tài khoản.");
    }
}
