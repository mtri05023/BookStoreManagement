package presentation;

import business.PersonService;
import business.StoreSession;
import model.Employee;
import model.Person;
import utility.InputHelper;
import java.util.LinkedHashMap;

/** Employee-only view of the shared, polymorphic Person[] directory. */
public final class EmployeeMenu {
    private static final String[] POSITIONS = {"Quản lý", "Thu ngân"};
    private final StoreSession session;
    private final InputHelper input;
    private final PersonService people;
    private final business.AccountService accounts;
    public EmployeeMenu(StoreSession session, InputHelper input, business.AccountService accounts) {
        this.session = session; this.input = input;
        this.accounts = accounts;
        people = new PersonService(session.state().getPeople(), session.state().getInvoices());
    }
    private Employee requireEmployee() {
        String id = input.readExistingCode("NV", "Mã nhân viên: ");
        Employee e = session.state().getEmployees().findById(id);
        if (e == null) throw new IllegalArgumentException("Không tìm thấy nhân viên có mã " + id);
        System.out.println("Đã tìm thấy nhân viên: " + e.getId() + " - " + e.getName());
        return e;
    }
    private void findEmployee() {
        while (true) {
            String id = input.readExistingCode("NV", "Mã nhân viên (0 để quay lại): ", "0");
            if (id.equals("0")) return;
            Employee employee = session.state().getEmployees().findById(id);
            if (employee != null) {
                System.out.println("Tìm nhân viên theo mã thành công:\n" + employee);
                return;
            }
            System.out.println("Sai mã nhân viên: " + id + ". Vui lòng nhập lại.");
        }
    }
    private Employee readEmployee(String id) {
        String name = input.read("Họ tên: ");
        String phone = readPhone();
        return new Employee(id, name, phone, readPosition());
    }
    private String readPhone() {
        return input.readPhone("Điện thoại (10 chữ số, bắt đầu bằng 0): ");
    }
    private String readPosition() {
        System.out.println("Chọn chức vụ:");
        for (int i = 0; i < POSITIONS.length; i++) System.out.println((i + 1) + ". " + POSITIONS[i]);
        while (true) {
            int choice = input.readInt("Chức vụ: ");
            if (choice >= 1 && choice <= POSITIONS.length) return POSITIONS[choice - 1];
            System.out.println("Lựa chọn không hợp lệ. Hãy chọn từ 1 đến " + POSITIONS.length + ".");
        }
    }
    private void ensureSingleManager(Employee[] proposed, String replacingId) {
        int managers = 0;
        for (Employee current : session.state().getEmployees().getAll()) {
            if ((replacingId == null || !current.getId().equalsIgnoreCase(replacingId))
                    && current.getPosition().equalsIgnoreCase("Quản lý")) managers++;
        }
        for (Employee employee : proposed)
            if (employee.getPosition().equalsIgnoreCase("Quản lý")) managers++;
        if (managers > 1) throw new IllegalArgumentException("Cửa hàng chỉ được có một quản lý.");
    }
    public void show() {
        while (true) {
            ConsoleView.options("QUẢN LÝ NHÂN VIÊN", "Thêm nhân viên", "Hiển thị danh sách", "Tìm theo mã",
                    "Tìm theo tên", "Tìm theo điện thoại", "Tìm theo chức vụ", "Sửa thông tin", "Xóa nhân viên",
                    "Thống kê theo chức vụ", "Thêm nhiều nhân viên");
            int choice = input.readInt("Chọn: ");
            try {
                switch (choice) {
                    case 0 -> { return; }
                    case 1 -> {
                        Employee e = readEmployee(input.readNewCode("NV", "Mã nhân viên: "));
                        ensureSingleManager(new Employee[] { e }, null);
                        session.change(() -> people.add(e));
                        System.out.println("Thêm nhân viên thành công! Đã lưu dữ liệu.");
                    }
                    case 2 -> {
                        int count = 0;
                        for (Person p : people.toArray()) if (p.getRole().equals("Employee")) { System.out.println(p); count++; }
                        System.out.println("Số nhân viên: " + count);
                    }
                    case 3 -> findEmployee();
                    case 4, 5, 6 -> {
                        while (true) {
                            String key = (choice == 5 ? input.readPhoneKeyword("Số điện thoại (0 để quay lại): ")
                                    : input.read("Từ khóa (0 để quay lại): ")).toLowerCase(java.util.Locale.ROOT);
                            if (key.equals("0")) break;
                            if (key.isBlank()) { System.out.println("Vui lòng nhập từ khóa tìm kiếm."); continue; }
                            int count = 0;
                            for (Employee e : session.state().getEmployees().getAll()) {
                                String value = choice == 4 ? e.getName() : choice == 5 ? e.getPhone() : e.getPosition();
                                if (value.toLowerCase(java.util.Locale.ROOT).contains(key)) { System.out.println(e); count++; }
                            }
                            if (count > 0) { System.out.println("Số nhân viên tìm thấy: " + count); break; }
                            System.out.println("Không tìm thấy nhân viên phù hợp. Vui lòng nhập lại.");
                        }
                    }
                    case 7 -> {
                        Employee e = requireEmployee();
                        Employee replacement = readEmployee(e.getId());
                        ensureSingleManager(new Employee[] { replacement }, e.getId());
                        session.change(() -> { if (!people.updateById(e.getId(), replacement)) throw new IllegalStateException("Không tìm thấy nhân viên."); });
                        System.out.println("Cập nhật nhân viên thành công! Đã lưu dữ liệu.");
                    }
                    case 8 -> {
                        Employee e = requireEmployee();
                        accounts.deleteEmployeeWithAccount(e.getId(), () -> session.change(() -> {
                            if (!people.delete(e.getId())) throw new IllegalStateException("Không tìm thấy nhân viên.");
                        }));
                        System.out.println("Xóa nhân viên và tài khoản liên kết thành công! Đã lưu dữ liệu.");
                    }
                    case 9 -> {
                        var counts = new LinkedHashMap<String, Integer>();
                        for (Employee e : session.state().getEmployees().getAll()) counts.merge(e.getPosition(), 1, Integer::sum);
                        if (counts.isEmpty()) System.out.println("Chưa có nhân viên.");
                        counts.forEach((role, count) -> System.out.println(role + ": " + count));
                        if (!counts.isEmpty()) System.out.println("Thống kê nhân viên theo chức vụ thành công.");
                    }
                    case 10 -> {
                        int n = input.readInt("Số lượng nhân viên cần thêm (1..1000): ");
                        if (n < 1 || n > 1000) throw new IllegalArgumentException("Số lượng phải trong 1..1000.");
                        Person[] batch = new Person[n];
                        for (int i = 0; i < n; i++) {
                            System.out.println("Nhân viên thứ " + (i + 1) + "/" + n);
                            batch[i] = readEmployee(input.readNewCode("NV", "Mã nhân viên: "));
                        }
                        Employee[] employees = java.util.Arrays.stream(batch).map(Employee.class::cast).toArray(Employee[]::new);
                        ensureSingleManager(employees, null);
                        session.change(() -> people.addAll(batch));
                        System.out.println("Đã thêm " + n + " nhân viên vào danh sách. Đã lưu dữ liệu.");
                    }
                    default -> System.out.println("Lựa chọn không hợp lệ.");
                }
            } catch (IllegalArgumentException | IllegalStateException | java.io.IOException e) { System.out.println(e.getMessage()); }
        }
    }
}
