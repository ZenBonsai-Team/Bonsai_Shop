package com.example.bonsai_shop.owner.controller;

import com.example.bonsai_shop.exception.OrderNotFoundException;
import com.example.bonsai_shop.moderator.dto.OrderDetailDTO;
import com.example.bonsai_shop.moderator.service.OrderDetailService;
import com.example.bonsai_shop.owner.service.OrderHandlingHistoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Locale;

@Controller
@RequestMapping("/owner/order-handling-history")
@PreAuthorize("hasRole('OWNER')")
@RequiredArgsConstructor
public class OwnerOrderHandlingHistoryController {

    private final OrderHandlingHistoryService orderHandlingHistoryService;
    private final OrderDetailService orderDetailService;

    @GetMapping
    public String history(@RequestParam(required = false) String search,
                          @RequestParam(defaultValue = "ALL") String status,
                          @RequestParam(defaultValue = "0") int page,
                          @RequestParam(required = false) Integer size,
                          Model model) {
        int pageSize = size != null ? size : orderHandlingHistoryService.defaultPageSize();
        var historyPage = orderHandlingHistoryService.findModeratorHandlingHistory(search, status, page, pageSize);

        model.addAttribute("historyPage", historyPage);
        model.addAttribute("historyItems", historyPage.getContent());
        model.addAttribute("search", search == null ? "" : search.trim());
        model.addAttribute("selectedStatus", status == null || status.isBlank() ? "ALL" : status.trim().toUpperCase(Locale.ROOT));
        model.addAttribute("completedStatus", "COMPLETED");
        model.addAttribute("cancelledStatus", "CANCELLED");
        model.addAttribute("role", "OWNER");
        model.addAttribute("activeMenu", "order-handling-history");
        model.addAttribute("activePage", "order-handling-history");
        return "owner/order_handling_history";
    }

    @GetMapping("/orders/{orderCode}")
    public String orderDetail(@PathVariable String orderCode, Model model) {
        try {
            OrderDetailDTO detail = orderDetailService.getOrderDetailByCode(orderCode, null);
            if (!isClosedOrder(detail)) {
                model.addAttribute("errorMessage", "Chỉ có thể xem chi tiết đơn đã hoàn thành hoặc đã hủy từ màn hình này.");
                model.addAttribute("orderCode", orderCode);
                return "moderator/order_not_found";
            }
            model.addAttribute("order", detail);
            model.addAttribute("role", "OWNER");
            model.addAttribute("activePage", "order-handling-history");
            model.addAttribute("activePageLabel", "Chi tiết đơn - " + orderCode);
            model.addAttribute("detailBackUrl", "/owner/order-handling-history");
            model.addAttribute("detailBackLabel", "Quay lại lịch sử");
            model.addAttribute("detailBreadcrumbLabel", "Lịch sử xử lý đơn");
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

    private boolean isClosedOrder(OrderDetailDTO detail) {
        if (detail == null || detail.getOrderStatus() == null) {
            return false;
        }
        return "COMPLETED".equalsIgnoreCase(detail.getOrderStatus())
                || "CANCELLED".equalsIgnoreCase(detail.getOrderStatus());
    }
}
