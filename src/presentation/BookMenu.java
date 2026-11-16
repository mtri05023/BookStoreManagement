package presentation;

import business.BookService;
import model.Book;
import utility.InputHelper;

public class BookMenu {
    private final BookService bookService;
    private final InputHelper input;
    public BookMenu(BookService bookService, InputHelper input) {
        this.bookService = bookService;
        this.input = input;
    }
    private business.StoreSession session;
    public BookMenu(business.StoreSession session, InputHelper input) {
        this(new BookService(session.state().getBooks(), session.state().getInvoices()), input);
        this.session = session;
    }
    public void show() {
        if (session != null) { showManaged(); return; }
        int choice;
        do {
            System.out.println("\n========== QUẢN LÝ SÁCH ==========");
            System.out.println("1. Thêm sách");
            System.out.println("2. Danh sách sách");
            System.out.println("3. Tìm sách");
            System.out.println("4. Cập nhật sách");
            System.out.println("5. Xóa sách");
            System.out.println("0. Quay lại");

            choice = input.readInt("Chọn: ");

            try {
                switch (choice) {
                    case 1 -> addBook();
                    case 2 -> showBooks();
                    case 3 -> findBook();
                    case 4 -> updateBook();
                    case 5 -> deleteBook();
                    case 0 -> { }
                    default -> System.out.println("Lựa chọn không hợp lệ.");
                }
            } catch (IllegalArgumentException | IllegalStateException e) {
                System.out.println(e.getMessage());
            }
        } while (choice != 0);
    }

    private void showManaged() {
        while (true) {
            ConsoleView.options("QUẢN LÝ SÁCH", "Thêm sách", "Hiển thị danh sách", "Tìm theo mã",
                    "Tìm theo tên hoặc từ khóa", "Sửa thông tin", "Xóa sách",
                    "Lọc theo khoảng giá", "Xem tồn kho");
            int choice = input.readInt("Chọn: ");
            try {
                switch (choice) {
                    case 0 -> { return; }
                    case 1 -> {
                        String id = input.readNewCode("S", "Mã sách: ");
                        if (bookService.findBook(id) != null) throw new IllegalArgumentException("Không thể thêm sách: Mã sách đã tồn tại.");
                        Book b = readBook(id, 0);
                        session.change(() -> {
                            if (!bookService.addBook(b)) throw new IllegalArgumentException("Mã sách đã tồn tại hoặc dữ liệu không hợp lệ.");
                        });
                        System.out.println("Thêm sách thành công! Đã lưu dữ liệu.");
                    }
                    case 2, 8 -> ConsoleView.books(bookService.getBooks().toArray(new Book[0]));
                    case 3 -> {
                        String id = input.readExistingCode("S", "Mã sách: ");
                        Book b = bookService.findBook(id);
                        if (b == null) System.out.println("Không tìm thấy sách có mã " + id);
                        else ConsoleView.books(new Book[] { b });
                    }
                    case 4 -> ConsoleView.books(bookService.search(input.read("Tên hoặc từ khóa: ")));
                    case 5 -> {
                        String id = input.readExistingCode("S", "Mã sách cần sửa: ");
                        Book old = bookService.findBook(id);
                        if (old == null) throw new IllegalArgumentException("Không tìm thấy sách: " + id);
                        System.out.println("Tồn kho được điều chỉnh riêng tại menu Quản lý tồn kho.");
                        Book b = readBook(old.getId(), old.getQuantity());
                        session.change(() -> bookService.updateBook(b));
                        System.out.println("Cập nhật thông tin sách thành công! Đã lưu dữ liệu.");
                    }
                    case 6 -> {
                        String id = input.readExistingCode("S", "Mã sách cần xóa: ");
                        session.change(() -> session.deleteBook(id));
                        System.out.println("Xóa sách thành công! Đã lưu dữ liệu.");
                    }
                    case 7 -> ConsoleView.books(bookService.filterPrice(input.readDouble("Giá thấp nhất: "), input.readDouble("Giá cao nhất: ")));
                    default -> System.out.println("Lựa chọn không hợp lệ.");
                }
            } catch (IllegalArgumentException | IllegalStateException e) { System.out.println(e.getMessage()); }
        }
    }
    private Book readBook(String id, int quantity) {
        String title = input.read("Tên sách: ");
        String author = input.read("Tác giả: ");
        Book existing = bookService.findBook(id);
        String category = existing == null ? "Chưa phân loại" : existing.getCategory();
        double cost = input.readDouble("Giá nhập (VNĐ, 0 nếu chưa có): ");
        double price = input.readDouble("Giá bán (VNĐ): ");
        if (bookService.findBook(id) == null) quantity = input.readInt("Tồn kho ban đầu: ");
        return new Book(id, title, author, category, cost, price, quantity);
    }
    public void addBook() {
        String id = input.read("Mã sách: ");
        String title = input.read("Tên sách: ");
        String author = input.read("Tác giả: ");
        double price = input.readDouble("Giá: ");
        int quantity = input.readInt("Số lượng: ");

        if (bookService.addBook(new Book(id, title, author, price, quantity)))
            System.out.println("Thêm sách thành công.");
        else
            System.out.println("Không thể thêm sách. Kiểm tra mã hoặc dữ liệu.");
    }

    public void showBooks() {
        System.out.printf("%-8s %-25s %-20s %10s %6s%n",
                "Mã", "Tên sách", "Tác giả", "Giá", "SL");
        for (Book book : bookService.getBooks())
            System.out.println(book);
    }

    public void findBook() {
        Book book = bookService.findBook(input.read("Nhập mã sách: "));
        System.out.println(book == null ? "Không tìm thấy." : book);
    }

    public void updateBook() {
        String id = input.read("Mã sách cần sửa: ");
        if (bookService.findBook(id) == null) {
            System.out.println("Không tìm thấy sách.");
            return;
        }
        String title = input.read("Tên mới: ");
        String author = input.read("Tác giả mới: ");
        double price = input.readDouble("Giá mới: ");
        int quantity = input.readInt("Số lượng mới: ");

        System.out.println(bookService.updateBook(id, title, author, price, quantity)
                ? "Cập nhật thành công." : "Cập nhật thất bại.");
    }

    public void deleteBook() {
        String id = input.read("Mã sách cần xóa: ");
        System.out.println(bookService.deleteBook(id)
                ? "Xóa thành công." : "Không tìm thấy sách.");
    }

}
