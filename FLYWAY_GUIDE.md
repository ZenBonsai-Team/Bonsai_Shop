# Hướng Dẫn Cập Nhật & Sửa Đổi Cơ Sở Dữ Liệu Với Flyway

Dự án hiện tại sử dụng **Flyway** để tự động quản lý phiên bản cơ sở dữ liệu. Tất cả các thành viên trong nhóm cần tuân thủ nghiêm ngặt các quy tắc dưới đây khi sửa đổi cấu trúc bảng (DDL) hoặc thêm dữ liệu mẫu (Seed Data).

---

## 📌 Nguyên Tắc Vàng (Quan trọng nhất)
> ⚠️ **TUYỆT ĐỐI KHÔNG sửa đổi các file Migration đã chạy và đã push lên Git (Ví dụ: `V1__init_schema.sql`).**
> 
> * **Lý do:** Flyway quản lý các file đã chạy bằng mã băm (checksum). Nếu bạn thay đổi nội dung file đã chạy dù chỉ 1 ký tự, hệ thống của các thành viên khác và server deploy sẽ bị lỗi biên dịch: `Migration checksum mismatch` và ứng dụng **không thể khởi động**.

---

## 🛠️ Quy Trình Thêm/Sửa Đổi Database (Thêm Bảng, Thêm Cột, Thêm Dữ Liệu)

Khi cần thay đổi bất cứ điều gì liên quan đến Database, hãy thực hiện theo 3 bước sau:

### Bước 1: Tạo file migration mới
Tạo một file `.sql` mới nằm trong thư mục:
📂 `src/main/resources/db/migration/`

**Quy tắc đặt tên file:**
* Bắt đầu bằng chữ **`V`** viết hoa.
* Theo sau là **Số thứ tự tăng dần** (ví dụ: `V2`, `V3`, `V4`...).
* Sử dụng **2 dấu gạch dưới liên tiếp (`__`)** để phân tách số thứ tự và mô tả.
* Mô tả ngắn gọn bằng tiếng Anh (dùng dấu gạch dưới `_` thay khoảng trắng).

*Ví dụ:*
* `V2__add_role_seller.sql` (Thêm role mới)
* `V3__create_table_notification.sql` (Tạo bảng thông báo)
* `V4__add_avatar_column_to_user.sql` (Thêm cột avatar vào bảng user)

---

### Bước 2: Viết câu lệnh SQL cần cập nhật
Trong file mới tạo, chỉ viết các câu lệnh SQL phục vụ cho sự thay đổi đó. Không viết lại toàn bộ Database.

*Ví dụ nội dung file `V2__add_role_seller.sql`:*
```sql
-- Thêm quyền ROLE_SELLER vào bảng role
INSERT INTO `role` (RoleName, Description) 
VALUES ('ROLE_SELLER', 'Người bán hàng')
ON DUPLICATE KEY UPDATE RoleName=RoleName;
```

---

### Bước 3: Chạy thử và đẩy lên Git
1. Chạy dự án ở local của bạn (`mvn spring-boot:run` hoặc lệnh `r`). Flyway sẽ quét file mới và tự động chạy SQL đó trên Database local của bạn.
2. Kiểm tra xem DB local đã thay đổi đúng ý chưa.
3. Commit file SQL mới này và push lên GitHub.

Các thành viên khác sau khi `pull` code mới về chỉ cần chạy lại dự án, Database của họ sẽ tự động được cập nhật đồng bộ theo.

---

## ❌ Cách Xử Lý Khi Gặp Lỗi Checksum Mismatch (Lỗi do sửa file cũ)

Nếu lỡ tay sửa file cũ dẫn đến dự án báo lỗi:
`Migration checksum mismatch for migration version X`

**Cách khắc phục:**
1. Khôi phục lại nội dung file cũ về trạng thái ban đầu (dùng Git để discard các thay đổi trên file đó).
2. Nếu muốn thực hiện thay đổi đó, hãy đưa nó vào một file phiên bản mới (ví dụ: `V5__...sql`) như hướng dẫn ở trên.
