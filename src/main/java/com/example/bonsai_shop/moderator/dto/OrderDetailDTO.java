package com.example.bonsai_shop.moderator.dto;

import com.example.bonsai_shop.finance.dto.FinancialLedgerDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * [DTO TỔNG HỢP CHI TIẾT ĐƠN HÀNG TOÀN DIỆN CHO MODERATOR - ORDER DETAIL DTO]
 *
 * Mục đích:
 * - Tập hợp toàn bộ thông tin về đơn hàng: Thông tin khách hàng, danh sách cây bonsai, tóm tắt tài chính, lịch sử thanh toán, dòng thời gian timeline, lịch sử xử lý của moderator, và các cờ phân quyền hành động (canApprove, canReject, canClaim, canReturnInventory, canComplete, canCustomerNoShow, canRecordFaultRefund).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderDetailDTO {
    private Integer orderId;
    private String orderCode;
    private String orderStatus;
    private String orderStatusLabel;
    private String paymentMethod;
    private String paymentMethodLabel;
    private String priority;
    private String priorityLabel;
    private String orderType;
    private LocalDateTime createdDate;
    private String assignedModeratorName;
    private LocalDateTime statusTimestamp;
    private String ageFormatted;
    private String currentStage;
    private String notes;
    private String internalNote;

    private boolean canApprove;
    private boolean canReject;
    private boolean canClaim;
    private boolean canUnclaim;
    private boolean canReturnInventory;
    private boolean canComplete;
    private boolean canCancel;
    private boolean canCustomerNoShow;
    private boolean canRecordFaultRefund;

    private CustomerInfoDTO customerInfo;
    private List<ProductSummaryDTO> products;
    private PaymentSummaryDTO paymentSummary;
    private List<PaymentHistoryDTO> paymentHistory;
    private List<FinancialLedgerDTO> ledgerHistory;
    private List<TimelineDTO> timeline;
    private List<HandlingHistoryDTO> handlingHistory;
}
