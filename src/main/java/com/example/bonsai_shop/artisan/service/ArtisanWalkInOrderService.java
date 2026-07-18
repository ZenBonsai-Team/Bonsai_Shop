package com.example.bonsai_shop.artisan.service;

import com.example.bonsai_shop.entity.Order;
import com.example.bonsai_shop.entity.OrderDetail;
import com.example.bonsai_shop.entity.OrderLog;
import com.example.bonsai_shop.entity.Payment;
import com.example.bonsai_shop.entity.Product;
import com.example.bonsai_shop.entity.User;
import com.example.bonsai_shop.product.repository.OrderLogRepository;
import com.example.bonsai_shop.product.repository.OrderRepository;
import com.example.bonsai_shop.product.repository.PaymentRepository;
import com.example.bonsai_shop.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
public class ArtisanWalkInOrderService {

    public static final String STATUS_PENDING_PAYMENT = "PENDING_PAYMENT";
    public static final String STATUS_COMPLETED = "COMPLETED";
    public static final String STATUS_CANCELLED = "CANCELLED";
    public static final String ORDER_TYPE_WALK_IN = "WALK_IN";
    public static final String ORDER_TYPE_ONLINE = "ONLINE";
    public static final String PRODUCT_AVAILABLE = "AVAILABLE";
    public static final String PRODUCT_RESERVED = "RESERVED";
    public static final String PRODUCT_SOLD = "SOLD";
    public static final String PAYMENT_METHOD_CASH = "CASH";
    public static final String PAYMENT_METHOD_VNPAY = "VNPAY";

    private final ArtisanProductService artisanProductService;
    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final OrderLogRepository orderLogRepository;

    public List<Product> getAvailableProducts(String artisanEmail) {
        Integer artisanUserId = artisanProductService.getArtisanUser(artisanEmail).getUserId();
        return productRepository.findByArtisanUserIdAndProductStatusOrderByCreatedAtDesc(
                artisanUserId,
                PRODUCT_AVAILABLE
        );
    }

    public Page<Order> getWalkInOrders(String artisanEmail, String status, int page, int size) {
        Integer artisanUserId = artisanProductService.getArtisanUser(artisanEmail).getUserId();
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.max(size, 1));
        return orderRepository.findByArtisanUserIdAndTypeAndStatus(
                artisanUserId,
                ORDER_TYPE_WALK_IN,
                status == null || status.isBlank() ? "ALL" : status,
                pageable
        );
    }

    @Transactional
    public Order createWalkInOrder(String artisanEmail,
                                   Integer productId,
                                   String customerName,
                                   String customerPhone,
                                   String shippingAddress,
                                   String paymentMethod,
                                   BigDecimal craneFee,
                                   BigDecimal shippingFee,
                                   String customerEmail,
                                   String notes) {
        User artisanUser = artisanProductService.getArtisanUser(artisanEmail);
        Product product = productRepository.findByProductIdAndArtisanUserId(productId, artisanUser.getUserId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy sản phẩm thuộc artisan này."));

        if (!PRODUCT_AVAILABLE.equalsIgnoreCase(product.getProductStatus())) {
            throw new RuntimeException("Chỉ có thể tạo walk-in order cho sản phẩm đang bán.");
        }

        BigDecimal normalizedCraneFee = nonNegative(craneFee, "Phí cẩu không được âm.");
        BigDecimal normalizedShippingFee = nonNegative(shippingFee, "Phí vận chuyển không được âm.");
        String normalizedPaymentMethod = normalizePaymentMethod(paymentMethod);
        BigDecimal totalAmount = product.getPrice()
                .add(normalizedCraneFee)
                .add(normalizedShippingFee);

        Order order = Order.builder()
                .orderCode(generateOrderCode())
                .customerName(requireText(customerName, "Vui lòng nhập tên khách walk-in."))
                .customerPhone(requireText(customerPhone, "Vui lòng nhập số điện thoại khách walk-in."))
                .customerEmail(blankToNull(customerEmail))
                .shippingAddress(requireText(shippingAddress, "Vui lòng nhập địa chỉ giao/nhận cây."))
                .orderDate(LocalDateTime.now())
                .totalAmount(totalAmount)
                .depositAmount(BigDecimal.ZERO)
                .craneFee(normalizedCraneFee)
                .shippingFee(normalizedShippingFee)
                .orderStatus(STATUS_PENDING_PAYMENT)
                .orderType(ORDER_TYPE_WALK_IN)
                .notes(blankToNull(notes))
                .build();

        OrderDetail detail = OrderDetail.builder()
                .order(order)
                .product(product)
                .priceAtPurchase(product.getPrice())
                .build();
        order.setOrderDetails(List.of(detail));

        Payment payment = Payment.builder()
                .order(order)
                .paymentMethod(normalizedPaymentMethod)
                .paymentStatus("PENDING")
                .paymentType("WALK_IN")
                .amount(totalAmount)
                .build();
        order.setPayment(payment);

        product.setProductStatus(PRODUCT_RESERVED);
        productRepository.save(product);
        Order savedOrder = orderRepository.save(order);
        log(savedOrder, artisanUser, "WALK_IN_CREATE", null, STATUS_PENDING_PAYMENT);
        return savedOrder;
    }

    @Transactional
    public Order cancelWalkInOrder(String artisanEmail, Integer orderId, String reason) {
        User artisanUser = artisanProductService.getArtisanUser(artisanEmail);
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy walk-in order."));

        Product product = getSingleProduct(order);
        if (product.getArtisan() == null || !artisanUser.getUserId().equals(product.getArtisan().getUserId())) {
            throw new RuntimeException("Order không thuộc artisan này.");
        }
        if (!ORDER_TYPE_WALK_IN.equalsIgnoreCase(order.getOrderType())) {
            throw new RuntimeException("Chỉ được hủy walk-in order.");
        }
        if (!STATUS_PENDING_PAYMENT.equalsIgnoreCase(order.getOrderStatus())) {
            throw new RuntimeException("Chỉ được hủy order đang chờ nhận tiền.");
        }

        String oldStatus = order.getOrderStatus();
        order.setOrderStatus(STATUS_CANCELLED);
        order.setNotes(appendCancelReason(order.getNotes(), reason));

        Payment payment = order.getPayment();
        if (payment != null) {
            payment.setPaymentStatus(STATUS_CANCELLED);
            paymentRepository.save(payment);
        }

        product.setProductStatus(PRODUCT_AVAILABLE);
        productRepository.save(product);

        Order savedOrder = orderRepository.save(order);
        log(savedOrder, artisanUser, "WALK_IN_CANCEL", oldStatus, STATUS_CANCELLED);
        return savedOrder;
    }

    @Transactional
    public Order updateWalkInOrder(String artisanEmail,
                                   Integer orderId,
                                   String customerName,
                                   String customerPhone,
                                   String shippingAddress,
                                   String paymentMethod,
                                   BigDecimal craneFee,
                                   BigDecimal shippingFee,
                                   String customerEmail,
                                   String notes) {
        User artisanUser = artisanProductService.getArtisanUser(artisanEmail);
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy walk-in order."));

        Product product = getSingleProduct(order);
        if (product.getArtisan() == null || !artisanUser.getUserId().equals(product.getArtisan().getUserId())) {
            throw new RuntimeException("Order không thuộc artisan này.");
        }
        if (!ORDER_TYPE_WALK_IN.equalsIgnoreCase(order.getOrderType())) {
            throw new RuntimeException("Chỉ được cập nhật walk-in order.");
        }
        if (!STATUS_PENDING_PAYMENT.equalsIgnoreCase(order.getOrderStatus())) {
            throw new RuntimeException("Chỉ được cập nhật order đang chờ nhận tiền.");
        }

        BigDecimal normalizedCraneFee = nonNegative(craneFee, "Phí cẩu không được âm.");
        BigDecimal normalizedShippingFee = nonNegative(shippingFee, "Phí vận chuyển không được âm.");
        String normalizedPaymentMethod = normalizePaymentMethod(paymentMethod);
        BigDecimal totalAmount = getBasePrice(order, product)
                .add(normalizedCraneFee)
                .add(normalizedShippingFee);

        order.setCustomerName(requireText(customerName, "Vui lòng nhập tên khách walk-in."));
        order.setCustomerPhone(requireText(customerPhone, "Vui lòng nhập số điện thoại khách walk-in."));
        order.setShippingAddress(requireText(shippingAddress, "Vui lòng nhập địa chỉ giao/nhận cây."));
        order.setCustomerEmail(blankToNull(customerEmail));
        order.setCraneFee(normalizedCraneFee);
        order.setShippingFee(normalizedShippingFee);
        order.setTotalAmount(totalAmount);
        order.setNotes(blankToNull(notes));

        Payment payment = order.getPayment();
        if (payment == null) {
            payment = Payment.builder()
                    .order(order)
                    .paymentStatus("PENDING")
                    .paymentType("WALK_IN")
                    .build();
        }
        payment.setPaymentMethod(normalizedPaymentMethod);
        payment.setAmount(totalAmount);
        paymentRepository.save(payment);
        order.setPayment(payment);

        Order savedOrder = orderRepository.save(order);
        log(savedOrder, artisanUser, "WALK_IN_UPDATE", STATUS_PENDING_PAYMENT, STATUS_PENDING_PAYMENT);
        return savedOrder;
    }

    @Transactional
    public Order confirmPayment(String artisanEmail, Integer orderId) {
        User artisanUser = artisanProductService.getArtisanUser(artisanEmail);
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy walk-in order."));

        Product product = getSingleProduct(order);
        if (product.getArtisan() == null || !artisanUser.getUserId().equals(product.getArtisan().getUserId())) {
            throw new RuntimeException("Order không thuộc artisan này.");
        }
        if (!STATUS_PENDING_PAYMENT.equalsIgnoreCase(order.getOrderStatus())) {
            throw new RuntimeException("Chỉ xác nhận thanh toán cho walk-in order đang chờ tiền.");
        }

        String oldStatus = order.getOrderStatus();
        Payment payment = order.getPayment();
        if (payment == null) {
            payment = Payment.builder()
                    .order(order)
                    .paymentMethod(PAYMENT_METHOD_CASH)
                    .paymentType("WALK_IN")
                    .amount(order.getTotalAmount())
                    .build();
        }
        payment.setPaymentStatus("SUCCESS");
        payment.setPaymentDate(LocalDateTime.now());
        paymentRepository.save(payment);

        order.setPayment(payment);
        order.setOrderStatus(STATUS_COMPLETED);
        product.setProductStatus(PRODUCT_SOLD);
        productRepository.save(product);
        Order savedOrder = orderRepository.save(order);
        log(savedOrder, artisanUser, "WALK_IN_PAYMENT_CONFIRMED", oldStatus, STATUS_COMPLETED);
        return savedOrder;
    }

    private Product getSingleProduct(Order order) {
        if (order.getOrderDetails() == null || order.getOrderDetails().isEmpty()) {
            throw new RuntimeException("Order chưa có sản phẩm.");
        }
        return order.getOrderDetails().get(0).getProduct();
    }

    private BigDecimal getBasePrice(Order order, Product product) {
        OrderDetail detail = order.getOrderDetails().get(0);
        if (detail.getPriceAtPurchase() != null) {
            return detail.getPriceAtPurchase();
        }
        return product.getPrice() == null ? BigDecimal.ZERO : product.getPrice();
    }

    private void log(Order order, User actionBy, String actionType, String fromStatus, String toStatus) {
        orderLogRepository.save(OrderLog.builder()
                .order(order)
                .actionBy(actionBy)
                .actionType(actionType)
                .fromStatus(fromStatus)
                .toStatus(toStatus)
                .actionAt(LocalDateTime.now())
                .build());
    }

    private String generateOrderCode() {
        String orderCode;
        do {
            orderCode = "BSMS-" + randomSixDigits();
        } while (orderRepository.findByOrderCode(orderCode).isPresent());
        return orderCode;
    }

    private String randomSixDigits() {
        return String.valueOf(ThreadLocalRandom.current().nextInt(100000, 1000000));
    }

    private String requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new RuntimeException(message);
        }
        return value.trim();
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String appendCancelReason(String currentNotes, String reason) {
        String normalizedReason = blankToNull(reason);
        if (normalizedReason == null) {
            normalizedReason = "Khách đổi ý không mua.";
        }
        String cancelNote = "Lý do hủy: " + normalizedReason;
        String normalizedCurrentNotes = blankToNull(currentNotes);
        return normalizedCurrentNotes == null ? cancelNote : normalizedCurrentNotes + "\n" + cancelNote;
    }

    private BigDecimal nonNegative(BigDecimal value, String message) {
        BigDecimal normalized = value == null ? BigDecimal.ZERO : value;
        if (normalized.compareTo(BigDecimal.ZERO) < 0) {
            throw new RuntimeException(message);
        }
        return normalized;
    }

    private String normalizePaymentMethod(String paymentMethod) {
        String normalized = paymentMethod == null || paymentMethod.isBlank()
                ? PAYMENT_METHOD_CASH
                : paymentMethod.trim().toUpperCase(Locale.ROOT);
        if (!PAYMENT_METHOD_CASH.equals(normalized) && !PAYMENT_METHOD_VNPAY.equals(normalized)) {
            throw new RuntimeException("Phương thức thanh toán không hợp lệ.");
        }
        return normalized;
    }
}
