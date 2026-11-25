package bookstore;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class EmployeeList {
    private Employee[] items;
    private int size;

    public EmployeeList() { items = new Employee[4]; }

    public void replaceAll(Employee[] values) {
        items = Arrays.copyOf(values, Math.max(4, values.length), Employee[].class);
        size = values.length;
    }

    public void add(Employee item) {
        if (item == null) throw new IllegalArgumentException("Dữ liệu không được null.");
        if (findById(item.getId()) != null) {
            throw new IllegalArgumentException("Mã đã tồn tại: " + item.getId());
        }
        if (size == items.length) items = Arrays.copyOf(items, items.length * 2);
        items[size++] = item;
    }

    public List<Employee> getAll() {
        return new ArrayList<>(Arrays.asList(Arrays.copyOf(items, size)));
    }

    public Employee findById(String id) {
        int index = Identifiable.indexOf(items, size, id);
        return index < 0 ? null : items[index];
    }

    public boolean delete(String id) {
        Employee item = findById(id);
        if (item == null) return false;
        for (int i = 0; i < size; i++) {
            if (items[i] == item) {
                for (int j = i; j < size - 1; j++) items[j] = items[j + 1];
                items[--size] = null;
                return true;
            }
        }
        return false;
    }
    public boolean updateById(String id, Employee replacement) {
        Employee old = findById(id);
        if (old == null) return false;
        for (int i = 0; i < size; i++) if (items[i] == old) {

            items[i] = replacement;
            return true;
        }
        return false;
    }
}
