package com.example.bonsai_shop.moderator.util;

import com.example.bonsai_shop.finance.enums.FaultParty;
import com.example.bonsai_shop.finance.enums.FinancialLedgerDirection;
import com.example.bonsai_shop.finance.enums.FinancialLedgerStatus;
import com.example.bonsai_shop.finance.enums.FinancialLedgerType;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;

import static org.junit.jupiter.api.Assertions.*;

class ModeratorDisplayLabelMapperTest {

    // =========================================================================
    // Group 1: orderStatusLabel
    // =========================================================================

    @Test
    @DisplayName("UT-UUT13-001: orderStatusLabel - Map đúng tất cả các giá trị trạng thái đơn hàng hợp lệ")
    void orderStatusLabel_validValues_returnsCorrectLabels() {
        assertEquals("Chờ kiểm duyệt", ModeratorDisplayLabelMapper.orderStatusLabel("PENDING"));
        assertEquals("Chờ kiểm duyệt", ModeratorDisplayLabelMapper.orderStatusLabel("pending"));
        assertEquals("Chờ kiểm duyệt", ModeratorDisplayLabelMapper.orderStatusLabel("WAITING_APPROVAL"));
        assertEquals("Chờ khách thanh toán", ModeratorDisplayLabelMapper.orderStatusLabel("PENDING_PAYMENT"));
        assertEquals("Chờ khách thanh toán", ModeratorDisplayLabelMapper.orderStatusLabel("WAITING_CUSTOMER_PAYMENT"));
        assertEquals("Đã thanh toán tiền đặt cọc", ModeratorDisplayLabelMapper.orderStatusLabel("DEPOSITED"));
        assertEquals("Đã thanh toán toàn bộ", ModeratorDisplayLabelMapper.orderStatusLabel("PAID"));
        assertEquals("Đã hoàn thành", ModeratorDisplayLabelMapper.orderStatusLabel("COMPLETED"));
        assertEquals("Đã chấm dứt đơn hàng", ModeratorDisplayLabelMapper.orderStatusLabel("CANCELLED"));
        assertEquals("Chờ thanh toán số tiền còn lại",
                ModeratorDisplayLabelMapper.orderStatusLabel("WAITING_DELIVERY_PAYMENT"));
    }

    @Test
    @DisplayName("UT-UUT13-002: orderStatusLabel - Value null hoặc blank -> Trả về '-'")
    void orderStatusLabel_nullOrBlank_returnsHyphen() {
        assertEquals("-", ModeratorDisplayLabelMapper.orderStatusLabel(null));
        assertEquals("-", ModeratorDisplayLabelMapper.orderStatusLabel(""));
        assertEquals("-", ModeratorDisplayLabelMapper.orderStatusLabel("   "));
    }

    @Test
    @DisplayName("UT-UUT13-003: orderStatusLabel - Value không xác định -> Trả về nguyên văn string")
    void orderStatusLabel_unknownValue_returnsOriginalValue() {
        assertEquals("UNKNOWN_STATUS", ModeratorDisplayLabelMapper.orderStatusLabel("UNKNOWN_STATUS"));
        assertEquals("custom_value", ModeratorDisplayLabelMapper.orderStatusLabel("custom_value"));
    }

    // =========================================================================
    // Group 2: paymentTypeLabel
    // =========================================================================

    @Test
    @DisplayName("UT-UUT13-004: paymentTypeLabel - Map đúng các loại thanh toán hợp lệ")
    void paymentTypeLabel_validValues_returnsCorrectLabels() {
        assertEquals("Thanh toán tiền đặt cọc", ModeratorDisplayLabelMapper.paymentTypeLabel("DEPOSIT"));
        assertEquals("Thanh toán tiền đặt cọc", ModeratorDisplayLabelMapper.paymentTypeLabel("deposit"));
        assertEquals("Thanh toán toàn bộ đơn hàng", ModeratorDisplayLabelMapper.paymentTypeLabel("FULL_PAYMENT"));
        assertEquals("Thanh toán số tiền còn lại", ModeratorDisplayLabelMapper.paymentTypeLabel("REMAINING_PAYMENT"));
    }

    @Test
    @DisplayName("UT-UUT13-005: paymentTypeLabel - Value null/blank -> '-' hoặc unknown -> nguyên văn")
    void paymentTypeLabel_nullOrUnknown_returnsFallback() {
        assertEquals("-", ModeratorDisplayLabelMapper.paymentTypeLabel(null));
        assertEquals("-", ModeratorDisplayLabelMapper.paymentTypeLabel("   "));
        assertEquals("INVALID_TYPE", ModeratorDisplayLabelMapper.paymentTypeLabel("INVALID_TYPE"));
    }

    // =========================================================================
    // Group 3: paymentMethodLabel
    // =========================================================================

    @Test
    @DisplayName("UT-UUT13-006: paymentMethodLabel - Map đúng các phương thức thanh toán hợp lệ")
    void paymentMethodLabel_validValues_returnsCorrectLabels() {
        assertEquals("Thanh toán trực tuyến qua VNPay", ModeratorDisplayLabelMapper.paymentMethodLabel("VNPAY"));
        assertEquals("Thanh toán tiền mặt", ModeratorDisplayLabelMapper.paymentMethodLabel("CASH"));
        assertEquals("Đặt cọc trước, thanh toán phần còn lại khi nhận cây",
                ModeratorDisplayLabelMapper.paymentMethodLabel("DEPOSIT"));
        assertEquals("Đặt cọc trước, thanh toán phần còn lại khi nhận cây",
                ModeratorDisplayLabelMapper.paymentMethodLabel("COD"));
        assertEquals("Chuyển khoản ngân hàng", ModeratorDisplayLabelMapper.paymentMethodLabel("BANK_TRANSFER"));
    }

    @Test
    @DisplayName("UT-UUT13-007: paymentMethodLabel - Value null/blank -> '-' hoặc unknown -> nguyên văn")
    void paymentMethodLabel_nullOrUnknown_returnsFallback() {
        assertEquals("-", ModeratorDisplayLabelMapper.paymentMethodLabel(null));
        assertEquals("-", ModeratorDisplayLabelMapper.paymentMethodLabel(""));
        assertEquals("BITCOIN", ModeratorDisplayLabelMapper.paymentMethodLabel("BITCOIN"));
    }

    // =========================================================================
    // Group 4: paymentStatusLabel
    // =========================================================================

    @Test
    @DisplayName("UT-UUT13-008: paymentStatusLabel - Map đúng các trạng thái thanh toán hợp lệ")
    void paymentStatusLabel_validValues_returnsCorrectLabels() {
        assertEquals("Chờ thanh toán", ModeratorDisplayLabelMapper.paymentStatusLabel("PENDING"));
        assertEquals("Thanh toán thành công", ModeratorDisplayLabelMapper.paymentStatusLabel("SUCCESS"));
        assertEquals("Thanh toán thành công", ModeratorDisplayLabelMapper.paymentStatusLabel("PAID"));
        assertEquals("Thanh toán thành công", ModeratorDisplayLabelMapper.paymentStatusLabel("COMPLETED"));
        assertEquals("Thanh toán thất bại", ModeratorDisplayLabelMapper.paymentStatusLabel("FAILED"));
        assertEquals("Đã hết hạn thanh toán", ModeratorDisplayLabelMapper.paymentStatusLabel("EXPIRED"));
        assertEquals("Đã hủy giao dịch", ModeratorDisplayLabelMapper.paymentStatusLabel("CANCELLED"));
    }

    @Test
    @DisplayName("UT-UUT13-009: paymentStatusLabel - Value null/blank -> '-' hoặc unknown -> nguyên văn")
    void paymentStatusLabel_nullOrUnknown_returnsFallback() {
        assertEquals("-", ModeratorDisplayLabelMapper.paymentStatusLabel(null));
        assertEquals("-", ModeratorDisplayLabelMapper.paymentStatusLabel("  "));
        assertEquals("REFUNDED", ModeratorDisplayLabelMapper.paymentStatusLabel("REFUNDED"));
    }

    // =========================================================================
    // Group 5: financialLedgerTypeLabel
    // =========================================================================

    @Test
    @DisplayName("UT-UUT13-010: financialLedgerTypeLabel - Map đúng tất cả Enum values của FinancialLedgerType")
    void financialLedgerTypeLabel_validEnums_returnsCorrectLabels() {
        assertEquals("Doanh thu từ đơn hàng đã hoàn thành",
                ModeratorDisplayLabelMapper.financialLedgerTypeLabel(FinancialLedgerType.COMPLETED_ORDER_REVENUE));
        assertEquals("Thu nhập từ tiền đặt cọc do khách bỏ đơn",
                ModeratorDisplayLabelMapper.financialLedgerTypeLabel(FinancialLedgerType.FORFEITED_DEPOSIT_INCOME));
        assertEquals("Hoàn lại toàn bộ tiền cho khách",
                ModeratorDisplayLabelMapper.financialLedgerTypeLabel(FinancialLedgerType.FULL_REFUND));
    }

    @Test
    @DisplayName("UT-UUT13-011: financialLedgerTypeLabel - Value enum null -> Trả về '-'")
    void financialLedgerTypeLabel_nullEnum_returnsHyphen() {
        assertEquals("-", ModeratorDisplayLabelMapper.financialLedgerTypeLabel(null));
    }

    // =========================================================================
    // Group 6: financialLedgerDirectionLabel
    // =========================================================================

    @Test
    @DisplayName("UT-UUT13-012: financialLedgerDirectionLabel - Map đúng tất cả Enum values của FinancialLedgerDirection")
    void financialLedgerDirectionLabel_validEnums_returnsCorrectLabels() {
        assertEquals("Khoản thu",
                ModeratorDisplayLabelMapper.financialLedgerDirectionLabel(FinancialLedgerDirection.INCOME));
        assertEquals("Khoản hoàn/chi ra",
                ModeratorDisplayLabelMapper.financialLedgerDirectionLabel(FinancialLedgerDirection.OUTFLOW));
    }

    @Test
    @DisplayName("UT-UUT13-013: financialLedgerDirectionLabel - Value enum null -> Trả về '-'")
    void financialLedgerDirectionLabel_nullEnum_returnsHyphen() {
        assertEquals("-", ModeratorDisplayLabelMapper.financialLedgerDirectionLabel(null));
    }

    // =========================================================================
    // Group 7: financialLedgerStatusLabel
    // =========================================================================

    @Test
    @DisplayName("UT-UUT13-014: financialLedgerStatusLabel - Map đúng tất cả Enum values của FinancialLedgerStatus")
    void financialLedgerStatusLabel_validEnums_returnsCorrectLabels() {
        assertEquals("Đã ghi nhận",
                ModeratorDisplayLabelMapper.financialLedgerStatusLabel(FinancialLedgerStatus.RECORDED));
        assertEquals("Đã hủy bản ghi",
                ModeratorDisplayLabelMapper.financialLedgerStatusLabel(FinancialLedgerStatus.VOIDED));
    }

    @Test
    @DisplayName("UT-UUT13-015: financialLedgerStatusLabel - Value enum null -> Trả về '-'")
    void financialLedgerStatusLabel_nullEnum_returnsHyphen() {
        assertEquals("-", ModeratorDisplayLabelMapper.financialLedgerStatusLabel(null));
    }

    // =========================================================================
    // Group 8: faultPartyLabel
    // =========================================================================

    @Test
    @DisplayName("UT-UUT13-016: faultPartyLabel - Map đúng tất cả Enum values của FaultParty")
    void faultPartyLabel_validEnums_returnsCorrectLabels() {
        assertEquals("Lỗi từ phía khách hàng", ModeratorDisplayLabelMapper.faultPartyLabel(FaultParty.CUSTOMER));
        assertEquals("Lỗi từ phía nhà vườn", ModeratorDisplayLabelMapper.faultPartyLabel(FaultParty.NURSERY));
        assertEquals("Lỗi trong quá trình vận chuyển",
                ModeratorDisplayLabelMapper.faultPartyLabel(FaultParty.DELIVERY));
        assertEquals("Nguyên nhân khác", ModeratorDisplayLabelMapper.faultPartyLabel(FaultParty.OTHER));
    }

    @Test
    @DisplayName("UT-UUT13-017: faultPartyLabel - Value enum null -> Trả về '-'")
    void faultPartyLabel_nullEnum_returnsHyphen() {
        assertEquals("-", ModeratorDisplayLabelMapper.faultPartyLabel(null));
    }

    // =========================================================================
    // Group 9: priorityLabel
    // =========================================================================

    @Test
    @DisplayName("UT-UUT13-018: priorityLabel - Map đúng tất cả các độ ưu tiên hợp lệ")
    void priorityLabel_validValues_returnsCorrectLabels() {
        assertEquals("Rất khẩn cấp", ModeratorDisplayLabelMapper.priorityLabel("CRITICAL"));
        assertEquals("Ưu tiên cao", ModeratorDisplayLabelMapper.priorityLabel("HIGH"));
        assertEquals("Ưu tiên trung bình", ModeratorDisplayLabelMapper.priorityLabel("MEDIUM"));
        assertEquals("Bình thường", ModeratorDisplayLabelMapper.priorityLabel("NORMAL"));
        assertEquals("Ưu tiên thấp", ModeratorDisplayLabelMapper.priorityLabel("LOW"));
    }

    @Test
    @DisplayName("UT-UUT13-019: priorityLabel - Value null/blank -> '-' hoặc unknown -> nguyên văn")
    void priorityLabel_nullOrUnknown_returnsFallback() {
        assertEquals("-", ModeratorDisplayLabelMapper.priorityLabel(null));
        assertEquals("-", ModeratorDisplayLabelMapper.priorityLabel(""));
        assertEquals("URGENT", ModeratorDisplayLabelMapper.priorityLabel("URGENT"));
    }

    // =========================================================================
    // Group 10: Utility Class Private Constructor
    // =========================================================================

    @Test
    @DisplayName("UT-UUT13-020: Private Constructor Coverage - Gọi constructor ẩn bằng Reflection")
    void privateConstructor_invokedViaReflection_createsInstance() throws Exception {
        Constructor<ModeratorDisplayLabelMapper> constructor = ModeratorDisplayLabelMapper.class
                .getDeclaredConstructor();
        assertTrue(java.lang.reflect.Modifier.isPrivate(constructor.getModifiers()));
        constructor.setAccessible(true);
        ModeratorDisplayLabelMapper instance = constructor.newInstance();
        assertNotNull(instance);
    }
}
