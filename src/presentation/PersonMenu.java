package presentation;

import business.PersonService;
import model.Customer;
import model.Employee;
import model.Person;
import utility.InputHelper;

/** Leader's worked example: actual array operations and runtime polymorphism. */
public class PersonMenu {
    private final PersonService people;
    private final InputHelper input;

    public PersonMenu(PersonService people, InputHelper input) {
        this.people = people;
        this.input = input;
    }

    public void show() {
        while (true) {
            System.out.println("\n=== DANH SÁCH NGƯỜI — MẪU MẢNG & ĐA HÌNH ===");
            System.out.println("1. Xuất danh sách  2. Nhập mới n người (thay danh sách)");
            System.out.println("3. Thêm 1 người   4. Thêm k người");
            System.out.println("5. Sửa theo mã    6. Xóa theo mã");
            System.out.println("7. Tìm nhiều khóa 8. Thống kê theo vai trò  0. Quay lại");
            int choice = input.readInt("Chọn: ");
            try {
                switch (choice) {
                    case 0 -> { return; }
                    case 1 -> print(people.toArray());
                    case 2 -> initialize();
                    case 3 -> people.add(readPerson(null));
                    case 4 -> people.addAll(readPeople());
                    case 5 -> update();
                    case 6 -> delete();
                    case 7 -> search();
                    case 8 -> System.out.println(people.countByRole());
                    default -> System.out.println("Lựa chọn không hợp lệ.");
                }
            } catch (IllegalArgumentException | IllegalStateException e) {
                System.out.println(e.getMessage());
            }
        }
    }

    private Person readPerson(Person existing) {
        int type = existing == null ? input.readInt("Loại: 1. Khách hàng, 2. Nhân viên: ")
                : existing instanceof Customer ? 1 : 2;
        if (type != 1 && type != 2) throw new IllegalArgumentException("Loại người không hợp lệ.");
        String id = existing == null ? input.read("Mã: ") : existing.getId();
        String name = input.read("Họ tên: ");
        return type == 1 ? new Customer(id, name, input.read("Điện thoại: "))
                : new Employee(id, name, input.read("Chức vụ: "));
    }

    private Person[] readPeople() {
        int count = input.readInt("Số người (1..1000): ");
        if (count < 1 || count > 1000) throw new IllegalArgumentException("Số người phải trong 1..1000.");
        Person[] batch = new Person[count];
        for (int i = 0; i < count; i++) {
            System.out.println("Người thứ " + (i + 1));
            batch[i] = readPerson(null);
        }
        return batch;
    }

    private void initialize() {
        if (!input.read("Thay toàn bộ danh sách. Gõ DONGY để tiếp tục: ").equals("DONGY")) return;
        people.initialize(readPeople());
    }

    private void update() {
        Person current = people.findById(input.read("Mã cần sửa: "));
        if (current == null) throw new IllegalArgumentException("Không tìm thấy người.");
        people.updateById(current.getId(), readPerson(current));
    }

    private void delete() {
        String id = input.read("Mã cần xóa: ").trim();
        System.out.println(people.delete(id) ? "Đã xóa." : "Không tìm thấy người.");
    }

    private void search() {
        String keyword = input.read("Mã/tên/điện thoại/chức vụ (trống = tất cả): ");
        String role = input.read("Vai trò Customer/Employee (trống = tất cả): ");
        int mode = input.readInt("1. Chính xác, 2. Gần đúng: ");
        if (mode != 1 && mode != 2) throw new IllegalArgumentException("Kiểu tìm kiếm không hợp lệ.");
        boolean exact = mode == 1;
        print(people.search(keyword, role, exact));
    }

    private void print(Person[] result) {
        System.out.println("Số người: " + result.length);
        for (Person person : result) {
            // Both abstract-method dispatch and toString dispatch are used, not merely declared.
            System.out.println(person.getRole() + " | " + person);
        }
    }
}
