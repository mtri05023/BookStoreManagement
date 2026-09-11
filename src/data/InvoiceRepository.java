package data;

import model.Invoice;
import java.util.ArrayList;
import java.util.List;

public class InvoiceRepository {
    private List<Invoice> invoices = new ArrayList<>();

    public void add(Invoice invoice) { invoices.add(invoice); }

    public List<Invoice> getAll() { return invoices; }

    public Invoice findById(String id) {
        for (Invoice invoice : invoices) {
            if (invoice.getId().equalsIgnoreCase(id)) return invoice;
        }
        return null;
    }
}
