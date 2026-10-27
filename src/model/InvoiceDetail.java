package model;

import java.util.Objects;

/** Immutable sale line. Price, book ID and title are historical snapshots. */
public class InvoiceDetail {
    private final Book book;
    private final String bookId;
    private final String bookTitle;
    private final int quantity;
    private final double price;

    public InvoiceDetail(Book book, int quantity) {
        this(book, quantity, Objects.requireNonNull(book).getPrice());
    }

    /** Use the saved unit price when loading an existing invoice. */
    public InvoiceDetail(Book book, int quantity, double price) {
        this(book, quantity, price, Objects.requireNonNull(book).getTitle());
    }

    /** Restore the historical title as well as the historical price. */
    public InvoiceDetail(Book book, int quantity, double price, String historicalTitle) {
        this.book = Objects.requireNonNull(book);
        if (quantity <= 0) throw new IllegalArgumentException("Số lượng phải lớn hơn 0.");
        this.quantity = quantity;
        this.price = ModelChecks.price(price);
        this.bookId = book.getId();
        this.bookTitle = ModelChecks.text(historicalTitle, "Tên sách lịch sử");
        if (!Double.isFinite(quantity * price)) throw new IllegalArgumentException("Thành tiền vượt giới hạn.");
    }

    /** Live catalog reference; use snapshot getters for historical output. */
    public Book getBook() { return book; }
    public String getBookId() { return bookId; }
    public String getBookTitle() { return bookTitle; }
    public int getQuantity() { return quantity; }
    public double getPrice() { return price; }
    public double getAmount() { return quantity * price; }
}
