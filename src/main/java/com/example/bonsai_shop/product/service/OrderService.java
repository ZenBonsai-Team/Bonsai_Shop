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

import java.util.ArrayList;
import java.util.stream.Collectors;

import com.example.bonsai_shop.config.VNPayConfig;
import com.example.bonsai_shop.entity.CartItem;
import com.example.bonsai_shop.entity.Order;
import com.example.bonsai_shop.entity.OrderDetail;
import com.example.bonsai_shop.entity.OrderHandling;
import com.example.bonsai_shop.entity.OrderLog;
import com.example.bonsai_shop.entity.Product;
import com.example.bonsai_shop.entity.User;
import com.example.bonsai_shop.product.dto.PurchaseOrderRequestDTO;
import com.example.bonsai_shop.product.event.OrderCreatedEvent;
import com.example.bonsai_shop.product.event.OrderPaidEvent;
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

    public static final BigDecimal MAX_ORDER_AMOUNT = new BigDecimal("200000000");

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final OrderLogRepository orderLogRepository;
    private final OrderHandlingRepository orderHandlingRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final MailService mailService;
    private final CartService cartService;

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

    @Transactional(readOnly = true)
    public List<Order> getOrdersByCustomerId(Integer customerId) {
        return orderRepository.findByCustomerUserIdWithDetailsOrderByOrderDateDesc(customerId);
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
    public Order getOrderByCodeWithDetails(String orderCode) {
        if (orderCode == null || orderCode.trim().isEmpty()) {
            return null;
        }
        return orderRepository.findByOrderCodeWithDetails(orderCode.trim()).orElse(null);
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
        if (!"PENDING".equalsIgnoreCase(order.getOrderStatus())) {
            throw new IllegalStateException("Chỉ được phép trả lại đơn hàng chưa duyệt!");
        }

        order.setAssignedTo(null);
        order.setAssignedAt(null);
        orderRepository.save(order);

        orderHandlingRepository.findAll().stream()
                .filter(h -> h.getOrder() != null && h.getOrder().getOrderId().equals(order.getOrderId()) 
                          && h.getModerator() != null && h.getModerator().getUserId().equals(moderator.getUserId())
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
        return verifyOrder(orderCode, craneFee, shippingFee, null, moderator);
    }

    @Transactional
    public boolean verifyOrder(String orderCode, BigDecimal craneFee, BigDecimal shippingFee, BigDecimal depositAmount, User moderator) {
        Order order = orderRepository.findByOrderCodeWithDetails(orderCode).orElse(null);
        if (order == null || !"PENDING".equalsIgnoreCase(order.getOrderStatus())) {
            return false;
        }

        // Kiểm tra quyền sở hữu đơn hàng
        if (order.getAssignedTo() == null || !order.getAssignedTo().getUserId().equals(moderator.getUserId())) {
            throw new SecurityException("Bạn không có quyền duyệt đơn hàng này!");
        }

        String oldStatus = order.getOrderStatus();
        order.setCraneFee(craneFee != null ? craneFee : BigDecimal.ZERO);
        order.setShippingFee(shippingFee != null ? shippingFee : BigDecimal.ZERO);
        if (depositAmount != null) {
            order.setDepositAmount(depositAmount);
        }
        order.setOrderStatus("APPROVED");

        BigDecimal originalAmount = order.getTotalAmount();
        BigDecimal newTotal = originalAmount.add(order.getCraneFee()).add(order.getShippingFee());
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
                .filter(h -> h.getOrder() != null && h.getOrder().getOrderId().equals(order.getOrderId()) 
                          && h.getModerator() != null && h.getModerator().getUserId().equals(moderator.getUserId())
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
        Order order = orderRepository.findByOrderCodeWithDetails(orderCode).orElse(null);
        if (order == null || !"PENDING".equalsIgnoreCase(order.getOrderStatus())) {
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

        if (order.getOrderDetails() != null) {
            for (OrderDetail detail : order.getOrderDetails()) {
                Product product = detail.getProduct();
                if (product != null) {
                    product.setProductStatus("AVAILABLE");
                    productRepository.save(product);
                }
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
                .filter(h -> h.getOrder() != null && h.getOrder().getOrderId().equals(order.getOrderId()) 
                          && h.getModerator() != null && h.getModerator().getUserId().equals(moderator.getUserId())
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

    @Transactional
    public boolean recordDepositPayment(String orderCode, BigDecimal depositAmount, User moderator) {
        Order order = orderRepository.findByOrderCode(orderCode).orElse(null);
        if (order == null || (!"PENDING".equalsIgnoreCase(order.getOrderStatus()) && !"APPROVED".equalsIgnoreCase(order.getOrderStatus()))) {
            return false;
        }

        String oldStatus = order.getOrderStatus();
        order.setDepositAmount(depositAmount);
        order.setOrderStatus("DEPOSITED");
        orderRepository.save(order);

        OrderLog log = OrderLog.builder()
                .order(order)
                .actionBy(moderator)
                .actionType("DEPOSIT")
                .fromStatus(oldStatus)
                .toStatus("DEPOSITED")
                .actionAt(LocalDateTime.now())
                .build();
        orderLogRepository.save(log);

        try {
            mailService.sendOrderDepositedEmail(order);
        } catch (Exception e) {
            // Log warning if email fail
        }

        return true;
    }

    // =========================================================================
    // VALIDATION & PRODUCT RESOLUTION — Refactored (Phương Án B)
    // =========================================================================

    /**
     * [PRIVATE] Nguồn duy nhất để đọc danh sách Product cần mua — Single Source of Truth.
     * Ưu tiên: productIds list > productId đơn > Cart của logged-in user.
     * Luôn đọc từ DB, không tin dữ liệu từ client.
     */
    private List<Product> resolveProductsToBuy(PurchaseOrderRequestDTO dto, User customer) {
        List<Product> productsToBuy = new ArrayList<>();
        if (dto.getProductIds() != null && !dto.getProductIds().isEmpty()) {
            productsToBuy.addAll(productRepository.findAllById(dto.getProductIds()));
        } else if (dto.getProductId() != null) {
            productRepository.findById(dto.getProductId()).ifPresent(productsToBuy::add);
        } else if (customer != null) {
            List<CartItem> cartItems = cartService.getCartItems(customer.getUserId());
            if (cartItems != null) {
                for (CartItem item : cartItems) {
                    productsToBuy.add(item.getProduct());
                }
            }
        }
        return productsToBuy;
    }

    /**
     * [PUBLIC] Kiểm tra tổng giá trị đơn hàng không vượt 200 triệu.
     * Nhận List<Product> đã resolve — KHÔNG đọc DB thêm lần nào.
     */
    public void validateOrderLimit(List<Product> products) {
        BigDecimal totalAmount = products.stream()
                .map(Product::getPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (totalAmount.compareTo(MAX_ORDER_AMOUNT) > 0) {
            throw new IllegalArgumentException(
                    "Tổng giá trị đơn hàng vượt quá giới hạn tối đa cho phép (tối đa 200.000.000 VNĐ)!");
        }
    }

    /**
     * [PUBLIC] Overload cũ — resolve products rồi delegate xuống validateOrderLimit(List).
     * Giữ nguyên signature để không phá vỡ các caller hiện tại (Controller).
     */
    public void validateOrderLimit(PurchaseOrderRequestDTO dto, User customer) {
        validateOrderLimit(resolveProductsToBuy(dto, customer));
    }

    /**
     * [PUBLIC] Kiểm tra danh sách product IDs theo giới hạn tiền.
     * Giữ nguyên để tương thích ngược. Delegate xuống validateOrderLimit(List).
     */
    public void validateProductIdsLimit(List<Integer> productIds) {
        if (productIds == null || productIds.isEmpty()) {
            return;
        }
        validateOrderLimit(productRepository.findAllById(productIds));
    }

    /**
     * [PUBLIC] Tải danh sách Product từ list IDs — đọc từ DB.
     * Dùng khi chỉ có productIds (ví dụ: Guest OTP endpoint).
     */
    public List<Product> getProductsByIds(List<Integer> productIds) {
        if (productIds == null || productIds.isEmpty()) {
            return new ArrayList<>();
        }
        return productRepository.findAllById(productIds);
    }

    /**
     * [PUBLIC] Tải danh sách Product cho một checkout request.
     * Dùng cho pre-validation ở Controller trước khi tạo Order.
     * Ủy quyền cho resolveProductsToBuy() — Single Source of Truth.
     */
    public List<Product> loadProductsForOrder(PurchaseOrderRequestDTO dto, User customer) {
        return resolveProductsToBuy(dto, customer);
    }

    /**
     * [PUBLIC] UX Validation Layer — kiểm tra trạng thái từng sản phẩm.
     * Trả về danh sách Product KHÔNG khả dụng (không throw — để caller quyết định).
     *
     * QUAN TRỌNG: Đây CHỈ là UX layer — giảm thất bại chắc chắn.
     * KHÔNG đảm bảo data integrity.
     * reserveIfAvailable() bên trong createOrder() mới là lớp bảo vệ cuối cùng.
     */
    public List<Product> validateProductAvailability(List<Product> products) {
        return products.stream()
                .filter(p -> !"AVAILABLE".equalsIgnoreCase(p.getProductStatus()))
                .collect(Collectors.toList());
    }

    // =========================================================================
    // ORDER CREATION
    // =========================================================================

    @Transactional
    public Order createOrder(PurchaseOrderRequestDTO dto, User customer) {
        // Bước 1: Resolve danh sách Product từ nguồn duy nhất — đọc tươi từ DB
        List<Product> productsToBuy = resolveProductsToBuy(dto, customer);

        if (productsToBuy.isEmpty()) {
            throw new IllegalArgumentException("Giỏ hàng của bạn đang trống! Vui lòng chọn sản phẩm trước.");
        }

        // Bước 2: Kiểm tra giới hạn tổng tiền (không đọc DB lại)
        validateOrderLimit(productsToBuy);

        // Bước 3: Soft-check trạng thái (informational — có thể bị stale do Hibernate session cache)
        // LƯU Ý: Đây không phải data guard — reserveIfAvailable() bên dưới mới là lớp bảo vệ thực sự
        for (Product prod : productsToBuy) {
            if (!"AVAILABLE".equalsIgnoreCase(prod.getProductStatus())) {
                throw new IllegalStateException("Tác phẩm '" + prod.getProductName() + "' đã được bán hoặc giữ chỗ!");
            }
        }

        String orderCode = "BSMS-" + VNPayConfig.getRandomNumber(6).toUpperCase();
        BigDecimal totalAmount = productsToBuy.stream()
                .map(Product::getPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Order order = Order.builder()
                .customer(customer)
                .orderCode(orderCode)
                .customerName(dto.getCustomerName())
                .customerPhone(dto.getCustomerPhone())
                .customerEmail(dto.getCustomerEmail())
                .shippingAddress(dto.getShippingAddress())
                .orderDate(LocalDateTime.now())
                .totalAmount(totalAmount)
                .depositAmount(BigDecimal.ZERO)
                .orderStatus("PENDING")
                .orderType("ONLINE")
                .build();

        // Bước 4: DATA INTEGRITY LAYER — Atomic Reserve
        // UPDATE WHERE status='AVAILABLE': chỉ 1 thread thắng, thread thua rollback toàn bộ @Transactional
        // KHÔNG ĐƯỢC BỎ — đây là lớp bảo vệ duy nhất đảm bảo không có 2 Order cho cùng 1 sản phẩm
        List<OrderDetail> details = productsToBuy.stream().map(prod -> {
            int reserved = productRepository.reserveIfAvailable(prod.getProductId());
            if (reserved == 0) {
                throw new IllegalStateException("Tác phẩm '" + prod.getProductName() + "' đã được bán hoặc giữ chỗ!");
            }
            prod.setProductStatus("RESERVED");
            return OrderDetail.builder()
                    .order(order)
                    .product(prod)
                    .priceAtPurchase(prod.getPrice())
                    .build();
        }).collect(Collectors.toList());

        order.setOrderDetails(details);
        Order savedOrder = orderRepository.save(order);

        if (customer != null) {
            cartService.clearCart(customer.getUserId());
        }

        initializeOrderDetails(savedOrder);
        eventPublisher.publishEvent(new OrderCreatedEvent(savedOrder));
        return savedOrder;
    }

    @Transactional
    public boolean processPaymentSuccess(String orderCode) {
        Order order = orderRepository.findByOrderCode(orderCode).orElse(null);
        if (order == null) {
            return false;
        }

        if ("PAID".equalsIgnoreCase(order.getOrderStatus())) {
            return true;
        }

        order.setOrderStatus("PAID");
        orderRepository.save(order);

        if (order.getOrderDetails() != null) {
            for (OrderDetail detail : order.getOrderDetails()) {
                Product prod = detail.getProduct();
                if (prod != null) {
                    prod.setProductStatus("SOLD");
                    productRepository.save(prod);
                }
            }
        }

        eventPublisher.publishEvent(new OrderPaidEvent(order));
        return true;
    }

    @Transactional
    public boolean recordFinalPayment(String orderCode, User moderator) {
        Order order = orderRepository.findByOrderCode(orderCode).orElse(null);
        if (order == null) {
            return false;
        }

        String oldStatus = order.getOrderStatus();
        boolean result = processPaymentSuccess(orderCode);

        if (result) {
            OrderLog log = OrderLog.builder()
                    .order(order)
                    .actionBy(moderator)
                    .actionType("PAID")
                    .fromStatus(oldStatus)
                    .toStatus("PAID")
                    .actionAt(LocalDateTime.now())
                    .build();
            orderLogRepository.save(log);
        }

        return result;
    }
}

