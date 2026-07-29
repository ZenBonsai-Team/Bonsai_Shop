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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class OrderExpirationService {

    private static final Logger log = LoggerFactory.getLogger(OrderExpirationService.class);

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private OrderHandlingRepository orderHandlingRepository;

    @Autowired
    private MailService mailService;

    @Transactional
    public void cancelExpiredOrders() {
        LocalDateTime now = LocalDateTime.now();

        // 1. Xử lý đơn Online (VNPay) quá 15 phút chưa thanh toán
        LocalDateTime onlineCutoff = now.minusMinutes(15);
        List<Order> expiredOnlineOrders = orderRepository.findExpiredOnlineOrders(onlineCutoff);
        for (Order order : expiredOnlineOrders) {
            cancelSingleOrder(order, "Tự động từ chối: Đơn hàng Online quá hạn 15 phút chưa thanh toán qua VNPay");
        }

        // 2. Xử lý đơn Offline quá 48 giờ chưa thanh toán / hoàn tất tiền
        LocalDateTime offlineCutoff = now.minusHours(48);
        List<Order> expiredOfflineOrders = orderRepository.findExpiredOfflineOrders(offlineCutoff);
        for (Order order : expiredOfflineOrders) {
            cancelSingleOrder(order, "Tự động từ chối: Đơn hàng quá hạn 48 giờ chưa chuẩn bị/thanh toán tiền");
        }
    }

    private void cancelSingleOrder(Order order, String reason) {
        log.info("Tự động từ chối đơn hàng quá hạn [{}]: {}", order.getOrderCode(), reason);

        // 1. Cập nhật Order Status -> REJECTED
        order.setOrderStatus("REJECTED");
        String currentNotes = order.getNotes();
        order.setNotes(currentNotes != null && !currentNotes.isEmpty() ? currentNotes + " | " + reason : reason);
        orderRepository.save(order);

        // 2. Cập nhật các bản ghi Payment PENDING thành EXPIRED
        if (order.getOrderId() != null) {
            List<Payment> payments = paymentRepository.findByOrderOrderIdOrderByPaymentIdAsc(order.getOrderId());
            if (payments != null) {
                for (Payment p : payments) {
                    if ("PENDING".equalsIgnoreCase(p.getPaymentStatus())) {
                        p.setPaymentStatus("EXPIRED");
                        paymentRepository.save(p);
                        log.info("Cập nhật Payment [#{} - {}] -> EXPIRED", p.getPaymentId(), p.getPaymentType());
                    }
                }
            }
        }

        // 3. Giải phóng OrderHandling (gán isActive = false, releasedAt = now)
        if (order.getOrderId() != null) {
            List<OrderHandling> handlings = orderHandlingRepository.findByOrderOrderIdOrderByHandledAtDesc(order.getOrderId());
            if (handlings != null) {
                for (OrderHandling h : handlings) {
                    if (Boolean.TRUE.equals(h.getIsActive())) {
                        h.setIsActive(false);
                        h.setReleasedAt(LocalDateTime.now());
                        orderHandlingRepository.save(h);
                        log.info("Giải phóng OrderHandling [#{} - Mod: {}]", h.getOrderHandlingId(),
                                h.getModerator() != null ? h.getModerator().getUsername() : "N/A");
                    }
                }
            }
        }

        // 4. Giải phóng sản phẩm cây cảnh về trạng thái AVAILABLE
        if (order.getOrderDetails() != null) {
            for (OrderDetail detail : order.getOrderDetails()) {
                Product product = detail.getProduct();
                if (product != null && !"AVAILABLE".equalsIgnoreCase(product.getProductStatus())) {
                    product.setProductStatus("AVAILABLE");
                    productRepository.save(product);
                    log.info("Giải phóng sản phẩm [ID: {} - {}] về trạng thái AVAILABLE", product.getProductId(), product.getProductName());
                }
            }
        }

        // 5. Gửi Email thông báo hết hạn thanh toán cho khách hàng
        try {
            mailService.sendOrderRejectedEmail(order, reason);
            log.info("Đã gửi email thông báo hết hạn/từ chối đơn hàng [{}] tới email [{}]", order.getOrderCode(), order.getCustomerEmail());
        } catch (Exception e) {
            log.warn("Không thể gửi email thông báo từ chối cho đơn hàng {}: {}", order.getOrderCode(), e.getMessage());
        }
    }
}
