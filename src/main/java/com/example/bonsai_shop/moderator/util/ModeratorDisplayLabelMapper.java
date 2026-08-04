package com.example.bonsai_shop.moderator.util;

import com.example.bonsai_shop.finance.enums.FaultParty;
import com.example.bonsai_shop.finance.enums.FinancialLedgerDirection;
import com.example.bonsai_shop.finance.enums.FinancialLedgerStatus;
import com.example.bonsai_shop.finance.enums.FinancialLedgerType;

public final class ModeratorDisplayLabelMapper {

    private ModeratorDisplayLabelMapper() {
    }

    public static String orderStatusLabel(String value) {
        return switch (normalize(value)) {
            case "PENDING" -> "Chờ kiểm duyệt";
            case "PENDING_PAYMENT" -> "Chờ khách thanh toán";
            case "DEPOSITED" -> "Đã thanh toán tiền đặt cọc";
            case "PAID" -> "Đã thanh toán toàn bộ";
            case "COMPLETED" -> "Đã hoàn thành";
            case "CANCELLED" -> "Đã chấm dứt đơn hàng";
            case "WAITING_APPROVAL" -> "Chờ kiểm duyệt";
            case "WAITING_CUSTOMER_PAYMENT" -> "Chờ khách thanh toán";
            case "WAITING_DELIVERY_PAYMENT" -> "Chờ thanh toán số tiền còn lại";
            default -> fallback(value);
        };
    }

    public static String paymentTypeLabel(String value) {
        return switch (normalize(value)) {
            case "DEPOSIT" -> "Thanh toán tiền đặt cọc";
            case "FULL_PAYMENT" -> "Thanh toán toàn bộ đơn hàng";
            case "REMAINING_PAYMENT" -> "Thanh toán số tiền còn lại";
            default -> fallback(value);
        };
    }

    public static String paymentMethodLabel(String value) {
        return switch (normalize(value)) {
            case "VNPAY" -> "Thanh toán trực tuyến qua VNPay";
            case "CASH" -> "Thanh toán tiền mặt";
            case "DEPOSIT", "COD" -> "Đặt cọc trước, thanh toán phần còn lại khi nhận cây";
            case "BANK_TRANSFER" -> "Chuyển khoản ngân hàng";
            default -> fallback(value);
        };
    }

    public static String paymentStatusLabel(String value) {
        return switch (normalize(value)) {
            case "PENDING" -> "Chờ thanh toán";
            case "SUCCESS", "PAID", "COMPLETED" -> "Thanh toán thành công";
            case "FAILED" -> "Thanh toán thất bại";
            case "EXPIRED" -> "Đã hết hạn thanh toán";
            case "CANCELLED" -> "Đã hủy giao dịch";
            default -> fallback(value);
        };
    }

    public static String financialLedgerTypeLabel(FinancialLedgerType value) {
        if (value == null) {
            return "-";
        }
        return switch (value) {
            case COMPLETED_ORDER_REVENUE -> "Doanh thu từ đơn hàng đã hoàn thành";
            case FORFEITED_DEPOSIT_INCOME -> "Thu nhập từ tiền đặt cọc do khách bỏ đơn";
            case PARTIAL_REFUND -> "Hoàn lại một phần tiền cho khách";
            case FULL_REFUND -> "Hoàn lại toàn bộ tiền cho khách";
        };
    }

    public static String financialLedgerDirectionLabel(FinancialLedgerDirection value) {
        if (value == null) {
            return "-";
        }
        return switch (value) {
            case INCOME -> "Khoản thu";
            case OUTFLOW -> "Khoản hoàn/chi ra";
        };
    }

    public static String financialLedgerStatusLabel(FinancialLedgerStatus value) {
        if (value == null) {
            return "-";
        }
        return switch (value) {
            case RECORDED -> "Đã ghi nhận";
            case VOIDED -> "Đã hủy bản ghi";
        };
    }

    public static String faultPartyLabel(FaultParty value) {
        if (value == null) {
            return "-";
        }
        return switch (value) {
            case CUSTOMER -> "Lỗi từ phía khách hàng";
            case NURSERY -> "Lỗi từ phía nhà vườn";
            case DELIVERY -> "Lỗi trong quá trình vận chuyển";
            case OTHER -> "Nguyên nhân khác";
        };
    }



    public static String priorityLabel(String value) {
        return switch (normalize(value)) {
            case "CRITICAL" -> "Rất khẩn cấp";
            case "HIGH" -> "Ưu tiên cao";
            case "MEDIUM" -> "Ưu tiên trung bình";
            case "NORMAL" -> "Bình thường";
            case "LOW" -> "Ưu tiên thấp";
            default -> fallback(value);
        };
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toUpperCase();
    }

    private static String fallback(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }
}
