# Hướng dẫn Tích hợp Cổng Thanh toán VNPay (Demo - Sandbox)

Tài liệu này hướng dẫn cách tích hợp cổng thanh toán **VNPay Sandbox** (môi trường thử nghiệm dành cho lập trình viên) vào dự án **Bonsai Shop** (Spring Boot + Thymeleaf). 

Vì đây là phiên bản Demo, bạn **không cần đăng ký doanh nghiệp** hay ký hợp đồng thật với VNPay. Toàn bộ thông tin cấu hình và thẻ ngân hàng dùng để kiểm thử đều được cung cấp sẵn dưới đây.

---

## 1. Thông tin Cấu hình VNPay Sandbox

Dưới đây là thông tin cấu hình thử nghiệm được cung cấp mặc định bởi VNPay:

| Tham số | Giá trị cấu hình test | Ý nghĩa |
| :--- | :--- | :--- |
| **vnp_TmnCode** | `2QXFA55W` | Mã định danh website của bạn tại hệ thống VNPay |
| **vnp_HashSecret** | `GETPGBVCFEPWZUZBNNSZKXTJLDNEZTRH` | Chuỗi mã khóa dùng để tạo chữ ký bảo mật checksum (HMAC-SHA512) |
| **vnp_PayUrl** | `https://sandbox.vnpayment.vn/paymentv2/vpcpay.html` | URL chuyển hướng khách hàng đến trang thanh toán của VNPay |
| **vnp_ReturnUrl** | `http://localhost:8080/vnpay/payment-callback` | URL nhận kết quả trả về từ VNPay sau khi khách hàng thanh toán xong |
| **vnp_IpnUrl** | `http://localhost:8080/vnpay/ipn` | URL để VNPay gọi ngầm cập nhật trạng thái đơn hàng (IPN - Instant Payment Notification) |

---

## 2. Thông tin Thẻ Ngân hàng Thử nghiệm (Test Cards)

Khi thực hiện thanh toán trên trang sandbox của VNPay, bạn chọn phương thức thanh toán **Thẻ nội địa và tài khoản ngân hàng** rồi nhập thông tin thẻ dưới đây:

*   **Ngân hàng:** **NCB** (Ngân hàng Quốc Dân)
*   **Số thẻ:** `9704198526191432198`
*   **Tên chủ thẻ:** `NGUYEN VAN A`
*   **Ngày phát hành:** `07/15`
*   **Mật khẩu OTP:** `123456`
*   **Số tiền tối thiểu:** `10,000 VND` (VNPay giới hạn số tiền thanh toán tối thiểu)

---

## 3. Các bước Tích hợp vào Mã nguồn Spring Boot

### Bước 1: Cấu hình `application.properties`

Thêm các thuộc tính cấu hình của VNPay vào file [application.properties](src/main/resources/application.properties):

```properties
# ================================================
# VNPAY CONFIGURATION (SANDBOX)
# ================================================
vnp.pay-url=https://sandbox.vnpayment.vn/paymentv2/vpcpay.html
vnp.tmn-code=2QXFA55W
vnp.hash-secret=GETPGBVCFEPWZUZBNNSZKXTJLDNEZTRH
vnp.return-url=http://localhost:8080/vnpay/payment-callback
```

---

### Bước 2: Tạo Class Tiện ích `VNPayConfig.java`

Tạo file Java cấu hình và chứa các phương thức mã hóa SHA-512 tại đường dẫn: `src/main/java/com/example/bonsai_shop/config/VNPayConfig.java`

```java
package com.example.bonsai_shop.config;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Random;

@Configuration
public class VNPayConfig {

    public static String vnp_PayUrl;
    public static String vnp_TmnCode;
    public static String vnp_HashSecret;
    public static String vnp_ReturnUrl;

    @Value("${vnp.pay-url}")
    public void setPayUrl(String payUrl) {
        vnp_PayUrl = payUrl;
    }

    @Value("${vnp.tmn-code}")
    public void setTmnCode(String tmnCode) {
        vnp_TmnCode = tmnCode;
    }

    @Value("${vnp.hash-secret}")
    public void setHashSecret(String hashSecret) {
        vnp_HashSecret = hashSecret;
    }

    @Value("${vnp.return-url}")
    public void setReturnUrl(String returnUrl) {
        vnp_ReturnUrl = returnUrl;
    }

    // Mã hóa HMAC-SHA512 bảo mật chữ ký giao dịch
    public static String hmacSHA512(final String key, final String data) {
        try {
            if (key == null || data == null) {
                throw new NullPointerException();
            }
            final Mac hmac512 = Mac.getInstance("HmacSHA512");
            byte[] hmacKeyBytes = key.getBytes(StandardCharsets.UTF_8);
            final SecretKeySpec secretKey = new SecretKeySpec(hmacKeyBytes, "HmacSHA512");
            hmac512.init(secretKey);
            byte[] dataBytes = data.getBytes(StandardCharsets.UTF_8);
            byte[] result = hmac512.doFinal(dataBytes);
            StringBuilder sb = new StringBuilder(2 * result.length);
            for (byte b : result) {
                sb.append(String.format("%02x", b & 0xff));
            }
            return sb.toString();
        } catch (Exception ex) {
            return "";
        }
    }

    // Lấy IP Client thực hiện giao dịch
    public static String getIpAddress(HttpServletRequest request) {
        String ipAddress;
        try {
            ipAddress = request.getHeader("X-FORWARDED-FOR");
            if (ipAddress == null || ipAddress.isEmpty() || "unknown".equalsIgnoreCase(ipAddress)) {
                ipAddress = request.getRemoteAddr();
            }
        } catch (Exception e) {
            ipAddress = "127.0.0.1";
        }
        return ipAddress;
    }

    // Tạo chuỗi số ngẫu nhiên cho mã tham chiếu giao dịch (vnp_TxnRef)
    public static String getRandomNumber(int len) {
        Random rnd = new Random();
        String chars = "0123456789";
        StringBuilder sb = new StringBuilder(len);
        for (int i = 0; i < len; i++) {
            sb.append(chars.charAt(rnd.nextInt(chars.length())));
        }
        return sb.toString();
    }
}
```

---

### Bước 3: Cấu hình Cho phép Truy cập (Spring Security)

Vì VNPay cần gọi trực tiếp URL Callback (`/vnpay/payment-callback`) và IPN (`/vnpay/ipn`), bạn cần cấu hình cho phép các URL này truy cập tự do mà không cần đăng nhập.

Mở file `SecurityConfig.java` và bổ sung `/vnpay/**` vào danh sách `requestMatchers().permitAll()`:

```java
// Trong SecurityConfig.java
.requestMatchers(
        "/",
        "/products/**",
        "/register",
        "/login",
        "/forgot-password",
        "/vnpay/**",    // ← THÊM DÒNG NÀY ĐỂ BỎ QUA XÁC THỰC VNPAY CALLBACK
        "/css/**",
        "/js/**",
        "/images/**"
).permitAll()
```

---

### Bước 4: Viết Controller Xử lý Giao dịch

Cập nhật mã nguồn vào file `PaymentController.java` tại `src/main/java/com/example/bonsai_shop/product/controller/PaymentController.java` để xử lý việc **tạo liên kết thanh toán** và **nhận kết quả trả về (Return URL)**:

```java
package com.example.bonsai_shop.product.controller;

import com.example.bonsai_shop.config.VNPayConfig;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;

@Controller
public class PaymentController {

    // Tạo link thanh toán VNPay
    @GetMapping("/vnpay/create-payment")
    public String createPayment(HttpServletRequest req, @RequestParam("amount") long amount) throws UnsupportedEncodingException {
        
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
        vnp_Params.put("vnp_OrderInfo", "Thanh toan don hang BSMS:" + vnp_TxnRef);
        vnp_Params.put("vnp_OrderType", orderType);
        vnp_Params.put("vnp_Locale", "vn");
        vnp_Params.put("vnp_ReturnUrl", VNPayConfig.vnp_ReturnUrl);
        vnp_Params.put("vnp_IpAddr", vnp_IpAddr);

        // Định dạng thời gian GTM+7
        Calendar cld = Calendar.getInstance(TimeZone.getTimeZone("Etc/GMT+7"));
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
        String vnp_CreateDate = formatter.format(cld.getTime());
        vnp_Params.put("vnp_CreateDate", vnp_CreateDate);
        
        cld.add(Calendar.MINUTE, 15); // Thời gian hết hạn thanh toán
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
                // Build hash data (quan trọng: VNPay yêu cầu replace '+' bằng '%20' khi encode)
                hashData.append(fieldName);
                hashData.append('=');
                hashData.append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII.toString()).replace("+", "%20"));
                
                // Build query string
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

    // Nhận thông báo kết quả trả về từ trình duyệt của khách hàng
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
                    sb.append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII.toString()).replace("+", "%20"));
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
            if ("00".equals(responseCode)) {
                model.addAttribute("status", "SUCCESS");
                model.addAttribute("message", "Thanh toán giao dịch thành công!");
                model.addAttribute("amount", Double.parseDouble(request.getParameter("vnp_Amount")) / 100);
                model.addAttribute("txnRef", request.getParameter("vnp_TxnRef"));
                model.addAttribute("orderInfo", request.getParameter("vnp_OrderInfo"));
            } else {
                model.addAttribute("status", "FAILED");
                model.addAttribute("message", "Thanh toán thất bại hoặc đã bị hủy (Mã lỗi: " + responseCode + ")");
            }
        } else {
            model.addAttribute("status", "INVALID_SIGNATURE");
            model.addAttribute("message", "Chữ ký kiểm tra bảo mật không hợp lệ!");
        }
        
        return "payment-result"; // View Thymeleaf hiển thị kết quả (payment-result.html)
    }
}
```

---

### Bước 5: Viết Controller xử lý IPN (Ngầm cập nhật DB)

Cập nhật mã nguồn vào file `IPNController.java` tại `src/main/java/com/example/bonsai_shop/product/controller/IPNController.java`.

> [!IMPORTANT]
> **Tại sao cần IPN?**
> Nếu người dùng tắt trình duyệt đột ngột sau khi thanh toán xong tại VNPay, hoặc gặp sự cố mất mạng, trình duyệt sẽ không chạy được URL `/vnpay/payment-callback`. Lúc này, hệ thống máy chủ VNPay sẽ tự động gọi ngầm đến endpoint IPN này để cập nhật trạng thái đơn hàng trong Database của bạn.

```java
package com.example.bonsai_shop.product.controller;

import com.example.bonsai_shop.config.VNPayConfig;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

@RestController
public class IPNController {

    @GetMapping("/vnpay/ipn")
    public Map<String, String> receiveIPN(HttpServletRequest request) {
        Map<String, String> response = new HashMap<>();
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
                    sb.append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII.toString()).replace("+", "%20"));
                    if (itr.hasNext()) {
                        sb.append('&');
                    }
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
            }
        }

        String signValue = VNPayConfig.hmacSHA512(VNPayConfig.vnp_HashSecret, sb.toString());

        if (signValue.equals(vnp_SecureHash)) {
            // TODO: BƯỚC XỬ LÝ NGHIỆP VỤ CỦA HỆ THỐNG
            // 1. Kiểm tra mã đơn hàng (vnp_TxnRef) có tồn tại trong Database hay không
            // 2. Kiểm tra số tiền thanh toán (vnp_Amount) khớp với giá trị đơn hàng trong DB
            // 3. Kiểm tra trạng thái đơn hàng hiện tại (chỉ cập nhật nếu đơn hàng ở trạng thái chờ thanh toán)
            
            boolean checkOrderId = true; // Giả sử đơn hàng tồn tại
            boolean checkAmount = true;  // Giả sử số tiền khớp
            boolean checkOrderStatus = true; // Giả sử đơn hàng chưa được xử lý thanh toán trước đó

            if (checkOrderId) {
                if (checkAmount) {
                    if (checkOrderStatus) {
                        String responseCode = request.getParameter("vnp_ResponseCode");
                        if ("00".equals(responseCode)) {
                            // Cập nhật trạng thái ĐÃ THANH TOÁN thành công cho đơn hàng trong DB
                        } else {
                            // Cập nhật trạng thái THANH TOÁN THẤT BẠI cho đơn hàng trong DB
                        }
                        
                        response.put("RspCode", "00");
                        response.put("Message", "Confirm Success");
                    } else {
                        response.put("RspCode", "02");
                        response.put("Message", "Order already confirmed");
                    }
                } else {
                    response.put("RspCode", "04");
                    response.put("Message", "Invalid Amount");
                }
            } else {
                response.put("RspCode", "01");
                response.put("Message", "Order not Found");
            }
        } else {
            response.put("RspCode", "97");
            response.put("Message", "Invalid Checksum");
        }

        return response;
    }
}
```

---

### Bước 6: Tạo Template hiển thị kết quả `payment-result.html`

Tạo trang HTML Thymeleaf hiển thị trạng thái thanh toán thành công hay thất bại tại đường dẫn: `src/main/resources/templates/payment-result.html`

```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org">
<head>
    <meta charset="UTF-8">
    <title>Kết quả thanh toán VNPay</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css" rel="stylesheet">
    <style>
        body {
            background-color: #f8f9fa;
        }
        .card-result {
            max-width: 550px;
            margin: 80px auto;
            border-radius: 15px;
            box-shadow: 0 4px 15px rgba(0, 0, 0, 0.1);
        }
    </style>
</head>
<body>
<div class="container">
    <div class="card card-result p-4 bg-white text-center">
        <!-- Thành công -->
        <div th:if="${status == 'SUCCESS'}">
            <div class="text-success mb-3">
                <svg xmlns="http://www.w3.org/2000/svg" width="64" height="64" fill="currentColor" class="bi bi-check-circle-fill" viewBox="0 0 16 16">
                    <path d="M16 8A8 8 0 1 1 0 8a8 8 0 0 1 16 0zm-3.97-3.03a.75.75 0 0 0-1.08.022L7.477 9.417 5.384 7.323a.75.75 0 0 0-1.06 1.06L6.97 11.03a.75.75 0 0 0 1.079-.02l3.992-4.99a.75.75 0 0 0-.01-1.05z"/>
                </svg>
            </div>
            <h2 class="text-success mb-4" th:text="${message}">Thanh toán thành công</h2>
            
            <div class="text-start">
                <p><strong>Mã giao dịch:</strong> <span th:text="${txnRef}"></span></p>
                <p><strong>Số tiền:</strong> <span th:text="${#numbers.formatDecimal(amount, 0, 'COMMA', 0, 'POINT')} + ' VND'"></span></p>
                <p><strong>Thông tin thanh toán:</strong> <span th:text="${orderInfo}"></span></p>
            </div>
        </div>

        <!-- Thất bại -->
        <div th:if="${status != 'SUCCESS'}">
            <div class="text-danger mb-3">
                <svg xmlns="http://www.w3.org/2000/svg" width="64" height="64" fill="currentColor" class="bi bi-x-circle-fill" viewBox="0 0 16 16">
                    <path d="M16 8A8 8 0 1 1 0 8a8 8 0 0 1 16 0zM5.354 4.646a.5.5 0 1 0-.708.708L7.293 8l-2.647 2.646a.5.5 0 0 0 .708.708L8 8.707l2.646 2.647a.5.5 0 0 0 .708-.708L8.707 8l2.647-2.646a.5.5 0 0 0-.708-.708L8 7.293 5.354 4.646z"/>
                </svg>
            </div>
            <h2 class="text-danger mb-4">Thanh toán không thành công</h2>
            <p class="text-muted" th:text="${message}">Mô tả chi tiết lỗi</p>
        </div>

        <hr class="my-4">
        <a href="/" class="btn btn-primary px-4 py-2">Quay lại trang chủ</a>
    </div>
</div>
</body>
</html>
```

---

### Bước 7: Tích hợp nút thanh toán vào giao diện mua hàng (Thymeleaf)

Trên giao diện xác nhận đặt hàng hoặc giỏ hàng, bạn thêm nút hoặc form chuyển hướng thanh toán:

```html
<!-- Form Thymeleaf gửi số tiền qua VNPay -->
<form th:action="@{/vnpay/create-payment}" method="get">
    <!-- Nhập/Gửi số tiền thanh toán (Ví dụ: 100,000 VND) -->
    <input type="hidden" name="amount" th:value="${totalPrice}" />
    
    <button type="submit" class="btn btn-success btn-lg w-100">
        Thanh toán trực tuyến qua VNPay
    </button>
</form>
```

---

## 4. Hướng dẫn Chạy Thử và Kiểm thử (Testing Flow)

1. **Chạy ứng dụng Spring Boot** (`mvnw spring-boot:run` hoặc nút Run trong IDE).
2. Thêm sản phẩm vào giỏ hàng và đi tới trang thanh toán.
3. Click vào nút **Thanh toán trực tuyến qua VNPay**.
4. Trình duyệt của bạn sẽ được chuyển hướng tự động đến cổng thanh toán sandbox của VNPay.
5. Chọn phương thức thanh toán **Thẻ nội địa NCB** và nhập thông tin thẻ test như ở [Mục 2](#2-thông-tin-thẻ-ngân-hàng-thử-nghiệm-test-cards).
6. Hệ thống VNPay sẽ gửi yêu cầu nhập mã OTP, nhập: `123456`.
7. Trình duyệt chuyển hướng trở lại trang của bạn tại `http://localhost:8080/vnpay/payment-callback` và hiển thị kết quả thành công.

> [!TIP]
> **Mẹo nâng cao: Để test IPN khi chạy local**
> VNPay Sandbox chạy trên môi trường internet công khai nên nó không thể gọi được link `http://localhost:8080/vnpay/ipn` trên máy của bạn. Để kiểm tra tính năng IPN cập nhật DB tự động, bạn có thể cài đặt công cụ **ngrok** hoặc **localtunnel** để tạo một đường link internet ảo (public tunnel) trỏ về localhost cổng 8080 của bạn (ví dụ: `https://abcd-1234.ngrok-free.app/vnpay/ipn`). Cập nhật link này trong tài khoản quản trị sandbox của bạn hoặc thay đổi cấu hình trả về.
