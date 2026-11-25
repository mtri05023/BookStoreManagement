package bookstore;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CustomerList {
    private Customer[] items;
    private int size;

    public CustomerList() { items = new Customer[4]; }

    public void replaceAll(Customer[] values) {
        items = Arrays.copyOf(values, Math.max(4, values.length), Customer[].class);
        size = values.length;
    }

    public void add(Customer item) {
        if (item == null) throw new IllegalArgumentException("Dữ liệu không được null.");
        if (findById(item.getId()) != null) {
            throw new IllegalArgumentException("Mã đã tồn tại: " + item.getId());
        }
        if (size == items.length) items = Arrays.copyOf(items, items.length * 2);
        checkPhone(item, null);
        items[size++] = item;
    }

    public List<Customer> getAll() {
        return new ArrayList<>(Arrays.asList(Arrays.copyOf(items, size)));
    }

    public Customer findById(String id) {
        int index = Identifiable.indexOf(items, size, id);
        return index < 0 ? null : items[index];
    }

    public boolean delete(String id) {
        Customer item = findById(id);
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
    public boolean updateById(String id, Customer replacement) {
        Customer old = findById(id);
        if (old == null) return false;
        for (int i = 0; i < size; i++) if (items[i] == old) {
            checkPhone(replacement, old);
            items[i] = replacement;
            return true;
        }
        return false;
    }
    private void checkPhone(Customer value, Customer ignored) {
        for (int i = 0; i < size; i++)
            if (items[i] != ignored && items[i].getPhone().equals(value.getPhone()))
                throw new IllegalArgumentException("Số điện thoại khách hàng đã tồn tại.");
    }
    public boolean addCustomer(Customer customer) {
        AccountList.require(AccountList.Permission.MANAGE_CUSTOMERS);
        if (customer == null) return false;
        if (findById(customer.getId()) != null) return false;
        if (getAll().stream().anyMatch(c -> c.getPhone().equals(customer.getPhone())))
            throw new IllegalArgumentException("Số điện thoại khách hàng đã tồn tại.");
        add(customer);
        return true;
    }

    public List<Customer> getCustomers() {
        AccountList.require(AccountList.Permission.READ_CUSTOMERS);
        return getAll();
    }

    public Customer findCustomer(String id) {
        AccountList.require(AccountList.Permission.READ_CUSTOMERS);
        return findById(id);
    }
}
