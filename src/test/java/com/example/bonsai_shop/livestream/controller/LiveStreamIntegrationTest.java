package com.example.bonsai_shop.livestream.controller;

import com.example.bonsai_shop.entity.*;
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
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * L2 INTEGRATION TEST (Chiêu 1 & Chiêu 2) cho Livestream.
 * Kết nối Controller -> Service -> Repository -> Database để kiểm tra luồng
 * chốt đơn trên livestream.
 */
@SpringBootTest
@Transactional // Tự động rollback dữ liệu để giữ môi trường sạch sẽ
public class LiveStreamIntegrationTest {

        @Autowired
        private WebApplicationContext context;

        private MockMvc mockMvc;

        @Autowired
        private LiveSessionRepository liveSessionRepository;

        @Autowired
        private LiveLeadRepository liveLeadRepository;

        @Autowired
        private ProductRepository productRepository;

        @Autowired
        private CategoryRepository categoryRepository;

        @Autowired
        private VarietyRepository varietyRepository;

        @Autowired
        private ProductSegmentRepository productSegmentRepository;

        private LiveSession ongoingSession;
        private Product testProduct;

        @BeforeEach
        void setUp() {
                // Thiết lập MockMvc từ Spring context
                mockMvc = MockMvcBuilders.webAppContextSetup(context)
                                .apply(SecurityMockMvcConfigurers.springSecurity())
                                .build();

                // Gieo các thực thể bắt buộc của Product
                Category cat = categoryRepository.save(Category.builder().categoryName("Tùng").build());
                Variety var = varietyRepository
                                .save(Variety.builder().varietyName("Tùng La Hán").category(cat).build());
                ProductSegment seg = productSegmentRepository
                                .save(ProductSegment.builder().segmentName("Cao Cấp").build());

                // Gieo dữ liệu Product (Seed Data)
                testProduct = Product.builder()
                                .productCode("BON-101")
                                .productName("Premium Tùng Bonsai")
                                .price(new BigDecimal("15000000"))
                                .productStatus("AVAILABLE")
                                .variety(var)
                                .segment(seg)
                                .build();
                testProduct = productRepository.save(testProduct);

                ongoingSession = LiveSession.builder()
                                .title("Special Bonsai Auction Live")
                                .streamUrl("https://youtube.com/live/dummy")
                                .status("ONGOING")
                                .build();
                ongoingSession = liveSessionRepository.save(ongoingSession);
        }

        @Test
        @DisplayName("TC-INT-LiveStream-001: Bắt đầu phiên LiveStream thành công và lưu vào DB")
        @WithMockUser(username = "artisan@bsms.com", roles = "ARTISAN")
        void testStartSession_CreatesActiveSessionInDatabase() throws Exception {
                String uniqueTitle = "New Live Session " + System.currentTimeMillis();
                Map<String, String> body = new HashMap<>();
                body.put("title", uniqueTitle);
                body.put("streamUrl", "https://youtube.com/live/new");

                mockMvc.perform(post("/api/live/start").with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(new ObjectMapper().writeValueAsString(body)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.success").value(true));

                // Kiểm chứng DB State: Kiểm tra xem session mới có ở trạng thái ONGOING không
                Optional<LiveSession> newSessionOpt = liveSessionRepository.findAll().stream()
                                .filter(s -> uniqueTitle.equals(s.getTitle()))
                                .findFirst();

                assertThat(newSessionOpt).isPresent();
                assertThat(newSessionOpt.get().getStatus()).isEqualTo("ONGOING");
        }

        @Test
        @DisplayName("TC-INT-LiveStream-004: Gửi tin nhắn chốt đơn phân tích lưu khách hàng tiềm năng vào DB")
        @WithMockUser(username = "customer@bsms.com", roles = "CUSTOMER")
        void testProcessChat_ChotDon_CreatesLiveLeadInDatabase() throws Exception {
                String uniqueViewer = "Khách hàng A " + System.currentTimeMillis();
                Map<String, Object> body = new HashMap<>();
                body.put("sessionId", ongoingSession.getSessionId());
                body.put("author", uniqueViewer);
                body.put("message", "Chốt BON-101 0987654321");
                body.put("source", "WEB");

                mockMvc.perform(post("/api/live/chat").with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(new ObjectMapper().writeValueAsString(body)))
                                .andExpect(status().isOk());

                // Kiểm chứng DB State: Hệ thống tự động phân tích cú pháp tạo ra khách hàng
                // tiềm năng chốt đơn (LiveLead) trong DB
                Optional<LiveLead> leadOpt = liveLeadRepository.findAll().stream()
                                .filter(l -> uniqueViewer.equals(l.getViewerName()))
                                .findFirst();

                assertThat(leadOpt).isPresent();
                LiveLead lead = leadOpt.get();
                assertThat(lead.getIntentType()).isEqualTo("CHOT_DON");
                assertThat(lead.getPhoneNumber()).isEqualTo("0987654321");
                assertThat(lead.getProduct().getProductId()).isEqualTo(testProduct.getProductId());
        }

        @Test
        @DisplayName("TC-INT-LiveStream-006: Cập nhật trạng thái khách hàng tiềm năng lưu vào DB")
        @WithMockUser(username = "artisan@bsms.com", roles = "ARTISAN")
        void testUpdateLeadStatus_PersistsInDatabase() throws Exception {
                String uniqueViewer = "Khách hàng B " + System.currentTimeMillis();
                // Tạo trước một Lead chờ tư vấn (PENDING)
                LiveLead pendingLead = LiveLead.builder()
                                .liveSession(ongoingSession)
                                .viewerName(uniqueViewer)
                                .rawComment("Tư vấn cây cảnh")
                                .intentType("TU_VAN")
                                .leadStatus("PENDING")
                                .build();
                pendingLead = liveLeadRepository.save(pendingLead);

                Map<String, String> body = new HashMap<>();
                body.put("status", "CONTACTED");
                body.put("notes", "Đã liên hệ tư vấn thành công");

                mockMvc.perform(put("/api/live/leads/" + pendingLead.getLeadId() + "/status").with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(new ObjectMapper().writeValueAsString(body)))
                                .andExpect(status().isOk());

                // Kiểm chứng DB State: Kiểm tra xem Lead đã lưu trạng thái mới chưa
                LiveLead updatedLead = liveLeadRepository.findById(pendingLead.getLeadId()).orElseThrow();
                assertThat(updatedLead.getLeadStatus()).isEqualTo("CONTACTED");
                assertThat(updatedLead.getNotes()).isEqualTo("Đã liên hệ tư vấn thành công");
        }
}
