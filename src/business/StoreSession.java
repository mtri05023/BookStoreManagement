package business;

import data.StoreState;
import data.StoreFile;
import data.StoreTextCodec;
import model.Book;
import java.io.IOException;
import java.time.LocalDate;
import java.util.Objects;

/** One durable command: validate/mutate/save, or restore every in-memory array on failure. */
public final class StoreSession {
    private final StoreState state;
    private final StoreFile file;
    public StoreSession(StoreState state, StoreFile file) {
        this.state = Objects.requireNonNull(state);
        this.file = Objects.requireNonNull(file);
    }
    public StoreState state() { return state; }
    public void save() throws IOException { file.save(state); }
    public void change(Runnable command) {
        var before = StoreTextCodec.encode(state);
        try {
            command.run();
            save();
        } catch (IOException | RuntimeException error) {
            try { state.restore(StoreTextCodec.decode(before)); }
            catch (IOException rollbackError) { throw new IllegalStateException("Không thể khôi phục phiên dữ liệu.", rollbackError); }
            throw new IllegalStateException("Thao tác thất bại, dữ liệu đã được hoàn tác: " + error.getMessage(), error);
        }
    }
    public void deleteBook(String id) {
        if (!new BookService(state.getBooks(), state.getInvoices()).deleteBook(id))
            throw new IllegalArgumentException("Không tìm thấy sách: " + id);
    }
    public void stock(String id, int value, boolean addition, String reason) {
        if (reason == null || reason.isBlank() || reason.contains("|") || reason.contains("\n") || reason.contains("\r"))
            throw new IllegalArgumentException("Phải nhập lý do hợp lệ, không chứa dấu |.");
        Book book = state.getBooks().findById(id);
        if (book == null) throw new IllegalArgumentException("Không tìm thấy sách: " + id);
        int old = book.getQuantity();
        if (addition) new BookService(state.getBooks()).restock(id, value);
        else book.setQuantity(value);
        state.addStockEvent("STOCK|" + LocalDate.now() + "|" + book.getId() + "|" + old + "|" + book.getQuantity() + "|" + reason.trim());
    }
}
