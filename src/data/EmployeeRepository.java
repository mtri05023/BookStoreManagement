package data;

import model.Employee;
import model.Person;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Typed view over the same central Person[] as CustomerRepository. */
public class EmployeeRepository implements ICrudRepository<Employee> {
    private final PersonDirectory people;

    public EmployeeRepository() { this(new PersonDirectory()); }

    public EmployeeRepository(PersonDirectory people) {
        this.people = Objects.requireNonNull(people);
    }

    @Override
    public void add(Employee employee) { people.add(employee); }

    @Override
    public Employee findById(String id) {
        Person person = people.findById(id);
        return person instanceof Employee employee ? employee : null;
    }

    @Override
    public List<Employee> getAll() {
        List<Employee> result = new ArrayList<>();
        for (Person person : people.toArray()) {
            if (person instanceof Employee employee) result.add(employee);
        }
        return result;
    }

    @Override
    public boolean delete(String id) { return findById(id) != null && people.delete(id); }
}
