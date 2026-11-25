package bookstore;

import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

public class CatalogMenu {
    private final BookStore session;
    private final InputHelper input;
    private final AccountList accounts;
    private final BookList books;
    private final CustomerList customers;
    private final InvoiceList invoices;
    private final InventoryReport inventory;
    private final SalesReport reports;
    private static final String[] POSITIONS = {"Quản lý", "Thu ngân"};
    public CatalogMenu(BookStore store, InputHelper input, AccountList accounts) {
        this.session = store;
        this.input = input;
        this.accounts = accounts;
        books = store.getBooks();
        customers = store.getCustomers();
        invoices = store.getInvoices();
        inventory = new InventoryReport(store, input);
        reports = new SalesReport(input, store);
    }
    public void show() {
        while (true) {
            System.out.println("\n========== BOOKSTORE MANAGEMENT ==========");
            try {
                String username = input.read("Tên đăng nhập (0 để thoát): ");
                if (username.equals("0")) { session.save(); return; }
                login(username);
            } catch (IllegalArgumentException | IllegalStateException | IOException e) {
                System.out.println("Không thể hoàn tất: " + e.getMessage());
            }
        }
    }

    private void login(String username) throws IOException {
        char[] password = input.readPassword("Mật khẩu: ");
        Account account;
        try { account = accounts.login(username, password); }
        finally { Arrays.fill(password, '\0'); }
        System.out.println("Đăng nhập thành công. Xin chào " + account.getUsername() + " (" + account.getRole() + ").");
        if (account.mustChangePassword()) forcePasswordChange();
        if (accounts.isAuthenticated()) {
            if (account.getRole() == Account.Role.ADMIN) showAdmin(); else showEmployee();
            session.save();
        }
        accounts.logout();
        System.out.println("Đã đăng xuất.");
    }

    private void forcePasswordChange() throws IOException {
        System.out.println("Bạn phải đổi mật khẩu tạm trước khi sử dụng hệ thống.");
        while (true) {
            char[] oldPassword = input.readPassword("Mật khẩu tạm hiện tại: ");
            char[] next = input.readPassword("Mật khẩu mới (3-128 ký tự): ");
            char[] confirm = input.readPassword("Nhập lại mật khẩu mới: ");
            try {
                if (!Arrays.equals(next, confirm)) { System.out.println("Mật khẩu xác nhận không khớp."); continue; }
                accounts.updateOwnPassword(oldPassword, next, accounts);
                System.out.println("Đã đổi mật khẩu.");
                return;
            } catch (IllegalArgumentException e) { System.out.println(e.getMessage()); }
            finally { Arrays.fill(oldPassword, '\0'); Arrays.fill(next, '\0'); Arrays.fill(confirm, '\0'); }
        }
    }

    private void changePassword() {
        while (true) {
            char[] oldPassword = input.readPassword("Mật khẩu hiện tại: ");
            char[] next = input.readPassword("Mật khẩu mới (3-128 ký tự): ");
            char[] confirm = input.readPassword("Nhập lại mật khẩu mới: ");
            try {
                if (!Arrays.equals(next, confirm)) { System.out.println("Mật khẩu xác nhận không khớp."); continue; }
                accounts.updateOwnPassword(oldPassword, next, accounts);
                System.out.println("Đổi mật khẩu thành công.");
                return;
            } catch (IllegalArgumentException | IOException e) { System.out.println("Không thể đổi mật khẩu: " + e.getMessage()); }
            finally { Arrays.fill(oldPassword, '\0'); Arrays.fill(next, '\0'); Arrays.fill(confirm, '\0'); }
        }
    }

    private void showAdmin() throws IOException {
        while (accounts.isAuthenticated()) {
            InputHelper.options("ADMIN MENU", "Quản lý sách", "Quản lý khách hàng", "Quản lý nhân viên",
                    "Quản lý nhà cung cấp", "Lập hóa đơn", "Quản lý hóa đơn", "Quản lý tồn kho",
                    "Báo cáo doanh thu", "Quản lý tài khoản", "Đổi mật khẩu", "Lưu dữ liệu");
            int choice = input.readInt("Chọn: ");
            try {
                switch (choice) {
                    case 0 -> { session.save(); return; }
                    case 1 -> manageBooks(); case 2 -> manageCustomers(); case 3 -> manageEmployees();
                    case 4 -> manageSuppliers(); case 5 -> checkout(); case 6 -> manageInvoices();
                    case 7 -> inventory.show(); case 8 -> reports.show(); case 9 -> manageAccounts();
                    case 10 -> changePassword(); case 11 -> { session.save(); System.out.println("Đã lưu dữ liệu."); }
                    default -> System.out.println("Lựa chọn không hợp lệ.");
                }
            } catch (IOException | IllegalArgumentException | IllegalStateException | SecurityException e) {
                System.out.println("Không thể hoàn tất thao tác: " + e.getMessage());
            }
        }
    }

    private void showEmployee() throws IOException {
        while (accounts.isAuthenticated()) {
            InputHelper.options("EMPLOYEE MENU", "Xem và tìm kiếm sách", "Quản lý khách hàng",
                    "Lập hóa đơn", "Xem hóa đơn của tôi", "Xem tồn kho", "Đổi mật khẩu");
            int choice = input.readInt("Chọn: ");
            try {
                switch (choice) {
                    case 0 -> { session.save(); return; }
                    case 1 -> employeeBooks(); case 2 -> employeeCustomers();
                    case 3 -> checkout(); case 4 -> showOwnInvoices();
                    case 5 -> inventory.showForEmployee(); case 6 -> changePassword();
                    default -> System.out.println("Lựa chọn không hợp lệ.");
                }
            } catch (IOException | IllegalArgumentException | IllegalStateException | SecurityException e) {
                System.out.println("Không thể hoàn tất thao tác: " + e.getMessage());
            }
        }
    }

    public void save() throws IOException { session.save(); }
    public void employeeBooks() {
        while (true) {
            InputHelper.options("SÁCH", "Danh sách", "Tìm theo mã", "Tìm theo tên hoặc từ khóa", "Lọc theo giá", "Sắp xếp");
            int choice = input.readInt("Chọn: ");
            try {
                switch (choice) {
                    case 0 -> { return; }
                    case 1 -> showBooks();
                    case 2 -> findBook();
                    case 3 -> searchBooks();
                    case 4 -> filterPriceInteractive();
                    case 5 -> InputHelper.books(books.sorted(input.readInt(
                            "Chọn tiêu chí sắp xếp (1 - Tên sách A-Z, 2 - Giá bán tăng dần, 3 - Số lượng tồn kho tăng dần): ")));
                    default -> System.out.println("Lựa chọn không hợp lệ.");
                }
            } catch (IllegalArgumentException | IllegalStateException e) { System.out.println(e.getMessage()); }
        }
    }

    private void manageBooks() {
        while (true) {
            InputHelper.options("QUẢN LÝ SÁCH", "Thêm sách", "Hiển thị danh sách", "Tìm theo mã",
                    "Tìm theo tên hoặc từ khóa", "Sửa thông tin", "Xóa sách",
                    "Lọc theo khoảng giá", "Xem tồn kho", "Thêm nhiều sách", "Thống kê số đầu sách theo thể loại");
            int choice = input.readInt("Chọn: ");
            try {
                switch (choice) {
                    case 0 -> { return; }
                    case 1 -> {
                        String id = input.readNewCode("S", "Mã sách: ");
                        if (books.findBook(id) != null) throw new IllegalArgumentException("Không thể thêm sách: Mã sách đã tồn tại.");
                        Book b = readBook(id, 0);
                        session.change(() -> {
                            if (!books.addBook(b)) throw new IllegalArgumentException("Mã sách đã tồn tại hoặc dữ liệu không hợp lệ.");
                        });
                        System.out.println("Thêm sách thành công! Đã lưu dữ liệu.");
                    }
                    case 2, 8 -> {
                        Book[] result = books.getBooks().toArray(new Book[0]);
                        InputHelper.books(result);
                        System.out.println(result.length == 0 ? "Danh sách sách hiện đang trống." : "Đã hiển thị toàn bộ sách.");
                    }
                    case 3 -> {
                        while (true) {
                            String id = input.readExistingCode("S", "Mã sách (0 để quay lại): ", "0");
                            if (id.equals("0")) break;
                            Book b = books.findBook(id);
                            if (b == null) {
                                System.out.println("Sai mã sách: " + id + ". Vui lòng nhập lại.");
                                continue;
                            }
                            InputHelper.books(new Book[] { b });
                            System.out.println("Tìm sách theo mã thành công.");
                            break;
                        }
                    }
                    case 4 -> {
                        searchBooks();
                    }
                    case 5 -> {
                        String id = input.readExistingCode("S", "Mã sách cần sửa: ");
                        Book old = books.findBook(id);
                        if (old == null) throw new IllegalArgumentException("Không tìm thấy sách: " + id);
                        System.out.println("Hiện tại: " + old);
                        Book b = readBookUpdate(old);
                        if (b == null) break;
                        if (!b.getId().equals(old.getId())) session.renameBook(old.getId(), b);
                        else session.change(() -> books.updateBook(b));
                        System.out.println("Cập nhật thông tin sách thành công! Đã lưu dữ liệu.");
                    }
                    case 6 -> {
                        String id = input.readExistingCode("S", "Mã sách cần xóa: ");
                        session.change(() -> session.deleteBook(id));
                        System.out.println("Xóa sách thành công! Đã lưu dữ liệu.");
                    }
                    case 7 -> {
                        filterPriceInteractive();
                    }
                    case 10 -> books.countByCategory().forEach((category, count) ->
                            System.out.printf("%s: %d đầu sách%n", category, count));
                    case 9 -> {
                        int count = input.readInt("Số lượng sách cần thêm (1..1000): ");
                        if (count < 1 || count > 1000)
                            throw new IllegalArgumentException("Số lượng sách phải trong 1..1000.");
                        Book[] batch = new Book[count];
                        for (int i = 0; i < count; i++) {
                            System.out.println("Sách thứ " + (i + 1) + "/" + count);
                            String id = input.readNewCode("S", "Mã sách: ");
                            if (books.findBook(id) != null)
                                throw new IllegalArgumentException("Không thể thêm danh sách: mã sách " + id + " đã tồn tại.");
                            batch[i] = readBook(id, 0);
                        }
                        session.change(() -> {
                            for (Book book : batch) {
                                if (!books.addBook(book))
                                    throw new IllegalArgumentException("Không thể thêm sách " + book.getId() + ".");
                            }
                        });
                        System.out.println("Đã thêm " + count + " sách thành công. Đã lưu dữ liệu.");
                    }
                    default -> System.out.println("Lựa chọn không hợp lệ.");
                }
            } catch (IllegalArgumentException | IllegalStateException e) { System.out.println(e.getMessage()); }
        }
    }
    private Book readBook(String id, int quantity) {
        String title = input.read("Tên sách: ");
        String author = input.read("Tác giả: ");
        Book existing = books.findBook(id);
        String category = existing == null ? "Chưa phân loại" : existing.getCategory();
        double cost;
        while (true) {
            cost = input.readDouble("Giá nhập (VNĐ, 0 nếu chưa có): ");
            if (cost >= 0) break;
            System.out.println("Giá nhập không được âm. Vui lòng nhập lại.");
        }
        double price;
        while (true) {
            price = input.readDouble("Giá bán (VNĐ, phải lớn hơn giá nhập): ");
            if (price > cost) break;
            System.out.println("Giá bán phải lớn hơn giá nhập. Vui lòng nhập lại.");
        }
        if (books.findBook(id) == null) quantity = input.readInt("Số sách cần thêm vào kho (quyển): ");
        return Book.create(id, title, author, category, cost, price, quantity);
    }
    private Book readBookUpdate(Book old) {
        InputHelper.options("SỬA THÔNG TIN SÁCH - CHỌN THÔNG TIN MUỐN SỬA", "Tên sách", "Tác giả", "Thể loại", "Giá nhập", "Giá bán", "Mã sách");
        int field = input.readInt("Chọn thông tin cần sửa: ");
        String id = old.getId(), title = old.getTitle(), author = old.getAuthor(), category = old.getCategory();
        double cost = old.getPurchasePrice(), price = old.getPrice();
        switch (field) {
            case 0 -> { return null; }
            case 1 -> title = input.read("Tên sách mới: ");
            case 2 -> author = input.read("Tác giả mới: ");
            case 3 -> category = input.read("Thể loại mới: ");
            case 4 -> {
                while (true) {
                    cost = input.readDouble("Giá nhập mới: ");
                    if (cost >= 0 && cost < price) break;
                    System.out.println("Giá nhập phải không âm và nhỏ hơn giá bán. Vui lòng nhập lại.");
                }
            }
            case 5 -> {
                while (true) {
                    price = input.readDouble("Giá bán mới: ");
                    if (price > cost) break;
                    System.out.println("Giá bán phải lớn hơn giá nhập. Vui lòng nhập lại.");
                }
            }
            case 6 -> id = input.readNewCode("S", "Mã sách mới: ");
            default -> throw new IllegalArgumentException("Lựa chọn không hợp lệ.");
        }
        if (field != 4 && field != 5 && price <= cost) {
            Book unchangedPrices = Book.restore(id, title, author, category, price, old.getQuantity());
            unchangedPrices.restorePurchasePrice(cost);
            return unchangedPrices;
        }
        return Book.create(id, title, author, category, cost, price, old.getQuantity());
    }

    public void showBooks() {
        System.out.printf("%-8s %-25s %-20s %10s %6s%n",
                "Mã", "Tên sách", "Tác giả", "Giá", "SL");
        for (Book book : books.getBooks())
            System.out.println(book);
    }

    public void findBook() {
        while (true) {
            String id = input.readExistingCode("S", "Nhập mã sách (0 để quay lại): ", "0");
            if (id.equals("0")) return;
            Book book = books.findBook(id);
            if (book != null) {
                System.out.println(book);
                return;
            }
            System.out.println("Sai mã sách: " + id + ". Vui lòng nhập lại.");
        }
    }

    private void searchBooks() {
        while (true) {
            String keyword = input.read("Tên hoặc từ khóa (0 để quay lại): ");
            if (keyword.equals("0")) return;
            if (keyword.isBlank()) { System.out.println("Vui lòng nhập tên hoặc từ khóa."); continue; }
            Book[] result = books.search(keyword);
            if (result.length > 0) {
                InputHelper.books(result);
                System.out.println("Tìm kiếm thành công: " + result.length + " sách.");
                return;
            }
            System.out.println("Không có sách khớp với từ khóa. Vui lòng nhập lại.");
        }
    }
    private void filterPriceInteractive() {
        while (true) {
            double min = input.readDouble("Giá thấp nhất (-1 để quay lại): ");
            if (min == -1) return;
            double max = input.readDouble("Giá cao nhất: ");
            try {
                Book[] result = books.filterPrice(min, max);
                if (result.length == 0) {
                    System.out.println("Không có sách trong khoảng giá đã chọn. Vui lòng nhập lại.");
                    continue;
                }
                InputHelper.books(result);
                System.out.println("Lọc khoảng giá thành công: " + result.length + " sách.");
                return;
            } catch (IllegalArgumentException error) {
                System.out.println(error.getMessage() + " Vui lòng nhập lại.");
            }
        }
    }

    public void employeeCustomers() {
        while (true) {
            InputHelper.options("KHÁCH HÀNG", "Danh sách", "Tìm theo mã", "Tìm theo tên", "Thêm khách hàng", "Cập nhật khách hàng");
            int choice = input.readInt("Chọn: ");
            try {
                switch (choice) {
                    case 0 -> { return; }
                    case 1 -> showCustomers();
                    case 2 -> findCustomer();
                    case 3 -> findByName();
                    case 4 -> session.addCustomer(readCustomer(newCustomerId()));
                    case 5 -> {
                        updateCustomerFields();
                    }
                    default -> System.out.println("Lựa chọn không hợp lệ.");
                }
            } catch (IllegalArgumentException | IllegalStateException e) { System.out.println(e.getMessage()); }
        }
    }

    private void manageCustomers() {
        while (true) {
            InputHelper.options("QUẢN LÝ KHÁCH HÀNG", "Thêm khách hàng", "Hiển thị danh sách", "Tìm theo mã",
                    "Tìm theo tên hoặc từ khóa", "Tìm theo số điện thoại", "Sửa thông tin", "Xóa khách hàng",
                    "Lịch sử mua hàng", "Tổng số hóa đơn", "Tổng chi tiêu", "Lần mua gần nhất", "Thống kê theo tổng chi tiêu",
                    "Thêm nhiều khách hàng");
            int choice = input.readInt("Chọn: ");
            try {
                switch (choice) {
                    case 0 -> { return; }
                    case 1 -> {
                        Customer c = readCustomer(newCustomerId());
                        session.addCustomer(c);
                        System.out.println("Thêm khách hàng thành công! Đã lưu dữ liệu.");
                    }
                    case 2 -> showCustomers();
                    case 3 -> findCustomer();
                    case 4, 5 -> {
                        while (true) {
                            String key = choice == 5 ? input.readPhoneKeyword("Số điện thoại (0 để quay lại): ")
                                    : input.read("Tên hoặc từ khóa (0 để quay lại): ");
                            if (key.equals("0")) break;
                            if (key.isBlank()) { System.out.println("Vui lòng nhập từ khóa tìm kiếm."); continue; }
                            int count = 0;
                            for (Customer c : customers.getCustomers()) {
                                String value = choice == 5 ? c.getPhone() : c.getName();
                                if (value.toLowerCase(java.util.Locale.ROOT).contains(key.toLowerCase(java.util.Locale.ROOT))) {
                                    System.out.println(c);
                                    count++;
                                }
                            }
                            if (count > 0) { System.out.println("Số khách hàng tìm thấy: " + count); break; }
                            System.out.println("Không tìm thấy khách hàng phù hợp. Vui lòng nhập lại.");
                        }
                    }
                    case 6 -> {
                        if (updateCustomerFields()) System.out.println("Cập nhật khách hàng thành công! Đã lưu dữ liệu.");
                    }
                    case 7 -> {
                        Customer c = requireCustomer();
                        session.deleteCustomer(c.getId());
                        System.out.println("Xóa khách hàng thành công! Đã lưu dữ liệu.");
                    }
                    case 8, 9, 10, 11 -> history(choice);
                    case 12 -> {
                        Customer[] customers = this.customers.getCustomers().toArray(new Customer[0]);
                        java.util.Arrays.sort(customers, java.util.Comparator.comparingDouble((Customer c) -> total(c.getId())).reversed());
                        System.out.println("Mã | Họ tên | Số hóa đơn | Tổng chi tiêu (VNĐ)");
                        for (Customer c : customers) System.out.printf(InputHelper.CURRENCY_LOCALE, "%s | %s | %d | %,.0f%n", c.getId(), c.getName(), invoices.history(c.getId()).length, total(c.getId()));
                        System.out.println(customers.length == 0 ? "Chưa có khách hàng để thống kê." : "Đã thống kê chi tiêu của " + customers.length + " khách hàng.");
                    }
                    case 13 -> {
                        int n = input.readInt("Số lượng khách hàng cần thêm (1..1000): ");
                        if (n < 1 || n > 1000) throw new IllegalArgumentException("Số lượng phải trong 1..1000.");
                        Customer[] batch = new Customer[n];
                        for (int i = 0; i < n; i++) {
                            System.out.println("Khách hàng thứ " + (i + 1) + "/" + n);
                            batch[i] = readCustomer(newCustomerId());
                        }
                        session.addCustomers(batch);
                        System.out.println("Đã thêm " + n + " khách hàng vào danh sách. Đã lưu dữ liệu.");
                    }
                    default -> System.out.println("Lựa chọn không hợp lệ.");
                }
            } catch (IllegalArgumentException | IllegalStateException e) { System.out.println(e.getMessage()); }
        }
    }
    private Customer readCustomer(String id) { return new Customer(id, input.read("Họ tên: "), input.readPhone("Điện thoại: ")); }
    private String newCustomerId() {
        String id = input.readNewCode("KH", "Mã khách hàng: ");
        if (customers.findCustomer(id) != null) throw new IllegalArgumentException("Không thể thêm khách hàng: Mã khách hàng đã tồn tại.");
        return id;
    }
    private void findByName() {
        while (true) {
            String key = input.read("Tên hoặc từ khóa (0 để quay lại): ").toLowerCase(java.util.Locale.ROOT);
            if (key.equals("0")) return;
            if (key.isBlank()) { System.out.println("Vui lòng nhập tên hoặc từ khóa."); continue; }
            int count = 0;
            for (Customer customer : customers.getCustomers()) {
                if (customer.getName().toLowerCase(java.util.Locale.ROOT).contains(key)) {
                    System.out.println(customer);
                    count++;
                }
            }
            if (count > 0) { System.out.println("Số khách hàng tìm thấy: " + count); return; }
            System.out.println("Không tìm thấy khách hàng phù hợp. Vui lòng nhập lại.");
        }
    }
    private boolean updateCustomerFields() {
        Customer current = requireCustomer();
        System.out.println("Hiện tại: " + current);
        InputHelper.options("SỬA KHÁCH HÀNG - CHỌN THÔNG TIN MUỐN SỬA", "Họ tên", "Số điện thoại", "Mã khách hàng");
        int field = input.readInt("Chọn thông tin cần sửa: ");
        switch (field) {
            case 0 -> { System.out.println("Đã hủy sửa thông tin."); return false; }
            case 1 -> session.updateCustomer(new Customer(current.getId(), input.read("Họ tên mới: "), current.getPhone()));
            case 2 -> {
                while (true) {
                    String phone = input.readPhone("Số điện thoại mới: ");
                    boolean duplicate = customers.getCustomers().stream().anyMatch(c ->
                            !c.getId().equalsIgnoreCase(current.getId()) && c.getPhone().equals(phone));
                    if (duplicate) { System.out.println("Số điện thoại khách hàng đã tồn tại. Vui lòng nhập lại."); continue; }
                    session.updateCustomer(new Customer(current.getId(), current.getName(), phone));
                    break;
                }
            }
            case 3 -> {
                String id = input.readNewCode("KH", "Mã khách hàng mới: ");
                session.renameCustomer(current.getId(), new Customer(id, current.getName(), current.getPhone()));
            }
            default -> throw new IllegalArgumentException("Lựa chọn không hợp lệ.");
        }
        return true;
    }
    private Customer requireCustomer() {
        String id = input.readExistingCode("KH", "Mã khách hàng: ");
        Customer c = customers.findCustomer(id);
        if (c != null) System.out.println("Tìm khách hàng thành công.");
        if (c == null) throw new IllegalArgumentException("Không tìm thấy khách hàng có mã " + id);
        return c;
    }
    private double total(String id) { return java.util.Arrays.stream(invoices.history(id)).mapToDouble(Invoice::getTotal).sum(); }
    private void history(int mode) {
        Customer c = requireCustomer();
        Invoice[] invoices = this.invoices.history(c.getId());
        System.out.println(c.getId() + " - " + c.getName() + " | SĐT: " + c.getPhone());
        if (invoices.length == 0) { System.out.println("Khách hàng chưa có giao dịch nào."); return; }
        if (mode == 8) for (Invoice i : invoices) InputHelper.invoice(i);
        if (mode == 8 || mode == 9) System.out.println("Tổng số hóa đơn: " + invoices.length);
        if (mode == 8 || mode == 10) System.out.printf(InputHelper.CURRENCY_LOCALE, "Tổng chi tiêu: %,.0f VNĐ%n", total(c.getId()));
        if (mode == 8 || mode == 11) System.out.println("Lần mua gần nhất: " + invoices[invoices.length - 1].getPaymentDate().format(InputHelper.DATE));
    }
    public void showCustomers() {
        String header = String.format("%-8s %-25s %-15s", "Mã", "Tên", "SĐT");
        String border = "-".repeat(header.length());
        System.out.println(border);
        System.out.println(header);
        System.out.println(border);
        for (Customer c : customers.getCustomers())
            System.out.println(c);
        System.out.println(border);
        System.out.println("Số khách hàng: " + customers.getCustomers().size());
    }

    public void findCustomer() {
        while (true) {
            String id = input.readExistingCode("KH", "Mã khách hàng (0 để quay lại): ", "0");
            if (id.equals("0")) return;
            Customer c = customers.findCustomer(id);
            if (c != null) { System.out.println(c); return; }
            System.out.println("Sai mã khách hàng: " + id + ". Vui lòng nhập lại.");
        }
    }
    private Employee requireEmployee() {
        String id = input.readExistingCode("NV", "Mã nhân viên: ");
        Employee e = session.getEmployees().findById(id);
        if (e == null) throw new IllegalArgumentException("Không tìm thấy nhân viên có mã " + id);
        System.out.println("Đã tìm thấy nhân viên: " + e.getId() + " - " + e.getName());
        return e;
    }
    private void findEmployee() {
        while (true) {
            String id = input.readExistingCode("NV", "Mã nhân viên (0 để quay lại): ", "0");
            if (id.equals("0")) return;
            Employee employee = session.getEmployees().findById(id);
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
    private String newEmployeeId() {
        String id = input.readNewCode("NV", "Mã nhân viên: ");
        if (session.getEmployees().findById(id) != null) throw new IllegalArgumentException("Không thể thêm nhân viên: Mã nhân viên đã tồn tại.");
        return id;
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
        for (Employee current : session.getEmployees().getAll()) {
            if ((replacingId == null || !current.getId().equalsIgnoreCase(replacingId))
                    && current.getPosition().equalsIgnoreCase("Quản lý")) managers++;
        }
        for (Employee employee : proposed)
            if (employee.getPosition().equalsIgnoreCase("Quản lý")) managers++;
        if (managers > 1) throw new IllegalArgumentException("Cửa hàng chỉ được có một quản lý.");
    }
    private void manageEmployees() {
        while (true) {
            InputHelper.options("QUẢN LÝ NHÂN VIÊN", "Thêm nhân viên", "Hiển thị danh sách", "Tìm theo mã",
                    "Tìm theo tên", "Tìm theo điện thoại", "Tìm theo chức vụ", "Sửa thông tin", "Xóa nhân viên",
                    "Thống kê theo chức vụ", "Thêm nhiều nhân viên");
            int choice = input.readInt("Chọn: ");
            try {
                switch (choice) {
                    case 0 -> { return; }
                    case 1 -> {
                        Employee e = readEmployee(newEmployeeId());
                        ensureSingleManager(new Employee[] { e }, null);
                        session.change(() -> session.getEmployees().add(e));
                        System.out.println("Thêm nhân viên thành công! Đã lưu dữ liệu.");
                    }
                    case 2 -> {
                        int count = 0;
                        for (Employee p : session.getEmployees().getAll()) { System.out.println(p); count++; }
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
                            for (Employee e : session.getEmployees().getAll()) {
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
                        session.change(() -> { if (!session.getEmployees().updateById(e.getId(), replacement)) throw new IllegalStateException("Không tìm thấy nhân viên."); });
                        System.out.println("Cập nhật nhân viên thành công! Đã lưu dữ liệu.");
                    }
                    case 8 -> {
                        Employee e = requireEmployee();
                        accounts.deleteEmployeeWithAccount(e.getId(), () -> session.change(() -> {
                            if (!session.deletePerson(e.getId(), false)) throw new IllegalStateException("Không tìm thấy nhân viên.");
                        }));
                        System.out.println("Xóa nhân viên và tài khoản liên kết thành công! Đã lưu dữ liệu.");
                    }
                    case 9 -> {
                        var counts = new LinkedHashMap<String, Integer>();
                        for (Employee e : session.getEmployees().getAll()) counts.merge(e.getPosition(), 1, Integer::sum);
                        if (counts.isEmpty()) System.out.println("Chưa có nhân viên.");
                        counts.forEach((role, count) -> System.out.println(role + ": " + count));
                        if (!counts.isEmpty()) System.out.println("Thống kê nhân viên theo chức vụ thành công.");
                    }
                    case 10 -> {
                        int n = input.readInt("Số lượng nhân viên cần thêm (1..1000): ");
                        if (n < 1 || n > 1000) throw new IllegalArgumentException("Số lượng phải trong 1..1000.");
                        Employee[] batch = new Employee[n];
                        for (int i = 0; i < n; i++) {
                            System.out.println("Nhân viên thứ " + (i + 1) + "/" + n);
                            batch[i] = readEmployee(newEmployeeId());
                        }
                        Employee[] employees = batch;
                        ensureSingleManager(employees, null);
                        session.change(() -> { for (Employee item : batch) session.getEmployees().add(item); });
                        System.out.println("Đã thêm " + n + " nhân viên vào danh sách. Đã lưu dữ liệu.");
                    }
                    default -> System.out.println("Lựa chọn không hợp lệ.");
                }
            } catch (IllegalArgumentException | IllegalStateException | java.io.IOException e) { System.out.println(e.getMessage()); }
        }
    }
    public void manageSuppliers() {
        while (true) {
            InputHelper.options("QUẢN LÝ NHÀ CUNG CẤP", "Danh sách nhà cung cấp", "Thêm nhà cung cấp",
                    "Sửa nhà cung cấp", "Xóa nhà cung cấp");
            int choice = input.readInt("Chọn: ");
            try {
                switch (choice) {
                    case 0 -> { return; }
                    case 1 -> listSuppliers();
                    case 2 -> {
                        Supplier supplier = readSupplier(input.readNewCode("NCC", "Mã nhà cung cấp: "));
                        session.change(() -> session.getSuppliers().add(supplier));
                        System.out.println("Thêm nhà cung cấp thành công! Đã lưu dữ liệu.");
                    }
                    case 3 -> {
                        Supplier current = requireSupplier();
                        Supplier replacement = readSupplier(current.getId());
                        session.change(() -> {
                            Supplier live = session.getSuppliers().findById(current.getId());
                            live.setName(replacement.getName());
                            live.setPhone(replacement.getPhone());
                        });
                        System.out.println("Cập nhật nhà cung cấp thành công! Đã lưu dữ liệu.");
                    }
                    case 4 -> {
                        Supplier current = requireSupplier();
                        for (String row : session.getStockEvents()) {
                            String[] fields = row.split("\\|", -1);
                            if ((fields.length == 4 && fields[0].equals("SOURCE") && fields[2].equalsIgnoreCase(current.getId()))
                                    || (fields.length == 8 && fields[0].equals("RECEIPT") && fields[5].equalsIgnoreCase(current.getId())))
                                throw new IllegalStateException("Không thể xóa nhà cung cấp đã có nguồn cung hoặc nhật ký nhập hàng.");
                        }
                        if (!input.read("Xóa " + current.getId() + " - " + current.getName() + "? Gõ DONGY: ").equals("DONGY")) {
                            System.out.println("Đã hủy thao tác.");
                            break;
                        }
                        session.change(() -> {
                            if (!session.getSuppliers().delete(current.getId()))
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

    private void listSuppliers() {
        var suppliers = session.getSuppliers().getAll();
        if (suppliers.isEmpty()) System.out.println("Chưa có nhà cung cấp.");
        else {
            suppliers.forEach(System.out::println);
            System.out.println("Tổng số nhà cung cấp: " + suppliers.size());
        }
    }

    private Supplier requireSupplier() {
        String id = input.readExistingCode("NCC", "Mã nhà cung cấp: ");
        Supplier supplier = session.getSuppliers().findById(id);
        if (supplier == null) throw new IllegalArgumentException("Không tìm thấy nhà cung cấp: " + id);
        return supplier;
    }

    private Supplier readSupplier(String id) {
        String name = input.read("Tên nhà cung cấp: ");
        String phone = input.readPhone("Số điện thoại (10 chữ số, bắt đầu bằng 0): ");
        return new Supplier(id, name, phone);
    }
    public void manageAccounts() {
        AccountList.require(AccountList.Permission.MANAGE_ACCOUNTS);
        while (true) {
            InputHelper.options("QUẢN LÝ TÀI KHOẢN", "Danh sách tài khoản", "Tạo tài khoản nhân viên",
                    "Khóa tài khoản", "Mở khóa tài khoản", "Đặt lại mật khẩu nhân viên", "Sửa tên đăng nhập/vai trò",
                    "Tìm tài khoản theo mã hoặc tên đăng nhập");
            int choice = input.readInt("Chọn: ");
            try {
                switch (choice) {
                    case 0 -> { return; }
                    case 1 -> listAccounts();
                    case 2 -> createEmployee();
                    case 3 -> { accounts.setActive(input.read("Mã tài khoản cần khóa: "), false); System.out.println("Đã khóa tài khoản."); }
                    case 4 -> { accounts.setActive(input.read("Mã tài khoản cần mở khóa: "), true); System.out.println("Đã mở khóa tài khoản."); }
                    case 5 -> {
                        String id = input.read("Mã tài khoản nhân viên: ");
                        char[] password = readConfirmedPassword();
                        try {
                            accounts.resetEmployeePassword(id, password);
                            System.out.println("Đã đặt mật khẩu mới cho tài khoản.");
                        } finally { java.util.Arrays.fill(password, '\0'); }
                    }
                    case 6 -> updateAccount();
                    case 7 -> findAccount();
                    default -> System.out.println("Lựa chọn không hợp lệ.");
                }
            } catch (IllegalArgumentException | IllegalStateException | java.io.IOException e) {
                System.out.println("Không thể hoàn tất: " + e.getMessage());
            }
        }
    }
    private void listAccounts() {
        for (Account account : accounts.repository().getAll()) printAccount(account);
    }
    private void findAccount() {
        while (true) {
            String key = input.read("Mã hoặc tên đăng nhập (0 để quay lại): ");
            if (key.equals("0")) return;
            if (key.isBlank()) { System.out.println("Vui lòng nhập mã hoặc tên đăng nhập."); continue; }
            Account account = accounts.repository().findById(key);
            if (account == null) account = accounts.repository().findByUsername(key);
            if (account != null) { printAccount(account); return; }
            System.out.println("Sai mã hoặc tên đăng nhập tài khoản. Vui lòng nhập lại.");
        }
    }
    private void printAccount(Account account) {
        System.out.printf("%s | %-20s | %-8s | nhân viên: %-8s | %s | %s%n", account.getId(), account.getUsername(),
                account.getRole(), account.getEmployeeId().isBlank() ? "-" : account.getEmployeeId(),
                account.isActive() ? "Hoạt động" : "Đã khóa", account.mustChangePassword() ? "Cần đổi mật khẩu" : "");
    }
    private void createEmployee() throws java.io.IOException {
        String employeeId = input.readExistingCode("NV", "Mã nhân viên: ");
        Employee employee = session.getEmployees().findById(employeeId);
        if (employee == null) throw new IllegalArgumentException("Không tìm thấy nhân viên.");
        if (accounts.repository().getAll().stream().anyMatch(a -> a.getEmployeeId().equalsIgnoreCase(employeeId)))
            throw new IllegalArgumentException("Nhân viên " + employeeId + " đã có tài khoản.");
        String username = input.read("Tên đăng nhập mới: ");
        char[] password = readConfirmedPassword();
        try {
            Account created = accounts.createEmployee(username, employeeId, employee, password);
            System.out.println("Tạo tài khoản thành công: " + created.getId());
        } finally { java.util.Arrays.fill(password, '\0'); }
    }
    private char[] readConfirmedPassword() {
        while (true) {
            char[] password = input.readPassword("Mật khẩu (3-128 ký tự): ");
            char[] confirmation = input.readPassword("Nhập lại mật khẩu: ");
            if (java.util.Arrays.equals(password, confirmation)) {
                java.util.Arrays.fill(confirmation, '\0');
                return password;
            }
            java.util.Arrays.fill(password, '\0');
            java.util.Arrays.fill(confirmation, '\0');
            System.out.println("Mật khẩu xác nhận không khớp. Vui lòng nhập lại.");
        }
    }
    private void updateAccount() throws java.io.IOException {
        String id = input.read("Mã tài khoản cần sửa: ");
        Account current = accounts.repository().findById(id);
        if (current == null) throw new IllegalArgumentException("Không tìm thấy tài khoản.");
        String username = input.read("Tên đăng nhập mới: ");
        System.out.println("1. ADMIN\n2. EMPLOYEE");
        int choice = input.readInt("Vai trò mới: ");
        Account.Role role = switch (choice) { case 1 -> Account.Role.ADMIN; case 2 -> Account.Role.EMPLOYEE;
            default -> throw new IllegalArgumentException("Vai trò không hợp lệ."); };
        String employeeId = current.getEmployeeId(); Employee linked = null;
        if (role == Account.Role.EMPLOYEE) {
            employeeId = input.readExistingCode("NV", "Mã nhân viên liên kết: ");
            linked = session.getEmployees().findById(employeeId);
        }
        accounts.updateAccount(id, username, role, employeeId, linked);
        System.out.println("Đã cập nhật tài khoản.");
    }
    private void checkout() {
        String id = input.readNewCode("HD", "Mã hóa đơn: ");
        if (id.isBlank() || id.contains("|")) throw new IllegalArgumentException("Mã hóa đơn không hợp lệ.");
        if (invoices.findById(id) != null) throw new IllegalArgumentException("Mã hóa đơn đã tồn tại: " + id);
        Customer customer;
        while (true) {
            String key = input.readExistingCode("KH", "Mã khách hàng: ");
            customer = customers.findCustomer(key);
            if (customer != null) break;
            System.out.println("Không tìm thấy khách hàng có mã " + key + ". Vui lòng nhập lại.");
        }
        Employee cashier;
        Account active = AccountList.currentAccount();
        if (active != null && active.getRole() == Account.Role.EMPLOYEE) {
            cashier = session.getEmployees().findById(active.getEmployeeId());
            if (cashier == null) throw new SecurityException("Tài khoản chưa liên kết nhân viên hợp lệ.");
        } else {
        while (true) {
            String key = input.readExistingCode("NV", "Mã nhân viên: ");
            Employee candidate = session.getEmployees().findById(key);
            if (candidate == null) {
                System.out.println("Không tìm thấy nhân viên có mã " + key + ". Vui lòng nhập lại.");
                continue;
            }
            try {
                cashier = invoices.requireInvoiceEmployee(candidate);
                break;
            } catch (IllegalArgumentException e) {
                System.out.println(e.getMessage() + " Vui lòng nhập mã nhân viên khác.");
            }
        }
        }
        Map<String, Integer> cart = new LinkedHashMap<>();
        System.out.println("Danh sách sách có thể mua:");
        InputHelper.books(books.getBooks().stream().filter(book -> book.getQuantity() > 0).toArray(Book[]::new));
        boolean finishedAddingBooks = false;
        while (!finishedAddingBooks) {
            String bookId = input.readExistingCode("S", "Mã sách cần lập hóa đơn: ");
            Book book = books.findBook(bookId);
            if (book == null) {
                System.out.println("Không tìm thấy sách có mã " + bookId + ". Vui lòng nhập lại.");
                continue;
            }
            if (book.getQuantity() == 0) {
                System.out.println("Sách đã hết hàng. Vui lòng chọn sách khác.");
                continue;
            }
            int quantity = input.readInt("Số lượng mua: ");
            if (quantity <= 0) {
                System.out.println("Số lượng phải lớn hơn 0.");
                continue;
            }
            long combinedQuantity = (long) cart.getOrDefault(book.getId(), 0) + quantity;
            if (combinedQuantity > book.getQuantity()) {
                System.out.println("Không đủ tồn kho cho tổng số lượng. Tồn hiện tại: " + book.getQuantity());
                continue;
            }
            cart.put(book.getId(), (int) combinedQuantity);
            printCart(cart);
            while (true) {
                String more = input.read("Có mua thêm sách không? (C/K): ");
                if (more.equalsIgnoreCase("C")) break;
                if (more.equalsIgnoreCase("K")) {
                    finishedAddingBooks = true;
                    break;
                }
                System.out.println("Vui lòng nhập C để thêm sách hoặc K để hoàn tất hóa đơn.");
            }
        }

        Invoice quote = invoices.prepareInvoice(id, customer.getId(), cashier, cart);
        InputHelper.invoice(quote);
        String answer;
        while (true) {
            answer = input.read("Nhập OK để đồng ý thanh toán hoặc HỦY để hủy hóa đơn: ").trim();
            if (answer.equalsIgnoreCase("OK") || answer.equalsIgnoreCase("HUY") || answer.equalsIgnoreCase("HỦY")) break;
            System.out.println("Lựa chọn không hợp lệ. Vui lòng nhập OK hoặc HỦY.");
        }
        if (answer.equalsIgnoreCase("HUY") || answer.equalsIgnoreCase("HỦY")) {
            quote.cancel();
            System.out.println("Đã hủy hóa đơn. Tồn kho và doanh thu không thay đổi.");
            return;
        }
        try {
            session.payInvoice(invoices, quote);
        } catch (IllegalArgumentException | IllegalStateException error) {
            throw new IllegalStateException("Thanh toán không thành công: " + error.getMessage(), error);
        }
        Invoice paid = invoices.findInvoice(id);
        System.out.println("THANH TOÁN THÀNH CÔNG!");
        InputHelper.invoice(paid);
        System.out.println("Tồn kho còn lại:");
        for (InvoiceDetail detail : paid.getDetails()) {
            Book remaining = books.findBook(detail.getBookId());
            System.out.println(remaining.getId() + " - " + remaining.getTitle() + ": " + remaining.getQuantity());
        }
    }
    private void printCart(Map<String, Integer> cart) {
        System.out.printf("%-10s %-28s %8s %12s %14s%n", "Mã sách", "Tên sách", "Số lượng", "Đơn giá", "Thành tiền");
        double total = 0;
        for (var entry : cart.entrySet()) {
            Book book = books.findBook(entry.getKey());
            double amount = book.getPrice() * entry.getValue(); total += amount;
            System.out.printf("%-10s %-28s %8d %12.0f %14.0f%n", book.getId(), book.getTitle(), entry.getValue(), book.getPrice(), amount);
        }
        System.out.printf(InputHelper.CURRENCY_LOCALE, "Tổng tiền tạm tính / phải thanh toán: %,.0f VNĐ%n", total);
    }
    public void manageInvoices() {
        AccountList.require(AccountList.Permission.VIEW_ALL_INVOICES);
        while (true) {
            InputHelper.options("QUẢN LÝ HÓA ĐƠN", "Xem tất cả", "Tìm theo mã hóa đơn", "Tìm theo mã khách hàng",
                    "Xem chi tiết hóa đơn", "Lọc theo ngày", "Lọc theo tháng", "Lọc theo năm", "Lọc theo khoảng thời gian",
                    "Sắp xếp theo ngày lập", "In hóa đơn theo mã");
            int choice = input.readInt("Chọn: ");
            try {
                if (choice == 0) return;
                Invoice[] result;
                switch (choice) {
                    case 1, 9 -> result = invoices.filter(java.time.LocalDate.MIN, java.time.LocalDate.MAX, null);
                    case 2, 4, 10 -> {
                        Invoice invoice = findInvoiceForMenu();
                        if (invoice == null) continue;
                        result = new Invoice[] { invoice };
                    }
                    case 3 -> {
                        String customerId = findCustomerIdForHistory();
                        if (customerId == null) continue;
                        result = invoices.history(customerId);
                    }
                    case 5, 6, 7, 8 -> {
                        var range = InputHelper.range(input, choice - 3);
                        result = invoices.filter(range[0], range[1], null);
                    }
                    default -> { System.out.println("Lựa chọn không hợp lệ."); continue; }
                }
                if (result.length == 0) System.out.println("Không có hóa đơn phù hợp.");
                for (Invoice invoice : result) InputHelper.invoice(invoice);
                System.out.println(result.length == 0 ? "Tìm/lọc hóa đơn không có kết quả."
                        : "Tìm/lọc hóa đơn thành công. Số hóa đơn: " + result.length);
            } catch (IllegalArgumentException | IllegalStateException | java.time.DateTimeException e) { System.out.println(e.getMessage()); }
        }
    }
    private Invoice findInvoiceForMenu() {
        while (true) {
            String id = input.readExistingCode("HD", "Mã hóa đơn (0 để quay lại): ", "0");
            if (id.equals("0")) return null;
            Invoice invoice = invoices.findInvoice(id);
            if (invoice != null) return invoice;
            System.out.println("Sai mã hóa đơn: " + id + ". Vui lòng nhập lại.");
        }
    }
    private String findCustomerIdForHistory() {
        while (true) {
            String id = input.readExistingCode("KH", "Mã khách hàng (0 để quay lại): ", "0");
            if (id.equals("0")) return null;
            if (customers.findCustomer(id) != null) return id;
            System.out.println("Sai mã khách hàng: " + id + ". Vui lòng nhập lại.");
        }
    }
    public void showOwnInvoices() {
        for (Invoice invoice : invoices.getInvoices()) printInvoice(invoice);
        if (invoices.getInvoices().isEmpty()) System.out.println("Bạn chưa lập hóa đơn nào.");
    }

    public void printInvoice(Invoice invoice) {
        System.out.println("\n============== HÓA ĐƠN ==============");
        System.out.println("Mã HĐ: " + invoice.getId());
        System.out.println("Khách hàng: " + invoice.getCustomerName());
        System.out.println("Nhân viên: " + invoice.getEmployeeName());
        System.out.println("--------------------------------------");

        System.out.println("Ngày lập: " + invoice.getCreatedDate());
        for (InvoiceDetail d : invoice.getDetails()) {
            System.out.printf("%-25s x%-3d %10.0f%n",
                    d.getBookTitle(), d.getQuantity(), d.getAmount());
        }

        System.out.println("--------------------------------------");
        System.out.printf(InputHelper.CURRENCY_LOCALE, "TỔNG TIỀN: %,.0f VNĐ%n", invoice.getTotal());
        System.out.println("======================================");
    }
}
