# BookStoreManagement

Đồ án Java Terminal quản lý cửa hàng sách. Phần Leader của Minh Trí đã có mẫu chạy được theo tiêu chí OOP, mảng đối tượng và file text của giảng viên.

## Bắt đầu từ đây

- [Bàn giao Leader, đối chiếu tiêu chí, sơ đồ lớp và kịch bản demo](docs/LEADER_MTRI.md)
- [Cấu trúc và API hiện tại để cả nhóm code tiếp](docs/CAU_TRUC_HIEN_TAI.md)
- [Quy tắc code chung và hướng dẫn từng module](QUY_TAC_CODE_CHUNG.md)
- [Báo cáo khảo sát ban đầu và kế hoạch tuần](docs/BAO_CAO_VA_KE_HOACH.md) — hiện trạng cũ, không dùng để xác định lớp/API hiện còn thiếu.

## Kiến trúc

`presentation -> business -> data`, cùng model và utility. Main tạo StoreState rồi truyền instance dùng chung qua constructor. PersonDirectory giữ Person[] thật; CustomerRepository và EmployeeRepository là hai view trên cùng mảng. Sách và hóa đơn dùng Book[] và Invoice[]. Các List trả ra chỉ là bản sao phục vụ thao tác/hiển thị.

```text
src/
  model/         Person, Customer, Employee, Book, Invoice, InvoiceDetail, ModelChecks
  data/          IRepository, ICrudRepository, PersonDirectory,
                 BookRepository, CustomerRepository, EmployeeRepository, InvoiceRepository,
                 StoreState, StoreTextCodec, StoreFile, SampleData
  business/      BookService, CustomerService, PersonService, InvoiceService, ReportService
  presentation/  Main, MainMenu, BookMenu, CustomerMenu, InvoiceMenu, PersonMenu
  utility/       StoreConfig, InputHelper, FileManager
samples/store.txt
tests/RegressionChecks.java
tests/LeaderChecks.java
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

File mặc định data/store.txt. Chưa có file: tạo mẫu một lần. Đã có file: nạp lại. File rỗng/hỏng: báo lỗi, không seed đè. Menu 6 là mẫu mảng/đa hình đầy đủ; menu 7 lưu text; menu 0 lưu và thoát. Lỗi lưu ở menu 0 giữ phiên để người dùng xử lý và thử lại. Giỏ đang nhập bị hủy/EOF không trừ kho.

Demo trên file riêng chưa tồn tại:

```powershell
java -cp build presentation.Main --data build/demo-store.txt
```

Chạy lại cùng đường dẫn để kiểm tra dữ liệu sau khởi động lại. samples/store.txt là mẫu schema phiên bản 1; không chạy demo ghi trực tiếp lên file mẫu đã theo dõi bằng Git.

## Kiểm thử

Nếu máy cho phép script: `powershell -NoProfile -File scripts/check.ps1`.

Nếu execution policy chặn script, dùng lệnh trực tiếp dưới đây. Tạo thư mục riêng mỗi lần vì kiểm tra gồm seed lần đầu và restart:

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
```

## Phạm vi

Lưu một snapshot UTF-8 bằng file tạm và atomic replace, gồm kho, người và hóa đơn có snapshot lịch sử. Thay đổi chưa lưu có thể mất nếu tiến trình bị tắt cưỡng bức; không tự lưu từng lần bán, chưa hỗ trợ nhiều tiến trình cùng ghi. Tiền vẫn dùng double và có kiểm tra hữu hạn, chưa bảo đảm chính xác tuyệt đối với tiền lẻ.

Phú/Nam/Long/Hoàng tiếp tục các chức năng module theo tài liệu bàn giao. Không tự commit/push/merge; build/ và runtime data/ được Git bỏ qua, samples/ được theo dõi.
