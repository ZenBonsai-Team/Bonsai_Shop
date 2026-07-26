package com.example.bonsai_shop.product.service;

import com.example.bonsai_shop.entity.Order;
import com.example.bonsai_shop.entity.OrderDetail;
import com.example.bonsai_shop.entity.Product;
import com.example.bonsai_shop.product.repository.OrderRepository;
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

    @Transactional
    public void cancelExpiredOrders() {
        LocalDateTime now = LocalDateTime.now();

        // 1. Xử lý đơn Online (VNPay) quá 15 phút chưa thanh toán
        LocalDateTime onlineCutoff = now.minusMinutes(15);
        List<Order> expiredOnlineOrders = orderRepository.findExpiredOnlineOrders(onlineCutoff);
        for (Order order : expiredOnlineOrders) {
            cancelSingleOrder(order, "Tự động hủy: Đơn hàng Online quá hạn 15 phút chưa thanh toán");
        }

        // 2. Xử lý đơn Offline quá 48 giờ chưa thanh toán / hoàn tất tiền
        LocalDateTime offlineCutoff = now.minusHours(48);
        List<Order> expiredOfflineOrders = orderRepository.findExpiredOfflineOrders(offlineCutoff);
        for (Order order : expiredOfflineOrders) {
            cancelSingleOrder(order, "Tự động hủy: Đơn hàng quá hạn 48 giờ chưa chuẩn bị/thanh toán tiền");
        }
    }

    private void cancelSingleOrder(Order order, String reason) {
        log.info("Tự động hủy đơn hàng [{}]: {}", order.getOrderCode(), reason);

        order.setOrderStatus("CANCELLED");
        String currentNotes = order.getNotes();
        order.setNotes(currentNotes != null && !currentNotes.isEmpty() ? currentNotes + " | " + reason : reason);
        orderRepository.save(order);

        if (order.getOrderDetails() != null) {
            for (OrderDetail detail : order.getOrderDetails()) {
                Product product = detail.getProduct();
                if (product != null && !"AVAILABLE".equalsIgnoreCase(product.getProductStatus())) {
                    product.setProductStatus("AVAILABLE");
                    productRepository.save(product);
                    log.info("Giải phóng sản phẩm [ID: {} - {}] về lại trạng thái AVAILABLE", product.getProductId(), product.getProductName());
                }
            }
        }
    }
}
