package com.example.bonsai_shop.owner.service;

import com.example.bonsai_shop.artisan.service.ArtisanInPersonOrderService;
import com.example.bonsai_shop.entity.Order;
import com.example.bonsai_shop.entity.OrderDetail;
import com.example.bonsai_shop.entity.Product;
import com.example.bonsai_shop.entity.User;
import com.example.bonsai_shop.owner.dto.InPersonOrderHistoryDTO;
import com.example.bonsai_shop.product.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
@RequiredArgsConstructor
public class InPersonOrderHistoryService {

    private static final int DEFAULT_PAGE_SIZE = 10;
    private static final int MAX_PAGE_SIZE = 100;

    private final OrderRepository orderRepository;

    @Transactional(readOnly = true)
    public Page<InPersonOrderHistoryDTO> findOwnerInPersonOrders(String search, String status, int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        Pageable pageable = PageRequest.of(safePage, safeSize);

        return orderRepository.searchOwnerInPersonOrders(
                        ArtisanInPersonOrderService.ORDER_TYPE_IN_PERSON,
                        normalizeStatus(status),
                        normalizeSearch(search),
                        pageable)
                .map(this::toDTO);
    }

    @Transactional(readOnly = true)
    public boolean isOwnerVisibleInPersonOrder(String orderCode) {
        if (orderCode == null || orderCode.isBlank()) {
            return false;
        }
        return orderRepository.findByOrderCodeWithDetails(orderCode.trim())
                .filter(order -> ArtisanInPersonOrderService.ORDER_TYPE_IN_PERSON.equalsIgnoreCase(order.getOrderType()))
                .map(this::isHandledByArtisan)
                .orElse(false);
    }

    public int defaultPageSize() {
        return DEFAULT_PAGE_SIZE;
    }

    private InPersonOrderHistoryDTO toDTO(Order order) {
        OrderDetail detail = firstDetail(order);
        Product product = detail != null ? detail.getProduct() : null;
        User handler = product != null ? product.getCreatedBy() : null;

        return InPersonOrderHistoryDTO.builder()
                .orderId(order.getOrderId())
                .orderCode(order.getOrderCode())
                .customerName(resolveCustomerName(order))
                .customerPhone(order.getCustomerPhone() != null && !order.getCustomerPhone().isBlank()
                        ? order.getCustomerPhone()
                        : "-")
                .handlerName(handler != null ? handler.getFullName() : "-")
                .handlerEmail(handler != null ? handler.getEmail() : "-")
                .productName(product != null ? product.getProductName() : "-")
                .totalAmount(order.getTotalAmount())
                .orderDate(order.getOrderDate())
                .orderStatus(order.getOrderStatus() != null ? order.getOrderStatus() : "-")
                .build();
    }

    private boolean isHandledByArtisan(Order order) {
        OrderDetail detail = firstDetail(order);
        Product product = detail != null ? detail.getProduct() : null;
        User handler = product != null ? product.getCreatedBy() : null;
        String roleName = handler != null && handler.getRole() != null ? handler.getRole().getRoleName() : null;
        if (roleName == null) {
            return false;
        }
        String normalizedRole = roleName.trim().toUpperCase(Locale.ROOT);
        return "ARTISAN".equals(normalizedRole) || "ROLE_ARTISAN".equals(normalizedRole);
    }

    private OrderDetail firstDetail(Order order) {
        return order != null && order.getOrderDetails() != null && !order.getOrderDetails().isEmpty()
                ? order.getOrderDetails().get(0)
                : null;
    }

    private String resolveCustomerName(Order order) {
        if (order == null) {
            return "-";
        }
        if (order.getCustomerName() != null && !order.getCustomerName().isBlank()) {
            return order.getCustomerName();
        }
        return order.getCustomer() != null ? order.getCustomer().getFullName() : "-";
    }

    private String normalizeSearch(String search) {
        return search == null ? null : search.trim();
    }

    private String normalizeStatus(String status) {
        return status == null || status.isBlank() ? "ALL" : status.trim().toUpperCase(Locale.ROOT);
    }
}
