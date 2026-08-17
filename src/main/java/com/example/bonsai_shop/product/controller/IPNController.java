package com.example.bonsai_shop.product.controller;

import com.example.bonsai_shop.config.VNPayConfig;
import com.example.bonsai_shop.entity.Order;
import com.example.bonsai_shop.entity.Payment;
import com.example.bonsai_shop.product.repository.OrderRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * [VAI TRÒ TRONG LUỒNG THANH TOÁN VNPAY - IPN WEBHOOK]
 *
 * Chịu trách nhiệm:
 * - Tiếp nhận thông báo kết quả giao dịch thanh toán bất đồng bộ ngầm (Instant
 * Payment Notification - IPN) Server-to-Server từ máy chủ VNPay.
 * - Đảm bảo tính toàn vẹn và chống gian lận dữ liệu qua kiểm tra chữ ký số
 * HMAC-SHA512.
 * - Kiểm tra 3 lớp an toàn dữ liệu:
 * 1. Check Order ID: Đơn hàng có tồn tại trong hệ thống không? (RspCode "01")
 * 2. Check Amount: Số tiền VNPay báo về có khớp chính xác với số tiền cần thanh
 * toán trong DB không? (RspCode "04")
 * 3. Check Order Status: Đơn hàng đã được xác nhận thanh toán trước đó chưa?
 * Tránh xử lý trùng lặp (RspCode "02").
 * - Cập nhật trạng thái Payment (SUCCESS/FAILED) và Order (DEPOSITED/PAID), bắn
 * sự kiện email nếu thành công.
 * - Trả về mã phản hồi chuẩn VNPay quy định (RspCode "00" - Confirm Success,
 * "97" - Invalid Checksum, ...).
 *
 * Các thao tác trên web đi qua class này:
 * - [VNPay Server tự động gọi ngầm khi có giao dịch] → GET /vnpay/ipn →
 * receiveIPN()
 *
 * Các thành phần phối hợp chính:
 * - VNPayConfig, OrderService, OrderRepository, PaymentRepository.
 */
@RestController
public class IPNController {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private com.example.bonsai_shop.product.service.OrderService orderService;

    @Autowired
    private com.example.bonsai_shop.product.repository.PaymentRepository paymentRepository;

    /**
     * [TIẾP NHẬN VÀ XÁC THỰC WEBHOOK IPN TỪ SERVER VNPAY]
     *
     * Khi nào được gọi:
     * - Máy chủ VNPay tự động gửi HTTP GET request ngầm (Server-to-Server) đến
     * endpoint này ngay khi giao dịch thanh toán hoàn tất (bất kể khách hàng có
     * đóng trình duyệt trước khi về Return URL hay không).
     *
     * API:
     * - HTTP: GET
     * - URL: /vnpay/ipn
     * - Người gọi: Máy chủ VNPay Gateway
     *
     * Dữ liệu nhận vào:
     * - Request params:
     * + vnp_TxnRef (String): Mã đơn hàng (orderCode).
     * + vnp_Amount (String): Số tiền giao dịch (*100).
     * + vnp_ResponseCode (String): Mã phản hồi kết quả ("00" thành công).
     * + vnp_TransactionStatus (String): Trạng thái giao dịch tại VNPay ("00" thành
     * công).
     * + vnp_SecureHash (String): Chữ ký kiểm tra bảo mật từ VNPay.
     *
     * Điều phối xử lý:
     * 1. Thu thập danh sách params và tính toán chữ ký HMAC-SHA512 với
     * VNPayConfig.vnp_HashSecret.
     * 2. Kiểm tra tính hợp lệ của chữ ký (signValue == vnp_SecureHash):
     * - Nếu sai chữ ký: Trả về RspCode "97" (Invalid Checksum).
     * 3. Tìm đơn hàng trong DB theo vnp_TxnRef (OrderRepository.findByOrderCode):
     * - Nếu không thấy: Trả về RspCode "01" (Order not Found).
     * 4. Tìm Payment PENDING mới nhất của đơn để lấy expectedAmount:
     * - So sánh vnpAmount == expectedAmount * 100:
     * + Nếu không khớp số tiền: Trả về RspCode "04" (Invalid Amount).
     * 5. Kiểm tra trạng thái đơn:
     * - Nếu đơn đã PAID hoặc đã DEPOSITED (với cọc): Trả về RspCode "02" (Order
     * already confirmed).
     * 6. Nếu kiểm tra đều hợp lệ:
     * - Nếu ResponseCode == "00" và TransactionStatus == "00": Gọi
     * OrderService.processPaymentSuccess(orderCode).
     * - Ngược lại: Gọi OrderService.processPaymentFailure(orderCode, responseCode,
     * transactionStatus, "IPN").
     * - Trả về RspCode "00" (Confirm Success).
     *
     * Dữ liệu trả về:
     * - HTTP status: 200 OK
     * - Response JSON: Map<String, String> {"RspCode": "00", "Message": "Confirm
     * Success"}
     *
     * Tác động dữ liệu:
     * - Bảng/Entity bị đọc: ORDER, PAYMENT
     * - Bảng/Entity bị ghi/cập nhật:
     * + PAYMENT: paymentStatus = "SUCCESS" / "FAILED", paymentDate = now
     * + ORDER: orderStatus: PENDING_PAYMENT → DEPOSITED hoặc PAID
     * + PRODUCT: productStatus (nếu thanh toán đủ 100%): RESERVED → SOLD
     */
    @GetMapping("/vnpay/ipn")
    @Transactional
    public Map<String, String> receiveIPN(HttpServletRequest request) {
        Map<String, String> response = new HashMap<>();
        Map<String, String> fields = new HashMap<>();

        for (Enumeration<String> params = request.getParameterNames(); params.hasMoreElements();) {
            String fieldName = params.nextElement();
            String fieldValue = request.getParameter(fieldName);
            if ((fieldValue != null) && (fieldValue.length() > 0)) {
                fields.put(fieldName, fieldValue);
            }
        }

        String vnp_SecureHash = request.getParameter("vnp_SecureHash");
        fields.remove("vnp_SecureHashType");
        fields.remove("vnp_SecureHash");

        List<String> fieldNames = new ArrayList<>(fields.keySet());
        Collections.sort(fieldNames);
        StringBuilder sb = new StringBuilder();
        Iterator<String> itr = fieldNames.iterator();
        while (itr.hasNext()) {
            String fieldName = itr.next();
            String fieldValue = fields.get(fieldName);
            if ((fieldValue != null) && (fieldValue.length() > 0)) {
                try {
                    sb.append(fieldName).append('=').append(
                            URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII.toString()));
                    if (itr.hasNext())
                        sb.append('&');
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
            }
        }

        String signValue = VNPayConfig.hmacSHA512(VNPayConfig.vnp_HashSecret, sb.toString());

        if (signValue.equals(vnp_SecureHash)) {
            String orderCode = request.getParameter("vnp_TxnRef");
            long vnpAmount = Long.parseLong(request.getParameter("vnp_Amount"));

            Order order = orderRepository.findByOrderCode(orderCode).orElse(null);
            boolean checkOrderId = order != null;
            boolean checkAmount = false;
            boolean checkOrderStatus = false;

            if (checkOrderId) {
                Payment pendingPayment = paymentRepository
                        .findTopByOrderOrderIdAndPaymentStatusOrderByPaymentIdDesc(order.getOrderId(), "PENDING")
                        .orElse(null);

                Payment callbackPayment = pendingPayment != null ? pendingPayment
                        : paymentRepository
                                .findByOrderOrderIdOrderByPaymentIdAsc(order.getOrderId())
                                .stream()
                                .filter(payment -> "FAILED".equalsIgnoreCase(payment.getPaymentStatus()))
                                .reduce((first, second) -> second)
                                .orElse(null);

                long expectedAmount = (callbackPayment != null && callbackPayment.getAmount() != null)
                        ? callbackPayment.getAmount().longValue() * 100
                        : order.getTotalAmount().longValue() * 100;

                checkAmount = vnpAmount == expectedAmount;

                checkOrderStatus = !"PAID".equalsIgnoreCase(order.getOrderStatus());
                if (pendingPayment != null && "DEPOSIT".equalsIgnoreCase(pendingPayment.getPaymentType())) {
                    checkOrderStatus = !"DEPOSITED".equalsIgnoreCase(order.getOrderStatus())
                            && !"PAID".equalsIgnoreCase(order.getOrderStatus());
                }
            }

            if (checkOrderId) {
                if (checkAmount) {
                    if (checkOrderStatus) {
                        String responseCode = request.getParameter("vnp_ResponseCode");
                        String transactionStatus = request.getParameter("vnp_TransactionStatus");
                        if (isSuccessfulVnPayResult(responseCode, transactionStatus)) {
                            orderService.processPaymentSuccess(orderCode);
                        } else {
                            orderService.processPaymentFailure(orderCode, responseCode, transactionStatus, "IPN");
                        }

                        response.put("RspCode", "00");
                        response.put("Message", "Confirm Success");
                    } else {
                        response.put("RspCode", "02");
                        response.put("Message", "Order already confirmed");
                    }
                } else {
                    response.put("RspCode", "04");
                    response.put("Message", "Invalid Amount");
                }
            } else {
                response.put("RspCode", "01");
                response.put("Message", "Order not Found");
            }
        } else {
            response.put("RspCode", "97");
            response.put("Message", "Invalid Checksum");
        }

        return response;
    }

    private boolean isSuccessfulVnPayResult(String responseCode, String transactionStatus) {
        return "00".equals(responseCode) && "00".equals(transactionStatus);
    }
}
