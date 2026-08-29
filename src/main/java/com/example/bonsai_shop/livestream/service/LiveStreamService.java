package com.example.bonsai_shop.livestream.service;

import com.example.bonsai_shop.entity.LiveChatMessage;
import com.example.bonsai_shop.entity.LiveLead;
import com.example.bonsai_shop.entity.LiveSession;
import com.example.bonsai_shop.entity.Product;
import com.example.bonsai_shop.livestream.repository.LiveChatMessageRepository;
import com.example.bonsai_shop.livestream.repository.LiveLeadRepository;
import com.example.bonsai_shop.livestream.repository.LiveSessionRepository;
import com.example.bonsai_shop.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.example.bonsai_shop.customer.repository.UserRepository;
import com.example.bonsai_shop.customer.repository.ModerationNotificationRepository;
import com.example.bonsai_shop.entity.ModerationNotification;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Lớp dịch vụ quản lý toàn bộ nghiệp vụ Live Stream của hệ thống.
 * Bao gồm: Quản lý phiên Live, đồng bộ tin nhắn từ YouTube Live API,
 * phát tin nhắn thời gian thực qua WebSocket, tự động quét đơn chốt (Leads)
 * bằng Regex.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class LiveStreamService {

    private final LiveSessionRepository liveSessionRepository;
    private final LiveLeadRepository liveLeadRepository;
    private final LiveChatMessageRepository liveChatMessageRepository;
    private final ProductRepository productRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final UserRepository userRepository;
    private final ModerationNotificationRepository notificationRepository;

    @Value("${youtube.api.key:}")
    private String youtubeApiKey;

    private final RestTemplate restTemplate = new RestTemplate();

    // Lưu cache để ánh xạ từ YouTube Video ID sang Live Chat ID
    private final Map<String, String> videoIdToChatIdCache = new HashMap<>();

    // Lưu token trang tiếp theo phục vụ việc phân trang kéo bình luận từ YouTube
    // API
    private final Map<String, String> videoIdToNextPageToken = new HashMap<>();

    // Biểu thức chính quy phát hiện số điện thoại di động Việt Nam (10 số, bắt đầu
    // bằng các đầu số phổ biến)
    private static final Pattern PHONE_PATTERN = Pattern.compile("(03|05|07|08|09)\\d{8}");

    // Biểu thức chính quy phát hiện mã sản phẩm (Ví dụ: BON-001, BON-1234)
    private static final Pattern PRODUCT_CODE_PATTERN = Pattern.compile("\\b(BON-\\d{3,5})\\b",
            Pattern.CASE_INSENSITIVE);

    /**
     * Bắt đầu một phiên Live Stream mới.
     * Hệ thống sẽ tự động chuyển các phiên Live đang chạy trước đó sang trạng thái
     * kết thúc (ENDED).
     */
    public LiveSession startSession(String title, String streamUrl) {
        // Đóng các phiên Live đang diễn ra (nếu có) trước khi mở phiên mới
        liveSessionRepository.findFirstByStatusOrderByStartTimeDesc("ONGOING")
                .ifPresent(session -> {
                    session.setStatus("ENDED");
                    session.setEndTime(LocalDateTime.now());
                    liveSessionRepository.save(session);
                });

        LiveSession session = LiveSession.builder()
                .title(title)
                .streamUrl(streamUrl)
                .status("ONGOING")
                .startTime(LocalDateTime.now())
                .build();
        LiveSession saved = liveSessionRepository.save(session);

        // Gửi thông báo hệ thống (Notification) tới toàn bộ thành viên trong hệ thống
        try {
            List<com.example.bonsai_shop.entity.User> allUsers = userRepository.findAll();
            List<ModerationNotification> notifications = new ArrayList<>();
            for (com.example.bonsai_shop.entity.User u : allUsers) {
                if (u.getEmail() != null && !u.getEmail().trim().isEmpty()) {
                    notifications.add(ModerationNotification.builder()
                            .targetUsername(u.getEmail())
                            .message("🔴 Nhà vườn Minh Kỷ Garden đang phát trực tiếp: \"" + title
                                    + "\". Hãy tham gia xem ngay!")
                            .isRead(false)
                            .createdAt(LocalDateTime.now())
                            .build());
                }
            }
            if (!notifications.isEmpty()) {
                notificationRepository.saveAll(notifications);
            }
        } catch (Exception e) {
            log.error("Failed to notify users about live session: ", e);
        }

        return saved;
    }

    /**
     * Kết thúc phiên Live Stream theo Session ID.
     * Giải phóng dữ liệu tin nhắn tạm thời trong DB để giảm tải lưu trữ.
     */
    public LiveSession endSession(Integer sessionId) {
        LiveSession session = liveSessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Session not found with ID: " + sessionId));
        session.setStatus("ENDED");
        session.setEndTime(LocalDateTime.now());
        LiveSession saved = liveSessionRepository.save(session);

        // Xóa sạch tin nhắn chat thô trong database vì không cần thiết sau khi live kết
        // thúc.
        // Chỉ giữ lại các bản ghi Leads chốt đơn (live_lead) được lưu trữ vĩnh viễn để
        // gọi chốt đơn.
        liveChatMessageRepository.deleteByLiveSessionSessionId(sessionId);
        log.info("Cleaned up chat messages for ended session #{}", sessionId);

        return saved;
    }

    /**
     * Lấy phiên Live Stream đang phát trực tiếp hiện tại.
     */
    public Optional<LiveSession> getActiveSession() {
        return liveSessionRepository.findFirstByStatusOrderByStartTimeDesc("ONGOING");
    }

    /**
     * Lấy tất cả lịch sử các phiên Live Stream.
     */
    public List<LiveSession> getAllSessions() {
        return liveSessionRepository.findAll();
    }

    /**
     * Lấy danh sách chốt đơn (Leads) phát sinh theo Session ID.
     */
    public List<LiveLead> getLeadsBySession(Integer sessionId) {
        return liveLeadRepository.findByLiveSessionSessionIdOrderByCreatedAtDesc(sessionId);
    }

    /**
     * Cập nhật trạng thái xử lý của Lead và gửi tín hiệu đồng bộ lên giao diện qua
     * WebSocket.
     */
    public LiveLead updateLeadStatus(Integer leadId, String status, String notes) {
        LiveLead lead = liveLeadRepository.findById(leadId)
                .orElseThrow(() -> new IllegalArgumentException("Lead not found with ID: " + leadId));
        lead.setLeadStatus(status);
        lead.setNotes(notes);
        LiveLead saved = liveLeadRepository.save(lead);

        // Phát tín hiệu cập nhật trạng thái Lead cho toàn bộ giao diện quản trị đang
        // lắng nghe
        messagingTemplate.convertAndSend("/topic/live-leads-update", saved);
        return saved;
    }

    /**
     * Thuật toán phân tích nội dung bình luận để phát hiện số điện thoại (SĐT) & mã
     * sản phẩm.
     * Nếu phát hiện bất kỳ một trong hai thông tin này, bình luận sẽ được lưu thành
     * một bản ghi "Khách hàng tiềm năng" (LiveLead).
     */
    public Optional<LiveLead> processComment(String viewerName, String commentContent, LiveSession session) {
        Matcher phoneMatcher = PHONE_PATTERN.matcher(commentContent);
        Matcher productMatcher = PRODUCT_CODE_PATTERN.matcher(commentContent);

        // 1. Quét tìm số điện thoại trong bình luận
        String phoneNumber = null;
        if (phoneMatcher.find()) {
            phoneNumber = phoneMatcher.group();
        }

        // 2. Quét tìm mã sản phẩm (BON-xxx) trong bình luận
        String productCode = null;
        Product product = null;
        if (productMatcher.find()) {
            productCode = productMatcher.group().toUpperCase();
            Optional<Product> productOpt = productRepository.findByProductCode(productCode);
            if (productOpt.isPresent()) {
                product = productOpt.get();
            }
        }

        // Nếu bình luận không chứa SĐT và cũng không chứa mã cây, bỏ qua (không tạo
        // Lead)
        if (phoneNumber == null && productCode == null) {
            return Optional.empty();
        }

        // 3. Phân loại mục đích bình luận (Chốt đơn, Cần tư vấn, hay Gọi lại sau)
        String intentType = classifyIntent(commentContent);

        LiveLead lead = LiveLead.builder()
                .liveSession(session)
                .product(product)
                .viewerName(viewerName)
                .phoneNumber(phoneNumber)
                .rawComment(commentContent)
                .intentType(intentType)
                .leadStatus("PENDING")
                .build();

        LiveLead saved = liveLeadRepository.save(lead);

        // 4. Phát tín hiệu đẩy tin thông báo Lead mới nhảy lên màn hình Admin qua
        // WebSocket
        messagingTemplate.convertAndSend("/topic/live-leads/" + session.getSessionId(), saved);
        messagingTemplate.convertAndSend("/topic/live-leads", saved);

        return Optional.of(saved);
    }

    /**
     * Phân loại mục đích của bình luận dựa trên từ khoá tiếng Việt/tiếng Anh phổ
     * thông.
     */
    private String classifyIntent(String text) {
        String lower = text.toLowerCase();
        // Nhóm từ khóa chốt mua hàng
        if (lower.contains("chốt") || lower.contains("chot") || lower.contains("mua") || lower.contains("lấy")
                || lower.contains("lay") || lower.contains("order") || lower.contains("đặt") || lower.contains("dat")) {
            return "CHOT_DON";
            // Nhóm từ khóa hỏi han giá cả, cần tư vấn thêm
        } else if (lower.contains("tư vấn") || lower.contains("tu van") || lower.contains("hỏi")
                || lower.contains("hoi")
                || lower.contains("xin giá") || lower.contains("xin gia") || lower.contains("bao gia")
                || lower.contains("giá bao nhiêu")) {
            return "TU_VAN";
            // Mặc định là cần gọi lại tư vấn chung
        } else {
            return "GOI_LAI";
        }
    }

    /**
     * Phương thức đồng bộ tin nhắn thời gian thực từ YouTube Live Chat API về hệ
     * thống.
     * Yêu cầu biến cấu hình 'youtube.api.key' phải được thiết lập hợp lệ trong
     * application.properties.
     */
    public void syncYouTubeComments(String youtubeVideoId, LiveSession session) {
        if (youtubeApiKey == null || youtubeApiKey.isEmpty()) {
            log.warn("YouTube API Key is missing. Skipping YouTube comment sync.");
            return;
        }

        try {
            // Lấy ID khung chat trực tiếp của luồng stream
            String liveChatId = videoIdToChatIdCache.computeIfAbsent(youtubeVideoId, this::fetchLiveChatId);
            if (liveChatId == null) {
                return;
            }

            String nextPageToken = videoIdToNextPageToken.get(youtubeVideoId);
            String url = "https://www.googleapis.com/youtube/v3/liveChat/messages?liveChatId=" + liveChatId
                    + "&part=snippet,authorDetails&maxResults=200&key=" + youtubeApiKey;
            if (nextPageToken != null) {
                url += "&pageToken=" + nextPageToken;
            }

            // Gọi API của Google để kéo bình luận
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);
            if (response != null) {
                videoIdToNextPageToken.put(youtubeVideoId, (String) response.get("nextPageToken"));
                List<Map<String, Object>> items = (List<Map<String, Object>>) response.get("items");
                if (items != null) {
                    String fmt = DateTimeFormatter.ofPattern("HH:mm").format(LocalDateTime.now());
                    for (Map<String, Object> item : items) {
                        Map<String, Object> snippet = (Map<String, Object>) item.get("snippet");
                        Map<String, Object> authorDetails = (Map<String, Object>) item.get("authorDetails");

                        if (snippet != null && authorDetails != null) {
                            String displayName = (String) authorDetails.get("displayName");
                            Map<String, Object> textMessageDetails = (Map<String, Object>) snippet
                                    .get("textMessageDetails");
                            if (textMessageDetails != null) {
                                String messageText = (String) textMessageDetails.get("messageText");

                                // A. Lưu tin nhắn vào lịch sử chat database
                                LiveChatMessage chatMsg = LiveChatMessage.builder()
                                        .liveSession(session)
                                        .author(displayName)
                                        .message(messageText)
                                        .source("YOUTUBE")
                                        .sentAt(LocalDateTime.now())
                                        .build();
                                liveChatMessageRepository.save(chatMsg);

                                // B. Phát tin nhắn tức thì qua WebSocket để hiển thị lên UI của khách hàng &
                                // quản trị viên
                                messagingTemplate.convertAndSend("/topic/live-chat/" + session.getSessionId(),
                                        (Object) Map.of(
                                                "author", displayName,
                                                "message", messageText,
                                                "time", fmt,
                                                "source", "YOUTUBE"));

                                // C. Chạy bộ quét lead để phát hiện khách hàng muốn mua hàng
                                processComment(displayName, messageText, session);
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("Failed to sync YouTube comments for video ID: " + youtubeVideoId, e);
        }
    }

    /**
     * Truy vấn thông tin của video stream trên YouTube để tìm khóa
     * 'activeLiveChatId' của phiên live.
     */
    private String fetchLiveChatId(String videoId) {
        try {
            String url = "https://www.googleapis.com/youtube/v3/videos?id=" + videoId
                    + "&part=liveStreamingDetails&key=" + youtubeApiKey;
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);
            if (response != null) {
                List<Map<String, Object>> items = (List<Map<String, Object>>) response.get("items");
                if (items != null && !items.isEmpty()) {
                    Map<String, Object> liveStreamingDetails = (Map<String, Object>) items.get(0)
                            .get("liveStreamingDetails");
                    if (liveStreamingDetails != null) {
                        return (String) liveStreamingDetails.get("activeLiveChatId");
                    }
                }
            }
        } catch (Exception e) {
            log.error("Failed to fetch liveChatId for YouTube video: " + videoId, e);
        }
        return null;
    }
}
