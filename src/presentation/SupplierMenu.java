package presentation;

import business.StoreSession;
import model.Supplier;
import utility.InputHelper;

public final class SupplierMenu {
    private final StoreSession session;
    private final InputHelper input;
    public SupplierMenu(StoreSession session, InputHelper input) {
        this.session = session;
        this.input = input;
    }

    public void show() {
        while (true) {
            ConsoleView.options("QUẢN LÝ NHÀ CUNG CẤP", "Danh sách nhà cung cấp", "Thêm nhà cung cấp",
                    "Sửa nhà cung cấp", "Xóa nhà cung cấp");
            int choice = input.readInt("Chọn: ");
            try {
                switch (choice) {
                    case 0 -> { return; }
                    case 1 -> list();
                    case 2 -> {
                        Supplier supplier = readSupplier(input.readNewCode("NCC", "Mã nhà cung cấp: "));
                        session.change(() -> session.state().getSuppliers().add(supplier));
                        System.out.println("Thêm nhà cung cấp thành công! Đã lưu dữ liệu.");
                    }
                    case 3 -> {
                        Supplier current = requireSupplier();
                        Supplier replacement = readSupplier(current.getId());
                        session.change(() -> {
                            Supplier live = session.state().getSuppliers().findById(current.getId());
                            live.setName(replacement.getName());
                            live.setPhone(replacement.getPhone());
                        });
                        System.out.println("Cập nhật nhà cung cấp thành công! Đã lưu dữ liệu.");
                    }
                    case 4 -> {
                        Supplier current = requireSupplier();
                        if (!input.read("Xóa " + current.getId() + " - " + current.getName() + "? Gõ DONGY: ").equals("DONGY")) {
                            System.out.println("Đã hủy thao tác.");
                            break;
                        }
                        session.change(() -> {
                            if (!session.state().getSuppliers().delete(current.getId()))
                                throw new IllegalStateException("Không tìm thấy nhà cung cấp.");
                        });
                        System.out.println("Xóa nhà cung cấp thành công! Đã lưu dữ liệu.");
                    }
                    default -> System.out.println("Lựa chọn không hợp lệ.");
                }
            } catch (IllegalArgumentException | IllegalStateException e) {
                System.out.println(e.getMessage());
            }
        }
    }

    private void list() {
        var suppliers = session.state().getSuppliers().getAll();
        if (suppliers.isEmpty()) System.out.println("Chưa có nhà cung cấp.");
        else {
            suppliers.forEach(System.out::println);
            System.out.println("Tổng số nhà cung cấp: " + suppliers.size());
        }
    }

    private Supplier requireSupplier() {
        String id = input.readExistingCode("NCC", "Mã nhà cung cấp: ");
        Supplier supplier = session.state().getSuppliers().findById(id);
        if (supplier == null) throw new IllegalArgumentException("Không tìm thấy nhà cung cấp: " + id);
        return supplier;
    }

    private Supplier readSupplier(String id) {
        String name = input.read("Tên nhà cung cấp: ");
        String phone = input.read("Số điện thoại (10 chữ số, bắt đầu bằng 0): ");
        return new Supplier(id, name, phone);
    }
}
