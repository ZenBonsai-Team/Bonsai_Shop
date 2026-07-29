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
            otpInput.classList.remove('is-invalid');
        });
    }
    
    // Đăng ký xóa trạng thái lỗi khi người dùng nhập liệu lại
    const inputs = ['custName', 'custPhone', 'custEmail', 'custAddress'];
    inputs.forEach(id => {
        const inputEl = document.getElementById(id);
        if (inputEl) {
            inputEl.addEventListener('input', () => {
                inputEl.classList.remove('is-invalid');
            });
        }
    });
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
    const paymentMethod = paymentMethodEl ? paymentMethodEl.value : 'COD';
    
    // --- BẮT ĐẦU VALIDATE PHÍA CLIENT ---
    let hasError = false;
    let firstErrorField = null;
    
    if (!custName) {
        custNameEl.classList.add('is-invalid');
        hasError = true;
        if (!firstErrorField) firstErrorField = custNameEl;
    } else {
        custNameEl.classList.remove('is-invalid');
    }
    
    const phoneRegex = /^(0|\+84)(\d{9})$/;
    if (!custPhone) {
        custPhoneEl.classList.add('is-invalid');
        document.getElementById('phoneFeedback').textContent = "Vui lòng nhập số điện thoại liên lạc.";
        hasError = true;
        if (!firstErrorField) firstErrorField = custPhoneEl;
    } else if (!phoneRegex.test(custPhone)) {
        custPhoneEl.classList.add('is-invalid');
        document.getElementById('phoneFeedback').textContent = "Số điện thoại không hợp lệ (Cần 10 chữ số bắt đầu bằng 0 hoặc +84).";
        hasError = true;
        if (!firstErrorField) firstErrorField = custPhoneEl;
    } else {
        custPhoneEl.classList.remove('is-invalid');
    }
    
    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    if (!custEmail) {
        custEmailEl.classList.add('is-invalid');
        document.getElementById('emailFeedback').textContent = "Vui lòng nhập địa chỉ email.";
        hasError = true;
        if (!firstErrorField) firstErrorField = custEmailEl;
    } else if (!emailRegex.test(custEmail)) {
        custEmailEl.classList.add('is-invalid');
        document.getElementById('emailFeedback').textContent = "Địa chỉ email không hợp lệ (ví dụ: name@domain.com).";
        hasError = true;
        if (!firstErrorField) firstErrorField = custEmailEl;
    } else {
        custEmailEl.classList.remove('is-invalid');
    }
    
    if (!custAddress) {
        custAddressEl.classList.add('is-invalid');
        hasError = true;
        if (!firstErrorField) firstErrorField = custAddressEl;
    } else {
        custAddressEl.classList.remove('is-invalid');
    }
    
    if (hasError) {
        const toastEl = document.getElementById('validationToast');
        if (toastEl) {
            const toast = new bootstrap.Toast(toastEl);
            toast.show();
        }
        
        if (firstErrorField) {
            firstErrorField.scrollIntoView({ behavior: 'smooth', block: 'center' });
            firstErrorField.focus();
        }
        return;
    }
    // --- KẾT THÚC VALIDATE PHÍA CLIENT ---

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
