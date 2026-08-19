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

/**
 * Controller cung cấp các API REST điều khiển tính năng Livestream,
 * quản lý phòng chat và theo dõi cơ hội chốt đơn (Leads).
 */
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
     * POST /api/live/start - Khởi chạy một phiên livestream mới.
     * Dữ liệu đầu vào: { "title": "...", "streamUrl": "..." }
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
     * POST /api/live/{sessionId}/end - Đóng phiên livestream hiện tại.
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
     * GET /api/live/status - API kiểm tra nhanh xem hệ thống có đang phát Live hay không.
     * Sử dụng cho hiệu ứng nhấp nháy màu đỏ hiển thị trên nút Live của thanh điều hướng (Navbar).
     */
    @GetMapping("/status")
    public ResponseEntity<?> getLiveStatus() {
        boolean isLive = liveSessionRepository.findFirstByStatusOrderByStartTimeDesc("ONGOING").isPresent();
        return ResponseEntity.ok(Map.of("live", isLive));
    }

    /**
     * GET /api/live/active - Lấy thông tin chi tiết của phiên Live đang chạy hiện tại.
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
     * GET /api/live/sessions/ended - Lấy danh sách các phiên Live Stream đã kết thúc.
     */
    @GetMapping("/sessions/ended")
    public ResponseEntity<List<LiveSession>> getEndedSessions() {
        return ResponseEntity.ok(liveSessionRepository.findByStatusOrderByStartTimeDesc("ENDED"));
    }

    /**
     * GET /api/live/{sessionId}/leads - Lấy toàn bộ danh sách chốt đơn của phiên Live hiện tại.
     */
    @GetMapping("/{sessionId}/leads")
    public ResponseEntity<List<LiveLead>> getLeads(@PathVariable Integer sessionId) {
        return ResponseEntity.ok(liveLeadRepository.findByLiveSessionSessionIdOrderByCreatedAtDesc(sessionId));
    }

    /**
     * PUT /api/live/leads/{leadId}/status - Cập nhật trạng thái xử lý đơn chốt (Ví dụ: Chuyển từ PENDING sang DONE).
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
     * GET /api/live/{sessionId}/chat-history - Lấy lịch sử 200 tin nhắn chat gần nhất.
     * Giúp tải nhanh lịch sử tin nhắn khi khách hàng vào xem live giữa chừng hoặc F5 lại trang.
     */
    @GetMapping("/{sessionId}/chat-history")
    public ResponseEntity<?> getChatHistory(@PathVariable Integer sessionId) {
        // Lấy 200 tin nhắn mới nhất, sau đó đảo ngược thứ tự để hiển thị từ cũ tới mới trên khung chat
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
     * POST /api/live/chat - Nhận tin nhắn chat từ Client (Khung chat trên Web của khách hoặc admin).
     * Sau khi nhận:
     * 1. Lưu tin nhắn vào Database.
     * 2. Phát tin nhắn qua WebSocket tới `/topic/live-chat/{sessionId}` để hiển thị tức thì trên tất cả màn hình.
     * 3. Gọi bộ trích lọc Lead để kiểm tra xem có chứa SĐT hoặc mã sản phẩm hay không.
     */
    @PostMapping("/chat")
    public ResponseEntity<?> processChat(@RequestBody Map<String, Object> body) {
        try {
            Integer sessionId = Integer.valueOf(body.get("sessionId").toString());
            String author = body.getOrDefault("author", "Ẩn danh").toString();
            String message = body.get("message").toString();
            String source = body.getOrDefault("source", "WEB").toString();
            String time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm"));

            // 1. Lưu trữ tin nhắn chat thô vào DB
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

            // 2. Broadcast tin nhắn qua WebSocket tới người xem khác và bảng điều khiển của admin
            messagingTemplate.convertAndSend("/topic/live-chat/" + sessionId,
                    (Object) Map.of("author", author, "message", message, "time", time, "source", source));

            // 3. Phân tích tin nhắn để phát hiện cơ hội chốt đơn
            liveSessionRepository.findById(sessionId).ifPresent(session ->
                liveStreamService.processComment(author, message, session)
            );

            return ResponseEntity.ok(Map.of("success", true));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * POST /api/live/{sessionId}/fetch-youtube - Đồng bộ thủ công các bình luận YouTube Live Chat.
     * Hữu dụng trong trường hợp không chạy cron tự động hoặc cần cưỡng chế kéo bình luận tức thì.
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

    /** Trích xuất Video ID từ đường dẫn đầy đủ của YouTube */
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
