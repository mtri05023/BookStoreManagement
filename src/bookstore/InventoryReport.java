package bookstore;

import java.math.BigDecimal;
import java.util.LinkedHashSet;

public class InventoryReport {
    private final BookStore session;
    private final InputHelper input;
    private final BookList books;
    public InventoryReport(BookStore session, InputHelper input) {
        this.session = session; this.input = input; books = session.getBooks();
    }
    public void show() {
        while (true) {
            int threshold = session.getLowStockThreshold();
            InputHelper.options("QUẢN LÝ TỒN KHO (ngưỡng cảnh báo: " + threshold + ")", "Xem toàn bộ tồn kho",
                    "Tìm theo mã", "Tìm theo tên", "Nhập hàng từ nhà cung cấp", "Điều chỉnh tồn kho (có lý do)",
                    "Sách sắp hết hàng", "Sách đã hết hàng", "Giá trị tồn kho", "Đổi ngưỡng cảnh báo");
            int choice = input.readInt("Chọn: ");
            try {
                switch (choice) {
                    case 0 -> { return; }
                    case 1 -> display(books.getBooks().toArray(new Book[0]));
                    case 2 -> {
                        while (true) {
                            String id = input.readExistingCode("S", "Mã sách (0 để quay lại): ", "0");
                            if (id.equals("0")) break;
                            Book b = books.findBook(id);
                            if (b == null) {
                                System.out.println("Sai mã sách: " + id + ". Vui lòng nhập lại.");
                                continue;
                            }
                            display(new Book[] { b });
                            break;
                        }
                    }
                    case 3 -> {
                        Book[] result = books.search(input.read("Tên sách: "));
                        display(result);
                        System.out.println(result.length == 0 ? "Không tìm thấy sách theo tên."
                                : "Tìm tồn kho theo tên thành công: " + result.length + " sách.");
                    }
                    case 4 -> {
                        if (session.getSuppliers().getAll().isEmpty())
                            throw new IllegalStateException("Chưa có nhà cung cấp. Hãy thêm nhà cung cấp trước khi nhập hàng.");
                        String id = input.readExistingCode("S", "Mã sách: ");
                        Book book = books.findBook(id);
                        if (book == null) throw new IllegalArgumentException("Không tìm thấy sách: " + id);
                        System.out.println("Nhà cung cấp hiện có:");
                        session.getSuppliers().getAll().forEach(System.out::println);
                        String supplierId = input.readExistingCode("NCC", "Mã nhà cung cấp: ");
                        if (session.getSuppliers().findById(supplierId) == null)
                            throw new IllegalArgumentException("Không tìm thấy nhà cung cấp: " + supplierId);
                        int quantity = input.readInt("Số lượng nhập thêm: ");
                        String reason = input.read("Lý do: ");
                        if (!input.read("Gõ DONGY để xác nhận: ").equals("DONGY")) { System.out.println("Đã hủy thao tác kho."); break; }
                        session.change(() -> session.receiveStock(id, quantity, supplierId, reason));
                        System.out.println("Nhập hàng từ nhà cung cấp thành công! Đã lưu dữ liệu.");
                    }
                    case 5 -> {
                        String id = input.readExistingCode("S", "Mã sách: ");
                        Book book = books.findBook(id);
                        if (book == null) throw new IllegalArgumentException("Không tìm thấy sách: " + id);
                        int quantity = input.readInt("Số lượng thực tế sau điều chỉnh: ");
                        String reason = input.read("Lý do: ");
                        if (!input.read("Gõ DONGY để xác nhận: ").equals("DONGY")) { System.out.println("Đã hủy thao tác kho."); break; }
                        session.change(() -> session.stock(id, quantity, false, reason));
                        System.out.println("Điều chỉnh tồn kho thành công! Đã lưu dữ liệu.");
                    }
                    case 6 -> {
                        Book[] result = books.getBooks().stream().filter(b -> b.getQuantity() > 0 && b.getQuantity() <= threshold).toArray(Book[]::new);
                        display(result);
                        System.out.println(result.length == 0 ? "Không có sách sắp hết hàng." : "Đã tìm thấy " + result.length + " sách sắp hết hàng.");
                    }
                    case 7 -> {
                        Book[] result = books.getBooks().stream().filter(b -> b.getQuantity() == 0).toArray(Book[]::new);
                        display(result);
                        System.out.println(result.length == 0 ? "Không có sách hết hàng." : "Có " + result.length + " sách hết hàng.");
                    }
                    case 8 -> {
                        BigDecimal cost = BigDecimal.ZERO, retail = BigDecimal.ZERO;
                        int unknown = 0;
                        for (Book b : books.getBooks()) {
                            cost = cost.add(BigDecimal.valueOf(b.getPurchasePrice()).multiply(BigDecimal.valueOf(b.getQuantity())));
                            retail = retail.add(BigDecimal.valueOf(b.getPrice()).multiply(BigDecimal.valueOf(b.getQuantity())));
                            if (b.getPurchasePrice() == 0 && b.getQuantity() > 0) unknown++;
                        }
                        System.out.printf(InputHelper.CURRENCY_LOCALE, "Giá trị theo giá nhập đã khai báo: %,.0f VNĐ%nGiá trị theo giá bán: %,.0f VNĐ%n", cost, retail);
                        if (unknown > 0) System.out.println("Có " + unknown + " đầu sách chưa khai báo giá nhập; tổng giá nhập chưa đầy đủ.");
                        System.out.println("Đã thống kê giá trị tồn kho thành công.");
                    }
                    case 9 -> {
                        int value = input.readInt("Ngưỡng cảnh báo mới (>=0): ");
                        if (value < 0) throw new IllegalArgumentException("Ngưỡng cảnh báo không được âm.");
                        session.change(() -> session.setLowStockThreshold(value));
                        System.out.println("Cập nhật ngưỡng cảnh báo thành công! Đã lưu dữ liệu.");
                    }
                    default -> System.out.println("Lựa chọn không hợp lệ.");
                }
            } catch (IllegalArgumentException | IllegalStateException e) { System.out.println(e.getMessage()); }
        }
    }
    public void showForEmployee() {
        AccountList.require(AccountList.Permission.VIEW_INVENTORY);
        display(books.getBooks().toArray(new Book[0]));
    }
    private void display(Book[] values) {
        int threshold = session.getLowStockThreshold();
        System.out.println(fit("Mã", 8) + " | " + fit("Tên sách", 28) + " | " + fit("Tác giả", 24)
                + " | " + String.format("%10s", "Giá bán") + " | " + String.format("%8s", "Tồn kho")
                + " | " + fit("Nhà cung cấp", 28) + " | " + "Trạng thái");
        System.out.println("-".repeat(8) + "-+-" + "-".repeat(28) + "-+-" + "-".repeat(24)
                + "-+-" + "-".repeat(10) + "-+-" + "-".repeat(8) + "-+-" + "-".repeat(28) + "-+-" + "-".repeat(10));
        for (Book b : values) {
            String status = b.getQuantity() == 0 ? "Hết hàng" : b.getQuantity() <= threshold ? "Sắp hết" : "Còn hàng";
            String suppliers = suppliersForBook(b.getId());
            System.out.println(fit(b.getId(), 8) + " | " + fit(b.getTitle(), 28) + " | " + fit(b.getAuthor(), 24)
                    + " | " + String.format("%10.0f", b.getPrice()) + " | " + String.format("%8d", b.getQuantity())
                    + " | " + fit(suppliers, 28) + " | " + fit(status, 10));
        }
        System.out.println(values.length == 0 ? "Không có sách tồn kho phù hợp." : "Đã hiển thị tồn kho của " + values.length + " sách.");
    }

    private String fit(String value, int width) {
        String text = value == null ? "" : value;
        if (text.length() > width) text = text.substring(0, width - 1) + "…";
        return String.format("%-" + width + "s", text);
    }

    private String suppliersForBook(String bookId) {
        LinkedHashSet<String> names = new LinkedHashSet<>();
        for (String row : session.getStockEvents()) {
            String[] fields = row.split("\\|", -1);
            if (fields.length == 4 && fields[0].equals("SOURCE") && fields[1].equalsIgnoreCase(bookId))
                names.add(fields[3]);
            else if (fields.length == 8 && fields[0].equals("RECEIPT") && fields[2].equalsIgnoreCase(bookId))
                names.add(fields[6]);
        }
        return names.isEmpty() ? "Chưa ghi nhận" : String.join(", ", names);
    }
}
