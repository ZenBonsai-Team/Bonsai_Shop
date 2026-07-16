package com.example.bonsai_shop.customer.service;

import lombok.RequiredArgsConstructor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    public void sendOtpEmail(String toEmail, String otpCode) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(toEmail);
        message.setSubject("🌿 Bonsai Shop - Mã OTP dùng để đăng nhập");
        message.setText(
                "Xin chào!\n\n" +
                        "Mã OTP của bạn là: " + otpCode + "\n\n" +
                        "Mã này có hiệu lực trong 5 phút.\n" +
                        "Trân trọng,\nBonsai Shop"
        );
        mailSender.send(message);
    }
    public void sendOtpResetPassword(String toEmail, String otpCode) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(toEmail);
        message.setSubject("🌿 Bonsai Shop - Mã OTP dùng để đổi lại mật khẩu");
        message.setText(
                "Xin chào!\n\n" +
                        "Mã OTP để đặt lại mật khẩu của bạn là: " + otpCode + "\n\n" +
                        "Mã này có hiệu lực trong 5 phút.\n" +
                        "Trân trọng,\nBonsai Shop"
        );
        mailSender.send(message);
    }
}