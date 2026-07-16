# HƯỚNG DẪN SỬ DỤNG FLYWAY TRONG DỰ ÁN BONSAI SHOP

Tài liệu này hướng dẫn cách thức hoạt động, cài đặt và quy trình phối hợp làm việc nhóm sử dụng **Flyway** để đồng bộ cơ sở dữ liệu (Database Schema & Seed Data) tự động giữa các thành viên.

---

## 1. Giới thiệu Flyway
Trong các dự án phần mềm có nhiều thành viên tham gia phát triển, việc đồng bộ cơ sở dữ liệu thường gặp khó khăn:
- Mỗi khi có ai đó thêm bảng, đổi tên cột hay sửa dữ liệu mẫu, các thành viên khác phải chạy các file `.sql` thủ công bằng tay.
- Rất dễ xảy ra tình trạng code chạy được trên máy người này nhưng crash trên máy người khác vì lệch cấu trúc DB.

**Flyway** là một thư viện quản lý phiên bản cơ sở dữ liệu (Database Migrations). Nó hoạt động tương tự như Git nhưng áp dụng cho Database. Nó tự động chạy các tệp tin SQL theo đúng thứ tự phiên bản để đảm bảo mọi máy phát triển (Local, Staging, Production) luôn có cấu trúc DB đồng nhất 100%.

---

## 2. Cách hoạt động của Flyway trong Spring Boot
Khi ứng dụng Spring Boot khởi động:
1. Thư viện Flyway sẽ tự động quét thư mục `src/main/resources/db/migration/` để tìm các tệp tin SQL di trú.
2. Nó kiểm tra bảng lịch sử di trú trong Database (tên là `flyway_schema_history` - bảng này được Flyway tự động tạo ra).
3. Nếu phát hiện thấy có tệp tin di trú nào chưa được chạy trên database local của bạn, nó sẽ tự động chạy tệp tin đó theo đúng thứ tự phiên bản (ví dụ: `V1` -> `V2` -> `V3`...).
4. Sau khi chạy xong, Flyway ghi nhận lại phiên bản, mã băm (checksum) của tệp tin đó vào bảng `flyway_schema_history` để không chạy lại lần sau.

---

## 3. Quy chuẩn đặt tên tệp Migration
Các tệp tin di trú SQL bắt buộc phải đặt trong thư mục:
`src/main/resources/db/migration/`

Tên tệp phải tuân theo quy tắc:
`V<Version>__<Description>.sql` (Chú ý: Có **2 dấu gạch dưới** liên tiếp ngăn cách giữa Phiên bản và Mô tả).

**Ví dụ:**
- `V1__init_schema.sql` (Kịch bản khởi tạo toàn bộ cấu trúc DB và dữ liệu hạt giống ban đầu)
- `V2__add_new_column_to_user.sql` (Bổ sung cột mới vào bảng người dùng)
- `V3__create_notification_table.sql` (Tạo bảng thông báo kiểm duyệt mới)

---

## 4. Quy trình phối hợp làm việc nhóm
Khi phát triển dự án chung, hãy tuân theo quy trình sau:

### Bước 1: Khi cần thay đổi cấu trúc DB local của bạn
1. **Tuyệt đối không** vào trực tiếp công cụ quản lý DB (MySQL Workbench, Navicat, DBeaver) để sửa cấu trúc bảng bằng tay mà không lưu lại.
2. Tạo một file `.sql` mới trong thư mục `src/main/resources/db/migration/` với phiên bản tăng dần tiếp theo (ví dụ: `V2__add_artisan_bio.sql`).
3. Viết mã SQL thay đổi cấu trúc đó vào file (ví dụ: `ALTER TABLE artisan_profile ADD COLUMN awards VARCHAR(255);`).
4. Khởi động lại ứng dụng Spring Boot local để Flyway tự áp dụng thay đổi vào DB local của bạn và kiểm tra tính đúng đắn.

### Bước 2: Commit mã nguồn lên Git
1. Khi push code lên GitHub, hãy đính kèm tệp tin SQL di trú mới này cùng với mã nguồn Java.
2. Các thành viên khác khi `git pull` code mới về chỉ cần khởi động lại ứng dụng Spring Boot. Flyway trên máy họ sẽ tự quét và chạy file SQL mới này vào DB local của họ một cách hoàn toàn tự động.

---

## 5. Giải quyết các sự cố thường gặp (Troubleshooting)

### Lỗi 1: Checksum Mismatch (Lệch mã Hash)
* **Nguyên nhân:** Xảy ra khi bạn chỉnh sửa nội dung của một tệp SQL di trú đã được đẩy lên Git và đã chạy trên máy của thành viên khác. Flyway sẽ báo lỗi vì mã băm file SQL local khác với mã băm đã lưu trong DB.
* **Cách khắc phục:** 
  1. Hạn chế tối đa việc chỉnh sửa các file SQL cũ đã push lên Git. Nếu cần thay đổi, hãy tạo một file `V` mới.
  2. Nếu đang ở môi trường phát triển local và muốn sửa nhanh, bạn có thể chạy lệnh:
     `mvn flyway:repair`
     Lệnh này sẽ đồng bộ lại mã băm checksum trong bảng lịch sử khớp với file SQL của bạn.
  3. Hoặc bạn có thể xóa sạch database (`DROP DATABASE bonsai_shop; CREATE DATABASE bonsai_shop;`) và chạy lại ứng dụng để khởi tạo lại toàn bộ từ đầu.

### Lỗi 2: Trùng số phiên bản (Version Conflict)
* **Nguyên nhân:** Hai thành viên cùng lúc tạo ra hai file di trú có số hiệu giống nhau (ví dụ cả hai đều tạo `V2__...sql` trên hai nhánh Git khác nhau). Khi merge nhánh sẽ xảy ra lỗi.
* **Cách khắc phục:** Khi merge code bị xung đột, hãy đổi tên số hiệu phiên bản của một trong hai file thành số lớn nhất tiếp theo (ví dụ đổi một file thành `V3__...sql`).
