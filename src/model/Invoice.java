package model;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class Invoice {
    private final String id;
    private final Customer customer;
    private final Employee employee;
    private final String customerName;
    private final String employeeName;
    private final String customerPhone;
    private final String employeePosition;
    private final LocalDate createdDate;
    private final List<InvoiceDetail> details = new ArrayList<>();
    private boolean completed;

    public Invoice(String id, Customer customer, Employee employee) {
        this(id, customer, employee, LocalDate.now());
    }

    public Invoice(String id, Customer customer, Employee employee, LocalDate createdDate) {
        this.id = ModelChecks.id(id);
        this.customer = Objects.requireNonNull(customer);
        this.employee = Objects.requireNonNull(employee);
        this.createdDate = Objects.requireNonNull(createdDate);
        this.customerName = customer.getName();
        this.employeeName = employee.getName();
        this.customerPhone = customer.getPhone();
        this.employeePosition = employee.getPosition();
    }

    public String getId() { return id; }
    public Customer getCustomer() { return customer; }
    public Employee getEmployee() { return employee; }
    public String getCustomerName() { return customerName; }
    public String getEmployeeName() { return employeeName; }
    public String getCustomerPhone() { return customerPhone; }
    public String getEmployeePosition() { return employeePosition; }
    public LocalDate getCreatedDate() { return createdDate; }
    public boolean isCompleted() { return completed; }
    public List<InvoiceDetail> getDetails() { return List.copyOf(details); }

    public void addDetail(InvoiceDetail detail) {
        if (completed) throw new IllegalStateException("Hóa đơn đã hoàn tất.");
        Objects.requireNonNull(detail);
        if (!Double.isFinite(getTotal() + detail.getAmount())) {
            throw new IllegalArgumentException("Tổng hóa đơn vượt giới hạn.");
        }
        details.add(detail);
    }

    public void complete() {
        if (details.isEmpty()) throw new IllegalStateException("Hóa đơn không được rỗng.");
        completed = true;
    }

    public double getTotal() {
        double total = 0;
        for (InvoiceDetail detail : details) total += detail.getAmount();
        return total;
    }
}
