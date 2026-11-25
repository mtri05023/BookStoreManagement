package bookstore;

import java.io.IOException;
import java.nio.file.Path;

/** Inject one failed store write after the linked account file has been saved. */
public final class FailureInjection {
    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    public static void main(String[] args) throws Exception {
        Path storePath = Path.of(args[0]);
        Path accountPath = Path.of(args[0] + ".accounts");
        BookStore state = new BookStore();
        state.getEmployees().add(new Employee("NV001", "Admin", "0900000001", "Quản lý"));
        state.getEmployees().add(new Employee("NV009", "Target", "0900000009", "Thu ngân"));
        new StoreFile(storePath).save(state);

        AccountList accounts = new AccountList();
        Account admin = new Account("A001", "adminqa", Account.hash("admin123".toCharArray()),
                Account.Role.ADMIN, "NV001", true, false);
        accounts.add(admin);
        accounts.add(new Account("A002", "targetqa", Account.hash("target123".toCharArray()),
                Account.Role.EMPLOYEE, "NV009", true, false));
        StoreFile accountFile = new StoreFile(accountPath);
        accountFile.save(accounts);

        AccountList login = accounts;
        AccountList.enable(login);
        login.login(admin);
        StoreFile failOnce = new StoreFile(storePath) {
            private boolean pending = true;
            @Override public void save(BookStore value) throws IOException {
                if (pending) {
                    pending = false;
                    throw new IOException("Injected store write failure");
                }
                super.save(value);
            }
        };
        BookStore session = state;
        state.useFile(failOnce);
        AccountList service = accounts;
        accounts.configure(accountFile, state.getEmployees());
        boolean rejected = false;
        try {
            service.deleteEmployeeWithAccount("NV009", () -> session.change(() -> {
                if (!state.deletePerson("NV009", false))
                    throw new IllegalStateException("Missing employee");
            }));
        } catch (IllegalStateException expected) {
            rejected = expected.getMessage().contains("Injected store write failure");
        }
        check(rejected, "Injected failure was not observed");
        check(state.getEmployees().findById("NV009") != null, "Employee missing in memory after failure");
        check(accounts.findByUsername("targetqa") != null, "Account missing in memory after failure");
        check(new StoreFile(storePath).load().getEmployees().findById("NV009") != null,
                "Employee missing after restart");
        check(new StoreFile(accountPath).loadAccounts().findByUsername("targetqa") != null,
                "Account missing after restart");
        System.out.println("PASS injected store failure: employee and account restored in memory and on disk");
    }
}
