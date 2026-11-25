import business.AccountService;
import business.PasswordUtil;
import business.PermissionService;
import business.PersonService;
import business.SessionManager;
import business.StoreSession;
import data.AccountFile;
import data.AccountRepository;
import data.StoreFile;
import data.StoreState;
import model.Account;
import model.Employee;
import model.Role;
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
        StoreState state = new StoreState();
        state.getEmployees().add(new Employee("NV001", "Admin", "0900000001", "Quản lý"));
        state.getEmployees().add(new Employee("NV009", "Target", "0900000009", "Thu ngân"));
        new StoreFile(storePath).save(state);

        AccountRepository accounts = new AccountRepository();
        Account admin = new Account("A001", "adminqa", PasswordUtil.hash("admin123".toCharArray()),
                Role.ADMIN, "NV001", true, false);
        accounts.add(admin);
        accounts.add(new Account("A002", "targetqa", PasswordUtil.hash("target123".toCharArray()),
                Role.EMPLOYEE, "NV009", true, false));
        AccountFile accountFile = new AccountFile(accountPath);
        accountFile.save(accounts);

        SessionManager login = new SessionManager();
        PermissionService.enable(login);
        login.login(admin);
        StoreFile failOnce = new StoreFile(storePath) {
            private boolean pending = true;
            @Override public void save(StoreState value) throws IOException {
                if (pending) {
                    pending = false;
                    throw new IOException("Injected store write failure");
                }
                super.save(value);
            }
        };
        StoreSession session = new StoreSession(state, failOnce);
        AccountService service = new AccountService(accounts, accountFile);
        boolean rejected = false;
        try {
            service.deleteEmployeeWithAccount("NV009", () -> session.change(() -> {
                if (!new PersonService(state.getPeople(), state.getInvoices()).delete("NV009"))
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
        check(new AccountFile(accountPath).load().findByUsername("targetqa") != null,
                "Account missing after restart");
        System.out.println("PASS injected store failure: employee and account restored in memory and on disk");
    }
}
