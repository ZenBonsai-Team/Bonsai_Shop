package com.example.bonsai_shop.product.service;

import com.example.bonsai_shop.entity.Order;
import com.example.bonsai_shop.entity.OrderDetail;
import com.example.bonsai_shop.entity.OrderHandling;
import com.example.bonsai_shop.entity.Payment;
import com.example.bonsai_shop.entity.Product;
import com.example.bonsai_shop.product.repository.OrderHandlingRepository;
import com.example.bonsai_shop.product.repository.OrderRepository;
import com.example.bonsai_shop.product.repository.PaymentRepository;
import com.example.bonsai_shop.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderExpirationService {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final PaymentRepository paymentRepository;
    private final OrderHandlingRepository orderHandlingRepository;
    private final MailService mailService;

    @Value("${order.expiration.in-person-minutes:1440}")
    private long inPersonExpirationMinutes;

    @Transactional
    public void cancelExpiredOrders() {
        LocalDateTime now = LocalDateTime.now();

        LocalDateTime onlineCutoff = now.minusMinutes(15);
        List<Order> expiredOnlineOrders = orderRepository.findExpiredOnlineOrders(onlineCutoff);
        for (Order order : expiredOnlineOrders) {
            cancelSingleOrder(order, "Tự động hủy: Đơn hàng online quá hạn 15 phút chưa thanh toán qua VNPay", "CANCELLED");
        }

        LocalDateTime offlineCutoff = now.minusHours(48);
        List<Order> expiredOfflineOrders = orderRepository.findExpiredOfflineOrders(offlineCutoff);
        for (Order order : expiredOfflineOrders) {
            cancelSingleOrder(order, "Tự động hủy: Đơn hàng quá hạn 48 giờ chưa chuẩn bị/thanh toán tiền", "CANCELLED");
        }

        LocalDateTime inPersonCutoff = now.minusMinutes(inPersonExpirationMinutes);
        List<Order> expiredInPersonOrders = orderRepository.findExpiredInPersonOrders(inPersonCutoff);
        for (Order order : expiredInPersonOrders) {
            cancelSingleOrder(order, "Tự động hủy: In-person order quá hạn " + inPersonExpirationMinutes + " phút chưa xác nhận thanh toán", "CANCELLED");
        }
    }

    private void cancelSingleOrder(Order order, String reason, String expiredStatus) {
        log.info("Tự động xử lý đơn hàng quá hạn [{}]: {}", order.getOrderCode(), reason);

        order.setOrderStatus(expiredStatus);
        String currentNotes = order.getNotes();
        order.setNotes(currentNotes != null && !currentNotes.isEmpty() ? currentNotes + " | " + reason : reason);
        orderRepository.save(order);

        expirePendingPayments(order);
        releaseOrderHandlings(order);
        releaseProducts(order);
        if (!"IN_PERSON".equalsIgnoreCase(order.getOrderType())) {
            sendExpirationEmail(order, reason);
        }
    }

    private void expirePendingPayments(Order order) {
        if (order.getOrderId() == null) {
            return;
        }
        List<Payment> payments = paymentRepository.findByOrderOrderIdOrderByPaymentIdAsc(order.getOrderId());
        if (payments == null) {
            return;
        }
        for (Payment payment : payments) {
            if ("PENDING".equalsIgnoreCase(payment.getPaymentStatus())) {
                payment.setPaymentStatus("EXPIRED");
                paymentRepository.save(payment);
                log.info("Cập nhật Payment [#{} - {}] -> EXPIRED", payment.getPaymentId(), payment.getPaymentType());
            }
        }
    }

    private void releaseOrderHandlings(Order order) {
        if (order.getOrderId() == null) {
            return;
        }
        List<OrderHandling> handlings = orderHandlingRepository.findByOrderOrderIdOrderByHandledAtDesc(order.getOrderId());
        if (handlings == null) {
            return;
        }
        for (OrderHandling handling : handlings) {
            if (Boolean.TRUE.equals(handling.getIsActive())) {
                handling.setIsActive(false);
                handling.setReleasedAt(LocalDateTime.now());
                orderHandlingRepository.save(handling);
                log.info("Giải phóng OrderHandling [#{} - Mod: {}]", handling.getOrderHandlingId(),
                        handling.getModerator() != null ? handling.getModerator().getUsername() : "N/A");
            }
        }
    }

    private void releaseProducts(Order order) {
        if (order.getOrderDetails() == null) {
            return;
        }
        for (OrderDetail detail : order.getOrderDetails()) {
            Product product = detail.getProduct();
            if (product != null && !"AVAILABLE".equalsIgnoreCase(product.getProductStatus())) {
                product.setProductStatus("AVAILABLE");
                productRepository.save(product);
                log.info("Giải phóng sản phẩm [ID: {} - {}] về trạng thái AVAILABLE", product.getProductId(), product.getProductName());
            }
        }
    }

    private void sendExpirationEmail(Order order, String reason) {
        try {
            mailService.sendOrderRejectedEmail(order, reason);
            log.info("Đã gửi email thông báo hết hạn/hủy đơn hàng [{}] tới email [{}]", order.getOrderCode(), order.getCustomerEmail());
        } catch (Exception e) {
            log.warn("Không thể gửi email thông báo hủy cho đơn hàng {}: {}", order.getOrderCode(), e.getMessage());
        }
    }
}
