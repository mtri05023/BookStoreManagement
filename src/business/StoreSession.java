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
        PermissionService.require(PermissionService.Permission.MANAGE_BOOKS);
        changeInternal(command);
    }
    public void addCustomer(model.Customer customer) {
        PermissionService.require(PermissionService.Permission.MANAGE_CUSTOMERS);
        changeInternal(() -> state.getCustomers().add(customer));
    }
    public void updateCustomer(model.Customer customer) {
        PermissionService.require(PermissionService.Permission.MANAGE_CUSTOMERS);
        changeInternal(() -> {
            if (!state.getPeople().updateById(customer.getId(), customer)) throw new IllegalArgumentException("Không tìm thấy khách hàng.");
        });
    }
    public void deleteCustomer(String id) {
        PermissionService.require(PermissionService.Permission.DELETE_CUSTOMERS);
        changeInternal(() -> {
            if (!new PersonService(state.getPeople(), state.getInvoices()).delete(id)) throw new IllegalArgumentException("Không tìm thấy khách hàng.");
        });
    }
    public void addCustomers(model.Customer[] customers) {
        PermissionService.require(PermissionService.Permission.MANAGE_CUSTOMERS);
        changeInternal(() -> { for (model.Customer customer : customers) state.getCustomers().add(customer); });
    }
    public void replaceCustomers(model.Customer[] customers) {
        PermissionService.require(PermissionService.Permission.MANAGE_EMPLOYEES);
        changeInternal(() -> new PersonService(state.getPeople(), state.getInvoices()).initializeRole("Customer", customers));
    }
    public void payInvoice(InvoiceService invoiceService, model.Invoice quote) {
        PermissionService.require(PermissionService.Permission.CREATE_INVOICE);
        changeInternal(() -> {
            java.util.Map<String, Integer> before = new java.util.LinkedHashMap<>();
            for (var detail : quote.getDetails()) {
                Book book = state.getBooks().findById(detail.getBookId());
                before.put(detail.getBookId(), book.getQuantity());
            }
            invoiceService.payInvoice(quote);
            before.forEach((id, quantity) -> state.addStockEvent("STOCK|" + LocalDate.now() + "|" + id + "|" + quantity + "|"
                    + state.getBooks().findById(id).getQuantity() + "|Bán hàng - hóa đơn " + quote.getId()));
        });
    }
    private void changeInternal(Runnable command) {
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
        PermissionService.require(PermissionService.Permission.MANAGE_BOOKS);
        if (!new BookService(state.getBooks(), state.getInvoices()).deleteBook(id))
            throw new IllegalArgumentException("Không tìm thấy sách: " + id);
    }
    public void stock(String id, int value, boolean addition, String reason) {
        PermissionService.require(PermissionService.Permission.MANAGE_INVENTORY);
        if (reason == null || reason.isBlank() || reason.contains("|") || reason.contains("\n") || reason.contains("\r"))
            throw new IllegalArgumentException("Phải nhập lý do hợp lệ, không chứa dấu |.");
        Book book = state.getBooks().findById(id);
        if (book == null) throw new IllegalArgumentException("Không tìm thấy sách: " + id);
        int old = book.getQuantity();
        if (addition) new BookService(state.getBooks()).restock(id, value);
        else book.setQuantity(value);
        state.addStockEvent("STOCK|" + LocalDate.now() + "|" + book.getId() + "|" + old + "|" + book.getQuantity() + "|" + reason.trim());
    }

    public void receiveStock(String bookId, int amount, String supplierId, String reason) {
        PermissionService.require(PermissionService.Permission.MANAGE_INVENTORY);
        if (reason == null || reason.isBlank() || reason.contains("|") || reason.contains("\n") || reason.contains("\r"))
            throw new IllegalArgumentException("Phải nhập lý do hợp lệ, không chứa dấu |.");
        if (amount <= 0) throw new IllegalArgumentException("Số lượng nhập phải lớn hơn 0.");
        Book book = state.getBooks().findById(bookId);
        if (book == null) throw new IllegalArgumentException("Không tìm thấy sách: " + bookId);
        var supplier = state.getSuppliers().findById(supplierId);
        if (supplier == null) throw new IllegalArgumentException("Không tìm thấy nhà cung cấp: " + supplierId);
        int old = book.getQuantity();
        try { book.setQuantity(Math.addExact(old, amount)); }
        catch (ArithmeticException e) { throw new IllegalArgumentException("Tồn kho vượt giới hạn.", e); }
        state.addStockEvent("RECEIPT|" + LocalDate.now() + "|" + book.getId() + "|" + old + "|"
                + book.getQuantity() + "|" + supplier.getId() + "|" + supplier.getName() + "|" + reason.trim());
    }

}
