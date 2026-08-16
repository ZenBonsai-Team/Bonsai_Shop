package com.example.bonsai_shop.product.controller;

import com.example.bonsai_shop.config.VNPayConfig;
import com.example.bonsai_shop.entity.Order;
import com.example.bonsai_shop.entity.Payment;
import com.example.bonsai_shop.entity.Product;
import com.example.bonsai_shop.product.repository.OrderRepository;
import com.example.bonsai_shop.product.repository.ProductRepository;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * [VAI TRÒ TRONG LUỒNG THANH TOÁN VNPAY]
 *
 * Chịu trách nhiệm:
 * - Điều hướng người dùng sang cổng thanh toán trực tuyến VNPay Gateway.
 * - Tạo URL thanh toán VNPay chuẩn mã hóa HMAC-SHA512 kèm thời hạn hết hạn 15 phút.
 * - Tiếp nhận Return URL (Payment Callback) từ trình duyệt khách hàng sau khi thanh toán trên VNPay.
 * - Xác thực chữ ký số bảo mật (Checksum), cập nhật trạng thái đơn hàng và hiển thị giao diện kết quả giao dịch (payment-result.html).
 *
 * Các thao tác trên web đi qua class này:
 * - [Khách bấm Link thanh toán trong Email duyệt đơn] → GET /vnpay/pay-order?orderCode=... → payOrder()
 * - [VNPay chuyển hướng khách về sau khi thanh toán] → GET /vnpay/payment-callback → paymentCallback()
 * - [Thanh toán trực tiếp theo mã sản phẩm (Test/Legacy)] → GET /vnpay/create-payment?productId=... → createPayment()
 *
 * Các thành phần phối hợp chính:
 * - VNPayConfig: Cung cấp tham số cấu hình TmnCode, HashSecret, PayUrl, ReturnUrl, thuật toán mã hóa hmacSHA512.
 * - OrderService: preparePendingVnPayPayment(), processPaymentSuccess(), processPaymentFailure().
 * - OrderRepository, PaymentRepository, ProductRepository, MailService.
 */
@Controller
public class PaymentController {

    @Autowired
    private ProductRepository productRepository;
    @Autowired
    private OrderRepository orderRepository;
    @Autowired
    private com.example.bonsai_shop.product.service.MailService mailService;
    @Autowired
    private com.example.bonsai_shop.product.service.OrderService orderService;
    @Autowired
    private com.example.bonsai_shop.product.repository.PaymentRepository paymentRepository;

    @Value("${order.expiration.online-minutes:15}")
    private int onlineExpirationMinutes;

    /**
     * [TẠO LINK THANH TOÁN VNPAY TRỰC TIẾP CHO SẢN PHẨM (LEGACY / TEST)]
     *
     * // TODO-AUDIT: Endpoint này tạo URL thanh toán trực tiếp từ productId mà không gắn với Order entity cụ thể trong database, chủ yếu dùng cho kịch bản test mua nhanh đơn lẻ hoặc luồng legacy.
     *
     * Khi nào được gọi trên web:
     * - Người dùng thực hiện: Bấm thanh toán ngay một cây bonsai cụ thể từ trang chi tiết sản phẩm.
     * - Màn hình/chức năng: Nút Mua ngay (Buy Now) thử nghiệm.
     *
     * API:
     * - HTTP: GET
     * - URL: /vnpay/create-payment
     * - Người gọi: Customer / Guest
     *
     * Dữ liệu nhận vào:
     * - Request param: productId (Integer)
     *
     * Điều phối xử lý:
     * 1. Tìm Product qua ProductRepository.findById(productId).
     * 2. Lấy giá sản phẩm, nhân 100 theo chuẩn VNPay.
     * 3. Tạo mã giao dịch ngẫu nhiên vnp_TxnRef (8 chữ số).
     * 4. Thiết lập thời gian hết hạn thanh toán 15 phút (vnp_ExpireDate = now + 15m).
     * 5. Sắp xếp tham số alphabet và băm chữ ký HMAC-SHA512 với HashSecret.
     * 6. Chuyển hướng trình duyệt (Redirect 302) sang cổng thanh toán VNPay.
     *
     * Dữ liệu trả về:
     * - HTTP status: 302 Found (Redirect)
     * - Redirect URL: URL cổng thanh toán VNPay kèm chuỗi truy vấn và mã băm bảo mật vnp_SecureHash.
     */
    @GetMapping("/vnpay/create-payment")
    public String createPayment(HttpServletRequest req, @RequestParam("productId") Integer productId)
            throws UnsupportedEncodingException {

        // 1. Lấy sản phẩm thực tế từ Database để đảm bảo an toàn về giá
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Sản phẩm không tồn tại!"));

        // 2. Chuyển đổi giá sản phẩm thành kiểu số nguyên (VND)
        long amount = product.getPrice().longValue();
        String vnp_Version = "2.1.0";
        String vnp_Command = "pay";
        String orderType = "other";

        // VNPay nhận số tiền nhân với 100 (ví dụ: 10,000đ gửi đi là 1000000)
        long totalAmount = amount * 100;

        String vnp_TxnRef = VNPayConfig.getRandomNumber(8); // Mã giao dịch duy nhất
        String vnp_IpAddr = VNPayConfig.getIpAddress(req);
        String vnp_TmnCode = VNPayConfig.vnp_TmnCode;

        Map<String, String> vnp_Params = new HashMap<>();
        vnp_Params.put("vnp_Version", vnp_Version);
        vnp_Params.put("vnp_Command", vnp_Command);
        vnp_Params.put("vnp_TmnCode", vnp_TmnCode);
        vnp_Params.put("vnp_Amount", String.valueOf(totalAmount));
        vnp_Params.put("vnp_CurrCode", "VND");

        vnp_Params.put("vnp_TxnRef", vnp_TxnRef);
        vnp_Params.put("vnp_OrderInfo", "Thanh toan don hang " + vnp_TxnRef);
        vnp_Params.put("vnp_OrderType", orderType);
        vnp_Params.put("vnp_Locale", "vn");
        vnp_Params.put("vnp_ReturnUrl", VNPayConfig.vnp_ReturnUrl);
        vnp_Params.put("vnp_IpAddr", vnp_IpAddr);

        // Định dạng thời gian GMT+7 (Asia/Ho_Chi_Minh)
        Calendar cld = Calendar.getInstance(TimeZone.getTimeZone("Asia/Ho_Chi_Minh"));
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
        String vnp_CreateDate = formatter.format(cld.getTime());
        vnp_Params.put("vnp_CreateDate", vnp_CreateDate);

        cld.add(Calendar.MINUTE, onlineExpirationMinutes); // Thời gian hết hạn thanh toán
        String vnp_ExpireDate = formatter.format(cld.getTime());
        vnp_Params.put("vnp_ExpireDate", vnp_ExpireDate);

        // Sắp xếp tham số theo alphabet
        List<String> fieldNames = new ArrayList<>(vnp_Params.keySet());
        Collections.sort(fieldNames);

        StringBuilder hashData = new StringBuilder();
        StringBuilder query = new StringBuilder();
        Iterator<String> itr = fieldNames.iterator();

        while (itr.hasNext()) {
            String fieldName = itr.next();
            String fieldValue = vnp_Params.get(fieldName);
            if ((fieldValue != null) && (fieldValue.length() > 0)) {
                // Build hashData
                hashData.append(fieldName);
                hashData.append('=');
                hashData.append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII.toString()));

                // Build query
                query.append(URLEncoder.encode(fieldName, StandardCharsets.US_ASCII.toString()));
                query.append('=');
                query.append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII.toString()));

                if (itr.hasNext()) {
                    query.append('&');
                    hashData.append('&');
                }
            }
        }

        String queryUrl = query.toString();
        String vnp_SecureHash = VNPayConfig.hmacSHA512(VNPayConfig.vnp_HashSecret, hashData.toString());
        queryUrl += "&vnp_SecureHash=" + vnp_SecureHash;

        String paymentUrl = VNPayConfig.vnp_PayUrl + "?" + queryUrl;

        // Chuyển hướng trình duyệt đến trang VNPay thanh toán
        return "redirect:" + paymentUrl;
    }

    /**
     * [TIẾP NHẬN KẾT QUẢ THANH TOÁN TỪ TRÌNH DUYỆT KHÁCH HÀNG (RETURN URL)]
     *
     * Khi nào được gọi trên web:
     * - Người dùng thực hiện: Sau khi khách hoàn tất hoặc hủy thao tác thanh toán trên giao diện cổng VNPay, VNPay tự động chuyển hướng trình duyệt của khách về Return URL hệ thống.
     * - Màn hình/chức năng: Trang thông báo kết quả thanh toán (payment-result.html).
     *
     * API:
     * - HTTP: GET
     * - URL: /vnpay/payment-callback
     * - Người gọi: Trình duyệt của khách hàng (Redirect từ VNPay)
     *
     * Dữ liệu nhận vào:
     * - Request params:
     *   + vnp_TxnRef (String): Mã đơn hàng (orderCode).
     *   + vnp_Amount (String): Số tiền đã thanh toán (*100).
     *   + vnp_ResponseCode (String): Mã kết quả ("00" là thành công).
     *   + vnp_TransactionStatus (String): Trạng thái giao dịch ("00" là thành công).
     *   + vnp_SecureHash (String): Chữ ký checksum từ VNPay.
     *
     * Điều phối xử lý:
     * 1. Thu thập tất cả tham số từ HttpServletRequest, loại bỏ SecureHash để chuẩn bị kiểm tra chữ ký.
     * 2. Băm chuỗi dữ liệu nhận được bằng thuật toán HMAC-SHA512 với VNPayConfig.vnp_HashSecret.
     * 3. So sánh chữ ký băm với vnp_SecureHash:
     *    - Nếu chữ ký hợp lệ và vnp_ResponseCode == "00" && vnp_TransactionStatus == "00":
     *      + Gọi OrderService.processPaymentSuccess(orderCode) để cập nhật Payment (SUCCESS) và Order (DEPOSITED hoặc PAID).
     *      + Đặt model attribute: status = "SUCCESS".
     *    - Nếu thanh toán thất bại/hủy:
     *      + Gọi OrderService.processPaymentFailure(orderCode, responseCode, transactionStatus, "RETURN_URL").
     *      + Đặt model attribute: status = "FAILED".
     *    - Nếu sai chữ ký:
     *      + Đặt model attribute: status = "INVALID_SIGNATURE".
     * 4. Trả về view "payment-result" hiển thị cho người dùng.
     *
     * Dữ liệu trả về:
     * - HTML View: payment-result (Thymeleaf template hiển thị trạng thái SUCCESS, FAILED hoặc INVALID_SIGNATURE kèm số tiền, mã đơn).
     *
     * Tác động dữ liệu:
     * - Bảng/Entity bị đọc: ORDER, PAYMENT
     * - Bảng/Entity bị ghi/cập nhật:
     *   + PAYMENT: paymentStatus = "SUCCESS" hoặc "FAILED", paymentDate
     *   + ORDER: orderStatus: PENDING_PAYMENT → DEPOSITED (nếu cọc) hoặc PAID (nếu thanh toán đủ)
     */
    @GetMapping("/vnpay/payment-callback")
    public String paymentCallback(HttpServletRequest request, Model model) {
        Map<String, String> fields = new HashMap<>();
        for (Enumeration<String> params = request.getParameterNames(); params.hasMoreElements();) {
            String fieldName = params.nextElement();
            String fieldValue = request.getParameter(fieldName);
            if ((fieldValue != null) && (fieldValue.length() > 0)) {
                fields.put(fieldName, fieldValue);
            }
        }

        String vnp_SecureHash = request.getParameter("vnp_SecureHash");
        fields.remove("vnp_SecureHashType");
        fields.remove("vnp_SecureHash");

        // Sắp xếp các tham số để kiểm tra chữ ký checksum
        List<String> fieldNames = new ArrayList<>(fields.keySet());
        Collections.sort(fieldNames);
        StringBuilder sb = new StringBuilder();
        Iterator<String> itr = fieldNames.iterator();
        while (itr.hasNext()) {
            String fieldName = itr.next();
            String fieldValue = fields.get(fieldName);
            if ((fieldValue != null) && (fieldValue.length() > 0)) {
                try {
                    sb.append(fieldName);
                    sb.append('=');
                    sb.append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII.toString()));
                    if (itr.hasNext()) {
                        sb.append('&');
                    }
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
            }
        }

        String signValue = VNPayConfig.hmacSHA512(VNPayConfig.vnp_HashSecret, sb.toString());

        // Kiểm tra tính hợp lệ của chữ ký để đảm bảo dữ liệu không bị thay đổi
        if (signValue.equals(vnp_SecureHash)) {
            String responseCode = request.getParameter("vnp_ResponseCode");
            String transactionStatus = request.getParameter("vnp_TransactionStatus");
            String orderCode = request.getParameter("vnp_TxnRef");
            if (isSuccessfulVnPayResult(responseCode, transactionStatus)) {
                orderService.processPaymentSuccess(orderCode);
                model.addAttribute("status", "SUCCESS");
                model.addAttribute("message", "Thanh toán giao dịch thành công!");
                model.addAttribute("amount", Double.parseDouble(request.getParameter("vnp_Amount")) / 100);
                model.addAttribute("txnRef", orderCode);
                model.addAttribute("orderInfo", request.getParameter("vnp_OrderInfo"));
            } else {
                orderService.processPaymentFailure(orderCode, responseCode, transactionStatus, "RETURN_URL");
                model.addAttribute("status", "FAILED");
                model.addAttribute("message", "Thanh toán thất bại hoặc đã bị hủy (Mã lỗi: " + responseCode + ")");
            }
        } else {
            model.addAttribute("status", "INVALID_SIGNATURE");
            model.addAttribute("message", "Chữ ký kiểm tra bảo mật không hợp lệ!");
        }

        return "payment-result"; // View Thymeleaf hiển thị kết quả (payment-result.html)
    }

    /**
     * [MỞ CỔNG THANH TOÁN VNPAY CHO ĐƠN HÀNG ĐÃ ĐƯỢC DUYỆT (PAY ORDER)]
     *
     * Khi nào được gọi trên web:
     * - Người dùng thực hiện: Khách hàng mở email duyệt đơn hàng ("Xác nhận đơn hàng BSMS-XXXXXX") và bấm nút "Thanh toán ngay qua VNPay" (hoặc link thanh toán từ giao diện tra cứu đơn).
     * - Màn hình/chức năng: Link thanh toán VNPay từ email / Web.
     *
     * API:
     * - HTTP: GET
     * - URL: /vnpay/pay-order?orderCode={orderCode}
     * - Người gọi: Customer
     *
     * Dữ liệu nhận vào:
     * - Request param: orderCode (String, ví dụ "BSMS-K18J29")
     *
     * Điều phối xử lý:
     * 1. Tìm Order trong CSDL theo orderCode.
     * 2. Gọi OrderService.preparePendingVnPayPayment(orderCode) để lấy/tạo bản ghi Payment PENDING với số tiền chính xác cần thanh toán (nếu là cọc = depositAmount; nếu thanh toán đủ = totalAmount).
     * 3. Thiết lập các tham số giao dịch gửi sang VNPay:
     *    - vnp_Amount = amount * 100
     *    - vnp_TxnRef = orderCode (Dùng chính mã đơn hàng để mapping khi nhận callback/IPN)
     *    - vnp_ExpireDate = now + 15 phút (giới hạn thời gian thanh toán trên cổng VNPay)
     *    - vnp_ReturnUrl = VNPayConfig.vnp_ReturnUrl
     * 4. Băm chữ ký bảo mật HMAC-SHA512 và tạo paymentUrl hoàn chỉnh.
     * 5. Redirect 302 đưa khách sang màn hình thanh toán của VNPay.
     *
     * Dữ liệu trả về:
     * - HTTP status: 302 Found (Redirect)
     * - Redirect URL: Cổng thanh toán VNPay.
     *
     * Tác động dữ liệu:
     * - Bảng/Entity bị đọc: ORDER, PAYMENT
     * - Bảng/Entity bị ghi/cập nhật: PAYMENT (nếu cần retry tạo lại bản ghi PENDING mới)
     * - Thay đổi trạng thái: Giữ nguyên PENDING_PAYMENT, chờ khách thực hiện thanh toán trên VNPay.
     */
    @GetMapping("/vnpay/pay-order")
    public String payOrder(HttpServletRequest req, @RequestParam("orderCode") String orderCode)
            throws UnsupportedEncodingException {

        // 1. Tìm đơn hàng trong CSDL theo orderCode
        Order order = orderRepository.findByOrderCode(orderCode)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn hàng: " + orderCode));

        // 2. Lấy số tiền từ Payment record PENDING gần nhất (nếu có, ví dụ: Đặt cọc = Deposit + Crane + Ship)
        Payment pendingPayment = orderService.preparePendingVnPayPayment(orderCode);

        long amount = pendingPayment.getAmount().longValue();

        String vnp_Version = "2.1.0";
        String vnp_Command = "pay";
        String orderType = "other";

        // VNPay quy định nhân 100
        long totalAmount = amount * 100;
        String vnp_TxnRef = orderCode; // Dùng trực tiếp orderCode làm mã tham chiếu giao dịch
        String vnp_IpAddr = VNPayConfig.getIpAddress(req);
        String vnp_TmnCode = VNPayConfig.vnp_TmnCode;

        Map<String, String> vnp_Params = new HashMap<>();
        vnp_Params.put("vnp_Version", vnp_Version);
        vnp_Params.put("vnp_Command", vnp_Command);
        vnp_Params.put("vnp_TmnCode", vnp_TmnCode);
        vnp_Params.put("vnp_Amount", String.valueOf(totalAmount));
        vnp_Params.put("vnp_CurrCode", "VND");
        vnp_Params.put("vnp_TxnRef", vnp_TxnRef);
        vnp_Params.put("vnp_OrderInfo", "Thanh toan don hang " + vnp_TxnRef);
        vnp_Params.put("vnp_OrderType", orderType);
        vnp_Params.put("vnp_Locale", "vn");
        vnp_Params.put("vnp_ReturnUrl", VNPayConfig.vnp_ReturnUrl);
        vnp_Params.put("vnp_IpAddr", vnp_IpAddr);

        Calendar cld = Calendar.getInstance(TimeZone.getTimeZone("Asia/Ho_Chi_Minh"));
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
        String vnp_CreateDate = formatter.format(cld.getTime());
        vnp_Params.put("vnp_CreateDate", vnp_CreateDate);

        cld.add(Calendar.MINUTE, onlineExpirationMinutes);
        String vnp_ExpireDate = formatter.format(cld.getTime());
        vnp_Params.put("vnp_ExpireDate", vnp_ExpireDate);

        List<String> fieldNames = new ArrayList<>(vnp_Params.keySet());
        Collections.sort(fieldNames);

        StringBuilder hashData = new StringBuilder();
        StringBuilder query = new StringBuilder();
        Iterator<String> itr = fieldNames.iterator();

        while (itr.hasNext()) {
            String fieldName = itr.next();
            String fieldValue = vnp_Params.get(fieldName);
            if ((fieldValue != null) && (fieldValue.length() > 0)) {
                // Build hashData
                hashData.append(fieldName);
                hashData.append('=');
                hashData.append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII.toString()));

                // Build query
                query.append(URLEncoder.encode(fieldName, StandardCharsets.US_ASCII.toString()));
                query.append('=');
                query.append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII.toString()));

                if (itr.hasNext()) {
                    query.append('&');
                    hashData.append('&');
                }
            }
        }

        String queryUrl = query.toString();
        String vnp_SecureHash = VNPayConfig.hmacSHA512(VNPayConfig.vnp_HashSecret, hashData.toString());
        queryUrl += "&vnp_SecureHash=" + vnp_SecureHash;

        String paymentUrl = VNPayConfig.vnp_PayUrl + "?" + queryUrl;
        
        System.out.println("==================================================");
        System.out.println("=== VNPAY PAY-ORDER DIAGNOSTIC LOG ===");
        System.out.println("vnp_TmnCode: " + vnp_TmnCode);
        System.out.println("vnp_HashSecret: " + VNPayConfig.vnp_HashSecret);
        System.out.println("hashData: " + hashData.toString());
        System.out.println("vnp_SecureHash: " + vnp_SecureHash);
        System.out.println("Generated Payment URL: " + paymentUrl);
        System.out.println("==================================================");

        return "redirect:" + paymentUrl;
    }

    private boolean isSuccessfulVnPayResult(String responseCode, String transactionStatus) {
        return "00".equals(responseCode) && "00".equals(transactionStatus);
    }
}
