package com.example.bonsai_shop.artisan.controller;

import com.example.bonsai_shop.artisan.service.ArtisanWalkInOrderService;
import com.example.bonsai_shop.entity.Order;
import com.example.bonsai_shop.entity.Product;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.util.List;

@Controller
@RequestMapping("/artisan/in-person-order")
@RequiredArgsConstructor
public class ArtisanWalkInOrderController {

    private final ArtisanWalkInOrderService walkInOrderService;

    @GetMapping
    public String index(@AuthenticationPrincipal UserDetails userDetails,
                        @RequestParam(defaultValue = "ALL") String status,
                        @RequestParam(defaultValue = "0") int page,
                        Model model) {
        List<Product> availableProducts = walkInOrderService.getAvailableProducts(userDetails.getUsername());
        Page<Order> orders = walkInOrderService.getWalkInOrders(userDetails.getUsername(), status, page, 10);

        model.addAttribute("availableProducts", availableProducts);
        model.addAttribute("orders", orders);
        model.addAttribute("selectedStatus", status);
        model.addAttribute("pendingPaymentStatus", ArtisanWalkInOrderService.STATUS_PENDING_PAYMENT);
        model.addAttribute("completedStatus", ArtisanWalkInOrderService.STATUS_COMPLETED);
        model.addAttribute("cancelledStatus", ArtisanWalkInOrderService.STATUS_CANCELLED);
        return "artisan/in-person-order";
    }

    @PostMapping
    public String create(@AuthenticationPrincipal UserDetails userDetails,
                         @RequestParam Integer productId,
                         @RequestParam String customerName,
                         @RequestParam String customerPhone,
                         @RequestParam String shippingAddress,
                         @RequestParam(defaultValue = ArtisanWalkInOrderService.PAYMENT_METHOD_CASH) String paymentMethod,
                         @RequestParam(defaultValue = "0") BigDecimal craneFee,
                         @RequestParam(defaultValue = "0") BigDecimal shippingFee,
                         @RequestParam(required = false) String customerEmail,
                         @RequestParam(required = false) String notes,
                         RedirectAttributes redirectAttributes) {
        try {
            Order order = walkInOrderService.createWalkInOrder(
                    userDetails.getUsername(),
                    productId,
                    customerName,
                    customerPhone,
                    shippingAddress,
                    paymentMethod,
                    craneFee,
                    shippingFee,
                    customerEmail,
                    notes
            );
            redirectAttributes.addFlashAttribute("success", "Đã tạo In-person Order " + order.getOrderCode() + " và reserve sản phẩm.");
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/artisan/in-person-order";
    }

    @PostMapping("/{orderId}/confirm-payment")
    public String confirmPayment(@AuthenticationPrincipal UserDetails userDetails,
                                 @PathVariable Integer orderId,
                                 RedirectAttributes redirectAttributes) {
        try {
            Order order = walkInOrderService.confirmPayment(userDetails.getUsername(), orderId);
            redirectAttributes.addFlashAttribute("success", "Đã xác nhận nhận tiền, hoàn tất order " + order.getOrderCode() + " và chuyển sản phẩm sang SOLD.");
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/artisan/in-person-order";
    }

    @PostMapping("/{orderId}/cancel")
    public String cancel(@AuthenticationPrincipal UserDetails userDetails,
                         @PathVariable Integer orderId,
                         @RequestParam(required = false) String reason,
                         RedirectAttributes redirectAttributes) {
        try {
            Order order = walkInOrderService.cancelWalkInOrder(userDetails.getUsername(), orderId, reason);
            redirectAttributes.addFlashAttribute("success", "Đã hủy In-person Order " + order.getOrderCode() + " và mở bán lại sản phẩm.");
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/artisan/in-person-order";
    }

    @PostMapping("/{orderId}/update")
    public String update(@AuthenticationPrincipal UserDetails userDetails,
                         @PathVariable Integer orderId,
                         @RequestParam String customerName,
                         @RequestParam String customerPhone,
                         @RequestParam String shippingAddress,
                         @RequestParam(defaultValue = ArtisanWalkInOrderService.PAYMENT_METHOD_CASH) String paymentMethod,
                         @RequestParam(defaultValue = "0") BigDecimal craneFee,
                         @RequestParam(defaultValue = "0") BigDecimal shippingFee,
                         @RequestParam(required = false) String customerEmail,
                         @RequestParam(required = false) String notes,
                         RedirectAttributes redirectAttributes) {
        try {
            Order order = walkInOrderService.updateWalkInOrder(
                    userDetails.getUsername(),
                    orderId,
                    customerName,
                    customerPhone,
                    shippingAddress,
                    paymentMethod,
                    craneFee,
                    shippingFee,
                    customerEmail,
                    notes
            );
            redirectAttributes.addFlashAttribute("success", "Đã cập nhật In-person Order " + order.getOrderCode() + ".");
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/artisan/in-person-order";
    }
}

