package data;

import model.Customer;
import model.Employee;
import model.Person;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Central object-array collection: Customer and Employee share one Person[]. */
public final class PersonDirectory implements ICrudRepository<Person> {
    private static final int INITIAL_CAPACITY = 4;
    private Person[] people;
    private int size;

    public PersonDirectory() {
        people = new Person[INITIAL_CAPACITY];
    }

    public PersonDirectory(Person[] initialPeople) {
        this();
        addAll(initialPeople);
    }

    public int size() { return size; }

    @Override
    public void add(Person person) {
        requirePerson(person);
        if (findById(person.getId()) != null) {
            throw new IllegalArgumentException("Mã người đã tồn tại: " + person.getId());
        }
        if (size == people.length) people = Arrays.copyOf(people, people.length * 2);
        people[size++] = person;
    }

    /** Validate the whole batch first: an invalid batch does not partly append. */
    public void addAll(Person[] additions) {
        if (additions == null) throw new IllegalArgumentException("Mảng người không được null.");
        PersonDirectory candidate = new PersonDirectory();
        for (int i = 0; i < size; i++) candidate.add(people[i]);
        for (Person person : additions) candidate.add(person);
        people = candidate.people;
        size = candidate.size;
    }

    /** Replace the initial n records only after validating all of them. */
    public void initialize(Person[] initialPeople) {
        PersonDirectory candidate = new PersonDirectory(initialPeople);
        people = candidate.people;
        size = candidate.size;
    }

    public boolean updateById(String id, Person replacement) {
        requirePerson(replacement);
        int index = indexOf(id);
        if (index < 0) return false;
        if (!people[index].getId().equals(replacement.getId())
                || people[index].getClass() != replacement.getClass()) {
            throw new IllegalArgumentException("Không đổi mã hoặc loại người khi sửa.");
        }
        people[index] = replacement;
        return true;
    }

    @Override
    public boolean delete(String id) {
        int index = indexOf(id);
        if (index < 0) return false;
        for (int i = index; i < size - 1; i++) people[i] = people[i + 1];
        people[--size] = null;
        return true;
    }

    @Override
    public Person findById(String id) {
        int index = indexOf(id);
        return index < 0 ? null : people[index];
    }

    public Person[] toArray() { return Arrays.copyOf(people, size); }

    @Override
    public List<Person> getAll() { return new ArrayList<>(Arrays.asList(toArray())); }

    /** AND between role and keyword; OR between ID/name/contact fields. */
    public Person[] search(String keyword, String role, boolean exact) {
        String key = normalize(keyword);
        String roleKey = normalize(role);
        Person[] matches = new Person[size];
        int count = 0;
        for (int i = 0; i < size; i++) {
            Person person = people[i];
            // Runtime polymorphism: getRole() is dispatched to the actual subtype.
            if (!roleKey.isEmpty() && !normalize(person.getRole()).equals(roleKey)) continue;
            String contact = person instanceof Customer customer ? customer.getPhone()
                    : ((Employee) person).getPosition();
            if (key.isEmpty() || matches(person.getId(), key, exact)
                    || matches(person.getName(), key, exact) || matches(contact, key, exact)) {
                matches[count++] = person;
            }
        }
        return Arrays.copyOf(matches, count);
    }

    public Map<String, Integer> countByRole() {
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (int i = 0; i < size; i++) counts.merge(people[i].getRole(), 1, Integer::sum);
        return counts;
    }

    private int indexOf(String id) {
        if (id == null) return -1;
        for (int i = 0; i < size; i++) {
            if (people[i].getId().equalsIgnoreCase(id.trim())) return i;
        }
        return -1;
    }

    private static void requirePerson(Person person) {
        if (!(person instanceof Customer) && !(person instanceof Employee)) {
            throw new IllegalArgumentException("Danh sách chỉ nhận Customer hoặc Employee.");
        }
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private static boolean matches(String value, String key, boolean exact) {
        return exact ? normalize(value).equals(key) : normalize(value).contains(key);
    }
}
