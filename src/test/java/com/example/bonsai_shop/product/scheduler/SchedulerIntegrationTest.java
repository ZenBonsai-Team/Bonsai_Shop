package com.example.bonsai_shop.product.scheduler;

import com.example.bonsai_shop.customer.repository.ModerationNotificationRepository;
import com.example.bonsai_shop.entity.*;
import com.example.bonsai_shop.product.repository.OrderRepository;
import com.example.bonsai_shop.product.repository.ReviewRepository;
import com.example.bonsai_shop.product.service.OrderExpirationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SchedulerIntegrationTest {

    @Mock
    private OrderExpirationService orderExpirationService;

    @InjectMocks
    private OrderExpirationScheduler orderExpirationScheduler;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private ReviewRepository reviewRepository;

    @Mock
    private ModerationNotificationRepository notificationRepository;

    @InjectMocks
    private ReviewReminderScheduler reviewReminderScheduler;

    // ==================== ORDER EXPIRATION SCHEDULER TESTS ====================

    @Test
    void scheduleOrderCleanup_Success() {
        doNothing().when(orderExpirationService).cancelExpiredOrders();

        orderExpirationScheduler.scheduleOrderCleanup();

        verify(orderExpirationService, times(1)).cancelExpiredOrders();
    }

    @Test
    void scheduleOrderCleanup_WhenExceptionThrown_ShouldCatchAndLog() {
        doThrow(new RuntimeException("DB down")).when(orderExpirationService).cancelExpiredOrders();

        // Should not crash the application
        orderExpirationScheduler.scheduleOrderCleanup();

        verify(orderExpirationService, times(1)).cancelExpiredOrders();
    }

    @Test
    void scheduleInPersonOrderCleanup_Success() {
        doNothing().when(orderExpirationService).cancelExpiredInPersonOrders();

        orderExpirationScheduler.scheduleInPersonOrderCleanup();

        verify(orderExpirationService, times(1)).cancelExpiredInPersonOrders();
    }

    @Test
    void scheduleInPersonOrderCleanup_WhenExceptionThrown_ShouldCatchAndLog() {
        doThrow(new RuntimeException("DB down")).when(orderExpirationService).cancelExpiredInPersonOrders();

        // Should not crash the application
        orderExpirationScheduler.scheduleInPersonOrderCleanup();

        verify(orderExpirationService, times(1)).cancelExpiredInPersonOrders();
    }

    // ==================== REVIEW REMINDER SCHEDULER TESTS ====================

    @Test
    void sendReviewReminders_WhenNoCompletedOrders_ShouldDoNothing() {
        when(orderRepository.findCompletedOrdersBefore(any(LocalDateTime.class))).thenReturn(new ArrayList<>());

        reviewReminderScheduler.sendReviewReminders();

        verify(notificationRepository, never()).save(any(ModerationNotification.class));
    }

    @Test
    void sendReviewReminders_WhenCustomerOrEmailIsNull_ShouldSkip() {
        Order order1 = new Order();
        order1.setCustomer(null);

        Order order2 = new Order();
        User customerNoEmail = new User();
        customerNoEmail.setEmail(null);
        order2.setCustomer(customerNoEmail);

        when(orderRepository.findCompletedOrdersBefore(any(LocalDateTime.class))).thenReturn(List.of(order1, order2));

        reviewReminderScheduler.sendReviewReminders();

        verify(notificationRepository, never()).save(any(ModerationNotification.class));
    }

    @Test
    void sendReviewReminders_WhenAlreadySent_ShouldSkip() {
        User customer = User.builder().userId(1).email("customer@test.com").build();
        Order order = Order.builder().orderCode("ORD-123").customer(customer).build();

        when(orderRepository.findCompletedOrdersBefore(any(LocalDateTime.class))).thenReturn(List.of(order));
        when(notificationRepository.existsByTargetUsernameAndMessageContaining(eq("customer@test.com"), anyString())).thenReturn(true);

        reviewReminderScheduler.sendReviewReminders();

        verify(notificationRepository, never()).save(any(ModerationNotification.class));
    }

    @Test
    void sendReviewReminders_WhenAllProductsReviewed_ShouldNotSendReminder() {
        User customer = User.builder().userId(1).email("customer@test.com").build();
        Product product = Product.builder().productId(10).build();
        OrderDetail detail = OrderDetail.builder().product(product).build();
        Order order = Order.builder().orderCode("ORD-123").customer(customer).orderDetails(List.of(detail)).build();

        when(orderRepository.findCompletedOrdersBefore(any(LocalDateTime.class))).thenReturn(List.of(order));
        when(notificationRepository.existsByTargetUsernameAndMessageContaining(eq("customer@test.com"), anyString())).thenReturn(false);
        when(reviewRepository.existsByCustomerUserIdAndProductProductId(1, 10)).thenReturn(true);

        reviewReminderScheduler.sendReviewReminders();

        verify(notificationRepository, never()).save(any(ModerationNotification.class));
    }

    @Test
    void sendReviewReminders_WhenProductNotReviewed_ShouldSendReminder() {
        User customer = User.builder().userId(1).email("customer@test.com").build();
        Product product = Product.builder().productId(10).build();
        OrderDetail detail = OrderDetail.builder().product(product).build();
        Order order = Order.builder().orderCode("ORD-123").customer(customer).orderDetails(List.of(detail)).build();

        when(orderRepository.findCompletedOrdersBefore(any(LocalDateTime.class))).thenReturn(List.of(order));
        when(notificationRepository.existsByTargetUsernameAndMessageContaining(eq("customer@test.com"), anyString())).thenReturn(false);
        when(reviewRepository.existsByCustomerUserIdAndProductProductId(1, 10)).thenReturn(false);

        reviewReminderScheduler.sendReviewReminders();

        verify(notificationRepository, times(1)).save(any(ModerationNotification.class));
    }

    @Test
    void sendReviewReminders_WhenExceptionThrown_ShouldCatchAndLog() {
        when(orderRepository.findCompletedOrdersBefore(any(LocalDateTime.class))).thenThrow(new RuntimeException("Connection timeout"));

        // Should catch and not crash
        reviewReminderScheduler.sendReviewReminders();

        verify(notificationRepository, never()).save(any(ModerationNotification.class));
    }
}
