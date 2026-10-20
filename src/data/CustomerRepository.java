package data;

import model.Customer;
import java.util.ArrayList;
import java.util.List;

public class CustomerRepository implements ICrudRepository<Customer> {
    private final List<Customer> customers = new ArrayList<>();

    @Override
    public void add(Customer customer) {
        if (customer == null) throw new IllegalArgumentException("Dữ liệu không được null.");
        if (findById(customer.getId()) != null) {
            throw new IllegalArgumentException("Mã đã tồn tại: " + customer.getId());
        }
        customers.add(customer);
    }

    @Override
    public List<Customer> getAll() { return new ArrayList<>(customers); }

    @Override
    public Customer findById(String id) {
        if (id == null || id.isBlank()) return null;
        id = id.trim();
        for (Customer customer : customers) {
            if (customer.getId().equalsIgnoreCase(id)) return customer;
        }
        return null;
    }
    @Override
    public boolean delete(String id) {
        Customer customer = findById(id);
        return customer != null && customers.remove(customer);
    }
}
