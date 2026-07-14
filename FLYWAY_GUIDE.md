# Hướng dẫn Quản lý Database bằng Flyway

Để đảm bảo cấu trúc và dữ liệu của Database luôn đồng bộ giữa tất cả các thành viên trong dự án, chúng ta sử dụng **Flyway** thay vì sửa đổi DB trực tiếp thông qua MySQL Workbench hay phpMyAdmin.

## Quy tắc bắt buộc
1. **Tuyệt đối không sửa Database trực tiếp** bằng tay. Nếu bạn thêm bảng, thêm cột, hoặc thay đổi dữ liệu chuẩn (seed data), bạn **PHẢI** tạo file migration của Flyway.
2. File migration phải được đặt trong thư mục: `src/main/resources/db/migration/`
3. Định dạng tên file: `V<Version>__<Tên_mô_tả>.sql` (Lưu ý: Có 2 dấu gạch dưới `__`).
   * Ví dụ: `V2__add_new_column_to_users.sql`
   * Ví dụ: `V3__insert_default_roles.sql`

## Cách hoạt động
- Mỗi khi bạn chạy ứng dụng Spring Boot, Flyway sẽ tự động kiểm tra xem có file `V...sql` nào mới so với những gì đã chạy trong database (bảng `flyway_schema_history`) hay không.
- Nếu có file mới, nó sẽ tự động chạy file đó để cập nhật DB cho bạn.
- Nhờ vậy, khi thành viên khác pull code về và chạy app, DB của họ cũng sẽ tự động được cập nhật giống hệt DB của bạn.

## Những lỗi thường gặp
- **Sửa file migration cũ:** Khi một file migration (ví dụ `V1__init_schema.sql`) đã được chạy, bạn **KHÔNG ĐƯỢC** sửa đổi nội dung của nó nữa (nếu sửa, Flyway sẽ báo lỗi checksum). Nếu muốn thay đổi gì, hãy tạo file `V2...sql` mới.
- **Xung đột version:** Nếu 2 người cùng tạo `V2`, hãy thảo luận đổi số version của 1 người thành `V3` trước khi merge code.

## Cách Fix khi Flyway bị lỗi Checksum
Nếu lỡ tay sửa file cũ khiến ứng dụng không khởi động được:
1. Mở MySQL, chạy lệnh: `DELETE FROM flyway_schema_history WHERE version = 'X';` (X là version bị lỗi).
2. Sửa lại file đó về nguyên trạng ban đầu, hoặc nếu đang ở local và database chưa có dữ liệu quan trọng, bạn có thể drop toàn bộ schema và cho Spring Boot (Flyway) tạo lại từ đầu.
