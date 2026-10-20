package data;

import model.Book;
import java.util.ArrayList;
import java.util.List;

public class BookRepository implements ICrudRepository<Book> {
    private final List<Book> books = new ArrayList<>();

    @Override
    public void add(Book book) {
        if (book == null) throw new IllegalArgumentException("Dữ liệu không được null.");
        if (findById(book.getId()) != null) {
            throw new IllegalArgumentException("Mã đã tồn tại: " + book.getId());
        }
        books.add(book);
    }

    @Override
    public List<Book> getAll() { return new ArrayList<>(books); }

    @Override
    public Book findById(String id) {
        if (id == null || id.isBlank()) return null;
        id = id.trim();
        for (Book book : books) {
            if (book.getId().equalsIgnoreCase(id)) return book;
        }
        return null;
    }

    @Override
    public boolean delete(String id) {
        Book book = findById(id);
        return book != null && books.remove(book);
    }
}
