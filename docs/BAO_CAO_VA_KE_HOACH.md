# Báo cáo hiện trạng và kế hoạch nhóm BookStoreManagement

> Đây là báo cáo khảo sát ban đầu. Sau khi bổ sung và sửa nền, xem [cấu trúc và hợp đồng hiện tại](CAU_TRUC_HIEN_TAI.md) để biết API đã có, lỗi đã sửa và phần bàn giao còn lại. Không dùng bảng hiện trạng cũ bên dưới để kết luận các lớp hiện vẫn thiếu.

Ngày khảo sát: 20/10/2026. Leader: Minh Trí (Mtri).

Tài liệu dựa trên 13 file Java và README hiện có. Đây là báo cáo kỹ thuật, hướng code và kế hoạch đề xuất cho tuần 19–25/10/2026; chưa có bằng chứng xác nhận deadline môn học hay tiến độ cá nhân. Không dùng số file để suy ra phần trăm hoàn thành đồ án.

## 1. Kết luận hiện trạng

Chương trình đã có bản chạy cơ bản trong bộ nhớ: CRUD sách, thêm/tìm/liệt kê khách hàng, lập/xem hóa đơn và cộng doanh thu. Chưa đáp ứng đầy đủ phân công OOP và lưu trữ. Điểm cần xử lý đầu tiên là hợp đồng chung và nghiệp vụ bán hàng, sau đó tách menu, lưu file và tích hợp.

| Phần | Đã có trong code | Thiếu hoặc cần sửa | Chủ trì |
|---|---|---|---|
| Kiến trúc | model/data/business/presentation | Person, IRepository, StoreConfig, MainMenu; Main đang ôm nhiều trách nhiệm | Trí |
| Sách | Book có private field, constructor, getter/setter; repository; service thêm/sửa/xóa/tìm/trừ kho | Thể loại, BookMenu, interface, kiểm tra chuỗi/giá hữu hạn, lưu file | Phú |
| Con người | Customer và Employee độc lập; khách có thêm/tìm/liệt kê | Kế thừa Person, override, sửa/xóa khách, CustomerMenu | Nam |
| Hóa đơn | Invoice/InvoiceDetail, chụp đơn giá vào detail, repository tìm/liệt kê | Ngày lập, InvoiceMenu, kiểm tra dữ liệu, bảo vệ lịch sử | Long |
| Bán hàng | Main kiểm tra khách/mã hóa đơn, BookService.sellBook trừ kho | InvoiceService hiện chỉ lưu/tìm/cộng doanh thu; phải nhận nghiệp vụ bán hàng | Trí + Long |
| Báo cáo | InvoiceService.getRevenue cộng tổng mọi hóa đơn | ReportService, lọc ngày, sách bán chạy, số lượng đã bán | Long |
| Tiện ích | Hàm nhập và seed trong Main | FileManager, InputHelper, dữ liệu txt, phục hồi sau khởi động | Hoàng |

README mô tả cây dự kiến với `terminal`, nhưng code dùng `presentation`. Đề xuất giữ `presentation` để giảm đổi package và sửa README khi kiến trúc được chốt. Tiếng Việt trong file README đọc đúng với UTF-8; đầu ra console ở môi trường kiểm tra bị lỗi hiển thị dấu, chưa đủ cơ sở kết luận source hỏng encoding.

## 2. Rủi ro cụ thể và thứ tự sửa

1. **P0 — trừ kho trước khi lưu hóa đơn:** `Main.createInvoice()` gọi `BookService.sellBook()` ngay khi nhập từng dòng. Nếu quá trình chưa hoàn tất bị gián đoạn thì trạng thái kho đã thay đổi. Chuyển toàn bộ xác nhận giao dịch vào InvoiceService; không trừ kho lúc đang xây giỏ.
2. **P0 — service có thể bị gọi vượt qua validation của menu:** `InvoiceService.addInvoice()` lưu trực tiếp, không chặn trùng mã, giỏ rỗng hoặc dòng âm. Validation bắt buộc ở service, không chỉ tại giao diện.
3. **P1 — mất dữ liệu khi thoát:** cả ba repository là ArrayList; seed lại mỗi lần chạy. Chưa có lưu/đọc file.
4. **P1 — thiếu bằng chứng OOP:** Customer/Employee chưa kế thừa; chưa có abstract class và interface. Override toString hiện có chưa thay thế được bài minh họa đa hình Person và IRepository.
5. **P1 — dữ liệu bị sửa từ ngoài:** `getAll()` và `Invoice.getDetails()` trả danh sách gốc. Mã ngoài có thể bỏ qua quy tắc thêm/xóa; hóa đơn đã lưu còn có thể được thêm dòng. Cần bảo vệ vòng đời hóa đơn.
6. **P1 — validation:** chuỗi rỗng và null chưa được xử lý nhất quán. So sánh `price <= 0` không chặn NaN; Infinity cũng được nhận. Dùng `Double.isFinite` nếu còn dùng double.
7. **P1 — EOF:** hàm nhập bắt Exception rộng và lặp lại; hết luồng nhập có thể lặp vô hạn. InputHelper cần xử lý EOF riêng.
8. **P2 — lịch sử và báo cáo:** đơn giá đã được chụp là điểm tốt; tên sách/khách vẫn qua tham chiếu đối tượng sống. Cần quyết định lưu snapshot để sửa dữ liệu hiện tại không làm đổi nội dung hóa đơn cũ.

## 3. Kiến trúc và cách kết nối

```text
Main: tạo config, repository, service, menu dùng chung
  -> MainMenu -> BookMenu / CustomerMenu / InvoiceMenu
  -> business: kiểm tra quy tắc và điều phối
  -> data: truy xuất dữ liệu qua IRepository
  -> FileManager: đọc/ghi UTF-8
model: Person, Customer, Employee, Book, Invoice, InvoiceDetail
utility: StoreConfig, InputHelper, FileManager
```

Main là nơi nối các instance; nếu BookMenu và InvoiceService dùng hai BookRepository khác nhau thì tồn kho sẽ lệch. Menu chỉ nhập, gọi service, hiển thị kết quả. Repository không đọc bàn phím; model không in menu. ReportService chỉ đọc hóa đơn đã hoàn tất, không trừ kho hay tính lại giá từ Book hiện tại.

### Hợp đồng đề xuất để leader chốt trước khi tích hợp

Các chữ ký dưới đây là thiết kế mục tiêu, **chưa có trong source**. Không đổi tất cả lớp một lượt mà không cập nhật và biên dịch nơi gọi.

```java
public interface IRepository<T> {
    void add(T entity);
    List<T> getAll();
    T findById(String id); // null nếu không có, giữ cách gọi hiện tại
}

public abstract class Person {
    private final String id;
    private String name;
    protected Person(String id, String name) { /* validate và gán */ }
    public abstract String getRole();
    // getter cho id/name; setter name có kiểm tra
}
```

Chỉ đưa phương thức chung thật sự vào interface. Xóa sách/khách là API riêng của repository tương ứng; không thêm xóa hóa đơn chỉ để đủ interface. Customer giữ phone, Employee giữ position; kế thừa id/name. Giữ constructor cũ nếu có thể: Customer(id, name, phone), Employee(id, name, position).

| API mục tiêu | Quy tắc |
|---|---|
| BookService(IRepository<Book>) | Dùng constructor injection; thao tác xóa qua thiết kế phù hợp, không ép kiểu interface sang concrete |
| CustomerService(IRepository<Customer>) | Tương tự; nếu cần CRUD riêng thì dùng interface chuyên biệt hoặc giữ dependency concrete trong bước chuyển tiếp |
| InvoiceService với repository hóa đơn/sách/khách dùng chung | Kiểm tra mã và thực thể từ nguồn chính; không tin Book do menu tự tạo |
| createInvoice(id, customerId, employee, Map<String,Integer> quantities) | Trả Invoice sau khi thành công; IllegalArgumentException cho đầu vào sai, IllegalStateException cho thiếu kho/trạng thái không hợp lệ |
| ReportService(InvoiceRepository hoặc IRepository<Invoice>) | Tổng doanh thu, tổng lượng bán, lượng theo mã sách; khoảng ngày gồm hai đầu |
| InputHelper dùng một Scanner | Đọc chuỗi không rỗng, int theo khoảng, tiền, LocalDate; EOF được truyền để menu thoát |
| FileManager.readLines/writeLines | Chỉ đọc/ghi dòng UTF-8; repository/codec chịu chuyển dòng thành model; không nuốt IOException |

Interface CRUD chuyên biệt là lựa chọn mở rộng sau khi leader chốt, không nên vừa inject IRepository vừa gọi phương thức delete chưa có trong interface. Có thể bắt đầu bằng interface tối thiểu cho add/getAll/findById để minh họa đa hình mà vẫn giữ repository chuyên biệt ở service cần xóa.

### Quy ước dữ liệu

- Mã: trim, chuẩn hóa uppercase; không phân biệt hoa thường khi tra cứu. Mã duy nhất theo loại: S001, KH001, NV001, HD001. Không cho đổi mã sau khi tạo; chặn ký tự phân cách file và xuống dòng.
- Tên/tác giả/thể loại không rỗng. Điện thoại lưu String để giữ số 0 đầu; quy tắc đề xuất 10 chữ số cho phạm vi demo, ghi rõ nếu nhóm muốn hỗ trợ quốc tế.
- Số lượng int >= 0; số lượng bán > 0. Giá > 0. Không chấp nhận NaN/Infinity nếu dùng double.
- Đề xuất tiền VND nguyên dùng long xuyên suốt Book, InvoiceDetail, service và báo cáo, với kiểm tra tràn khi nhân/cộng. Code hiện tại dùng double: không trộn hai kiểu trong lúc ghép nhánh; Trí chốt thời điểm chuyển trước khi các bạn sửa.
- Ngày lập dùng LocalDate, lưu ISO `yyyy-MM-dd`; không lưu ngày thành chuỗi định dạng tùy ý trong model.
- Chọn static có ý nghĩa: hằng đường dẫn/định dạng trong StoreConfig hoặc hàm tiện ích; dùng instance cho dữ liệu và service.
- Repository trả bản sao danh sách hoặc view không cho thêm/xóa; điều này không tự ngăn việc sửa field của từng phần tử. Service là nơi quản lý cập nhật, hóa đơn hoàn tất cần snapshot/bảo vệ riêng.

## 4. Luồng bán hàng do Minh Trí phụ trách

1. Menu thu giỏ tạm; chưa cập nhật kho. Nếu nhập cùng mã nhiều lần thì cộng số lượng, kiểm tra tràn int.
2. InvoiceService kiểm tra mã hóa đơn, khách, nhân viên, ngày và giỏ không rỗng.
3. Lấy sách từ repository; kiểm tra toàn bộ số lượng cộng dồn của từng mã và tồn kho. Một dòng sai thì dừng toàn bộ.
4. Chụp đơn giá và dữ liệu cần in lịch sử; tính thành tiền/tổng tiền từ snapshot.
5. Chuẩn bị trạng thái kho mới và hóa đơn mới, thực hiện lưu theo cơ chế đã thống nhất; chỉ báo thành công khi hoàn tất. Không được để lại kho đã trừ nếu thêm/lưu hóa đơn thất bại.
6. Khóa thay đổi giỏ sau hoàn tất; lần gọi lặp cùng mã bị từ chối trước khi trừ kho.

Ứng dụng Terminal một luồng chưa cần khóa đa luồng hoặc database. Tuy nhiên ghi nhiều file không tự có tính nguyên tử: ghi file tạm rồi replace từng file vẫn có thể lệch nếu chương trình chết giữa hai lần replace. Đề xuất snapshot toàn bộ trạng thái bán hàng trong một file để commit bằng một lần thay thế; nếu dùng nhiều file phải có backup/journal và phục hồi. Hoàng và Trí chọn một phương án rồi kiểm thử lỗi ghi, không tuyên bố an toàn giao dịch khi chưa triển khai.

## 5. Hợp đồng file đề xuất

Phương án dễ hiểu cho demo: một snapshot `data/store.txt` có phiên bản và loại bản ghi; ghi toàn bộ trạng thái, file tạm cùng thư mục rồi thay thế theo khả năng filesystem, giữ backup khi cần. Seed riêng chỉ dùng khi người dùng chủ động tạo dữ liệu mẫu; không seed lại nếu file có dữ liệu.

```text
VERSION|1
BOOK|S001|Java Core|Nguyen Van A|Lap trinh|150000|20
CUSTOMER|KH001|Nguyen Van Minh|0901234567
EMPLOYEE|NV001|Admin|Quan ly
INVOICE|HD001|KH001|NV001|2026-10-20|Nguyen Van Minh|Admin
DETAIL|HD001|S001|Java Core|2|150000
```

Đây là schema đề xuất sau khi thống nhất tiền long và snapshot tên. Tổng hóa đơn được tính từ detail, không lưu thêm tổng dễ lệch. Không nhận `|`, CR hoặc LF trong trường nhập cho phiên bản này; nếu muốn cho phép thì phải thiết kế escaping thống nhất. Parse giữ trường rỗng (`split` có limit âm), kiểm tra đủ cột, kiểu dữ liệu, mã trùng, phiên bản và liên kết hóa đơn/detail. Đọc vào cấu trúc tạm và chỉ thay trạng thái khi toàn bộ file hợp lệ; không âm thầm nạp một nửa file lỗi.

File không tồn tại được xem là lần chạy đầu và khởi tạo rỗng; file rỗng được xử lý có thông báo; file hỏng hoặc không có quyền đọc/ghi phải báo rõ và giữ dữ liệu cũ. Không bắt IOException rồi trả danh sách rỗng vì có thể dẫn đến ghi đè dữ liệu thật.

## 6. Phân công và đầu ra nghiệm thu

Tỷ trọng được cung cấp: Trí 30%, Phú 20%, Nam 20%, Long 20% = 90%. Phần Hoàng chưa có số; **đề xuất 10% còn lại**, chưa phải tỷ trọng đã xác nhận. Tỷ trọng đóng góp khác với phần trăm tiến độ.

| Người | Việc tuần này | Đầu ra để leader nghiệm thu |
|---|---|---|
| Minh Trí — 30% | Chốt hợp đồng, Person/IRepository, StoreConfig, Main/MainMenu; chuyển nghiệp vụ bán hàng; review và tích hợp | Build chung chạy được; cùng instance repository; bán thành công/thất bại đúng; checklist OOP và demo |
| Phú — 20% | Thêm category, hoàn thiện CRUD/tìm kiếm/tồn kho, BookMenu, phối hợp codec sách | Thêm/sửa/xóa/tìm chạy qua menu; chặn trùng mã/giá sai/kho âm; không có đường bán riêng bỏ qua InvoiceService |
| Nam — 20% | Customer/Employee extends Person, override getRole, CRUD khách, CustomerMenu | Không trùng id/name ở lớp con; demo List<Person> gọi getRole; sửa/xóa/tìm và validation đúng |
| Long — 20% | Ngày lập, snapshot/chi tiết hóa đơn, InvoiceMenu, repository và ReportService | Lưu/đọc đúng chi tiết; tìm hóa đơn; báo cáo doanh thu/lượng bán/top sách; phối hợp API với Trí |
| Hoàng — đề xuất 10% | InputHelper, FileManager, dữ liệu giả, hướng dẫn chạy; kiểm thử nhập/file | UTF-8; đọc/ghi round-trip; xử lý EOF/file trống/thiếu/hỏng; hướng dẫn chạy trên máy khác |

Nam không cần tự mở rộng thành CRUD nhân viên nếu đề bài chỉ yêu cầu lớp Employee. Hoàng làm I/O chung; các chủ module chịu trách nhiệm ánh xạ model của mình, tránh dồn toàn bộ lưu trữ vào một người. Long và Trí chia rõ: Long xây dữ liệu/menu/báo cáo, Trí sở hữu quy trình chốt bán và kho.

### Lịch đề xuất từ ngày khảo sát

| Mốc | Trí | Phú / Nam / Long / Hoàng | Điều kiện qua mốc |
|---|---|---|---|
| Thứ 3, 20/10 | Chốt package, API, tiền, file; tạo khung Person/IRepository | Đọc hợp đồng, ghi phụ thuộc và nhận việc | Có chữ ký chung bằng văn bản; build nền xanh |
| Thứ 4, 21/10 | MainMenu và wiring, khung InvoiceService | Phú: Book/category; Nam: kế thừa/CRUD; Long: model/ngày; Hoàng: input/I/O | Model/API gọi được, không merge code không biên dịch |
| Thứ 5, 22/10 | Hoàn thiện validation bán, nối shared repositories | Menu riêng, codec từng module, ReportService, dữ liệu mẫu | Demo luồng sách -> khách -> bán -> báo cáo trong bộ nhớ |
| Thứ 6, 23/10 | Ghép lưu trữ và kiểm tra lỗi giao dịch | Kiểm thử liên module, sửa lỗi theo chủ sở hữu | Tắt/mở còn dữ liệu; lỗi ghi không báo bán thành công giả |
| Thứ 7, 24/10 | Kiểm thử tổng thể, rà OOP, chốt bản demo | Kiểm tra chéo, cập nhật hướng dẫn và bằng chứng | Không còn lỗi P0; checklist bắt buộc đạt |
| Chủ nhật, 25/10 | Dự phòng, tổng duyệt và chốt bàn giao | Mỗi bạn trình bày phần mình, xử lý lỗi còn lại | Demo trên máy khác; ghi rõ hạn chế còn lại |

Nếu deadline thực tế sớm hơn, ưu tiên lõi OOP + CRUD + bán đúng + lưu dữ liệu; báo cáo nâng cao và giao diện đẹp làm sau. Không tự giảm yêu cầu môn học khi chưa có rubric.

## 7. Git và theo dõi tiến độ

Đề xuất nhánh: `feature/tri-core`, `feature/phu-books`, `feature/nam-people`, `feature/long-invoices-reports`, `feature/hoang-utilities`. Trí giữ việc sửa Main và hợp đồng chung để tránh xung đột. Nhánh làm việc lấy từ nhánh tích hợp mà nhóm thống nhất; không mặc định repo đã có develop hoặc cấu hình bảo vệ nhánh.

Mỗi PR mô tả chức năng, file/API đổi, phụ thuộc, cách kiểm chứng và lỗi còn biết. Trí review build chung trước khi merge; thay đổi Person/IRepository/schema phải thông báo trong mô tả PR và cập nhật nơi gọi. Không force-push nhánh dùng chung, không commit file `.class`, thông tin cá nhân hay đường dẫn riêng máy.

Mỗi cuối ngày cập nhật theo mẫu dưới đây trong công cụ nhóm đang dùng; tài liệu này không tự gửi thông báo cho thành viên:

```text
Người / ngày:
Đã xong: ... (nhánh hoặc commit/PR, ca chạy thực tế)
Đang làm: ...
Vướng: ... (cần ai, cần API nào)
Bàn giao tiếp theo: ... (mốc giờ nhóm tự chốt)
Trạng thái: Chưa làm / Đang làm / Chờ review / Đã nghiệm thu
```

Leader chỉ ghi “Đã nghiệm thu” sau khi chạy được trên nhánh tích hợp, không dựa vào thông báo “đã code xong”.

## 8. Checklist kiểm thử và OOP

Các ca dưới đây là **kế hoạch chưa chạy**, trừ phần bằng chứng ở mục 9.

| Nhóm ca | Kỳ vọng | Người chính |
|---|---|---|
| Sách | Trùng mã kể cả khác hoa/thường bị chặn; chuỗi trống, giá <= 0, NaN/Infinity và kho âm bị chặn; category sửa được | Phú |
| Khách | CRUD đầy đủ; mã trùng/số điện thoại sai bị chặn; xóa khách không làm mất lịch sử hóa đơn | Nam |
| Person | Không khởi tạo trực tiếp abstract; List<Person> chứa Customer/Employee và gọi đúng override | Nam + Trí |
| Interface | Repository implements IRepository; gọi cùng hợp đồng qua biến interface, không chỉ viết interface bỏ trống | Trí + Phú |
| Bán đúng | S001 giá 150000, tồn 20, bán 2 -> tồn 18, tổng 300000 | Trí + Long |
| Bán sai | Bán 21 khi tồn 20, bán 0/âm, sách thiếu, khách thiếu, giỏ rỗng, mã hóa đơn trùng -> không đổi kho/hóa đơn | Trí |
| Giỏ trùng sách | Cùng sách hai dòng có tổng vượt kho -> từ chối toàn bộ | Trí |
| Nhiều sách | Một sách đủ, một sách thiếu -> không trừ bất kỳ sách nào | Trí |
| Lịch sử | Đổi giá/tên sách sau bán không đổi đơn giá/tên snapshot và tổng hóa đơn cũ | Long |
| Báo cáo | Nhiều hóa đơn, ngày biên, không có dữ liệu, top sách đồng hạng; tổng tiền/lượng khớp chi tiết | Long |
| Lưu trữ | Đọc/ghi giữ dấu và dữ liệu; restart không seed trùng; file thiếu/trống/hỏng và lỗi ghi xử lý rõ | Hoàng + Trí |
| Nhập liệu | Chữ thay số, vượt int/long, ngày sai, chọn ngoài menu, EOF -> không crash/lặp vô hạn | Hoàng |

Đề xuất khi xóa sách/khách đã được hóa đơn tham chiếu: chặn xóa trong phiên bản đầu, cho sửa thông tin hiện tại nếu hóa đơn có snapshot. Nếu chọn xóa mềm phải thiết kế trường trạng thái và cập nhật đồng bộ tìm kiếm/lưu file; không tự thêm ngoài phạm vi.

## 9. Bằng chứng kiểm chứng thực tế

Ngày 20/10/2026, môi trường có Java/Javac 27. Đã biên dịch toàn bộ 13 source bằng `javac -encoding UTF-8 --release 17` vào thư mục tạm, thành công. Việc này xác nhận source biên dịch với đích 17; chưa thay thế kiểm tra chạy trên JDK 17 thật.

Đã chạy smoke test qua `presentation.Main` với chuỗi nhập `3, HD001, KH001, S001, 2, 0, 5, 0`: tạo hóa đơn 2 sách Java Core, tổng 300000, menu doanh thu hiển thị 300000 và thoát. Ca này chưa kiểm chứng tồn kho sau bán, lưu file, rollback, input lỗi hoặc kế thừa/interface. Console kiểm tra hiển thị lỗi dấu tiếng Việt, cần kiểm tra encoding Terminal khi chuẩn bị demo.

Lệnh chạy thủ công từ thư mục repo bằng PowerShell, sau khi có JDK 17+:

```powershell
New-Item -ItemType Directory -Force build | Out-Null
$sources = @(Get-ChildItem src -Recurse -Filter *.java | ForEach-Object { $_.FullName })
javac -encoding UTF-8 --release 17 -d build @sources
if ($LASTEXITCODE -eq 0) { java -cp build presentation.Main }
```

Không commit `build/`. Khi tách menu, giữ entry point `presentation.Main` để hướng dẫn chạy không thay đổi.

## 10. Kịch bản demo và việc leader cần chốt

Demo đề xuất: giới thiệu kiến trúc và vai trò -> tạo/sửa sách -> thêm khách -> bán hai sách -> thử thiếu kho và chỉ ra kho không đổi -> xem hóa đơn/ngày -> thống kê -> thoát/mở lại -> trình bày Person và IRepository bằng code. Mỗi thành viên giải thích phần mình và một trường hợp sai đã xử lý.

Trí cần chốt với nhóm: deadline/rubric thực tế, Hoàng có nhận 10% hay không, đích Java 17, tiền long, schema snapshot, quy tắc xóa và chữ ký tạo hóa đơn. Trong lúc chưa chốt, dùng source hiện tại làm nền; các lựa chọn trong tài liệu là đề xuất, không phải quyết định nhóm đã thông qua.
