package com.example.bonsai_shop.moderator.service;

import com.example.bonsai_shop.entity.Order;
import com.example.bonsai_shop.entity.OrderDetail;
import com.example.bonsai_shop.entity.Product;
import com.example.bonsai_shop.moderator.dto.MyOrderDTO;
import com.example.bonsai_shop.moderator.dto.MyOrderKPIsDTO;
import com.example.bonsai_shop.moderator.util.ModeratorDisplayLabelMapper;
import com.example.bonsai_shop.product.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * [SERVICE QUẢN LÝ DANH SÁCH ĐƠN HÀNG CỦA TÔI (MY ORDERS) CHO MODERATOR]
 *
 * Chịu trách nhiệm:
 * - Tính toán các chỉ số KPI cá nhân theo tab bộ lọc (MyOrderKPIsDTO): CRITICAL (sắp hết hạn), Chờ duyệt (WAITING_APPROVAL), Chờ khách trả tiền (WAITING_CUSTOMER_PAYMENT), Chờ giao & thu nốt tiền (WAITING_DELIVERY_PAYMENT), Hoàn thành (COMPLETED), Đã hủy (CANCELLED).
 * - Lọc và phân trang danh sách MyOrderDTO cho giao diện Moderator.
 * - Tính toán mức độ ưu tiên theo thời gian tồn đọng (Priority: CRITICAL, HIGH, MEDIUM, LOW) và thời gian xử lý SLA (Handling Time).
 *
 * Các thành phần phối hợp chính:
 * - OrderRepository, ModeratorDisplayLabelMapper.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MyOrderService {

    private final OrderRepository orderRepository;

    /**
     * [TÍNH TOÁN KPI CÁ NHÂN CỦA MODERATOR]
     *
     * Mục đích:
     * - Cung cấp số liệu đếm số lượng đơn hàng theo từng nhóm trạng thái để hiển thị lên các thẻ KPI trên giao diện My Orders.
     *
     * Được gọi từ:
     * - ModeratorOrderController.viewMyOrders()
     * - ModeratorOrderController.getMyOrdersData()
     */
    @Transactional(readOnly = true)
    public MyOrderKPIsDTO getMyOrderKPIs(Integer moderatorId) {
        if (moderatorId == null) {
            return MyOrderKPIsDTO.builder().build();
        }

        List<Order> allMyOrders = orderRepository.searchMyOrders(moderatorId, null, null, Pageable.unpaged()).getContent();

        long criticalCount = 0;
        long waitingApprovalCount = 0;
        long waitingPaymentCount = 0;
        long waitingDeliveryCount = 0;
        long completedCount = 0;
        long cancelledCount = 0;

        for (Order order : allMyOrders) {
            String status = order.getOrderStatus() != null ? order.getOrderStatus().toUpperCase() : "PENDING";
            String priority = calculatePriority(order);

            if ("CRITICAL".equalsIgnoreCase(priority)) {
                criticalCount++;
            }

            if ("PENDING".equals(status) || "WAITING_APPROVAL".equals(status)) {
                waitingApprovalCount++;
            } else if ("PENDING_PAYMENT".equals(status) || "WAITING_CUSTOMER_PAYMENT".equals(status)) {
                waitingPaymentCount++;
            } else if ("DEPOSITED".equals(status) || "PAID".equals(status) || "WAITING_DELIVERY_PAYMENT".equals(status)) {
                waitingDeliveryCount++;
            } else if ("COMPLETED".equals(status)) {
                completedCount++;
            } else if ("CANCELLED".equals(status)) {
                cancelledCount++;
            }
        }

        return MyOrderKPIsDTO.builder()
                .criticalCount(criticalCount)
                .waitingApprovalCount(waitingApprovalCount)
                .waitingPaymentCount(waitingPaymentCount)
                .waitingDeliveryCount(waitingDeliveryCount)
                .completedCount(completedCount)
                .cancelledCount(cancelledCount)
                .build();
    }

    @Transactional(readOnly = true)
    public Page<MyOrderDTO> getMyOrdersFiltered(
            Integer moderatorId,
            String cardFilter,
            String search,
            String priorityFilter,
            String statusFilter,
            String sort,
            int page,
            int limit) {

        if (moderatorId == null) {
            return new PageImpl<>(List.of(), PageRequest.of(0, limit), 0);
        }

        // Fetch all candidate orders for the moderator to perform in-memory filter matching
        List<Order> allOrders = orderRepository.searchMyOrders(moderatorId, null, search, Pageable.unpaged()).getContent();

        // Apply filters
        List<MyOrderDTO> dtoList = new ArrayList<>();
        for (Order order : allOrders) {
            MyOrderDTO dto = convertToMyOrderDTO(order);
            if (dto == null) continue;

            // 1. Filter by Card Filter
            if (cardFilter != null && !cardFilter.isBlank() && !"ALL".equalsIgnoreCase(cardFilter)) {
                if ("CRITICAL".equalsIgnoreCase(cardFilter)) {
                    if (!"CRITICAL".equalsIgnoreCase(dto.getPriority())) continue;
                } else if ("WAITING_APPROVAL".equalsIgnoreCase(cardFilter) || "PENDING".equalsIgnoreCase(cardFilter)) {
                    if (!"PENDING".equalsIgnoreCase(dto.getOrderStatus()) && !"WAITING_APPROVAL".equalsIgnoreCase(dto.getOrderStatus())) continue;
                } else if ("WAITING_CUSTOMER_PAYMENT".equalsIgnoreCase(cardFilter) || "PENDING_PAYMENT".equalsIgnoreCase(cardFilter)) {
                    if (!"PENDING_PAYMENT".equalsIgnoreCase(dto.getOrderStatus()) && !"WAITING_CUSTOMER_PAYMENT".equalsIgnoreCase(dto.getOrderStatus())) continue;
                } else if ("WAITING_DELIVERY_PAYMENT".equalsIgnoreCase(cardFilter)) {
                    if (!"DEPOSITED".equalsIgnoreCase(dto.getOrderStatus()) && !"PAID".equalsIgnoreCase(dto.getOrderStatus()) && !"WAITING_DELIVERY_PAYMENT".equalsIgnoreCase(dto.getOrderStatus())) continue;
                } else if ("COMPLETED".equalsIgnoreCase(cardFilter)) {
                    if (!"COMPLETED".equalsIgnoreCase(dto.getOrderStatus())) continue;
                } else if ("CANCELLED".equalsIgnoreCase(cardFilter)) {
                    if (!"CANCELLED".equalsIgnoreCase(dto.getOrderStatus())) continue;
                }
            }

            // 2. Filter by Dropdown Priority Filter
            if (priorityFilter != null && !priorityFilter.isBlank() && !"ALL".equalsIgnoreCase(priorityFilter)) {
                if (!priorityFilter.equalsIgnoreCase(dto.getPriority())) continue;
            }

            // 3. Filter by Dropdown Status Filter
            if (statusFilter != null && !statusFilter.isBlank() && !"ALL".equalsIgnoreCase(statusFilter)) {
                if (!statusFilter.equalsIgnoreCase(dto.getOrderStatus())) continue;
            }

            // 4. Search Filter by OrderCode, CustomerName, Phone
            if (search != null && !search.isBlank()) {
                String q = search.trim().toLowerCase();
                boolean matchCode = dto.getOrderCode() != null && dto.getOrderCode().toLowerCase().contains(q);
                boolean matchName = dto.getCustomerName() != null && dto.getCustomerName().toLowerCase().contains(q);
                boolean matchPhone = dto.getCustomerPhone() != null && dto.getCustomerPhone().toLowerCase().contains(q);
                if (!matchCode && !matchName && !matchPhone) continue;
            }

            dtoList.add(dto);
        }

        // Sorting
        dtoList.sort((a, b) -> {
            if ("date_asc".equalsIgnoreCase(sort)) {
                return a.getStatusTimestamp() != null && b.getStatusTimestamp() != null
                        ? a.getStatusTimestamp().compareTo(b.getStatusTimestamp()) : 0;
            } else if ("price_desc".equalsIgnoreCase(sort)) {
                return (b.getTotalAmount() != null ? b.getTotalAmount() : BigDecimal.ZERO)
                        .compareTo(a.getTotalAmount() != null ? a.getTotalAmount() : BigDecimal.ZERO);
            } else if ("price_asc".equalsIgnoreCase(sort)) {
                return (a.getTotalAmount() != null ? a.getTotalAmount() : BigDecimal.ZERO)
                        .compareTo(b.getTotalAmount() != null ? b.getTotalAmount() : BigDecimal.ZERO);
            } else { // default: date_desc
                return b.getStatusTimestamp() != null && a.getStatusTimestamp() != null
                        ? b.getStatusTimestamp().compareTo(a.getStatusTimestamp()) : 0;
            }
        });

        // Pagination
        int total = dtoList.size();
        int fromIndex = Math.min((page - 1) * limit, total);
        int toIndex = Math.min(fromIndex + limit, total);
        List<MyOrderDTO> pagedList = dtoList.subList(fromIndex, toIndex);

        return new PageImpl<>(pagedList, PageRequest.of(page - 1, limit), total);
    }

    public MyOrderDTO convertToMyOrderDTO(Order order) {
        if (order == null) return null;

        try {
            BigDecimal craneFee = order.getCraneFee() != null ? order.getCraneFee() : BigDecimal.ZERO;
            BigDecimal shippingFee = order.getShippingFee() != null ? order.getShippingFee() : BigDecimal.ZERO;
            BigDecimal totalAmount = order.getTotalAmount() != null ? order.getTotalAmount() : BigDecimal.ZERO;
            BigDecimal depositAmount = order.getDepositAmount() != null ? order.getDepositAmount() : BigDecimal.ZERO;

            BigDecimal treePrice = BigDecimal.ZERO;
            if (order.getOrderDetails() != null && !order.getOrderDetails().isEmpty()) {
                treePrice = order.getOrderDetails().stream()
                        .map(d -> (d.getPriceAtPurchase() != null ? d.getPriceAtPurchase() : BigDecimal.ZERO)
                                .multiply(BigDecimal.valueOf(d.getQuantity() != null ? d.getQuantity() : 1)))
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
            } else {
                treePrice = totalAmount.subtract(craneFee).subtract(shippingFee);
                if (treePrice.compareTo(BigDecimal.ZERO) < 0) treePrice = BigDecimal.ZERO;
            }

            BigDecimal remainingPaymentAmount = BigDecimal.ZERO;
            if (depositAmount.compareTo(BigDecimal.ZERO) > 0) {
                remainingPaymentAmount = treePrice.subtract(depositAmount);
                if (remainingPaymentAmount.compareTo(BigDecimal.ZERO) < 0) remainingPaymentAmount = BigDecimal.ZERO;
            }

            String custName = order.getCustomerName();
            String custEmail = order.getCustomerEmail();
            String custPhone = order.getCustomerPhone();
            String custAddress = order.getShippingAddress();

            try {
                if (order.getCustomer() != null) {
                    if (custName == null || custName.isBlank()) custName = order.getCustomer().getFullName();
                    if (custEmail == null || custEmail.isBlank()) custEmail = order.getCustomer().getEmail();
                    if (custPhone == null || custPhone.isBlank()) custPhone = order.getCustomer().getPhone();
                    if (custAddress == null || custAddress.isBlank()) custAddress = order.getCustomer().getAddress();
                }
            } catch (Exception ignored) {}

            if (custName == null || custName.isBlank()) custName = "Khách hàng";

            LocalDateTime statusTimestamp = order.getAssignedAt() != null ? order.getAssignedAt()
                    : (order.getOrderDate() != null ? order.getOrderDate() : LocalDateTime.now());

            String ageFormatted = formatAge(statusTimestamp);
            String priority = calculatePriority(order);

            String firstProductName = null;
            String firstProductImage = null;
            int itemCount = 0;

            if (order.getOrderDetails() != null && !order.getOrderDetails().isEmpty()) {
                itemCount = order.getOrderDetails().size();
                OrderDetail detail0 = order.getOrderDetails().get(0);
                if (detail0 != null && detail0.getProduct() != null) {
                    Product p = detail0.getProduct();
                    firstProductName = p.getProductName();
                    try { firstProductImage = p.getFirstImageUrl(); } catch (Exception ignored) {}
                }
            }

            String orderStatus = order.getOrderStatus() != null ? order.getOrderStatus() : "PENDING";

            return MyOrderDTO.builder()
                    .orderId(order.getOrderId())
                    .orderCode(order.getOrderCode())
                    .customerName(custName)
                    .customerPhone(custPhone)
                    .customerEmail(custEmail)
                    .shippingAddress(custAddress)
                    .depositAmount(depositAmount)
                    .remainingPaymentAmount(remainingPaymentAmount)
                    .totalAmount(totalAmount)
                    .priority(priority)
                    .priorityLabel(ModeratorDisplayLabelMapper.priorityLabel(priority))
                    .orderStatus(orderStatus)
                    .orderStatusLabel(ModeratorDisplayLabelMapper.orderStatusLabel(orderStatus))
                    .orderType(order.getOrderType() != null ? order.getOrderType() : "ONLINE")
                    .statusTimestamp(statusTimestamp)
                    .ageFormatted(ageFormatted)
                    .itemCount(itemCount)
                    .firstProductName(firstProductName)
                    .firstProductImage(firstProductImage)
                    .build();
        } catch (Exception e) {
            log.error("Lỗi khi convert Order sang MyOrderDTO cho orderId: " + order.getOrderId(), e);
            return null;
        }
    }

    public String calculatePriority(Order order) {
        if (order == null) return "NORMAL";
        String status = order.getOrderStatus() != null ? order.getOrderStatus().toUpperCase() : "PENDING";
        BigDecimal total = order.getTotalAmount() != null ? order.getTotalAmount() : BigDecimal.ZERO;
        LocalDateTime refTime = order.getAssignedAt() != null ? order.getAssignedAt()
                : (order.getOrderDate() != null ? order.getOrderDate() : LocalDateTime.now());

        long hoursElapsed = Duration.between(refTime, LocalDateTime.now()).toHours();
        boolean isHighValue = total.compareTo(new BigDecimal("50000000")) >= 0;

        if ("COMPLETED".equals(status) || "CANCELLED".equals(status)) {
            return "LOW";
        } else if (("PENDING".equals(status) && hoursElapsed >= 24) || (isHighValue && hoursElapsed >= 12 && "PENDING".equals(status))) {
            return "CRITICAL";
        } else if (isHighValue || ("PENDING".equals(status) && hoursElapsed >= 6)) {
            return "HIGH";
        } else {
            return "NORMAL";
        }
    }

    public String formatAge(LocalDateTime timestamp) {
        if (timestamp == null) return "-";
        long minutes = Duration.between(timestamp, LocalDateTime.now()).toMinutes();
        if (minutes < 1) return "Vừa xong";
        if (minutes < 60) return minutes + " phút";
        long hours = minutes / 60;
        if (hours < 24) return hours + " giờ";
        long days = hours / 24;
        return days + " ngày";
    }
}
