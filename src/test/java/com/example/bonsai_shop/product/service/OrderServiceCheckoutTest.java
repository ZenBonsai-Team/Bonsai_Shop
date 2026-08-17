package com.example.bonsai_shop.product.service;

import com.example.bonsai_shop.customer.repository.ModerationNotificationRepository;
import com.example.bonsai_shop.product.dto.PurchaseOrderRequestDTO;
import com.example.bonsai_shop.entity.CartItem;
import com.example.bonsai_shop.entity.Order;
import com.example.bonsai_shop.entity.Payment;
import com.example.bonsai_shop.entity.Product;
import com.example.bonsai_shop.entity.User;
import com.example.bonsai_shop.product.event.OrderCreatedEvent;
import com.example.bonsai_shop.finance.service.FinancialLedgerService;
import com.example.bonsai_shop.product.enums.PaymentType;
import com.example.bonsai_shop.product.repository.OrderHandlingRepository;
import com.example.bonsai_shop.product.repository.OrderLogRepository;
import com.example.bonsai_shop.product.repository.OrderRepository;
import com.example.bonsai_shop.product.repository.PaymentRepository;
import com.example.bonsai_shop.product.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataAccessException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OrderServiceCheckoutTest {

    private OrderRepository orderRepository;
    private ProductRepository productRepository;
    private PaymentRepository paymentRepository;
    private ApplicationEventPublisher eventPublisher;
    private MailService mailService;
    private CartService cartService;
    private OrderService orderService;

    @BeforeEach
    void setUp() {
        orderRepository = mock(OrderRepository.class);
        productRepository = mock(ProductRepository.class);
        OrderLogRepository orderLogRepository = mock(OrderLogRepository.class);
        OrderHandlingRepository orderHandlingRepository = mock(OrderHandlingRepository.class);
        paymentRepository = mock(PaymentRepository.class);
        eventPublisher = mock(ApplicationEventPublisher.class);
        mailService = mock(MailService.class);
        cartService = mock(CartService.class);
        FinancialLedgerService financialLedgerService = mock(FinancialLedgerService.class);
        ModerationNotificationRepository notificationRepository = mock(ModerationNotificationRepository.class);

        orderService = new OrderService(
                orderRepository,
                productRepository,
                orderLogRepository,
                orderHandlingRepository,
                paymentRepository,
                eventPublisher,
                mailService,
                cartService,
                financialLedgerService,
                notificationRepository);
    }

    @Test
    @DisplayName("UT-UUT01-001: Tạo đơn đặt cọc (DEPOSIT) thành công")
    void createOrder_deposit_success() {
        User customer = User.builder().userId(10).fullName("Nguyễn Văn A").build();
        Product product = Product.builder().productId(10).price(new BigDecimal("1000000")).productStatus("AVAILABLE")
                .build();

        PurchaseOrderRequestDTO dto = new PurchaseOrderRequestDTO();
        dto.setProductIds(List.of(10));
        dto.setPaymentMethod("DEPOSIT");
        dto.setCustomerName("Nguyễn Văn A");

        when(productRepository.findAllById(List.of(10))).thenReturn(List.of(product));
        when(productRepository.reserveIfAvailable(10)).thenReturn(1);
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order o = invocation.getArgument(0);
            o.setOrderId(100);
            return o;
        });

        Order result = orderService.createOrder(dto, customer);

        assertThat(result).isNotNull();
        assertThat(result.getOrderCode()).startsWith("BSMS-");
        assertThat(result.getOrderStatus()).isEqualTo("PENDING");
        assertThat(result.getTotalAmount()).isEqualTo(new BigDecimal("1000000"));
        assertThat(result.getDepositAmount()).isEqualTo(BigDecimal.ZERO);
        assertThat(product.getProductStatus()).isEqualTo("RESERVED");

        ArgumentCaptor<Payment> paymentCaptor = ArgumentCaptor.forClass(Payment.class);
        verify(paymentRepository, times(1)).save(paymentCaptor.capture());
        Payment savedPayment = paymentCaptor.getValue();
        assertThat(savedPayment.getPaymentType()).isEqualTo(PaymentType.DEPOSIT.name());
        assertThat(savedPayment.getPaymentMethod()).isEqualTo("DEPOSIT");
        assertThat(savedPayment.getPaymentStatus()).isEqualTo("PENDING");
        assertThat(savedPayment.getAmount()).isEqualTo(new BigDecimal("1000000"));

        verify(cartService, times(1)).clearCart(10);

        ArgumentCaptor<Object> eventCaptor = ArgumentCaptor.forClass(Object.class);
        verify(eventPublisher, times(1)).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue()).isInstanceOf(OrderCreatedEvent.class);
        assertThat(((OrderCreatedEvent) eventCaptor.getValue()).getOrder().getOrderId()).isEqualTo(100);

        verify(mailService, never()).sendOrderCreatedEmail(any());
    }

    @Test
    @DisplayName("UT-UUT01-002: Tạo đơn thanh toán 100% (FULL_PAYMENT) thành công")
    void createOrder_fullPayment_success() {
        User customer = User.builder().userId(10).build();
        Product product = Product.builder().productId(10).price(new BigDecimal("1000000")).productStatus("AVAILABLE")
                .build();

        PurchaseOrderRequestDTO dto = new PurchaseOrderRequestDTO();
        dto.setProductIds(List.of(10));
        dto.setPaymentMethod("FULL_PAYMENT");

        when(productRepository.findAllById(List.of(10))).thenReturn(List.of(product));
        when(productRepository.reserveIfAvailable(10)).thenReturn(1);
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Order result = orderService.createOrder(dto, customer);

        assertThat(result).isNotNull();

        ArgumentCaptor<Payment> paymentCaptor = ArgumentCaptor.forClass(Payment.class);
        verify(paymentRepository, times(1)).save(paymentCaptor.capture());
        Payment savedPayment = paymentCaptor.getValue();
        assertThat(savedPayment.getPaymentType()).isEqualTo(PaymentType.FULL_PAYMENT.name());
        assertThat(savedPayment.getPaymentMethod()).isEqualTo("FULL_PAYMENT");
        assertThat(savedPayment.getAmount()).isEqualTo(new BigDecimal("1000000"));

        verify(cartService, times(1)).clearCart(10);
        verify(eventPublisher, times(1)).publishEvent(any(OrderCreatedEvent.class));
    }

    @Test
    @DisplayName("UT-UUT01-003: Tạo đơn thất bại do DTO không có sản phẩm")
    void createOrder_emptyProducts_throwsIllegalArgumentException() {
        User customer = User.builder().userId(10).build();
        PurchaseOrderRequestDTO dto = new PurchaseOrderRequestDTO();

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> orderService.createOrder(dto, customer));

        assertThat(exception.getMessage()).isEqualTo("Giỏ hàng của bạn đang trống! Vui lòng chọn sản phẩm trước.");
        verify(orderRepository, never()).save(any());
        verify(paymentRepository, never()).save(any());
        verify(cartService, never()).clearCart(anyInt());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName("UT-UUT01-004: Tạo đơn khách vãng lai thành công (Customer = null)")
    void createOrder_guestCheckout_success() {
        Product product = Product.builder().productId(10).price(new BigDecimal("1000000")).productStatus("AVAILABLE")
                .build();

        PurchaseOrderRequestDTO dto = new PurchaseOrderRequestDTO();
        dto.setProductIds(List.of(10));
        dto.setPaymentMethod("DEPOSIT");

        when(productRepository.findAllById(List.of(10))).thenReturn(List.of(product));
        when(productRepository.reserveIfAvailable(10)).thenReturn(1);
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Order result = orderService.createOrder(dto, null);

        assertThat(result).isNotNull();
        assertThat(result.getCustomer()).isNull();
        verify(cartService, never()).clearCart(anyInt());
        verify(eventPublisher, times(1)).publishEvent(any(OrderCreatedEvent.class));
    }

    @Test
    @DisplayName("UT-UUT01-005: Nạp sản phẩm cho đơn hàng thành công (loadProductsForOrder)")
    void loadProductsForOrder_success() {
        User customer = User.builder().userId(10).build();
        Product p1 = Product.builder().productId(10).build();
        Product p2 = Product.builder().productId(20).build();

        PurchaseOrderRequestDTO dto = new PurchaseOrderRequestDTO();
        dto.setProductIds(List.of(10, 20));

        when(productRepository.findAllById(List.of(10, 20))).thenReturn(List.of(p1, p2));

        List<Product> products = orderService.loadProductsForOrder(dto, customer);

        assertThat(products).isNotNull().hasSize(2);
        verify(productRepository, times(1)).findAllById(List.of(10, 20));
    }

    @Test
    @DisplayName("UT-UUT01-006: Nạp sản phẩm trả về danh sách rỗng khi sản phẩm không tồn tại (loadProductsForOrder)")
    void loadProductsForOrder_notFound_returnsEmptyList() {
        User customer = User.builder().userId(10).build();
        PurchaseOrderRequestDTO dto = new PurchaseOrderRequestDTO();
        dto.setProductIds(List.of(999));

        when(productRepository.findAllById(List.of(999))).thenReturn(List.of());

        List<Product> products = orderService.loadProductsForOrder(dto, customer);

        assertThat(products).isNotNull().isEmpty();
        verify(productRepository, times(1)).findAllById(List.of(999));
    }

    @Test
    @DisplayName("UT-UUT01-007: Kiểm tra tồn kho khả dụng trả về danh sách rỗng khi tất cả cây đều AVAILABLE (validateProductAvailability)")
    void validateProductAvailability_allAvailable_returnsEmptyList() {
        Product p1 = Product.builder().productId(10).productStatus("AVAILABLE").build();

        List<Product> unAvailable = orderService.validateProductAvailability(List.of(p1));

        assertThat(unAvailable).isNotNull().isEmpty();
    }

    @Test
    @DisplayName("UT-UUT01-008: Kiểm tra tồn kho lọc ra danh sách cây đã bán SOLD (validateProductAvailability)")
    void validateProductAvailability_containsSoldProduct_returnsUnAvailableList() {
        Product p1 = Product.builder().productId(10).productName("Tùng La Hán").productStatus("SOLD").build();

        List<Product> unAvailable = orderService.validateProductAvailability(List.of(p1));

        assertThat(unAvailable).isNotNull().hasSize(1);
        assertThat(unAvailable.get(0).getProductId()).isEqualTo(10);
        assertThat(unAvailable.get(0).getProductStatus()).isEqualTo("SOLD");
    }

    @Test
    @DisplayName("UT-UUT01-009: Kiểm tra tồn kho lọc ra danh sách cây đang giữ chỗ RESERVED (validateProductAvailability)")
    void validateProductAvailability_containsReservedProduct_returnsUnAvailableList() {
        Product p1 = Product.builder().productId(10).productName("Mai Vàng").productStatus("RESERVED").build();
        Product p2 = Product.builder().productId(20).productStatus("AVAILABLE").build();

        List<Product> unAvailable = orderService.validateProductAvailability(List.of(p1, p2));

        assertThat(unAvailable).isNotNull().hasSize(1);
        assertThat(unAvailable.get(0).getProductId()).isEqualTo(10);
    }

    @Test
    @DisplayName("UT-UUT01-010: Tra cứu đơn theo Mã khách hàng hợp lệ (getOrdersByCustomerId)")
    void getOrdersByCustomerId_success() {
        Order o1 = Order.builder().orderId(1).build();
        Order o2 = Order.builder().orderId(2).build();

        when(orderRepository.findByCustomerUserIdWithDetailsOrderByOrderDateDesc(10)).thenReturn(List.of(o1, o2));

        List<Order> orders = orderService.getOrdersByCustomerId(10);

        assertThat(orders).isNotNull().hasSize(2);
        verify(orderRepository, times(1)).findByCustomerUserIdWithDetailsOrderByOrderDateDesc(10);
    }

    @Test
    @DisplayName("UT-UUT01-011: Tra cứu đơn theo Mã khách hàng null (getOrdersByCustomerId)")
    void getOrdersByCustomerId_nullCustomerId_returnsEmptyList() {
        when(orderRepository.findByCustomerUserIdWithDetailsOrderByOrderDateDesc(null)).thenReturn(List.of());

        List<Order> orders = orderService.getOrdersByCustomerId(null);

        assertThat(orders).isNotNull().isEmpty();
        verify(orderRepository, times(1)).findByCustomerUserIdWithDetailsOrderByOrderDateDesc(null);
    }

    @Test
    @DisplayName("UT-UUT01-012: Tra cứu đơn theo mã OrderCode tồn tại (getOrderByCode)")
    void getOrderByCode_found() {
        Order order = Order.builder().orderId(100).orderCode("BSMS-123").build();

        when(orderRepository.findByOrderCode("BSMS-123")).thenReturn(Optional.of(order));

        Order result = orderService.getOrderByCode("BSMS-123");

        assertThat(result).isNotNull();
        assertThat(result.getOrderCode()).isEqualTo("BSMS-123");
        verify(orderRepository, times(1)).findByOrderCode("BSMS-123");
    }

    @Test
    @DisplayName("UT-UUT01-013: Tra cứu đơn theo mã OrderCode không tồn tại (getOrderByCode)")
    void getOrderByCode_notFound() {
        when(orderRepository.findByOrderCode("BSMS-999")).thenReturn(Optional.empty());

        Order result = orderService.getOrderByCode("BSMS-999");

        assertThat(result).isNull();
        verify(orderRepository, times(1)).findByOrderCode("BSMS-999");
    }

    @Test
    @DisplayName("UT-UUT01-014: Tra cứu đơn kèm chi tiết theo OrderCode hợp lệ (getOrderByCodeWithDetails)")
    void getOrderByCodeWithDetails_found() {
        Order order = Order.builder().orderId(100).orderCode("BSMS-123").build();

        when(orderRepository.findByOrderCodeWithDetails("BSMS-123")).thenReturn(Optional.of(order));

        Order result = orderService.getOrderByCodeWithDetails("BSMS-123");

        assertThat(result).isNotNull();
        assertThat(result.getOrderCode()).isEqualTo("BSMS-123");
        verify(orderRepository, times(1)).findByOrderCodeWithDetails("BSMS-123");
    }

    @Test
    @DisplayName("UT-UUT01-015: Tra cứu đơn kèm chi tiết theo OrderCode rỗng (getOrderByCodeWithDetails)")
    void getOrderByCodeWithDetails_nullOrBlankCode() {
        Order result = orderService.getOrderByCodeWithDetails("");

        assertThat(result).isNull();
        verify(orderRepository, never()).findByOrderCodeWithDetails(any());
    }

    @Test
    @DisplayName("UT-UUT01-016: Tạo đơn thất bại do sản phẩm có trạng thái không phải AVAILABLE trong pre-check (createOrder)")
    void createOrder_productNotAvailable_throwsIllegalStateException() {
        User customer = User.builder().userId(10).build();
        Product product = Product.builder().productId(10).productName("Mai Vàng").productStatus("SOLD").build();

        PurchaseOrderRequestDTO dto = new PurchaseOrderRequestDTO();
        dto.setProductIds(List.of(10));

        when(productRepository.findAllById(List.of(10))).thenReturn(List.of(product));

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> orderService.createOrder(dto, customer));

        assertThat(exception.getMessage()).isEqualTo("Tác phẩm 'Mai Vàng' đã được bán hoặc giữ chỗ!");
        verify(orderRepository, never()).save(any());
        verify(paymentRepository, never()).save(any());
    }

    @Test
    @DisplayName("UT-UUT01-017: Tạo đơn thất bại do reserveIfAvailable trả về 0 trong quá trình tranh chấp giữ chỗ (createOrder)")
    void createOrder_reserveCountZero_throwsIllegalStateException() {
        User customer = User.builder().userId(10).build();
        Product product = Product.builder().productId(10).productName("Tùng La Hán").price(new BigDecimal("1000000"))
                .productStatus("AVAILABLE").build();

        PurchaseOrderRequestDTO dto = new PurchaseOrderRequestDTO();
        dto.setProductIds(List.of(10));

        when(productRepository.findAllById(List.of(10))).thenReturn(List.of(product));
        when(productRepository.reserveIfAvailable(10)).thenReturn(0);

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> orderService.createOrder(dto, customer));

        assertThat(exception.getMessage()).isEqualTo("Tác phẩm 'Tùng La Hán' đã được bán hoặc giữ chỗ!");
        verify(orderRepository, never()).save(any());
        verify(paymentRepository, never()).save(any());
    }

    @Test
    @DisplayName("UT-UUT01-018: Tạo đơn thành công từ giỏ hàng (Cart Checkout)")
    void createOrder_fromCart_success() {
        User customer = User.builder().userId(10).build();
        Product product = Product.builder().productId(10).price(new BigDecimal("500000")).productStatus("AVAILABLE")
                .build();
        CartItem item = CartItem.builder().product(product).build();

        PurchaseOrderRequestDTO dto = new PurchaseOrderRequestDTO(); // productIds & productId null

        when(cartService.getCartItems(10)).thenReturn(List.of(item));
        when(productRepository.reserveIfAvailable(10)).thenReturn(1);
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Order result = orderService.createOrder(dto, customer);

        assertThat(result).isNotNull();
        assertThat(result.getTotalAmount()).isEqualTo(new BigDecimal("500000"));
        verify(cartService, times(1)).clearCart(10);
    }

    @Test
    @DisplayName("UT-UUT01-019: Dependency Failure: orderRepository.save ném DataAccessException trong createOrder")
    void createOrder_orderRepositorySaveThrowsDataAccessException_propagatesException() {
        User customer = User.builder().userId(10).build();
        Product product = Product.builder().productId(10).price(new BigDecimal("1000000")).productStatus("AVAILABLE")
                .build();

        PurchaseOrderRequestDTO dto = new PurchaseOrderRequestDTO();
        dto.setProductIds(List.of(10));

        when(productRepository.findAllById(List.of(10))).thenReturn(List.of(product));
        when(productRepository.reserveIfAvailable(10)).thenReturn(1);
        when(orderRepository.save(any(Order.class))).thenThrow(new DataAccessException("DB Save Timeout") {
        });

        DataAccessException exception = assertThrows(DataAccessException.class,
                () -> orderService.createOrder(dto, customer));

        assertThat(exception.getMessage()).isEqualTo("DB Save Timeout");
        assertThat(product.getProductStatus()).isEqualTo("RESERVED");

        verify(orderRepository, times(1)).save(any());
        verify(paymentRepository, never()).save(any());
        verify(cartService, never()).clearCart(anyInt());
        verify(eventPublisher, never()).publishEvent(any());
    }
}
