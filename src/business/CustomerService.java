package business;

import data.ICrudRepository;
import model.Customer;
import java.util.List;

public class CustomerService {
    private final ICrudRepository<Customer> repository;

    public CustomerService(ICrudRepository<Customer> repository) {
        this.repository = java.util.Objects.requireNonNull(repository);
    }

    public boolean addCustomer(Customer customer) {
        if (customer == null) return false;
        if (repository.findById(customer.getId()) != null) return false;
        repository.add(customer);
        return true;
    }

    public List<Customer> getCustomers() {
        return repository.getAll();
    }

    public Customer findCustomer(String id) {
        return repository.findById(id);
    }
}
