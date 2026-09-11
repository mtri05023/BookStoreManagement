# BookStoreManagement

Ứng dụng quản lý cửa hàng sách bằng Java, chạy trên Terminal.

## Kiến trúc 3 lớp

- presentation: giao diện Terminal
- business: xử lý nghiệp vụ
- data: quản lý dữ liệu
- model: các đối tượng nghiệp vụ

## Chạy bằng IntelliJ IDEA

Mở thư mục project, đặt `src` là Sources Root và chạy:

presentation.Main

## Chức năng

- Quản lý sách: thêm, xem, tìm, sửa, xóa
- Quản lý khách hàng
- Lập hóa đơn
- Tự động trừ tồn kho khi bán
- Xem hóa đơn
- Thống kê doanh thu

Dữ liệu hiện lưu bằng ArrayList, chưa dùng database.
