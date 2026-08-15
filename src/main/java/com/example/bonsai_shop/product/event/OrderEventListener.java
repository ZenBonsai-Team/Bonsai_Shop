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

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderEventListener {
    private final MailService mailService;
    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleOrderCreatedEvent(OrderCreatedEvent event) {
        log.info("Bắt đầu xử lý gửi email xác nhận tạo đơn hàng: {}", event.getOrder().getOrderCode());
        mailService.sendOrderCreatedEmail(event.getOrder());
    }

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

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleOrderRejectedEvent(OrderRejectedEvent event) {
        log.info("Bắt đầu xử lý gửi email hủy đơn hàng: {}", event.getOrder().getOrderCode());
        mailService.sendOrderRejectedEmail(event.getOrder(), event.getReason());
    }

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
