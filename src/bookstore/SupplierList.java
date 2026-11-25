package bookstore;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SupplierList {
    public SupplierList() { }
    private Supplier[] suppliers = new Supplier[4];
    private int size;

    public void replaceAll(Supplier[] values) {
        suppliers = Arrays.copyOf(values, Math.max(4, values.length));
        size = values.length;
    }
     public void add(Supplier supplier) {
        if (supplier == null) throw new IllegalArgumentException("Nhà cung cấp không được để trống.");
        if (findById(supplier.getId()) != null) throw new IllegalArgumentException("Mã nhà cung cấp đã tồn tại: " + supplier.getId());
        if (size == suppliers.length) suppliers = Arrays.copyOf(suppliers, suppliers.length * 2);
        suppliers[size++] = supplier;
    }
     public List<Supplier> getAll() { return new ArrayList<>(Arrays.asList(Arrays.copyOf(suppliers, size))); }
     public Supplier findById(String id) {
        if (id == null) return null;
        for (int i = 0; i < size; i++) if (suppliers[i].getId().equalsIgnoreCase(id.trim())) return suppliers[i];
        return null;
    }
     public boolean delete(String id) {
        Supplier found = findById(id);
        if (found == null) return false;
        for (int i = 0; i < size; i++) if (suppliers[i] == found) {
            System.arraycopy(suppliers, i + 1, suppliers, i, size - i - 1);
            suppliers[--size] = null;
            return true;
        }
        return false;
    }
}
