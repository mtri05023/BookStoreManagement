package data;

import model.Book;
import java.util.ArrayList;
import java.util.List;
import java.util.Arrays;

public class BookRepository implements ICrudRepository<Book> {
    private Book[] books;
    private int size;

    public BookRepository() { books = new Book[4]; }

    @Override
    public void add(Book book) {
        if (book == null) throw new IllegalArgumentException("Dữ liệu không được null.");
        if (findById(book.getId()) != null) {
            throw new IllegalArgumentException("Mã đã tồn tại: " + book.getId());
        }
        if (size == books.length) books = Arrays.copyOf(books, books.length * 2);
        books[size++] = book;
    }

    @Override
    public List<Book> getAll() {
        return new ArrayList<>(Arrays.asList(Arrays.copyOf(books, size)));
    }

    @Override
    public Book findById(String id) {
        if (id == null || id.isBlank()) return null;
        id = id.trim();
        for (int i = 0; i < size; i++) {
            Book book = books[i];
            if (book.getId().equalsIgnoreCase(id)) return book;
        }
        return null;
    }

    @Override
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
}
