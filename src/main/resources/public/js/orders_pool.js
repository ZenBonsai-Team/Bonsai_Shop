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
        searchInput.addEventListener('input', (e) => {
            clearTimeout(searchDebounceTimer);
            searchDebounceTimer = setTimeout(() => {
                DashboardState.searchQuery = e.target.value;
                DashboardState.currentPage = 1;
                renderPool();
            }, 300);
        });
    }

    if (sortSelect) {
        sortSelect.addEventListener('change', (e) => {
            DashboardState.sortBy = e.target.value;
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
        console.error("Error fetching pool orders:", err);
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
                    <div>Kho đơn hàng chung hiện tại trống!</div>
                </td>
            </tr>
        `;
        return;
    }

    orders.forEach(order => {
        const tr = document.createElement('tr');
        const prodNameHtml = order.product ? `
            <span class="prod-name-link text-primary fw-bold text-decoration-underline" 
                  style="cursor: pointer;"
                  onclick="event.stopPropagation(); openProductDetailDrawer(${order.product.id})">
                ${order.product.name}
            </span>
        ` : '<span class="text-muted">Không có</span>';

        tr.innerHTML = `
            <td class="col-code">${order.orderCode}</td>
            <td><strong>${order.customer ? order.customer.name : 'N/A'}</strong></td>
            <td>${prodNameHtml}</td>
            <td class="col-price">${new Intl.NumberFormat('vi-VN', {style: 'currency', currency: 'VND'}).format(order.totalAmount)}</td>
            <td>${new Date(order.orderDate).toLocaleString('vi-VN')}</td>
            <td><span class="status-badge pending">${order.orderStatus}</span></td>
            <td class="text-center">
                <button class="btn btn-sm btn-success btn-claim-action" data-code="${order.orderCode}">
                    <i class="fa-solid fa-hand"></i> Nhận đơn
                </button>
            </td>
        `;

        tr.querySelector('.btn-claim-action').addEventListener('click', (e) => {
            e.stopPropagation();
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
        const response = await fetch(`/api/orders/${orderCode}/claim`, {
            method: 'POST',
            headers: headers
        });

        const result = await response.json();
        if (response.ok && result.success) {
            alert("Nhận đơn hàng thành công! Đơn hàng đã được chuyển vào mục Đơn Của Tôi.");
            renderPool();
        } else if (response.status === 409) {
            alert("Xung đột: Đơn hàng này đã bị một Moderator khác nhận trước đó!");
            renderPool();
        } else {
            alert(result.message || "Lỗi khi nhận đơn hàng.");
        }
    } catch (err) {
        console.error("Error claiming order:", err);
        alert("Có lỗi kết nối đến máy chủ.");
    }
}

function renderPagination(result) {
    const infoEl = document.getElementById('paginationInfo');
    const controlsEl = document.getElementById('paginationControls');
    
    if (infoEl) {
        infoEl.textContent = `Hiển thị ${result.orders ? result.orders.length : 0} trong tổng số ${result.totalCount || 0} đơn chờ trong Pool`;
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

        document.getElementById('prodDrawerName').textContent = data.productName;
        document.getElementById('prodDrawerCode').textContent = `Mã cây: ${data.productCode}`;
        document.getElementById('prodDrawerDesc').textContent = data.description || 'Không có mô tả chi tiết cho tác phẩm này.';
        
        const priceFormatted = new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(data.price || 0);
        document.getElementById('prodDrawerPrice').textContent = priceFormatted;

        document.getElementById('prodDrawerStyle').textContent = data.style || 'Chưa cập nhật';
        document.getElementById('prodDrawerAge').textContent = data.age ? `${data.age} năm` : 'Chưa cập nhật';
        document.getElementById('prodDrawerHeight').textContent = data.height ? `${data.height} cm` : 'Chưa cập nhật';
        document.getElementById('prodDrawerDiameter').textContent = data.trunkDiameter ? `${data.trunkDiameter} cm` : 'Chưa cập nhật';
        
        const imgUrl = data.imageUrl || '/images/default-tree.jpg';
        document.getElementById('prodDrawerImg').src = imgUrl;

        const statusEl = document.getElementById('prodDrawerStatus');
        statusEl.textContent = data.productStatus;
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
        console.error("Lỗi khi tải chi tiết sản phẩm:", error);
        document.getElementById('prodDrawerLoading').classList.add('d-none');
        document.getElementById('prodDrawerError').classList.remove('d-none');
        document.getElementById('prodDrawerErrorMessage').textContent = 
            error.message.includes('404') ? 'Không tìm thấy tác phẩm này trên hệ thống!' : 'Lỗi kết nối máy chủ, vui lòng kiểm tra lại mạng.';
    }
}

