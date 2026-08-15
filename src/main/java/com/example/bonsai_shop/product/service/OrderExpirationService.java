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

/**
 * [SERVICE TỰ ĐỘNG XỬ LÝ HẾT HẠN ĐƠN HÀNG - ORDER EXPIRATION SERVICE]
 *
 * Chịu trách nhiệm:
 * - Quét và tự động hủy các đơn hàng trực tuyến (ONLINE) quá hạn thanh toán 15 phút (PENDING / PENDING_PAYMENT).
 * - Quét và tự động hủy các đơn hàng quá hạn 48 giờ (offline / fallback).
 * - Quét và tự động hủy các đơn hàng mua tại vườn (IN_PERSON) quá thời gian cấu hình (mặc định 1440 phút / 24h).
 * - Thực hiện chuỗi dọn dẹp tài nguyên khi đơn hết hạn (cancelSingleOrder):
 *   1. Chuyển trạng thái Order sang CANCELLED, ghi lý do vào notes.
 *   2. Đánh dấu các bản ghi Payment PENDING thành EXPIRED.
 *   3. Giải phóng các phiên xử lý OrderHandling đang active (isActive = false, releasedAt = now).
 *   4. Giải phóng toàn bộ cây trong đơn từ RESERVED về AVAILABLE trong PRODUCT table.
 *   5. Gửi email thông báo hủy do quá hạn thanh toán cho khách hàng qua MailService.
 *
 * Các thành phần phối hợp chính:
 * - Scheduler: OrderExpirationScheduler (chạy ngầm mỗi 60 giây).
 * - Repositories: OrderRepository, ProductRepository, PaymentRepository, OrderHandlingRepository.
 * - Services: MailService.
 */
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

    /**
     * [QUÉT VÀ HỦY ĐƠN HÀNG ONLINE / OFFLINE QUÁ HẠN THANH TOÁN]
     *
     * Mục đích:
     * - Tìm kiếm các đơn hàng ONLINE ở trạng thái PENDING hoặc PENDING_PAYMENT tạo từ hơn 15 phút trước.
     * - Tìm kiếm các đơn hàng OFFLINE tạo từ hơn 48 giờ trước chưa hoàn tất.
     * - Hủy đơn và trả lại cây cho khách khác mua.
     *
     * Được gọi từ:
     * - OrderExpirationScheduler.scanExpiredOrders() (Định kỳ mỗi 60s)
     *
     * Tác động DB:
     * - ORDER: orderStatus: PENDING/PENDING_PAYMENT → CANCELLED, notes
     * - PAYMENT: paymentStatus: PENDING → EXPIRED
     * - ORDER_HANDLING: isActive = false, releasedAt = now
     * - PRODUCT: productStatus: RESERVED → AVAILABLE
     */
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

    }

    /**
     * [QUÉT VÀ HỦY ĐƠN HÀNG TẠI VƯỜN (IN_PERSON) QUÁ HẠN]
     *
     * Mục đích:
     * - Hủy các đơn hàng IN_PERSON chưa hoàn tất sau khoảng thời gian inPersonExpirationMinutes (mặc định 24h).
     */
    @Transactional
    public void cancelExpiredInPersonOrders() {
        LocalDateTime inPersonCutoff = LocalDateTime.now().minusMinutes(inPersonExpirationMinutes);
        List<Order> expiredInPersonOrders = orderRepository.findExpiredInPersonOrders(inPersonCutoff);
        log.info("In-person expiration scan: timeout={} minutes, cutoff={}, matchedOrders={}",
                inPersonExpirationMinutes, inPersonCutoff, expiredInPersonOrders.size());
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
