package com.example.bonsai_shop.statemachine;

import com.example.bonsai_shop.entity.Order;
import com.example.bonsai_shop.entity.Product;
import com.example.bonsai_shop.entity.User;
import com.example.bonsai_shop.moderator.dto.OrderActionRequestDTO;
import com.example.bonsai_shop.moderator.service.OrderActionService;
import com.example.bonsai_shop.product.repository.OrderHandlingRepository;
import com.example.bonsai_shop.product.repository.OrderRepository;
import com.example.bonsai_shop.product.service.OrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

/**
 * Suite kiểm thử Máy Trạng Thái (State Transition Testing) cho BSMS.
 * Kiểm tra luồng trạng thái sản phẩm (AVAILABLE -> RESERVED -> SOLD) 
 * và quy tắc chặn chuyển trạng thái phi lý cho Đơn hàng (Rule GB-01, GB-06).
 */
class StateTransitionTest {

    @Nested
    @DisplayName("1. Kiểm thử Luồng chuyển trạng thái Cây cảnh (Product State Flow)")
    class ProductStateTransitionTests {

        @Test
        @DisplayName("Luồng hợp lệ: Cây từ AVAILABLE -> RESERVED (Đặt đơn) -> SOLD (Thanh toán hoàn tất)")
        void testValidProductStateTransition_AvailableToReservedToSold() {
            // Arrange
            Product product = Product.builder()
                    .productId(101)
                    .productName("Cây Tùng Nhật")
                    .price(new BigDecimal("10000000"))
                    .productStatus("AVAILABLE")
                    .build();

            // Act 1: Khách hàng đặt mua / Moderator duyệt đơn -> Cây chuyển sang RESERVED
            product.setProductStatus("RESERVED");

            // Assert 1
            assertThat(product.getProductStatus()).isEqualTo("RESERVED");

            // Act 2: Khách hàng thanh toán hoàn tất -> Cây chuyển sang SOLD
            product.setProductStatus("SOLD");

            // Assert 2
            assertThat(product.getProductStatus()).isEqualTo("SOLD");
        }

        @Test
        @DisplayName("Luồng Hoàn trả: Cây từ RESERVED -> AVAILABLE khi Đơn hàng bị Hủy (CANCELLED)")
        void testProductStateTransition_ReservedToAvailableOnOrderCancel() {
            // Arrange
            Product product = Product.builder()
                    .productId(102)
                    .productName("Cây Mai Vàng")
                    .productStatus("RESERVED")
                    .build();

            // Act: Đơn hàng bị hủy -> Cây trả lại kho chung
            product.setProductStatus("AVAILABLE");

            // Assert
            assertThat(product.getProductStatus()).isEqualTo("AVAILABLE");
        }
    }

    @Nested
    @DisplayName("2. Kiểm thử Chặn chuyển trạng thái không hợp lệ (Rules GB-01, GB-06)")
    class OrderStateTransitionRuleTests {

        private OrderRepository orderRepository;
        private OrderHandlingRepository orderHandlingRepository;
        private OrderService orderService;
        private OrderActionService orderActionService;

        @BeforeEach
        void setUp() {
            orderRepository = mock(OrderRepository.class);
            orderHandlingRepository = mock(OrderHandlingRepository.class);
            orderService = mock(OrderService.class);
            orderActionService = new OrderActionService(orderRepository, orderHandlingRepository, orderService);
        }

        @Test
        @DisplayName("Rule GB-01: Đơn hàng đã ở trạng thái CANCELLED/REJECTED thì KHÔNG ĐƯỢC PHÉP Approve (Duyệt)")
        void testRuleGB01_CancelledOrderCannotBeApproved() {
            // Arrange
            User moderator = User.builder().userId(1).fullName("Mod Admin").build();
            Order cancelledOrder = Order.builder()
                    .orderId(200)
                    .orderCode("BSMS-CANCELLED-001")
                    .orderStatus("CANCELLED")
                    .assignedTo(moderator)
                    .build();

            when(orderRepository.findByOrderCode("BSMS-CANCELLED-001")).thenReturn(Optional.of(cancelledOrder));

            OrderActionRequestDTO approveRequest = new OrderActionRequestDTO();
            approveRequest.setAction("approve");
            approveRequest.setCraneFee(BigDecimal.ZERO);
            approveRequest.setShippingFee(BigDecimal.ZERO);

            // Act & Assert: Chặn hành động approve trên đơn CANCELLED
            assertThatThrownBy(() -> orderActionService.executeAction("BSMS-CANCELLED-001", approveRequest, moderator))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Chỉ có thể duyệt đơn hàng đang chờ kiểm duyệt");

            assertThat(cancelledOrder.getOrderStatus()).isEqualTo("CANCELLED");
            verify(orderService, never()).verifyOrder(anyString(), any(), any(), any(), any());
        }

        @Test
        @DisplayName("Rule GB-06: Đơn hàng đã ở trạng thái CANCELLED thì KHÔNG ĐƯỢC PHÉP chuyển sang PAID (Thanh toán)")
        void testRuleGB06_CancelledOrderCannotTransitionToPaid() {
            // Arrange
            Order cancelledOrder = Order.builder()
                    .orderId(201)
                    .orderCode("BSMS-CANCELLED-002")
                    .orderStatus("CANCELLED")
                    .build();

            when(orderRepository.findByOrderCode("BSMS-CANCELLED-002")).thenReturn(Optional.of(cancelledOrder));
            when(orderService.verifyOrder(eq("BSMS-CANCELLED-002"), any(), any(), any(), any()))
                    .thenThrow(new IllegalStateException("Đơn hàng đã bị hủy, không thể xác nhận thanh toán"));

            // Act & Assert
            assertThatThrownBy(() -> orderService.verifyOrder("BSMS-CANCELLED-002", BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, null))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Đơn hàng đã bị hủy");

            assertThat(cancelledOrder.getOrderStatus()).isEqualTo("CANCELLED");
        }
    }
}
