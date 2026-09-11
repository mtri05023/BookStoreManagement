package business;

import data.InvoiceRepository;
import model.Invoice;
import java.util.List;

public class InvoiceService {
    private InvoiceRepository repository;

    public InvoiceService(InvoiceRepository repository) {
        this.repository = repository;
    }

    public void addInvoice(Invoice invoice) {
        repository.add(invoice);
    }

    public List<Invoice> getInvoices() {
        return repository.getAll();
    }

    public Invoice findInvoice(String id) {
        return repository.findById(id);
    }

    public double getRevenue() {
        double total = 0;
        for (Invoice invoice : repository.getAll()) {
            total += invoice.getTotal();
        }
        return total;
    }
}
