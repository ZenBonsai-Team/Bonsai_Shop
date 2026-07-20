package com.example.bonsai_shop.product.controller;

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
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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

    @GetMapping
    public ResponseEntity<Map<String, Object>> getOrders(
            @RequestParam(defaultValue = "") String search,
            @RequestParam(defaultValue = "ALL") String status,
            @RequestParam(defaultValue = "date_desc") String sort,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "5") int limit) {

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
            @AuthenticationPrincipal UserDetails currentUser) {

        Map<String, Object> response = new HashMap<>();
        if (currentUser == null) {
            response.put("success", false);
            response.put("message", "Chưa đăng nhập hệ thống.");
            return ResponseEntity.status(401).body(response);
        }

        User moderator = userRepository.findByEmail(currentUser.getUsername()).orElse(null);
        if (moderator == null) {
            response.put("success", false);
            response.put("message", "Người dùng không hợp lệ.");
            return ResponseEntity.badRequest().body(response);
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
            @AuthenticationPrincipal UserDetails currentUser) {

        Map<String, Object> response = new HashMap<>();
        if (currentUser == null) {
            response.put("success", false);
            response.put("message", "Chưa đăng nhập hệ thống.");
            return ResponseEntity.status(401).body(response);
        }

        User moderator = userRepository.findByEmail(currentUser.getUsername()).orElse(null);
        if (moderator == null) {
            response.put("success", false);
            response.put("message", "Người dùng không hợp lệ.");
            return ResponseEntity.badRequest().body(response);
        }

        String reason = payload.getOrDefault("reason", "");
        boolean success = orderService.rejectOrder(orderCode, reason, moderator);
        response.put("success", success);
        response.put("message", success ? "Từ chối duyệt đơn hàng thành công." : "Thao tác thất bại.");

        return ResponseEntity.ok(response);
    }

    @PostMapping("/checkout")
    @Transactional
    public ResponseEntity<Map<String, Object>> checkout(
            @Valid @RequestBody PurchaseOrderRequestDTO dto,
            @AuthenticationPrincipal UserDetails currentUser,
            HttpServletRequest request) throws UnsupportedEncodingException {

        Map<String, Object> response = new HashMap<>();

        // 1. Kiểm tra sản phẩm
        Product product = productRepository.findById(dto.getProductId())
                .orElse(null);
        if (product == null) {
            response.put("success", false);
            response.put("message", "Sản phẩm cây cảnh này không tồn tại!");
            return ResponseEntity.badRequest().body(response);
        }

        // Đảm bảo cây đang ở trạng thái AVAILABLE
        if (!"AVAILABLE".equals(product.getProductStatus())) {
            response.put("success", false);
            response.put("message", "Tác phẩm này đã được bán hoặc đã có khách đặt trước!");
            return ResponseEntity.badRequest().body(response);
        }

        // 2. Lấy thông tin user hiện tại (nếu đã đăng nhập)
        User customer = null;
        if (currentUser != null) {
            boolean isStaffOrAdmin = currentUser.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_OWNER") || a.getAuthority().equals("ROLE_ARTISAN") 
                            || a.getAuthority().equals("ROLE_MODERATOR") || a.getAuthority().equals("ROLE_CONTENT_MODERATOR")
                            || a.getAuthority().equals("ROLE_ADMIN") || a.getAuthority().equals("ROLE_SELLER"));
            if (isStaffOrAdmin) {
                response.put("success", false);
                response.put("message", "Tài khoản quản trị, nhà vườn hoặc kiểm duyệt viên không được phép thực hiện đặt hàng!");
                return ResponseEntity.status(403).body(response);
            }
            customer = userRepository.findByEmail(currentUser.getUsername()).orElse(null);
        }

        // 3. Khởi tạo BonsaiOrder
        String orderCode = "BSMS-" + VNPayConfig.getRandomNumber(6).toUpperCase();
        Order order = Order.builder()
                .customer(customer)
                .orderCode(orderCode)
                .customerName(dto.getCustomerName())
                .customerPhone(dto.getCustomerPhone())
                .customerEmail(dto.getCustomerEmail())
                .shippingAddress(dto.getShippingAddress())
                .orderDate(LocalDateTime.now())
                .totalAmount(product.getPrice())
                .depositAmount(BigDecimal.ZERO)
                .orderStatus("PENDING")
                .build();

        // 4. Thiết lập chi tiết đơn hàng (OrderDetail)
        OrderDetail detail = OrderDetail.builder()
                .order(order)
                .product(product)
                .priceAtPurchase(product.getPrice())
                .build();
        order.setOrderDetails(Collections.singletonList(detail));

        // 5. Cập nhật trạng thái sản phẩm thành RESERVED để tránh người khác đặt mua
        // trùng
        product.setProductStatus("RESERVED");
        productRepository.save(product);

        // Lưu đơn hàng vào cơ sở dữ liệu
        orderRepository.save(order);

        // 6. Xử lý phân nhánh Phương thức thanh toán
        if ("VNPAY".equalsIgnoreCase(dto.getPaymentMethod())) {
            // Logic tạo URL thanh toán VNPay (Tạm thời không nhảy sang trang VNPay mà xử lý hiển thị thành công giống COD)
            response.put("success", true);
            response.put("paymentMethod", "VNPAY");
            response.put("orderCode", orderCode);
        } else {
            // Thanh toán COD thành công
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
        OrderResponseDTO.ProductDTO prodcutDTO = null;
        if (order.getOrderDetails() != null && !order.getOrderDetails().isEmpty()) {
            OrderDetail detail = order.getOrderDetails().get(0);
            Product prod = detail.getProduct();
            if (prod != null) {
                prodcutDTO = OrderResponseDTO.ProductDTO.builder()
                        .id(prod.getProductId())
                        .name(prod.getProductName())
                        .image(prod.getFirstImageUrl())
                        .price(prod.getPrice())
                        .build();
            }
        }

        // Lấy thông tin từ các thuộc tính trực tiếp của Order tránh NullPointerException
        return OrderResponseDTO.builder()
                .orderId(order.getOrderId())
                .orderCode(order.getOrderCode())
                .customer(OrderResponseDTO.CustomerDTO.builder()
                        .name(order.getCustomerName())
                        .email(order.getCustomerEmail())
                        .phone(order.getCustomerPhone())
                        .address(order.getShippingAddress()) // Bổ sung address cho Drawer
                        .build())
                .product(prodcutDTO)
                .quantity(1) // Đảm bảo luôn có số lượng để không bị NaN giá trị
                .totalAmount(order.getTotalAmount())
                .depositAmount(order.getDepositAmount())
                .orderDate(order.getOrderDate())
                .orderStatus(order.getOrderStatus())
                .craneFee(order.getCraneFee())
                .shippingFee(order.getShippingFee())
                .notes(order.getNotes())
                .build();
    }
}