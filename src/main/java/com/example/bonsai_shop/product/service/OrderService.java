package com.example.bonsai_shop.product.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.bonsai_shop.entity.Order;
import com.example.bonsai_shop.entity.OrderHandling;
import com.example.bonsai_shop.entity.OrderLog;
import com.example.bonsai_shop.entity.Product;
import com.example.bonsai_shop.entity.User;
import com.example.bonsai_shop.product.repository.OrderHandlingRepository;
import com.example.bonsai_shop.product.repository.OrderLogRepository;
import com.example.bonsai_shop.product.repository.OrderRepository;
import com.example.bonsai_shop.product.repository.ProductRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final OrderLogRepository orderLogRepository;
    private final OrderHandlingRepository orderHandlingRepository;

    @Transactional
    public Page<Order> getFilteredOrders(String search, String status, String sort, int page, int limit) {
        Sort springSort;
        if ("date_asc".equals(sort)) {
            springSort = Sort.by(Sort.Direction.ASC, "orderDate");
        } else if ("price_desc".equals(sort)) {
            springSort = Sort.by(Sort.Direction.DESC, "totalAmount");
        } else if ("price_asc".equals(sort)) {
            springSort = Sort.by(Sort.Direction.ASC, "totalAmount");
        } else {
            springSort = Sort.by(Sort.Direction.DESC, "orderDate"); // default
        }
        Pageable pageable = PageRequest.of(page - 1, limit, springSort);
        return orderRepository.searchOrdersForModerator(status, search, pageable);
    }

    @Transactional(readOnly = true)
    public Order getOrderByCode(String orderCode) {
        return orderRepository.findByOrderCode(orderCode).orElse(null);
    }

    @Transactional(readOnly = true)
    public Map<String, Long> getKPIs() {
        Map<String, Long> kpis = new HashMap<>();
        kpis.put("total", orderRepository.count());
        kpis.put("pending", orderRepository.countByOrderStatus("PENDING"));
        kpis.put("approved", orderRepository.countByOrderStatus("APPROVED"));
        kpis.put("paid", orderRepository.countByOrderStatus("PAID"));
        kpis.put("cancelled", orderRepository.countByOrderStatus("CANCELLED"));
        return kpis;
    }

    public boolean verifyOrder(String orderCode, BigDecimal craneFee, BigDecimal shippingFee, User moderator) {
        Order order = orderRepository.findByOrderCode(orderCode).orElse(null);
        if (order == null || !"PENDING".equals(order.getOrderStatus())) {
            return false;
        }

        String oldStatus = order.getOrderStatus();

        // 1. Cập nhật phí xe cẩu, phí vận chuyện và trạng thái duyệt
        order.setCraneFee(craneFee);
        order.setShippingFee(shippingFee);
        order.setOrderStatus("APPROVED");

        // 2. Tính lại tổng tiền: totalAmount = Tiền cây gốc + phí cẩu + phí ship
        BigDecimal originalAmount = order.getTotalAmount();
        BigDecimal newTotal = originalAmount.add(craneFee).add(shippingFee);
        order.setTotalAmount(newTotal);

        orderRepository.save(order);

        return true;
    }

    public boolean rejectOrder(String orderCode, String reason, User moderator) {
        Order order = orderRepository.findByOrderCode(orderCode).orElse(null);
        if (order == null || !"PENDING".equals(order.getOrderStatus())) {
            return false;
        }
        String oldStatus = order.getOrderStatus();

        // 1. Cập nhật trạng thái và lý do từ chối
        order.setOrderStatus("REJECTED");
        order.setNotes("Từ chối duyệt với lý do: " + reason);
        orderRepository.save(order);

        // 2. Giải phóng sản phẩm Bonsai về lại trạng thái AVAILABLE
        if (order.getOrderDetails() != null && !order.getOrderDetails().isEmpty()) {
            Product product = order.getOrderDetails().get(0).getProduct();
            if (product != null) {
                product.setProductStatus("AVAILABLE");
                productRepository.save(product);
            }
        }

        // 3. Ghi OrderLog nhật ký hoạt động
        OrderLog log = OrderLog.builder().order(order).actionBy(moderator).actionType("REJECT").fromStatus(oldStatus)
                .toStatus("REJECTED").actionAt(LocalDateTime.now()).build();
        orderLogRepository.save(log);

        // 4. Lưu OrderHandling
        OrderHandling handling = OrderHandling.builder().order(order).moderator(moderator)
                .handledAt(LocalDateTime.now()).isActive(true).build();
        orderHandlingRepository.save(handling);

        return true;
    }
}
