package presentation;

import business.ReportService;
import utility.InputHelper;

public class MainMenu {
    private final BookMenu bookMenu;
    private final CustomerMenu customerMenu;
    private final InvoiceMenu invoiceMenu;
    private final ReportService reportService;
    private final InputHelper input;
    public MainMenu(BookMenu books, CustomerMenu customers, InvoiceMenu invoices, ReportService reports, InputHelper input) {
        this.bookMenu = books;
        this.customerMenu = customers;
        this.invoiceMenu = invoices;
        this.reportService = reports;
        this.input = input;
    }
    public void show() {
        int choice;
        do {
            System.out.println("\n======================================");
            System.out.println("       QUẢN LÝ CỬA HÀNG SÁCH");
            System.out.println("======================================");
            System.out.println("1. Quản lý sách");
            System.out.println("2. Quản lý khách hàng");
            System.out.println("3. Lập hóa đơn");
            System.out.println("4. Xem hóa đơn");
            System.out.println("5. Thống kê doanh thu");
            System.out.println("0. Thoát");

            choice = input.readInt("Chọn: ");

            try {
                switch (choice) {
                    case 1 -> bookMenu.show();
                    case 2 -> customerMenu.show();
                    case 3 -> invoiceMenu.createInvoice();
                    case 4 -> invoiceMenu.showInvoices();
                    case 5 -> System.out.printf("Doanh thu: %,.0f VNĐ%n",
                            reportService.getRevenue());
                    case 0 -> System.out.println("Đã thoát chương trình.");
                    default -> System.out.println("Lựa chọn không hợp lệ.");
                }
            } catch (IllegalArgumentException | IllegalStateException e) {
                System.out.println(e.getMessage());
            }
        } while (choice != 0);
    }
}
