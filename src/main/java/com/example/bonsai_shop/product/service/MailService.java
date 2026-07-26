package com.example.bonsai_shop.product.service;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.internet.MimeMessage;

import com.example.bonsai_shop.entity.Order;
import com.example.bonsai_shop.entity.OrderDetail;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class MailService {
    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    private final Map<String, Instant> suppressionCache = new ConcurrentHashMap<>();
    private static final long SUPPRESSION_TIME_SECONDS = 60;
    private static final int MAX_RETRIES = 3;

    public void sendOrderApprovedEmail(Order order) {
        String orderCode = order.getOrderCode();
        String toEmail = order.getCustomerEmail();

        if (toEmail == null || toEmail.trim().isEmpty()) {
            log.warn("Không tìm thấy email của khách hàng", orderCode);
            return;
        }

        Instant now = Instant.now();
        if (suppressionCache.containsKey(orderCode)) {
            Instant lastSent = suppressionCache.get(orderCode);
            long secondsSinceLastSent = now.getEpochSecond() - lastSent.getEpochSecond();
            if (secondsSinceLastSent < SUPPRESSION_TIME_SECONDS) {
                log.info("Suppression Rule: Đơn hàng {} vừa được gửi email {} giây trước. Bỏ qua", orderCode,
                        secondsSinceLastSent);
                return;
            }
        }
        suppressionCache.put(orderCode, now);

        String paymentLink = "http://localhost:8080/vnpay/pay-order?orderCode=" + orderCode;
        String emailContent = buildAprovedTemplate(order, paymentLink);

        sendHtmlEmailWithRetry(toEmail, "Xác nhân đơn hàng #" + orderCode + " - Bonsai Shop", emailContent, orderCode);
    }

    public void sendOrderDepositedEmail(Order order) {
        String orderCode = order.getOrderCode();
        String toEmail = order.getCustomerEmail();

        if (toEmail == null || toEmail.trim().isEmpty()) {
            log.warn("Không tìm thấy email của khách hàng", orderCode);
            return;
        }
        String emailContent = buildDepositedTemplate(order);
        sendHtmlEmailWithRetry(toEmail, "Xác nhận đặt cọc thành công #" + orderCode + " - Bonsai Shop", emailContent, orderCode);
    }

    public void sendOrderFinalReceiptEmail(Order order) {
        String orderCode = order.getOrderCode();
        String toEmail = order.getCustomerEmail();

        if (toEmail == null || toEmail.trim().isEmpty()) {
            log.warn("Không tìm thấy email của khách hàng", orderCode);
            return;
        }
        String emailContent = buildFinalReceiptTemplate(order);
        sendHtmlEmailWithRetry(toEmail, "Hóa đơn hoàn tất thanh toán #" + orderCode + " - Bonsai Shop", emailContent, orderCode);
    }

    public void sendOrderRejectedEmail(Order order, String reason) {
        String orderCode = order.getOrderCode();
        String toEmail = order.getCustomerEmail();

        if (toEmail == null || toEmail.trim().isEmpty()) {
            log.warn("Không tìm thấy email của khách hàng", orderCode);
            return;
        }
        String emailContent = buildRejectedTemplate(order, reason);
        sendHtmlEmailWithRetry(toEmail, "Thông báo hủy đơn hàng #" + orderCode + " - Bonsai Shop", emailContent,
                orderCode);
    }

    private String buildRejectedTemplate(Order order, String reason) {
        String customerName = order.getCustomerName() != null ? order.getCustomerName() : "Quý khách hàng";
        String productName = "";
        if (order.getOrderDetails() != null && !order.getOrderDetails().isEmpty()) {
            OrderDetail detail = order.getOrderDetails().get(0);
            if (detail.getProduct() != null) {
                productName = detail.getProduct().getProductName();
            }
        }

        return "<div style=\"font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; max-width: 600px; margin: 0 auto; padding: 25px; border: 1px solid #e2e8f0; border-radius: 12px; background-color: #ffffff;\">"
                +
                "  <div style=\"text-align: center; background: linear-gradient(135deg, #c53030, #e53e3e); color: white; padding: 20px; border-radius: 8px 8px 0 0;\">"
                +
                "    <h2 style=\"margin: 0;\">Thông Báo Từ Chối Đơn Hàng</h2>" +
                "    <p style=\"margin: 5px 0 0 0;\">Mã đơn hàng: #" + order.getOrderCode() + "</p>" +
                "  </div>" +
                "  <div style=\"padding: 20px 0; color: #1a202c; line-height: 1.6;\">" +
                "    <p>Xin chào <strong>" + customerName + "</strong>,</p>" +
                "    <p>Rất tiếc, đơn đặt hàng sản phẩm <strong>" + productName + "</strong> (Mã đơn hàng: <strong>#"
                + order.getOrderCode() + "</strong>) của bạn đã bị từ chối duyệt.</p>" +
                "    <div style=\"background-color: #fff5f5; border-left: 4px solid #e53e3e; padding: 15px; border-radius: 4px; margin: 20px 0;\">"
                +
                "      <h4 style=\"margin: 0 0 5px 0; color: #9b2c2c;\">Lý do từ chối:</h4>" +
                "      <p style=\"margin: 0; color: #c53030;\">" + reason + "</p>" +
                "    </div>" +
                "  <div style=\"text-align: center; font-size: 12px; color: #a0aec0; border-top: 1px solid #edf2f7; padding-top: 15px;\">"
                +
                "    © " + java.time.LocalDate.now().getYear() + " Bonsai Shop. All rights reserved." +
                "  </div>" +
                "</div>";
    }

    private String buildAprovedTemplate(Order order, String paymentLink) {
        String customerName = order.getCustomerName() != null ? order.getCustomerName() : "Quý khách hàng";
        String productName = "";
        if (order.getOrderDetails() != null && !order.getOrderDetails().isEmpty()) {
            OrderDetail detail = order.getOrderDetails().get(0);
            if (detail.getProduct() != null) {
                productName = detail.getProduct().getProductName();
            }
        }

        return "<div style=\"font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; max-width: 600px; margin: 0 auto; padding: 25px; border: 1px solid #e2e8f0; border-radius: 12px; background-color: #ffffff;\">"
                +
                "  <div style=\"text-align: center; background: linear-gradient(135deg, #2e7d32, #4caf50); color: white; padding: 20px; border-radius: 8px 8px 0 0;\">"
                +
                "    <h2 style=\"margin: 0;\">Đơn Hàng Đã Được Phê Duyệt!</h2>" +
                "    <p style=\"margin: 5px 0 0 0;\">Mã đơn hàng: #" + order.getOrderCode() + "</p>" +
                "  </div>" +
                "  <div style=\"padding: 20px 0; color: #1a202c; line-height: 1.6;\">" +
                "    <p>Xin chào <strong>" + customerName + "</strong>,</p>" +
                "    <p>Đơn hàng đặt mua cây cảnh của bạn đã được kiểm duyệt thành công. Dưới đây là thông tin chi phí chi tiết:</p>"
                +
                "    <table style=\"width: 100%; border-collapse: collapse; margin: 20px 0;\">" +
                "      <tr style=\"background-color: #f7fafc; border-bottom: 1px solid #edf2f7;\">" +
                "        <td style=\"padding: 12px;\"><strong>Sản phẩm:</strong></td>" +
                "        <td style=\"text-align: right; padding: 12px;\">" + productName + "</td>" +
                "      </tr>" +
                "      <tr style=\"border-bottom: 1px solid #edf2f7;\">" +
                "        <td style=\"padding: 12px;\"><strong>Phí cẩu hạ cây:</strong></td>" +
                "        <td style=\"text-align: right; padding: 12px;\">" + order.getCraneFee() + " VND</td>" +
                "      </tr>" +
                "      <tr style=\"border-bottom: 1px solid #edf2f7;\">" +
                "        <td style=\"padding: 12px;\"><strong>Phí vận chuyển:</strong></td>" +
                "        <td style=\"text-align: right; padding: 12px;\">" + order.getShippingFee() + " VND</td>" +
                "      </tr>" +
                "      <tr style=\"background-color: #f0fff4; font-weight: bold; color: #276749;\">" +
                "        <td style=\"padding: 12px;\">Tổng chi phí thanh toán:</td>" +
                "        <td style=\"text-align: right; padding: 12px;\">" + order.getTotalAmount() + " VND</td>" +
                "      </tr>" +
                "    </table>" +
                "    <p>Vui lòng nhấp vào nút bên dưới để tiến hành thanh toán trực tuyến trong vòng 24 giờ:</p>" +
                "    <div style=\"text-align: center; margin: 30px 0;\">" +
                "      <a href=\"" + paymentLink
                + "\" style=\"background-color: #2e7d32; color: white; padding: 14px 30px; text-decoration: none; font-weight: bold; border-radius: 6px; display: inline-block;\">"
                +
                "        TIẾN HÀNH THANH TOÁN" +
                "      </a>" +
                "    </div>" +
                "  </div>" +
                "  <div style=\"text-align: center; font-size: 12px; color: #a0aec0; border-top: 1px solid #edf2f7; padding-top: 15px;\">"
                +
                "    © " + java.time.LocalDate.now().getYear() + " Bonsai Shop. All rights reserved." +
                "  </div>" +
                "</div>";
    }

    private String buildDepositedTemplate(Order order) {
        String customerName = order.getCustomerName() != null ? order.getCustomerName() : "Quý khách hàng";
        java.math.BigDecimal deposit = order.getDepositAmount() != null ? order.getDepositAmount() : java.math.BigDecimal.ZERO;
        java.math.BigDecimal total = order.getTotalAmount() != null ? order.getTotalAmount() : java.math.BigDecimal.ZERO;
        java.math.BigDecimal remaining = total.subtract(deposit);

        return "<div style=\"font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; max-width: 600px; margin: 0 auto; padding: 25px; border: 1px solid #e2e8f0; border-radius: 12px; background-color: #ffffff;\">" +
                "  <div style=\"text-align: center; background: linear-gradient(135deg, #2b6cb0, #3182ce); color: white; padding: 20px; border-radius: 8px 8px 0 0;\">" +
                "    <h2 style=\"margin: 0;\">Xác Nhận Nhận Tiền Đặt Cọc</h2>" +
                "    <p style=\"margin: 5px 0 0 0;\">Mã đơn hàng: #" + order.getOrderCode() + "</p>" +
                "  </div>" +
                "  <div style=\"padding: 20px 0; color: #1a202c; line-height: 1.6;\">" +
                "    <p>Xin chào <strong>" + customerName + "</strong>,</p>" +
                "    <p>Bonsai Shop đã nhận thành công khoản <strong>tiền đặt cọc (Giai đoạn 1)</strong> cho đơn hàng của bạn.</p>" +
                "    <table style=\"width: 100%; border-collapse: collapse; margin: 20px 0;\">" +
                "      <tr style=\"background-color: #f7fafc; border-bottom: 1px solid #edf2f7;\">" +
                "        <td style=\"padding: 12px;\"><strong>Tổng giá trị đơn hàng:</strong></td>" +
                "        <td style=\"text-align: right; padding: 12px;\">" + total + " VND</td>" +
                "      </tr>" +
                "      <tr style=\"border-bottom: 1px solid #edf2f7; color: #2b6cb0; font-weight: bold;\">" +
                "        <td style=\"padding: 12px;\">Đã thanh toán (Cọc 30%):</td>" +
                "        <td style=\"text-align: right; padding: 12px;\">" + deposit + " VND</td>" +
                "      </tr>" +
                "      <tr style=\"background-color: #fffaf0; font-weight: bold; color: #c05621;\">" +
                "        <td style=\"padding: 12px;\">Số tiền còn lại (Thanh toán khi nhận cây):</td>" +
                "        <td style=\"text-align: right; padding: 12px;\">" + remaining + " VND</td>" +
                "      </tr>" +
                "    </table>" +
                "    <p>Đội ngũ Bonsai Shop đang tiến hành chèn bảo vệ và đóng bọc cây để cẩu đến địa chỉ của bạn. Vui lòng chuẩn bị số tiền còn lại (<strong>" + remaining + " VND</strong>) để thanh toán cho tài xế/Shipper khi nhận hàng.</p>" +
                "  </div>" +
                "  <div style=\"text-align: center; font-size: 12px; color: #a0aec0; border-top: 1px solid #edf2f7; padding-top: 15px;\">" +
                "    © " + java.time.LocalDate.now().getYear() + " Bonsai Shop. All rights reserved." +
                "  </div>" +
                "</div>";
    }

    private String buildFinalReceiptTemplate(Order order) {
        String customerName = order.getCustomerName() != null ? order.getCustomerName() : "Quý khách hàng";
        java.math.BigDecimal total = order.getTotalAmount() != null ? order.getTotalAmount() : java.math.BigDecimal.ZERO;

        return "<div style=\"font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; max-width: 600px; margin: 0 auto; padding: 25px; border: 1px solid #e2e8f0; border-radius: 12px; background-color: #ffffff;\">" +
                "  <div style=\"text-align: center; background: linear-gradient(135deg, #276749, #2f855a); color: white; padding: 20px; border-radius: 8px 8px 0 0;\">" +
                "    <h2 style=\"margin: 0;\">Hóa Đơn Hoàn Tất Thanh Toán 100%</h2>" +
                "    <p style=\"margin: 5px 0 0 0;\">Mã đơn hàng: #" + order.getOrderCode() + "</p>" +
                "  </div>" +
                "  <div style=\"padding: 20px 0; color: #1a202c; line-height: 1.6;\">" +
                "    <p>Xin chào <strong>" + customerName + "</strong>,</p>" +
                "    <p>Giao dịch đơn hàng <strong>#" + order.getOrderCode() + "</strong> đã hoàn thành thanh toán 100%. Cảm ơn bạn đã tin tưởng và chọn mua tác phẩm Bonsai tại cửa hàng của chúng tôi!</p>" +
                "    <table style=\"width: 100%; border-collapse: collapse; margin: 20px 0;\">" +
                "      <tr style=\"background-color: #f0fff4; font-weight: bold; color: #22543d;\">" +
                "        <td style=\"padding: 12px;\">Tổng tiền đã thanh toán hoàn tất:</td>" +
                "        <td style=\"text-align: right; padding: 12px;\">" + total + " VND</td>" +
                "      </tr>" +
                "    </table>" +
                "  </div>" +
                "  <div style=\"text-align: center; font-size: 12px; color: #a0aec0; border-top: 1px solid #edf2f7; padding-top: 15px;\">" +
                "    © " + java.time.LocalDate.now().getYear() + " Bonsai Shop. All rights reserved." +
                "  </div>" +
                "</div>";
    }

    private void sendHtmlEmailWithRetry(String toEmail, String subject, String emailContent, String orderCode) {
        int attempt = 0;
        boolean success = false;

        while (attempt < MAX_RETRIES && !success) {
            attempt++;
            try {
                log.info("Bắt đầu gửi email (Lần {}/{}) tới: {}, Tiêu đề: {}", attempt, MAX_RETRIES, toEmail,
                        subject);
                MimeMessage message = mailSender.createMimeMessage();
                MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
                helper.setFrom(fromEmail);
                helper.setTo(toEmail);
                helper.setSubject(subject);
                helper.setText(emailContent, true);
                mailSender.send(message);
                success = true;
                log.info("Gửi email thành công cho đơn hàng {} ở lần thử thứ {}", orderCode, attempt);
            } catch (Exception e) {
                log.error("Lỗi gửi email cho đơn hàng {} ở lần thứ {}: {}", orderCode, attempt, e.getMessage(), e);
                if (attempt < MAX_RETRIES) {
                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                    }
                } else {
                    log.error("Gửi email thất bại hoàn toàn sau {} lần thử cho đơn hàng: {}", MAX_RETRIES, orderCode);
                }
            }
        }
    }
}
