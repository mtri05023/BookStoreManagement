package bookstore;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class BookList {
    private Book[] books;
    private int size;

    public BookList() { books = new Book[4]; }

    public void replaceAll(Book[] values) {
        books = Arrays.copyOf(values, Math.max(4, values.length), Book[].class);
        size = values.length;
    }

    public void add(Book book) {
        if (book == null) throw new IllegalArgumentException("Dữ liệu không được null.");
        if (findById(book.getId()) != null) {
            throw new IllegalArgumentException("Mã đã tồn tại: " + book.getId());
        }
        if (size == books.length) books = Arrays.copyOf(books, books.length * 2);
        books[size++] = book;
    }

    public List<Book> getAll() {
        return new ArrayList<>(Arrays.asList(Arrays.copyOf(books, size)));
    }

    public Book findById(String id) {
        int index = Identifiable.indexOf(books, size, id);
        return index < 0 ? null : books[index];
    }

    public boolean delete(String id) {
        Book book = findById(id);
        if (book == null) return false;
        for (int i = 0; i < size; i++) {
            if (books[i] == book) {
                for (int j = i; j < size - 1; j++) books[j] = books[j + 1];
                books[--size] = null;
                return true;
            }
        }
        return false;
    }
    public boolean addBook(Book book) {
        AccountList.require(AccountList.Permission.MANAGE_BOOKS);
        if (book == null) return false;
        if (findById(book.getId()) != null) return false;
        if (!Double.isFinite(book.getPrice()) || book.getPrice() <= 0 || book.getQuantity() < 0) return false;
        add(book);
        return true;
    }

    public List<Book> getBooks() {
        AccountList.require(AccountList.Permission.READ_BOOKS);
        return getAll();
    }

    public Book findBook(String id) {
        AccountList.require(AccountList.Permission.READ_BOOKS);
        return findById(id);
    }

    public boolean deleteBook(String id, InvoiceList invoices) {
        AccountList.require(AccountList.Permission.MANAGE_BOOKS);
        if (invoices != null && id != null) {
            for (Invoice invoice : invoices.getAll())
                for (var detail : invoice.getDetails())
                    if (detail.getBookId().equalsIgnoreCase(id.trim()))
                        throw new IllegalStateException("Không thể xóa sách vì đã phát sinh giao dịch.");
        }
        return delete(id);
    }

    public void updateBook(Book replacement) {
        AccountList.require(AccountList.Permission.MANAGE_BOOKS);
        Book current = findBook(replacement.getId());
        if (current == null) throw new IllegalArgumentException("Không tìm thấy sách: " + replacement.getId());
        replacement.setQuantity(current.getQuantity());
        for (int i = 0; i < size; i++) if (books[i] == current) {
            books[i] = replacement;
            return;
        }
    }

    public boolean updateBook(String id, String title, String author,
                              double price, int quantity) {
        AccountList.require(AccountList.Permission.MANAGE_BOOKS);
        Book book = findById(id);
        if (book == null || !Double.isFinite(price) || price <= book.getPurchasePrice() || quantity < 0
                || !validText(title) || !validText(author)) return false;

        book.setTitle(title);
        book.setAuthor(author);
        book.setPrice(price);
        book.setQuantity(quantity);
        return true;
    }

    public java.util.Map<String, Integer> countByCategory() {
        AccountList.require(AccountList.Permission.READ_BOOKS);
        var counts = new java.util.LinkedHashMap<String, Integer>();
        for (int i = 0; i < size; i++) counts.merge(books[i].getCategory(), 1, Integer::sum);
        return counts;
    }

    public Book[] search(String keyword) {
        AccountList.require(AccountList.Permission.READ_BOOKS);
        String key = keyword.trim().toLowerCase(java.util.Locale.ROOT);
        return getAll().stream().filter(b -> (b.getId() + " " + b.getTitle() + " " + b.getAuthor())
                .toLowerCase(java.util.Locale.ROOT).contains(key)).toArray(Book[]::new);
    }
    public Book[] filterCategory(String category) {
        AccountList.require(AccountList.Permission.READ_BOOKS);
        return getAll().stream().filter(b -> b.getCategory().equalsIgnoreCase(category.trim())).toArray(Book[]::new);
    }
    public Book[] filterPrice(double min, double max) {
        AccountList.require(AccountList.Permission.READ_BOOKS);
        if (!Double.isFinite(min) || !Double.isFinite(max) || min < 0 || min > max)
            throw new IllegalArgumentException("Khoảng giá không hợp lệ.");
        return getAll().stream().filter(b -> b.getPrice() >= min && b.getPrice() <= max).toArray(Book[]::new);
    }
    public Book[] sorted(int mode) {
        AccountList.require(AccountList.Permission.READ_BOOKS);
        Book[] result = getAll().toArray(new Book[0]);
        java.util.Comparator<Book> comparator = switch (mode) {
            case 1 -> java.util.Comparator.comparing(Book::getTitle, String.CASE_INSENSITIVE_ORDER);
            case 2 -> java.util.Comparator.comparingDouble(Book::getPrice);
            case 3 -> java.util.Comparator.comparingInt(Book::getQuantity);
            default -> throw new IllegalArgumentException("Kiểu sắp xếp không hợp lệ.");
        };
        java.util.Arrays.sort(result, comparator);
        return result;
    }
    public void restock(String id, int amount) {
        AccountList.require(AccountList.Permission.MANAGE_INVENTORY);
        if (amount <= 0) throw new IllegalArgumentException("Số lượng nhập phải lớn hơn 0.");
        Book book = findBook(id);
        if (book == null) throw new IllegalArgumentException("Không tìm thấy sách: " + id);
        try { book.setQuantity(Math.addExact(book.getQuantity(), amount)); }
        catch (ArithmeticException e) { throw new IllegalArgumentException("Tồn kho vượt giới hạn.", e); }
    }
    private boolean validText(String value) {
        return value != null && !value.isBlank() && !value.contains("|")
                && !value.contains("\r") && !value.contains("\n");
    }
}
