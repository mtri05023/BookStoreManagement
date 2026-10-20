# BookStoreManagement

Ứng dụng quản lý cửa hàng sách bằng Java thuần, chạy trên Terminal. Minh Trí phụ trách kiến trúc và tích hợp nhóm.

## Bắt đầu làm việc

- [Cấu trúc, hợp đồng hiện tại và phần bàn giao](docs/CAU_TRUC_HIEN_TAI.md)
- [Quy tắc code chung và hướng dẫn cho từng thành viên](QUY_TAC_CODE_CHUNG.md)
- [Báo cáo khảo sát ban đầu và kế hoạch tuần](docs/BAO_CAO_VA_KE_HOACH.md) — phần hiện trạng là lịch sử trước các lần sửa cấu trúc.

## Kiến trúc

Ba tầng xử lý: `presentation -> business -> data`; `model` chứa đối tượng nghiệp vụ, `utility` chứa tiện ích. Dùng duy nhất `presentation` cho giao diện.

```text
src/
  model/         Person, ModelChecks, Customer, Employee, Book, Invoice, InvoiceDetail
  data/          IRepository, ICrudRepository, BookRepository, CustomerRepository, InvoiceRepository
  business/      BookService, CustomerService, InvoiceService, ReportService
  presentation/  Main, MainMenu, BookMenu, CustomerMenu, InvoiceMenu
  utility/       StoreConfig, InputHelper, FileManager
tests/RegressionChecks.java
scripts/check.ps1
```

## Chạy chương trình

Cần JDK 17 trở lên. Tại thư mục gốc repo trong PowerShell:

```powershell
New-Item -ItemType Directory -Force build | Out-Null
$sources = @(Get-ChildItem src -Recurse -Filter *.java | ForEach-Object { $_.FullName })
javac -encoding UTF-8 --release 17 -d build @sources
if ($LASTEXITCODE -eq 0) { java -cp build presentation.Main }
```

Kiểm tra hồi quy bằng `powershell -NoProfile -File scripts/check.ps1` nếu máy cho phép chạy script. Nếu execution policy chặn, dùng các lệnh trực tiếp, không cần đổi policy:

```powershell
New-Item -ItemType Directory -Force build/check | Out-Null
$sources = @(Get-ChildItem src -Recurse -Filter *.java | ForEach-Object { $_.FullName })
javac -Xlint:all -encoding UTF-8 --release 17 -d build/check @sources tests/RegressionChecks.java
if ($LASTEXITCODE -eq 0) { java -cp build/check RegressionChecks build/check }
```

Dữ liệu hiện nằm trong bộ nhớ, seed lại mỗi lần chạy. FileManager chưa được nối để tự nạp/lưu repository. Bán hàng kiểm tra cả giỏ và lưu vào repository bộ nhớ trước khi trừ kho; chưa phải giao dịch lưu file an toàn. Thư mục build và file class được bỏ qua bởi Git.
