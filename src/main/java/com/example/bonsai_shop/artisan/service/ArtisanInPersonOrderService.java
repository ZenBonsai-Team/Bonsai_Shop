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
import com.example.bonsai_shop.product.service.MailService;
import com.example.bonsai_shop.finance.service.FinancialLedgerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
@Slf4j
public class ArtisanInPersonOrderService {

    public static final String STATUS_PENDING_PAYMENT = "PENDING_PAYMENT";
    public static final String STATUS_COMPLETED = "COMPLETED";
    public static final String STATUS_CANCELLED = "CANCELLED";
    public static final String ORDER_TYPE_IN_PERSON = "IN_PERSON";
    public static final String ORDER_TYPE_ONLINE = "ONLINE";
    public static final String PRODUCT_AVAILABLE = "AVAILABLE";
    public static final String PRODUCT_RESERVED = "RESERVED";
    public static final String PRODUCT_SOLD = "SOLD";
    public static final String PAYMENT_METHOD_CASH = "CASH";
    public static final String PAYMENT_METHOD_VNPAY = "VNPAY";
    public static final String PAYMENT_TYPE_FULL_PAYMENT = "FULL_PAYMENT";

    private final ArtisanProductService artisanProductService;
    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final OrderLogRepository orderLogRepository;
    private final MailService mailService;
    private final FinancialLedgerService financialLedgerService;

    public List<Product> getAvailableProducts(String artisanEmail) {
        Integer artisanUserId = artisanProductService.getArtisanUser(artisanEmail).getUserId();
        return productRepository.findByCreatedByUserIdAndProductStatusOrderByCreatedAtDesc(
                artisanUserId,
                PRODUCT_AVAILABLE
        );
    }

    public Page<Order> getInPersonOrders(String artisanEmail, String status, int page, int size) {
        Integer artisanUserId = artisanProductService.getArtisanUser(artisanEmail).getUserId();
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.max(size, 1));
        return orderRepository.findByArtisanUserIdAndTypeAndStatus(
                artisanUserId,
                ORDER_TYPE_IN_PERSON,
                status == null || status.isBlank() ? "ALL" : status,
                pageable
        );
    }

    @Transactional
    public Order createInPersonOrder(String artisanEmail,
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
        Product product = productRepository.findByProductIdAndCreatedByUserId(productId, artisanUser.getUserId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy sản phẩm thuộc artisan này."));

        if (!PRODUCT_AVAILABLE.equalsIgnoreCase(product.getProductStatus())) {
            throw new RuntimeException("Chỉ có thể tạo in-person order cho sản phẩm đang bán.");
        }

        BigDecimal normalizedCraneFee = nonNegative(craneFee, "Phí cẩu không được âm.");
        BigDecimal normalizedShippingFee = nonNegative(shippingFee, "Phí vận chuyển không được âm.");
        String normalizedPaymentMethod = normalizePaymentMethod(paymentMethod);
        BigDecimal totalAmount = product.getPrice()
                .add(normalizedCraneFee)
                .add(normalizedShippingFee);

        Order order = Order.builder()
                .orderCode(generateOrderCode())
                .customerName(requireText(customerName, "Vui lòng nhập tên khách in-person."))
                .customerPhone(requireText(customerPhone, "Vui lòng nhập số điện thoại khách in-person."))
                .customerEmail(requireEmail(customerEmail))
                .shippingAddress(requireText(shippingAddress, "Vui lòng nhập địa chỉ giao/nhận cây."))
                .orderDate(LocalDateTime.now())
                .totalAmount(totalAmount)
                .depositAmount(BigDecimal.ZERO)
                .craneFee(normalizedCraneFee)
                .shippingFee(normalizedShippingFee)
                .orderStatus(STATUS_PENDING_PAYMENT)
                .orderType(ORDER_TYPE_IN_PERSON)
                .notes(blankToNull(notes))
                .build();

        OrderDetail detail = OrderDetail.builder()
                .order(order)
                .product(product)
                .priceAtPurchase(product.getPrice())
                .build();
        order.setOrderDetails(List.of(detail));

        int reserved = productRepository.reserveIfAvailable(product.getProductId());
        if (reserved == 0) {
            throw new RuntimeException("Sản phẩm đã được đặt hoặc không còn khả dụng.");
        }
        product.setProductStatus(PRODUCT_RESERVED);
        Order savedOrder = orderRepository.save(order);

        Payment payment = Payment.builder()
                .order(savedOrder)
                .paymentMethod(normalizedPaymentMethod)
                .paymentStatus("PENDING")
                .paymentType(PAYMENT_TYPE_FULL_PAYMENT)
                .amount(totalAmount)
                .build();
        paymentRepository.save(payment);
        log(savedOrder, artisanUser, "IN_PERSON_CREATE", null, STATUS_PENDING_PAYMENT);
        return savedOrder;
    }

    @Transactional
    public Order cancelInPersonOrder(String artisanEmail, Integer orderId, String reason) {
        User artisanUser = artisanProductService.getArtisanUser(artisanEmail);
        Order order = orderRepository.findByIdWithDetailsForUpdate(orderId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy in-person order."));

        Product product = getSingleProduct(order);
        ensureOwnedByArtisan(product, artisanUser);
        if (!ORDER_TYPE_IN_PERSON.equalsIgnoreCase(order.getOrderType())) {
            throw new RuntimeException("Chỉ được hủy in-person order.");
        }
        if (!STATUS_PENDING_PAYMENT.equalsIgnoreCase(order.getOrderStatus())) {
            throw new RuntimeException("Chỉ được hủy order đang chờ nhận tiền.");
        }

        String oldStatus = order.getOrderStatus();
        order.setOrderStatus(STATUS_CANCELLED);
        order.setNotes(appendCancelReason(order.getNotes(), reason));

        Payment payment = getFirstPayment(order);
        if (payment != null) {
            payment.setPaymentStatus(STATUS_CANCELLED);
            paymentRepository.save(payment);
        }

        product.setProductStatus(PRODUCT_AVAILABLE);
        productRepository.save(product);

        Order savedOrder = orderRepository.save(order);
        log(savedOrder, artisanUser, "IN_PERSON_CANCEL", oldStatus, STATUS_CANCELLED);
        return savedOrder;
    }

    @Transactional
    public Order updateInPersonOrder(String artisanEmail,
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
        Order order = orderRepository.findByIdWithDetailsForUpdate(orderId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy in-person order."));

        Product product = getSingleProduct(order);
        ensureOwnedByArtisan(product, artisanUser);
        if (!ORDER_TYPE_IN_PERSON.equalsIgnoreCase(order.getOrderType())) {
            throw new RuntimeException("Chỉ được cập nhật in-person order.");
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

        order.setCustomerName(requireText(customerName, "Vui lòng nhập tên khách in-person."));
        order.setCustomerPhone(requireText(customerPhone, "Vui lòng nhập số điện thoại khách in-person."));
        order.setShippingAddress(requireText(shippingAddress, "Vui lòng nhập địa chỉ giao/nhận cây."));
        order.setCustomerEmail(requireEmail(customerEmail));
        order.setCraneFee(normalizedCraneFee);
        order.setShippingFee(normalizedShippingFee);
        order.setTotalAmount(totalAmount);
        order.setNotes(blankToNull(notes));

        Payment payment = getFirstPayment(order);
        if (payment == null) {
            payment = Payment.builder()
                    .order(order)
                    .paymentStatus("PENDING")
                    .paymentType(PAYMENT_TYPE_FULL_PAYMENT)
                    .build();
        }
        payment.setPaymentMethod(normalizedPaymentMethod);
        payment.setAmount(totalAmount);
        paymentRepository.save(payment);

        Order savedOrder = orderRepository.save(order);
        log(savedOrder, artisanUser, "IN_PERSON_UPDATE", STATUS_PENDING_PAYMENT, STATUS_PENDING_PAYMENT);
        return savedOrder;
    }

    @Transactional
    public Order confirmPayment(String artisanEmail, Integer orderId) {
        User artisanUser = artisanProductService.getArtisanUser(artisanEmail);
        Order order = orderRepository.findByIdWithDetailsForUpdate(orderId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy in-person order."));

        Product product = getSingleProduct(order);
        ensureOwnedByArtisan(product, artisanUser);
        if (STATUS_COMPLETED.equalsIgnoreCase(order.getOrderStatus())) {
            return order;
        }
        if (!STATUS_PENDING_PAYMENT.equalsIgnoreCase(order.getOrderStatus())) {
            throw new RuntimeException("Chỉ xác nhận thanh toán cho in-person order đang chờ tiền.");
        }

        String oldStatus = order.getOrderStatus();
        Payment payment = getFirstPayment(order);
        if (payment == null) {
            payment = Payment.builder()
                    .order(order)
                    .paymentMethod(PAYMENT_METHOD_CASH)
                    .paymentType(PAYMENT_TYPE_FULL_PAYMENT)
                    .amount(order.getTotalAmount())
                    .build();
        }
        payment.setPaymentStatus("SUCCESS");
        payment.setPaymentDate(LocalDateTime.now());
        paymentRepository.save(payment);

        LocalDateTime completedAt = LocalDateTime.now();
        order.setOrderStatus(STATUS_COMPLETED);
        order.setCompletedAt(completedAt);
        product.setProductStatus(PRODUCT_SOLD);
        productRepository.save(product);
        Order savedOrder = orderRepository.save(order);
        if (financialLedgerService.recordCompletedOrderRevenueIfAbsent(savedOrder, artisanUser, completedAt) != null) {
            log(savedOrder, artisanUser, "COMPLETED_ORDER_REVENUE_RECORDED", STATUS_COMPLETED, STATUS_COMPLETED);
        }
        log(savedOrder, artisanUser, "IN_PERSON_PAYMENT_CONFIRMED", oldStatus, STATUS_COMPLETED);
        try {
            mailService.sendInPersonOrderPaidEmail(savedOrder);
        } catch (Exception e) {
            log.warn("Không thể gửi email xác nhận thanh toán in-person order {}: {}", savedOrder.getOrderCode(), e.getMessage());
        }
        return savedOrder;
    }

    private Product getSingleProduct(Order order) {
        if (order.getOrderDetails() == null || order.getOrderDetails().isEmpty()) {
            throw new RuntimeException("Order chưa có sản phẩm.");
        }
        return order.getOrderDetails().get(0).getProduct();
    }

    private void ensureOwnedByArtisan(Product product, User artisanUser) {
        if (product.getCreatedBy() == null || !artisanUser.getUserId().equals(product.getCreatedBy().getUserId())) {
            throw new RuntimeException("Order không thuộc artisan này.");
        }
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

    private String requireEmail(String value) {
        String email = requireText(value, "Vui lòng nhập email khách in-person.");
        if (!email.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) {
            throw new RuntimeException("Email khách in-person không hợp lệ.");
        }
        return email;
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

    private Payment getFirstPayment(Order order) {
        if (order.getPayments() != null && !order.getPayments().isEmpty()) {
            return order.getPayments().get(0);
        }
        return paymentRepository.findTopByOrderOrderIdAndPaymentStatusOrderByPaymentIdDesc(order.getOrderId(), "PENDING").orElse(null);
    }
}

