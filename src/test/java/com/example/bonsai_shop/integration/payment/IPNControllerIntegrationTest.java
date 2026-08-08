package com.example.bonsai_shop.integration.payment;

import com.example.bonsai_shop.config.VNPayConfig;
import com.example.bonsai_shop.entity.Order;
import com.example.bonsai_shop.entity.Payment;
import com.example.bonsai_shop.integration.support.BaseControllerIntegrationTest;
import com.example.bonsai_shop.product.repository.OrderRepository;
import com.example.bonsai_shop.product.repository.PaymentRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

public class IPNControllerIntegrationTest extends BaseControllerIntegrationTest {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    private String calculateVnPayChecksum(Map<String, String> params) throws Exception {
        StringBuilder sb = new StringBuilder();
        Iterator<String> itr = params.keySet().iterator();
        while (itr.hasNext()) {
            String key = itr.next();
            String val = params.get(key);
            sb.append(key).append('=').append(URLEncoder.encode(val, StandardCharsets.US_ASCII));
            if (itr.hasNext()) {
                sb.append('&');
            }
        }
        return VNPayConfig.hmacSHA512(VNPayConfig.vnp_HashSecret, sb.toString());
    }

    @DisplayName("TC-IT-IPN-01: IPN Webhook xử lý giao dịch thành công (RspCode 00)")
    @Test
    void testIpnSuccess() throws Exception {
        Order order = new Order();
        order.setOrderCode("ORD-IPN-01");
        order.setOrderStatus("PENDING_PAYMENT");
        order.setTotalAmount(new BigDecimal("1000000"));
        order.setOrderDate(LocalDateTime.now());
        Order savedOrder = orderRepository.save(order);

        Payment payment = new Payment();
        payment.setOrder(savedOrder);
        payment.setAmount(new BigDecimal("1000000"));
        payment.setPaymentStatus("PENDING");
        payment.setPaymentType("FULL");
        paymentRepository.save(payment);

        Map<String, String> params = new TreeMap<>();
        params.put("vnp_Amount", "100000000");
        params.put("vnp_BankCode", "NCB");
        params.put("vnp_CardType", "ATM");
        params.put("vnp_OrderInfo", "Thanh toan IPN ORD-IPN-01");
        params.put("vnp_PayDate", "20260808160000");
        params.put("vnp_ResponseCode", "00");
        params.put("vnp_TmnCode", "TEST_TMN_CODE");
        params.put("vnp_TransactionNo", "14000001");
        params.put("vnp_TransactionStatus", "00");
        params.put("vnp_TxnRef", "ORD-IPN-01");

        String secureHash = calculateVnPayChecksum(params);

        mockMvc.perform(MockMvcRequestBuilders.get("/vnpay/ipn")
                        .param("vnp_Amount", "100000000")
                        .param("vnp_BankCode", "NCB")
                        .param("vnp_CardType", "ATM")
                        .param("vnp_OrderInfo", "Thanh toan IPN ORD-IPN-01")
                        .param("vnp_PayDate", "20260808160000")
                        .param("vnp_ResponseCode", "00")
                        .param("vnp_TmnCode", "TEST_TMN_CODE")
                        .param("vnp_TransactionNo", "14000001")
                        .param("vnp_TransactionStatus", "00")
                        .param("vnp_TxnRef", "ORD-IPN-01")
                        .param("vnp_SecureHash", secureHash))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.RspCode").value("00"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.Message").value("Confirm Success"));
    }

    @DisplayName("TC-IT-IPN-02: IPN Webhook gửi sai chữ ký bảo mật Checksum (RspCode 97)")
    @Test
    void testIpnInvalidChecksum() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/vnpay/ipn")
                        .param("vnp_Amount", "100000000")
                        .param("vnp_ResponseCode", "00")
                        .param("vnp_TxnRef", "ORD-IPN-02")
                        .param("vnp_SecureHash", "INVALID_CHECKSUM_SIGNATURE"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.RspCode").value("97"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.Message").value("Invalid Checksum"));
    }

    @DisplayName("TC-IT-IPN-03: IPN Webhook gửi sai số tiền vnp_Amount (RspCode 04)")
    @Test
    void testIpnInvalidAmount() throws Exception {
        Order order = new Order();
        order.setOrderCode("ORD-IPN-03");
        order.setOrderStatus("PENDING_PAYMENT");
        order.setTotalAmount(new BigDecimal("1000000"));
        order.setOrderDate(LocalDateTime.now());
        Order savedOrder = orderRepository.save(order);

        Payment payment = new Payment();
        payment.setOrder(savedOrder);
        payment.setAmount(new BigDecimal("1000000"));
        payment.setPaymentStatus("PENDING");
        payment.setPaymentType("FULL");
        paymentRepository.save(payment);

        Map<String, String> params = new TreeMap<>();
        params.put("vnp_Amount", "50000000"); // 500k instead of 1M
        params.put("vnp_ResponseCode", "00");
        params.put("vnp_TransactionStatus", "00");
        params.put("vnp_TxnRef", "ORD-IPN-03");

        String secureHash = calculateVnPayChecksum(params);

        mockMvc.perform(MockMvcRequestBuilders.get("/vnpay/ipn")
                        .param("vnp_Amount", "50000000")
                        .param("vnp_ResponseCode", "00")
                        .param("vnp_TransactionStatus", "00")
                        .param("vnp_TxnRef", "ORD-IPN-03")
                        .param("vnp_SecureHash", secureHash))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.RspCode").value("04"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.Message").value("Invalid Amount"));
    }

    @DisplayName("TC-IT-IPN-04: IPN Webhook gửi đơn hàng không tồn tại (RspCode 01)")
    @Test
    void testIpnOrderNotFound() throws Exception {
        Map<String, String> params = new TreeMap<>();
        params.put("vnp_Amount", "100000000");
        params.put("vnp_ResponseCode", "00");
        params.put("vnp_TransactionStatus", "00");
        params.put("vnp_TxnRef", "ORD-NOT-EXIST");

        String secureHash = calculateVnPayChecksum(params);

        mockMvc.perform(MockMvcRequestBuilders.get("/vnpay/ipn")
                        .param("vnp_Amount", "100000000")
                        .param("vnp_ResponseCode", "00")
                        .param("vnp_TransactionStatus", "00")
                        .param("vnp_TxnRef", "ORD-NOT-EXIST")
                        .param("vnp_SecureHash", secureHash))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.RspCode").value("01"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.Message").value("Order not Found"));
    }

    @DisplayName("TC-IT-IPN-05: IPN Webhook trùng lặp khi đơn đã PAID (RspCode 02)")
    @Test
    void testIpnDuplicateAlreadyConfirmed() throws Exception {
        Order order = new Order();
        order.setOrderCode("ORD-IPN-05");
        order.setOrderStatus("PAID");
        order.setTotalAmount(new BigDecimal("1000000"));
        order.setOrderDate(LocalDateTime.now());
        orderRepository.save(order);

        Map<String, String> params = new TreeMap<>();
        params.put("vnp_Amount", "100000000");
        params.put("vnp_ResponseCode", "00");
        params.put("vnp_TransactionStatus", "00");
        params.put("vnp_TxnRef", "ORD-IPN-05");

        String secureHash = calculateVnPayChecksum(params);

        mockMvc.perform(MockMvcRequestBuilders.get("/vnpay/ipn")
                        .param("vnp_Amount", "100000000")
                        .param("vnp_ResponseCode", "00")
                        .param("vnp_TransactionStatus", "00")
                        .param("vnp_TxnRef", "ORD-IPN-05")
                        .param("vnp_SecureHash", secureHash))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.RspCode").value("02"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.Message").value("Order already confirmed"));
    }
}
