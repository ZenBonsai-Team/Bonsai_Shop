package com.example.bonsai_shop.finance.dto;

import com.example.bonsai_shop.finance.enums.FaultParty;
import com.example.bonsai_shop.finance.enums.FinancialLedgerDirection;
import com.example.bonsai_shop.finance.enums.FinancialLedgerStatus;
import com.example.bonsai_shop.finance.enums.FinancialLedgerType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FinancialLedgerDTO {
    private Integer financialLedgerId;
    private Integer orderId;
    private Integer relatedPaymentId;
    private Integer recordedById;
    private String recordedByName;
    private FinancialLedgerType ledgerType;
    private String ledgerTypeLabel;
    private BigDecimal amount;
    private FinancialLedgerDirection direction;
    private String directionLabel;
    private FaultParty faultParty;
    private String faultPartyLabel;
    private String reason;
    private String evidenceNote;
    private String externalReference;
    private FinancialLedgerStatus ledgerStatus;
    private String ledgerStatusLabel;
    private LocalDateTime recognizedAt;
    private LocalDateTime createdAt;
}
