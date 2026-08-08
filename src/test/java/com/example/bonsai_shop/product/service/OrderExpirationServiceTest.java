package com.example.bonsai_shop.product.service;

import com.example.bonsai_shop.entity.Order;
import com.example.bonsai_shop.entity.OrderDetail;
import com.example.bonsai_shop.entity.OrderHandling;
import com.example.bonsai_shop.entity.Payment;
import com.example.bonsai_shop.entity.Product;
import com.example.bonsai_shop.entity.User;
import com.example.bonsai_shop.product.repository.OrderHandlingRepository;
import com.example.bonsai_shop.product.repository.OrderRepository;
import com.example.bonsai_shop.product.repository.PaymentRepository;
import com.example.bonsai_shop.product.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DataAccessException;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OrderExpirationServiceTest {

    private OrderRepository orderRepository;
    private ProductRepository productRepository;
    private PaymentRepository paymentRepository;
    private OrderHandlingRepository orderHandlingRepository;
    private MailService mailService;
    private OrderExpirationService orderExpirationService;

    @BeforeEach
    void setUp() {
        orderRepository = mock(OrderRepository.class);
        productRepository = mock(ProductRepository.class);
        paymentRepository = mock(PaymentRepository.class);
        orderHandlingRepository = mock(OrderHandlingRepository.class);
        mailService = mock(MailService.class);

        orderExpirationService = new OrderExpirationService(
                orderRepository,
                productRepository,
                paymentRepository,
                orderHandlingRepository,
                mailService
        );
        ReflectionTestUtils.setField(orderExpirationService, "inPersonExpirationMinutes", 1440L);
    }

    // =========================================================================
    // Group 1: No Expired Orders (Empty Scans)
    // =========================================================================

    @Test
    @DisplayName("UT-UUT10-001: cancelExpiredOrders - Không có đơn hàng nào hết hạn")
    void cancelExpiredOrders_noExpiredOrders_doesNothing() {
        when(orderRepository.findExpiredOnlineOrders(any())).thenReturn(List.of());
        when(orderRepository.findExpiredOfflineOrders(any())).thenReturn(List.of());
        when(orderRepository.findExpiredInPersonOrders(any())).thenReturn(List.of());

        orderExpirationService.cancelExpiredOrders();

        verify(orderRepository, never()).save(any());
        verify(paymentRepository, never()).save(any());
        verify(orderHandlingRepository, never()).save(any());
        verify(productRepository, never()).save(any());
        verify(mailService, never()).sendOrderRejectedEmail(any(), any());
    }

    // =========================================================================
    // Group 2: Expired Online Orders Cancellation
    // =========================================================================

    @Test
    @DisplayName("UT-UUT10-002: cancelExpiredOrders - Hủy đơn Online quá hạn 15 phút chưa thanh toán VNPay")
    void cancelExpiredOrders_expiredOnlineOrder_cancelsAndSendsEmail() {
        Order onlineOrder = Order.builder()
                .orderId(101)
                .orderCode("BSMS-ONLINE-101")
                .orderStatus("PENDING")
                .orderType("ONLINE")
                .customerEmail("customer@example.com")
                .build();

        when(orderRepository.findExpiredOnlineOrders(any())).thenReturn(List.of(onlineOrder));
        when(orderRepository.findExpiredOfflineOrders(any())).thenReturn(List.of());
        when(orderRepository.findExpiredInPersonOrders(any())).thenReturn(List.of());
        when(paymentRepository.findByOrderOrderIdOrderByPaymentIdAsc(101)).thenReturn(List.of());
        when(orderHandlingRepository.findByOrderOrderIdOrderByHandledAtDesc(101)).thenReturn(List.of());

        orderExpirationService.cancelExpiredOrders();

        assertEquals("CANCELLED", onlineOrder.getOrderStatus());
        assertTrue(onlineOrder.getNotes().contains("Tự động hủy: Đơn hàng online quá hạn 15 phút chưa thanh toán qua VNPay"));
        verify(orderRepository).save(onlineOrder);
        verify(mailService).sendOrderRejectedEmail(eq(onlineOrder), contains("quá hạn 15 phút"));
    }

    @Test
    @DisplayName("UT-UUT10-003: cancelExpiredOrders - Hủy đơn Online có thông tin notes cũ")
    void cancelExpiredOrders_orderWithExistingNotes_appendsReason() {
        Order onlineOrder = Order.builder()
                .orderId(102)
                .orderCode("BSMS-ONLINE-102")
                .orderStatus("PENDING")
                .orderType("ONLINE")
                .notes("Giao giờ hành chính")
                .customerEmail("customer@example.com")
                .build();

        when(orderRepository.findExpiredOnlineOrders(any())).thenReturn(List.of(onlineOrder));
        when(orderRepository.findExpiredOfflineOrders(any())).thenReturn(List.of());
        when(orderRepository.findExpiredInPersonOrders(any())).thenReturn(List.of());

        orderExpirationService.cancelExpiredOrders();

        assertEquals("Giao giờ hành chính | Tự động hủy: Đơn hàng online quá hạn 15 phút chưa thanh toán qua VNPay", onlineOrder.getNotes());
    }

    @Test
    @DisplayName("UT-UUT10-004: cancelExpiredOrders - Hủy đơn Online có các Payment ở trạng thái PENDING -> EXPIRED")
    void cancelExpiredOrders_pendingPayments_updatedToExpired() {
        Order onlineOrder = Order.builder()
                .orderId(103)
                .orderCode("BSMS-ONLINE-103")
                .orderStatus("PENDING")
                .orderType("ONLINE")
                .build();

        Payment pendingPayment = Payment.builder()
                .paymentId(1)
                .paymentStatus("PENDING")
                .paymentType("FULL")
                .build();

        when(orderRepository.findExpiredOnlineOrders(any())).thenReturn(List.of(onlineOrder));
        when(orderRepository.findExpiredOfflineOrders(any())).thenReturn(List.of());
        when(orderRepository.findExpiredInPersonOrders(any())).thenReturn(List.of());
        when(paymentRepository.findByOrderOrderIdOrderByPaymentIdAsc(103)).thenReturn(List.of(pendingPayment));

        orderExpirationService.cancelExpiredOrders();

        assertEquals("EXPIRED", pendingPayment.getPaymentStatus());
        verify(paymentRepository).save(pendingPayment);
    }

    @Test
    @DisplayName("UT-UUT10-005: cancelExpiredOrders - Hủy đơn Online có Payment không phải PENDING (VD: FAILED)")
    void cancelExpiredOrders_nonPendingPayments_remainsUnchanged() {
        Order onlineOrder = Order.builder()
                .orderId(104)
                .orderCode("BSMS-ONLINE-104")
                .orderStatus("PENDING")
                .orderType("ONLINE")
                .build();

        Payment failedPayment = Payment.builder()
                .paymentId(2)
                .paymentStatus("FAILED")
                .paymentType("FULL")
                .build();

        when(orderRepository.findExpiredOnlineOrders(any())).thenReturn(List.of(onlineOrder));
        when(orderRepository.findExpiredOfflineOrders(any())).thenReturn(List.of());
        when(orderRepository.findExpiredInPersonOrders(any())).thenReturn(List.of());
        when(paymentRepository.findByOrderOrderIdOrderByPaymentIdAsc(104)).thenReturn(List.of(failedPayment));

        orderExpirationService.cancelExpiredOrders();

        assertEquals("FAILED", failedPayment.getPaymentStatus());
        verify(paymentRepository, never()).save(failedPayment);
    }

    @Test
    @DisplayName("UT-UUT10-006: cancelExpiredOrders - Hủy đơn Online có OrderHandling đang active -> giải phóng")
    void cancelExpiredOrders_activeOrderHandling_released() {
        Order onlineOrder = Order.builder()
                .orderId(105)
                .orderCode("BSMS-ONLINE-105")
                .orderStatus("PENDING")
                .orderType("ONLINE")
                .build();

        User moderator = User.builder().userId(10).username("mod10").build();
        OrderHandling activeHandling = OrderHandling.builder()
                .orderHandlingId(50)
                .moderator(moderator)
                .isActive(true)
                .build();

        when(orderRepository.findExpiredOnlineOrders(any())).thenReturn(List.of(onlineOrder));
        when(orderRepository.findExpiredOfflineOrders(any())).thenReturn(List.of());
        when(orderRepository.findExpiredInPersonOrders(any())).thenReturn(List.of());
        when(orderHandlingRepository.findByOrderOrderIdOrderByHandledAtDesc(105)).thenReturn(List.of(activeHandling));

        orderExpirationService.cancelExpiredOrders();

        assertFalse(activeHandling.getIsActive());
        assertNotNull(activeHandling.getReleasedAt());
        verify(orderHandlingRepository).save(activeHandling);
    }

    @Test
    @DisplayName("UT-UUT10-007: cancelExpiredOrders - Hủy đơn Online có sản phẩm trong orderDetails -> AVAILABLE")
    void cancelExpiredOrders_reservedProducts_releasedToAvailable() {
        Product reservedProduct = Product.builder()
                .productId(201)
                .productName("Cây Tùng Bách")
                .productStatus("RESERVED")
                .build();

        OrderDetail detail = OrderDetail.builder()
                .orderDetailId(1)
                .product(reservedProduct)
                .build();

        Order onlineOrder = Order.builder()
                .orderId(106)
                .orderCode("BSMS-ONLINE-106")
                .orderStatus("PENDING")
                .orderType("ONLINE")
                .orderDetails(List.of(detail))
                .build();

        when(orderRepository.findExpiredOnlineOrders(any())).thenReturn(List.of(onlineOrder));
        when(orderRepository.findExpiredOfflineOrders(any())).thenReturn(List.of());
        when(orderRepository.findExpiredInPersonOrders(any())).thenReturn(List.of());

        orderExpirationService.cancelExpiredOrders();

        assertEquals("AVAILABLE", reservedProduct.getProductStatus());
        verify(productRepository).save(reservedProduct);
    }

    @Test
    @DisplayName("UT-UUT10-008: cancelExpiredOrders - Hủy đơn Online có sản phẩm đã là AVAILABLE -> Bỏ qua")
    void cancelExpiredOrders_alreadyAvailableProducts_skipped() {
        Product availableProduct = Product.builder()
                .productId(202)
                .productName("Cây Mai Vàng")
                .productStatus("AVAILABLE")
                .build();

        OrderDetail detail = OrderDetail.builder()
                .orderDetailId(2)
                .product(availableProduct)
                .build();

        Order onlineOrder = Order.builder()
                .orderId(107)
                .orderCode("BSMS-ONLINE-107")
                .orderStatus("PENDING")
                .orderType("ONLINE")
                .orderDetails(List.of(detail))
                .build();

        when(orderRepository.findExpiredOnlineOrders(any())).thenReturn(List.of(onlineOrder));
        when(orderRepository.findExpiredOfflineOrders(any())).thenReturn(List.of());
        when(orderRepository.findExpiredInPersonOrders(any())).thenReturn(List.of());

        orderExpirationService.cancelExpiredOrders();

        assertEquals("AVAILABLE", availableProduct.getProductStatus());
        verify(productRepository, never()).save(availableProduct);
    }

    // =========================================================================
    // Group 3: Expired Offline Orders Cancellation
    // =========================================================================

    @Test
    @DisplayName("UT-UUT10-009: cancelExpiredOrders - Hủy đơn Offline quá hạn 48 giờ")
    void cancelExpiredOrders_expiredOfflineOrder_cancelsAndSendsEmail() {
        Order offlineOrder = Order.builder()
                .orderId(108)
                .orderCode("BSMS-OFFLINE-108")
                .orderStatus("PENDING")
                .orderType("OFFLINE")
                .customerEmail("offline@example.com")
                .build();

        when(orderRepository.findExpiredOnlineOrders(any())).thenReturn(List.of());
        when(orderRepository.findExpiredOfflineOrders(any())).thenReturn(List.of(offlineOrder));
        when(orderRepository.findExpiredInPersonOrders(any())).thenReturn(List.of());

        orderExpirationService.cancelExpiredOrders();

        assertEquals("CANCELLED", offlineOrder.getOrderStatus());
        assertTrue(offlineOrder.getNotes().contains("Tự động hủy: Đơn hàng quá hạn 48 giờ"));
        verify(orderRepository).save(offlineOrder);
        verify(mailService).sendOrderRejectedEmail(eq(offlineOrder), contains("48 giờ"));
    }

    // =========================================================================
    // Group 4: Expired In-Person Orders Cancellation
    // =========================================================================

    @Test
    @DisplayName("UT-UUT10-010: cancelExpiredOrders - Hủy đơn In-Person quá hạn KHÔNG gửi email")
    void cancelExpiredOrders_expiredInPersonOrder_cancelsWithoutEmail() {
        Order inPersonOrder = Order.builder()
                .orderId(109)
                .orderCode("BSMS-INPERSON-109")
                .orderStatus("PENDING")
                .orderType("IN_PERSON")
                .customerEmail("walkin@example.com")
                .build();

        when(orderRepository.findExpiredOnlineOrders(any())).thenReturn(List.of());
        when(orderRepository.findExpiredOfflineOrders(any())).thenReturn(List.of());
        when(orderRepository.findExpiredInPersonOrders(any())).thenReturn(List.of(inPersonOrder));

        orderExpirationService.cancelExpiredOrders();

        assertEquals("CANCELLED", inPersonOrder.getOrderStatus());
        assertTrue(inPersonOrder.getNotes().contains("In-person order quá hạn"));
        verify(orderRepository).save(inPersonOrder);
        verify(mailService, never()).sendOrderRejectedEmail(any(), any());
    }

    // =========================================================================
    // Group 5: Exception Handling & Edge Cases
    // =========================================================================

    @Test
    @DisplayName("UT-UUT10-011: cancelExpiredOrders - Gửi email thông báo hủy bị ném Exception")
    void cancelExpiredOrders_emailServiceThrowsException_swallowsErrorAndContinues() {
        Order onlineOrder = Order.builder()
                .orderId(110)
                .orderCode("BSMS-ONLINE-110")
                .orderStatus("PENDING")
                .orderType("ONLINE")
                .customerEmail("error@example.com")
                .build();

        when(orderRepository.findExpiredOnlineOrders(any())).thenReturn(List.of(onlineOrder));
        when(orderRepository.findExpiredOfflineOrders(any())).thenReturn(List.of());
        when(orderRepository.findExpiredInPersonOrders(any())).thenReturn(List.of());
        doThrow(new RuntimeException("SMTP connection error")).when(mailService).sendOrderRejectedEmail(any(), any());

        orderExpirationService.cancelExpiredOrders();

        assertEquals("CANCELLED", onlineOrder.getOrderStatus());
        verify(orderRepository).save(onlineOrder);
    }

    @Test
    @DisplayName("UT-UUT10-012: cancelExpiredOrders - Đơn hàng có orderDetails = null hoặc orderId = null")
    void cancelExpiredOrders_nullDetailsOrOrderId_handlesSafely() {
        Order nullOrder = Order.builder()
                .orderId(null)
                .orderCode("BSMS-NULL")
                .orderStatus("PENDING")
                .orderType("ONLINE")
                .orderDetails(null)
                .build();

        when(orderRepository.findExpiredOnlineOrders(any())).thenReturn(List.of(nullOrder));
        when(orderRepository.findExpiredOfflineOrders(any())).thenReturn(List.of());
        when(orderRepository.findExpiredInPersonOrders(any())).thenReturn(List.of());

        orderExpirationService.cancelExpiredOrders();

        assertEquals("CANCELLED", nullOrder.getOrderStatus());
        verify(orderRepository).save(nullOrder);
        verify(paymentRepository, never()).findByOrderOrderIdOrderByPaymentIdAsc(any());
        verify(orderHandlingRepository, never()).findByOrderOrderIdOrderByHandledAtDesc(any());
    }

    @Test
    @DisplayName("UT-UUT10-013: cancelExpiredOrders - orderRepository.save ném DataAccessException")
    void cancelExpiredOrders_repositorySaveThrowsException_propagatesError() {
        Order onlineOrder = Order.builder()
                .orderId(111)
                .orderCode("BSMS-ONLINE-111")
                .orderStatus("PENDING")
                .orderType("ONLINE")
                .build();

        when(orderRepository.findExpiredOnlineOrders(any())).thenReturn(List.of(onlineOrder));
        when(orderRepository.save(any())).thenThrow(new DataAccessException("DB Connection Lost") {});

        assertThrows(DataAccessException.class, () -> orderExpirationService.cancelExpiredOrders());
    }
}
