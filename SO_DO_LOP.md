# Quản lý cửa hàng sách

## Chạy chương trình

Dùng JDK 17 trở lên. Từ thư mục chứa `src` và `data`, chạy trong Windows Command Prompt:

```bat
javac -encoding UTF-8 -d out src\bookstore\*.java
java -cp out bookstore.Main --data data/store.txt
```

Trong IDE, đặt `src` làm thư mục mã nguồn và chạy `bookstore.Main`. Dữ liệu tài khoản đi kèm nằm trong `data/store.txt.accounts`. Chạy bộ kiểm tra trên bản sao dữ liệu bằng `python tests/regression_console.py`.

## Các lớp trong package bookstore

| Nhóm | Lớp và vai trò |
|---|---|
| Sách | `Book` giữ thông tin chung. `CategorizedBook` lưu nhãn do cửa hàng nhập. `UnclassifiedBook` biểu diễn sách chưa được phân loại. |
| Danh sách trung tâm | `BookList` quản lý mảng `Book[]`, tăng sức chứa, tìm, sửa, xóa, lọc và sắp xếp. Mục 1 → 9 cho nhập nhiều sách. Mục 1 → 10 gọi `getCategory()` trên từng sách để đếm số đầu sách theo thể loại. |
| Khách hàng, nhân viên | `Customer`, `CustomerList`, `Employee`, `EmployeeList` quản lý hai mảng đối tượng riêng. |
| Nhà cung cấp | `Supplier`, `SupplierList` lưu và tra cứu nhà cung cấp. |
| Bán hàng | `Invoice` chứa mảng `InvoiceDetail[]`. `InvoiceDetail` giữ giá và tên sách lúc bán. `InvoiceList` quản lý hóa đơn, kiểm tra giỏ hàng và thanh toán. |
| Tài khoản | `Account` lưu thông tin đăng nhập. `AccountList` kiểm tra đăng nhập, quyền và thay đổi tài khoản. |
| Báo cáo | `SalesReport` tính doanh thu. `InventoryReport` hiển thị và xử lý các lựa chọn quản lý kho. |
| Chạy và lưu | `Main` khởi động. `CatalogMenu` hiển thị menu theo quyền. `BookStore` phối hợp thay đổi dữ liệu và khôi phục khi lưu lỗi. `StoreFile` đọc/ghi file text. `InputHelper` nhập, kiểm tra và hiển thị dữ liệu. |

Có 22 lớp cấp cao nhất và một interface. `Identifiable` quy định `getId()`; hàm `indexOf` dùng chung khi tìm mã trong mảng sách, khách hàng và nhân viên. Các enum trong mã mô tả quyền và trạng thái, các record hỗ trợ đọc dòng dữ liệu và tính báo cáo.
