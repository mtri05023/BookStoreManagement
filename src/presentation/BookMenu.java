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

    public void showForEmployee() {
        while (true) {
            ConsoleView.options("SÁCH", "Danh sách", "Tìm theo mã", "Tìm theo tên hoặc từ khóa", "Lọc theo giá", "Sắp xếp");
            int choice = input.readInt("Chọn: ");
            try {
                switch (choice) {
                    case 0 -> { return; }
                    case 1 -> showBooks();
                    case 2 -> findBook();
                    case 3 -> searchBooks();
                    case 4 -> filterPriceInteractive();
                    case 5 -> ConsoleView.books(bookService.sorted(input.readInt(
                            "Chọn tiêu chí sắp xếp (1 - Tên sách A-Z, 2 - Giá bán tăng dần, 3 - Số lượng tồn kho tăng dần): ")));
                    default -> System.out.println("Lựa chọn không hợp lệ.");
                }
            } catch (IllegalArgumentException | IllegalStateException e) { System.out.println(e.getMessage()); }
        }
    }

    private void showManaged() {
        while (true) {
            ConsoleView.options("QUẢN LÝ SÁCH", "Thêm sách", "Hiển thị danh sách", "Tìm theo mã",
                    "Tìm theo tên hoặc từ khóa", "Sửa thông tin", "Xóa sách",
                    "Lọc theo khoảng giá", "Xem tồn kho", "Thêm nhiều sách");
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
                    case 2, 8 -> {
                        Book[] result = bookService.getBooks().toArray(new Book[0]);
                        ConsoleView.books(result);
                        System.out.println(result.length == 0 ? "Danh sách sách hiện đang trống." : "Đã hiển thị toàn bộ sách.");
                    }
                    case 3 -> {
                        while (true) {
                            String id = input.readExistingCode("S", "Mã sách (0 để quay lại): ", "0");
                            if (id.equals("0")) break;
                            Book b = bookService.findBook(id);
                            if (b == null) {
                                System.out.println("Sai mã sách: " + id + ". Vui lòng nhập lại.");
                                continue;
                            }
                            ConsoleView.books(new Book[] { b });
                            System.out.println("Tìm sách theo mã thành công.");
                            break;
                        }
                    }
                    case 4 -> {
                        searchBooks();
                    }
                    case 5 -> {
                        String id = input.readExistingCode("S", "Mã sách cần sửa: ");
                        Book old = bookService.findBook(id);
                        if (old == null) throw new IllegalArgumentException("Không tìm thấy sách: " + id);
                        System.out.println("Hiện tại: " + old);
                        Book b = readBookUpdate(old);
                        if (b == null) break;
                        if (!b.getId().equals(old.getId())) session.renameBook(old.getId(), b);
                        else session.change(() -> bookService.updateBook(b));
                        System.out.println("Cập nhật thông tin sách thành công! Đã lưu dữ liệu.");
                    }
                    case 6 -> {
                        String id = input.readExistingCode("S", "Mã sách cần xóa: ");
                        session.change(() -> session.deleteBook(id));
                        System.out.println("Xóa sách thành công! Đã lưu dữ liệu.");
                    }
                    case 7 -> {
                        filterPriceInteractive();
                    }
                    case 9 -> {
                        int count = input.readInt("Số lượng sách cần thêm (1..1000): ");
                        if (count < 1 || count > 1000)
                            throw new IllegalArgumentException("Số lượng sách phải trong 1..1000.");
                        Book[] batch = new Book[count];
                        for (int i = 0; i < count; i++) {
                            System.out.println("Sách thứ " + (i + 1) + "/" + count);
                            String id = input.readNewCode("S", "Mã sách: ");
                            if (bookService.findBook(id) != null)
                                throw new IllegalArgumentException("Không thể thêm danh sách: mã sách " + id + " đã tồn tại.");
                            batch[i] = readBook(id, 0);
                        }
                        session.change(() -> {
                            for (Book book : batch) {
                                if (!bookService.addBook(book))
                                    throw new IllegalArgumentException("Không thể thêm sách " + book.getId() + ".");
                            }
                        });
                        System.out.println("Đã thêm " + count + " sách thành công. Đã lưu dữ liệu.");
                    }
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
        double cost;
        while (true) {
            cost = input.readDouble("Giá nhập (VNĐ, 0 nếu chưa có): ");
            if (cost >= 0) break;
            System.out.println("Giá nhập không được âm. Vui lòng nhập lại.");
        }
        double price;
        while (true) {
            price = input.readDouble("Giá bán (VNĐ, phải lớn hơn giá nhập): ");
            if (price > cost) break;
            System.out.println("Giá bán phải lớn hơn giá nhập. Vui lòng nhập lại.");
        }
        if (bookService.findBook(id) == null) quantity = input.readInt("Số sách cần thêm vào kho (quyển): ");
        return new Book(id, title, author, category, cost, price, quantity);
    }
    private Book readBookUpdate(Book old) {
        ConsoleView.options("SỬA THÔNG TIN SÁCH - CHỌN THÔNG TIN MUỐN SỬA", "Tên sách", "Tác giả", "Thể loại", "Giá nhập", "Giá bán", "Mã sách");
        int field = input.readInt("Chọn thông tin cần sửa: ");
        String id = old.getId(), title = old.getTitle(), author = old.getAuthor(), category = old.getCategory();
        double cost = old.getPurchasePrice(), price = old.getPrice();
        switch (field) {
            case 0 -> { return null; }
            case 1 -> title = input.read("Tên sách mới: ");
            case 2 -> author = input.read("Tác giả mới: ");
            case 3 -> category = input.read("Thể loại mới: ");
            case 4 -> {
                while (true) {
                    cost = input.readDouble("Giá nhập mới: ");
                    if (cost >= 0 && cost < price) break;
                    System.out.println("Giá nhập phải không âm và nhỏ hơn giá bán. Vui lòng nhập lại.");
                }
            }
            case 5 -> {
                while (true) {
                    price = input.readDouble("Giá bán mới: ");
                    if (price > cost) break;
                    System.out.println("Giá bán phải lớn hơn giá nhập. Vui lòng nhập lại.");
                }
            }
            case 6 -> id = input.readNewCode("S", "Mã sách mới: ");
            default -> throw new IllegalArgumentException("Lựa chọn không hợp lệ.");
        }
        if (field != 4 && field != 5 && price <= cost) {
            Book unchangedPrices = new Book(id, title, author, price, old.getQuantity());
            unchangedPrices.setCategory(category);
            unchangedPrices.restorePurchasePrice(cost);
            return unchangedPrices;
        }
        return new Book(id, title, author, category, cost, price, old.getQuantity());
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
        while (true) {
            String id = input.readExistingCode("S", "Nhập mã sách (0 để quay lại): ", "0");
            if (id.equals("0")) return;
            Book book = bookService.findBook(id);
            if (book != null) {
                System.out.println(book);
                return;
            }
            System.out.println("Sai mã sách: " + id + ". Vui lòng nhập lại.");
        }
    }

    private void searchBooks() {
        while (true) {
            String keyword = input.read("Tên hoặc từ khóa (0 để quay lại): ");
            if (keyword.equals("0")) return;
            if (keyword.isBlank()) { System.out.println("Vui lòng nhập tên hoặc từ khóa."); continue; }
            Book[] result = bookService.search(keyword);
            if (result.length > 0) {
                ConsoleView.books(result);
                System.out.println("Tìm kiếm thành công: " + result.length + " sách.");
                return;
            }
            System.out.println("Không có sách khớp với từ khóa. Vui lòng nhập lại.");
        }
    }
    private void filterPriceInteractive() {
        while (true) {
            double min = input.readDouble("Giá thấp nhất (-1 để quay lại): ");
            if (min == -1) return;
            double max = input.readDouble("Giá cao nhất: ");
            try {
                Book[] result = bookService.filterPrice(min, max);
                if (result.length == 0) {
                    System.out.println("Không có sách trong khoảng giá đã chọn. Vui lòng nhập lại.");
                    continue;
                }
                ConsoleView.books(result);
                System.out.println("Lọc khoảng giá thành công: " + result.length + " sách.");
                return;
            } catch (IllegalArgumentException error) {
                System.out.println(error.getMessage() + " Vui lòng nhập lại.");
            }
        }
    }

    public void updateBook() {
        String id = input.read("Mã sách cần sửa: ");
        Book old = bookService.findBook(id);
        if (old == null) {
            System.out.println("Không tìm thấy sách.");
            return;
        }
        System.out.println("Hiện tại: " + old);
        Book replacement = readBookUpdate(old);
        if (replacement == null) return;
        if (!replacement.getId().equals(old.getId()))
            throw new IllegalStateException("Đổi mã sách cần phiên quản lý để kiểm tra hóa đơn và nhật ký kho.");
        bookService.updateBook(replacement);
        System.out.println("Cập nhật thành công.");
    }

    public void deleteBook() {
        String id = input.read("Mã sách cần xóa: ");
        System.out.println(bookService.deleteBook(id)
                ? "Xóa thành công." : "Không tìm thấy sách.");
    }

}
