package com.example.bonsai_shop.customer.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    public void sendOtpEmail(String toEmail, String otpCode) {
        SimpleMailMessage message = new SimpleMailMessage();
        if (fromEmail != null) message.setFrom(fromEmail);
        message.setTo(toEmail);
        message.setSubject("🌿 Bonsai Shop - Mã OTP dùng để đăng nhập");
        message.setText(
                "Xin chào!\n\n" +
                        "Mã OTP của bạn là: " + otpCode + "\n\n" +
                        "Mã này có hiệu lực trong 5 phút.\n" +
                        "Trân trọng,\nBonsai Shop"
        );
        try {
            mailSender.send(message);
            System.out.println(">>> OTP Email sent successfully to " + toEmail);
        } catch (org.springframework.mail.MailException e) {
            System.err.println("==================================================");
            System.err.println("=== EMAIL SENDING FAILED (SMTP Error) ===");
            System.err.println("Error details: " + e.getMessage());
            System.err.println("Generated SIGNUP OTP for " + toEmail + " is: " + otpCode);
            System.err.println("==================================================");
        }
    }
    public void sendOtpResetPassword(String toEmail, String otpCode) {
        SimpleMailMessage message = new SimpleMailMessage();
        if (fromEmail != null) message.setFrom(fromEmail);
        message.setTo(toEmail);
        message.setSubject("🌿 Bonsai Shop - Mã OTP dùng để đổi lại mật khẩu");
        message.setText(
                "Xin chào!\n\n" +
                        "Mã OTP để đặt lại mật khẩu của bạn là: " + otpCode + "\n\n" +
                        "Mã này có hiệu lực trong 5 phút.\n" +
                        "Trân trọng,\nBonsai Shop"
        );
        try {
            mailSender.send(message);
            System.out.println(">>> Reset Password OTP Email sent successfully to " + toEmail);
        } catch (org.springframework.mail.MailException e) {
            System.err.println("==================================================");
            System.err.println("=== EMAIL SENDING FAILED (SMTP Error) ===");
            System.err.println("Error details: " + e.getMessage());
            System.err.println("Generated RESET PASSWORD OTP for " + toEmail + " is: " + otpCode);
            System.err.println("==================================================");
        }
    }

    public void sendGuestOrderOtp(String toEmail, String otpCode) {
        SimpleMailMessage message = new SimpleMailMessage();
        if (fromEmail != null) message.setFrom(fromEmail);
        message.setTo(toEmail);
        message.setSubject("🌿 Bonsai Shop - Mã OTP xác nhận đặt hàng");
        message.setText(
                "Xin chào!\n\n" +
                        "Mã OTP để xác nhận đặt hàng của bạn là: " + otpCode + "\n\n" +
                        "Mã này có hiệu lực trong 5 phút.\n" +
                        "Trân trọng,\nBonsai Shop"
        );
        try {
            mailSender.send(message);
            System.out.println(">>> Guest Order OTP Email sent successfully to " + toEmail);
        } catch (org.springframework.mail.MailException e) {
            System.err.println("==================================================");
            System.err.println("=== EMAIL SENDING FAILED (SMTP Error) ===");
            System.err.println("Error details: " + e.getMessage());
            System.err.println("Generated GUEST ORDER OTP for " + toEmail + " is: " + otpCode);
            System.err.println("==================================================");
        }
    }
}