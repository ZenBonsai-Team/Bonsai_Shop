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

    // Hien thi lich su cac don da duoc moderator xu ly de Owner tra cuu.
    @GetMapping
    public String history(@RequestParam(required = false) String search,
                          @RequestParam(defaultValue = "ALL") String status,
                          @RequestParam(defaultValue = "0") int page,
                          @RequestParam(required = false) Integer size,
                          Model model) {
        // Neu request khong truyen size thi dung page size mac dinh cua service.
        int pageSize = size != null ? size : orderHandlingHistoryService.defaultPageSize();
        // Tim lich su theo search/status va phan trang da validate o service.
        var historyPage = orderHandlingHistoryService.findModeratorHandlingHistory(search, status, page, pageSize);

        // Dua du lieu va trang thai filter ve view de render bang va pagination.
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

    // Xem chi tiet mot don trong lich su xu ly; chi cho xem don da hoan thanh hoac da huy.
    @GetMapping("/orders/{orderCode}")
    public String orderDetail(@PathVariable String orderCode, Model model) {
        try {
            OrderDetailDTO detail = orderDetailService.getOrderDetailByCode(orderCode, null);
            // Validate don phai o trang thai dong de tranh Owner mo man hinh lich su cho don dang xu ly.
            if (!isClosedOrder(detail)) {
                model.addAttribute("errorMessage", "Chỉ có thể xem chi tiết đơn đã hoàn thành hoặc đã hủy từ màn hình này.");
                model.addAttribute("orderCode", orderCode);
                return "moderator/order_not_found";
            }
            // Tai su dung template chi tiet don cua moderator nhung cau hinh breadcrumb/back link cho Owner.
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

    // Kiem tra don da ket thuc chua, gom COMPLETED va CANCELLED.
    private boolean isClosedOrder(OrderDetailDTO detail) {
        if (detail == null || detail.getOrderStatus() == null) {
            return false;
        }
        return "COMPLETED".equalsIgnoreCase(detail.getOrderStatus())
                || "CANCELLED".equalsIgnoreCase(detail.getOrderStatus());
    }
}
