package com.example.bonsai_shop.owner.controller;

import com.example.bonsai_shop.exception.OrderNotFoundException;
import com.example.bonsai_shop.moderator.dto.OrderDetailDTO;
import com.example.bonsai_shop.moderator.service.OrderDetailService;
import com.example.bonsai_shop.owner.service.InPersonOrderHistoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/owner/in-person-order-history")
@PreAuthorize("hasRole('OWNER')")
@RequiredArgsConstructor
public class OwnerInPersonOrderHistoryController {

    private final InPersonOrderHistoryService inPersonOrderHistoryService;
    private final OrderDetailService orderDetailService;

    // Hien thi lich su don tai vuon cua cac artisan cho Owner theo doi.
    @GetMapping
    public String history(@RequestParam(required = false) String search,
                          @RequestParam(defaultValue = "ALL") String status,
                          @RequestParam(defaultValue = "0") int page,
                          @RequestParam(required = false) Integer size,
                          Model model) {
        // Neu request khong truyen size thi dung page size mac dinh cua service.
        int pageSize = size != null ? size : inPersonOrderHistoryService.defaultPageSize();
        // Tim don tai vuon theo search/status va phan trang da validate o service.
        var ordersPage = inPersonOrderHistoryService.findOwnerInPersonOrders(search, status, page, pageSize);

        // Dua du lieu, filter va cac status tab ve view.
        model.addAttribute("orders", ordersPage);
        model.addAttribute("search", search == null ? "" : search.trim());
        model.addAttribute("selectedStatus", status == null || status.isBlank() ? "ALL" : status.trim().toUpperCase());
        model.addAttribute("pendingPaymentStatus", "PENDING_PAYMENT");
        model.addAttribute("completedStatus", "COMPLETED");
        model.addAttribute("cancelledStatus", "CANCELLED");
        model.addAttribute("role", "OWNER");
        model.addAttribute("activeMenu", "in-person-order-history");
        model.addAttribute("activePage", "in-person-order-history");
        return "owner/in_person_order_history";
    }

    // Xem chi tiet don tai vuon o che do chi doc danh cho Owner.
    @GetMapping("/orders/{orderCode}")
    public String orderDetail(@PathVariable String orderCode, Model model) {
        try {
            // Validate don phai la don tai vuon cua artisan moi duoc hien thi trong luong Owner nay.
            if (!inPersonOrderHistoryService.isOwnerVisibleInPersonOrder(orderCode)) {
                model.addAttribute("errorMessage", "Không tìm thấy đơn tại vườn thuộc tài khoản artisan: " + orderCode);
                model.addAttribute("orderCode", orderCode);
                return "moderator/order_not_found";
            }

            OrderDetailDTO detail = orderDetailService.getOrderDetailByCode(orderCode, null);
            // Tat tat ca action de Owner chi xem lich su, khong thao tac xu ly don.
            makeReadOnly(detail);
            model.addAttribute("order", detail);
            model.addAttribute("role", "OWNER");
            model.addAttribute("activePage", "in-person-order-history");
            model.addAttribute("activePageLabel", "Chi tiết đơn tại vườn - " + orderCode);
            model.addAttribute("detailBackUrl", "/owner/in-person-order-history");
            model.addAttribute("detailBackLabel", "Quay lại lịch sử đơn tại vườn");
            model.addAttribute("detailBreadcrumbLabel", "Lịch sử đơn tại vườn");
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

    // Tat cac co quyen hanh dong tren DTO chi tiet don de template render che do read-only.
    private void makeReadOnly(OrderDetailDTO detail) {
        detail.setCanApprove(false);
        detail.setCanReject(false);
        detail.setCanClaim(false);
        detail.setCanUnclaim(false);
        detail.setCanReturnInventory(false);
        detail.setCanComplete(false);
        detail.setCanCancel(false);
        detail.setCanCustomerNoShow(false);
        detail.setCanRecordFaultRefund(false);
    }
}
