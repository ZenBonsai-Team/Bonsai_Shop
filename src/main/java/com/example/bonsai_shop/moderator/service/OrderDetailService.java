package com.example.bonsai_shop.moderator.service;

import com.example.bonsai_shop.entity.Order;
import com.example.bonsai_shop.entity.OrderDetail;
import com.example.bonsai_shop.entity.OrderHandling;
import com.example.bonsai_shop.entity.Payment;
import com.example.bonsai_shop.entity.Product;
import com.example.bonsai_shop.entity.User;
import com.example.bonsai_shop.exception.OrderNotFoundException;
import com.example.bonsai_shop.moderator.dto.CustomerInfoDTO;
import com.example.bonsai_shop.moderator.dto.HandlingHistoryDTO;
import com.example.bonsai_shop.moderator.dto.OrderDetailDTO;
import com.example.bonsai_shop.moderator.dto.PaymentHistoryDTO;
import com.example.bonsai_shop.moderator.dto.PaymentSummaryDTO;
import com.example.bonsai_shop.moderator.dto.ProductSummaryDTO;
import com.example.bonsai_shop.moderator.dto.TimelineDTO;
import com.example.bonsai_shop.product.repository.OrderHandlingRepository;
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

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderDetailService {

    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final OrderHandlingRepository orderHandlingRepository;
    private final MyOrderService myOrderService;

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
                if (custName == null || custName.isBlank()) custName = order.getCustomer().getFullName();
                if (custPhone == null || custPhone.isBlank()) custPhone = order.getCustomer().getPhone();
                if (custEmail == null || custEmail.isBlank()) custEmail = order.getCustomer().getEmail();
                if (custAddress == null || custAddress.isBlank()) custAddress = order.getCustomer().getAddress();
            }
        } catch (Exception ignored) {
        }

        if (custName == null || custName.isBlank()) custName = "Khách hàng";

        CustomerInfoDTO customerInfo = CustomerInfoDTO.builder()
                .fullName(custName)
                .phone(custPhone != null ? custPhone : "-")
                .email(custEmail != null ? custEmail : "-")
                .address(custAddress != null ? custAddress : "-")
                .customerNote(order.getNotes())
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
                    if (p != null) imgUrl = p.getFirstImageUrl();
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
        List<PaymentHistoryDTO> paymentHistoryList = new ArrayList<>();
        String paymentMethod = resolvePaymentMethod(paymentEntities, order);

        int payIndex = 1;
        if (paymentEntities != null && !paymentEntities.isEmpty()) {
            for (Payment p : paymentEntities) {
                BigDecimal amt = p.getAmount() != null ? p.getAmount() : BigDecimal.ZERO;
                String pStatus = p.getPaymentStatus() != null ? p.getPaymentStatus() : "PENDING";
                if ("SUCCESS".equalsIgnoreCase(pStatus) || "PAID".equalsIgnoreCase(pStatus) || "COMPLETED".equalsIgnoreCase(pStatus)) {
                    paidAmount = paidAmount.add(amt);
                }

                paymentHistoryList.add(PaymentHistoryDTO.builder()
                        .paymentId(p.getPaymentId())
                        .paymentNumber(payIndex++)
                        .method(p.getPaymentMethod() != null ? p.getPaymentMethod() : "VNPay")
                        .paymentType(p.getPaymentType() != null ? p.getPaymentType() : "DEPOSIT")
                        .amount(amt)
                        .status(pStatus)
                        .createdTime(p.getPaymentDate())
                        .transactionCode("PAY-" + p.getPaymentId())
                        .vnpayRef(p.getNotes() != null ? p.getNotes() : "-")
                        .notes(p.getNotes())
                        .build());
            }
        }

        BigDecimal remainingPaymentAmount = grandTotal.subtract(depositAmount);
        if (remainingPaymentAmount.compareTo(BigDecimal.ZERO) < 0) {
            remainingPaymentAmount = BigDecimal.ZERO;
        }

        PaymentSummaryDTO paymentSummary = PaymentSummaryDTO.builder()
                .treePrice(computedTreePrice)
                .shippingFee(shippingFee)
                .craneFee(craneFee)
                .depositAmount(depositAmount)
                .paidAmount(paidAmount)
                .remainingPaymentAmount(remainingPaymentAmount)
                .grandTotal(grandTotal)
                .build();

        String currentStatus = order.getOrderStatus() != null ? order.getOrderStatus().toUpperCase() : "PENDING";
        List<TimelineDTO> timeline = buildOrderTimeline(currentStatus, order.getOrderDate(), order.getAssignedAt());

        List<OrderHandling> handlings = orderHandlingRepository.findByOrderOrderIdOrderByHandledAtDesc(order.getOrderId());
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

        String priority = myOrderService.calculatePriority(order);
        LocalDateTime statusTimestamp = order.getAssignedAt() != null ? order.getAssignedAt()
                : (order.getOrderDate() != null ? order.getOrderDate() : LocalDateTime.now());
        String ageFormatted = myOrderService.formatAge(statusTimestamp);
        String assignedName = order.getAssignedTo() != null ? order.getAssignedTo().getFullName() : "Chưa phân bổ";

        return OrderDetailDTO.builder()
                .orderId(order.getOrderId())
                .orderCode(order.getOrderCode())
                .orderStatus(currentStatus)
                .paymentMethod(paymentMethod)
                .priority(priority)
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
                .customerInfo(customerInfo)
                .products(productList)
                .paymentSummary(paymentSummary)
                .paymentHistory(paymentHistoryList)
                .timeline(timeline)
                .handlingHistory(handlingHistoryList)
                .build();
    }

    private String resolvePaymentMethod(List<Payment> paymentEntities, Order order) {
        boolean isDeposit = paymentEntities != null && paymentEntities.stream().anyMatch(p ->
                "DEPOSIT".equalsIgnoreCase(p.getPaymentType()) ||
                "DEPOSIT".equalsIgnoreCase(p.getPaymentMethod()) ||
                "COD".equalsIgnoreCase(p.getPaymentMethod())
        );

        if (!isDeposit && order != null && order.getDepositAmount() != null) {
            isDeposit = order.getDepositAmount().compareTo(BigDecimal.ZERO) > 0;
        }

        return isDeposit ? "COD" : "VNPAY";
    }

    private List<TimelineDTO> buildOrderTimeline(String status, LocalDateTime createdDate, LocalDateTime assignedDate) {
        List<TimelineDTO> list = new ArrayList<>();
        boolean isCancelled = "CANCELLED".equals(status);

        list.add(TimelineDTO.builder().stage("CREATED").label("Khởi tạo đơn hàng").timestamp(createdDate).completed(true).current("PENDING".equals(status) && assignedDate == null).build());
        list.add(TimelineDTO.builder().stage("CLAIMED").label("Tiếp nhận đơn").timestamp(assignedDate).completed(assignedDate != null).current("PENDING".equals(status) && assignedDate != null).build());
        list.add(TimelineDTO.builder().stage("APPROVED").label("Đã phê duyệt").timestamp(null).completed("PENDING_PAYMENT".equals(status) || "DEPOSITED".equals(status) || "PAID".equals(status) || "COMPLETED".equals(status)).current("PENDING_PAYMENT".equals(status)).build());
        list.add(TimelineDTO.builder().stage("DEPOSITED").label("Đã đặt cọc/Thanh toán").timestamp(null).completed("DEPOSITED".equals(status) || "PAID".equals(status) || "COMPLETED".equals(status)).current("DEPOSITED".equals(status) || "PAID".equals(status)).build());

        if (isCancelled) {
            list.add(TimelineDTO.builder().stage("CANCELLED").label("Đã huỷ đơn").timestamp(null).completed(true).current(true).build());
        } else {
            list.add(TimelineDTO.builder().stage("COMPLETED").label("Hoàn thành").timestamp(null).completed("COMPLETED".equals(status)).current("COMPLETED".equals(status)).build());
        }

        return list;
    }
}
