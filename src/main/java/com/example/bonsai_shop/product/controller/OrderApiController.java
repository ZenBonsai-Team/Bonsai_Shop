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

@RestController
@RequestMapping("/api/orders")
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
    public ResponseEntity<Map<String, Object>> sendGuestOtp(@RequestBody Map<String, String> payload) {
        Map<String, Object> response = new HashMap<>();
        String email = payload.get("email");
        if (email == null || !email.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) {
            response.put("success", false);
            response.put("message", "Địa chỉ Email không hợp lệ!");
            return ResponseEntity.badRequest().body(response);
        }

        try {
            registerOtpRepository.deleteByEmail(email);
        } catch (Exception ignored) {}

        String otpCode = String.format("%06d", new java.security.SecureRandom().nextInt(1000000));
        PasswordResetOtp otp = PasswordResetOtp.builder()
                .email(email)
                .otpCode(otpCode)
                .expiredAt(LocalDateTime.now().plusMinutes(5))
                .isUsed(false)
                .createdAt(LocalDateTime.now())
                .build();

        registerOtpRepository.save(otp);
        emailService.sendGuestOrderOtp(email, otpCode);

        response.put("success", true);
        response.put("message", "Mã OTP xác nhận đơn hàng đã được gửi tới Email: " + email);
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
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Lỗi máy chủ khi xử lý: " + e.getMessage());
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
     * API Lấy thống kê số lượng đơn hàng theo các trạng thái (KPIs)
     */
    @GetMapping("/kpis")
    public ResponseEntity<Map<String, Long>> getKPIs() {
        return ResponseEntity.ok(orderService.getKPIs());
    }

    /**
     * API Lấy chi tiết một đơn hàng theo mã đơn
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
     * API Duyệt đơn hàng (Cập nhật phí cẩu, phí ship)
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

        BigDecimal craneFee = new BigDecimal(payload.getOrDefault("craneFee", 0).toString());
        BigDecimal shippingFee = new BigDecimal(payload.getOrDefault("shippingFee", 0).toString());

        boolean success = orderService.verifyOrder(orderCode, craneFee, shippingFee, moderator);
        response.put("success", success);
        response.put("message", success ? "Duyệt đơn hàng thành công." : "Duyệt đơn hàng thất bại.");

        return ResponseEntity.ok(response);
    }

    /**
     * API Từ chối duyệt đơn hàng (Có lý do)
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

    @PostMapping("/checkout")
    @Transactional
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
                    "Tài khoản quản trị, nhà vườn hoặc kiểm duyệt viên không được phép thực hiện đặt hàng!");
            return ResponseEntity.status(403).body(response);
        }

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

        // 1. Lấy danh sách sản phẩm cần đặt hàng (từ DTO hoặc từ Giỏ hàng trong DB)
        List<Product> productsToBuy = new ArrayList<>();
        if (dto.getProductIds() != null && !dto.getProductIds().isEmpty()) {
            for (Integer pId : dto.getProductIds()) {
                productRepository.findById(pId).ifPresent(productsToBuy::add);
            }
        } else if (dto.getProductId() != null) {
            productRepository.findById(dto.getProductId()).ifPresent(productsToBuy::add);
        } else if (customer != null) {
            List<CartItem> cartItems = cartService.getCartItems(customer.getUserId());
            if (cartItems != null) {
                for (CartItem item : cartItems) {
                    productsToBuy.add(item.getProduct());
                }
            }
        }

        if (productsToBuy.isEmpty()) {
            response.put("success", false);
            response.put("message", "Giỏ hàng của bạn đang trống! Vui lòng chọn sản phẩm trước.");
            return ResponseEntity.badRequest().body(response);
        }

        // 2. Kiểm tra tính khả dụng của tất cả các sản phẩm
        for (Product prod : productsToBuy) {
            if (!"AVAILABLE".equalsIgnoreCase(prod.getProductStatus()) || Boolean.FALSE.equals(prod.getIsVisible())) {
                response.put("success", false);
                response.put("message",
                        "Tác phẩm '" + prod.getProductName() + "' đã được bán hoặc giữ chỗ bởi khách hàng khác!");
                return ResponseEntity.badRequest().body(response);
            }
        }

        // 3. Khởi tạo Đơn Hàng mới (Order)
        String orderCode = "BSMS-" + VNPayConfig.getRandomNumber(6).toUpperCase();
        BigDecimal totalAmount = productsToBuy.stream()
                .map(Product::getPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Order order = Order.builder()
                .customer(customer)
                .orderCode(orderCode)
                .customerName(dto.getCustomerName())
                .customerPhone(dto.getCustomerPhone())
                .customerEmail(dto.getCustomerEmail())
                .shippingAddress(dto.getShippingAddress())
                .orderDate(LocalDateTime.now())
                .totalAmount(totalAmount)
                .depositAmount(BigDecimal.ZERO)
                .orderStatus("PENDING")
                .orderType("ONLINE")
                .build();

        // 4. Liên kết các sản phẩm chi tiết (OrderDetail)
        List<OrderDetail> details = productsToBuy.stream().map(prod -> {
            int reserved = productRepository.reserveIfAvailable(prod.getProductId());
            if (reserved == 0) {
                throw new IllegalStateException("Tác phẩm '" + prod.getProductName() + "' đã được bán hoặc giữ chỗ!");
            }
            prod.setProductStatus("RESERVED");
            return OrderDetail.builder()
                    .order(order)
                    .product(prod)
                    .priceAtPurchase(prod.getPrice())
                    .build();
        }).collect(Collectors.toList());

        order.setOrderDetails(details);
        orderRepository.save(order);

        // 5. Xóa sạch giỏ hàng DB nếu là User đã đăng nhập
        if (customer != null) {
            cartService.clearCart(customer.getUserId());
        }

        // 6. Xử lý phân nhánh Phương thức thanh toán
        if ("VNPAY".equalsIgnoreCase(dto.getPaymentMethod())) {
            response.put("success", true);
            response.put("paymentMethod", "VNPAY");
            response.put("orderCode", orderCode);
        } else {
            response.put("success", true);
            response.put("paymentMethod", "COD");
            response.put("orderCode", orderCode);
        }

        return ResponseEntity.ok(response);
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
                        URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII.toString()).replace("+", "%20"));
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
                .totalAmount(order.getTotalAmount())
                .depositAmount(order.getDepositAmount())
                .orderDate(order.getOrderDate())
                .orderStatus(order.getOrderStatus())
                .orderType(order.getOrderType())
                .craneFee(order.getCraneFee())
                .shippingFee(order.getShippingFee())
                .notes(order.getNotes())
                .assignedToUsername(order.getAssignedTo() != null ? order.getAssignedTo().getUsername() : null)
                .assignedToFullName(order.getAssignedTo() != null ? order.getAssignedTo().getFullName() : null)
                .assignedAt(order.getAssignedAt())
                .handlingHistory(handlingHistory)
                .build();
    }
}
