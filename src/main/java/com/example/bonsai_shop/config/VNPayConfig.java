package com.example.bonsai_shop.config;

import java.nio.charset.StandardCharsets;
import java.util.Random;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * [CẤU HÌNH CỔNG THANH TOÁN VNPAY & THUẬT TOÁN MÃ HÓA BẢO MẬT]
 *
 * Chịu trách nhiệm:
 * - Nạp các tham số cấu hình môi trường VNPay Sandbox/Production từ application.properties:
 *   + vnp_PayUrl: Đường dẫn cổng thanh toán VNPay
 *   + vnp_TmnCode: Terminal ID (Mã định danh website của merchant)
 *   + vnp_HashSecret: Khóa bí mật (Secret Key) dùng để sinh và kiểm tra chữ ký số HMAC-SHA512
 *   + vnp_ReturnUrl: Địa chỉ URL chuyển hướng trình duyệt của khách về sau khi thanh toán (/vnpay/payment-callback)
 * - Cung cấp các tiện ích mã hóa:
 *   + hmacSHA512(): Thuật toán sinh chữ ký số toàn vẹn dữ liệu
 *   + getIpAddress(): Trích xuất địa chỉ IP của client (hỗ trợ đọc qua header proxy X-FORWARDED-FOR)
 *   + getRandomNumber(): Sinh chuỗi số ngẫu nhiên cho mã tham chiếu giao dịch.
 */
@Configuration
public class VNPayConfig {
    public static String vnp_PayUrl;
    public static String vnp_TmnCode;
    public static String vnp_HashSecret;
    public static String vnp_ReturnUrl;

    @Value("${vnp.pay-url}")
    public void setPayUrl(String payUrl) {
        vnp_PayUrl = payUrl;
    }

    @Value("${vnp.tmn-code}")
    public void setTmnCode(String tmnCode) {
        vnp_TmnCode = tmnCode;
    }

    @Value("${vnp.hash-secret}")
    public void setHashSecret(String hashSecret) {
        vnp_HashSecret = hashSecret;
    }

    @Value("${vnp.return-url}")
    public void setReturnUrl(String returnUrl) {
        vnp_ReturnUrl = returnUrl;
    }

    /**
     * [THUẬT TOÁN BĂM CHỮ KÝ SỐ BẢO MẬT HMAC-SHA512]
     *
     * @param key Khóa bí mật (Hash Secret)
     * @param data Chuỗi dữ liệu các tham số đã sắp xếp theo thứ tự alphabet
     * @return Chuỗi Hex mã băm SecureHash 128 ký tự
     */
    public static String hmacSHA512(final String key, final String data) {
        try {
            if (key == null || data == null) {
                throw new NullPointerException();
            }
            final Mac hmac512 = Mac.getInstance("HmacSHA512");
            byte[] hmacKeyBytes = key.getBytes(StandardCharsets.UTF_8);
            final SecretKeySpec secretKey = new SecretKeySpec(hmacKeyBytes, "HmacSHA512");
            hmac512.init(secretKey);
            byte[] dataBytes = data.getBytes(StandardCharsets.UTF_8);
            byte[] result = hmac512.doFinal(dataBytes);
            StringBuilder sb = new StringBuilder(2 * result.length);
            for (byte b : result) {
                sb.append(String.format("%02x", b & 0xff));
            }
            return sb.toString();
        } catch (Exception e) {
            return "Something wrong here";
        }
    }

    // Lấy IP Client thực hiện giao dịch
    public static String getIpAddress(HttpServletRequest request) {
        String ipAddress;
        try {
            ipAddress = request.getHeader("X-FORWARDED-FOR");
            if (ipAddress == null || ipAddress.isEmpty() || "unknown".equalsIgnoreCase(ipAddress)) {
                ipAddress = request.getRemoteAddr();
            }
        } catch (Exception e) {
            ipAddress = "127.0.0.1";
        }
        if (ipAddress == null || ipAddress.isEmpty() || "0:0:0:0:0:0:0:1".equals(ipAddress)) {
            ipAddress = "127.0.0.1";
        }
        return ipAddress;
    }

    // Tạo chuỗi số ngẫu nhiên cho mã tham chiếu giao dịch (vnp_TxnRef)
    public static String getRandomNumber(int len) {
        Random rnd = new Random();
        String chars = "0123456789";
        StringBuilder sb = new StringBuilder(len);
        for (int i = 0; i < len; i++) {
            sb.append(chars.charAt(rnd.nextInt(chars.length())));
        }
        return sb.toString();
    }
}
