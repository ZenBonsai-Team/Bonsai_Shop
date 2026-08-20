package com.example.bonsai_shop.artisan1.service;

import com.example.bonsai_shop.artisan.service.ArtisanInPersonOrderService;
import com.example.bonsai_shop.artisan.service.ArtisanProductService;
import com.example.bonsai_shop.entity.Order;
import com.example.bonsai_shop.entity.OrderDetail;
import com.example.bonsai_shop.entity.OrderLog;
import com.example.bonsai_shop.entity.Payment;
import com.example.bonsai_shop.entity.Product;
import com.example.bonsai_shop.entity.User;
import com.example.bonsai_shop.finance.service.FinancialLedgerService;
import com.example.bonsai_shop.product.repository.OrderLogRepository;
import com.example.bonsai_shop.product.repository.OrderRepository;
import com.example.bonsai_shop.product.repository.PaymentRepository;
import com.example.bonsai_shop.product.repository.ProductRepository;
import com.example.bonsai_shop.product.service.MailService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ArtisanInPersonOrderServiceTest {

    private ArtisanProductService artisanProductService;
    private ProductRepository productRepository;
    private OrderRepository orderRepository;
    private PaymentRepository paymentRepository;
    private OrderLogRepository orderLogRepository;
    private MailService mailService;
    private ArtisanInPersonOrderService inPersonOrderService;

    @BeforeEach
    void setUp() {
        artisanProductService = mock(ArtisanProductService.class);
        productRepository = mock(ProductRepository.class);
        orderRepository = mock(OrderRepository.class);
        paymentRepository = mock(PaymentRepository.class);
        orderLogRepository = mock(OrderLogRepository.class);
        mailService = mock(MailService.class);
        FinancialLedgerService financialLedgerService = mock(FinancialLedgerService.class);

        inPersonOrderService = new ArtisanInPersonOrderService(
                artisanProductService,
                productRepository,
                orderRepository,
                paymentRepository,
                orderLogRepository,
                mailService,
                financialLedgerService
        );
    }

    @Test
    void getInPersonOrders_WhenStatusIsBlank_ShouldUseAllFilter() {
        User artisan = artisan(10);
        Page<Order> expectedOrders = new PageImpl<>(List.of(order(1)));

        when(artisanProductService.getArtisanUser("artisan@test.com")).thenReturn(artisan);
        when(orderRepository.searchByArtisanUserIdAndTypeAndStatus(
                10,
                "IN_PERSON",
                "ALL",
                "",
                PageRequest.of(0, 10)
        )).thenReturn(expectedOrders);

        Page<Order> result = inPersonOrderService.getInPersonOrders("artisan@test.com", "   ", "", 0, 10);

        assertThat(result).isEqualTo(expectedOrders);
        verify(orderRepository).searchByArtisanUserIdAndTypeAndStatus(
                10,
                "IN_PERSON",
                "ALL",
                "",
                PageRequest.of(0, 10)
        );
    }

    @Test
    void getInPersonOrders_WhenPageOrSizeInvalid_ShouldNormalizePageable() {
        User artisan = artisan(10);
        Page<Order> expectedOrders = new PageImpl<>(List.of(order(1)));

        when(artisanProductService.getArtisanUser("artisan@test.com")).thenReturn(artisan);
        when(orderRepository.searchByArtisanUserIdAndTypeAndStatus(
                10,
                "IN_PERSON",
                "ALL",
                "",
                PageRequest.of(0, 1)
        )).thenReturn(expectedOrders);

        Page<Order> result = inPersonOrderService.getInPersonOrders("artisan@test.com", "ALL", "", -1, 0);

        assertThat(result).isEqualTo(expectedOrders);
        verify(orderRepository).searchByArtisanUserIdAndTypeAndStatus(
                10,
                "IN_PERSON",
                "ALL",
                "",
                PageRequest.of(0, 1)
        );
    }

    @Test
    void getInPersonOrders_WhenKeywordProvided_ShouldTrimKeyword() {
        User artisan = artisan(10);
        Page<Order> expectedOrders = new PageImpl<>(List.of(order(1)));

        when(artisanProductService.getArtisanUser("artisan@test.com")).thenReturn(artisan);
        when(orderRepository.searchByArtisanUserIdAndTypeAndStatus(
                10,
                "IN_PERSON",
                "ALL",
                "BSMS-001",
                PageRequest.of(0, 10)
        )).thenReturn(expectedOrders);

        Page<Order> result = inPersonOrderService.getInPersonOrders("artisan@test.com", "ALL", "  BSMS-001  ", 0, 10);

        assertThat(result).isEqualTo(expectedOrders);
        verify(orderRepository).searchByArtisanUserIdAndTypeAndStatus(
                10,
                "IN_PERSON",
                "ALL",
                "BSMS-001",
                PageRequest.of(0, 10)
        );
    }

    @Test
    void createInPersonOrder_WhenRequestIsValid_ShouldReserveProductAndCreateOrderPaymentLog() {
        User artisan = artisan(10);
        Product product = product(101, artisan, "AVAILABLE");
        mockProductLookup(artisan, product);
        when(productRepository.reserveIfAvailable(101)).thenReturn(1);
        when(orderRepository.findByOrderCode(anyString())).thenReturn(Optional.empty());
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Order result = createOrder("CASH", BigDecimal.ZERO, BigDecimal.ZERO, "Valid note");

        ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
        ArgumentCaptor<Payment> paymentCaptor = ArgumentCaptor.forClass(Payment.class);
        verify(productRepository).reserveIfAvailable(101);
        verify(orderRepository).save(orderCaptor.capture());
        verify(paymentRepository).save(paymentCaptor.capture());
        verify(orderLogRepository).save(any(OrderLog.class));
        assertThat(result).isEqualTo(orderCaptor.getValue());
        assertThat(product.getProductStatus()).isEqualTo("RESERVED");
        assertThat(orderCaptor.getValue().getOrderStatus()).isEqualTo("PENDING_PAYMENT");
        assertThat(orderCaptor.getValue().getOrderType()).isEqualTo("IN_PERSON");
        assertThat(paymentCaptor.getValue().getPaymentStatus()).isEqualTo("PENDING");
        assertThat(paymentCaptor.getValue().getPaymentMethod()).isEqualTo("CASH");
    }

    @Test
    void createInPersonOrder_WhenProductIsNotAvailable_ShouldRejectBeforeReserve() {
        User artisan = artisan(10);
        Product product = product(101, artisan, "SOLD");
        mockProductLookup(artisan, product);

        assertThatThrownBy(() -> createOrder("CASH", BigDecimal.ZERO, BigDecimal.ZERO, null))
                .isInstanceOf(RuntimeException.class);

        verifyNoCreateSideEffects();
    }

    @Test
    void createInPersonOrder_WhenReservationFails_ShouldRejectBeforeCreateOrder() {
        User artisan = artisan(10);
        Product product = product(101, artisan, "AVAILABLE");
        mockProductLookup(artisan, product);
        when(productRepository.reserveIfAvailable(101)).thenReturn(0);
        when(orderRepository.findByOrderCode(anyString())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> createOrder("CASH", BigDecimal.ZERO, BigDecimal.ZERO, null))
                .isInstanceOf(RuntimeException.class);

        verify(productRepository).reserveIfAvailable(101);
        verify(orderRepository, never()).save(any(Order.class));
        verify(paymentRepository, never()).save(any(Payment.class));
        verify(orderLogRepository, never()).save(any(OrderLog.class));
    }

    @Test
    void createInPersonOrder_WhenPaymentMethodInvalid_ShouldRejectBeforeReserve() {
        User artisan = artisan(10);
        Product product = product(101, artisan, "AVAILABLE");
        mockProductLookup(artisan, product);

        assertThatThrownBy(() -> createOrder("BANK", BigDecimal.ZERO, BigDecimal.ZERO, null))
                .isInstanceOf(RuntimeException.class);

        verifyNoCreateSideEffects();
    }

    @Test
    void createInPersonOrder_WhenFeeIsNegative_ShouldRejectBeforeReserve() {
        User artisan = artisan(10);
        Product product = product(101, artisan, "AVAILABLE");
        mockProductLookup(artisan, product);

        assertThatThrownBy(() -> createOrder("CASH", new BigDecimal("-1"), BigDecimal.ZERO, null))
                .isInstanceOf(RuntimeException.class);

        verifyNoCreateSideEffects();
    }

    @Test
    void createInPersonOrder_WhenCustomerInformationInvalid_ShouldRejectBeforeReserve() {
        User artisan = artisan(10);
        Product product = product(101, artisan, "AVAILABLE");
        mockProductLookup(artisan, product);

        assertThatThrownBy(() -> inPersonOrderService.createInPersonOrder(
                "artisan@test.com",
                101,
                "Walk-in Customer",
                "09000abc00",
                "FPT HCM",
                "CASH",
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                "customer@test.com",
                null
        )).isInstanceOf(RuntimeException.class);

        verifyNoCreateSideEffects();
    }

    @Test
    void createInPersonOrder_WhenNotesExceedsMaximumLength_ShouldRejectBeforeReserve() {
        User artisan = artisan(10);
        Product product = product(101, artisan, "AVAILABLE");
        mockProductLookup(artisan, product);

        assertThatThrownBy(() -> createOrder("CASH", BigDecimal.ZERO, BigDecimal.ZERO, "a".repeat(401)))
                .isInstanceOf(RuntimeException.class);

        verifyNoCreateSideEffects();
    }

    @Test
    void cancelInPersonOrder_WhenPendingInPersonOrderIsValid_ShouldCancelPaymentAndReleaseProduct() {
        User artisan = artisan(10);
        Product product = product(101, artisan, "RESERVED");
        Order order = inPersonOrder(1, product, "PENDING_PAYMENT", "IN_PERSON");
        Payment payment = pendingPayment(11, order);
        order.setPayments(List.of(payment));

        mockOrderLookup(artisan, order);
        when(orderRepository.save(order)).thenReturn(order);

        Order result = inPersonOrderService.cancelInPersonOrder("artisan@test.com", 1);

        assertThat(result).isEqualTo(order);
        assertThat(order.getOrderStatus()).isEqualTo("CANCELLED");
        assertThat(payment.getPaymentStatus()).isEqualTo("CANCELLED");
        assertThat(product.getProductStatus()).isEqualTo("AVAILABLE");
        verify(paymentRepository).save(payment);
        verify(productRepository).save(product);
        verify(orderRepository).save(order);
        verify(orderLogRepository).save(any(OrderLog.class));
    }

    @Test
    void cancelInPersonOrder_WhenExistingNotesAreTooLong_ShouldTrimNotesBeforeAppendingCancelReason() {
        User artisan = artisan(10);
        Product product = product(101, artisan, "RESERVED");
        Order order = inPersonOrder(1, product, "PENDING_PAYMENT", "IN_PERSON");
        Payment payment = pendingPayment(11, order);
        order.setPayments(List.of(payment));
        order.setNotes("a".repeat(500));

        mockOrderLookup(artisan, order);
        when(orderRepository.save(order)).thenReturn(order);

        inPersonOrderService.cancelInPersonOrder("artisan@test.com", 1);

        assertThat(order.getNotes()).hasSizeLessThanOrEqualTo(500);
        assertThat(order.getNotes()).endsWith("Lý do hủy: Khách đổi ý không mua.");
        verify(orderRepository).save(order);
    }

    @Test
    void cancelInPersonOrder_WhenOrderBelongsToAnotherArtisan_ShouldRejectWithoutPersisting() {
        User artisan = artisan(10);
        Product product = product(101, artisan(99), "RESERVED");
        Order order = inPersonOrder(1, product, "PENDING_PAYMENT", "IN_PERSON");
        Payment payment = pendingPayment(11, order);
        order.setPayments(List.of(payment));

        mockOrderLookup(artisan, order);

        assertThatThrownBy(() -> inPersonOrderService.cancelInPersonOrder("artisan@test.com", 1))
                .isInstanceOf(RuntimeException.class);

        assertThat(order.getOrderStatus()).isEqualTo("PENDING_PAYMENT");
        assertThat(payment.getPaymentStatus()).isEqualTo("PENDING");
        assertThat(product.getProductStatus()).isEqualTo("RESERVED");
        verify(productRepository, never()).save(any(Product.class));
        verify(paymentRepository, never()).save(any(Payment.class));
        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    void updateInPersonOrder_WhenPendingInPersonOrderIsValid_ShouldUpdateOrderPaymentAndLog() {
        User artisan = artisan(10);
        Product product = product(101, artisan, "RESERVED");
        Order order = inPersonOrder(1, product, "PENDING_PAYMENT", "IN_PERSON");
        Payment payment = pendingPayment(11, order);
        order.setPayments(List.of(payment));

        mockOrderLookup(artisan, order);
        when(orderRepository.save(order)).thenReturn(order);

        Order result = inPersonOrderService.updateInPersonOrder(
                "artisan@test.com",
                1,
                "Updated Customer",
                "0911111111",
                "Updated Address",
                "VNPAY",
                new BigDecimal("200000"),
                new BigDecimal("75000"),
                "updated@test.com",
                "Updated notes"
        );

        assertThat(result).isEqualTo(order);
        assertThat(order.getCustomerName()).isEqualTo("Updated Customer");
        assertThat(order.getTotalAmount()).isEqualByComparingTo("1775000");
        assertThat(payment.getPaymentMethod()).isEqualTo("VNPAY");
        assertThat(payment.getAmount()).isEqualByComparingTo("1775000");
        verify(paymentRepository).save(payment);
        verify(orderRepository).save(order);
        verify(orderLogRepository).save(any(OrderLog.class));
    }

    @Test
    void updateInPersonOrder_WhenOrderStatusOrTypeInvalid_ShouldRejectWithoutPersisting() {
        User artisan = artisan(10);
        Product product = product(101, artisan, "RESERVED");
        Order order = inPersonOrder(1, product, "COMPLETED", "IN_PERSON");

        mockOrderLookup(artisan, order);

        assertThatThrownBy(() -> updateOrder())
                .isInstanceOf(RuntimeException.class);

        verify(orderRepository, never()).save(any(Order.class));
        verify(paymentRepository, never()).save(any(Payment.class));
        verify(orderLogRepository, never()).save(any(OrderLog.class));
    }

    @Test
    void confirmPayment_WhenPendingInPersonOrderIsValid_ShouldCompleteOrderPaymentProductAndSendEmail() {
        User artisan = artisan(10);
        Product product = product(101, artisan, "RESERVED");
        Order order = inPersonOrder(1, product, "PENDING_PAYMENT", "IN_PERSON");
        Payment payment = pendingPayment(11, order);
        order.setPayments(List.of(payment));

        mockOrderLookup(artisan, order);
        when(orderRepository.save(order)).thenReturn(order);

        Order result = inPersonOrderService.confirmPayment("artisan@test.com", 1);

        assertThat(result).isEqualTo(order);
        assertThat(order.getOrderStatus()).isEqualTo("COMPLETED");
        assertThat(order.getCompletedAt()).isNotNull();
        assertThat(payment.getPaymentStatus()).isEqualTo("SUCCESS");
        assertThat(payment.getPaymentDate()).isNotNull();
        assertThat(product.getProductStatus()).isEqualTo("SOLD");
        verify(paymentRepository).save(payment);
        verify(productRepository).save(product);
        verify(orderRepository).save(order);
        verify(orderLogRepository).save(any(OrderLog.class));
        verify(mailService).sendInPersonOrderPaidEmail(order);
    }

    @Test
    void confirmPayment_WhenOrderAlreadyCompleted_ShouldReturnUnchangedOrder() {
        User artisan = artisan(10);
        Product product = product(101, artisan, "SOLD");
        Order order = inPersonOrder(1, product, "COMPLETED", "IN_PERSON");
        Payment payment = pendingPayment(11, order);
        order.setPayments(List.of(payment));

        mockOrderLookup(artisan, order);

        Order result = inPersonOrderService.confirmPayment("artisan@test.com", 1);

        assertThat(result).isEqualTo(order);
        assertThat(order.getOrderStatus()).isEqualTo("COMPLETED");
        assertThat(payment.getPaymentStatus()).isEqualTo("PENDING");
        assertThat(product.getProductStatus()).isEqualTo("SOLD");
        verify(paymentRepository, never()).save(any(Payment.class));
        verify(productRepository, never()).save(any(Product.class));
        verify(orderRepository, never()).save(any(Order.class));
        verify(orderLogRepository, never()).save(any(OrderLog.class));
        verify(mailService, never()).sendInPersonOrderPaidEmail(any(Order.class));
    }

    @Test
    void confirmPayment_WhenOrderHasNoOrderDetail_ShouldRejectWithoutPersisting() {
        User artisan = artisan(10);
        Order order = order(1);
        order.setOrderStatus("PENDING_PAYMENT");
        order.setOrderType("IN_PERSON");
        order.setOrderDetails(List.of());
        Payment payment = pendingPayment(11, order);
        order.setPayments(List.of(payment));

        when(artisanProductService.getArtisanUser("artisan@test.com")).thenReturn(artisan);
        when(orderRepository.findByIdWithDetailsForUpdate(1)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> inPersonOrderService.confirmPayment("artisan@test.com", 1))
                .isInstanceOf(RuntimeException.class);

        assertThat(payment.getPaymentStatus()).isEqualTo("PENDING");
        verify(paymentRepository, never()).save(any(Payment.class));
        verify(productRepository, never()).save(any(Product.class));
        verify(orderRepository, never()).save(any(Order.class));
    }

    private Order createOrder(String paymentMethod, BigDecimal craneFee, BigDecimal shippingFee, String notes) {
        return inPersonOrderService.createInPersonOrder(
                "artisan@test.com",
                101,
                "Walk-in Customer",
                "0900000000",
                "FPT HCM",
                paymentMethod,
                craneFee,
                shippingFee,
                "customer@test.com",
                notes
        );
    }

    private Order updateOrder() {
        return inPersonOrderService.updateInPersonOrder(
                "artisan@test.com",
                1,
                "Updated Customer",
                "0911111111",
                "Updated Address",
                "CASH",
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                "updated@test.com",
                null
        );
    }

    private void mockProductLookup(User artisan, Product product) {
        when(artisanProductService.getArtisanUser("artisan@test.com")).thenReturn(artisan);
        when(productRepository.findByProductIdAndCreatedByUserId(101, artisan.getUserId()))
                .thenReturn(Optional.of(product));
    }

    private void mockOrderLookup(User artisan, Order order) {
        when(artisanProductService.getArtisanUser("artisan@test.com")).thenReturn(artisan);
        when(orderRepository.findByIdWithDetailsForUpdate(1)).thenReturn(Optional.of(order));
    }

    private void verifyNoCreateSideEffects() {
        verify(productRepository, never()).reserveIfAvailable(any(Integer.class));
        verify(orderRepository, never()).save(any(Order.class));
        verify(paymentRepository, never()).save(any(Payment.class));
        verify(orderLogRepository, never()).save(any(OrderLog.class));
    }

    private User artisan(Integer userId) {
        return User.builder()
                .userId(userId)
                .email("artisan@test.com")
                .fullName("Artisan")
                .build();
    }

    private Product product(Integer productId, User artisan, String status) {
        return Product.builder()
                .productId(productId)
                .createdBy(artisan)
                .productName("Bonsai " + productId)
                .productStatus(status)
                .price(new BigDecimal("1500000"))
                .build();
    }

    private Order order(Integer orderId) {
        return Order.builder()
                .orderId(orderId)
                .orderCode("BSMS-100001")
                .orderStatus("PENDING_PAYMENT")
                .orderType("IN_PERSON")
                .build();
    }

    private Order inPersonOrder(Integer orderId, Product product, String status, String orderType) {
        Order order = Order.builder()
                .orderId(orderId)
                .orderCode("BSMS-100001")
                .customerName("Walk-in Customer")
                .customerPhone("0900000000")
                .customerEmail("customer@test.com")
                .shippingAddress("FPT HCM")
                .totalAmount(new BigDecimal("1500000"))
                .craneFee(BigDecimal.ZERO)
                .shippingFee(BigDecimal.ZERO)
                .orderStatus(status)
                .orderType(orderType)
                .build();
        order.setOrderDetails(List.of(OrderDetail.builder()
                .order(order)
                .product(product)
                .priceAtPurchase(product.getPrice())
                .build()));
        return order;
    }

    private Payment pendingPayment(Integer paymentId, Order order) {
        return Payment.builder()
                .paymentId(paymentId)
                .order(order)
                .paymentMethod("CASH")
                .paymentStatus("PENDING")
                .paymentType("FULL_PAYMENT")
                .amount(order.getTotalAmount())
                .build();
    }
}
