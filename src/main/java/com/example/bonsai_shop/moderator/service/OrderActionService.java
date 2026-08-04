package com.example.bonsai_shop.moderator.service;

import com.example.bonsai_shop.entity.Order;
import com.example.bonsai_shop.entity.OrderHandling;
import com.example.bonsai_shop.entity.User;
import com.example.bonsai_shop.exception.OrderNotFoundException;
import com.example.bonsai_shop.moderator.dto.OrderActionRequestDTO;
import com.example.bonsai_shop.product.repository.OrderHandlingRepository;
import com.example.bonsai_shop.product.repository.OrderRepository;
import com.example.bonsai_shop.product.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderActionService {

    private final OrderRepository orderRepository;
    private final OrderHandlingRepository orderHandlingRepository;
    private final OrderService orderService;

    @Transactional
    public Map<String, Object> executeAction(String orderCode, OrderActionRequestDTO request, User moderator) {
        if (request == null || request.getAction() == null || request.getAction().isBlank()) {
            throw new IllegalArgumentException("Hành động không hợp lệ.");
        }

        Order order = resolveOrder(orderCode);
        String status = order.getOrderStatus() != null ? order.getOrderStatus().toUpperCase() : "PENDING";
        Integer moderatorId = moderator != null ? moderator.getUserId() : null;
        Integer assignedId = order.getAssignedTo() != null ? order.getAssignedTo().getUserId() : null;
        boolean isAssignedToMe = moderatorId != null && moderatorId.equals(assignedId);
        String action = request.getAction().trim().toLowerCase();

        return switch (action) {
            case "claim" -> handleClaim(order, status, moderator, moderatorId);
            case "approve" -> handleApprove(order, status, moderator, isAssignedToMe, request);
            case "reject" -> handleReject(order, status, moderator, isAssignedToMe, request.getReason());
            case "return_inventory", "unclaim" -> handleReturnInventory(order, status, moderator, isAssignedToMe);
            case "complete" -> handleComplete(order, status, moderator, isAssignedToMe, request.getReason());
            case "customer_no_show" -> handleCustomerNoShow(order, status, moderator, isAssignedToMe, request.getReason());
            case "record_fault_refund", "fault_refund" -> handleFaultRefund(order, status, moderator, isAssignedToMe, request);
            case "cancel" -> throw new IllegalStateException("Hành động huỷ không còn hợp lệ trên trang chi tiết đơn hàng.");
            default -> throw new IllegalArgumentException("Hành động không hợp lệ: " + request.getAction());
        };
    }

    private Map<String, Object> handleClaim(Order order, String status, User moderator, Integer moderatorId) {
        if (order.getAssignedTo() != null) {
            throw new IllegalStateException("Đơn hàng này đã có người nhận.");
        }
        if (!"PENDING".equals(status)) {
            throw new IllegalStateException("Không thể tiếp nhận đơn hàng vì trạng thái đơn hàng không còn phù hợp.");
        }

        order.setAssignedTo(moderator);
        order.setAssignedAt(LocalDateTime.now());
        createHandling(order, moderator);
        orderRepository.save(order);
        log.info("[ACTION] claim - order={} by moderator={}", order.getOrderCode(), moderatorId);
        return success(order.getOrderCode(), "claim", order.getOrderStatus());
    }

    private Map<String, Object> handleApprove(Order order, String status, User moderator,
                                              boolean isAssignedToMe, OrderActionRequestDTO request) {
        if (!isAssignedToMe) {
            throw new IllegalStateException("Bạn không phụ trách đơn này.");
        }
        if (!"PENDING".equals(status)) {
            throw new IllegalStateException("Chỉ có thể duyệt đơn hàng đang chờ kiểm duyệt.");
        }

        boolean success = orderService.verifyOrder(
                order.getOrderCode(),
                request.getCraneFee(),
                request.getShippingFee(),
                request.getDepositAmount(),
                moderator
        );
        if (!success) {
            throw new IllegalStateException("Không thể duyệt đơn hàng.");
        }

        log.info("[ACTION] approve - order={}", order.getOrderCode());
        return success(order.getOrderCode(), "approve", "PENDING_PAYMENT");
    }

    private Map<String, Object> handleReject(Order order, String status, User moderator,
                                             boolean isAssignedToMe, String reason) {
        if (!isAssignedToMe) {
            throw new IllegalStateException("Bạn không phụ trách đơn này.");
        }
        if (!"PENDING".equals(status)) {
            throw new IllegalStateException("Chỉ có thể từ chối đơn hàng đang chờ kiểm duyệt.");
        }

        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("Lý do từ chối là bắt buộc.");
        }

        boolean rejected = orderService.rejectOrder(order.getOrderCode(), normalizeReason(reason), moderator);
        if (!rejected) {
            throw new IllegalStateException("Không thể từ chối đơn hàng.");
        }

        log.info("[ACTION] reject - order={} reason={}", order.getOrderCode(), reason);
        return success(order.getOrderCode(), "reject", "CANCELLED");
    }

    private Map<String, Object> handleReturnInventory(Order order, String status, User moderator,
                                                      boolean isAssignedToMe) {
        if (!isAssignedToMe) {
            throw new IllegalStateException("Bạn không phụ trách đơn này.");
        }
        if (!"PENDING".equals(status)) {
            throw new IllegalStateException("Không thể trả lại kho chung sau khi đơn hàng đã được duyệt.");
        }

        closeHandling(order);
        order.setAssignedTo(null);
        order.setAssignedAt(null);
        orderRepository.save(order);
        log.info("[ACTION] return_inventory - order={}", order.getOrderCode());
        return success(order.getOrderCode(), "return_inventory", order.getOrderStatus());
    }

    private Map<String, Object> handleComplete(Order order, String status, User moderator,
                                               boolean isAssignedToMe, String reason) {
        if (!isAssignedToMe) {
            throw new IllegalStateException("Bạn không phụ trách đơn này.");
        }
        if (!"PAID".equals(status) && !"DEPOSITED".equals(status)) {
            throw new IllegalStateException("Chỉ hoàn thành đơn khi khách đã thanh toán.");
        }

        if ("DEPOSITED".equals(status)) {
            String notes = normalizeReason(reason);
            orderService.confirmRemainingPayment(
                    order.getOrderCode(),
                    notes.isBlank() ? "Moderator confirmed remaining payment" : notes,
                    moderator
            );
        } else {
            orderService.completePaidOrder(order.getOrderCode(), moderator);
        }
        closeHandling(order);
        log.info("[ACTION] complete - order={}", order.getOrderCode());
        return success(order.getOrderCode(), "complete", "DEPOSITED".equals(status) ? "PAID" : "COMPLETED");
    }

    private Map<String, Object> handleCustomerNoShow(Order order, String status, User moderator,
                                                     boolean isAssignedToMe, String reason) {
        if (!isAssignedToMe) {
            throw new IllegalStateException("Bạn không phụ trách đơn này.");
        }
        if (!"DEPOSITED".equals(status)) {
            throw new IllegalStateException("Chỉ có thể ghi nhận khách không nhận hàng sau khi khách đã thanh toán tiền đặt cọc.");
        }
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("Lý do khách không nhận hàng là bắt buộc.");
        }

        orderService.markDepositedOrderCustomerNoShow(order.getOrderCode(), normalizeReason(reason), moderator);
        closeHandling(order);
        log.info("[ACTION] customer_no_show - order={}", order.getOrderCode());
        return success(order.getOrderCode(), "customer_no_show", "CANCELLED");
    }

    private Map<String, Object> handleFaultRefund(Order order, String status, User moderator,
                                                  boolean isAssignedToMe, OrderActionRequestDTO request) {
        if (!isAssignedToMe) {
            throw new IllegalStateException("Bạn không phụ trách đơn này.");
        }
        if (!"DEPOSITED".equals(status) && !"PAID".equals(status) && !"COMPLETED".equals(status)) {
            throw new IllegalStateException("Chỉ có thể ghi nhận hoàn tiền khi đơn đã có khoản thanh toán thành công.");
        }

        orderService.recordFaultRefundAndCancel(
                order.getOrderCode(),
                request.getFaultParty(),
                request.getRefundAmount(),
                request.getReason(),
                request.getEvidenceNote(),
                request.getExternalReference(),
                request.getCustomerKeepsTree(),
                request.getProductResolution(),
                moderator
        );
        closeHandling(order);
        log.info("[ACTION] record_fault_refund - order={}", order.getOrderCode());
        return success(order.getOrderCode(), "record_fault_refund", "CANCELLED");
    }

    private Map<String, Object> success(String orderCode, String action, String newStatus) {
        return Map.of(
                "success", true,
                "orderCode", orderCode,
                "action", action,
                "newStatus", newStatus
        );
    }

    private String normalizeReason(String reason) {
        return reason == null ? "" : reason.trim();
    }

    private void createHandling(Order order, User moderator) {
        OrderHandling handling = OrderHandling.builder()
                .order(order)
                .moderator(moderator)
                .handledAt(LocalDateTime.now())
                .isActive(true)
                .build();
        orderHandlingRepository.save(handling);
    }

    private Order resolveOrder(String orderCode) {
        String normalized = orderCode != null ? orderCode.trim() : null;
        if (normalized == null || normalized.isBlank()) {
            throw new OrderNotFoundException("Mã đơn hàng không hợp lệ");
        }

        Order order = orderRepository.findByOrderCode(normalized)
                .orElseGet(() -> {
                    if (normalized.matches("\\d+")) {
                        return orderRepository.findById(Integer.parseInt(normalized)).orElse(null);
                    }
                    return null;
                });

        if (order == null) {
            throw new OrderNotFoundException("Không tìm thấy đơn hàng: " + normalized);
        }
        return order;
    }

    private void closeHandling(Order order) {
        orderHandlingRepository.findByOrderOrderIdOrderByHandledAtDesc(order.getOrderId())
                .stream()
                .filter(h -> Boolean.TRUE.equals(h.getIsActive()))
                .findFirst()
                .ifPresent(h -> {
                    h.setReleasedAt(LocalDateTime.now());
                    h.setIsActive(false);
                    orderHandlingRepository.save(h);
                });
    }
}
