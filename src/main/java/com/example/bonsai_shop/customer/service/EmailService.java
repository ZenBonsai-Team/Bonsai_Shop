package com.example.bonsai_shop.customer.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.internet.MimeMessage;

@Service
@RequiredArgsConstructor
public class EmailService {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    private static final int MAX_RETRIES = 3;

    public void sendOtpEmail(String toEmail, String otpCode) {
        String emailContent = buildOtpTemplate("Mã OTP Đăng Nhập", 
            "Mã OTP của bạn là: " + otpCode, otpCode);
        sendHtmlEmailWithRetry(toEmail, "🌿 Minh Kỷ Garden - Mã OTP dùng để đăng nhập", emailContent, "SIGNUP_OTP_" + toEmail);
    }

    public void sendOtpResetPassword(String toEmail, String otpCode) {
        String emailContent = buildOtpTemplate("Mã OTP Đặt Lại Mật Khẩu", 
            "Mã OTP để đặt lại mật khẩu của bạn là: " + otpCode, otpCode);
        sendHtmlEmailWithRetry(toEmail, "🌿 Minh Kỷ Garden - Mã OTP dùng để đổi lại mật khẩu", emailContent, "RESET_OTP_" + toEmail);
    }

    public void sendGuestOrderOtp(String toEmail, String otpCode) {
        String emailContent = buildOtpTemplate("Mã OTP Xác Nhận Đặt Hàng", 
            "Mã OTP để xác nhận đặt hàng của bạn là: " + otpCode, otpCode);
        sendHtmlEmailWithRetry(toEmail, "🌿 Minh Kỷ Garden - Mã OTP xác nhận đặt hàng", emailContent, "GUEST_OTP_" + toEmail);
    }

    public void sendGuestOrderOtpOrThrow(String toEmail, String otpCode) throws Exception {
        String emailContent = buildOtpTemplate("Mã OTP Xác Nhận Đặt Hàng", 
            "Mã OTP để xác nhận đặt hàng của bạn là: " + otpCode, otpCode);
        sendHtmlEmailOrThrow(toEmail, "🌿 Minh Kỷ Garden - Mã OTP xác nhận đặt hàng", emailContent);
    }

    private String buildOtpTemplate(String title, String message, String otpCode) {
        return "<div style=\"font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; max-width: 600px; margin: 0 auto; padding: 25px; border: 1px solid #e2e8f0; border-radius: 12px; background-color: #ffffff;\">"
                + "  <div style=\"text-align: center; background: linear-gradient(135deg, #2e7d32, #4caf50); color: white; padding: 20px; border-radius: 8px 8px 0 0;\">"
                + "    <h2 style=\"margin: 0;\">" + title + "</h2>"
                + "    <p style=\"margin: 5px 0 0 0;\">Minh Kỷ Garden</p>"
                + "  </div>"
                + "  <div style=\"padding: 30px 20px; color: #1a202c; line-height: 1.8; text-align: center;\">"
                + "    <p style=\"font-size: 16px; margin-bottom: 30px;\">Xin chào,</p>"
                + "    <p style=\"font-size: 15px; margin-bottom: 25px;\">" + message + "</p>"
                + "    <div style=\"background-color: #f0f4ff; border: 2px solid #2e7d32; padding: 25px; border-radius: 8px; margin: 30px 0;\">"
                + "      <p style=\"font-size: 13px; color: #666; margin: 0 0 10px 0; font-weight: 600; text-transform: uppercase; letter-spacing: 1px;\">Mã OTP của bạn:</p>"
                + "      <p style=\"font-size: 32px; font-weight: 700; letter-spacing: 3px; margin: 0; color: #2e7d32; font-family: 'Courier New', monospace;\">" + otpCode + "</p>"
                + "    </div>"
                + "    <p style=\"font-size: 14px; color: #666; margin: 25px 0 0 0;\"><strong>Mã này có hiệu lực trong 5 phút.</strong></p>"
                + "    <p style=\"font-size: 13px; color: #999; margin: 15px 0 0 0;\">Nếu bạn không yêu cầu mã này, vui lòng bỏ qua email này.</p>"
                + "  </div>"
                + "  <div style=\"text-align: center; font-size: 12px; color: #a0aec0; border-top: 1px solid #edf2f7; padding-top: 15px; margin-top: 20px;\">"
                + "    © " + java.time.LocalDate.now().getYear() + " Minh Kỷ Garden. All rights reserved."
                + "  </div>"
                + "</div>";
    }

    private void sendHtmlEmailWithRetry(String toEmail, String subject, String emailContent, String identifier) {
        int attempt = 0;
        boolean success = false;

        while (attempt < MAX_RETRIES && !success) {
            attempt++;
            try {
                log.info("Bắt đầu gửi email (Lần {}/{}) tới: {}, Tiêu đề: {}", attempt, MAX_RETRIES, toEmail, subject);
                MimeMessage message = mailSender.createMimeMessage();
                MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
                if (fromEmail != null) helper.setFrom(fromEmail);
                helper.setTo(toEmail);
                helper.setSubject(subject);
                helper.setText(emailContent, true);
                mailSender.send(message);
                success = true;
                log.info("Gửi email thành công cho {} ở lần thử thứ {}", identifier, attempt);
            } catch (Exception e) {
                log.error("Lỗi gửi email cho {} ở lần thứ {}: {}", identifier, attempt, e.getMessage(), e);
                if (attempt < MAX_RETRIES) {
                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                    }
                } else {
                    log.error("Gửi email thất bại hoàn toàn sau {} lần thử cho: {}", MAX_RETRIES, identifier);
                }
            }
        }
    }

    private void sendHtmlEmailOrThrow(String toEmail, String subject, String emailContent) throws Exception {
        try {
            log.info("Gửi email tới: {}, Tiêu đề: {}", toEmail, subject);
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            if (fromEmail != null) helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject(subject);
            helper.setText(emailContent, true);
            mailSender.send(message);
            log.info("Gửi email thành công tới: {}", toEmail);
        } catch (Exception e) {
            log.error("Lỗi gửi email tới {}: {}", toEmail, e.getMessage(), e);
            throw e;
        }
    }
}