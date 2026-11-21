package presentation;

import business.*;
import data.*;
import model.Account;
import model.Role;
import utility.InputHelper;
import utility.StoreConfig;
import java.io.IOException;
import java.util.Arrays;

public class MainMenu {
    private final InputHelper input;
    private final StoreSession session;
    private final AccountService accountService;
    private final AuthService authService;
    private final SessionManager loginSession;
    private final BookMenu books;
    private final CustomerMenu customers;
    private final InvoiceMenu invoices;
    private final EmployeeMenu employees;
    private final InventoryMenu inventory;
    private final SupplierMenu suppliers;
    private final ReportMenu reports;
    private final AccountMenu accounts;

    /** Keeps the original constructor available for existing callers. */
    public MainMenu(BookMenu bookMenu, CustomerMenu customerMenu, InvoiceMenu invoiceMenu,
                    ReportService reportService, InputHelper input, PersonMenu personMenu,
                    StoreState state, StoreFile storeFile) {
        this.input = input; this.session = null; this.accountService = null; this.authService = null;
        this.loginSession = null; this.books = bookMenu; this.customers = customerMenu; this.invoices = invoiceMenu;
        this.employees = null; this.inventory = null; this.suppliers = null;
        this.reports = null; this.accounts = null;
    }

    public MainMenu(StoreSession session, InputHelper input, AccountService accountService,
                    AuthService authService, SessionManager loginSession) {
        this.input = input; this.session = session; this.accountService = accountService;
        this.authService = authService; this.loginSession = loginSession;
        StoreState state = session.state();
        BookService bookService = new BookService(state.getBooks(), state.getInvoices());
        CustomerService customerService = new CustomerService(state.getCustomers());
        InvoiceService invoiceService = new InvoiceService(state.getInvoices(), state.getBooks(), state.getCustomers(), state.getEmployees());
        this.books = new BookMenu(session, input);
        this.customers = new CustomerMenu(session, input, invoiceService);
        this.invoices = new InvoiceMenu(session, input, invoiceService, () -> {
            Account current = PermissionService.currentAccount();
            return current != null && current.getRole() == Role.EMPLOYEE
                    ? state.getEmployees().findById(current.getEmployeeId())
                    : state.getEmployees().findById(StoreConfig.DEFAULT_EMPLOYEE_ID);
        });
        this.employees = new EmployeeMenu(session, input);
        this.inventory = new InventoryMenu(session, input);
        this.suppliers = new SupplierMenu(session, input);
        this.reports = new ReportMenu(new ReportService(state.getInvoices()), input, state);
        this.accounts = new AccountMenu(accountService, state, input);
    }

    public void show() {
        if (authService == null) throw new IllegalStateException("Menu phải được khởi tạo qua luồng đăng nhập.");
        while (true) {
            System.out.println("\n========== BOOKSTORE MANAGEMENT ==========");
            try {
                String username = input.read("Tên đăng nhập (0 để thoát): ");
                if (username.equals("0")) { session.save(); return; }
                login(username);
            } catch (IllegalArgumentException | IllegalStateException | IOException e) {
                System.out.println("Không thể hoàn tất: " + e.getMessage());
            }
        }
    }

    private void login(String username) throws IOException {
        char[] password = input.readPassword("Mật khẩu: ");
        Account account;
        try { account = authService.login(username, password); }
        finally { Arrays.fill(password, '\0'); }
        System.out.println("Đăng nhập thành công. Xin chào " + account.getUsername() + " (" + account.getRole() + ").");
        if (account.mustChangePassword()) forcePasswordChange();
        if (loginSession.isAuthenticated()) {
            if (account.getRole() == Role.ADMIN) showAdmin(); else showEmployee();
            session.save();
        }
        authService.logout();
        System.out.println("Đã đăng xuất.");
    }

    private void forcePasswordChange() throws IOException {
        System.out.println("Bạn phải đổi mật khẩu tạm trước khi sử dụng hệ thống.");
        while (true) {
            char[] oldPassword = input.readPassword("Mật khẩu tạm hiện tại: ");
            char[] next = input.readPassword("Mật khẩu mới (3-128 ký tự): ");
            char[] confirm = input.readPassword("Nhập lại mật khẩu mới: ");
            try {
                if (!Arrays.equals(next, confirm)) { System.out.println("Mật khẩu xác nhận không khớp."); continue; }
                accountService.updateOwnPassword(oldPassword, next, loginSession);
                System.out.println("Đã đổi mật khẩu.");
                return;
            } catch (IllegalArgumentException e) { System.out.println(e.getMessage()); }
            finally { Arrays.fill(oldPassword, '\0'); Arrays.fill(next, '\0'); Arrays.fill(confirm, '\0'); }
        }
    }

    private void changePassword() {
        while (true) {
            char[] oldPassword = input.readPassword("Mật khẩu hiện tại: ");
            char[] next = input.readPassword("Mật khẩu mới (3-128 ký tự): ");
            char[] confirm = input.readPassword("Nhập lại mật khẩu mới: ");
            try {
                if (!Arrays.equals(next, confirm)) { System.out.println("Mật khẩu xác nhận không khớp."); continue; }
                accountService.updateOwnPassword(oldPassword, next, loginSession);
                System.out.println("Đổi mật khẩu thành công.");
                return;
            } catch (IllegalArgumentException | IOException e) { System.out.println("Không thể đổi mật khẩu: " + e.getMessage()); }
            finally { Arrays.fill(oldPassword, '\0'); Arrays.fill(next, '\0'); Arrays.fill(confirm, '\0'); }
        }
    }

    private void showAdmin() throws IOException {
        while (loginSession.isAuthenticated()) {
            ConsoleView.options("ADMIN MENU", "Quản lý sách", "Quản lý khách hàng", "Quản lý nhân viên",
                    "Quản lý nhà cung cấp", "Lập hóa đơn", "Quản lý hóa đơn", "Quản lý tồn kho",
                    "Báo cáo doanh thu", "Quản lý tài khoản", "Đổi mật khẩu", "Lưu dữ liệu");
            int choice = input.readInt("Chọn: ");
            try {
                switch (choice) {
                    case 0 -> { session.save(); return; }
                    case 1 -> books.show(); case 2 -> customers.show(); case 3 -> employees.show();
                    case 4 -> suppliers.show(); case 5 -> invoices.createInvoice(); case 6 -> invoices.manage();
                    case 7 -> inventory.show(); case 8 -> reports.show(); case 9 -> accounts.show();
                    case 10 -> changePassword(); case 11 -> { session.save(); System.out.println("Đã lưu dữ liệu."); }
                    default -> System.out.println("Lựa chọn không hợp lệ.");
                }
            } catch (IOException | IllegalArgumentException | IllegalStateException | SecurityException e) {
                System.out.println("Không thể hoàn tất thao tác: " + e.getMessage());
            }
        }
    }

    private void showEmployee() throws IOException {
        while (loginSession.isAuthenticated()) {
            ConsoleView.options("EMPLOYEE MENU", "Xem và tìm kiếm sách", "Quản lý khách hàng",
                    "Lập hóa đơn", "Xem hóa đơn của tôi", "Xem tồn kho", "Đổi mật khẩu");
            int choice = input.readInt("Chọn: ");
            try {
                switch (choice) {
                    case 0 -> { session.save(); return; }
                    case 1 -> books.showForEmployee(); case 2 -> customers.showForEmployee();
                    case 3 -> invoices.createInvoice(); case 4 -> invoices.showOwnInvoices();
                    case 5 -> inventory.showForEmployee(); case 6 -> changePassword();
                    default -> System.out.println("Lựa chọn không hợp lệ.");
                }
            } catch (IOException | IllegalArgumentException | IllegalStateException | SecurityException e) {
                System.out.println("Không thể hoàn tất thao tác: " + e.getMessage());
            }
        }
    }

    public void save() throws IOException { session.save(); }
}
