package bookstore;

public abstract class Book implements Identifiable {
    private final String id;
    private String title;
    private String author;
    private double price;
    private int quantity;

    private double purchasePrice;

    public Book(String id, String title, String author, double price, int quantity) {
        this.id = InputHelper.id(id);
        this.title = InputHelper.text(title, "Tên sách");
        this.author = InputHelper.text(author, "Tác giả");
        this.price = InputHelper.price(price);
        if (quantity < 0) throw new IllegalArgumentException("Tồn kho không được âm.");
        this.quantity = quantity;
    }

    public abstract String getCategory();
    public double getPurchasePrice() { return purchasePrice; }
    public void setPurchasePrice(double value) {
        setPrices(value, price);
    }
    public void setPrices(double purchasePrice, double salePrice) {
        double validSalePrice = InputHelper.price(salePrice);
        if (!Double.isFinite(purchasePrice) || purchasePrice < 0)
            throw new IllegalArgumentException("Giá nhập phải hữu hạn và không âm.");
        if (validSalePrice <= purchasePrice)
            throw new IllegalArgumentException("Giá bán phải lớn hơn giá nhập.");
        this.purchasePrice = purchasePrice;
        this.price = validSalePrice;
    }
    /** Preserve historical rows while requiring corrected prices on the next price edit. */
    public void restorePurchasePrice(double value) {
        if (!Double.isFinite(value) || value < 0) throw new IllegalArgumentException("Giá nhập phải hữu hạn và không âm.");
        purchasePrice = value;
    }
    public String getId() { return id; }
    public String getTitle() { return title; }
    public String getAuthor() { return author; }
    public double getPrice() { return price; }
    public int getQuantity() { return quantity; }

    public void setTitle(String title) { this.title = InputHelper.text(title, "Tên sách"); }
    public void setAuthor(String author) { this.author = InputHelper.text(author, "Tác giả"); }
    public void setPrice(double price) { setPrices(purchasePrice, price); }
    public void setQuantity(int quantity) {
        if (quantity < 0) throw new IllegalArgumentException("Tồn kho không được âm.");
        this.quantity = quantity;
    }

    @Override
    public String toString() {
        return String.format("%-8s %-25s %-20s %10.0f %6d",
                id, title, author, price, quantity);
    }
    public static Book create(String id, String title, String author, double price, int quantity) {
        return new UnclassifiedBook(id, title, author, price, quantity);
    }

    public static Book restore(String id, String title, String author, String category, double price, int quantity) {
        String label = InputHelper.text(category, "Thể loại");
        if (label.equals("Chưa phân loại")) return create(id, title, author, price, quantity);
        return new CategorizedBook(id, title, author, label, price, quantity);
    }

    public static Book create(String id, String title, String author, String category,
                              double cost, double price, int quantity) {
        Book book = restore(id, title, author, category, price, quantity);
        book.setPrices(cost, price);
        return book;
    }
}
