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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class LiveStreamService {

    private final LiveSessionRepository liveSessionRepository;
    private final LiveLeadRepository liveLeadRepository;
    private final LiveChatMessageRepository liveChatMessageRepository;
    private final ProductRepository productRepository;
    private final SimpMessagingTemplate messagingTemplate;

    @Value("${youtube.api.key:}")
    private String youtubeApiKey;

    private final RestTemplate restTemplate = new RestTemplate();
    private final Map<String, String> videoIdToChatIdCache = new HashMap<>();
    private final Map<String, String> videoIdToNextPageToken = new HashMap<>();

    // Patterns
    private static final Pattern PHONE_PATTERN = Pattern.compile("(03|05|07|08|09)\\d{8}");
    private static final Pattern PRODUCT_CODE_PATTERN = Pattern.compile("\\b(BON-\\d{3,5})\\b", Pattern.CASE_INSENSITIVE);

    public LiveSession startSession(String title, String streamUrl) {
        // End any active sessions first
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
        return liveSessionRepository.save(session);
    }

    public LiveSession endSession(Integer sessionId) {
        LiveSession session = liveSessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Session not found with ID: " + sessionId));
        session.setStatus("ENDED");
        session.setEndTime(LocalDateTime.now());
        LiveSession saved = liveSessionRepository.save(session);

        // Clean up chat messages for this session.
        // The raw chat is only needed while the session is ONGOING (for refresh-safe history).
        // Leads (phone numbers, product codes) are already persisted in live_lead table.
        liveChatMessageRepository.deleteByLiveSessionSessionId(sessionId);
        log.info("Cleaned up chat messages for ended session #{}", sessionId);

        return saved;
    }

    public Optional<LiveSession> getActiveSession() {
        return liveSessionRepository.findFirstByStatusOrderByStartTimeDesc("ONGOING");
    }

    public List<LiveSession> getAllSessions() {
        return liveSessionRepository.findAll();
    }

    public List<LiveLead> getLeadsBySession(Integer sessionId) {
        return liveLeadRepository.findByLiveSessionSessionIdOrderByCreatedAtDesc(sessionId);
    }

    public LiveLead updateLeadStatus(Integer leadId, String status, String notes) {
        LiveLead lead = liveLeadRepository.findById(leadId)
                .orElseThrow(() -> new IllegalArgumentException("Lead not found with ID: " + leadId));
        lead.setLeadStatus(status);
        lead.setNotes(notes);
        LiveLead saved = liveLeadRepository.save(lead);

        // Notify UI of status update
        messagingTemplate.convertAndSend("/topic/live-leads-update", saved);
        return saved;
    }

    /**
     * Parse comment and capture leads.
     */
    public Optional<LiveLead> processComment(String viewerName, String commentContent, LiveSession session) {
        Matcher phoneMatcher = PHONE_PATTERN.matcher(commentContent);
        Matcher productMatcher = PRODUCT_CODE_PATTERN.matcher(commentContent);

        String phoneNumber = null;
        if (phoneMatcher.find()) {
            phoneNumber = phoneMatcher.group();
        }

        String productCode = null;
        Product product = null;
        if (productMatcher.find()) {
            productCode = productMatcher.group().toUpperCase();
            Optional<Product> productOpt = productRepository.findByProductCode(productCode);
            if (productOpt.isPresent()) {
                product = productOpt.get();
            }
        }

        // We only save a lead if phone number is present or product code is matched
        if (phoneNumber == null && productCode == null) {
            return Optional.empty();
        }

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

        // Send real-time lead notification via WebSocket
        messagingTemplate.convertAndSend("/topic/live-leads/" + session.getSessionId(), saved);
        messagingTemplate.convertAndSend("/topic/live-leads", saved);

        return Optional.of(saved);
    }

    private String classifyIntent(String text) {
        String lower = text.toLowerCase();
        if (lower.contains("chốt") || lower.contains("chot") || lower.contains("mua") || lower.contains("lấy") 
                || lower.contains("lay") || lower.contains("order") || lower.contains("đặt") || lower.contains("dat")) {
            return "CHOT_DON";
        } else if (lower.contains("tư vấn") || lower.contains("tu van") || lower.contains("hỏi") || lower.contains("hoi") 
                || lower.contains("xin giá") || lower.contains("xin gia") || lower.contains("bao gia") || lower.contains("giá bao nhiêu")) {
            return "TU_VAN";
        } else {
            return "GOI_LAI";
        }
    }

    /**
     * Poll comments from YouTube Live and broadcast them to the live chat WebSocket topic.
     * Requires youtube.api.key to be set in application.properties.
     */
    public void syncYouTubeComments(String youtubeVideoId, LiveSession session) {
        if (youtubeApiKey == null || youtubeApiKey.isEmpty()) {
            log.warn("YouTube API Key is missing. Skipping YouTube comment sync.");
            return;
        }

        try {
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
                            Map<String, Object> textMessageDetails = (Map<String, Object>) snippet.get("textMessageDetails");
                            if (textMessageDetails != null) {
                                String messageText = (String) textMessageDetails.get("messageText");

                                // 1. Save message to DB
                                LiveChatMessage chatMsg = LiveChatMessage.builder()
                                        .liveSession(session)
                                        .author(displayName)
                                        .message(messageText)
                                        .source("YOUTUBE")
                                        .sentAt(LocalDateTime.now())
                                        .build();
                                liveChatMessageRepository.save(chatMsg);

                                // 2. Broadcast to all subscribers (moderator + viewer)
                                messagingTemplate.convertAndSend("/topic/live-chat/" + session.getSessionId(),
                                        (Object) Map.of(
                                                "author", displayName,
                                                "message", messageText,
                                                "time", fmt,
                                                "source", "YOUTUBE"
                                        ));

                                // 3. Lead detection
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

    private String fetchLiveChatId(String videoId) {
        try {
            String url = "https://www.googleapis.com/youtube/v3/videos?id=" + videoId 
                    + "&part=liveStreamingDetails&key=" + youtubeApiKey;
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);
            if (response != null) {
                List<Map<String, Object>> items = (List<Map<String, Object>>) response.get("items");
                if (items != null && !items.isEmpty()) {
                    Map<String, Object> liveStreamingDetails = (Map<String, Object>) items.get(0).get("liveStreamingDetails");
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
