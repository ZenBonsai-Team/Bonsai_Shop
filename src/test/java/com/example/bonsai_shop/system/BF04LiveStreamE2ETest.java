package com.example.bonsai_shop.system;

import com.example.bonsai_shop.customer.repository.RoleRepository;
import com.example.bonsai_shop.customer.repository.UserRepository;
import com.example.bonsai_shop.customer.service.CustomUserDetails;
import com.example.bonsai_shop.entity.Category;
import com.example.bonsai_shop.entity.LiveLead;
import com.example.bonsai_shop.entity.LiveSession;
import com.example.bonsai_shop.entity.Product;
import com.example.bonsai_shop.entity.ProductSegment;
import com.example.bonsai_shop.entity.Role;
import com.example.bonsai_shop.entity.User;
import com.example.bonsai_shop.entity.Variety;
import com.example.bonsai_shop.livestream.repository.LiveLeadRepository;
import com.example.bonsai_shop.livestream.repository.LiveSessionRepository;
import com.example.bonsai_shop.product.repository.CategoryRepository;
import com.example.bonsai_shop.product.repository.ProductRepository;
import com.example.bonsai_shop.product.repository.ProductSegmentRepository;
import com.example.bonsai_shop.product.repository.VarietyRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * L3 SYSTEM TEST (HTTP Flow Testing) cho BF-04: Livestream & Auto Lead Capture.
 * Mô phỏng toàn bộ vòng đời của một phiên Live Stream từ lúc khởi tạo,
 * nhận tin nhắn, tự động chốt Lead bằng regex, cập nhật trạng thái Lead cho đến khi kết thúc phiên.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class BF04LiveStreamE2ETest {

    @Autowired
    private MockMvc mockMvc;

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

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private LiveSessionRepository liveSessionRepository;

    @Autowired
    private LiveLeadRepository liveLeadRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private User moderatorEntity;
    private Product testProduct;

    @BeforeEach
    void setUp() {
        // 1. Tạo vai trò CONTENT_MODERATOR và tài khoản kiểm thử
        Role modRole = roleRepository.findByRoleName("CONTENT_MODERATOR")
                .orElseGet(() -> roleRepository.save(Role.builder().roleName("CONTENT_MODERATOR").build()));

        moderatorEntity = userRepository.findByEmail("moderator.live@test.com")
                .orElseGet(() -> userRepository.save(User.builder()
                        .fullName("Live Moderator")
                        .email("moderator.live@test.com")
                        .username("moderator_live")
                        .password("$2a$10$dummyHash")
                        .role(modRole)
                        .status("ACTIVE")
                        .build()));

        // 2. Tạo Category, Variety, Segment để lưu Product
        Category category = categoryRepository.findAll().stream().findFirst()
                .orElseGet(() -> categoryRepository.save(Category.builder()
                        .categoryName("Live Category")
                        .description("Category for Live Tests")
                        .build()));

        Variety variety = varietyRepository.findAll().stream().findFirst()
                .orElseGet(() -> varietyRepository.save(Variety.builder()
                        .category(category)
                        .varietyName("Bonsai Live Variety")
                        .description("Variety for Live Tests")
                        .build()));

        ProductSegment segment = productSegmentRepository.findAll().stream().findFirst()
                .orElseGet(() -> productSegmentRepository.save(ProductSegment.builder()
                        .segmentName("Standard Live Segment")
                        .build()));

        // 3. Tạo sản phẩm với mã cố định BON-101 để test regex chốt đơn
        testProduct = productRepository.findByProductCode("BON-101")
                .orElseGet(() -> productRepository.save(Product.builder()
                        .productCode("BON-101")
                        .productName("Cây Tùng Bonsai Live")
                        .price(new BigDecimal("5000000"))
                        .productStatus("AVAILABLE")
                        .isVisible(true)
                        .isPublicPrice(true)
                        .variety(variety)
                        .segment(segment)
                        .age(8)
                        .height(55.0f)
                        .trunkDiameter(7.0f)
                        .style("Trực")
                        .description("Cây tùng dùng cho test Live Stream")
                        .createdAt(LocalDateTime.now())
                        .build()));
    }

    private RequestPostProcessor liveModerator() {
        return user(new CustomUserDetails(moderatorEntity,
                List.of(new SimpleGrantedAuthority("ROLE_CONTENT_MODERATOR"))));
    }

    @Test
    @DisplayName("TC-SYS-BF04-LIVE-001: Quy trình đầy đủ từ tạo phiên live, nhận chat chốt lead đến đóng live")
    void tcSysBf04Live001_fullLiveCycleAndLeadCapture() throws Exception {
        // Bước 1: Khởi tạo một phiên Livestream mới (Ongoing)
        Map<String, String> startBody = Map.of(
                "title", "Phiên Live Bán Tùng Đẹp",
                "streamUrl", "https://www.youtube.com/watch?v=testStreamId"
        );

        MvcResult startResult = mockMvc.perform(post("/api/live/start")
                        .with(liveModerator())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(startBody)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.sessionId").exists())
                .andExpect(jsonPath("$.status").value("ONGOING"))
                .andReturn();

        Map<?, ?> responseMap = objectMapper.readValue(startResult.getResponse().getContentAsString(), Map.class);
        Integer sessionId = (Integer) responseMap.get("sessionId");
        assertNotNull(sessionId);

        // Kiểm tra cơ sở dữ liệu xem session đã lưu đúng chưa
        LiveSession sessionInDb = liveSessionRepository.findById(sessionId).orElse(null);
        assertNotNull(sessionInDb);
        assertEquals("ONGOING", sessionInDb.getStatus());
        assertEquals("Phiên Live Bán Tùng Đẹp", sessionInDb.getTitle());

        // Bước 2: Khách hàng gửi bình luận chứa thông tin chốt đơn (Mã cây BON-101 + SĐT 0987654321)
        Map<String, Object> chatBody = Map.of(
                "sessionId", sessionId,
                "author", "Khách Hàng A",
                "message", "Tôi muốn chốt cây BON-101, số điện thoại của tôi là 0987654321 nhé shop!",
                "source", "YOUTUBE"
        );

        mockMvc.perform(post("/api/live/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(chatBody)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        // Kiểm tra xem hệ thống có tự động nhận diện và tạo cơ hội chốt đơn (LiveLead) hay không
        List<LiveLead> leads = liveLeadRepository.findByLiveSessionSessionIdOrderByCreatedAtDesc(sessionId);
        assertFalse(leads.isEmpty(), "Hệ thống phải tự động tạo Lead từ bình luận hợp lệ");
        
        LiveLead capturedLead = leads.get(0);
        assertEquals("Khách Hàng A", capturedLead.getViewerName());
        assertEquals("0987654321", capturedLead.getPhoneNumber());
        assertEquals("CHOT_DON", capturedLead.getIntentType()); // Phải tự nhận diện intent chốt đơn
        assertEquals("PENDING", capturedLead.getLeadStatus());
        assertEquals(testProduct.getProductId(), capturedLead.getProduct().getProductId());

        // Bước 3: Moderator cập nhật trạng thái Lead sang CONTACTED
        Map<String, String> updateBody = Map.of(
                "status", "CONTACTED",
                "notes", "Đã gọi điện thoại chốt đơn với khách hàng thành công"
        );

        mockMvc.perform(put("/api/live/leads/" + capturedLead.getLeadId() + "/status")
                        .with(liveModerator())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateBody)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        // Kiểm tra trạng thái Lead trong DB sau cập nhật
        LiveLead updatedLead = liveLeadRepository.findById(capturedLead.getLeadId()).orElse(null);
        assertNotNull(updatedLead);
        assertEquals("CONTACTED", updatedLead.getLeadStatus());
        assertEquals("Đã gọi điện thoại chốt đơn với khách hàng thành công", updatedLead.getNotes());

        // Bước 4: Kết thúc phiên livestream
        mockMvc.perform(post("/api/live/" + sessionId + "/end")
                        .with(liveModerator()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        // Xác thực trạng thái phiên Live đã chuyển sang ENDED
        LiveSession endedSession = liveSessionRepository.findById(sessionId).orElse(null);
        assertNotNull(endedSession);
        assertEquals("ENDED", endedSession.getStatus());
        assertNotNull(endedSession.getEndTime());
    }
}
