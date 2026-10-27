# Cấu trúc và hợp đồng hiện tại — bản mẫu Leader

Hướng dẫn đầy đủ, sơ đồ lớp, rubric và demo ở [LEADER_MTRI.md](LEADER_MTRI.md). File này là hợp đồng ngắn để các thành viên nối module.

## Các phần đã chạy thật

- Person abstract, Customer/Employee kế thừa và override getRole.
- PersonDirectory là danh sách trung tâm dùng Person[] + size, có initialize n, add 1/k, updateById, delete/dồn mảng, search chính xác/gần đúng theo từ khóa + vai trò, countByRole.
- CustomerRepository và EmployeeRepository chia sẻ PersonDirectory trong StoreState. Không tạo kho con người riêng trong mỗi menu.
- BookRepository dùng Book[], InvoiceRepository dùng Invoice[]; có tăng sức chứa và trả bản sao kết quả.
- PersonService giữ quy tắc khởi tạo/xóa, PersonMenu làm mẫu nhập/xuất và đa hình ở menu 6.
- InvoiceService kiểm tra cả giỏ, gộp mã sách trùng, tính tiền từ snapshot, chỉ cập nhật kho khi hóa đơn hợp lệ. Nhân viên được tra lại trong repository.
- StoreFile/StoreTextCodec nạp và lưu snapshot UTF-8, bao gồm tên/giá/ngày lịch sử. Main nạp lúc mở, menu 7/0 và EOF lưu thao tác đã hoàn tất. Không seed chồng dữ liệu cũ.
- Menu bắt lỗi đầu vào/nghiệp vụ; lỗi lưu ở menu 0 không làm thoát phiên. File hỏng không bị ghi đè bằng mẫu.

## Chữ ký constructor và API chính

```java
BookService(ICrudRepository<Book> repository)
CustomerService(ICrudRepository<Customer> repository)
PersonService(PersonDirectory people, IRepository<Invoice> invoices)
InvoiceService(IRepository<Invoice> invoices, IRepository<Book> books,
               IRepository<Customer> customers, IRepository<Employee> employees)
ReportService(IRepository<Invoice> invoices)
PersonMenu(PersonService people, InputHelper input)
StoreFile(Path path)

Invoice createInvoice(String id, String customerId, Employee employee,
                      Map<String, Integer> quantities)
StoreState StoreFile.load()
void StoreFile.save(StoreState state)
```

Các chữ ký là tham khảo, không chép thành code có dấu ; trong concrete class. Constructor InvoiceService đã đổi từ 3 sang 4 repository, Main và test đã cập nhật.

IRepository: add/getAll/findById. ICrudRepository extends IRepository, thêm delete cho sách/khách/nhân viên/danh sách người; không mở chức năng xóa hóa đơn. add từ chối lỗi trước khi đổi dữ liệu; find không có trả null; getAll không trả list gốc, nhưng đối tượng trong list vẫn là tham chiếu sống.

Mã trim/uppercase, không trùng trong PersonDirectory; chuỗi bắt buộc không rỗng và không có dấu phân cách file hoặc xuống dòng. Giá hữu hạn >0, kho >=0, lượng bán >0. Model lỗi ném IllegalArgumentException; checkout thiếu kho ném IllegalStateException; lỗi file là IOException. CustomerService còn thiếu kiểm tra định dạng phone chuyên biệt, giao Nam hoàn thiện.

Bán qua createInvoice; báo cáo qua ReportService. Không thêm lại sellBook hoặc addInvoice công khai làm lối bỏ qua checkout. Invoice.complete khóa chi tiết; getDetails không sửa được. Dùng getBookTitle/getBookId/getPrice, getCustomerName/getEmployeeName/getCustomerPhone/getEmployeePosition khi làm lịch sử. getBook/getCustomer/getEmployee không phải API để cập nhật danh mục.

PersonService chặn xóa người có hóa đơn và NV001, chặn thay toàn bộ danh sách khi đã có hóa đơn. Nam bổ sung nút xóa khách bằng quy tắc này, không bypass qua repository. BookService hiện cho xóa danh mục sách; loader vẫn giữ hóa đơn lịch sử bằng snapshot. Không tự nạp sách lưu trữ vào danh mục bán hiện tại.

## File và cấu hình

StoreConfig: DEFAULT_EMPLOYEE_ID, STORE_NAME, FILE_VERSION, TEXT_CHARSET, STORE_FILE và static resolveStoreFile(args). Mặc định data/store.txt; --data <path> chọn file khác.

Schema VERSION|1 có BOOK, CUSTOMER, EMPLOYEE, INVOICE, DETAIL và END với tổng số bản ghi. Định nghĩa chính xác từng cột trong LEADER_MTRI.md, dữ liệu hợp lệ ở samples/store.txt. Đổi field/schema phải cập nhật encode, decode, version/migration, sample và test cùng lúc.

StoreFile.save dùng FileManager.writeLinesAtomic trên một snapshot; không thay bằng ghi từng repository thành nhiều file rời. Đây là lưu phiên qua menu, không phải tự commit đĩa trong mỗi createInvoice. Nếu filesystem không hỗ trợ atomic replace thì báo lỗi, không tự ghi đè fallback.

## Phần thành viên tiếp tục

| Người | Phần tiếp tục |
|---|---|
| Trí | Review PR, chốt các thay đổi hợp đồng/schema, tổng hợp module hoàn thiện, chạy kiểm thử và demo |
| Phú | category, tìm sách nhiều trường, BookMenu/BookService đầy đủ và schema sách mở rộng |
| Nam | CustomerMenu/CustomerService sửa/xóa, định dạng phone, trình bày kế thừa/đa hình; dùng mẫu PersonService |
| Long | Báo cáo top sách/lọc ngày, tìm/xem hóa đơn chi tiết; dùng dữ liệu lịch sử đã được bảo vệ |
| Hoàng | Mở rộng InputHelper/FileManager, bộ dữ liệu và kiểm thử môi trường; mẫu nạp/lưu đã nối để học theo |

Phần mẫu trung tâm có thao tác khách/nhân viên nhưng không đồng nghĩa menu riêng của các bạn đã hoàn tất.

## Kiểm chứng và giới hạn

Java thuần, đích 17; có bộ RegressionChecks và LeaderChecks. README chứa lệnh chạy trực tiếp nếu PowerShell chặn script. Ghi kết quả thực tế của lần chạy cuối trong bàn giao/PR, không tự đánh dấu checklist đã qua khi mới viết test.

Một tiến trình, chưa đồng bộ nhiều người dùng; chưa tự lưu từng lần bán; dữ liệu chưa lưu có thể mất khi tắt cưỡng bức. Double có giới hạn tiền lẻ. Chỉ schema phiên bản 1 đang hỗ trợ; file VERSION khác sẽ báo lỗi để tránh đọc nhầm.
