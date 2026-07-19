/**
 * Main Controller for the Order Moderator Dashboard Page
 * Under: src/main/resources/public/js/moderator_orders.js
 */

// Central Page State Manager
const DashboardState = {
    searchQuery: '',
    selectedStatus: 'ALL',
    sortBy: 'date_desc',
    currentPage: 1,
    pageSize: 8
};

document.addEventListener('DOMContentLoaded', () => {
    // Initial setup and render
    initApp();
});

/**
 * Attaches events listeners and triggers initial render
 */
function initApp() {
    const searchInput = document.getElementById('orderSearchInput');
    const sortSelect = document.getElementById('orderSortSelect');
    const tabContainer = document.getElementById('statusFilterTabs');

    // Search input handler with 300ms debounce
    let searchDebounceTimer;
    searchInput.addEventListener('input', (e) => {
        clearTimeout(searchDebounceTimer);
        searchDebounceTimer = setTimeout(() => {
            DashboardState.searchQuery = e.target.value;
            DashboardState.currentPage = 1; // Reset to page 1 on search
            renderDashboard();
        }, 300);
    });

    // Sort select change handler
    sortSelect.addEventListener('change', (e) => {
        DashboardState.sortBy = e.target.value;
        DashboardState.currentPage = 1;
        renderDashboard();
    });

    // Status Tab buttons handler
    tabContainer.addEventListener('click', (e) => {
        const btn = e.target.closest('.tab-btn');
        if (!btn) return;

        // Toggle Active tab CSS classes
        tabContainer.querySelectorAll('.tab-btn').forEach(t => t.classList.remove('active'));
        btn.classList.add('active');

        // Update state and refresh
        DashboardState.selectedStatus = btn.dataset.status;
        DashboardState.currentPage = 1;

        renderDashboard();
    });

    // Initialize components
    ConfirmDialog.init();
    OrderDrawer.init();

    // Trigger initial render
    renderDashboard();
}

/**
 * Performs state queries, updates KPIs, updates data table, and draws pagination
 */
async function renderDashboard() {
    // 1. Fetch data from Service API Layer
    const result = await apiFetchOrders({
        search: DashboardState.searchQuery,
        status: DashboardState.selectedStatus,
        sort: DashboardState.sortBy,
        page: DashboardState.currentPage,
        limit: DashboardState.pageSize
    });

    // 2. Render KPIs counts
    updateKPIs();

    // 3. Render Data Table
    renderTable(result.orders);

    // 4. Render Pagination Controls
    renderPagination(result);
}

/**
 * Dynamically computes and updates the top KPI indicators from active database
 */
async function updateKPIs() {
    try {
        const response = await fetch('/api/orders/kpis');
        if(!response.ok){
            throw new Error('Không thể lấy dữ liệu KPIs');
        }

        const data = await response.json();
    
        document.getElementById('kpiTotalCount').textContent = data.total || 0;
        document.getElementById('kpiPendingCount').textContent = data.pending || 0;
        document.getElementById('kpiApprovedCount').textContent = data.approved || 0;
        document.getElementById('kpiPaidCount').textContent = data.paid || 0;
        document.getElementById('kpiRejectedCount').textContent = data.rejected || 0;
    } catch (error) {
        console.error("Không thể cập nhật số liệu KPIs: ", error);
    }
}

/**
 * Clears and fills table rows with orders
 * 
 * @param {Array} orders List of orders to draw
 */
function renderTable(orders) {
    const tableBody = document.getElementById('ordersTableBody');
    tableBody.innerHTML = '';

    if (orders.length === 0) {
        tableBody.innerHTML = `
            <tr>
                <td colspan="7" class="empty-state">
                    <div class="empty-state-icon"><i class="fa-solid fa-ban"></i></div>
                    <div class="empty-state-title">Không tìm thấy đơn hàng nào</div>
                    <div class="empty-state-desc">Hãy thử đổi từ khóa tìm kiếm hoặc lọc trạng thái khác.</div>
                </td>
            </tr>
        `;
        return;
    }

    orders.forEach(order => {
        const tr = document.createElement('tr');
        
        // Setup row click trigger to open Detail Drawer
        tr.addEventListener('click', () => {
            OrderDrawer.open(order.orderCode, () => {
                // Refresh dashboard when drawer actions finish successfully
                renderDashboard();
            });
        });

        tr.innerHTML = `
            <td class="col-code">${order.orderCode}</td>
            <td class="col-customer">
                <strong>${escapeHtml(order.customer.name)}</strong>
                <span class="cust-phone">${escapeHtml(order.customer.phone)}</span>
            </td>
            <td class="col-product">
                ${order.product ? `
                <span class="prod-name-link" 
                      style="cursor: pointer; font-weight: 600; text-decoration: underline; color: var(--primary-color);" 
                      onclick="event.stopPropagation(); openProductDetailDrawer(${order.product.id})">
                    ${escapeHtml(order.product.name)}
                </span>
                ` : '<span class="text-muted">Không có sản phẩm</span>'}
            </td>
            <td class="col-price">${formatVND(order.totalAmount)}</td>
            <td>${formatDate(order.orderDate)}</td>
            <td>
                <span class="status-badge ${order.orderStatus.toLowerCase()}">${order.orderStatus}</span>
            </td>
            <td>
                <button class="page-btn active" style="padding: 4px 8px; font-size:12px;"><i class="fa-solid fa-eye"></i> Xem chi tiết</button>
            </td>
        `;

        tableBody.appendChild(tr);
    });
}

/**
 * Draws previous, next, and numerical pagination page controls
 * 
 * @param {Object} result Paginated object returned from API
 */
function renderPagination(result) {
    const infoEl = document.getElementById('paginationInfo');
    const controlsEl = document.getElementById('paginationControls');

    // 1. Calculate showing text
    const totalCount = result.totalCount;
    const limit = DashboardState.pageSize;
    const start = totalCount === 0 ? 0 : (DashboardState.currentPage - 1) * limit + 1;
    const end = Math.min(DashboardState.currentPage * limit, totalCount);

    infoEl.textContent = `Hiển thị ${start} đến ${end} trong tổng số ${totalCount} đơn hàng`;

    // 2. Draw Page buttons
    controlsEl.innerHTML = '';
    if (result.pages <= 1) return; // No pagination controls needed if single page

    // Previous Button
    const prevBtn = document.createElement('button');
    prevBtn.className = 'page-btn';
    prevBtn.textContent = ' Trước';
    prevBtn.disabled = DashboardState.currentPage === 1;
    prevBtn.addEventListener('click', () => {
        DashboardState.currentPage--;
        renderDashboard();
    });
    controlsEl.appendChild(prevBtn);

    // Numbered Buttons
    for (let i = 1; i <= result.pages; i++) {
        const pageBtn = document.createElement('button');
        pageBtn.className = `page-btn ${DashboardState.currentPage === i ? 'active' : ''}`;
        pageBtn.textContent = i;
        pageBtn.addEventListener('click', () => {
            DashboardState.currentPage = i;
            renderDashboard();
        });
        controlsEl.appendChild(pageBtn);
    }

    // Next Button
    const nextBtn = document.createElement('button');
    nextBtn.className = 'page-btn';
    nextBtn.textContent = 'Sau ';
    nextBtn.disabled = DashboardState.currentPage === result.pages;
    nextBtn.addEventListener('click', () => {
        DashboardState.currentPage++;
        renderDashboard();
    });
    controlsEl.appendChild(nextBtn);
}

/**
 * Formats a raw ISO date string to a neat readable text
 * 
 * @param {string} dateStr ISO Date string
 * @returns {string} Formatted output (dd/MM/yyyy HH:mm)
 */
function formatDate(dateStr) {
    if (!dateStr) return '-';
    const date = new Date(dateStr);
    
    const day = String(date.getDate()).padStart(2, '0');
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const year = date.getFullYear();
    const hours = String(date.getHours()).padStart(2, '0');
    const minutes = String(date.getMinutes()).padStart(2, '0');

    return `${day}/${month}/${year} ${hours}:${minutes}`;
}

/**
 * Currency Formatting to VND
 */
function formatVND(num) {
    return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(num);
}

/**
 * Simple HTML sanitizer to prevent XSS issues
 */
function escapeHtml(text) {
    if (!text) return '';
    const map = {
        '&': '&amp;',
        '<': '&lt;',
        '>': '&gt;',
        '"': '&quot;',
        "'": '&#039;'
    };
    return text.toString().replace(/[&<>"']/g, function(m) { return map[m]; });
}

/**
 * Mở Side Drawer và tải thông tin chi tiết của cây cảnh dựa trên ProductID
 * @param {number} productId ID của sản phẩm cần lấy dữ liệu
 */
async function openProductDetailDrawer(productId) {
    const drawerEl = document.getElementById('productDetailDrawer');
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
        
        const priceFormatted = new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(data.price);
        document.getElementById('prodDrawerPrice').textContent = priceFormatted;

        document.getElementById('prodDrawerStyle').textContent = data.style || 'Chưa cập nhật';
        document.getElementById('prodDrawerAge').textContent = data.age ? `${data.age} năm` : 'Chưa cập nhật';
        document.getElementById('prodDrawerHeight').textContent = data.height ? `${data.height} cm` : 'Chưa cập nhật';
        document.getElementById('prodDrawerDiameter').textContent = data.trunkDiameter ? `${data.trunkDiameter} cm` : 'Chưa cập nhật';
        
        const imgUrl = data.imageUrl || 'https://images.unsplash.com/photo-1599599810769-bcde5a160d32?auto=format&fit=crop&q=80&w=800';
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
