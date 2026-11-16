# Sơ đồ lớp BookStoreManagement

Sơ đồ tập trung vào các quan hệ nghiệp vụ và OOP chính. Các lớp giao diện gọi service; `StoreSession` phối hợp lưu toàn bộ trạng thái sau mỗi lệnh thay đổi.

```mermaid
classDiagram
    class Person {
        <<abstract>>
        -String id
        -String name
        +getRole()* String
    }
    class Customer {
        -String phone
        +getRole() String
        +toString() String
    }
    class Employee {
        -String phone
        -String position
        +getRole() String
        +toString() String
    }
    Person <|-- Customer
    Person <|-- Employee
    class IRepository~T~ {
        <<interface>>
        +add(T)
        +findById(String) T
        +getAll() List
    }
    class ICrudRepository~T~ {
        <<interface>>
        +delete(String) boolean
    }
    IRepository <|-- ICrudRepository
    ICrudRepository <|.. PersonDirectory
    ICrudRepository <|.. CustomerRepository
    ICrudRepository <|.. EmployeeRepository
    ICrudRepository <|.. BookRepository
    IRepository <|.. InvoiceRepository
    class PersonDirectory {
        -Person[] people
        -int size
        +initialize(Person[])
        +addAll(Person[])
        +updateById(String, Person) boolean
        +search(String, String, boolean) Person[]
        +countByRole() Map
    }
    class BookRepository {
        -Book[] books
        -int size
    }
    class InvoiceRepository {
        -Invoice[] invoices
        -int size
    }
    PersonDirectory o-- Person
    CustomerRepository --> PersonDirectory : cùng mảng
    EmployeeRepository --> PersonDirectory : cùng mảng
    BookRepository o-- Book
    InvoiceRepository o-- Invoice
    class Book {
        -String id
        -String title
        -String category
        -double purchasePrice
        -double price
        -int quantity
    }
    Invoice --> Customer : tham chiếu và snapshot
    Invoice --> Employee : tham chiếu và snapshot
    Invoice "1" *-- "1..*" InvoiceDetail
    InvoiceDetail --> Book : tham chiếu và snapshot giá/tên
    StoreState *-- PersonDirectory
    StoreState *-- BookRepository
    StoreState *-- CustomerRepository
    StoreState *-- EmployeeRepository
    StoreState *-- InvoiceRepository
    StoreSession --> StoreState
    StoreSession --> StoreFile
    StoreFile --> StoreTextCodec
    StoreFile --> FileManager
    PersonService --> PersonDirectory
    PersonService --> InvoiceRepository : bảo vệ lịch sử
    BookService --> ICrudRepository
    CustomerService --> ICrudRepository
    InvoiceService --> IRepository
    ReportService --> IRepository
    Main --> MainMenu
    MainMenu --> BookMenu
    MainMenu --> CustomerMenu
    MainMenu --> EmployeeMenu
    MainMenu --> InvoiceMenu
    MainMenu --> InventoryMenu
    MainMenu --> ReportMenu
    MainMenu --> StoreSession
    EmployeeMenu --> PersonService
    CustomerMenu --> PersonService
    InvoiceMenu --> InvoiceService
    InventoryMenu --> StoreSession
    ReportMenu --> ReportService
```

Các mảng `Person[]`, `Book[]`, `Invoice[]` tự tăng kích thước bằng `Arrays.copyOf`; xóa phần tử dồn các phần tử còn lại. `List` trả về từ repository là bản sao; không thay thế mảng lưu trữ chính. Chi tiết hóa đơn vẫn dùng danh sách bất biến từ phía người gọi, theo API hiện có.

Đa hình được gọi thật: `PersonDirectory.search`, `countByRole`, `PersonService.initializeRole` và màn hình danh sách nhân viên gọi `getRole()` qua biến `Person`; khi in đối tượng qua `Person`, Java chọn `toString()` của lớp con.

`PersonMenu` và constructor/API cũ được giữ để các bài kiểm tra và ví dụ thao tác mảng hiện có tiếp tục dùng. Ứng dụng thực tế khởi động qua `Main.main → runInteractive`, với menu khách hàng và nhân viên riêng.
