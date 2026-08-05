package com.example.bonsai_shop.product.service;

import com.example.bonsai_shop.entity.Order;
import com.example.bonsai_shop.entity.OrderDetail;
import com.example.bonsai_shop.entity.Product;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class MailServiceTest {

    private MailService mailService;

    @BeforeEach
    void setUp() {
        JavaMailSender mailSender = mock(JavaMailSender.class);
        mailService = new MailService(mailSender);
        ReflectionTestUtils.setField(mailService, "fromEmail", "test@bonsai.com");
        ReflectionTestUtils.setField(mailService, "baseUrl", "http://localhost:8080");
    }

    @Test
    @DisplayName("Kiểm tra email phê duyệt đơn đặt cọc với bộ dữ liệu chuẩn của người dùng")
    void testBuildApprovedTemplate_DepositOrder_CalculatesAmountsCorrectly() {
        // Arrange
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

        // Act
        String html = (String) ReflectionTestUtils.invokeMethod(mailService, "buildAprovedTemplate", order, "http://localhost:8080/vnpay/pay-order?orderCode=BSMS-TEST-001");

        // Assert
        // Phần 1: Giá trị đơn hàng
        assertThat(html).contains("1. GIÁ TRỊ ĐƠN HÀNG");
        assertThat(html).contains("100.000.000 VND"); // Giá cây
        assertThat(html).contains("363.636 VND");    // Phí vận chuyển
        assertThat(html).contains("636.336 VND");    // Phí xe cẩu
        assertThat(html).contains("100.999.972 VND"); // Tổng giá trị đơn hàng

        // Phần 2: Thanh toán đặt cọc qua VNPay
        assertThat(html).contains("2. THANH TOÁN ĐẶT CỌC QUA VNPAY");
        assertThat(html).contains("KHÁCH CẦN THANH TOÁN NGAY:");
        assertThat(html).contains("TIẾN HÀNH THANH TOÁN (12.000.000 VND)");

        // Phần 3: Thanh toán khi nhận cây (Phần còn lại)
        assertThat(html).contains("3. THANH TOÁN KHI NHẬN CÂY (PHẦN CÒN LẠI)");
        assertThat(html).contains("Phần còn lại của giá cây:");
        assertThat(html).contains("88.000.000 VND"); // 100.000.000 - 12.000.000
        assertThat(html).contains("TỔNG THANH TOÁN KHI NHẬN CÂY:");
        assertThat(html).contains("88.999.972 VND"); // 88.000.000 + 363.636 + 636.336
    }

    @Test
    @DisplayName("Kiểm tra email phê duyệt đơn thanh toán 100% không bị ảnh hưởng")
    void testBuildApprovedTemplate_FullPaymentOrder_RemainsCorrect() {
        // Arrange
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

        // Act
        String html = (String) ReflectionTestUtils.invokeMethod(mailService, "buildAprovedTemplate", order, "http://localhost:8080/vnpay/pay-order?orderCode=BSMS-FULL-002");

        // Assert
        assertThat(html).contains("THANH TOÁN 100% QUA VNPAY:");
        assertThat(html).contains("51.500.000 VND");
        assertThat(html).contains("TIẾN HÀNH THANH TOÁN (51.500.000 VND)");
    }
}
