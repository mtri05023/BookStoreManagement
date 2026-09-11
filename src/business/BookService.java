package business;

import data.BookRepository;
import model.Book;
import java.util.List;

public class BookService {
    private BookRepository repository;

    public BookService(BookRepository repository) {
        this.repository = repository;
    }

    public boolean addBook(Book book) {
        if (repository.findById(book.getId()) != null) return false;
        if (book.getPrice() <= 0 || book.getQuantity() < 0) return false;
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
        if (book == null || price <= 0 || quantity < 0) return false;

        book.setTitle(title);
        book.setAuthor(author);
        book.setPrice(price);
        book.setQuantity(quantity);
        return true;
    }

    public boolean sellBook(String id, int quantity) {
        Book book = repository.findById(id);
        if (book == null || quantity <= 0 || book.getQuantity() < quantity) return false;
        book.setQuantity(book.getQuantity() - quantity);
        return true;
    }
}
