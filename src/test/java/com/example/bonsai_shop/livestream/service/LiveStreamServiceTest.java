package com.example.bonsai_shop.livestream.service;

import com.example.bonsai_shop.customer.repository.UserRepository;
import com.example.bonsai_shop.customer.repository.ModerationNotificationRepository;
import com.example.bonsai_shop.entity.*;
import com.example.bonsai_shop.livestream.repository.LiveChatMessageRepository;
import com.example.bonsai_shop.livestream.repository.LiveLeadRepository;
import com.example.bonsai_shop.livestream.repository.LiveSessionRepository;
import com.example.bonsai_shop.product.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class LiveStreamServiceTest {

    @Mock private LiveSessionRepository liveSessionRepository;
    @Mock private LiveLeadRepository liveLeadRepository;
    @Mock private LiveChatMessageRepository liveChatMessageRepository;
    @Mock private ProductRepository productRepository;
    @Mock private SimpMessagingTemplate messagingTemplate;
    @Mock private UserRepository userRepository;
    @Mock private ModerationNotificationRepository notificationRepository;
    @Mock private RestTemplate mockRestTemplate;

    @InjectMocks
    private LiveStreamService liveStreamService;

    private LiveSession activeSession;

    @BeforeEach
    public void setUp() {
        activeSession = LiveSession.builder()
                .sessionId(1)
                .title("Phiên Live Cũ")
                .status("ONGOING")
                .startTime(LocalDateTime.now())
                .build();
    }

    @Test
    public void testStartSession_EndsPreviousSessionAndNotifiesUsers() {
        // TC-UNIT-LiveService-001
        when(liveSessionRepository.findFirstByStatusOrderByStartTimeDesc("ONGOING"))
                .thenReturn(Optional.of(activeSession));

        User user1 = User.builder().userId(1).email("user1@gmail.com").build();
        when(userRepository.findAll()).thenReturn(List.of(user1));
        when(liveSessionRepository.save(any(LiveSession.class))).thenAnswer(inv -> inv.getArgument(0));

        LiveSession newSession = liveStreamService.startSession("Live Tùng", "url");

        assertEquals("Live Tùng", newSession.getTitle());
        assertEquals("ONGOING", newSession.getStatus());
        assertEquals("ENDED", activeSession.getStatus());
        verify(liveSessionRepository, times(1)).save(activeSession);
        verify(notificationRepository, times(1)).saveAll(anyList());
    }

    @Test
    public void testEndSession_SavesEndedStatusAndDeletesChats() {
        // TC-UNIT-LiveService-002
        when(liveSessionRepository.findById(1)).thenReturn(Optional.of(activeSession));
        when(liveSessionRepository.save(any(LiveSession.class))).thenAnswer(inv -> inv.getArgument(0));

        LiveSession endedSession = liveStreamService.endSession(1);

        assertEquals("ENDED", endedSession.getStatus());
        verify(liveChatMessageRepository, times(1)).deleteByLiveSessionSessionId(1);
    }

    @Test
    public void testProcessComment_NoPhoneAndNoProductCode_ReturnsEmpty() {
        // TC-UNIT-LiveService-003
        Optional<LiveLead> lead = liveStreamService.processComment("A", "Cây đẹp", activeSession);
        assertFalse(lead.isPresent());
    }

    @Test
    public void testProcessComment_ChotDon_Success() {
        // TC-UNIT-LiveService-004
        Product product = Product.builder().productId(10).productCode("BON-101").build();
        when(productRepository.findByProductCode("BON-101")).thenReturn(Optional.of(product));
        when(liveLeadRepository.save(any(LiveLead.class))).thenAnswer(inv -> inv.getArgument(0));

        Optional<LiveLead> leadOpt = liveStreamService.processComment("B", "Chốt BON-101 0987654321", activeSession);

        assertTrue(leadOpt.isPresent());
        LiveLead lead = leadOpt.get();
        assertEquals("CHOT_DON", lead.getIntentType());
        assertEquals("0987654321", lead.getPhoneNumber());
        assertEquals(product, lead.getProduct());
    }

    @Test
    public void testProcessComment_TuVan_Success() {
        // TC-UNIT-LiveService-005
        Product product = Product.builder().productId(11).productCode("BON-102").build();
        when(productRepository.findByProductCode("BON-102")).thenReturn(Optional.of(product));
        when(liveLeadRepository.save(any(LiveLead.class))).thenAnswer(inv -> inv.getArgument(0));

        Optional<LiveLead> leadOpt = liveStreamService.processComment("C", "Tư vấn BON-102 0912345678 nhé", activeSession);

        assertTrue(leadOpt.isPresent());
        assertEquals("TU_VAN", leadOpt.get().getIntentType());
    }

    @Test
    public void testProcessComment_GoiLai_Success() {
        // TC-UNIT-LiveService-006
        when(liveLeadRepository.save(any(LiveLead.class))).thenAnswer(inv -> inv.getArgument(0));

        Optional<LiveLead> leadOpt = liveStreamService.processComment("D", "Gọi lại số 0976543210 cho tôi", activeSession);

        assertTrue(leadOpt.isPresent());
        assertEquals("GOI_LAI", leadOpt.get().getIntentType());
        assertEquals("0976543210", leadOpt.get().getPhoneNumber());
    }

    @Test
    public void testProcessComment_ProductNotFound() {
        // TC-UNIT-LiveService-007
        when(productRepository.findByProductCode("BON-999")).thenReturn(Optional.empty());
        when(liveLeadRepository.save(any(LiveLead.class))).thenAnswer(inv -> inv.getArgument(0));

        Optional<LiveLead> leadOpt = liveStreamService.processComment("E", "chốt BON-999 0987654321", activeSession);

        assertTrue(leadOpt.isPresent());
        assertNull(leadOpt.get().getProduct());
        assertEquals("0987654321", leadOpt.get().getPhoneNumber());
    }

    @Test
    public void testUpdateLeadStatus_Success() {
        // TC-UNIT-LiveService-008
        LiveLead lead = LiveLead.builder().leadId(1).leadStatus("PENDING").build();
        when(liveLeadRepository.findById(1)).thenReturn(Optional.of(lead));
        when(liveLeadRepository.save(any(LiveLead.class))).thenAnswer(inv -> inv.getArgument(0));

        LiveLead updated = liveStreamService.updateLeadStatus(1, "CONTACTED", "Đã tư vấn");

        assertEquals("CONTACTED", updated.getLeadStatus());
        assertEquals("Đã tư vấn", updated.getNotes());
        verify(messagingTemplate, times(1)).convertAndSend(eq("/topic/live-leads-update"), eq(updated));
    }

    @Test
    public void testUpdateLeadStatus_NotFound() {
        // TC-UNIT-LiveService-009
        when(liveLeadRepository.findById(999)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> {
            liveStreamService.updateLeadStatus(999, "CONTACTED", "Note");
        });
    }

    @Test
    public void testSyncYouTubeComments_ApiKeyMissing() {
        // TC-UNIT-LiveService-010
        ReflectionTestUtils.setField(liveStreamService, "youtubeApiKey", "");

        // Should return early and not invoke restTemplate calls
        liveStreamService.syncYouTubeComments("vid", activeSession);
        verifyNoInteractions(mockRestTemplate);
    }

    @Test
    public void testSyncYouTubeComments_Success() {
        // TC-UNIT-LiveService-011
        ReflectionTestUtils.setField(liveStreamService, "youtubeApiKey", "test-key");
        ReflectionTestUtils.setField(liveStreamService, "restTemplate", mockRestTemplate);

        // 1. Mock fetchLiveChatId response
        Map<String, Object> videoResponse = new HashMap<>();
        Map<String, Object> item = new HashMap<>();
        Map<String, Object> liveStreamingDetails = new HashMap<>();
        liveStreamingDetails.put("activeLiveChatId", "chat-id-123");
        item.put("liveStreamingDetails", liveStreamingDetails);
        videoResponse.put("items", List.of(item));

        when(mockRestTemplate.getForObject(contains("/videos"), eq(Map.class))).thenReturn(videoResponse);

        // 2. Mock liveChat/messages response
        Map<String, Object> chatResponse = new HashMap<>();
        chatResponse.put("nextPageToken", "token123");
        
        Map<String, Object> messageItem = new HashMap<>();
        Map<String, Object> snippet = new HashMap<>();
        Map<String, Object> textMessageDetails = new HashMap<>();
        textMessageDetails.put("messageText", "chốt BON-101 0987654321");
        snippet.put("textMessageDetails", textMessageDetails);
        
        Map<String, Object> authorDetails = new HashMap<>();
        authorDetails.put("displayName", "User YT");
        
        messageItem.put("snippet", snippet);
        messageItem.put("authorDetails", authorDetails);
        chatResponse.put("items", List.of(messageItem));

        when(mockRestTemplate.getForObject(contains("/liveChat/messages"), eq(Map.class))).thenReturn(chatResponse);
        when(productRepository.findByProductCode("BON-101")).thenReturn(Optional.empty());

        liveStreamService.syncYouTubeComments("vid", activeSession);

        verify(liveChatMessageRepository, times(1)).save(any(LiveChatMessage.class));
        verify(liveLeadRepository, times(1)).save(any(LiveLead.class));
    }

    @Test
    public void testSyncYouTubeComments_Exception() {
        // TC-UNIT-LiveService-012
        ReflectionTestUtils.setField(liveStreamService, "youtubeApiKey", "test-key");
        ReflectionTestUtils.setField(liveStreamService, "restTemplate", mockRestTemplate);

        // Mock ném exception
        when(mockRestTemplate.getForObject(anyString(), eq(Map.class))).thenThrow(new RuntimeException("API Error"));

        // Should catch the exception without crashing
        assertDoesNotThrow(() -> {
            liveStreamService.syncYouTubeComments("vid", activeSession);
        });
    }
}
