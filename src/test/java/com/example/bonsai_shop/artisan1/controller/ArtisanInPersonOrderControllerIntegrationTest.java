package com.example.bonsai_shop.artisan1.controller;

import com.example.bonsai_shop.customer.repository.RoleRepository;
import com.example.bonsai_shop.customer.repository.UserRepository;
import com.example.bonsai_shop.entity.Category;
import com.example.bonsai_shop.entity.Order;
import com.example.bonsai_shop.entity.OrderDetail;
import com.example.bonsai_shop.entity.Payment;
import com.example.bonsai_shop.entity.Product;
import com.example.bonsai_shop.entity.ProductSegment;
import com.example.bonsai_shop.entity.Role;
import com.example.bonsai_shop.entity.User;
import com.example.bonsai_shop.entity.Variety;
import com.example.bonsai_shop.integration.support.AbstractDatabaseSafeIntegrationTest;
import com.example.bonsai_shop.product.repository.CategoryRepository;
import com.example.bonsai_shop.product.repository.OrderRepository;
import com.example.bonsai_shop.product.repository.PaymentRepository;
import com.example.bonsai_shop.product.repository.ProductRepository;
import com.example.bonsai_shop.product.repository.ProductSegmentRepository;
import com.example.bonsai_shop.product.repository.VarietyRepository;
import com.example.bonsai_shop.product.service.MailService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@Transactional
class ArtisanInPersonOrderControllerIntegrationTest extends AbstractDatabaseSafeIntegrationTest {

    private static final String ARTISAN_EMAIL = "artisan-in-person-controller-it@example.com";
    private static final String OTHER_ARTISAN_EMAIL = "other-in-person-controller-it@example.com";

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private VarietyRepository varietyRepository;

    @Autowired
    private ProductSegmentRepository productSegmentRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @MockitoBean
    private MailService mailService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUpMockMvc() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        TestArtisanPrincipal principal = new TestArtisanPrincipal(ARTISAN_EMAIL);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void index_WhenArtisanRequestsManagementPage_ShouldDisplayProductsAndOrders() throws Exception {
        User artisan = createTestArtisan();
        Product availableProduct = createProduct(artisan, "AVAILABLE", true);
        Order order = createInPersonOrder(artisan, createProduct(artisan, "RESERVED", true), "PENDING_PAYMENT");

        mockMvc.perform(get("/artisan/in-person-order"))
                .andExpect(status().isOk())
                .andExpect(view().name("artisan/in-person-order"))
                .andExpect(model().attribute("availableProducts", hasItem(hasProperty("productId", is(availableProduct.getProductId())))))
                .andExpect(model().attribute("orders", hasProperty("content", hasItem(hasProperty("orderId", is(order.getOrderId()))))))
                .andExpect(model().attribute("selectedStatus", "ALL"))
                .andExpect(model().attribute("pendingPaymentStatus", "PENDING_PAYMENT"))
                .andExpect(model().attribute("completedStatus", "COMPLETED"))
                .andExpect(model().attribute("cancelledStatus", "CANCELLED"));
    }

    @Test
    void index_WhenStatusFilterProvided_ShouldReturnOrdersForThatStatus() throws Exception {
        User artisan = createTestArtisan();
        Order pendingOrder = createInPersonOrder(artisan, createProduct(artisan, "RESERVED", true), "PENDING_PAYMENT");
        createInPersonOrder(artisan, createProduct(artisan, "SOLD", true), "COMPLETED");

        mockMvc.perform(get("/artisan/in-person-order")
                        .param("status", "PENDING_PAYMENT"))
                .andExpect(status().isOk())
                .andExpect(view().name("artisan/in-person-order"))
                .andExpect(model().attribute("selectedStatus", "PENDING_PAYMENT"))
                .andExpect(model().attribute("orders", hasProperty("content", hasItem(hasProperty("orderId", is(pendingOrder.getOrderId()))))));
    }

    @Test
    void index_WhenPageRequested_ShouldRequestThatPageFromService() throws Exception {
        User artisan = createTestArtisan();
        for (int orderIndex = 0; orderIndex < 12; orderIndex++) {
            createInPersonOrder(artisan, createProduct(artisan, "RESERVED", true), "PENDING_PAYMENT");
        }

        mockMvc.perform(get("/artisan/in-person-order")
                        .param("page", "1"))
                .andExpect(status().isOk())
                .andExpect(view().name("artisan/in-person-order"))
                .andExpect(model().attributeExists("orders"));
    }

//    @Test
//    void index_WhenKeywordProvided_ShouldPassTrimmedKeywordToService() throws Exception {
//        User artisan = createTestArtisan();
//        Order matchingOrder = createInPersonOrder(artisan, createProduct(artisan, "RESERVED", true), "PENDING_PAYMENT");
//
//        mockMvc.perform(get("/artisan/in-person-order")
//                        .param("keyword", "  " + matchingOrder.getOrderCode() + "  "))
//                .andExpect(status().isOk())
//                .andExpect(view().name("artisan/in-person-order"))
//                .andExpect(model().attribute("selectedKeyword", matchingOrder.getOrderCode()))
//                .andExpect(model().attribute("orders", hasProperty("content", hasItem(hasProperty("orderId", is(matchingOrder.getOrderId()))))));
//    }

    @Test
    void create_WhenRequestIsValid_ShouldRedirectWithSuccess() throws Exception {
        User artisan = createTestArtisan();
        Product product = createProduct(artisan, "AVAILABLE", true);

        mockMvc.perform(validCreatePost(product.getProductId()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/artisan/in-person-order#walkInOrdersSection"))
                .andExpect(flash().attributeExists("success"));

        entityManager.flush();
        entityManager.clear();
        assertTrue(orderRepository.findAll().stream()
                .anyMatch(order -> "IN_PERSON".equals(order.getOrderType())
                        && "Walk-in Customer".equals(order.getCustomerName())));
        assertEquals("RESERVED", productRepository.findById(product.getProductId()).orElseThrow().getProductStatus());
    }

    @Test
    void create_WhenProductUnavailable_ShouldRedirectWithError() throws Exception {
        User artisan = createTestArtisan();
        Product product = createProduct(artisan, "RESERVED", true);

        mockMvc.perform(validCreatePost(product.getProductId()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/artisan/in-person-order/"))
                .andExpect(flash().attributeExists("error"));
    }

    @Test
    void create_WhenProductBelongsToAnotherArtisan_ShouldRedirectWithError() throws Exception {
        User otherArtisan = createOtherArtisan();
        Product product = createProduct(otherArtisan, "AVAILABLE", true);

        mockMvc.perform(validCreatePost(product.getProductId()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/artisan/in-person-order/"))
                .andExpect(flash().attributeExists("error"));
    }

    @Test
    void create_WhenRequiredCustomerInformationInvalid_ShouldRedirectWithError() throws Exception {
        User artisan = createTestArtisan();
        Product product = createProduct(artisan, "AVAILABLE", true);

        mockMvc.perform(post("/artisan/in-person-order")
                        .param("productId", product.getProductId().toString())
                        .param("customerName", "")
                        .param("customerPhone", "")
                        .param("shippingAddress", "")
                        .param("paymentMethod", "CASH")
                        .param("craneFee", "0")
                        .param("shippingFee", "0")
                        .param("customerEmail", ""))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/artisan/in-person-order/"))
                .andExpect(flash().attributeExists("error"));
    }

    @Test
    void create_WhenFeeIsNegative_ShouldRedirectWithError() throws Exception {
        User artisan = createTestArtisan();
        Product product = createProduct(artisan, "AVAILABLE", true);

        mockMvc.perform(post("/artisan/in-person-order")
                        .param("productId", product.getProductId().toString())
                        .param("customerName", "Walk-in Customer")
                        .param("customerPhone", "0900000000")
                        .param("shippingAddress", "FPT HCM")
                        .param("paymentMethod", "CASH")
                        .param("craneFee", "-1")
                        .param("shippingFee", "0")
                        .param("customerEmail", "customer@test.com"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/artisan/in-person-order/"))
                .andExpect(flash().attributeExists("error"));
    }

    @Test
    void create_WhenPaymentMethodUnsupported_ShouldRedirectWithError() throws Exception {
        User artisan = createTestArtisan();
        Product product = createProduct(artisan, "AVAILABLE", true);

        mockMvc.perform(post("/artisan/in-person-order")
                        .param("productId", product.getProductId().toString())
                        .param("customerName", "Walk-in Customer")
                        .param("customerPhone", "0900000000")
                        .param("shippingAddress", "FPT HCM")
                        .param("paymentMethod", "BANK_TRANSFER")
                        .param("craneFee", "0")
                        .param("shippingFee", "0")
                        .param("customerEmail", "customer@test.com"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/artisan/in-person-order/"))
                .andExpect(flash().attributeExists("error"));
    }

    @Test
    void update_WhenPendingOrderIsEditable_ShouldRedirectWithSuccess() throws Exception {
        User artisan = createTestArtisan();
        Order order = createInPersonOrder(artisan, createProduct(artisan, "RESERVED", true), "PENDING_PAYMENT");

        mockMvc.perform(validUpdatePost(order.getOrderId()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/artisan/in-person-order#walkInOrdersSection"))
                .andExpect(flash().attributeExists("success"));

        entityManager.flush();
        entityManager.clear();
        assertEquals("Updated Customer", orderRepository.findById(order.getOrderId()).orElseThrow().getCustomerName());
        assertEquals("VNPAY", paymentRepository.findByOrderOrderIdOrderByPaymentIdAsc(order.getOrderId()).get(0).getPaymentMethod());
    }

    @Test
    void update_WhenOrderIsNonEditable_ShouldRedirectWithError() throws Exception {
        User artisan = createTestArtisan();
        Order order = createInPersonOrder(artisan, createProduct(artisan, "SOLD", true), "COMPLETED");

        mockMvc.perform(validUpdatePost(order.getOrderId()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/artisan/in-person-order#walkInOrdersSection"))
                .andExpect(flash().attributeExists("error"));
    }

    @Test
    void update_WhenOrderBelongsToAnotherArtisan_ShouldRedirectWithError() throws Exception {
        User otherArtisan = createOtherArtisan();
        Order order = createInPersonOrder(otherArtisan, createProduct(otherArtisan, "RESERVED", true), "PENDING_PAYMENT");

        mockMvc.perform(validUpdatePost(order.getOrderId()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/artisan/in-person-order#walkInOrdersSection"))
                .andExpect(flash().attributeExists("error"));
    }

    @Test
    void confirmPayment_WhenPendingOrderIsValid_ShouldRedirectWithSuccess() throws Exception {
        User artisan = createTestArtisan();
        Product product = createProduct(artisan, "RESERVED", true);
        Order order = createInPersonOrder(artisan, product, "PENDING_PAYMENT");

        mockMvc.perform(post("/artisan/in-person-order/" + order.getOrderId() + "/confirm-payment"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/artisan/in-person-order#walkInOrdersSection"))
                .andExpect(flash().attributeExists("success"));

        entityManager.flush();
        entityManager.clear();
        assertEquals("COMPLETED", orderRepository.findById(order.getOrderId()).orElseThrow().getOrderStatus());
        assertEquals("SOLD", productRepository.findById(product.getProductId()).orElseThrow().getProductStatus());
        assertEquals("SUCCESS", paymentRepository.findByOrderOrderIdOrderByPaymentIdAsc(order.getOrderId()).get(0).getPaymentStatus());
    }

    @Test
    void confirmPayment_WhenOrderStateIsInvalid_ShouldRedirectWithError() throws Exception {
        User artisan = createTestArtisan();
        Order order = createInPersonOrder(artisan, createProduct(artisan, "AVAILABLE", true), "CANCELLED");

        mockMvc.perform(post("/artisan/in-person-order/" + order.getOrderId() + "/confirm-payment"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/artisan/in-person-order#walkInOrdersSection"))
                .andExpect(flash().attributeExists("error"));
    }

    @Test
    void confirmPayment_WhenOrderBelongsToAnotherArtisan_ShouldRedirectWithError() throws Exception {
        User otherArtisan = createOtherArtisan();
        Order order = createInPersonOrder(otherArtisan, createProduct(otherArtisan, "RESERVED", true), "PENDING_PAYMENT");

        mockMvc.perform(post("/artisan/in-person-order/" + order.getOrderId() + "/confirm-payment"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/artisan/in-person-order#walkInOrdersSection"))
                .andExpect(flash().attributeExists("error"));
    }

    @Test
    void cancel_WhenPendingOrderIsValid_ShouldRedirectWithSuccess() throws Exception {
        User artisan = createTestArtisan();
        Product product = createProduct(artisan, "RESERVED", true);
        Order order = createInPersonOrder(artisan, product, "PENDING_PAYMENT");

        mockMvc.perform(post("/artisan/in-person-order/" + order.getOrderId() + "/cancel"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/artisan/in-person-order#walkInOrdersSection"))
                .andExpect(flash().attributeExists("success"));

        entityManager.flush();
        entityManager.clear();
        assertEquals("CANCELLED", orderRepository.findById(order.getOrderId()).orElseThrow().getOrderStatus());
        assertEquals("AVAILABLE", productRepository.findById(product.getProductId()).orElseThrow().getProductStatus());
    }

    @Test
    void cancel_WhenOrderAlreadyCompleted_ShouldRedirectWithError() throws Exception {
        User artisan = createTestArtisan();
        Order order = createInPersonOrder(artisan, createProduct(artisan, "SOLD", true), "COMPLETED");

        mockMvc.perform(post("/artisan/in-person-order/" + order.getOrderId() + "/cancel"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/artisan/in-person-order#walkInOrdersSection"))
                .andExpect(flash().attributeExists("error"));
    }

    @Test
    void cancel_WhenOrderBelongsToAnotherArtisan_ShouldRedirectWithError() throws Exception {
        User otherArtisan = createOtherArtisan();
        Order order = createInPersonOrder(otherArtisan, createProduct(otherArtisan, "RESERVED", true), "PENDING_PAYMENT");

        mockMvc.perform(post("/artisan/in-person-order/" + order.getOrderId() + "/cancel"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/artisan/in-person-order#walkInOrdersSection"))
                .andExpect(flash().attributeExists("error"));
    }

    private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder validCreatePost(Integer productId) {
        return post("/artisan/in-person-order")
                .param("productId", productId.toString())
                .param("customerName", "Walk-in Customer")
                .param("customerPhone", "0900000000")
                .param("shippingAddress", "FPT HCM")
                .param("paymentMethod", "CASH")
                .param("craneFee", "100000")
                .param("shippingFee", "50000")
                .param("customerEmail", "customer@test.com")
                .param("notes", "Pay at store");
    }

    private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder validUpdatePost(Integer orderId) {
        return post("/artisan/in-person-order/" + orderId + "/update")
                .param("customerName", "Updated Customer")
                .param("customerPhone", "0911111111")
                .param("shippingAddress", "Updated Address")
                .param("paymentMethod", "VNPAY")
                .param("craneFee", "200000")
                .param("shippingFee", "75000")
                .param("customerEmail", "updated@test.com")
                .param("notes", "Updated notes");
    }

    private Order createInPersonOrder(User artisan, Product product, String status) {
        Order order = Order.builder()
                .orderCode("AIPOC-IT-" + System.nanoTime())
                .customerName("Walk-in Customer")
                .customerPhone("0900000000")
                .customerEmail("customer@test.com")
                .shippingAddress("FPT HCM")
                .orderDate(LocalDateTime.now())
                .totalAmount(product.getPrice().add(new BigDecimal("150000")))
                .depositAmount(BigDecimal.ZERO)
                .craneFee(new BigDecimal("100000"))
                .shippingFee(new BigDecimal("50000"))
                .orderStatus(status)
                .orderType("IN_PERSON")
                .notes("Pay at store")
                .build();
        OrderDetail orderDetail = OrderDetail.builder()
                .order(order)
                .product(product)
                .priceAtPurchase(product.getPrice())
                .quantity(1)
                .build();
        order.setOrderDetails(List.of(orderDetail));
        Order savedOrder = orderRepository.save(order);
        Payment payment = Payment.builder()
                .order(savedOrder)
                .paymentMethod("CASH")
                .paymentStatus("COMPLETED".equals(status) ? "SUCCESS" : "PENDING")
                .paymentType("FULL_PAYMENT")
                .amount(savedOrder.getTotalAmount())
                .build();
        savedOrder.setPayments(List.of(payment));
        paymentRepository.save(payment);
        entityManager.flush();
        entityManager.clear();
        return savedOrder;
    }

    private Product createProduct(User artisan, String status, boolean visible) {
        Category category = categoryRepository.save(Category.builder()
                .categoryName("AIPOC Category " + System.nanoTime())
                .description("Integration test category")
                .build());
        Variety variety = varietyRepository.save(Variety.builder()
                .category(category)
                .varietyName("AIPOC Variety " + System.nanoTime())
                .description("Integration test variety")
                .build());
        ProductSegment segment = productSegmentRepository.save(ProductSegment.builder()
                .segmentName("AIPOC Segment " + System.nanoTime())
                .build());
        return productRepository.save(Product.builder()
                .createdBy(artisan)
                .variety(variety)
                .segment(segment)
                .productCode("AIPOC-P-" + System.nanoTime())
                .productName("In Person Integration Bonsai")
                .description("Product used by in-person controller integration test")
                .treeStory("In-person integration")
                .age(10)
                .height(40.0f)
                .trunkDiameter(7.0f)
                .style("Formal Upright")
                .price(new BigDecimal("3000000"))
                .productStatus(status)
                .isVisible(visible)
                .isPublicPrice(true)
                .viewCount(0)
                .createdAt(LocalDateTime.now())
                .build());
    }

    private User createTestArtisan() {
        return createArtisan(ARTISAN_EMAIL, "Artisan In Person Controller IT");
    }

    private User createOtherArtisan() {
        return createArtisan(OTHER_ARTISAN_EMAIL, "Other In Person Controller IT");
    }

    private User createArtisan(String email, String fullName) {
        return userRepository.findByEmail(email).orElseGet(() -> {
            Role role = roleRepository.findByRoleName("ARTISAN")
                    .orElseGet(() -> roleRepository.save(Role.builder().roleName("ARTISAN").description("ARTISAN").build()));
            return userRepository.save(User.builder()
                    .email(email)
                    .username(email)
                    .fullName(fullName)
                    .phone("0987654321")
                    .password("password123")
                    .status("ACTIVE")
                    .role(role)
                    .build());
        });
    }

    private record TestArtisanPrincipal(String username) implements UserDetails {

        public String getFullName() {
            return "Artisan In Person Controller IT";
        }

        public String getAvatar() {
            return "";
        }

        @Override
        public String getUsername() {
            return username;
        }

        @Override
        public Collection<? extends GrantedAuthority> getAuthorities() {
            return List.of(new SimpleGrantedAuthority("ROLE_ARTISAN"));
        }

        @Override
        public String getPassword() {
            return "password123";
        }
    }
}
