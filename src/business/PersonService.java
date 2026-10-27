package business;

import data.IRepository;
import data.PersonDirectory;
import model.Employee;
import model.Invoice;
import model.Person;
import utility.StoreConfig;
import java.util.Map;
import java.util.Objects;

/** Business rules around the central Person[]; menus only collect/display data. */
public class PersonService {
    private final PersonDirectory people;
    private final IRepository<Invoice> invoices;

    public PersonService(PersonDirectory people, IRepository<Invoice> invoices) {
        this.people = Objects.requireNonNull(people);
        this.invoices = Objects.requireNonNull(invoices);
    }

    public void initialize(Person[] initialPeople) {
        if (!invoices.getAll().isEmpty()) {
            throw new IllegalStateException("Đã có hóa đơn, không được thay toàn bộ danh sách người.");
        }
        PersonDirectory candidate = new PersonDirectory(initialPeople);
        if (!(candidate.findById(StoreConfig.DEFAULT_EMPLOYEE_ID) instanceof Employee)) {
            throw new IllegalArgumentException("Danh sách mới phải có nhân viên " + StoreConfig.DEFAULT_EMPLOYEE_ID);
        }
        people.initialize(candidate.toArray());
    }

    public void add(Person person) { people.add(person); }
    public void addAll(Person[] additions) { people.addAll(additions); }
    public boolean updateById(String id, Person replacement) { return people.updateById(id, replacement); }
    public Person findById(String id) { return people.findById(id); }
    public Person[] toArray() { return people.toArray(); }
    public Person[] search(String keyword, String role, boolean exact) { return people.search(keyword, role, exact); }
    public Map<String, Integer> countByRole() { return people.countByRole(); }

    public boolean delete(String id) {
        if (id == null) return false;
        String key = id.trim();
        if (key.equalsIgnoreCase(StoreConfig.DEFAULT_EMPLOYEE_ID)) {
            throw new IllegalStateException("Không xóa nhân viên mặc định đang lập hóa đơn.");
        }
        for (Invoice invoice : invoices.getAll()) {
            if (invoice.getCustomer().getId().equalsIgnoreCase(key)
                    || invoice.getEmployee().getId().equalsIgnoreCase(key)) {
                throw new IllegalStateException("Người này đã có hóa đơn, không được xóa.");
            }
        }
        return people.delete(key);
    }
}
