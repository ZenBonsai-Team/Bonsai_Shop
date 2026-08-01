/**
 * My Orders Page JavaScript - Isolated Module for Moderator Portal
 */
document.addEventListener('DOMContentLoaded', () => {

    // State Variables
    let activeCardFilter = 'ALL';
    let currentPage = 1;
    let searchDebounceTimeout = null;

    // DOM Elements
    const cardElements = document.querySelectorAll('.my-kpi-card');
    const searchInput = document.getElementById('orderSearchInput');
    const prioritySelect = document.getElementById('priorityFilterSelect');
    const statusSelect = document.getElementById('statusFilterSelect');
    const sortSelect = document.getElementById('orderSortSelect');
    const tableBody = document.getElementById('myOrdersTableBody');
    const paginationInfo = document.getElementById('paginationInfo');
    const paginationControls = document.getElementById('paginationControls');

    // Initialize Event Listeners
    initCardFilterEvents();
    initToolbarEvents();
    startAgeRefreshTimer();

    // 1. KPI Card Click Handler (Toggle & Switch Filter)
    function initCardFilterEvents() {
        cardElements.forEach(card => {
            card.addEventListener('click', () => {
                const targetFilter = card.dataset.filter;

                if (activeCardFilter === targetFilter) {
                    // Toggle Off
                    activeCardFilter = 'ALL';
                    card.classList.remove('active');
                } else {
                    // Switch Filter
                    cardElements.forEach(c => c.classList.remove('active'));
                    activeCardFilter = targetFilter;
                    card.classList.add('active');
                }

                fetchMyOrders(1);
            });
        });
    }

    // 2. Toolbar Events (Search & Dropdown Selects)
    function initToolbarEvents() {
        if (searchInput) {
            searchInput.addEventListener('input', () => {
                clearTimeout(searchDebounceTimeout);
                searchDebounceTimeout = setTimeout(() => {
                    fetchMyOrders(1);
                }, 300);
            });
        }

        if (prioritySelect) {
            prioritySelect.addEventListener('change', () => fetchMyOrders(1));
        }

        if (statusSelect) {
            statusSelect.addEventListener('change', () => fetchMyOrders(1));
        }

        if (sortSelect) {
            sortSelect.addEventListener('change', () => fetchMyOrders(1));
        }
    }

    // 3. Fetch Orders & KPI Counts from Backend API
    function fetchMyOrders(page = 1) {
        currentPage = page;

        const params = new URLSearchParams({
            search: searchInput ? searchInput.value.trim() : '',
            cardFilter: activeCardFilter,
            priority: prioritySelect ? prioritySelect.value : 'ALL',
            status: statusSelect ? statusSelect.value : 'ALL',
            sort: sortSelect ? sortSelect.value : 'date_desc',
            page: page,
            limit: 8
        });

        fetch(`/moderator/orders/api/my-orders?${params.toString()}`)
            .then(res => {
                if (!res.ok) throw new Error('Kh\u00f4ng th\u1ec3 t\u1ea3i d\u1eef li\u1ec7u \u0111\u01a1n h\u00e0ng');
                return res.json();
            })
            .then(data => {
                updateKPIStats(data.kpis);
                renderTableRows(data.orders);
                renderPagination(data.totalCount, data.totalPages, data.currentPage, data.pageSize);
                updateDisplayedAges();
            })
            .catch(err => {
                console.error('Error fetching my orders:', err);
                if (tableBody) {
                    tableBody.innerHTML = `
                        <tr>
                            <td colspan="8" style="text-align: center; color: #e53e3e; padding: 30px;">
                                L\u1ed7i k\u1ebft n\u1ed1i d\u1eef li\u1ec7u. Vui l\u00f2ng th\u1eed l\u1ea1i sau.
                            </td>
                        </tr>
                    `;
                }
            });
    }

    // 4. Update KPI Card Metrics Display
    function updateKPIStats(kpis) {
        if (!kpis) return;
        const setVal = (id, val) => {
            const el = document.getElementById(id);
            if (el) el.innerText = val != null ? val : 0;
        };

        setVal('kpiCriticalCount', kpis.criticalCount);
        setVal('kpiApprovalCount', kpis.waitingApprovalCount);
        setVal('kpiPaymentCount', kpis.waitingPaymentCount);
        setVal('kpiDeliveryCount', kpis.waitingDeliveryCount);
        setVal('kpiCompletedCount', kpis.completedCount);
        setVal('kpiCancelledCount', kpis.cancelledCount);
    }

    // 5. Render Table Rows
    function renderTableRows(orders) {
        if (!tableBody) return;

        if (!orders || orders.length === 0) {
            tableBody.innerHTML = `
                <tr>
                    <td colspan="8" style="text-align: center; padding: 40px; color: #a0aec0;">
                        Kh\u00f4ng t\u00ecm th\u1ea5y \u0111\u01a1n h\u00e0ng n\u00e0o ph\u00f9 h\u1ee3p
                    </td>
                </tr>
            `;
            return;
        }

        tableBody.innerHTML = orders.map(order => {
            const depositStr = formatCurrency(order.depositAmount);
            const remainingStr = formatCurrency(order.remainingPaymentAmount);
            const priorityClass = `badge-priority-${(order.priority || 'normal').toLowerCase()}`;
            const statusClass = `badge-status-${(order.orderStatus || 'pending').toLowerCase()}`;
            const orderCode = encodeURIComponent(order.orderCode || '');

            return `
                <tr>
                    <td class="col-code">
                        <strong>${escapeHtml(order.orderCode || '')}</strong>
                    </td>
                    <td class="col-customer">
                        <strong>${escapeHtml(order.customerName || 'Kh\u00e1ch h\u00e0ng')}</strong>
                        <span class="cust-phone">${escapeHtml(order.customerPhone || '-')}</span>
                    </td>
                    <td class="col-money">
                        ${depositStr}
                    </td>
                    <td class="col-money">
                        ${remainingStr}
                    </td>
                    <td class="col-priority">
                        <span class="badge-priority ${priorityClass}">
                            ${escapeHtml(order.priority || 'NORMAL')}
                        </span>
                    </td>
                    <td class="col-age">
                        <span class="age-timer-element" data-timestamp="${order.statusTimestamp || ''}">
                            ${escapeHtml(order.ageFormatted || '-')}
                        </span>
                    </td>
                    <td class="col-status">
                        <span class="badge-status ${statusClass}">
                            ${escapeHtml(order.orderStatus || 'PENDING')}
                        </span>
                    </td>
                    <td class="col-action">
                        <a href="/moderator/orders/${orderCode}" class="btn-view-detail">
                            <i class="fa-regular fa-eye"></i> Chi ti\u1ebft
                        </a>
                    </td>
                </tr>
            `;
        }).join('');
    }

    function renderPagination(totalCount, totalPages, currentPage, pageSize) {
        if (paginationInfo) {
            const start = totalCount > 0 ? (currentPage - 1) * pageSize + 1 : 0;
            const end = Math.min(currentPage * pageSize, totalCount);
            paginationInfo.innerText = `Hi\u1ec3n th\u1ecb ${start} - ${end} tr\u00ean t\u1ed5ng s\u1ed1 ${totalCount} \u0111\u01a1n h\u00e0ng`;
        }

        if (!paginationControls) return;
        paginationControls.innerHTML = '';

        if (totalPages <= 1) return;

        // Previous Button
        const prevBtn = document.createElement('button');
        prevBtn.className = `btn btn-sm btn-outline-secondary ${currentPage === 1 ? 'disabled' : ''}`;
        prevBtn.innerText = 'Tr\u01b0\u1edbc';
        prevBtn.disabled = currentPage === 1;
        prevBtn.onclick = () => fetchMyOrders(currentPage - 1);
        paginationControls.appendChild(prevBtn);

        // Page Numbers
        for (let i = 1; i <= totalPages; i++) {
            if (i === 1 || i === totalPages || (i >= currentPage - 2 && i <= currentPage + 2)) {
                const pageBtn = document.createElement('button');
                pageBtn.className = `btn btn-sm ${i === currentPage ? 'btn-primary' : 'btn-outline-secondary'}`;
                pageBtn.innerText = i;
                pageBtn.onclick = () => fetchMyOrders(i);
                paginationControls.appendChild(pageBtn);
            }
        }

        // Next Button
        const nextBtn = document.createElement('button');
        nextBtn.className = `btn btn-sm btn-outline-secondary ${currentPage === totalPages ? 'disabled' : ''}`;
        nextBtn.innerText = 'Sau';
        nextBtn.disabled = currentPage === totalPages;
        nextBtn.onclick = () => fetchMyOrders(currentPage + 1);
        paginationControls.appendChild(nextBtn);
    }

    // 7. Client-Side Periodic AGE Timer Refresh (1-Minute Refresh Loop)
    function startAgeRefreshTimer() {
        updateDisplayedAges();
        setInterval(updateDisplayedAges, 60000); // 1 minute
    }

    function updateDisplayedAges() {
        const ageElements = document.querySelectorAll('.age-timer-element');
        const now = new Date();

        ageElements.forEach(el => {
            const rawTs = el.getAttribute('data-timestamp');
            if (!rawTs) return;

            const tsDate = new Date(rawTs);
            if (isNaN(tsDate.getTime())) return;

            const diffMs = now - tsDate;
            if (diffMs < 0) {
                el.innerText = 'V\u1eeba xong';
                return;
            }

            const diffMins = Math.floor(diffMs / (1000 * 60));
            if (diffMins < 1) {
                el.innerText = 'V\u1eeba xong';
            } else if (diffMins < 60) {
                el.innerText = `${diffMins} ph\u00fat`;
            } else {
                const diffHours = Math.floor(diffMins / 60);
                if (diffHours < 24) {
                    el.innerText = `${diffHours} gi\u1edd`;
                } else {
                    const diffDays = Math.floor(diffHours / 24);
                    el.innerText = `${diffDays} ng\u00e0y`;
                }
            }
        });
    }

    // Helper Utility Functions
    function formatCurrency(amount) {
        const numericAmount = Number(amount);
        if (!Number.isFinite(numericAmount)) return '0 \u20ab';
        return new Intl.NumberFormat('vi-VN', {
            maximumFractionDigits: 0
        }).format(numericAmount) + ' \u20ab';
    }

    function escapeHtml(str) {
        if (!str) return '';
        return String(str)
            .replace(/&/g, '&amp;')
            .replace(/</g, '&lt;')
            .replace(/>/g, '&gt;')
            .replace(/"/g, '&quot;')
            .replace(/'/g, '&#039;');
    }
});
