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
    public void show() {
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
