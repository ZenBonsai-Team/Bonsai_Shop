package com.example.bonsai_shop.owner.service;

import com.example.bonsai_shop.entity.Order;
import com.example.bonsai_shop.entity.OrderHandling;
import com.example.bonsai_shop.entity.User;
import com.example.bonsai_shop.owner.dto.OrderHandlingHistoryDTO;
import com.example.bonsai_shop.product.repository.OrderHandlingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OrderHandlingHistoryService {

    private static final int DEFAULT_PAGE_SIZE = 15;
    private static final int MAX_PAGE_SIZE = 100;

    private final OrderHandlingRepository orderHandlingRepository;

    @Transactional(readOnly = true)
    public Page<OrderHandlingHistoryDTO> findModeratorHandlingHistory(String search, int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        Pageable pageable = PageRequest.of(safePage, safeSize);

        return orderHandlingRepository.findModeratorHandlingHistory(normalizeSearch(search), pageable)
                .map(this::toDTO);
    }

    public int defaultPageSize() {
        return DEFAULT_PAGE_SIZE;
    }

    private String normalizeSearch(String search) {
        return search == null ? null : search.trim();
    }

    private OrderHandlingHistoryDTO toDTO(OrderHandling handling) {
        Order order = handling.getOrder();
        User moderator = handling.getModerator();

        return OrderHandlingHistoryDTO.builder()
                .handlingId(handling.getOrderHandlingId())
                .orderCode(order != null ? order.getOrderCode() : "-")
                .customerName(resolveCustomerName(order))
                .customerEmail(resolveCustomerEmail(order))
                .moderatorName(moderator != null ? moderator.getFullName() : "-")
                .moderatorEmail(moderator != null ? moderator.getEmail() : "-")
                .orderDate(order != null ? order.getOrderDate() : null)
                .totalAmount(order != null ? order.getTotalAmount() : null)
                .status(Boolean.TRUE.equals(handling.getIsActive()) ? "Đang phụ trách" : "Đã bàn giao")
                .orderStatus(order != null && order.getOrderStatus() != null ? order.getOrderStatus() : "-")
                .handledAt(handling.getHandledAt())
                .releasedAt(handling.getReleasedAt())
                .build();
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

    private String resolveCustomerEmail(Order order) {
        if (order == null) {
            return "-";
        }
        if (order.getCustomerEmail() != null && !order.getCustomerEmail().isBlank()) {
            return order.getCustomerEmail();
        }
        return order.getCustomer() != null ? order.getCustomer().getEmail() : "-";
    }
}
