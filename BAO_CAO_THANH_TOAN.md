# Sửa nghiệp vụ lập hóa đơn và thanh toán

Báo cáo này cập nhật phần thanh toán trong báo cáo nâng cấp trước. Không sửa trực tiếp dữ liệu cửa hàng hiện có.

## Lỗi và cách xử lý

- Xác nhận cũ chỉ so sánh đúng chuỗi DONGY và chấp nhận mọi giá trị khác như quay lại giỏ. Nay dùng `trim()` và `equalsIgnoreCase()`: DONGY thanh toán, N quay lại giỏ, HUY hủy và kết thúc; giá trị khác yêu cầu nhập lại ngay tại bước xác nhận.
- Repository từng tự gọi `complete()` khi thêm hóa đơn. Nay chỉ nhận hóa đơn đã PAID, không tự biến bản nháp thành giao dịch đã thanh toán.
- Thống kê doanh thu từng có lọc hoàn tất nhưng thống kê số lượng bán chưa lọc. Nay tất cả doanh thu, số lượng bán và lịch sử mua hàng đều chỉ lấy PAID.
- Trước đây chỉ có boolean completed và ngày lập. Nay `Invoice.Status` phân biệt DRAFT, PAID, CANCELLED; giữ `isCompleted()` để tương thích API cũ. Hủy giỏ không ghi vào kho hóa đơn bán hàng, không trừ kho hay tính doanh thu. Không hỗ trợ dùng hủy giỏ để hủy một hóa đơn đã thanh toán.
- Bổ sung `createdAt`, `paidAt`. Ngày doanh thu là ngày thanh toán; hóa đơn cũ chưa có thời điểm thanh toán dùng ngày lịch sử đã lưu, không tự gán thời gian mới.

## Thanh toán đồng bộ

1. `InvoiceService.prepareInvoice` kiểm tra mã không trùng, khách hàng và nhân viên còn tồn tại, mọi sách còn tồn tại, số lượng dương/tồn kho, tổng tiền hữu hạn. Chỉ tạo bản xem trước DRAFT, chưa thay đổi dữ liệu.
2. Màn hình xem trước có mã, ngày giờ lập, thông tin khách hàng/nhân viên, chi tiết số lượng–đơn giá–thành tiền, tổng tiền, giảm giá 0 và số phải thanh toán.
3. Khi xác nhận, `payInvoice` kiểm tra lại dữ liệu và so sánh đơn giá/tổng tiền với bản đã hiển thị. Nếu giá đổi, yêu cầu xem và xác nhận lại. Bản ghi đã thanh toán giữ thời điểm lập của bản xem trước, đơn giá lịch sử và thời điểm thanh toán thực tế.
4. Ứng dụng chạy thanh toán qua `StoreSession.change`: hóa đơn, tồn kho và nhật ký kho cùng được lưu trong một snapshot. Nếu ghi file lỗi, phục hồi toàn bộ mảng; bản xem trước vẫn DRAFT. Chỉ in THANH TOÁN THÀNH CÔNG sau khi lưu xong.
5. Biên nhận hiển thị mã, tổng tiền đã thanh toán, thời gian và tồn kho còn lại từng sách. Thất bại hiển thị nguyên nhân và giữ người dùng trong quy trình lập hóa đơn để xử lý hoặc hủy.

API `createInvoice` hiện có tiếp tục dùng chung bước kiểm tra/commit. Entry point menu cũ được giữ cho giao thức kiểm thử cũ; CLI thực tế đi qua `Main.main → runInteractive` và bước xác nhận mới.

## File text và tương thích

Phiên bản 3 giữ các bản ghi sách/người/nhật ký và END như phiên bản 2. Dòng INVOICE giữ 9 trường cũ, bổ sung lần lượt `createdAt|paidAt|PAID`. Dữ liệu v1/v2 vẫn đọc được. `paidAt` rỗng chỉ biểu thị thời điểm cũ không có thông tin; giao diện ghi rõ điều đó.

Trước khi ghi v1/v2 sang v3, `StoreFile` sao lưu nguyên byte thành `.v1.bak`/`.v2.bak`. Nếu tên backup đã có nội dung khác, dùng tên mới chứa UUID, không ghi đè backup cũ. Dữ liệu hỏng bị từ chối, không tự seed đè lên file lỗi. Mọi test sử dụng file riêng dưới build.

## Giá nhập 0 và thể loại

`SampleData` gọi constructor Book cũ, chỉ có giá bán và số lượng; do đó giá nhập mặc định 0, thể loại mặc định “Chưa phân loại”. File v1 cũng chưa có hai trường này. Codec đọc v2/v3 giữ đúng giá nhập/thể loại đã có, không làm mất trường đã lưu.

Quy tắc: giá bán phải hữu hạn và >0; giá nhập hữu hạn, >=0; 0 là chưa có thông tin giá nhập. Giao diện nay hiển thị “Chưa có” thay vì 0 để tránh hiểu nhầm là giá mua thực tế. Giá trị tồn kho theo giá nhập đã có cảnh báo khi thông tin chưa đầy đủ.

Theo yêu cầu trước, menu sách đã bỏ thể loại và sắp xếp, bảng hiển thị tác giả. Nếu còn thấy cột thể loại, cần chạy đúng classpath build vừa biên dịch, tránh bản cũ trong out.

Cách bổ sung an toàn: xác định giá nhập thực từ chứng từ rồi vào Quản lý sách → Sửa thông tin để nhập giá đúng. Không đoán giá nhập, không chạy lại seed lên dữ liệu hiện có, không bật lại chức năng thể loại đã bỏ. Chưa sửa SampleData hoặc giá trị trong store.txt vì không có thông tin giá nhập thực tế.

## File thay đổi trong đợt này

- `src/model/Invoice.java`: trạng thái, thời điểm và kiểm tra chuyển trạng thái.
- `src/business/InvoiceService.java`: xem trước, kiểm tra lại giá/tham chiếu, thanh toán.
- `src/business/ReportService.java`: lọc PAID cho mọi phép thống kê, lọc ngày thanh toán.
- `src/data/InvoiceRepository.java`: bỏ tự hoàn tất hóa đơn.
- `src/data/StoreTextCodec.java`, `StoreFile.java`, `src/utility/StoreConfig.java`: v3 và backup v1/v2.
- `src/presentation/InvoiceMenu.java`, `ConsoleView.java`: xác nhận, biên nhận, hiển thị thông tin thiếu.
- `src/presentation/CustomerMenu.java`, `ReportMenu.java`: ngày mua gần nhất/ngày thanh toán.
- `tests/PaymentChecks.java`: bộ test mới; `tests/UpgradeChecks.java`: cập nhật fixture theo v3, trạng thái PAID tường minh và quy tắc xác nhận mới. Không sửa hai bộ RegressionChecks/LeaderChecks.
- `README.md` và báo cáo này: cách chạy, định dạng và kiểm thử.

Không thay đổi các mảng chính, abstract/interface, mô hình kế thừa hoặc tiêu chí OOP. Không khôi phục script kiểm thử đã bị xóa trong cây làm việc; README cung cấp lệnh trực tiếp chạy cả bốn bộ test.

## Kết quả nghiệm thu

Biên dịch toàn bộ source và tests bằng `javac -Xlint:all -encoding UTF-8 --release 17`, không lỗi/cảnh báo. Bytecode của bản chạy là Java 17 (major 61); 40 class/enum/lớp lồng trong build trùng byte với bản đã kiểm thử. Máy hiện có JDK 27 nên phần thực thi được kiểm tra bằng Java 27, chưa xác nhận trên JVM 17 riêng.

| Bộ kiểm thử | Kết quả |
|---|---:|
| RegressionChecks | 34 đạt |
| LeaderChecks | 64 đạt |
| UpgradeChecks | 77 đạt |
| PaymentChecks | 83 đạt |
| Tổng | **258 đạt** |

Log nghiệm thu: `build/payment-final-141aeff4c1dd4532b0ba7b748c983ce4/`. PaymentChecks chạy các luồng yêu cầu: DONGY, dongy, Dongy và khoảng trắng; xác nhận sai/rỗng; N quay lại giỏ; HUY không phân biệt hoa/thường; mua đủ tồn/vượt tồn; nhiều loại sách; tồn kho sau bán; thoát/khởi động lại; doanh thu và lịch sử. Có kiểm tra thêm lỗi ghi file, đổi giá sau bản xem trước, mất khách hàng/nhân viên/sách, chặn draft/cancelled, ngày thanh toán qua nửa đêm và chuyển v2→v3.

CLI thực tế cũng được chạy bằng `java -cp build presentation.Main --data <file-thử-riêng>`: nhập sai → N → dongy tạo đúng một hóa đơn; giao dịch tiếp theo nhập Huy không ghi nhận bán hàng. Timestamp, trạng thái và tồn kho lưu đúng trong file v3.

Các lệnh phát triển/kiểm thử chỉ ghi fixture dưới build, không trực tiếp sửa dữ liệu cửa hàng. Trong phiên làm việc, file chạy thật đã được chuyển từ v2 sang v3; đã kiểm tra toàn bộ bản ghi ngoài dòng VERSION vẫn giống bản backup `data/store.txt.v2.bak`. SHA-256 backup là `87D9DB8722A1705A9ABF4E93D59F40C5002F7C533BFDA7EC7893117567D1471A`, đúng với hash file lúc bắt đầu kiểm tra. Không ghi đè thay đổi đang có trong samples/store.txt.

Chạy phiên bản đã biên dịch:

```powershell
java -cp build presentation.Main
```
