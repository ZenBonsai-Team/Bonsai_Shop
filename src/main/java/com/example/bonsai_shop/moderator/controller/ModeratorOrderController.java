package com.example.bonsai_shop.moderator.controller;

import com.example.bonsai_shop.config.SecurityUtils;
import com.example.bonsai_shop.customer.repository.UserRepository;
import com.example.bonsai_shop.entity.User;
import com.example.bonsai_shop.exception.OrderNotFoundException;
import com.example.bonsai_shop.moderator.dto.MyOrderDTO;
import com.example.bonsai_shop.moderator.dto.MyOrderKPIsDTO;
import com.example.bonsai_shop.moderator.dto.OrderActionRequestDTO;
import com.example.bonsai_shop.moderator.dto.OrderDetailDTO;
import com.example.bonsai_shop.moderator.service.MyOrderService;
import com.example.bonsai_shop.moderator.service.OrderActionService;
import com.example.bonsai_shop.moderator.service.OrderDetailService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * [VAI TRÒ TRONG LUỒNG XỬ LÝ ĐƠN HÀNG CỦA ORDER MODERATOR]
 *
 * Chịu trách nhiệm:
 * - Điều hướng và hiển thị giao diện Web (Thymeleaf UI) cho Order Moderator:
 *   + Kho đơn hàng chung (Orders Pool)
 *   + Danh sách đơn hàng cá nhân phụ trách (My Orders)
 *   + Màn hình chi tiết đơn hàng độc lập (Order Detail)
 * - Cung cấp REST/JSON API phục vụ frontend tương tác động (AJAX):
 *   + Lấy dữ liệu phân trang và KPI đơn hàng của tôi (/api/my-orders)
 *   + Lấy dữ liệu chi tiết đơn hàng (/api/detail/{orderCode})
 *   + Thực hiện các hành động xử lý đơn hàng tập trung (/api/action/{orderCode}): claim, approve, reject, return_inventory/unclaim, complete, customer_no_show, record_fault_refund.
 *
 * Các thao tác trên web đi qua class này:
 * - [Truy cập Dashboard Đơn hàng] → GET /moderator/orders → viewOrdersDashboardRedirect()
 * - [Mở trang Kho đơn hàng chung] → GET /moderator/orders/pool → viewOrdersPool()
 * - [Mở trang Đơn hàng của tôi] → GET /moderator/orders/my → viewMyOrders()
 * - [Frontend tải danh sách My Orders qua AJAX] → GET /moderator/orders/api/my-orders → getMyOrdersData()
 * - [Mở trang Chi tiết đơn hàng] → GET /moderator/orders/{orderCode} → viewOrderDetail()
 * - [Frontend tải JSON Chi tiết đơn qua AJAX] → GET /moderator/orders/api/detail/{orderCode} → getOrderDetailJson()
 * - [Moderator bấm nút hành động trên UI Chi tiết đơn] → POST /moderator/orders/api/action/{orderCode} → executeOrderAction()
 *
 * Các thành phần phối hợp chính:
 * - MyOrderService, OrderDetailService, OrderActionService, UserRepository.
 * - MyOrderDTO, MyOrderKPIsDTO, OrderDetailDTO, OrderActionRequestDTO.
 */
@Controller
@RequestMapping("/moderator/orders")
@RequiredArgsConstructor
public class ModeratorOrderController {

    private final MyOrderService myOrderService;
    private final OrderDetailService orderDetailService;
    private final OrderActionService orderActionService;
    private final UserRepository userRepository;

    /**
     * [ĐIỀU HƯỚNG ĐẾN KHO ĐƠN HÀNG CHUNG]
     *
     * Khi nào được gọi trên web:
     * - Người dùng bấm vào menu "Quản lý đơn hàng" trên thanh điều hướng Moderator.
     *
     * API:
     * - HTTP: GET
     * - URL: /moderator/orders
     * - Người gọi: Order Moderator
     *
     * Dữ liệu trả về:
     * - Redirect: /moderator/orders/pool
     */
    @GetMapping
    public String viewOrdersDashboardRedirect() {
        return "redirect:/moderator/orders/pool";
    }

    /**
     * [HIỂN THỊ GIAO DIỆN KHO ĐƠN HÀNG CHUNG (ORDERS POOL)]
     *
     * Khi nào được gọi trên web:
     * - Moderator mở trang "Kho Đơn Hàng Chung" để xem và nhận các đơn PENDING mới tạo.
     * - Màn hình: moderator/orders_pool.html.
     *
     * API:
     * - HTTP: GET
     * - URL: /moderator/orders/pool
     * - Người gọi: Order Moderator
     *
     * Dữ liệu trả về:
     * - Thymeleaf View: "moderator/orders_pool"
     */
    @GetMapping("/pool")
    public String viewOrdersPool(Model model) {
        model.addAttribute("role", "MODERATOR");
        model.addAttribute("activePage", "orders-pool");
        model.addAttribute("activePageLabel", "Orders Pool - Kho Đơn Hàng Chung");
        return "moderator/orders_pool";
    }

    /**
     * [HIỂN THỊ GIAO DIỆN ĐƠN HÀNG CỦA TÔI (MY ORDERS)]
     *
     * Khi nào được gọi trên web:
     * - Moderator mở trang "Đơn hàng của tôi" để xem các đơn mình đang xử lý theo các tab trạng thái/mức độ ưu tiên.
     * - Màn hình: moderator/my_orders.html.
     *
     * API:
     * - HTTP: GET
     * - URL: /moderator/orders/my
     * - Người gọi: Order Moderator
     *
     * Dữ liệu nhận vào:
     * - Request params: search, cardFilter (CRITICAL, WAITING_APPROVAL, WAITING_CUSTOMER_PAYMENT, WAITING_DELIVERY_PAYMENT, COMPLETED, CANCELLED), priority, status, sort, page, limit.
     * - Authentication: Principal của Moderator.
     *
     * Điều phối xử lý:
     * 1. Lấy thông tin Moderator từ SecurityUtils.getCurrentUser().
     * 2. Gọi MyOrderService.getMyOrderKPIs(moderatorId) để tính số lượng thẻ KPI.
     * 3. Gọi MyOrderService.getMyOrdersFiltered(...) để lấy danh sách đơn đã phân trang và lọc theo tiêu chí.
     * 4. Gắn các model attribute phục vụ render Thymeleaf template.
     *
     * Dữ liệu trả về:
     * - Thymeleaf View: "moderator/my_orders"
     */
    @GetMapping("/my")
    public String viewMyOrders(
            @RequestParam(defaultValue = "") String search,
            @RequestParam(defaultValue = "ALL") String cardFilter,
            @RequestParam(defaultValue = "ALL") String priority,
            @RequestParam(defaultValue = "ALL") String status,
            @RequestParam(defaultValue = "date_desc") String sort,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "8") int limit,
            @AuthenticationPrincipal Object principal,
            Model model) {

        User moderator = SecurityUtils.getCurrentUser(principal, userRepository);
        Integer moderatorId = moderator != null ? moderator.getUserId() : null;

        MyOrderKPIsDTO kpis = myOrderService.getMyOrderKPIs(moderatorId);
        Page<MyOrderDTO> orderPage = myOrderService.getMyOrdersFiltered(
                moderatorId, cardFilter, search, priority, status, sort, page, limit);

        model.addAttribute("role", "MODERATOR");
        model.addAttribute("activePage", "my-orders");
        model.addAttribute("activePageLabel", "Đơn hàng của tôi");
        model.addAttribute("kpis", kpis);
        model.addAttribute("orders", orderPage.getContent());
        model.addAttribute("totalCount", orderPage.getTotalElements());
        model.addAttribute("totalPages", orderPage.getTotalPages() > 0 ? orderPage.getTotalPages() : 1);
        model.addAttribute("currentPage", page);
        model.addAttribute("search", search);
        model.addAttribute("cardFilter", cardFilter);
        model.addAttribute("priority", priority);
        model.addAttribute("status", status);
        model.addAttribute("sort", sort);

        return "moderator/my_orders";
    }

    /**
     * [API LẤY DỮ LIỆU MY ORDERS VÀ KPIS (JSON)]
     *
     * Khi nào được gọi trên web:
     * - Frontend JavaScript gọi ngầm khi người dùng lọc/chuyển trang/tìm kiếm trên màn hình My Orders mà không tải lại toàn bộ trang.
     *
     * API:
     * - HTTP: GET
     * - URL: /moderator/orders/api/my-orders
     * - Người gọi: Frontend AJAX
     *
     * Dữ liệu trả về:
     * - Response JSON: Map chứa kpis (MyOrderKPIsDTO), orders (List<MyOrderDTO>), totalCount, totalPages, currentPage, pageSize.
     */
    @GetMapping("/api/my-orders")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getMyOrdersData(
            @RequestParam(defaultValue = "") String search,
            @RequestParam(defaultValue = "ALL") String cardFilter,
            @RequestParam(defaultValue = "ALL") String priority,
            @RequestParam(defaultValue = "ALL") String status,
            @RequestParam(defaultValue = "date_desc") String sort,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "8") int limit,
            @AuthenticationPrincipal Object principal) {

        User moderator = SecurityUtils.getCurrentUser(principal, userRepository);
        Integer moderatorId = moderator != null ? moderator.getUserId() : null;

        MyOrderKPIsDTO kpis = myOrderService.getMyOrderKPIs(moderatorId);
        Page<MyOrderDTO> orderPage = myOrderService.getMyOrdersFiltered(
                moderatorId, cardFilter, search, priority, status, sort, page, limit);

        Map<String, Object> response = new HashMap<>();
        response.put("kpis", kpis);
        response.put("orders", orderPage.getContent());
        response.put("totalCount", orderPage.getTotalElements());
        response.put("totalPages", orderPage.getTotalPages() > 0 ? orderPage.getTotalPages() : 1);
        response.put("currentPage", page);
        response.put("pageSize", limit);

        return ResponseEntity.ok(response);
    }

    /**
     * [HIỂN THỊ GIAO DIỆN CHI TIẾT ĐƠN HÀNG (DEDICATED PAGE)]
     *
     * Khi nào được gọi trên web:
     * - Moderator bấm vào một đơn hàng để mở trang chi tiết độc lập (/moderator/orders/{orderCode}).
     * - Màn hình: moderator/order_detail.html.
     *
     * API:
     * - HTTP: GET
     * - URL: /moderator/orders/{orderCode}
     * - Người gọi: Order Moderator
     *
     * Dữ liệu nhận vào:
     * - Path variable: orderCode (Mã đơn hàng)
     * - Authentication: Principal của Moderator
     *
     * Điều phối xử lý:
     * 1. Gọi OrderDetailService.getOrderDetailByCode(orderCode, moderator).
     * 2. Trả về đối tượng OrderDetailDTO chứa thông tin khách hàng, sản phẩm, tóm tắt thanh toán, lịch sử thanh toán, timeline, lịch sử xử lý, và các quyền hành động (canApprove, canReject, canClaim, canComplete...).
     * 3. Render view Thymeleaf "moderator/order_detail".
     *
     * Dữ liệu trả về:
     * - Thymeleaf View: "moderator/order_detail" (hoặc "moderator/order_not_found" nếu lỗi).
     */
    @GetMapping("/{orderCode}")
    public String viewOrderDetail(
            @PathVariable String orderCode,
            @AuthenticationPrincipal Object principal,
            Model model) {

        User moderator = SecurityUtils.getCurrentUser(principal, userRepository);

        try {
            OrderDetailDTO detail = orderDetailService.getOrderDetailByCode(orderCode, moderator);
            model.addAttribute("order", detail);
            model.addAttribute("activePage", "my-orders");
            model.addAttribute("activePageLabel", "Chi tiết đơn - " + orderCode);
            return "moderator/order_detail";
        } catch (OrderNotFoundException e) {
            model.addAttribute("errorMessage", e.getMessage());
            model.addAttribute("orderCode", orderCode);
            return "moderator/order_not_found";
        } catch (Exception e) {
            model.addAttribute("errorMessage", "Đã xảy ra lỗi khi tải đơn hàng: " + e.getMessage());
            model.addAttribute("orderCode", orderCode);
            return "moderator/order_not_found";
        }
    }

    /**
     * [API LẤY CHI TIẾT ĐƠN HÀNG (JSON)]
     *
     * Khi nào được gọi trên web:
     * - Frontend gọi qua AJAX để lấy toàn bộ dữ liệu OrderDetailDTO hiển thị lên Drawer / Modal.
     *
     * API:
     * - HTTP: GET
     * - URL: /moderator/orders/api/detail/{orderCode}
     * - Người gọi: Frontend AJAX
     *
     * Dữ liệu trả về:
     * - Response JSON: OrderDetailDTO
     */
    @GetMapping("/api/detail/{orderCode}")
    @ResponseBody
    public ResponseEntity<?> getOrderDetailJson(
            @PathVariable String orderCode,
            @AuthenticationPrincipal Object principal) {

        User moderator = SecurityUtils.getCurrentUser(principal, userRepository);

        try {
            OrderDetailDTO detail = orderDetailService.getOrderDetailByCode(orderCode, moderator);
            return ResponseEntity.ok(detail);
        } catch (OrderNotFoundException e) {
            Map<String, String> err = new HashMap<>();
            err.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(err);
        } catch (Exception e) {
            Map<String, String> err = new HashMap<>();
            err.put("error", "Lỗi máy chủ: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(err);
        }
    }

    /**
     * [API ĐIỀU PHỐI HÀNH ĐỘNG XỬ LÝ ĐƠN HÀNG CỦA MODERATOR]
     *
     * Khi nào được gọi trên web:
     * - Người dùng thực hiện: Moderator bấm các nút hành động trên trang chi tiết đơn hàng:
     *   + "Tiếp nhận đơn" (action = "claim")
     *   + "Duyệt đơn" (action = "approve") kèm craneFee, shippingFee, depositAmount
     *   + "Từ chối duyệt" (action = "reject") kèm reason
     *   + "Trả về kho chung" (action = "return_inventory" / "unclaim")
     *   + "Hoàn tất đơn / Thu đủ tiền" (action = "complete")
     *   + "Khách không nhận" (action = "customer_no_show") kèm reason
     *   + "Ghi nhận hoàn tiền do lỗi" (action = "record_fault_refund")
     *
     * API:
     * - HTTP: POST
     * - URL: /moderator/orders/api/action/{orderCode}
     * - Người gọi: Order Moderator
     *
     * Dữ liệu nhận vào:
     * - Path variable: orderCode (Mã đơn hàng)
     * - Request body: OrderActionRequestDTO request
     *   + action (String): Tên hành động
     *   + reason (String): Lý do (nếu từ chối/hủy/hoàn tiền)
     *   + craneFee, shippingFee, depositAmount (BigDecimal): Các khoản phí khi duyệt
     *   + faultParty, refundAmount, evidenceNote, externalReference, productResolution: Khi hoàn tiền do lỗi
     * - Authentication: Principal của Moderator
     *
     * Điều phối xử lý:
     * 1. Lấy thông tin Moderator từ SecurityUtils.getCurrentUser().
     * 2. Gọi OrderActionService.executeAction(orderCode, request, moderator).
     * 3. OrderActionService phân phối đến handler tương ứng (handleClaim, handleApprove, handleReject, handleReturnInventory, handleComplete, handleCustomerNoShow, handleFaultRefund).
     * 4. Handler gọi các method nghiệp vụ tương ứng trong OrderService (verifyOrder, rejectOrder, confirmRemainingPayment, completePaidOrder, markDepositedOrderCustomerNoShow, recordFaultRefundAndCancel).
     *
     * Dữ liệu trả về:
     * - HTTP status: 200 OK (Thành công), 400 Bad Request, 404 Not Found, 500 Internal Server Error.
     * - Response JSON: Map {"success": true, "orderCode": "...", "action": "...", "newStatus": "..."}
     */
    @PostMapping("/api/action/{orderCode}")
    @ResponseBody
    public ResponseEntity<?> executeOrderAction(
            @PathVariable String orderCode,
            @RequestBody OrderActionRequestDTO request,
            @AuthenticationPrincipal Object principal) {

        User moderator = SecurityUtils.getCurrentUser(principal, userRepository);

        try {
            Map<String, Object> result = orderActionService.executeAction(
                    orderCode,
                    request,
                    moderator
            );
            return ResponseEntity.ok(result);
        } catch (OrderNotFoundException e) {
            Map<String, String> err = new HashMap<>();
            err.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(err);
        } catch (IllegalStateException | IllegalArgumentException e) {
            Map<String, String> err = new HashMap<>();
            err.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(err);
        } catch (org.springframework.dao.DataAccessException | jakarta.persistence.PersistenceException e) {
            Map<String, String> err = new HashMap<>();
            err.put("error", "Lỗi cơ sở dữ liệu: Yêu cầu không hợp lệ hoặc dữ liệu vượt quá giới hạn hệ thống.");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(err);
        } catch (Exception e) {
            Map<String, String> err = new HashMap<>();
            err.put("error", "Lỗi hệ thống: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(err);
        }
    }
}
