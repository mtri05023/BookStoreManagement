package model;

import java.util.ArrayList;
import java.util.List;

public class Invoice {
    private String id;
    private Customer customer;
    private Employee employee;
    private List<InvoiceDetail> details = new ArrayList<>();

    public Invoice(String id, Customer customer, Employee employee) {
        this.id = id;
        this.customer = customer;
        this.employee = employee;
    }

    public String getId() { return id; }
    public Customer getCustomer() { return customer; }
    public Employee getEmployee() { return employee; }
    public List<InvoiceDetail> getDetails() { return details; }

    public void addDetail(InvoiceDetail detail) {
        details.add(detail);
    }

    public double getTotal() {
        double total = 0;
        for (InvoiceDetail detail : details) {
            total += detail.getAmount();
        }
        return total;
    }
}
