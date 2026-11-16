# Báo cáo nâng cấp BookStoreManagement

Cập nhật quản lý file theo yêu cầu mới: dữ liệu đã gộp về duy nhất `data/store.txt` (18 sách, 13 khách hàng, 6 nhân viên); bỏ các file mẫu/import/backup dư và dọn log, fixture kiểm thử cũ trong build. Các đường dẫn log và file mẫu trong nội dung báo cáo dưới đây mô tả các đợt kiểm tra trước, không phải file dữ liệu đang dùng.

Điều chỉnh giao diện theo yêu cầu tiếp theo: đã bỏ mục sắp xếp sách và lọc theo thể loại; thêm/sửa sách không còn hỏi thể loại. Bảng sách và kết quả tìm kiếm hiển thị tác giả thay cho thể loại. Trường dữ liệu và API thể loại/sắp xếp cũ được giữ để tương thích; sửa sách bảo toàn thể loại đã lưu. Bộ test giao diện được cập nhật theo menu mới: 34 + 64 + 77 = 175 kiểm tra đạt. Bản build đã được biên dịch lại.

## Phân tích và kế hoạch đã thực hiện

Đọc mã nguồn các package, hai bộ kiểm thử ban đầu và dữ liệu phiên bản 1. Các vấn đề chính: menu người trùng menu khách hàng; thiếu điện thoại nhân viên/thể loại/giá nhập; thiếu giỏ hàng có xác nhận, lịch sử mua hàng, bộ lọc thời gian và giao diện tồn kho; dữ liệu chỉ lưu khi thoát; lỗi ghi file sau thay đổi có nguy cơ làm phiên trong bộ nhớ khác dữ liệu trên đĩa.

Thứ tự xử lý: mở rộng model và codec tương thích dữ liệu cũ → thêm cơ chế lưu/hoàn tác → hoàn thiện service và menu → kiểm thử nghiệp vụ/di chuyển dữ liệu → biên dịch toàn bộ nguồn vào build → kiểm tra CLI thực tế và dữ liệu gốc.

## Các file đã sửa

| Nhóm | File |
|---|---|
| Model | `src/model/Book.java`, `Employee.java` |
| Business | `src/business/BookService.java`, `PersonService.java`, `InvoiceService.java`, `ReportService.java` |
| Data | `src/data/BookRepository.java`, `InvoiceRepository.java`, `PersonDirectory.java`, `StoreState.java`, `StoreFile.java`, `StoreTextCodec.java` |
| Presentation | `src/presentation/Main.java`, `MainMenu.java`, `BookMenu.java`, `CustomerMenu.java`, `InvoiceMenu.java` |
| Utility | `src/utility/InputHelper.java`, `StoreConfig.java` |
| Kiểm thử/tài liệu | `scripts/check.ps1`, `README.md`, báo cáo này và `SO_DO_LOP.md` |

`PersonMenu.java` có thay đổi tên menu từ yêu cầu trước, tiếp tục được giữ. Ba file `docs/BAO_CAO_VA_KE_HOACH.md`, `docs/CAU_TRUC_HIEN_TAI.md`, `docs/LEADER_MTRI.md` đã ở trạng thái bị xóa trước đợt nâng cấp này; không khôi phục hoặc sửa các thay đổi đó.

## Class mới

- `business.StoreSession`: điều phối một thao tác thay đổi + lưu file, hoàn tác toàn bộ trạng thái nếu thất bại.
- `presentation.EmployeeMenu`: giao diện chỉ quản lý nhân viên.
- `presentation.InventoryMenu`: tồn kho, nhập hàng, kiểm kê có lý do, cảnh báo, giá trị tồn kho và nhật ký.
- `presentation.ReportMenu`: lựa chọn thời gian và hiển thị các thống kê.
- `presentation.ConsoleView`: dùng chung định dạng bảng, hóa đơn và nhập khoảng thời gian.
- `tests/UpgradeChecks.java`: kiểm thử nghiệm thu mới, không thay đổi hai bộ test cũ.

Các lớp mới tách trách nhiệm còn thiếu; không tạo repository hoặc danh sách trung tâm thứ hai.

## Nghiệp vụ đã hoàn thiện

1. **Sách:** thêm, sửa, xóa hợp lệ; tìm mã/từ khóa; thể loại; giá nhập/giá bán; khoảng giá; sắp xếp tên/giá/tồn kho. Sửa thông tin không âm thầm đổi tồn kho. Sách đã bán bị chặn xóa.
2. **Khách hàng:** thêm, xem, tìm mã/từ khóa/điện thoại, sửa/xóa; hóa đơn thực tế tạo thành lịch sử mua hàng, tổng số hóa đơn, tổng chi tiêu, lần mua gần nhất và xếp hạng chi tiêu.
3. **Nhân viên:** thêm/xem/tìm mã/tên/điện thoại/chức vụ, sửa/xóa, thống kê chức vụ. Danh sách không hiển thị khách hàng. Nhân viên được chọn khi lập hóa đơn, không buộc dùng NV001.
4. **Mảng đối tượng:** vẫn thêm một phần tử, thêm k phần tử, nhập lại n phần tử, sửa/xóa/tìm kiếm và tăng kích thước mảng. Khách hàng và nhân viên có chức năng nhập lại riêng, giữ nguyên danh sách loại còn lại; chặn nhập lại khi đã có hóa đơn.
5. **Giỏ hàng:** hiển thị danh mục; tìm sách; cộng số lượng khi nhập cùng mã; kiểm tra tổng số lượng so với tồn; sửa/xóa dòng; xem tổng tạm tính; chọn khách hàng và nhân viên; xác nhận thanh toán bằng DONGY. Hủy/EOF không tạo hóa đơn, không trừ kho.
6. **Hóa đơn:** tìm mã/khách hàng, chi tiết có số lượng–đơn giá–thành tiền; bộ lọc ngày/tháng/năm/khoảng ngày; sắp xếp theo ngày. Giá/tên sách, tên và thông tin khách hàng/nhân viên lịch sử được bảo toàn.
7. **Doanh thu:** hôm nay/ngày/tháng/năm/khoảng ngày; doanh thu, số hóa đơn, số lượng sách, số khách hàng không trùng, doanh thu từng sách, sách bán chạy, doanh thu theo khách hàng. Ngày bắt đầu và kết thúc đều được tính. Chỉ đọc hóa đơn hoàn tất và đơn giá lịch sử.
8. **Kho:** xem/tìm tồn kho; nhập số dương; điều chỉnh số thực tế kèm lý do và xác nhận; cảnh báo ngưỡng mặc định 5, đổi ngưỡng và lưu cấu hình; sách hết hàng; giá trị nhập và giá trị bán; nhật ký nhập/kiểm kê/bán hàng từ khi nâng cấp.
9. **Lưu:** tự lưu sau mọi thao tác thay đổi thành công, hoặc menu 8/0. Khi lưu lỗi, khôi phục các mảng vào cùng repository để các service và view khách hàng/nhân viên không giữ repository cũ.

## Bảo toàn dữ liệu và lỗi được xử lý

- `data/store.txt` được giữ nguyên trong quá trình phát triển và kiểm thử; mọi fixture sử dụng thư mục riêng dưới build.
- Đọc cả `VERSION|1` và `VERSION|2`. Lần lưu file v1 tạo bản sao byte nguyên gốc `.v1.bak` trước khi ghi v2; nếu tên backup đã có nội dung khác thì tạo tên backup khác, không ghi đè backup cũ.
- V2 thêm thể loại/giá nhập của sách, điện thoại nhân viên, cấu hình ngưỡng kho và bản ghi STOCK. END kiểm tra số bản ghi chính, chi tiết và nhật ký kho.
- Dữ liệu cũ thiếu giá nhập có giá trị 0 (chưa biết), thể loại “Chưa phân loại”, điện thoại nhân viên trống. Tổng giá trị nhập có cảnh báo nếu còn sách chưa khai báo giá nhập.
- File không tồn tại: tạo mẫu một lần. File rỗng/hỏng/không đúng phiên bản: báo lỗi và dừng, giữ nguyên file. Không tự seed đè lên dữ liệu lỗi.
- Ghi snapshot bằng file tạm và atomic replace. Lỗi lưu trong thanh toán không để lại hóa đơn hoặc trừ kho một phần; có kiểm thử mô phỏng lỗi này.
- Nhập thêm kho kiểm tra số dương và tràn int; điều chỉnh không âm, lý do không rỗng. Không bán vượt tồn kho kể cả nhiều lần thêm cùng sách.
- Ngày `dd/MM/yyyy` được kiểm tra nghiêm ngặt, có thử ngày 31/02 và năm nhuận. API nhập ISO cũ vẫn được chấp nhận để tương thích test.
- Giữ cấu hình UTF-8 cho Scanner/System.out/System.err và cấu hình console Windows từ yêu cầu trước.

## Kiểm thử và tương thích

Hai bộ test cũ giữ nguyên nội dung: RegressionChecks (34) và LeaderChecks (64). Bộ UpgradeChecks bổ sung 76 kiểm tra, gồm luồng menu thực tế, chuyển đổi v1/backup có hóa đơn cũ, trường mới, nhập lại/thêm nhiều, cập nhật/xóa, lịch sử, giỏ hàng sửa/xóa/hủy/xác nhận, ngày/tháng/năm, tồn kho, báo cáo và rollback khi lỗi ghi file. Tổng **174 kiểm tra đạt** ở lần biên dịch và nghiệm thu cuối, log trong `build/final-check-5c06d01b345c4ff7bf00c47302fa0b4f/`.

`Main.run(args, Scanner)` và constructor menu ban đầu giữ giao thức nhập cũ cho mã kiểm thử và ví dụ đang gọi chúng. `Main.main` gọi `runInteractive(args, Scanner)` là giao diện nâng cấp; các test mới chạy đúng entry point này. Không thay hay xóa test cũ để làm test qua.

Máy hiện có JDK 27. Biên dịch toàn bộ bằng `javac -Xlint:all -encoding UTF-8 --release 17`: đích Java 17, không dùng API cao hơn Java 17. Kiểm thử thực thi bằng Java 27 trên máy; chưa chạy bằng JVM 17 riêng vì chưa cài JVM đó.

## Tiêu chí OOP

Sau nâng cấp có 35 class cấp ngoài trong src và 2 interface. Các class đều có constructor, kể cả constructor private của lớp tiện ích. Giữ nguyên Person abstract/getRole abstract, Customer/Employee kế thừa, đa hình qua Person[], IRepository/ICrudRepository, các mảng chính và cơ chế tự tăng kích thước, static hợp lý, file text UTF-8. Xem `SO_DO_LOP.md`.

Các điểm có thể trình bày ở tiêu chí sáng tạo: lịch sử mua hàng lấy từ hóa đơn thực; giá lịch sử; hoàn tác khi lưu lỗi; cảnh báo tồn kho có cấu hình; báo cáo theo thời gian.

## Phạm vi còn lại

- Chưa có hệ thống khuyến mãi: hóa đơn hiển thị giảm giá 0 và thực thanh toán bằng tổng tiền. Đây là mục tùy chọn khi hệ thống có khuyến mãi; không tạo menu khuyến mãi giả.
- Chưa có nhà cung cấp/phiếu nhập chi tiết; nhật ký kho hiện ghi ngày, mã sách, tồn trước/sau và lý do. Không tạo hệ thống nhập hàng riêng vượt phạm vi yêu cầu.
- Không sửa/hủy hóa đơn đã thanh toán hay hoàn tiền; hủy giỏ trước thanh toán đã hỗ trợ. Các tính năng sau thanh toán cần quy tắc hoàn kho/hoàn tiền riêng.
- Model tiền giữ kiểu double để tương thích API cũ; tổng hợp bảng báo cáo và giá trị kho có dùng BigDecimal, nhưng chưa chuyển toàn bộ model sang kiểu tiền chính xác cho tiền lẻ.
- Phạm vi sử dụng một tiến trình ghi một file. Chưa có khóa liên tiến trình.

## Chạy và kiểm tra build

Tại thư mục gốc, PowerShell:

```powershell
$sources = @(Get-ChildItem src -Recurse -Filter *.java | ForEach-Object { $_.FullName })
javac -Xlint:all -encoding UTF-8 --release 17 -d build @sources
if ($LASTEXITCODE -eq 0) { java -cp build presentation.Main }
```

Hoặc lệnh ngắn vẫn đúng:

```powershell
javac -encoding UTF-8 --release 17 -d build -sourcepath src src/presentation/Main.java
java -Dfile.encoding=UTF-8 -cp build presentation.Main
```

Script `scripts/check.ps1` biên dịch toàn bộ src và tests vào thư mục riêng, rồi chạy cả ba bộ test. Nếu PowerShell chặn script, dùng các lệnh trực tiếp trong README. `out/` không nằm trong classpath của lệnh chạy; bản chạy chính thức là `build/`.

Đã kiểm tra mọi file Java có class được biên dịch mới trong build. CLI thực tế cũng đã được chạy từ console Windows mã 437: tự chuyển sang 65001, hiện đúng menu mới và tiếng Việt. SHA-256 của `data/store.txt` trước và sau công việc đều là `56C6672938A69F40477ABFFDE989A12E3C6FBECC8008C509AF098DAAB513D346`.
