package com.example.bonsai_shop.moderator.service;

import com.example.bonsai_shop.entity.Category;
import com.example.bonsai_shop.entity.Order;
import com.example.bonsai_shop.entity.OrderDetail;
import com.example.bonsai_shop.entity.OrderHandling;
import com.example.bonsai_shop.entity.OrderLog;
import com.example.bonsai_shop.entity.Payment;
import com.example.bonsai_shop.entity.Product;
import com.example.bonsai_shop.entity.User;
import com.example.bonsai_shop.entity.Variety;
import com.example.bonsai_shop.exception.OrderNotFoundException;
import com.example.bonsai_shop.finance.service.FinancialLedgerService;
import com.example.bonsai_shop.moderator.dto.OrderDetailDTO;
import com.example.bonsai_shop.product.repository.OrderHandlingRepository;
import com.example.bonsai_shop.product.repository.OrderLogRepository;
import com.example.bonsai_shop.product.repository.OrderRepository;
import com.example.bonsai_shop.product.repository.PaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class OrderDetailServiceTest {

    private OrderRepository orderRepository;
    private PaymentRepository paymentRepository;
    private OrderHandlingRepository orderHandlingRepository;
    private OrderLogRepository orderLogRepository;
    private MyOrderService myOrderService;
    private FinancialLedgerService financialLedgerService;
    private OrderDetailService orderDetailService;

    @BeforeEach
    void setUp() {
        orderRepository = mock(OrderRepository.class);
        paymentRepository = mock(PaymentRepository.class);
        orderHandlingRepository = mock(OrderHandlingRepository.class);
        orderLogRepository = mock(OrderLogRepository.class);
        myOrderService = mock(MyOrderService.class);
        financialLedgerService = mock(FinancialLedgerService.class);

        orderDetailService = new OrderDetailService(
                orderRepository,
                paymentRepository,
                orderHandlingRepository,
                orderLogRepository,
                myOrderService,
                financialLedgerService
        );

        // Default mock behaviors for financialLedgerService and myOrderService to prevent NPEs in tests
        when(financialLedgerService.sumRecognizedCompletedRevenue(any())).thenReturn(BigDecimal.ZERO);
        when(financialLedgerService.sumForfeitedDepositIncome(any())).thenReturn(BigDecimal.ZERO);
        when(financialLedgerService.sumFullRefunds(any())).thenReturn(BigDecimal.ZERO);
        when(financialLedgerService.sumNetRecognizedAmount(any())).thenReturn(BigDecimal.ZERO);
        when(financialLedgerService.calculateRefundableCash(any())).thenReturn(BigDecimal.ZERO);
        when(financialLedgerService.getLedgerHistory(any())).thenReturn(new ArrayList<>());
        when(myOrderService.calculatePriority(any())).thenReturn("MEDIUM");
        when(myOrderService.formatAge(any())).thenReturn("1 giờ");
    }

    // =========================================================================
    // Group 1: getOrderDetailByCode
    // =========================================================================

    @Test
    @DisplayName("UT-UUT06-001: getOrderDetailByCode - orderCode null hoặc rỗng")
    void getOrderDetailByCode_nullOrBlankCode_throwsOrderNotFoundException() {
        User moderator = User.builder().userId(5).build();

        OrderNotFoundException ex1 = assertThrows(OrderNotFoundException.class,
                () -> orderDetailService.getOrderDetailByCode(null, moderator));
        assertThat(ex1.getMessage()).isEqualTo("Mã đơn hàng không hợp lệ");

        OrderNotFoundException ex2 = assertThrows(OrderNotFoundException.class,
                () -> orderDetailService.getOrderDetailByCode("   ", moderator));
        assertThat(ex2.getMessage()).isEqualTo("Mã đơn hàng không hợp lệ");
    }

    @Test
    @DisplayName("UT-UUT06-002: getOrderDetailByCode - Không tìm thấy đơn hàng")
    void getOrderDetailByCode_orderNotFound_throwsOrderNotFoundException() {
        User moderator = User.builder().userId(5).build();
        when(orderRepository.findByOrderCodeWithDetails("BSMS-999")).thenReturn(Optional.empty());
        when(orderRepository.findByOrderCode("BSMS-999")).thenReturn(Optional.empty());

        OrderNotFoundException ex = assertThrows(OrderNotFoundException.class,
                () -> orderDetailService.getOrderDetailByCode("BSMS-999", moderator));

        assertThat(ex.getMessage()).isEqualTo("Không tìm thấy đơn hàng: BSMS-999");
    }

    @Test
    @DisplayName("UT-UUT06-003: getOrderDetailByCode - Tra cứu thành công với mã chuỗi (BSMS-123)")
    void getOrderDetailByCode_success_byStringCode() {
        User moderator = User.builder().userId(5).fullName("Moderator 5").build();
        Order order = Order.builder()
                .orderId(100)
                .orderCode("BSMS-123")
                .orderStatus("PENDING")
                .assignedTo(moderator)
                .customerName("Nguyễn Văn A")
                .customerPhone("0988888888")
                .build();

        when(orderRepository.findByOrderCodeWithDetails("BSMS-123")).thenReturn(Optional.of(order));

        OrderDetailDTO result = orderDetailService.getOrderDetailByCode("BSMS-123", moderator);

        assertNotNull(result);
        assertThat(result.getOrderId()).isEqualTo(100);
        assertThat(result.getOrderCode()).isEqualTo("BSMS-123");
        assertThat(result.getCustomerInfo().getFullName()).isEqualTo("Nguyễn Văn A");
    }

    @Test
    @DisplayName("UT-UUT06-004: getOrderDetailByCode - Tra cứu thành công bằng ID số (fallback findById)")
    void getOrderDetailByCode_success_byNumericIdFallback() {
        User moderator = User.builder().userId(5).build();
        Order order = Order.builder()
                .orderId(100)
                .orderCode("BSMS-100")
                .orderStatus("PENDING")
                .build();

        when(orderRepository.findByOrderCodeWithDetails("100")).thenReturn(Optional.empty());
        when(orderRepository.findByOrderCode("100")).thenReturn(Optional.empty());
        when(orderRepository.findById(100)).thenReturn(Optional.of(order));

        OrderDetailDTO result = orderDetailService.getOrderDetailByCode("100", moderator);

        assertNotNull(result);
        assertThat(result.getOrderId()).isEqualTo(100);
    }

    // =========================================================================
    // Group 2: buildOrderDetailDTO
    // =========================================================================

    @Test
    @DisplayName("UT-UUT06-005: buildOrderDetailDTO - Đơn PENDING chưa phân công (assignedTo == null)")
    void buildOrderDetailDTO_unassignedPendingOrder() {
        User currentModerator = User.builder().userId(5).build();
        Order order = Order.builder()
                .orderId(100)
                .orderCode("BSMS-123")
                .orderStatus("PENDING")
                .assignedTo(null)
                .customerName(null)
                .customerPhone(null)
                .customerEmail(null)
                .shippingAddress(null)
                .build();

        OrderDetailDTO dto = orderDetailService.buildOrderDetailDTO(order, currentModerator);

        assertNotNull(dto);
        assertThat(dto.getCustomerInfo().getFullName()).isEqualTo("Khách hàng");
        assertThat(dto.getCustomerInfo().getPhone()).isEqualTo("-");
        assertThat(dto.getCustomerInfo().getEmail()).isEqualTo("-");
        assertThat(dto.getCustomerInfo().getAddress()).isEqualTo("-");
        assertThat(dto.getAssignedModeratorName()).isEqualTo("Chưa phân bổ");

        assertTrue(dto.isCanClaim());
        assertFalse(dto.isCanApprove());
        assertFalse(dto.isCanReject());
        assertFalse(dto.isCanUnclaim());
        assertFalse(dto.isCanReturnInventory());
    }

    @Test
    @DisplayName("UT-UUT06-006: buildOrderDetailDTO - Đơn PENDING phân công cho currentModerator")
    void buildOrderDetailDTO_assignedPendingOrderToMe() {
        User moderator5 = User.builder().userId(5).fullName("Moderator 5").build();
        Order order = Order.builder()
                .orderId(100)
                .orderCode("BSMS-123")
                .orderStatus("PENDING")
                .assignedTo(moderator5)
                .customerName("Trần Văn B")
                .build();

        OrderDetailDTO dto = orderDetailService.buildOrderDetailDTO(order, moderator5);

        assertNotNull(dto);
        assertThat(dto.getAssignedModeratorName()).isEqualTo("Moderator 5");
        assertTrue(dto.isCanApprove());
        assertTrue(dto.isCanReject());
        assertTrue(dto.isCanUnclaim());
        assertTrue(dto.isCanReturnInventory());
        assertFalse(dto.isCanClaim());
    }

    @Test
    @DisplayName("UT-UUT06-007: buildOrderDetailDTO - Lấy thông tin khách từ Customer entity khi Order fields bị null/blank")
    void buildOrderDetailDTO_customerEntityFallback() {
        User moderator = User.builder().userId(5).build();
        User customerEntity = User.builder()
                .fullName("Lê Văn C")
                .phone("0912345678")
                .email("levanc@gmail.com")
                .address("123 Phố Cổ")
                .build();

        Order order = Order.builder()
                .orderId(100)
                .orderCode("BSMS-123")
                .orderStatus("PENDING")
                .customer(customerEntity)
                .customerName("   ")
                .customerPhone(null)
                .customerEmail("")
                .shippingAddress(null)
                .build();

        OrderDetailDTO dto = orderDetailService.buildOrderDetailDTO(order, moderator);

        assertNotNull(dto);
        assertThat(dto.getCustomerInfo().getFullName()).isEqualTo("Lê Văn C");
        assertThat(dto.getCustomerInfo().getPhone()).isEqualTo("0912345678");
        assertThat(dto.getCustomerInfo().getEmail()).isEqualTo("levanc@gmail.com");
        assertThat(dto.getCustomerInfo().getAddress()).isEqualTo("123 Phố Cổ");
    }

    @Test
    @DisplayName("UT-UUT06-008: buildOrderDetailDTO - Đơn có danh sách orderDetails đầy đủ")
    void buildOrderDetailDTO_withOrderDetails() {
        User moderator = User.builder().userId(5).build();

        Category cat = Category.builder().categoryName("Tùng La Hán").build();
        Variety var = Variety.builder().category(cat).build();
        Product prod1 = Product.builder()
                .productId(10)
                .productCode("TREE-01")
                .productName("Cây Tùng Cổ Thụ")
                .variety(var)
                .build();

        OrderDetail detail1 = OrderDetail.builder()
                .product(prod1)
                .priceAtPurchase(new BigDecimal("2000000"))
                .quantity(1)
                .build();

        Order order = Order.builder()
                .orderId(100)
                .orderCode("BSMS-123")
                .orderStatus("PENDING")
                .totalAmount(new BigDecimal("2000000"))
                .depositAmount(new BigDecimal("500000"))
                .orderDetails(List.of(detail1))
                .build();

        OrderDetailDTO dto = orderDetailService.buildOrderDetailDTO(order, moderator);

        assertNotNull(dto);
        assertThat(dto.getProducts()).hasSize(1);
        assertThat(dto.getProducts().get(0).getProductCode()).isEqualTo("TREE-01");
        assertThat(dto.getProducts().get(0).getProductName()).isEqualTo("Cây Tùng Cổ Thụ");
        assertThat(dto.getProducts().get(0).getCategoryName()).isEqualTo("Tùng La Hán");
        assertThat(dto.getProducts().get(0).getSubtotal()).isEqualByComparingTo("2000000");
        assertThat(dto.getPaymentSummary().getTreePrice()).isEqualByComparingTo("2000000");
    }

    @Test
    @DisplayName("UT-UUT06-009: buildOrderDetailDTO - Fallback treePrice khi orderDetails = null")
    void buildOrderDetailDTO_nullOrderDetails_treePriceFallback() {
        User moderator = User.builder().userId(5).build();
        Order order = Order.builder()
                .orderId(100)
                .orderCode("BSMS-123")
                .orderStatus("PENDING")
                .totalAmount(new BigDecimal("1500000"))
                .craneFee(new BigDecimal("200000"))
                .shippingFee(new BigDecimal("100000"))
                .orderDetails(null)
                .build();

        OrderDetailDTO dto = orderDetailService.buildOrderDetailDTO(order, moderator);

        assertNotNull(dto);
        assertThat(dto.getProducts()).isEmpty();
        assertThat(dto.getPaymentSummary().getTreePrice()).isEqualByComparingTo("1200000"); // 1500000 - 200000 - 100000
    }

    @Test
    @DisplayName("UT-UUT06-010: buildOrderDetailDTO - Xử lý danh sách thanh toán paymentEntities và paymentMethod")
    void buildOrderDetailDTO_withPayments() {
        User moderator = User.builder().userId(5).build();
        Order order = Order.builder()
                .orderId(100)
                .orderCode("BSMS-123")
                .orderStatus("DEPOSITED")
                .totalAmount(new BigDecimal("1000000"))
                .depositAmount(new BigDecimal("300000"))
                .build();

        Payment p1 = Payment.builder()
                .paymentId(1)
                .paymentType("DEPOSIT")
                .paymentMethod("VNPAY")
                .paymentStatus("SUCCESS")
                .amount(new BigDecimal("300000"))
                .paymentDate(LocalDateTime.now())
                .notes("Cọc VNPay")
                .build();

        when(paymentRepository.findByOrderOrderIdOrderByPaymentIdAsc(100)).thenReturn(List.of(p1));

        OrderDetailDTO dto = orderDetailService.buildOrderDetailDTO(order, moderator);

        assertNotNull(dto);
        assertThat(dto.getPaymentMethod()).isEqualTo("DEPOSIT");
        assertThat(dto.getPaymentSummary().getPaidAmount()).isEqualByComparingTo("300000");
        assertThat(dto.getPaymentSummary().getSuccessfulDepositAmount()).isEqualByComparingTo("300000");
        assertThat(dto.getPaymentSummary().getRemainingPaymentAmount()).isEqualByComparingTo("700000");
        assertThat(dto.getPaymentHistory()).hasSize(1);
        assertThat(dto.getPaymentHistory().get(0).getPaymentId()).isEqualTo(1);
    }

    @Test
    @DisplayName("UT-UUT06-011: buildOrderDetailDTO - Đơn DEPOSITED phân công cho currentModerator có cọc thành công")
    void buildOrderDetailDTO_depositedOrderFlags() {
        User moderator5 = User.builder().userId(5).build();
        Order order = Order.builder()
                .orderId(100)
                .orderCode("BSMS-123")
                .orderStatus("DEPOSITED")
                .assignedTo(moderator5)
                .build();

        Payment depositPayment = Payment.builder()
                .paymentId(1)
                .paymentType("DEPOSIT")
                .paymentStatus("SUCCESS")
                .amount(new BigDecimal("500000"))
                .build();

        when(paymentRepository.findByOrderOrderIdOrderByPaymentIdAsc(100)).thenReturn(List.of(depositPayment));
        when(financialLedgerService.calculateRefundableCash(order)).thenReturn(new BigDecimal("500000"));

        OrderDetailDTO dto = orderDetailService.buildOrderDetailDTO(order, moderator5);

        assertNotNull(dto);
        assertTrue(dto.isCanComplete());
        assertTrue(dto.isCanCustomerNoShow());
        assertTrue(dto.isCanRecordFaultRefund());
    }

    @Test
    @DisplayName("UT-UUT06-012: buildOrderDetailDTO - Đơn DEPOSITED nhưng đã bị tịch thu cọc (hasForfeitedDeposit = true)")
    void buildOrderDetailDTO_depositedOrderWithForfeitedDeposit_canCustomerNoShowIsFalse() {
        User moderator5 = User.builder().userId(5).build();
        Order order = Order.builder()
                .orderId(100)
                .orderCode("BSMS-123")
                .orderStatus("DEPOSITED")
                .assignedTo(moderator5)
                .build();

        Payment depositPayment = Payment.builder()
                .paymentId(1)
                .paymentType("DEPOSIT")
                .paymentStatus("SUCCESS")
                .amount(new BigDecimal("500000"))
                .build();

        when(paymentRepository.findByOrderOrderIdOrderByPaymentIdAsc(100)).thenReturn(List.of(depositPayment));
        when(financialLedgerService.sumForfeitedDepositIncome(order)).thenReturn(new BigDecimal("500000"));

        OrderDetailDTO dto = orderDetailService.buildOrderDetailDTO(order, moderator5);

        assertNotNull(dto);
        assertFalse(dto.isCanCustomerNoShow());
    }

    @Test
    @DisplayName("UT-UUT06-013: buildOrderDetailDTO - Đơn PAID phân công cho currentModerator")
    void buildOrderDetailDTO_paidOrderFlags() {
        User moderator5 = User.builder().userId(5).build();
        Order order = Order.builder()
                .orderId(100)
                .orderCode("BSMS-123")
                .orderStatus("PAID")
                .assignedTo(moderator5)
                .build();

        when(financialLedgerService.calculateRefundableCash(order)).thenReturn(new BigDecimal("1000000"));

        OrderDetailDTO dto = orderDetailService.buildOrderDetailDTO(order, moderator5);

        assertNotNull(dto);
        assertTrue(dto.isCanComplete());
        assertTrue(dto.isCanRecordFaultRefund());
        assertFalse(dto.isCanCustomerNoShow());
    }

    @Test
    @DisplayName("UT-UUT06-014: buildOrderDetailDTO - Xử lý timeline đơn hàng CANCELLED")
    void buildOrderDetailDTO_timelineCancelled() {
        User moderator = User.builder().userId(5).build();
        Order order = Order.builder()
                .orderId(100)
                .orderCode("BSMS-123")
                .orderStatus("CANCELLED")
                .orderDate(LocalDateTime.now().minusDays(2))
                .build();

        OrderLog logCancel = OrderLog.builder()
                .order(order)
                .toStatus("CANCELLED")
                .actionAt(LocalDateTime.now().minusDays(1))
                .build();

        when(orderLogRepository.findByOrderOrderIdOrderByActionAtAsc(100)).thenReturn(List.of(logCancel));

        OrderDetailDTO dto = orderDetailService.buildOrderDetailDTO(order, moderator);

        assertNotNull(dto);
        assertThat(dto.getTimeline()).isNotEmpty();
        assertThat(dto.getTimeline().get(dto.getTimeline().size() - 1).getStage()).isEqualTo("CANCELLED");
        assertTrue(dto.getTimeline().get(dto.getTimeline().size() - 1).isCompleted());
    }

    @Test
    @DisplayName("UT-UUT06-015: buildOrderDetailDTO - Xử lý handlingHistory")
    void buildOrderDetailDTO_handlingHistory() {
        User moderator5 = User.builder().userId(5).fullName("Moderator 5").build();
        Order order = Order.builder()
                .orderId(100)
                .orderCode("BSMS-123")
                .orderStatus("PENDING")
                .build();

        OrderHandling handling = OrderHandling.builder()
                .orderHandlingId(1)
                .order(order)
                .moderator(moderator5)
                .isActive(true)
                .handledAt(LocalDateTime.now())
                .build();

        when(orderHandlingRepository.findByOrderOrderIdOrderByHandledAtDesc(100)).thenReturn(List.of(handling));

        OrderDetailDTO dto = orderDetailService.buildOrderDetailDTO(order, moderator5);

        assertNotNull(dto);
        assertThat(dto.getHandlingHistory()).hasSize(1);
        assertThat(dto.getHandlingHistory().get(0).getHandlingId()).isEqualTo(1);
        assertThat(dto.getHandlingHistory().get(0).getModeratorName()).isEqualTo("Moderator 5");
    }

    @Test
    @DisplayName("UT-UUT06-016: Dependency Failure - orderLogRepository ném DataAccessException")
    void buildOrderDetailDTO_dependencyFailure_throwsException() {
        User moderator = User.builder().userId(5).build();
        Order order = Order.builder().orderId(100).orderCode("BSMS-123").orderStatus("PENDING").build();

        when(orderLogRepository.findByOrderOrderIdOrderByActionAtAsc(100))
                .thenThrow(new DataAccessException("DB Log Read Error") {});

        assertThrows(DataAccessException.class,
                () -> orderDetailService.buildOrderDetailDTO(order, moderator));
    }
}
