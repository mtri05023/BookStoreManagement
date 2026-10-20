package model;

public class Book {
    private final String id;
    private String title;
    private String author;
    private double price;
    private int quantity;

    public Book(String id, String title, String author, double price, int quantity) {
        this.id = ModelChecks.id(id);
        this.title = ModelChecks.text(title, "Tên sách");
        this.author = ModelChecks.text(author, "Tác giả");
        this.price = ModelChecks.price(price);
        if (quantity < 0) throw new IllegalArgumentException("Tồn kho không được âm.");
        this.quantity = quantity;
    }

    public String getId() { return id; }
    public String getTitle() { return title; }
    public String getAuthor() { return author; }
    public double getPrice() { return price; }
    public int getQuantity() { return quantity; }

    public void setTitle(String title) { this.title = ModelChecks.text(title, "Tên sách"); }
    public void setAuthor(String author) { this.author = ModelChecks.text(author, "Tác giả"); }
    public void setPrice(double price) { this.price = ModelChecks.price(price); }
    public void setQuantity(int quantity) {
        if (quantity < 0) throw new IllegalArgumentException("Tồn kho không được âm.");
        this.quantity = quantity;
    }

    @Override
    public String toString() {
        return String.format("%-8s %-25s %-20s %10.0f %6d",
                id, title, author, price, quantity);
    }
}
