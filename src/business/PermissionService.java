package business;

import model.Account;
import model.Role;

/** Shared policy used at the business boundary; the presentation menu is not a security boundary. */
public final class PermissionService {
    public enum Permission { READ_BOOKS, MANAGE_BOOKS, READ_CUSTOMERS, MANAGE_CUSTOMERS, DELETE_CUSTOMERS,
        MANAGE_EMPLOYEES, CREATE_INVOICE, VIEW_OWN_INVOICES, VIEW_ALL_INVOICES, VIEW_INVENTORY,
        MANAGE_INVENTORY, MANAGE_SUPPLIERS, VIEW_REPORTS, MANAGE_ACCOUNTS, CHANGE_PASSWORD }
    private static SessionManager session;
    private PermissionService() { }
    public static void enable(SessionManager manager) { session = java.util.Objects.requireNonNull(manager); }
    public static Account currentAccount() { return session == null ? null : session.currentAccount(); }
    public static boolean isAdmin() { return currentAccount() != null && currentAccount().getRole() == Role.ADMIN; }
    public static void require(Permission permission) {
        Account account = currentAccount();
        if (account == null || !account.isActive()) throw new SecurityException("Vui lòng đăng nhập để sử dụng chức năng.");
        boolean allowed = switch (permission) {
            case READ_BOOKS, READ_CUSTOMERS, VIEW_INVENTORY, CREATE_INVOICE, VIEW_OWN_INVOICES, CHANGE_PASSWORD -> true;
            case MANAGE_CUSTOMERS -> true;
            case MANAGE_BOOKS, DELETE_CUSTOMERS, MANAGE_EMPLOYEES, VIEW_ALL_INVOICES,
                    MANAGE_INVENTORY, MANAGE_SUPPLIERS, VIEW_REPORTS, MANAGE_ACCOUNTS -> account.getRole() == Role.ADMIN;
        };
        if (!allowed) throw new SecurityException("Tài khoản " + account.getRole() + " không có quyền thực hiện thao tác này.");
    }
}
