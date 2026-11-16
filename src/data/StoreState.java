package data;

/** One session; the two typed person repositories share one authoritative array. */
public class StoreState {
    private final PersonDirectory people;
    private final BookRepository books;
    private final CustomerRepository customers;
    private final EmployeeRepository employees;
    private final InvoiceRepository invoices;

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
    public void restore(StoreState snapshot) {
        people.initialize(snapshot.getPeople().toArray());
        books.replaceAll(snapshot.getBooks().getAll().toArray(new model.Book[0]));
        invoices.replaceAll(snapshot.getInvoices().getAll().toArray(new model.Invoice[0]));
        stockEvents = java.util.Arrays.copyOf(snapshot.getStockEvents(), Math.max(4, snapshot.eventCount));
        eventCount = snapshot.eventCount;
        lowStockThreshold = snapshot.lowStockThreshold;
    }
    public StoreState() {
        people = new PersonDirectory();
        books = new BookRepository();
        customers = new CustomerRepository(people);
        employees = new EmployeeRepository(people);
        invoices = new InvoiceRepository();
    }

    public PersonDirectory getPeople() { return people; }
    public BookRepository getBooks() { return books; }
    public CustomerRepository getCustomers() { return customers; }
    public EmployeeRepository getEmployees() { return employees; }
    public InvoiceRepository getInvoices() { return invoices; }
}
