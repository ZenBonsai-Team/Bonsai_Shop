package com.example.bonsai_shop.livestream.controller;

import com.example.bonsai_shop.entity.*;
import com.example.bonsai_shop.livestream.repository.LiveChatMessageRepository;
import com.example.bonsai_shop.livestream.repository.LiveLeadRepository;
import com.example.bonsai_shop.livestream.repository.LiveSessionRepository;
import com.example.bonsai_shop.livestream.service.LiveStreamService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class LiveStreamApiControllerTest {

    @Mock private LiveStreamService liveStreamService;
    @Mock private LiveSessionRepository liveSessionRepository;
    @Mock private LiveLeadRepository liveLeadRepository;
    @Mock private LiveChatMessageRepository liveChatMessageRepository;
    @Mock private SimpMessagingTemplate messagingTemplate;

    @InjectMocks
    private LiveStreamApiController liveStreamApiController;

    private LiveSession activeSession;

    @BeforeEach
    public void setUp() {
        activeSession = LiveSession.builder()
                .sessionId(1)
                .title("Phiên Live Test")
                .status("ONGOING")
                .startTime(LocalDateTime.now())
                .build();
    }

    @Test
    public void testStartSession_Success() {
        // TC-UNIT-LiveAPI-001
        Map<String, String> body = new HashMap<>();
        body.put("title", "Live Tùng");
        body.put("streamUrl", "url");

        when(liveStreamService.startSession("Live Tùng", "url")).thenReturn(activeSession);

        ResponseEntity<?> response = liveStreamApiController.startSession(body);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        Map<String, Object> respMap = (Map<String, Object>) response.getBody();
        assertTrue((Boolean) respMap.get("success"));
        assertEquals(1, respMap.get("sessionId"));
    }

    @Test
    public void testStartSession_Exception() {
        // TC-UNIT-LiveAPI-002
        Map<String, String> body = new HashMap<>();
        when(liveStreamService.startSession(anyString(), anyString())).thenThrow(new RuntimeException("Error starting"));

        ResponseEntity<?> response = liveStreamApiController.startSession(body);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    public void testEndSession_Success() {
        // TC-UNIT-LiveAPI-003
        when(liveStreamService.endSession(1)).thenReturn(activeSession);

        ResponseEntity<?> response = liveStreamApiController.endSession(1);

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    public void testEndSession_Exception() {
        // TC-UNIT-LiveAPI-004
        when(liveStreamService.endSession(999)).thenThrow(new IllegalArgumentException("Not found"));

        ResponseEntity<?> response = liveStreamApiController.endSession(999);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    public void testGetLiveStatus_True() {
        // TC-UNIT-LiveAPI-005
        when(liveSessionRepository.findFirstByStatusOrderByStartTimeDesc("ONGOING"))
                .thenReturn(Optional.of(activeSession));

        ResponseEntity<?> response = liveStreamApiController.getLiveStatus();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        Map<String, Object> resp = (Map<String, Object>) response.getBody();
        assertTrue((Boolean) resp.get("live"));
    }

    @Test
    public void testGetActiveSession_None() {
        // TC-UNIT-LiveAPI-006
        when(liveSessionRepository.findFirstByStatusOrderByStartTimeDesc("ONGOING"))
                .thenReturn(Optional.empty());

        ResponseEntity<?> response = liveStreamApiController.getActiveSession();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        Map<String, Object> resp = (Map<String, Object>) response.getBody();
        assertNull(resp.get("sessionId"));
    }

    @Test
    public void testGetLeads_Success() {
        // TC-UNIT-LiveAPI-007
        LiveLead lead = LiveLead.builder().leadId(1).viewerName("Khách A").build();
        when(liveLeadRepository.findByLiveSessionSessionIdOrderByCreatedAtDesc(1))
                .thenReturn(List.of(lead));

        ResponseEntity<List<LiveLead>> response = liveStreamApiController.getLeads(1);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().size());
    }

    @Test
    public void testUpdateLeadStatus_Success() {
        // TC-UNIT-LiveAPI-008
        LiveLead lead = LiveLead.builder().leadId(1).build();
        when(liveLeadRepository.findById(1)).thenReturn(Optional.of(lead));

        Map<String, String> body = new HashMap<>();
        body.put("status", "CONTACTED");
        body.put("notes", "Đã tư vấn");

        ResponseEntity<?> response = liveStreamApiController.updateLeadStatus(1, body);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("CONTACTED", lead.getLeadStatus());
        assertEquals("Đã tư vấn", lead.getNotes());
        verify(liveLeadRepository, times(1)).save(lead);
    }

    @Test
    public void testUpdateLeadStatus_Exception() {
        // TC-UNIT-LiveAPI-009
        when(liveLeadRepository.findById(999)).thenReturn(Optional.empty());

        ResponseEntity<?> response = liveStreamApiController.updateLeadStatus(999, new HashMap<>());

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    public void testGetChatHistory_Empty() {
        // TC-UNIT-LiveAPI-010
        when(liveChatMessageRepository.findTop200ByLiveSessionSessionIdOrderBySentAtDesc(1))
                .thenReturn(Collections.emptyList());

        ResponseEntity<?> response = liveStreamApiController.getChatHistory(1);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(((List) response.getBody()).isEmpty());
    }

    @Test
    public void testGetChatHistory_Reversed() {
        // TC-UNIT-LiveAPI-011
        LiveChatMessage msg1 = LiveChatMessage.builder()
                .author("Khách A").message("Chào").sentAt(LocalDateTime.of(2026, 8, 7, 10, 0)).build();
        LiveChatMessage msg2 = LiveChatMessage.builder()
                .author("Khách B").message("Hỏi giá").sentAt(LocalDateTime.of(2026, 8, 7, 10, 5)).build();

        when(liveChatMessageRepository.findTop200ByLiveSessionSessionIdOrderBySentAtDesc(1))
                .thenReturn(Arrays.asList(msg2, msg1));

        ResponseEntity<?> response = liveStreamApiController.getChatHistory(1);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        List<Map<String, String>> list = (List<Map<String, String>>) response.getBody();
        assertEquals(2, list.size());
        assertEquals("Khách A", list.get(0).get("author"));
        assertEquals("Khách B", list.get(1).get("author"));
    }

    @Test
    public void testProcessChat_Success() {
        // TC-UNIT-LiveAPI-012
        when(liveSessionRepository.findById(1)).thenReturn(Optional.of(activeSession));

        Map<String, Object> body = new HashMap<>();
        body.put("sessionId", 1);
        body.put("author", "Khách A");
        body.put("message", "Chào");
        body.put("source", "WEB");

        ResponseEntity<?> response = liveStreamApiController.processChat(body);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(liveChatMessageRepository, times(1)).save(any(LiveChatMessage.class));
        verify(messagingTemplate, times(1)).convertAndSend(eq("/topic/live-chat/1"), any(Object.class));
        verify(liveStreamService, times(1)).processComment(eq("Khách A"), eq("Chào"), eq(activeSession));
    }
}
