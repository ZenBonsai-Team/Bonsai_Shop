package com.example.bonsai_shop.moderator.controller;

import com.example.bonsai_shop.entity.LiveSession;
import com.example.bonsai_shop.entity.Review;
import com.example.bonsai_shop.livestream.repository.LiveSessionRepository;
import com.example.bonsai_shop.product.service.ReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@Controller
@RequestMapping("/moderator")
@RequiredArgsConstructor
public class ModeratorViewController {

    private final LiveSessionRepository liveSessionRepository;
    private final ReviewService reviewService;

    /**
     * GET /moderator/live-session - Live Stream Control Panel
     */
    @GetMapping("/live-session")
    public String liveSessionPage(Model model) {
        // Get current active session if any
        liveSessionRepository.findFirstByStatusOrderByStartTimeDesc("ONGOING")
                .ifPresent(session -> model.addAttribute("activeSession", session));

        // Recent sessions list
        List<LiveSession> recentSessions = liveSessionRepository.findAllByOrderByStartTimeDesc();
        model.addAttribute("recentSessions", recentSessions);
        model.addAttribute("activePage", "live-session");
        model.addAttribute("activePageLabel", "Live Stream Control Panel");
        return "moderator/live-session";
    }

    /**
     * GET /moderator/reviews - Reviews Management Page
     */
    @GetMapping("/reviews")
    public String reviewsPage(Model model) {
        List<Review> pendingReviews = reviewService.getReviewsByStatus("PENDING");
        List<Review> approvedReviews = reviewService.getReviewsByStatus("APPROVED");
        List<Review> rejectedReviews = reviewService.getReviewsByStatus("REJECTED");

        model.addAttribute("pendingReviews", pendingReviews);
        model.addAttribute("approvedReviews", approvedReviews);
        model.addAttribute("rejectedReviews", rejectedReviews);
        model.addAttribute("activePage", "reviews");
        model.addAttribute("activePageLabel", "Quản lý Đánh giá");
        return "moderator/reviews";
    }
}
