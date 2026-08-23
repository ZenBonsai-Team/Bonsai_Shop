package com.example.bonsai_shop.system;

import com.example.bonsai_shop.customer.repository.RoleRepository;
import com.example.bonsai_shop.customer.service.CustomUserDetails;
import com.example.bonsai_shop.data.service.CloudinaryStorageService;
import com.example.bonsai_shop.entity.*;
import com.example.bonsai_shop.owner.repository.AccountRepository;
import com.example.bonsai_shop.owner.service.AccountService;
import com.example.bonsai_shop.product.repository.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
// System test cho chuc nang ban hang truc tiep tai vuon (ArtisanInPersonOrderService).
class BF06InPersonOrderSystemTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private AccountRepository accountRepository;
    @Autowired private AccountService accountService;
    @Autowired private RoleRepository roleRepository;
    @Autowired private CategoryRepository categoryRepository;
    @Autowired private VarietyRepository varietyRepository;
    @Autowired private ProductSegmentRepository segmentRepository;
    @Autowired private TagRepository tagRepository;
    @Autowired private ProductRepository productRepository;
    @Autowired private OrderRepository orderRepository;
    @Autowired private PaymentRepository paymentRepository;

    // CloudinaryStorageService is mocked to avoid real uploads in any upstream calls
    @MockitoBean
    private CloudinaryStorageService cloudinaryStorageService;

    // ======================== HELPERS ========================

    private RequestPostProcessor artisanUser() {
        User artisan = findOrCreateArtisan();
        return user(new CustomUserDetails(artisan,
                List.of(new SimpleGrantedAuthority("ROLE_ARTISAN"))));
    }

    private User findOrCreateArtisan() {
        Role role = findRole("ARTISAN", "ROLE_ARTISAN");
        String email = "artisan.inperson@test.com";
        User artisan = accountRepository.findAll().stream()
                .filter(u -> email.equalsIgnoreCase(u.getEmail()))
                .findFirst()
                .orElseGet(() -> {
                    accountService.createAccount("InPerson Artisan", email, "123456", "0910000088", role.getRoleId());
                    return accountRepository.findAll().stream()
                            .filter(u -> email.equalsIgnoreCase(u.getEmail()))
                            .findFirst().orElseThrow();
                });
        artisan.setStatus("ACTIVE");
        artisan.setRole(role);
        return accountRepository.save(artisan);
    }

    private Role findRole(String... names) {
        return roleRepository.findAll().stream()
                .filter(r -> { for (String n : names) if (n.equalsIgnoreCase(r.getRoleName())) return true; return false; })
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Role not found"));
    }

    private Product createAvailableProduct() {
        User artisan = findOrCreateArtisan();
        Category cat = categoryRepository.save(Category.builder().categoryName("IPO Cat " + System.nanoTime()).build());
        Variety var = varietyRepository.save(Variety.builder().category(cat).varietyName("IPO Var " + System.nanoTime()).build());
        ProductSegment seg = segmentRepository.save(ProductSegment.builder().segmentName("IPO Seg " + System.nanoTime()).build());

        Product product = Product.builder()
                .productName("IPO Bonsai " + System.nanoTime())
                .productCode("TMP-BF06-" + System.nanoTime())
                .productStatus("AVAILABLE")
                .price(new BigDecimal("2500000"))
                .isPublicPrice(true)
                .isVisible(true)
                .createdBy(artisan)
                .variety(var)
                .segment(seg)
                .viewCount(0)
                .createdAt(LocalDateTime.now())
                .build();
        return productRepository.save(product);
    }

    private Order createInPersonOrder(Product product) {
        // Call the controller endpoint to create the order properly
        try {
            mockMvc.perform(post("/artisan/in-person-order")
                            .with(artisanUser())
                            .with(csrf())
                            .param("productId", String.valueOf(product.getProductId()))
                            .param("customerName", "Nguyen Van A")
                            .param("customerPhone", "0901234567")
                            .param("shippingAddress", "123 Nguyen Trai, Quan 1, TP.HCM")
                            .param("customerEmail", "customer@test.com")
                            .param("paymentMethod", "CASH")
                            .param("craneFee", "0")
                            .param("shippingFee", "0"))
                    .andExpect(status().is3xxRedirection());
        } catch (Exception e) {
            throw new RuntimeException("Failed to create in-person order via controller", e);
        }
        return orderRepository.findAll().stream()
                .filter(o -> "IN_PERSON".equals(o.getOrderType()))
                .filter(o -> o.getOrderDetails() != null && !o.getOrderDetails().isEmpty())
                .filter(o -> o.getOrderDetails().get(0).getProduct().getProductId().equals(product.getProductId()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("In-person order was not created"));
    }

    // ======================== TESTS ========================

    @Test
    void tcSysBF06001_artisanCanViewInPersonOrderDashboard() throws Exception {
        // TC: Artisan mo trang quan ly don tai vuon thanh cong
        mockMvc.perform(get("/artisan/in-person-order")
                        .with(artisanUser()))
                .andExpect(status().isOk())
                .andExpect(view().name("artisan/in-person-order"))
                .andExpect(model().attributeExists("availableProducts"))
                .andExpect(model().attributeExists("orders"))
                .andExpect(model().attributeExists("selectedStatus"))
                .andExpect(model().attributeExists("pendingPaymentStatus"))
                .andExpect(model().attributeExists("completedStatus"))
                .andExpect(model().attributeExists("cancelledStatus"));
    }

    @Test
    void tcSysBF06002_artisanCanCreateInPersonOrderSuccessfully() throws Exception {
        // TC: Artisan tao don tai vuon thanh cong
        Product product = createAvailableProduct();

        mockMvc.perform(post("/artisan/in-person-order")
                        .with(artisanUser())
                        .with(csrf())
                        .param("productId", String.valueOf(product.getProductId()))
                        .param("customerName", "Nguyen Van A")
                        .param("customerPhone", "0901234567")
                        .param("shippingAddress", "123 Nguyen Trai, Quan 1, TP.HCM")
                        .param("customerEmail", "customer@test.com")
                        .param("paymentMethod", "CASH")
                        .param("craneFee", "50000")
                        .param("shippingFee", "100000"))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attributeExists("success"));

        // Product should now be RESERVED
        Product updated = productRepository.findById(product.getProductId()).orElseThrow();
        assertEquals("RESERVED", updated.getProductStatus());

        // Order should be saved
        List<Order> orders = orderRepository.findAll().stream()
                .filter(o -> "IN_PERSON".equals(o.getOrderType()))
                .filter(o -> "PENDING_PAYMENT".equals(o.getOrderStatus()))
                .toList();
        assertFalse(orders.isEmpty());
    }

    @Test
    void tcSysBF06003_createOrderFailsWithInvalidPhone() throws Exception {
        // TC: Tao don that bai khi so dien thoai khong hop le
        Product product = createAvailableProduct();

        mockMvc.perform(post("/artisan/in-person-order")
                        .with(artisanUser())
                        .with(csrf())
                        .param("productId", String.valueOf(product.getProductId()))
                        .param("customerName", "Nguyen Van A")
                        .param("customerPhone", "12345") // invalid phone
                        .param("shippingAddress", "123 Nguyen Trai, Quan 1, TP.HCM")
                        .param("customerEmail", "customer@test.com")
                        .param("paymentMethod", "CASH")
                        .param("craneFee", "0")
                        .param("shippingFee", "0"))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attributeExists("error"));

        // Product should still be AVAILABLE
        Product unchanged = productRepository.findById(product.getProductId()).orElseThrow();
        assertEquals("AVAILABLE", unchanged.getProductStatus());
    }

    @Test
    void tcSysBF06004_createOrderFailsWithInvalidEmail() throws Exception {
        // TC: Tao don that bai khi email khong hop le
        Product product = createAvailableProduct();

        mockMvc.perform(post("/artisan/in-person-order")
                        .with(artisanUser())
                        .with(csrf())
                        .param("productId", String.valueOf(product.getProductId()))
                        .param("customerName", "Nguyen Van A")
                        .param("customerPhone", "0901234567")
                        .param("shippingAddress", "123 Nguyen Trai, Quan 1, TP.HCM")
                        .param("customerEmail", "invalid-email") // invalid email
                        .param("paymentMethod", "CASH")
                        .param("craneFee", "0")
                        .param("shippingFee", "0"))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attributeExists("error"));
    }

    @Test
    void tcSysBF06005_createOrderFailsWithShortCustomerName() throws Exception {
        // TC: Tao don that bai khi ten khach hang qua ngan (duoi 3 ky tu)
        Product product = createAvailableProduct();

        mockMvc.perform(post("/artisan/in-person-order")
                        .with(artisanUser())
                        .with(csrf())
                        .param("productId", String.valueOf(product.getProductId()))
                        .param("customerName", "AB") // too short
                        .param("customerPhone", "0901234567")
                        .param("shippingAddress", "123 Nguyen Trai, Quan 1, TP.HCM")
                        .param("customerEmail", "customer@test.com")
                        .param("paymentMethod", "CASH")
                        .param("craneFee", "0")
                        .param("shippingFee", "0"))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attributeExists("error"));
    }

    @Test
    void tcSysBF06006_createOrderFailsWithInvalidPaymentMethod() throws Exception {
        // TC: Tao don that bai khi payment method khong hop le
        Product product = createAvailableProduct();

        mockMvc.perform(post("/artisan/in-person-order")
                        .with(artisanUser())
                        .with(csrf())
                        .param("productId", String.valueOf(product.getProductId()))
                        .param("customerName", "Nguyen Van An")
                        .param("customerPhone", "0901234567")
                        .param("shippingAddress", "123 Nguyen Trai, Quan 1, TP.HCM")
                        .param("customerEmail", "customer@test.com")
                        .param("paymentMethod", "BITCOIN") // invalid
                        .param("craneFee", "0")
                        .param("shippingFee", "0"))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attributeExists("error"));
    }

    @Test
    void tcSysBF06007_createOrderWithVNPayPaymentMethod() throws Exception {
        // TC: Tao don thanh cong voi phuong thuc thanh toan VNPAY
        Product product = createAvailableProduct();

        mockMvc.perform(post("/artisan/in-person-order")
                        .with(artisanUser())
                        .with(csrf())
                        .param("productId", String.valueOf(product.getProductId()))
                        .param("customerName", "Tran Thi Bich")
                        .param("customerPhone", "0912345678")
                        .param("shippingAddress", "456 Le Loi, Quan 3, TP.HCM")
                        .param("customerEmail", "tran@test.com")
                        .param("paymentMethod", "VNPAY")
                        .param("craneFee", "0")
                        .param("shippingFee", "0"))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attributeExists("success"));
    }

    @Test
    void tcSysBF06008_artisanCanConfirmPaymentForPendingOrder() throws Exception {
        // TC: Artisan xac nhan thanh toan don thanh cong, san pham chuyen SOLD
        Product product = createAvailableProduct();
        Order order = createInPersonOrder(product);

        mockMvc.perform(post("/artisan/in-person-order/" + order.getOrderId() + "/confirm-payment")
                        .with(artisanUser())
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attributeExists("success"));

        Order completed = orderRepository.findById(order.getOrderId()).orElseThrow();
        assertEquals("COMPLETED", completed.getOrderStatus());

        Product sold = productRepository.findById(product.getProductId()).orElseThrow();
        assertEquals("SOLD", sold.getProductStatus());
    }

    @Test
    void tcSysBF06009_confirmPaymentFailsForNonExistentOrder() throws Exception {
        // TC: Xac nhan thanh toan that bai khi orderId khong ton tai
        mockMvc.perform(post("/artisan/in-person-order/99999/confirm-payment")
                        .with(artisanUser())
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attributeExists("error"));
    }

    @Test
    void tcSysBF06010_artisanCanCancelPendingOrder() throws Exception {
        // TC: Artisan huy don PENDING thanh cong, san pham quay lai AVAILABLE
        Product product = createAvailableProduct();
        Order order = createInPersonOrder(product);

        mockMvc.perform(post("/artisan/in-person-order/" + order.getOrderId() + "/cancel")
                        .with(artisanUser())
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attributeExists("success"));

        Order cancelled = orderRepository.findById(order.getOrderId()).orElseThrow();
        assertEquals("CANCELLED", cancelled.getOrderStatus());

        Product available = productRepository.findById(product.getProductId()).orElseThrow();
        assertEquals("AVAILABLE", available.getProductStatus());
    }

    @Test
    void tcSysBF06011_cancelOrderFailsForNonExistentOrder() throws Exception {
        // TC: Huy don that bai khi orderId khong ton tai
        mockMvc.perform(post("/artisan/in-person-order/99999/cancel")
                        .with(artisanUser())
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attributeExists("error"));
    }

    @Test
    void tcSysBF06012_cancelCompletedOrderShouldFail() throws Exception {
        // TC: Huy don da hoan thanh phai that bai
        Product product = createAvailableProduct();
        Order order = createInPersonOrder(product);

        // First confirm payment
        mockMvc.perform(post("/artisan/in-person-order/" + order.getOrderId() + "/confirm-payment")
                        .with(artisanUser())
                        .with(csrf()))
                .andExpect(status().is3xxRedirection());

        // Then try to cancel — should fail
        mockMvc.perform(post("/artisan/in-person-order/" + order.getOrderId() + "/cancel")
                        .with(artisanUser())
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attributeExists("error"));

        // Order remains COMPLETED
        Order stillCompleted = orderRepository.findById(order.getOrderId()).orElseThrow();
        assertEquals("COMPLETED", stillCompleted.getOrderStatus());
    }

    @Test
    void tcSysBF06013_artisanCanUpdatePendingOrder() throws Exception {
        // TC: Artisan cap nhat thong tin don dang cho thanh toan
        Product product = createAvailableProduct();
        Order order = createInPersonOrder(product);

        mockMvc.perform(post("/artisan/in-person-order/" + order.getOrderId() + "/update")
                        .with(artisanUser())
                        .with(csrf())
                        .param("customerName", "Pham Van B Updated")
                        .param("customerPhone", "0987654321")
                        .param("shippingAddress", "789 Hai Ba Trung, Quan Binh Thanh, TP.HCM")
                        .param("customerEmail", "updated@test.com")
                        .param("paymentMethod", "VNPAY")
                        .param("craneFee", "200000")
                        .param("shippingFee", "150000"))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attributeExists("success"));

        Order updated = orderRepository.findById(order.getOrderId()).orElseThrow();
        assertEquals("Pham Van B Updated", updated.getCustomerName());
        assertEquals("0987654321", updated.getCustomerPhone());
    }

    @Test
    void tcSysBF06014_updateOrderFailsWithInvalidPhone() throws Exception {
        // TC: Cap nhat don that bai khi so dien thoai khong hop le
        Product product = createAvailableProduct();
        Order order = createInPersonOrder(product);

        mockMvc.perform(post("/artisan/in-person-order/" + order.getOrderId() + "/update")
                        .with(artisanUser())
                        .with(csrf())
                        .param("customerName", "Pham Van B")
                        .param("customerPhone", "999") // invalid
                        .param("shippingAddress", "123 Le Loi, Quan 1, TP.HCM")
                        .param("customerEmail", "test@test.com")
                        .param("paymentMethod", "CASH")
                        .param("craneFee", "0")
                        .param("shippingFee", "0"))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attributeExists("error"));
    }

    @Test
    void tcSysBF06015_viewOrdersWithStatusFilter() throws Exception {
        // TC: Artisan loc don theo trang thai PENDING_PAYMENT
        mockMvc.perform(get("/artisan/in-person-order")
                        .with(artisanUser())
                        .param("status", "PENDING_PAYMENT")
                        .param("keyword", "")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(view().name("artisan/in-person-order"))
                .andExpect(model().attributeExists("orders"));
    }

    @Test
    void tcSysBF06016_viewOrdersWithKeywordSearch() throws Exception {
        // TC: Artisan tim don theo tu khoa
        mockMvc.perform(get("/artisan/in-person-order")
                        .with(artisanUser())
                        .param("status", "ALL")
                        .param("keyword", "BSMS")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("orders"));
    }

    @Test
    void tcSysBF06017_createOrderFailsForProductNotBelongingToArtisan() throws Exception {
        // TC: Tao don that bai khi productId khong thuoc artisan nay
        mockMvc.perform(post("/artisan/in-person-order")
                        .with(artisanUser())
                        .with(csrf())
                        .param("productId", "99999") // non-existent product
                        .param("customerName", "Nguyen Van A")
                        .param("customerPhone", "0901234567")
                        .param("shippingAddress", "123 Nguyen Trai, Quan 1, TP.HCM")
                        .param("customerEmail", "customer@test.com")
                        .param("paymentMethod", "CASH")
                        .param("craneFee", "0")
                        .param("shippingFee", "0"))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attributeExists("error"));
    }

    @Test
    void tcSysBF06018_createOrderFailsWhenProductAlreadyReserved() throws Exception {
        // TC: Tao don that bai khi san pham da duoc dat cho (RESERVED)
        Product product = createAvailableProduct();
        // First order - should succeed and reserve the product
        createInPersonOrder(product);

        // Second order for same product - should fail since now RESERVED
        mockMvc.perform(post("/artisan/in-person-order")
                        .with(artisanUser())
                        .with(csrf())
                        .param("productId", String.valueOf(product.getProductId()))
                        .param("customerName", "Le Thi C")
                        .param("customerPhone", "0909090909")
                        .param("shippingAddress", "321 Vo Van Tan, Quan 3, TP.HCM")
                        .param("customerEmail", "lethi@test.com")
                        .param("paymentMethod", "CASH")
                        .param("craneFee", "0")
                        .param("shippingFee", "0"))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attributeExists("error"));
    }

    @Test
    void tcSysBF06019_doubleConfirmPaymentIsIdempotent() throws Exception {
        // TC: Xac nhan thanh toan 2 lan phai an toan (idempotent)
        Product product = createAvailableProduct();
        Order order = createInPersonOrder(product);

        // First confirm
        mockMvc.perform(post("/artisan/in-person-order/" + order.getOrderId() + "/confirm-payment")
                        .with(artisanUser())
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attributeExists("success"));

        // Second confirm — should succeed (idempotent, returns same completed order)
        mockMvc.perform(post("/artisan/in-person-order/" + order.getOrderId() + "/confirm-payment")
                        .with(artisanUser())
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attributeExists("success"));

        Order completed = orderRepository.findById(order.getOrderId()).orElseThrow();
        assertEquals("COMPLETED", completed.getOrderStatus());
    }

    @Test
    void tcSysBF06020_createOrderWithNotesSaved() throws Exception {
        // TC: Tao don voi ghi chu va kiem tra duoc luu vao DB
        Product product = createAvailableProduct();

        mockMvc.perform(post("/artisan/in-person-order")
                        .with(artisanUser())
                        .with(csrf())
                        .param("productId", String.valueOf(product.getProductId()))
                        .param("customerName", "Hoang Thi D")
                        .param("customerPhone", "0933333333")
                        .param("shippingAddress", "99 Dien Bien Phu, Quan Binh Thanh, TP.HCM")
                        .param("customerEmail", "hoang@test.com")
                        .param("paymentMethod", "CASH")
                        .param("craneFee", "0")
                        .param("shippingFee", "0")
                        .param("notes", "Khach muon giao buoi sang"))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attributeExists("success"));

        Order savedOrder = orderRepository.findAll().stream()
                .filter(o -> "IN_PERSON".equals(o.getOrderType()))
                .filter(o -> o.getOrderDetails() != null && !o.getOrderDetails().isEmpty())
                .filter(o -> o.getOrderDetails().get(0).getProduct().getProductId().equals(product.getProductId()))
                .findFirst()
                .orElseThrow();
        assertEquals("Khach muon giao buoi sang", savedOrder.getNotes());
    }
}
