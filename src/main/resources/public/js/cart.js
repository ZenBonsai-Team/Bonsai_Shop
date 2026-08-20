let loadedCartItems = [];

document.addEventListener('DOMContentLoaded', () => {
    loadCart();

    const btnProceed = document.getElementById('btnProceedCheckout');
    if (btnProceed) {
        btnProceed.addEventListener('click', (e) => {
            e.preventDefault();
            if (!loadedCartItems || loadedCartItems.length === 0) {
                BSMSToast.warning("Giỏ hàng của bạn đang trống! Vui lòng chọn tác phẩm trước.");
                return;
            }
            const unavailableItem = loadedCartItems.find(item => item.productStatus && item.productStatus !== 'AVAILABLE');
            if (unavailableItem) {
                BSMSToast.error(`Tác phẩm "${unavailableItem.productName}" đã có người đặt mua hoặc giữ chỗ. Vui lòng xóa khỏi giỏ hàng trước khi đặt.`);
                return;
            }
            window.location.href = '/checkout';
        });
    }
});

async function loadCart() {
    try {
        const response = await fetch('/api/cart');
        if (response.ok) {
            const items = await response.json();
            if (items && items.length > 0) {
                loadedCartItems = items;
                renderCart(items);
                if (typeof window.updateCartBadge === 'function') {
                    window.updateCartBadge();
                }
                return;
            }
        }
    } catch (error) {
    }

    // Fallback cho Khách vãng lai (Guest Cart)
    const guestCart = JSON.parse(localStorage.getItem('bonsai_guest_cart') || '[]');
    if (guestCart.length > 0) {
        const items = [];
        for (const pId of guestCart) {
            try {
                const res = await fetch(`/api/products/${pId}`);
                if (res.ok) {
                    const prod = await res.json();
                    items.push({
                        productId: prod.productId,
                        productName: prod.productName,
                        productImage: prod.imageUrl,
                        price: prod.price,
                        productStatus: prod.productStatus
                    });
                }
            } catch (err) {}
        }
        loadedCartItems = items;
        renderCart(items);
    } else {
        loadedCartItems = [];
        renderCart([]);
    }

    if (typeof window.updateCartBadge === 'function') {
        window.updateCartBadge();
    }
}

function renderCart(items) {
    const tableBody = document.getElementById('cartTableBody');
    const mainContent = document.getElementById('cartMainContent');
    const emptyState = document.getElementById('emptyCartState');
    
    if (!tableBody) return;
    tableBody.innerHTML = '';
    
    if (!items || items.length === 0) {
        mainContent.classList.add('d-none');
        emptyState.classList.remove('d-none');
        return;
    }
    
    mainContent.classList.remove('d-none');
    emptyState.classList.add('d-none');
    
    let subtotal = 0;
    
    items.forEach(item => {
        const tr = document.createElement('tr');
        const itemTotal = (item.price || 0) * (item.quantity || 1);
        subtotal += itemTotal;
        
        const imgUrl = item.productImage || 'https://images.unsplash.com/photo-1599599810769-bcde5a160d32?auto=format&fit=crop&q=80&w=600';
        const isUnavailable = item.productStatus && item.productStatus !== 'AVAILABLE';
        const statusBadge = isUnavailable
            ? `<span class="badge bg-danger ms-2"><i class="fa-solid fa-lock me-1"></i>${item.productStatus === 'RESERVED' ? 'Đã giữ chỗ' : 'Đã bán'}</span>`
            : '';

        if (isUnavailable) {
            tr.classList.add('table-danger');
        }

        tr.innerHTML = `
            <td>
                <div class="d-flex align-items-center gap-3">
                    <img src="${imgUrl}" class="cart-prod-img" alt="${item.productName}">
                    <div>
                        <a href="/product/${item.productId}" class="cart-prod-name">${item.productName}</a>
                        <div class="d-flex align-items-center gap-1">
                            <span class="text-muted small">Mã: Bonsai-${item.productId}</span>
                            ${statusBadge}
                        </div>
                    </div>
                </div>
            </td>
            <td>
                <span class="cart-price">${formatVND(item.price || 0)}</span>
            </td>
            <td>
                <button class="btn-remove-item" onclick="removeItem(${item.productId})" title="Xóa khỏi giỏ">
                    <i class="fa-regular fa-trash-can"></i>
                </button>
            </td>
        `;
        tableBody.appendChild(tr);
    });
    
    document.getElementById('cartSubtotal').textContent = formatVND(subtotal);
    document.getElementById('cartTotal').textContent = formatVND(subtotal);
}

async function removeItem(productId) {
    BSMSConfirm({
        title: "Xóa khỏi giỏ hàng?",
        message: "Bạn có chắc chắn muốn xóa tác phẩm này khỏi giỏ hàng?",
        type: "danger",
        confirmText: "Xóa tác phẩm",
        cancelText: "Bỏ qua",
        onConfirm: async () => {
            const csrfToken = document.querySelector('meta[name="_csrf"]')?.getAttribute('content');
            const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.getAttribute('content');
            
            const headers = { 'Content-Type': 'application/json' };
            if (csrfHeader && csrfToken) headers[csrfHeader] = csrfToken;
            
            try {
                const response = await fetch(`/api/cart/items/${productId}`, {
                    method: 'DELETE',
                    headers: headers
                });
                
                if (response.ok) {
                    loadCart();
                    BSMSToast.success("Đã xóa tác phẩm khỏi giỏ hàng.");
                } else {
                    // Xóa trong LocalStorage của Guest
                    let guestCart = JSON.parse(localStorage.getItem('bonsai_guest_cart') || '[]');
                    guestCart = guestCart.filter(id => id !== productId);
                    localStorage.setItem('bonsai_guest_cart', JSON.stringify(guestCart));
                    loadCart();
                    BSMSToast.success("Đã xóa tác phẩm khỏi giỏ hàng.");
                }
            } catch (err) {
                console.error("Lỗi khi xóa sản phẩm:", err);
                let guestCart = JSON.parse(localStorage.getItem('bonsai_guest_cart') || '[]');
                guestCart = guestCart.filter(id => id !== productId);
                localStorage.setItem('bonsai_guest_cart', JSON.stringify(guestCart));
                loadCart();
                BSMSToast.success("Đã xóa tác phẩm khỏi giỏ hàng.");
            }
        }
    });
}

function formatVND(value) {
    return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(value);
}
