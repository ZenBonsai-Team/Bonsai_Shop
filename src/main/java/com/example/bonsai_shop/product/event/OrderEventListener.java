package com.example.bonsai_shop.product.event;

import com.example.bonsai_shop.entity.Order;
import com.example.bonsai_shop.product.repository.OrderRepository;
import com.example.bonsai_shop.product.repository.PaymentRepository;
import com.example.bonsai_shop.product.service.MailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * [LISTENER LẮNG NGHE VÀ XỬ LÝ SỰ KIỆN ĐƠN HÀNG - ORDER EVENT LISTENER]
 *
 * Chịu trách nhiệm:
 * - Lắng nghe các domain event liên quan đến vòng đời đơn hàng sau khi Transaction đã commit thành công (@TransactionalEventListener AFTER_COMMIT).
 * - Thực thi bất đồng bộ (@Async) để không làm block luồng xử lý chính của người dùng.
 * - Điều phối việc gửi email thông báo cho khách hàng qua MailService.
 *
 * Các sự kiện xử lý:
 * 1. OrderCreatedEvent: Gửi email tiếp nhận đơn hàng mới (sendOrderCreatedEmail).
 * 2. OrderVerifiedEvent: Gửi email duyệt đơn kèm link thanh toán VNPay (sendOrderApprovedEmail).
 * 3. OrderRejectedEvent: Gửi email thông báo từ chối/hủy đơn kèm lý do (sendOrderRejectedEmail).
 * 4. OrderPaidEvent: Gửi email hóa đơn xác nhận thanh toán đủ 100% (sendOrderFinalReceiptEmail).
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OrderEventListener {
    private final MailService mailService;
    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;

    /**
     * [LẮNG NGHE SỰ KIỆN ĐƠN HÀNG VỪA TẠO (ORDER CREATED)]
     *
     * Khi nào được kích hoạt:
     * - OrderService.createOrder() hoàn tất và transaction checkout commit thành công.
     *
     * Hành động:
     * - Gửi email thông báo đơn hàng đang chờ Moderator liên hệ và duyệt.
     */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleOrderCreatedEvent(OrderCreatedEvent event) {
        log.info("Bắt đầu xử lý gửi email xác nhận tạo đơn hàng: {}", event.getOrder().getOrderCode());
        mailService.sendOrderCreatedEmail(event.getOrder());
    }

    /**
     * [LẮNG NGHE SỰ KIỆN ĐƠN HÀNG ĐƯỢC DUYỆT (ORDER VERIFIED)]
     *
     * Khi nào được kích hoạt:
     * - OrderService.verifyOrder() hoàn tất và transaction duyệt đơn commit thành công.
     *
     * Hành động:
     * - Tải lại thông tin đơn hàng và Payment PENDING mới nhất trong transaction mới (REQUIRES_NEW).
     * - Gửi email thông báo duyệt đơn kèm link thanh toán VNPay (/vnpay/pay-order?orderCode=...).
     */
    @Async
    @Transactional(readOnly = true, propagation = Propagation.REQUIRES_NEW)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleOrderVerifiedEvent(OrderVerifiedEvent event) {
        String orderCode = event.getOrder().getOrderCode();
        log.info("Bắt đầu xử lý gửi email phê duyệt đơn hàng: {}", orderCode);

        Order order = orderRepository.findByOrderCodeWithDetails(orderCode).orElse(event.getOrder());
        if (order.getOrderId() != null) {
            order.setPayments(paymentRepository.findByOrderOrderIdOrderByPaymentIdAsc(order.getOrderId()));
        }

        mailService.sendOrderApprovedEmail(order);
    }

    /**
     * [LẮNG NGHE SỰ KIỆN ĐƠN HÀNG BỊ TỪ CHỐI / HỦY (ORDER REJECTED)]
     *
     * Khi nào được kích hoạt:
     * - OrderService.rejectOrder(), OrderExpirationService.cancelSingleOrder(), hoặc markDepositedOrderCustomerNoShow().
     *
     * Hành động:
     * - Gửi email thông báo đơn đã hủy kèm lý do chi tiết cho khách.
     */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleOrderRejectedEvent(OrderRejectedEvent event) {
        log.info("Bắt đầu xử lý gửi email hủy đơn hàng: {}", event.getOrder().getOrderCode());
        mailService.sendOrderRejectedEmail(event.getOrder(), event.getReason());
    }

    /**
     * [LẮNG NGHE SỰ KIỆN ĐƠN HÀNG ĐÃ THANH TOÁN 100% (ORDER PAID)]
     *
     * Khi nào được kích hoạt:
     * - OrderService.processPaymentSuccess() (khi thanh toán FULL_PAYMENT) hoặc confirmRemainingPayment() (khi thu nốt tiền mặt đợt 2).
     *
     * Hành động:
     * - Gửi email hóa đơn xác nhận hoàn tất thanh toán 100% cho khách hàng.
     */
    @Async
    @Transactional(readOnly = true, propagation = Propagation.REQUIRES_NEW)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleOrderPaidEvent(OrderPaidEvent event) {
        String orderCode = event.getOrder().getOrderCode();
        log.info("Bắt đầu xử lý gửi email xác nhận thanh toán 100%: {}", orderCode);
        Order order = orderRepository.findByOrderCodeWithDetails(orderCode).orElse(event.getOrder());
        mailService.sendOrderFinalReceiptEmail(order);
    }
}
