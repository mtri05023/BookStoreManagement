# BookStoreManagement

Ứng dụng quản lý cửa hàng sách bằng Java, chạy trên Terminal.

## Kiến trúc 3 lớp

Project được xây dựng theo mô hình 3 lớp:

- `terminal`: giao diện và tương tác với người dùng trên Terminal
- `business`: xử lý nghiệp vụ của hệ thống
- `data`: quản lý và lưu trữ dữ liệu
- `model`: chứa các đối tượng nghiệp vụ
- `utility`: các lớp tiện ích dùng chung

Luồng xử lý:

Terminal → Business → Data → File

## Cấu trúc project

```text
BookStoreManagement/
└── src/
    ├── model/
    │   ├── Person.java
    │   ├── Customer.java
    │   ├── Employee.java
    │   ├── Book.java
    │   ├── Invoice.java
    │   └── InvoiceDetail.java
    │
    ├── data/
    │   ├── IRepository.java
    │   ├── BookRepository.java
    │   ├── CustomerRepository.java
    │   └── InvoiceRepository.java
    │
    ├── business/
    │   ├── BookService.java
    │   ├── CustomerService.java
    │   ├── InvoiceService.java
    │   └── ReportService.java
    │
    ├── utility/
    │   ├── FileManager.java
    │   ├── StoreConfig.java
    │   └── InputHelper.java
    │
    └── terminal/
        ├── Main.java
        ├── MainMenu.java
        ├── BookMenu.java
        ├── CustomerMenu.java
        └── InvoiceMenu.java