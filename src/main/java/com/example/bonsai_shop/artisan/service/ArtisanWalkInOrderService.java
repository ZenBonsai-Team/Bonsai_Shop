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
    public static final String ORDER_TYPE_IN_PERSON = "IN_PERSON";
    public static final String ORDER_TYPE_ONLINE = "ONLINE";
    public static final String PRODUCT_AVAILABLE = "AVAILABLE";
    public static final String PRODUCT_RESERVED = "RESERVED";
    public static final String PRODUCT_SOLD = "SOLD";
    public static final String PAYMENT_METHOD_CASH = "CASH";
    public static final String PAYMENT_METHOD_VNPAY = "VNPAY";
    public static final String PAYMENT_TYPE_IN_PERSON = "IN_PERSON";

    private final ArtisanProductService artisanProductService;
    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final OrderLogRepository orderLogRepository;

    public List<Product> getAvailableProducts(String artisanEmail) {
        Integer artisanUserId = artisanProductService.getArtisanUser(artisanEmail).getUserId();
        return productRepository.findByCreatedByUserIdAndProductStatusOrderByCreatedAtDesc(
                artisanUserId,
                PRODUCT_AVAILABLE
        );
    }

    public Page<Order> getWalkInOrders(String artisanEmail, String status, int page, int size) {
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
        Product product = productRepository.findByProductIdAndCreatedByUserId(productId, artisanUser.getUserId())
                .orElseThrow(() -> new RuntimeException("KhÃ´ng tÃ¬m tháº¥y sáº£n pháº©m thuá»™c artisan nÃ y."));

        if (!PRODUCT_AVAILABLE.equalsIgnoreCase(product.getProductStatus())) {
            throw new RuntimeException("Chá»‰ cÃ³ thá»ƒ táº¡o in-person order cho sáº£n pháº©m Ä‘ang bÃ¡n.");
        }

        BigDecimal normalizedCraneFee = nonNegative(craneFee, "PhÃ­ cáº©u khÃ´ng Ä‘Æ°á»£c Ã¢m.");
        BigDecimal normalizedShippingFee = nonNegative(shippingFee, "PhÃ­ váº­n chuyá»ƒn khÃ´ng Ä‘Æ°á»£c Ã¢m.");
        String normalizedPaymentMethod = normalizePaymentMethod(paymentMethod);
        BigDecimal totalAmount = product.getPrice()
                .add(normalizedCraneFee)
                .add(normalizedShippingFee);

        Order order = Order.builder()
                .orderCode(generateOrderCode())
                .customerName(requireText(customerName, "Vui lÃ²ng nháº­p tÃªn khÃ¡ch in-person."))
                .customerPhone(requireText(customerPhone, "Vui lÃ²ng nháº­p sá»‘ Ä‘iá»‡n thoáº¡i khÃ¡ch in-person."))
                .customerEmail(blankToNull(customerEmail))
                .shippingAddress(requireText(shippingAddress, "Vui lÃ²ng nháº­p Ä‘á»‹a chá»‰ giao/nháº­n cÃ¢y."))
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

        Payment payment = Payment.builder()
                .order(order)
                .paymentMethod(normalizedPaymentMethod)
                .paymentStatus("PENDING")
                .paymentType(PAYMENT_TYPE_IN_PERSON)
                .amount(totalAmount)
                .build();
        paymentRepository.save(payment);

        int reserved = productRepository.reserveIfAvailable(product.getProductId());
        if (reserved == 0) {
            throw new RuntimeException("Sáº£n pháº©m Ä‘Ã£ Ä‘Æ°á»£c Ä‘áº·t hoáº·c khÃ´ng cÃ²n kháº£ dá»¥ng.");
        }
        product.setProductStatus(PRODUCT_RESERVED);
        Order savedOrder = orderRepository.save(order);
        log(savedOrder, artisanUser, "IN_PERSON_CREATE", null, STATUS_PENDING_PAYMENT);
        return savedOrder;
    }

    @Transactional
    public Order cancelWalkInOrder(String artisanEmail, Integer orderId, String reason) {
        User artisanUser = artisanProductService.getArtisanUser(artisanEmail);
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("KhÃ´ng tÃ¬m tháº¥y in-person order."));

        Product product = getSingleProduct(order);
        ensureOwnedByArtisan(product, artisanUser);
        if (!ORDER_TYPE_IN_PERSON.equalsIgnoreCase(order.getOrderType())) {
            throw new RuntimeException("Chá»‰ Ä‘Æ°á»£c há»§y in-person order.");
        }
        if (!STATUS_PENDING_PAYMENT.equalsIgnoreCase(order.getOrderStatus())) {
            throw new RuntimeException("Chá»‰ Ä‘Æ°á»£c há»§y order Ä‘ang chá» nháº­n tiá»n.");
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
                .orElseThrow(() -> new RuntimeException("KhÃ´ng tÃ¬m tháº¥y in-person order."));

        Product product = getSingleProduct(order);
        ensureOwnedByArtisan(product, artisanUser);
        if (!ORDER_TYPE_IN_PERSON.equalsIgnoreCase(order.getOrderType())) {
            throw new RuntimeException("Chá»‰ Ä‘Æ°á»£c cáº­p nháº­t in-person order.");
        }
        if (!STATUS_PENDING_PAYMENT.equalsIgnoreCase(order.getOrderStatus())) {
            throw new RuntimeException("Chá»‰ Ä‘Æ°á»£c cáº­p nháº­t order Ä‘ang chá» nháº­n tiá»n.");
        }

        BigDecimal normalizedCraneFee = nonNegative(craneFee, "PhÃ­ cáº©u khÃ´ng Ä‘Æ°á»£c Ã¢m.");
        BigDecimal normalizedShippingFee = nonNegative(shippingFee, "PhÃ­ váº­n chuyá»ƒn khÃ´ng Ä‘Æ°á»£c Ã¢m.");
        String normalizedPaymentMethod = normalizePaymentMethod(paymentMethod);
        BigDecimal totalAmount = getBasePrice(order, product)
                .add(normalizedCraneFee)
                .add(normalizedShippingFee);

        order.setCustomerName(requireText(customerName, "Vui lÃ²ng nháº­p tÃªn khÃ¡ch in-person."));
        order.setCustomerPhone(requireText(customerPhone, "Vui lÃ²ng nháº­p sá»‘ Ä‘iá»‡n thoáº¡i khÃ¡ch in-person."));
        order.setShippingAddress(requireText(shippingAddress, "Vui lÃ²ng nháº­p Ä‘á»‹a chá»‰ giao/nháº­n cÃ¢y."));
        order.setCustomerEmail(blankToNull(customerEmail));
        order.setCraneFee(normalizedCraneFee);
        order.setShippingFee(normalizedShippingFee);
        order.setTotalAmount(totalAmount);
        order.setNotes(blankToNull(notes));

        Payment payment = getFirstPayment(order);
        if (payment == null) {
            payment = Payment.builder()
                    .order(order)
                    .paymentStatus("PENDING")
                    .paymentType(PAYMENT_TYPE_IN_PERSON)
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
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("KhÃ´ng tÃ¬m tháº¥y in-person order."));

        Product product = getSingleProduct(order);
        ensureOwnedByArtisan(product, artisanUser);
        if (!STATUS_PENDING_PAYMENT.equalsIgnoreCase(order.getOrderStatus())) {
            throw new RuntimeException("Chá»‰ xÃ¡c nháº­n thanh toÃ¡n cho in-person order Ä‘ang chá» tiá»n.");
        }

        String oldStatus = order.getOrderStatus();
        Payment payment = getFirstPayment(order);
        if (payment == null) {
            payment = Payment.builder()
                    .order(order)
                    .paymentMethod(PAYMENT_METHOD_CASH)
                    .paymentType(PAYMENT_TYPE_IN_PERSON)
                    .amount(order.getTotalAmount())
                    .build();
        }
        payment.setPaymentStatus("SUCCESS");
        payment.setPaymentDate(LocalDateTime.now());
        paymentRepository.save(payment);

        order.setOrderStatus(STATUS_COMPLETED);
        product.setProductStatus(PRODUCT_SOLD);
        productRepository.save(product);
        Order savedOrder = orderRepository.save(order);
        log(savedOrder, artisanUser, "IN_PERSON_PAYMENT_CONFIRMED", oldStatus, STATUS_COMPLETED);
        return savedOrder;
    }

    private Product getSingleProduct(Order order) {
        if (order.getOrderDetails() == null || order.getOrderDetails().isEmpty()) {
            throw new RuntimeException("Order chÆ°a cÃ³ sáº£n pháº©m.");
        }
        return order.getOrderDetails().get(0).getProduct();
    }

    private void ensureOwnedByArtisan(Product product, User artisanUser) {
        if (product.getCreatedBy() == null || !artisanUser.getUserId().equals(product.getCreatedBy().getUserId())) {
            throw new RuntimeException("Order khong thuoc artisan nay.");
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

    private String appendCancelReason(String currentNotes, String reason) {
        String normalizedReason = blankToNull(reason);
        if (normalizedReason == null) {
            normalizedReason = "KhÃ¡ch Ä‘á»•i Ã½ khÃ´ng mua.";
        }
        String cancelNote = "LÃ½ do há»§y: " + normalizedReason;
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

