package com.example.bonsai_shop.product.repository;

import com.example.bonsai_shop.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository để truy cập bảng PAYMENT.
 *
 * Sau refactor (V3 migration), quan hệ Order-Payment là 1-N:
 *   1 Order có thể có nhiều Payment records.
 *
 * Naming convention Spring Data JPA:
 *   findBy + [FieldPath] + [Condition]
 *   FieldPath: order.orderId → Order entity field "order", nested field "orderId"
 */
@Repository
public interface PaymentRepository extends JpaRepository<Payment, Integer> {

    /**
     * Lấy tất cả Payment của một Order, sắp xếp theo ID tăng dần (thứ tự tạo).
     *
     * Dùng để: hiển thị lịch sử payment trong Order Detail.
     *
     * @param orderId ID của Order cần lấy Payment
     * @return List<Payment> sắp xếp theo PaymentID tăng dần
     */
    List<Payment> findByOrderOrderIdOrderByPaymentIdAsc(Integer orderId);

    /**
     * Lấy Payment PENDING mới nhất của một Order.
     *
     * Dùng để:
     *   1. PaymentController.payOrder() → lấy amount đúng để gửi VNPay
     *   2. IPNController.receiveIPN() → validate amount từ VNPay callback
     *   3. processPaymentSuccess() → xác định đây là DEPOSIT hay FULL_PAYMENT
     *
     * Tại sao "mới nhất"? Phòng trường hợp Payment cũ bị lỗi và tạo Payment mới thay thế.
     *
     * @param orderId ID của Order
     * @param paymentStatus Trạng thái cần tìm (thường là "PENDING")
     * @return Payment mới nhất với status đó, hoặc empty nếu không có
     */
    Optional<Payment> findTopByOrderOrderIdAndPaymentStatusOrderByPaymentIdDesc(
            Integer orderId, String paymentStatus);

    /**
     * Lấy tất cả Payment theo loại giao dịch của một Order.
     *
     * Dùng để: tính remainingAmount = TotalAmount - sum(DEPOSIT payments đã SUCCESS)
     *
     * @param orderId ID của Order
     * @param paymentType Loại payment: "DEPOSIT", "FULL_PAYMENT", "REMAINING_PAYMENT"
     * @return List<Payment> có paymentType tương ứng
     */
    List<Payment> findByOrderOrderIdAndPaymentType(Integer orderId, String paymentType);

    /**
     * Lấy Payment theo loại VÀ trạng thái của một Order.
     *
     * Dùng để: kiểm tra xem DEPOSIT đã SUCCESS chưa trước khi cho phép REMAINING_PAYMENT.
     *
     * @param orderId ID của Order
     * @param paymentType Loại payment (ví dụ: "DEPOSIT")
     * @param paymentStatus Trạng thái (ví dụ: "SUCCESS")
     * @return Payment nếu tìm thấy, empty nếu không
     */
    Optional<Payment> findByOrderOrderIdAndPaymentTypeAndPaymentStatus(
            Integer orderId, String paymentType, String paymentStatus);
}
