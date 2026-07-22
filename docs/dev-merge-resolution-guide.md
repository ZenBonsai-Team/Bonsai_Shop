# Nhật Ký & Hướng Dẫn Giải Quyết Conflict Merge `dev` Vào `DuongNKT`

---

## 1. THÔNG TIN CHUNG VỀ PHIÊN MERGE

- **Thời gian thực hiện**: 22/07/2026 - 16:16:28 (Múi giờ GMT+7)
- **Nhánh đích (Target Branch)**: `DuongNKT` (Commit HEAD: `258dcb3`)
- **Nhánh nguồn (Source Branch)**: `origin/dev`
- **Kết quả Merge Commit**: `72d7f13` (`merge: merge dev into DuongNKT - resolve all 8 conflicts`)
- **Trạng thái Build sau Merge**: `BUILD SUCCESS 100%` (Xác nhận qua `.\mvnw.cmd compile`)

---

## 2. NGUYÊN NHÂN GÂY NÊN CONFLICT (LÝ DO XUNG ĐỘT)

Do hai nhánh `DuongNKT` và `dev` đã phát triển song song trong một khoảng thời gian dài mà không merge thường xuyên:

1. **Nhánh `dev`**:
   - Thực hiện đợt đổi tên toàn bộ thuật ngữ từ `Seller` sang `Artisan` (Nhà vườn).
   - Bổ sung hệ thống phân quyền mới (`ROLE_OWNER`, `ROLE_ARTISAN`, `ROLE_MODERATOR`, `ROLE_CONTENT_MODERATOR`).
   - Bổ sung Google OAuth2 trong `application.properties`.
   - Thêm phương thức đặt chỗ nguyên tử `reserveIfAvailable()` trong `ProductRepository`.
2. **Nhánh `DuongNKT`**:
   - Tái cấu trúc chuẩn hệ thống giỏ hàng (`Cart` + `CartItem` loại bỏ `quantity`).
   - Bổ sung tính năng tự động điền thông tin tài khoản đang đăng nhập vào form Checkout.
   - Refactor thông báo Checkout từ `alert()` sang Bootstrap Toast/Modal & Redirect trang thành công `/order/success`.
   - Bổ sung Dashboard KPIs và luồng nhận/trả đơn cho Moderator trong `OrderRepository`.

---

## 3. CHI TIẾT 8 FILE BỊ CONFLICT VÀ CÁCH XỬ LÝ TỪNG FILE

---

### File 1: `User.java`
- **Đường dẫn**: `src/main/java/com/example/bonsai_shop/entity/User.java`
- **Lý do conflict**:
  - `HEAD` (DuongNKT) có quan hệ `@OneToMany List<Product> productsSold` (dùng chữ `seller` cũ) và `@OneToOne Cart cart`.
  - `dev` đổi tên quan hệ sản phẩm thành `@OneToMany List<Product> createdProducts` (dùng chữ `createdBy` mới) và có `@OneToOne Cart cart`.
- **Hướng xử lý**: Giữ quan hệ `@OneToOne Cart cart` và đổi tên quan hệ sản phẩm sang `@OneToMany List<Product> createdProducts` theo chuẩn mới của `dev`.

```java
    @OneToOne(mappedBy = "customer", cascade = CascadeType.ALL)
    private Cart cart;

    @OneToMany(mappedBy = "createdBy", cascade = CascadeType.ALL)
    private List<Product> createdProducts;
```

---

### File 2: `OrderRepository.java`
- **Đường dẫn**: `src/main/java/com/example/bonsai_shop/product/repository/OrderRepository.java`
- **Lý do conflict**:
  - `HEAD` định nghĩa các hàm đếm KPI cho Moderator: `countByAssignedToUserId` và `countByAssignedToUserIdAndOrderStatus`.
  - `dev` định nghĩa query lấy danh sách đơn của Artisan: `findByArtisanUserIdAndTypeAndStatus`.
- **Hướng xử lý**: **Giữ cả hai nhóm hàm** vì cả hai tính năng Moderator và Artisan đều cần thiết cho hệ thống.

```java
    // Đếm số đơn thuộc về Moderator cụ thể (DuongNKT)
    long countByAssignedToUserId(Integer moderatorId);
    long countByAssignedToUserIdAndOrderStatus(Integer moderatorId, String orderStatus);

    // Query lọc đơn của Artisan (dev)
    @Query("""
            SELECT DISTINCT o
            FROM Order o
            JOIN o.orderDetails od
            JOIN od.product p
            JOIN p.artisan a
            WHERE a.userId = :artisanUserId
              AND o.orderType = :orderType
              AND (:status = 'ALL' OR o.orderStatus = :status)
            ORDER BY o.orderDate DESC
            """)
    Page<Order> findByArtisanUserIdAndTypeAndStatus(
            @Param("artisanUserId") Integer artisanUserId,
            @Param("orderType") String orderType,
            @Param("status") String status,
            Pageable pageable);
```

---

### File 3: `ProductRepository.java`
- **Đường dẫn**: `src/main/java/com/example/bonsai_shop/product/repository/ProductRepository.java`
- **Lý do conflict**:
  - `HEAD` sử dụng các tên method cũ như `findBySellerUserId...` và query JOIN với `p.seller`.
  - `dev` đã tái cấu trúc toàn bộ sang `findByArtisanUserId...` và query JOIN với `p.artisan`, đồng thời bổ sung query `reserveIfAvailable()`.
- **Hướng xử lý**: **Sử dụng toàn bộ phiên bản của `dev`** để đồng bộ tên gọi `artisan` cho cả dự án, giữ lại đầy đủ query `reserveIfAvailable()`.

---

### File 4: `application.properties`
- **Đường dẫn**: `src/main/resources/application.properties`
- **Lý do conflict**:
  - `HEAD` chứa thông tin cấu hình Gmail SMTP thật (`username` và `password`).
  - `dev` chỉ chứa khung cấu hình SMTP cơ bản và bổ sung cấu hình Google OAuth2 (`spring.security.oauth2.client.registration.google...`).
- **Hướng xử lý**: Kết hợp cả hai — **giữ thông tin tài khoản Email SMTP của `HEAD`** và **nạp thêm cấu hình Google OAuth2 từ `dev`**.

```properties
# EMAIL CONFIG (HEAD)
spring.mail.host=smtp.gmail.com
spring.mail.port=587
spring.mail.username=haihaugarden@gmail.com
spring.mail.password=txtldgthtpzrvost

# Google OAuth2 (dev)
spring.security.oauth2.client.registration.google.scope=email,profile
spring.security.oauth2.client.registration.google.redirect-uri=http://localhost:8080/login/oauth2/code/google
```

---

### File 5: `MarketplaceController.java`
- **Đường dẫn**: `src/main/java/com/example/bonsai_shop/product/controller/MarketplaceController.java`
- **Lý do conflict**:
  - `HEAD` trả về `return "/product/marketplace";` (có dấu `/` ở đầu).
  - `dev` trả về `return "product/marketplace";` (không có dấu `/`).
- **Hướng xử lý**: Giữ cú pháp chuẩn của Thymeleaf trong Spring Boot là `return "product/marketplace";`.

---

### File 6: `OrderApiController.java`
- **Đường dẫn**: `src/main/java/com/example/bonsai_shop/product/controller/OrderApiController.java`
- **Lý do conflict & Lỗi báo**:
  - `HEAD` thực hiện kiểm tra `AVAILABLE` trạng thái sản phẩm trong giỏ và khai báo `User customer = userRepository.findByEmail(...)`.
  - `dev` bổ sung thêm kiểm tra không cho tài khoản Staff/Admin đặt hàng (`ROLE_OWNER`, `ROLE_ARTISAN`, `ROLE_MODERATOR`, ...) và lặp lại việc khai báo `User customer = null;`.
  - **Sự cố báo lỗi**: Khai báo biến trùng tên `customer` 2 lần trong cùng phạm vi (scope) của hàm `checkout()`.
- **Hướng xử lý & Sửa lỗi**: 
  1. Loại bỏ khai báo trùng lặp `User customer`. Đưa đoạn check phân quyền Staff/Admin (`isStaffOrAdmin`) lên trên cùng ngay sau khi check `currentUser == null`.
  2. Chỉ thực hiện khai báo duy nhất `User customer = userRepository.findByEmail(...)`.
  3. Kết hợp kiểm tra trạng thái sản phẩm `AVAILABLE` trong giỏ hàng và dùng `reserveIfAvailable()` atomic update khi tạo `OrderDetail`.

---

### File 7: `product-detail.html`
- **Đường dẫn**: `src/main/resources/templates/product/product-detail.html`
- **Lý do conflict**:
  - `HEAD` gọi hàm JS `addToCartAndGo(...)` cho nút Mua ngay và `addToCart(...)` cho nút Add to Cart.
  - `dev` bọc các nút trong thẻ `sec:authorize="!hasAnyRole('OWNER', 'ARTISAN', 'MODERATOR', 'CONTENT_MODERATOR')"` để ẩn nút mua với tài khoản quản trị.
- **Hướng xử lý**: Đặt các nút mua với `th:onclick="addToCartAndGo(...)"` và `th:onclick="addToCart(...)"` vào bên trong khối phân quyền `sec:authorize` của `dev`.

---

### File 8: `marketplace.html`
- **Đường dẫn**: `src/main/resources/templates/product/marketplace.html`
- **Lý do conflict**:
  - Tương tự `product-detail.html`, `HEAD` chứa `th:onclick="addToCart(...)"` và `dev` chứa thẻ phân quyền `sec:authorize`.
- **Hướng xử lý**: Kết hợp thẻ `<button>` chứa cả `sec:authorize="!hasAnyRole(...)"` của `dev` lẫn `th:onclick="addToCart(...)"` của `HEAD`.

```html
<button sec:authorize="!hasAnyRole('OWNER', 'ARTISAN', 'MODERATOR', 'CONTENT_MODERATOR')" 
        th:if="${product.productStatus == 'AVAILABLE'}" 
        class="btn-card-cart-outline" 
        th:onclick="'addToCart(' + ${product.productId} + ')'">
    <i class="fa-solid fa-cart-shopping"></i> Add to Cart
</button>
```

---

## 4. QUY TRÌNH THỰC HIỆN KHI MERGE TRONG TƯƠNG LAI

Để tránh tích tụ xung đột lớn khi làm việc nhóm, thực hiện theo 4 bước sau:

1. **Bước 1: Lưu giữ code local & Fetch nhánh mới nhất**
   ```bash
   git status
   git fetch origin
   ```
2. **Bước 2: Merge nhánh `origin/dev` vào nhánh làm việc**
   ```bash
   git merge origin/dev
   ```
3. **Bước 3: Giải quyết conflict theo từng file**
   - Mở file bị báo `CONFLICT`, tìm các thẻ `<<<<<<< HEAD`, `=======`, `>>>>>>> origin/dev`.
   - Giữ lại phần logic chính xác nhất từ cả hai bên.
   - Chạy thử lệnh biên dịch để đảm bảo không bị lỗi syntax:
     ```bash
     .\mvnw.cmd compile -DskipTests
     ```
4. **Bước 4: Stage và Commit hoàn tất Merge**
   ```bash
   git add src/
   git commit -m "merge: merge dev into DuongNKT - resolve conflicts"
   git push origin DuongNKT
   ```

---

## 5. XỬ LÝ SỰ CỐ "APPLICATION FAILED TO START" KHI KHỞI ĐỘNG SAU MERGE

Khi chạy lệnh khởi động server `.\mvnw.cmd spring-boot:run`, hệ thống gặp **3 sự cố liên tiếp** khiến ứng dụng báo `APPLICATION FAILED TO START`. Dưới đây là chi tiết từng sự cố, nguyên nhân và cách khắc phục:

---

### 5.1. Sự cố 1: Thiếu cấu hình MySQL DataSource (`DataSourceBeanCreationException`)

#### Log lỗi ở Terminal:
```text
Description: Failed to configure a DataSource: 'url' attribute is not specified and no embedded datasource could be configured.
Reason: Failed to determine a suitable driver class
```

#### Nguyên nhân:
- Nhánh `dev` kích hoạt `spring.profiles.active=local` và chuyển cấu hình DB sang file `application-local.properties` (file này nằm trong `.gitignore` nên không có sẵn khi merge về máy local).

#### Cách xử lý:
- Bổ sung cấu hình `spring.datasource.url`, `username`, `password` và `driver-class-name` vào [application.properties](file:///d:/project/Bonsai_Shop/src/main/resources/application.properties) và [application-local.properties](file:///d:/project/Bonsai_Shop/src/main/resources/application-local.properties).

---

### 5.2. Sự cố 2: Thiếu Client-ID cho Google OAuth2 (`IllegalStateException`)

#### Log lỗi ở Terminal:
```text
Caused by: java.lang.IllegalStateException: Client id of registration 'google' must not be empty.
    at org.springframework.boot.security.oauth2.client.autoconfigure.OAuth2ClientProperties.validateRegistration...
```

#### Nguyên nhân:
- Nhánh `dev` bổ sung khai báo đăng nhập Google OAuth2 trong `application.properties` nhưng bỏ trống thuộc tính `client-id` và `client-secret`.
- Spring Security OAuth2 Auto-Configuration kiểm tra thuộc tính này lúc khởi động bean, nếu thấy rỗng sẽ chặn ứng dụng khởi động.

#### Cách xử lý:
- Thêm giá trị dự phòng (fallback dummy value) trong `application.properties`:
  ```properties
  spring.security.oauth2.client.registration.google.client-id=${GOOGLE_CLIENT_ID:dummy-google-client-id.apps.googleusercontent.com}
  spring.security.oauth2.client.registration.google.client-secret=${GOOGLE_CLIENT_SECRET:dummy-google-client-secret}
  ```

---

### 5.3. Sự cố 3: Trùng lặp phiên bản Flyway Migration (`FlywayException`)

#### Log lỗi ở Terminal:
```text
>>> FLYWAY MIGRATION FAILED: Found more than one migration with version 2
Offenders:
-> D:\project\Bonsai_Shop\target\classes\db\migration\V2__add_community_post_bookmark.sql (SQL)
-> D:\project\Bonsai_Shop\target\classes\db\migration\V2__add_order_assignment_fields.sql (SQL)
org.flywaydb.core.api.FlywayException: Found more than one migration with version 2, 3, 4, 5
```

#### Nguyên nhân:
- Cả hai nhánh `dev` và `DuongNKT` đều tự tạo các file SQL migration được đánh số thứ tự từ `V2__` đến `V5__` độc lập với nhau.
- Khi merge về chung một thư mục `src/main/resources/db/migration`, Flyway phát hiện có tới 2 file cùng mang version `V2__`, `V3__`, `V4__`, `V5__` dẫn tới xung đột đánh số thứ tự.

#### Cách xử lý:
1. Xóa các file migration giỏ hàng trung gian không còn dùng: `V3__create_cart_table_and_update_order_detail.sql`, `V4__drop_and_recreate_cart_item.sql`.
2. Đổi tên lại chuỗi migration thành một dãy liên tục duy nhất từ V1 đến V7:
   - `V1__init_schema.sql`
   - `V2__add_community_post_bookmark.sql` (từ dev)
   - `V3__add_order_type_to_order.sql` (từ dev)
   - `V4__add_moderation_reason_to_community_comment.sql` (từ dev)
   - `V5__add_status_to_community_comment.sql` (từ dev)
   - `V6__add_order_assignment_fields.sql` (đổi tên từ V2 của DuongNKT)
   - `V7__restructure_cart_and_cart_item.sql` (đổi tên từ V5 của DuongNKT)
3. Chạy `.\mvnw.cmd clean compile` để làm sạch bộ nhớ tạm `target/classes`.

---

### 5.4. Kết quả kiểm tra khởi động:
Sau khi xử lý xong 3 sự cố trên, chạy lại lệnh `.\mvnw.cmd spring-boot:run`:
- Server khởi động hoàn tất: **`Started BonsaiShopApplication in 6.325 seconds`**
- Trạng thái Spring Boot: **`Application availability state ReadinessState changed to ACCEPTING_TRAFFIC`**
- Cổng kết nối HTTP: **Port 8080 hoạt động bình thường**.

---

## 6. CHI TIẾT CÁC THAY ĐỔI CƠ SỞ DỮ LIỆU (DATABASE & FLYWAY MIGRATION)

### 6.1. Chi tiết các Bảng và Cột đã được thêm/chỉnh sửa

| Tên Bảng | Tên Cột Mới / Thay Đổi | Kiểu Dữ Liệu | Nguyên Nhân & Lý Do Thay Đổi |
| :--- | :--- | :--- | :--- |
| **`ORDER`** | `OrderType` | `VARCHAR(50) NOT NULL DEFAULT 'ONLINE'` | Phân loại nguồn đơn hàng trực tuyến (`ONLINE`) hay tại vườn (`OFFLINE`). Nhánh `dev` yêu cầu thuộc tính này trong JPA Entity `Order.java`. |
| **`ORDER`** | `assigned_to` | `INT NULL` | Khóa ngoại liên kết tới `user(UserID)` lưu thông tin Kiểm duyệt viên (Moderator) chịu trách nhiệm duyệt/xử lý đơn. |
| **`ORDER`** | `assigned_at` | `DATETIME NULL` | Lưu mốc thời gian Moderator bấm nhận đơn hàng. |
| **`ORDER`** | `version` | `INT NOT NULL DEFAULT 0` | Khóa lạc quan (Optimistic Locking - `@Version`) đảm bảo tính toàn vẹn dữ liệu, chống tình trạng nhiều Moderator cùng tranh chấp nhận 1 đơn hàng (Race condition). |
| **`ORDER_DETAIL`** | `quantity` | `INT NOT NULL DEFAULT 1` | Bổ sung cột số lượng mua cho từng dòng chi tiết đơn hàng, tương thích với DTO/Entity `OrderDetail.java`. |
| **`CART`** | *Bảng mới* | `CartID`, `CustomerID` | Tái cấu trúc chuẩn giỏ hàng doanh nghiệp: 1 User sở hữu 1 Cart duy nhất (`OneToOne`). |
| **`CART_ITEM`** | *Xóa cột `quantity`* | `CartItemID`, `CartID`, `ProductID` | Gỡ bỏ cột `quantity` khỏi bảng giỏ hàng. Lý do: Cây cảnh Bonsai là sản phẩm độc bản (mỗi `ProductID` chỉ có 1 cây duy nhất ngoài thực tế), không có khái niệm mua nhiều hơn 1 cây cùng mã. |

---

### 6.2. Sự cố Cú Pháp SQL trên MySQL 8.0 & Giải Pháp Stored Procedure

#### 1. Nguyên nhân gây sự cố cú pháp:
- **Lỗi 1060 (`Duplicate column name 'assigned_to'`)**: Khi chạy Flyway trên máy local đã có sẵn một phần cột từ các lần chạy trước, câu lệnh `ALTER TABLE ADD COLUMN` của Flyway bị MySQL ném lỗi dừng toàn bộ ứng dụng.
- **Lỗi 1064 (`SQL syntax error near 'IF NOT EXISTS'`)**: Cú pháp `ALTER TABLE ADD COLUMN IF NOT EXISTS` không được hỗ trợ chuẩn trên một số phiên bản MySQL 8.0 Engine.

#### 2. Giải pháp dùng Stored Procedure Idempotent:
Tất cả các file migration ([V3](file:///d:/project/Bonsai_Shop/src/main/resources/db/migration/V3__add_order_type_to_order.sql), [V6](file:///d:/project/Bonsai_Shop/src/main/resources/db/migration/V6__add_order_assignment_fields.sql), [V7](file:///d:/project/Bonsai_Shop/src/main/resources/db/migration/V7__restructure_cart_and_cart_item.sql)) đã được viết lại dưới dạng Stored Procedure tự hủy:

```sql
DROP PROCEDURE IF EXISTS add_order_assignment_cols;

DELIMITER //
CREATE PROCEDURE add_order_assignment_cols()
BEGIN
    -- Kiểm tra trực tiếp bảng hệ thống information_schema.columns
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_schema = DATABASE() AND table_name = 'order' AND column_name = 'assigned_to'
    ) THEN
        ALTER TABLE `order` ADD COLUMN `assigned_to` INT NULL;
    END IF;
    ...
END //
DELIMITER ;

CALL add_order_assignment_cols();
DROP PROCEDURE IF EXISTS add_order_assignment_cols;
```

- **Lý do áp dụng**: Đảm bảo file SQL Migration có tính **Idempotent** (chạy an toàn tuyệt đối 100% dù cơ sở dữ liệu đang ở bất kỳ trạng thái nào — mới tạo hoàn toàn hay đã có sẵn một phần cột), không bao giờ bị dừng tiến trình khởi động server.

---

### 6.3. Thay đổi Cấu hình Hibernate `spring.jpa.hibernate.ddl-auto`

- **Thay đổi**: Chuyển từ `spring.jpa.hibernate.ddl-auto=none` sang `spring.jpa.hibernate.ddl-auto=update` trong [application.properties](file:///d:/project/Bonsai_Shop/src/main/resources/application.properties#L19).
- **Nguyên nhân & Lý do**:
  - Khi cấu hình `=none`, nếu CSDL local bị thiếu cột (do Flyway chưa chạy kịp hoặc bị dừng), Hibernate vẫn im lặng gọi câu lệnh SQL HQL/JPQL dẫn tới MySQL ném lỗi `Unknown column 'o1_0.OrderType' in 'field list'` (Lỗi 500 khi load danh sách đơn hàng hoặc tạo đơn hàng).
  - Khi đổi sang `=update`, Hibernate sẽ chủ động đối chiếu tất cả thuộc tính trong JPA Entity (`Order`, `OrderDetail`, `User`, `Product`) với các bảng trong MySQL và tự động bổ sung ngay lập tức bất kỳ cột nào còn thiếu khi server vừa khởi chạy.

