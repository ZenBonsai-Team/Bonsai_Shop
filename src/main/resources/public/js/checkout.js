const CHECKOUT_NAME_MAX_LENGTH = 50;
const CHECKOUT_ADDRESS_MAX_LENGTH = 255;
const CHECKOUT_NOTES_MAX_LENGTH = 400;
document.addEventListener('DOMContentLoaded', () => {
    loadCheckoutSummary();
    
    const placeOrderBtn = document.getElementById('btnPlaceOrder');
    if (placeOrderBtn) {
        placeOrderBtn.addEventListener('click', placeOrder);
    }

    const confirmOtpBtn = document.getElementById('btnConfirmGuestOtp');
    if (confirmOtpBtn) {
        confirmOtpBtn.addEventListener('click', confirmGuestOtpAndCheckout);
    }

    const resendOtpBtn = document.getElementById('btnResendGuestOtp');
    if (resendOtpBtn) {
        resendOtpBtn.addEventListener('click', resendGuestOtp);
    }

    const otpInput = document.getElementById('guestOtpInput');
    if (otpInput) {
        otpInput.addEventListener('input', () => {
            otpInput.value = otpInput.value.replace(/\D/g, '').slice(0, 6);
            otpInput.classList.remove('is-invalid');
        });
        otpInput.addEventListener('keypress', (e) => {
            if (!/[0-9]/.test(e.key)) {
                e.preventDefault();
            }
        });
    }

    const phoneInput = document.getElementById('custPhone');
    if (phoneInput) {
        phoneInput.addEventListener('input', () => {
            phoneInput.value = phoneInput.value.replace(/\D/g, '').slice(0, 10);
            validateCheckoutField('custPhone');
        });
        phoneInput.addEventListener('keypress', (e) => {
            if (!/[0-9]/.test(e.key)) {
                e.preventDefault();
            }
        });
    }
    
    // Đăng ký xóa trạng thái lỗi khi người dùng nhập liệu lại
    const inputs = ['custName', 'custPhone', 'custEmail', 'custAddress', 'orderNotes'];
    inputs.forEach(id => {
        const inputEl = document.getElementById(id);
        if (inputEl) {
            inputEl.addEventListener('input', () => validateCheckoutField(id));
            inputEl.addEventListener('blur', () => validateCheckoutField(id));
        }
    });
    updateNotesCounter();
});

async function loadCheckoutSummary() {
    const urlParams = new URLSearchParams(window.location.search);
    const singleProductId = urlParams.get('productId');

    try {
        const response = await fetch('/api/cart');
        if (response.ok) {
            const items = await response.json();
            if (items && items.length > 0) {
                renderSummary(items);
                return;
            }
        }
    } catch (error) {
    }

    // Fallback cho Khách vãng lai (Guest)
    let guestProductIds = [];
    if (singleProductId) {
        guestProductIds.push(parseInt(singleProductId));
    } else {
        const guestCart = JSON.parse(localStorage.getItem('bonsai_guest_cart') || '[]');
        guestProductIds = guestCart;
    }

    if (guestProductIds.length > 0) {
        const items = [];
        for (const pId of guestProductIds) {
            try {
                const res = await fetch(`/api/products/${pId}`);
                if (res.ok) {
                    const prod = await res.json();
                    items.push({
                        productId: prod.productId,
                        productName: prod.productName,
                        price: prod.price
                    });
                }
            } catch (err) {}
        }
        window.guestProductIdsToBuy = guestProductIds;
        renderSummary(items);
    } else {
        renderSummary([]);
    }
}

function renderSummary(items) {
    const container = document.getElementById('checkoutSummaryItems');
    if (!container) return;
    container.innerHTML = '';
    
    if (!items || items.length === 0) {
        const modalEl = document.getElementById('businessErrorModal');
        if (modalEl) {
            document.getElementById('errorProductTitle').textContent = "Giỏ hàng trống";
            document.getElementById('errorProductDescription').textContent = "Không tìm thấy sản phẩm nào để tiến hành thanh toán.";
            const modal = new bootstrap.Modal(modalEl);
            modal.show();
        } else {
            BSMSToast.warning("Giỏ hàng của bạn đang trống! Không thể thanh toán.");
            setTimeout(() => { window.location.href = '/cart'; }, 1500);
        }
        return;
    }
    
    let subtotal = 0;
    
    items.forEach(item => {
        const itemTotal = (item.price || 0) * (item.quantity || 1);
        subtotal += itemTotal;
        
        const div = document.createElement('div');
        div.className = 'order-summary-item';
        div.innerHTML = `
            <div>
                <span class="summary-item-name">${item.productName}</span>
            </div>
            <span class="summary-item-price">${formatVND(itemTotal)}</span>
        `;
        container.appendChild(div);
    });
    
    document.getElementById('checkoutSubtotal').textContent = formatVND(subtotal);
}

function setCheckoutFieldState(input, feedbackId, isValid, message) {
    if (!input) return false;
    const feedback = feedbackId ? document.getElementById(feedbackId) : null;
    input.classList.toggle('is-valid', isValid);
    input.classList.toggle('is-invalid', !isValid);
    if (feedback && message) feedback.textContent = message;
    return isValid;
}

function validateCheckoutField(fieldId) {
    const input = document.getElementById(fieldId);
    const value = input?.value?.trim() || '';

    if (fieldId === 'custName') {
        return setCheckoutFieldState(input, 'nameFeedback', value.length >= 3 && value.length <= CHECKOUT_NAME_MAX_LENGTH, 'H\u1ecd v\u00e0 t\u00ean ng\u01b0\u1eddi nh\u1eadn ph\u1ea3i c\u00f3 t\u1eeb 3 \u0111\u1ebfn 50 k\u00fd t\u1ef1.');
    }

    if (fieldId === 'custPhone') {
        return setCheckoutFieldState(input, 'phoneFeedback', /^0\d{9}$/.test(value), 'S\u1ed1 \u0111i\u1ec7n tho\u1ea1i kh\u00f4ng h\u1ee3p l\u1ec7 (10 ch\u1eef s\u1ed1 b\u1eaft \u0111\u1ea7u b\u1eb1ng 0).');
    }

    if (fieldId === 'custEmail') {
        return setCheckoutFieldState(input, 'emailFeedback', /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(value) && value.length <= 255, '\u0110\u1ecba ch\u1ec9 email kh\u00f4ng h\u1ee3p l\u1ec7 ho\u1eb7c v\u01b0\u1ee3t qu\u00e1 255 k\u00fd t\u1ef1.');
    }

    if (fieldId === 'custAddress') {
        return setCheckoutFieldState(input, 'addressFeedback', value.length > 0 && value.length <= CHECKOUT_ADDRESS_MAX_LENGTH, '\u0110\u1ecba ch\u1ec9 nh\u1eadn kh\u00f4ng \u0111\u01b0\u1ee3c tr\u1ed1ng v\u00e0 kh\u00f4ng v\u01b0\u1ee3t qu\u00e1 255 k\u00fd t\u1ef1.');
    }

    if (fieldId === 'orderNotes') {
        updateNotesCounter();
        return setCheckoutFieldState(input, 'notesFeedback', value.length <= CHECKOUT_NOTES_MAX_LENGTH, 'Ghi ch\u00fa kh\u00f4ng \u0111\u01b0\u1ee3c v\u01b0\u1ee3t qu\u00e1 400 k\u00fd t\u1ef1.');
    }

    return true;
}

function updateNotesCounter() {
    const notesInput = document.getElementById('orderNotes');
    const counter = document.getElementById('notesCounter');
    if (!notesInput || !counter) return;

    const length = notesInput.value.trim().length;
    counter.textContent = length + '/400';
    counter.classList.toggle('text-danger', length > CHECKOUT_NOTES_MAX_LENGTH);
    counter.classList.toggle('text-muted', length <= CHECKOUT_NOTES_MAX_LENGTH);
}

function validateCheckoutForm() {
    const fieldIds = ['custName', 'custPhone', 'custEmail', 'custAddress', 'orderNotes'];
    const firstInvalidFieldId = fieldIds.find(fieldId => !validateCheckoutField(fieldId));
    if (!firstInvalidFieldId) return true;

    const firstErrorField = document.getElementById(firstInvalidFieldId);
    const toastEl = document.getElementById('validationToast');
    if (toastEl) {
        const toast = new bootstrap.Toast(toastEl);
        toast.show();
    }
    firstErrorField?.scrollIntoView({ behavior: 'smooth', block: 'center' });
    firstErrorField?.focus();
    return false;
}

let pendingOrderPayload = null;

async function placeOrder() {
    const custNameEl = document.getElementById('custName');
    const custPhoneEl = document.getElementById('custPhone');
    const custEmailEl = document.getElementById('custEmail');
    const custAddressEl = document.getElementById('custAddress');
    
    const custName = custNameEl.value.trim();
    const custPhone = custPhoneEl.value.trim();
    const custEmail = custEmailEl.value.trim();
    const custAddress = custAddressEl.value.trim();
    const notes = document.getElementById('orderNotes').value.trim();
    
    const paymentMethodEl = document.querySelector('input[name="paymentMethod"]:checked');
    const paymentMethod = paymentMethodEl ? paymentMethodEl.value : 'DEPOSIT';
    
    if (!validateCheckoutForm()) {
        return;
    }

    pendingOrderPayload = {
        customerName: custName,
        customerPhone: custPhone,
        customerEmail: custEmail,
        shippingAddress: custAddress,
        notes: notes,
        paymentMethod: paymentMethod,
        productIds: window.guestProductIdsToBuy || null
    };

    // Nếu là Khách vãng lai (có window.guestProductIdsToBuy) -> Yêu cầu gửi OTP trước
    if (window.guestProductIdsToBuy && window.guestProductIdsToBuy.length > 0) {
        sendGuestOtpAndShowModal(custEmail);
        return;
    }

    // Nếu là User đã đăng nhập -> Tiến hành Checkout trực tiếp
    executeCheckoutApi(pendingOrderPayload);
}

async function sendGuestOtpAndShowModal(email) {
    const btn = document.getElementById('btnPlaceOrder');
    btn.disabled = true;
    btn.textContent = "Đang gửi mã OTP...";

    const csrfToken = document.querySelector('meta[name="_csrf"]')?.getAttribute('content');
    const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.getAttribute('content');
    const headers = { 'Content-Type': 'application/json' };
    if (csrfHeader && csrfToken) headers[csrfHeader] = csrfToken;

    try {
        const response = await fetch('/api/orders/send-guest-otp', {
            method: 'POST',
            headers: headers,
            body: JSON.stringify({
                email: email,
                productIds: window.guestProductIdsToBuy || null
            })
        });
        const result = await response.json();

        btn.disabled = false;
        btn.textContent = "Xác nhận đặt hàng";

        if (response.ok && result.success) {
            document.getElementById('targetOtpEmail').textContent = email;
            document.getElementById('guestOtpInput').value = '';
            document.getElementById('guestOtpInput').classList.remove('is-invalid');

            const modalEl = document.getElementById('guestOtpModal');
            if (modalEl) {
                const modal = new bootstrap.Modal(modalEl);
                modal.show();
            }
        } else {
            showBusinessErrorModal(result.message || "Không thể gửi mã OTP. Vui lòng kiểm tra lại Email!");
        }
    } catch (error) {
        console.error('Error sending guest OTP:', error);
        btn.disabled = false;
        btn.textContent = "Xác nhận đặt hàng";
        showBusinessErrorModal("Không thể kết nối đến máy lưu trữ OTP. Vui lòng kiểm tra lại mạng!");
    }
}

async function confirmGuestOtpAndCheckout() {
    const otpInput = document.getElementById('guestOtpInput');
    const otpCode = otpInput ? otpInput.value.trim() : '';

    if (!otpCode || otpCode.length !== 6) {
        if (otpInput) {
            otpInput.classList.add('is-invalid');
            document.getElementById('guestOtpFeedback').textContent = "Vui lòng nhập đủ 6 chữ số OTP.";
        }
        return;
    }

    if (!pendingOrderPayload) return;

    pendingOrderPayload.otpCode = otpCode;

    const confirmBtn = document.getElementById('btnConfirmGuestOtp');
    confirmBtn.disabled = true;
    confirmBtn.textContent = "Đang xác thực OTP & Tạo đơn...";

    await executeCheckoutApi(pendingOrderPayload, confirmBtn);
}

async function resendGuestOtp() {
    if (!pendingOrderPayload || !pendingOrderPayload.customerEmail) return;
    const resendLink = document.getElementById('btnResendGuestOtp');
    resendLink.textContent = "Đang gửi lại...";
    await sendGuestOtpAndShowModal(pendingOrderPayload.customerEmail);
    resendLink.textContent = "Gửi lại OTP";
}

async function executeCheckoutApi(payload, confirmBtn = null) {
    const btn = document.getElementById('btnPlaceOrder');
    btn.disabled = true;
    btn.textContent = "Đang xử lý đặt hàng...";

    const csrfToken = document.querySelector('meta[name="_csrf"]')?.getAttribute('content');
    const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.getAttribute('content');
    const headers = { 'Content-Type': 'application/json' };
    if (csrfHeader && csrfToken) headers[csrfHeader] = csrfToken;

    try {
        const response = await fetch('/api/orders/checkout', {
            method: 'POST',
            headers: headers,
            body: JSON.stringify(payload)
        });

        const result = await response.json();

        if (response.ok && result.success) {
            localStorage.removeItem('bonsai_guest_cart');
            if (typeof window.updateCartBadge === 'function') {
                window.updateCartBadge();
            }
            // Tất cả các đơn hàng (COD hoặc Thanh toán 1 lần) đều chuyển hướng đến trang xác nhận tạo đơn thành công
            // Chờ Moderator kiểm duyệt và nhập phụ phí cẩu & vận chuyển trước khi thanh toán
            window.location.href = `/order/success?orderCode=${result.orderCode}`;
        } else {
            if (confirmBtn) {
                confirmBtn.disabled = false;
                confirmBtn.textContent = "Xác nhận & Hoàn tất đặt hàng";
            }
            btn.disabled = false;
            btn.textContent = "Xác nhận đặt hàng";

            if (result.requireOtp) {
                sendGuestOtpAndShowModal(payload.customerEmail);
            } else {
                const otpInput = document.getElementById('guestOtpInput');
                if (otpInput && document.getElementById('guestOtpModal').classList.contains('show')) {
                    otpInput.classList.add('is-invalid');
                    document.getElementById('guestOtpFeedback').textContent = result.message || "Mã OTP không đúng.";
                } else {
                    showBusinessErrorModal(result.message || "Đặt hàng thất bại. Vui lòng thử lại!");
                }
            }
        }
    } catch (error) {
        console.error('Error executing checkout:', error);
        if (confirmBtn) {
            confirmBtn.disabled = false;
            confirmBtn.textContent = "Xác nhận & Hoàn tất đặt hàng";
        }
        btn.disabled = false;
        btn.textContent = "Xác nhận đặt hàng";
        showBusinessErrorModal("Không thể kết nối đến máy chủ. Vui lòng kiểm tra lại đường truyền mạng.");
    }
}

function showBusinessErrorModal(message) {
    const modalEl = document.getElementById('businessErrorModal');
    if (!modalEl) {
        BSMSToast.error(message);
        setTimeout(() => { window.location.href = '/cart'; }, 1500);
        return;
    }
    
    const titleEl = document.getElementById('errorProductTitle');
    const descEl = document.getElementById('errorProductDescription');
    
    const match = message.match(/Tác phẩm '(.*?)'/);
    if (match && match[1]) {
        titleEl.textContent = match[0];
        descEl.textContent = "đã được bán hoặc giữ chỗ bởi khách hàng khác.";
    } else {
        titleEl.textContent = "Giao dịch không thành công";
        descEl.textContent = message;
    }
    
    const modal = new bootstrap.Modal(modalEl);
    modal.show();
}

function formatVND(value) {
    return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(value);
}
