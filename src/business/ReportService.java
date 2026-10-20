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

    public double getRevenue() {
        return repository.getAll().stream().mapToDouble(Invoice::getTotal).sum();
    }

    public long getTotalQuantitySold() {
        return repository.getAll().stream()
                .flatMap(invoice -> invoice.getDetails().stream())
                .mapToLong(InvoiceDetail::getQuantity).sum();
    }

    public Map<String, Long> getQuantitySoldByBook() {
        Map<String, Long> result = new LinkedHashMap<>();
        for (Invoice invoice : repository.getAll()) {
            for (InvoiceDetail detail : invoice.getDetails()) {
                result.merge(detail.getBookId(), (long) detail.getQuantity(), Long::sum);
            }
        }
        return result;
    }
}
