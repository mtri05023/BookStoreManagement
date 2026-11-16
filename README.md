# BookStoreManagement

Đồ án Java Terminal quản lý cửa hàng sách, dùng OOP, mảng đối tượng và file text. Các thao tác thay đổi dữ liệu được tự động lưu; nếu lưu lỗi, dữ liệu trong bộ nhớ được hoàn tác.

## Bắt đầu từ đây

- [Báo cáo nâng cấp, nghiệp vụ, định dạng file và kiểm thử](BAO_CAO_NANG_CAP.md)
- [Sửa nghiệp vụ thanh toán, trạng thái và thời điểm giao dịch](BAO_CAO_THANH_TOAN.md)
- [Sơ đồ lớp và đối chiếu OOP](SO_DO_LOP.md)
- [Quy tắc code chung và hướng dẫn từng module](QUY_TAC_CODE_CHUNG.md)

## Kiến trúc

`presentation -> business -> data`, cùng model và utility. Main tạo StoreState rồi truyền instance dùng chung qua constructor. PersonDirectory giữ Person[] thật; CustomerRepository và EmployeeRepository là hai view trên cùng mảng. Sách và hóa đơn dùng Book[] và Invoice[]. Các List trả ra chỉ là bản sao phục vụ thao tác/hiển thị.

```text
src/
  model/         Person, Customer, Employee, Book, Invoice, InvoiceDetail, ModelChecks
  data/          IRepository, ICrudRepository, PersonDirectory,
                 BookRepository, CustomerRepository, EmployeeRepository, InvoiceRepository,
                 StoreState, StoreTextCodec, StoreFile, SampleData
  business/      BookService, CustomerService, PersonService, InvoiceService, ReportService, StoreSession
  presentation/  Main, MainMenu, BookMenu, CustomerMenu, InvoiceMenu, PersonMenu,
                 EmployeeMenu, InventoryMenu, ReportMenu, ConsoleView
  utility/       StoreConfig, InputHelper, FileManager
tests/RegressionChecks.java
tests/LeaderChecks.java
tests/UpgradeChecks.java
scripts/check.ps1
```

## Chạy

Cần JDK 17 trở lên, chạy từ thư mục gốc bằng PowerShell:

```powershell
New-Item -ItemType Directory -Force build | Out-Null
$sources = @(Get-ChildItem src -Recurse -Filter *.java | ForEach-Object { $_.FullName })
javac -encoding UTF-8 --release 17 -d build @sources
if ($LASTEXITCODE -eq 0) { java -cp build presentation.Main }
```

File mặc định `data/store.txt`. Chưa có file: tạo mẫu một lần. Đã có file: nạp lại. File rỗng/hỏng: báo lỗi, không tạo mẫu đè lên. Menu 6 quản lý nhân viên, menu 7 quản lý tồn kho, menu 8 lưu toàn bộ dữ liệu, menu 0 lưu và thoát. Mỗi thao tác thay đổi tự lưu; lỗi lưu sẽ hoàn tác thao tác. Giỏ đang nhập bị hủy/EOF không trừ kho. Ngày trên giao diện nhập theo `dd/MM/yyyy`.

Mã mới có tiền tố hiển thị sẵn: sách `S`, khách hàng `KH`, nhân viên `NV`, hóa đơn `HD`. Chỉ cần nhập số, ví dụ nhập `1` tại ô mã sách sẽ thành `S001`; nhập `12` tại ô khách hàng thành `KH012`. Phần số phải dương, tối đa 9 chữ số; `1` và `001` là cùng một mã. Vẫn nhận mã đầy đủ đúng tiền tố khi sao chép từ danh sách. Mã dữ liệu cũ được giữ nguyên; nếu cần truy cập mã cũ không đúng mẫu (ví dụ `01`), nhập `=01` trong ô tìm/sửa/xóa để phân biệt với `S001`/`KH001`. Không thể tạo mới mã dạng cũ này.

Quy trình bán hàng: nhập mã hóa đơn → chọn khách hàng → chọn nhân viên → thêm/sửa/xóa sách trong giỏ → nhập `0` → gõ `DONGY` để thanh toán (không phân biệt hoa/thường), `N` để quay lại giỏ, `HUY` để kết thúc giao dịch. Nội dung sai sẽ được yêu cầu nhập lại. Dùng `DS` xem danh sách, `TIM` tìm sách, `GIO` xem giỏ, `SUA` đổi số lượng, `XOA` xóa dòng và `-1` hủy giỏ.

Ứng dụng chỉ dùng một file dữ liệu chính: `data/store.txt`. Dữ liệu nhập trước đây đã được gộp vào file này; các file mẫu, bản nhập riêng và backup dư đã được dọn. Chạy bình thường không cần tham số `--data`.

Theo yêu cầu dùng thử, đã bổ sung 40 hóa đơn mẫu `HD001`–`HD040` cho nhiều khách hàng/nhân viên, ở nhiều ngày/tháng/năm. Các hóa đơn mẫu đã thanh toán, được tính vào lịch sử và doanh thu, đồng thời trừ tồn kho tương ứng. Menu khách hàng đã bỏ mục 13 (thêm nhiều) và 14 (nhập lại danh sách).

Ứng dụng đọc được phiên bản 1, 2 và 3. Trước khi lưu file cũ sang phiên bản 3, bản gốc được sao lưu thành `<tên-file>.v1.bak` hoặc `.v2.bak`. V3 lưu trạng thái PAID, ngày giờ lập và thời điểm thanh toán. Hóa đơn cũ vẫn được giữ, thời điểm thanh toán chưa biết được hiển thị rõ, không tự gán thời gian hiện tại. Giá nhập cũ chưa biết vẫn lưu bằng 0 nhưng hiển thị “Chưa có”; thể loại được giữ trong file để tương thích, không hiện hoặc nhập trên menu sách.

## Kiểm thử

Dùng lệnh trực tiếp dưới đây. Tạo thư mục riêng mỗi lần vì kiểm tra gồm seed lần đầu và restart:

```powershell
$checkPath = Join-Path build ('check-' + [guid]::NewGuid().ToString('N'))
New-Item -ItemType Directory -Force $checkPath | Out-Null
$sources = @(Get-ChildItem src -Recurse -Filter *.java | ForEach-Object { $_.FullName })
$testSources = @(Get-ChildItem tests -Filter *.java | ForEach-Object { $_.FullName })
javac -Xlint:all -encoding UTF-8 --release 17 -d $checkPath @sources @testSources
if ($LASTEXITCODE -ne 0) { throw 'Compilation failed' }
java -cp $checkPath RegressionChecks $checkPath
if ($LASTEXITCODE -ne 0) { throw 'Regression checks failed' }
java -cp $checkPath LeaderChecks $checkPath
if ($LASTEXITCODE -ne 0) { throw 'Leader checks failed' }
java -cp $checkPath UpgradeChecks $checkPath
if ($LASTEXITCODE -ne 0) { throw 'Upgrade checks failed' }
java -cp $checkPath PaymentChecks $checkPath
if ($LASTEXITCODE -ne 0) { throw 'Payment checks failed' }
java -cp $checkPath CodeChecks $checkPath
if ($LASTEXITCODE -ne 0) { throw 'Code checks failed' }
```

## Phạm vi

Lưu một snapshot UTF-8 bằng file tạm và atomic replace, gồm kho, người, hóa đơn có snapshot lịch sử, nhật ký kho và ngưỡng cảnh báo. Thanh toán chỉ thành công sau khi toàn bộ hóa đơn và tồn kho được lưu. Chưa hỗ trợ nhiều tiến trình cùng ghi. Tiền vẫn dùng double và có kiểm tra hữu hạn; báo cáo tổng hợp có sử dụng BigDecimal, nhưng chưa bảo đảm chính xác tuyệt đối với tiền lẻ trong mọi API.

Phú/Nam/Long/Hoàng tiếp tục các chức năng module theo tài liệu bàn giao. Không tự commit/push/merge; build/ và runtime data/ được Git bỏ qua, samples/ được theo dõi.
