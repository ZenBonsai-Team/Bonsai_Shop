const DashboardState = {
    searchQuery: '',
    sortBy: 'date_desc',
    currentPage: 1,
    pageSize: 8
};

const csrfToken = document.querySelector('meta[name="_csrf"]')?.getAttribute('content');
const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.getAttribute('content');

document.addEventListener('DOMContentLoaded', () => {
    initPool();
});

function initPool() {
    const searchInput = document.getElementById('orderSearchInput');
    const sortSelect = document.getElementById('orderSortSelect');

    let searchDebounceTimer;
    if (searchInput) {
        searchInput.addEventListener('input', (event) => {
            clearTimeout(searchDebounceTimer);
            searchDebounceTimer = setTimeout(() => {
                DashboardState.searchQuery = event.target.value;
                DashboardState.currentPage = 1;
                renderPool();
            }, 300);
        });
    }

    if (sortSelect) {
        sortSelect.addEventListener('change', (event) => {
            DashboardState.sortBy = event.target.value;
            DashboardState.currentPage = 1;
            renderPool();
        });
    }

    renderPool();
}

async function renderPool() {
    const params = new URLSearchParams({
        search: DashboardState.searchQuery,
        sort: DashboardState.sortBy,
        page: DashboardState.currentPage,
        limit: DashboardState.pageSize
    });

    try {
        const response = await fetch(`/api/orders/pool?${params.toString()}`);
        if (!response.ok) return;
        const result = await response.json();

        renderTable(result.orders);
        renderPagination(result);
    } catch (err) {
        console.error('Lỗi khi tải kho đơn hàng chung:', err);
    }
}

function renderTable(orders) {
    const tableBody = document.getElementById('ordersTableBody');
    if (!tableBody) return;
    tableBody.innerHTML = '';

    if (!orders || orders.length === 0) {
        tableBody.innerHTML = `
            <tr>
                <td colspan="7" class="text-center p-4">
                    <div class="text-muted"><i class="fa-solid fa-inbox fa-2x mb-2"></i></div>
                    <div>Kho đơn hàng chung hiện tại trống.</div>
                </td>
            </tr>
        `;
        return;
    }

    orders.forEach(order => {
        const tr = document.createElement('tr');
        tr.innerHTML = `
            <td class="col-code">${escapeHtml(order.orderCode || '-')}</td>
            <td><strong>${escapeHtml(order.customer ? order.customer.name : '-')}</strong></td>
            <td><span class="fw-bold text-dark">${order.items ? order.items.length : 1} cây</span></td>
            <td class="col-price">${new Intl.NumberFormat('vi-VN', {style: 'currency', currency: 'VND'}).format(order.totalAmount || 0)}</td>
            <td>${order.orderDate ? new Date(order.orderDate).toLocaleString('vi-VN') : '-'}</td>
            <td><span class="status-badge pending">${escapeHtml(window.OrderModeratorLabels?.orderStatus(order.orderStatus) || '-')}</span></td>
            <td class="text-center">
                <button class="btn btn-sm btn-success btn-claim-action" data-code="${escapeHtml(order.orderCode || '')}">
                    <i class="fa-solid fa-hand"></i> Tiếp nhận đơn hàng
                </button>
            </td>
        `;

        tr.querySelector('.btn-claim-action').addEventListener('click', (event) => {
            event.stopPropagation();
            claimOrder(order.orderCode);
        });

        tableBody.appendChild(tr);
    });
}

async function claimOrder(orderCode) {
    const headers = { 'Content-Type': 'application/json' };
    if (csrfHeader && csrfToken) {
        headers[csrfHeader] = csrfToken;
    }

    try {
        const response = await fetch(`/api/orders/${encodeURIComponent(orderCode)}/claim`, {
            method: 'POST',
            headers
        });

        const result = await response.json();
        if (response.ok && result.success) {
            showSuccess('Tiếp nhận đơn hàng thành công. Đơn hàng đã được chuyển vào mục Đơn của tôi.');
            renderPool();
        } else if (response.status === 409) {
            showError('Đơn hàng này đã được nhân viên khác tiếp nhận trước đó.');
            renderPool();
        } else {
            showError(result.message || 'Không thể tiếp nhận đơn hàng. Vui lòng thử lại.');
        }
    } catch (err) {
        console.error('Lỗi khi tiếp nhận đơn hàng:', err);
        showError('Có lỗi kết nối đến máy chủ. Vui lòng thử lại.');
    }
}

function renderPagination(result) {
    const infoEl = document.getElementById('paginationInfo');
    const controlsEl = document.getElementById('paginationControls');

    if (infoEl) {
        infoEl.textContent = `Hiển thị ${result.orders ? result.orders.length : 0} trong tổng số ${result.totalCount || 0} đơn hàng chờ tiếp nhận`;
    }
    if (!controlsEl) return;
    controlsEl.innerHTML = '';
    if (!result.pages || result.pages <= 1) return;

    for (let i = 1; i <= result.pages; i++) {
        const btn = document.createElement('button');
        btn.className = `btn btn-sm mx-1 ${DashboardState.currentPage === i ? 'btn-primary' : 'btn-outline-secondary'}`;
        btn.textContent = i;
        btn.addEventListener('click', () => {
            DashboardState.currentPage = i;
            renderPool();
        });
        controlsEl.appendChild(btn);
    }
}

async function openProductDetailDrawer(productId) {
    const drawerEl = document.getElementById('productDetailDrawer');
    if (!drawerEl) return;
    const bsOffcanvas = bootstrap.Offcanvas.getOrCreateInstance(drawerEl);

    document.getElementById('prodDrawerLoading').classList.remove('d-none');
    document.getElementById('prodDrawerError').classList.add('d-none');
    document.getElementById('prodDrawerContent').classList.add('d-none');

    bsOffcanvas.show();

    try {
        const response = await fetch(`/api/products/${productId}`);
        if (!response.ok) {
            throw new Error(`Mã lỗi hệ thống: ${response.status}`);
        }
        const data = await response.json();

        document.getElementById('prodDrawerName').textContent = data.productName || '-';
        document.getElementById('prodDrawerCode').textContent = `Mã cây: ${data.productCode || '-'}`;
        document.getElementById('prodDrawerDesc').textContent = data.description || 'Không có mô tả chi tiết cho tác phẩm này.';

        const priceFormatted = new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(data.price || 0);
        document.getElementById('prodDrawerPrice').textContent = priceFormatted;

        document.getElementById('prodDrawerStyle').textContent = data.style || 'Chưa cập nhật';
        document.getElementById('prodDrawerAge').textContent = data.age ? `${data.age} năm` : 'Chưa cập nhật';
        document.getElementById('prodDrawerHeight').textContent = data.height ? `${data.height} cm` : 'Chưa cập nhật';
        document.getElementById('prodDrawerDiameter').textContent = data.trunkDiameter ? `${data.trunkDiameter} cm` : 'Chưa cập nhật';

        document.getElementById('prodDrawerImg').src = data.imageUrl || '/images/default-tree.jpg';

        const statusEl = document.getElementById('prodDrawerStatus');
        statusEl.textContent = productStatusLabel(data.productStatus);
        if (data.productStatus === 'AVAILABLE') {
            statusEl.className = 'badge bg-success';
        } else if (data.productStatus === 'RESERVED') {
            statusEl.className = 'badge bg-warning text-dark';
        } else {
            statusEl.className = 'badge bg-danger';
        }

        document.getElementById('prodDrawerLoading').classList.add('d-none');
        document.getElementById('prodDrawerContent').classList.remove('d-none');
    } catch (error) {
        console.error('Lỗi khi tải chi tiết sản phẩm:', error);
        document.getElementById('prodDrawerLoading').classList.add('d-none');
        document.getElementById('prodDrawerError').classList.remove('d-none');
        document.getElementById('prodDrawerErrorMessage').textContent =
            error.message.includes('404') ? 'Không tìm thấy tác phẩm này trên hệ thống.' : 'Lỗi kết nối máy chủ, vui lòng kiểm tra lại mạng.';
    }
}

function productStatusLabel(status) {
    const labels = {
        AVAILABLE: 'Có thể bán',
        RESERVED: 'Đang được giữ cho đơn hàng',
        SOLD: 'Đã bán'
    };
    return labels[String(status || '').toUpperCase()] || 'Chưa xác định';
}

function showSuccess(message) {
    if (typeof BSMSToast !== 'undefined') {
        BSMSToast.success(message);
    } else {
        alert(message);
    }
}

function showError(message) {
    if (typeof BSMSToast !== 'undefined') {
        BSMSToast.error(message);
    } else {
        alert(message);
    }
}

function escapeHtml(value) {
    return String(value ?? '')
        .replaceAll('&', '&amp;')
        .replaceAll('<', '&lt;')
        .replaceAll('>', '&gt;')
        .replaceAll('"', '&quot;')
        .replaceAll("'", '&#039;');
}
