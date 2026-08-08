package com.example.bonsai_shop.integration.job;

import com.example.bonsai_shop.entity.*;
import com.example.bonsai_shop.integration.support.AbstractDatabaseSafeIntegrationTest;
import com.example.bonsai_shop.product.repository.*;
import com.example.bonsai_shop.product.service.OrderExpirationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@Transactional
public class OrderExpirationServiceIntegrationTest extends AbstractDatabaseSafeIntegrationTest {

    @Autowired
    private OrderExpirationService orderExpirationService;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private OrderDetailRepository orderDetailRepository;

    @Autowired
    private OrderHandlingRepository orderHandlingRepository;

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
        product.setProductStatus("RESERVED");
        product.setVariety(variety);
        product.setSegment(segment);
        return productRepository.save(product);
    }

    @DisplayName("TC-IT-JOB-01: Exceeds 15m Online Order Expiration & Inventory Release")
    @Test
    void testCancelExpiredOnlineOrders() {
        Product product = createTestProduct("TREE-JOB-01", "Cây Online Quá Hạn", new BigDecimal("1000000"));

        Order order = new Order();
        order.setOrderCode("ORD-JOB-01");
        order.setOrderStatus("PENDING");
        order.setOrderType("ONLINE");
        order.setOrderDate(LocalDateTime.now().minusMinutes(20)); // Exceeds 15 mins
        Order savedOrder = orderRepository.save(order);

        OrderDetail detail = OrderDetail.builder()
                .order(savedOrder)
                .product(product)
                .quantity(1)
                .priceAtPurchase(new BigDecimal("1000000"))
                .build();
        orderDetailRepository.save(detail);
        savedOrder.setOrderDetails(List.of(detail));

        Payment payment = new Payment();
        payment.setOrder(savedOrder);
        payment.setAmount(new BigDecimal("1000000"));
        payment.setPaymentStatus("PENDING");
        payment.setPaymentType("FULL");
        paymentRepository.save(payment);

        OrderHandling handling = OrderHandling.builder()
                .order(savedOrder)
                .handledAt(LocalDateTime.now().minusMinutes(20))
                .isActive(true)
                .build();
        orderHandlingRepository.save(handling);

        // Execute Background Job directly
        orderExpirationService.cancelExpiredOrders();

        Order updatedOrder = orderRepository.findByOrderCode("ORD-JOB-01").orElseThrow();
        assertEquals("CANCELLED", updatedOrder.getOrderStatus());

        Product updatedProduct = productRepository.findById(product.getProductId()).orElseThrow();
        assertEquals("AVAILABLE", updatedProduct.getProductStatus());

        Payment updatedPayment = paymentRepository.findById(payment.getPaymentId()).orElseThrow();
        assertEquals("EXPIRED", updatedPayment.getPaymentStatus());

        OrderHandling updatedHandling = orderHandlingRepository.findById(handling.getOrderHandlingId()).orElseThrow();
        assertFalse(updatedHandling.getIsActive());
        assertNotNull(updatedHandling.getReleasedAt());
    }

    @DisplayName("TC-IT-JOB-02: Exceeds 48h Offline Order Expiration & Inventory Release")
    @Test
    void testCancelExpiredOfflineOrders() {
        Product product = createTestProduct("TREE-JOB-02", "Cây Offline Quá Hạn", new BigDecimal("2000000"));

        Order order = new Order();
        order.setOrderCode("ORD-JOB-02");
        order.setOrderStatus("PENDING_PAYMENT");
        order.setOrderType("IN_PERSON");
        order.setOrderDate(LocalDateTime.now().minusHours(50)); // Exceeds 48 hours
        Order savedOrder = orderRepository.save(order);

        OrderDetail detail = OrderDetail.builder()
                .order(savedOrder)
                .product(product)
                .quantity(1)
                .priceAtPurchase(new BigDecimal("2000000"))
                .build();
        orderDetailRepository.save(detail);
        savedOrder.setOrderDetails(List.of(detail));

        orderExpirationService.cancelExpiredOrders();

        Order updatedOrder = orderRepository.findByOrderCode("ORD-JOB-02").orElseThrow();
        assertEquals("CANCELLED", updatedOrder.getOrderStatus());

        Product updatedProduct = productRepository.findById(product.getProductId()).orElseThrow();
        assertEquals("AVAILABLE", updatedProduct.getProductStatus());
    }

    @DisplayName("TC-IT-JOB-03: Under Threshold / No Eligible Orders - Job completes silently")
    @Test
    void testCancelExpiredOrdersUnderThreshold() {
        Product product = createTestProduct("TREE-JOB-03", "Cây Chưa Quá Hạn", new BigDecimal("1500000"));

        Order order = new Order();
        order.setOrderCode("ORD-JOB-03");
        order.setOrderStatus("PENDING");
        order.setOrderType("ONLINE");
        order.setOrderDate(LocalDateTime.now().minusMinutes(5)); // Under 15 mins
        Order savedOrder = orderRepository.save(order);

        OrderDetail detail = OrderDetail.builder()
                .order(savedOrder)
                .product(product)
                .quantity(1)
                .priceAtPurchase(new BigDecimal("1500000"))
                .build();
        orderDetailRepository.save(detail);
        savedOrder.setOrderDetails(List.of(detail));

        orderExpirationService.cancelExpiredOrders();

        Order updatedOrder = orderRepository.findByOrderCode("ORD-JOB-03").orElseThrow();
        assertEquals("PENDING", updatedOrder.getOrderStatus());

        Product updatedProduct = productRepository.findById(product.getProductId()).orElseThrow();
        assertEquals("RESERVED", updatedProduct.getProductStatus());
    }
}
