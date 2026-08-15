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

@Controller
@RequestMapping("/moderator/orders")
@RequiredArgsConstructor
public class ModeratorOrderController {

    private final MyOrderService myOrderService;
    private final OrderDetailService orderDetailService;
    private final OrderActionService orderActionService;
    private final UserRepository userRepository;

    // ===== REDIRECT TO POOL =====
    @GetMapping
    public String viewOrdersDashboardRedirect() {
        return "redirect:/moderator/orders/pool";
    }

    // ===== ORDERS POOL PAGE =====
    @GetMapping("/pool")
    public String viewOrdersPool(Model model) {
        model.addAttribute("role", "MODERATOR");
        model.addAttribute("activePage", "orders-pool");
        model.addAttribute("activePageLabel", "Orders Pool - Kho Đơn Hàng Chung");
        return "moderator/orders_pool";
    }

    // ===== MY ORDERS PAGE =====
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
        model.addAttribute("activePageLabel", "\u0110\u01a1n h\u00e0ng c\u1ee7a t\u00f4i");
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

    // ===== MY ORDERS JSON API =====
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

    // ===== ORDER DETAIL PAGE (Dedicated Page — No Drawer) =====
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

    // ===== ORDER DETAIL JSON API =====
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

    // ===== ORDER ACTION ENDPOINT (claim / approve / reject / complete / cancel / unclaim) =====
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
