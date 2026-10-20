package data;

import model.Invoice;
import java.util.ArrayList;
import java.util.List;

public class InvoiceRepository implements IRepository<Invoice> {
    private final List<Invoice> invoices = new ArrayList<>();

    @Override
    public void add(Invoice invoice) {
        if (invoice == null) throw new IllegalArgumentException("Dữ liệu không được null.");
        if (findById(invoice.getId()) != null) {
            throw new IllegalArgumentException("Mã đã tồn tại: " + invoice.getId());
        }
        invoice.complete();
        invoices.add(invoice);
    }

    @Override
    public List<Invoice> getAll() { return new ArrayList<>(invoices); }

    @Override
    public Invoice findById(String id) {
        if (id == null || id.isBlank()) return null;
        id = id.trim();
        for (Invoice invoice : invoices) {
            if (invoice.getId().equalsIgnoreCase(id)) return invoice;
        }
        return null;
    }
}
