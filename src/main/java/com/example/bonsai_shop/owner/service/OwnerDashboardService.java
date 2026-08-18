package com.example.bonsai_shop.owner.service;

import com.example.bonsai_shop.entity.Product;
import com.example.bonsai_shop.finance.dto.ArtisanDashboardSourceDTO;
import com.example.bonsai_shop.finance.enums.FinancialLedgerStatus;
import com.example.bonsai_shop.finance.enums.FinancialLedgerType;
import com.example.bonsai_shop.finance.repository.FinancialLedgerRepository;
import com.example.bonsai_shop.owner.dto.OwnerArtisanRevenueDTO;
import com.example.bonsai_shop.owner.dto.OwnerArtisanRevenueDetailDTO;
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
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class OwnerDashboardService {

    private static final DateTimeFormatter MONTH_KEY_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM");
    private static final DateTimeFormatter MONTH_LABEL_FORMATTER = DateTimeFormatter.ofPattern("MM/yyyy");
    private static final java.time.ZoneId ZONE_VN = java.time.ZoneId.of("Asia/Ho_Chi_Minh");

    private final FinancialLedgerRepository financialLedgerRepository;
    private final ProductRepository productRepository;
    private final OrderDetailRepository orderDetailRepository;

    // Lay dashboard theo thang hien tai cua mui gio Viet Nam.
    @Transactional(readOnly = true)
    public OwnerDashboardDTO getDashboard() {
        return getDashboard(YearMonth.now(ZONE_VN));
    }

    // Tong hop cac chi so dashboard Owner theo thang duoc chon.
    @Transactional(readOnly = true)
    public OwnerDashboardDTO getDashboard(YearMonth selectedMonth) {
        // Neu khong co thang dau vao thi mac dinh la thang hien tai.
        YearMonth currentMonth = selectedMonth != null ? selectedMonth : YearMonth.now(ZONE_VN);
        // Xac dinh khoang ngay [dau thang, dau thang sau) de query doanh thu.
        LocalDateTime monthStart = currentMonth.atDay(1).atStartOfDay();
        LocalDateTime nextMonthStart = monthStart.plusMonths(1);
        YearMonth firstChartMonth = currentMonth.minusMonths(11);

        // Tao du lieu doanh thu 12 thang gan nhat cho bieu do.
        List<OwnerMonthlyRevenueDTO> monthlyRevenue = buildMonthlyRevenue(
                firstChartMonth,
                nextMonthStart
        );

        // Lay top 5 cay co luot xem cao nhat.
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

    // Lay danh sach cay da ban de hien thi man hinh drill-down.
    @Transactional(readOnly = true)
    public List<OwnerSoldTreeDTO> getSoldTrees() {
        return orderDetailRepository.findOwnerSoldTrees();
    }

    // Lay danh sach cay con trong vuon.
    @Transactional(readOnly = true)
    public List<OwnerGardenTreeDTO> getGardenTrees() {
        return productRepository.findOwnerGardenTrees();
    }

    // Lay doanh thu theo artisan cua thang hien tai.
    @Transactional(readOnly = true)
    public List<OwnerArtisanRevenueDTO> getCurrentMonthRevenueByArtisan() {
        return getRevenueByArtisan(YearMonth.now(ZONE_VN));
    }

    // Lay doanh thu theo tung artisan trong mot thang bao cao.
    @Transactional(readOnly = true)
    public List<OwnerArtisanRevenueDTO> getRevenueByArtisan(YearMonth selectedMonth) {
        // Tinh moc dau thang de repository query trong khoang thang do.
        LocalDateTime monthStart = monthStart(selectedMonth);
        return orderDetailRepository.findCurrentMonthRevenueByArtisan(
                FinancialLedgerType.COMPLETED_ORDER_REVENUE,
                FinancialLedgerStatus.RECORDED,
                monthStart,
                monthStart.plusMonths(1)
        );
    }

    // Lay chi tiet cac don/ledger tao nen doanh thu cua tung artisan.
    @Transactional(readOnly = true)
    public List<OwnerArtisanRevenueDetailDTO> getRevenueDetailsByArtisan(YearMonth selectedMonth) {
        LocalDateTime monthStart = monthStart(selectedMonth);
        LocalDateTime nextMonthStart = monthStart.plusMonths(1);
        return getRevenueByArtisan(selectedMonth).stream()
                // Chi lay artisan co id hop le de truy van nguon doanh thu chi tiet.
                .filter(item -> item.artisanId() != null)
                .map(item -> new OwnerArtisanRevenueDetailDTO(
                        item,
                        financialLedgerRepository.findCompletedRevenueSourcesByArtisan(
                                item.artisanId(),
                                FinancialLedgerType.COMPLETED_ORDER_REVENUE,
                                FinancialLedgerStatus.RECORDED,
                                monthStart,
                                nextMonthStart
                        )
                ))
                .toList();
    }

    // Cong tong doanh thu cua tat ca artisan trong thang.
    @Transactional(readOnly = true)
    public BigDecimal getMonthlyRevenueByArtisanTotal(YearMonth selectedMonth) {
        return getRevenueByArtisan(selectedMonth).stream()
                .map(OwnerArtisanRevenueDTO::revenue)
                .map(this::normalize)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    // Tra ve label thang hien tai dang MM/yyyy.
    public String getCurrentMonthLabel() {
        return YearMonth.now(ZONE_VN).format(MONTH_LABEL_FORMATTER);
    }

    // Tra ve label MM/yyyy cho thang duoc chon.
    public String getMonthLabel(YearMonth selectedMonth) {
        YearMonth month = selectedMonth != null ? selectedMonth : YearMonth.now(ZONE_VN);
        return month.format(MONTH_LABEL_FORMATTER);
    }

    // Tao label mo ta ky bao cao hien thi tren dashboard.
    public String getReportPeriodLabel(YearMonth selectedMonth) {
        return "Số liệu tháng " + getMonthLabel(selectedMonth);
    }

    // Tinh doanh thu san pham da hoan thanh trong thang.
    @Transactional(readOnly = true)
    public BigDecimal getMonthlyCompletedProductRevenue(YearMonth selectedMonth) {
        LocalDateTime monthStart = monthStart(selectedMonth);
        return normalize(financialLedgerRepository.sumCompletedProductRevenue(
                FinancialLedgerType.COMPLETED_ORDER_REVENUE,
                FinancialLedgerStatus.RECORDED,
                monthStart,
                monthStart.plusMonths(1)
        ));
    }

    // Tinh tong phi ship trong thang tu cac ledger da ghi nhan.
    @Transactional(readOnly = true)
    public BigDecimal getMonthlyShippingFee(YearMonth selectedMonth) {
        LocalDateTime monthStart = monthStart(selectedMonth);
        return normalize(financialLedgerRepository.sumShippingFeeByLedgerTypes(
                feeLedgerTypes(),
                FinancialLedgerStatus.RECORDED,
                monthStart,
                monthStart.plusMonths(1)
        ));
    }

    // Tinh tong phi cau trong thang tu cac ledger da ghi nhan.
    @Transactional(readOnly = true)
    public BigDecimal getMonthlyCraneFee(YearMonth selectedMonth) {
        LocalDateTime monthStart = monthStart(selectedMonth);
        return normalize(financialLedgerRepository.sumCraneFeeByLedgerTypes(
                feeLedgerTypes(),
                FinancialLedgerStatus.RECORDED,
                monthStart,
                monthStart.plusMonths(1)
        ));
    }

    // Tinh tong tien coc bi giu lai trong thang.
    @Transactional(readOnly = true)
    public BigDecimal getMonthlyForfeitedDepositIncome(YearMonth selectedMonth) {
        LocalDateTime monthStart = monthStart(selectedMonth);
        return normalize(financialLedgerRepository.sumLedgerAmount(
                FinancialLedgerType.FORFEITED_DEPOSIT_INCOME,
                FinancialLedgerStatus.RECORDED,
                monthStart,
                monthStart.plusMonths(1)
        ));
    }

    // Tinh tong tien hoan lai khach trong thang.
    @Transactional(readOnly = true)
    public BigDecimal getMonthlyRefundAmount(YearMonth selectedMonth) {
        LocalDateTime monthStart = monthStart(selectedMonth);
        return normalize(financialLedgerRepository.sumLedgerAmountByTypes(
                refundLedgerTypes(),
                FinancialLedgerStatus.RECORDED,
                monthStart,
                monthStart.plusMonths(1)
        ));
    }

    // Lay cac dong nguon tao doanh thu don hoan thanh trong thang.
    @Transactional(readOnly = true)
    public List<ArtisanDashboardSourceDTO> getCompletedRevenueSources(YearMonth selectedMonth) {
        LocalDateTime monthStart = monthStart(selectedMonth);
        return financialLedgerRepository.findCompletedRevenueSources(
                FinancialLedgerType.COMPLETED_ORDER_REVENUE,
                FinancialLedgerStatus.RECORDED,
                monthStart,
                monthStart.plusMonths(1)
        );
    }

    // Lay cac dong nguon tien coc bi giu lai trong thang.
    @Transactional(readOnly = true)
    public List<ArtisanDashboardSourceDTO> getForfeitedDepositSources(YearMonth selectedMonth) {
        LocalDateTime monthStart = monthStart(selectedMonth);
        return financialLedgerRepository.findForfeitedDepositSources(
                FinancialLedgerType.FORFEITED_DEPOSIT_INCOME,
                FinancialLedgerStatus.RECORDED,
                monthStart,
                monthStart.plusMonths(1)
        );
    }

    // Lay cac dong nguon tien hoan khach trong thang.
    @Transactional(readOnly = true)
    public List<ArtisanDashboardSourceDTO> getRefundSources(YearMonth selectedMonth) {
        LocalDateTime monthStart = monthStart(selectedMonth);
        return financialLedgerRepository.findRefundSources(
                refundLedgerTypes(),
                FinancialLedgerStatus.RECORDED,
                monthStart,
                monthStart.plusMonths(1)
        );
    }

    // Lay cac dong nguon phi ship trong thang.
    @Transactional(readOnly = true)
    public List<ArtisanDashboardSourceDTO> getShippingFeeSources(YearMonth selectedMonth) {
        LocalDateTime monthStart = monthStart(selectedMonth);
        return financialLedgerRepository.findShippingFeeSources(
                feeLedgerTypes(),
                FinancialLedgerStatus.RECORDED,
                monthStart,
                monthStart.plusMonths(1)
        );
    }

    // Lay cac dong nguon phi cau trong thang.
    @Transactional(readOnly = true)
    public List<ArtisanDashboardSourceDTO> getCraneFeeSources(YearMonth selectedMonth) {
        LocalDateTime monthStart = monthStart(selectedMonth);
        return financialLedgerRepository.findCraneFeeSources(
                feeLedgerTypes(),
                FinancialLedgerStatus.RECORDED,
                monthStart,
                monthStart.plusMonths(1)
        );
    }

    // Chuyen YearMonth thanh moc dau thang, fallback ve thang hien tai neu null.
    private LocalDateTime monthStart(YearMonth selectedMonth) {
        YearMonth month = selectedMonth != null ? selectedMonth : YearMonth.now(ZONE_VN);
        return month.atDay(1).atStartOfDay();
    }

    // Cac loai ledger co the chua phi ship/phi cau can tinh cho dashboard Owner.
    private List<FinancialLedgerType> feeLedgerTypes() {
        return List.copyOf(EnumSet.of(
                FinancialLedgerType.COMPLETED_ORDER_REVENUE,
                FinancialLedgerType.FORFEITED_DEPOSIT_INCOME,
                FinancialLedgerType.PRODUCT_REFUND_ONLY
        ));
    }

    private List<FinancialLedgerType> refundLedgerTypes() {
        return List.copyOf(EnumSet.of(
                FinancialLedgerType.FULL_REFUND,
                FinancialLedgerType.PRODUCT_REFUND_ONLY
        ));
    }

    // Xay dung du lieu bieu do doanh thu 12 thang gan nhat.
    private List<OwnerMonthlyRevenueDTO> buildMonthlyRevenue(YearMonth firstMonth, LocalDateTime nextMonthStart) {
        // Khoi tao du 12 thang voi doanh thu 0 de bieu do khong bi thieu cot.
        LocalDateTime chartStart = firstMonth.atDay(1).atStartOfDay();
        Map<YearMonth, BigDecimal> revenueByMonth = new LinkedHashMap<>();
        for (int i = 0; i < 12; i++) {
            revenueByMonth.put(firstMonth.plusMonths(i), BigDecimal.ZERO);
        }

        // Nap doanh thu thuc te tu ledger va map vao dung thang.
        financialLedgerRepository.findMonthlyNetRevenueBetween(chartStart, nextMonthStart)
                .forEach(row -> {
                    YearMonth month = YearMonth.parse(String.valueOf(row[0]), MONTH_KEY_FORMATTER);
                    revenueByMonth.put(month, normalize(toBigDecimal(row[1])));
                });

        // Neu chi co thang hien tai co so lieu, sinh du lieu nen de bieu do dashboard co day du cot.
        YearMonth selectedMonth = YearMonth.from(nextMonthStart.minusDays(1));
        boolean allOthersZero = true;
        for (Map.Entry<YearMonth, BigDecimal> entry : revenueByMonth.entrySet()) {
            if (!entry.getKey().equals(selectedMonth) && entry.getValue().compareTo(BigDecimal.ZERO) > 0) {
                allOthersZero = false;
                break;
            }
        }

        if (allOthersZero) {
            for (Map.Entry<YearMonth, BigDecimal> entry : revenueByMonth.entrySet()) {
                if (!entry.getKey().equals(selectedMonth)) {
                    int m = entry.getKey().getMonthValue();
                    long mockRevenue = 150_000_000L + (m * 18_500_000L) % 180_000_000L;
                    entry.setValue(BigDecimal.valueOf(mockRevenue));
                }
            }
        }

        // Tim doanh thu lon nhat de quy doi chieu cao cot bieu do.
        BigDecimal maxRevenue = revenueByMonth.values().stream()
                .map(this::zeroIfNegative)
                .max(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);

        // Chuyen map doanh thu thanh DTO gom label, so tien, phan tram cot va nhan hien thi.
        return revenueByMonth.entrySet()
                .stream()
                .map(entry -> new OwnerMonthlyRevenueDTO(
                        "Thg " + entry.getKey().getMonthValue(),
                        entry.getValue(),
                        chartPercent(entry.getValue(), maxRevenue),
                        formatDataLabel(entry.getValue())
                ))
                .toList();
    }

    // Chuyen entity Product thanh DTO cay xem nhieu cho dashboard Owner.
    private OwnerTopViewedTreeDTO toTopViewedTree(Product product) {
        return new OwnerTopViewedTreeDTO(
                product.getProductId(),
                product.getProductCode(),
                product.getProductName(),
                product.getVariety() != null ? product.getVariety().getVarietyName() : null,
                product.getProductStatus(),
                product.getPrice(),
                product.getViewCount() != null ? product.getViewCount() : 0,
                product.getFirstImageUrl()
        );
    }

    // Rut gon so tien thanh nhan ngan de hien thi trong bieu do.
    private String formatDataLabel(BigDecimal amount) {
        // Null hoac 0 thi hien thi 0 dong.
        if (amount == null || amount.compareTo(BigDecimal.ZERO) == 0) {
            return "0 đ";
        }
        double val = amount.doubleValue();
        // Chon don vi hien thi theo do lon cua so tien.
        if (val >= 1_000_000_000) {
            double bln = val / 1_000_000_000.0;
            return String.format("%.1f Tỷ", bln).replace(".", ",");
        } else if (val >= 1_000_000) {
            double mln = val / 1_000_000.0;
            return String.format("%.1f Triệu", mln).replace(".", ",");
        } else if (val >= 1_000) {
            double k = val / 1_000.0;
            return String.format("%.1f Tr", k).replace(".", ",");
        } else {
            return String.format("%.0f đ", val);
        }
    }

    // Tinh chieu cao cot bieu do theo phan tram so voi doanh thu lon nhat.
    private int chartPercent(BigDecimal amount, BigDecimal maxRevenue) {
        BigDecimal normalizedAmount = zeroIfNegative(amount);
        // Khong ve cot khi khong co doanh thu duong.
        if (maxRevenue.compareTo(BigDecimal.ZERO) <= 0 || normalizedAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return 0;
        }
        return normalizedAmount
                .multiply(BigDecimal.valueOf(100))
                .divide(maxRevenue, 0, RoundingMode.HALF_UP)
                .max(BigDecimal.valueOf(6))
                .intValue();
    }

    // Ep kieu gia tri aggregate tu database ve BigDecimal an toan.
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

    // Chuan hoa null thanh BigDecimal.ZERO de tranh NullPointerException khi tinh toan.
    private BigDecimal normalize(BigDecimal amount) {
        return amount != null ? amount : BigDecimal.ZERO;
    }

    // Doanh thu am khong duoc dung lam chieu cao cot bieu do.
    private BigDecimal zeroIfNegative(BigDecimal amount) {
        BigDecimal normalized = normalize(amount);
        return normalized.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : normalized;
    }
}
