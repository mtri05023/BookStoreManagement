package business;

import data.IRepository;
import model.Invoice;
import model.InvoiceDetail;
import java.util.LinkedHashMap;
import java.util.Map;

public class ReportService {
    private final IRepository<Invoice> repository;

    public ReportService(IRepository<Invoice> repository) {
        this.repository = java.util.Objects.requireNonNull(repository);
    }

    public model.Invoice[] filter(java.time.LocalDate from, java.time.LocalDate to) {
        if (from == null || to == null || from.isAfter(to)) throw new IllegalArgumentException("Khoảng ngày không hợp lệ.");
        return repository.getAll().stream().filter(Invoice::isCompleted)
                .filter(i -> !i.getPaymentDate().isBefore(from) && !i.getPaymentDate().isAfter(to)).toArray(Invoice[]::new);
    }
    public double getRevenue(java.time.LocalDate from, java.time.LocalDate to) {
        return java.util.Arrays.stream(filter(from, to)).mapToDouble(Invoice::getTotal).sum();
    }
    public java.util.Map<String, Double> revenueByCustomer(java.time.LocalDate from, java.time.LocalDate to) {
        java.util.Map<String, Double> result = new LinkedHashMap<>();
        for (Invoice i : filter(from, to)) result.merge(i.getCustomer().getId(), i.getTotal(), Double::sum);
        return result;
    }
    public double getRevenue() {
        return repository.getAll().stream().filter(Invoice::isCompleted).mapToDouble(Invoice::getTotal).sum();
    }

    public long getTotalQuantitySold() {
        return repository.getAll().stream()
                .filter(Invoice::isCompleted)
                .flatMap(invoice -> invoice.getDetails().stream())
                .mapToLong(InvoiceDetail::getQuantity).sum();
    }

    public Map<String, Long> getQuantitySoldByBook() {
        Map<String, Long> result = new LinkedHashMap<>();
        for (Invoice invoice : repository.getAll()) {
            if (!invoice.isCompleted()) continue;
            for (InvoiceDetail detail : invoice.getDetails()) {
                result.merge(detail.getBookId(), (long) detail.getQuantity(), Long::sum);
            }
        }
        return result;
    }
}
