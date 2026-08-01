package com.example.bonsai_shop.moderator.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderDetailDTO {
    private Integer orderId;
    private String orderCode;
    private String orderStatus;
    private String paymentMethod;
    private String priority;
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

    private CustomerInfoDTO customerInfo;
    private List<ProductSummaryDTO> products;
    private PaymentSummaryDTO paymentSummary;
    private List<PaymentHistoryDTO> paymentHistory;
    private List<TimelineDTO> timeline;
    private List<HandlingHistoryDTO> handlingHistory;
}
