# HƯỚNG DẪN TRIỂN KHAI CHI TIẾT CƠ CHẾ PHÂN BỔ ĐƠN HÀNG (ORDER ASSIGNMENT IMPLEMENTATION GUIDE)

**Tác giả:** Senior Full-stack Software Architect, Technical Writer & Business Analyst  
**Mục tiêu:** Hướng dẫn chi tiết từng bước kèm theo **toàn bộ mã nguồn (Source Code)** để bạn tự triển khai refactor module Quản lý đơn hàng (Order Moderator) sang mô hình 2 không gian riêng biệt: **Orders Pool (Kho đơn hàng chung)** và **My Orders (Đơn hàng của tôi)**.

> [!IMPORTANT]
> **TÍNH NĂNG BẢO TỒN VÀ BỔ SUNG MỚI:**
> 1. **Bảo tồn 100% Giao diện & Bộ lọc**: Giữ nguyên ô Tìm kiếm (Search Debounce 300ms) và Dropdown Sắp xếp 4 tiêu chí (`date_desc`, `date_asc`, `price_desc`, `price_asc`). Mặc định luôn là **"Mới nhất xếp trước" (`date_desc`)**.
> 2. **Giữ nguyên 5 KPI Cards, Tabs trạng thái, Drawer chi tiết** tại màn hình *My Orders*.
> 3. **Thêm Timeline Lịch sử bàn giao đơn (Handling Audit Timeline)** trong Drawer chi tiết: Hiển thị ai từng nhận đơn, nhận lúc nào, ngưng quản lý lúc nào, ai tiếp quản.
> 4. **Dashboard Thống kê Năng suất Moderator**: Cập nhật số liệu cá nhân của Moderator đang đăng nhập.

---

## 1. Quy tắc Nghiệp vụ (Business Rules)

1. **Orders Pool (Kho đơn hàng chung - `/moderator/orders/pool`)**:
   - Chứa tất cả đơn hàng `PENDING` **chưa có ai nhận** (`assignedTo IS NULL`).
   - Giữ nguyên thanh Tìm kiếm + Bộ lọc Sắp xếp (Mặc định: **Mới nhất xếp trước**).
   - Chỉ được bấm nút **"Nhận đơn" (Claim)**. Không được Duyệt/Từ chối trực tiếp ở Pool.

2. **My Orders (Đơn hàng của tôi - `/moderator/orders/my`)**:
   - Chỉ chứa các đơn hàng đã được giao cho Moderator hiện tại đăng nhập (`assignedTo == currentModerator`).
   - Độc quyền thao tác: Nhập phí xe cẩu, phí ship, Phê duyệt (Approve), Từ chối (Reject), hoặc **Trả đơn (Unclaim)** về lại Pool.
   - Drawer xem chi tiết bổ sung phân đoạn **Lịch sử bàn giao & xử lý (Timeline)**.
   - Header hiển thị 5 KPI Cards số liệu cá nhân của Moderator.

3. **Cơ chế Khóa & Kiểm toán (Locking & Audit Trail)**:
   - Sử dụng khóa lạc quan `@Version` trong bảng `ORDER` chống đè dữ liệu (Race Condition).
   - Bảng `ORDER_HANDLING` lưu chi tiết thời gian bắt đầu nhận (`handledAt`), thời gian kết thúc (`releasedAt`), và trạng thái phiên (`isActive`).

---

## 2. Chi tiết các Bước Triển Khai & Mã Nguồn

---

### BƯỚC 1: Tạo Script Database Migration

Tạo file mới tại đường dẫn: `src/main/resources/db/migration/V2__add_order_assignment_fields.sql`

```sql
-- Thêm các cột phân bổ và quản lý phiên bản cho bảng ORDER
ALTER TABLE `order` ADD COLUMN `assigned_to` INT NULL;
ALTER TABLE `order` ADD COLUMN `assigned_at` DATETIME NULL;
ALTER TABLE `order` ADD COLUMN `version` INT NOT NULL DEFAULT 0;

-- Tạo khóa ngoại liên kết cột assigned_to với bảng USER
ALTER TABLE `order` 
ADD CONSTRAINT `fk_order_assigned_moderator` 
FOREIGN KEY (`assigned_to`) REFERENCES `user` (`UserID`) 
ON DELETE SET NULL;
```

---

### BƯỚC 2: Cập nhật Entity `Order.java`

Mở file: `src/main/java/com/example/bonsai_shop/entity/Order.java`  
Thêm các trường `assignedTo`, `assignedAt`, `version`, và mối quan hệ `orderHandlings`:

```java
package com.example.bonsai_shop.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "`ORDER`")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "OrderID")
    private Integer orderId;

    @ManyToOne
    @JoinColumn(name = "CustomerID")
    private User customer;

    @Column(name = "OrderCode", nullable = false, unique = true, length = 100)
    private String orderCode;

    @Column(name = "CustomerName", length = 255)
    private String customerName;

    @Column(name = "CustomerPhone", length = 20)
    private String customerPhone;

    @Column(name = "CustomerEmail", length = 255)
    private String customerEmail;

    @Column(name = "ShippingAddress", length = 500)
    private String shippingAddress;

    @Column(name = "OrderDate")
    private LocalDateTime orderDate = LocalDateTime.now();

    @Column(name = "TotalAmount", precision = 15, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "DepositAmount", precision = 15, scale = 2)
    private BigDecimal depositAmount = BigDecimal.ZERO;

    @Column(name = "OrderStatus", length = 50)
    private String orderStatus = "PENDING";

    @Column(name = "CraneFee", precision = 15, scale = 2)
    private BigDecimal craneFee = BigDecimal.ZERO;

    @Column(name = "ShippingFee", precision = 15, scale = 2)
    private BigDecimal shippingFee = BigDecimal.ZERO;

    @Column(name = "Notes", length = 500)
    private String notes;

    // --- CÁC TRƯỜNG MỚI BỔ SUNG ---
    @ManyToOne
    @JoinColumn(name = "assigned_to")
    private User assignedTo;

    @Column(name = "assigned_at")
    private LocalDateTime assignedAt;

    @Version
    @Column(name = "version")
    private Integer version = 0;
    // ------------------------------

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL)
    private List<OrderDetail> orderDetails;

    @OneToOne(mappedBy = "order", cascade = CascadeType.ALL)
    private Payment payment;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL)
    private List<OrderLog> orderLogs;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL)
    private List<OrderHandling> orderHandlings;
}
```

---

### BƯỚC 3: Cập nhật `OrderResponseDTO.java`

Mở file: `src/main/java/com/example/bonsai_shop/product/dto/OrderResponseDTO.java`  
Thêm thông tin phân bổ và class `OrderHandlingDTO` cho Timeline:

```java
package com.example.bonsai_shop.product.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderResponseDTO {
    private Integer orderId;
    private String orderCode;
    private CustomerDTO customer;
    private ProductDTO product;
    private Integer quantity;
    private BigDecimal totalAmount;
    private BigDecimal depositAmount;
    private LocalDateTime orderDate;
    private String orderStatus;
    private BigDecimal craneFee;
    private BigDecimal shippingFee;
    private String notes;

    // --- THÔNG TIN PHÂN BỔ & TIMELINE ---
    private String assignedToUsername;
    private String assignedToFullName;
    private LocalDateTime assignedAt;
    private List<OrderHandlingDTO> handlingHistory;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CustomerDTO {
        private String name;
        private String phone;
        private String email;
        private String address;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProductDTO {
        private Integer id;
        private String name;
        private String image;
        private BigDecimal price;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderHandlingDTO {
        private Integer handlingId;
        private String moderatorUsername;
        private String moderatorFullName;
        private LocalDateTime handledAt;
        private LocalDateTime releasedAt;
        private Boolean isActive;
    }
}
```

---

### BƯỚC 4: Cập nhật Repositories

#### 4.1. `OrderHandlingRepository.java`
Mở file: `src/main/java/com/example/bonsai_shop/product/repository/OrderHandlingRepository.java`

```java
package com.example.bonsai_shop.product.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.example.bonsai_shop.entity.OrderHandling;

@Repository
public interface OrderHandlingRepository extends JpaRepository<OrderHandling, Integer> {
    List<OrderHandling> findByOrderOrderIdOrderByHandledAtDesc(Integer orderId);
}
```

#### 4.2. `OrderRepository.java`
Mở file: `src/main/java/com/example/bonsai_shop/product/repository/OrderRepository.java`

```java
package com.example.bonsai_shop.product.repository;

import com.example.bonsai_shop.entity.Order;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface OrderRepository extends JpaRepository<Order, Integer> {
    Optional<Order> findByOrderCode(String orderCode);

    long countByOrderStatus(String orderStatus);

    // Đếm số đơn thuộc về Moderator cụ thể
    long countByAssignedToUserId(Integer moderatorId);
    long countByAssignedToUserIdAndOrderStatus(Integer moderatorId, String orderStatus);

    @Query("SELECT DISTINCT o FROM Order o " +
            "LEFT JOIN o.orderDetails od " +
            "LEFT JOIN od.product p " +
            "WHERE (:status = 'ALL' OR o.orderStatus = :status) AND " +
            "(:search IS NULL OR :search = '' OR " +
            " LOWER(o.orderCode) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            " LOWER(o.customerName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            " LOWER(p.productName) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<Order> searchOrdersForModerator(
            @Param("status") String status,
            @Param("search") String search,
            Pageable pageable);

    // 1. Orders Pool Query (Chưa có ai nhận & đang PENDING)
    @Query("SELECT DISTINCT o FROM Order o " +
            "LEFT JOIN o.orderDetails od " +
            "LEFT JOIN od.product p " +
            "WHERE o.assignedTo IS NULL AND o.orderStatus = 'PENDING' AND " +
            "(:search IS NULL OR :search = '' OR " +
            " LOWER(o.orderCode) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            " LOWER(o.customerName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            " LOWER(p.productName) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<Order> searchOrdersPool(
            @Param("search") String search,
            Pageable pageable);

    // 2. My Orders Query (Được gán cho Moderator cụ thể)
    @Query("SELECT DISTINCT o FROM Order o " +
            "LEFT JOIN o.orderDetails od " +
            "LEFT JOIN od.product p " +
            "WHERE o.assignedTo.userId = :moderatorId AND " +
            "(:status = 'ALL' OR o.orderStatus = :status) AND " +
            "(:search IS NULL OR :search = '' OR " +
            " LOWER(o.orderCode) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            " LOWER(o.customerName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            " LOWER(p.productName) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<Order> searchMyOrders(
            @Param("moderatorId") Integer moderatorId,
            @Param("status") String status,
            @Param("search") String search,
            Pageable pageable);
}
```

---

### BƯỚC 5: Cập nhật `OrderService.java`

Mở file: `src/main/java/com/example/bonsai_shop/product/service/OrderService.java`

```java
package com.example.bonsai_shop.product.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.bonsai_shop.entity.Order;
import com.example.bonsai_shop.entity.OrderHandling;
import com.example.bonsai_shop.entity.OrderLog;
import com.example.bonsai_shop.entity.Product;
import com.example.bonsai_shop.entity.User;
import com.example.bonsai_shop.product.event.OrderRejectedEvent;
import com.example.bonsai_shop.product.event.OrderVerifiedEvent;
import com.example.bonsai_shop.product.repository.OrderHandlingRepository;
import com.example.bonsai_shop.product.repository.OrderLogRepository;
import com.example.bonsai_shop.product.repository.OrderRepository;
import com.example.bonsai_shop.product.repository.ProductRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final OrderLogRepository orderLogRepository;
    private final OrderHandlingRepository orderHandlingRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional(readOnly = true)
    public Page<Order> getFilteredOrders(String search, String status, String sort, int page, int limit) {
        Sort springSort = resolveSort(sort);
        Pageable pageable = PageRequest.of(page - 1, limit, springSort);
        return orderRepository.searchOrdersForModerator(status, search, pageable);
    }

    @Transactional(readOnly = true)
    public Page<Order> getPoolOrders(String search, String sort, int page, int limit) {
        Sort springSort = resolveSort(sort);
        Pageable pageable = PageRequest.of(page - 1, limit, springSort);
        return orderRepository.searchOrdersPool(search, pageable);
    }

    @Transactional(readOnly = true)
    public Page<Order> getMyOrders(Integer moderatorId, String search, String status, String sort, int page, int limit) {
        Sort springSort = resolveSort(sort);
        Pageable pageable = PageRequest.of(page - 1, limit, springSort);
        return orderRepository.searchMyOrders(moderatorId, status, search, pageable);
    }

    private Sort resolveSort(String sort) {
        if ("date_asc".equals(sort)) {
            return Sort.by(Sort.Direction.ASC, "orderDate");
        } else if ("price_desc".equals(sort)) {
            return Sort.by(Sort.Direction.DESC, "totalAmount");
        } else if ("price_asc".equals(sort)) {
            return Sort.by(Sort.Direction.ASC, "totalAmount");
        } else {
            return Sort.by(Sort.Direction.DESC, "orderDate"); // default: date_desc (Từ mới nhất)
        }
    }

    @Transactional(readOnly = true)
    public Order getOrderByCode(String orderCode) {
        return orderRepository.findByOrderCode(orderCode).orElse(null);
    }

    @Transactional(readOnly = true)
    public Map<String, Long> getKPIs() {
        Map<String, Long> kpis = new HashMap<>();
        kpis.put("total", orderRepository.count());
        kpis.put("pending", orderRepository.countByOrderStatus("PENDING"));
        kpis.put("approved", orderRepository.countByOrderStatus("APPROVED"));
        kpis.put("paid", orderRepository.countByOrderStatus("PAID"));
        kpis.put("cancelled", orderRepository.countByOrderStatus("CANCELLED"));
        kpis.put("rejected", orderRepository.countByOrderStatus("REJECTED"));
        return kpis;
    }

    @Transactional(readOnly = true)
    public Map<String, Long> getModeratorPersonalKPIs(Integer moderatorId) {
        Map<String, Long> kpis = new HashMap<>();
        kpis.put("total", orderRepository.countByAssignedToUserId(moderatorId));
        kpis.put("pending", orderRepository.countByAssignedToUserIdAndOrderStatus(moderatorId, "PENDING"));
        kpis.put("approved", orderRepository.countByAssignedToUserIdAndOrderStatus(moderatorId, "APPROVED"));
        kpis.put("paid", orderRepository.countByAssignedToUserIdAndOrderStatus(moderatorId, "PAID"));
        kpis.put("rejected", orderRepository.countByAssignedToUserIdAndOrderStatus(moderatorId, "REJECTED"));
        return kpis;
    }

    @Transactional(readOnly = true)
    public List<OrderHandling> getOrderHandlingHistory(Integer orderId) {
        return orderHandlingRepository.findByOrderOrderIdOrderByHandledAtDesc(orderId);
    }

    @Transactional
    public boolean claimOrder(String orderCode, User moderator) {
        Order order = orderRepository.findByOrderCode(orderCode)
                .orElseThrow(() -> new RuntimeException("Đơn hàng không tồn tại!"));

        if (order.getAssignedTo() != null) {
            throw new IllegalStateException("Đơn hàng đã được nhận bởi người khác!");
        }
        if (!"PENDING".equals(order.getOrderStatus())) {
            throw new IllegalStateException("Chỉ được phép nhận đơn hàng đang chờ duyệt!");
        }

        order.setAssignedTo(moderator);
        order.setAssignedAt(LocalDateTime.now());
        orderRepository.save(order);

        OrderHandling handling = OrderHandling.builder()
                .order(order)
                .moderator(moderator)
                .handledAt(LocalDateTime.now())
                .isActive(true)
                .build();
        orderHandlingRepository.save(handling);

        return true;
    }

    @Transactional
    public boolean unclaimOrder(String orderCode, User moderator) {
        Order order = orderRepository.findByOrderCode(orderCode)
                .orElseThrow(() -> new RuntimeException("Đơn hàng không tồn tại!"));

        if (order.getAssignedTo() == null || !order.getAssignedTo().getUserId().equals(moderator.getUserId())) {
            throw new IllegalStateException("Bạn không sở hữu quyền xử lý đơn hàng này!");
        }
        if (!"PENDING".equals(order.getOrderStatus())) {
            throw new IllegalStateException("Chỉ được phép trả lại đơn hàng chưa duyệt!");
        }

        order.setAssignedTo(null);
        order.setAssignedAt(null);
        orderRepository.save(order);

        orderHandlingRepository.findAll().stream()
                .filter(h -> h.getOrder().getOrderId().equals(order.getOrderId()) 
                          && h.getModerator().getUserId().equals(moderator.getUserId())
                          && Boolean.TRUE.equals(h.getIsActive()))
                .forEach(h -> {
                    h.setIsActive(false);
                    h.setReleasedAt(LocalDateTime.now());
                    orderHandlingRepository.save(h);
                });

        return true;
    }

    @Transactional
    public boolean verifyOrder(String orderCode, BigDecimal craneFee, BigDecimal shippingFee, User moderator) {
        Order order = orderRepository.findByOrderCode(orderCode).orElse(null);
        if (order == null || !"PENDING".equals(order.getOrderStatus())) {
            return false;
        }

        // Kiểm tra quyền sở hữu đơn hàng
        if (order.getAssignedTo() == null || !order.getAssignedTo().getUserId().equals(moderator.getUserId())) {
            throw new SecurityException("Bạn không có quyền duyệt đơn hàng này!");
        }

        String oldStatus = order.getOrderStatus();
        order.setCraneFee(craneFee);
        order.setShippingFee(shippingFee);
        order.setOrderStatus("APPROVED");

        BigDecimal originalAmount = order.getTotalAmount();
        BigDecimal newTotal = originalAmount.add(craneFee).add(shippingFee);
        order.setTotalAmount(newTotal);
        orderRepository.save(order);

        OrderLog log = OrderLog.builder()
                .order(order)
                .actionBy(moderator)
                .actionType("VERIFY")
                .fromStatus(oldStatus)
                .toStatus("APPROVED")
                .actionAt(LocalDateTime.now())
                .build();
        orderLogRepository.save(log);

        orderHandlingRepository.findAll().stream()
                .filter(h -> h.getOrder().getOrderId().equals(order.getOrderId()) 
                          && h.getModerator().getUserId().equals(moderator.getUserId())
                          && Boolean.TRUE.equals(h.getIsActive()))
                .forEach(h -> {
                    h.setIsActive(false);
                    h.setReleasedAt(LocalDateTime.now());
                    orderHandlingRepository.save(h);
                });

        initializeOrderDetails(order);
        eventPublisher.publishEvent(new OrderVerifiedEvent(order));
        return true;
    }

    @Transactional
    public boolean rejectOrder(String orderCode, String reason, User moderator) {
        Order order = orderRepository.findByOrderCode(orderCode).orElse(null);
        if (order == null || !"PENDING".equals(order.getOrderStatus())) {
            return false;
        }

        // Kiểm tra quyền sở hữu đơn hàng
        if (order.getAssignedTo() == null || !order.getAssignedTo().getUserId().equals(moderator.getUserId())) {
            throw new SecurityException("Bạn không có quyền từ chối duyệt đơn hàng này!");
        }

        String oldStatus = order.getOrderStatus();
        order.setOrderStatus("REJECTED");
        order.setNotes("Từ chối duyệt với lý do: " + reason);
        orderRepository.save(order);

        if (order.getOrderDetails() != null && !order.getOrderDetails().isEmpty()) {
            Product product = order.getOrderDetails().get(0).getProduct();
            if (product != null) {
                product.setProductStatus("AVAILABLE");
                productRepository.save(product);
            }
        }

        OrderLog log = OrderLog.builder()
                .order(order)
                .actionBy(moderator)
                .actionType("REJECT")
                .fromStatus(oldStatus)
                .toStatus("REJECTED")
                .actionAt(LocalDateTime.now())
                .build();
        orderLogRepository.save(log);

        orderHandlingRepository.findAll().stream()
                .filter(h -> h.getOrder().getOrderId().equals(order.getOrderId()) 
                          && h.getModerator().getUserId().equals(moderator.getUserId())
                          && Boolean.TRUE.equals(h.getIsActive()))
                .forEach(h -> {
                    h.setIsActive(false);
                    h.setReleasedAt(LocalDateTime.now());
                    orderHandlingRepository.save(h);
                });

        initializeOrderDetails(order);
        eventPublisher.publishEvent(new OrderRejectedEvent(order, reason));
        return true;
    }

    private void initializeOrderDetails(Order order) {
        if (order.getOrderDetails() != null) {
            order.getOrderDetails().forEach(detail -> {
                if (detail.getProduct() != null) {
                    detail.getProduct().getProductName();
                }
            });
        }
    }
}
```

---

### BƯỚC 6: Cập nhật Controllers

#### 6.1. `OrderApiController.java`
Mở file: `src/main/java/com/example/bonsai_shop/product/controller/OrderApiController.java`  
Thêm các endpoint `/pool`, `/my`, `/my-stats`, `/{orderCode}/claim`, `/{orderCode}/unclaim` và cập nhật `convertToDTO`:

```java
    @GetMapping("/pool")
    public ResponseEntity<Map<String, Object>> getPoolOrders(
            @RequestParam(defaultValue = "") String search,
            @RequestParam(defaultValue = "date_desc") String sort,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "8") int limit) {
        
        Page<Order> orderPage = orderService.getPoolOrders(search, sort, page, limit);
        List<OrderResponseDTO> dtoList = orderPage.getContent().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());

        Map<String, Object> response = new HashMap<>();
        response.put("orders", dtoList);
        response.put("totalCount", orderPage.getTotalElements());
        response.put("pages", orderPage.getTotalPages());
        response.put("currentPage", page);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/my")
    public ResponseEntity<Map<String, Object>> getMyOrders(
            @RequestParam(defaultValue = "") String search,
            @RequestParam(defaultValue = "ALL") String status,
            @RequestParam(defaultValue = "date_desc") String sort,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "8") int limit,
            @AuthenticationPrincipal UserDetails currentUser) {
        
        if (currentUser == null) {
            return ResponseEntity.status(401).build();
        }
        User moderator = userRepository.findByEmail(currentUser.getUsername()).orElse(null);
        if (moderator == null) {
            return ResponseEntity.badRequest().build();
        }

        Page<Order> orderPage = orderService.getMyOrders(moderator.getUserId(), search, status, sort, page, limit);
        List<OrderResponseDTO> dtoList = orderPage.getContent().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());

        Map<String, Object> response = new HashMap<>();
        response.put("orders", dtoList);
        response.put("totalCount", orderPage.getTotalElements());
        response.put("pages", orderPage.getTotalPages());
        response.put("currentPage", page);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/my-stats")
    public ResponseEntity<Map<String, Long>> getMyStats(
            @AuthenticationPrincipal UserDetails currentUser) {
        if (currentUser == null) {
            return ResponseEntity.status(401).build();
        }
        User moderator = userRepository.findByEmail(currentUser.getUsername()).orElse(null);
        if (moderator == null) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(orderService.getModeratorPersonalKPIs(moderator.getUserId()));
    }

    @PostMapping("/{orderCode}/claim")
    public ResponseEntity<Map<String, Object>> claimOrder(
            @PathVariable String orderCode,
            @AuthenticationPrincipal UserDetails currentUser) {
        
        Map<String, Object> response = new HashMap<>();
        if (currentUser == null) {
            response.put("success", false);
            response.put("message", "Chưa đăng nhập.");
            return ResponseEntity.status(401).body(response);
        }
        User moderator = userRepository.findByEmail(currentUser.getUsername()).orElse(null);
        if (moderator == null) {
            response.put("success", false);
            response.put("message", "Người dùng không hợp lệ.");
            return ResponseEntity.badRequest().body(response);
        }

        try {
            boolean success = orderService.claimOrder(orderCode, moderator);
            response.put("success", success);
            response.put("message", "Nhận đơn hàng thành công.");
            return ResponseEntity.ok(response);
        } catch (IllegalStateException e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.status(409).body(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Lỗi máy chủ khi xử lý: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    @PostMapping("/{orderCode}/unclaim")
    public ResponseEntity<Map<String, Object>> unclaimOrder(
            @PathVariable String orderCode,
            @AuthenticationPrincipal UserDetails currentUser) {
        
        Map<String, Object> response = new HashMap<>();
        if (currentUser == null) {
            response.put("success", false);
            response.put("message", "Chưa đăng nhập.");
            return ResponseEntity.status(401).body(response);
        }
        User moderator = userRepository.findByEmail(currentUser.getUsername()).orElse(null);
        if (moderator == null) {
            response.put("success", false);
            response.put("message", "Người dùng không hợp lệ.");
            return ResponseEntity.badRequest().body(response);
        }

        try {
            boolean success = orderService.unclaimOrder(orderCode, moderator);
            response.put("success", success);
            response.put("message", "Đã trả đơn hàng về Pool thành công.");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    private OrderResponseDTO convertToDTO(Order order) {
        OrderResponseDTO.ProductDTO productDTO = null;
        if (order.getOrderDetails() != null && !order.getOrderDetails().isEmpty()) {
            OrderDetail detail = order.getOrderDetails().get(0);
            Product prod = detail.getProduct();
            if (prod != null) {
                productDTO = OrderResponseDTO.ProductDTO.builder()
                        .id(prod.getProductId())
                        .name(prod.getProductName())
                        .image(prod.getFirstImageUrl())
                        .price(prod.getPrice())
                        .build();
            }
        }

        List<OrderResponseDTO.OrderHandlingDTO> handlingHistory = null;
        if (order.getOrderId() != null) {
            handlingHistory = orderService.getOrderHandlingHistory(order.getOrderId()).stream()
                    .map(h -> OrderResponseDTO.OrderHandlingDTO.builder()
                            .handlingId(h.getOrderHandlingId())
                            .moderatorUsername(h.getModerator() != null ? h.getModerator().getUsername() : "N/A")
                            .moderatorFullName(h.getModerator() != null ? h.getModerator().getFullName() : "N/A")
                            .handledAt(h.getHandledAt())
                            .releasedAt(h.getReleasedAt())
                            .isActive(h.getIsActive())
                            .build())
                    .collect(Collectors.toList());
        }

        return OrderResponseDTO.builder()
                .orderId(order.getOrderId())
                .orderCode(order.getOrderCode())
                .customer(OrderResponseDTO.CustomerDTO.builder()
                        .name(order.getCustomerName())
                        .email(order.getCustomerEmail())
                        .phone(order.getCustomerPhone())
                        .address(order.getShippingAddress())
                        .build())
                .product(productDTO)
                .quantity(1)
                .totalAmount(order.getTotalAmount())
                .depositAmount(order.getDepositAmount())
                .orderDate(order.getOrderDate())
                .orderStatus(order.getOrderStatus())
                .craneFee(order.getCraneFee())
                .shippingFee(order.getShippingFee())
                .notes(order.getNotes())
                .assignedToUsername(order.getAssignedTo() != null ? order.getAssignedTo().getUsername() : null)
                .assignedToFullName(order.getAssignedTo() != null ? order.getAssignedTo().getFullName() : null)
                .assignedAt(order.getAssignedAt())
                .handlingHistory(handlingHistory)
                .build();
    }
```

#### 6.2. `ModeratorOrderController.java`
Mở file: `src/main/java/com/example/bonsai_shop/moderator/controller/ModeratorOrderController.java`

```java
package com.example.bonsai_shop.moderator.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/moderator/orders")
public class ModeratorOrderController {

    @GetMapping
    public String viewOrdersDashboardRedirect() {
        return "redirect:/moderator/orders/pool";
    }

    @GetMapping("/pool")
    public String viewOrdersPool(Model model) {
        model.addAttribute("role", "MODERATOR");
        model.addAttribute("activePage", "orders-pool");
        model.addAttribute("activePageLabel", "Orders Pool - Kho Đơn Hàng Chung");
        return "moderator/orders_pool";
    }

    @GetMapping("/my")
    public String viewMyOrders(Model model) {
        model.addAttribute("role", "MODERATOR");
        model.addAttribute("activePage", "my-orders");
        model.addAttribute("activePageLabel", "Đơn hàng của tôi (My Orders)");
        return "moderator/my_orders";
    }
}
```

---

### BƯỚC 7: Cập nhật Sidebar Navigation

Mở file: `src/main/resources/templates/fragments/sidebar.html`  
Cập nhật block menu Moderator:

```html
            <!-- 2. ORDER MODERATOR MENU -->
            <th:block sec:authorize="hasRole('MODERATOR')">
                <a th:href="@{/moderator/orders/pool}" 
                   class="sidebar-nav-item" 
                   th:classappend="${activePage == 'pool' or activePage == 'orders-pool' ? 'active' : ''}">
                    <span class="nav-icon"><i class="fa-solid fa-box"></i></span>
                    <span>Kho Đơn Hàng (Pool)</span>
                </a>
                <a th:href="@{/moderator/orders/my}" 
                   class="sidebar-nav-item" 
                   th:classappend="${activePage == 'my' or activePage == 'my-orders' ? 'active' : ''}">
                    <span class="nav-icon"><i class="fa-solid fa-clipboard-list"></i></span>
                    <span>Đơn Của Tôi (My Orders)</span>
                </a>
            </th:block>
```

---

### BƯỚC 8: Tạo Giao Diện HTML Templates

#### 8.1. `orders_pool.html`
Tạo file mới tại: `src/main/resources/templates/moderator/orders_pool.html`

```html
<!DOCTYPE html>
<html lang="vi" 
      xmlns:th="http://www.thymeleaf.org"
      th:replace="~{fragments/dashboard-layout :: layout(~{::title}, ~{::#moderator-content}, ~{::#page-styles}, ~{::#page-scripts})}"
      th:with="paramActivePage='orders-pool'">
<head>
    <title>Orders Pool - Kho Đơn Hàng Chung</title>
    <th:block id="page-styles">
        <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.2/dist/css/bootstrap.min.css">
        <link rel="stylesheet" href="/css/moderator-orders.css">
        <meta name="_csrf" th:content="${_csrf != null ? _csrf.token : ''}"/>
        <meta name="_csrf_header" th:content="${_csrf != null ? _csrf.headerName : 'X-CSRF-TOKEN'}"/>
    </th:block>
</head>
<body>
    <div id="moderator-content">
        <!-- Sub Navigation Tabs -->
        <div class="mb-4 d-flex gap-2">
            <a href="/moderator/orders/pool" class="btn btn-primary">
                <i class="fa-solid fa-box-open me-1"></i> Kho Đơn Hàng (Pool)
            </a>
            <a href="/moderator/orders/my" class="btn btn-outline-secondary">
                <i class="fa-solid fa-user-check me-1"></i> Đơn Của Tôi (My Orders)
            </a>
        </div>

        <div class="management-card">
            <!-- Toolbar Header: Search & Sort (Default: Từ mới nhất) -->
            <div class="toolbar-header">
                <div class="toolbar-search-row">
                    <div class="search-input-wrapper">
                        <span class="search-icon"><i class="fa-solid fa-magnifying-glass"></i></span>
                        <input type="text" id="orderSearchInput" placeholder="Tìm kiếm đơn chờ duyệt trong Kho chung (Mã đơn, khách hàng, tên cây)...">
                    </div>
                    <div class="toolbar-filters-group">
                        <div class="sort-select-wrapper">
                            <select id="orderSortSelect">
                                <option value="date_desc" selected>Mới nhất xếp trước</option>
                                <option value="date_asc">Cũ nhất xếp trước</option>
                                <option value="price_desc">Giá trị lớn nhất</option>
                                <option value="price_asc">Giá trị nhỏ nhất</option>
                            </select>
                        </div>
                    </div>
                </div>
            </div>

            <!-- Table Container -->
            <div class="table-container">
                <table class="orders-table" id="ordersDataTable">
                    <thead>
                        <tr>
                            <th>Mã Đơn Hàng</th>
                            <th>Khách Hàng</th>
                            <th>Sản Phẩm Cây Cảnh</th>
                            <th class="col-price">Tổng Tiền</th>
                            <th>Ngày Đặt</th>
                            <th>Trạng Thái</th>
                            <th style="width: 160px; text-align: center;">Thao tác</th>
                        </tr>
                    </thead>
                    <tbody id="ordersTableBody">
                        <tr>
                            <td colspan="7" class="empty-state">
                                <div class="empty-state-icon"><i class="fa-solid fa-rotate fa-spin"></i></div>
                                <div class="empty-state-title">Đang tải dữ liệu từ Kho đơn chung...</div>
                            </td>
                        </tr>
                    </tbody>
                </table>
            </div>

            <!-- Pagination Toolbar Footer -->
            <div class="pagination-footer">
                <div class="pagination-info" id="paginationInfo">Hiển thị đơn hàng trong Pool</div>
                <div class="pagination-controls" id="paginationControls"></div>
            </div>
        </div>
    </div>

    <th:block id="page-scripts">
        <script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.2/dist/js/bootstrap.bundle.min.js"></script>
        <script src="/js/orders_pool.js"></script>
    </th:block>
</body>
</html>
```

#### 8.2. `my_orders.html`
Tạo file mới tại: `src/main/resources/templates/moderator/my_orders.html`

```html
<!DOCTYPE html>
<html lang="vi" 
      xmlns:th="http://www.thymeleaf.org"
      th:replace="~{fragments/dashboard-layout :: layout(~{::title}, ~{::#moderator-content}, ~{::#page-styles}, ~{::#page-scripts})}"
      th:with="paramActivePage='my-orders'">
<head>
    <title>My Orders - Đơn Hàng Của Tôi</title>
    <th:block id="page-styles">
        <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.2/dist/css/bootstrap.min.css">
        <link rel="stylesheet" href="/css/moderator-orders.css">
        <meta name="_csrf" th:content="${_csrf != null ? _csrf.token : ''}"/>
        <meta name="_csrf_header" th:content="${_csrf != null ? _csrf.headerName : 'X-CSRF-TOKEN'}"/>
        <style>
            .timeline-item {
                position: relative;
                padding-left: 24px;
                padding-bottom: 12px;
                border-left: 2px solid #e2e8f0;
            }
            .timeline-item::before {
                content: '';
                position: absolute;
                left: -6px;
                top: 2px;
                width: 10px;
                height: 10px;
                border-radius: 50%;
                background: #319795;
            }
            .timeline-item.active::before {
                background: #38a169;
                box-shadow: 0 0 0 3px rgba(56, 161, 105, 0.2);
            }
            .timeline-item.released::before {
                background: #e53e3e;
            }
            .timeline-time {
                font-size: 11px;
                color: #718096;
            }
            .timeline-text {
                font-size: 13px;
                font-weight: 600;
                color: #2d3748;
            }
        </style>
    </th:block>
</head>
<body>
    <div id="moderator-content">
        <!-- Sub Navigation Tabs -->
        <div class="mb-4 d-flex gap-2">
            <a href="/moderator/orders/pool" class="btn btn-outline-secondary">
                <i class="fa-solid fa-box-open me-1"></i> Kho Đơn Hàng (Pool)
            </a>
            <a href="/moderator/orders/my" class="btn btn-primary">
                <i class="fa-solid fa-user-check me-1"></i> Đơn Của Tôi (My Orders)
            </a>
        </div>

        <!-- KPI Metrics Header Cards (Personal Stats) -->
        <section class="kpi-row">
            <div class="kpi-card kpi-all">
                <div class="kpi-icon-wrapper"><i class="fa-solid fa-box"></i></div>
                <div class="kpi-data">
                    <span class="kpi-label">Đơn tôi đã nhận</span>
                    <span class="kpi-value" id="kpiTotalCount">0</span>
                </div>
            </div>
            <div class="kpi-card kpi-pending">
                <div class="kpi-icon-wrapper"><i class="fa-solid fa-gauge-high"></i></div>
                <div class="kpi-data">
                    <span class="kpi-label">Chờ tôi duyệt</span>
                    <span class="kpi-value" id="kpiPendingCount">0</span>
                </div>
            </div>
            <div class="kpi-card kpi-approved">
                <div class="kpi-icon-wrapper"><i class="fa-solid fa-circle-check"></i></div>
                <div class="kpi-data">
                    <span class="kpi-label">Tôi đã duyệt</span>
                    <span class="kpi-value" id="kpiApprovedCount">0</span>
                </div>
            </div>
            <div class="kpi-card kpi-paid">
                <div class="kpi-icon-wrapper"><i class="fa-solid fa-money-bill-wave"></i></div>
                <div class="kpi-data">
                    <span class="kpi-label">Đã thanh toán</span>
                    <span class="kpi-value" id="kpiPaidCount">0</span>
                </div>
            </div>
            <div class="kpi-card kpi-rejected">
                <div class="kpi-icon-wrapper"><i class="fa-solid fa-circle-xmark"></i></div>
                <div class="kpi-data">
                    <span class="kpi-label">Tôi đã từ chối</span>
                    <span class="kpi-value" id="kpiRejectedCount">0</span>
                </div>
            </div>
        </section>

        <!-- Main Management Card -->
        <div class="management-card">
            <div class="toolbar-header">
                <div class="toolbar-search-row">
                    <div class="search-input-wrapper">
                        <span class="search-icon"><i class="fa-solid fa-magnifying-glass"></i></span>
                        <input type="text" id="orderSearchInput" placeholder="Tìm kiếm trong danh sách đơn của tôi...">
                    </div>
                    <div class="toolbar-filters-group">
                        <div class="sort-select-wrapper">
                            <select id="orderSortSelect">
                                <option value="date_desc" selected>Mới nhất xếp trước</option>
                                <option value="date_asc">Cũ nhất xếp trước</option>
                                <option value="price_desc">Giá trị lớn nhất</option>
                                <option value="price_asc">Giá trị nhỏ nhất</option>
                            </select>
                        </div>
                    </div>
                </div>

                <div class="filter-tabs" id="statusFilterTabs">
                    <button class="tab-btn active" data-status="ALL">Tất cả đơn hàng</button>
                    <button class="tab-btn" data-status="PENDING">Chờ duyệt (Pending)</button>
                    <button class="tab-btn" data-status="APPROVED">Đã duyệt (Approved)</button>
                    <button class="tab-btn" data-status="PAID">Đã thanh toán (Paid)</button>
                    <button class="tab-btn" data-status="CANCELLED">Đã hủy (Cancelled)</button>
                    <button class="tab-btn" data-status="REJECTED">Từ chối (Rejected)</button>
                </div>
            </div>

            <!-- Table -->
            <div class="table-container">
                <table class="orders-table" id="ordersDataTable">
                    <thead>
                        <tr>
                            <th>Mã Đơn Hàng</th>
                            <th>Khách Hàng</th>
                            <th>Sản Phẩm Cây Cảnh</th>
                            <th class="col-price">Tổng Tiền</th>
                            <th>Ngày Đặt</th>
                            <th>Trạng Thái</th>
                            <th style="width: 140px; text-align: center;">Thao tác</th>
                        </tr>
                    </thead>
                    <tbody id="ordersTableBody">
                        <tr>
                            <td colspan="7" class="empty-state">Đang tải danh sách của bạn...</td>
                        </tr>
                    </tbody>
                </table>
            </div>

            <div class="pagination-footer">
                <div class="pagination-info" id="paginationInfo">Hiển thị đơn hàng</div>
                <div class="pagination-controls" id="paginationControls"></div>
            </div>
        </div>

        <!-- Component Drawer Chi tiết Đơn hàng & Timeline Lịch sử -->
        <div class="drawer-backdrop" id="drawerBackdrop"></div>
        <div class="drawer-panel" id="drawerPanel">
            <div class="drawer-header">
                <div class="drawer-title">
                    <span>Chi Tiết Đơn Hàng</span>
                    <span class="col-code" id="drawerOrderCode">BSMS-XXXXX</span>
                </div>
                <button class="drawer-close-btn" id="btnDrawerClose">&times;</button>
            </div>
            
            <div class="drawer-body">
                <div class="drawer-section" style="display:flex; justify-content:space-between; align-items:center;">
                    <span class="detail-label">Trạng thái:</span>
                    <span id="drawerStatusBadge" class="status-badge pending">PENDING</span>
                </div>

                <!-- Customer Details -->
                <div class="drawer-section">
                    <h4 class="section-title">Khách hàng đặt hàng</h4>
                    <div class="detail-grid">
                        <span class="detail-label">Họ tên:</span> <span class="detail-value" id="drawerCustName">-</span>
                        <span class="detail-label">Điện thoại:</span> <span class="detail-value" id="drawerCustPhone">-</span>
                        <span class="detail-label">Email:</span> <span class="detail-value" id="drawerCustEmail">-</span>
                        <span class="detail-label">Địa chỉ:</span> <span class="detail-value" id="drawerCustAddress">-</span>
                    </div>
                </div>

                <!-- Product Details -->
                <div class="drawer-section">
                    <h4 class="section-title">Thông tin tác phẩm Bonsai</h4>
                    <div class="product-card-info">
                        <img src="" id="drawerProdImg" class="product-card-img" style="width: 80px; height: 80px; object-fit: cover;">
                        <div class="product-card-details">
                            <span class="product-card-name" id="drawerProdName">Bonsai Name</span>
                            <span class="product-card-price" id="drawerProdPrice">0 VND</span>
                        </div>
                    </div>
                </div>

                <!-- Fees Form -->
                <div class="drawer-section">
                    <h4 class="section-title">Phí Vận Chuyển & Cẩu Cây</h4>
                    <div class="fees-form-group">
                        <div class="input-field-group">
                            <label>Phí xe cẩu (Crane Fee)</label>
                            <input type="number" id="inputCraneFee" class="form-control" placeholder="0">
                        </div>
                        <div class="input-field-group mt-2">
                            <label>Phí vận chuyển (Ship)</label>
                            <input type="number" id="inputShippingFee" class="form-control" placeholder="0">
                        </div>
                    </div>
                </div>

                <!-- Handling Timeline Audit Log -->
                <div class="drawer-section">
                    <h4 class="section-title"><i class="fa-solid fa-clock-rotate-left me-1"></i> Lịch sử phân bổ & xử lý đơn</h4>
                    <div id="handlingTimelineContainer" class="mt-3">
                        <div class="text-muted small">Chưa có thông tin chuyển giao.</div>
                    </div>
                </div>

                <!-- Actions / Reject reason box -->
                <div class="reject-reason-box mt-3" id="rejectReasonBox" style="display: none;">
                    <label style="font-size:12px; font-weight:700; color:#c53030;">Nhập lý do từ chối duyệt đơn:</label>
                    <textarea id="textareaRejectReason" class="form-control mt-1" rows="3" placeholder="Lý do chi tiết..."></textarea>
                    <div class="mt-2 d-flex justify-content-end gap-2">
                        <button class="btn btn-secondary btn-sm" id="btnRejectCancel">Hủy</button>
                        <button class="btn btn-danger btn-sm" id="btnRejectConfirm">Xác nhận Từ chối</button>
                    </div>
                </div>
            </div>

            <!-- Drawer Footer Action Buttons -->
            <div class="drawer-footer d-flex gap-2 p-3">
                <button class="btn btn-outline-danger flex-grow-1" id="btnUnclaimOrder"><i class="fa-solid fa-undo"></i> Trả đơn về Pool</button>
                <button class="btn btn-danger flex-grow-1" id="btnRejectOrder"><i class="fa-solid fa-circle-xmark"></i> Từ chối</button>
                <button class="btn btn-success flex-grow-1" id="btnVerifyOrder"><i class="fa-solid fa-circle-check"></i> Phê duyệt</button>
            </div>
        </div>
    </div>

    <th:block id="page-scripts">
        <script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.2/dist/js/bootstrap.bundle.min.js"></script>
        <script src="/js/my_orders.js"></script>
    </th:block>
</body>
</html>
```

---

### BƯỚC 9: Tạo Client JavaScript Files

#### 9.1. `orders_pool.js`
Tạo file mới tại: `src/main/resources/public/js/orders_pool.js`

```javascript
const DashboardState = {
    searchQuery: '',
    sortBy: 'date_desc',
    currentPage: 1,
    pageSize: 8
};

const csrfToken = document.querySelector('meta[name="_csrf"]')?.getAttribute('content');
const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.getAttribute('content');

document.addEventListener('DOMContentLoaded', () => {
    initPool();
});

function initPool() {
    const searchInput = document.getElementById('orderSearchInput');
    const sortSelect = document.getElementById('orderSortSelect');

    let searchDebounceTimer;
    if (searchInput) {
        searchInput.addEventListener('input', (e) => {
            clearTimeout(searchDebounceTimer);
            searchDebounceTimer = setTimeout(() => {
                DashboardState.searchQuery = e.target.value;
                DashboardState.currentPage = 1;
                renderPool();
            }, 300);
        });
    }

    if (sortSelect) {
        sortSelect.addEventListener('change', (e) => {
            DashboardState.sortBy = e.target.value;
            DashboardState.currentPage = 1;
            renderPool();
        });
    }

    renderPool();
}

async function renderPool() {
    const params = new URLSearchParams({
        search: DashboardState.searchQuery,
        sort: DashboardState.sortBy,
        page: DashboardState.currentPage,
        limit: DashboardState.pageSize
    });

    try {
        const response = await fetch(`/api/orders/pool?${params.toString()}`);
        if (!response.ok) return;
        const result = await response.json();

        renderTable(result.orders);
        renderPagination(result);
    } catch (err) {
        console.error("Error fetching pool orders:", err);
    }
}

function renderTable(orders) {
    const tableBody = document.getElementById('ordersTableBody');
    if (!tableBody) return;
    tableBody.innerHTML = '';

    if (!orders || orders.length === 0) {
        tableBody.innerHTML = `
            <tr>
                <td colspan="7" class="text-center p-4">
                    <div class="text-muted"><i class="fa-solid fa-inbox fa-2x mb-2"></i></div>
                    <div>Kho đơn hàng chung hiện tại trống!</div>
                </td>
            </tr>
        `;
        return;
    }

    orders.forEach(order => {
        const tr = document.createElement('tr');
        tr.innerHTML = `
            <td class="col-code">${order.orderCode}</td>
            <td><strong>${order.customer ? order.customer.name : 'N/A'}</strong></td>
            <td>${order.product ? order.product.name : 'Không có'}</td>
            <td class="col-price">${new Intl.NumberFormat('vi-VN', {style: 'currency', currency: 'VND'}).format(order.totalAmount)}</td>
            <td>${new Date(order.orderDate).toLocaleString('vi-VN')}</td>
            <td><span class="status-badge pending">${order.orderStatus}</span></td>
            <td class="text-center">
                <button class="btn btn-sm btn-success btn-claim-action" data-code="${order.orderCode}">
                    <i class="fa-solid fa-hand"></i> Nhận đơn
                </button>
            </td>
        `;

        tr.querySelector('.btn-claim-action').addEventListener('click', (e) => {
            e.stopPropagation();
            claimOrder(order.orderCode);
        });

        tableBody.appendChild(tr);
    });
}

async function claimOrder(orderCode) {
    const headers = { 'Content-Type': 'application/json' };
    if (csrfHeader && csrfToken) {
        headers[csrfHeader] = csrfToken;
    }

    try {
        const response = await fetch(`/api/orders/${orderCode}/claim`, {
            method: 'POST',
            headers: headers
        });

        const result = await response.json();
        if (response.ok && result.success) {
            alert("Nhận đơn hàng thành công! Đơn hàng đã được chuyển vào mục Đơn Của Tôi.");
            renderPool();
        } else if (response.status === 409) {
            alert("Xung đột: Đơn hàng này đã bị một Moderator khác nhận trước đó!");
            renderPool();
        } else {
            alert(result.message || "Lỗi khi nhận đơn hàng.");
        }
    } catch (err) {
        console.error("Error claiming order:", err);
        alert("Có lỗi kết nối đến máy chủ.");
    }
}

function renderPagination(result) {
    const infoEl = document.getElementById('paginationInfo');
    const controlsEl = document.getElementById('paginationControls');
    
    if (infoEl) {
        infoEl.textContent = `Hiển thị ${result.orders ? result.orders.length : 0} trong tổng số ${result.totalCount || 0} đơn chờ trong Pool`;
    }
    if (!controlsEl) return;
    controlsEl.innerHTML = '';
    if (!result.pages || result.pages <= 1) return;

    for (let i = 1; i <= result.pages; i++) {
        const btn = document.createElement('button');
        btn.className = `btn btn-sm mx-1 ${DashboardState.currentPage === i ? 'btn-primary' : 'btn-outline-secondary'}`;
        btn.textContent = i;
        btn.addEventListener('click', () => {
            DashboardState.currentPage = i;
            renderPool();
        });
        controlsEl.appendChild(btn);
    }
}
```

#### 9.2. `my_orders.js`
Tạo file mới tại: `src/main/resources/public/js/my_orders.js`

```javascript
const DashboardState = {
    searchQuery: '',
    selectedStatus: 'ALL',
    sortBy: 'date_desc',
    currentPage: 1,
    pageSize: 8
};

let activeOrderCode = null;
const csrfToken = document.querySelector('meta[name="_csrf"]')?.getAttribute('content');
const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.getAttribute('content');

document.addEventListener('DOMContentLoaded', () => {
    initApp();
});

function initApp() {
    const searchInput = document.getElementById('orderSearchInput');
    const sortSelect = document.getElementById('orderSortSelect');
    const tabContainer = document.getElementById('statusFilterTabs');

    let searchDebounceTimer;
    if (searchInput) {
        searchInput.addEventListener('input', (e) => {
            clearTimeout(searchDebounceTimer);
            searchDebounceTimer = setTimeout(() => {
                DashboardState.searchQuery = e.target.value;
                DashboardState.currentPage = 1;
                renderDashboard();
            }, 300);
        });
    }

    if (sortSelect) {
        sortSelect.addEventListener('change', (e) => {
            DashboardState.sortBy = e.target.value;
            DashboardState.currentPage = 1;
            renderDashboard();
        });
    }

    if (tabContainer) {
        tabContainer.addEventListener('click', (e) => {
            const btn = e.target.closest('.tab-btn');
            if (!btn) return;
            tabContainer.querySelectorAll('.tab-btn').forEach(t => t.classList.remove('active'));
            btn.classList.add('active');
            DashboardState.selectedStatus = btn.dataset.status;
            DashboardState.currentPage = 1;
            renderDashboard();
        });
    }

    initDrawerEvents();
    renderDashboard();
}

async function renderDashboard() {
    updatePersonalKPIs();

    const params = new URLSearchParams({
        search: DashboardState.searchQuery,
        status: DashboardState.selectedStatus,
        sort: DashboardState.sortBy,
        page: DashboardState.currentPage,
        limit: DashboardState.pageSize
    });

    try {
        const response = await fetch(`/api/orders/my?${params.toString()}`);
        if (!response.ok) return;
        const result = await response.json();

        renderTable(result.orders);
        renderPagination(result);
    } catch (err) {
        console.error("Lỗi khi tải danh sách đơn hàng:", err);
    }
}

async function updatePersonalKPIs() {
    try {
        const response = await fetch('/api/orders/my-stats');
        if (!response.ok) return;
        const data = await response.json();

        document.getElementById('kpiTotalCount').textContent = data.total || 0;
        document.getElementById('kpiPendingCount').textContent = data.pending || 0;
        document.getElementById('kpiApprovedCount').textContent = data.approved || 0;
        document.getElementById('kpiPaidCount').textContent = data.paid || 0;
        document.getElementById('kpiRejectedCount').textContent = data.rejected || 0;
    } catch (err) {
        console.error("Lỗi cập nhật KPI cá nhân:", err);
    }
}

function renderTable(orders) {
    const tableBody = document.getElementById('ordersTableBody');
    if (!tableBody) return;
    tableBody.innerHTML = '';

    if (!orders || orders.length === 0) {
        tableBody.innerHTML = `
            <tr>
                <td colspan="7" class="text-center p-4">Không tìm thấy đơn hàng nào trong mục Đơn Của Tôi.</td>
            </tr>
        `;
        return;
    }

    orders.forEach(order => {
        const tr = document.createElement('tr');
        tr.style.cursor = 'pointer';
        tr.addEventListener('click', () => openDrawer(order));

        tr.innerHTML = `
            <td class="col-code">${order.orderCode}</td>
            <td><strong>${order.customer ? order.customer.name : 'N/A'}</strong></td>
            <td>${order.product ? order.product.name : 'Không có'}</td>
            <td class="col-price">${new Intl.NumberFormat('vi-VN', {style: 'currency', currency: 'VND'}).format(order.totalAmount)}</td>
            <td>${new Date(order.orderDate).toLocaleString('vi-VN')}</td>
            <td><span class="status-badge ${order.orderStatus.toLowerCase()}">${order.orderStatus}</span></td>
            <td class="text-center">
                <button class="btn btn-sm btn-outline-primary"><i class="fa-solid fa-eye"></i> Chi tiết</button>
            </td>
        `;

        tableBody.appendChild(tr);
    });
}

function initDrawerEvents() {
    const backdrop = document.getElementById('drawerBackdrop');
    const closeBtn = document.getElementById('btnDrawerClose');
    const unclaimBtn = document.getElementById('btnUnclaimOrder');
    const verifyBtn = document.getElementById('btnVerifyOrder');
    const rejectBtn = document.getElementById('btnRejectOrder');
    const rejectConfirmBtn = document.getElementById('btnRejectConfirm');
    const rejectCancelBtn = document.getElementById('btnRejectCancel');

    if (backdrop) backdrop.addEventListener('click', closeDrawer);
    if (closeBtn) closeBtn.addEventListener('click', closeDrawer);

    if (unclaimBtn) {
        unclaimBtn.addEventListener('click', () => {
            if (confirm("Bạn có chắc chắn muốn trả lại đơn hàng này về Kho đơn chung (Pool)?")) {
                unclaimOrder(activeOrderCode);
            }
        });
    }

    if (verifyBtn) {
        verifyBtn.addEventListener('click', () => {
            verifyOrder(activeOrderCode);
        });
    }

    if (rejectBtn) {
        rejectBtn.addEventListener('click', () => {
            document.getElementById('rejectReasonBox').style.display = 'block';
        });
    }

    if (rejectCancelBtn) {
        rejectCancelBtn.addEventListener('click', () => {
            document.getElementById('rejectReasonBox').style.display = 'none';
        });
    }

    if (rejectConfirmBtn) {
        rejectConfirmBtn.addEventListener('click', () => {
            const reason = document.getElementById('textareaRejectReason').value.trim();
            if (!reason) {
                alert("Vui lòng nhập lý do từ chối!");
                return;
            }
            rejectOrder(activeOrderCode, reason);
        });
    }
}

function openDrawer(order) {
    activeOrderCode = order.orderCode;
    document.getElementById('drawerOrderCode').textContent = order.orderCode;
    
    const badge = document.getElementById('drawerStatusBadge');
    badge.textContent = order.orderStatus;
    badge.className = `status-badge ${order.orderStatus.toLowerCase()}`;

    if (order.customer) {
        document.getElementById('drawerCustName').textContent = order.customer.name || '-';
        document.getElementById('drawerCustPhone').textContent = order.customer.phone || '-';
        document.getElementById('drawerCustEmail').textContent = order.customer.email || '-';
        document.getElementById('drawerCustAddress').textContent = order.customer.address || '-';
    }

    if (order.product) {
        document.getElementById('drawerProdName').textContent = order.product.name || 'Bonsai';
        document.getElementById('drawerProdPrice').textContent = new Intl.NumberFormat('vi-VN', {style: 'currency', currency: 'VND'}).format(order.product.price || 0);
        document.getElementById('drawerProdImg').src = order.product.image || '/images/default-tree.jpg';
    }

    document.getElementById('inputCraneFee').value = order.craneFee || 0;
    document.getElementById('inputShippingFee').value = order.shippingFee || 0;

    // Render Handling Timeline Log
    renderTimeline(order.handlingHistory);

    const isPending = order.orderStatus === 'PENDING';
    document.getElementById('btnUnclaimOrder').style.display = isPending ? 'block' : 'none';
    document.getElementById('btnVerifyOrder').style.display = isPending ? 'block' : 'none';
    document.getElementById('btnRejectOrder').style.display = isPending ? 'block' : 'none';
    document.getElementById('rejectReasonBox').style.display = 'none';

    document.getElementById('drawerBackdrop').classList.add('show');
    document.getElementById('drawerPanel').classList.add('open');
}

function closeDrawer() {
    document.getElementById('drawerBackdrop').classList.remove('show');
    document.getElementById('drawerPanel').classList.remove('open');
}

function renderTimeline(history) {
    const container = document.getElementById('handlingTimelineContainer');
    if (!container) return;
    container.innerHTML = '';

    if (!history || history.length === 0) {
        container.innerHTML = '<div class="text-muted small">Chưa có thông tin chuyển giao.</div>';
        return;
    }

    history.forEach(item => {
        const div = document.createElement('div');
        div.className = `timeline-item ${item.isActive ? 'active' : 'released'}`;
        
        const handledTimeStr = item.handledAt ? new Date(item.handledAt).toLocaleString('vi-VN') : 'N/A';
        const releasedTimeStr = item.releasedAt ? new Date(item.releasedAt).toLocaleString('vi-VN') : null;
        const statusText = item.isActive ? 
            `<span class="text-success font-weight-bold">Đang xử lý (Active)</span>` : 
            `<span class="text-secondary">Đã ngưng quản lý (${releasedTimeStr})</span>`;

        div.innerHTML = `
            <div class="timeline-text">${item.moderatorFullName} (@${item.moderatorUsername})</div>
            <div class="timeline-time"><i class="fa-regular fa-clock me-1"></i> Bắt đầu: ${handledTimeStr}</div>
            <div class="timeline-time mt-1">Trạng thái: ${statusText}</div>
        `;
        container.appendChild(div);
    });
}

async function unclaimOrder(orderCode) {
    const headers = { 'Content-Type': 'application/json' };
    if (csrfHeader && csrfToken) headers[csrfHeader] = csrfToken;

    try {
        const response = await fetch(`/api/orders/${orderCode}/unclaim`, {
            method: 'POST',
            headers: headers
        });
        const result = await response.json();

        if (response.ok && result.success) {
            alert("Đã trả đơn về Kho đơn chung!");
            closeDrawer();
            renderDashboard();
        } else {
            alert(result.message || "Lỗi khi trả đơn.");
        }
    } catch (err) {
        console.error("Lỗi khi trả đơn:", err);
    }
}

async function verifyOrder(orderCode) {
    const craneFee = parseFloat(document.getElementById('inputCraneFee').value) || 0;
    const shippingFee = parseFloat(document.getElementById('inputShippingFee').value) || 0;

    const headers = { 'Content-Type': 'application/json' };
    if (csrfHeader && csrfToken) headers[csrfHeader] = csrfToken;

    try {
        const response = await fetch(`/api/orders/${orderCode}/verify`, {
            method: 'POST',
            headers: headers,
            body: JSON.stringify({ craneFee, shippingFee })
        });
        const result = await response.json();

        if (response.ok && result.success) {
            alert("Phê duyệt đơn hàng thành công!");
            closeDrawer();
            renderDashboard();
        } else {
            alert(result.message || "Không thể phê duyệt đơn hàng.");
        }
    } catch (err) {
        console.error("Lỗi khi phê duyệt:", err);
    }
}

async function rejectOrder(orderCode, reason) {
    const headers = { 'Content-Type': 'application/json' };
    if (csrfHeader && csrfToken) headers[csrfHeader] = csrfToken;

    try {
        const response = await fetch(`/api/orders/${orderCode}/reject`, {
            method: 'POST',
            headers: headers,
            body: JSON.stringify({ reason })
        });
        const result = await response.json();

        if (response.ok && result.success) {
            alert("Từ chối đơn hàng thành công!");
            closeDrawer();
            renderDashboard();
        } else {
            alert(result.message || "Lỗi khi từ chối đơn hàng.");
        }
    } catch (err) {
        console.error("Lỗi từ chối:", err);
    }
}

function renderPagination(result) {
    const infoEl = document.getElementById('paginationInfo');
    const controlsEl = document.getElementById('paginationControls');
    
    if (infoEl) {
        infoEl.textContent = `Hiển thị ${result.orders ? result.orders.length : 0} trong tổng số ${result.totalCount || 0} đơn của bạn`;
    }
    if (!controlsEl) return;
    controlsEl.innerHTML = '';
    if (!result.pages || result.pages <= 1) return;

    for (let i = 1; i <= result.pages; i++) {
        const btn = document.createElement('button');
        btn.className = `btn btn-sm mx-1 ${DashboardState.currentPage === i ? 'btn-primary' : 'btn-outline-secondary'}`;
        btn.textContent = i;
        btn.addEventListener('click', () => {
            DashboardState.currentPage = i;
            renderDashboard();
        });
        controlsEl.appendChild(btn);
    }
}
```

---

## 3. Kiểm thử sau khi bạn gõ xong code

Sau khi bạn copy và paste các đoạn mã trên vào dự án, bạn chạy lệnh sau trong Terminal để kiểm tra biên dịch:

```bash
./mvnw clean test-compile
```

Nếu màn hình hiển thị **`BUILD SUCCESS`**, bạn khởi chạy dự án:
```bash
./mvnw spring-boot:run
```
và truy cập đường dẫn `http://localhost:8080/moderator/orders/pool` để trải nghiệm hệ thống!
