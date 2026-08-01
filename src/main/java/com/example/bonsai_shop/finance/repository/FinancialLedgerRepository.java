package com.example.bonsai_shop.finance.repository;

import com.example.bonsai_shop.entity.FinancialLedger;
import com.example.bonsai_shop.finance.enums.FinancialLedgerStatus;
import com.example.bonsai_shop.finance.enums.FinancialLedgerType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FinancialLedgerRepository extends JpaRepository<FinancialLedger, Integer> {

    List<FinancialLedger> findByOrderOrderIdOrderByRecognizedAtAscFinancialLedgerIdAsc(Integer orderId);

    List<FinancialLedger> findByOrderOrderIdAndLedgerStatus(Integer orderId, FinancialLedgerStatus ledgerStatus);

    boolean existsByOrderOrderIdAndLedgerTypeAndLedgerStatus(Integer orderId,
                                                            FinancialLedgerType ledgerType,
                                                            FinancialLedgerStatus ledgerStatus);

    boolean existsByRelatedPaymentPaymentIdAndLedgerTypeAndLedgerStatus(Integer relatedPaymentId,
                                                                        FinancialLedgerType ledgerType,
                                                                        FinancialLedgerStatus ledgerStatus);
}
