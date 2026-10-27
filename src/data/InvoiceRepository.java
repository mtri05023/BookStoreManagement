package data;

import model.Invoice;
import java.util.ArrayList;
import java.util.List;
import java.util.Arrays;

public class InvoiceRepository implements IRepository<Invoice> {
    private Invoice[] invoices;
    private int size;

    public InvoiceRepository() { invoices = new Invoice[4]; }

    @Override
    public void add(Invoice invoice) {
        if (invoice == null) throw new IllegalArgumentException("Dữ liệu không được null.");
        if (findById(invoice.getId()) != null) {
            throw new IllegalArgumentException("Mã đã tồn tại: " + invoice.getId());
        }
        if (size == invoices.length) invoices = Arrays.copyOf(invoices, invoices.length * 2);
        invoice.complete();
        invoices[size++] = invoice;
    }

    @Override
    public List<Invoice> getAll() {
        return new ArrayList<>(Arrays.asList(Arrays.copyOf(invoices, size)));
    }

    @Override
    public Invoice findById(String id) {
        if (id == null || id.isBlank()) return null;
        id = id.trim();
        for (int i = 0; i < size; i++) {
            Invoice invoice = invoices[i];
            if (invoice.getId().equalsIgnoreCase(id)) return invoice;
        }
        return null;
    }
}
