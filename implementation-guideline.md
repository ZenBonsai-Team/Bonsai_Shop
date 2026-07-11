# HƯỚNG DẪN TÍCH HỢP HỆ THỐNG XỬ LÝ ĐƠN HÀNG (ORDER MODERATOR) - BẢN CẬP NHẬT SỬA LỖI

Tài liệu này giải thích lý do cần trường `quantity` trong DTO, chỉ ra các nguyên nhân khiến trang của bạn chưa load được dữ liệu và cung cấp mã nguồn chuẩn xác để sửa đổi.

---

## ❓ 1. TẠI SAO CẦN CÓ `quantity` TRONG `convertToDTO`?
Trên giao diện Frontend:
* **Hiển thị thông tin**: Trong Drawer chi tiết đơn hàng ([order-drawer.js](file:///d:/project/Bonsai_Shop/src/main/resources/public/js/components/order-drawer.js)), có đoạn hiển thị số lượng đặt mua:
  ```javascript
  this.elQty.textContent = order.quantity;
  ```
* **Tính toán tài chính**: Giao diện tính toán lại giá trị gốc của Bonsai nhân với số lượng:
  ```javascript
  this.elBasePrice.textContent = this.formatVND(order.product.price * order.quantity);
  ```
* **Hậu quả nếu thiếu**: Nếu DTO không trả về `quantity`, giá trị của nó ở JS sẽ là `undefined`. Điều này khiến ô hiển thị số lượng bị trống, và phép tính nhân tiền sẽ cho ra kết quả `NaN` (Not a Number), làm lỗi giao diện tài chính. Vì Bonsai là sản phẩm độc nhất, mỗi đơn hàng chỉ chứa 1 sản phẩm, ta gán cứng `.quantity(1)` trong DTO để đảm bảo JS chạy đúng.

---

## 🔍 2. CÁC NGUYÊN NHÂN TRANG ORDERS CHƯA LOAD ĐƯỢC NỘI DUNG

### Lỗi 1: Sai tên Key trả về từ API
Trong `OrderApiController.java`, phương thức `getOrders` trả về key `"object"`:
```java
response.put("object", dtoList); // <-- SAI
```
Tuy nhiên, Frontend ([moderator_orders.js](file:///d:/project/Bonsai_Shop/src/main/resources/public/js/moderator_orders.js)) và [orders.js](file:///d:/project/Bonsai_Shop/src/main/resources/public/js/mock-data/orders.js) lại đang chờ nhận danh sách từ key `"orders"`:
```javascript
renderTable(result.orders); // result.orders sẽ bị undefined dẫn đến lỗi
```
* **Cách sửa**: Đổi `"object"` thành `"orders"`. Đồng thời bổ sung trả về `"currentPage"` để phân trang hoạt động ổn định.

### Lỗi 2: Trùng biến và chưa định nghĩa biến ở Frontend (`updateKPIs`)
Trong file [moderator_orders.js](file:///d:/project/Bonsai_Shop/src/main/resources/public/js/moderator_orders.js) bạn vừa sửa:
```javascript
const date = await response.json(); // Nhận dữ liệu vào biến 'date'

document.getElementById('kpiTotalCount').textContent = total; // Lỗi: Biến 'total' chưa được định nghĩa
```
Các biến `total`, `pending`, `approved`, `paid`, `rejected` chưa được khai báo ở đâu, dẫn đến lỗi Javascript `ReferenceError` làm đứng toàn bộ trang web.
* **Cách sửa**: Lấy dữ liệu trực tiếp từ thuộc tính của JSON trả về (ví dụ: `data.total`).

### Lỗi 3: Thiếu các API Endpoints thiết yếu ở Backend
Bạn mới chỉ tạo API `GET /api/orders` mà chưa tạo các API:
* `GET /api/orders/kpis` (Tải số lượng KPI hiển thị ở header)
* `GET /api/orders/{orderCode}` (Tải chi tiết khi click dòng)
* `POST /api/orders/{orderCode}/verify` (Phê duyệt đơn hàng)
* `POST /api/orders/{orderCode}/reject` (Từ chối đơn hàng)
Khi JS gọi các API này sẽ bị lỗi `404 Not Found`.

### Lỗi 4: Nguy cơ lỗi NullPointerException và thiếu địa chỉ ở `convertToDTO`
Bạn đang lấy thông tin khách hàng từ thực thể liên kết `order.getCustomer().getFullName()`. 
* Nếu đơn hàng được đặt bởi khách vãng lai (không đăng nhập), `order.getCustomer()` sẽ là `null` gây crash chương trình (`NullPointerException`).
* Thiết lập `CustomerDTO` bị thiếu trường `address` (địa chỉ giao hàng), khiến Drawer chi tiết hiển thị địa chỉ của khách hàng bị trống/undefined.
* **Cách sửa**: Lấy trực tiếp từ các trường phẳng có sẵn trên Order (`customerName`, `customerEmail`, `customerPhone`, `shippingAddress`).

---

## 🛠️ 3. MÃ NGUỒN CHUẨN XÁC ĐỂ BẠN THỰC HIỆN SỬA ĐỔI

### A. Cập nhật Backend [OrderApiController.java](file:///d:/project/Bonsai_Shop/src/main/java/com/example/bonsai_shop/product/controller/OrderApiController.java)

Hãy thay thế phương thức `getOrders`, `convertToDTO` và bổ sung các API bị thiếu vào cuối class `OrderApiController`:

```java
    /**
     * API Lấy danh sách đơn hàng có phân trang, tìm kiếm và lọc trạng thái
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> getOrders(
            @RequestParam(defaultValue = "") String search,
            @RequestParam(defaultValue = "ALL") String status,
            @RequestParam(defaultValue = "date_desc") String sort,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "5") int limit) {

        Page<Order> orderPage = orderService.getFilteredOrders(search, status, sort, page, limit);

        List<OrderResponseDTO> dtoList = orderPage.getContent().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());

        Map<String, Object> response = new HashMap<>();
        response.put("orders", dtoList); // Sửa từ "object" thành "orders"
        response.put("totalCount", orderPage.getTotalElements());
        response.put("pages", orderPage.getTotalPages());
        response.put("currentPage", page); // Bổ sung currentPage cho frontend hiển thị

        return ResponseEntity.ok(response);
    }

    /**
     * API Lấy thống kê số lượng đơn hàng theo các trạng thái (KPIs)
     */
    @GetMapping("/kpis")
    public ResponseEntity<Map<String, Long>> getKPIs() {
        return ResponseEntity.ok(orderService.getKPIs());
    }

    /**
     * API Lấy chi tiết một đơn hàng theo mã đơn
     */
    @GetMapping("/{orderCode}")
    public ResponseEntity<OrderResponseDTO> getOrderDetail(@PathVariable String orderCode) {
        Order order = orderService.getOrderByCode(orderCode);
        if (order == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(convertToDTO(order));
    }

    /**
     * API Duyệt đơn hàng (Cập nhật phí cẩu, phí ship)
     */
    @PostMapping("/{orderCode}/verify")
    public ResponseEntity<Map<String, Object>> verifyOrder(
            @PathVariable String orderCode,
            @RequestBody Map<String, Object> payload,
            @AuthenticationPrincipal UserDetails currentUser) {

        Map<String, Object> response = new HashMap<>();
        if (currentUser == null) {
            response.put("success", false);
            response.put("message", "Chưa đăng nhập hệ thống.");
            return ResponseEntity.status(401).body(response);
        }

        User moderator = userRepository.findByEmail(currentUser.getUsername()).orElse(null);
        if (moderator == null) {
            response.put("success", false);
            response.put("message", "Người dùng không hợp lệ.");
            return ResponseEntity.badRequest().body(response);
        }

        BigDecimal craneFee = new BigDecimal(payload.getOrDefault("craneFee", 0).toString());
        BigDecimal shippingFee = new BigDecimal(payload.getOrDefault("shippingFee", 0).toString());

        boolean success = orderService.verifyOrder(orderCode, craneFee, shippingFee, moderator);
        response.put("success", success);
        response.put("message", success ? "Duyệt đơn hàng thành công." : "Duyệt đơn hàng thất bại.");

        return ResponseEntity.ok(response);
    }

    /**
     * API Từ chối duyệt đơn hàng (Có lý do)
     */
    @PostMapping("/{orderCode}/reject")
    public ResponseEntity<Map<String, Object>> rejectOrder(
            @PathVariable String orderCode,
            @RequestBody Map<String, String> payload,
            @AuthenticationPrincipal UserDetails currentUser) {

        Map<String, Object> response = new HashMap<>();
        if (currentUser == null) {
            response.put("success", false);
            response.put("message", "Chưa đăng nhập hệ thống.");
            return ResponseEntity.status(401).body(response);
        }

        User moderator = userRepository.findByEmail(currentUser.getUsername()).orElse(null);
        if (moderator == null) {
            response.put("success", false);
            response.put("message", "Người dùng không hợp lệ.");
            return ResponseEntity.badRequest().body(response);
        }

        String reason = payload.getOrDefault("reason", "");
        boolean success = orderService.rejectOrder(orderCode, reason, moderator);
        response.put("success", success);
        response.put("message", success ? "Từ chối duyệt đơn hàng thành công." : "Thao tác thất bại.");

        return ResponseEntity.ok(response);
    }

    /**
     * Helper chuyển đổi dữ liệu an toàn tránh lỗi NullPointer và đệ quy
     */
    private OrderResponseDTO convertToDTO(Order order) {
        OrderResponseDTO.ProductDTO prodcutDTO = null;
        if (order.getOrderDetails() != null && !order.getOrderDetails().isEmpty()) {
            OrderDetail detail = order.getOrderDetails().get(0);
            Product prod = detail.getProduct();
            if (prod != null) {
                prodcutDTO = OrderResponseDTO.ProductDTO.builder()
                        .id(prod.getProductId())
                        .name(prod.getProductName())
                        .image(prod.getFirstImageUrl())
                        .price(prod.getPrice())
                        .build();
            }
        }

        // Lấy thông tin từ các thuộc tính trực tiếp của Order tránh NullPointerException
        return OrderResponseDTO.builder()
                .orderId(order.getOrderId())
                .orderCode(order.getOrderCode())
                .customer(OrderResponseDTO.CustomerDTO.builder()
                        .name(order.getCustomerName())
                        .email(order.getCustomerEmail())
                        .phone(order.getCustomerPhone())
                        .address(order.getShippingAddress()) // Bổ sung address cho Drawer
                        .build())
                .product(prodcutDTO)
                .quantity(1) // Đảm bảo luôn có số lượng để không bị NaN giá trị
                .totalAmount(order.getTotalAmount())
                .depositAmount(order.getDepositAmount())
                .orderDate(order.getOrderDate())
                .orderStatus(order.getOrderStatus())
                .craneFee(order.getCraneFee())
                .shippingFee(order.getShippingFee())
                .notes(order.getNotes())
                .build();
    }
```

---

### B. Cập nhật Frontend [moderator_orders.js](file:///d:/project/Bonsai_Shop/src/main/resources/public/js/moderator_orders.js)

Sửa lại hàm `updateKPIs()` thành dạng chuẩn để lấy dữ liệu từ kết quả HTTP Response JSON:

```javascript
async function updateKPIs() {
    try {
        const response = await fetch('/api/orders/kpis');
        if(!response.ok){
            throw new Error('Không thể lấy dữ liệu KPIs');
        }

        const data = await response.json(); // Sửa 'date' thành 'data'
    
        // Gán giá trị lấy được từ đối tượng 'data' trả về
        document.getElementById('kpiTotalCount').textContent = data.total || 0;
        document.getElementById('kpiPendingCount').textContent = data.pending || 0;
        document.getElementById('kpiApprovedCount').textContent = data.approved || 0;
        document.getElementById('kpiPaidCount').textContent = data.paid || 0;
        document.getElementById('kpiRejectedCount').textContent = data.rejected || 0;
    } catch (error) {
        console.error("Không thể cập nhật số liệu KPIs: ", error);
    }
}
```
