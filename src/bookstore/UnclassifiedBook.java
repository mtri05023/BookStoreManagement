package bookstore;

public final class UnclassifiedBook extends Book {
    public UnclassifiedBook(String id, String title, String author, double price, int quantity) {
        super(id, title, author, price, quantity);
    }

    @Override
    public String getCategory() { return "Chưa phân loại"; }
}
