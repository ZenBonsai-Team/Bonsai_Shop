package com.example.bonsai_shop.owner.service;

import com.example.bonsai_shop.entity.Order;
import com.example.bonsai_shop.entity.OrderDetail;
import com.example.bonsai_shop.entity.OrderHandling;
import com.example.bonsai_shop.entity.Product;
import com.example.bonsai_shop.entity.User;
import com.example.bonsai_shop.owner.dto.OwnerOrderHistoryDTO;
import com.example.bonsai_shop.product.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class OwnerOrderHistoryService {

    private static final int DEFAULT_PAGE_SIZE = 10;
    private static final int MAX_PAGE_SIZE = 100;
    public static final String TYPE_ALL = "ALL";
    public static final String TYPE_ONLINE = "ONLINE";
    public static final String TYPE_IN_PERSON = "IN_PERSON";

    private final OrderRepository orderRepository;

    @Transactional(readOnly = true)
    public Page<OwnerOrderHistoryDTO> findOwnerOrderHistory(String search, String type, String status, int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        Pageable pageable = PageRequest.of(safePage, safeSize);

        return orderRepository.searchOwnerOrderHistory(
                        normalizeType(type),
                        normalizeStatus(status),
                        normalizeSearch(search),
                        pageable)
                .map(this::toDTO);
    }

    public int defaultPageSize() {
        return DEFAULT_PAGE_SIZE;
    }

    public String normalizeType(String type) {
        if (type == null || type.isBlank()) {
            return TYPE_ALL;
        }
        String normalized = type.trim().toUpperCase(Locale.ROOT);
        return TYPE_ONLINE.equals(normalized) || TYPE_IN_PERSON.equals(normalized) ? normalized : TYPE_ALL;
    }

    private OwnerOrderHistoryDTO toDTO(Order order) {
        OrderDetail detail = firstDetail(order);
        Product product = detail != null ? detail.getProduct() : null;
        User handler = resolveHandler(order, product);

        return OwnerOrderHistoryDTO.builder()
                .orderId(order.getOrderId())
                .orderCode(order.getOrderCode())
                .orderType(order.getOrderType() != null ? order.getOrderType() : "-")
                .customerName(resolveCustomerName(order))
                .customerContact(resolveCustomerContact(order))
                .handlerRoleLabel(resolveHandlerRoleLabel(order))
                .handlerName(handler != null ? handler.getFullName() : "Ch\u01b0a ph\u00e2n c\u00f4ng")
                .handlerEmail(handler != null ? handler.getEmail() : "-")
                .productName(product != null ? product.getProductName() : "-")
                .totalAmount(order.getTotalAmount())
                .orderDate(order.getOrderDate())
                .orderStatus(order.getOrderStatus() != null ? order.getOrderStatus() : "-")
                .build();
    }

    private User resolveHandler(Order order, Product product) {
        if (TYPE_IN_PERSON.equalsIgnoreCase(order.getOrderType())) {
            return product != null ? product.getCreatedBy() : null;
        }
        return latestHandling(order) != null ? latestHandling(order).getModerator() : order.getAssignedTo();
    }

    private String resolveHandlerRoleLabel(Order order) {
        if (order != null && TYPE_IN_PERSON.equalsIgnoreCase(order.getOrderType())) {
            return "Ngh\u1ec7 nh\u00e2n";
        }
        return "Moderator";
    }

    private OrderHandling latestHandling(Order order) {
        if (order == null || order.getOrderHandlings() == null || order.getOrderHandlings().isEmpty()) {
            return null;
        }
        return order.getOrderHandlings().stream()
                .max(Comparator.comparing(OrderHandling::getHandledAt, Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(OrderHandling::getOrderHandlingId, Comparator.nullsLast(Comparator.naturalOrder())))
                .orElse(null);
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

    private String resolveCustomerContact(Order order) {
        if (order == null) {
            return "-";
        }
        if (order.getCustomerEmail() != null && !order.getCustomerEmail().isBlank()) {
            return order.getCustomerEmail();
        }
        if (order.getCustomerPhone() != null && !order.getCustomerPhone().isBlank()) {
            return order.getCustomerPhone();
        }
        if (order.getCustomer() != null && order.getCustomer().getEmail() != null) {
            return order.getCustomer().getEmail();
        }
        return "-";
    }

    private String normalizeSearch(String search) {
        return search == null ? null : search.trim();
    }

    private String normalizeStatus(String status) {
        return status == null || status.isBlank() ? TYPE_ALL : status.trim().toUpperCase(Locale.ROOT);
    }
}
