package model;

public class InvoiceDetail {
    private Book book;
    private int quantity;
    private double price;

    public InvoiceDetail(Book book, int quantity) {
        this.book = book;
        this.quantity = quantity;
        this.price = book.getPrice();
    }

    public Book getBook() { return book; }
    public int getQuantity() { return quantity; }
    public double getPrice() { return price; }

    public double getAmount() {
        return quantity * price;
    }
}
