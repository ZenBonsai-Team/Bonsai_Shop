(function () {
    if (window.__bsmsWishlistInitialized) {
        return;
    }
    window.__bsmsWishlistInitialized = true;

    const STORAGE_KEY = 'bsms_guest_wishlist';
    const LEGACY_STORAGE_KEY = 'wishlist_favorites';
    const fallbackImage = 'https://images.unsplash.com/photo-1599599810769-bcde5a160d32?auto=format&fit=crop&q=80&w=600';

    document.addEventListener('DOMContentLoaded', async () => {
        migrateLegacyWishlistStorage();
        const mode = await detectWishlistMode();
        initWishlistButtons(mode);
        updateWishlistBadge(mode);
        if (document.getElementById('wishlistGrid')) {
            loadWishlistPage(mode);
        }
    });

    async function detectWishlistMode() {
        try {
            const response = await fetch('/api/wishlist/count', { credentials: 'same-origin' });
            if (response.ok) {
                return 'customer';
            }
            if (response.status === 403) {
                return 'forbidden';
            }
            return 'guest';
        } catch (error) {
            return 'guest';
        }
    }

    function getCsrfHeaders() {
        const csrfToken = document.querySelector('meta[name="_csrf"]')?.getAttribute('content');
        const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.getAttribute('content');
        const headers = { 'Content-Type': 'application/json' };
        if (csrfHeader && csrfToken) {
            headers[csrfHeader] = csrfToken;
        }
        return headers;
    }

    function readGuestWishlist() {
        try {
            const ids = JSON.parse(localStorage.getItem(STORAGE_KEY) || '[]');
            return [...new Set(ids.map(Number).filter(Number.isInteger))];
        } catch (error) {
            return [];
        }
    }

    function writeGuestWishlist(productIds) {
        localStorage.setItem(STORAGE_KEY, JSON.stringify([...new Set(productIds)]));
    }

    function migrateLegacyWishlistStorage() {
        const current = readGuestWishlist();
        let legacy = [];
        try {
            legacy = JSON.parse(localStorage.getItem(LEGACY_STORAGE_KEY) || '[]')
                .map(Number)
                .filter(Number.isInteger);
        } catch (error) {
            legacy = [];
        }
        if (legacy.length > 0) {
            writeGuestWishlist([...current, ...legacy]);
            localStorage.removeItem(LEGACY_STORAGE_KEY);
        }
    }

    async function fetchCustomerWishlist() {
        const response = await fetch('/api/wishlist', { credentials: 'same-origin' });
        if (!response.ok) {
            const error = new Error('Wishlist unavailable');
            error.status = response.status;
            throw error;
        }
        return response.json();
    }

    function initWishlistButtons(mode) {
        const buttons = document.querySelectorAll('.card-btn-wishlist, .btn-main-wishlist');
        if (buttons.length === 0) {
            return;
        }

        if (mode === 'forbidden') {
            buttons.forEach(button => button.remove());
            return;
        }

        buttons.forEach(button => {
            const productId = Number(button.dataset.productId);
            if (!Number.isInteger(productId)) {
                return;
            }
            button.addEventListener('click', event => {
                event.preventDefault();
                event.stopPropagation();
                toggleWishlist(productId, button, mode);
            });
        });

        if (mode === 'customer') {
            fetchCustomerWishlist()
                .then(items => {
                    const customerIds = items.map(item => Number(item.productId));
                    buttons.forEach(button => {
                        const productId = Number(button.dataset.productId);
                        setButtonState(button, customerIds.includes(productId));
                    });
                })
                .catch(() => {
                    buttons.forEach(button => setButtonState(button, false));
                });
            return;
        }

        const guestIds = readGuestWishlist();
        buttons.forEach(button => {
            const productId = Number(button.dataset.productId);
            setButtonState(button, guestIds.includes(productId));
        });
    }

    async function toggleWishlist(productId, button, mode) {
        if (mode === 'customer') {
            try {
                const response = await fetch('/api/wishlist/toggle', {
                    method: 'POST',
                    credentials: 'same-origin',
                    headers: getCsrfHeaders(),
                    body: JSON.stringify({ productId })
                });

                if (response.ok) {
                    const result = await response.json();
                    setButtonState(button, Boolean(result.active));
                    showToast(result.message || (result.active ? 'Đã thêm vào Wishlist.' : 'Đã xóa khỏi Wishlist.'), 'success');
                    updateWishlistBadge('customer');
                    return;
                }

                const message = await readErrorMessage(response);
                showToast(message || `Không thể cập nhật Wishlist. Mã lỗi ${response.status}.`, 'error');
                return;
            } catch (error) {
                showToast('Không thể cập nhật Wishlist. Vui lòng kiểm tra kết nối.', 'error');
                return;
            }
        }

        const guestIds = readGuestWishlist();
        const active = !guestIds.includes(productId);
        const nextIds = active ? [...guestIds, productId] : guestIds.filter(id => id !== productId);
        writeGuestWishlist(nextIds);
        setButtonState(button, active);
        showToast(active ? 'Đã thêm vào Wishlist.' : 'Đã xóa khỏi Wishlist.', 'success');
        updateWishlistBadge('guest');
    }

    function setButtonState(button, active) {
        if (!button) {
            return;
        }
        button.classList.toggle('active', active);
        button.setAttribute('aria-pressed', String(active));
        const icon = button.querySelector('i');
        if (icon) {
            icon.classList.toggle('fa-solid', active);
            icon.classList.toggle('fa-regular', !active);
        }
    }

    async function loadWishlistPage(mode) {
        if (mode === 'forbidden') {
            renderWishlist([]);
            return;
        }

        if (mode === 'customer') {
            try {
                const items = await fetchCustomerWishlist();
                renderWishlist(items);
            } catch (error) {
                renderWishlist([]);
            }
            return;
        }

        const guestIds = readGuestWishlist();
        const items = [];
        for (const productId of guestIds) {
            try {
                const response = await fetch(`/api/products/${productId}`);
                if (response.ok) {
                    const product = await response.json();
                    items.push(toGuestWishlistItem(product));
                }
            } catch (error) {
            }
        }
        renderWishlist(items);
    }

    function toGuestWishlistItem(product) {
        const isVisible = product.isVisible !== false;
        const isLuxury = product.segmentId === 3;
        return {
            productId: product.productId,
            productCode: product.productCode,
            productName: product.productName,
            productImage: product.imageUrl,
            price: product.price,
            isPublicPrice: product.isPublicPrice !== false,
            productStatus: product.productStatus,
            isVisible,
            segmentId: product.segmentId,
            segmentName: product.segmentName,
            canAddToCart: product.productStatus === 'AVAILABLE' && isVisible && !isLuxury,
            detailUrl: isVisible ? (isLuxury ? `/bonsai-luxury-detail/${product.productId}` : `/product/${product.productId}`) : null
        };
    }

    function renderWishlist(items) {
        const grid = document.getElementById('wishlistGrid');
        const emptyState = document.getElementById('emptyWishlistState');
        if (!grid || !emptyState) {
            return;
        }

        grid.innerHTML = '';
        if (!items || items.length === 0) {
            grid.classList.add('d-none');
            emptyState.classList.remove('d-none');
            return;
        }

        grid.classList.remove('d-none');
        emptyState.classList.add('d-none');
        items.forEach(item => grid.appendChild(createWishlistCard(item)));
    }

    function createWishlistCard(item) {
        const card = document.createElement('article');
        card.className = 'wishlist-card';
        const status = (item.productStatus || 'UNAVAILABLE').toUpperCase();
        const isHiddenByVisibility = item.isVisible === false;
        const statusClass = isHiddenByVisibility ? 'not-visible' : status.toLowerCase();
        const statusLabel = isHiddenByVisibility ? 'Không hiển thị' : status;
        const canViewDetails = item.isVisible !== false && Boolean(item.detailUrl);
        const detailUrl = canViewDetails ? item.detailUrl : '';
        const canAddToCart = canViewDetails && item.canAddToCart === true && status === 'AVAILABLE';
        const segmentLabel = item.segmentName ? `<span>${escapeHtml(item.segmentName)}</span>` : '';
        const visibilityLabel = isHiddenByVisibility ? '<span>Không hiển thị</span>' : '';
        const priceLabel = item.isPublicPrice === false ? 'Li\u00ean h\u1ec7' : formatVND(item.price || 0);
        const nameHtml = canViewDetails
            ? `<a class="wishlist-name" href="${escapeHtml(detailUrl)}" data-wishlist-detail="${item.productId}">${escapeHtml(item.productName || 'Sản phẩm')}</a>`
            : `<span class="wishlist-name wishlist-name-disabled" title="Sản phẩm đang tạm ẩn, không thể xem chi tiết">${escapeHtml(item.productName || 'Sản phẩm')}</span>`;
        const detailButton = canViewDetails
            ? `<a class="btn btn-outline-dark btn-sm" href="${escapeHtml(detailUrl)}" data-wishlist-detail="${item.productId}">Chi tiết</a>`
            : `<button class="btn btn-outline-secondary btn-sm" type="button" disabled title="Sản phẩm đang tạm ẩn, không thể xem chi tiết">Chi tiết</button>`;
        const addToCartButton = canAddToCart
            ? `<button class="btn btn-success btn-sm" type="button" data-wishlist-add-cart="${item.productId}" title="Thêm vào giỏ hàng" aria-label="Thêm vào giỏ hàng"><i class="fa-solid fa-cart-shopping"></i></button>`
            : `<button class="btn btn-outline-secondary btn-sm" type="button" disabled title="Không thể thêm giỏ" aria-label="Không thể thêm giỏ"><i class="fa-solid fa-cart-shopping"></i></button>`;

        card.innerHTML = `
            <div class="wishlist-image-wrap">
                <img src="${escapeHtml(item.productImage || fallbackImage)}" class="wishlist-image" alt="${escapeHtml(item.productName || 'Bonsai')}">
                <span class="wishlist-status ${statusClass}">${escapeHtml(statusLabel)}</span>
            </div>
            <div class="wishlist-body">
                ${nameHtml}
                <div class="wishlist-meta">${[escapeHtml(item.productCode || `Bonsai-${item.productId}`), segmentLabel, visibilityLabel].filter(Boolean).join(' • ')}</div>
                <div class="wishlist-price">${priceLabel}</div>
                <div class="wishlist-actions">
                    ${detailButton}
                    ${addToCartButton}
                    <button class="btn btn-outline-danger btn-sm" type="button" data-remove-wishlist="${item.productId}">
                        <i class="fa-regular fa-trash-can"></i>
                    </button>
                </div>
            </div>
        `;
        card.querySelector('[data-remove-wishlist]')?.addEventListener('click', () => removeWishlistItem(item.productId));
        card.querySelector('[data-wishlist-add-cart]')?.addEventListener('click', () => addWishlistItemToCart(item.productId));
        card.querySelectorAll('[data-wishlist-detail]').forEach(link => {
            link.addEventListener('click', event => verifyWishlistDetailNavigation(event, item.productId, detailUrl));
        });
        return card;
    }

    async function verifyWishlistDetailNavigation(event, productId, detailUrl) {
        event.preventDefault();
        const mode = await detectWishlistMode();
        const product = await fetchCurrentProduct(productId);
        if (!canViewCurrentProductDetail(product)) {
            showToast('Sản phẩm đang tạm ẩn, không thể xem chi tiết.', 'error');
            loadWishlistPage(mode);
            return;
        }
        window.location.href = detailUrl;
    }

    async function addWishlistItemToCart(productId) {
        const mode = await detectWishlistMode();
        try {
            const response = await fetch('/api/cart/items', {
                method: 'POST',
                credentials: 'same-origin',
                headers: getCsrfHeaders(),
                body: JSON.stringify({ productId })
            });
            const result = await response.json().catch(() => null);

            if (response.status === 401) {
                const product = await fetchCurrentProduct(productId);
                if (!canGuestAddToCart(product)) {
                    showToast('Sản phẩm không còn đủ điều kiện thêm vào giỏ.', 'error');
                    loadWishlistPage('guest');
                    return;
                }
                addGuestCartItem(productId);
                removeGuestWishlistItem(productId);
                showToast('Đã thêm tác phẩm vào giỏ hàng.', 'success');
                if (typeof window.updateCartBadge === 'function') {
                    window.updateCartBadge();
                }
                updateWishlistBadge('guest');
                loadWishlistPage('guest');
                return;
            }

            if (response.ok && result && result.success) {
                if (mode === 'customer') {
                    await deleteCustomerWishlistItem(productId);
                    updateWishlistBadge('customer');
                    loadWishlistPage('customer');
                }
                showToast(result.message || 'Đã thêm vào giỏ hàng.', 'success');
                if (typeof window.updateCartBadge === 'function') {
                    window.updateCartBadge();
                }
                return;
            }

            showToast(result?.message || 'Sản phẩm không còn đủ điều kiện thêm vào giỏ.', 'error');
            loadWishlistPage(mode);
        } catch (error) {
            showToast('Không thể thêm vào giỏ hàng. Vui lòng kiểm tra kết nối.', 'error');
            loadWishlistPage(mode);
        }
    }

    async function fetchCurrentProduct(productId) {
        const response = await fetch(`/api/products/${productId}`, { credentials: 'same-origin' });
        if (!response.ok) {
            return null;
        }
        return response.json();
    }

    function canGuestAddToCart(product) {
        if (!product) {
            return false;
        }
        const isVisible = product.isVisible !== false;
        const isLuxury = product.segmentId === 3;
        return product.productStatus === 'AVAILABLE' && isVisible && !isLuxury;
    }

    function canViewCurrentProductDetail(product) {
        if (!product) {
            return false;
        }
        return product.isVisible !== false && product.productStatus !== 'DRAFT';
    }

    function addGuestCartItem(productId) {
        let guestCart = [];
        try {
            guestCart = JSON.parse(localStorage.getItem('bonsai_guest_cart') || '[]');
        } catch (error) {
            guestCart = [];
        }
        if (!guestCart.includes(productId)) {
            guestCart.push(productId);
            localStorage.setItem('bonsai_guest_cart', JSON.stringify(guestCart));
        }
    }

    function removeGuestWishlistItem(productId) {
        writeGuestWishlist(readGuestWishlist().filter(id => id !== productId));
    }

    async function deleteCustomerWishlistItem(productId) {
        await fetch(`/api/wishlist/items/${productId}`, {
            method: 'DELETE',
            credentials: 'same-origin',
            headers: getCsrfHeaders()
        });
    }
    async function removeWishlistItem(productId) {
        const mode = await detectWishlistMode();
        if (mode === 'customer') {
            try {
                const response = await fetch(`/api/wishlist/items/${productId}`, {
                    method: 'DELETE',
                    credentials: 'same-origin',
                    headers: getCsrfHeaders()
                });
                if (response.ok) {
                    showToast('Đã xóa khỏi Wishlist.', 'success');
                    updateWishlistBadge('customer');
                    loadWishlistPage('customer');
                    return;
                }
                const message = await readErrorMessage(response);
                showToast(message || `Không thể xóa khỏi Wishlist. Mã lỗi ${response.status}.`, 'error');
                return;
            } catch (error) {
                showToast('Không thể xóa khỏi Wishlist. Vui lòng kiểm tra kết nối.', 'error');
                return;
            }
        }

        if (mode === 'guest') {
            writeGuestWishlist(readGuestWishlist().filter(id => id !== productId));
            showToast('Đã xóa khỏi Wishlist.', 'success');
            updateWishlistBadge('guest');
            loadWishlistPage('guest');
        }
    }

    function updateWishlistBadge(mode) {
        const badge = document.getElementById('wishlistBadgeCount');
        if (!badge) {
            return;
        }

        if (mode === 'customer') {
            fetch('/api/wishlist/count', { credentials: 'same-origin' })
                .then(response => response.ok ? response.json() : { count: 0 })
                .then(data => setBadgeCount(badge, Number(data.count || 0)))
                .catch(() => setBadgeCount(badge, 0));
            return;
        }

        if (mode === 'forbidden') {
            setBadgeCount(badge, 0);
            return;
        }

        setBadgeCount(badge, readGuestWishlist().length);
    }

    function setBadgeCount(badge, count) {
        badge.textContent = count;
        badge.style.display = count > 0 ? 'flex' : 'none';
    }

    function showToast(message, type) {
        if (window.BSMSToast && typeof window.BSMSToast[type] === 'function') {
            window.BSMSToast[type](message);
        }
    }

    async function readErrorMessage(response) {
        try {
            const contentType = response.headers.get('content-type') || '';
            if (contentType.includes('application/json')) {
                const data = await response.json();
                return data.message || data.error || '';
            }
            const text = await response.text();
            return text && text.length < 160 ? text : '';
        } catch (error) {
            return '';
        }
    }

    function formatVND(value) {
        return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(value);
    }

    function escapeHtml(value) {
        return String(value ?? '')
            .replaceAll('&', '&amp;')
            .replaceAll('<', '&lt;')
            .replaceAll('>', '&gt;')
            .replaceAll('"', '&quot;')
            .replaceAll("'", '&#039;');
    }

    window.updateWishlistBadge = () => detectWishlistMode().then(updateWishlistBadge);
})();
