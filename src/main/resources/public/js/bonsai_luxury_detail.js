/* ==========================================================================
   Bonsai Luxury — Interactive Premium Script
   ========================================================================== */
document.addEventListener('DOMContentLoaded', () => {

    // Helper Selectors
    const $ = (sel, ctx = document) => Array.from(ctx.querySelectorAll(sel));
    const one = (sel, ctx = document) => ctx.querySelector(sel);
    const escapeHtml = s => String(s).replace(/[&<>"']/g, m => ({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'})[m]);

    // Toast Premium Notification System (Sử dụng đồng bộ flash-message của CSS)
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

        setTimeout(() => {
            msg.remove();
        }, 5000);
    };

    /* ----------------------------------------------------------------------
       1. DROPDOWN USER PROFILE LOGIC
       ---------------------------------------------------------------------- */
    const userBtn = one('.user-btn-premium');
    const userMenu = one('.user-menu-premium');

    if (userBtn && userMenu) {
        userBtn.addEventListener('click', (e) => {
            e.stopPropagation();
            const isExpanded = userBtn.getAttribute('aria-expanded') === 'true';
            userBtn.setAttribute('aria-expanded', !isExpanded);
            userMenu.classList.toggle('active');
        });

        document.addEventListener('click', (e) => {
            if (!userMenu.contains(e.target)) {
                userMenu.classList.remove('active');
                userBtn.setAttribute('aria-expanded', 'false');
            }
        });
    }

    /* ----------------------------------------------------------------------
       2. THUMBNAIL INTERACTION (Sửa lỗi transition time)
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
                }, 150); // Đã sửa từ 15000ms thành 150ms chuẩn mượt mà
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
            lightbox.className = 'popup';
            lightbox.setAttribute('role', 'dialog');
            lightbox.innerHTML = `
                <div class="popup-content" style="max-width: 800px; padding: 10px; background: transparent; border: none; box-shadow: none;">
                    <button class="popup-close" style="color: #FFF; background: rgba(0,0,0,0.5); top: -40px; right: 0;" aria-label="Đóng ảnh lớn">✕</button>
                    <img src="${escapeHtml(imgSrc)}" alt="Bonsai Luxury Zoom" style="width: 100%; height: auto; max-height: 85vh; object-fit: contain; border: 1px solid rgba(255,255,255,0.2);">
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

        // Cài đặt ngày mặc định tối thiểu là ngày mai
        if (dateInput) {
            const tomorrow = new Date();
            tomorrow.setDate(tomorrow.getDate() + 1);
            const yyyy = tomorrow.getFullYear();
            const mm = String(tomorrow.getMonth() + 1).padStart(2, '0');
            const dd = String(tomorrow.getDate()).padStart(2, '0');
            const minDate = `${yyyy}-${mm}-${dd}`;

            dateInput.min = minDate;
            dateInput.value = minDate;
        }

        if (timeInput) timeInput.value = "";

        if (bookingModal) {
            bookingModal.style.display = 'flex';
            bookingModal.setAttribute('aria-hidden', 'false');
            void bookingModal.offsetWidth; // Trigger reflow
            bookingModal.style.opacity = '1';
        }
    };

    document.querySelectorAll('.schedule-btn').forEach(btn => {
        btn.addEventListener('click', (e) => {
            const authenticated = document.body.dataset.authenticated === "true";
            if (!authenticated) {
                window.location.href = "/login";
                return;
            }
            openBookingModal(e);
        });
    });

    closeBookingBtn?.addEventListener('click', closeModal);
    cancelBookingBtn?.addEventListener('click', closeModal);

    bookingModal?.addEventListener('click', e => {
        if (e.target === bookingModal) closeModal();
    });

    // Validate Form Đặt Lịch
    actualBookingForm?.addEventListener('submit', e => {
        if (dateInput.value) {
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

        if (!timeInput.value) {
            e.preventDefault();
            showToast("Vui lòng chọn giờ xem.", "error");
            return;
        }

        if (timeInput.value < "08:00" || timeInput.value > "17:00") {
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

    // Auto-update Footer Year
    const yearEl = document.getElementById('year');
    if (yearEl) yearEl.textContent = new Date().getFullYear();
});