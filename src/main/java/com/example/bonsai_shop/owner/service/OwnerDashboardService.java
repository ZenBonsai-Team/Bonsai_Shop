package com.example.bonsai_shop.owner.service;

import com.example.bonsai_shop.entity.Product;
import com.example.bonsai_shop.finance.enums.FinancialLedgerStatus;
import com.example.bonsai_shop.finance.enums.FinancialLedgerType;
import com.example.bonsai_shop.finance.repository.FinancialLedgerRepository;
import com.example.bonsai_shop.owner.dto.OwnerArtisanRevenueDTO;
import com.example.bonsai_shop.owner.dto.OwnerDashboardDTO;
import com.example.bonsai_shop.owner.dto.OwnerGardenTreeDTO;
import com.example.bonsai_shop.owner.dto.OwnerMonthlyRevenueDTO;
import com.example.bonsai_shop.owner.dto.OwnerSoldTreeDTO;
import com.example.bonsai_shop.owner.dto.OwnerTopViewedTreeDTO;
import com.example.bonsai_shop.product.repository.OrderDetailRepository;
import com.example.bonsai_shop.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class OwnerDashboardService {

    private static final DateTimeFormatter MONTH_KEY_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM");
    private static final DateTimeFormatter MONTH_LABEL_FORMATTER = DateTimeFormatter.ofPattern("MM/yyyy");

    private final FinancialLedgerRepository financialLedgerRepository;
    private final ProductRepository productRepository;
    private final OrderDetailRepository orderDetailRepository;

    @Transactional(readOnly = true)
    public OwnerDashboardDTO getDashboard() {
        YearMonth currentMonth = YearMonth.now();
        LocalDateTime monthStart = currentMonth.atDay(1).atStartOfDay(); // tạo thời điểm tháng hiện tại
        LocalDateTime nextMonthStart = monthStart.plusMonths(1); // xác định thời điểm bắt đầu tháng tiếp theo để tính doanh thu tháng hiện tại
        YearMonth firstChartMonth = currentMonth.minusMonths(11); // xác định 12 tháng cho biểu đồ

        List<OwnerMonthlyRevenueDTO> monthlyRevenue = buildMonthlyRevenue(
                firstChartMonth,
                nextMonthStart
        );

        List<OwnerTopViewedTreeDTO> topViewedTrees = productRepository.findTopViewedTrees(PageRequest.of(0, 5))
                .stream()
                .map(this::toTopViewedTree)
                .toList();

        return new OwnerDashboardDTO(
                normalize(financialLedgerRepository.sumNetRevenue(FinancialLedgerStatus.RECORDED)),
                normalize(financialLedgerRepository.sumNetRevenueBetween(
                        FinancialLedgerStatus.RECORDED,
                        monthStart,
                        nextMonthStart
                )),
                productRepository.countTreesInGarden(),
                productRepository.countByProductStatus("SOLD"),
                monthlyRevenue,
                topViewedTrees
        );
    }

    @Transactional(readOnly = true)
    public List<OwnerSoldTreeDTO> getSoldTrees() {
        return orderDetailRepository.findOwnerSoldTrees();
    }

    @Transactional(readOnly = true)
    public List<OwnerGardenTreeDTO> getGardenTrees() {
        return productRepository.findOwnerGardenTrees();
    }

    @Transactional(readOnly = true)
    public List<OwnerArtisanRevenueDTO> getCurrentMonthRevenueByArtisan() {
        YearMonth currentMonth = YearMonth.now();
        LocalDateTime monthStart = currentMonth.atDay(1).atStartOfDay();
        return orderDetailRepository.findCurrentMonthRevenueByArtisan(
                FinancialLedgerType.COMPLETED_ORDER_REVENUE,
                FinancialLedgerStatus.RECORDED,
                monthStart,
                monthStart.plusMonths(1)
        );
    }

    public String getCurrentMonthLabel() {
        return YearMonth.now().format(MONTH_LABEL_FORMATTER);
    }

    private List<OwnerMonthlyRevenueDTO> buildMonthlyRevenue(YearMonth firstMonth, LocalDateTime nextMonthStart) {
        LocalDateTime chartStart = firstMonth.atDay(1).atStartOfDay();
        Map<YearMonth, BigDecimal> revenueByMonth = new LinkedHashMap<>();
        for (int i = 0; i < 12; i++) {
            revenueByMonth.put(firstMonth.plusMonths(i), BigDecimal.ZERO);
        }

        financialLedgerRepository.findMonthlyNetRevenueBetween(chartStart, nextMonthStart)
                .forEach(row -> {
                    YearMonth month = YearMonth.parse(String.valueOf(row[0]), MONTH_KEY_FORMATTER);
                    revenueByMonth.put(month, normalize(toBigDecimal(row[1])));
                });

        BigDecimal maxRevenue = revenueByMonth.values().stream()
                .map(this::zeroIfNegative)
                .max(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);

        return revenueByMonth.entrySet()
                .stream()
                .map(entry -> new OwnerMonthlyRevenueDTO(
                        entry.getKey().format(MONTH_LABEL_FORMATTER),
                        entry.getValue(),
                        chartPercent(entry.getValue(), maxRevenue)
                ))
                .toList();
    }

    private OwnerTopViewedTreeDTO toTopViewedTree(Product product) {
        return new OwnerTopViewedTreeDTO(
                product.getProductId(),
                product.getProductCode(),
                product.getProductName(),
                product.getVariety() != null ? product.getVariety().getVarietyName() : null,
                product.getProductStatus(),
                product.getPrice(),
                product.getViewCount() != null ? product.getViewCount() : 0
        );
    }

    private int chartPercent(BigDecimal amount, BigDecimal maxRevenue) {
        BigDecimal normalizedAmount = zeroIfNegative(amount);
        if (maxRevenue.compareTo(BigDecimal.ZERO) <= 0 || normalizedAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return 0;
        }
        return normalizedAmount
                .multiply(BigDecimal.valueOf(100))
                .divide(maxRevenue, 0, RoundingMode.HALF_UP)
                .max(BigDecimal.valueOf(6))
                .intValue();
    }

    private BigDecimal toBigDecimal(Object value) {
        if (value == null) {
            return BigDecimal.ZERO;
        }
        if (value instanceof BigDecimal decimal) {
            return decimal;
        }
        if (value instanceof Number number) {
            return BigDecimal.valueOf(number.doubleValue());
        }
        return new BigDecimal(String.valueOf(value));
    }

    private BigDecimal normalize(BigDecimal amount) {
        return amount != null ? amount : BigDecimal.ZERO;
    }

    private BigDecimal zeroIfNegative(BigDecimal amount) {
        BigDecimal normalized = normalize(amount);
        return normalized.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : normalized;
    }
}
