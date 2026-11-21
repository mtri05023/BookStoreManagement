package business;

import data.IRepository;
import data.PersonDirectory;
import model.Invoice;
import model.Person;
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
        PermissionService.require(PermissionService.Permission.MANAGE_EMPLOYEES);
        if (!invoices.getAll().isEmpty()) {
            throw new IllegalStateException("Đã có hóa đơn, không được thay toàn bộ danh sách người.");
        }
        people.initialize(initialPeople);
    }

    /** Replace one typed list while retaining the other view in the same Person[]. */
    public void initializeRole(String role, Person[] replacements) {
        PermissionService.require(PermissionService.Permission.MANAGE_EMPLOYEES);
        if (!"Customer".equals(role) && !"Employee".equals(role))
            throw new IllegalArgumentException("Loại danh sách không hợp lệ.");
        if (replacements == null) throw new IllegalArgumentException("Danh sách mới không được null.");
        Person[] current = people.toArray();
        Person[] combined = new Person[current.length + replacements.length];
        int count = 0;
        for (Person p : current) if (!p.getRole().equals(role)) combined[count++] = p;
        for (Person p : replacements) {
            if (p == null || !p.getRole().equals(role)) throw new IllegalArgumentException("Phần tử không đúng loại danh sách.");
            combined[count++] = p;
        }
        initialize(java.util.Arrays.copyOf(combined, count));
    }

    public void add(Person person) {
        if (person instanceof model.Employee) PermissionService.require(PermissionService.Permission.MANAGE_EMPLOYEES);
        else PermissionService.require(PermissionService.Permission.MANAGE_CUSTOMERS);
        people.add(person);
    }
    public void addAll(Person[] additions) {
        for (Person person : additions) if (person instanceof model.Employee)
            PermissionService.require(PermissionService.Permission.MANAGE_EMPLOYEES);
        else PermissionService.require(PermissionService.Permission.MANAGE_CUSTOMERS);
        people.addAll(additions);
    }
    public boolean updateById(String id, Person replacement) {
        if (replacement instanceof model.Employee) PermissionService.require(PermissionService.Permission.MANAGE_EMPLOYEES);
        else PermissionService.require(PermissionService.Permission.MANAGE_CUSTOMERS);
        return people.updateById(id, replacement);
    }
    public Person findById(String id) {
        Person person = people.findById(id);
        PermissionService.require(person instanceof model.Employee ? PermissionService.Permission.MANAGE_EMPLOYEES
                : PermissionService.Permission.READ_CUSTOMERS);
        return person;
    }
    public Person[] toArray() { PermissionService.require(PermissionService.Permission.MANAGE_EMPLOYEES); return people.toArray(); }
    public Person[] search(String keyword, String role, boolean exact) {
        PermissionService.require("Employee".equals(role) ? PermissionService.Permission.MANAGE_EMPLOYEES : PermissionService.Permission.READ_CUSTOMERS);
        return people.search(keyword, role, exact);
    }
    public Map<String, Integer> countByRole() { PermissionService.require(PermissionService.Permission.MANAGE_EMPLOYEES); return people.countByRole(); }

    public boolean delete(String id) {
        Person target = people.findById(id);
        if (target instanceof model.Employee) PermissionService.require(PermissionService.Permission.MANAGE_EMPLOYEES);
        else PermissionService.require(PermissionService.Permission.DELETE_CUSTOMERS);
        if (id == null) return false;
        String key = id.trim();
        for (Invoice invoice : invoices.getAll()) {
            if (invoice.getCustomer().getId().equalsIgnoreCase(key)
                    || invoice.getEmployee().getId().equalsIgnoreCase(key)) {
                String type = invoice.getCustomer().getId().equalsIgnoreCase(key) ? "khách hàng" : "nhân viên";
                throw new IllegalStateException("Không thể xóa " + type + " có mã " + key + " vì đã phát sinh hóa đơn.");
            }
        }
        return people.delete(key);
    }
}
