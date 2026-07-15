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
    btnVerify: null,
    btnReject: null,
    
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
        this.btnVerify = document.getElementById('btnVerifyOrder');
        this.btnReject = document.getElementById('btnRejectOrder');
        
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
        this.btnRejectCancel.addEventListener('click', () => this.hideRejectReasonInput());
        this.btnRejectConfirm.addEventListener('click', () => this.handleRejectConfirm());

        // Automatically format fees while typing
        this.inputCraneFee.addEventListener('input', (e) => this.sanitizeNumericInput(e.target));
        this.inputShippingFee.addEventListener('input', (e) => this.sanitizeNumericInput(e.target));
    },

    /**
     * Prevents letters in fee inputs
     */
    sanitizeNumericInput(input) {
        input.value = input.value.replace(/[^0-9]/g, '');
        this.calculateFinalTotalOnTheFly();
    },

    /**
     * Updates final total amount dynamically based on input fees
     */
    calculateFinalTotalOnTheFly() {
        if (!this.currentOrder) return;
        const base = this.currentOrder.product.price * this.currentOrder.quantity;
        const crane = Number(this.inputCraneFee.value) || 0;
        const shipping = Number(this.inputShippingFee.value) || 0;
        const total = base + crane + shipping;
        this.elFinalTotal.textContent = this.formatVND(total);
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
            alert('Không tìm thấy thông tin chi tiết đơn hàng!');
            return;
        }

        this.currentOrder = order;

        // Render fields
        this.elCode.textContent = order.orderCode;
        
        // Status Badge class skins
        this.elStatusBadge.className = `status-badge ${order.orderStatus.toLowerCase()}`;
        this.elStatusBadge.textContent = order.orderStatus;
        
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
        this.elBasePrice.textContent = this.formatVND(order.product.price * order.quantity);
        this.elDeposit.textContent = this.formatVND(order.depositAmount);
        this.elFinalTotal.textContent = this.formatVND(order.totalAmount);
        
        // Control fields state based on Order Status
        if (order.orderStatus === 'PENDING') {
            this.inputCraneFee.disabled = false;
            this.inputShippingFee.disabled = false;
            
            // Default input values
            this.inputCraneFee.value = order.craneFee !== null ? order.craneFee : '';
            this.inputShippingFee.value = order.shippingFee !== null ? order.shippingFee : '';
            
            // Show verification action footer bar
            this.btnVerify.style.display = 'block';
            this.btnReject.style.display = 'block';
        } else {
            this.inputCraneFee.disabled = true;
            this.inputShippingFee.disabled = true;
            
            // Display locked values
            this.inputCraneFee.value = order.craneFee !== null ? order.craneFee : 0;
            this.inputShippingFee.value = order.shippingFee !== null ? order.shippingFee : 0;
            
            // Hide verification actions if already verified/cancelled/rejected
            this.btnVerify.style.display = 'none';
            this.btnReject.style.display = 'none';
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
        const crane = Number(this.inputCraneFee.value) || 0;
        const shipping = Number(this.inputShippingFee.value) || 0;

        ConfirmDialog.show({
            title: "Xác nhận duyệt đơn hàng",
            message: `Bạn có chắc chắn muốn duyệt đơn hàng ${this.currentOrder.orderCode} với các khoản chi phí bổ sung sau không?`,
            summary: [
                { label: "Mã đơn hàng", value: this.currentOrder.orderCode },
                { label: "Phí xe cẩu (Crane Fee)", value: this.formatVND(crane) },
                { label: "Phí vận chuyển (Shipping)", value: this.formatVND(shipping) },
                { label: "Tổng tiền đơn hàng", value: this.elFinalTotal.textContent }
            ],
            onConfirm: async () => {
                // Call Mock API to verify
                const res = await apiVerifyOrder(this.currentOrder.orderCode, crane, shipping);
                if (res.success) {
                    alert('Duyệt đơn hàng thành công!');
                    this.close();
                    if (this.onSuccessCallback) this.onSuccessCallback();
                } else {
                    alert(res.message || 'Có lỗi xảy ra khi duyệt đơn hàng!');
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
            alert('Vui lòng nhập lý do từ chối!');
            return;
        }

        ConfirmDialog.show({
            title: "Từ chối duyệt đơn hàng",
            message: `Bạn có chắc chắn từ chối duyệt đơn hàng ${this.currentOrder.orderCode}?`,
            summary: [
                { label: "Mã đơn hàng", value: this.currentOrder.orderCode },
                { label: "Lý do từ chối", value: reason }
            ],
            onConfirm: async () => {
                const res = await apiRejectOrder(this.currentOrder.orderCode, reason);
                if (res.success) {
                    alert('Đơn hàng đã bị từ chối duyệt.');
                    this.close();
                    if (this.onSuccessCallback) this.onSuccessCallback();
                } else {
                    alert(res.message);
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
