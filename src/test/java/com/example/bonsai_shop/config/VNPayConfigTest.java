package com.example.bonsai_shop.config;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class VNPayConfigTest {

    // =========================================================================
    // Group 1: Spring @Value Setters & Static Config Fields
    // =========================================================================

    @Test
    @DisplayName("UT-UUT09-001: setPayUrl / setTmnCode / setHashSecret / setReturnUrl - Gán giá trị static config fields")
    void testSetters_populatesStaticFields() {
        VNPayConfig config = new VNPayConfig();
        config.setPayUrl("https://sandbox.vnpayment.vn/paymentv2/vpcpay.html");
        config.setTmnCode("TESTTMN");
        config.setHashSecret("TESTSECRET123");
        config.setReturnUrl("http://localhost:8080/vnpay-return");

        assertEquals("https://sandbox.vnpayment.vn/paymentv2/vpcpay.html", VNPayConfig.vnp_PayUrl);
        assertEquals("TESTTMN", VNPayConfig.vnp_TmnCode);
        assertEquals("TESTSECRET123", VNPayConfig.vnp_HashSecret);
        assertEquals("http://localhost:8080/vnpay-return", VNPayConfig.vnp_ReturnUrl);
    }

    // =========================================================================
    // Group 2: hmacSHA512(key, data)
    // =========================================================================

    @Test
    @DisplayName("UT-UUT09-002: hmacSHA512 - key hoặc data bị null trả về 'Something wrong here'")
    void hmacSHA512_nullInput_returnsErrorMessage() {
        assertEquals("Something wrong here", VNPayConfig.hmacSHA512(null, "someData"));
        assertEquals("Something wrong here", VNPayConfig.hmacSHA512("someKey", null));
        assertEquals("Something wrong here", VNPayConfig.hmacSHA512(null, null));
    }

    @Test
    @DisplayName("UT-UUT09-003: hmacSHA512 - Hash thành công dữ liệu hợp lệ trả về chuỗi hex 128 ký tự")
    void hmacSHA512_validInput_returns128HexChars() {
        String key = "secret_key_123";
        String data = "vnp_Amount=10000000&vnp_Command=pay&vnp_CreateDate=20260808000000";

        String hash = VNPayConfig.hmacSHA512(key, data);

        assertNotNull(hash);
        assertEquals(128, hash.length());
        assertTrue(hash.matches("^[0-9a-f]{128}$"), "Mã hash HmacSHA512 phải là chuỗi hex 128 ký tự chữ thường.");
    }

    @Test
    @DisplayName("UT-UUT09-004: hmacSHA512 - Tính nhất quán kết quả hash (Deterministic)")
    void hmacSHA512_deterministicOutput() {
        String key = "secret_key_abc";
        String data = "test_data_xyz";

        String hash1 = VNPayConfig.hmacSHA512(key, data);
        String hash2 = VNPayConfig.hmacSHA512(key, data);

        assertEquals(hash1, hash2);
    }

    @Test
    @DisplayName("UT-UUT09-005: hmacSHA512 - Dữ liệu rỗng key='' phát sinh exception crypto và trả về 'Something wrong here'")
    void hmacSHA512_emptyKey_returnsErrorMessage() {
        String hash = VNPayConfig.hmacSHA512("", "someData");
        assertEquals("Something wrong here", hash);
    }

    // =========================================================================
    // Group 3: getIpAddress(HttpServletRequest)
    // =========================================================================

    @Test
    @DisplayName("UT-UUT09-006: getIpAddress - Header X-FORWARDED-FOR hợp lệ")
    void getIpAddress_validXForwardedFor_returnsHeaderValue() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader("X-FORWARDED-FOR")).thenReturn("203.162.1.1");

        String ip = VNPayConfig.getIpAddress(request);

        assertEquals("203.162.1.1", ip);
    }

    @Test
    @DisplayName("UT-UUT09-007: getIpAddress - Header X-FORWARDED-FOR bị null fallback getRemoteAddr")
    void getIpAddress_nullXForwardedFor_returnsRemoteAddr() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader("X-FORWARDED-FOR")).thenReturn(null);
        when(request.getRemoteAddr()).thenReturn("192.168.1.50");

        String ip = VNPayConfig.getIpAddress(request);

        assertEquals("192.168.1.50", ip);
    }

    @Test
    @DisplayName("UT-UUT09-008: getIpAddress - Header X-FORWARDED-FOR rỗng fallback getRemoteAddr")
    void getIpAddress_emptyXForwardedFor_returnsRemoteAddr() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader("X-FORWARDED-FOR")).thenReturn("");
        when(request.getRemoteAddr()).thenReturn("192.168.1.50");

        String ip = VNPayConfig.getIpAddress(request);

        assertEquals("192.168.1.50", ip);
    }

    @Test
    @DisplayName("UT-UUT09-009: getIpAddress - Header X-FORWARDED-FOR là 'unknown' fallback getRemoteAddr")
    void getIpAddress_unknownXForwardedFor_returnsRemoteAddr() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader("X-FORWARDED-FOR")).thenReturn("UNKNOWN");
        when(request.getRemoteAddr()).thenReturn("192.168.1.50");

        String ip = VNPayConfig.getIpAddress(request);

        assertEquals("192.168.1.50", ip);
    }

    @Test
    @DisplayName("UT-UUT09-010: getIpAddress - IPv6 loopback 0:0:0:0:0:0:0:1 chuyển thành 127.0.0.1")
    void getIpAddress_ipv6Loopback_returnsLocalhostIp() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader("X-FORWARDED-FOR")).thenReturn(null);
        when(request.getRemoteAddr()).thenReturn("0:0:0:0:0:0:0:1");

        String ip = VNPayConfig.getIpAddress(request);

        assertEquals("127.0.0.1", ip);
    }

    @Test
    @DisplayName("UT-UUT09-011: getIpAddress - Cả X-FORWARDED-FOR và getRemoteAddr đều null/rỗng")
    void getIpAddress_allNullOrEmpty_returnsDefaultLocalhost() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader("X-FORWARDED-FOR")).thenReturn(null);
        when(request.getRemoteAddr()).thenReturn(null);

        String ip = VNPayConfig.getIpAddress(request);

        assertEquals("127.0.0.1", ip);
    }

    @Test
    @DisplayName("UT-UUT09-012: getIpAddress - request gặp ngoại lệ trả về 127.0.0.1")
    void getIpAddress_requestThrowsException_returnsDefaultLocalhost() {
        String ipNullRequest = VNPayConfig.getIpAddress(null);
        assertEquals("127.0.0.1", ipNullRequest);

        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader("X-FORWARDED-FOR")).thenThrow(new RuntimeException("Servlet container error"));

        String ipException = VNPayConfig.getIpAddress(request);
        assertEquals("127.0.0.1", ipException);
    }

    // =========================================================================
    // Group 4: getRandomNumber(int len)
    // =========================================================================

    @Test
    @DisplayName("UT-UUT09-013: getRandomNumber - Tạo chuỗi số ngẫu nhiên với độ dài len = 8")
    void getRandomNumber_validLength_returnsNumericString() {
        String numStr = VNPayConfig.getRandomNumber(8);

        assertNotNull(numStr);
        assertEquals(8, numStr.length());
        assertTrue(numStr.matches("^\\d{8}$"), "Mã ngẫu nhiên phải chứa đúng 8 chữ số.");
    }

    @Test
    @DisplayName("UT-UUT09-014: getRandomNumber - Độ dài len = 0 trả về chuỗi rỗng")
    void getRandomNumber_zeroLength_returnsEmptyString() {
        String numStr = VNPayConfig.getRandomNumber(0);

        assertNotNull(numStr);
        assertEquals(0, numStr.length());
        assertEquals("", numStr);
    }

    @Test
    @DisplayName("UT-UUT09-015: getRandomNumber - Độ dài len < 0 ném NegativeArraySizeException")
    void getRandomNumber_negativeLength_throwsException() {
        assertThrows(NegativeArraySizeException.class, () -> VNPayConfig.getRandomNumber(-5));
    }
}
