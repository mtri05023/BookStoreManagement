package business;

import data.ICrudRepository;
import model.Book;
import java.util.List;

public class BookService {
    private final ICrudRepository<Book> repository;
    private data.IRepository<model.Invoice> invoices;

    public BookService(ICrudRepository<Book> repository) {
        this.repository = java.util.Objects.requireNonNull(repository);
    }

    public BookService(ICrudRepository<Book> repository, data.IRepository<model.Invoice> invoices) {
        this(repository);
        this.invoices = java.util.Objects.requireNonNull(invoices);
    }

    public boolean addBook(Book book) {
        if (book == null) return false;
        if (repository.findById(book.getId()) != null) return false;
        if (!Double.isFinite(book.getPrice()) || book.getPrice() <= 0 || book.getQuantity() < 0) return false;
        repository.add(book);
        return true;
    }

    public List<Book> getBooks() {
        return repository.getAll();
    }

    public Book findBook(String id) {
        return repository.findById(id);
    }

    public boolean deleteBook(String id) {
        if (invoices != null && id != null) {
            for (model.Invoice invoice : invoices.getAll())
                for (var detail : invoice.getDetails())
                    if (detail.getBookId().equalsIgnoreCase(id.trim()))
                        throw new IllegalStateException("Không thể xóa sách vì đã phát sinh giao dịch.");
        }
        return repository.delete(id);
    }

    public void updateBook(Book replacement) {
        Book current = findBook(replacement.getId());
        if (current == null) throw new IllegalArgumentException("Không tìm thấy sách: " + replacement.getId());
        // The replacement is fully validated by its constructor before changing any field.
        current.setTitle(replacement.getTitle());
        current.setAuthor(replacement.getAuthor());
        current.setCategory(replacement.getCategory());
        current.setPurchasePrice(replacement.getPurchasePrice());
        current.setPrice(replacement.getPrice());
        // Stock is changed separately through an audited inventory command.
    }

    public boolean updateBook(String id, String title, String author,
                              double price, int quantity) {
        Book book = repository.findById(id);
        if (book == null || !Double.isFinite(price) || price <= 0 || quantity < 0
                || !validText(title) || !validText(author)) return false;

        book.setTitle(title);
        book.setAuthor(author);
        book.setPrice(price);
        book.setQuantity(quantity);
        return true;
    }

    public Book[] search(String keyword) {
        String key = keyword.trim().toLowerCase(java.util.Locale.ROOT);
        return repository.getAll().stream().filter(b -> (b.getId() + " " + b.getTitle() + " " + b.getAuthor())
                .toLowerCase(java.util.Locale.ROOT).contains(key)).toArray(Book[]::new);
    }
    public Book[] filterCategory(String category) {
        return repository.getAll().stream().filter(b -> b.getCategory().equalsIgnoreCase(category.trim())).toArray(Book[]::new);
    }
    public Book[] filterPrice(double min, double max) {
        if (!Double.isFinite(min) || !Double.isFinite(max) || min < 0 || min > max)
            throw new IllegalArgumentException("Khoảng giá không hợp lệ.");
        return repository.getAll().stream().filter(b -> b.getPrice() >= min && b.getPrice() <= max).toArray(Book[]::new);
    }
    public Book[] sorted(int mode) {
        Book[] result = repository.getAll().toArray(new Book[0]);
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
