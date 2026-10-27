# Quy tắc code chung và hướng dẫn triển khai từng phần

Áp dụng cho nhóm BookStoreManagement. Minh Trí (Mtri) phụ trách kiến trúc và tích hợp; Phú phụ trách sách; Nam phụ trách con người; Long phụ trách hóa đơn/báo cáo; Hoàng phụ trách tiện ích và file.

Tài liệu này hướng dẫn thành viên tiếp tục code trên cấu trúc đã có, ngày 20/10/2026. **Các API ghi “cần bổ sung” là thiết kế hướng dẫn, chưa phải phương thức có sẵn.** Không chép đè toàn bộ lớp bằng ví dụ. Khi đổi hợp đồng dùng chung, ghi vào PR để Trí cập nhật nơi gọi.

## Cập nhật sau rà soát cấu trúc

Đọc [hợp đồng hiện tại](docs/CAU_TRUC_HIEN_TAI.md) và [mẫu Leader theo tiêu chí thầy](docs/LEADER_MTRI.md) trước khi dùng các mẫu bên dưới. Đã có mảng Person[] trung tâm, Book[]/Invoice[], PersonService/PersonMenu, checkout tập trung, snapshot lịch sử và lưu/nạp text thật. Báo cáo khảo sát ban đầu chỉ mô tả trạng thái trước lần sửa nền.

## 1. Đọc gì trước khi code?

1. Đọc [cấu trúc hiện tại](docs/CAU_TRUC_HIEN_TAI.md) để biết file mình phụ trách.
2. Đọc lớp model, repository, service và menu của module mình, theo thứ tự đó.
3. Kiểm tra `git status` để tránh ghi đè việc đang làm của người khác.
4. Xác định phương thức nào đã có, phương thức nào cần thêm, ai đang gọi nó.
5. Viết quy tắc đầu vào/thành công/thất bại trước khi viết thân phương thức.
6. Code từng phần nhỏ, biên dịch toàn bộ, chạy cả luồng liên quan rồi bàn giao.

Không tự đổi tên package, đổi kiểu tiền, chữ ký constructor chung hoặc định dạng file giữa chừng. Nếu cần đổi, mô tả lý do và danh sách nơi bị ảnh hưởng; Trí điều phối tích hợp. Không cần chờ leader cho từng thay đổi nội bộ không ảnh hưởng hợp đồng.

## 2. Mỗi tầng được viết gì?

| Tầng | Được làm | Ví dụ sai cần tránh |
|---|---|---|
| model | Chứa dữ liệu, giữ trạng thái hợp lệ, tính toán trực tiếp từ dữ liệu của mình | Book tự mở Scanner hoặc ghi file |
| data | Thêm/đọc/tìm/xóa trong tập dữ liệu; ánh xạ model với file khi tích hợp | Repository hỏi người dùng có muốn mua không |
| business | Kiểm tra nghiệp vụ, phối hợp repository, quyết định thay đổi trạng thái | Menu tự trừ kho và tự cộng doanh thu |
| presentation | Nhập, hiển thị, gọi service, xử lý quay lại/thoát | Menu tạo repository riêng hoặc lưu trực tiếp hóa đơn |
| utility | Đọc dữ liệu nhập, đọc/ghi dòng file, hằng cấu hình | FileManager tự quyết định sách nào được bán |

Luồng xử lý: `Menu -> Service -> Repository`. Repository giữ mảng trong phiên; MainMenu dùng StoreFile/StoreTextCodec để lưu toàn bộ StoreState qua FileManager. Model là dữ liệu trao đổi. Không tự gắn ghi file từng repository vào giữa checkout vì sẽ phá thiết kế snapshot một file.

## 3. Chuẩn viết từng dòng và từng khối

### Tên và cách trình bày

- Java 17 làm đích biên dịch, UTF-8, mỗi file chứa một lớp public trùng tên file.
- Thụt 4 dấu cách. Tên lớp PascalCase: `CustomerService`. Biến/phương thức camelCase: `findCustomer`. Hằng UPPER_SNAKE_CASE: `STORE_FILE`.
- Tên code dùng tiếng Anh; thông báo cho người dùng dùng tiếng Việt. Import rõ lớp, hạn chế `import model.*`.
- Field để `private`; dependency dùng `private final` nếu không thay instance. Không tạo getter/setter cho mọi field một cách máy móc.
- Mã định danh không có setter. Dùng `static` cho hằng/hàm tiện ích hợp lý, không dùng để chia sẻ tất cả dữ liệu ứng dụng.
- Một phương thức làm một việc, ưu tiên kiểm tra lỗi và return sớm. Không gộp nhập liệu, cập nhật dữ liệu và in toàn bộ menu vào một phương thức dài.
- Comment giải thích lý do hoặc quy tắc khó; không cần ghi “tăng i” ngay trên `i++`. Phần chú thích từng dòng dưới đây phục vụ học và thống nhất cách viết.

### Khung service có chú thích từng dòng

Đoạn minh họa dùng đúng `IRepository` hiện có. Đây là mẫu đọc và bổ sung tư duy, **không phải yêu cầu tạo thêm một lớp SampleService trong repo**.

```java
package business;                         // 01: File thuộc thư mục src/business.

import data.IRepository;                 // 02: Phụ thuộc hợp đồng truy xuất dữ liệu.
import model.Book;                       // 03: Đối tượng nghiệp vụ của module.

public class SampleService {             // 04: Tên minh họa; không chép đè BookService.
    private final IRepository<Book> repository; // 05: Giữ repository được truyền vào.

    public SampleService(IRepository<Book> repository) { // 06: Constructor injection.
        this.repository = repository;    // 07: Không new repository thứ hai tại đây.
    }

    public boolean addBook(Book book) {   // 08: true thành công, false đầu vào không đạt.
        if (book == null) return false;  // 09: Kiểm tra trước khi gọi getter, tránh NPE.
        String id = book.getId();         // 10: Lấy mã để kiểm tra.
        if (id == null || id.isBlank()) return false; // 11: Chặn null và chuỗi trắng.
        if (!id.equals(id.trim())) return false; // 12: Mẫu này từ chối khoảng trắng biên.
        if (repository.findById(id) != null) return false; // 13: Chặn mã trùng.
        double price = book.getPrice();  // 14: Hiện tại project dùng double.
        if (!Double.isFinite(price) || price <= 0) return false; // 15: Chặn NaN/vô cực.
        if (book.getQuantity() < 0) return false; // 16: Tồn kho không âm.
        repository.add(book);            // 17: Chỉ đổi dữ liệu sau khi đã kiểm tra.
        return true;                     // 18: Không thông báo thành công trước bước add.
    }
}
```

Mẫu chưa kiểm tra title/author/category; thành viên phải bổ sung theo quy tắc sách ở mục 6. Cách chuẩn hóa mã mục tiêu là trim và uppercase ngay lúc tạo/đọc model, sau đó service kiểm tra lại. Trong bước chuyển tiếp, có thể từ chối mã có khoảng trắng như mẫu thay vì âm thầm đổi đối tượng mà nơi gọi không biết.

**Chú ý interface:** IRepository có `add`, `getAll`, `findById`. ICrudRepository extends IRepository và thêm `delete`, được BookService/CustomerService sử dụng qua constructor. InvoiceService/ReportService nhận IRepository, không có quyền xóa hóa đơn qua hợp đồng đó.

### Khung menu gọi service, giải thích từng bước

Ví dụ này phù hợp các API hiện có; đặt trong lớp menu đang giữ `input` và `bookService`:

```java
public void findBook() {
    String id = input.read("Mã sách: ");   // 01: Menu chịu trách nhiệm hỏi người dùng.
    Book book = bookService.findBook(id);  // 02: Không gọi repository trực tiếp.
    if (book == null) {                    // 03: Tra cứu không có kết quả.
        System.out.println("Không tìm thấy sách."); // 04: Chỉ hiển thị, không đổi dữ liệu.
        return;                           // 05: Dừng trước khi dùng đối tượng null.
    }
    System.out.println(book);             // 06: Có dữ liệu mới hiển thị.
}
```

## 4. Quy tắc dữ liệu dùng chung

| Dữ liệu | Quy tắc cần áp dụng ở service/model | Khi không đạt |
|---|---|---|
| Mã | Không null/rỗng; trim; chuẩn hóa uppercase bằng Locale.ROOT khi triển khai; không trùng trong cùng loại | Không thêm/sửa dữ liệu |
| Chuỗi bắt buộc | Không null, không chỉ khoảng trắng | Thông báo nhập lại |
| Giá | Với double hiện tại: hữu hạn và > 0 | Từ chối trước mọi cập nhật |
| Tồn kho | int >= 0 | Không sửa sách |
| Số lượng mua | int > 0, tổng theo mã không vượt tồn | Từ chối toàn bộ đơn |
| Điện thoại | String để giữ 0 đầu; đề xuất 10 chữ số cho demo, nhóm cần chốt phạm vi | Không lưu khách không hợp lệ |
| Ngày | LocalDate; nhập và lưu ISO yyyy-MM-dd | Ngày không tồn tại bị từ chối |
| Trường lưu file | Với định dạng phân cách bằng dấu `|` đề xuất: không chứa dấu phân cách hoặc CR/LF | Từ chối hoặc triển khai escaping chung |

Hiện tại tiền dùng double. **Không tự đổi riêng Book hoặc InvoiceDetail sang long**: sẽ làm API và tính tiền lệch nhau. Chuyển sang long VND nguyên là đề xuất trong báo cáo, chỉ triển khai đồng bộ khi Trí chốt. Khi giữ double, kiểm tra cả kết quả nhân/cộng có hữu hạn; không chỉ kiểm tra đơn giá.

Quy tắc return đang dùng: thêm/sửa/xóa sách hoặc khách trả boolean; tìm không thấy trả null; danh sách rỗng trả list rỗng. FileManager truyền IOException cho nơi gọi xử lý. API bán hàng mới có thể dùng exception nghiệp vụ theo mục 8; không tự đổi toàn bộ API boolean của người khác.

## 5. Minh Trí — khung chung và tích hợp

### Person và IRepository đã có

1. Person chứa id/name; Customer/Employee gọi `super(id, name)` ở đầu constructor.
2. Person là abstract, có `getRole()` abstract; lớp con override.
3. Không khai báo lại id/name trong lớp con vì sẽ tạo hai bản dữ liệu dễ lệch.
4. Validation id/name đã có qua ModelChecks trong model: không rỗng, trim và mã uppercase. Giữ quy tắc khi mở rộng.
5. IRepository có add/getAll/findById; ICrudRepository thêm delete cho sách/khách. Các service đã nhận interface qua constructor.

### Main, MainMenu và StoreConfig

1. Main tạo mỗi repository một lần.
2. Main truyền cùng repository cho các service có liên quan; menu nhận service, không tự tạo dữ liệu.
3. Main tạo một Scanner và một InputHelper; menu con không đóng Scanner.
4. MainMenu chỉ điều hướng; không thêm vòng lặp CRUD vào Main lần nữa.
5. StoreConfig chứa hằng và đường dẫn dùng chung; không hardcode đường dẫn `C:\...` hoặc `D:\...` của máy cá nhân.
6. Khi tích hợp file: nạp một lần lúc khởi động; chỉ seed theo chế độ dữ liệu mẫu rõ ràng, không seed chồng lên file cũ.

### Thứ tự tích hợp đề xuất

Chốt mã/kiểu tiền/API -> model chung -> repository -> service từng module -> menu -> bán hàng -> lưu trữ -> báo cáo -> demo. Không merge code chưa biên dịch với lý do “bạn khác sẽ sửa sau”.

## 6. Phú — module sách

### Book.java

1. Giữ id, title, author, price, quantity hiện có; thêm `private String category`.
2. Thêm getter/setter category có kiểm tra không rỗng.
3. Nếu thêm constructor có category, giữ constructor 5 tham số đang được Main/menu gọi hoặc cập nhật toàn bộ nơi gọi trong cùng thay đổi. Nếu giữ overload cũ thì đặt thể loại mặc định rõ ràng như `Khác`.
4. Không thêm setter id; sửa sách không làm thay mã.
5. Sửa toString và tiêu đề bảng BookMenu để thể loại hiển thị đúng cột.

### BookRepository.java

1. Giữ implements IRepository<Book>.
2. findById tìm không phân biệt hoa/thường; xử lý id null theo quy ước thống nhất.
3. BookRepository đã dùng Book[] + size; getAll trả bản sao list, không đổi storage về ArrayList.
4. StoreTextCodec đã ánh xạ dữ liệu sách. Thêm category thì cập nhật schema, loader, sample và test cùng Trí/Hoàng.

### BookService.java — viết theo thứ tự kiểm tra

- `addBook`: null -> mã -> tên/tác giả/thể loại -> giá -> số lượng -> trùng mã -> add. Thất bại không thay dữ liệu.
- `updateBook`: tìm sách -> kiểm tra **tất cả** giá trị mới -> mới gọi các setter. Không sửa title rồi mới phát hiện giá âm.
- `deleteBook`: tìm sách -> kiểm tra liên kết hóa đơn theo hợp đồng Trí chốt -> xóa. Đề xuất chặn xóa sách đã bán trong phiên bản đầu.
- `findBook`: tìm mã hiện có. Nếu thêm tìm theo tên/tác giả/thể loại, dùng API riêng như `searchBooks(String keyword)` trả List<Book>, không đổi kiểu trả về findBook.
- Cập nhật tồn kho nhập mới phải qua service. `sellBook` đã bỏ; bán hàng dùng InvoiceService.createInvoice để không bỏ qua hóa đơn.

### BookMenu.java

Nhập đủ trường -> gọi service -> dựa trên kết quả để thông báo. Thêm lựa chọn tìm kiếm mới thì cập nhật phần in menu và switch cùng lúc. Không tự setQuantity hoặc ghi file từ menu.

**Nghiệm thu:** CRUD chạy qua menu, thể loại được giữ, trùng mã bị chặn, giá NaN/Infinity và kho âm không được lưu; cập nhật sai không sửa dở dữ liệu.

## 7. Nam — khách hàng và nhân viên

### Customer.java và Employee.java

1. Giữ extends Person đã có; constructor gọi super.
2. Customer giữ phone, Employee giữ position.
3. Override getRole trả vai trò phù hợp; có thể override toString để hiển thị các field của mình.
4. Dùng getId/getName từ Person khi định dạng, không thêm field trùng tên ở lớp con.
5. Chưa cần CRUD nhân viên nếu yêu cầu môn học chỉ yêu cầu lớp Employee; nhân viên mặc định đang được Main tạo.

### Repository và service khách

Hiện đã có addCustomer/getCustomers/findCustomer và CustomerRepository.delete. PersonService/PersonMenu đã làm mẫu sửa/xóa trên mảng chung. CustomerRepository dùng chung PersonDirectory với EmployeeRepository. Hai API ở CustomerService dưới đây vẫn cần Nam bổ sung và nối menu riêng:

```java
// Trong CustomerRepository: trả false nếu không tìm thấy.
public boolean delete(String id);

// Trong CustomerService: kiểm tra hết dữ liệu trước khi gọi setter.
public boolean updateCustomer(String id, String name, String phone);
public boolean deleteCustomer(String id);
```

Các dòng trên là **khai báo hợp đồng**, không chép nguyên dạng dấu `;` vào concrete class vì phương thức phải có thân `{ ... }`.

Thêm khách: kiểm tra null -> mã/tên/phone -> trùng mã -> repository.add. Sửa khách: tìm -> kiểm tra tên và phone -> cập nhật -> true; xem mẫu PersonService.updateById. Xóa khách: dùng quy tắc PersonService.delete đã chặn người có hóa đơn và nhân viên mặc định; không tự tạo InvoiceRepository rỗng hoặc bypass kiểm tra.

### CustomerMenu.java

Thêm lựa chọn sửa/xóa, gọi API mới, xử lý không tìm thấy. Điện thoại nhập chuỗi, không dùng readInt. Khi sửa tên hiện tại, hóa đơn cũ cần snapshot nếu nhóm yêu cầu giữ đúng thông tin lúc bán.

Demo đa hình sau khi tạo dữ liệu hợp lệ:

```java
List<Person> people = List.of(
        new Customer("KH001", "Khach mau", "0900000000"),
        new Employee("NV001", "Nhan vien mau", "Thu ngan"));
for (Person person : people) {
    System.out.println(person.getRole()); // Chạy override theo đối tượng thực tế.
}
```

Import List, Person, Customer, Employee ở file chạy demo. Không cần tạo một lớp ứng dụng khác chỉ để chạy đoạn này.

**Nghiệm thu:** CRUD khách đủ, kiểm tra đầu vào, kế thừa không lặp field và giải thích được tại sao Person không new trực tiếp được.

## 8. Minh Trí + Long — nghiệp vụ lập hóa đơn

**Đã sửa:** InvoiceMenu gom giỏ tạm, InvoiceService.createInvoice kiểm tra toàn bộ giỏ trước khi lưu vào repository và trừ kho trong bộ nhớ. Hủy -1 hoặc EOF trước xác nhận không đổi kho. MainMenu lưu snapshot toàn phiên ở mục 7/0; không tự lưu từng lần bán. addInvoice và sellBook đã bỏ để tránh đường bán bỏ qua checkout.

### Hợp đồng đã có và đã nối từ Main

```java
// ĐÃ CÓ trong InvoiceService.
public Invoice createInvoice(String id, String customerId,
        Employee employee, Map<String, Integer> quantities);
```

Constructor hiện nhận IRepository<Invoice>, IRepository<Book>, IRepository<Customer>, IRepository<Employee> theo thứ tự đó. Main đã truyền instance chung. Service tra lại nhân viên từ repository để không tin đối tượng Employee tự tạo ở ngoài. InvoiceMenu dùng Supplier lấy nhân viên hiện tại nếu người đó đã được sửa trong mảng.

### Viết thân phương thức theo từng bước

1. Kiểm tra id không trống và không trùng hóa đơn đã có.
2. Kiểm tra employee hợp lệ; lấy customer từ repository bằng customerId, thiếu thì từ chối.
3. Kiểm tra giỏ không null/rỗng. Chuẩn hóa mã sách và gộp số lượng theo mã, kể cả khi đầu vào chứa `s001` và `S001`.
4. Kiểm tra lượng từng mã > 0 và phép cộng không tràn int.
5. Duyệt toàn bộ giỏ: lấy Book chính thức, kiểm tra tồn kho và giá. **Không setQuantity trong vòng kiểm tra này.**
6. Tạo Invoice/InvoiceDetail bằng đơn giá tại lúc xác nhận bán, tính tổng; chặn kết quả không hữu hạn nếu còn double.
7. Chuẩn bị cả kho mới và hóa đơn mới. Hiện checkout chỉ commit trong bộ nhớ sau khi kiểm tra; menu lưu snapshot toàn phiên theo lựa chọn người dùng.
8. Chỉ cập nhật trạng thái khi điều kiện hoàn tất thỏa mãn. Lưu file thất bại phải báo lỗi, giữ file trước đó và cho người dùng thử lưu lại; không coi thay đổi chưa lưu là đã bền vững. Nếu sau này cần tự lưu từng lần bán, phải thiết kế thêm rollback/commit và test, không chỉ chèn write vào giữa.
9. Trả Invoice đã hoàn tất; menu mới in thông báo bán thành công.

Checkout hiện dùng IllegalArgumentException cho dữ liệu sai, IllegalStateException cho thiếu kho. Menu đã bắt hai loại và hiển thị thông báo. Không bắt Exception rồi vẫn in “thành công”. Không gọi repository.add thêm lần nữa sau createInvoice.

### Ví dụ nghiệp vụ bắt buộc

| Tình huống | Kết quả đúng |
|---|---|
| S001 tồn 20, giá 150000; mua 2 | Tồn 18, hóa đơn 300000 |
| S001 tồn 20; mua 21 | Không lưu hóa đơn, tồn vẫn 20 |
| S001 nhập 12 rồi thêm 10 | Tổng 22, từ chối; không chỉ kiểm tra từng dòng độc lập |
| S001 đủ nhưng S002 thiếu | Không trừ kho cả S001 lẫn S002 |
| Hủy hoặc hết dữ liệu nhập trước xác nhận | Không trừ kho |
| Trùng HD001 | Không thêm hóa đơn, không trừ kho lần hai |
| Lưu snapshot thất bại | Báo lỗi; giữ phiên bộ nhớ và file cũ, không báo đã lưu/thoát thành công |

Một luồng Terminal không cần giải bài toán nhiều người mua cùng lúc ở phiên bản hiện tại. Nhưng ghi nhiều file vẫn có nguy cơ lệch khi chương trình dừng giữa chừng; không coi “ghi file tạm” là đủ nếu còn phải thay thế nhiều file.

## 9. Long — model hóa đơn và báo cáo

### Invoice.java

1. LocalDate createdDate đã có; constructor 4 tham số nhận ngày lịch sử.
2. Giữ constructor hiện tại bằng cách gọi constructor mới với LocalDate.now nếu muốn tương thích. Khi đọc file, bắt buộc truyền ngày đã lưu, không dùng ngày hôm nay.
3. getTotal cộng thành tiền từ detail; không lấy giá sách hiện tại để tính lại.
4. getDetails đã trả list không sửa được; không đổi về danh sách gốc.
5. complete/isCompleted đã có; InvoiceRepository.add hoàn tất hóa đơn, sau đó addDetail bị chặn.
6. Snapshot mã/tên/giá sách, tên/phone khách, tên/chức vụ nhân viên đã có và StoreTextCodec phục hồi đúng từ text. Không đổi loader về chụp thông tin hiện tại.

### InvoiceDetail.java

1. Kiểm tra book không null và quantity > 0.
2. Giữ đơn giá tại lúc bán; getter getAmount tính quantity * price đã chụp.
3. Constructor `(Book book, int quantity, double price, String historicalTitle)` đã có để phục hồi cả giá và tên lịch sử. Constructor 2/3 tham số phục vụ tạo mới hoặc tương thích.
4. Nếu lưu snapshot tên/mã, ghi rõ field nào phục vụ lịch sử và field nào phục vụ tham chiếu hiện tại.

### InvoiceRepository.java

Giữ findById/getAll/add theo interface, bổ sung validation/tránh trùng theo hợp đồng. Không tự thêm sửa/xóa hóa đơn hoàn tất nếu chưa có yêu cầu nghiệp vụ. Nạp hóa đơn cần nối đúng detail theo mã hóa đơn và kiểm tra liên kết.

### ReportService.java

Đã có `getRevenue()`, `getTotalQuantitySold()`, `getQuantitySoldByBook()`. Map theo mã sách hiện trả lượng đã bán, **chưa phải danh sách đã sắp xếp bán chạy**.

Việc cần thêm:

1. Lọc hóa đơn theo ngày, bao gồm ngày đầu và ngày cuối; từ chối khoảng ngày đảo ngược.
2. Top sách: cộng lượng theo mã -> sắp lượng giảm dần -> cùng lượng thì mã tăng dần -> lấy số phần tử yêu cầu.
3. Không có dữ liệu thì doanh thu/lượng bằng 0, danh sách rỗng.
4. Chỉ dùng hóa đơn hoàn tất nếu bổ sung vòng đời nháp.
5. Không tính lượng từ tồn kho ban đầu trừ tồn kho hiện tại, vì còn nghiệp vụ nhập/sửa kho.
6. Nối các báo cáo mới vào menu, không chỉ viết phương thức không có đường sử dụng.

**Nghiệm thu:** giá sách thay đổi không đổi hóa đơn cũ; đọc lại giữ ngày/giá; doanh thu và lượng bán đối chiếu được bằng tay với dữ liệu mẫu.

## 10. Hoàng — nhập liệu và file

### InputHelper.java

Hiện đã có read/readInt/readDouble/readDate. readInt/readDouble mới kiểm tra kiểu, chưa đảm bảo giá dương hoặc số lượng không âm; service vẫn phải kiểm tra nghiệp vụ.

1. Thêm hàm chuỗi bắt buộc nếu cần, ví dụ readRequiredString; lặp khi chuỗi trắng.
2. Thêm overload nhập int theo min/max khi nhóm cần; kiểm tra min <= max trước vòng lặp.
3. Khi người dùng nhập chữ thay số, hiển thị thông báo và nhập lại.
4. Khi hết input, để NoSuchElementException truyền về Main như hiện có; không bắt rộng rồi lặp mãi.
5. Ngày dùng LocalDate.parse; ngày 2026-02-30 phải bị từ chối.
6. Không đóng Scanner từ menu hoặc helper; Main quản lý vòng đời.

### FileManager.java

Đã có readLines/writeLines và writeLinesAtomic. StoreFile dùng atomic write cho snapshot tại StoreConfig.STORE_FILE hoặc đường dẫn --data. writeLines ghi trực tiếp chỉ là hàm tiện ích, không dùng thay atomic write cho dữ liệu ứng dụng.

1. Schema phiên bản 1 đã được triển khai trong StoreTextCodec, mô tả đúng trong LEADER_MTRI.md. Schema ở báo cáo khảo sát cũ là lịch sử, không dùng thay hợp đồng hiện tại.
2. Hoàng mở rộng I/O và test; Phú/Nam/Long phối hợp thay đổi codec dữ liệu của mình trên mẫu đã chạy.
3. Đọc file vào cấu trúc tạm; kiểm tra đủ cột, kiểu số/ngày, mã trùng và liên kết trước khi thay trạng thái ứng dụng.
4. Nếu dùng dấu `|`, tách bằng `line.split("\\|", -1)` để giữ cột rỗng. Báo số dòng lỗi; không bỏ qua dòng hỏng một cách im lặng.
5. File không tồn tại có thể là lần chạy đầu; lỗi không có quyền đọc khác với file không tồn tại, không coi cả hai là dữ liệu rỗng.
6. File rỗng phải có xử lý rõ. Không tự ghi đè file hỏng bằng seed khi vừa nạp lỗi.
7. writeLinesAtomic đã ghi file tạm cùng thư mục rồi atomic replace một snapshot. Nếu filesystem không hỗ trợ thì báo lỗi, không fallback ghi đè. Chưa bảo đảm mất điện tuyệt đối hoặc nhiều tiến trình cùng ghi; không tuyên bố vượt phạm vi kiểm chứng.
8. Không bắt IOException rồi trả true; để caller thông báo và giữ trạng thái nhất quán.
9. Nạp lại detail bằng đơn giá lịch sử đã lưu, không theo giá Book mới.

**Nghiệm thu:** tiếng Việt round-trip đúng, file thiếu/trống/hỏng được xử lý, lỗi ghi không làm mất file cũ theo cơ chế đã chọn; restart giữ dữ liệu và không seed trùng.

## 11. Các lỗi ghép code thường gặp

| Lỗi | Nguyên nhân | Cách xử lý |
|---|---|---|
| cannot find symbol | Gọi API chưa có hoặc khác tên | Kiểm tra chữ ký thật, thống nhất chủ module |
| Constructor cannot be applied | Thêm tham số nhưng quên Main/menu/loader | Giữ overload hoặc sửa đồng bộ các nơi gọi |
| Kho ở menu không khớp hóa đơn | Có hai BookRepository khác nhau | Tạo một instance và truyền từ Main |
| Có dòng hóa đơn nhưng không có doanh thu đúng | Lưu nháp/rỗng hoặc tính bằng giá hiện tại | Chỉ lưu hoàn tất, dùng snapshot đơn giá |
| Đóng một menu làm menu khác không nhập được | Menu đóng Scanner chung | Chỉ đóng ở Main |
| EOF làm lặp không dừng | Catch Exception bao cả scanner.nextLine | Chỉ catch lỗi parse, cho EOF kết thúc |
| Sửa sách thất bại nhưng tên đã đổi | Gọi setter trước khi kiểm tra hết | Validate toàn bộ trước khi mutate |
| Interface không gọi được delete | IRepository không khai báo delete | Dùng hợp đồng chuyên biệt đã thống nhất |
| Sửa tên sách làm hóa đơn cũ đổi tên | Hóa đơn giữ tham chiếu sống | Lưu snapshot thông tin lịch sử |

## 12. Quy trình Git và bàn giao

1. Làm trên nhánh riêng theo module: `feature/tri-core`, `feature/phu-books`, `feature/nam-people`, `feature/long-invoices-reports`, `feature/hoang-utilities`.
2. Kiểm tra trạng thái trước khi sửa; không xóa/reset thay đổi chưa rõ chủ sở hữu.
3. Commit nhỏ theo một chức năng; tên gợi ý `feat(book): add category validation` hoặc `fix(invoice): validate whole cart before stock update`.
4. Trước bàn giao, cập nhật từ nhánh tích hợp theo cách nhóm thống nhất; không tự force-push nhánh dùng chung.
5. Biên dịch toàn bộ, chạy ca đúng và ca sai, ghi kết quả thật vào PR.
6. Đổi API chung phải liệt kê nơi gọi và phối hợp người chịu ảnh hưởng. Trí review và tích hợp.
7. Không commit build, class, dữ liệu cá nhân, đường dẫn riêng máy. Không đẩy dữ liệu mẫu đè dữ liệu làm việc của bạn khác.

Mẫu bàn giao:

```text
Người phụ trách:
Nhánh / commit / PR:
Chức năng đã làm:
File và chữ ký phương thức thay đổi:
Module bị ảnh hưởng / cần ai phối hợp:
Ca đã chạy và kết quả thực tế:
Ca chưa chạy / việc còn thiếu:
Hướng chạy để leader kiểm tra:
```

## 13. Biên dịch, chạy và tự kiểm tra trước review

Từ thư mục gốc repo trong PowerShell, cần JDK 17+:

```powershell
New-Item -ItemType Directory -Force build | Out-Null
$sources = @(Get-ChildItem src -Recurse -Filter *.java | ForEach-Object { $_.FullName })
javac -encoding UTF-8 --release 17 -d build @sources
if ($LASTEXITCODE -eq 0) { java -cp build presentation.Main }
```

Không commit thư mục build. Nếu lỗi dấu tiếng Việt, kiểm tra source UTF-8 và encoding Terminal trước khi sửa toàn bộ chuỗi trong source.

Checklist cá nhân trước review:

- [ ] Toàn bộ source biên dịch được, không chỉ riêng file mình.
- [ ] Menu cũ vẫn chạy; thay constructor đã cập nhật Main.
- [ ] Có ca thành công và ít nhất một ca sai quan trọng của nghiệp vụ vừa sửa.
- [ ] Dữ liệu không bị sửa dở khi validation thất bại.
- [ ] Không tạo repository/Scanner riêng ngoài wiring đã thống nhất.
- [ ] Không ghi “đã xong lưu file/bán hàng” nếu mới tạo lớp/hàm khung.
- [ ] Nêu rõ API mới chưa có ở module khác và cách phối hợp trước merge.
- [ ] Có thể giải thích code của mình và tiêu chí OOP tương ứng.

Đây là hướng dẫn triển khai; checklist trong tài liệu không có nghĩa là các ca đã được chạy hoặc tính năng đã hoàn thành. Đối chiếu source và bằng chứng kiểm thử trong từng lần bàn giao.
