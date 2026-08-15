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

import java.util.Locale;

@Service
@RequiredArgsConstructor
public class OrderHandlingHistoryService {

    private static final int DEFAULT_PAGE_SIZE = 10;
    private static final int MAX_PAGE_SIZE = 100;

    private final OrderHandlingRepository orderHandlingRepository;

    // Tim lich su moderator xu ly don hang cho Owner, co search/status va phan trang.
    @Transactional(readOnly = true)
    public Page<OrderHandlingHistoryDTO> findModeratorHandlingHistory(String search, String status, int page, int size) {
        // Validate page khong am de tranh loi PageRequest.
        int safePage = Math.max(page, 0);
        // Gioi han size trong khoang 1..MAX_PAGE_SIZE de tranh query qua lon.
        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        Pageable pageable = PageRequest.of(safePage, safeSize);

        // Chuan hoa filter truoc khi truyen xuong repository, sau do map entity sang DTO.
        return orderHandlingRepository.findModeratorHandlingHistory(normalizeSearch(search), normalizeStatus(status), pageable)
                .map(this::toDTO);
    }

    // Page size mac dinh dung khi controller khong nhan duoc tham so size.
    public int defaultPageSize() {
        return DEFAULT_PAGE_SIZE;
    }

    // Cat khoang trang search; null giu nguyen de repository hieu la khong loc search.
    private String normalizeSearch(String search) {
        return search == null ? null : search.trim();
    }

    // Chuan hoa status ve uppercase, mac dinh ALL khi bo trong.
    private String normalizeStatus(String status) {
        return status == null || status.isBlank() ? "ALL" : status.trim().toUpperCase(Locale.ROOT);
    }

    // Chuyen OrderHandling entity sang DTO phuc vu bang lich su cua Owner.
    private OrderHandlingHistoryDTO toDTO(OrderHandling handling) {
        Order order = handling.getOrder();
        User moderator = handling.getModerator();

        // Lay thong tin tu order/moderator neu co, fallback "-" de view khong bi null.
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

    // Lay ten khach hang uu tien tu order snapshot, fallback sang entity customer.
    private String resolveCustomerName(Order order) {
        if (order == null) {
            return "-";
        }
        if (order.getCustomerName() != null && !order.getCustomerName().isBlank()) {
            return order.getCustomerName();
        }
        return order.getCustomer() != null ? order.getCustomer().getFullName() : "-";
    }

    // Lay email khach hang uu tien tu order snapshot, fallback sang entity customer.
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
