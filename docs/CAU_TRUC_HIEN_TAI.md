# Cấu trúc và hợp đồng hiện tại sau rà soát

Cập nhật 20/10/2026. Đọc file này trước khi mở rộng module; báo cáo khảo sát ban đầu chỉ phản ánh source trước khi sửa nền.

## Nền đã sửa

- Giữ package presentation; Main nối các repository dùng chung, menu không tự tạo repository.
- Person abstract giữ id/name; Customer/Employee kế thừa và override getRole. ModelChecks là helper package-private trong model, không phụ thuộc UI/service.
- Model kiểm tra chuỗi bắt buộc, chuẩn hóa mã trim/uppercase, chặn giá không hữu hạn và tồn kho âm. Điện thoại hiện chỉ kiểm tra không rỗng, quy tắc định dạng chi tiết vẫn giao Nam.
- IRepository có add/getAll/findById; ICrudRepository extends IRepository và thêm delete, chỉ dùng cho sách/khách. Không ép hóa đơn có API xóa.
- Ba repository chặn null/mã trùng, tìm mã trim và không phân biệt hoa thường; getAll trả bản sao danh sách, không phải deep copy đối tượng.
- BookService/CustomerService nhận ICrudRepository; InvoiceService/ReportService nhận IRepository. BookService.updateBook kiểm tra hết đầu vào trước khi sửa để không cập nhật dở.
- InvoiceService sở hữu checkout. InvoiceMenu gom giỏ tạm, 0 xác nhận, -1 hủy; nhập từng dòng không trừ kho. EOF trước xác nhận cũng không trừ kho.
- Invoice có LocalDate, snapshot tên khách/nhân viên; InvoiceDetail giữ snapshot mã/tên/giá sách. Hóa đơn hoàn tất không cho thêm chi tiết, getDetails trả list không sửa được.
- Menu bắt lỗi dữ liệu/nghiệp vụ, tiếp tục điều hướng; EOF truyền về Main để thoát. Dọn import dư và thêm xử lý lựa chọn menu sai.

## Chữ ký thực tế để các bạn dùng

```java
BookService(ICrudRepository<Book> repository)
CustomerService(ICrudRepository<Customer> repository)
ReportService(IRepository<Invoice> repository)
InvoiceService(IRepository<Invoice> invoices,
               IRepository<Book> books,
               IRepository<Customer> customers)

Invoice createInvoice(String id, String customerId, Employee employee,
                      Map<String, Integer> quantities)
```

Chữ ký ở đây dùng để tra cứu, không chép thành khai báo phương thức không có thân trong concrete class.

Checkout: chuẩn hóa/gộp mã trùng trong giỏ, chặn tràn số lượng, kiểm tra tất cả sách và tồn kho, tạo hóa đơn rồi thêm vào repository bộ nhớ trước khi cập nhật lượng còn lại. Nếu repository hiện tại từ chối thêm, kho không đổi. Khi viết repository mới, add phải kiểm tra lỗi trước khi thay dữ liệu; không thêm xong rồi ném lỗi như thể chưa thêm. Ứng dụng một luồng; chưa hỗ trợ giao dịch nhiều người dùng.

**Thay đổi cần biết:** đã bỏ InvoiceService.addInvoice, InvoiceService.getRevenue và BookService.sellBook. Bán hàng gọi createInvoice; thống kê gọi ReportService.getRevenue. Main và menu đã cập nhật. Không thêm lại lối tắt bỏ qua kiểm tra cả giỏ.

Model có lỗi đầu vào thì ném IllegalArgumentException; service thêm/sửa sách vẫn giữ boolean cho đầu vào kiểm tra được tại service. Menu phải bắt lỗi constructor khi tự new model. Checkout ném IllegalArgumentException cho dữ liệu sai và IllegalStateException cho thiếu kho. Null dependency constructor là lỗi lập trình, không phải lỗi để yêu cầu người dùng nhập lại.

Constructor cũ Book/Customer/Employee/Invoice vẫn dùng được với dữ liệu hợp lệ. Invoice thêm constructor nhận LocalDate; InvoiceDetail thêm constructor nhận đơn giá lịch sử. Loader snapshot tên lịch sử từ file chưa có: không dùng constructor chụp tên hiện tại rồi tuyên bố đã phục hồi đúng lịch sử.

Getter getBook/getCustomer/getEmployee vẫn là tham chiếu sống để tương thích. Khi in/báo cáo lịch sử dùng getBookId/getBookTitle/getPrice, getCustomerName/getEmployeeName. Không sửa danh mục qua các tham chiếu đó.

## Việc mỗi thành viên bắt đầu làm tiếp

| Người | Phần tiếp tục | Điểm kết nối cần giữ |
|---|---|---|
| Trí | Review hợp đồng, cơ chế commit lưu file với Hoàng, kiểm tra liên kết khi xóa, tích hợp/demo | Một luồng checkout duy nhất, repository chung |
| Phú | category, tìm sách theo tên/tác giả/thể loại, kiểm thử CRUD và codec sách | Giữ constructor đang gọi hoặc thêm overload; không mở lại sellBook |
| Nam | CustomerService sửa/xóa và menu; định dạng phone, codec khách/nhân viên | CustomerRepository.delete đã có; không lặp field Person |
| Long | Top sách, lọc ngày và menu báo cáo; loader đầy đủ snapshot hóa đơn | Dùng giá/ngày/tên lịch sử; chỉ hóa đơn hoàn tất |
| Hoàng | Dữ liệu file, cơ chế nạp/lưu, xử lý lỗi ghi, hướng dẫn chạy | FileManager hiện chỉ I/O dòng, chưa atomic commit |

Quy tắc chặn xóa sách/khách đã có hóa đơn chưa nối vào service. Trí phối hợp các chủ module trước khi bổ sung API xóa khách; hiện deleteBook còn xóa trong danh mục nhưng snapshot hóa đơn trong bộ nhớ vẫn giữ thông tin bán. Không coi việc xóa trong bộ nhớ là đã có hợp đồng lưu/nạp lịch sử khi mất bản ghi liên kết.

## Giới hạn hiện tại

Vẫn dùng double cho tiền; chưa tự đổi sang long để tránh phá API nhóm. Chặn NaN/Infinity và tràn kết quả nhưng chưa giải quyết mọi sai số tiền lẻ. Chưa tích hợp lưu file, không phục hồi dữ liệu sau thoát. StoreConfig.STORE_FILE là đường dẫn dành cho bước tiếp theo. FileManager.writeLines ghi trực tiếp; phải bổ sung cơ chế snapshot/commit và kiểm thử lỗi trước khi dùng cho checkout.

## Bằng chứng kiểm tra

Biên dịch toàn bộ với javac 27, đích --release 17 và -Xlint:all; không còn cảnh báo ở lần kiểm tra cuối. Chạy RegressionChecks: 34 kiểm tra đạt, gồm đa hình Person, interface, mã trùng/null, danh sách bảo vệ, validation, cập nhật không dở, thiếu kho nhiều sách, giỏ trùng mã, hóa đơn trùng, snapshot lịch sử, báo cáo, giả lập repository từ chối thêm, hủy/EOF khi nhập giỏ, retry và UTF-8 round-trip. Đã chạy thêm luồng menu bán 2 sách Java Core, hóa đơn và doanh thu đều 300000.

Script PowerShell có thể bị execution policy trên máy chặn; README có lệnh trực tiếp để chạy cùng các kiểm tra. Đây là kiểm tra hồi quy nền, không thay thế kiểm thử các chức năng thành viên sẽ bổ sung.
