package business;

import data.ICrudRepository;
import model.Book;
import java.util.List;

public class BookService {
    private final ICrudRepository<Book> repository;

    public BookService(ICrudRepository<Book> repository) {
        this.repository = java.util.Objects.requireNonNull(repository);
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
        return repository.delete(id);
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

    private boolean validText(String value) {
        return value != null && !value.isBlank() && !value.contains("|")
                && !value.contains("\r") && !value.contains("\n");
    }
}
