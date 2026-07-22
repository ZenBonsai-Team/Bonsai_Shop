document.addEventListener('DOMContentLoaded', () => {
    loadCart();
});

async function loadCart() {
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
        renderCart(items);
        
        if (typeof window.updateCartBadge === 'function') {
            window.updateCartBadge();
        }
    } catch (error) {
        console.error('Error loading cart:', error);
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
        
        tr.innerHTML = `
            <td>
                <div class="d-flex align-items-center gap-3">
                    <img src="${imgUrl}" class="cart-prod-img" alt="${item.productName}">
                    <div>
                        <a href="/product/${item.productId}" class="cart-prod-name">${item.productName}</a>
                        <span class="text-muted small">Mã: Bonsai-${item.productId}</span>
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
    if (!confirm("Bạn có chắc chắn muốn xóa tác phẩm này khỏi giỏ hàng?")) return;
    
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
        } else {
            alert("Lỗi khi xóa sản phẩm.");
        }
    } catch (error) {
        console.error('Error removing item:', error);
        alert("Không thể kết nối đến máy chủ.");
    }
}

function formatVND(value) {
    return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(value);
}
