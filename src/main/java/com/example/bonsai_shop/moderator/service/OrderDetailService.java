package com.example.bonsai_shop.moderator.service;

import com.example.bonsai_shop.entity.Order;
import com.example.bonsai_shop.entity.OrderDetail;
import com.example.bonsai_shop.entity.OrderHandling;
import com.example.bonsai_shop.entity.Payment;
import com.example.bonsai_shop.entity.Product;
import com.example.bonsai_shop.entity.User;
import com.example.bonsai_shop.exception.OrderNotFoundException;
import com.example.bonsai_shop.finance.dto.FinancialLedgerDTO;
import com.example.bonsai_shop.finance.service.FinancialLedgerService;
import com.example.bonsai_shop.moderator.dto.CustomerInfoDTO;
import com.example.bonsai_shop.moderator.dto.HandlingHistoryDTO;
import com.example.bonsai_shop.moderator.dto.OrderDetailDTO;
import com.example.bonsai_shop.moderator.dto.PaymentHistoryDTO;
import com.example.bonsai_shop.moderator.dto.PaymentSummaryDTO;
import com.example.bonsai_shop.moderator.dto.ProductSummaryDTO;
import com.example.bonsai_shop.moderator.dto.TimelineDTO;
import com.example.bonsai_shop.moderator.util.ModeratorDisplayLabelMapper;
import com.example.bonsai_shop.entity.OrderLog;
import com.example.bonsai_shop.product.repository.OrderHandlingRepository;
import com.example.bonsai_shop.product.repository.OrderLogRepository;
import com.example.bonsai_shop.product.repository.OrderRepository;
import com.example.bonsai_shop.product.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * [SERVICE TỔNG HỢP CHI TIẾT ĐƠN HÀNG CHO MODERATOR - ORDER DETAIL SERVICE]
 *
 * Chịu trách nhiệm:
 * - Tập hợp toàn bộ thông tin về một đơn hàng (OrderDetailDTO) phục vụ màn hình chi tiết:
 *   + Thông tin khách hàng (CustomerInfoDTO)
 *   + Danh sách tác phẩm cây bonsai (ProductSummaryDTO)
 *   + Tóm tắt tài chính thanh toán (PaymentSummaryDTO: Đã thu, Còn thiếu, Tiền cọc, Phí vận chuyển, Phí cẩu)
 *   + Lịch sử các lần thanh toán (PaymentHistoryDTO)
 *   + Dòng thời gian tiến trình đơn (TimelineDTO)
 *   + Lịch sử tiếp nhận & bàn giao của các Moderator (HandlingHistoryDTO)
 *   + Lịch sử nhật ký thay đổi trạng thái (OrderLog)
 *   + Phân quyền hiển thị các nút hành động (canApprove, canReject, canClaim, canReturnInventory, canComplete, canCustomerNoShow, canRecordFaultRefund).
 *
 * Các thành phần phối hợp chính:
 * - OrderRepository, PaymentRepository, OrderHandlingRepository, OrderLogRepository, FinancialLedgerService.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OrderDetailService {

    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final OrderHandlingRepository orderHandlingRepository;
    private final OrderLogRepository orderLogRepository;
    private final MyOrderService myOrderService;
    private final FinancialLedgerService financialLedgerService;

    /**
     * [LẤY ĐẦY ĐỦ CHI TIẾT ĐƠN HÀNG KÈM QUYỀN HÀNH ĐỘNG CỦA MODERATOR]
     *
     * Mục đích:
     * - Cung cấp toàn bộ dữ liệu phức hợp cho giao diện xem chi tiết / Drawer của Moderator.
     *
     * Được gọi từ:
     * - ModeratorOrderController.viewOrderDetail()
     * - ModeratorOrderController.getOrderDetailJson()
     */
    @Transactional(readOnly = true)
    public OrderDetailDTO getOrderDetailByCode(String orderCode, User currentModerator) {
        if (orderCode == null || orderCode.isBlank()) {
            throw new OrderNotFoundException("Mã đơn hàng không hợp lệ");
        }

        Order order = resolveOrder(orderCode);
        return buildOrderDetailDTO(order, currentModerator);
    }

    private Order resolveOrder(String orderCode) {
        String normalized = orderCode != null ? orderCode.trim() : null;
        if (normalized == null || normalized.isBlank()) {
            throw new OrderNotFoundException("Mã đơn hàng không hợp lệ");
        }

        Order order = orderRepository.findByOrderCodeWithDetails(normalized)
                .orElseGet(() -> orderRepository.findByOrderCode(normalized)
                        .orElseGet(() -> {
                            if (normalized.matches("\\d+")) {
                                return orderRepository.findById(Integer.parseInt(normalized)).orElse(null);
                            }
                            return null;
                        }));

        if (order == null) {
            throw new OrderNotFoundException("Không tìm thấy đơn hàng: " + normalized);
        }
        return order;
    }

    public OrderDetailDTO buildOrderDetailDTO(Order order, User currentModerator) {
        String custName = order.getCustomerName();
        String custPhone = order.getCustomerPhone();
        String custEmail = order.getCustomerEmail();
        String custAddress = order.getShippingAddress();

        try {
            if (order.getCustomer() != null) {
                if (custName == null || custName.isBlank())
                    custName = order.getCustomer().getFullName();
                if (custPhone == null || custPhone.isBlank())
                    custPhone = order.getCustomer().getPhone();
                if (custEmail == null || custEmail.isBlank())
                    custEmail = order.getCustomer().getEmail();
                if (custAddress == null || custAddress.isBlank())
                    custAddress = order.getCustomer().getAddress();
            }
        } catch (Exception ignored) {
        }

        if (custName == null || custName.isBlank())
            custName = "Khách hàng";

        CustomerInfoDTO customerInfo = CustomerInfoDTO.builder()
                .fullName(custName)
                .phone(custPhone != null ? custPhone : "-")
                .email(custEmail != null ? custEmail : "-")
                .address(custAddress != null ? custAddress : "-")
                .customerNote(null)
                .build();

        List<ProductSummaryDTO> productList = new ArrayList<>();
        BigDecimal computedTreePrice = BigDecimal.ZERO;

        if (order.getOrderDetails() != null && !order.getOrderDetails().isEmpty()) {
            for (OrderDetail detail : order.getOrderDetails()) {
                Product p = detail.getProduct();
                String pCode = p != null ? p.getProductCode() : "-";
                String pName = p != null ? p.getProductName() : "Bonsai";
                String catName = (p != null && p.getVariety() != null && p.getVariety().getCategory() != null)
                        ? p.getVariety().getCategory().getCategoryName()
                        : "Bonsai";
                String imgUrl = null;
                try {
                    if (p != null)
                        imgUrl = p.getFirstImageUrl();
                } catch (Exception ignored) {
                }

                BigDecimal price = detail.getPriceAtPurchase() != null ? detail.getPriceAtPurchase() : BigDecimal.ZERO;
                int qty = detail.getQuantity() != null ? detail.getQuantity() : 1;
                BigDecimal subtotal = price.multiply(BigDecimal.valueOf(qty));
                computedTreePrice = computedTreePrice.add(subtotal);

                productList.add(ProductSummaryDTO.builder()
                        .productId(p != null ? p.getProductId() : null)
                        .productCode(pCode)
                        .productName(pName)
                        .categoryName(catName)
                        .imageUrl(imgUrl)
                        .quantity(qty)
                        .price(price)
                        .deposit(order.getDepositAmount() != null ? order.getDepositAmount() : BigDecimal.ZERO)
                        .subtotal(subtotal)
                        .build());
            }
        }

        BigDecimal craneFee = order.getCraneFee() != null ? order.getCraneFee() : BigDecimal.ZERO;
        BigDecimal shippingFee = order.getShippingFee() != null ? order.getShippingFee() : BigDecimal.ZERO;
        BigDecimal grandTotal = order.getTotalAmount() != null ? order.getTotalAmount() : BigDecimal.ZERO;
        BigDecimal depositAmount = order.getDepositAmount() != null ? order.getDepositAmount() : BigDecimal.ZERO;

        if (computedTreePrice.compareTo(BigDecimal.ZERO) == 0) {
            computedTreePrice = grandTotal.subtract(craneFee).subtract(shippingFee);
            if (computedTreePrice.compareTo(BigDecimal.ZERO) < 0) {
                computedTreePrice = BigDecimal.ZERO;
            }
        }

        List<Payment> paymentEntities = paymentRepository.findByOrderOrderIdOrderByPaymentIdAsc(order.getOrderId());
        BigDecimal paidAmount = BigDecimal.ZERO;
        BigDecimal successfulDepositAmount = BigDecimal.ZERO;
        BigDecimal successfulRemainingPaymentAmount = BigDecimal.ZERO;
        BigDecimal successfulFullPaymentAmount = BigDecimal.ZERO;
        List<PaymentHistoryDTO> paymentHistoryList = new ArrayList<>();
        String paymentMethod = resolvePaymentMethod(paymentEntities, order);

        int payIndex = 1;
        if (paymentEntities != null && !paymentEntities.isEmpty()) {
            for (Payment p : paymentEntities) {
                BigDecimal amt = p.getAmount() != null ? p.getAmount() : BigDecimal.ZERO;
                String pStatus = p.getPaymentStatus() != null ? p.getPaymentStatus() : "PENDING";
                if ("SUCCESS".equalsIgnoreCase(pStatus) || "PAID".equalsIgnoreCase(pStatus)
                        || "COMPLETED".equalsIgnoreCase(pStatus)) {
                    paidAmount = paidAmount.add(amt);
                    if ("DEPOSIT".equalsIgnoreCase(p.getPaymentType())) {
                        successfulDepositAmount = successfulDepositAmount.add(amt);
                    } else if ("REMAINING_PAYMENT".equalsIgnoreCase(p.getPaymentType())) {
                        successfulRemainingPaymentAmount = successfulRemainingPaymentAmount.add(amt);
                    } else if ("FULL_PAYMENT".equalsIgnoreCase(p.getPaymentType())) {
                        successfulFullPaymentAmount = successfulFullPaymentAmount.add(amt);
                    }
                }

                paymentHistoryList.add(PaymentHistoryDTO.builder()
                        .paymentId(p.getPaymentId())
                        .paymentNumber(payIndex++)
                        .method(p.getPaymentMethod() != null ? p.getPaymentMethod() : "VNPay")
                        .methodLabel(ModeratorDisplayLabelMapper
                                .paymentMethodLabel(p.getPaymentMethod() != null ? p.getPaymentMethod() : "VNPAY"))
                        .paymentType(p.getPaymentType() != null ? p.getPaymentType() : "DEPOSIT")
                        .paymentTypeLabel(ModeratorDisplayLabelMapper
                                .paymentTypeLabel(p.getPaymentType() != null ? p.getPaymentType() : "DEPOSIT"))
                        .amount(amt)
                        .status(pStatus)
                        .statusLabel(ModeratorDisplayLabelMapper.paymentStatusLabel(pStatus))
                        .createdTime(p.getPaymentDate())
                        .transactionCode("PAY-" + p.getPaymentId())
                        .vnpayRef(p.getNotes() != null ? p.getNotes() : "-")
                        .notes(p.getNotes())
                        .build());
            }
        }

        BigDecimal totalCashReceived = paidAmount;
        BigDecimal remainingPaymentAmount = grandTotal.subtract(totalCashReceived);
        if (remainingPaymentAmount.compareTo(BigDecimal.ZERO) < 0) {
            remainingPaymentAmount = BigDecimal.ZERO;
        }
        BigDecimal recognizedCompletedRevenue = financialLedgerService.sumRecognizedCompletedRevenue(order);
        BigDecimal forfeitedDepositIncome = financialLedgerService.sumForfeitedDepositIncome(order);
        BigDecimal fullRefundAmount = financialLedgerService.sumFullRefunds(order);
        BigDecimal netRecognizedAmount = financialLedgerService.sumNetRecognizedAmount(order);
        BigDecimal refundableCash = financialLedgerService.calculateRefundableCash(order);
        List<FinancialLedgerDTO> ledgerHistory = financialLedgerService.getLedgerHistory(order.getOrderId());

        PaymentSummaryDTO paymentSummary = PaymentSummaryDTO.builder()
                .treePrice(computedTreePrice)
                .shippingFee(shippingFee)
                .craneFee(craneFee)
                .depositAmount(depositAmount)
                .paidAmount(paidAmount)
                .remainingPaymentAmount(remainingPaymentAmount)
                .grandTotal(grandTotal)
                .orderTotal(grandTotal)
                .successfulDepositAmount(successfulDepositAmount)
                .successfulRemainingPaymentAmount(successfulRemainingPaymentAmount)
                .successfulFullPaymentAmount(successfulFullPaymentAmount)
                .totalCashReceived(totalCashReceived)
                .recognizedCompletedRevenue(recognizedCompletedRevenue)
                .forfeitedDepositIncome(forfeitedDepositIncome)
                .fullRefundAmount(fullRefundAmount)
                .netRecognizedAmount(netRecognizedAmount)
                .refundableCash(refundableCash)
                .build();

        String currentStatus = order.getOrderStatus() != null ? order.getOrderStatus().toUpperCase() : "PENDING";
        List<TimelineDTO> timeline = buildOrderTimeline(order, currentStatus, paymentEntities);

        List<OrderHandling> handlings = orderHandlingRepository
                .findByOrderOrderIdOrderByHandledAtDesc(order.getOrderId());
        List<HandlingHistoryDTO> handlingHistoryList = new ArrayList<>();
        if (handlings != null) {
            for (OrderHandling h : handlings) {
                String modName = h.getModerator() != null ? h.getModerator().getFullName() : "System Moderator";
                handlingHistoryList.add(HandlingHistoryDTO.builder()
                        .handlingId(h.getOrderHandlingId())
                        .moderatorName(modName)
                        .action(h.getIsActive() != null && h.getIsActive() ? "Tiếp nhận đơn" : "Xử lý/Bàn giao")
                        .handledAt(h.getHandledAt())
                        .reason(h.getReleasedAt() != null ? "Bàn giao lại kho" : "Đang phụ trách")
                        .durationFormatted(h.getHandledAt() != null ? myOrderService.formatAge(h.getHandledAt()) : "-")
                        .build());
            }
        }

        Integer currentUserId = currentModerator != null ? currentModerator.getUserId() : null;
        Integer assignedUserId = order.getAssignedTo() != null ? order.getAssignedTo().getUserId() : null;
        boolean isAssignedToMe = currentUserId != null && currentUserId.equals(assignedUserId);

        boolean canReject = "PENDING".equals(currentStatus) && isAssignedToMe;
        boolean canApprove = "PENDING".equals(currentStatus) && isAssignedToMe;
        boolean canClaim = assignedUserId == null && "PENDING".equals(currentStatus);
        boolean canReturnInventory = isAssignedToMe && "PENDING".equals(currentStatus);
        boolean canUnclaim = canReturnInventory;
        boolean canComplete = ("PAID".equals(currentStatus) || "DEPOSITED".equals(currentStatus)) && isAssignedToMe;
        boolean canCancel = false;
        boolean hasForfeitedDeposit = forfeitedDepositIncome.compareTo(BigDecimal.ZERO) > 0;
        boolean canCustomerNoShow = "DEPOSITED".equals(currentStatus)
                && isAssignedToMe
                && successfulDepositAmount.compareTo(BigDecimal.ZERO) > 0
                && !hasForfeitedDeposit;
        boolean canRecordFaultRefund = ("DEPOSITED".equals(currentStatus) || "PAID".equals(currentStatus))
                && isAssignedToMe;

        String priority = myOrderService.calculatePriority(order);
        LocalDateTime statusTimestamp = order.getAssignedAt() != null ? order.getAssignedAt()
                : (order.getOrderDate() != null ? order.getOrderDate() : LocalDateTime.now());
        String ageFormatted = myOrderService.formatAge(statusTimestamp);
        String assignedName = order.getAssignedTo() != null ? order.getAssignedTo().getFullName() : "Chưa phân bổ";

        return OrderDetailDTO.builder()
                .orderId(order.getOrderId())
                .orderCode(order.getOrderCode())
                .orderStatus(currentStatus)
                .orderStatusLabel(ModeratorDisplayLabelMapper.orderStatusLabel(currentStatus))
                .paymentMethod(paymentMethod)
                .paymentMethodLabel(ModeratorDisplayLabelMapper.paymentMethodLabel(paymentMethod))
                .priority(priority)
                .priorityLabel(ModeratorDisplayLabelMapper.priorityLabel(priority))
                .orderType(order.getOrderType() != null ? order.getOrderType() : "ONLINE")
                .createdDate(order.getOrderDate())
                .assignedModeratorName(assignedName)
                .statusTimestamp(statusTimestamp)
                .ageFormatted(ageFormatted)
                .currentStage(currentStatus)
                .notes(order.getNotes())
                .internalNote(null)
                .canApprove(canApprove)
                .canReject(canReject)
                .canClaim(canClaim)
                .canUnclaim(canUnclaim)
                .canReturnInventory(canReturnInventory)
                .canComplete(canComplete)
                .canCancel(canCancel)
                .canCustomerNoShow(canCustomerNoShow)
                .canRecordFaultRefund(canRecordFaultRefund)
                .customerInfo(customerInfo)
                .products(productList)
                .paymentSummary(paymentSummary)
                .paymentHistory(paymentHistoryList)
                .ledgerHistory(ledgerHistory)
                .timeline(timeline)
                .handlingHistory(handlingHistoryList)
                .build();
    }

    private String resolvePaymentMethod(List<Payment> paymentEntities, Order order) {
        boolean isDeposit = paymentEntities != null && paymentEntities.stream()
                .anyMatch(p -> "DEPOSIT".equalsIgnoreCase(p.getPaymentType()) ||
                        "DEPOSIT".equalsIgnoreCase(p.getPaymentMethod()) ||
                        "COD".equalsIgnoreCase(p.getPaymentMethod()));

        if (!isDeposit && order != null && order.getDepositAmount() != null) {
            isDeposit = order.getDepositAmount().compareTo(BigDecimal.ZERO) > 0;
        }

        return isDeposit ? "DEPOSIT" : "VNPAY";
    }

    private List<TimelineDTO> buildOrderTimeline(Order order, String status, List<Payment> paymentEntities) {
        List<TimelineDTO> list = new ArrayList<>();
        boolean isCancelled = "CANCELLED".equals(status);
        LocalDateTime createdDate = order != null ? order.getOrderDate() : null;
        LocalDateTime assignedDate = order != null ? order.getAssignedAt() : null;

        List<OrderLog> logs = order != null && order.getOrderId() != null
                ? orderLogRepository.findByOrderOrderIdOrderByActionAtAsc(order.getOrderId())
                : List.of();

        LocalDateTime approvedAt = logs.stream()
                .filter(l -> "PENDING_PAYMENT".equalsIgnoreCase(l.getToStatus())
                        || "APPROVED".equalsIgnoreCase(l.getActionType())
                        || "DEPOSITED".equalsIgnoreCase(l.getToStatus())
                        || "PAID".equalsIgnoreCase(l.getToStatus())
                        || "COMPLETED".equalsIgnoreCase(l.getToStatus()))
                .map(OrderLog::getActionAt)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);

        LocalDateTime depositedOrPaidAt = paymentEntities != null ? paymentEntities.stream()
                .filter(p -> "SUCCESS".equalsIgnoreCase(p.getPaymentStatus()))
                .map(Payment::getPaymentDate)
                .filter(Objects::nonNull)
                .findFirst()
                .orElseGet(() -> logs.stream()
                        .filter(l -> "DEPOSITED".equalsIgnoreCase(l.getToStatus())
                                || "PAID".equalsIgnoreCase(l.getToStatus()))
                        .map(OrderLog::getActionAt)
                        .filter(Objects::nonNull)
                        .findFirst()
                        .orElse(null))
                : null;

        LocalDateTime completedAt = logs.stream()
                .filter(l -> "COMPLETED".equalsIgnoreCase(l.getToStatus())
                        || "ORDER_COMPLETED".equalsIgnoreCase(l.getActionType()))
                .map(OrderLog::getActionAt)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);

        LocalDateTime cancelledAt = logs.stream()
                .filter(l -> "CANCELLED".equalsIgnoreCase(l.getToStatus()))
                .map(OrderLog::getActionAt)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);

        list.add(TimelineDTO.builder().stage("CREATED").label("Khởi tạo đơn hàng").timestamp(createdDate)
                .completed(true).current("PENDING".equals(status) && assignedDate == null).build());
        list.add(TimelineDTO.builder().stage("CLAIMED").label("Tiếp nhận đơn").timestamp(assignedDate)
                .completed(assignedDate != null).current("PENDING".equals(status) && assignedDate != null).build());
        list.add(
                TimelineDTO.builder().stage("APPROVED").label("Đã phê duyệt").timestamp(approvedAt)
                        .completed("PENDING_PAYMENT".equals(status) || "DEPOSITED".equals(status)
                                || "PAID".equals(status) || "COMPLETED".equals(status))
                        .current("PENDING_PAYMENT".equals(status)).build());
        list.add(TimelineDTO.builder().stage("DEPOSITED").label("Đã đặt cọc/Thanh toán").timestamp(depositedOrPaidAt)
                .completed("DEPOSITED".equals(status) || "PAID".equals(status) || "COMPLETED".equals(status))
                .current("DEPOSITED".equals(status) || "PAID".equals(status)).build());

        if (isCancelled) {
            list.add(TimelineDTO.builder().stage("CANCELLED").label("Đã huỷ đơn").timestamp(cancelledAt).completed(true)
                    .current(true).build());
        } else {
            list.add(TimelineDTO.builder().stage("COMPLETED").label("Hoàn thành").timestamp(completedAt)
                    .completed("COMPLETED".equals(status)).current("COMPLETED".equals(status)).build());
        }

        return list;
    }
}
