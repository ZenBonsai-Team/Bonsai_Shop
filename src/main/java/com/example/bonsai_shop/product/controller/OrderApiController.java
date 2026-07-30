package com.example.bonsai_shop.product.controller;

import com.example.bonsai_shop.config.SecurityUtils;
import com.example.bonsai_shop.config.VNPayConfig;
import com.example.bonsai_shop.entity.Order;
import com.example.bonsai_shop.entity.OrderDetail;
import com.example.bonsai_shop.entity.Product;
import com.example.bonsai_shop.entity.User;
import com.example.bonsai_shop.product.dto.OrderResponseDTO;
import com.example.bonsai_shop.product.dto.PurchaseOrderRequestDTO;
import com.example.bonsai_shop.product.repository.ProductRepository;
import com.example.bonsai_shop.product.service.OrderService;
import com.example.bonsai_shop.product.repository.OrderRepository;
import com.example.bonsai_shop.customer.repository.UserRepository;
import com.example.bonsai_shop.product.service.CartService;
import com.example.bonsai_shop.entity.CartItem;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import com.example.bonsai_shop.customer.repository.RegisterOtpRepository;
import com.example.bonsai_shop.customer.service.EmailService;
import com.example.bonsai_shop.entity.PasswordResetOtp;

import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/orders")
@Slf4j
public class OrderApiController {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderService orderService;

    @Autowired
    private CartService cartService;

    @Autowired
    private RegisterOtpRepository registerOtpRepository;

    @Autowired
    private EmailService emailService;

    @PostMapping("/send-guest-otp")
    public ResponseEntity<Map<String, Object>> sendGuestOtp(@RequestBody Map<String, Object> payload) {
        Map<String, Object> response = new HashMap<>();

        // [1] Validate email format
        String email = payload.get("email") != null ? payload.get("email").toString() : null;
        if (email == null || !email.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) {
            response.put("success", false);
            response.put("message", "Äá»‹a chá»‰ Email khÃ´ng há»£p lá»‡!");
            return ResponseEntity.badRequest().body(response);
        }

        // [2] Parse productIds an toÃ n â€” khÃ´ng tin dá»¯ liá»‡u tá»« client
        List<Integer> productIds = new ArrayList<>();
        if (payload.containsKey("productIds") && payload.get("productIds") instanceof List<?>) {
            try {
                List<?> rawList = (List<?>) payload.get("productIds");
                productIds = rawList.stream()
                        .map(item -> Integer.valueOf(item.toString()))
                        .collect(Collectors.toList());
            } catch (NumberFormatException | ClassCastException e) {
                log.warn("[sendGuestOtp] productIds format khÃ´ng há»£p lá»‡ tá»« client: {}", e.getMessage());
                response.put("success", false);
                response.put("message", "Äá»‹nh dáº¡ng danh sÃ¡ch sáº£n pháº©m khÃ´ng há»£p lá»‡.");
                return ResponseEntity.badRequest().body(response);
            }
        }

        // [3] Pre-validate: load products tá»« DB + kiá»ƒm tra limit + kiá»ƒm tra availability
        // Má»¥c Ä‘Ã­ch: Fail Fast â€” khÃ´ng gá»­i OTP khi biáº¿t cháº¯c sáº£n pháº©m khÃ´ng cÃ²n kháº£ dá»¥ng
        if (!productIds.isEmpty()) {
            List<Product> products = orderService.getProductsByIds(productIds);

            try {
                orderService.validateOrderLimit(products);
            } catch (IllegalArgumentException e) {
                response.put("success", false);
                response.put("message", e.getMessage());
                return ResponseEntity.badRequest().body(response);
            }

            // UX Validation Layer â€” kiá»ƒm tra tráº¡ng thÃ¡i sáº£n pháº©m
            // LÆ¯U Ã: Ä‘Ã¢y KHÃ”NG pháº£i data guard. reserveIfAvailable() trong createOrder() má»›i lÃ  lá»›p báº£o vá»‡ cuá»‘i cÃ¹ng.
            List<Product> unavailableProducts = orderService.validateProductAvailability(products);
            if (!unavailableProducts.isEmpty()) {
                List<String> unavailableNames = unavailableProducts.stream()
                        .map(Product::getProductName)
                        .collect(Collectors.toList());
                List<Map<String, Object>> unavailableDetails = unavailableProducts.stream()
                        .map(p -> {
                            Map<String, Object> detail = new HashMap<>();
                            detail.put("productId", p.getProductId());
                            detail.put("productName", p.getProductName());
                            detail.put("status", p.getProductStatus());
                            return detail;
                        })
                        .collect(Collectors.toList());
                response.put("success", false);
                response.put("errorType", "PRODUCTS_UNAVAILABLE");
                response.put("message", "Má»™t sá»‘ tÃ¡c pháº©m khÃ´ng cÃ²n kháº£ dá»¥ng: " + String.join(", ", unavailableNames)
                        + ". Vui lÃ²ng lÃ m má»›i giá» hÃ ng vÃ  thá»­ láº¡i.");
                response.put("unavailableProducts", unavailableDetails);
                return ResponseEntity.badRequest().body(response);
            }
        }

        // [4] Rate limit â€” cooldown 60 giÃ¢y (khÃ´ng cáº§n Redis á»Ÿ quy mÃ´ hiá»‡n táº¡i)
        PasswordResetOtp latestOtp = registerOtpRepository
                .findTopByEmailOrderByCreatedAtDesc(email).orElse(null);
        if (latestOtp != null && latestOtp.getCreatedAt().isAfter(LocalDateTime.now().minusSeconds(60))) {
            long secondsElapsed = java.time.Duration
                    .between(latestOtp.getCreatedAt(), LocalDateTime.now()).getSeconds();
            long secondsRemaining = 60 - secondsElapsed;
            response.put("success", false);
            response.put("message", "Vui lÃ²ng Ä‘á»£i " + secondsRemaining + " giÃ¢y trÆ°á»›c khi gá»­i láº¡i mÃ£ OTP.");
            response.put("retryAfterSeconds", secondsRemaining);
            return ResponseEntity.status(429).body(response);
        }

        // [5] Generate OTP code
        String otpCode = String.format("%06d", new java.security.SecureRandom().nextInt(1000000));

        // [6] Gá»­i email TRÆ¯á»šC â€” náº¿u fail thÃ¬ khÃ´ng lÆ°u DB (trÃ¡nh OTP "ma")
        try {
            emailService.sendGuestOrderOtpOrThrow(email, otpCode);
        } catch (Exception e) {
            log.error("[sendGuestOtp] KhÃ´ng thá»ƒ gá»­i OTP Ä‘áº¿n {}: {}", email, e.getMessage());
            response.put("success", false);
            response.put("message", "KhÃ´ng thá»ƒ gá»­i mÃ£ xÃ¡c nháº­n. Vui lÃ²ng thá»­ láº¡i sau.");
            return ResponseEntity.internalServerError().body(response);
        }

        // [7] LÆ°u OTP vÃ o DB SAU KHI email Ä‘Ã£ gá»­i thÃ nh cÃ´ng
        try {
            registerOtpRepository.deleteByEmail(email);
        } catch (Exception e) {
            log.warn("[sendGuestOtp] KhÃ´ng thá»ƒ xÃ³a OTP cÅ© cho {}: {}", email, e.getMessage());
        }

        PasswordResetOtp otp = PasswordResetOtp.builder()
                .email(email)
                .otpCode(otpCode)
                .expiredAt(LocalDateTime.now().plusMinutes(5))
                .isUsed(false)
                .createdAt(LocalDateTime.now())
                .build();
        registerOtpRepository.save(otp);

        response.put("success", true);
        response.put("message", "MÃ£ OTP xÃ¡c nháº­n Ä‘Æ¡n hÃ ng Ä‘Ã£ Ä‘Æ°á»£c gá»­i tá»›i Email: " + email);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/pool")
    public ResponseEntity<Map<String, Object>> getPoolOrders(
            @RequestParam(defaultValue = "") String search,
            @RequestParam(defaultValue = "date_desc") String sort,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "8") int limit) {
        Page<Order> orderPage = orderService.getPoolOrders(search, sort, page, limit);
        List<OrderResponseDTO> dtoList = orderPage.getContent().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
        Map<String, Object> response = new HashMap<>();
        response.put("orders", dtoList);
        response.put("totalCount", orderPage.getTotalElements());
        response.put("pages", orderPage.getTotalPages());
        response.put("currentPage", page);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> getOrders(
            @RequestParam(defaultValue = "") String search,
            @RequestParam(defaultValue = "ALL") String status,
            @RequestParam(defaultValue = "date_desc") String sort,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "8") int limit) {

        Page<Order> orderPage = orderService.getFilteredOrders(search, status, sort, page, limit);

        List<OrderResponseDTO> dtoList = orderPage.getContent().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());

        Map<String, Object> response = new HashMap<>();
        response.put("orders", dtoList);
        response.put("totalCount", orderPage.getTotalElements());
        response.put("pages", orderPage.getTotalPages());
        response.put("currentPage", page);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/my")
    public ResponseEntity<Map<String, Object>> getMyOrders(
            @RequestParam(defaultValue = "") String search,
            @RequestParam(defaultValue = "ALL") String status,
            @RequestParam(defaultValue = "date_desc") String sort,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "8") int limit,
            @AuthenticationPrincipal Object principal) {

        User moderator = SecurityUtils.getCurrentUser(principal, userRepository);
        if (moderator == null) {
            return ResponseEntity.status(401).build();
        }

        Page<Order> orderPage = orderService.getMyOrders(moderator.getUserId(), search, status, sort, page, limit);
        List<OrderResponseDTO> dtoList = orderPage.getContent().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());

        Map<String, Object> response = new HashMap<>();
        response.put("orders", dtoList);
        response.put("totalCount", orderPage.getTotalElements());
        response.put("pages", orderPage.getTotalPages());
        response.put("currentPage", page);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/my-stats")
    public ResponseEntity<Map<String, Long>> getMyStats(
            @AuthenticationPrincipal Object principal) {
        User moderator = SecurityUtils.getCurrentUser(principal, userRepository);
        if (moderator == null) {
            return ResponseEntity.status(401).build();
        }
        return ResponseEntity.ok(orderService.getModeratorPersonalKPIs(moderator.getUserId()));
    }

    @PostMapping("/{orderCode}/claim")
    public ResponseEntity<Map<String, Object>> claimOrder(
            @PathVariable String orderCode,
            @AuthenticationPrincipal Object principal) {

        Map<String, Object> response = new HashMap<>();
        User moderator = SecurityUtils.getCurrentUser(principal, userRepository);
        if (moderator == null) {
            response.put("success", false);
            response.put("message", "ChÆ°a Ä‘Äƒng nháº­p.");
            return ResponseEntity.status(401).body(response);
        }

        try {
            boolean success = orderService.claimOrder(orderCode, moderator);
            response.put("success", success);
            response.put("message", "Nháº­n Ä‘Æ¡n hÃ ng thÃ nh cÃ´ng.");
            return ResponseEntity.ok(response);
        } catch (IllegalStateException e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.status(409).body(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Lá»—i mÃ¡y chá»§ khi xá»­ lÃ½: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    @PostMapping("/{orderCode}/unclaim")
    public ResponseEntity<Map<String, Object>> unclaimOrder(
            @PathVariable String orderCode,
            @AuthenticationPrincipal Object principal) {

        Map<String, Object> response = new HashMap<>();
        User moderator = SecurityUtils.getCurrentUser(principal, userRepository);
        if (moderator == null) {
            response.put("success", false);
            response.put("message", "ChÆ°a Ä‘Äƒng nháº­p.");
            return ResponseEntity.status(401).body(response);
        }

        try {
            boolean success = orderService.unclaimOrder(orderCode, moderator);
            response.put("success", success);
            response.put("message", "ÄÃ£ tráº£ Ä‘Æ¡n hÃ ng vá» Pool thÃ nh cÃ´ng.");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * API Láº¥y thá»‘ng kÃª sá»‘ lÆ°á»£ng Ä‘Æ¡n hÃ ng theo cÃ¡c tráº¡ng thÃ¡i (KPIs)
     */
    @GetMapping("/kpis")
    public ResponseEntity<Map<String, Long>> getKPIs() {
        return ResponseEntity.ok(orderService.getKPIs());
    }

    /**
     * API Láº¥y chi tiáº¿t má»™t Ä‘Æ¡n hÃ ng theo mÃ£ Ä‘Æ¡n
     */
    @GetMapping("/{orderCode}")
    public ResponseEntity<OrderResponseDTO> getOrderDetail(@PathVariable String orderCode) {
        Order order = orderService.getOrderByCode(orderCode);
        if (order == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(convertToDTO(order));
    }

    /**
     * API Duyá»‡t Ä‘Æ¡n hÃ ng (Cáº­p nháº­t phÃ­ cáº©u, phÃ­ ship, sá»‘ tiá»n Ä‘áº·t cá»c náº¿u cÃ³)
     */
    @PostMapping("/{orderCode}/verify")
    public ResponseEntity<Map<String, Object>> verifyOrder(
            @PathVariable String orderCode,
            @RequestBody Map<String, Object> payload,
            @AuthenticationPrincipal Object principal) {

        Map<String, Object> response = new HashMap<>();
        User moderator = SecurityUtils.getCurrentUser(principal, userRepository);
        if (moderator == null) {
            response.put("success", false);
            response.put("message", "ChÆ°a Ä‘Äƒng nháº­p há»‡ thá»‘ng.");
            return ResponseEntity.status(401).body(response);
        }

        BigDecimal craneFee = new BigDecimal(payload.getOrDefault("craneFee", 0).toString());
        BigDecimal shippingFee = new BigDecimal(payload.getOrDefault("shippingFee", 0).toString());
        BigDecimal depositAmount = null;
        if (payload.containsKey("depositAmount") && payload.get("depositAmount") != null) {
            depositAmount = new BigDecimal(payload.get("depositAmount").toString());
        }

        try {
            boolean success = orderService.verifyOrder(orderCode, craneFee, shippingFee, depositAmount, moderator);
            response.put("success", success);
            response.put("message", success ? "Duyệt đơn hàng thành công." : "Duyệt đơn hàng thất bại.");
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        } catch (SecurityException e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.status(403).body(response);
        } catch (Exception e) {
            log.error("Lỗi khi duyệt đơn hàng {}", orderCode, e);
            response.put("success", false);
            response.put("message", "Lỗi hệ thống: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * API Moderator xÃ¡c nháº­n Ä‘Ã£ thu Ä‘á»§ tiá»n pháº§n cÃ²n láº¡i (Chuyá»ƒn Order tá»« DEPOSITED -> PAID, Product -> SOLD)
     */
    @PostMapping("/{orderCode}/confirm-remaining-payment")
    public ResponseEntity<Map<String, Object>> confirmRemainingPayment(
            @PathVariable String orderCode,
            @RequestBody(required = false) Map<String, String> payload,
            @AuthenticationPrincipal Object principal) {

        Map<String, Object> response = new HashMap<>();
        User moderator = SecurityUtils.getCurrentUser(principal, userRepository);
        if (moderator == null) {
            response.put("success", false);
            response.put("message", "ChÆ°a Ä‘Äƒng nháº­p há»‡ thá»‘ng.");
            return ResponseEntity.status(401).body(response);
        }

        String notes = (payload != null) ? payload.getOrDefault("notes", "") : "";
        try {
            boolean success = orderService.confirmRemainingPayment(orderCode, notes, moderator);
            response.put("success", success);
            response.put("message", success ? "XÃ¡c nháº­n Ä‘Ã£ thanh toÃ¡n Ä‘áº§y Ä‘á»§ thÃ nh cÃ´ng!" : "XÃ¡c nháº­n tháº¥t báº¡i.");
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        } catch (IllegalStateException e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.status(409).body(response);
        } catch (Exception e) {
            log.error("Lá»—i khi xÃ¡c nháº­n thanh toÃ¡n Ä‘á»§ Ä‘Æ¡n {}", orderCode, e);
            response.put("success", false);
            response.put("message", "Lá»—i há»‡ thá»‘ng: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * API Tá»« chá»‘i duyá»‡t Ä‘Æ¡n hÃ ng (CÃ³ lÃ½ do)
     */
    @PostMapping("/{orderCode}/complete")
    public ResponseEntity<Map<String, Object>> completePaidOrder(
            @PathVariable String orderCode,
            @AuthenticationPrincipal Object principal) {

        Map<String, Object> response = new HashMap<>();
        User moderator = SecurityUtils.getCurrentUser(principal, userRepository);
        if (moderator == null) {
            response.put("success", false);
            response.put("message", "Chưa đăng nhập hệ thống.");
            return ResponseEntity.status(401).body(response);
        }

        try {
            boolean success = orderService.completePaidOrder(orderCode, moderator);
            response.put("success", success);
            response.put("message", success ? "Đơn hàng đã hoàn thành." : "Thao tác thất bại.");
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        } catch (IllegalStateException e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.status(409).body(response);
        } catch (Exception e) {
            log.error("Lỗi khi hoàn thành đơn {}", orderCode, e);
            response.put("success", false);
            response.put("message", "Lỗi hệ thống: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    @PostMapping("/{orderCode}/customer-no-show")
    public ResponseEntity<Map<String, Object>> markCustomerNoShow(
            @PathVariable String orderCode,
            @RequestBody(required = false) Map<String, String> payload,
            @AuthenticationPrincipal Object principal) {

        Map<String, Object> response = new HashMap<>();
        User moderator = SecurityUtils.getCurrentUser(principal, userRepository);
        if (moderator == null) {
            response.put("success", false);
            response.put("message", "Chưa đăng nhập hệ thống.");
            return ResponseEntity.status(401).body(response);
        }

        String notes = payload != null ? payload.getOrDefault("notes", "") : "";
        try {
            boolean success = orderService.markDepositedOrderCustomerNoShow(orderCode, notes, moderator);
            response.put("success", success);
            response.put("message", success ? "Đã hủy đơn vì khách không nhận. Tiền cọc không hoàn." : "Thao tác thất bại.");
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        } catch (IllegalStateException e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.status(409).body(response);
        } catch (Exception e) {
            log.error("Lỗi khi hủy đơn vì khách không nhận {}", orderCode, e);
            response.put("success", false);
            response.put("message", "Lỗi hệ thống: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    @PostMapping("/{orderCode}/reject")
    public ResponseEntity<Map<String, Object>> rejectOrder(
            @PathVariable String orderCode,
            @RequestBody Map<String, String> payload,
            @AuthenticationPrincipal Object principal) {

        Map<String, Object> response = new HashMap<>();
        User moderator = SecurityUtils.getCurrentUser(principal, userRepository);
        if (moderator == null) {
            response.put("success", false);
            response.put("message", "ChÆ°a Ä‘Äƒng nháº­p há»‡ thá»‘ng.");
            return ResponseEntity.status(401).body(response);
        }

        String reason = payload.getOrDefault("reason", "");
        try {
            boolean success = orderService.rejectOrder(orderCode, reason, moderator);
            response.put("success", success);
            response.put("message", success ? "Tá»« chá»‘i duyá»‡t Ä‘Æ¡n hÃ ng thÃ nh cÃ´ng." : "Thao tÃ¡c tháº¥t báº¡i.");
            return ResponseEntity.ok(response);
        } catch (SecurityException e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.status(403).body(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Lá»—i xá»­ lÃ½ tá»« chá»‘i: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PostMapping("/checkout")
    public ResponseEntity<Map<String, Object>> checkout(
            @Valid @RequestBody PurchaseOrderRequestDTO dto,
            @AuthenticationPrincipal Object principal,
            HttpServletRequest request) throws UnsupportedEncodingException {

        Map<String, Object> response = new HashMap<>();

        User customer = SecurityUtils.getCurrentUser(principal, userRepository);
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isStaffOrAdmin = auth != null && auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_OWNER") || a.getAuthority().equals("ROLE_ARTISAN")
                        || a.getAuthority().equals("ROLE_MODERATOR")
                        || a.getAuthority().equals("ROLE_CONTENT_MODERATOR")
                        || a.getAuthority().equals("ROLE_ADMIN") || a.getAuthority().equals("ROLE_SELLER"));
        if (isStaffOrAdmin) {
            response.put("success", false);
            response.put("message",
                    "TÃ i khoáº£n quáº£n trá»‹, nhÃ  vÆ°á»n hoáº·c kiá»ƒm duyá»‡t viÃªn khÃ´ng Ä‘Æ°á»£c phÃ©p thá»±c hiá»‡n Ä‘áº·t hÃ ng!");
            return ResponseEntity.status(403).body(response);
        }

        // Kiá»ƒm tra giá»›i háº¡n Ä‘Æ¡n hÃ ng (â‰¤ 200 triá»‡u VNÄ) sá»›m trÆ°á»›c khi yÃªu cáº§u/xÃ¡c thá»±c OTP
        try {
            orderService.validateOrderLimit(dto, customer);
        } catch (IllegalArgumentException e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }

        // [Má»šI] Pre-validate tráº¡ng thÃ¡i sáº£n pháº©m cho Logged-in User
        // Guest: Ä‘Ã£ Ä‘Æ°á»£c validate trong /send-guest-otp trÆ°á»›c khi gá»­i OTP
        // LÆ¯U Ã: Ä‘Ã¢y lÃ  UX layer â€” khÃ´ng thay tháº¿ Ä‘Æ°á»£c reserveIfAvailable() trong createOrder()
        if (customer != null) {
            List<Product> productsToCheck = orderService.loadProductsForOrder(dto, customer);
            List<Product> unavailableProducts = orderService.validateProductAvailability(productsToCheck);
            if (!unavailableProducts.isEmpty()) {
                List<String> unavailableNames = unavailableProducts.stream()
                        .map(Product::getProductName)
                        .collect(Collectors.toList());
                List<Map<String, Object>> unavailableDetails = unavailableProducts.stream()
                        .map(p -> {
                            Map<String, Object> detail = new HashMap<>();
                            detail.put("productId", p.getProductId());
                            detail.put("productName", p.getProductName());
                            detail.put("status", p.getProductStatus());
                            return detail;
                        })
                        .collect(Collectors.toList());
                response.put("success", false);
                response.put("errorType", "PRODUCTS_UNAVAILABLE");
                response.put("message", "Má»™t sá»‘ tÃ¡c pháº©m khÃ´ng cÃ²n kháº£ dá»¥ng: "
                        + String.join(", ", unavailableNames)
                        + ". Vui lÃ²ng xÃ³a khá»i giá» hÃ ng vÃ  chá»n sáº£n pháº©m khÃ¡c.");
                response.put("unavailableProducts", unavailableDetails);
                return ResponseEntity.badRequest().body(response);
            }
        }

        // XÃ¡c thá»±c mÃ£ OTP náº¿u lÃ  KhÃ¡ch vÃ£ng lai (Guest Checkout)
        if (customer == null) {
            String otpCode = dto.getOtpCode();
            if (otpCode == null || otpCode.trim().isEmpty()) {
                response.put("success", false);
                response.put("requireOtp", true);
                response.put("message", "Vui lÃ²ng nháº­p mÃ£ OTP xÃ¡c nháº­n Ä‘Æ°á»£c gá»­i vá» Email.");
                return ResponseEntity.badRequest().body(response);
            }

            PasswordResetOtp otp = registerOtpRepository.findTopByEmailOrderByCreatedAtDesc(dto.getCustomerEmail())
                    .orElse(null);
            if (otp == null || Boolean.TRUE.equals(otp.getIsUsed()) || otp.getExpiredAt().isBefore(LocalDateTime.now())
                    || !otp.getOtpCode().equals(otpCode.trim())) {
                response.put("success", false);
                response.put("message", "MÃ£ OTP khÃ´ng há»£p lá»‡ hoáº·c Ä‘Ã£ háº¿t háº¡n. Vui lÃ²ng láº¥y mÃ£ má»›i vÃ  thá»­ láº¡i!");
                return ResponseEntity.badRequest().body(response);
            }

            otp.setIsUsed(true);
            registerOtpRepository.save(otp);
        }

        try {
            Order createdOrder = orderService.createOrder(dto, customer);
            response.put("success", true);
            response.put("paymentMethod", dto.getPaymentMethod() != null ? dto.getPaymentMethod() : "COD");
            response.put("orderCode", createdOrder.getOrderCode());
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException | IllegalStateException e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Lá»—i táº¡o Ä‘Æ¡n hÃ ng: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    private String buildVNPayUrl(HttpServletRequest req, String orderCode, BigDecimal amount)
            throws UnsupportedEncodingException {
        long amountLong = amount.longValue() * 100;
        String vnp_Version = "2.1.0";
        String vnp_Command = "pay";
        String vnp_IpAddr = VNPayConfig.getIpAddress(req);

        Map<String, String> vnp_Params = new HashMap<>();
        vnp_Params.put("vnp_Version", vnp_Version);
        vnp_Params.put("vnp_Command", vnp_Command);
        vnp_Params.put("vnp_TmnCode", VNPayConfig.vnp_TmnCode);
        vnp_Params.put("vnp_Amount", String.valueOf(amountLong));
        vnp_Params.put("vnp_CurrCode", "VND");
        vnp_Params.put("vnp_TxnRef", orderCode);
        vnp_Params.put("vnp_OrderInfo", "Thanh toan don hang BSMS:" + orderCode);
        vnp_Params.put("vnp_OrderType", "other");
        vnp_Params.put("vnp_Locale", "vn");
        vnp_Params.put("vnp_ReturnUrl", VNPayConfig.vnp_ReturnUrl);
        vnp_Params.put("vnp_IpAddr", vnp_IpAddr);

        Calendar cld = Calendar.getInstance(TimeZone.getTimeZone("Etc/GMT+7"));
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
        vnp_Params.put("vnp_CreateDate", formatter.format(cld.getTime()));

        cld.add(Calendar.MINUTE, 15);
        vnp_Params.put("vnp_ExpireDate", formatter.format(cld.getTime()));

        List<String> fieldNames = new ArrayList<>(vnp_Params.keySet());
        Collections.sort(fieldNames);
        StringBuilder hashData = new StringBuilder();
        StringBuilder query = new StringBuilder();

        for (int i = 0; i < fieldNames.size(); i++) {
            String fieldName = fieldNames.get(i);
            String fieldValue = vnp_Params.get(fieldName);
            if ((fieldValue != null) && (fieldValue.length() > 0)) {
                hashData.append(fieldName).append('=').append(
                        URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII.toString()));
                query.append(URLEncoder.encode(fieldName, StandardCharsets.US_ASCII.toString())).append('=')
                        .append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII.toString()));
                if (i < fieldNames.size() - 1) {
                    query.append('&');
                    hashData.append('&');
                }
            }
        }

        String queryUrl = query.toString();
        String vnp_SecureHash = VNPayConfig.hmacSHA512(VNPayConfig.vnp_HashSecret, hashData.toString());
        queryUrl += "&vnp_SecureHash=" + vnp_SecureHash;

        return VNPayConfig.vnp_PayUrl + "?" + queryUrl;
    }

    private OrderResponseDTO convertToDTO(Order order) {
        OrderResponseDTO.ProductDTO productDTO = null;
        List<OrderResponseDTO.OrderItemDTO> itemsDTO = new ArrayList<>();

        if (order.getOrderDetails() != null && !order.getOrderDetails().isEmpty()) {
            // First item (for backwards compatibility if any client code still uses
            // .product)
            OrderDetail detail0 = order.getOrderDetails().get(0);
            Product prod0 = detail0.getProduct();
            if (prod0 != null) {
                productDTO = OrderResponseDTO.ProductDTO.builder()
                        .id(prod0.getProductId())
                        .name(prod0.getProductName())
                        .image(prod0.getFirstImageUrl())
                        .price(detail0.getPriceAtPurchase())
                        .build();
            }

            // Fill all items list
            for (OrderDetail detail : order.getOrderDetails()) {
                Product prod = detail.getProduct();
                if (prod != null) {
                    itemsDTO.add(OrderResponseDTO.OrderItemDTO.builder()
                            .id(prod.getProductId())
                            .name(prod.getProductName())
                            .image(prod.getFirstImageUrl())
                            .price(detail.getPriceAtPurchase())
                            .quantity(detail.getQuantity())
                            .build());
                }
            }
        }

        List<OrderResponseDTO.OrderHandlingDTO> handlingHistory = null;
        if (order.getOrderId() != null) {
            handlingHistory = orderService.getOrderHandlingHistory(order.getOrderId()).stream()
                    .map(h -> OrderResponseDTO.OrderHandlingDTO.builder()
                            .handlingId(h.getOrderHandlingId())
                            .moderatorUsername(h.getModerator() != null ? h.getModerator().getUsername() : "N/A")
                            .moderatorFullName(h.getModerator() != null ? h.getModerator().getFullName() : "N/A")
                            .handledAt(h.getHandledAt())
                            .releasedAt(h.getReleasedAt())
                            .isActive(h.getIsActive())
                            .build())
                    .collect(Collectors.toList());
        }

        List<com.example.bonsai_shop.product.dto.PaymentDTO> paymentDTOs = null;
        if (order.getPayments() != null && !order.getPayments().isEmpty()) {
            paymentDTOs = order.getPayments().stream()
                    .map(p -> com.example.bonsai_shop.product.dto.PaymentDTO.builder()
                            .paymentId(p.getPaymentId())
                            .paymentType(p.getPaymentType())
                            .paymentMethod(p.getPaymentMethod())
                            .paymentStatus(p.getPaymentStatus())
                            .amount(p.getAmount())
                            .paymentDate(p.getPaymentDate())
                            .notes(p.getNotes())
                            .build())
                    .collect(Collectors.toList());
        }

        BigDecimal craneFee = order.getCraneFee() != null ? order.getCraneFee() : BigDecimal.ZERO;
        BigDecimal shippingFee = order.getShippingFee() != null ? order.getShippingFee() : BigDecimal.ZERO;
        BigDecimal totalAmount = order.getTotalAmount() != null ? order.getTotalAmount() : BigDecimal.ZERO;
        BigDecimal depositAmount = order.getDepositAmount() != null ? order.getDepositAmount() : BigDecimal.ZERO;

        BigDecimal treePrice = BigDecimal.ZERO;
        if (order.getOrderDetails() != null && !order.getOrderDetails().isEmpty()) {
            treePrice = order.getOrderDetails().stream()
                    .map(d -> (d.getPriceAtPurchase() != null ? d.getPriceAtPurchase() : BigDecimal.ZERO)
                            .multiply(BigDecimal.valueOf(d.getQuantity() != null ? d.getQuantity() : 1)))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        } else {
            treePrice = totalAmount.subtract(craneFee).subtract(shippingFee);
            if (treePrice.compareTo(BigDecimal.ZERO) < 0) treePrice = BigDecimal.ZERO;
        }

        String resolvedPaymentMethod = null;
        boolean isDepositFlow = false;

        if (order.getPayments() != null && !order.getPayments().isEmpty()) {
            resolvedPaymentMethod = order.getPayments().get(0).getPaymentMethod();
            isDepositFlow = order.getPayments().stream().anyMatch(p -> 
                "DEPOSIT".equalsIgnoreCase(p.getPaymentType()) ||
                "DEPOSIT".equalsIgnoreCase(p.getPaymentMethod()) ||
                "COD".equalsIgnoreCase(p.getPaymentMethod())
            );
        } else {
            isDepositFlow = depositAmount.compareTo(BigDecimal.ZERO) > 0;
        }

        if (resolvedPaymentMethod == null) {
            resolvedPaymentMethod = isDepositFlow ? "COD" : "VNPAY";
        }

        BigDecimal immediatePaymentAmount;
        BigDecimal remainingPaymentAmount;

        if (isDepositFlow) {
            // Thanh toán ngay Nấc 1 = Deposit + Shipping + Crane
            immediatePaymentAmount = depositAmount.add(craneFee).add(shippingFee);
            // Thanh toán khi nhận cây Nấc 2 = Tree Price - Deposit
            remainingPaymentAmount = treePrice.subtract(depositAmount);
            if (remainingPaymentAmount.compareTo(BigDecimal.ZERO) < 0) {
                remainingPaymentAmount = BigDecimal.ZERO;
            }
        } else {
            // Thanh toán toàn bộ 1 lần = Total Amount (Tree Price + Shipping + Crane)
            immediatePaymentAmount = totalAmount;
            remainingPaymentAmount = BigDecimal.ZERO;
        }

        return OrderResponseDTO.builder()
                .orderId(order.getOrderId())
                .orderCode(order.getOrderCode())
                .customer(OrderResponseDTO.CustomerDTO.builder()
                        .name(order.getCustomerName())
                        .email(order.getCustomerEmail())
                        .phone(order.getCustomerPhone())
                        .address(order.getShippingAddress())
                        .build())
                .product(productDTO)
                .items(itemsDTO)
                .quantity(itemsDTO.stream().mapToInt(OrderResponseDTO.OrderItemDTO::getQuantity).sum())
                .treePrice(treePrice)
                .totalAmount(totalAmount)
                .depositAmount(depositAmount)
                .immediatePaymentAmount(immediatePaymentAmount)
                .remainingPaymentAmount(remainingPaymentAmount)
                .orderDate(order.getOrderDate())
                .orderStatus(order.getOrderStatus())
                .orderType(order.getOrderType())
                .paymentMethod(resolvedPaymentMethod)
                .craneFee(craneFee)
                .shippingFee(shippingFee)
                .notes(order.getNotes())
                .payments(paymentDTOs)
                .assignedToUsername(order.getAssignedTo() != null ? order.getAssignedTo().getUsername() : null)
                .assignedToFullName(order.getAssignedTo() != null ? order.getAssignedTo().getFullName() : null)
                .assignedAt(order.getAssignedAt())
                .handlingHistory(handlingHistory)
                .build();
    }
}
