(function (window) {
    const ORDER_STATUS = {
        PENDING: 'Chờ kiểm duyệt',
        PENDING_PAYMENT: 'Chờ khách thanh toán',
        DEPOSITED: 'Đã thanh toán tiền đặt cọc',
        PAID: 'Đã thanh toán toàn bộ',
        COMPLETED: 'Đã hoàn thành',
        CANCELLED: 'Đã chấm dứt đơn hàng',
        WAITING_APPROVAL: 'Chờ kiểm duyệt',
        WAITING_CUSTOMER_PAYMENT: 'Chờ khách thanh toán',
        WAITING_DELIVERY_PAYMENT: 'Chờ thanh toán số tiền còn lại'
    };

    const PAYMENT_TYPE = {
        DEPOSIT: 'Thanh toán tiền đặt cọc',
        FULL_PAYMENT: 'Thanh toán toàn bộ đơn hàng',
        REMAINING_PAYMENT: 'Thanh toán số tiền còn lại'
    };

    const PAYMENT_METHOD = {
        VNPAY: 'Thanh toán trực tuyến qua VNPay',
        CASH: 'Thanh toán tiền mặt',
        COD: 'Đặt cọc trước, thanh toán phần còn lại khi nhận cây',
        DEPOSIT: 'Đặt cọc trước, thanh toán phần còn lại khi nhận cây',
        BANK_TRANSFER: 'Chuyển khoản ngân hàng'
    };

    const PAYMENT_STATUS = {
        PENDING: 'Chờ thanh toán',
        SUCCESS: 'Thanh toán thành công',
        PAID: 'Thanh toán thành công',
        COMPLETED: 'Thanh toán thành công',
        FAILED: 'Thanh toán thất bại',
        EXPIRED: 'Đã hết hạn thanh toán',
        CANCELLED: 'Đã hủy giao dịch'
    };

    const PRIORITY = {
        CRITICAL: 'Rất khẩn cấp',
        HIGH: 'Ưu tiên cao',
        MEDIUM: 'Ưu tiên trung bình',
        NORMAL: 'Bình thường',
        LOW: 'Ưu tiên thấp'
    };

    const FINANCIAL_LEDGER_TYPE = {
        COMPLETED_ORDER_REVENUE: 'Doanh thu từ đơn hàng đã hoàn thành',
        FORFEITED_DEPOSIT_INCOME: 'Thu nhập từ tiền đặt cọc do khách bỏ đơn',
        FULL_REFUND: 'Hoàn lại toàn bộ tiền cho khách'
    };

    const FINANCIAL_LEDGER_DIRECTION = {
        INCOME: 'Khoản thu',
        OUTFLOW: 'Khoản hoàn/chi ra'
    };

    const FINANCIAL_LEDGER_STATUS = {
        RECORDED: 'Đã ghi nhận',
        VOIDED: 'Đã hủy bản ghi'
    };

    const FAULT_PARTY = {
        CUSTOMER: 'Lỗi từ phía khách hàng',
        NURSERY: 'Lỗi từ phía nhà vườn',
        DELIVERY: 'Lỗi trong quá trình vận chuyển',
        OTHER: 'Nguyên nhân khác'
    };

    const FINANCIAL_RESOLUTION = {
        FORFEITED_DEPOSIT_INCOME: 'Thu nhập từ tiền đặt cọc do khách bỏ đơn',
        REFUND_RECORDED: 'Đã ghi nhận hoàn tiền',
        REVENUE_RECOGNIZED: 'Đã ghi nhận doanh thu của đơn hoàn thành',
        DEPOSIT_RECEIVED_PENDING_COMPLETION: 'Đã thu tiền đặt cọc, chờ hoàn tất đơn',
        FULL_PAYMENT_RECEIVED_PENDING_COMPLETION: 'Đã thu toàn bộ tiền, chờ hoàn tất đơn',
        CANCELLED_NO_FINANCIAL_RECOGNITION: 'Đã chấm dứt, không ghi nhận doanh thu',
        CASH_RECEIVED_PENDING_RECOGNITION: 'Đã thu tiền, chờ đủ điều kiện ghi nhận',
        OPEN: 'Đang xử lý'
    };

    function normalize(value) {
        return String(value || '').trim().toUpperCase();
    }

    function label(dictionary, value) {
        const key = normalize(value);
        return dictionary[key] || value || '-';
    }

    window.OrderModeratorLabels = {
        orderStatus: value => label(ORDER_STATUS, value),
        paymentType: value => label(PAYMENT_TYPE, value),
        paymentMethod: value => label(PAYMENT_METHOD, value),
        paymentStatus: value => label(PAYMENT_STATUS, value),
        priority: value => label(PRIORITY, value),
        financialLedgerType: value => label(FINANCIAL_LEDGER_TYPE, value),
        financialLedgerDirection: value => label(FINANCIAL_LEDGER_DIRECTION, value),
        financialLedgerStatus: value => label(FINANCIAL_LEDGER_STATUS, value),
        faultParty: value => label(FAULT_PARTY, value),
        financialResolution: value => label(FINANCIAL_RESOLUTION, value)
    };
})(window);
