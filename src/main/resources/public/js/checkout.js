document.addEventListener('DOMContentLoaded', () => {
    loadCheckoutSummary();
    
    const placeOrderBtn = document.getElementById('btnPlaceOrder');
    if (placeOrderBtn) {
        placeOrderBtn.addEventListener('click', placeOrder);
    }
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
        alert("Giỏ hàng của bạn đang trống! Không thể thanh toán.");
        window.location.href = '/cart';
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
    const custName = document.getElementById('custName').value.trim();
    const custPhone = document.getElementById('custPhone').value.trim();
    const custEmail = document.getElementById('custEmail').value.trim();
    const custAddress = document.getElementById('custAddress').value.trim();
    const notes = document.getElementById('orderNotes').value.trim();
    
    const paymentMethodEl = document.querySelector('input[name="paymentMethod"]:checked');
    const paymentMethod = paymentMethodEl ? paymentMethodEl.value : 'COD';
    
    if (!custName || !custPhone || !custEmail || !custAddress) {
        alert("Vui lòng điền đầy đủ các thông tin giao hàng bắt buộc!");
        return;
    }
    
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
            alert(`Đặt hàng thành công! Mã đơn của bạn là: ${result.orderCode}. Vui lòng chờ cuộc gọi từ Moderator để xác nhận phí xe cẩu/ship.`);
            if (typeof window.updateCartBadge === 'function') {
                window.updateCartBadge();
            }
            window.location.href = '/home';
        } else {
            alert(result.message || "Đặt hàng thất bại. Vui lòng thử lại!");
            btn.disabled = false;
            btn.textContent = "Xác nhận đặt hàng";
        }
    } catch (error) {
        console.error('Error placing order:', error);
        alert("Không thể kết nối đến máy chủ.");
        btn.disabled = false;
        btn.textContent = "Xác nhận đặt hàng";
    }
}

function formatVND(value) {
    return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(value);
}
