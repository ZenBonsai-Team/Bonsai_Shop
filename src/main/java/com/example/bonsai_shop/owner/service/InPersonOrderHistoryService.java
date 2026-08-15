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

    // Tim danh sach don tai vuon cua artisan de Owner theo doi, co search/status va phan trang.
    @Transactional(readOnly = true)
    public Page<InPersonOrderHistoryDTO> findOwnerInPersonOrders(String search, String status, int page, int size) {
        // Validate page khong am de tranh loi PageRequest.
        int safePage = Math.max(page, 0);
        // Gioi han size trong khoang 1..MAX_PAGE_SIZE de tranh query qua lon.
        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        Pageable pageable = PageRequest.of(safePage, safeSize);

        // Chi lay order type IN_PERSON va filter theo status/search da chuan hoa.
        return orderRepository.searchOwnerInPersonOrders(
                        ArtisanInPersonOrderService.ORDER_TYPE_IN_PERSON,
                        normalizeStatus(status),
                        normalizeSearch(search),
                        pageable)
                .map(this::toDTO);
    }

    // Kiem tra orderCode co phai don tai vuon do artisan phu trach de Owner duoc xem chi tiet hay khong.
    @Transactional(readOnly = true)
    public boolean isOwnerVisibleInPersonOrder(String orderCode) {
        // Validate orderCode rong/null thi khong hop le.
        if (orderCode == null || orderCode.isBlank()) {
            return false;
        }
        // Tim don kem detail, yeu cau order type la IN_PERSON va handler la artisan.
        return orderRepository.findByOrderCodeWithDetails(orderCode.trim())
                .filter(order -> ArtisanInPersonOrderService.ORDER_TYPE_IN_PERSON.equalsIgnoreCase(order.getOrderType()))
                .map(this::isHandledByArtisan)
                .orElse(false);
    }

    // Page size mac dinh dung khi controller khong nhan duoc tham so size.
    public int defaultPageSize() {
        return DEFAULT_PAGE_SIZE;
    }

    // Chuyen Order entity sang DTO hien thi trong bang lich su don tai vuon.
    private InPersonOrderHistoryDTO toDTO(Order order) {
        // Lay detail dau tien de xac dinh cay va artisan phu trach.
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

    // Kiem tra nguoi tao san pham trong don co role ARTISAN hay khong.
    private boolean isHandledByArtisan(Order order) {
        OrderDetail detail = firstDetail(order);
        Product product = detail != null ? detail.getProduct() : null;
        User handler = product != null ? product.getCreatedBy() : null;
        String roleName = handler != null && handler.getRole() != null ? handler.getRole().getRoleName() : null;
        // Khong co role thi khong coi la don cua artisan.
        if (roleName == null) {
            return false;
        }
        // Chuan hoa ten role de chap nhan ca ARTISAN va ROLE_ARTISAN.
        String normalizedRole = roleName.trim().toUpperCase(Locale.ROOT);
        return "ARTISAN".equals(normalizedRole) || "ROLE_ARTISAN".equals(normalizedRole);
    }

    // Lay order detail dau tien vi moi don tai vuon hien tai gan voi mot san pham chinh.
    private OrderDetail firstDetail(Order order) {
        return order != null && order.getOrderDetails() != null && !order.getOrderDetails().isEmpty()
                ? order.getOrderDetails().get(0)
                : null;
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

    // Cat khoang trang search; null giu nguyen de repository hieu la khong loc search.
    private String normalizeSearch(String search) {
        return search == null ? null : search.trim();
    }

    // Chuan hoa status ve uppercase, mac dinh ALL khi bo trong.
    private String normalizeStatus(String status) {
        return status == null || status.isBlank() ? "ALL" : status.trim().toUpperCase(Locale.ROOT);
    }
}
