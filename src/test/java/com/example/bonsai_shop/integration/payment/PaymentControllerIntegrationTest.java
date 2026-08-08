package com.example.bonsai_shop.integration.payment;

import com.example.bonsai_shop.config.VNPayConfig;
import com.example.bonsai_shop.entity.*;
import com.example.bonsai_shop.integration.support.BaseControllerIntegrationTest;
import com.example.bonsai_shop.product.repository.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

public class PaymentControllerIntegrationTest extends BaseControllerIntegrationTest {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private VarietyRepository varietyRepository;

    @Autowired
    private ProductSegmentRepository productSegmentRepository;

    private Product createTestProduct(String code, String name, BigDecimal price) {
        Category category = categoryRepository.findAll().stream().findFirst()
                .orElseGet(() -> categoryRepository.save(Category.builder().categoryName("General Category").build()));
        Variety variety = varietyRepository.findAll().stream().findFirst()
                .orElseGet(() -> varietyRepository.save(Variety.builder().category(category).varietyName("General Variety").build()));
        ProductSegment segment = productSegmentRepository.findAll().stream().findFirst()
                .orElseGet(() -> productSegmentRepository.save(ProductSegment.builder().segmentName("General Segment").build()));

        Product product = new Product();
        product.setProductCode(code);
        product.setProductName(name);
        product.setPrice(price);
        product.setProductStatus("AVAILABLE");
        product.setVariety(variety);
        product.setSegment(segment);
        return productRepository.save(product);
    }

    @DisplayName("TC-IT-PAY-01: Legacy Candidate Test - GET /vnpay/create-payment")
    @Test
    void testCreatePaymentLegacy() throws Exception {
        Product product = createTestProduct("TREE-PAY-01", "Cây Lẻ Legacy", new BigDecimal("500000"));

        mockMvc.perform(MockMvcRequestBuilders.get("/vnpay/create-payment")
                        .param("productId", product.getProductId().toString()))
                .andExpect(MockMvcResultMatchers.status().is3xxRedirection());
    }

    @DisplayName("TC-IT-PAY-02: GET /vnpay/pay-order sinh URL Redirect thanh toán VNPay")
    @Test
    void testPayOrderRedirect() throws Exception {
        Order order = new Order();
        order.setOrderCode("ORD-VNPAY-02");
        order.setOrderStatus("PENDING_PAYMENT");
        order.setTotalAmount(new BigDecimal("1000000"));
        order.setDepositAmount(new BigDecimal("200000"));
        order.setOrderDate(LocalDateTime.now());
        Order savedOrder = orderRepository.save(order);

        Payment payment = new Payment();
        payment.setOrder(savedOrder);
        payment.setAmount(new BigDecimal("200000"));
        payment.setPaymentStatus("PENDING");
        payment.setPaymentType("DEPOSIT");
        paymentRepository.save(payment);

        mockMvc.perform(MockMvcRequestBuilders.get("/vnpay/pay-order")
                        .param("orderCode", "ORD-VNPAY-02"))
                .andExpect(MockMvcResultMatchers.status().is3xxRedirection());
    }

    @DisplayName("TC-IT-PAY-03: GET /vnpay/payment-callback xử lý trả về thành công")
    @Test
    void testPaymentCallbackSuccess() throws Exception {
        Order order = new Order();
        order.setOrderCode("ORD-VNPAY-03");
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
        params.put("vnp_OrderInfo", "Thanh toan don hang ORD-VNPAY-03");
        params.put("vnp_PayDate", "20260808160000");
        params.put("vnp_ResponseCode", "00");
        params.put("vnp_TmnCode", "TEST_TMN_CODE");
        params.put("vnp_TransactionNo", "14000000");
        params.put("vnp_TransactionStatus", "00");
        params.put("vnp_TxnRef", "ORD-VNPAY-03");

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
        String secureHash = VNPayConfig.hmacSHA512(VNPayConfig.vnp_HashSecret, sb.toString());

        mockMvc.perform(MockMvcRequestBuilders.get("/vnpay/payment-callback")
                        .param("vnp_Amount", "100000000")
                        .param("vnp_BankCode", "NCB")
                        .param("vnp_CardType", "ATM")
                        .param("vnp_OrderInfo", "Thanh toan don hang ORD-VNPAY-03")
                        .param("vnp_PayDate", "20260808160000")
                        .param("vnp_ResponseCode", "00")
                        .param("vnp_TmnCode", "TEST_TMN_CODE")
                        .param("vnp_TransactionNo", "14000000")
                        .param("vnp_TransactionStatus", "00")
                        .param("vnp_TxnRef", "ORD-VNPAY-03")
                        .param("vnp_SecureHash", secureHash))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.view().name("payment-result"))
                .andExpect(MockMvcResultMatchers.model().attribute("status", "SUCCESS"));
    }

    @DisplayName("TC-IT-PAY-04: GET /vnpay/payment-callback xử lý khách hủy thanh toán (code 24)")
    @Test
    void testPaymentCallbackUserCancelled() throws Exception {
        Order order = new Order();
        order.setOrderCode("ORD-VNPAY-04");
        order.setOrderStatus("PENDING_PAYMENT");
        order.setTotalAmount(new BigDecimal("1000000"));
        order.setOrderDate(LocalDateTime.now());
        orderRepository.save(order);

        Map<String, String> params = new TreeMap<>();
        params.put("vnp_Amount", "100000000");
        params.put("vnp_ResponseCode", "24");
        params.put("vnp_TransactionStatus", "02");
        params.put("vnp_TxnRef", "ORD-VNPAY-04");

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
        String secureHash = VNPayConfig.hmacSHA512(VNPayConfig.vnp_HashSecret, sb.toString());

        mockMvc.perform(MockMvcRequestBuilders.get("/vnpay/payment-callback")
                        .param("vnp_Amount", "100000000")
                        .param("vnp_ResponseCode", "24")
                        .param("vnp_TransactionStatus", "02")
                        .param("vnp_TxnRef", "ORD-VNPAY-04")
                        .param("vnp_SecureHash", secureHash))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.view().name("payment-result"))
                .andExpect(MockMvcResultMatchers.model().attribute("status", "FAILED"));
    }

    @DisplayName("TC-IT-PAY-05: GET /vnpay/payment-callback chữ ký Checksum sai (INVALID_SIGNATURE)")
    @Test
    void testPaymentCallbackInvalidSignature() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/vnpay/payment-callback")
                        .param("vnp_Amount", "100000000")
                        .param("vnp_ResponseCode", "00")
                        .param("vnp_TxnRef", "ORD-FAKE")
                        .param("vnp_SecureHash", "WRONG_INVALID_HASH_VALUE"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.view().name("payment-result"))
                .andExpect(MockMvcResultMatchers.model().attribute("status", "INVALID_SIGNATURE"));
    }
}
