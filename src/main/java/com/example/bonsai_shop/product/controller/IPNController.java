package com.example.bonsai_shop.product.controller;

import com.example.bonsai_shop.config.VNPayConfig;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

@RestController
public class IPNController {

    @GetMapping("/vnpay/ipn")
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
                    sb.append(fieldName);
                    sb.append('=');
                    sb.append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII.toString()).replace("+", "%20"));
                    if (itr.hasNext()) {
                        sb.append('&');
                    }
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
            }
        }

        String signValue = VNPayConfig.hmacSHA512(VNPayConfig.vnp_HashSecret, sb.toString());

        if (signValue.equals(vnp_SecureHash)) {
            // TODO: BƯỚC XỬ LÝ NGHIỆP VỤ CỦA HỆ THỐNG
            // 1. Kiểm tra mã đơn hàng (vnp_TxnRef) có tồn tại trong Database hay không
            // 2. Kiểm tra số tiền thanh toán (vnp_Amount) khớp với giá trị đơn hàng trong DB
            // 3. Kiểm tra trạng thái đơn hàng hiện tại (chỉ cập nhật nếu đơn hàng ở trạng thái chờ thanh toán)

            boolean checkOrderId = true; // Giả sử đơn hàng tồn tại
            boolean checkAmount = true;  // Giả sử số tiền khớp
            boolean checkOrderStatus = true; // Giả sử đơn hàng chưa được xử lý thanh toán trước đó

            if (checkOrderId) {
                if (checkAmount) {
                    if (checkOrderStatus) {
                        String responseCode = request.getParameter("vnp_ResponseCode");
                        if ("00".equals(responseCode)) {
                            // Cập nhật trạng thái ĐÃ THANH TOÁN thành công cho đơn hàng trong DB
                        } else {
                            // Cập nhật trạng thái THANH TOÁN THẤT BẠI cho đơn hàng trong DB
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
}