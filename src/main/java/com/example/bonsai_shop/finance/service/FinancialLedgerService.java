package com.example.bonsai_shop.finance.service;

import com.example.bonsai_shop.entity.FinancialLedger;
import com.example.bonsai_shop.entity.Order;
import com.example.bonsai_shop.entity.OrderDetail;
import com.example.bonsai_shop.entity.Payment;
import com.example.bonsai_shop.entity.User;
import com.example.bonsai_shop.finance.dto.FinancialLedgerDTO;
import com.example.bonsai_shop.finance.enums.FaultParty;
import com.example.bonsai_shop.finance.enums.FinancialLedgerDirection;
import com.example.bonsai_shop.finance.enums.FinancialLedgerStatus;
import com.example.bonsai_shop.finance.enums.FinancialLedgerType;
import com.example.bonsai_shop.finance.repository.FinancialLedgerRepository;
import com.example.bonsai_shop.moderator.util.ModeratorDisplayLabelMapper;
import com.example.bonsai_shop.product.enums.PaymentType;
import com.example.bonsai_shop.product.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class FinancialLedgerService {

    private static final BigDecimal ZERO = BigDecimal.ZERO;

    private final FinancialLedgerRepository financialLedgerRepository;
    private final PaymentRepository paymentRepository;

    @Transactional(readOnly = true)
    public Payment requireSuccessfulDepositPayment(Order order) {
        if (order == null || order.getOrderId() == null) {
            throw new IllegalArgumentException("Đơn hàng không hợp lệ.");
        }

        return paymentRepository.findByOrderOrderIdAndPaymentType(order.getOrderId(), PaymentType.DEPOSIT.name())
                .stream()
                .filter(this::isSuccessfulPayment)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Không tìm thấy giao dịch đặt cọc thành công của đơn hàng."));
    }

    @Transactional
    public FinancialLedger recordCompletedOrderRevenueIfAbsent(Order order, User actor, LocalDateTime recognizedAt) {
        requireOrder(order);
        requireActor(actor);
        if (financialLedgerRepository.existsByOrderOrderIdAndLedgerTypeAndLedgerStatus(
                order.getOrderId(),
                FinancialLedgerType.COMPLETED_ORDER_REVENUE,
                FinancialLedgerStatus.RECORDED
        )) {
            return null;
        }

        BigDecimal amount = calculateCompletedOrderRevenue(order);
        return financialLedgerRepository.save(FinancialLedger.builder()
                .order(order)
                .recordedBy(actor)
                .ledgerType(FinancialLedgerType.COMPLETED_ORDER_REVENUE)
                .direction(FinancialLedgerDirection.INCOME)
                .amount(amount)
                .reason("Order completed")
                .ledgerStatus(FinancialLedgerStatus.RECORDED)
                .recognizedAt(recognizedAt != null ? recognizedAt : LocalDateTime.now())
                .createdAt(LocalDateTime.now())
                .build());
    }

    @Transactional
    public FinancialLedger recordForfeitedDepositIncome(Order order, Payment depositPayment, BigDecimal amount,
                                                        String reason, User actor) {
        if (depositPayment == null || depositPayment.getPaymentId() == null) {
            throw new IllegalArgumentException("Không tìm thấy giao dịch đặt cọc để ghi nhận giữ cọc.");
        }
        if (hasRecordedDepositForfeiture(depositPayment)) {
            throw new IllegalStateException("Khoản tiền đặt cọc của đơn hàng này đã được xử lý trước đó.");
        }
        requireOrder(order);
        requireActor(actor);

        BigDecimal normalizedAmount = requirePositiveAmount(amount, "Số tiền giữ cọc");
        BigDecimal depositAmount = money(depositPayment.getAmount());
        if (normalizedAmount.compareTo(depositAmount) > 0) {
            throw new IllegalArgumentException("Số tiền giữ cọc không được vượt quá số tiền cọc đã thanh toán.");
        }

        return financialLedgerRepository.save(FinancialLedger.builder()
                .order(order)
                .relatedPayment(depositPayment)
                .recordedBy(actor)
                .ledgerType(FinancialLedgerType.FORFEITED_DEPOSIT_INCOME)
                .direction(FinancialLedgerDirection.INCOME)
                .amount(normalizedAmount)
                .faultParty(FaultParty.CUSTOMER)
                .reason(requireReason(reason))
                .ledgerStatus(FinancialLedgerStatus.RECORDED)
                .recognizedAt(LocalDateTime.now())
                .createdAt(LocalDateTime.now())
                .build());
    }

    @Transactional
    public FinancialLedger recordManualFaultRefund(Order order, FaultParty faultParty, BigDecimal amount,
                                                   String reason, String evidenceNote,
                                                   String externalReference, User actor) {
        requireOrder(order);
        if (faultParty != FaultParty.NURSERY && faultParty != FaultParty.DELIVERY) {
            throw new IllegalArgumentException("Bên chịu trách nhiệm khi hoàn tiền phải là nhà vườn hoặc quá trình vận chuyển.");
        }
        requireActor(actor);

        BigDecimal normalizedAmount = requirePositiveAmount(amount, "Số tiền hoàn");
        BigDecimal refundableAmount = calculateRefundableCash(order);
        if (normalizedAmount.compareTo(refundableAmount) > 0) {
            throw new IllegalArgumentException("Số tiền hoàn không được lớn hơn tổng số tiền khách đã thanh toán còn có thể hoàn.");
        }

        Payment relatedPayment = resolveRelatedRefundPayment(order);
        FinancialLedgerType type = normalizedAmount.compareTo(refundableAmount) == 0
                ? FinancialLedgerType.FULL_REFUND
                : FinancialLedgerType.PARTIAL_REFUND;

        return financialLedgerRepository.save(FinancialLedger.builder()
                .order(order)
                .relatedPayment(relatedPayment)
                .recordedBy(actor)
                .ledgerType(type)
                .direction(FinancialLedgerDirection.OUTFLOW)
                .amount(normalizedAmount)
                .faultParty(faultParty)
                .reason(requireReason(reason))
                .evidenceNote(blankToNull(evidenceNote))
                .externalReference(blankToNull(externalReference))
                .ledgerStatus(FinancialLedgerStatus.RECORDED)
                .recognizedAt(LocalDateTime.now())
                .createdAt(LocalDateTime.now())
                .build());
    }

    @Transactional(readOnly = true)
    public boolean hasRecordedDepositForfeiture(Payment depositPayment) {
        return depositPayment != null
                && depositPayment.getPaymentId() != null
                && financialLedgerRepository.existsByRelatedPaymentPaymentIdAndLedgerTypeAndLedgerStatus(
                depositPayment.getPaymentId(),
                FinancialLedgerType.FORFEITED_DEPOSIT_INCOME,
                FinancialLedgerStatus.RECORDED
        );
    }

    @Transactional(readOnly = true)
    public BigDecimal sumRecognizedCompletedRevenue(Order order) {
        return sumByType(order, FinancialLedgerType.COMPLETED_ORDER_REVENUE);
    }

    @Transactional(readOnly = true)
    public BigDecimal sumForfeitedDepositIncome(Order order) {
        return sumByType(order, FinancialLedgerType.FORFEITED_DEPOSIT_INCOME);
    }

    @Transactional(readOnly = true)
    public BigDecimal sumPartialRefunds(Order order) {
        return sumByType(order, FinancialLedgerType.PARTIAL_REFUND);
    }

    @Transactional(readOnly = true)
    public BigDecimal sumFullRefunds(Order order) {
        return sumByType(order, FinancialLedgerType.FULL_REFUND);
    }

    @Transactional(readOnly = true)
    public BigDecimal sumRecordedRefunds(Order order) {
        return sumPartialRefunds(order).add(sumFullRefunds(order));
    }

    @Transactional(readOnly = true)
    public BigDecimal sumNetRecognizedAmount(Order order) {
        return sumRecognizedCompletedRevenue(order)
                .add(sumForfeitedDepositIncome(order))
                .subtract(sumRecordedRefunds(order));
    }

    @Transactional(readOnly = true)
    public BigDecimal calculateRefundableCash(Order order) {
        if (order == null || order.getOrderId() == null) {
            return ZERO;
        }

        BigDecimal successfulCash = paymentRepository.findByOrderOrderIdOrderByPaymentIdAsc(order.getOrderId())
                .stream()
                .filter(this::isSuccessfulPayment)
                .map(Payment::getAmount)
                .map(this::money)
                .reduce(ZERO, BigDecimal::add);

        BigDecimal refunds = sumRecordedRefunds(order);
        BigDecimal refundable = successfulCash.subtract(refunds);
        return refundable.compareTo(ZERO) < 0 ? ZERO : refundable;
    }

    @Transactional(readOnly = true)
    public List<FinancialLedgerDTO> getLedgerHistory(Integer orderId) {
        if (orderId == null) {
            return List.of();
        }

        return financialLedgerRepository.findByOrderOrderIdOrderByRecognizedAtAscFinancialLedgerIdAsc(orderId)
                .stream()
                .map(this::toDTO)
                .toList();
    }

    public BigDecimal calculateCompletedOrderRevenue(Order order) {
        requireOrder(order);
        BigDecimal treeAmount = calculateTreeAmount(order);
        BigDecimal shippingFee = money(order.getShippingFee());
        BigDecimal craneFee = money(order.getCraneFee());
        return treeAmount.add(shippingFee).add(craneFee);
    }

    public BigDecimal calculateTreeAmount(Order order) {
        if (order == null || order.getOrderDetails() == null || order.getOrderDetails().isEmpty()) {
            return ZERO;
        }

        return order.getOrderDetails().stream()
                .map(this::calculateDetailAmount)
                .reduce(ZERO, BigDecimal::add);
    }

    private BigDecimal calculateDetailAmount(OrderDetail detail) {
        if (detail == null) {
            return ZERO;
        }
        BigDecimal price = money(detail.getPriceAtPurchase());
        int quantity = detail.getQuantity() != null ? detail.getQuantity() : 1;
        return price.multiply(BigDecimal.valueOf(quantity));
    }

    private BigDecimal sumByType(Order order, FinancialLedgerType type) {
        if (order == null || order.getOrderId() == null) {
            return ZERO;
        }

        return financialLedgerRepository
                .findByOrderOrderIdAndLedgerStatus(order.getOrderId(), FinancialLedgerStatus.RECORDED)
                .stream()
                .filter(log -> type == log.getLedgerType())
                .map(FinancialLedger::getAmount)
                .map(this::money)
                .reduce(ZERO, BigDecimal::add);
    }

    private Payment resolveRelatedRefundPayment(Order order) {
        List<Payment> payments = paymentRepository.findByOrderOrderIdOrderByPaymentIdAsc(order.getOrderId());

        return payments.stream()
                .filter(this::isSuccessfulPayment)
                .filter(payment -> PaymentType.FULL_PAYMENT.name().equalsIgnoreCase(payment.getPaymentType()))
                .findFirst()
                .orElseGet(() -> payments.stream()
                        .filter(this::isSuccessfulPayment)
                        .filter(payment -> PaymentType.REMAINING_PAYMENT.name().equalsIgnoreCase(payment.getPaymentType()))
                        .findFirst()
                        .orElseGet(() -> payments.stream()
                                .filter(this::isSuccessfulPayment)
                                .filter(payment -> PaymentType.DEPOSIT.name().equalsIgnoreCase(payment.getPaymentType()))
                                .findFirst()
                                .orElseGet(() -> payments.stream()
                                        .filter(this::isSuccessfulPayment)
                                        .findFirst()
                                        .orElse(null))));
    }

    private boolean isSuccessfulPayment(Payment payment) {
        return payment != null && "SUCCESS".equalsIgnoreCase(payment.getPaymentStatus());
    }

    private BigDecimal requirePositiveAmount(BigDecimal amount, String label) {
        BigDecimal normalized = money(amount);
        if (normalized.compareTo(ZERO) <= 0) {
            throw new IllegalArgumentException(label + " phải lớn hơn 0.");
        }
        return normalized;
    }

    private String requireReason(String reason) {
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("Vui lòng nhập lý do trước khi xác nhận.");
        }
        return reason.trim();
    }

    private void requireOrder(Order order) {
        if (order == null || order.getOrderId() == null) {
            throw new IllegalArgumentException("Đơn hàng không hợp lệ.");
        }
    }

    private void requireActor(User actor) {
        if (actor == null || actor.getUserId() == null) {
            throw new IllegalArgumentException("Người thực hiện là bắt buộc.");
        }
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private BigDecimal money(BigDecimal amount) {
        return amount != null ? amount : ZERO;
    }

    private FinancialLedgerDTO toDTO(FinancialLedger ledger) {
        return FinancialLedgerDTO.builder()
                .financialLedgerId(ledger.getFinancialLedgerId())
                .orderId(ledger.getOrder() != null ? ledger.getOrder().getOrderId() : null)
                .relatedPaymentId(ledger.getRelatedPayment() != null ? ledger.getRelatedPayment().getPaymentId() : null)
                .recordedById(ledger.getRecordedBy() != null ? ledger.getRecordedBy().getUserId() : null)
                .recordedByName(ledger.getRecordedBy() != null ? ledger.getRecordedBy().getFullName() : null)
                .ledgerType(ledger.getLedgerType())
                .ledgerTypeLabel(ModeratorDisplayLabelMapper.financialLedgerTypeLabel(ledger.getLedgerType()))
                .amount(ledger.getAmount())
                .direction(ledger.getDirection())
                .directionLabel(ModeratorDisplayLabelMapper.financialLedgerDirectionLabel(ledger.getDirection()))
                .faultParty(ledger.getFaultParty())
                .faultPartyLabel(ModeratorDisplayLabelMapper.faultPartyLabel(ledger.getFaultParty()))
                .reason(ledger.getReason())
                .evidenceNote(ledger.getEvidenceNote())
                .externalReference(ledger.getExternalReference())
                .ledgerStatus(ledger.getLedgerStatus())
                .ledgerStatusLabel(ModeratorDisplayLabelMapper.financialLedgerStatusLabel(ledger.getLedgerStatus()))
                .recognizedAt(ledger.getRecognizedAt())
                .createdAt(ledger.getCreatedAt())
                .build();
    }
}
