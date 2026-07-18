# Hướng dẫn Sửa lỗi Hiển thị Cây không Công khai Giá (Showroom) trên Marketplace

Tài liệu này giải thích luồng hoạt động của chức năng tải danh sách sản phẩm trên Marketplace và hướng dẫn từng bước cách khắc phục lỗi hiển thị các sản phẩm không công khai giá (những cây lẽ ra thuộc showroom VIP).

---

## 1. Luồng Tải Danh sách Sản phẩm trên Marketplace trong Code

Quá trình tải danh sách sản phẩm diễn ra theo trình tự sau:

1. **Yêu cầu HTTP (HTTP Request):**
   Khi người dùng truy cập đường dẫn `/marketplace` hoặc thay đổi các bộ lọc, yêu cầu được gửi tới `MarketplaceController` và xử lý tại phương thức `marketplace(...)` trong [MarketplaceController.java](file:///d:/project/Bonsai_Shop/src/main/java/com/example/bonsai_shop/product/controller/MarketplaceController.java).

2. **Trích xuất Tham số (Extract Parameters):**
   Controller nhận các tham số tìm kiếm, phân trang và sắp xếp: `keyword`, `status`, `availableOnly`, `segment`, `category`, `minPrice`, `maxPrice`, `ages`, `species`, `styles`, `priceRanges`, `page`, `sort`.

3. **Gọi Tầng Service (Call Service Layer):**
   Controller chuyển tiếp các tham số này đến phương thức `getFilteredProducts(...)` thuộc lớp [ProductService.java](file:///d:/project/Bonsai_Shop/src/main/java/com/example/bonsai_shop/product/service/ProductService.java).

4. **Xây dựng Câu truy vấn Động (Build Dynamic Query - JPA Specification):**
   `ProductService` sử dụng phương thức static `ProductSpecifications.filterProducts(...)` từ lớp [ProductSpecifications.java](file:///d:/project/Bonsai_Shop/src/main/java/com/example/bonsai_shop/product/repository/ProductSpecifications.java) để xây dựng một đối tượng `Specification<Product>` chứa các điều kiện (Predicate) lọc dữ liệu.

5. **Truy vấn Cơ sở Dữ liệu (Execute Database Query):**
   `ProductService` gọi `productRepository.findAll(specification, pageable)` để tìm kiếm sản phẩm phân trang trong cơ sở dữ liệu. Kết quả trả về là một đối tượng `Page<Product>`.

6. **Render Giao diện (Render Template):**
   Controller đưa danh sách sản phẩm vào Model (`model.addAttribute("products", products)`) và trả về Thymeleaf template [marketplace.html](file:///d:/project/Bonsai_Shop/src/main/resources/templates/product/marketplace.html) để hiển thị lên trình duyệt.

---

## 2. Nguyên nhân Xuất hiện các Cây không được Định giá (`isPublicPrice = false`)

Trong thiết kế hệ thống:
- Các cây có thuộc tính `isPublicPrice = false` là các cây thuộc diện trưng bày Showroom VIP, không có giá niêm yết công khai (thường hiển thị chữ **"Liên hệ"** thay vì nút "Add to Cart" mua trực tuyến). Những cây này thuộc về Showroom VIP (`/bonsai-luxury`).
- Các cây trên Marketplace bắt buộc phải được công khai giá bán (`isPublicPrice = true`).

**Tuy nhiên, trong mã nguồn hiện tại:**
Tại tệp [ProductSpecifications.java](file:///d:/project/Bonsai_Shop/src/main/java/com/example/bonsai_shop/product/repository/ProductSpecifications.java), không có điều kiện mặc định nào lọc bỏ các sản phẩm có `isPublicPrice = false`. Điều kiện này chỉ được thêm vào một cách **bị động** khi người dùng điền giá trị `minPrice` hoặc `maxPrice`:

```java
// Chỉ kích hoạt lọc isPublicPrice khi người dùng chọn lọc khoảng giá
if (minPrice != null) {
    predicates.add(cb.equal(root.get("isPublicPrice"), true));
    predicates.add(cb.ge(root.get("price"), minPrice));
}
if (maxPrice != null) {
    predicates.add(cb.equal(root.get("isPublicPrice"), true));
    predicates.add(cb.le(root.get("price"), maxPrice));
}
```

Nếu người dùng truy cập Marketplace bình thường mà không nhập khoảng giá lọc (trường hợp mặc định), hệ thống sẽ lấy tất cả các sản phẩm hoạt động, bao gồm cả những cây `isPublicPrice = false` (Showroom). Vì thế, chúng vẫn hiển thị trên lưới sản phẩm của Marketplace kèm nhãn "Liên hệ".

---

## 3. Các Bước Sửa đổi Chi tiết

Để sửa lỗi này, chúng ta cần cấu hình điều kiện `isPublicPrice = true` làm điều kiện bắt buộc (Global Filter) cho Marketplace.

### Bước 1: Sửa đổi tệp `ProductSpecifications.java`
Mở tệp [ProductSpecifications.java](file:///d:/project/Bonsai_Shop/src/main/java/com/example/bonsai_shop/product/repository/ProductSpecifications.java) và thực hiện các chỉnh sửa sau:

1. Thêm điều kiện bắt buộc `isPublicPrice = true` ngay sau khi kiểm tra trạng thái hiển thị của sản phẩm (khoảng dòng 35).
2. Xóa các dòng kiểm tra `isPublicPrice = true` trùng lặp trong khối `minPrice` và `maxPrice` để tối ưu hóa câu truy vấn.

**Đoạn code cần sửa đổi:**
```java
// 1. Thêm bộ lọc bắt buộc cho toàn bộ Marketplace:
predicates.add(cb.not(root.get("productStatus").in("DRAFT", "HIDDEN")));

// Bổ sung dòng dưới đây:
predicates.add(cb.equal(root.get("isPublicPrice"), true));
```

```java
// 2. Tối giản các điều kiện minPrice / maxPrice:
if (minPrice != null) {
    // Xóa dòng này: predicates.add(cb.equal(root.get("isPublicPrice"), true));
    predicates.add(cb.ge(root.get("price"), minPrice));
}

if (maxPrice != null) {
    // Xóa dòng này: predicates.add(cb.equal(root.get("isPublicPrice"), true));
    predicates.add(cb.le(root.get("price"), maxPrice));
}
```

---

### Bước 2: Sửa đổi tệp `ProductRepository.java` (Khuyên dùng để đồng bộ hóa)
Mở tệp [ProductRepository.java](file:///d:/project/Bonsai_Shop/src/main/java/com/example/bonsai_shop/product/repository/ProductRepository.java) và tìm đến phương thức `findMarketplaceProducts` để bổ sung điều kiện `p.isPublicPrice = true`:

```java
    @Query("""
        SELECT new com.example.bonsai_shop.product.dto.ProductCardDTO(
            ...
        )
        FROM Product p
        JOIN p.variety v
        JOIN p.seller u
        LEFT JOIN p.productMedias m
        WHERE p.productStatus = 'AVAILABLE'
          AND p.isPublicPrice = true -- Bổ sung điều kiện này
          AND (m.isThumbnail = true OR m IS NULL)
    """)
    Page<ProductCardDTO> findMarketplaceProducts(Pageable pageable);
```

---

## 4. Cách Xác minh Kết quả (Verification)
Sau khi áp dụng thay đổi và biên dịch lại ứng dụng:
1. Truy cập trang `/marketplace`.
2. Kiểm tra danh sách hiển thị: Tất cả các sản phẩm đều phải hiển thị giá cụ thể (ví dụ: `4.500.000 đ`). Không được phép xuất hiện sản phẩm nào có nhãn giá là **"Liên hệ"** hoặc có nút Add to Cart bị vô hiệu hóa liên quan đến giá.
3. Truy cập trang `/bonsai-luxury` (Showroom VIP): Đảm bảo các sản phẩm cao cấp, không công khai giá (`isPublicPrice = false`) vẫn xuất hiện đầy đủ tại đây.
