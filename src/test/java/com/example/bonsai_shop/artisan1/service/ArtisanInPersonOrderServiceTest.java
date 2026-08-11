package com.example.bonsai_shop.artisan1.service;

import com.example.bonsai_shop.artisan.service.ArtisanInPersonOrderService;
import com.example.bonsai_shop.artisan.service.ArtisanProductService;
import com.example.bonsai_shop.entity.Order;
import com.example.bonsai_shop.entity.OrderLog;
import com.example.bonsai_shop.entity.Payment;
import com.example.bonsai_shop.entity.Product;
import com.example.bonsai_shop.entity.User;
import com.example.bonsai_shop.entity.FinancialLedger;
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
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ArtisanInPersonOrderServiceTest {

    private ArtisanProductService artisanProductService;
    private ProductRepository productRepository;
    private OrderRepository orderRepository;
    private PaymentRepository paymentRepository;
    private OrderLogRepository orderLogRepository;
    private MailService mailService;
    private FinancialLedgerService financialLedgerService;
    private ArtisanInPersonOrderService inPersonOrderService;

    @BeforeEach
    void setUp() {
        artisanProductService = mock(ArtisanProductService.class);
        productRepository = mock(ProductRepository.class);
        orderRepository = mock(OrderRepository.class);
        paymentRepository = mock(PaymentRepository.class);
        orderLogRepository = mock(OrderLogRepository.class);
        mailService = mock(MailService.class);
        financialLedgerService = mock(FinancialLedgerService.class);

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
    void getAvailableProducts_WhenArtisanExists_ShouldReturnAvailableProducts() {
        User artisan = artisan(10);
        List<Product> expectedProducts = List.of(product(101, artisan, "AVAILABLE"));

        when(artisanProductService.getArtisanUser("artisan@test.com"))
                .thenReturn(artisan);
        when(productRepository.findByCreatedByUserIdAndProductStatusOrderByCreatedAtDesc(10, "AVAILABLE"))
                .thenReturn(expectedProducts);

        List<Product> result = inPersonOrderService.getAvailableProducts("artisan@test.com");

        assertThat(result).isEqualTo(expectedProducts);
        verify(productRepository).findByCreatedByUserIdAndProductStatusOrderByCreatedAtDesc(10, "AVAILABLE");
    }

    @Test
    void getInPersonOrders_WhenStatusProvided_ShouldReturnFilteredOrdersPage() {
        User artisan = artisan(10);
        Page<Order> expectedOrders = new PageImpl<>(List.of(order(1)));

        when(artisanProductService.getArtisanUser("artisan@test.com"))
                .thenReturn(artisan);
        when(orderRepository.findByArtisanUserIdAndTypeAndStatus(
                10,
                "IN_PERSON",
                "PENDING_PAYMENT",
                PageRequest.of(0, 10)
        )).thenReturn(expectedOrders);

        Page<Order> result = inPersonOrderService.getInPersonOrders("artisan@test.com", "PENDING_PAYMENT", 0, 10);

        assertThat(result).isEqualTo(expectedOrders);
        verify(orderRepository).findByArtisanUserIdAndTypeAndStatus(
                10,
                "IN_PERSON",
                "PENDING_PAYMENT",
                PageRequest.of(0, 10)
        );
    }

    @Test
    void createInPersonOrder_WhenRequestIsValid_ShouldCreateOrderPaymentLogAndReserveProduct() {
        User artisan = artisan(10);
        Product product = product(101, artisan, "AVAILABLE");

        when(artisanProductService.getArtisanUser("artisan@test.com"))
                .thenReturn(artisan);
        when(productRepository.findByProductIdAndCreatedByUserId(101, 10))
                .thenReturn(Optional.of(product));
        when(productRepository.reserveIfAvailable(101))
                .thenReturn(1);
        when(orderRepository.findByOrderCode(anyString()))
                .thenReturn(Optional.empty());
        when(orderRepository.save(any(Order.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Order result = inPersonOrderService.createInPersonOrder(
                "artisan@test.com",
                101,
                "Walk-in Customer",
                "0900000000",
                "FPT HCM",
                "CASH",
                new BigDecimal("100000"),
                new BigDecimal("50000"),
                "customer@test.com",
                "Pay at store"
        );

        ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
        ArgumentCaptor<Payment> paymentCaptor = ArgumentCaptor.forClass(Payment.class);
        ArgumentCaptor<OrderLog> orderLogCaptor = ArgumentCaptor.forClass(OrderLog.class);
        verify(productRepository).reserveIfAvailable(101);
        verify(orderRepository).save(orderCaptor.capture());
        verify(paymentRepository).save(paymentCaptor.capture());
        verify(orderLogRepository).save(orderLogCaptor.capture());
        assertThat(result).isEqualTo(orderCaptor.getValue());
        assertThat(product.getProductStatus()).isEqualTo("RESERVED");
        assertThat(orderCaptor.getValue().getOrderStatus()).isEqualTo("PENDING_PAYMENT");
        assertThat(orderCaptor.getValue().getOrderType()).isEqualTo("IN_PERSON");
        assertThat(orderCaptor.getValue().getTotalAmount()).isEqualByComparingTo("1650000");
        assertThat(paymentCaptor.getValue().getPaymentMethod()).isEqualTo("CASH");
        assertThat(paymentCaptor.getValue().getPaymentStatus()).isEqualTo("PENDING");
        assertThat(paymentCaptor.getValue().getPaymentType()).isEqualTo("FULL_PAYMENT");
        assertThat(paymentCaptor.getValue().getAmount()).isEqualByComparingTo("1650000");
        assertThat(orderLogCaptor.getValue().getActionType()).isEqualTo("IN_PERSON_CREATE");
    }

    @Test
    void createInPersonOrder_WhenProductNotOwnedByArtisan_ShouldThrowException() {
        User artisan = artisan(10);

        when(artisanProductService.getArtisanUser("artisan@test.com"))
                .thenReturn(artisan);
        when(productRepository.findByProductIdAndCreatedByUserId(101, 10))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> createValidInPersonOrder())
                .isInstanceOf(RuntimeException.class);

        verify(productRepository, never()).reserveIfAvailable(any(Integer.class));
        verify(orderRepository, never()).save(any(Order.class));
        verify(paymentRepository, never()).save(any(Payment.class));
        verify(orderLogRepository, never()).save(any(OrderLog.class));
    }

    @Test
    void createInPersonOrder_WhenProductIsNotAvailable_ShouldThrowException() {
        User artisan = artisan(10);
        Product product = product(101, artisan, "SOLD");

        when(artisanProductService.getArtisanUser("artisan@test.com"))
                .thenReturn(artisan);
        when(productRepository.findByProductIdAndCreatedByUserId(101, 10))
                .thenReturn(Optional.of(product));

        assertThatThrownBy(() -> createValidInPersonOrder())
                .isInstanceOf(RuntimeException.class);

        verify(productRepository, never()).reserveIfAvailable(any(Integer.class));
        verify(orderRepository, never()).save(any(Order.class));
        verify(paymentRepository, never()).save(any(Payment.class));
        verify(orderLogRepository, never()).save(any(OrderLog.class));
    }

    @Test
    void createInPersonOrder_WhenProductReservationFails_ShouldThrowException() {
        User artisan = artisan(10);
        Product product = product(101, artisan, "AVAILABLE");

        when(artisanProductService.getArtisanUser("artisan@test.com"))
                .thenReturn(artisan);
        when(productRepository.findByProductIdAndCreatedByUserId(101, 10))
                .thenReturn(Optional.of(product));
        when(productRepository.reserveIfAvailable(101))
                .thenReturn(0);
        when(orderRepository.findByOrderCode(anyString()))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> createValidInPersonOrder())
                .isInstanceOf(RuntimeException.class);

        verify(productRepository).reserveIfAvailable(101);
        verify(orderRepository, never()).save(any(Order.class));
        verify(paymentRepository, never()).save(any(Payment.class));
        verify(orderLogRepository, never()).save(any(OrderLog.class));
    }

    @Test
    void cancelInPersonOrder_WhenOrderIsPendingPayment_ShouldCancelOrderPaymentAndReleaseProduct() {
        User artisan = artisan(10);
        Product product = product(101, artisan, "RESERVED");
        Order order = inPersonOrder(1, product, "PENDING_PAYMENT", "IN_PERSON");
        Payment payment = pendingPayment(11, order);
        order.setPayments(List.of(payment));

        when(artisanProductService.getArtisanUser("artisan@test.com"))
                .thenReturn(artisan);
        when(orderRepository.findById(1))
                .thenReturn(Optional.of(order));
        when(orderRepository.save(order))
                .thenReturn(order);

        Order result = inPersonOrderService.cancelInPersonOrder("artisan@test.com", 1, "Customer changed mind");

        assertThat(result).isEqualTo(order);
        assertThat(order.getOrderStatus()).isEqualTo("CANCELLED");
        assertThat(order.getNotes()).contains("Customer changed mind");
        assertThat(product.getProductStatus()).isEqualTo("AVAILABLE");
        assertThat(payment.getPaymentStatus()).isEqualTo("CANCELLED");
        verify(paymentRepository).save(payment);
        verify(productRepository).save(product);
        verify(orderRepository).save(order);
        verify(orderLogRepository).save(any(OrderLog.class));
    }

    @Test
    void cancelInPersonOrder_WhenOrderDoesNotExist_ShouldThrowException() {
        User artisan = artisan(10);

        when(artisanProductService.getArtisanUser("artisan@test.com"))
                .thenReturn(artisan);
        when(orderRepository.findById(1))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> inPersonOrderService.cancelInPersonOrder("artisan@test.com", 1, "No reason"))
                .isInstanceOf(RuntimeException.class);

        verify(productRepository, never()).save(any(Product.class));
        verify(paymentRepository, never()).save(any(Payment.class));
    }

    @Test
    void cancelInPersonOrder_WhenOrderIsNotOwnedByArtisan_ShouldThrowException() {
        User artisan = artisan(10);
        User anotherArtisan = artisan(99);
        Product product = product(101, anotherArtisan, "RESERVED");
        Order order = inPersonOrder(1, product, "PENDING_PAYMENT", "IN_PERSON");
        Payment payment = pendingPayment(11, order);
        order.setPayments(List.of(payment));

        when(artisanProductService.getArtisanUser("artisan@test.com"))
                .thenReturn(artisan);
        when(orderRepository.findById(1))
                .thenReturn(Optional.of(order));

        assertThatThrownBy(() -> inPersonOrderService.cancelInPersonOrder("artisan@test.com", 1, "No reason"))
                .isInstanceOf(RuntimeException.class);

        assertThat(order.getOrderStatus()).isEqualTo("PENDING_PAYMENT");
        assertThat(product.getProductStatus()).isEqualTo("RESERVED");
        assertThat(payment.getPaymentStatus()).isEqualTo("PENDING");
        verify(productRepository, never()).save(any(Product.class));
        verify(paymentRepository, never()).save(any(Payment.class));
        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    void cancelInPersonOrder_WhenOrderTypeOrStatusInvalid_ShouldThrowException() {
        User artisan = artisan(10);
        Product product = product(101, artisan, "RESERVED");
        Order order = inPersonOrder(1, product, "COMPLETED", "ONLINE");

        when(artisanProductService.getArtisanUser("artisan@test.com"))
                .thenReturn(artisan);
        when(orderRepository.findById(1))
                .thenReturn(Optional.of(order));

        assertThatThrownBy(() -> inPersonOrderService.cancelInPersonOrder("artisan@test.com", 1, "No reason"))
                .isInstanceOf(RuntimeException.class);

        assertThat(order.getOrderStatus()).isEqualTo("COMPLETED");
        verify(productRepository, never()).save(any(Product.class));
        verify(paymentRepository, never()).save(any(Payment.class));
        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    void updateInPersonOrder_WhenRequestIsValid_ShouldUpdateOrderAndPayment() {
        User artisan = artisan(10);
        Product product = product(101, artisan, "RESERVED");
        Order order = inPersonOrder(1, product, "PENDING_PAYMENT", "IN_PERSON");
        Payment payment = pendingPayment(11, order);
        order.setPayments(List.of(payment));

        when(artisanProductService.getArtisanUser("artisan@test.com"))
                .thenReturn(artisan);
        when(orderRepository.findById(1))
                .thenReturn(Optional.of(order));
        when(orderRepository.save(order))
                .thenReturn(order);

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
        assertThat(order.getCustomerPhone()).isEqualTo("0911111111");
        assertThat(order.getShippingAddress()).isEqualTo("Updated Address");
        assertThat(order.getCustomerEmail()).isEqualTo("updated@test.com");
        assertThat(order.getTotalAmount()).isEqualByComparingTo("1775000");
        assertThat(order.getNotes()).isEqualTo("Updated notes");
        assertThat(payment.getPaymentMethod()).isEqualTo("VNPAY");
        assertThat(payment.getAmount()).isEqualByComparingTo("1775000");
        verify(paymentRepository).save(payment);
        verify(orderRepository).save(order);
        verify(orderLogRepository).save(any(OrderLog.class));
    }

    @Test
    void updateInPersonOrder_WhenOrderNotOwnedByArtisanOrOnline_ShouldThrowException() {
        User artisan = artisan(10);
        User anotherArtisan = artisan(99);
        Product product = product(101, anotherArtisan, "RESERVED");
        Order order = inPersonOrder(1, product, "PENDING_PAYMENT", "ONLINE");

        when(artisanProductService.getArtisanUser("artisan@test.com"))
                .thenReturn(artisan);
        when(orderRepository.findById(1))
                .thenReturn(Optional.of(order));

        assertThatThrownBy(() -> updateValidInPersonOrder())
                .isInstanceOf(RuntimeException.class);

        verify(orderRepository, never()).save(any(Order.class));
        verify(paymentRepository, never()).save(any(Payment.class));
    }

    @Test
    void updateInPersonOrder_WhenOrderIsNotPendingPayment_ShouldThrowException() {
        User artisan = artisan(10);
        Product product = product(101, artisan, "RESERVED");
        Order order = inPersonOrder(1, product, "COMPLETED", "IN_PERSON");

        when(artisanProductService.getArtisanUser("artisan@test.com"))
                .thenReturn(artisan);
        when(orderRepository.findById(1))
                .thenReturn(Optional.of(order));

        assertThatThrownBy(() -> updateValidInPersonOrder())
                .isInstanceOf(RuntimeException.class);

        verify(orderRepository, never()).save(any(Order.class));
        verify(paymentRepository, never()).save(any(Payment.class));
    }

    @Test
    void updateInPersonOrder_WhenPaymentMissing_ShouldCreateNewPayment() {
        User artisan = artisan(10);
        Product product = product(101, artisan, "RESERVED");
        Order order = inPersonOrder(1, product, "PENDING_PAYMENT", "IN_PERSON");
        order.setPayments(List.of());

        when(artisanProductService.getArtisanUser("artisan@test.com"))
                .thenReturn(artisan);
        when(orderRepository.findById(1))
                .thenReturn(Optional.of(order));
        when(paymentRepository.findTopByOrderOrderIdAndPaymentStatusOrderByPaymentIdDesc(1, "PENDING"))
                .thenReturn(Optional.empty());
        when(orderRepository.save(order))
                .thenReturn(order);

        inPersonOrderService.updateInPersonOrder(
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

        ArgumentCaptor<Payment> paymentCaptor = ArgumentCaptor.forClass(Payment.class);
        verify(paymentRepository).save(paymentCaptor.capture());
        assertThat(paymentCaptor.getValue().getOrder()).isEqualTo(order);
        assertThat(paymentCaptor.getValue().getPaymentStatus()).isEqualTo("PENDING");
        assertThat(paymentCaptor.getValue().getPaymentType()).isEqualTo("FULL_PAYMENT");
        assertThat(paymentCaptor.getValue().getAmount()).isEqualByComparingTo("1500000");
    }

    @Test
    void confirmPayment_WhenOrderIsPendingPayment_ShouldCompleteOrderPaymentAndProduct() {
        User artisan = artisan(10);
        Product product = product(101, artisan, "RESERVED");
        Order order = inPersonOrder(1, product, "PENDING_PAYMENT", "IN_PERSON");
        Payment payment = pendingPayment(11, order);
        order.setPayments(List.of(payment));

        when(artisanProductService.getArtisanUser("artisan@test.com"))
                .thenReturn(artisan);
        when(orderRepository.findById(1))
                .thenReturn(Optional.of(order));
        when(orderRepository.save(order))
                .thenReturn(order);

        Order result = inPersonOrderService.confirmPayment("artisan@test.com", 1);

        assertThat(result).isEqualTo(order);
        assertThat(payment.getPaymentStatus()).isEqualTo("SUCCESS");
        assertThat(payment.getPaymentDate()).isNotNull();
        assertThat(order.getOrderStatus()).isEqualTo("COMPLETED");
        assertThat(order.getCompletedAt()).isNotNull();
        assertThat(product.getProductStatus()).isEqualTo("SOLD");
        verify(paymentRepository).save(payment);
        verify(productRepository).save(product);
        verify(orderRepository).save(order);
        verify(mailService).sendInPersonOrderPaidEmail(order);
    }

    @Test
    void confirmPayment_WhenOrderNotOwnedOrInvalidStatus_ShouldThrowException() {
        User artisan = artisan(10);
        User anotherArtisan = artisan(99);
        Product product = product(101, anotherArtisan, "RESERVED");
        Order order = inPersonOrder(1, product, "COMPLETED", "IN_PERSON");
        Payment payment = pendingPayment(11, order);
        order.setPayments(List.of(payment));

        when(artisanProductService.getArtisanUser("artisan@test.com"))
                .thenReturn(artisan);
        when(orderRepository.findById(1))
                .thenReturn(Optional.of(order));

        assertThatThrownBy(() -> inPersonOrderService.confirmPayment("artisan@test.com", 1))
                .isInstanceOf(RuntimeException.class);

        assertThat(payment.getPaymentStatus()).isEqualTo("PENDING");
        assertThat(product.getProductStatus()).isEqualTo("RESERVED");
        verify(paymentRepository, never()).save(any(Payment.class));
        verify(productRepository, never()).save(any(Product.class));
        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    void confirmPayment_WhenLedgerCreated_ShouldLogCompletedOrderRevenueRecorded() {
        User artisan = artisan(10);
        Product product = product(101, artisan, "RESERVED");
        Order order = inPersonOrder(1, product, "PENDING_PAYMENT", "IN_PERSON");
        Payment payment = pendingPayment(11, order);
        order.setPayments(List.of(payment));

        when(artisanProductService.getArtisanUser("artisan@test.com"))
                .thenReturn(artisan);
        when(orderRepository.findById(1))
                .thenReturn(Optional.of(order));
        when(orderRepository.save(order))
                .thenReturn(order);
        when(financialLedgerService.recordCompletedOrderRevenueIfAbsent(any(Order.class), any(User.class), any()))
                .thenReturn(FinancialLedger.builder().order(order).recordedBy(artisan).amount(order.getTotalAmount()).build());

        inPersonOrderService.confirmPayment("artisan@test.com", 1);

        ArgumentCaptor<OrderLog> orderLogCaptor = ArgumentCaptor.forClass(OrderLog.class);
        verify(orderLogRepository, org.mockito.Mockito.times(2)).save(orderLogCaptor.capture());
        assertThat(orderLogCaptor.getAllValues())
                .extracting(OrderLog::getActionType)
                .contains("COMPLETED_ORDER_REVENUE_RECORDED", "IN_PERSON_PAYMENT_CONFIRMED");
    }

    @Test
    void confirmPayment_WhenEmailSendingFails_ShouldKeepOrderPaymentAndProductCompleted() {
        User artisan = artisan(10);
        Product product = product(101, artisan, "RESERVED");
        Order order = inPersonOrder(1, product, "PENDING_PAYMENT", "IN_PERSON");
        Payment payment = pendingPayment(11, order);
        order.setPayments(List.of(payment));

        when(artisanProductService.getArtisanUser("artisan@test.com"))
                .thenReturn(artisan);
        when(orderRepository.findById(1))
                .thenReturn(Optional.of(order));
        when(orderRepository.save(order))
                .thenReturn(order);
        doThrow(new RuntimeException("SMTP unavailable"))
                .when(mailService).sendInPersonOrderPaidEmail(order);

        Order result = inPersonOrderService.confirmPayment("artisan@test.com", 1);

        assertThat(result.getOrderStatus()).isEqualTo("COMPLETED");
        assertThat(payment.getPaymentStatus()).isEqualTo("SUCCESS");
        assertThat(product.getProductStatus()).isEqualTo("SOLD");
        verify(orderRepository).save(order);
    }

    @Test
    void confirmPayment_WhenOrderHasNoOrderDetail_ShouldThrowException() {
        User artisan = artisan(10);
        Order order = inPersonOrder(1, product(101, artisan, "RESERVED"), "PENDING_PAYMENT", "IN_PERSON");
        order.setOrderDetails(List.of());
        Payment payment = pendingPayment(11, order);
        order.setPayments(List.of(payment));

        when(artisanProductService.getArtisanUser("artisan@test.com"))
                .thenReturn(artisan);
        when(orderRepository.findById(1))
                .thenReturn(Optional.of(order));

        assertThatThrownBy(() -> inPersonOrderService.confirmPayment("artisan@test.com", 1))
                .isInstanceOf(RuntimeException.class);

        assertThat(payment.getPaymentStatus()).isEqualTo("PENDING");
        verify(productRepository, never()).save(any(Product.class));
        verify(paymentRepository, never()).save(any(Payment.class));
    }

    private Order createValidInPersonOrder() {
        return inPersonOrderService.createInPersonOrder(
                "artisan@test.com",
                101,
                "Walk-in Customer",
                "0900000000",
                "FPT HCM",
                "CASH",
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                "customer@test.com",
                null
        );
    }

    private Order updateValidInPersonOrder() {
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
        order.setOrderDetails(List.of(com.example.bonsai_shop.entity.OrderDetail.builder()
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
