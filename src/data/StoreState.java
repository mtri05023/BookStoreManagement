package data;

/** One session; the two typed person repositories share one authoritative array. */
public class StoreState {
    private final PersonDirectory people;
    private final BookRepository books;
    private final CustomerRepository customers;
    private final EmployeeRepository employees;
    private final InvoiceRepository invoices;

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
