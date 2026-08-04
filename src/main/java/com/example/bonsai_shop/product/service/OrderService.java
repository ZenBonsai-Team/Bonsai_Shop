package com.example.bonsai_shop.product.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.stream.Collectors;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.bonsai_shop.config.VNPayConfig;
import com.example.bonsai_shop.entity.CartItem;
import com.example.bonsai_shop.entity.Order;
import com.example.bonsai_shop.entity.OrderDetail;
import com.example.bonsai_shop.entity.OrderHandling;
import com.example.bonsai_shop.entity.OrderLog;
import com.example.bonsai_shop.entity.Payment;
import com.example.bonsai_shop.entity.Product;
import com.example.bonsai_shop.entity.User;
import com.example.bonsai_shop.finance.enums.FaultParty;
import com.example.bonsai_shop.entity.FinancialLedger;
import com.example.bonsai_shop.finance.service.FinancialLedgerService;
import com.example.bonsai_shop.product.dto.PurchaseOrderRequestDTO;
import com.example.bonsai_shop.product.enums.PaymentMethod;
import com.example.bonsai_shop.product.enums.PaymentType;
import com.example.bonsai_shop.product.event.OrderCreatedEvent;
import com.example.bonsai_shop.product.event.OrderPaidEvent;
import com.example.bonsai_shop.product.event.OrderRejectedEvent;
import com.example.bonsai_shop.product.event.OrderVerifiedEvent;
import com.example.bonsai_shop.product.repository.OrderHandlingRepository;
import com.example.bonsai_shop.product.repository.OrderLogRepository;
import com.example.bonsai_shop.product.repository.OrderRepository;
import com.example.bonsai_shop.product.repository.PaymentRepository;
import com.example.bonsai_shop.product.repository.ProductRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private static final String STATUS_PENDING_PAYMENT = "PENDING_PAYMENT";
    private static final String STATUS_PENDING = "PENDING";
    private static final BigDecimal ZERO = BigDecimal.ZERO;

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final OrderLogRepository orderLogRepository;
    private final OrderHandlingRepository orderHandlingRepository;
    private final PaymentRepository paymentRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final MailService mailService;
    private final CartService cartService;
    private final FinancialLedgerService financialLedgerService;

    @Transactional(readOnly = true)
    public Page<Order> getFilteredOrders(String search, String status, String sort, int page, int limit) {
        Sort springSort = resolveSort(sort);
        Pageable pageable = PageRequest.of(page - 1, limit, springSort);
        return orderRepository.searchOrdersForModerator(resolveStatusFilter(status), search, pageable);
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
        return orderRepository.searchMyOrders(moderatorId, resolveStatusFilter(status), search, pageable);
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

    private List<String> resolveStatusFilter(String status) {
        if (status == null || status.isBlank() || "ALL".equalsIgnoreCase(status)) {
            return null;
        }
        return List.of(status);
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
        kpis.put("approved", orderRepository.countByOrderStatus(STATUS_PENDING_PAYMENT));
        kpis.put("paid", orderRepository.countByOrderStatus("PAID"));
        kpis.put("cancelled", orderRepository.countByOrderStatus("CANCELLED"));
        kpis.put("rejected", orderRepository.countByOrderStatus("CANCELLED"));
        return kpis;
    }

    @Transactional(readOnly = true)
    public Map<String, Long> getModeratorPersonalKPIs(Integer moderatorId) {
        Map<String, Long> kpis = new HashMap<>();
        kpis.put("total", orderRepository.countByAssignedToUserId(moderatorId));
        kpis.put("pending", orderRepository.countByAssignedToUserIdAndOrderStatus(moderatorId, "PENDING"));
        kpis.put("approved", orderRepository.countByAssignedToUserIdAndOrderStatus(moderatorId, STATUS_PENDING_PAYMENT));
        kpis.put("paid", orderRepository.countByAssignedToUserIdAndOrderStatus(moderatorId, "PAID"));
        kpis.put("rejected", orderRepository.countByAssignedToUserIdAndOrderStatus(moderatorId, "CANCELLED"));
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
        if (order == null || !STATUS_PENDING.equalsIgnoreCase(order.getOrderStatus())) {
            return false;
        }

        // Kiểm tra quyền sở hữu đơn hàng
        if (order.getAssignedTo() == null || !order.getAssignedTo().getUserId().equals(moderator.getUserId())) {
            throw new SecurityException("Bạn không có quyền duyệt đơn hàng này!");
        }

        String oldStatus = order.getOrderStatus();
        
        // Tính chính xác giá gốc các cây trong đơn hàng (không bao gồm phụ phí)
        BigDecimal treePrice = resolveTreePrice(order);
        BigDecimal normalizedCraneFee = normalizeNonNegativeAmount(craneFee, "Phí cẩu");
        BigDecimal normalizedShippingFee = normalizeNonNegativeAmount(shippingFee, "Phí vận chuyển");

        order.setCraneFee(normalizedCraneFee);
        order.setShippingFee(normalizedShippingFee);

        // Tong gia tri thuc te cua toan bo don hang = Tree Price + Crane Fee + Shipping Fee
        BigDecimal newTotal = treePrice.add(normalizedCraneFee).add(normalizedShippingFee);
        order.setTotalAmount(newTotal);

        List<Payment> existingPayments = paymentRepository.findByOrderOrderIdOrderByPaymentIdAsc(order.getOrderId());
        
        boolean isDepositFlow = existingPayments.stream().anyMatch(p -> 
                PaymentType.DEPOSIT.name().equalsIgnoreCase(p.getPaymentType()) ||
                "DEPOSIT".equalsIgnoreCase(p.getPaymentMethod()) ||
                "COD".equalsIgnoreCase(p.getPaymentMethod())
        );

        if (isDepositFlow) {
            if (depositAmount == null || depositAmount.compareTo(ZERO) <= 0) {
                throw new IllegalArgumentException("Vui lòng nhập số tiền đặt cọc.");
            }
            validateWholeNumberAmount(depositAmount, "Tiền đặt cọc");
            if (depositAmount.compareTo(newTotal) > 0) {
                throw new IllegalArgumentException("Số tiền đặt cọc không được vượt quá tổng giá trị cây.");
            }
            order.setDepositAmount(depositAmount);

            // Số tiền cần thanh toán lần 1 = deposit + phí cẩu + phí ship
            BigDecimal amountToPay = depositAmount;

            Payment depositPayment = existingPayments.stream()
                    .filter(p -> PaymentType.DEPOSIT.name().equalsIgnoreCase(p.getPaymentType()))
                    .findFirst()
                    .orElse(null);

            if (depositPayment != null) {
                depositPayment.setAmount(amountToPay);
                paymentRepository.save(depositPayment);
            } else {
                depositPayment = Payment.builder()
                        .order(order)
                        .paymentType(PaymentType.DEPOSIT.name())
                        .paymentMethod(PaymentMethod.VNPAY.name())
                        .paymentStatus("PENDING")
                        .amount(amountToPay)
                        .build();
                paymentRepository.save(depositPayment);
            }
        } else {
            // Thanh toán toàn bộ 1 lần = treePrice + phí cẩu + phí ship
            order.setDepositAmount(ZERO);

            Payment fullPayment = existingPayments.stream()
                    .filter(p -> PaymentType.FULL_PAYMENT.name().equalsIgnoreCase(p.getPaymentType()))
                    .findFirst()
                    .orElse(null);

            if (fullPayment != null) {
                fullPayment.setAmount(newTotal);
                paymentRepository.save(fullPayment);
            } else {
                fullPayment = Payment.builder()
                        .order(order)
                        .paymentType(PaymentType.FULL_PAYMENT.name())
                        .paymentMethod(PaymentMethod.VNPAY.name())
                        .paymentStatus("PENDING")
                        .amount(newTotal)
                        .build();
                paymentRepository.save(fullPayment);
            }
        }

        order.setOrderStatus(STATUS_PENDING_PAYMENT);
        orderRepository.save(order);

        OrderLog log = OrderLog.builder()
                .order(order)
                .actionBy(moderator)
                .actionType("VERIFY")
                .fromStatus(oldStatus)
                .toStatus(STATUS_PENDING_PAYMENT)
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
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("Lý do từ chối là bắt buộc.");
        }
        Order order = orderRepository.findByOrderCodeWithDetails(orderCode).orElse(null);
        if (order == null || !"PENDING".equalsIgnoreCase(order.getOrderStatus())) {
            return false;
        }

        // Kiểm tra quyền sở hữu đơn hàng
        if (order.getAssignedTo() == null || !order.getAssignedTo().getUserId().equals(moderator.getUserId())) {
            throw new SecurityException("Bạn không có quyền hủy đơn hàng này!");
        }

        String oldStatus = order.getOrderStatus();
        order.setOrderStatus("CANCELLED");
        order.setNotes("Hủy đơn với lý do: " + reason);
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
                .toStatus("CANCELLED")
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
        if (order == null || (!"PENDING".equalsIgnoreCase(order.getOrderStatus()) && !STATUS_PENDING_PAYMENT.equalsIgnoreCase(order.getOrderStatus()))) {
            return false;
        }

        String oldStatus = order.getOrderStatus();
        order.setDepositAmount(depositAmount);
        order.setOrderStatus("DEPOSITED");
        orderRepository.save(order);

        OrderLog logEntry = OrderLog.builder()
                .order(order)
                .actionBy(moderator)
                .actionType("DEPOSIT")
                .fromStatus(oldStatus)
                .toStatus("DEPOSITED")
                .actionAt(LocalDateTime.now())
                .build();
        orderLogRepository.save(logEntry);

        try {
            mailService.sendOrderDepositedEmail(order);
        } catch (Exception e) {
            log.warn("Không thể gửi email thông báo đặt cọc đơn {}: {}", orderCode, e.getMessage());
        }

        return true;
    }

    // =========================================================================
    // VALIDATION & PRODUCT RESOLUTION — Refactored (Phương Án B)
    // =========================================================================

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

    public List<Product> getProductsByIds(List<Integer> productIds) {
        if (productIds == null || productIds.isEmpty()) {
            return new ArrayList<>();
        }
        return productRepository.findAllById(productIds);
    }

    public List<Product> loadProductsForOrder(PurchaseOrderRequestDTO dto, User customer) {
        return resolveProductsToBuy(dto, customer);
    }

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
        List<Product> productsToBuy = resolveProductsToBuy(dto, customer);

        if (productsToBuy.isEmpty()) {
            throw new IllegalArgumentException("Giỏ hàng của bạn đang trống! Vui lòng chọn sản phẩm trước.");
        }

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

        // Khởi tạo bản ghi Payment PENDING ban đầu duy nhất theo 1-N Model
        String rawMethod = dto.getPaymentMethod() != null ? dto.getPaymentMethod() : PaymentMethod.DEPOSIT.name();
        String pType = (PaymentMethod.DEPOSIT.name().equalsIgnoreCase(rawMethod) || "COD".equalsIgnoreCase(rawMethod)) 
                ? PaymentType.DEPOSIT.name() 
                : PaymentType.FULL_PAYMENT.name();

        Payment initialPayment = Payment.builder()
                .order(savedOrder)
                .paymentType(pType)
                .paymentMethod(rawMethod)
                .paymentStatus("PENDING")
                .amount(totalAmount)
                .build();
        paymentRepository.save(initialPayment);

        if (customer != null) {
            cartService.clearCart(customer.getUserId());
        }

        initializeOrderDetails(savedOrder);
        eventPublisher.publishEvent(new OrderCreatedEvent(savedOrder));
        return savedOrder;
    }

    // =========================================================================
    // PAYMENT PROCESSING REFACTOR (1-N Model)
    // =========================================================================

    @Transactional
    public boolean processPaymentSuccess(String orderCode) {
        Order order = orderRepository.findByOrderCode(orderCode).orElse(null);
        if (order == null) {
            return false;
        }

        if ("PAID".equalsIgnoreCase(order.getOrderStatus())) {
            recordFullPaymentRevenueLedgerIfPossible(order, LocalDateTime.now());
            return true;
        }

        // Tìm Payment record PENDING gần nhất
        Payment pendingPayment = paymentRepository
                .findTopByOrderOrderIdAndPaymentStatusOrderByPaymentIdDesc(order.getOrderId(), "PENDING")
                .orElse(null);

        if (pendingPayment != null) {
            pendingPayment.setPaymentStatus("SUCCESS");
            pendingPayment.setPaymentDate(LocalDateTime.now());
            paymentRepository.save(pendingPayment);

            if (PaymentType.DEPOSIT.name().equalsIgnoreCase(pendingPayment.getPaymentType())) {
                order.setOrderStatus("DEPOSITED");
                orderRepository.save(order);
                try {
                    mailService.sendOrderDepositedEmail(order);
                } catch (Exception e) {
                    log.warn("Không thể gửi email thông báo đặt cọc đơn {}: {}", orderCode, e.getMessage());
                }
                return true;
            }
        }

        // Trường hợp FULL_PAYMENT hoặc fallback
        order.setOrderStatus("PAID");
        orderRepository.save(order);
        markProductsAsSold(order);
        recordFullPaymentRevenueLedgerIfPossible(order, LocalDateTime.now());

        eventPublisher.publishEvent(new OrderPaidEvent(order));
        return true;
    }

    @Transactional
    public boolean processPaymentFailure(String orderCode, String responseCode, String transactionStatus, String source) {
        Order order = orderRepository.findByOrderCode(orderCode).orElse(null);
        if (order == null) {
            return false;
        }

        Payment pendingPayment = paymentRepository
                .findTopByOrderOrderIdAndPaymentStatusOrderByPaymentIdDesc(order.getOrderId(), "PENDING")
                .orElse(null);

        if (pendingPayment == null) {
            return paymentRepository.findByOrderOrderIdOrderByPaymentIdAsc(order.getOrderId()).stream()
                    .anyMatch(payment -> "FAILED".equalsIgnoreCase(payment.getPaymentStatus()));
        }

        pendingPayment.setPaymentStatus("FAILED");
        pendingPayment.setNotes(buildVnPayFailureNote(responseCode, transactionStatus, source));
        paymentRepository.save(pendingPayment);

        if (STATUS_PENDING_PAYMENT.equalsIgnoreCase(order.getOrderStatus())) {
            appendOrderNote(order, "Thanh toán VNPay thất bại. Khách có thể thử thanh toán lại nếu đơn còn hiệu lực.");
            orderRepository.save(order);
        }

        return true;
    }

    @Transactional
    public Payment preparePendingVnPayPayment(String orderCode) {
        Order order = orderRepository.findByOrderCode(orderCode)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn hàng: " + orderCode));

        if (!STATUS_PENDING_PAYMENT.equalsIgnoreCase(order.getOrderStatus())) {
            throw new IllegalStateException("Đơn hàng hiện không ở trạng thái chờ thanh toán.");
        }

        Payment pendingPayment = paymentRepository
                .findTopByOrderOrderIdAndPaymentStatusOrderByPaymentIdDesc(order.getOrderId(), "PENDING")
                .orElse(null);
        if (pendingPayment != null) {
            return pendingPayment;
        }

        List<Payment> payments = paymentRepository.findByOrderOrderIdOrderByPaymentIdAsc(order.getOrderId());
        Payment latestFailedVnPayPayment = payments.stream()
                .filter(payment -> PaymentMethod.VNPAY.name().equalsIgnoreCase(payment.getPaymentMethod())
                        || PaymentType.DEPOSIT.name().equalsIgnoreCase(payment.getPaymentType())
                        || PaymentType.FULL_PAYMENT.name().equalsIgnoreCase(payment.getPaymentType()))
                .filter(payment -> "FAILED".equalsIgnoreCase(payment.getPaymentStatus())
                        || "EXPIRED".equalsIgnoreCase(payment.getPaymentStatus()))
                .reduce((first, second) -> second)
                .orElse(null);

        String paymentType = latestFailedVnPayPayment != null && latestFailedVnPayPayment.getPaymentType() != null
                ? latestFailedVnPayPayment.getPaymentType()
                : (order.getDepositAmount() != null && order.getDepositAmount().compareTo(ZERO) > 0
                ? PaymentType.DEPOSIT.name()
                : PaymentType.FULL_PAYMENT.name());

        BigDecimal amount = latestFailedVnPayPayment != null && latestFailedVnPayPayment.getAmount() != null
                ? latestFailedVnPayPayment.getAmount()
                : (PaymentType.DEPOSIT.name().equalsIgnoreCase(paymentType)
                ? order.getDepositAmount()
                : order.getTotalAmount());

        if (amount == null || amount.compareTo(ZERO) <= 0) {
            throw new IllegalStateException("Số tiền thanh toán không hợp lệ.");
        }

        String retryPaymentMethod = PaymentType.DEPOSIT.name().equalsIgnoreCase(paymentType)
                ? PaymentMethod.DEPOSIT.name()
                : PaymentMethod.VNPAY.name();

        Payment retryPayment = Payment.builder()
                .order(order)
                .paymentType(paymentType)
                .paymentMethod(retryPaymentMethod)
                .paymentStatus("PENDING")
                .amount(amount)
                .notes("Retry VNPay payment after previous failed/expired attempt")
                .build();
        return paymentRepository.save(retryPayment);
    }

    private String buildVnPayFailureNote(String responseCode, String transactionStatus, String source) {
        String note = "VNPay payment failed"
                + " source=" + safeGatewayValue(source)
                + ", responseCode=" + safeGatewayValue(responseCode)
                + ", transactionStatus=" + safeGatewayValue(transactionStatus)
                + ", at=" + LocalDateTime.now();
        return note.length() > 500 ? note.substring(0, 500) : note;
    }

    private String safeGatewayValue(String value) {
        return value == null || value.isBlank() ? "N/A" : value.trim();
    }

    @Transactional
    public boolean confirmRemainingPayment(String orderCode, String notes, User moderator) {
        Order order = orderRepository.findByOrderCode(orderCode).orElse(null);
        if (order == null) {
            throw new IllegalArgumentException("Không tìm thấy đơn hàng: " + orderCode);
        }

        if (!"DEPOSITED".equalsIgnoreCase(order.getOrderStatus())) {
            throw new IllegalStateException("Đơn hàng phải ở trạng thái ĐÃ ĐẶT CỌC (DEPOSITED) mới được xác nhận thanh toán phần còn lại!");
        }

        validateAssignedModerator(order, moderator);

        // Tính tổng tiền deposit đã thanh toán thành công
        BigDecimal treePrice = resolveTreePrice(order);

        List<Payment> depositPayments = paymentRepository.findByOrderOrderIdAndPaymentType(order.getOrderId(), PaymentType.DEPOSIT.name());
        BigDecimal depositPaid = depositPayments.stream()
                .filter(p -> "SUCCESS".equalsIgnoreCase(p.getPaymentStatus()))
                .map(Payment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalAmount = order.getTotalAmount() != null ? order.getTotalAmount() : treePrice;
        BigDecimal remainingAmount = totalAmount.subtract(depositPaid);
        if (remainingAmount.compareTo(ZERO) < 0) {
            remainingAmount = ZERO;
        }

        // Tạo Payment record #2: REMAINING_PAYMENT
        Payment remainingPayment = Payment.builder()
                .order(order)
                .paymentType(PaymentType.REMAINING_PAYMENT.name())
                .paymentMethod(PaymentMethod.CASH.name())
                .paymentStatus("SUCCESS")
                .amount(remainingAmount)
                .paymentDate(LocalDateTime.now())
                .notes(notes)
                .build();
        paymentRepository.save(remainingPayment);

        String oldStatus = order.getOrderStatus();
        LocalDateTime paidAt = LocalDateTime.now();
        order.setOrderStatus("PAID");
        orderRepository.save(order);
        markProductsAsSold(order);

        OrderLog logEntry = OrderLog.builder()
                .order(order)
                .actionBy(moderator)
                .actionType("REMAINING_PAYMENT_CONFIRMED")
                .fromStatus(oldStatus)
                .toStatus("PAID")
                .actionAt(paidAt)
                .build();
        orderLogRepository.save(logEntry);

        eventPublisher.publishEvent(new OrderPaidEvent(order));
        return true;
    }

    private BigDecimal resolveTreePrice(Order order) {
        if (order == null) {
            return ZERO;
        }

        if (order.getOrderDetails() != null && !order.getOrderDetails().isEmpty()) {
            return order.getOrderDetails().stream()
                    .map(d -> {
                        BigDecimal price = d.getPriceAtPurchase() != null ? d.getPriceAtPurchase() : BigDecimal.ZERO;
                        int quantity = d.getQuantity() != null ? d.getQuantity() : 1;
                        return price.multiply(BigDecimal.valueOf(quantity));
                    })
                    .reduce(ZERO, BigDecimal::add);
        }

        BigDecimal craneFee = order.getCraneFee() != null ? order.getCraneFee() : ZERO;
        BigDecimal shippingFee = order.getShippingFee() != null ? order.getShippingFee() : ZERO;
        BigDecimal totalAmount = order.getTotalAmount() != null ? order.getTotalAmount() : ZERO;
        BigDecimal treePrice = totalAmount.subtract(craneFee).subtract(shippingFee);
        return treePrice.compareTo(ZERO) < 0 ? ZERO : treePrice;
    }

    private BigDecimal normalizeNonNegativeAmount(BigDecimal amount, String label) {
        BigDecimal normalized = amount != null ? amount : ZERO;
        if (normalized.compareTo(ZERO) < 0) {
            throw new IllegalArgumentException(label + " không được âm.");
        }
        validateWholeNumberAmount(normalized, label);
        return normalized;
    }

    private void validateWholeNumberAmount(BigDecimal amount, String label) {
        if (amount != null && amount.stripTrailingZeros().scale() > 0) {
            throw new IllegalArgumentException(label + " phải là số nguyên.");
        }
    }

    @Transactional
    public boolean markDepositedOrderCustomerNoShow(String orderCode, String notes, User moderator) {
        Order order = orderRepository.findByOrderCode(orderCode).orElse(null);
        if (order == null) {
            throw new IllegalArgumentException("Không tìm thấy đơn hàng: " + orderCode);
        }
        validateAssignedModerator(order, moderator);
        if (!"DEPOSITED".equalsIgnoreCase(order.getOrderStatus())) {
            throw new IllegalStateException("Chỉ có thể ghi nhận khách không nhận hàng sau khi khách đã thanh toán tiền đặt cọc.");
        }

        String oldStatus = order.getOrderStatus();
        String reason = requireReason(notes);
        Payment depositPayment = financialLedgerService.requireSuccessfulDepositPayment(order);
        BigDecimal forfeitedAmount = depositPayment.getAmount() != null ? depositPayment.getAmount() : ZERO;
        financialLedgerService.recordForfeitedDepositIncome(order, depositPayment, forfeitedAmount, reason, moderator);
        appendOrderNote(order, reason + " Tiền cọc được ghi nhận giữ lại do lỗi khách hàng.");
        order.setOrderStatus("CANCELLED");
        orderRepository.save(order);
        releaseProducts(order);

        OrderLog logEntry = OrderLog.builder()
                .order(order)
                .actionBy(moderator)
                .actionType("FORFEITED_DEPOSIT_INCOME_RECORDED")
                .fromStatus(oldStatus)
                .toStatus("CANCELLED")
                .actionAt(LocalDateTime.now())
                .build();
        orderLogRepository.save(logEntry);

        eventPublisher.publishEvent(new OrderRejectedEvent(order, reason));
        return true;
    }

    @Transactional
    public boolean recordFaultRefundAndCancel(String orderCode, String faultPartyValue, BigDecimal refundAmount,
                                              String reason, String evidenceNote, String externalReference,
                                              Boolean customerKeepsTree, String productResolution,
                                              User moderator) {
        Order order = orderRepository.findByOrderCode(orderCode).orElse(null);
        if (order == null) {
            throw new IllegalArgumentException("Không tìm thấy đơn hàng: " + orderCode);
        }
        validateAssignedModerator(order, moderator);

        String oldStatus = order.getOrderStatus();
        boolean validStatus = "DEPOSITED".equalsIgnoreCase(oldStatus)
                || "PAID".equalsIgnoreCase(oldStatus)
                || "COMPLETED".equalsIgnoreCase(oldStatus);
        if (!validStatus) {
            throw new IllegalStateException("Chỉ có thể ghi nhận hoàn tiền khi đơn đã có khoản thanh toán thành công.");
        }

        FaultParty faultParty = parseFaultParty(faultPartyValue);
        if (faultParty != FaultParty.NURSERY && faultParty != FaultParty.DELIVERY) {
            throw new IllegalArgumentException("Bên chịu trách nhiệm phải là nhà vườn hoặc quá trình vận chuyển.");
        }

        // Tự động xác định số tiền hoàn 100% dựa trên tổng số tiền thực tế khách đã thanh toán
        BigDecimal refundableCash = financialLedgerService.calculateRefundableCash(order);
        if (refundableCash == null || refundableCash.compareTo(ZERO) <= 0) {
            throw new IllegalStateException("Đơn hàng này không có khoản thanh toán thành công nào còn có thể hoàn tiền.");
        }

        BigDecimal calculatedRefundAmount = refundableCash;
        String normalizedReason = requireReason(reason);

        FinancialLedger refundLedger = financialLedgerService.recordManualFaultRefund(
                order,
                faultParty,
                calculatedRefundAmount,
                normalizedReason,
                evidenceNote,
                externalReference,
                moderator
        );

        appendOrderNote(order, normalizedReason + " Hoàn tiền 100% chỉ được ghi nhận thủ công, không tự động chuyển khoản.");

        // Nghiệp vụ cố định: Đơn chuyển CANCELLED, khách không giữ cây, toàn bộ cây trong đơn trả về AVAILABLE
        order.setOrderStatus("CANCELLED");
        orderRepository.save(order);
        releaseProducts(order);

        OrderLog logEntry = OrderLog.builder()
                .order(order)
                .actionBy(moderator)
                .actionType(refundLedger.getLedgerType().name() + "_RECORDED")
                .fromStatus(oldStatus)
                .toStatus("CANCELLED")
                .actionAt(LocalDateTime.now())
                .build();
        orderLogRepository.save(logEntry);

        eventPublisher.publishEvent(new OrderRejectedEvent(order, normalizedReason));
        return true;
    }

    @Transactional
    public boolean completePaidOrder(String orderCode, User moderator) {
        Order order = orderRepository.findByOrderCode(orderCode).orElse(null);
        if (order == null) {
            throw new IllegalArgumentException("Không tìm thấy đơn hàng: " + orderCode);
        }
        validateAssignedModerator(order, moderator);
        if (!"PAID".equalsIgnoreCase(order.getOrderStatus())) {
            throw new IllegalStateException("Trạng thái hiện tại của đơn không cho phép xác nhận hoàn thành.");
        }

        BigDecimal refundableCash = financialLedgerService.calculateRefundableCash(order);
        BigDecimal totalRequired = order.getTotalAmount() != null ? order.getTotalAmount() : ZERO;
        if (refundableCash == null || refundableCash.compareTo(totalRequired) < 0) {
            throw new IllegalStateException("Không thể hoàn thành đơn vì khách hàng chưa thanh toán đầy đủ.");
        }

        String oldStatus = order.getOrderStatus();
        LocalDateTime completedAt = LocalDateTime.now();
        order.setOrderStatus("COMPLETED");
        orderRepository.save(order);
        markProductsAsSold(order);
        recordCompletedRevenueLedger(order, moderator, completedAt);

        OrderLog logEntry = OrderLog.builder()
                .order(order)
                .actionBy(moderator)
                .actionType("ORDER_COMPLETED")
                .fromStatus(oldStatus)
                .toStatus("COMPLETED")
                .actionAt(completedAt)
                .build();
        orderLogRepository.save(logEntry);
        return true;
    }

    private void validateAssignedModerator(Order order, User moderator) {
        Integer moderatorId = moderator != null ? moderator.getUserId() : null;
        Integer assignedId = order != null && order.getAssignedTo() != null ? order.getAssignedTo().getUserId() : null;
        if (moderatorId == null || !moderatorId.equals(assignedId)) {
            throw new IllegalStateException("Bạn không phụ trách đơn này.");
        }
    }

    private String requireReason(String reason) {
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("Lý do là bắt buộc.");
        }
        return reason.trim();
    }

    private FaultParty parseFaultParty(String faultPartyValue) {
        if (faultPartyValue == null || faultPartyValue.isBlank()) {
            throw new IllegalArgumentException("Bên chịu lỗi là bắt buộc.");
        }
        try {
            return FaultParty.valueOf(faultPartyValue.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Bên chịu lỗi không hợp lệ: " + faultPartyValue);
        }
    }

    private void appendOrderNote(Order order, String note) {
        String normalized = note == null ? "" : note.trim();
        if (normalized.isBlank()) {
            return;
        }
        String currentNotes = order.getNotes();
        String merged = currentNotes == null || currentNotes.isBlank() ? normalized : currentNotes + " | " + normalized;
        order.setNotes(merged.length() > 500 ? merged.substring(0, 500) : merged);
    }

    private String requireProductResolution(String productResolution) {
        if (productResolution == null || productResolution.isBlank()) {
            throw new IllegalArgumentException("Vui lòng chọn cách xử lý cây trước khi xác nhận hoàn tiền.");
        }
        String normalized = productResolution.trim().toUpperCase();
        return switch (normalized) {
            case "RETURNED_AND_RESELLABLE", "RETURNED_AND_DAMAGED", "NOT_RETURNED", "UNDER_INSPECTION" -> normalized;
            default -> throw new IllegalArgumentException("Cách xử lý cây không hợp lệ: " + productResolution);
        };
    }

    private void applyProductResolution(Order order, String productResolution) {
        switch (productResolution) {
            case "RETURNED_AND_RESELLABLE" -> releaseProducts(order);
            case "UNDER_INSPECTION" -> markProductsAsReserved(order);
            case "RETURNED_AND_DAMAGED", "NOT_RETURNED" -> markProductsAsSold(order);
            default -> throw new IllegalArgumentException("Cách xử lý cây không hợp lệ: " + productResolution);
        }
    }

    private void recordCompletedRevenueLedger(Order order, User actor, LocalDateTime completedAt) {
        FinancialLedger ledger = financialLedgerService.recordCompletedOrderRevenueIfAbsent(order, actor, completedAt);
        if (ledger == null) {
            return;
        }

        OrderLog ledgerLog = OrderLog.builder()
                .order(order)
                .actionBy(actor)
                .actionType("COMPLETED_ORDER_REVENUE_RECORDED")
                .fromStatus(order.getOrderStatus())
                .toStatus(order.getOrderStatus())
                .actionAt(completedAt != null ? completedAt : LocalDateTime.now())
                .build();
        orderLogRepository.save(ledgerLog);
    }

    private void recordFullPaymentRevenueLedgerIfPossible(Order order, LocalDateTime paidAt) {
        if (order == null || order.getOrderId() == null) {
            return;
        }

        boolean hasSuccessfulFullPayment = paymentRepository.findByOrderOrderIdOrderByPaymentIdAsc(order.getOrderId())
                .stream()
                .anyMatch(payment -> PaymentType.FULL_PAYMENT.name().equalsIgnoreCase(payment.getPaymentType())
                        && "SUCCESS".equalsIgnoreCase(payment.getPaymentStatus()));
        if (!hasSuccessfulFullPayment) {
            return;
        }

        User actor = order.getAssignedTo();
        if (actor == null || actor.getUserId() == null) {
            log.warn("Cannot record full-payment revenue ledger for order {} because assigned moderator is missing.",
                    order.getOrderCode());
            return;
        }

        recordCompletedRevenueLedger(order, actor, paidAt);
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
            OrderLog logEntry = OrderLog.builder()
                    .order(order)
                    .actionBy(moderator)
                    .actionType("PAID")
                    .fromStatus(oldStatus)
                    .toStatus("PAID")
                    .actionAt(LocalDateTime.now())
                    .build();
            orderLogRepository.save(logEntry);
        }

        return result;
    }

    private void markProductsAsSold(Order order) {
        if (order.getOrderDetails() != null) {
            for (OrderDetail detail : order.getOrderDetails()) {
                Product prod = detail.getProduct();
                if (prod != null) {
                    prod.setProductStatus("SOLD");
                    productRepository.save(prod);
                }
            }
        }
    }

    private void markProductsAsReserved(Order order) {
        if (order.getOrderDetails() != null) {
            for (OrderDetail detail : order.getOrderDetails()) {
                Product prod = detail.getProduct();
                if (prod != null) {
                    prod.setProductStatus("RESERVED");
                    productRepository.save(prod);
                }
            }
        }
    }

    private void releaseProducts(Order order) {
        if (order.getOrderDetails() != null) {
            for (OrderDetail detail : order.getOrderDetails()) {
                Product prod = detail.getProduct();
                if (prod != null) {
                    prod.setProductStatus("AVAILABLE");
                    productRepository.save(prod);
                }
            }
        }
    }
}
