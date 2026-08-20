/**
 * Order Detail slide-out Drawer Component
 * Under: src/main/resources/public/js/components/order-drawer.js
 */

const OrderDrawer = {
    backdropEl: null,
    panelEl: null,
    currentOrder: null,
    onSuccessCallback: null,

    // Mapped Elements
    elCode: null,
    elStatusBadge: null,
    elCustName: null,
    elCustPhone: null,
    elCustEmail: null,
    elCustAddress: null,
    elNotes: null,
    elProdImg: null,
    elProdName: null,
    elProdPrice: null,
    elQty: null,
    elBasePrice: null,
    elDeposit: null,
    elFinalTotal: null,
    
    // Inputs & Forms
    inputCraneFee: null,
    inputShippingFee: null,
    inputDepositAmount: null,
    groupDepositAmount: null,
    btnVerify: null,
    btnReject: null,
    btnCustomerNoShow: null,
    btnCompletePaidOrder: null,
    
    // Reject Box
    rejectBox: null,
    textareaRejectReason: null,
    btnRejectConfirm: null,
    btnRejectCancel: null,

    /**
     * Initializes components bindings
     */
    init() {
        this.backdropEl = document.getElementById('drawerBackdrop');
        this.panelEl = document.getElementById('drawerPanel');
        
        // Map UI Labels
        this.elCode = document.getElementById('drawerOrderCode');
        this.elStatusBadge = document.getElementById('drawerStatusBadge');
        this.elCustName = document.getElementById('drawerCustName');
        this.elCustPhone = document.getElementById('drawerCustPhone');
        this.elCustEmail = document.getElementById('drawerCustEmail');
        this.elCustAddress = document.getElementById('drawerCustAddress');
        this.elNotes = document.getElementById('drawerNotes');
        this.elProdImg = document.getElementById('drawerProdImg');
        this.elProdName = document.getElementById('drawerProdName');
        this.elProdPrice = document.getElementById('drawerProdPrice');
        this.elQty = document.getElementById('drawerQty');
        this.elBasePrice = document.getElementById('drawerBasePrice');
        this.elDeposit = document.getElementById('drawerDeposit');
        this.elFinalTotal = document.getElementById('drawerFinalTotal');
        
        // Map Inputs & Buttons
        this.inputCraneFee = document.getElementById('inputCraneFee');
        this.inputShippingFee = document.getElementById('inputShippingFee');
        this.inputDepositAmount = document.getElementById('inputDepositAmount');
        this.groupDepositAmount = document.getElementById('groupDepositAmount');
        this.btnVerify = document.getElementById('btnVerifyOrder');
        this.btnReject = document.getElementById('btnRejectOrder');
        this.btnCustomerNoShow = document.getElementById('btnCustomerNoShow');
        this.btnCompletePaidOrder = document.getElementById('btnCompletePaidOrder');
        
        // Rejection Section
        this.rejectBox = document.getElementById('rejectReasonBox');
        this.textareaRejectReason = document.getElementById('textareaRejectReason');
        this.btnRejectConfirm = document.getElementById('btnRejectConfirm');
        this.btnRejectCancel = document.getElementById('btnRejectCancel');

        // Bind Close triggers
        document.getElementById('btnDrawerClose').addEventListener('click', () => this.close());
        this.backdropEl.addEventListener('click', () => this.close());

        // Setup Event Listeners
        this.btnVerify.addEventListener('click', () => this.handleVerifyClick());
        this.btnReject.addEventListener('click', () => this.showRejectReasonInput());
        if (this.btnCustomerNoShow) this.btnCustomerNoShow.addEventListener('click', () => this.handleCustomerNoShow());
        if (this.btnCompletePaidOrder) this.btnCompletePaidOrder.addEventListener('click', () => this.handleCompletePaidOrder());
        this.btnRejectCancel.addEventListener('click', () => this.hideRejectReasonInput());
        this.btnRejectConfirm.addEventListener('click', () => this.handleRejectConfirm());

        // Automatically format fees while typing
        if (this.inputCraneFee) this.inputCraneFee.addEventListener('input', (e) => this.sanitizeNumericInput(e.target));
        if (this.inputShippingFee) this.inputShippingFee.addEventListener('input', (e) => this.sanitizeNumericInput(e.target));
        if (this.inputDepositAmount) this.inputDepositAmount.addEventListener('input', (e) => this.sanitizeNumericInput(e.target));
    },

    /**
     * Prevents letters in fee inputs and limits maximum digit length
     */
    sanitizeNumericInput(input) {
        const maxLen = input.id === 'inputDepositAmount' ? 12 : 9;
        input.value = input.value.replace(/[^0-9]/g, '').slice(0, maxLen);
        this.calculateFinalTotalOnTheFly();
    },

    readNullableAmount(input) {
        if (!input) return null;
        const raw = String(input.value || '').trim();
        if (!raw) return null;
        const value = Number(raw);
        return Number.isFinite(value) ? value : null;
    },

    formatOptionalVND(value) {
        return value === null || value === undefined ? 'Chưa nhập' : this.formatVND(value);
    },

    /**
     * Updates final total amount dynamically based on input fees
     */
    calculateFinalTotalOnTheFly() {
        if (!this.currentOrder) return;
        const base = this.currentOrder.treePrice !== undefined ? this.currentOrder.treePrice : 
            ((this.currentOrder.product ? this.currentOrder.product.price : 0) * (this.currentOrder.quantity || 1));
        const crane = this.readNullableAmount(this.inputCraneFee);
        const shipping = this.readNullableAmount(this.inputShippingFee);
        const isDeposit = (this.currentOrder.paymentMethod === 'DEPOSIT' || this.currentOrder.paymentMethod === 'COD');
        const depositVal = isDeposit ? this.readNullableAmount(this.inputDepositAmount) : null;
        const craneValue = crane ?? 0;
        const shippingValue = shipping ?? 0;
        const effectiveDeposit = depositVal ?? 0;

        const total = base + craneValue + shippingValue;
        const payment1Total = isDeposit ? effectiveDeposit : total;
        const remainingPay = isDeposit ? Math.max(0, total - effectiveDeposit) : 0;

        // Nhóm 1: GIÁ TRỊ ĐƠN HÀNG
        if (this.elBasePrice) this.elBasePrice.textContent = this.formatVND(base);

        const shipValEl = document.getElementById('drawerShippingFeeVal');
        if (shipValEl) shipValEl.textContent = this.formatOptionalVND(shipping);

        const craneValEl = document.getElementById('drawerCraneFeeVal');
        if (craneValEl) craneValEl.textContent = this.formatOptionalVND(crane);

        if (this.elFinalTotal) this.elFinalTotal.textContent = this.formatVND(total);

        // Nhóm 2: THANH TOÁN NGAY (VNPAY)
        if (this.elDeposit) this.elDeposit.textContent = isDeposit ? this.formatOptionalVND(depositVal) : "Không (Trả 100%)";

        const payNowShipEl = document.getElementById('drawerPayNowShip');
        if (payNowShipEl) payNowShipEl.textContent = this.formatOptionalVND(shipping);

        const payNowCraneEl = document.getElementById('drawerPayNowCrane');
        if (payNowCraneEl) payNowCraneEl.textContent = this.formatOptionalVND(crane);

        const pay1El = document.getElementById('drawerPayment1Total');
        if (pay1El) pay1El.textContent = this.formatVND(payment1Total);

        // Nhóm 3: THANH TOÁN KHI NHẬN CÂY
        const remSection = document.getElementById('groupRemainingSection');
        if (remSection) remSection.style.display = isDeposit ? 'block' : 'none';

        const remEl = document.getElementById('drawerRemainingPay');
        if (remEl) remEl.textContent = this.formatVND(remainingPay);
    },

    /**
     * Opens the drawer with the specific order code
     * 
     * @param {string} orderCode 
     * @param {Function} onSuccess Call when order state successfully transitions (updates)
     */
    async open(orderCode, onSuccess) {
        if (!this.panelEl) this.init();
        
        this.onSuccessCallback = onSuccess;
        this.hideRejectReasonInput();

        // Fetch Detail Mock API
        const order = await apiFetchOrderDetail(orderCode);
        if (!order) {
            BSMSToast.error('Không tìm thấy thông tin chi tiết đơn hàng!');
            return;
        }

        this.currentOrder = order;

        // Render fields
        this.elCode.textContent = order.orderCode;
        
        // Status Badge class skins
        this.elStatusBadge.className = `status-badge ${order.orderStatus.toLowerCase()}`;
        this.elStatusBadge.textContent = order.orderStatusLabel || window.OrderModeratorLabels?.orderStatus(order.orderStatus) || '-';
        
        this.elCustName.textContent = order.customer.name;
        this.elCustPhone.textContent = order.customer.phone;
        this.elCustEmail.textContent = order.customer.email;
        this.elCustAddress.textContent = order.customer.address;
        this.elNotes.textContent = order.notes || 'Không có ghi chú thêm.';
        
        // Bonsai Details
        this.elProdImg.src = order.product.image;
        this.elProdImg.onerror = () => { this.elProdImg.src = 'https://picsum.photos/200/200?random=' + order.product.id; };
        this.elProdName.textContent = order.product.name;
        this.elProdPrice.textContent = this.formatVND(order.product.price);
        this.elQty.textContent = order.quantity;
        
        // Financial Details
        const basePrice = (order.product ? order.product.price : 0) * (order.quantity || 1);
        this.elBasePrice.textContent = this.formatVND(basePrice);
        this.elDeposit.textContent = this.formatOptionalVND(order.depositAmount);
        this.elFinalTotal.textContent = this.formatVND(order.totalAmount);
        
        const isPending = order.orderStatus === 'PENDING';
        const isDeposited = order.orderStatus === 'DEPOSITED';
        const isPaid = order.orderStatus === 'PAID';
        const isDeposit = (order.paymentMethod === 'DEPOSIT' || order.paymentMethod === 'COD');

        if (this.groupDepositAmount) {
            this.groupDepositAmount.style.display = isDeposit ? 'block' : 'none';
        }
        if (this.inputDepositAmount) {
            if (isDeposit) {
                const defaultDeposit = (order.depositAmount && order.depositAmount > 0) ? 
                    order.depositAmount : '';
                this.inputDepositAmount.value = defaultDeposit;
                this.inputDepositAmount.disabled = !isPending;
            } else {
                this.inputDepositAmount.value = '';
                this.inputDepositAmount.disabled = true;
            }
        }

        // Control fields state based on Order Status
        if (isPending) {
            if (this.inputCraneFee) {
                this.inputCraneFee.disabled = false;
                this.inputCraneFee.value = order.craneFee !== null ? order.craneFee : '';
            }
            if (this.inputShippingFee) {
                this.inputShippingFee.disabled = false;
                this.inputShippingFee.value = order.shippingFee !== null ? order.shippingFee : '';
            }
            
            // Show verification action footer bar
            if (this.btnVerify) this.btnVerify.style.display = 'block';
            if (this.btnReject) this.btnReject.style.display = 'block';
            if (this.btnCustomerNoShow) this.btnCustomerNoShow.style.display = 'none';
            if (this.btnCompletePaidOrder) this.btnCompletePaidOrder.style.display = 'none';
        } else {
            if (this.inputCraneFee) {
                this.inputCraneFee.disabled = true;
                this.inputCraneFee.value = order.craneFee !== null ? order.craneFee : '';
            }
            if (this.inputShippingFee) {
                this.inputShippingFee.disabled = true;
                this.inputShippingFee.value = order.shippingFee !== null ? order.shippingFee : '';
            }
            
            // Hide verification actions if already verified/cancelled/rejected
            if (this.btnVerify) this.btnVerify.style.display = 'none';
            if (this.btnReject) this.btnReject.style.display = 'none';
            if (this.btnCustomerNoShow) this.btnCustomerNoShow.style.display = (isDeposited || isPaid) ? 'block' : 'none';
            if (this.btnCompletePaidOrder) this.btnCompletePaidOrder.style.display = isPaid ? 'block' : 'none';
        }

        this.calculateFinalTotalOnTheFly();

        // Slide panel in
        this.backdropEl.classList.add('show');
        this.panelEl.classList.add('show');
    },

    /**
     * Triggers verification flow
     */
    handleVerifyClick() {
        const base = (this.currentOrder.product ? this.currentOrder.product.price : 0) * (this.currentOrder.quantity || 1);
        const crane = this.readNullableAmount(this.inputCraneFee);
        const shipping = this.readNullableAmount(this.inputShippingFee);
        const isDeposit = (this.currentOrder.paymentMethod === 'DEPOSIT' || this.currentOrder.paymentMethod === 'COD');
        const depositAmount = isDeposit ? this.readNullableAmount(this.inputDepositAmount) : null;
        const craneValue = crane ?? 0;
        const shippingValue = shipping ?? 0;

        if (isDeposit && (!depositAmount || depositAmount <= 0)) {
            BSMSToast.error('Vui lòng nhập số tiền đặt cọc.');
            if (this.inputDepositAmount) this.inputDepositAmount.focus();
            return;
        }

        const effectiveDeposit = isDeposit ? depositAmount : 0;
        if (isDeposit && effectiveDeposit > base) {
            BSMSToast.error('Tiền đặt cọc không được vượt quá giá trị của cây.');
            if (this.inputDepositAmount) this.inputDepositAmount.focus();
            return;
        }

        const pay1 = isDeposit ? effectiveDeposit : (base + craneValue + shippingValue);

        ConfirmDialog.show({
            title: "Xác nhận duyệt đơn hàng",
            message: `Bạn có chắc chắn muốn duyệt đơn hàng ${this.currentOrder.orderCode} không?`,
            summary: [
                { label: "Mã đơn hàng", value: this.currentOrder.orderCode },
                { label: "Phương thức thanh toán", value: window.OrderModeratorLabels?.paymentMethod(isDeposit ? "DEPOSIT" : "VNPAY") || (isDeposit ? "Đặt cọc trước, thanh toán phần còn lại khi nhận cây" : "Thanh toán trực tuyến qua VNPay") },
                ...(isDeposit ? [{ label: "Tiền đặt cọc cây", value: this.formatVND(effectiveDeposit) }] : []),
                { label: "Phí xe cẩu", value: this.formatOptionalVND(crane) },
                { label: "Phí vận chuyển", value: this.formatOptionalVND(shipping) },
                { label: "Tổng giá trị đơn hàng", value: this.formatVND(base + craneValue + shippingValue) },
                { label: "Số tiền khách cần thanh toán ngay", value: this.formatVND(pay1) }
            ],
            onConfirm: async () => {
                const res = await apiVerifyOrder(this.currentOrder.orderCode, crane, shipping, depositAmount);
                if (res.success) {
                    BSMSToast.success('Duyệt đơn hàng thành công!');
                    this.close();
                    if (this.onSuccessCallback) this.onSuccessCallback();
                } else {
                    BSMSToast.error(res.message || 'Có lỗi xảy ra khi duyệt đơn hàng!');
                }
            }
        });
    },

    /**
     * Toggles rejection reason panel
     */
    showRejectReasonInput() {
        this.rejectBox.style.display = 'flex';
        this.textareaRejectReason.focus();
        this.btnVerify.disabled = true;
        this.btnReject.disabled = true;
    },

    hideRejectReasonInput() {
        this.rejectBox.style.display = 'none';
        this.textareaRejectReason.value = '';
        this.btnVerify.disabled = false;
        this.btnReject.disabled = false;
    },

    /**
     * Handles rejection confirm click
     */
    async handleRejectConfirm() {
        const reason = this.textareaRejectReason.value.trim();
        if (!reason) {
            BSMSToast.warning('Vui lòng nhập lý do từ chối!');
            return;
        }

        ConfirmDialog.show({
            title: "Từ chối đơn hàng",
            message: `Đơn hàng ${this.currentOrder.orderCode} sẽ bị từ chối và cây trong đơn sẽ được giải phóng theo quy tắc hiện tại.`,
            summary: [
                { label: "Mã đơn hàng", value: this.currentOrder.orderCode },
                { label: "Lý do từ chối", value: reason }
            ],
            onConfirm: async () => {
                const res = await apiRejectOrder(this.currentOrder.orderCode, reason);
                if (res.success) {
                    BSMSToast.success('Đơn hàng đã được từ chối.');
                    this.close();
                    if (this.onSuccessCallback) this.onSuccessCallback();
                } else {
                    BSMSToast.error(res.message || 'Lỗi khi từ chối đơn hàng!');
                }
            }
        });
    },

    handleCustomerNoShow() {
        const isPaid = this.currentOrder && this.currentOrder.orderStatus === 'PAID';
        ConfirmDialog.show({
            title: "Xác nhận khách không nhận hàng",
            message: isPaid
                ? "Đơn hàng đã thanh toán toàn bộ sẽ bị chấm dứt và sản phẩm được mở bán lại theo quy tắc hiện tại. Thông tin thanh toán hiện có được giữ nguyên."
                : "Đơn hàng sẽ bị chấm dứt do lỗi từ phía khách hàng. Tiền đặt cọc đã thanh toán sẽ được ghi nhận là khoản nhà vườn được giữ.",
            summary: [
                { label: "Mã đơn hàng", value: this.currentOrder.orderCode },
                { label: "Lý do", value: "Khách không nhận hàng hoặc không thanh toán số tiền còn lại" }
            ],
            onConfirm: async () => {
                const notes = isPaid
                    ? "Khách không nhận do lỗi nhà vườn. Cần hoàn tiền ngoài hệ thống."
                    : "Khách không nhận hàng / không thanh toán phần còn lại.";
                const res = await apiCustomerNoShow(this.currentOrder.orderCode, notes);
                if (res.success) {
                    BSMSToast.success("Đã chấm dứt đơn vì khách không nhận hàng. Sản phẩm đã được mở bán lại.");
                    this.close();
                    if (this.onSuccessCallback) this.onSuccessCallback();
                } else {
                    BSMSToast.error(res.message || "Lỗi khi hủy đơn vì khách không nhận.");
                }
            }
        });
    },

    handleCompletePaidOrder() {
        ConfirmDialog.show({
            title: "Xác nhận hoàn thành đơn hàng",
            message: "Đơn hàng sẽ được xác nhận hoàn thành. Doanh thu của đơn chỉ đủ điều kiện ghi nhận sau khi đơn chuyển sang trạng thái hoàn thành.",
            summary: [
                { label: "Mã đơn hàng", value: this.currentOrder.orderCode }
            ],
            onConfirm: async () => {
                const res = await apiCompletePaidOrder(this.currentOrder.orderCode);
                if (res.success) {
                    BSMSToast.success("Đơn hàng đã hoàn thành.");
                    this.close();
                    if (this.onSuccessCallback) this.onSuccessCallback();
                } else {
                    BSMSToast.error(res.message || "Lỗi khi hoàn thành đơn.");
                }
            }
        });
    },

    /**
     * Closes/Slides-out the drawer panel
     */
    close() {
        if (this.panelEl) {
            this.panelEl.classList.remove('show');
            this.backdropEl.classList.remove('show');
        }
        this.currentOrder = null;
    },

    /**
     * Formats number to VND currency style
     */
    formatVND(num) {
        return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(num);
    }
};
