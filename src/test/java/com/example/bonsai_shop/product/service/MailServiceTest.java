package com.example.bonsai_shop.product.service;

import com.example.bonsai_shop.entity.Order;
import com.example.bonsai_shop.entity.OrderDetail;
import com.example.bonsai_shop.entity.Product;

import jakarta.mail.internet.MimeMessage;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MailServiceTest {

    private JavaMailSender mailSender;
    private MailService mailService;
    private MimeMessage mimeMessage;

    @BeforeEach
    void setUp() {
        mailSender = mock(JavaMailSender.class);
        mimeMessage = mock(MimeMessage.class);
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        mailService = new MailService(mailSender);
        ReflectionTestUtils.setField(mailService, "fromEmail", "test@bonsai.com");
        ReflectionTestUtils.setField(mailService, "baseUrl", "http://localhost:8080");
    }

    // =========================================================================
    // Group 1: sendOrderCreatedEmail
    // =========================================================================

    @Test
    @DisplayName("UT-UUT12-001: sendOrderCreatedEmail - customerEmail null hoặc blank -> bỏ qua không gửi")
    void sendOrderCreatedEmail_nullOrBlankEmail_doesNotSend() {
        Order nullEmailOrder = Order.builder().orderCode("BSMS-001").customerEmail(null).build();
        mailService.sendOrderCreatedEmail(nullEmailOrder);

        Order blankEmailOrder = Order.builder().orderCode("BSMS-002").customerEmail("   ").build();
        mailService.sendOrderCreatedEmail(blankEmailOrder);

        verify(mailSender, never()).createMimeMessage();
        verify(mailSender, never()).send(any(MimeMessage.class));
    }

    @Test
    @DisplayName("UT-UUT12-002: sendOrderCreatedEmail - Email hợp lệ -> gửi mail ghi nhận đơn hàng thành công")
    void sendOrderCreatedEmail_validEmail_sendsEmailSuccessfully() {
        Order order = createSampleOrder("BSMS-CREATED-001", "customer@example.com");

        mailService.sendOrderCreatedEmail(order);

        verify(mailSender, times(1)).createMimeMessage();
        verify(mailSender, times(1)).send(mimeMessage);
    }

    @Test
    @DisplayName("UT-UUT12-003: sendOrderCreatedEmail - orderDetails null hoặc rỗng -> gửi mail với template rỗng chi tiết")
    void sendOrderCreatedEmail_nullOrderDetails_sendsEmailWithFallbackTemplate() {
        Order order = Order.builder()
                .orderCode("BSMS-NO-DETAILS")
                .customerName("Khách Hàng")
                .customerEmail("customer@example.com")
                .totalAmount(new BigDecimal("500000"))
                .orderDetails(null)
                .build();

        mailService.sendOrderCreatedEmail(order);

        verify(mailSender, times(1)).send(mimeMessage);
    }

    // =========================================================================
    // Group 2: sendOrderApprovedEmail
    // =========================================================================

    @Test
    @DisplayName("UT-UUT12-004: sendOrderApprovedEmail - customerEmail null hoặc blank -> bỏ qua")
    void sendOrderApprovedEmail_nullOrBlankEmail_doesNotSend() {
        Order nullEmailOrder = Order.builder().orderCode("BSMS-APP-001").customerEmail(null).build();
        mailService.sendOrderApprovedEmail(nullEmailOrder);

        verify(mailSender, never()).send(any(MimeMessage.class));
    }

    @Test
    @DisplayName("UT-UUT12-005: sendOrderApprovedEmail - Email hợp lệ, gửi lần đầu -> gửi thành công")
    void sendOrderApprovedEmail_validFirstTime_sendsEmail() {
        Order order = createSampleOrder("BSMS-APP-100", "app@example.com");

        mailService.sendOrderApprovedEmail(order);

        verify(mailSender, times(1)).send(mimeMessage);
    }

    @Test
    @DisplayName("UT-UUT12-006: sendOrderApprovedEmail - Suppression Rule: Gửi lần 2 trong 60s -> bỏ qua lần 2")
    void sendOrderApprovedEmail_suppressionRule_suppressesSecondSend() {
        Order order = createSampleOrder("BSMS-SUPPRESS-01", "suppress@example.com");

        mailService.sendOrderApprovedEmail(order); // Lần 1
        mailService.sendOrderApprovedEmail(order); // Lần 2 (trong 60s)

        // mailSender.send chỉ được gọi 1 lần ở lượt thứ 1
        verify(mailSender, times(1)).send(mimeMessage);
    }

    // =========================================================================
    // Group 3: sendOrderDepositedEmail
    // =========================================================================

    @Test
    @DisplayName("UT-UUT12-007: sendOrderDepositedEmail - customerEmail null hoặc blank -> bỏ qua")
    void sendOrderDepositedEmail_nullOrBlankEmail_doesNotSend() {
        Order order = Order.builder().orderCode("BSMS-DEP-001").customerEmail("   ").build();
        mailService.sendOrderDepositedEmail(order);

        verify(mailSender, never()).send(any(MimeMessage.class));
    }

    @Test
    @DisplayName("UT-UUT12-008: sendOrderDepositedEmail - Email hợp lệ -> gửi mail xác nhận cọc thành công")
    void sendOrderDepositedEmail_validEmail_sendsEmail() {
        Order order = createSampleOrder("BSMS-DEP-100", "dep@example.com");
        order.setDepositAmount(new BigDecimal("200000"));

        mailService.sendOrderDepositedEmail(order);

        verify(mailSender, times(1)).send(mimeMessage);
    }

    // =========================================================================
    // Group 4: sendOrderFinalReceiptEmail
    // =========================================================================

    @Test
    @DisplayName("UT-UUT12-009: sendOrderFinalReceiptEmail - customerEmail null hoặc blank -> bỏ qua")
    void sendOrderFinalReceiptEmail_nullOrBlankEmail_doesNotSend() {
        Order order = Order.builder().orderCode("BSMS-FINAL-001").customerEmail(null).build();
        mailService.sendOrderFinalReceiptEmail(order);

        verify(mailSender, never()).send(any(MimeMessage.class));
    }

    @Test
    @DisplayName("UT-UUT12-010: sendOrderFinalReceiptEmail - Email hợp lệ -> gửi mail hóa đơn hoàn tất")
    void sendOrderFinalReceiptEmail_validEmail_sendsEmail() {
        Order order = createSampleOrder("BSMS-FINAL-100", "final@example.com");

        mailService.sendOrderFinalReceiptEmail(order);

        verify(mailSender, times(1)).send(mimeMessage);
    }

    // =========================================================================
    // Group 5: sendInPersonOrderPaidEmail
    // =========================================================================

    @Test
    @DisplayName("UT-UUT12-011: sendInPersonOrderPaidEmail - customerEmail null hoặc blank -> bỏ qua")
    void sendInPersonOrderPaidEmail_nullOrBlankEmail_doesNotSend() {
        Order order = Order.builder().orderCode("BSMS-INPERSON-001").customerEmail("").build();
        mailService.sendInPersonOrderPaidEmail(order);

        verify(mailSender, never()).send(any(MimeMessage.class));
    }

    @Test
    @DisplayName("UT-UUT12-012: sendInPersonOrderPaidEmail - Email hợp lệ -> gửi mail xác nhận tại cửa hàng")
    void sendInPersonOrderPaidEmail_validEmail_sendsEmail() {
        Order order = createSampleOrder("BSMS-INPERSON-100", "inperson@example.com");

        mailService.sendInPersonOrderPaidEmail(order);

        verify(mailSender, times(1)).send(mimeMessage);
    }

    // =========================================================================
    // Group 6: sendOrderRejectedEmail
    // =========================================================================

    @Test
    @DisplayName("UT-UUT12-013: sendOrderRejectedEmail - customerEmail null hoặc blank -> bỏ qua")
    void sendOrderRejectedEmail_nullOrBlankEmail_doesNotSend() {
        Order order = Order.builder().orderCode("BSMS-REJ-001").customerEmail(null).build();
        mailService.sendOrderRejectedEmail(order, "Quá hạn thanh toán");

        verify(mailSender, never()).send(any(MimeMessage.class));
    }

    @Test
    @DisplayName("UT-UUT12-014: sendOrderRejectedEmail - Email hợp lệ -> gửi mail thông báo hủy đơn kèm lý do")
    void sendOrderRejectedEmail_validEmail_sendsEmailWithReason() {
        Order order = createSampleOrder("BSMS-REJ-100", "rejected@example.com");

        mailService.sendOrderRejectedEmail(order, "Hết hàng trong kho");

        verify(mailSender, times(1)).send(mimeMessage);
    }

    // =========================================================================
    // Group 7: Mail Sender Retry Mechanism & Dependency Exception
    // =========================================================================

    @Test
    @DisplayName("UT-UUT12-015: sendHtmlEmailWithRetry - Ném exception lần 1, gửi thành công ở lần 2 (Retry Success)")
    void sendHtmlEmailWithRetry_failsFirstTimeSucceedsSecondTime_retriesAndSucceeds() {
        Order order = createSampleOrder("BSMS-RETRY-001", "retry@example.com");

        doThrow(new RuntimeException("Connection Timeout"))
                .doNothing()
                .when(mailSender).send(any(MimeMessage.class));

        mailService.sendOrderCreatedEmail(order);

        verify(mailSender, times(2)).send(mimeMessage);
    }

    @Test
    @DisplayName("UT-UUT12-016: sendHtmlEmailWithRetry - Ném exception cả 3 lần (MAX_RETRIES) -> Thử đủ 3 lần, nuốt exception an toàn")
    void sendHtmlEmailWithRetry_failsAllThreeTimes_swallowsExceptionSafely() {
        Order order = createSampleOrder("BSMS-RETRY-002", "failall@example.com");

        doThrow(new RuntimeException("SMTP Server Down"))
                .when(mailSender).send(any(MimeMessage.class));

        mailService.sendOrderCreatedEmail(order);

        verify(mailSender, times(3)).send(mimeMessage);
    }

    // =========================================================================
    // Group 8: Template Builder Assertions
    // =========================================================================

    @Test
    @DisplayName("UT-UUT12-017: Kiểm tra email phê duyệt đơn đặt cọc với bộ dữ liệu chuẩn của người dùng")
    void testBuildApprovedTemplate_DepositOrder_CalculatesAmountsCorrectly() {
        Product p = Product.builder().productName("Tác phẩm Bonsai Siêu Cổ").price(new BigDecimal("100000000")).build();
        OrderDetail detail = OrderDetail.builder().product(p).quantity(1).priceAtPurchase(new BigDecimal("100000000")).build();

        Order order = Order.builder()
                .orderCode("BSMS-TEST-001")
                .customerName("Nguyễn Văn A")
                .customerEmail("khachhang@example.com")
                .depositAmount(new BigDecimal("12000000"))
                .shippingFee(new BigDecimal("363636"))
                .craneFee(new BigDecimal("636336"))
                .totalAmount(new BigDecimal("100999972"))
                .orderDetails(List.of(detail))
                .build();

        String html = (String) ReflectionTestUtils.invokeMethod(mailService, "buildAprovedTemplate", order, "http://localhost:8080/vnpay/pay-order?orderCode=BSMS-TEST-001");

        assertThat(html).contains("1. GIÁ TRỊ ĐƠN HÀNG");
        assertThat(html).contains("100.000.000 VND");
        assertThat(html).contains("363.636 VND");
        assertThat(html).contains("636.336 VND");
        assertThat(html).contains("100.999.972 VND");

        assertThat(html).contains("2. THANH TOÁN ĐẶT CỌC QUA VNPAY");
        assertThat(html).contains("KHÁCH CẦN THANH TOÁN NGAY:");
        assertThat(html).contains("TIẾN HÀNH THANH TOÁN (12.000.000 VND)");

        assertThat(html).contains("3. THANH TOÁN KHI NHẬN CÂY (PHẦN CÒN LẠI)");
        assertThat(html).contains("Phần còn lại của giá cây:");
        assertThat(html).contains("88.000.000 VND");
        assertThat(html).contains("TỔNG THANH TOÁN KHI NHẬN CÂY:");
        assertThat(html).contains("88.999.972 VND");
    }

    @Test
    @DisplayName("UT-UUT12-018: Kiểm tra email phê duyệt đơn thanh toán 100% không bị ảnh hưởng")
    void testBuildApprovedTemplate_FullPaymentOrder_RemainsCorrect() {
        Product p = Product.builder().productName("Cây Tùng Nhật").price(new BigDecimal("50000000")).build();
        OrderDetail detail = OrderDetail.builder().product(p).quantity(1).priceAtPurchase(new BigDecimal("50000000")).build();

        Order order = Order.builder()
                .orderCode("BSMS-FULL-002")
                .customerName("Trần Văn B")
                .depositAmount(BigDecimal.ZERO)
                .shippingFee(new BigDecimal("500000"))
                .craneFee(new BigDecimal("1000000"))
                .totalAmount(new BigDecimal("51500000"))
                .orderDetails(List.of(detail))
                .build();

        String html = (String) ReflectionTestUtils.invokeMethod(mailService, "buildAprovedTemplate", order, "http://localhost:8080/vnpay/pay-order?orderCode=BSMS-FULL-002");

        assertThat(html).contains("THANH TOÁN 100% QUA VNPAY:");
        assertThat(html).contains("51.500.000 VND");
        assertThat(html).contains("TIẾN HÀNH THANH TOÁN (51.500.000 VND)");
    }

    // =========================================================================
    // Helper Fixture Builders
    // =========================================================================

    private Order createSampleOrder(String orderCode, String email) {
        Product product = Product.builder().productId(1).productName("Cây Sanh Cổ").price(new BigDecimal("5000000")).build();
        OrderDetail detail = OrderDetail.builder().orderDetailId(1).product(product).quantity(1).priceAtPurchase(new BigDecimal("5000000")).build();

        return Order.builder()
                .orderId(100)
                .orderCode(orderCode)
                .customerName("Nguyễn Văn Test")
                .customerEmail(email)
                .shippingAddress("123 Phố Cổ, Hà Nội")
                .depositAmount(new BigDecimal("1000000"))
                .shippingFee(new BigDecimal("200000"))
                .craneFee(new BigDecimal("300000"))
                .totalAmount(new BigDecimal("5500000"))
                .orderDetails(List.of(detail))
                .build();
    }
}
