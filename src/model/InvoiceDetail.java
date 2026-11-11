package model;

import java.util.Objects;

public class InvoiceDetail {
    private final Book book;
    private final String bookId;
    private final String bookTitle;
    private final int quantity;
    private final double price;

    public InvoiceDetail(Book book, int quantity) {
        this(book, quantity, Objects.requireNonNull(book).getPrice());
    }

    public InvoiceDetail(Book book, int quantity, double price) {
        this(book, quantity, price, Objects.requireNonNull(book).getTitle());
    }

    public InvoiceDetail(Book book, int quantity, double price, String historicalTitle) {
        this.book = Objects.requireNonNull(book);
        if (quantity <= 0) throw new IllegalArgumentException("So luong phai lon hon 0.");
        this.quantity = quantity;
        this.price = ModelChecks.price(price);
        this.bookId = book.getId();
        this.bookTitle = ModelChecks.text(historicalTitle, "Ten sach lich su");
        if (!Double.isFinite(quantity * price)) throw new IllegalArgumentException("Thanh tien khong hop le.");
    }
    public Book getBook() { return book; }
    public String getBookId() { return bookId; }
    public String getBookTitle() { return bookTitle; }
    public int getQuantity() { return quantity; }
    public double getPrice() { return price; }
    public double getAmount() { return quantity * price; }
}
