package com.example.bonsai_shop.product.event;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.example.bonsai_shop.product.service.MailService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderEventListener {
    private final MailService mailService;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleOrderCreatedEvent(OrderCreatedEvent event) {
        log.info("Bắt đầu xử lý gửi email xác nhận tạo đơn hàng: {}", event.getOrder().getOrderCode());
        mailService.sendOrderCreatedEmail(event.getOrder());
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleOrderVerifiedEvent(OrderVerifiedEvent event) {
        log.info("Bắt đầu xử lý gửi email phê duyệt đơn hàng: {}", event.getOrder().getOrderCode());
        mailService.sendOrderApprovedEmail(event.getOrder());
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleOrderRejectedEvent(OrderRejectedEvent event) {
        log.info("Bắt đầu xử lý gửi email từ chối đơn hàng: {}", event.getOrder().getOrderCode());
        mailService.sendOrderRejectedEmail(event.getOrder(), event.getReason());
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleOrderPaidEvent(OrderPaidEvent event) {
        log.info("Bắt đầu xử lý gửi email xác nhận thanh toán 100%: {}", event.getOrder().getOrderCode());
        mailService.sendOrderFinalReceiptEmail(event.getOrder());
    }
}
