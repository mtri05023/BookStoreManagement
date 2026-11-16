package presentation;

import business.ReportService;
import utility.InputHelper;
import utility.StoreConfig;
import data.StoreState;
import data.StoreFile;
import java.io.IOException;

public class MainMenu {
    private final BookMenu bookMenu;
    private final CustomerMenu customerMenu;
    private final InvoiceMenu invoiceMenu;
    private final ReportService reportService;
    private final InputHelper input;
    private final PersonMenu personMenu;
    private final StoreState state;
    private final StoreFile storeFile;

    public MainMenu(BookMenu books, CustomerMenu customers, InvoiceMenu invoices, ReportService reports,
                    InputHelper input, PersonMenu personMenu, StoreState state, StoreFile storeFile) {
        this.bookMenu = books;
        this.customerMenu = customers;
        this.invoiceMenu = invoices;
        this.reportService = reports;
        this.input = input;
        this.personMenu = personMenu;
        this.state = state;
        this.storeFile = storeFile;
    }
    private business.StoreSession session;
    private EmployeeMenu employeeMenu;
    private InventoryMenu inventoryMenu;
    private ReportMenu reportsMenu;
    public MainMenu(business.StoreSession session, InputHelper input) {
        this(null, null, null, new ReportService(session.state().getInvoices()), input, null, session.state(), null);
        this.session = session;
        var sales = new business.InvoiceService(state.getInvoices(), state.getBooks(), state.getCustomers(), state.getEmployees());
        managedBooks = new BookMenu(session, input);
        managedCustomers = new CustomerMenu(session, input, sales);
        managedInvoices = new InvoiceMenu(session, input, sales);
        employeeMenu = new EmployeeMenu(session, input);
        inventoryMenu = new InventoryMenu(session, input);
        reportsMenu = new ReportMenu(reportService, input);
    }
    private BookMenu managedBooks;
    private CustomerMenu managedCustomers;
    private InvoiceMenu managedInvoices;
    private void showManaged() {
        while (true) {
            ConsoleView.optionsWithZero(StoreConfig.STORE_NAME, "Lưu và thoát", "Quản lý sách", "Quản lý khách hàng", "Lập hóa đơn",
                    "Quản lý hóa đơn", "Thống kê doanh thu", "Quản lý nhân viên", "Quản lý tồn kho", "Lưu toàn bộ dữ liệu");
            System.out.println("Chọn 0 để lưu và thoát. Các thao tác thay đổi được tự động lưu.");
            int choice = input.readInt("Chọn: ");
            try {
                switch (choice) {
                    case 0 -> { save(); System.out.println("Đã thoát chương trình."); return; }
                    case 1 -> managedBooks.show();
                    case 2 -> managedCustomers.show();
                    case 3 -> managedInvoices.createInvoice();
                    case 4 -> managedInvoices.manage();
                    case 5 -> reportsMenu.show();
                    case 6 -> employeeMenu.show();
                    case 7 -> inventoryMenu.show();
                    case 8 -> save();
                    default -> System.out.println("Lựa chọn không hợp lệ.");
                }
            } catch (IOException | IllegalArgumentException | IllegalStateException e) {
                System.out.println("Không thể hoàn tất thao tác: " + e.getMessage());
            }
        }
    }
    public void show() {
        if (session != null) { showManaged(); return; }
        int choice;
        do {
            System.out.println("\n======================================");
            System.out.println("       QUẢN LÝ " + StoreConfig.STORE_NAME);
            System.out.println("======================================");
            System.out.println("1. Quản lý sách");
            System.out.println("2. Quản lý khách hàng");
            System.out.println("3. Lập hóa đơn");
            System.out.println("4. Xem hóa đơn");
            System.out.println("5. Thống kê doanh thu");
            System.out.println("6. Quản lý khách hàng và nhân viên");
            System.out.println("7. Lưu toàn bộ dữ liệu text");
            System.out.println("0. Lưu và thoát");
            System.out.println("Thay đổi được giữ trên đĩa khi chọn 7 hoặc 0.");

            choice = input.readInt("Chọn: ");

            try {
                switch (choice) {
                    case 1 -> bookMenu.show();
                    case 2 -> customerMenu.show();
                    case 3 -> invoiceMenu.createInvoice();
                    case 4 -> invoiceMenu.showInvoices();
                    case 5 -> {
                        System.out.printf("Doanh thu: %,.0f VNĐ%n", reportService.getRevenue());
                        System.out.println("Số lượng đã bán: " + reportService.getTotalQuantitySold());
                        System.out.println("Lượng theo mã sách: " + reportService.getQuantitySoldByBook());
                    }
                    case 6 -> personMenu.show();
                    case 7 -> save();
                    case 0 -> {
                        save();
                        System.out.println("Đã thoát chương trình.");
                        return;
                    }
                    default -> System.out.println("Lựa chọn không hợp lệ.");
                }
            } catch (IllegalArgumentException | IllegalStateException | IOException e) {
                // A failed save must not cause option 0 to exit and lose the live session.
                choice = -1;
                System.out.println(e.getMessage());
            }
        } while (choice != 0);
    }

    public void save() throws IOException {
        if (session != null) {
            session.save();
            System.out.println("Lưu dữ liệu thành công!");
            return;
        }
        storeFile.save(state);
        System.out.println("Đã lưu: " + storeFile.getPath().toAbsolutePath());
    }
}
