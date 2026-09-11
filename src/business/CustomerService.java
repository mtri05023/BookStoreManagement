package business;

import data.CustomerRepository;
import model.Customer;
import java.util.List;

public class CustomerService {
    private CustomerRepository repository;

    public CustomerService(CustomerRepository repository) {
        this.repository = repository;
    }

    public boolean addCustomer(Customer customer) {
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
