package com.example.bonsai_shop.integration.cart;

import com.example.bonsai_shop.customer.repository.RoleRepository;
import com.example.bonsai_shop.customer.repository.UserRepository;
import com.example.bonsai_shop.customer.service.CustomUserDetails;
import com.example.bonsai_shop.entity.*;
import com.example.bonsai_shop.integration.support.BaseControllerIntegrationTest;
import com.example.bonsai_shop.product.repository.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;

public class CartApiControllerIntegrationTest extends BaseControllerIntegrationTest {

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

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

    @DisplayName("TC-IT-CART-02: GET /api/cart lấy danh sách sản phẩm trong giỏ hàng")
    @Test
    void testGetCartSuccess() throws Exception {
        User customer = createTestUser("cart.get@example.com", "CUSTOMER");
        CustomUserDetails userDetails = new CustomUserDetails(customer, List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER")));

        mockMvc.perform(MockMvcRequestBuilders.get("/api/cart").with(user(userDetails)))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$").isArray());
    }

    @DisplayName("TC-IT-CART-03: POST /api/cart/items thêm sản phẩm vào giỏ hàng")
    @Test
    void testAddToCartSuccess() throws Exception {
        User customer = createTestUser("cart.add@example.com", "CUSTOMER");
        CustomUserDetails userDetails = new CustomUserDetails(customer, List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER")));
        Product product = createTestProduct("TREE-CART-03", "Cây Giỏ Hàng", new BigDecimal("300000"));

        Map<String, Integer> payload = new HashMap<>();
        payload.put("productId", product.getProductId());

        mockMvc.perform(MockMvcRequestBuilders.post("/api/cart/items").with(csrf()).with(user(userDetails))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.success").value(true));
    }

    @DisplayName("TC-IT-CART-04: DELETE /api/cart/items/{productId} xóa sản phẩm khỏi giỏ hàng")
    @Test
    void testRemoveFromCartSuccess() throws Exception {
        User customer = createTestUser("cart.del@example.com", "CUSTOMER");
        CustomUserDetails userDetails = new CustomUserDetails(customer, List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER")));
        Product product = createTestProduct("TREE-CART-04", "Cây Xóa Giỏ", new BigDecimal("400000"));

        mockMvc.perform(MockMvcRequestBuilders.delete("/api/cart/items/" + product.getProductId()).with(csrf()).with(user(userDetails)))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.success").value(true));
    }

    @DisplayName("TC-IT-CART-05: POST /api/cart/sync đồng bộ giỏ hàng khách vãng lai sau login")
    @Test
    void testSyncGuestCartSuccess() throws Exception {
        User customer = createTestUser("cart.sync@example.com", "CUSTOMER");
        CustomUserDetails userDetails = new CustomUserDetails(customer, List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER")));
        Product product = createTestProduct("TREE-CART-05", "Cây Sync Giỏ", new BigDecimal("500000"));

        List<Integer> productIds = List.of(product.getProductId());

        mockMvc.perform(MockMvcRequestBuilders.post("/api/cart/sync").with(csrf()).with(user(userDetails))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(productIds)))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.success").value(true));
    }
}
