package com.example.bonsai_shop.livestream.controller;

import com.example.bonsai_shop.entity.LiveChatMessage;
import com.example.bonsai_shop.entity.LiveLead;
import com.example.bonsai_shop.entity.LiveSession;
import com.example.bonsai_shop.livestream.repository.LiveChatMessageRepository;
import com.example.bonsai_shop.livestream.repository.LiveLeadRepository;
import com.example.bonsai_shop.livestream.repository.LiveSessionRepository;
import com.example.bonsai_shop.livestream.service.LiveStreamService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/live")
@RequiredArgsConstructor
public class LiveStreamApiController {

    private final LiveStreamService liveStreamService;
    private final LiveSessionRepository liveSessionRepository;
    private final LiveLeadRepository liveLeadRepository;
    private final LiveChatMessageRepository liveChatMessageRepository;
    private final SimpMessagingTemplate messagingTemplate;

    /**
     * POST /api/live/start - Start a new live session
     */
    @PostMapping("/start")
    public ResponseEntity<?> startSession(@RequestBody Map<String, String> body) {
        try {
            String title = body.getOrDefault("title", "Phiên Live Bonsai");
            String streamUrl = body.getOrDefault("streamUrl", "");
            LiveSession session = liveStreamService.startSession(title, streamUrl);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "sessionId", session.getSessionId(),
                    "title", session.getTitle(),
                    "status", session.getStatus()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * POST /api/live/{sessionId}/end - End a live session
     */
    @PostMapping("/{sessionId}/end")
    public ResponseEntity<?> endSession(@PathVariable Integer sessionId) {
        try {
            liveStreamService.endSession(sessionId);
            return ResponseEntity.ok(Map.of("success", true));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * GET /api/live/status - Lightweight status check: is there an active live session?
     * Used by the floating live bubble on the navbar to update its appearance.
     */
    @GetMapping("/status")
    public ResponseEntity<?> getLiveStatus() {
        boolean isLive = liveSessionRepository.findFirstByStatusOrderByStartTimeDesc("ONGOING").isPresent();
        return ResponseEntity.ok(Map.of("live", isLive));
    }

    /**
     * GET /api/live/active - Get currently active session
     */
    @GetMapping("/active")
    public ResponseEntity<?> getActiveSession() {
        return liveSessionRepository.findFirstByStatusOrderByStartTimeDesc("ONGOING")
                .map(session -> ResponseEntity.ok((Object) Map.of(
                        "sessionId", session.getSessionId(),
                        "title", session.getTitle(),
                        "streamUrl", session.getStreamUrl() != null ? session.getStreamUrl() : "",
                        "status", session.getStatus(),
                        "startTime", session.getStartTime().toString()
                )))
                .orElse(ResponseEntity.ok(java.util.Collections.singletonMap("sessionId", null)));
    }

    /**
     * GET /api/live/{sessionId}/leads - Get all leads for a session
     */
    @GetMapping("/{sessionId}/leads")
    public ResponseEntity<List<LiveLead>> getLeads(@PathVariable Integer sessionId) {
        return ResponseEntity.ok(liveLeadRepository.findByLiveSessionSessionIdOrderByCreatedAtDesc(sessionId));
    }

    /**
     * PUT /api/live/leads/{leadId}/status - Update lead status
     */
    @PutMapping("/leads/{leadId}/status")
    public ResponseEntity<?> updateLeadStatus(
            @PathVariable Integer leadId,
            @RequestBody Map<String, String> body) {
        try {
            LiveLead lead = liveLeadRepository.findById(leadId)
                    .orElseThrow(() -> new IllegalArgumentException("Lead not found: " + leadId));
            lead.setLeadStatus(body.get("status"));
            if (body.containsKey("notes")) {
                lead.setNotes(body.get("notes"));
            }
            liveLeadRepository.save(lead);
            return ResponseEntity.ok(Map.of("success", true));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * GET /api/live/{sessionId}/chat-history - Load last 200 persisted chat messages.
     * Only the most recent 200 messages are returned (sorted chronologically oldest→newest).
     * This ensures page reload is fast regardless of how many total messages were sent.
     */
    @GetMapping("/{sessionId}/chat-history")
    public ResponseEntity<?> getChatHistory(@PathVariable Integer sessionId) {
        // Fetch last 200 (DESC), then reverse to show oldest→newest in UI
        List<LiveChatMessage> messages = liveChatMessageRepository
                .findTop200ByLiveSessionSessionIdOrderBySentAtDesc(sessionId);
        java.util.Collections.reverse(messages);

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("HH:mm");
        List<Map<String, String>> result = messages.stream()
                .map(m -> Map.of(
                        "author", m.getAuthor(),
                        "message", m.getMessage(),
                        "time", m.getSentAt().format(fmt),
                        "source", m.getSource() != null ? m.getSource() : "WEB"
                ))
                .collect(Collectors.toList());
        return ResponseEntity.ok(result);
    }

    /**
     * POST /api/live/chat - Process a chat message from user web chat or moderator panel.
     *
     * Flow:
     *  1. Save message to DB (for persistence / history reload)
     *  2. Broadcast to /topic/live-chat/{sessionId} so BOTH moderator and viewer see it in real-time
     *  3. Process for lead detection (phone + product code)
     */
    @PostMapping("/chat")
    public ResponseEntity<?> processChat(@RequestBody Map<String, Object> body) {
        try {
            Integer sessionId = Integer.valueOf(body.get("sessionId").toString());
            String author = body.getOrDefault("author", "Ẩn danh").toString();
            String message = body.get("message").toString();
            String source = body.getOrDefault("source", "WEB").toString();
            String time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm"));

            // 1. Persist message to DB
            liveSessionRepository.findById(sessionId).ifPresent(session -> {
                LiveChatMessage chatMsg = LiveChatMessage.builder()
                        .liveSession(session)
                        .author(author)
                        .message(message)
                        .source(source)
                        .sentAt(LocalDateTime.now())
                        .build();
                liveChatMessageRepository.save(chatMsg);
            });

            // 2. Broadcast to ALL subscribers (both moderator and viewer pages)
            messagingTemplate.convertAndSend("/topic/live-chat/" + sessionId,
                    (Object) Map.of("author", author, "message", message, "time", time, "source", source));

            // 3. Lead detection (phone + product code)
            liveSessionRepository.findById(sessionId).ifPresent(session ->
                liveStreamService.processComment(author, message, session)
            );

            return ResponseEntity.ok(Map.of("success", true));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * POST /api/live/{sessionId}/fetch-youtube - Manually fetch new YouTube live chat messages.
     *
     * Note: YouTube Data API v3 key must be configured in application.properties as youtube.api.key.
     * Without it, this endpoint will return success=false with an explanation message.
     */
    @PostMapping("/{sessionId}/fetch-youtube")
    public ResponseEntity<?> fetchYouTubeChat(
            @PathVariable Integer sessionId,
            @RequestParam(required = false) String videoId) {
        try {
            boolean[] synced = {false};
            liveSessionRepository.findById(sessionId).ifPresent(session -> {
                String vid = videoId != null ? videoId :
                        (session.getStreamUrl() != null ? extractVideoId(session.getStreamUrl()) : null);
                if (vid != null) {
                    liveStreamService.syncYouTubeComments(vid, session);
                    synced[0] = true;
                }
            });
            if (synced[0]) {
                return ResponseEntity.ok(Map.of("success", true));
            } else {
                return ResponseEntity.ok(Map.of(
                        "success", false,
                        "message", "Không tìm thấy Video ID hoặc YouTube API Key chưa được cấu hình."
                ));
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /** Extract YouTube video ID from URL */
    private String extractVideoId(String url) {
        if (url == null) return null;
        if (url.contains("v=")) {
            int idx = url.indexOf("v=") + 2;
            int end = url.indexOf("&", idx);
            return end == -1 ? url.substring(idx) : url.substring(idx, end);
        }
        return url;
    }
}
