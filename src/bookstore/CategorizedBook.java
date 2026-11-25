package bookstore;

public final class CategorizedBook extends Book {
    private final String category;

    public CategorizedBook(String id, String title, String author, String category, double price, int quantity) {
        super(id, title, author, price, quantity);
        this.category = InputHelper.text(category, "Thể loại");
    }

    @Override
    public String getCategory() { return category; }
}
