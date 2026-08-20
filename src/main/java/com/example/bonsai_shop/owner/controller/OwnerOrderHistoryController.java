package com.example.bonsai_shop.owner.controller;

import com.example.bonsai_shop.exception.OrderNotFoundException;
import com.example.bonsai_shop.moderator.dto.OrderDetailDTO;
import com.example.bonsai_shop.moderator.service.OrderDetailService;
import com.example.bonsai_shop.owner.service.OwnerOrderHistoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/owner/order-history")
@PreAuthorize("hasRole('OWNER')")
@RequiredArgsConstructor
public class OwnerOrderHistoryController {

    private final OwnerOrderHistoryService ownerOrderHistoryService;
    private final OrderDetailService orderDetailService;

    @GetMapping
    public String history(@RequestParam(required = false) String search,
                          @RequestParam(defaultValue = "ALL") String type,
                          @RequestParam(defaultValue = "ALL") String status,
                          @RequestParam(defaultValue = "0") int page,
                          @RequestParam(required = false) Integer size,
                          Model model) {
        int pageSize = size != null ? size : ownerOrderHistoryService.defaultPageSize();
        String selectedType = ownerOrderHistoryService.normalizeType(type);
        var historyPage = ownerOrderHistoryService.findOwnerOrderHistory(search, selectedType, status, page, pageSize);

        model.addAttribute("historyPage", historyPage);
        model.addAttribute("historyItems", historyPage.getContent());
        model.addAttribute("search", search == null ? "" : search.trim());
        model.addAttribute("selectedType", selectedType);
        model.addAttribute("selectedStatus", status == null || status.isBlank() ? "ALL" : status.trim().toUpperCase());
        model.addAttribute("allType", OwnerOrderHistoryService.TYPE_ALL);
        model.addAttribute("onlineType", OwnerOrderHistoryService.TYPE_ONLINE);
        model.addAttribute("inPersonType", OwnerOrderHistoryService.TYPE_IN_PERSON);
        model.addAttribute("pendingStatus", "PENDING");
        model.addAttribute("pendingPaymentStatus", "PENDING_PAYMENT");
        model.addAttribute("depositedStatus", "DEPOSITED");
        model.addAttribute("paidStatus", "PAID");
        model.addAttribute("completedStatus", "COMPLETED");
        model.addAttribute("cancelledStatus", "CANCELLED");
        model.addAttribute("role", "OWNER");
        model.addAttribute("activeMenu", "order-history");
        model.addAttribute("activePage", "order-history");
        return "owner/order_history";
    }

    @GetMapping("/orders/{orderCode}")
    public String orderDetail(@PathVariable String orderCode, Model model) {
        try {
            OrderDetailDTO detail = orderDetailService.getOrderDetailByCode(orderCode, null);
            makeReadOnly(detail);
            model.addAttribute("order", detail);
            model.addAttribute("role", "OWNER");
            model.addAttribute("activePage", "order-history");
            model.addAttribute("activePageLabel", "Chi tiết đơn - " + orderCode);
            model.addAttribute("detailBackUrl", "/owner/order-history");
            model.addAttribute("detailBackLabel", "Quay lại lịch sử đơn hàng");
            model.addAttribute("detailBreadcrumbLabel", "Lịch sử đơn hàng");
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
