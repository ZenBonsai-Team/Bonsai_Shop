/* ==========================================================================
   Bonsai Luxury — Interactive Premium Script
   ========================================================================== */
document.addEventListener('DOMContentLoaded', () => {

    const $ = (sel, ctx = document) => Array.from(ctx.querySelectorAll(sel));
    const one = (sel, ctx = document) => ctx.querySelector(sel);
    const escapeHtml = s => String(s).replace(/[&<>"']/g, m => ({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'})[m]);

    // Toast Notification System
    const showToast = (text, type = 'success') => {
        let container = one('.flash-container');
        if (!container) {
            container = document.createElement('div');
            container.className = 'flash-container';
            document.body.appendChild(container);
        }
        const msg = document.createElement('div');
        msg.className = `flash-message flash-${type}`;
        msg.innerHTML = `
            <span class="flash-icon">${type === 'success' ? '✓' : '✕'}</span>
            <span>${escapeHtml(text)}</span>
        `;
        container.appendChild(msg);
        setTimeout(() => msg.remove(), 5000);
    };

    /* ----------------------------------------------------------------------
       1. HEADER INTERACTIVITY (User Menu, Notif, Mobile Nav, Search)
       ---------------------------------------------------------------------- */
    const userBtn = one('.user-btn-premium');
    const userMenu = one('.user-menu-premium');
    const notifBtn = one('#notificationBellBtn');
    const notifWrap = one('#notificationWrap');
    const notifList = one('#notificationList');
    const notifCountBadge = one('#notificationCount');
    const markAllReadBtn = one('#markAllReadBtn');

    // Khai báo đầy đủ biến giao diện Header
    const mobileMenuBtn = one('#mobileMenuBtn');
    const mainNav = one('#mainNav');
    const searchInput = one('#siteSearch');
    const searchClear = one('#searchClearBtn');

    // 1. Lấy CSRF Token bảo mật
    const getCsrfToken = () => {
        const token = document.querySelector('meta[name="_csrf"]')?.getAttribute('content');
        const header = document.querySelector('meta[name="_csrf_header"]')?.getAttribute('content');
        return { token, header };
    };

    // 2. Mở / Đóng Dropdown Avatar User
    if (userBtn && userMenu) {
        userBtn.addEventListener('click', (e) => {
            e.stopPropagation();
            const isExpanded = userBtn.getAttribute('aria-expanded') === 'true';
            userBtn.setAttribute('aria-expanded', !isExpanded);
            userMenu.classList.toggle('active');
            if (notifWrap) notifWrap.classList.remove('active');
        });
    }

    // 3. Lấy số lượng thông báo chưa đọc
    const fetchNotificationCount = async () => {
        if (!notifCountBadge) return;
        try {
            const res = await fetch('/notification/count');
            if (res.ok) {
                const count = await res.json();
                if (count > 0) {
                    notifCountBadge.textContent = count > 99 ? '99+' : count;
                    notifCountBadge.style.display = 'flex';
                } else {
                    notifCountBadge.style.display = 'none';
                }
            }
        } catch (error) {
            console.error("Lỗi lấy số lượng thông báo:", error);
        }
    };

    // 4. Hiển thị danh sách thông báo
    const renderNotifications = (notifications) => {
        if (!notifList) return;
        notifList.innerHTML = '';
        if (!notifications || notifications.length === 0) {
            notifList.innerHTML = '<li class="notification-empty" style="padding: 1rem; text-align: center;">Không có thông báo.</li>';
            return;
        }

        notifications.forEach(notif => {
            const li = document.createElement('li');
            li.className = 'notification-item';

            const message = notif.message || notif.content || 'Bạn có thông báo mới';
            const isUnread = notif.isRead === false;

            li.style.fontWeight = isUnread ? '600' : '400';
            li.style.opacity = isUnread ? '1' : '0.7';
            li.style.padding = '10px';
            li.style.borderBottom = '1px solid #eee';
            li.style.cursor = isUnread ? 'pointer' : 'default';

            li.innerHTML = escapeHtml(message);

            // Gán sự kiện click đọc 1 thông báo (chỉ chạy 1 lần)
            if (isUnread) {
                const handleRead = async () => {
                    await markAsRead(notif.notificationId, li);
                    li.removeEventListener('click', handleRead);
                };
                li.addEventListener('click', handleRead);
            }
            notifList.appendChild(li);
        });
    };

    // 5. Gọi API lấy danh sách thông báo
    const fetchNotifications = async () => {
        if (!notifList) return;
        try {
            notifList.innerHTML = '<li class="notification-item" style="padding: 1rem; text-align: center;">Đang tải...</li>';
            const res = await fetch('/notification');
            if (res.ok) {
                const notifications = await res.json();
                renderNotifications(notifications);
            }
        } catch (error) {
            notifList.innerHTML = '<li class="notification-item" style="padding: 1rem; color: red; text-align: center;">Lỗi tải thông báo.</li>';
            console.error("Lỗi lấy danh sách thông báo:", error);
        }
    };

    // 6. Đánh dấu 1 thông báo là đã đọc
    const markAsRead = async (id, liElement) => {
        const csrf = getCsrfToken();
        const headers = { 'Content-Type': 'application/json' };
        if (csrf.token && csrf.header) headers[csrf.header] = csrf.token;

        try {
            const res = await fetch(`/notification/read/${id}`, { method: 'POST', headers });
            const text = await res.text();
            console.log(res.status, text);
            if (res.ok) {
                liElement.style.fontWeight = '400';
                liElement.style.opacity = '0.7';
                liElement.style.cursor = 'default';
                fetchNotificationCount();
            }
        } catch (error) {
            console.error("Lỗi đánh dấu đã đọc:", error);
        }
    };

    // 7. Xử lý nút "ĐỌC TẤT CẢ" thông báo
    if (markAllReadBtn) {
        markAllReadBtn.addEventListener('click', async (e) => {
            e.stopPropagation();
            const csrf = getCsrfToken();
            const headers = { 'Content-Type': 'application/json' };
            if (csrf.token && csrf.header) headers[csrf.header] = csrf.token;

            try {
                const res = await fetch('/notification/read-all', { method: 'POST', headers });
                if (res.ok) {
                    await fetchNotifications();
                    await fetchNotificationCount();
                }
            } catch (error) {
                console.error("Lỗi đánh dấu tất cả đã đọc:", error);
            }
        });
    }

    // 8. Xử lý click Mở/Đóng Chuông thông báo
    if (notifBtn && notifWrap) {
        notifBtn.addEventListener('click', (e) => {
            e.stopPropagation();
            const isActive = notifWrap.classList.contains('active');
            notifWrap.classList.toggle('active');

            if (!isActive) {
                fetchNotifications();
            }

            if (userMenu) {
                userMenu.classList.remove('active');
                if (userBtn) userBtn.setAttribute('aria-expanded', 'false');
            }
        });
    }

    // 9. Đếm số lượng thông báo khi vừa tải trang
    fetchNotificationCount();

    // 10. Mobile Nav Toggle
    if (mobileMenuBtn && mainNav) {
        mobileMenuBtn.addEventListener('click', () => {
            const isExpanded = mobileMenuBtn.getAttribute('aria-expanded') === 'true';
            mobileMenuBtn.setAttribute('aria-expanded', !isExpanded);
            mainNav.classList.toggle('active');
        });
    }

    // 11. Search Interaction
    if (searchInput && searchClear) {
        searchInput.addEventListener('input', () => {
            searchClear.style.display = searchInput.value.length > 0 ? 'block' : 'none';
        });
        searchClear.addEventListener('click', () => {
            searchInput.value = '';
            searchClear.style.display = 'none';
            searchInput.focus();
        });
    }

    // 12. Tự động đóng Dropdown khi click ngoài màn hình
    document.addEventListener('click', (e) => {
        if (userMenu && !userMenu.contains(e.target) && userBtn && !userBtn.contains(e.target)) {
            userMenu.classList.remove('active');
            userBtn.setAttribute('aria-expanded', 'false');
        }
        if (notifWrap && !notifWrap.contains(e.target)) {
            notifWrap.classList.remove('active');
        }
    });

    /* ----------------------------------------------------------------------
       2. THUMBNAIL INTERACTION
       ---------------------------------------------------------------------- */
    const mainImg = one('#mainImage');
    const thumbBtns = $('.thumb');

    thumbBtns.forEach(thumb => {
        thumb.addEventListener('click', function() {
            if (!mainImg) return;
            thumbBtns.forEach(t => t.classList.remove('active'));
            this.classList.add('active');

            const newSrc = this.getAttribute('data-src');
            if (newSrc) {
                mainImg.style.opacity = '0.3';
                setTimeout(() => {
                    mainImg.src = newSrc;
                    mainImg.style.opacity = '1';
                }, 150);
            }
        });
    });

    /* ----------------------------------------------------------------------
       3. LIGHTBOX PREVIEW GALLERY
       ---------------------------------------------------------------------- */
    const galleryItems = $('.gallery-item');
    const overlayRoot = document.getElementById('overlayRoot') || (() => {
        const d = document.createElement('div'); d.id = 'overlayRoot'; document.body.appendChild(d); return d;
    })();

    galleryItems.forEach(item => {
        item.addEventListener('click', function() {
            const imgSrc = this.getAttribute('data-src') || one('img', this)?.src;
            if (!imgSrc) return;

            const lightbox = document.createElement('div');
            lightbox.className = 'popup lightbox-popup';
            lightbox.setAttribute('role', 'dialog');
            lightbox.innerHTML = `
                <div class="popup-content lightbox-content">
                    <button class="popup-close lightbox-close" aria-label="Đóng ảnh lớn">✕</button>
                    <img src="${escapeHtml(imgSrc)}" class="lightbox-img" alt="Bonsai Luxury Zoom">
                </div>
            `;
            overlayRoot.appendChild(lightbox);

            const closeBox = () => lightbox.remove();
            lightbox.querySelector('.popup-close')?.addEventListener('click', closeBox);
            lightbox.addEventListener('click', (ev) => { if (ev.target === lightbox) closeBox(); });
        });
    });

    /* ----------------------------------------------------------------------
       4. PREMIUM BOOKING MODAL
       ---------------------------------------------------------------------- */
    const bookingModal = document.getElementById('bookingModal');
    const modalProductId = document.getElementById('modalProductId');
    const modalProductTitle = document.getElementById('modalProductTitle');
    const actualBookingForm = document.getElementById('actualBookingForm');
    const closeBookingBtn = document.getElementById('closeBookingBtn');
    const cancelBookingBtn = document.getElementById('cancelBookingBtn');
    const dateInput = document.getElementById('appointmentDate');
    const timeInput = document.getElementById('appointmentTime');

    const closeModal = () => {
        if (!bookingModal) return;
        bookingModal.style.opacity = '0';
        setTimeout(() => {
            bookingModal.style.display = 'none';
            bookingModal.setAttribute('aria-hidden', 'true');
        }, 300);
    };

    const openBookingModal = (e) => {
        const btn = e.currentTarget;
        const id = btn.dataset.id;
        const title = document.getElementById('productTitle')?.textContent?.trim() || "Tác phẩm độc bản";

        if (modalProductId) modalProductId.value = id;
        if (modalProductTitle) modalProductTitle.textContent = title;

        if (dateInput) {
            const tomorrow = new Date();
            tomorrow.setDate(tomorrow.getDate() + 1);
            dateInput.min = tomorrow.toISOString().split('T')[0];
            dateInput.value = tomorrow.toISOString().split('T')[0];
        }
        if (timeInput) timeInput.value = "";

        if (bookingModal) {
            bookingModal.style.display = 'flex';
            bookingModal.setAttribute('aria-hidden', 'false');
            void bookingModal.offsetWidth;
            bookingModal.style.opacity = '1';
        }
    };

    document.querySelectorAll('.schedule-btn').forEach(btn => {
        btn.addEventListener('click', (e) => {
            openBookingModal(e);
        });
    });

    closeBookingBtn?.addEventListener('click', closeModal);
    cancelBookingBtn?.addEventListener('click', closeModal);
    bookingModal?.addEventListener('click', e => { if (e.target === bookingModal) closeModal(); });

    actualBookingForm?.addEventListener('submit', e => {
        if (dateInput && dateInput.value) {
            const selected = new Date(dateInput.value);
            const today = new Date();
            today.setHours(0, 0, 0, 0);
            selected.setHours(0, 0, 0, 0);
            if (selected <= today) {
                e.preventDefault();
                showToast("Vui lòng chọn ngày từ ngày mai.", "error");
                return;
            }
        }
        if (timeInput && (!timeInput.value || timeInput.value < "08:00" || timeInput.value > "17:00")) {
            e.preventDefault();
            showToast("Vui lòng chọn thời gian từ 08:00 đến 17:00.", "error");
            return;
        }

        const submitBtn = actualBookingForm.querySelector(".submit-booking-btn");
        if (submitBtn) {
            submitBtn.disabled = true;
            submitBtn.textContent = "Đang xử lý...";
        }
    });

    // Cập nhật năm tự động ở footer
    const yearEl = document.getElementById('year');
    if (yearEl) yearEl.textContent = new Date().getFullYear();
});