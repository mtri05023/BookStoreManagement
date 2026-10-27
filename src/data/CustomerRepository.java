package data;

import model.Customer;
import model.Person;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class CustomerRepository implements ICrudRepository<Customer> {
    private final PersonDirectory people;

    public CustomerRepository() { this(new PersonDirectory()); }

    public CustomerRepository(PersonDirectory people) {
        this.people = Objects.requireNonNull(people);
    }

    @Override
    public void add(Customer customer) {
        people.add(customer);
    }

    @Override
    public List<Customer> getAll() {
        List<Customer> result = new ArrayList<>();
        for (Person person : people.toArray()) {
            if (person instanceof Customer customer) result.add(customer);
        }
        return result;
    }

    @Override
    public Customer findById(String id) {
        Person person = people.findById(id);
        return person instanceof Customer customer ? customer : null;
    }
    @Override
    public boolean delete(String id) {
        return findById(id) != null && people.delete(id);
    }
}
