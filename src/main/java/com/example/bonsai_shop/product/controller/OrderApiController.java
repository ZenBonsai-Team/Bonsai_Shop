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
import org.springframework.beans.factory.annotation.Value;
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

/**
 * [VAI TRÒ TRONG LUỒNG ĐẶT ĐƠN]
 *
 * Chịu trách nhiệm:
 * - Tiếp nhận và điều phối các yêu cầu REST API liên quan đến đơn hàng (Order).
 * - Cung cấp API gửi mã OTP cho khách vãng lai (Guest Checkout).
 * - Cung cấp API Checkout (đặt hàng) cho cả khách đăng nhập và khách vãng lai.
 * - Cung cấp các API quản lý và xử lý đơn hàng cho Order Moderator:
 *   + Xem danh sách đơn trong kho chung (Orders Pool) và đơn cá nhân (My Orders).
 *   + Tiếp nhận (Claim) / Trả lại kho (Unclaim) đơn hàng.
 *   + Duyệt đơn (Verify/Approve) với phí cẩu, phí vận chuyển và số tiền đặt cọc.
 *   + Xác nhận thanh toán phần còn lại (Confirm remaining payment), hoàn tất đơn (Complete), hủy do khách không nhận (Customer No-Show), từ chối đơn (Reject).
 *
 * Các thao tác trên web đi qua class này:
 * - [Khách vãng lai gửi OTP] → POST /api/orders/send-guest-otp → sendGuestOtp()
 * - [Khách bấm Đặt hàng / Checkout] → POST /api/orders/checkout → checkout()
 * - [Moderator xem Orders Pool] → GET /api/orders/pool → getPoolOrders()
 * - [Moderator xem tất cả đơn] → GET /api/orders → getOrders()
 * - [Moderator xem đơn của tôi] → GET /api/orders/my → getMyOrders()
 * - [Moderator xem thống kê cá nhân] → GET /api/orders/my-stats → getMyStats()
 * - [Moderator xem KPI tổng quan] → GET /api/orders/kpis → getKPIs()
 * - [Moderator xem chi tiết đơn] → GET /api/orders/{orderCode} → getOrderDetail()
 * - [Moderator nhận đơn] → POST /api/orders/{orderCode}/claim → claimOrder()
 * - [Moderator trả đơn về Pool] → POST /api/orders/{orderCode}/unclaim → unclaimOrder()
 * - [Moderator duyệt đơn & áp phí] → POST /api/orders/{orderCode}/verify → verifyOrder()
 * - [Moderator xác nhận thu đủ tiền đợt 2] → POST /api/orders/{orderCode}/confirm-remaining-payment → confirmRemainingPayment()
 * - [Moderator hoàn thành đơn đã thanh toán 100%] → POST /api/orders/{orderCode}/complete → completePaidOrder()
 * - [Moderator hủy đơn vì khách không nhận] → POST /api/orders/{orderCode}/customer-no-show → markCustomerNoShow()
 * - [Moderator từ chối đơn] → POST /api/orders/{orderCode}/reject → rejectOrder()
 *
 * Các thành phần phối hợp chính:
 * - OrderService, CartService, EmailService
 * - OrderRepository, ProductRepository, UserRepository, RegisterOtpRepository
 * - PurchaseOrderRequestDTO, OrderResponseDTO, PasswordResetOtp, Order, Product, Payment
 */
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

    @Value("${order.expiration.online-minutes:15}")
    private int onlineExpirationMinutes;

    /**
     * [GỬI MÃ OTP XÁC THỰC CHO KHÁCH VÃNG LAI (GUEST CHECKOUT)]
     *
     * Khi nào được gọi trên web:
     * - Người dùng thực hiện: Nhập Email tại bước thanh toán (Checkout) khi chưa đăng nhập tài khoản và bấm "Gửi mã xác nhận" / "Lấy mã OTP".
     * - Màn hình/chức năng: Màn hình Giỏ hàng / Checkout cho Guest Customer.
     *
     * API:
     * - HTTP: POST
     * - URL: /api/orders/send-guest-otp
     * - Người gọi: Guest Customer (Chưa đăng nhập)
     *
     * Dữ liệu nhận vào:
     * - Request body: Map<String, Object> payload
     * - Các field quan trọng:
     *   + email (String): Email của khách nhận mã OTP.
     *   + productIds (List<Integer>): Danh sách ID cây khách đang chọn mua để kiểm tra trước tính khả dụng.
     *
     * Điều phối xử lý:
     * 1. Controller kiểm tra định dạng email và parse danh sách productIds.
     * 2. Nếu có productIds, gọi OrderService.getProductsByIds() và OrderService.validateProductAvailability() để kiểm tra nhanh trạng thái (Fail-fast nếu cây không AVAILABLE).
     * 3. Kiểm tra rate-limit chống spam qua RegisterOtpRepository.findTopByEmailOrderByCreatedAtDesc() (cooldown 60 giây).
     * 4. Sinh mã OTP 6 chữ số ngẫu nhiên.
     * 5. Gửi email chứa OTP trước qua EmailService.sendGuestOrderOtpOrThrow().
     * 6. Sau khi gửi email thành công, xóa OTP cũ qua RegisterOtpRepository.deleteByEmail() và lưu bản ghi PasswordResetOtp mới với thời hạn 5 phút.
     *
     * Dữ liệu trả về:
     * - HTTP status: 200 OK (Thành công), 400 Bad Request (Email sai/Cây không còn), 429 Too Many Requests (Spam trong 60s), 500 Internal Server Error (Lỗi gửi mail).
     * - Response body: Map<String, Object> ("success", "message", "retryAfterSeconds", "unavailableProducts").
     * - Ý nghĩa dữ liệu frontend nhận được: Thông báo để hiển thị đếm ngược cooldown hoặc báo khách nhập mã OTP từ hộp thư.
     *
     * Tác động dữ liệu:
     * - Bảng/Entity bị đọc: PRODUCT (kiểm tra status), PASSWORD_RESET_OTP (kiểm tra rate limit).
     * - Bảng/Entity bị ghi/cập nhật: PASSWORD_RESET_OTP (xóa cũ, lưu OTP mới hết hạn sau 5 phút).
     * - Thay đổi trạng thái: Không đổi trạng thái Order/Product tại bước này.
     */
    @PostMapping("/send-guest-otp")
    public ResponseEntity<Map<String, Object>> sendGuestOtp(@RequestBody Map<String, Object> payload) {
        Map<String, Object> response = new HashMap<>();

        // [1] Validate email format
        String email = payload.get("email") != null ? payload.get("email").toString() : null;
        if (email == null || !email.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) {
            response.put("success", false);
            response.put("message", "Địa chỉ Email không hợp lệ!");
            return ResponseEntity.badRequest().body(response);
        }

        // [2] Parse productIds an toàn — không tin dữ liệu từ client
        List<Integer> productIds = new ArrayList<>();
        if (payload.containsKey("productIds") && payload.get("productIds") instanceof List<?>) {
            try {
                List<?> rawList = (List<?>) payload.get("productIds");
                productIds = rawList.stream()
                        .map(item -> Integer.valueOf(item.toString()))
                        .collect(Collectors.toList());
            } catch (NumberFormatException | ClassCastException e) {
                log.warn("[sendGuestOtp] productIds format không hợp lệ từ client: {}", e.getMessage());
                response.put("success", false);
                response.put("message", "Định dạng danh sách sản phẩm không hợp lệ.");
                return ResponseEntity.badRequest().body(response);
            }
        }

        // [3] Pre-validate: load products từ DB + kiểm tra limit + kiểm tra availability
        // Mục đích: Fail Fast — không gửi OTP khi biết chắc sản phẩm không còn khả dụng
        if (!productIds.isEmpty()) {
            List<Product> products = orderService.getProductsByIds(productIds);

            // UX Validation Layer — kiểm tra trạng thái sản phẩm
            // LƯU Ý: đây KHÔNG phải data guard. reserveIfAvailable() trong createOrder() mới là lớp bảo vệ cuối cùng.
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
                response.put("message", "Một số tác phẩm không còn khả dụng: " + String.join(", ", unavailableNames)
                        + ". Vui lòng làm mới giỏ hàng và thử lại.");
                response.put("unavailableProducts", unavailableDetails);
                return ResponseEntity.badRequest().body(response);
            }
        }

        // [4] Rate limit — cooldown 60 giây (không cần Redis ở quy mô hiện tại)
        PasswordResetOtp latestOtp = registerOtpRepository
                .findTopByEmailOrderByCreatedAtDesc(email).orElse(null);
        if (latestOtp != null && latestOtp.getCreatedAt().isAfter(LocalDateTime.now().minusSeconds(60))) {
            long secondsElapsed = java.time.Duration
                    .between(latestOtp.getCreatedAt(), LocalDateTime.now()).getSeconds();
            long secondsRemaining = 60 - secondsElapsed;
            response.put("success", false);
            response.put("message", "Vui lòng đợi " + secondsRemaining + " giây trước khi gửi lại mã OTP.");
            response.put("retryAfterSeconds", secondsRemaining);
            return ResponseEntity.status(429).body(response);
        }

        // [5] Generate OTP code
        String otpCode = String.format("%06d", new java.security.SecureRandom().nextInt(1000000));

        // [6] Gửi email TRƯỚC — nếu fail thì không lưu DB (tránh OTP "ma")
        try {
            emailService.sendGuestOrderOtpOrThrow(email, otpCode);
        } catch (Exception e) {
            log.error("[sendGuestOtp] Không thể gửi OTP đến {}: {}", email, e.getMessage());
            response.put("success", false);
            response.put("message", "Không thể gửi mã xác nhận. Vui lòng thử lại sau.");
            return ResponseEntity.internalServerError().body(response);
        }

        // [7] Lưu OTP vào DB SAU KHI email đã gửi thành công
        try {
            registerOtpRepository.deleteByEmail(email);
        } catch (Exception e) {
            log.warn("[sendGuestOtp] Không thể xóa OTP cũ cho {}: {}", email, e.getMessage());
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
        response.put("message", "Mã OTP xác nhận đơn hàng đã được gửi tới Email: " + email);
        return ResponseEntity.ok(response);
    }

    /**
     * [LẤY DANH SÁCH ĐƠN HÀNG TRONG KHO CHUNG (ORDERS POOL)]
     *
     * Khi nào được gọi trên web:
     * - Người dùng thực hiện: Truy cập trang Kho đơn hàng chung (Orders Pool) để tìm các đơn PENDING chưa có ai nhận để claim.
     * - Màn hình/chức năng: Moderator - Orders Pool (/moderator/orders/pool).
     *
     * API:
     * - HTTP: GET
     * - URL: /api/orders/pool
     * - Người gọi: Order Moderator
     *
     * Dữ liệu nhận vào:
     * - Request params:
     *   + search (String): Từ khóa tìm kiếm (Mã đơn, tên khách, tên cây).
     *   + sort (String): Kiểu sắp xếp (date_desc, date_asc, price_desc, price_asc).
     *   + page (int): Số trang (mặc định 1).
     *   + limit (int): Số lượng bản ghi trên 1 trang (mặc định 8).
     *
     * Điều phối xử lý:
     * 1. Controller gọi OrderService.getPoolOrders(search, sort, page, limit)
     * 2. Service gọi OrderRepository.searchOrdersPool(search, pageable) để lấy các đơn có assignedTo IS NULL và orderStatus = 'PENDING'.
     * 3. Controller map danh sách Order entity sang OrderResponseDTO qua convertToDTO().
     *
     * Dữ liệu trả về:
     * - HTTP status: 200 OK
     * - Response body: Map chứa "orders" (List<OrderResponseDTO>), "totalCount", "pages", "currentPage".
     *
     * Tác động dữ liệu:
     * - Bảng/Entity bị đọc: ORDER, ORDER_DETAIL, PRODUCT
     * - Bảng/Entity bị ghi/cập nhật: Không có (Chỉ đọc).
     */
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

    /**
     * [LẤY TẤT CẢ ĐƠN HÀNG CÓ PHÂN TRANG VÀ LỌC TRẠNG THÁI]
     *
     * Khi nào được gọi trên web:
     * - Người dùng thực hiện: Xem danh sách tổng hợp đơn hàng theo bộ lọc trạng thái.
     * - Màn hình/chức năng: Quản trị đơn hàng / Moderator Orders Dashboard.
     *
     * API:
     * - HTTP: GET
     * - URL: /api/orders
     * - Người gọi: Order Moderator / Admin
     *
     * Dữ liệu nhận vào:
     * - Request params: search, status (ALL, PENDING, PENDING_PAYMENT, DEPOSITED, PAID, COMPLETED, CANCELLED), sort, page, limit.
     *
     * Điều phối xử lý:
     * 1. Controller gọi OrderService.getFilteredOrders(search, status, sort, page, limit)
     * 2. Service gọi OrderRepository.searchOrdersForModerator(...)
     * 3. Controller map sang List<OrderResponseDTO>.
     *
     * Dữ liệu trả về:
     * - HTTP status: 200 OK
     * - Response body: Map chứa danh sách orders, totalCount, pages, currentPage.
     */
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

    /**
     * [LẤY DANH SÁCH ĐƠN HÀNG CỦA MODERATOR HIỆN TẠI (MY ORDERS)]
     *
     * Khi nào được gọi trên web:
     * - Người dùng thực hiện: Truy cập trang "Đơn hàng của tôi" để theo dõi các đơn mình đã nhận xử lý.
     * - Màn hình/chức năng: Moderator - My Orders (/moderator/orders/my).
     *
     * API:
     * - HTTP: GET
     * - URL: /api/orders/my
     * - Người gọi: Order Moderator (Đã đăng nhập)
     *
     * Dữ liệu nhận vào:
     * - Request params: search, status, sort, page, limit
     * - Authentication: Principal của Moderator đang đăng nhập.
     *
     * Điều phối xử lý:
     * 1. Lấy thông tin Moderator từ SecurityUtils.getCurrentUser().
     * 2. Controller gọi OrderService.getMyOrders(moderatorId, search, status, sort, page, limit).
     * 3. Service gọi OrderRepository.searchMyOrders(...) với assignedTo.userId = moderatorId.
     *
     * Dữ liệu trả về:
     * - HTTP status: 200 OK (hoặc 401 Unauthorized nếu chưa đăng nhập).
     * - Response body: Map chứa danh sách đơn hàng đã gán cho Moderator.
     */
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

    /**
     * [LẤY THỐNG KÊ KPI CÁ NHÂN CỦA MODERATOR ĐANG ĐĂNG NHẬP]
     *
     * API:
     * - HTTP: GET
     * - URL: /api/orders/my-stats
     * - Người gọi: Order Moderator
     *
     * Điều phối xử lý:
     * 1. Lấy moderator từ principal.
     * 2. Gọi OrderService.getModeratorPersonalKPIs(moderatorId).
     * 3. Đếm số lượng đơn theo các trạng thái PENDING, PENDING_PAYMENT, PAID, CANCELLED thuộc về moderator.
     *
     * Dữ liệu trả về:
     * - Map<String, Long> gồm các chỉ số total, pending, approved, paid, rejected.
     */
    @GetMapping("/my-stats")
    public ResponseEntity<Map<String, Long>> getMyStats(
            @AuthenticationPrincipal Object principal) {
        User moderator = SecurityUtils.getCurrentUser(principal, userRepository);
        if (moderator == null) {
            return ResponseEntity.status(401).build();
        }
        return ResponseEntity.ok(orderService.getModeratorPersonalKPIs(moderator.getUserId()));
    }

    /**
     * [TIẾP NHẬN ĐƠN HÀNG TỪ KHO CHUNG (CLAIM ORDER)]
     *
     * Khi nào được gọi trên web:
     * - Người dùng thực hiện: Moderator bấm nút "Tiếp nhận đơn" (Claim) trên card đơn hàng ở trang Orders Pool.
     * - Màn hình/chức năng: Moderator - Orders Pool.
     *
     * API:
     * - HTTP: POST
     * - URL: /api/orders/{orderCode}/claim
     * - Người gọi: Order Moderator
     *
     * Dữ liệu nhận vào:
     * - Path variable: orderCode (Mã đơn hàng, ví dụ "BSMS-ABC123").
     * - Authentication: Principal của Moderator đang đăng nhập.
     *
     * Điều phối xử lý:
     * 1. Controller gọi OrderService.claimOrder(orderCode, moderator)
     * 2. Service tìm đơn qua OrderRepository.findByOrderCode().
     * 3. Kiểm tra điều kiện nghiệp vụ:
     *    - Đơn phải chưa có ai nhận (assignedTo == null).
     *    - Đơn phải ở trạng thái "PENDING".
     * 4. Cập nhật Order: setAssignedTo(moderator), setAssignedAt(now). Lưu bằng orderRepository.save().
     * 5. Tạo và lưu bản ghi OrderHandling mới (isActive = true) qua orderHandlingRepository.save().
     *
     * Dữ liệu trả về:
     * - HTTP status: 200 OK (Thành công), 401 (Chưa đăng nhập), 409 Conflict (Đơn đã có người nhận hoặc sai trạng thái), 500 (Lỗi hệ thống).
     * - Response body: Map {"success": true, "message": "Nhận đơn hàng thành công."}
     *
     * Tác động dữ liệu:
     * - Bảng/Entity bị đọc: ORDER
     * - Bảng/Entity bị ghi/cập nhật:
     *   + ORDER: assigned_to = moderator.userId, assigned_at = now
     *   + ORDER_HANDLING: thêm bản ghi mới với isActive = true
     * - Thay đổi trạng thái: Order vẫn giữ "PENDING" nhưng đã được gán người phụ trách.
     */
    @PostMapping("/{orderCode}/claim")
    public ResponseEntity<Map<String, Object>> claimOrder(
            @PathVariable String orderCode,
            @AuthenticationPrincipal Object principal) {

        Map<String, Object> response = new HashMap<>();
        User moderator = SecurityUtils.getCurrentUser(principal, userRepository);
        if (moderator == null) {
            response.put("success", false);
            response.put("message", "Chưa đăng nhập.");
            return ResponseEntity.status(401).body(response);
        }

        try {
            boolean success = orderService.claimOrder(orderCode, moderator);
            response.put("success", success);
            response.put("message", "Nhận đơn hàng thành công.");
            return ResponseEntity.ok(response);
        } catch (IllegalStateException e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.status(409).body(response);
        } catch (org.springframework.dao.DataAccessException | jakarta.persistence.PersistenceException e) {
            response.put("success", false);
            response.put("message", "Lỗi cơ sở dữ liệu: Dữ liệu không hợp lệ hoặc vượt quá giới hạn.");
            return ResponseEntity.badRequest().body(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Lỗi máy chủ khi xử lý: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * [TRẢ LẠI ĐƠN HÀNG VỀ KHO CHUNG (UNCLAIM / RETURN INVENTORY)]
     *
     * Khi nào được gọi trên web:
     * - Người dùng thực hiện: Moderator bấm "Trả về kho chung" trên trang My Orders hoặc Order Detail khi không thể tiếp tục xử lý đơn chưa duyệt.
     * - Màn hình/chức năng: Moderator - My Orders / Order Detail.
     *
     * API:
     * - HTTP: POST
     * - URL: /api/orders/{orderCode}/unclaim
     * - Người gọi: Order Moderator đang phụ trách đơn
     *
     * Điều phối xử lý:
     * 1. Controller gọi OrderService.unclaimOrder(orderCode, moderator)
     * 2. Service kiểm tra quyền sở hữu (order.getAssignedTo() == moderator) và trạng thái đơn phải là "PENDING".
     * 3. Set assignedTo = null, assignedAt = null trên Order.
     * 4. Cập nhật các bản ghi OrderHandling đang active: set isActive = false, releasedAt = now.
     *
     * Dữ liệu trả về:
     * - HTTP status: 200 OK (Thành công), 400 (Lỗi nghiệp vụ).
     *
     * Tác động dữ liệu:
     * - ORDER: assigned_to = null, assigned_at = null
     * - ORDER_HANDLING: isActive = false, releasedAt = now
     */
    @PostMapping("/{orderCode}/unclaim")
    public ResponseEntity<Map<String, Object>> unclaimOrder(
            @PathVariable String orderCode,
            @AuthenticationPrincipal Object principal) {

        Map<String, Object> response = new HashMap<>();
        User moderator = SecurityUtils.getCurrentUser(principal, userRepository);
        if (moderator == null) {
            response.put("success", false);
            response.put("message", "Chưa đăng nhập.");
            return ResponseEntity.status(401).body(response);
        }

        try {
            boolean success = orderService.unclaimOrder(orderCode, moderator);
            response.put("success", success);
            response.put("message", "Đã trả đơn hàng về Pool thành công.");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * [LẤY THỐNG KÊ TỔNG QUAN HỆ THỐNG (KPIS)]
     *
     * API:
     * - HTTP: GET
     * - URL: /api/orders/kpis
     * - Người gọi: Order Moderator
     *
     * Điều phối xử lý:
     * 1. Gọi OrderService.getKPIs()
     * 2. Truy vấn đếm số lượng đơn toàn hệ thống theo PENDING, PENDING_PAYMENT, PAID, CANCELLED.
     *
     * Dữ liệu trả về:
     * - Map<String, Long> gồm total, pending, approved, paid, cancelled.
     */
    @GetMapping("/kpis")
    public ResponseEntity<Map<String, Long>> getKPIs() {
        return ResponseEntity.ok(orderService.getKPIs());
    }

    /**
     * [LẤY CHI TIẾT ĐƠN HÀNG THEO MÃ ĐƠN (ORDER CODE)]
     *
     * Khi nào được gọi trên web:
     * - Người dùng thực hiện: Bấm vào xem chi tiết đơn hàng hoặc mở trang Order Detail.
     * - Màn hình/chức năng: Chi tiết đơn hàng / Drawer xem nhanh.
     *
     * API:
     * - HTTP: GET
     * - URL: /api/orders/{orderCode}
     * - Người gọi: Customer / Moderator
     *
     * Điều phối xử lý:
     * 1. Gọi OrderService.getOrderByCode(orderCode)
     * 2. Convert Order entity sang OrderResponseDTO (tính toán treePrice, immediatePaymentAmount, remainingPaymentAmount, payments history, handling history).
     *
     * Dữ liệu trả về:
     * - HTTP status: 200 OK (hoặc 404 Not Found).
     * - Response body: OrderResponseDTO đầy đủ thông tin khách hàng, sản phẩm, thanh toán.
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
     * [PHÊ DUYỆT ĐƠN HÀNG VÀ ÁP PHÍ / TIỀN CỌC (VERIFY / APPROVE ORDER)]
     *
     * Khi nào được gọi trên web:
     * - Người dùng thực hiện: Moderator liên hệ thỏa thuận với khách, sau đó nhập Phí cẩu (Crane Fee), Phí vận chuyển (Shipping Fee), và Số tiền đặt cọc (Deposit Amount) rồi bấm "Xác nhận duyệt đơn".
     * - Màn hình/chức năng: Modal Duyệt đơn hàng trên giao diện Moderator.
     *
     * API:
     * - HTTP: POST
     * - URL: /api/orders/{orderCode}/verify
     * - Người gọi: Order Moderator đang phụ trách đơn
     *
     * Dữ liệu nhận vào:
     * - Path variable: orderCode
     * - Request body: Map<String, Object> payload (craneFee, shippingFee, depositAmount)
     * - Authentication: Principal của Moderator
     *
     * Điều phối xử lý:
     * 1. Controller trích xuất và parse craneFee, shippingFee, depositAmount.
     * 2. Gọi OrderService.verifyOrder(orderCode, craneFee, shippingFee, depositAmount, moderator).
     * 3. Service kiểm tra quyền phụ trách và trạng thái PENDING.
     * 4. Tính toán:
     *    - treePrice = tổng giá các cây trong OrderDetail.
     *    - totalAmount mới = treePrice + craneFee + shippingFee.
     *    - Nếu flow Đặt cọc (DEPOSIT/COD): kiểm tra depositAmount hợp lệ (<= treePrice), tạo/cập nhật Payment record #1 với paymentType = "DEPOSIT", amount = depositAmount, status = "PENDING".
     *    - Nếu flow Thanh toán đủ (FULL_PAYMENT/VNPAY): set depositAmount = 0, tạo/cập nhật Payment record #1 với paymentType = "FULL_PAYMENT", amount = totalAmount, status = "PENDING".
     * 5. Cập nhật Order: orderStatus = "PENDING_PAYMENT".
     * 6. Ghi bản ghi OrderLog (actionType = "VERIFY", fromStatus = "PENDING", toStatus = "PENDING_PAYMENT").
     * 7. Đóng bản ghi OrderHandling hiện tại (isActive = false, releasedAt = now).
     * 8. Bắn sự kiện OrderVerifiedEvent → OrderEventListener bắt sự kiện và gọi MailService.sendOrderApprovedEmail() gửi email kèm link thanh toán VNPay cho khách.
     *
     * Dữ liệu trả về:
     * - HTTP status: 200 OK, 400 Bad Request, 403 Forbidden.
     * - Response body: Map {"success": true, "message": "Duyệt đơn hàng thành công."}
     *
     * Tác động dữ liệu:
     * - Bảng/Entity bị đọc: ORDER, ORDER_DETAIL, PAYMENT, USER
     * - Bảng/Entity bị ghi/cập nhật:
     *   + ORDER: craneFee, shippingFee, totalAmount, depositAmount, orderStatus = "PENDING_PAYMENT"
     *   + PAYMENT: lưu Payment record với status = "PENDING"
     *   + ORDER_LOG: thêm bản ghi log "VERIFY"
     *   + ORDER_HANDLING: isActive = false
     * - Thay đổi trạng thái: Order: PENDING → PENDING_PAYMENT
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
            response.put("message", "Chưa đăng nhập hệ thống.");
            return ResponseEntity.status(401).body(response);
        }

        try {
            BigDecimal craneFee = parseNullableBigDecimal(payload.get("craneFee"));
            BigDecimal shippingFee = parseNullableBigDecimal(payload.get("shippingFee"));
            BigDecimal depositAmount = parseNullableBigDecimal(payload.get("depositAmount"));
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
        } catch (org.springframework.dao.DataAccessException | jakarta.persistence.PersistenceException e) {
            response.put("success", false);
            response.put("message", "Lỗi cơ sở dữ liệu: Dữ liệu không hợp lệ hoặc vượt quá giới hạn.");
            return ResponseEntity.badRequest().body(response);
        } catch (Exception e) {
            log.error("Lỗi khi duyệt đơn hàng {}", orderCode, e);
            response.put("success", false);
            response.put("message", "Lỗi hệ thống: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * [MODERATOR XÁC NHẬN ĐÃ THU ĐỦ TIỀN ĐỢT 2 CHO ĐƠN ĐẶT CỌC]
     *
     * Khi nào được gọi trên web:
     * - Người dùng thực hiện: Đơn hàng đã ở trạng thái DEPOSITED (khách đã cọc thành công). Khi giao cây đến nhà và thu tiền mặt/chuyển khoản phần còn lại, Moderator bấm "Xác nhận thu đủ tiền".
     * - Màn hình/chức năng: Moderator - Order Detail.
     *
     * API:
     * - HTTP: POST
     * - URL: /api/orders/{orderCode}/confirm-remaining-payment
     * - Người gọi: Order Moderator đang phụ trách đơn
     *
     * Dữ liệu nhận vào:
     * - Path variable: orderCode
     * - Request body: Map<String, String> payload (notes)
     * - Authentication: Principal của Moderator
     *
     * Điều phối xử lý:
     * 1. Controller gọi OrderService.confirmRemainingPayment(orderCode, notes, moderator).
     * 2. Service kiểm tra trạng thái đơn phải là "DEPOSITED" và đúng Moderator phụ trách.
     * 3. Tính tiền còn lại: remainingAmount = totalAmount - depositPaid.
     * 4. Tạo Payment record #2: paymentType = "REMAINING_PAYMENT", paymentMethod = "CASH", paymentStatus = "SUCCESS", amount = remainingAmount, paymentDate = now.
     * 5. Cập nhật Order: orderStatus = "COMPLETED", completedAt = now.
     * 6. Cập nhật trạng thái tất cả cây trong đơn sang "SOLD" qua markProductsAsSold().
     * 7. Ghi nhận sổ cái doanh thu hoàn tất qua FinancialLedgerService.recordCompletedOrderRevenueIfAbsent().
     * 8. Ghi OrderLog (actionType = "REMAINING_PAYMENT_CONFIRMED", fromStatus = "DEPOSITED", toStatus = "COMPLETED").
     * 9. Bắn sự kiện OrderPaidEvent để gửi hóa đơn hoàn tất qua email.
     *
     * Dữ liệu trả về:
     * - HTTP status: 200 OK, 400, 409.
     * - Response body: Map {"success": true, "message": "Xác nhận đã thanh toán đầy đủ thành công!"}
     *
     * Tác động dữ liệu:
     * - PAYMENT: thêm bản ghi REMAINING_PAYMENT (SUCCESS)
     * - ORDER: orderStatus = "COMPLETED", completedAt = now
     * - PRODUCT: productStatus: RESERVED → SOLD
     * - FINANCIAL_LEDGER: ghi nhận doanh thu
     * - ORDER_LOG: log REMAINING_PAYMENT_CONFIRMED
     * - Thay đổi trạng thái: Order: DEPOSITED → COMPLETED; Product: RESERVED → SOLD
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
            response.put("message", "Chưa đăng nhập hệ thống.");
            return ResponseEntity.status(401).body(response);
        }

        String notes = (payload != null) ? payload.getOrDefault("notes", "") : "";
        try {
            boolean success = orderService.confirmRemainingPayment(orderCode, notes, moderator);
            response.put("success", success);
            response.put("message", success ? "Xác nhận đã thanh toán đầy đủ thành công!" : "Xác nhận thất bại.");
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        } catch (IllegalStateException e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.status(409).body(response);
        } catch (org.springframework.dao.DataAccessException | jakarta.persistence.PersistenceException e) {
            response.put("success", false);
            response.put("message", "Lỗi cơ sở dữ liệu: Dữ liệu không hợp lệ hoặc vượt quá giới hạn.");
            return ResponseEntity.badRequest().body(response);
        } catch (Exception e) {
            log.error("Lỗi khi xác nhận thanh toán đủ đơn {}", orderCode, e);
            response.put("success", false);
            response.put("message", "Lỗi hệ thống: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * [XÁC NHẬN HOÀN TẤT ĐƠN HÀNG ĐÃ THANH TOÁN 100% (COMPLETE PAID ORDER)]
     *
     * Khi nào được gọi trên web:
     * - Người dùng thực hiện: Đơn hàng ở trạng thái "PAID" (khách đã thanh toán 100% qua VNPay), sau khi giao cây thành công, Moderator bấm "Hoàn tất đơn hàng".
     * - Màn hình/chức năng: Moderator - Order Detail.
     *
     * API:
     * - HTTP: POST
     * - URL: /api/orders/{orderCode}/complete
     * - Người gọi: Order Moderator
     *
     * Điều phối xử lý:
     * 1. Controller gọi OrderService.completePaidOrder(orderCode, moderator).
     * 2. Service kiểm tra trạng thái đơn phải là "PAID" và đã thu đủ 100% tiền.
     * 3. Cập nhật Order: orderStatus = "COMPLETED", completedAt = now.
     * 4. Cập nhật Product: chuyển sang "SOLD".
     * 5. Ghi nhận sổ cái doanh thu và gửi thông báo nhắc đánh giá sản phẩm.
     * 6. Ghi OrderLog (actionType = "ORDER_COMPLETED", toStatus = "COMPLETED").
     *
     * Dữ liệu trả về:
     * - HTTP status: 200 OK
     * - Response body: Map {"success": true, "message": "Đơn hàng đã hoàn thành."}
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
        } catch (org.springframework.dao.DataAccessException | jakarta.persistence.PersistenceException e) {
            response.put("success", false);
            response.put("message", "Lỗi cơ sở dữ liệu: Dữ liệu không hợp lệ hoặc vượt quá giới hạn.");
            return ResponseEntity.badRequest().body(response);
        } catch (Exception e) {
            log.error("Lỗi khi hoàn thành đơn {}", orderCode, e);
            response.put("success", false);
            response.put("message", "Lỗi hệ thống: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * [GHI NHẬN KHÁCH KHÔNG NHẬN HÀNG & TỊCH THU TIỀN CỌC (CUSTOMER NO-SHOW)]
     *
     * Khi nào được gọi trên web:
     * - Người dùng thực hiện: Đơn hàng ở trạng thái DEPOSITED, khi giao cây khách từ chối nhận vô lý hoặc không liên lạc được, Moderator bấm "Khách không nhận hàng".
     * - Màn hình/chức năng: Moderator - Order Detail.
     *
     * API:
     * - HTTP: POST
     * - URL: /api/orders/{orderCode}/customer-no-show
     * - Người gọi: Order Moderator
     *
     * Điều phối xử lý:
     * 1. Controller gọi OrderService.markDepositedOrderCustomerNoShow(orderCode, notes, moderator).
     * 2. Service kiểm tra trạng thái đơn phải là "DEPOSITED".
     * 3. Ghi nhận giữ lại tiền cọc (Forfeited Deposit Income) vào sổ cái tài chính FinancialLedger.
     * 4. Cập nhật Order: orderStatus = "CANCELLED", ghi chú lý do.
     * 5. Giải phóng cây về trạng thái "AVAILABLE" qua releaseProducts().
     * 6. Ghi OrderLog và bắn sự kiện OrderRejectedEvent.
     *
     * Dữ liệu trả về:
     * - HTTP status: 200 OK
     * - Response body: Map {"success": true, "message": "Đã hủy đơn vì khách không nhận. Tiền cọc không hoàn."}
     *
     * Tác động dữ liệu:
     * - ORDER: orderStatus = "CANCELLED"
     * - PRODUCT: productStatus = "AVAILABLE" (Giải phóng cây để người khác mua)
     * - FINANCIAL_LEDGER: ghi nhận thu nhập từ tiền cọc bị tịch thu
     */
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
        } catch (org.springframework.dao.DataAccessException | jakarta.persistence.PersistenceException e) {
            response.put("success", false);
            response.put("message", "Lỗi cơ sở dữ liệu: Dữ liệu không hợp lệ hoặc vượt quá giới hạn.");
            return ResponseEntity.badRequest().body(response);
        } catch (Exception e) {
            log.error("Lỗi khi hủy đơn vì khách không nhận {}", orderCode, e);
            response.put("success", false);
            response.put("message", "Lỗi hệ thống: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * [TỪ CHỐI / HỦY ĐƠN HÀNG CHỜ DUYỆT (REJECT ORDER)]
     *
     * Khi nào được gọi trên web:
     * - Người dùng thực hiện: Moderator kiểm tra đơn hàng PENDING nhưng không hợp lệ (ví dụ: địa chỉ không hỗ trợ, cây bị lỗi, khách đổi ý trước khi duyệt) và bấm "Từ chối duyệt đơn" kèm lý do.
     * - Màn hình/chức năng: Modal Từ chối đơn trên giao diện Moderator.
     *
     * API:
     * - HTTP: POST
     * - URL: /api/orders/{orderCode}/reject
     * - Người gọi: Order Moderator đang phụ trách đơn
     *
     * Dữ liệu nhận vào:
     * - Path variable: orderCode
     * - Request body: Map<String, String> payload (reason)
     * - Authentication: Principal của Moderator
     *
     * Điều phối xử lý:
     * 1. Controller gọi OrderService.rejectOrder(orderCode, reason, moderator).
     * 2. Service kiểm tra quyền phụ trách và trạng thái đơn phải là "PENDING".
     * 3. Cập nhật Order: orderStatus = "CANCELLED", ghi chú lý do từ chối.
     * 4. Giải phóng toàn bộ cây trong đơn từ "RESERVED" về "AVAILABLE" trong PRODUCT.
     * 5. Ghi OrderLog (actionType = "REJECT", fromStatus = "PENDING", toStatus = "CANCELLED").
     * 6. Đóng bản ghi OrderHandling (isActive = false, releasedAt = now).
     * 7. Bắn sự kiện OrderRejectedEvent → OrderEventListener gọi MailService.sendOrderRejectedEmail() gửi email thông báo hủy đơn kèm lý do cho khách.
     *
     * Dữ liệu trả về:
     * - HTTP status: 200 OK, 400 Bad Request, 403 Forbidden.
     * - Response body: Map {"success": true, "message": "Từ chối duyệt đơn hàng thành công."}
     *
     * Tác động dữ liệu:
     * - ORDER: orderStatus: PENDING → CANCELLED
     * - PRODUCT: productStatus: RESERVED → AVAILABLE (Cây mở bán lại trên Marketplace)
     * - ORDER_LOG: ghi nhận log REJECT
     * - ORDER_HANDLING: isActive = false
     */
    @PostMapping("/{orderCode}/reject")
    public ResponseEntity<Map<String, Object>> rejectOrder(
            @PathVariable String orderCode,
            @RequestBody Map<String, String> payload,
            @AuthenticationPrincipal Object principal) {

        Map<String, Object> response = new HashMap<>();
        User moderator = SecurityUtils.getCurrentUser(principal, userRepository);
        if (moderator == null) {
            response.put("success", false);
            response.put("message", "Chưa đăng nhập hệ thống.");
            return ResponseEntity.status(401).body(response);
        }

        String reason = payload.getOrDefault("reason", "");
        try {
            boolean success = orderService.rejectOrder(orderCode, reason, moderator);
            response.put("success", success);
            response.put("message", success ? "Từ chối duyệt đơn hàng thành công." : "Thao tác thất bại.");
            return ResponseEntity.ok(response);
        } catch (SecurityException e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.status(403).body(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Lỗi xử lý từ chối: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * [ĐẶT HÀNG / CHECKOUT (KHÁCH ĐÃ ĐĂNG NHẬP HOẶC KHÁCH VÃNG LAI)]
     *
     * Khi nào được gọi trên web:
     * - Người dùng thực hiện: Điền đầy đủ thông tin nhận hàng, chọn phương thức thanh toán (DEPOSIT / FULL), nhập mã OTP (nếu là Guest) và bấm "Đặt hàng ngay".
     * - Màn hình/chức năng: Màn hình Checkout / Giỏ hàng (/checkout).
     *
     * API:
     * - HTTP: POST
     * - URL: /api/orders/checkout
     * - Người gọi: Customer (Đăng nhập) hoặc Guest Customer (Vãng lai)
     *
     * Dữ liệu nhận vào:
     * - Request body: PurchaseOrderRequestDTO dto
     *   + customerName (String): Tên người nhận hàng
     *   + customerPhone (String): SĐT người nhận (10 số)
     *   + customerEmail (String): Email nhận hóa đơn và thông tin đơn hàng
     *   + shippingAddress (String): Địa chỉ giao hàng
     *   + paymentMethod (String): "DEPOSIT" (Đặt cọc) hoặc "FULL" / "VNPAY" (Thanh toán 100%)
     *   + productIds (List<Integer>) / productId (Integer): Danh sách cây đặt mua
     *   + otpCode (String): Mã xác thực OTP gửi về email (bắt buộc đối với Guest)
     *   + notes (String): Ghi chú giao hàng
     * - Authentication: Principal (null nếu là Guest)
     *
     * Điều phối xử lý:
     * 1. Chặn tài khoản nhân viên/admin (OWNER, ARTISAN, MODERATOR, ADMIN) không cho phép tự đặt hàng.
     * 2. Pre-validate trạng thái cây (nếu đã đăng nhập): kiểm tra cây còn AVAILABLE không.
     * 3. Xác thực OTP nếu là Guest:
     *    - Tìm OTP mới nhất trong RegisterOtpRepository theo email.
     *    - Kiểm tra OTP chưa dùng (isUsed = false), còn hạn (expiredAt > now), đúng mã (otpCode).
     *    - Đánh dấu OTP đã dùng (isUsed = true) và lưu lại.
     * 4. Gọi OrderService.createOrder(dto, customer):
     *    - Load sản phẩm từ DB.
     *    - Thực hiện giữ chỗ nguyên tử (Atomic Reserve) từng cây qua productRepository.reserveIfAvailable(productId).
     *    - Cập nhật productStatus của cây sang "RESERVED".
     *    - Tạo Order entity với orderStatus = "PENDING", orderType = "ONLINE", mã đơn sinh dạng "BSMS-XXXXXX".
     *    - Tạo danh sách OrderDetail lưu snapshot giá mua (priceAtPurchase = product.price).
     *    - Lưu Order vào ORDER table.
     *    - Khởi tạo 1 bản ghi Payment ban đầu với paymentStatus = "PENDING" (paymentType = DEPOSIT hoặc FULL_PAYMENT theo lựa chọn của khách).
     *    - Nếu khách đã đăng nhập: xóa sạch giỏ hàng qua CartService.clearCart().
     *    - Bắn sự kiện OrderCreatedEvent → gửi email xác nhận đã tiếp nhận đơn hàng.
     *
     * Dữ liệu trả về:
     * - HTTP status: 200 OK, 400 Bad Request, 403 Forbidden, 500 Internal Server Error.
     * - Response body: Map {"success": true, "paymentMethod": "DEPOSIT", "orderCode": "BSMS-ABC123"}
     *
     * Tác động dữ liệu:
     * - Bảng/Entity bị đọc: PRODUCT, PASSWORD_RESET_OTP, CART_ITEM
     * - Bảng/Entity bị ghi/cập nhật:
     *   + ORDER: tạo mới bản ghi (orderStatus = "PENDING")
     *   + ORDER_DETAIL: tạo các bản ghi chi tiết (snapshot giá)
     *   + PRODUCT: productStatus: AVAILABLE → RESERVED (giữ chỗ không cho người khác mua)
     *   + PAYMENT: tạo 1 bản ghi ban đầu (paymentStatus = "PENDING")
     *   + PASSWORD_RESET_OTP: isUsed = true (đối với Guest)
     *   + CART_ITEM: xóa giỏ hàng (đối với Customer)
     * - Thay đổi trạng thái: Order: null → PENDING; Product: AVAILABLE → RESERVED; Payment: null → PENDING
     */
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
                        || a.getAuthority().equals("ROLE_ADMIN"));
        if (isStaffOrAdmin) {
            response.put("success", false);
            response.put("message",
                    "TÃ i khoáº£n quáº£n trá»‹, nhÃ  vÆ°á»n hoáº·c kiá»ƒm duyá»‡t viÃªn khÃ´ng Ä‘Æ°á»£c phÃ©p thá»±c hiá»‡n Ä‘áº·t hÃ ng!");
            return ResponseEntity.status(403).body(response);
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
        // Xác thực mã OTP nếu là Khách vãng lai (Guest Checkout)
        if (customer == null) {
            String otpCode = dto.getOtpCode();
            if (otpCode == null || otpCode.trim().isEmpty()) {
                response.put("success", false);
                response.put("requireOtp", true);
                response.put("message", "Vui lòng nhập mã OTP xác nhận được gửi về Email.");
                return ResponseEntity.badRequest().body(response);
            }

            PasswordResetOtp otp = registerOtpRepository.findTopByEmailOrderByCreatedAtDesc(dto.getCustomerEmail())
                    .orElse(null);
            if (otp == null || Boolean.TRUE.equals(otp.getIsUsed()) || otp.getExpiredAt().isBefore(LocalDateTime.now())
                    || !otp.getOtpCode().equals(otpCode.trim())) {
                response.put("success", false);
                response.put("message", "Mã OTP không hợp lệ hoặc đã hết hạn. Vui lòng lấy mã mới và thử lại!");
                return ResponseEntity.badRequest().body(response);
            }

            otp.setIsUsed(true);
            registerOtpRepository.save(otp);
        }

        try {
            Order createdOrder = orderService.createOrder(dto, customer);
            response.put("success", true);
            response.put("paymentMethod", dto.getPaymentMethod() != null ? dto.getPaymentMethod() : "DEPOSIT");
            response.put("orderCode", createdOrder.getOrderCode());
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException | IllegalStateException e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        } catch (org.springframework.dao.DataAccessException | jakarta.persistence.PersistenceException e) {
            response.put("success", false);
            response.put("message", "Lỗi cơ sở dữ liệu: Dữ liệu không hợp lệ hoặc vượt quá giới hạn.");
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

        cld.add(Calendar.MINUTE, onlineExpirationMinutes);
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
            resolvedPaymentMethod = isDepositFlow ? "DEPOSIT" : "VNPAY";
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

    private BigDecimal parseNullableBigDecimal(Object value) {
        if (value == null) {
            return null;
        }

        String raw = value.toString().trim();
        if (raw.isEmpty() || "null".equalsIgnoreCase(raw)) {
            return null;
        }

        return new BigDecimal(raw);
    }
}
