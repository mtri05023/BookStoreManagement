package data;

import model.Book;
import model.Customer;
import model.Employee;
import model.Supplier;
import utility.StoreConfig;

/** Fake data for the first run; never merged over a saved snapshot. */
public final class SampleData {
    private SampleData() { }

    public static StoreState create() {
        StoreState state = new StoreState();
        state.getBooks().add(new Book("S001", "Java Core", "Tác giả mẫu", "Lập trình", 100000, 150000, 20));
        state.getBooks().add(new Book("S002", "Lập trình OOP", "Tác giả mẫu", "Lập trình", 65000, 100000, 10));
        state.getCustomers().add(new Customer("KH001", "Khách hàng mẫu", "0900000000"));
        state.getCustomers().add(new Customer("KH002", "Khách hàng thứ hai", "0910000000"));
        state.getEmployees().add(new Employee(StoreConfig.DEFAULT_EMPLOYEE_ID, "Nhân viên mẫu", "Thu ngân"));
        addDefaultSuppliers(state);
        return state;
    }

    public static void addDefaultSuppliers(StoreState state) {
        state.getSuppliers().add(new Supplier("NCC001", "Nhà sách Minh Châu", "0900000201"));
        state.getSuppliers().add(new Supplier("NCC002", "Công ty Sách Ánh Dương", "0900000202"));
        state.getSuppliers().add(new Supplier("NCC003", "Nhà phân phối Tri Thức", "0900000203"));
    }
}
