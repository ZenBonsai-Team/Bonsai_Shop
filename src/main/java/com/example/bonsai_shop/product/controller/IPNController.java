package com.example.bonsai_shop.product.controller;

import com.example.bonsai_shop.config.VNPayConfig;
import com.example.bonsai_shop.entity.Order;
import com.example.bonsai_shop.entity.OrderDetail;
import com.example.bonsai_shop.entity.Product;
import com.example.bonsai_shop.product.repository.OrderRepository;
import com.example.bonsai_shop.product.repository.ProductRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

@RestController
public class IPNController {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private ProductRepository productRepository;

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
                    if (itr.hasNext()) sb.append('&');
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
                long expectedAmount = order.getTotalAmount().longValue() * 100;
                checkAmount = Math.abs(vnpAmount - expectedAmount) < 1000;
                checkOrderStatus = !"PAID".equalsIgnoreCase(order.getOrderStatus());
            }

            if (checkOrderId) {
                if (checkAmount) {
                    if (checkOrderStatus) {
                        String responseCode = request.getParameter("vnp_ResponseCode");
                        if ("00".equals(responseCode)) {
                            order.setOrderStatus("PAID");
                            orderRepository.save(order);

                            if (order.getOrderDetails() != null) {
                                for (OrderDetail detail : order.getOrderDetails()) {
                                    Product prod = detail.getProduct();
                                    if (prod != null) {
                                        prod.setProductStatus("SOLD");
                                        productRepository.save(prod);
                                    }
                                }
                            }
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