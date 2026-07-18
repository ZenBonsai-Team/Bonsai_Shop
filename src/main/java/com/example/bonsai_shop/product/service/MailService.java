package com.example.bonsai_shop.product.service;

import org.springframework.mail.javamail.JavaMailSender;

public class MailService {
    /*
     * 1. Nhận yêu cầu gửi mail cho đơn hàng
     * 2. Kiểm tra Suppression Rules: Nếu đơn hàng có yêu cầu gửi trùng lặp trong
     * vòng 60s
     * yêu cầu t2 sẽ bị bỏ quá
     * 3. Thiết lập nội dung HTML theo mẫu thiết kế TEMPLATE ID: TMP-ORD-CONF
     * 4. Áp dụng Retry PolicyL Nếu gửi lỗi, thử lại tối đa 3 lần, mỗi lần cách nhau
     * 2s
     */
    // private final JavaMailSender mailSender;

}
