const DashboardState = {
    searchQuery: '',
    selectedStatus: 'ALL',
    sortBy: 'date_desc',
    currentPage: 1,
    pageSize: 8
};

const orderStatusLabels = {
    'PENDING': 'Chờ xử lý',
    'APPROVED': 'Đã duyệt',
    'PAID': 'Đã thanh toán',
    'REJECTED': 'Đã từ chối',
    'COMPLETED': 'Hoàn thành',
    'CANCELLED': 'Đã hủy',
    'CONFIRMED': 'Đã xác nhận',
    'SHIPPING': 'Đang giao',
    'DELIVERED': 'Đã giao'
};

let activeOrderCode = null;
let currentActiveOrder = null;
const csrfToken = document.querySelector('meta[name="_csrf"]')?.getAttribute('content');
const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.getAttribute('content');

document.addEventListener('DOMContentLoaded', () => {
    initApp();
});

function initApp() {
    const searchInput = document.getElementById('orderSearchInput');
    const sortSelect = document.getElementById('orderSortSelect');
    const tabContainer = document.getElementById('statusFilterTabs');

    let searchDebounceTimer;
    if (searchInput) {
        searchInput.addEventListener('input', (e) => {
            clearTimeout(searchDebounceTimer);
            searchDebounceTimer = setTimeout(() => {
                DashboardState.searchQuery = e.target.value;
                DashboardState.currentPage = 1;
                renderDashboard();
            }, 300);
        });
    }

    if (sortSelect) {
        sortSelect.addEventListener('change', (e) => {
            DashboardState.sortBy = e.target.value;
            DashboardState.currentPage = 1;
            renderDashboard();
        });
    }

    if (tabContainer) {
        tabContainer.addEventListener('click', (e) => {
            const btn = e.target.closest('.tab-btn');
            if (!btn) return;
            tabContainer.querySelectorAll('.tab-btn').forEach(t => t.classList.remove('active'));
            btn.classList.add('active');
            DashboardState.selectedStatus = btn.dataset.status;
            DashboardState.currentPage = 1;
            renderDashboard();
        });
    }

    initDrawerEvents();
    renderDashboard();
}

async function renderDashboard() {
    updatePersonalKPIs();

    const params = new URLSearchParams({
        search: DashboardState.searchQuery,
        status: DashboardState.selectedStatus,
        sort: DashboardState.sortBy,
        page: DashboardState.currentPage,
        limit: DashboardState.pageSize
    });

    try {
        const response = await fetch(`/api/orders/my?${params.toString()}`);
        if (!response.ok) return;
        const result = await response.json();

        renderTable(result.orders);
        renderPagination(result);
    } catch (err) {
        console.error("Lỗi khi tải danh sách đơn hàng:", err);
    }
}

async function updatePersonalKPIs() {
    try {
        const response = await fetch('/api/orders/my-stats');
        if (!response.ok) return;
        const data = await response.json();

        document.getElementById('kpiTotalCount').textContent = data.total || 0;
        document.getElementById('kpiPendingCount').textContent = data.pending || 0;
        document.getElementById('kpiApprovedCount').textContent = data.approved || 0;
        document.getElementById('kpiPaidCount').textContent = data.paid || 0;
        document.getElementById('kpiRejectedCount').textContent = data.rejected || 0;
    } catch (err) {
        console.error("Lỗi cập nhật KPI cá nhân:", err);
    }
}

function renderTable(orders) {
    const tableBody = document.getElementById('ordersTableBody');
    if (!tableBody) return;
    tableBody.innerHTML = '';

    if (!orders || orders.length === 0) {
        tableBody.innerHTML = `
            <tr>
                <td colspan="7" class="text-center p-4">Không tìm thấy đơn hàng nào trong mục Đơn Của Tôi.</td>
            </tr>
        `;
        return;
    }

    orders.forEach(order => {
        const tr = document.createElement('tr');
        tr.style.cursor = 'pointer';
        tr.addEventListener('click', () => openDrawer(order));

        tr.innerHTML = `
            <td class="col-code">${order.orderCode}</td>
            <td><strong>${order.customer ? order.customer.name : 'N/A'}</strong></td>
            <td><span class="fw-bold text-dark">${order.items ? order.items.length : 1} cây</span></td>
            <td class="col-price">${formatVND(order.totalAmount || 0)}</td>
            <td>${new Date(order.orderDate).toLocaleString('vi-VN')}</td>
            <td><span class="status-badge ${order.orderStatus.toLowerCase()}">${orderStatusLabels[order.orderStatus] || order.orderStatus}</span></td>
            <td class="text-center">
                <button class="btn btn-sm btn-outline-primary"><i class="fa-solid fa-eye"></i> Chi tiết</button>
            </td>
        `;

        tableBody.appendChild(tr);
    });
}

function initDrawerEvents() {
    const backdrop = document.getElementById('drawerBackdrop');
    const closeBtn = document.getElementById('btnDrawerClose');
    const unclaimBtn = document.getElementById('btnUnclaimOrder');
    const verifyBtn = document.getElementById('btnVerifyOrder');
    const rejectBtn = document.getElementById('btnRejectOrder');
    const rejectConfirmBtn = document.getElementById('btnRejectConfirm');
    const rejectCancelBtn = document.getElementById('btnRejectCancel');

    const craneInput = document.getElementById('inputCraneFee');
    const shipInput = document.getElementById('inputShippingFee');

    if (backdrop) backdrop.addEventListener('click', closeDrawer);
    if (closeBtn) closeBtn.addEventListener('click', closeDrawer);

    if (craneInput) craneInput.addEventListener('input', updateLiveTotals);
    if (shipInput) shipInput.addEventListener('input', updateLiveTotals);
    const depositInput = document.getElementById('inputDepositAmount');
    if (depositInput) depositInput.addEventListener('input', updateLiveTotals);

    if (unclaimBtn) {
        unclaimBtn.addEventListener('click', () => {
            if (confirm("Bạn có chắc chắn muốn trả lại đơn hàng này về Kho đơn chung (Pool)?")) {
                unclaimOrder(activeOrderCode);
            }
        });
    }

    if (verifyBtn) {
        verifyBtn.addEventListener('click', () => {
            verifyOrder(activeOrderCode);
        });
    }

    if (rejectBtn) {
        rejectBtn.addEventListener('click', () => {
            const box = document.getElementById('rejectReasonBox');
            if (box) {
                box.style.display = 'block';
                box.scrollIntoView({ behavior: 'smooth' });
                const textarea = document.getElementById('textareaRejectReason');
                if (textarea) textarea.focus();
            }
        });
    }

    if (rejectCancelBtn) {
        rejectCancelBtn.addEventListener('click', () => {
            const box = document.getElementById('rejectReasonBox');
            if (box) box.style.display = 'none';
        });
    }

    if (rejectConfirmBtn) {
        rejectConfirmBtn.addEventListener('click', () => {
            const reason = document.getElementById('textareaRejectReason').value.trim();
            if (!reason) {
                alert("Vui lòng nhập lý do từ chối!");
                return;
            }
            rejectOrder(activeOrderCode, reason);
        });
    }
}

function updateLiveTotals() {
    if (!currentActiveOrder) return;
    const basePrice = currentActiveOrder.treePrice !== undefined ? currentActiveOrder.treePrice : 
        (currentActiveOrder.items ? 
            currentActiveOrder.items.reduce((sum, item) => sum + ((item.price || 0) * (item.quantity || 1)), 0) : 
            (currentActiveOrder.totalAmount || 0));

    const craneFee = parseFloat(document.getElementById('inputCraneFee')?.value) || 0;
    const shippingFee = parseFloat(document.getElementById('inputShippingFee')?.value) || 0;

    const isDeposit = (currentActiveOrder.paymentMethod === 'DEPOSIT' || currentActiveOrder.paymentMethod === 'COD');
    const depositInput = document.getElementById('inputDepositAmount');
    let depositVal = 0;
    if (isDeposit) {
        depositVal = (depositInput && depositInput.value !== '') ? 
            (parseFloat(depositInput.value) || 0) : Math.round(basePrice * 0.3);
    }

    const finalTotal = basePrice + craneFee + shippingFee;
    const payment1Amount = isDeposit ? (depositVal + craneFee + shippingFee) : finalTotal;
    const remainingPay = isDeposit ? Math.max(0, basePrice - depositVal) : 0;

    // Nhóm 1: GIÁ TRỊ ĐƠN HÀNG
    const basePriceEl = document.getElementById('drawerBasePrice');
    if (basePriceEl) basePriceEl.textContent = formatVND(basePrice);

    const shipValEl = document.getElementById('drawerShippingFeeVal');
    if (shipValEl) shipValEl.textContent = formatVND(shippingFee);

    const craneValEl = document.getElementById('drawerCraneFeeVal');
    if (craneValEl) craneValEl.textContent = formatVND(craneFee);

    const finalTotalEl = document.getElementById('drawerFinalTotal');
    if (finalTotalEl) finalTotalEl.textContent = formatVND(finalTotal);

    // Nhóm 2: THANH TOÁN NGAY (VNPAY)
    const depositEl = document.getElementById('drawerDeposit');
    if (depositEl) depositEl.textContent = isDeposit ? formatVND(depositVal) : "Không (Trả 100%)";

    const payNowShipEl = document.getElementById('drawerPayNowShip');
    if (payNowShipEl) payNowShipEl.textContent = formatVND(shippingFee);

    const payNowCraneEl = document.getElementById('drawerPayNowCrane');
    if (payNowCraneEl) payNowCraneEl.textContent = formatVND(craneFee);

    const pay1El = document.getElementById('drawerPayment1Total');
    if (pay1El) pay1El.textContent = formatVND(payment1Amount);

    // Nhóm 3: THANH TOÁN KHI NHẬN CÂY
    const remSection = document.getElementById('groupRemainingSection');
    if (remSection) remSection.style.display = isDeposit ? 'block' : 'none';

    const remEl = document.getElementById('drawerRemainingPay');
    if (remEl) remEl.textContent = formatVND(remainingPay);
}

function openDrawer(order) {
    if (!order) return;
    activeOrderCode = order.orderCode;
    currentActiveOrder = order;
    
    const codeEl = document.getElementById('drawerOrderCode');
    if (codeEl) codeEl.textContent = order.orderCode || 'BSMS-XXXXX';
    
    const badge = document.getElementById('drawerStatusBadge');
    if (badge) {
        badge.textContent = orderStatusLabels[order.orderStatus] || order.orderStatus || 'Chờ xử lý';
        badge.className = `status-badge ${(order.orderStatus || 'PENDING').toLowerCase()}`;
    }

    if (order.customer) {
        if (document.getElementById('drawerCustName')) document.getElementById('drawerCustName').textContent = order.customer.name || '-';
        if (document.getElementById('drawerCustPhone')) document.getElementById('drawerCustPhone').textContent = order.customer.phone || '-';
        if (document.getElementById('drawerCustEmail')) document.getElementById('drawerCustEmail').textContent = order.customer.email || '-';
        if (document.getElementById('drawerCustAddress')) document.getElementById('drawerCustAddress').textContent = order.customer.address || '-';
    } else {
        if (document.getElementById('drawerCustName')) document.getElementById('drawerCustName').textContent = '-';
        if (document.getElementById('drawerCustPhone')) document.getElementById('drawerCustPhone').textContent = '-';
        if (document.getElementById('drawerCustEmail')) document.getElementById('drawerCustEmail').textContent = '-';
        if (document.getElementById('drawerCustAddress')) document.getElementById('drawerCustAddress').textContent = '-';
    }

    // Render list of products in drawer
    const productsContainer = document.getElementById('drawerProductsContainer');
    if (productsContainer) {
        productsContainer.innerHTML = '';
        if (order.items && order.items.length > 0) {
            order.items.forEach(item => {
                const card = document.createElement('div');
                card.className = 'product-card-info p-2 border rounded d-flex align-items-center gap-3';
                const imgUrl = item.image || '/images/default-tree.jpg';
                card.innerHTML = `
                    <img src="${imgUrl}" class="product-card-img" style="width: 60px; height: 60px; object-fit: cover; border-radius: 4px;">
                    <div class="product-card-details">
                        <span class="product-card-name text-primary fw-bold text-decoration-underline cursor-pointer" 
                              onclick="openProductDetailDrawer(${item.id})">
                            ${item.name}
                        </span>
                        <div class="small text-muted mt-1">
                            <span>Đơn giá: <strong class="text-success">${formatVND(item.price || 0)}</strong></span>
                        </div>
                    </div>
                `;
                productsContainer.appendChild(card);
            });
        } else {
            productsContainer.innerHTML = '<div class="text-muted small">Không có thông tin sản phẩm.</div>';
        }
    }

    if (document.getElementById('drawerNotes')) document.getElementById('drawerNotes').textContent = order.notes || 'Không có yêu cầu đặc biệt.';

    const basePrice = order.items ? 
        order.items.reduce((sum, item) => sum + ((item.price || 0) * (item.quantity || 1)), 0) : 
        (order.totalAmount || 0);
    const craneFee = order.craneFee || 0;
    const shippingFee = order.shippingFee || 0;
    const deposit = order.depositAmount || 0;

    if (document.getElementById('drawerBasePrice')) document.getElementById('drawerBasePrice').textContent = formatVND(basePrice);
    if (document.getElementById('drawerDeposit')) document.getElementById('drawerDeposit').textContent = formatVND(deposit);

    const isPending = order.orderStatus === 'PENDING';
    const isDeposit = (order.paymentMethod === 'DEPOSIT' || order.paymentMethod === 'COD');
    const groupDeposit = document.getElementById('groupDepositAmount');
    const depositInput = document.getElementById('inputDepositAmount');

    if (groupDeposit) {
        groupDeposit.style.display = isDeposit ? 'block' : 'none';
    }

    if (depositInput) {
        if (isDeposit) {
            const defaultDeposit = (order.depositAmount && order.depositAmount > 0) ? 
                order.depositAmount : Math.round(basePrice * 0.3);
            depositInput.value = defaultDeposit;
            depositInput.disabled = !isPending;
        } else {
            depositInput.value = 0;
            depositInput.disabled = true;
        }
    }

    const craneInput = document.getElementById('inputCraneFee');
    const shipInput = document.getElementById('inputShippingFee');

    if (craneInput) {
        craneInput.value = craneFee;
        craneInput.disabled = !isPending;
    }
    if (shipInput) {
        shipInput.value = shippingFee;
        shipInput.disabled = !isPending;
    }

    updateLiveTotals();

    // Render Handling Timeline Log
    renderTimeline(order.handlingHistory);

    if (document.getElementById('btnUnclaimOrder')) document.getElementById('btnUnclaimOrder').style.display = isPending ? 'block' : 'none';
    if (document.getElementById('btnVerifyOrder')) document.getElementById('btnVerifyOrder').style.display = isPending ? 'block' : 'none';
    if (document.getElementById('btnRejectOrder')) document.getElementById('btnRejectOrder').style.display = isPending ? 'block' : 'none';
    if (document.getElementById('rejectReasonBox')) document.getElementById('rejectReasonBox').style.display = 'none';

    const backdrop = document.getElementById('drawerBackdrop');
    const panel = document.getElementById('drawerPanel');
    if (backdrop) backdrop.classList.add('show');
    if (panel) panel.classList.add('show');
}

function closeDrawer() {
    const backdrop = document.getElementById('drawerBackdrop');
    const panel = document.getElementById('drawerPanel');
    if (backdrop) backdrop.classList.remove('show');
    if (panel) panel.classList.remove('show');
}

function renderTimeline(history) {
    const container = document.getElementById('handlingTimelineContainer');
    if (!container) return;
    container.innerHTML = '';

    if (!history || history.length === 0) {
        container.innerHTML = '<div class="text-muted small">Chưa có thông tin chuyển giao.</div>';
        return;
    }

    history.forEach(item => {
        const div = document.createElement('div');
        div.className = `timeline-item ${item.isActive ? 'active' : 'released'}`;
        
        const handledTimeStr = item.handledAt ? new Date(item.handledAt).toLocaleString('vi-VN') : 'N/A';
        const releasedTimeStr = item.releasedAt ? new Date(item.releasedAt).toLocaleString('vi-VN') : null;
        const statusText = item.isActive ? 
            `<span class="text-success font-weight-bold">Đang xử lý (Active)</span>` : 
            `<span class="text-secondary">Đã ngưng quản lý (${releasedTimeStr})</span>`;

        div.innerHTML = `
            <div class="timeline-text">${item.moderatorFullName} (@${item.moderatorUsername})</div>
            <div class="timeline-time"><i class="fa-regular fa-clock me-1"></i> Bắt đầu: ${handledTimeStr}</div>
            <div class="timeline-time mt-1">Trạng thái: ${statusText}</div>
        `;
        container.appendChild(div);
    });
}

async function unclaimOrder(orderCode) {
    const headers = { 'Content-Type': 'application/json' };
    if (csrfHeader && csrfToken) headers[csrfHeader] = csrfToken;

    try {
        const response = await fetch(`/api/orders/${orderCode}/unclaim`, {
            method: 'POST',
            headers: headers
        });
        const result = await response.json();

        if (response.ok && result.success) {
            alert("Đã trả đơn về Kho đơn chung!");
            closeDrawer();
            renderDashboard();
        } else {
            alert(result.message || "Lỗi khi trả đơn.");
        }
    } catch (err) {
        console.error("Lỗi khi trả đơn:", err);
    }
}

async function verifyOrder(orderCode) {
    const craneFee = parseFloat(document.getElementById('inputCraneFee').value) || 0;
    const shippingFee = parseFloat(document.getElementById('inputShippingFee').value) || 0;
    const depositInput = document.getElementById('inputDepositAmount');
    const depositAmount = (depositInput && depositInput.value !== '') ? parseFloat(depositInput.value) : null;

    const headers = { 'Content-Type': 'application/json' };
    if (csrfHeader && csrfToken) headers[csrfHeader] = csrfToken;

    try {
        const response = await fetch(`/api/orders/${orderCode}/verify`, {
            method: 'POST',
            headers: headers,
            body: JSON.stringify({ craneFee, shippingFee, depositAmount })
        });
        const result = await response.json();

        if (response.ok && result.success) {
            alert("Phê duyệt đơn hàng thành công!");
            closeDrawer();
            renderDashboard();
        } else {
            alert(result.message || "Không thể phê duyệt đơn hàng.");
        }
    } catch (err) {
        console.error("Lỗi khi phê duyệt:", err);
    }
}

async function rejectOrder(orderCode, reason) {
    const headers = { 'Content-Type': 'application/json' };
    if (csrfHeader && csrfToken) headers[csrfHeader] = csrfToken;

    try {
        const response = await fetch(`/api/orders/${orderCode}/reject`, {
            method: 'POST',
            headers: headers,
            body: JSON.stringify({ reason })
        });
        const result = await response.json();

        if (response.ok && result.success) {
            alert("Từ chối đơn hàng thành công!");
            closeDrawer();
            renderDashboard();
        } else {
            alert(result.message || "Lỗi khi từ chối đơn hàng.");
        }
    } catch (err) {
        console.error("Lỗi từ chối:", err);
    }
}

function renderPagination(result) {
    const infoEl = document.getElementById('paginationInfo');
    const controlsEl = document.getElementById('paginationControls');
    
    if (infoEl) {
        infoEl.textContent = `Hiển thị ${result.orders ? result.orders.length : 0} trong tổng số ${result.totalCount || 0} đơn của bạn`;
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
            renderDashboard();
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
        
        const priceFormatted = formatVND(data.price || 0);
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

function formatVND(num) {
    return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(num);
}
