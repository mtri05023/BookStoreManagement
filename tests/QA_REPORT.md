# Kết quả kiểm thử console

Entry point: `presentation.Main`. Bộ kiểm thử: `tests/regression_console.py`. Mỗi lần chạy tạo thư mục tạm trong `data`, sao chép `data/store.txt`, biên dịch toàn bộ `src` với `--release 17`, chạy tiến trình Java mới với `--data`, rồi xóa bản sao. Không dùng mật khẩu hoặc tệp tài khoản gốc. Trong repository không có bộ test cũ.

Lần chạy toàn bộ gần nhất sau khi bỏ lựa chọn 14: `python -u tests/regression_console.py` trả exit code 0, **19 nhóm ca PASS**, không có nhóm ca FAIL. Fixture ép lỗi lưu file được chạy bằng `tests/FailureInjection.java` trong cùng bộ test.

Máy kiểm thử có JDK 27 (`java 27`, `javac 27`). Đã kiểm tra `where javac`, các thư mục cài Java/JDK thường dùng và registry JavaSoft; chưa tìm thấy JDK 17. Lệnh biên dịch `javac --release 17 -encoding UTF-8` thành công; các ca thực thi chạy trên Java 27. Cần chạy thêm trên JDK 17 thật khi máy có sẵn.

## Ánh xạ menu đã đọc từ mã và chọn trong console

| Vai trò / menu | Lựa chọn → hàm hoặc nhánh thực tế |
| --- | --- |
| ADMIN | 1→`BookMenu.show`; 2→`CustomerMenu.showManaged`; 3→`EmployeeMenu.show`; 4→`SupplierMenu.show`; 5→`InvoiceMenu.checkout`; 6→`InvoiceMenu.manage`; 7→`InventoryMenu.show`; 8→`ReportMenu.show`; 9→`AccountMenu.show`; 10→`MainMenu.changePassword`; 11→`StoreSession.save`; 0→lưu và đăng xuất. |
| EMPLOYEE | 1→`BookMenu.showForEmployee`; 2→`CustomerMenu.showForEmployee`; 3→`InvoiceMenu.checkout`; 4→`InvoiceMenu.showOwnInvoices`; 5→`InventoryMenu.showForEmployee`; 6→`MainMenu.changePassword`; 0→lưu và đăng xuất. |
| ADMIN / Sách | 1→thêm qua `BookService.addBook`; 2→danh sách; 3→`findBook`; 4→`search`; 5→sửa trường qua `updateBook` hoặc `StoreSession.renameBook`; 6→`deleteBook`; 7→`filterPrice`; 8→danh sách/tồn; 9→thêm nhiều. |
| EMPLOYEE / Sách | 1→danh sách; 2→`findBook`; 3→`search`; 4→`filterPrice`; 5→`sorted`. |
| ADMIN / Khách | 1→`StoreSession.addCustomer`; 2→danh sách; 3→tìm mã; 4→`PersonService.search`; 5→tìm SĐT; 6→sửa trường/đổi mã; 7→`deleteCustomer`; 8→lịch sử; 9→số hóa đơn; 10→tổng chi tiêu; 11→lần mua gần nhất; 12→xếp chi tiêu; 13→`addCustomers`. Lựa chọn 14 đã bị xóa. |
| EMPLOYEE / Khách | 1→danh sách; 2→tìm mã; 3→`findByName`; 4→`addCustomer`; 5→`updateCustomerFields`. |
| ADMIN / Nhân viên | 1→`PersonService.add`; 2→danh sách; 3→tìm mã; 4→tìm tên; 5→tìm SĐT; 6→tìm chức vụ; 7→`updateById`; 8→`AccountService.deleteEmployeeWithAccount`; 9→thống kê chức vụ; 10→`addAll`. |
| ADMIN / Nhà cung cấp | 1→danh sách; 2→thêm; 3→sửa; 4→xóa có kiểm tra nhật ký. |
| ADMIN / Hóa đơn | 1→xem tất cả; 2→tìm mã; 3→lịch sử khách; 4→chi tiết; 5→lọc ngày; 6→lọc tháng; 7→lọc năm; 8→lọc khoảng; 9→sắp ngày; 10→in theo mã. |
| ADMIN / Tồn kho | 1→xem; 2→tìm mã; 3→tìm tên; 4→`receiveStock`; 5→`stock`; 6→sắp hết; 7→hết; 8→giá trị tồn; 9→đổi ngưỡng. |
| ADMIN / Doanh thu | 1→hôm nay; 2→ngày; 3→tháng; 4→năm; 5→khoảng; 6→số sách bán; 7→doanh thu sách; 8→top sách; 9→hóa đơn; 10→khách hàng; 11→nhà cung cấp. Tất cả gọi `ReportMenu.summary` với khoảng và mode tương ứng. |
| ADMIN / Tài khoản | 1→danh sách; 2→`createEmployee`; 3→`setActive(false)`; 4→`setActive(true)`; 5→`resetEmployeePassword`; 6→`updateAccount`; 7→tìm theo mã hoặc tên đăng nhập (mới, chưa chạy test). |

## Ca đã đối chiếu

| Vai trò | Đường dẫn menu | Dữ liệu thử | Mong đợi | Thực tế | Trạng thái |
| --- | --- | --- | --- | --- | --- |
| ADMIN | 4→2 | NCC990; 4 SĐT sai rồi `0912345678` | Hỏi lại 4 lần, tạo 1 NCC | 4 thông báo, 1 bản ghi sau lưu | PASS |
| ADMIN | 2→1,6,13 | KH990/KH991; SĐT trùng, sửa tên, sửa SĐT, 2 khách cùng SĐT trong batch | Chặn trùng, giữ dữ liệu cũ; sửa tên không đổi SĐT; batch hoàn tác | Đúng dữ liệu trong file, không có KH997/KH998 | PASS |
| EMPLOYEE | 2→3,2 | Tên chứa `An`; mã KH990 | Tên thấy KH990 và KH991; mã thấy KH990 | Đúng lời nhắc và kết quả console | PASS |
| ADMIN/EMPLOYEE | 3→8; 9→1; đăng nhập | NV990, tài khoản `qaemployee` | Xóa cả hai, không đăng nhập được trước/sau khởi động lại | Tài khoản biến mất; cả hai lần đăng nhập bị từ chối | PASS |
| EMPLOYEE | Đăng nhập | `orphanqa` liên kết NV999 không tồn tại | Từ chối dù mật khẩu đúng | Bị từ chối | PASS |
| ADMIN | 1→5; 2→6 | Sửa riêng tên sách, giá bán, SĐT khách; chọn 0; đổi mã trùng; đổi mã chưa giao dịch | Chỉ trường chọn đổi; 0 không lưu; trùng bị chặn | S991 và KH993 lưu đúng; các trường khác giữ nguyên | PASS |
| ADMIN | 1→5; 2→6 | Đổi mã S017/KH990 đã có HD | Chặn để giữ lịch sử | Đều bị chặn rõ ràng | PASS |
| ADMIN | 5→6→2; 2→8 | HD990, S017 giá 170.000, tồn 10, mua 1 | HD 170.000, tồn 9, tra được sau mở lại | Đúng trong console và file | PASS |
| ADMIN | 5 | HD991 cùng sách, chọn HUY | Không ghi HD, tồn 9 | Không có HD991, tồn 9 | PASS |
| ADMIN | 8→1..11, mở báo cáo | Bộ hóa đơn mẫu và HD990 | Mỗi lựa chọn vào báo cáo | In đủ 11 báo cáo, có bảng khách/NCC | PASS |
| ADMIN | 8→1..11, số liệu chính xác | Fixture riêng: 2 ngày, KH001/KH002, NCC001/NCC002, S001/S002/S003, HD001/HD002 và HD003 hủy | Hôm nay 550; hôm trước 400; toàn kỳ 950, 8 cuốn; S002 3/600, S003 3/150, S001 2/200; KH002 550, KH001 400; Alpha 5/350 đứng trước Beta 3/600 | Khớp từng số tổng, số hóa đơn, số cuốn, thứ tự sách/khách/NCC và danh sách hóa đơn; HD003 không được ghi | PASS |
| ADMIN | 7→1..9; 4→4 | S017, NCC990, nhập/điều chỉnh, xóa NCC có nhật ký | Tồn đúng; chặn xóa NCC được tham chiếu | Tồn 9 và chặn xóa | PASS |
| ADMIN | 2→2..13; 6→1..10 | KH990, HD990, batch KH995/996 | Mỗi mục còn lại chạy; thêm nhiều vẫn hoạt động | Đúng thông báo và dữ liệu file | PASS |
| ADMIN | 1→1..9; 3→1..10; 4→1..4; 9→1..6 | S990..993, NV990..994, NCC990/991, A002 | Thêm/sửa/xóa/lọc/tra cứu và cập nhật tài khoản | Các bản ghi đã đối chiếu trong file; menu chạy đủ nhánh | PASS |
| EMPLOYEE | 1→1..5; 2→1..5; 3..6 | NV992, tài khoản đổi tên/mật khẩu | Xem, tìm, thêm/sửa khách, hủy HD, xem hóa đơn riêng, xem kho, đổi mật khẩu | Các đầu ra và file đã đối chiếu | PASS |
| Khởi động | `--data` bản sao của `store.txt.v3.bak` | Dữ liệu phiên bản 3 | Đọc được dữ liệu cũ và tới màn hình đăng nhập | Đã tới lời nhắc tên đăng nhập, exit 0 | PASS |
| ADMIN | Khách hàng→14 trước và sau hóa đơn | Hai fixture, một không có và một có hóa đơn | Menu không còn mục 14; nhập 14 báo lựa chọn không hợp lệ; dữ liệu không đổi | Cả hai fixture báo lựa chọn không hợp lệ; danh sách không đổi | PASS |
| ADMIN | Sách→Thêm | Mã `0`, `1234567890`, `9`, `S009`, `123456789` | 0 và 10 chữ số sai; 9 chuẩn hóa S009; trùng S009 bị chặn; 9 chữ số được nhận | Khớp; chỉ một S009 và một S123456789 | PASS |
| ADMIN | Lập hóa đơn | S001 tồn 5, mua đúng 5; S002 tồn 8, mua 9; thêm S002 hai lần 5+4 | Mua đúng tồn còn 0; hai ca vượt tồn bị chặn và không lưu hóa đơn | HD010 lưu, S001=0; không có HD011/HD012, S002/S003 giữ nguyên | PASS |
| ADMIN | Nhân viên→Xóa, ép lỗi lưu | NV009/A002, `StoreFile.save` báo IOException sau khi file tài khoản đã lưu | Hoàn tác cả hai bên ngay và sau nạp lại | NV009 và A002 còn trong bộ nhớ và hai file sau nạp lại | PASS |
| ADMIN | Khách hàng→Tìm SĐT; Nhân viên→Tìm SĐT | `09ab` rồi `0900000001`; `abc` rồi `0900000003` | Từ chối chữ, hỏi lại ngay; từ khóa số tìm đúng người | Hai thông báo nhập lại; tìm thấy đúng KH001 và NV001; dữ liệu không đổi | PASS |

## Chưa chạy hoặc chưa đối chiếu đủ

| Nhóm | Trạng thái | Còn thiếu |
| --- | --- | --- |
| Runtime JDK 17 | CHƯA CHẠY | Máy chỉ có JDK 27; mới kiểm tra bytecode/nguồn bằng `--release 17`. |
| Tình huống nhập liệu ngoài nhóm ưu tiên | CHƯA CHẠY | Chưa chạy hết ma trận rỗng/âm/quá 9 chữ số cho mọi loại mã, sai ngày, sai mật khẩu hiện tại và xác nhận mật khẩu lệch. Các ca mã sách và mua vượt tồn ưu tiên đã PASS ở trên. |
| Lỗi ghi đồng thời khi hoàn tác | CHƯA CHẠY | Đã ép lỗi ghi file cửa hàng sau khi file tài khoản lưu và xác nhận hoàn tác. Chưa mô phỏng thêm lỗi I/O thứ hai đúng lúc lưu lại tài khoản khi hoàn tác. |

## Lỗi mới trong lần kiểm thử này

Không phát hiện lỗi ứng dụng mới ở các ca ưu tiên. Lần chạy đầu của fixture báo cáo thất bại vì script test thiếu một dòng nhập số lượng: sau lời nhắc `Số lượng mua`, script gửi `K`, nên console tiếp tục hỏi số nguyên và ca không tới bước HỦY. Đã thêm số lượng `1` vào `tests/regression_remaining.py`; chạy riêng các ca mới sau sửa đều PASS. Đây là lỗi dữ liệu đầu vào của test, không phải lỗi xử lý hóa đơn.

Sau đó phát hiện thiếu ràng buộc ở hai ô tìm kiếm SĐT: trước sửa nhập chữ vẫn được dùng làm từ khóa và trả kết quả rỗng. Đã thêm `InputHelper.readPhoneKeyword` và dùng tại `CustomerMenu`/`EmployeeMenu`; sau sửa nhập chữ được báo lỗi và hỏi lại, còn chuỗi số có thể tìm theo một phần SĐT. Ca console tương ứng PASS.

Chạy lại trong PowerShell: đặt `JAVA_HOME` tới JDK 17 nếu có, rồi `python -u tests/regression_console.py`. Script tự biên dịch từ source, không dùng `build` hiện có. Bộ test chỉ đọc `data/store.txt` để tạo bản sao và thao tác trên bản sao. Tệp gốc đã có thay đổi trong phiên làm việc ngày 25/11/2026; các lệnh test không ghi vào tệp gốc.
