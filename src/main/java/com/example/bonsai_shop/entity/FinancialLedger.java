package com.example.bonsai_shop.entity;

import com.example.bonsai_shop.finance.enums.FaultParty;
import com.example.bonsai_shop.finance.enums.FinancialLedgerDirection;
import com.example.bonsai_shop.finance.enums.FinancialLedgerStatus;
import com.example.bonsai_shop.finance.enums.FinancialLedgerType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "financial_ledger")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FinancialLedger {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "FinancialLedgerID")
    private Integer financialLedgerId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "OrderID", nullable = false)
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "RelatedPaymentID")
    private Payment relatedPayment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "RecordedByID", nullable = false)
    private User recordedBy;

    @Enumerated(EnumType.STRING)
    @Column(name = "LedgerType", nullable = false, length = 50)
    private FinancialLedgerType ledgerType;

    @Column(name = "Amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "Direction", nullable = false, length = 20)
    private FinancialLedgerDirection direction;

    @Enumerated(EnumType.STRING)
    @Column(name = "FaultParty", length = 30)
    private FaultParty faultParty;

    @Column(name = "Reason", length = 1000)
    private String reason;

    @Column(name = "EvidenceNote", length = 1000)
    private String evidenceNote;

    @Column(name = "ExternalReference", length = 255)
    private String externalReference;

    @Enumerated(EnumType.STRING)
    @Column(name = "LedgerStatus", nullable = false, length = 30)
    @Builder.Default
    private FinancialLedgerStatus ledgerStatus = FinancialLedgerStatus.RECORDED;

    @Column(name = "RecognizedAt", nullable = false)
    @Builder.Default
    private LocalDateTime recognizedAt = LocalDateTime.now();

    @Column(name = "CreatedAt", nullable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
