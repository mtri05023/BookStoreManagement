package bookstore;

import java.io.IOException;
import java.time.LocalDate;
import java.util.Map;

public class BookStore {
    private final BookList books;
    private final CustomerList customers;
    private final EmployeeList employees;
    private final InvoiceList invoices;
    private final SupplierList suppliers;

    private String[] stockEvents = new String[4];
    private int eventCount;
    private int lowStockThreshold = 5;
    public int getLowStockThreshold() { return lowStockThreshold; }
    public void setLowStockThreshold(int value) {
        if (value < 0) throw new IllegalArgumentException("Ngưỡng cảnh báo không được âm.");
        lowStockThreshold = value;
    }
    public String[] getStockEvents() { return java.util.Arrays.copyOf(stockEvents, eventCount); }
    public void addStockEvent(String row) {
        if (eventCount == stockEvents.length) stockEvents = java.util.Arrays.copyOf(stockEvents, stockEvents.length * 2);
        stockEvents[eventCount++] = row;
    }
    public void restore(BookStore snapshot) {
        customers.replaceAll(snapshot.customers.getAll().toArray(new Customer[0]));
        employees.replaceAll(snapshot.employees.getAll().toArray(new Employee[0]));
        books.replaceAll(snapshot.getBooks().getAll().toArray(new Book[0]));
        invoices.replaceAll(snapshot.getInvoices().getAll().toArray(new Invoice[0]));
        suppliers.replaceAll(snapshot.getSuppliers().getAll().toArray(new Supplier[0]));
        stockEvents = java.util.Arrays.copyOf(snapshot.getStockEvents(), Math.max(4, snapshot.eventCount));
        eventCount = snapshot.eventCount;
        lowStockThreshold = snapshot.lowStockThreshold;
    }
    public BookStore() {
        books = new BookList();
        customers = new CustomerList();
        employees = new EmployeeList();
        invoices = new InvoiceList();
        invoices.bind(books, customers, employees);
        suppliers = new SupplierList();
    }

    public BookList getBooks() { return books; }
    public CustomerList getCustomers() { return customers; }
    public EmployeeList getEmployees() { return employees; }
    public InvoiceList getInvoices() { return invoices; }
    public SupplierList getSuppliers() { return suppliers; }
    private StoreFile file;
    void useFile(StoreFile file) { this.file = file; }
    public void save() throws IOException { file.save(this); }
    public void change(Runnable command) {
        AccountList.require(AccountList.Permission.MANAGE_BOOKS);
        changeInternal(command);
    }
    public void addCustomer(Customer customer) {
        AccountList.require(AccountList.Permission.MANAGE_CUSTOMERS);
        changeInternal(() -> getCustomers().add(customer));
    }
    public void updateCustomer(Customer customer) {
        AccountList.require(AccountList.Permission.MANAGE_CUSTOMERS);
        changeInternal(() -> {
            if (!customers.updateById(customer.getId(), customer)) throw new IllegalArgumentException("Không tìm thấy khách hàng.");
        });
    }
    public void renameCustomer(String oldId, Customer replacement) {
        AccountList.require(AccountList.Permission.MANAGE_CUSTOMERS);
        changeInternal(() -> {
            if (getCustomers().findById(oldId) == null) throw new IllegalArgumentException("Không tìm thấy khách hàng.");
            if (customers.findById(replacement.getId()) != null)
                throw new IllegalArgumentException("Mã khách hàng đã tồn tại.");
            for (var invoice : getInvoices().getAll())
                if (invoice.getCustomer().getId().equalsIgnoreCase(oldId))
                    throw new IllegalStateException("Không thể đổi mã khách hàng đã phát sinh hóa đơn.");
            var people = customers.getAll().toArray(new Customer[0]);
            for (int i = 0; i < people.length; i++)
                if (people[i].getId().equalsIgnoreCase(oldId)) people[i] = replacement;
            customers.replaceAll(people);
        });
    }
    public void renameBook(String oldId, Book replacement) {
        AccountList.require(AccountList.Permission.MANAGE_BOOKS);
        changeInternal(() -> {
            if (getBooks().findById(oldId) == null) throw new IllegalArgumentException("Không tìm thấy sách.");
            if (getBooks().findById(replacement.getId()) != null)
                throw new IllegalArgumentException("Mã sách đã tồn tại.");
            for (var invoice : getInvoices().getAll())
                for (var detail : invoice.getDetails())
                    if (detail.getBookId().equalsIgnoreCase(oldId))
                        throw new IllegalStateException("Không thể đổi mã sách đã phát sinh hóa đơn.");
            for (String row : getStockEvents()) {
                String[] fields = row.split("\\|", -1);
                if (fields.length > 2 && ((fields[0].equals("SOURCE") && fields[1].equalsIgnoreCase(oldId))
                        || (!fields[0].equals("SOURCE") && fields[2].equalsIgnoreCase(oldId))))
                    throw new IllegalStateException("Không thể đổi mã sách đã có nhật ký kho/nguồn cung.");
            }
            var books = getBooks().getAll().toArray(new Book[0]);
            for (int i = 0; i < books.length; i++)
                if (books[i].getId().equalsIgnoreCase(oldId)) books[i] = replacement;
            getBooks().replaceAll(books);
        });
    }
    public void deleteCustomer(String id) {
        AccountList.require(AccountList.Permission.DELETE_CUSTOMERS);
        changeInternal(() -> {
            if (!deletePerson(id, true)) throw new IllegalArgumentException("Không tìm thấy khách hàng.");
        });
    }
    public void addCustomers(Customer[] customers) {
        AccountList.require(AccountList.Permission.MANAGE_CUSTOMERS);
        changeInternal(() -> { for (Customer customer : customers) getCustomers().add(customer); });
    }
    public void payInvoice(InvoiceList invoiceService, Invoice quote) {
        AccountList.require(AccountList.Permission.CREATE_INVOICE);
        changeInternal(() -> {
            java.util.Map<String, Integer> before = new java.util.LinkedHashMap<>();
            for (var detail : quote.getDetails()) {
                Book book = getBooks().findById(detail.getBookId());
                before.put(detail.getBookId(), book.getQuantity());
            }
            invoiceService.payInvoice(quote);
            before.forEach((id, quantity) -> addStockEvent("STOCK|" + LocalDate.now() + "|" + id + "|" + quantity + "|"
                    + getBooks().findById(id).getQuantity() + "|Bán hàng - hóa đơn " + quote.getId()));
        });
    }
    private void changeInternal(Runnable command) {
        var before = StoreFile.encode(this);
        try {
            command.run();
            save();
        } catch (IOException | RuntimeException error) {
            try { restore(StoreFile.decode(before)); }
            catch (IOException rollbackError) { throw new IllegalStateException("Không thể khôi phục phiên dữ liệu.", rollbackError); }
            throw new IllegalStateException("Thao tác thất bại, dữ liệu đã được hoàn tác: " + error.getMessage(), error);
        }
    }
    public void deleteBook(String id) {
        AccountList.require(AccountList.Permission.MANAGE_BOOKS);
        if (!books.deleteBook(id, invoices))
            throw new IllegalArgumentException("Không tìm thấy sách: " + id);
    }
    public void stock(String id, int value, boolean addition, String reason) {
        AccountList.require(AccountList.Permission.MANAGE_INVENTORY);
        if (reason == null || reason.isBlank() || reason.contains("|") || reason.contains("\n") || reason.contains("\r"))
            throw new IllegalArgumentException("Phải nhập lý do hợp lệ, không chứa dấu |.");
        Book book = getBooks().findById(id);
        if (book == null) throw new IllegalArgumentException("Không tìm thấy sách: " + id);
        int old = book.getQuantity();
        if (addition) books.restock(id, value);
        else book.setQuantity(value);
        addStockEvent("STOCK|" + LocalDate.now() + "|" + book.getId() + "|" + old + "|" + book.getQuantity() + "|" + reason.trim());
    }

    public void receiveStock(String bookId, int amount, String supplierId, String reason) {
        AccountList.require(AccountList.Permission.MANAGE_INVENTORY);
        if (reason == null || reason.isBlank() || reason.contains("|") || reason.contains("\n") || reason.contains("\r"))
            throw new IllegalArgumentException("Phải nhập lý do hợp lệ, không chứa dấu |.");
        if (amount <= 0) throw new IllegalArgumentException("Số lượng nhập phải lớn hơn 0.");
        Book book = getBooks().findById(bookId);
        if (book == null) throw new IllegalArgumentException("Không tìm thấy sách: " + bookId);
        var supplier = getSuppliers().findById(supplierId);
        if (supplier == null) throw new IllegalArgumentException("Không tìm thấy nhà cung cấp: " + supplierId);
        int old = book.getQuantity();
        try { book.setQuantity(Math.addExact(old, amount)); }
        catch (ArithmeticException e) { throw new IllegalArgumentException("Tồn kho vượt giới hạn.", e); }
        addStockEvent("RECEIPT|" + LocalDate.now() + "|" + book.getId() + "|" + old + "|"
                + book.getQuantity() + "|" + supplier.getId() + "|" + supplier.getName() + "|" + reason.trim());
    }
    boolean deletePerson(String id, boolean customer) {
        AccountList.require(customer ? AccountList.Permission.DELETE_CUSTOMERS : AccountList.Permission.MANAGE_EMPLOYEES);
        if (id == null) return false;
        String key = id.trim();
        for (Invoice invoice : invoices.getAll()) {
            if (invoice.getCustomer().getId().equalsIgnoreCase(key)
                    || invoice.getEmployee().getId().equalsIgnoreCase(key)) {
                String type = invoice.getCustomer().getId().equalsIgnoreCase(key) ? "khách hàng" : "nhân viên";
                throw new IllegalStateException("Không thể xóa " + type + " có mã " + key + " vì đã phát sinh hóa đơn.");
            }
        }
        return customer ? customers.delete(key) : employees.delete(key);
    }
    public static BookStore create() {
        BookStore state = new BookStore();
        state.getBooks().add(Book.create("S001", "Java Core", "Tác giả mẫu", "Lập trình", 100000, 150000, 20));
        state.getBooks().add(Book.create("S002", "Lập trình OOP", "Tác giả mẫu", "Lập trình", 65000, 100000, 10));
        state.getCustomers().add(new Customer("KH001", "Khách hàng mẫu", "0900000000"));
        state.getCustomers().add(new Customer("KH002", "Khách hàng thứ hai", "0910000000"));
        state.getEmployees().add(new Employee(StoreFile.DEFAULT_EMPLOYEE_ID, "Nhân viên mẫu", "Thu ngân"));
        addDefaultSuppliers(state);
        return state;
    }

    public static void addDefaultSuppliers(BookStore state) {
        state.getSuppliers().add(new Supplier("NCC001", "Nhà sách Minh Châu", "0900000201"));
        state.getSuppliers().add(new Supplier("NCC002", "Công ty Sách Ánh Dương", "0900000202"));
        state.getSuppliers().add(new Supplier("NCC003", "Nhà phân phối Tri Thức", "0900000203"));
    }
}
