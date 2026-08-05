package com.example.bonsai_shop.product.scheduler;

import com.example.bonsai_shop.customer.repository.ModerationNotificationRepository;
import com.example.bonsai_shop.entity.ModerationNotification;
import com.example.bonsai_shop.entity.Order;
import com.example.bonsai_shop.entity.OrderDetail;
import com.example.bonsai_shop.product.repository.OrderRepository;
import com.example.bonsai_shop.product.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class ReviewReminderScheduler {

    private final OrderRepository orderRepository;
    private final ReviewRepository reviewRepository;
    private final ModerationNotificationRepository notificationRepository;

    // Run every day at 10:00 AM
    @Scheduled(cron = "0 0 10 * * ?")
    public void sendReviewReminders() {
        log.info("Starting scheduled review reminder task...");
        try {
            LocalDateTime sevenDaysAgo = LocalDateTime.now().minusDays(7);
            List<Order> oldCompletedOrders = orderRepository.findCompletedOrdersBefore(sevenDaysAgo);

            for (Order order : oldCompletedOrders) {
                if (order.getCustomer() == null || order.getCustomer().getEmail() == null) {
                    continue;
                }

                String username = order.getCustomer().getEmail();
                String reminderKey = "nhắc nhở đánh giá (7 ngày) cho đơn hàng " + order.getOrderCode();
                boolean alreadySent = notificationRepository.existsByTargetUsernameAndMessageContaining(username, reminderKey);

                if (alreadySent) {
                    continue;
                }

                boolean hasUnreviewed = false;
                if (order.getOrderDetails() != null) {
                    for (OrderDetail detail : order.getOrderDetails()) {
                        if (detail.getProduct() != null) {
                            boolean reviewed = reviewRepository.existsByCustomerUserIdAndProductProductId(
                                    order.getCustomer().getUserId(),
                                    detail.getProduct().getProductId()
                            );
                            if (!reviewed) {
                                hasUnreviewed = true;
                                break;
                            }
                        }
                    }
                }

                if (hasUnreviewed) {
                    ModerationNotification notification = ModerationNotification.builder()
                            .targetUsername(username)
                            .message("🔔 [Nhắc nhở] Đã 7 ngày kể từ khi bạn nhận đơn hàng " + order.getOrderCode() + ". Đừng quên chia sẻ cảm nhận và cho sao các cây bonsai bạn đã mua nhé! (" + reminderKey + ")")
                            .isRead(false)
                            .createdAt(LocalDateTime.now())
                            .build();
                    notificationRepository.save(notification);
                    log.info("Sent 7-day review reminder for order: {} to {}", order.getOrderCode(), username);
                }
            }
        } catch (Exception e) {
            log.error("Error running review reminder task: ", e);
        }
    }
}
