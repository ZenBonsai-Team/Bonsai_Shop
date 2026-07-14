# BSMS - Bonsai Shop Management System (Hệ thống Quản lý Cửa hàng Cây cảnh)

BSMS là hệ thống quản lý và bán hàng trực tuyến dành riêng cho các cửa hàng kinh doanh cây cảnh (Bonsai). Hệ thống hỗ trợ khách hàng mua sắm, đặt lịch hẹn xem cây trực tiếp tại cửa hàng, đồng thời cung cấp giao diện quản trị giúp cửa hàng quản lý sản phẩm, đơn hàng và phân quyền hệ thống một cách tối ưu.

---

## 🚀 Tính năng nổi bật

### 1. Quản lý danh mục & Sản phẩm (Catalog Management)

- **Thông tin sản phẩm**: Quản lý chi tiết cây cảnh (tên, mô tả, giá cả, hình ảnh sản phẩm đa phương tiện qua `ProductMedia`).
- **Phân loại sản phẩm**: Phân nhóm theo phân khúc (`ProductSegment`), thể loại (`Category`), chủng loại (`Variety`), và từ khóa (`Tag`).

### 2. Mua sắm & Đơn hàng (E-commerce Flow)

- **Giỏ hàng (`Cart`)**: Cho phép khách hàng thêm, sửa, xóa sản phẩm trong giỏ hàng.
- **Đơn hàng (`BonsaiOrder`)**: Quy trình đặt hàng, chi tiết đơn hàng (`OrderDetail`), và ghi nhận lịch sử xử lý đơn hàng (`OrderLog`, `OrderHandling`).
- **Thanh toán (`Payment`)**: Hỗ trợ tích hợp và ghi nhận trạng thái thanh toán của các đơn hàng.

### 3. Đặt lịch hẹn xem cây (`ViewingAppointment`)

- Hỗ trợ khách hàng đặt lịch hẹn đến trực tiếp cửa hàng/vườn cây để xem và kiểm tra cây cảnh trước khi quyết định mua.
- Quản lý trạng thái lịch hẹn (Chờ duyệt, Đã duyệt, Đã hoàn thành, Hủy).

### 4. Tương tác & Trải nghiệm khách hàng

- **Sản phẩm yêu thích (`Wishlist`)**: Lưu lại các cây cảnh mà khách hàng quan tâm.
- **Đánh giá & Phản hồi (`Review`)**: Khách hàng có thể viết đánh giá, chấm điểm sao cho các sản phẩm đã mua.
- **Khôi phục mật khẩu**: Chức năng gửi OTP qua email (`PasswordResetOtp`) giúp lấy lại mật khẩu an toàn.

### 5. Phân quyền hệ thống bảo mật cao (Role-Based Access Control - RBAC)

- Phân quyền động chi tiết đến từng hành động nghiệp vụ (`BusinessAction`) cho mỗi vai trò (`Role`) thông qua bảng trung gian `RoleAction`.

---

## 🛠️ Công nghệ sử dụng

- **Backend**: Spring Boot 4.0.6 (Java 21)
- **Security**: Spring Security (xác thực và phân quyền truy cập)
- **Database**: MySQL Server
- **ORM**: Spring Data JPA / Hibernate 7
- **Frontend**: Thymeleaf, HTML5, CSS3, JavaScript
- **Build Tool**: Maven

---

## 📋 Hướng dẫn cài đặt & Chạy ứng dụng

### Yêu cầu hệ thống

- **Java Development Kit (JDK)**: Phiên bản 21
- **Database**: MySQL Server (phiên bản 8.0 trở lên)
- **Build Tool**: Maven (đã tích hợp sẵn Maven Wrapper trong dự án)

### Bước 1: Cấu hình cơ sở dữ liệu

1. Mở phần mềm quản trị MySQL (ví dụ: MySQL Workbench, Navicat, DBeaver hoặc Terminal).
2. Tạo một database mới tên là `bonsai_shop`:
   ```sql
   CREATE DATABASE bonsai_shop CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
   ```
3. Chạy script SQL database schema của nhóm bạn để tạo các bảng dữ liệu cần thiết.

### Bước 2: Cấu hình ứng dụng

Mở file [application.properties](src/main/resources/application.properties) và chỉnh sửa thông tin kết nối database phù hợp với máy của bạn:

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/bonsai_shop?useSSL=false&serverTimezone=Asia/Ho_Chi_Minh&allowPublicKeyRetrieval=true&characterEncoding=UTF-8
spring.datasource.username=root
spring.datasource.password=<MẬT_KHẨU_MYSQL_CỦA_BẠN>
```

### Bước 3: Khởi chạy dự án từ Terminal

Di chuyển vào thư mục gốc của dự án và chạy các lệnh dưới đây tùy theo hệ điều hành:

- **Trên Windows (PowerShell):**
  ```powershell
  $env:JAVA_HOME = "C:\Program Files\Microsoft\jdk-21.0.11.10-hotspot"  # Chỉ định JDK 21 nếu cần
  .\mvnw spring-boot:run
  ```
- **Trên Windows (Command Prompt - cmd):**
  ```cmd
  set JAVA_HOME=C:\Program Files\Microsoft\jdk-21.0.11.10-hotspot
  mvnw spring-boot:run
  ```
- **Trên Linux / macOS / Git Bash:**
  ```bash
  export JAVA_HOME="/path/to/your/jdk-21"
  ./mvnw spring-boot:run
  ```

Sau khi ứng dụng khởi chạy thành công, truy cập giao diện tại: **[http://localhost:8080](http://localhost:8080)**

---

## 📂 Cấu trúc thư mục dự án chính

```text
Bonsai_Shop/
├── .mvn/                   # Cấu hình Maven Wrapper
├── src/
│   ├── main/
│   │   ├── java/           # Mã nguồn Java (Controller, Service, Repository, Entity, Config)
│   │   └── resources/
│   │       ├── public/     # Các file tĩnh (CSS, JS, Images)
│   │       ├── templates/  # Giao diện HTML (Thymeleaf views)
│   │       └── application.properties # Cấu hình ứng dụng
├── pom.xml                 # File quản lý thư viện Maven
└── README.md               # Tài liệu hướng dẫn dự án
```

---

## 👥 Thành viên nhóm phát triển

| Họ và Tên                  | Vai trò            | Email                       |
| -------------------------- | ------------------ | --------------------------- |
| **Bùi Nam Kha**            | Developer / Leader | khabnhe182308@fpt.edu.vn    |
| **Phạm Quang Anh**         | Developer          | anhpqhe180656@fpt.edu.vn    |
| **Nguyễn Kiều Tùng Dương** | Developer          | duongnkthe186476@fpt.edu.vn |

|
| _(Các thành viên khác)_ | ... | ... |
