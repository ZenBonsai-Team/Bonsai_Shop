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
import com.example.bonsai_shop.customer.repository.ModerationNotificationRepository;
import com.example.bonsai_shop.entity.ModerationNotification;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * [SERVICE TRỌNG TÂM QUẢN LÝ VÀ XỬ LÝ ĐƠN HÀNG - ORDER SERVICE]
 *
 * Chịu trách nhiệm:
 * - Tiếp nhận và tạo đơn hàng Checkout (Guest & Authenticated), đặt chỗ giữ cây
 * nguyên tử (Atomic Reservation).
 * - Quản lý Orders Pool: Tra cứu, phân trang, lọc theo tiêu chí Moderator.
 * - Điều phối quy trình xử lý của Moderator: Tiếp nhận (claim), trả lại
 * (unclaim), phê duyệt & tính phí (verify), từ chối (reject).
 * - Quản lý thanh toán VNPay 1-N (Đặt cọc DEPOSIT / Thanh toán đủ
 * FULL_PAYMENT):
 * + Chuẩn bị bản ghi thanh toán PENDING (preparePendingVnPayPayment).
 * + Xử lý kết quả thành công (processPaymentSuccess) chuyển trạng thái sang
 * DEPOSITED hoặc PAID.
 * + Xử lý thất bại (processPaymentFailure).
 * + Xác nhận thu nốt đợt 2 (confirmRemainingPayment) và hoàn tất đơn
 * (COMPLETED, chuyển Product sang SOLD).
 * - Xử lý các ngoại lệ nghiệp vụ: Khách bùng cọc
 * (markDepositedOrderCustomerNoShow), hoàn tiền do lỗi nhà vườn/vận chuyển
 * (recordFaultRefundAndCancel).
 * - Tương tác hệ thống: Ghi log (OrderLog), lưu vết xử lý (OrderHandling), bắn
 * sự kiện (OrderCreatedEvent, OrderVerifiedEvent, OrderPaidEvent,
 * OrderRejectedEvent), gửi email thông báo (MailService), ghi nhận sổ cái tài
 * chính (FinancialLedgerService).
 *
 * Các thành phần phối hợp chính:
 * - Repositories: OrderRepository, ProductRepository, PaymentRepository,
 * OrderLogRepository, OrderHandlingRepository.
 * - Services: CartService, MailService, FinancialLedgerService.
 * - Events: ApplicationEventPublisher.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private static final String STATUS_PENDING_PAYMENT = "PENDING_PAYMENT";
    private static final String STATUS_PENDING = "PENDING";
    private static final BigDecimal ZERO = BigDecimal.ZERO;
    private static final BigDecimal MAX_FEE_AMOUNT = new BigDecimal("200000000");
    private static final BigDecimal MAX_MONEY_AMOUNT = new BigDecimal("999999999999.99");

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final OrderLogRepository orderLogRepository;
    private final OrderHandlingRepository orderHandlingRepository;
    private final PaymentRepository paymentRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final MailService mailService;
    private final CartService cartService;
    private final FinancialLedgerService financialLedgerService;
    private final ModerationNotificationRepository notificationRepository;

    /**
     * [LẤY DANH SÁCH TỔNG HỢP ĐƠN HÀNG CÓ PHÂN TRANG VÀ LỌC TRẠNG THÁI]
     *
     * Mục đích:
     * - Phục vụ màn hình tra cứu chung cho Moderator / Admin.
     *
     * Được gọi từ:
     * - OrderApiController.getOrders()
     *
     * Input & Output:
     * - Input: search (String), status (String), sort (String), page (int), limit
     * (int)
     * - Output: Page<Order>
     *
     * Tác động DB:
     * - Bảng đọc: ORDER, USER, PRODUCT
     */
    @Transactional(readOnly = true)
    public Page<Order> getFilteredOrders(String search, String status, String sort, int page, int limit) {
        Sort springSort = resolveSort(sort);
        Pageable pageable = PageRequest.of(page - 1, limit, springSort);
        return orderRepository.searchOrdersForModerator(resolveStatusFilter(status), search, pageable);
    }

    /**
     * [LẤY DANH SÁCH ĐƠN HÀNG TRONG KHO CHUNG (ORDERS POOL)]
     *
     * Mục đích:
     * - Lấy các đơn hàng có assignedTo IS NULL và orderStatus = 'PENDING' để
     * Moderator nhận xử lý.
     *
     * Được gọi từ:
     * - OrderApiController.getPoolOrders()
     *
     * Input & Output:
     * - Input: search (String), sort (String), page (int), limit (int)
     * - Output: Page<Order>
     *
     * Tác động DB:
     * - Bảng đọc: ORDER
     */
    @Transactional(readOnly = true)
    public Page<Order> getPoolOrders(String search, String sort, int page, int limit) {
        Sort springSort = resolveSort(sort);
        Pageable pageable = PageRequest.of(page - 1, limit, springSort);
        return orderRepository.searchOrdersPool(search, pageable);
    }

    /**
     * [LẤY DANH SÁCH ĐƠN HÀNG DO MODERATOR CỤ THỂ PHỤ TRÁCH]
     *
     * Mục đích:
     * - Phục vụ màn hình "Đơn hàng của tôi" (My Orders) theo moderatorId.
     *
     * Được gọi từ:
     * - OrderApiController.getMyOrders()
     *
     * Input & Output:
     * - Input: moderatorId (Integer), search (String), status (String), sort
     * (String), page (int), limit (int)
     * - Output: Page<Order>
     */
    @Transactional(readOnly = true)
    public Page<Order> getMyOrders(Integer moderatorId, String search, String status, String sort, int page,
            int limit) {
        Sort springSort = resolveSort(sort);
        Pageable pageable = PageRequest.of(page - 1, limit, springSort);
        return orderRepository.searchMyOrders(moderatorId, resolveStatusFilter(status), search, pageable);
    }

    /**
     * [LẤY LỊCH SỬ ĐƠN HÀNG CỦA KHÁCH HÀNG]
     *
     * Được gọi từ:
     * - Profile / Lịch sử mua hàng của Khách hàng
     */
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
        kpis.put("approved",
                orderRepository.countByAssignedToUserIdAndOrderStatus(moderatorId, STATUS_PENDING_PAYMENT));
        kpis.put("paid", orderRepository.countByAssignedToUserIdAndOrderStatus(moderatorId, "PAID"));
        kpis.put("rejected", orderRepository.countByAssignedToUserIdAndOrderStatus(moderatorId, "CANCELLED"));
        return kpis;
    }

    @Transactional(readOnly = true)
    public List<OrderHandling> getOrderHandlingHistory(Integer orderId) {
        return orderHandlingRepository.findByOrderOrderIdOrderByHandledAtDesc(orderId);
    }

    /**
     * [TIẾP NHẬN ĐƠN HÀNG TỪ KHO CHUNG (CLAIM ORDER)]
     *
     * Mục đích:
     * - Gán quyền xử lý đơn hàng PENDING cho Moderator đang đăng nhập.
     *
     * Được gọi từ:
     * - OrderApiController.claimOrder()
     * - OrderActionService.handleClaim()
     *
     * Các bước thực hiện:
     * 1. Tìm đơn theo orderCode trong DB.
     * 2. Kiểm tra điều kiện: assignedTo phải null, orderStatus phải là "PENDING".
     * 3. Gán assignedTo = moderator, assignedAt = now trên Order.
     * 4. Tạo bản ghi OrderHandling mới (isActive = true) để theo dõi phiên xử lý.
     *
     * Tác động DB:
     * - ORDER: assigned_to, assigned_at
     * - ORDER_HANDLING: thêm bản ghi mới
     */
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

    /**
     * [TRẢ LẠI ĐƠN HÀNG VỀ KHO CHUNG (UNCLAIM ORDER)]
     *
     * Mục đích:
     * - Hủy gán người phụ trách khi Moderator không thể xử lý đơn PENDING.
     *
     * Được gọi từ:
     * - OrderApiController.unclaimOrder()
     * - OrderActionService.handleReturnInventory()
     *
     * Các bước thực hiện:
     * 1. Kiểm tra đơn thuộc quyền moderator và có trạng thái "PENDING".
     * 2. Set assignedTo = null, assignedAt = null trên Order.
     * 3. Tìm các bản ghi OrderHandling đang active của moderator trên đơn này và
     * set isActive = false, releasedAt = now.
     *
     * Tác động DB:
     * - ORDER: assigned_to = null, assigned_at = null
     * - ORDER_HANDLING: isActive = false, releasedAt = now
     */
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

    /**
     * [PHÊ DUYỆT ĐƠN HÀNG, ÁP PHÍ VẬN CHUYỂN/CẨU VÀ TÍNH TIỀN THANH TOÁN ĐỢT 1]
     *
     * Mục đích:
     * - Moderator xác nhận đơn hàng sau khi liên hệ thỏa thuận với khách.
     * - Tính toán lại tổng tiền: treePrice + craneFee + shippingFee.
     * - Khởi tạo/cập nhật số tiền cần thanh toán cho Payment record PENDING.
     * - Chuyển trạng thái sang PENDING_PAYMENT và phát sự kiện gửi link thanh toán
     * cho khách.
     *
     * Được gọi từ:
     * - OrderApiController.verifyOrder()
     * - OrderActionService.handleApprove()
     *
     * Các bước thực hiện:
     * 1. Tìm Order và kiểm tra đúng Moderator phụ trách + trạng thái đơn là
     * "PENDING".
     * 2. Tính giá cây gốc treePrice từ OrderDetail.
     * 3. Chuẩn hóa craneFee, shippingFee (>= 0, <= 200tr, là số nguyên).
     * 4. Tính newTotal = treePrice + craneFee + shippingFee.
     * 5. Kiểm tra flow Đặt cọc (DEPOSIT) hay Thanh toán đủ (FULL_PAYMENT):
     * - Nếu DEPOSIT: Kiểm tra depositAmount (0 < depositAmount <= treePrice),
     * tạo/cập nhật Payment record với paymentType = DEPOSIT, amount =
     * depositAmount, status = PENDING.
     * - Nếu FULL_PAYMENT: Set depositAmount = 0, tạo/cập nhật Payment record với
     * paymentType = FULL_PAYMENT, amount = newTotal, status = PENDING.
     * 6. Cập nhật Order: orderStatus = "PENDING_PAYMENT".
     * 7. Ghi bản ghi OrderLog (actionType = "VERIFY", fromStatus = "PENDING",
     * toStatus = "PENDING_PAYMENT").
     * 8. Đóng OrderHandling active (isActive = false, releasedAt = now).
     * 9. Phát sự kiện OrderVerifiedEvent → gửi email link thanh toán VNPay cho
     * khách.
     *
     * Tác động DB:
     * - ORDER: craneFee, shippingFee, totalAmount, depositAmount, orderStatus =
     * PENDING_PAYMENT
     * - PAYMENT: amount, paymentType, paymentStatus = PENDING
     * - ORDER_LOG: log "VERIFY"
     * - ORDER_HANDLING: isActive = false
     */
    @Transactional
    public boolean verifyOrder(String orderCode, BigDecimal craneFee, BigDecimal shippingFee, BigDecimal depositAmount,
            User moderator) {
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

        // Tong gia tri thuc te cua toan bo don hang = Tree Price + Crane Fee + Shipping
        // Fee
        BigDecimal newTotal = treePrice.add(normalizedCraneFee).add(normalizedShippingFee);
        if (newTotal.compareTo(MAX_MONEY_AMOUNT) > 0) {
            throw new IllegalArgumentException("Tổng tiền đơn hàng không được vượt quá 999.999.999.999 VNĐ.");
        }
        order.setTotalAmount(newTotal);

        List<Payment> existingPayments = paymentRepository.findByOrderOrderIdOrderByPaymentIdAsc(order.getOrderId());

        boolean isDepositFlow = existingPayments.stream()
                .anyMatch(p -> PaymentType.DEPOSIT.name().equalsIgnoreCase(p.getPaymentType()) ||
                        "DEPOSIT".equalsIgnoreCase(p.getPaymentMethod()) ||
                        "COD".equalsIgnoreCase(p.getPaymentMethod()));

        if (isDepositFlow) {
            if (depositAmount == null || depositAmount.compareTo(ZERO) <= 0) {
                throw new IllegalArgumentException("Vui lòng nhập số tiền đặt cọc.");
            }
            validateWholeNumberAmount(depositAmount, "Tiền đặt cọc");
            if (depositAmount.compareTo(treePrice) > 0) {
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

    /**
     * [TỪ CHỐI / HỦY ĐƠN HÀNG PENDING KÈM LÝ DO]
     *
     * Mục đích:
     * - Moderator hủy đơn hàng chưa duyệt và giải phóng toàn bộ cây về trạng thái
     * AVAILABLE.
     *
     * Được gọi từ:
     * - OrderApiController.rejectOrder()
     * - OrderActionService.handleReject()
     *
     * Các bước thực hiện:
     * 1. Kiểm tra đơn thuộc quyền moderator và ở trạng thái "PENDING".
     * 2. Đổi orderStatus = "CANCELLED", nối lý do vào notes.
     * 3. Giải phóng toàn bộ Product trong đơn: productStatus: RESERVED → AVAILABLE.
     * 4. Ghi OrderLog (actionType = "REJECT", toStatus = "CANCELLED").
     * 5. Đóng OrderHandling active.
     * 6. Phát sự kiện OrderRejectedEvent → gửi email thông báo hủy cho khách.
     *
     * Tác động DB:
     * - ORDER: orderStatus = CANCELLED, notes
     * - PRODUCT: productStatus = AVAILABLE
     * - ORDER_LOG: log "REJECT"
     * - ORDER_HANDLING: isActive = false
     */
    @Transactional
    public boolean rejectOrder(String orderCode, String reason, User moderator) {
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("Lý do từ chối là bắt buộc.");
        }
        if (reason.trim().length() > 500) {
            throw new IllegalArgumentException("Nội dung không được vượt quá 500 ký tự.");
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
        appendOrderNote(order, "Hủy đơn với lý do: " + reason);
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

    /**
     * [GHI NHẬN ĐẶT CỌC THỦ CÔNG (DIRECT DEPOSIT)]
     */
    @Transactional
    public boolean recordDepositPayment(String orderCode, BigDecimal depositAmount, User moderator) {
        Order order = orderRepository.findByOrderCode(orderCode).orElse(null);
        if (order == null || (!"PENDING".equalsIgnoreCase(order.getOrderStatus())
                && !STATUS_PENDING_PAYMENT.equalsIgnoreCase(order.getOrderStatus()))) {
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

    /**
     * [TẠO ĐƠN HÀNG MỚI VÀ GIỮ CHỖ CÂY NGUYÊN TỬ (CREATE ORDER & ATOMIC RESERVE)]
     *
     * Mục đích:
     * - Tiếp nhận thông tin checkout của khách (đăng nhập hoặc vãng lai).
     * - Thực hiện giữ chỗ nguyên tử (Atomic Reserve) trên từng cây qua DB query
     * UPDATE PRODUCT SET productStatus='RESERVED' WHERE productId=? AND
     * productStatus='AVAILABLE'.
     * - Sinh mã đơn hàng chuẩn định dạng "BSMS-XXXXXX".
     * - Lưu Order (PENDING), OrderDetail (snapshot priceAtPurchase), và khởi tạo
     * Payment record đầu tiên (PENDING).
     * - Xóa giỏ hàng nếu là user đăng nhập.
     * - Bắn sự kiện OrderCreatedEvent để gửi email xác nhận.
     *
     * Được gọi từ:
     * - OrderApiController.checkout()
     *
     * Input & Output:
     * - Input: dto (PurchaseOrderRequestDTO), customer (User - nullable nếu là
     * Guest)
     * - Output: Order entity đã được persist
     *
     * Tác động DB:
     * - Bảng đọc: PRODUCT, CART_ITEM
     * - Bảng ghi:
     * + ORDER: tạo mới (orderStatus = PENDING, orderType = ONLINE)
     * + ORDER_DETAIL: tạo mới chi tiết đơn hàng
     * + PRODUCT: productStatus: AVAILABLE → RESERVED
     * + PAYMENT: tạo 1 bản ghi ban đầu (paymentStatus = PENDING)
     * + CART_ITEM: xóa nếu là logged-in customer
     *
     * Sự kiện phát sinh:
     * - OrderCreatedEvent (gửi email thông báo đơn đã tạo)
     */
    @Transactional
    public Order createOrder(PurchaseOrderRequestDTO dto, User customer) {
        if (dto.getCustomerName() != null && dto.getCustomerName().length() > 255) {
            throw new IllegalArgumentException("Tên khách hàng không được vượt quá 255 ký tự.");
        }
        if (dto.getCustomerEmail() != null && dto.getCustomerEmail().length() > 255) {
            throw new IllegalArgumentException("Email không được vượt quá 255 ký tự.");
        }
        if (dto.getShippingAddress() != null && dto.getShippingAddress().length() > 500) {
            throw new IllegalArgumentException("Địa chỉ nhận hàng không được vượt quá 500 ký tự.");
        }
        if (dto.getNotes() != null && dto.getNotes().length() > 500) {
            throw new IllegalArgumentException("Ghi chú không được vượt quá 500 ký tự.");
        }

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
                .notes(dto.getNotes())
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

    /**
     * [XỬ LÝ THANH TOÁN VNPAY THÀNH CÔNG (PAYMENT SUCCESS CALLBACK / IPN)]
     *
     * Mục đích:
     * - Tiếp nhận tín hiệu thanh toán thành công từ PaymentController (Return URL)
     * hoặc IPNController (Webhook IPN).
     * - Cập nhật Payment record PENDING thành SUCCESS và ghi nhận paymentDate.
     * - Nếu paymentType = DEPOSIT: Chuyển Order sang DEPOSITED, gửi email xác nhận
     * đặt cọc thành công.
     * - Nếu paymentType = FULL_PAYMENT: Chuyển Order sang PAID, phát sự kiện
     * OrderPaidEvent.
     *
     * Được gọi từ:
     * - PaymentController.paymentCallback()
     * - IPNController.receiveIPN()
     *
     * Tác động DB:
     * - PAYMENT: paymentStatus = "SUCCESS", paymentDate = now
     * - ORDER: orderStatus = "DEPOSITED" (nếu cọc) hoặc "PAID" (nếu thanh toán đủ
     * 100%)
     */
    @Transactional
    public boolean processPaymentSuccess(String orderCode) {
        Order order = orderRepository.findByOrderCode(orderCode).orElse(null);
        if (order == null) {
            return false;
        }

        if ("PAID".equalsIgnoreCase(order.getOrderStatus())) {
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
        sendReviewReminderNotification(order);

        eventPublisher.publishEvent(new OrderPaidEvent(order));
        return true;
    }

    /**
     * [XỬ LÝ THANH TOÁN VNPAY THẤT BẠI HOẶC BỊ HỦY]
     *
     * Mục đích:
     * - Ghi nhận lý do thất bại vào Payment record để hỗ trợ đối soát.
     *
     * Được gọi từ:
     * - PaymentController.paymentCallback()
     * - IPNController.receiveIPN()
     *
     * Tác động DB:
     * - PAYMENT: paymentStatus = "FAILED", notes
     */
    @Transactional
    public boolean processPaymentFailure(String orderCode, String responseCode, String transactionStatus,
            String source) {
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

    /**
     * [CHUẨN BỊ BẢN GHI THANH TOÁN PENDING CHO VNPAY (HỖ TRỢ RETRY)]
     *
     * Mục đích:
     * - Đảm bảo luôn có 1 bản ghi Payment PENDING với số tiền chính xác khi khách
     * bấm vào link thanh toán VNPay.
     * - Nếu khách từng thanh toán lỗi/hết hạn trước đó, hàm sẽ tự tạo lại bản ghi
     * PENDING retry mới.
     *
     * Được gọi từ:
     * - PaymentController.payOrder()
     */
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

    /**
     * [XÁC NHẬN THU ĐỦ TIỀN ĐỢT 2 VÀ HOÀN TẤT ĐƠN ĐẶT CỌC (CONFIRM REMAINING
     * PAYMENT)]
     *
     * Mục đích:
     * - Dành cho đơn hàng đã cọc (DEPOSITED). Khi Moderator giao cây và thu nốt số
     * tiền còn lại (CASH/Chuyển khoản).
     * - Tạo Payment record #2 với paymentType = REMAINING_PAYMENT, amount =
     * totalAmount - depositPaid, status = SUCCESS.
     * - Chuyển Order sang COMPLETED.
     * - Chuyển Product sang SOLD.
     * - Ghi nhận doanh thu vào Sổ cái tài chính FinancialLedger.
     * - Bắn sự kiện OrderPaidEvent.
     *
     * Được gọi từ:
     * - OrderApiController.confirmRemainingPayment()
     * - OrderActionService.handleComplete() (khi đơn là DEPOSITED)
     *
     * Tác động DB:
     * - PAYMENT: thêm bản ghi REMAINING_PAYMENT (SUCCESS)
     * - ORDER: orderStatus: DEPOSITED → COMPLETED, completedAt = now
     * - PRODUCT: productStatus: RESERVED → SOLD
     * - FINANCIAL_LEDGER: ghi nhận doanh thu
     * - ORDER_LOG: log REMAINING_PAYMENT_CONFIRMED
     */
    @Transactional
    public boolean confirmRemainingPayment(String orderCode, String notes, User moderator) {
        if (notes != null && notes.trim().length() > 500) {
            throw new IllegalArgumentException("Ghi chú thanh toán không được vượt quá 500 ký tự.");
        }
        Order order = orderRepository.findByOrderCode(orderCode).orElse(null);
        if (order == null) {
            throw new IllegalArgumentException("Không tìm thấy đơn hàng: " + orderCode);
        }

        if (!"DEPOSITED".equalsIgnoreCase(order.getOrderStatus())) {
            throw new IllegalStateException(
                    "Đơn hàng phải ở trạng thái ĐÃ ĐẶT CỌC (DEPOSITED) mới được xác nhận thanh toán phần còn lại!");
        }

        validateAssignedModerator(order, moderator);

        // Tính tổng tiền deposit đã thanh toán thành công
        BigDecimal treePrice = resolveTreePrice(order);

        List<Payment> depositPayments = paymentRepository.findByOrderOrderIdAndPaymentType(order.getOrderId(),
                PaymentType.DEPOSIT.name());
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
        LocalDateTime completedAt = LocalDateTime.now();
        order.setOrderStatus("COMPLETED");
        order.setCompletedAt(completedAt);
        orderRepository.save(order);
        sendReviewReminderNotification(order);
        markProductsAsSold(order);
        recordCompletedRevenueLedger(order, moderator, completedAt);

        OrderLog logEntry = OrderLog.builder()
                .order(order)
                .actionBy(moderator)
                .actionType("REMAINING_PAYMENT_CONFIRMED")
                .fromStatus(oldStatus)
                .toStatus("COMPLETED")
                .actionAt(completedAt)
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
        if (normalized.compareTo(MAX_FEE_AMOUNT) > 0) {
            throw new IllegalArgumentException(label + " không được vượt quá 200.000.000 VNĐ.");
        }
        validateWholeNumberAmount(normalized, label);
        return normalized;
    }

    private void validateWholeNumberAmount(BigDecimal amount, String label) {
        if (amount != null && amount.stripTrailingZeros().scale() > 0) {
            throw new IllegalArgumentException(label + " phải là số nguyên.");
        }
    }

    /**
     * [GHI NHẬN KHÁCH HÀNG BÙNG CỌC VÀ TỊCH THU TIỀN CỌC (CUSTOMER NO-SHOW)]
     *
     * Mục đích:
     * - Khi đơn hàng ở trạng thái DEPOSITED nhưng khách hàng từ chối nhận cây khi
     * giao đến.
     * - Ghi nhận giữ lại toàn bộ số tiền cọc (Forfeited Deposit Income) vào Sổ cái
     * tài chính FinancialLedger.
     * - Cập nhật Order sang CANCELLED.
     * - Giải phóng cây về AVAILABLE để mở bán lại trên sàn.
     *
     * Được gọi từ:
     * - OrderApiController.markCustomerNoShow()
     * - OrderActionService.handleCustomerNoShow()
     *
     * Tác động DB:
     * - FINANCIAL_LEDGER: ghi nhận khoản thu tịch thu cọc
     * - ORDER: orderStatus: DEPOSITED → CANCELLED, notes
     * - PRODUCT: productStatus: RESERVED → AVAILABLE
     * - ORDER_LOG: log "FORFEITED_DEPOSIT_INCOME_RECORDED"
     */
    @Transactional
    public boolean markDepositedOrderCustomerNoShow(String orderCode, String notes, User moderator) {
        Order order = orderRepository.findByOrderCode(orderCode).orElse(null);
        if (order == null) {
            throw new IllegalArgumentException("Không tìm thấy đơn hàng: " + orderCode);
        }
        validateAssignedModerator(order, moderator);
        if (!"DEPOSITED".equalsIgnoreCase(order.getOrderStatus())) {
            throw new IllegalStateException(
                    "Chỉ có thể ghi nhận khách không nhận hàng sau khi khách đã thanh toán tiền đặt cọc.");
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

    /**
     * [GHI NHẬN HOÀN TIỀN DO LỖI NHÀ VƯỜN / VẬN CHUYỂN VÀ HỦY ĐƠN]
     *
     * Mục đích:
     * - Xử lý trường hợp cây bị gãy, hư hỏng trong quá trình vận chuyển hoặc do lỗi
     * nhà vườn sau khi khách đã cọc hoặc thanh toán.
     * - Ghi nhận bút toán hoàn tiền 100% thủ công vào Sổ cái tài chính
     * (FinancialLedger).
     * - Đổi Order sang CANCELLED và giải phóng cây về AVAILABLE.
     *
     * Được gọi từ:
     * - OrderActionService.handleFaultRefund()
     */
    @Transactional
    public boolean recordFaultRefundAndCancel(String orderCode, String faultPartyValue, BigDecimal refundAmount,
            String reason, String evidenceNote, String externalReference,
            Boolean customerKeepsTree, String productResolution,
            User moderator) {
        if (evidenceNote != null && evidenceNote.trim().length() > 1000) {
            throw new IllegalArgumentException("Minh chứng không được vượt quá 1000 ký tự.");
        }
        if (externalReference != null && externalReference.trim().length() > 255) {
            throw new IllegalArgumentException("Mã tham chiếu không được vượt quá 255 ký tự.");
        }
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

        // Tự động xác định số tiền hoàn 100% dựa trên tổng số tiền thực tế khách đã
        // thanh toán
        BigDecimal refundableCash = financialLedgerService.calculateRefundableCash(order);
        if (refundableCash == null || refundableCash.compareTo(ZERO) <= 0) {
            throw new IllegalStateException(
                    "Đơn hàng này không có khoản thanh toán thành công nào còn có thể hoàn tiền.");
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
                moderator);

        appendOrderNote(order,
                normalizedReason + " Hoàn tiền 100% chỉ được ghi nhận thủ công, không tự động chuyển khoản.");

        // Nghiệp vụ cố định: Đơn chuyển CANCELLED, khách không giữ cây, toàn bộ cây
        // trong đơn trả về AVAILABLE
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

    /**
     * [HOÀN TẤT ĐƠN HÀNG ĐÃ THANH TOÁN 100% VNPAY (COMPLETE PAID ORDER)]
     *
     * Mục đích:
     * - Dành cho đơn hàng đã thanh toán 100% qua VNPay (orderStatus = "PAID").
     * - Moderator xác nhận sau khi cây đã được giao thành công tới tay khách.
     * - Cập nhật Order sang COMPLETED, chuyển Product sang SOLD, ghi nhận doanh thu
     * vào Sổ cái.
     *
     * Được gọi từ:
     * - OrderApiController.completePaidOrder()
     * - OrderActionService.handleComplete() (khi đơn là PAID)
     *
     * Tác động DB:
     * - ORDER: orderStatus: PAID → COMPLETED, completedAt = now
     * - PRODUCT: productStatus: RESERVED → SOLD
     * - FINANCIAL_LEDGER: ghi nhận doanh thu hoàn tất
     * - ORDER_LOG: log "ORDER_COMPLETED"
     */
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
        order.setCompletedAt(completedAt);
        orderRepository.save(order);
        sendReviewReminderNotification(order);
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
        String trimmed = reason.trim();
        if (trimmed.length() > 500) {
            throw new IllegalArgumentException("Lý do không được vượt quá 500 ký tự.");
        }
        return trimmed;
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

    /**
     * [GHI NHẬN THANH TOÁN HOÀN TẤT THỦ CÔNG (DIRECT FINAL PAYMENT)]
     */
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

    private void sendReviewReminderNotification(Order order) {
        if (order != null && order.getCustomer() != null && order.getCustomer().getEmail() != null) {
            try {
                ModerationNotification notification = ModerationNotification.builder()
                        .targetUsername(order.getCustomer().getEmail())
                        .message("🎉 Đơn hàng " + order.getOrderCode()
                                + " đã được thanh toán đầy đủ / hoàn thành! Hãy đánh giá và cho sao (Review & Rating) cho cây bonsai bạn đã mua nhé!")
                        .isRead(false)
                        .createdAt(LocalDateTime.now())
                        .build();
                notificationRepository.save(notification);
            } catch (Exception e) {
                log.error("Failed to send review reminder notification for order: " + order.getOrderCode(), e);
            }
        }
    }
}
