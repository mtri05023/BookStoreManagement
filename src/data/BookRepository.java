package data;

import model.Book;
import java.util.ArrayList;
import java.util.List;

public class BookRepository {
    private List<Book> books = new ArrayList<>();

    public void add(Book book) { books.add(book); }

    public List<Book> getAll() { return books; }

    public Book findById(String id) {
        for (Book book : books) {
            if (book.getId().equalsIgnoreCase(id)) return book;
        }
        return null;
    }

    public boolean delete(String id) {
        Book book = findById(id);
        return book != null && books.remove(book);
    }
}
