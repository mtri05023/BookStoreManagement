package presentation;

import business.PersonService;
import business.StoreSession;
import model.Employee;
import model.Person;
import utility.InputHelper;
import java.util.LinkedHashMap;

/** Employee-only view of the shared, polymorphic Person[] directory. */
public final class EmployeeMenu {
    private final StoreSession session;
    private final InputHelper input;
    private final PersonService people;
    public EmployeeMenu(StoreSession session, InputHelper input) {
        this.session = session; this.input = input;
        people = new PersonService(session.state().getPeople(), session.state().getInvoices());
    }
    private Employee requireEmployee() {
        String id = input.readExistingCode("NV", "Mã nhân viên: ");
        Employee e = session.state().getEmployees().findById(id);
        if (e == null) throw new IllegalArgumentException("Không tìm thấy nhân viên có mã " + id);
        return e;
    }
    private Employee readEmployee(String id) {
        return new Employee(id, input.read("Họ tên: "), input.read("Điện thoại: "), input.read("Chức vụ: "));
    }
    public void show() {
        while (true) {
            ConsoleView.options("QUẢN LÝ NHÂN VIÊN", "Thêm nhân viên", "Hiển thị danh sách", "Tìm theo mã",
                    "Tìm theo tên", "Tìm theo điện thoại", "Tìm theo chức vụ", "Sửa thông tin", "Xóa nhân viên",
                    "Thống kê theo chức vụ");
            int choice = input.readInt("Chọn: ");
            try {
                switch (choice) {
                    case 0 -> { return; }
                    case 1 -> {
                        Employee e = readEmployee(input.readNewCode("NV", "Mã nhân viên: "));
                        session.change(() -> people.add(e));
                        System.out.println("Thêm nhân viên thành công! Đã lưu dữ liệu.");
                    }
                    case 2 -> {
                        int count = 0;
                        for (Person p : people.toArray()) if (p.getRole().equals("Employee")) { System.out.println(p); count++; }
                        System.out.println("Số nhân viên: " + count);
                    }
                    case 3 -> System.out.println(requireEmployee());
                    case 4, 5, 6 -> {
                        String key = input.read("Từ khóa: ").toLowerCase(java.util.Locale.ROOT);
                        int count = 0;
                        for (Employee e : session.state().getEmployees().getAll()) {
                            String value = choice == 4 ? e.getName() : choice == 5 ? e.getPhone() : e.getPosition();
                            if (value.toLowerCase(java.util.Locale.ROOT).contains(key)) { System.out.println(e); count++; }
                        }
                        System.out.println("Số nhân viên tìm thấy: " + count);
                    }
                    case 7 -> {
                        Employee e = requireEmployee();
                        Employee replacement = readEmployee(e.getId());
                        session.change(() -> { if (!people.updateById(e.getId(), replacement)) throw new IllegalStateException("Không tìm thấy nhân viên."); });
                        System.out.println("Cập nhật nhân viên thành công! Đã lưu dữ liệu.");
                    }
                    case 8 -> {
                        Employee e = requireEmployee();
                        session.change(() -> { if (!people.delete(e.getId())) throw new IllegalStateException("Không tìm thấy nhân viên."); });
                        System.out.println("Xóa nhân viên thành công! Đã lưu dữ liệu.");
                    }
                    case 9 -> {
                        var counts = new LinkedHashMap<String, Integer>();
                        for (Employee e : session.state().getEmployees().getAll()) counts.merge(e.getPosition(), 1, Integer::sum);
                        if (counts.isEmpty()) System.out.println("Chưa có nhân viên.");
                        counts.forEach((role, count) -> System.out.println(role + ": " + count));
                    }
                    default -> System.out.println("Lựa chọn không hợp lệ.");
                }
            } catch (IllegalArgumentException | IllegalStateException e) { System.out.println(e.getMessage()); }
        }
    }
}
