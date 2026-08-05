package com.example.bonsai_shop.livestream.controller;

import com.example.bonsai_shop.entity.LiveLead;
import com.example.bonsai_shop.entity.LiveSession;
import com.example.bonsai_shop.livestream.repository.LiveLeadRepository;
import com.example.bonsai_shop.livestream.repository.LiveSessionRepository;
import com.example.bonsai_shop.livestream.service.LiveStreamService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/live")
@RequiredArgsConstructor
public class LiveStreamApiController {

    private final LiveStreamService liveStreamService;
    private final LiveSessionRepository liveSessionRepository;
    private final LiveLeadRepository liveLeadRepository;
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
                .orElse(ResponseEntity.ok(Map.of("sessionId", (Object) null)));
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
     * POST /api/live/chat - Process a chat message from YouTube or web chat room
     */
    @PostMapping("/chat")
    public ResponseEntity<?> processChat(@RequestBody Map<String, Object> body) {
        try {
            Integer sessionId = Integer.valueOf(body.get("sessionId").toString());
            String author = body.getOrDefault("author", "Ẩn danh").toString();
            String message = body.get("message").toString();
            String time = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"));

            // Broadcast message to all viewers in the chat room via WebSocket
            messagingTemplate.convertAndSend("/topic/live-chat/" + sessionId,
                    (Object) Map.of("author", author, "message", message, "time", time));

            // Also process for lead detection (phone + product code matching)
            liveSessionRepository.findById(sessionId).ifPresent(session ->
                liveStreamService.processComment(author, message, session)
            );
            return ResponseEntity.ok(Map.of("success", true));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * POST /api/live/{sessionId}/fetch-youtube - Manually fetch new YouTube live chat messages
     */
    @PostMapping("/{sessionId}/fetch-youtube")
    public ResponseEntity<?> fetchYouTubeChat(
            @PathVariable Integer sessionId,
            @RequestParam(required = false) String videoId) {
        try {
            liveSessionRepository.findById(sessionId).ifPresent(session -> {
                String vid = videoId != null ? videoId :
                        (session.getStreamUrl() != null ? extractVideoId(session.getStreamUrl()) : null);
                if (vid != null) {
                    liveStreamService.syncYouTubeComments(vid, session);
                }
            });
            return ResponseEntity.ok(Map.of("success", true));
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
