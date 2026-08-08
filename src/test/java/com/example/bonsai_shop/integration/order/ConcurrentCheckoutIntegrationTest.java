package com.example.bonsai_shop.integration.order;

import com.example.bonsai_shop.customer.repository.RoleRepository;
import com.example.bonsai_shop.customer.repository.UserRepository;
import com.example.bonsai_shop.entity.*;
import com.example.bonsai_shop.integration.support.AbstractDatabaseSafeIntegrationTest;
import com.example.bonsai_shop.product.dto.PurchaseOrderRequestDTO;
import com.example.bonsai_shop.product.repository.*;
import com.example.bonsai_shop.product.service.OrderService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

public class ConcurrentCheckoutIntegrationTest extends AbstractDatabaseSafeIntegrationTest {

    @Autowired
    private OrderService orderService;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private VarietyRepository varietyRepository;

    @Autowired
    private ProductSegmentRepository productSegmentRepository;

    private final String productCode = "TREE-CONCURRENT-07";
    private final String userAEmail = "concA@example.com";
    private final String userBEmail = "concB@example.com";

    private User createTestUser(String email, String roleName) {
        Role role = roleRepository.findByRoleName(roleName)
                .orElseGet(() -> roleRepository.save(Role.builder().roleName(roleName).description(roleName).build()));
        User user = new User();
        user.setEmail(email);
        user.setFullName("Test " + roleName);
        user.setPhone("0987654321");
        user.setPassword("password123");
        user.setRole(role);
        return userRepository.save(user);
    }

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

    @AfterEach
    void cleanupTestData() {
        try {
            Optional<Product> prodOpt = productRepository.findByProductCode(productCode);
            if (prodOpt.isPresent()) {
                Product prod = prodOpt.get();
                List<Order> orders = orderRepository.findAll();
                for (Order o : orders) {
                    if (o.getCustomerEmail() != null && (o.getCustomerEmail().equals(userAEmail) || o.getCustomerEmail().equals(userBEmail))) {
                        orderRepository.delete(o);
                    }
                }
                productRepository.delete(prod);
            }
            userRepository.findByEmail(userAEmail).ifPresent(u -> userRepository.delete(u));
            userRepository.findByEmail(userBEmail).ifPresent(u -> userRepository.delete(u));
        } catch (Exception ignored) {
        }
    }

    @DisplayName("TC-IT-ORD-07: Concurrent Checkout Guard - 2 khách mua cùng 1 cây chỉ 1 đơn thành công")
    @Test
    void testConcurrentCheckoutOneWinnerOnly() throws Exception {
        User userA = createTestUser(userAEmail, "CUSTOMER");
        User userB = createTestUser(userBEmail, "CUSTOMER");
        Product product = createTestProduct(productCode, "Cây Độc Bản Tranh Chấp", new BigDecimal("5000000"));

        PurchaseOrderRequestDTO dtoA = new PurchaseOrderRequestDTO();
        dtoA.setCustomerName("Customer A");
        dtoA.setCustomerPhone("0987654321");
        dtoA.setCustomerEmail(userAEmail);
        dtoA.setShippingAddress("Address A");
        dtoA.setPaymentMethod("COD");
        dtoA.setProductIds(List.of(product.getProductId()));

        PurchaseOrderRequestDTO dtoB = new PurchaseOrderRequestDTO();
        dtoB.setCustomerName("Customer B");
        dtoB.setCustomerPhone("0912345678");
        dtoB.setCustomerEmail(userBEmail);
        dtoB.setShippingAddress("Address B");
        dtoB.setPaymentMethod("COD");
        dtoB.setProductIds(List.of(product.getProductId()));

        ExecutorService executorService = Executors.newFixedThreadPool(2);
        CountDownLatch readyLatch = new CountDownLatch(2);
        CountDownLatch startLatch = new CountDownLatch(1);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        Callable<Void> taskA = () -> {
            readyLatch.countDown();
            startLatch.await();
            try {
                orderService.createOrder(dtoA, userA);
                successCount.incrementAndGet();
            } catch (Exception e) {
                failureCount.incrementAndGet();
            }
            return null;
        };

        Callable<Void> taskB = () -> {
            readyLatch.countDown();
            startLatch.await();
            try {
                orderService.createOrder(dtoB, userB);
                successCount.incrementAndGet();
            } catch (Exception e) {
                failureCount.incrementAndGet();
            }
            return null;
        };

        Future<Void> fA = executorService.submit(taskA);
        Future<Void> fB = executorService.submit(taskB);

        readyLatch.await(5, TimeUnit.SECONDS);
        startLatch.countDown(); // Fire both threads simultaneously

        fA.get(10, TimeUnit.SECONDS);
        fB.get(10, TimeUnit.SECONDS);
        executorService.shutdown();

        // Exactly 1 thread succeeds, 1 thread fails
        assertEquals(1, successCount.get(), "Chỉ đúng 1 đơn hàng tạo thành công!");
        assertEquals(1, failureCount.get(), "Đúng 1 luồng thất bại do cây bị tranh chấp!");

        Product updatedProduct = productRepository.findById(product.getProductId()).orElseThrow();
        assertEquals("RESERVED", updatedProduct.getProductStatus(), "Trạng thái cây chuyển sang RESERVED!");
    }
}
