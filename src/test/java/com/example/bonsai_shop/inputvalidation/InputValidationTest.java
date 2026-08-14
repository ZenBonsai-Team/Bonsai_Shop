package com.example.bonsai_shop.inputvalidation;

import com.example.bonsai_shop.artisan.controller.ArtisanProductController;
import com.example.bonsai_shop.artisan.dto.ArtisanProductFormDTO;
import com.example.bonsai_shop.artisan.service.ArtisanProductService;
import com.example.bonsai_shop.artisan.service.ProductJournalService;
import com.example.bonsai_shop.customer.repository.UserRepository;
import com.example.bonsai_shop.product.controller.OrderApiController;
import com.example.bonsai_shop.product.dto.PurchaseOrderRequestDTO;
import com.example.bonsai_shop.product.service.OrderService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Suite kiểm thử Input Validation cho BSMS.
 * Phủ các DTO/Form request: PurchaseOrderRequestDTO, ArtisanProductFormDTO.
 */
class InputValidationTest {

    private Validator validator;

    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Nested
    @DisplayName("1. Kiểm thử Input Validation - PurchaseOrderRequestDTO")
    class PurchaseOrderRequestDTOValidationTests {

        private MockMvc mockMvc;
        private OrderService orderService;
        private UserRepository userRepository;
        private ObjectMapper objectMapper;

        @BeforeEach
        void init() {
            orderService = mock(OrderService.class);
            userRepository = mock(UserRepository.class);
            OrderApiController controller = new OrderApiController();
            ReflectionTestUtils.setField(controller, "orderService", orderService);
            ReflectionTestUtils.setField(controller, "userRepository", userRepository);

            mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
            objectMapper = new ObjectMapper();
        }

        @Test
        @DisplayName("Negative Test: Gửi payload trống đến /api/orders/checkout kỳ vọng HTTP 400 Bad Request")
        void testCheckout_EmptyPayload_Returns400BadRequest() throws Exception {
            // Arrange
            PurchaseOrderRequestDTO dto = new PurchaseOrderRequestDTO(); // Form trống hoàn toàn

            // Act & Assert
            mockMvc.perform(post("/api/orders/checkout")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Negative Test: Gửi sđt và email sai định dạng đến /api/orders/checkout kỳ vọng vi phạm Pattern và Email")
        void testCheckout_InvalidPhoneAndEmail_ReturnsValidationErrors() {
            // Arrange
            PurchaseOrderRequestDTO dto = new PurchaseOrderRequestDTO();
            dto.setCustomerName("Nguyen Van A");
            dto.setCustomerPhone("12345"); // SĐT không đủ 10 chữ số
            dto.setCustomerEmail("invalid-email-format"); // Email không đúng định dạng
            dto.setShippingAddress("123 Hanoi");
            dto.setPaymentMethod("VNPAY");

            // Act
            Set<ConstraintViolation<PurchaseOrderRequestDTO>> violations = validator.validate(dto);

            // Assert
            assertThat(violations).hasSize(2);
            assertThat(violations).extracting(ConstraintViolation::getMessage)
                    .contains("Số điện thoại không hợp lệ (Cần 10 chữ số)", "Email không hợp lệ");
        }

        @Test
        @DisplayName("Positive Test: Gửi dữ liệu hợp lệ đến PurchaseOrderRequestDTO vượt qua kiểm tra validation")
        void testCheckout_ValidPayload_PassesValidation() {
            // Arrange
            PurchaseOrderRequestDTO dto = new PurchaseOrderRequestDTO();
            dto.setCustomerName("Nguyen Van A");
            dto.setCustomerPhone("0912345678");
            dto.setCustomerEmail("nguyenvana@gmail.com");
            dto.setShippingAddress("123 Le Loi, District 1, HCMC");
            dto.setPaymentMethod("VNPAY");

            // Act
            Set<ConstraintViolation<PurchaseOrderRequestDTO>> violations = validator.validate(dto);

            // Assert
            assertThat(violations).isEmpty();
        }
    }

    @Nested
    @DisplayName("2. Kiểm thử Input Validation - ArtisanProductFormDTO")
    class ArtisanProductFormDTOValidationTests {

        private MockMvc mockMvc;

        @BeforeEach
        void init() {
            ArtisanProductService artisanProductService = mock(ArtisanProductService.class);
            ProductJournalService productJournalService = mock(ProductJournalService.class);
            ArtisanProductController controller = new ArtisanProductController(artisanProductService, productJournalService);
            mockMvc = MockMvcBuilders.standaloneSetup(controller)
                    .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                    .build();
        }

        @Test
        @DisplayName("Negative Test: Submit form rỗng đến /artisan/products kỳ vọng trả về View 'artisan/product-form' với BindingResult lỗi")
        void testCreateProduct_EmptyForm_ReturnsFormViewWithBindingErrors() throws Exception {
            // Act & Assert
            mockMvc.perform(post("/artisan/products")
                            .param("productName", "")
                            .param("style", "")
                            .param("age", "-1")
                            .param("height", "0.00")
                            .param("trunkDiameter", "0.00")
                            .param("price", "-500.00"))
                    .andExpect(status().isOk())
                    .andExpect(view().name("artisan/product-form"))
                    .andExpect(model().attributeHasFieldErrors("productForm",
                            "varietyId", "segmentId", "productName", "age", "height", "trunkDiameter", "style", "price"));
        }

        @Test
        @DisplayName("Negative Test: Submit field 'style' chứa số/ký tự đặc biệt vi phạm Pattern")
        void testCreateProduct_InvalidStylePattern_FailsValidation() {
            // Arrange
            ArtisanProductFormDTO form = ArtisanProductFormDTO.builder()
                    .varietyId(1)
                    .segmentId(1)
                    .productName("Cây Tùng La Hán")
                    .age(10)
                    .height(1.5f)
                    .trunkDiameter(0.2f)
                    .style("Style_123!") // Chứa chữ số và ký tự đặc biệt
                    .price(new BigDecimal("5000000"))
                    .build();

            // Act
            Set<ConstraintViolation<ArtisanProductFormDTO>> violations = validator.validate(form);

            // Assert
            assertThat(violations).hasSize(1);
            assertThat(violations.iterator().next().getMessage()).isEqualTo("Dáng cây chỉ được nhập chữ, khoảng trắng và dấu ' -.");
        }

        @Test
        @DisplayName("Negative Test: Submit giá sản phẩm vượt giới hạn DB")
        void testCreateProduct_PriceExceedsLimit_FailsValidation() {
            ArtisanProductFormDTO form = ArtisanProductFormDTO.builder()
                    .varietyId(1)
                    .segmentId(1)
                    .productName("Cây Tùng La Hán")
                    .age(10)
                    .height(1.5f)
                    .trunkDiameter(0.2f)
                    .style("Dáng Trực")
                    .price(new BigDecimal("1000000000000"))
                    .build();

            Set<ConstraintViolation<ArtisanProductFormDTO>> violations = validator.validate(form);

            assertThat(violations)
                    .extracting(ConstraintViolation::getMessage)
                    .contains("Giá sản phẩm không được vượt quá 999.999.999.999 VNĐ.");
        }

        @Test
        @DisplayName("Positive Test: Submit ArtisanProductFormDTO với dữ liệu chuẩn vượt qua kiểm duyệt validation")
        void testCreateProduct_ValidForm_PassesValidation() {
            // Arrange
            ArtisanProductFormDTO form = ArtisanProductFormDTO.builder()
                    .varietyId(1)
                    .segmentId(2)
                    .productName("Cây Sanh Cổ BSMS")
                    .age(15)
                    .height(2.0f)
                    .trunkDiameter(0.3f)
                    .style("Dáng Trực")
                    .price(new BigDecimal("15000000"))
                    .build();

            // Act
            Set<ConstraintViolation<ArtisanProductFormDTO>> violations = validator.validate(form);

            // Assert
            assertThat(violations).isEmpty();
        }
    }
}
