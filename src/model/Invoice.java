package model;

import java.time.LocalDate;
import java.time.LocalDateTime;
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
    public enum Status { DRAFT, PAID, CANCELLED }
    private Status status = Status.DRAFT;
    private final LocalDateTime createdAt;
    private LocalDateTime paidAt;

    public Invoice(String id, Customer customer, Employee employee) {
        this(id, customer, employee, LocalDateTime.now());
    }

    public Invoice(String id, Customer customer, Employee employee, LocalDate createdDate) {
        this(id, customer, employee, Objects.requireNonNull(createdDate).atStartOfDay());
    }

    public Invoice(String id, Customer customer, Employee employee, LocalDateTime createdAt) {
        this.id = ModelChecks.id(id);
        this.customer = Objects.requireNonNull(customer);
        this.employee = Objects.requireNonNull(employee);
        this.createdAt = Objects.requireNonNull(createdAt);
        this.createdDate = createdAt.toLocalDate();
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
    public boolean isCompleted() { return status == Status.PAID; }
    public Status getStatus() { return status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getPaidAt() { return paidAt; }
    public LocalDate getPaymentDate() { return paidAt == null ? createdDate : paidAt.toLocalDate(); }
    public List<InvoiceDetail> getDetails() { return List.copyOf(details); }

    public void addDetail(InvoiceDetail detail) {
        if (status != Status.DRAFT) throw new IllegalStateException("Chỉ được sửa hóa đơn đang lập.");
        Objects.requireNonNull(detail);
        if (!Double.isFinite(getTotal() + detail.getAmount())) {
            throw new IllegalArgumentException("Tổng hóa đơn vượt giới hạn.");
        }
        details.add(detail);
    }

    public void complete() {
        if (status == Status.PAID) return;
        if (status == Status.CANCELLED) throw new IllegalStateException("Không được thanh toán hóa đơn đã hủy.");
        if (details.isEmpty()) throw new IllegalStateException("Hóa đơn không được rỗng.");
        if (!Double.isFinite(getTotal()) || getTotal() <= 0) throw new IllegalStateException("Tổng tiền không hợp lệ.");
        LocalDateTime time = LocalDateTime.now();
        if (time.isBefore(createdAt)) throw new IllegalStateException("Ngày giờ lập hóa đơn nằm trong tương lai.");
        paidAt = time;
        status = Status.PAID;
    }

    public void cancel() {
        if (status == Status.PAID) throw new IllegalStateException("Không thể hủy hóa đơn đã thanh toán bằng thao tác hủy giỏ.");
        status = Status.CANCELLED;
    }

    /** Loading old paid invoices must not invent a historical payment time. */
    public void restorePaidAt(LocalDateTime time) {
        if (status != Status.DRAFT || details.isEmpty()) throw new IllegalStateException("Trạng thái hóa đơn lưu trữ không hợp lệ.");
        if (time != null && time.isBefore(createdAt)) throw new IllegalArgumentException("Thanh toán trước thời điểm lập hóa đơn.");
        paidAt = time;
        status = Status.PAID;
    }

    public double getTotal() {
        double total = 0;
        for (InvoiceDetail detail : details) total += detail.getAmount();
        return total;
    }
}
