package com.example.bonsai_shop.artisan.controller;

import com.example.bonsai_shop.artisan.service.ArtisanInPersonOrderService;
import com.example.bonsai_shop.entity.Order;
import com.example.bonsai_shop.entity.Product;
import com.example.bonsai_shop.product.service.OrderExpirationService;
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
// Controller xử lý đơn bán trực tiếp tại vườn của artisan.
public class ArtisanInPersonOrderController {

    private final ArtisanInPersonOrderService inPersonOrderService;
    private final OrderExpirationService orderExpirationService;

    @GetMapping({"", "/"})
    // Hiển thị danh sách đơn tại vườn và sản phẩm còn có thể bán.
    public String index(@AuthenticationPrincipal UserDetails userDetails,
                        @RequestParam(defaultValue = "ALL") String status,
                        @RequestParam(defaultValue = "") String keyword,
                        @RequestParam(defaultValue = "0") int page,
                        @RequestParam(defaultValue = "10") int size,
                        Model model) {
        orderExpirationService.cancelExpiredInPersonOrders();

        List<Product> availableProducts = inPersonOrderService.getAvailableProducts(userDetails.getUsername());
        int pageSize = Math.min(Math.max(size, 1), 50);
        String normalizedKeyword = keyword == null ? "" : keyword.trim();
        Page<Order> orders = inPersonOrderService.getInPersonOrders(userDetails.getUsername(), status, normalizedKeyword, page, pageSize);

        model.addAttribute("availableProducts", availableProducts);
        model.addAttribute("orders", orders);
        model.addAttribute("selectedStatus", status);
        model.addAttribute("selectedKeyword", normalizedKeyword);
        model.addAttribute("selectedSize", pageSize);
        model.addAttribute("pendingPaymentStatus", ArtisanInPersonOrderService.STATUS_PENDING_PAYMENT);
        model.addAttribute("completedStatus", ArtisanInPersonOrderService.STATUS_COMPLETED);
        model.addAttribute("cancelledStatus", ArtisanInPersonOrderService.STATUS_CANCELLED);
        return "artisan/in-person-order";
    }

    @PostMapping
    // Tạo đơn tại vườn từ dữ liệu khách hàng và phí phát sinh.
    public String create(@AuthenticationPrincipal UserDetails userDetails,
                         @RequestParam Integer productId,
                         @RequestParam String customerName,
                         @RequestParam String customerPhone,
                         @RequestParam String shippingAddress,
                         @RequestParam(defaultValue = ArtisanInPersonOrderService.PAYMENT_METHOD_CASH) String paymentMethod,
                         @RequestParam(defaultValue = "0") BigDecimal craneFee,
                         @RequestParam(defaultValue = "0") BigDecimal shippingFee,
                         @RequestParam String customerEmail,
                         @RequestParam(required = false) String notes,
                         RedirectAttributes redirectAttributes) {
        try {
            Order order = inPersonOrderService.createInPersonOrder(
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
            redirectAttributes.addFlashAttribute("createErrorField", resolveOrderFormErrorField(e.getMessage()));
            keepCreateForm(redirectAttributes, productId, customerName, customerPhone, shippingAddress,
                    paymentMethod, craneFee, shippingFee, customerEmail, notes);
            return "redirect:/artisan/in-person-order/";
        }
        return "redirect:/artisan/in-person-order#walkInOrdersSection";
    }

    @PostMapping("/{orderId}/confirm-payment")
    // Xác nhận thanh toán và hoàn tất đơn tại vườn.
    public String confirmPayment(@AuthenticationPrincipal UserDetails userDetails,
                                 @PathVariable Integer orderId,
                                 RedirectAttributes redirectAttributes) {
        try {
            Order order = inPersonOrderService.confirmPayment(userDetails.getUsername(), orderId);
            redirectAttributes.addFlashAttribute("success", "Đã xác nhận nhận tiền, order " + order.getOrderCode() + " đã hoàn thành và sản phẩm sang SOLD.");
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/artisan/in-person-order#walkInOrdersSection";
    }

    @PostMapping("/{orderId}/cancel")
    // Hủy đơn tại vườn với lý do mặc định từ hệ thống.
    public String cancel(@AuthenticationPrincipal UserDetails userDetails,
                         @PathVariable Integer orderId,
                         RedirectAttributes redirectAttributes) {
        try {
            Order order = inPersonOrderService.cancelInPersonOrder(userDetails.getUsername(), orderId);
            redirectAttributes.addFlashAttribute("success", "Đã hủy In-person Order " + order.getOrderCode() + " và mở bán lại sản phẩm.");
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/artisan/in-person-order#walkInOrdersSection";
    }

    @PostMapping("/{orderId}/update")
    // Cập nhật thông tin khách hàng, phí và phương thức thanh toán của đơn.
    public String update(@AuthenticationPrincipal UserDetails userDetails,
                         @PathVariable Integer orderId,
                         @RequestParam String customerName,
                         @RequestParam String customerPhone,
                         @RequestParam String shippingAddress,
                         @RequestParam(defaultValue = ArtisanInPersonOrderService.PAYMENT_METHOD_CASH) String paymentMethod,
                         @RequestParam(defaultValue = "0") BigDecimal craneFee,
                         @RequestParam(defaultValue = "0") BigDecimal shippingFee,
                         @RequestParam String customerEmail,
                         @RequestParam(required = false) String notes,
                         RedirectAttributes redirectAttributes) {
        try {
            Order order = inPersonOrderService.updateInPersonOrder(
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
            redirectAttributes.addFlashAttribute("editErrorField", resolveOrderFormErrorField(e.getMessage()));
            keepEditForm(redirectAttributes, orderId, customerName, customerPhone, shippingAddress,
                    paymentMethod, craneFee, shippingFee, customerEmail, notes);
        }
        return "redirect:/artisan/in-person-order#walkInOrdersSection";
    }

    // Giữ lại dữ liệu form tạo đơn khi redirect sau lỗi validation.
    private void keepCreateForm(RedirectAttributes redirectAttributes,
                                Integer productId,
                                String customerName,
                                String customerPhone,
                                String shippingAddress,
                                String paymentMethod,
                                BigDecimal craneFee,
                                BigDecimal shippingFee,
                                String customerEmail,
                                String notes) {
        redirectAttributes.addFlashAttribute("createProductId", productId);
        redirectAttributes.addFlashAttribute("createCustomerName", customerName);
        redirectAttributes.addFlashAttribute("createCustomerPhone", customerPhone);
        redirectAttributes.addFlashAttribute("createShippingAddress", shippingAddress);
        redirectAttributes.addFlashAttribute("createPaymentMethod", paymentMethod);
        redirectAttributes.addFlashAttribute("createCraneFee", craneFee);
        redirectAttributes.addFlashAttribute("createShippingFee", shippingFee);
        redirectAttributes.addFlashAttribute("createCustomerEmail", customerEmail);
        redirectAttributes.addFlashAttribute("createNotes", notes);
    }

    // Giữ lại dữ liệu form sửa đơn khi redirect sau lỗi validation.
    private void keepEditForm(RedirectAttributes redirectAttributes,
                              Integer orderId,
                              String customerName,
                              String customerPhone,
                              String shippingAddress,
                              String paymentMethod,
                              BigDecimal craneFee,
                              BigDecimal shippingFee,
                              String customerEmail,
                              String notes) {
        redirectAttributes.addFlashAttribute("editOrderId", orderId);
        redirectAttributes.addFlashAttribute("editCustomerName", customerName);
        redirectAttributes.addFlashAttribute("editCustomerPhone", customerPhone);
        redirectAttributes.addFlashAttribute("editShippingAddress", shippingAddress);
        redirectAttributes.addFlashAttribute("editPaymentMethod", paymentMethod);
        redirectAttributes.addFlashAttribute("editCraneFee", craneFee);
        redirectAttributes.addFlashAttribute("editShippingFee", shippingFee);
        redirectAttributes.addFlashAttribute("editCustomerEmail", customerEmail);
        redirectAttributes.addFlashAttribute("editNotes", notes);
    }

    // Suy ra field lỗi để giao diện focus đúng input.
    private String resolveOrderFormErrorField(String message) {
        if (message == null) {
            return null;
        }
        if (message.contains("sản phẩm")) {
            return "productId";
        }
        if (message.contains("Tên khách")) {
            return "customerName";
        }
        if (message.contains("Số điện thoại")) {
            return "customerPhone";
        }
        if (message.contains("Địa chỉ")) {
            return "shippingAddress";
        }
        if (message.contains("Email")) {
            return "customerEmail";
        }
        if (message.contains("Phí")) {
            return "craneFee";
        }
        if (message.contains("Ghi chú")) {
            return "notes";
        }
        return null;
    }
}

