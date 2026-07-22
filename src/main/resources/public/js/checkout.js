document.addEventListener('DOMContentLoaded', () => {
    loadCheckoutSummary();
    
    const placeOrderBtn = document.getElementById('btnPlaceOrder');
    if (placeOrderBtn) {
        placeOrderBtn.addEventListener('click', placeOrder);
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
    try {
        const response = await fetch('/api/cart');
        if (response.status === 401) {
            window.location.href = '/login';
            return;
        }
        if (!response.ok) {
            throw new Error('Failed to fetch cart');
        }
        
        const items = await response.json();
        renderSummary(items);
    } catch (error) {
        console.error('Error loading checkout summary:', error);
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
            alert("Giỏ hàng của bạn đang trống! Không thể thanh toán.");
            window.location.href = '/cart';
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
                <span class="summary-item-qty d-block">Số lượng: ${item.quantity || 1}</span>
            </div>
            <span class="summary-item-price">${formatVND(itemTotal)}</span>
        `;
        container.appendChild(div);
    });
    
    document.getElementById('checkoutSubtotal').textContent = formatVND(subtotal);
}

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
    
    // --- BẮT ĐẦU VALIDATE PHÍA CLIENT (Inline Validation) ---
    let hasError = false;
    let firstErrorField = null;
    
    // 1. Kiểm tra Họ tên
    if (!custName) {
        custNameEl.classList.add('is-invalid');
        hasError = true;
        if (!firstErrorField) firstErrorField = custNameEl;
    } else {
        custNameEl.classList.remove('is-invalid');
    }
    
    // 2. Kiểm tra Số điện thoại
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
    
    // 3. Kiểm tra Email
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
    
    // 4. Kiểm tra Địa chỉ
    if (!custAddress) {
        custAddressEl.classList.add('is-invalid');
        hasError = true;
        if (!firstErrorField) firstErrorField = custAddressEl;
    } else {
        custAddressEl.classList.remove('is-invalid');
    }
    
    // Xử lý khi có lỗi validate
    if (hasError) {
        // Kích hoạt Toast thông báo chung
        const toastEl = document.getElementById('validationToast');
        if (toastEl) {
            const toast = new bootstrap.Toast(toastEl);
            toast.show();
        }
        
        // Focus và cuộn mượt tới trường lỗi đầu tiên
        if (firstErrorField) {
            firstErrorField.scrollIntoView({ behavior: 'smooth', block: 'center' });
            firstErrorField.focus();
        }
        return;
    }
    // --- KẾT THÚC VALIDATE PHÍA CLIENT ---
    
    const csrfToken = document.querySelector('meta[name="_csrf"]')?.getAttribute('content');
    const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.getAttribute('content');
    
    const headers = { 'Content-Type': 'application/json' };
    if (csrfHeader && csrfToken) headers[csrfHeader] = csrfToken;
    
    const payload = {
        customerName: custName,
        customerPhone: custPhone,
        customerEmail: custEmail,
        shippingAddress: custAddress,
        notes: notes,
        paymentMethod: paymentMethod
    };
    
    const btn = document.getElementById('btnPlaceOrder');
    btn.disabled = true;
    btn.textContent = "Đang xử lý đặt hàng...";
    
    try {
        const response = await fetch('/api/orders/checkout', {
            method: 'POST',
            headers: headers,
            body: JSON.stringify(payload)
        });
        
        const result = await response.json();
        
        if (response.ok && result.success) {
            if (typeof window.updateCartBadge === 'function') {
                window.updateCartBadge();
            }
            // Chuyển hướng sang trang Success chuyên nghiệp
            window.location.href = `/order/success?orderCode=${result.orderCode}`;
        } else {
            // Case 2: Business Error
            showBusinessErrorModal(result.message || "Đặt hàng thất bại. Vui lòng thử lại!");
            btn.disabled = false;
            btn.textContent = "Xác nhận đặt hàng";
        }
    } catch (error) {
        console.error('Error placing order:', error);
        showBusinessErrorModal("Không thể kết nối đến máy chủ. Vui lòng kiểm tra lại đường truyền mạng.");
        btn.disabled = false;
        btn.textContent = "Xác nhận đặt hàng";
    }
}

/**
 * Hiển thị Modal báo lỗi nghiệp vụ thay thế cho alert()
 */
function showBusinessErrorModal(message) {
    const modalEl = document.getElementById('businessErrorModal');
    if (!modalEl) {
        alert(message);
        window.location.href = '/cart';
        return;
    }
    
    const titleEl = document.getElementById('errorProductTitle');
    const descEl = document.getElementById('errorProductDescription');
    
    // Kiểm tra định dạng lỗi sản phẩm bị bán/giữ chỗ
    const match = message.match(/Tác phẩm '(.*?)'/);
    if (match && match[1]) {
        titleEl.textContent = match[0]; // Ví dụ: Tác phẩm 'Si Nhật Đậu Chậu Gốm'
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
