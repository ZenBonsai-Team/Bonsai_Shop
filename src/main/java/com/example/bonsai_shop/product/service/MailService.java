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

    public void sendOrderCreatedEmail(Order order) {
        String orderCode = order.getOrderCode();
        String toEmail = order.getCustomerEmail();

        if (toEmail == null || toEmail.trim().isEmpty()) {
            log.warn("Không tìm thấy email của khách hàng cho đơn hàng {}", orderCode);
            return;
        }

        String emailContent = buildCreatedTemplate(order);
        sendHtmlEmailWithRetry(toEmail, "Xác nhận ghi nhận đơn hàng #" + orderCode + " - Bonsai Shop", emailContent, orderCode);
    }

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

    private String buildProductNamesSummary(Order order) {
        if (order.getOrderDetails() == null || order.getOrderDetails().isEmpty()) {
            return "N/A";
        }
        StringBuilder sb = new StringBuilder();
        for (OrderDetail detail : order.getOrderDetails()) {
            if (detail.getProduct() != null) {
                if (sb.length() > 0) {
                    sb.append(", ");
                }
                sb.append(detail.getProduct().getProductName());
                if (detail.getQuantity() != null && detail.getQuantity() > 1) {
                    sb.append(" (x").append(detail.getQuantity()).append(")");
                }
            }
        }
        return sb.length() > 0 ? sb.toString() : "N/A";
    }

    private String buildProductTableRows(Order order) {
        if (order.getOrderDetails() == null || order.getOrderDetails().isEmpty()) {
            return "<tr><td colspan=\"4\" style=\"padding: 10px; text-align: center; color: #a0aec0;\">Không có thông tin chi tiết sản phẩm</td></tr>";
        }
        StringBuilder sb = new StringBuilder();
        int stt = 1;
        for (OrderDetail detail : order.getOrderDetails()) {
            String pName = (detail.getProduct() != null && detail.getProduct().getProductName() != null)
                    ? detail.getProduct().getProductName()
                    : "Tác phẩm Bonsai";
            java.math.BigDecimal price = detail.getPriceAtPurchase() != null
                    ? detail.getPriceAtPurchase()
                    : (detail.getProduct() != null ? detail.getProduct().getPrice() : java.math.BigDecimal.ZERO);
            int qty = detail.getQuantity() != null ? detail.getQuantity() : 1;

            sb.append("<tr style=\"border-bottom: 1px solid #edf2f7;\">")
              .append("<td style=\"padding: 10px 12px; color: #4a5568;\">").append(stt++).append("</td>")
              .append("<td style=\"padding: 10px 12px; color: #2d3748;\"><strong>").append(pName).append("</strong></td>")
              .append("<td style=\"padding: 10px 12px; text-align: center; color: #4a5568;\">").append(qty).append("</td>")
              .append("<td style=\"padding: 10px 12px; text-align: right; color: #2d3748; font-weight: 500;\">").append(price).append(" VND</td>")
              .append("</tr>");
        }
        return sb.toString();
    }

    private String buildRejectedTemplate(Order order, String reason) {
        String customerName = order.getCustomerName() != null ? order.getCustomerName() : "Quý khách hàng";
        String productSummary = buildProductNamesSummary(order);
        String productTable = buildProductTableRows(order);

        return "<div style=\"font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; max-width: 600px; margin: 0 auto; padding: 25px; border: 1px solid #e2e8f0; border-radius: 12px; background-color: #ffffff;\">"
                + "  <div style=\"text-align: center; background: linear-gradient(135deg, #c53030, #e53e3e); color: white; padding: 20px; border-radius: 8px 8px 0 0;\">"
                + "    <h2 style=\"margin: 0;\">Thông Báo Từ Chối Đơn Hàng</h2>"
                + "    <p style=\"margin: 5px 0 0 0;\">Mã đơn hàng: #" + order.getOrderCode() + "</p>"
                + "  </div>"
                + "  <div style=\"padding: 20px 0; color: #1a202c; line-height: 1.6;\">"
                + "    <p>Xin chào <strong>" + customerName + "</strong>,</p>"
                + "    <p>Rất tiếc, đơn đặt hàng sản phẩm <strong>" + productSummary + "</strong> (Mã đơn hàng: <strong>#" + order.getOrderCode() + "</strong>) của bạn đã bị từ chối duyệt.</p>"
                + "    <div style=\"background-color: #fff5f5; border-left: 4px solid #e53e3e; padding: 15px; border-radius: 4px; margin: 20px 0;\">"
                + "      <h4 style=\"margin: 0 0 5px 0; color: #9b2c2c;\">Lý do từ chối:</h4>"
                + "      <p style=\"margin: 0; color: #c53030;\">" + reason + "</p>"
                + "    </div>"
                + "    <h4 style=\"color: #2d3748; margin: 20px 0 10px 0;\">Danh sách sản phẩm trong đơn hàng:</h4>"
                + "    <table style=\"width: 100%; border-collapse: collapse; margin-bottom: 20px;\">"
                + "      <thead>"
                + "        <tr style=\"background-color: #edf2f7; color: #4a5568; font-size: 13px;\">"
                + "          <th style=\"padding: 8px 12px; text-align: left;\">STT</th>"
                + "          <th style=\"padding: 8px 12px; text-align: left;\">Tên tác phẩm</th>"
                + "          <th style=\"padding: 8px 12px; text-align: center;\">SL</th>"
                + "          <th style=\"padding: 8px 12px; text-align: right;\">Đơn giá</th>"
                + "        </tr>"
                + "      </thead>"
                + "      <tbody>" + productTable + "</tbody>"
                + "    </table>"
                + "  </div>"
                + "  <div style=\"text-align: center; font-size: 12px; color: #a0aec0; border-top: 1px solid #edf2f7; padding-top: 15px;\">"
                + "    © " + java.time.LocalDate.now().getYear() + " Bonsai Shop. All rights reserved."
                + "  </div>"
                + "</div>";
    }

    private String formatVND(java.math.BigDecimal amount) {
        if (amount == null) return "0 VND";
        return String.format("%,d VND", amount.longValue()).replace(',', '.');
    }

    private String buildAprovedTemplate(Order order, String paymentLink) {
        String customerName = order.getCustomerName() != null ? order.getCustomerName() : "Quý khách hàng";
        String productTable = buildProductTableRows(order);

        java.math.BigDecimal craneFee = order.getCraneFee() != null ? order.getCraneFee() : java.math.BigDecimal.ZERO;
        java.math.BigDecimal shippingFee = order.getShippingFee() != null ? order.getShippingFee() : java.math.BigDecimal.ZERO;
        java.math.BigDecimal totalAmount = order.getTotalAmount() != null ? order.getTotalAmount() : java.math.BigDecimal.ZERO;
        java.math.BigDecimal depositAmount = order.getDepositAmount() != null ? order.getDepositAmount() : java.math.BigDecimal.ZERO;

        java.math.BigDecimal treePrice = java.math.BigDecimal.ZERO;
        if (order.getOrderDetails() != null && !order.getOrderDetails().isEmpty()) {
            treePrice = order.getOrderDetails().stream()
                    .map(d -> (d.getPriceAtPurchase() != null ? d.getPriceAtPurchase() : java.math.BigDecimal.ZERO)
                            .multiply(java.math.BigDecimal.valueOf(d.getQuantity() != null ? d.getQuantity() : 1)))
                    .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);
        } else {
            treePrice = totalAmount.subtract(craneFee).subtract(shippingFee);
            if (treePrice.compareTo(java.math.BigDecimal.ZERO) < 0) treePrice = java.math.BigDecimal.ZERO;
        }

        boolean isDeposit = "DEPOSIT".equalsIgnoreCase(order.getPaymentMethod()) || "COD".equalsIgnoreCase(order.getPaymentMethod());

        java.math.BigDecimal immediatePayment = isDeposit ? depositAmount.add(craneFee).add(shippingFee) : totalAmount;
        java.math.BigDecimal remainingPayment = isDeposit ? treePrice.subtract(depositAmount) : java.math.BigDecimal.ZERO;
        if (remainingPayment.compareTo(java.math.BigDecimal.ZERO) < 0) remainingPayment = java.math.BigDecimal.ZERO;

        StringBuilder financialBlocks = new StringBuilder();

        if (isDeposit) {
            financialBlocks.append("<div style=\"margin: 20px 0; border: 1px solid #e2e8f0; border-radius: 8px; overflow: hidden;\">")
                .append("<div style=\"background-color: #f7fafc; padding: 12px 15px; border-bottom: 1px solid #e2e8f0;\">")
                .append("<strong style=\"color: #2d3748; font-size: 15px;\">1. GIÁ TRỊ ĐƠN HÀNG</strong>")
                .append("</div>")
                .append("<table style=\"width: 100%; border-collapse: collapse;\">")
                .append("<tr><td style=\"padding: 8px 15px;\">Giá cây:</td><td style=\"text-align: right; padding: 8px 15px; font-weight: 500;\">").append(formatVND(treePrice)).append("</td></tr>")
                .append("<tr><td style=\"padding: 8px 15px;\">Phí vận chuyển:</td><td style=\"text-align: right; padding: 8px 15px; font-weight: 500;\">").append(formatVND(shippingFee)).append("</td></tr>")
                .append("<tr><td style=\"padding: 8px 15px;\">Phí xe cẩu:</td><td style=\"text-align: right; padding: 8px 15px; font-weight: 500;\">").append(formatVND(craneFee)).append("</td></tr>")
                .append("<tr style=\"border-top: 1px solid #edf2f7; background-color: #edf2f7;\"><td style=\"padding: 10px 15px;\"><strong>Tổng giá trị đơn hàng:</strong></td><td style=\"text-align: right; padding: 10px 15px;\"><strong>").append(formatVND(totalAmount)).append("</strong></td></tr>")
                .append("</table>")

                .append("<div style=\"background-color: #e6fffa; padding: 12px 15px; border-top: 2px solid #319795; border-bottom: 1px solid #e2e8f0;\">")
                .append("<strong style=\"color: #234e52; font-size: 15px;\">2. THANH TOÁN NGAY QUA VNPAY</strong>")
                .append("</div>")
                .append("<table style=\"width: 100%; border-collapse: collapse;\">")
                .append("<tr><td style=\"padding: 8px 15px;\">Tiền đặt cọc cây:</td><td style=\"text-align: right; padding: 8px 15px; font-weight: 500;\">").append(formatVND(depositAmount)).append("</td></tr>")
                .append("<tr><td style=\"padding: 8px 15px;\">Phí vận chuyển:</td><td style=\"text-align: right; padding: 8px 15px; font-weight: 500;\">").append(formatVND(shippingFee)).append("</td></tr>")
                .append("<tr><td style=\"padding: 8px 15px;\">Phí xe cẩu:</td><td style=\"text-align: right; padding: 8px 15px; font-weight: 500;\">").append(formatVND(craneFee)).append("</td></tr>")
                .append("<tr style=\"border-top: 1px solid #e2e8f0; background-color: #f0fff4; color: #22543d;\"><td style=\"padding: 10px 15px;\"><strong style=\"font-size: 15px;\">KHÁCH CẦN THANH TOÁN NGAY:</strong></td><td style=\"text-align: right; padding: 10px 15px;\"><strong style=\"font-size: 16px; color: #2e7d32;\">").append(formatVND(immediatePayment)).append("</strong></td></tr>")
                .append("</table>")

                .append("<div style=\"background-color: #fffaf0; padding: 12px 15px; border-top: 2px solid #dd6b20; border-bottom: 1px solid #e2e8f0;\">")
                .append("<strong style=\"color: #7b341e; font-size: 15px;\">3. THANH TOÁN KHI NHẬN CÂY (NẤC CÒN LẠI)</strong>")
                .append("</div>")
                .append("<table style=\"width: 100%; border-collapse: collapse;\">")
                .append("<tr><td style=\"padding: 12px 15px;\">Phần còn lại của giá cây (Giá cây - Tiền cọc):</td><td style=\"text-align: right; padding: 12px 15px; font-weight: bold; color: #c05621; font-size: 15px;\">").append(formatVND(remainingPayment)).append("</td></tr>")
                .append("</table>")
                .append("</div>");
        } else {
            financialBlocks.append("<table style=\"width: 100%; border-collapse: collapse; margin: 20px 0; border: 1px solid #e2e8f0; border-radius: 8px;\">")
                .append("<tr style=\"border-bottom: 1px solid #edf2f7;\"><td style=\"padding: 12px;\">Giá cây:</td><td style=\"text-align: right; padding: 12px;\">").append(formatVND(treePrice)).append("</td></tr>")
                .append("<tr style=\"border-bottom: 1px solid #edf2f7;\"><td style=\"padding: 12px;\">Phí cẩu hạ cây:</td><td style=\"text-align: right; padding: 12px;\">").append(formatVND(craneFee)).append("</td></tr>")
                .append("<tr style=\"border-bottom: 1px solid #edf2f7;\"><td style=\"padding: 12px;\">Phí vận chuyển:</td><td style=\"text-align: right; padding: 12px;\">").append(formatVND(shippingFee)).append("</td></tr>")
                .append("<tr style=\"background-color: #f0fff4; font-weight: bold; color: #276749;\"><td style=\"padding: 12px;\">THANH TOÁN 100% QUA VNPAY:</td><td style=\"text-align: right; padding: 12px; font-size: 16px;\">").append(formatVND(totalAmount)).append("</td></tr>")
                .append("</table>");
        }

        return "<div style=\"font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; max-width: 600px; margin: 0 auto; padding: 25px; border: 1px solid #e2e8f0; border-radius: 12px; background-color: #ffffff;\">"
                + "  <div style=\"text-align: center; background: linear-gradient(135deg, #2e7d32, #4caf50); color: white; padding: 20px; border-radius: 8px 8px 0 0;\">"
                + "    <h2 style=\"margin: 0;\">Đơn Hàng Đã Được Phê Duyệt!</h2>"
                + "    <p style=\"margin: 5px 0 0 0;\">Mã đơn hàng: #" + order.getOrderCode() + "</p>"
                + "  </div>"
                + "  <div style=\"padding: 20px 0; color: #1a202c; line-height: 1.6;\">"
                + "    <p>Xin chào <strong>" + customerName + "</strong>,</p>"
                + "    <p>Đơn hàng đặt mua cây cảnh của bạn đã được kiểm duyệt thành công. Dưới đây là thông tin chi tiết danh sách tác phẩm và chi phí:</p>"
                + "    <h4 style=\"color: #2d3748; margin: 20px 0 10px 0;\">Danh sách tác phẩm Bonsai:</h4>"
                + "    <table style=\"width: 100%; border-collapse: collapse; margin-bottom: 20px;\">"
                + "      <thead>"
                + "        <tr style=\"background-color: #edf2f7; color: #4a5568; font-size: 13px;\">"
                + "          <th style=\"padding: 8px 12px; text-align: left;\">STT</th>"
                + "          <th style=\"padding: 8px 12px; text-align: left;\">Tên tác phẩm</th>"
                + "          <th style=\"padding: 8px 12px; text-align: center;\">SL</th>"
                + "          <th style=\"padding: 8px 12px; text-align: right;\">Đơn giá</th>"
                + "        </tr>"
                + "      </thead>"
                + "      <tbody>" + productTable + "</tbody>"
                + "    </table>"
                + financialBlocks.toString()
                + "    <p>Vui lòng nhấp vào nút bên dưới để tiến hành thanh toán trực tuyến trong vòng 24 giờ:</p>"
                + "    <div style=\"text-align: center; margin: 30px 0;\">"
                + "      <a href=\"" + paymentLink + "\" style=\"background-color: #2e7d32; color: white; padding: 14px 30px; text-decoration: none; font-weight: bold; border-radius: 6px; display: inline-block;\">"
                + "        TIẾN HÀNH THANH TOÁN (" + formatVND(immediatePayment) + ")"
                + "      </a>"
                + "    </div>"
                + "  </div>"
                + "  <div style=\"text-align: center; font-size: 12px; color: #a0aec0; border-top: 1px solid #edf2f7; padding-top: 15px;\">"
                + "    © " + java.time.LocalDate.now().getYear() + " Bonsai Shop. All rights reserved."
                + "  </div>"
                + "</div>";
    }

    private String buildDepositedTemplate(Order order) {
        String customerName = order.getCustomerName() != null ? order.getCustomerName() : "Quý khách hàng";
        java.math.BigDecimal deposit = order.getDepositAmount() != null ? order.getDepositAmount() : java.math.BigDecimal.ZERO;
        java.math.BigDecimal craneFee = order.getCraneFee() != null ? order.getCraneFee() : java.math.BigDecimal.ZERO;
        java.math.BigDecimal shippingFee = order.getShippingFee() != null ? order.getShippingFee() : java.math.BigDecimal.ZERO;
        java.math.BigDecimal total = order.getTotalAmount() != null ? order.getTotalAmount() : java.math.BigDecimal.ZERO;

        java.math.BigDecimal treePrice = java.math.BigDecimal.ZERO;
        if (order.getOrderDetails() != null && !order.getOrderDetails().isEmpty()) {
            treePrice = order.getOrderDetails().stream()
                    .map(d -> (d.getPriceAtPurchase() != null ? d.getPriceAtPurchase() : java.math.BigDecimal.ZERO)
                            .multiply(java.math.BigDecimal.valueOf(d.getQuantity() != null ? d.getQuantity() : 1)))
                    .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);
        } else {
            treePrice = total.subtract(craneFee).subtract(shippingFee);
            if (treePrice.compareTo(java.math.BigDecimal.ZERO) < 0) treePrice = java.math.BigDecimal.ZERO;
        }

        java.math.BigDecimal remaining = treePrice.subtract(deposit);
        if (remaining.compareTo(java.math.BigDecimal.ZERO) < 0) remaining = java.math.BigDecimal.ZERO;

        java.math.BigDecimal paidNac1 = deposit.add(craneFee).add(shippingFee);
        String productTable = buildProductTableRows(order);

        return "<div style=\"font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; max-width: 600px; margin: 0 auto; padding: 25px; border: 1px solid #e2e8f0; border-radius: 12px; background-color: #ffffff;\">" +
                "  <div style=\"text-align: center; background: linear-gradient(135deg, #2b6cb0, #3182ce); color: white; padding: 20px; border-radius: 8px 8px 0 0;\">" +
                "    <h2 style=\"margin: 0;\">Xác Nhận Thanh Toán Nấc 1 Thành Công</h2>" +
                "    <p style=\"margin: 5px 0 0 0;\">Mã đơn hàng: #" + order.getOrderCode() + "</p>" +
                "  </div>" +
                "  <div style=\"padding: 20px 0; color: #1a202c; line-height: 1.6;\">" +
                "    <p>Xin chào <strong>" + customerName + "</strong>,</p>" +
                "    <p>Bonsai Shop đã nhận thành công khoản <strong>thanh toán Nấc 1 (" + formatVND(paidNac1) + ")</strong> (bao gồm Tiền cọc cây + Phí xe cẩu + Phí vận chuyển) cho đơn hàng của bạn.</p>" +
                "    <h4 style=\"color: #2d3748; margin: 20px 0 10px 0;\">Danh sách tác phẩm Bonsai:</h4>" +
                "    <table style=\"width: 100%; border-collapse: collapse; margin-bottom: 20px;\">" +
                "      <thead>" +
                "        <tr style=\"background-color: #edf2f7; color: #4a5568; font-size: 13px;\">" +
                "          <th style=\"padding: 8px 12px; text-align: left;\">STT</th>" +
                "          <th style=\"padding: 8px 12px; text-align: left;\">Tên tác phẩm</th>" +
                "          <th style=\"padding: 8px 12px; text-align: center;\">SL</th>" +
                "          <th style=\"padding: 8px 12px; text-align: right;\">Đơn giá</th>" +
                "        </tr>" +
                "      </thead>" +
                "      <tbody>" + productTable + "</tbody>" +
                "    </table>" +
                "    <table style=\"width: 100%; border-collapse: collapse; margin: 20px 0; border: 1px solid #e2e8f0; border-radius: 8px;\">" +
                "      <tr style=\"background-color: #f7fafc; border-bottom: 1px solid #edf2f7;\">" +
                "        <td style=\"padding: 12px;\"><strong>Giá cây:</strong></td>" +
                "        <td style=\"text-align: right; padding: 12px;\">" + formatVND(treePrice) + "</td>" +
                "      </tr>" +
                "      <tr style=\"border-bottom: 1px solid #edf2f7; color: #2b6cb0; font-weight: bold;\">" +
                "        <td style=\"padding: 12px;\">Đã thanh toán (Nấc 1: Cọc + Phí cẩu/ship):</td>" +
                "        <td style=\"text-align: right; padding: 12px;\">" + formatVND(paidNac1) + "</td>" +
                "      </tr>" +
                "      <tr style=\"background-color: #fffaf0; font-weight: bold; color: #c05621;\">" +
                "        <td style=\"padding: 12px;\">Số tiền còn lại (Thanh toán khi nhận cây):</td>" +
                "        <td style=\"text-align: right; padding: 12px; font-size: 15px;\">" + formatVND(remaining) + "</td>" +
                "      </tr>" +
                "    </table>" +
                "    <p>Đội ngũ Bonsai Shop đang tiến hành chèn bảo vệ và đóng bọc cây để cẩu đến địa chỉ của bạn. Vui lòng chuẩn bị phần tiền cây còn lại (<strong>" + formatVND(remaining) + "</strong>) để thanh toán cho tài xế/Shipper khi nhận hàng.</p>" +
                "  </div>" +
                "  <div style=\"text-align: center; font-size: 12px; color: #a0aec0; border-top: 1px solid #edf2f7; padding-top: 15px;\">" +
                "    © " + java.time.LocalDate.now().getYear() + " Bonsai Shop. All rights reserved." +
                "  </div>" +
                "</div>";
    }

    private String buildFinalReceiptTemplate(Order order) {
        String customerName = order.getCustomerName() != null ? order.getCustomerName() : "Quý khách hàng";
        java.math.BigDecimal total = order.getTotalAmount() != null ? order.getTotalAmount() : java.math.BigDecimal.ZERO;
        String productTable = buildProductTableRows(order);

        return "<div style=\"font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; max-width: 600px; margin: 0 auto; padding: 25px; border: 1px solid #e2e8f0; border-radius: 12px; background-color: #ffffff;\">" +
                "  <div style=\"text-align: center; background: linear-gradient(135deg, #276749, #2f855a); color: white; padding: 20px; border-radius: 8px 8px 0 0;\">" +
                "    <h2 style=\"margin: 0;\">Hóa Đơn Hoàn Tất Thanh Toán 100%</h2>" +
                "    <p style=\"margin: 5px 0 0 0;\">Mã đơn hàng: #" + order.getOrderCode() + "</p>" +
                "  </div>" +
                "  <div style=\"padding: 20px 0; color: #1a202c; line-height: 1.6;\">" +
                "    <p>Xin chào <strong>" + customerName + "</strong>,</p>" +
                "    <p>Giao dịch đơn hàng <strong>#" + order.getOrderCode() + "</strong> đã hoàn thành thanh toán 100%. Cảm ơn bạn đã tin tưởng và chọn mua tác phẩm Bonsai tại cửa hàng của chúng tôi!</p>" +
                "    <h4 style=\"color: #2d3748; margin: 20px 0 10px 0;\">Danh sách tác phẩm Bonsai đã mua:</h4>" +
                "    <table style=\"width: 100%; border-collapse: collapse; margin-bottom: 20px;\">" +
                "      <thead>" +
                "        <tr style=\"background-color: #edf2f7; color: #4a5568; font-size: 13px;\">" +
                "          <th style=\"padding: 8px 12px; text-align: left;\">STT</th>" +
                "          <th style=\"padding: 8px 12px; text-align: left;\">Tên tác phẩm</th>" +
                "          <th style=\"padding: 8px 12px; text-align: center;\">SL</th>" +
                "          <th style=\"padding: 8px 12px; text-align: right;\">Đơn giá</th>" +
                "        </tr>" +
                "      </thead>" +
                "      <tbody>" + productTable + "</tbody>" +
                "    </table>" +
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

    private String buildCreatedTemplate(Order order) {
        String customerName = order.getCustomerName() != null ? order.getCustomerName() : "Quý khách hàng";
        java.math.BigDecimal total = order.getTotalAmount() != null ? order.getTotalAmount() : java.math.BigDecimal.ZERO;
        String productTable = buildProductTableRows(order);

        return "<div style=\"font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; max-width: 600px; margin: 0 auto; padding: 25px; border: 1px solid #e2e8f0; border-radius: 12px; background-color: #ffffff;\">" +
                "  <div style=\"text-align: center; background: linear-gradient(135deg, #319795, #3182ce); color: white; padding: 20px; border-radius: 8px 8px 0 0;\">" +
                "    <h2 style=\"margin: 0;\">Hệ Thống Đã Ghi Nhận Đơn Hàng!</h2>" +
                "    <p style=\"margin: 5px 0 0 0;\">Mã đơn hàng: #" + order.getOrderCode() + "</p>" +
                "  </div>" +
                "  <div style=\"padding: 20px 0; color: #1a202c; line-height: 1.6;\">" +
                "    <p>Xin chào <strong>" + customerName + "</strong>,</p>" +
                "    <p>Bonsai Shop đã nhận được yêu cầu đặt hàng của bạn. Đơn hàng của bạn hiện đang ở trạng thái <strong>CHỜ KIỂM DUYỆT (PENDING)</strong>.</p>" +
                "    <div style=\"background-color: #ebf8ff; border-left: 4px solid #3182ce; padding: 15px; border-radius: 4px; margin: 20px 0;\">" +
                "      <h4 style=\"margin: 0 0 5px 0; color: #2b6cb0;\">Lưu ý quan trọng:</h4>" +
                "      <p style=\"margin: 0; color: #2c5282;\">Đây là email xác nhận hệ thống đã ghi nhận đơn hàng. Đội ngũ Moderator sẽ kiểm tra tình trạng cây, tính toán chi phí cẩu/vận chuyển và phê duyệt đơn hàng trong thời gian sớm nhất. Bạn sẽ nhận được email hướng dẫn thanh toán ngay khi đơn hàng được duyệt.</p>" +
                "    </div>" +
                "    <h4 style=\"color: #2d3748; margin: 20px 0 10px 0;\">Chi tiết danh sách cây cảnh đặt mua:</h4>" +
                "    <table style=\"width: 100%; border-collapse: collapse; margin: 10px 0 20px 0;\">" +
                "      <thead>" +
                "        <tr style=\"background-color: #edf2f7; color: #4a5568; font-size: 13px;\">" +
                "          <th style=\"padding: 8px 12px; text-align: left;\">STT</th>" +
                "          <th style=\"padding: 8px 12px; text-align: left;\">Tên tác phẩm</th>" +
                "          <th style=\"padding: 8px 12px; text-align: center;\">SL</th>" +
                "          <th style=\"padding: 8px 12px; text-align: right;\">Đơn giá</th>" +
                "        </tr>" +
                "      </thead>" +
                "      <tbody>" + productTable + "</tbody>" +
                "    </table>" +
                "    <table style=\"width: 100%; border-collapse: collapse;\">" +
                "      <tr style=\"border-bottom: 1px solid #edf2f7;\">" +
                "        <td style=\"padding: 12px;\"><strong>Địa chỉ giao hàng:</strong></td>" +
                "        <td style=\"text-align: right; padding: 12px;\">" + (order.getShippingAddress() != null ? order.getShippingAddress() : "N/A") + "</td>" +
                "      </tr>" +
                "      <tr style=\"background-color: #f0fff4; font-weight: bold; color: #276749;\">" +
                "        <td style=\"padding: 12px;\">Tạm tính giá trị cây:</td>" +
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
