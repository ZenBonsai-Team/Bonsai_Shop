package com.example.bonsai_shop.moderator.service;

import com.example.bonsai_shop.entity.Order;
import com.example.bonsai_shop.entity.OrderDetail;
import com.example.bonsai_shop.entity.Product;
import com.example.bonsai_shop.entity.User;
import com.example.bonsai_shop.moderator.dto.MyOrderDTO;
import com.example.bonsai_shop.moderator.dto.MyOrderKPIsDTO;
import com.example.bonsai_shop.product.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MyOrderServiceTest {

    private OrderRepository orderRepository;
    private MyOrderService myOrderService;

    @BeforeEach
    void setUp() {
        orderRepository = mock(OrderRepository.class);
        myOrderService = new MyOrderService(orderRepository);
    }

    // =========================================================================
    // Group 1: formatAge
    // =========================================================================

    @Test
    @DisplayName("UT-UUT07-001: formatAge - timestamp = null")
    void formatAge_nullTimestamp_returnsDash() {
        String result = myOrderService.formatAge(null);
        assertEquals("-", result);
    }

    @Test
    @DisplayName("UT-UUT07-002: formatAge - timestamp mới tạo (vừa xong)")
    void formatAge_justNow_returnsVuaXong() {
        LocalDateTime now = LocalDateTime.now();
        String result = myOrderService.formatAge(now);
        assertEquals("Vừa xong", result);
    }

    @Test
    @DisplayName("UT-UUT07-003: formatAge - timestamp cách 35 phút")
    void formatAge_35MinutesAgo_returns35Phut() {
        LocalDateTime timestamp = LocalDateTime.now().minusMinutes(35);
        String result = myOrderService.formatAge(timestamp);
        assertEquals("35 phút", result);
    }

    @Test
    @DisplayName("UT-UUT07-004: formatAge - timestamp cách 8 giờ")
    void formatAge_8HoursAgo_returns8Gio() {
        LocalDateTime timestamp = LocalDateTime.now().minusHours(8);
        String result = myOrderService.formatAge(timestamp);
        assertEquals("8 giờ", result);
    }

    @Test
    @DisplayName("UT-UUT07-005: formatAge - timestamp cách 3 ngày")
    void formatAge_3DaysAgo_returns3Ngay() {
        LocalDateTime timestamp = LocalDateTime.now().minusDays(3);
        String result = myOrderService.formatAge(timestamp);
        assertEquals("3 ngày", result);
    }

    // =========================================================================
    // Group 2: calculatePriority
    // =========================================================================

    @Test
    @DisplayName("UT-UUT07-006: calculatePriority - order = null")
    void calculatePriority_nullOrder_returnsNormal() {
        String result = myOrderService.calculatePriority(null);
        assertEquals("NORMAL", result);
    }

    @Test
    @DisplayName("UT-UUT07-007: calculatePriority - Đơn COMPLETED hoặc CANCELLED")
    void calculatePriority_completedOrCancelled_returnsLow() {
        Order completedOrder = Order.builder().orderStatus("COMPLETED").build();
        Order cancelledOrder = Order.builder().orderStatus("CANCELLED").build();

        assertEquals("LOW", myOrderService.calculatePriority(completedOrder));
        assertEquals("LOW", myOrderService.calculatePriority(cancelledOrder));
    }

    @Test
    @DisplayName("UT-UUT07-008: calculatePriority - Đơn PENDING tạo >= 24 giờ")
    void calculatePriority_pendingOver24Hours_returnsCritical() {
        Order order = Order.builder()
                .orderStatus("PENDING")
                .orderDate(LocalDateTime.now().minusHours(25))
                .totalAmount(new BigDecimal("10000000"))
                .build();

        assertEquals("CRITICAL", myOrderService.calculatePriority(order));
    }

    @Test
    @DisplayName("UT-UUT07-009: calculatePriority - Đơn PENDING giá trị cao (>= 50M) tạo >= 12 giờ")
    void calculatePriority_pendingHighValueOver12Hours_returnsCritical() {
        Order order = Order.builder()
                .orderStatus("PENDING")
                .orderDate(LocalDateTime.now().minusHours(13))
                .totalAmount(new BigDecimal("60000000"))
                .build();

        assertEquals("CRITICAL", myOrderService.calculatePriority(order));
    }

    @Test
    @DisplayName("UT-UUT07-010: calculatePriority - Đơn PENDING giá trị cao (>= 50M) mới tạo 2 giờ")
    void calculatePriority_pendingHighValueNew_returnsHigh() {
        Order order = Order.builder()
                .orderStatus("PENDING")
                .orderDate(LocalDateTime.now().minusHours(2))
                .totalAmount(new BigDecimal("60000000"))
                .build();

        assertEquals("HIGH", myOrderService.calculatePriority(order));
    }

    @Test
    @DisplayName("UT-UUT07-011: calculatePriority - Đơn PENDING giá trị thường (< 50M) tạo >= 6 giờ")
    void calculatePriority_pendingNormalValueOver6Hours_returnsHigh() {
        Order order = Order.builder()
                .orderStatus("PENDING")
                .orderDate(LocalDateTime.now().minusHours(7))
                .totalAmount(new BigDecimal("10000000"))
                .build();

        assertEquals("HIGH", myOrderService.calculatePriority(order));
    }

    @Test
    @DisplayName("UT-UUT07-012: calculatePriority - Đơn PENDING giá trị thường (< 50M) mới tạo 2 giờ")
    void calculatePriority_pendingNormalValueNew_returnsNormal() {
        Order order = Order.builder()
                .orderStatus("PENDING")
                .orderDate(LocalDateTime.now().minusHours(2))
                .totalAmount(new BigDecimal("10000000"))
                .build();

        assertEquals("NORMAL", myOrderService.calculatePriority(order));
    }

    // =========================================================================
    // Group 3: convertToMyOrderDTO
    // =========================================================================

    @Test
    @DisplayName("UT-UUT07-013: convertToMyOrderDTO - order = null")
    void convertToMyOrderDTO_nullOrder_returnsNull() {
        assertNull(myOrderService.convertToMyOrderDTO(null));
    }

    @Test
    @DisplayName("UT-UUT07-014: convertToMyOrderDTO - Đơn hàng hợp lệ có đầy đủ orderDetails và customer entity")
    void convertToMyOrderDTO_validOrderWithDetailsAndCustomer() {
        Product product = Product.builder()
                .productId(1)
                .productName("Cây Tùng Nhật")
                .build();

        OrderDetail detail = OrderDetail.builder()
                .product(product)
                .priceAtPurchase(new BigDecimal("2000000"))
                .quantity(2)
                .build();

        User customer = User.builder()
                .fullName("Nguyễn Văn Khách")
                .email("khach@gmail.com")
                .phone("0987654321")
                .address("Hà Nội")
                .build();

        Order order = Order.builder()
                .orderId(100)
                .orderCode("BSMS-100")
                .customer(customer)
                .orderStatus("PENDING")
                .totalAmount(new BigDecimal("4000000"))
                .depositAmount(new BigDecimal("1000000"))
                .orderDetails(List.of(detail))
                .orderDate(LocalDateTime.now().minusHours(1))
                .build();

        MyOrderDTO dto = myOrderService.convertToMyOrderDTO(order);

        assertNotNull(dto);
        assertEquals(100, dto.getOrderId());
        assertEquals("BSMS-100", dto.getOrderCode());
        assertEquals("Nguyễn Văn Khách", dto.getCustomerName());
        assertEquals("0987654321", dto.getCustomerPhone());
        assertEquals("khach@gmail.com", dto.getCustomerEmail());
        assertThat(dto.getDepositAmount()).isEqualByComparingTo("1000000");
        assertThat(dto.getRemainingPaymentAmount()).isEqualByComparingTo("3000000");
        assertEquals(1, dto.getItemCount());
        assertEquals("Cây Tùng Nhật", dto.getFirstProductName());
    }

    @Test
    @DisplayName("UT-UUT07-015: convertToMyOrderDTO - Đơn hàng không có orderDetails (fallback treePrice calculation)")
    void convertToMyOrderDTO_noOrderDetails_treePriceFallback() {
        Order order = Order.builder()
                .orderId(101)
                .orderCode("BSMS-101")
                .customerName("Trần Văn B")
                .orderStatus("DEPOSITED")
                .totalAmount(new BigDecimal("5000000"))
                .craneFee(new BigDecimal("500000"))
                .shippingFee(new BigDecimal("200000"))
                .depositAmount(new BigDecimal("1000000"))
                .orderDetails(null)
                .build();

        MyOrderDTO dto = myOrderService.convertToMyOrderDTO(order);

        assertNotNull(dto);
        // treePrice = 5000000 - 500000 - 200000 = 4300000
        // remaining = 4300000 - 1000000 = 3300000
        assertThat(dto.getRemainingPaymentAmount()).isEqualByComparingTo("3300000");
    }

    @Test
    @DisplayName("UT-UUT07-016: convertToMyOrderDTO - depositAmount vượt treePrice")
    void convertToMyOrderDTO_depositExceedsTreePrice_remainingAmountIsZero() {
        Order order = Order.builder()
                .orderId(102)
                .orderCode("BSMS-102")
                .totalAmount(new BigDecimal("1000000"))
                .depositAmount(new BigDecimal("1500000")) // Cọc lớn hơn tổng
                .orderDetails(null)
                .build();

        MyOrderDTO dto = myOrderService.convertToMyOrderDTO(order);

        assertNotNull(dto);
        assertThat(dto.getRemainingPaymentAmount()).isEqualByComparingTo("0");
    }

    @Test
    @DisplayName("UT-UUT07-017: convertToMyOrderDTO - Product firstImageUrl ném Exception")
    void convertToMyOrderDTO_productImageThrowsException_handledSilently() {
        Product mockProduct = mock(Product.class);
        when(mockProduct.getProductName()).thenReturn("Cây Si Cổ Thụ");
        when(mockProduct.getFirstImageUrl()).thenThrow(new RuntimeException("Image load error"));

        OrderDetail detail = OrderDetail.builder()
                .product(mockProduct)
                .priceAtPurchase(new BigDecimal("1000000"))
                .quantity(1)
                .build();

        Order order = Order.builder()
                .orderId(103)
                .orderCode("BSMS-103")
                .orderDetails(List.of(detail))
                .build();

        MyOrderDTO dto = myOrderService.convertToMyOrderDTO(order);

        assertNotNull(dto);
        assertEquals("Cây Si Cổ Thụ", dto.getFirstProductName());
        assertNull(dto.getFirstProductImage());
    }

    @Test
    @DisplayName("UT-UUT07-018: convertToMyOrderDTO - Ngoại lệ tổng quát khi convert")
    void convertToMyOrderDTO_unexpectedException_returnsNull() {
        Order mockOrder = mock(Order.class);
        when(mockOrder.getOrderId()).thenReturn(100);
        when(mockOrder.getCraneFee()).thenThrow(new RuntimeException("Calculation error"));

        MyOrderDTO dto = myOrderService.convertToMyOrderDTO(mockOrder);

        assertNull(dto);
    }

    // =========================================================================
    // Group 4: getMyOrderKPIs
    // =========================================================================

    @Test
    @DisplayName("UT-UUT07-019: getMyOrderKPIs - moderatorId = null")
    void getMyOrderKPIs_nullModeratorId_returnsEmptyKPIs() {
        MyOrderKPIsDTO kpis = myOrderService.getMyOrderKPIs(null);

        assertNotNull(kpis);
        assertEquals(0L, kpis.getCriticalCount());
        assertEquals(0L, kpis.getWaitingApprovalCount());
    }

    @Test
    @DisplayName("UT-UUT07-020: getMyOrderKPIs - Tính toán chính xác các chỉ số KPI")
    void getMyOrderKPIs_validModeratorId_returnsAccurateKPIs() {
        Order o1 = Order.builder().orderId(1).orderStatus("PENDING").orderDate(LocalDateTime.now().minusHours(25))
                .build(); // Critical & Waiting Approval
        Order o2 = Order.builder().orderId(2).orderStatus("PENDING_PAYMENT").build(); // Waiting Payment
        Order o3 = Order.builder().orderId(3).orderStatus("DEPOSITED").build(); // Waiting Delivery
        Order o4 = Order.builder().orderId(4).orderStatus("COMPLETED").build(); // Completed
        Order o5 = Order.builder().orderId(5).orderStatus("CANCELLED").build(); // Cancelled

        List<Order> orderList = List.of(o1, o2, o3, o4, o5);
        Page<Order> orderPage = new PageImpl<>(orderList);

        when(orderRepository.searchMyOrders(eq(5), any(), any(), any())).thenReturn(orderPage);

        MyOrderKPIsDTO kpis = myOrderService.getMyOrderKPIs(5);

        assertNotNull(kpis);
        assertEquals(1, kpis.getCriticalCount());
        assertEquals(1, kpis.getWaitingApprovalCount());
        assertEquals(1, kpis.getWaitingPaymentCount());
        assertEquals(1, kpis.getWaitingDeliveryCount());
        assertEquals(1, kpis.getCompletedCount());
        assertEquals(1, kpis.getCancelledCount());
    }

    // =========================================================================
    // Group 5: getMyOrdersFiltered
    // =========================================================================

    @Test
    @DisplayName("UT-UUT07-021: getMyOrdersFiltered - moderatorId = null")
    void getMyOrdersFiltered_nullModeratorId_returnsEmptyPage() {
        Page<MyOrderDTO> result = myOrderService.getMyOrdersFiltered(null, null, null, null, null, null, 1, 10);

        assertNotNull(result);
        assertThat(result.getContent()).isEmpty();
    }

    @Test
    @DisplayName("UT-UUT07-022: getMyOrdersFiltered - Lọc theo cardFilter (CRITICAL, PENDING, DEPOSITED, COMPLETED)")
    void getMyOrdersFiltered_cardFilterMatching() {
        Order oCritical = Order.builder().orderId(1).orderCode("BSMS-01").orderStatus("PENDING")
                .orderDate(LocalDateTime.now().minusHours(25)).build();
        Order oPending = Order.builder().orderId(2).orderCode("BSMS-02").orderStatus("PENDING")
                .orderDate(LocalDateTime.now().minusHours(1)).build();
        Order oDeposited = Order.builder().orderId(3).orderCode("BSMS-03").orderStatus("DEPOSITED").build();

        List<Order> orderList = List.of(oCritical, oPending, oDeposited);
        when(orderRepository.searchMyOrders(eq(5), any(), any(), any())).thenReturn(new PageImpl<>(orderList));

        // Lọc CRITICAL
        Page<MyOrderDTO> resCritical = myOrderService.getMyOrdersFiltered(5, "CRITICAL", null, null, null, null, 1, 10);
        assertThat(resCritical.getContent()).hasSize(1);
        assertEquals("BSMS-01", resCritical.getContent().get(0).getOrderCode());

        // Lọc WAITING_DELIVERY_PAYMENT (DEPOSITED)
        Page<MyOrderDTO> resDelivery = myOrderService.getMyOrdersFiltered(5, "WAITING_DELIVERY_PAYMENT", null, null,
                null, null, 1, 10);
        assertThat(resDelivery.getContent()).hasSize(1);
        assertEquals("BSMS-03", resDelivery.getContent().get(0).getOrderCode());
    }

    @Test
    @DisplayName("UT-UUT07-023: getMyOrdersFiltered - Lọc theo priorityFilter, statusFilter và search")
    void getMyOrdersFiltered_priorityStatusAndSearchFilters() {
        Order o1 = Order.builder().orderId(1).orderCode("BSMS-100").customerName("Nguyễn Văn A")
                .customerPhone("0988111222").orderStatus("DEPOSITED").totalAmount(new BigDecimal("60000000")).build();

        when(orderRepository.searchMyOrders(eq(5), any(), eq("100"), any())).thenReturn(new PageImpl<>(List.of(o1)));

        Page<MyOrderDTO> result = myOrderService.getMyOrdersFiltered(5, "ALL", "100", "HIGH", "DEPOSITED", "date_desc",
                1, 10);

        assertThat(result.getContent()).hasSize(1);
        assertEquals("BSMS-100", result.getContent().get(0).getOrderCode());
    }

    @Test
    @DisplayName("UT-UUT07-024: getMyOrdersFiltered - Sắp xếp theo price_desc, price_asc, date_asc")
    void getMyOrdersFiltered_sortingOption() {
        Order o1 = Order.builder().orderId(1).orderCode("BSMS-01").orderStatus("PENDING")
                .totalAmount(new BigDecimal("1000000")).orderDate(LocalDateTime.now().minusDays(2)).build();
        Order o2 = Order.builder().orderId(2).orderCode("BSMS-02").orderStatus("PENDING")
                .totalAmount(new BigDecimal("5000000")).orderDate(LocalDateTime.now().minusDays(1)).build();

        when(orderRepository.searchMyOrders(eq(5), any(), any(), any())).thenReturn(new PageImpl<>(List.of(o1, o2)));

        // Sắp xếp price_desc
        Page<MyOrderDTO> resPriceDesc = myOrderService.getMyOrdersFiltered(5, null, null, null, null, "price_desc", 1,
                10);
        assertEquals("BSMS-02", resPriceDesc.getContent().get(0).getOrderCode());

        // Sắp xếp price_asc
        Page<MyOrderDTO> resPriceAsc = myOrderService.getMyOrdersFiltered(5, null, null, null, null, "price_asc", 1,
                10);
        assertEquals("BSMS-01", resPriceAsc.getContent().get(0).getOrderCode());

        // Sắp xếp date_asc
        Page<MyOrderDTO> resDateAsc = myOrderService.getMyOrdersFiltered(5, null, null, null, null, "date_asc", 1, 10);
        assertEquals("BSMS-01", resDateAsc.getContent().get(0).getOrderCode());
    }

    @Test
    @DisplayName("UT-UUT07-025: getMyOrdersFiltered - Phân trang dữ liệu")
    void getMyOrdersFiltered_pagination() {
        List<Order> list = new ArrayList<>();
        LocalDateTime baseTime = LocalDateTime.now().minusDays(30);
        for (int i = 1; i <= 15; i++) {
            list.add(Order.builder()
                    .orderId(i)
                    .orderCode(String.format("BSMS-%02d", i))
                    .orderStatus("PENDING")
                    .orderDate(baseTime.plusDays(i)) // Ngày tăng dần từ BSMS-01 (cũ nhất) đến BSMS-15 (mới nhất)
                    .build());
        }

        when(orderRepository.searchMyOrders(eq(5), any(), any(), any())).thenReturn(new PageImpl<>(list));

        // Sort date_asc: BSMS-01, BSMS-02, ..., BSMS-15
        // Trang 2, limit 10 (lấy từ vị trí 10 đến 14 -> 5 phần tử: BSMS-11, BSMS-12,
        // BSMS-13, BSMS-14, BSMS-15)
        Page<MyOrderDTO> page2 = myOrderService.getMyOrdersFiltered(5, null, null, null, null, "date_asc", 2, 10);

        assertEquals(15, page2.getTotalElements());
        assertThat(page2.getContent()).hasSize(5);
        assertEquals("BSMS-11", page2.getContent().get(0).getOrderCode());
    }
}
