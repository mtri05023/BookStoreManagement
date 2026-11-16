package presentation;

import business.BookService;
import business.StoreSession;
import model.Book;
import utility.InputHelper;
import java.math.BigDecimal;

public final class InventoryMenu {
    private final StoreSession session;
    private final InputHelper input;
    private final BookService books;
    public InventoryMenu(StoreSession session, InputHelper input) {
        this.session = session; this.input = input; books = new BookService(session.state().getBooks());
    }
    public void show() {
        while (true) {
            int threshold = session.state().getLowStockThreshold();
            ConsoleView.options("QUẢN LÝ TỒN KHO (ngưỡng cảnh báo: " + threshold + ")", "Xem toàn bộ tồn kho",
                    "Tìm theo mã", "Tìm theo tên", "Nhập thêm sách", "Điều chỉnh tồn kho (có lý do)",
                    "Sách sắp hết hàng", "Sách đã hết hàng", "Giá trị tồn kho", "Đổi ngưỡng cảnh báo");
            int choice = input.readInt("Chọn: ");
            try {
                switch (choice) {
                    case 0 -> { return; }
                    case 1 -> display(books.getBooks().toArray(new Book[0]));
                    case 2 -> {
                        String id = input.readExistingCode("S", "Mã sách: "); Book b = books.findBook(id);
                        if (b == null) throw new IllegalArgumentException("Không tìm thấy sách: " + id);
                        display(new Book[] { b });
                    }
                    case 3 -> display(books.search(input.read("Tên sách: ")));
                    case 4, 5 -> {
                        String id = input.readExistingCode("S", "Mã sách: ");
                        int quantity = input.readInt(choice == 4 ? "Số lượng nhập thêm: " : "Số lượng thực tế sau điều chỉnh: ");
                        String reason = input.read("Lý do: ");
                        if (!input.read("Gõ DONGY để xác nhận: ").equals("DONGY")) { System.out.println("Đã hủy thao tác kho."); break; }
                        session.change(() -> session.stock(id, quantity, choice == 4, reason));
                        System.out.println((choice == 4 ? "Nhập hàng" : "Điều chỉnh tồn kho") + " thành công! Đã lưu dữ liệu.");
                    }
                    case 6 -> display(books.getBooks().stream().filter(b -> b.getQuantity() > 0 && b.getQuantity() <= threshold).toArray(Book[]::new));
                    case 7 -> display(books.getBooks().stream().filter(b -> b.getQuantity() == 0).toArray(Book[]::new));
                    case 8 -> {
                        BigDecimal cost = BigDecimal.ZERO, retail = BigDecimal.ZERO;
                        int unknown = 0;
                        for (Book b : books.getBooks()) {
                            cost = cost.add(BigDecimal.valueOf(b.getPurchasePrice()).multiply(BigDecimal.valueOf(b.getQuantity())));
                            retail = retail.add(BigDecimal.valueOf(b.getPrice()).multiply(BigDecimal.valueOf(b.getQuantity())));
                            if (b.getPurchasePrice() == 0 && b.getQuantity() > 0) unknown++;
                        }
                        System.out.printf("Giá trị theo giá nhập đã khai báo: %,.0f VNĐ%nGiá trị theo giá bán: %,.0f VNĐ%n", cost, retail);
                        if (unknown > 0) System.out.println("Có " + unknown + " đầu sách chưa khai báo giá nhập; tổng giá nhập chưa đầy đủ.");
                    }
                    case 9 -> {
                        int value = input.readInt("Ngưỡng cảnh báo mới (>=0): ");
                        if (value < 0) throw new IllegalArgumentException("Ngưỡng cảnh báo không được âm.");
                        session.change(() -> session.state().setLowStockThreshold(value));
                        System.out.println("Cập nhật ngưỡng cảnh báo thành công! Đã lưu dữ liệu.");
                    }
                    default -> System.out.println("Lựa chọn không hợp lệ.");
                }
            } catch (IllegalArgumentException | IllegalStateException e) { System.out.println(e.getMessage()); }
        }
    }
    private void display(Book[] values) {
        int threshold = session.state().getLowStockThreshold();
        ConsoleView.books(values);
        for (Book b : values) System.out.println(b.getId() + ": " + (b.getQuantity() == 0 ? "Hết hàng" : b.getQuantity() <= threshold ? "Sắp hết" : "Còn hàng"));
    }
}
