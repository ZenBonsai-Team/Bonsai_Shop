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
// Service xử lý nghiệp vụ bán hàng trực tiếp tại vườn.
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
    private static final int CUSTOMER_NAME_MIN_LENGTH = 3;
    private static final int CUSTOMER_NAME_MAX_LENGTH = 50;
    private static final int CUSTOMER_EMAIL_MAX_LENGTH = 100;
    private static final int SHIPPING_ADDRESS_MAX_LENGTH = 255;
    private static final int NOTES_MAX_LENGTH = 500;
    private static final int ORDER_NOTE_MAX_LENGTH = 400;
    private static final String DEFAULT_MANUAL_CANCEL_REASON = "Khách đổi ý không mua.";
    private static final BigDecimal MAX_FEE_AMOUNT = new BigDecimal("200000000");
    private static final BigDecimal MAX_MONEY_AMOUNT = new BigDecimal("999999999999.99");

    private final ArtisanProductService artisanProductService;
    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final OrderLogRepository orderLogRepository;
    private final MailService mailService;
    private final FinancialLedgerService financialLedgerService;

    // Lấy sản phẩm của artisan còn khả dụng để lập đơn tại vườn.
    public List<Product> getAvailableProducts(String artisanEmail) {
        Integer artisanUserId = artisanProductService.getArtisanUser(artisanEmail).getUserId();
        return productRepository.findByCreatedByUserIdAndProductStatusOrderByCreatedAtDesc(
                artisanUserId,
                PRODUCT_AVAILABLE
        );
    }

    // Lấy danh sách đơn tại vườn theo trạng thái và phân trang.
    public Page<Order> getInPersonOrders(String artisanEmail, String status, String keyword, int page, int size) {
        Integer artisanUserId = artisanProductService.getArtisanUser(artisanEmail).getUserId();
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.max(size, 1));
        return orderRepository.searchByArtisanUserIdAndTypeAndStatus(
                artisanUserId,
                ORDER_TYPE_IN_PERSON,
                status == null || status.isBlank() ? "ALL" : status,
                keyword == null ? "" : keyword.trim(),
                pageable
        );
    }

    @Transactional
    // Tạo đơn tại vườn, giữ chỗ sản phẩm và tạo payment tương ứng.
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
        validateMoney(totalAmount, "Tổng tiền đơn hàng không được vượt quá 999.999.999.999 VNĐ.");

        Order order = Order.builder()
                .orderCode(generateOrderCode())
                .customerName(requireCustomerName(customerName))
                .customerPhone(requirePhone(customerPhone))
                .customerEmail(requireEmail(customerEmail))
                .shippingAddress(requireShippingAddress(shippingAddress))
                .orderDate(LocalDateTime.now())
                .totalAmount(totalAmount)
                .depositAmount(BigDecimal.ZERO)
                .craneFee(normalizedCraneFee)
                .shippingFee(normalizedShippingFee)
                .orderStatus(STATUS_PENDING_PAYMENT)
                .orderType(ORDER_TYPE_IN_PERSON)
                .notes(optionalText(notes, ORDER_NOTE_MAX_LENGTH, "Ghi chú không được vượt quá 400 ký tự."))
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
    // Hủy đơn tại vườn, trả sản phẩm về AVAILABLE nếu còn giữ chỗ.
    public Order cancelInPersonOrder(String artisanEmail, Integer orderId) {
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
        order.setNotes(appendCancelReason(order.getNotes()));

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
    // Cập nhật thông tin đơn khi đơn còn chờ thanh toán.
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
        validateMoney(totalAmount, "Tổng tiền đơn hàng không được vượt quá 999.999.999.999 VNĐ.");

        order.setCustomerName(requireCustomerName(customerName));
        order.setCustomerPhone(requirePhone(customerPhone));
        order.setShippingAddress(requireShippingAddress(shippingAddress));
        order.setCustomerEmail(requireEmail(customerEmail));
        order.setCraneFee(normalizedCraneFee);
        order.setShippingFee(normalizedShippingFee);
        order.setTotalAmount(totalAmount);
        order.setNotes(optionalText(notes, ORDER_NOTE_MAX_LENGTH, "Ghi chú không được vượt quá 400 ký tự."));

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
    // Ghi nhận thanh toán, chuyển đơn hoàn tất và ghi ledger doanh thu.
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

    // Lấy sản phẩm duy nhất từ order detail của đơn tại vườn.
    private Product getSingleProduct(Order order) {
        if (order.getOrderDetails() == null || order.getOrderDetails().isEmpty()) {
            throw new RuntimeException("Order chưa có sản phẩm.");
        }
        return order.getOrderDetails().get(0).getProduct();
    }

    // Đảm bảo sản phẩm thuộc artisan đang thao tác.
    private void ensureOwnedByArtisan(Product product, User artisanUser) {
        if (product.getCreatedBy() == null || !artisanUser.getUserId().equals(product.getCreatedBy().getUserId())) {
            throw new RuntimeException("Order không thuộc artisan này.");
        }
    }

    // Xác định giá gốc của sản phẩm trong đơn.
    private BigDecimal getBasePrice(Order order, Product product) {
        OrderDetail detail = order.getOrderDetails().get(0);
        if (detail.getPriceAtPurchase() != null) {
            return detail.getPriceAtPurchase();
        }
        return product.getPrice() == null ? BigDecimal.ZERO : product.getPrice();
    }

    // Ghi log thay đổi trạng thái đơn.
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

    // Sinh mã đơn tại vườn không trùng.
    private String generateOrderCode() {
        String orderCode;
        do {
            orderCode = "BSMS-" + randomSixDigits();
        } while (orderRepository.findByOrderCode(orderCode).isPresent());
        return orderCode;
    }

    // Sinh hậu tố số ngẫu nhiên cho mã đơn.
    private String randomSixDigits() {
        return String.valueOf(ThreadLocalRandom.current().nextInt(100000, 1000000));
    }

    // Validate text bắt buộc với thông báo lỗi đơn giản.
    private String requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new RuntimeException(message);
        }
        return value.trim();
    }

    // Validate text bắt buộc kèm giới hạn độ dài.
    private String requireText(String value, String requiredMessage, int maxLength, String lengthMessage) {
        String normalized = requireText(value, requiredMessage);
        if (normalized.length() > maxLength) {
            throw new RuntimeException(lengthMessage);
        }
        return normalized;
    }

    // Chuẩn hóa text tùy chọn và kiểm tra độ dài.
    private String optionalText(String value, int maxLength, String lengthMessage) {
        String normalized = blankToNull(value);
        if (normalized != null && normalized.length() > maxLength) {
            throw new RuntimeException(lengthMessage);
        }
        return normalized;
    }

    // Đổi chuỗi rỗng thành null.
    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    // Validate email khách hàng.
    private String requireEmail(String value) {
        String email = requireText(value, "Vui lòng nhập email khách in-person.", CUSTOMER_EMAIL_MAX_LENGTH, "Email khách không được vượt quá 100 ký tự.");
        if (!email.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) {
            throw new RuntimeException("Email khách in-person không hợp lệ.");
        }
        return email;
    }

    // Validate tên khách hàng.
    private String requireCustomerName(String value) {
        String name = requireText(value, "Vui l\u00f2ng nh\u1eadp t\u00ean kh\u00e1ch in-person.");
        if (name.length() < CUSTOMER_NAME_MIN_LENGTH || name.length() > CUSTOMER_NAME_MAX_LENGTH) {
            throw new RuntimeException("T\u00ean kh\u00e1ch ph\u1ea3i c\u00f3 t\u1eeb 3 \u0111\u1ebfn 50 k\u00fd t\u1ef1.");
        }
        return name;
    }

    // Validate số điện thoại khách hàng.
    private String requirePhone(String value) {
        String phone = requireText(value, "Vui lòng nhập số điện thoại khách in-person.");
        if (!phone.matches("^0[0-9]{9,10}$")) {
            throw new RuntimeException("Số điện thoại phải gồm 10-11 chữ số và bắt đầu bằng 0.");
        }
        return phone;
    }

    // Validate địa chỉ giao/nhận cây.
    private String requireShippingAddress(String value) {
        String address = requireText(value, "Vui lòng nhập địa chỉ giao/nhận cây.", SHIPPING_ADDRESS_MAX_LENGTH, "Địa chỉ giao/nhận cây không được vượt quá 255 ký tự.");
        if (!address.matches("^[\\p{L}\\p{M}\\p{N}\\s,./()\\-]+$")) {
            throw new RuntimeException("Địa chỉ chỉ được chứa chữ, số, khoảng trắng và các dấu , . / - ( ).");
        }
        return address;
    }

    // Gắn lý do hủy vào ghi chú đơn.
    private String appendCancelReason(String currentNotes) {
        String normalizedReason = DEFAULT_MANUAL_CANCEL_REASON;
        String cancelNote = "Lý do hủy: " + normalizedReason;
        String normalizedCurrentNotes = blankToNull(currentNotes);
        String finalNotes = normalizedCurrentNotes == null ? cancelNote : normalizedCurrentNotes + "\n" + cancelNote;
        if (finalNotes.length() > NOTES_MAX_LENGTH) {
            int maxCurrentNotesLength = NOTES_MAX_LENGTH - cancelNote.length() - 1;
            String truncatedCurrentNotes = normalizedCurrentNotes.substring(0, Math.max(maxCurrentNotesLength, 0)).trim();
            return truncatedCurrentNotes.isBlank() ? cancelNote : truncatedCurrentNotes + "\n" + cancelNote;
        }
        return finalNotes;
    }

    // Chuẩn hóa số tiền null thành 0 và chặn giá trị âm.
    private BigDecimal nonNegative(BigDecimal value, String message) {
        BigDecimal normalized = value == null ? BigDecimal.ZERO : value;
        if (normalized.compareTo(BigDecimal.ZERO) < 0) {
            throw new RuntimeException(message);
        }
        if (normalized.compareTo(MAX_FEE_AMOUNT) > 0) {
            throw new RuntimeException("Phí không được vượt quá 200.000.000 VNĐ.");
        }
        return normalized;
    }

    // Kiểm tra số tiền không vượt ngưỡng hệ thống.
    private void validateMoney(BigDecimal value, String message) {
        if (value != null && value.compareTo(MAX_MONEY_AMOUNT) > 0) {
            throw new RuntimeException(message);
        }
    }

    // Chuẩn hóa phương thức thanh toán về CASH hoặc VNPAY.
    private String normalizePaymentMethod(String paymentMethod) {
        String normalized = paymentMethod == null || paymentMethod.isBlank()
                ? PAYMENT_METHOD_CASH
                : paymentMethod.trim().toUpperCase(Locale.ROOT);
        if (!PAYMENT_METHOD_CASH.equals(normalized) && !PAYMENT_METHOD_VNPAY.equals(normalized)) {
            throw new RuntimeException("Phương thức thanh toán không hợp lệ.");
        }
        return normalized;
    }

    // Lấy payment đầu tiên gắn với đơn.
    private Payment getFirstPayment(Order order) {
        if (order.getPayments() != null && !order.getPayments().isEmpty()) {
            return order.getPayments().get(0);
        }
        return paymentRepository.findTopByOrderOrderIdAndPaymentStatusOrderByPaymentIdDesc(order.getOrderId(), "PENDING").orElse(null);
    }
}

