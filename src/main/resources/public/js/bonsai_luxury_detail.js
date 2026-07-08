/* bonsai_luxury.js - Premium interactions (full) */
document.addEventListener('DOMContentLoaded', () => {
    /* ---------------------------
       Helper utilities
       --------------------------- */
    const $ = (sel, ctx = document) => Array.from(ctx.querySelectorAll(sel));
    const one = (sel, ctx = document) => ctx.querySelector(sel);
    const escapeHtml = s => String(s).replace(/[&<>"']/g, m => ({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'})[m]);
    const formatVND = v => {
        const n = Number(v);
        return Number.isNaN(n) ? v : n.toLocaleString('vi-VN') + '₫';
    };
    const showToast = (text) => {
        const t = document.createElement('div');
        t.className = 'toast';
        t.textContent = text;
        document.body.appendChild(t);
        setTimeout(() => t.remove(), 3500);
    };

    /* ---------------------------
       Header interactions
       --------------------------- */
    (function headerInit(){
        const hamburger = one('.hamburger');
        const mainNav = one('#mainNav');
        const searchInput = one('#siteSearch') || one('.search-input');
        const searchClear = one('.search-clear');
        const cartCount = one('.cart-count');

        if (hamburger && mainNav) {
            hamburger.addEventListener('click', () => {
                const expanded = hamburger.getAttribute('aria-expanded') === 'true';
                hamburger.setAttribute('aria-expanded', String(!expanded));
                mainNav.classList.toggle('open', !expanded);
            });
        }

        if (searchInput && searchClear) {
            searchInput.addEventListener('input', () => {
                searchClear.hidden = !searchInput.value.trim();
            });
            searchClear.addEventListener('click', () => {
                searchInput.value = '';
                searchClear.hidden = true;
                searchInput.focus();
                searchInput.dispatchEvent(new Event('input', { bubbles: true }));
            });
        }

        if (cartCount) {
            let last = Number(cartCount.textContent || 0);
            const obs = new MutationObserver(() => {
                const n = Number(cartCount.textContent || 0);
                if (n > last) {
                    const cart = one('.cart');
                    if (cart) cart.animate([{ transform: 'scale(1)' }, { transform: 'scale(1.08)' }, { transform: 'scale(1)' }], { duration: 420, easing: 'ease-out' });
                }
                last = n;
            });
            obs.observe(cartCount, { childList: true, characterData: true, subtree: true });
        }

        // Close mobile nav on link click
        one('#mainNav')?.addEventListener('click', (e) => {
            if (e.target.matches('.nav-link') && mainNav.classList.contains('open')) {
                mainNav.classList.remove('open');
                hamburger?.setAttribute('aria-expanded', 'false');
            }
        });
    })();

    /* ---------------------------
       Hero slider (auto + manual)
       --------------------------- */
    (function heroSlider(){
        const slider = one('#heroSlider');
        if (!slider) return;
        const slides = $('.slide', slider);
        if (!slides.length) return;
        let idx = 0;
        const show = i => slides.forEach((s,j) => s.classList.toggle('active', j === i));
        show(idx);
        const next = () => { idx = (idx + 1) % slides.length; show(idx); };
        const prev = () => { idx = (idx - 1 + slides.length) % slides.length; show(idx); };
        let timer = setInterval(next, 5000);
        slider.addEventListener('mouseenter', () => clearInterval(timer));
        slider.addEventListener('mouseleave', () => timer = setInterval(next, 5000));
        one('.slider-controls .prev')?.addEventListener('click', () => { prev(); reset(); });
        one('.slider-controls .next')?.addEventListener('click', () => { next(); reset(); });
        function reset(){ clearInterval(timer); timer = setInterval(next, 5000); }
    })();

    /* ---------------------------
       Studio slider (About)
       --------------------------- */
    (function studioSlider(){
        const slides = $('.studio-slide');
        if (!slides.length) return;
        const prevBtn = one('.prev-studio');
        const nextBtn = one('.next-studio');
        let idx = 0;
        const show = i => slides.forEach((s,j) => s.classList.toggle('active', j === i));
        show(idx);
        const next = () => { idx = (idx + 1) % slides.length; show(idx); updateContact(); };
        const prev = () => { idx = (idx - 1 + slides.length) % slides.length; show(idx); updateContact(); };
        let timer = setInterval(next, 6000);
        prevBtn?.addEventListener('click', () => { prev(); reset(); });
        nextBtn?.addEventListener('click', () => { next(); reset(); });
        one('.studio-slider')?.addEventListener('mouseenter', () => clearInterval(timer));
        one('.studio-slider')?.addEventListener('mouseleave', () => timer = setInterval(next, 6000));
        function reset(){ clearInterval(timer); timer = setInterval(next, 6000); }
        function updateContact(){
            const current = slides[idx];
            const name = current?.dataset?.name || '';
            $('.contact-studio').forEach(b => b.dataset.studio = name);
        }
        updateContact();
    })();

    /* ---------------------------
       Scroll reveal for cards
       --------------------------- */
    (function reveal(){
        const revealOnScroll = () => {
            $('.card').forEach(card => {
                const rect = card.getBoundingClientRect();
                if (rect.top < window.innerHeight - 80) card.classList.add('visible');
            });
        };
        revealOnScroll();
        window.addEventListener('scroll', revealOnScroll, { passive: true });
    })();

    /* ---------------------------
       Quick view & product modals
       --------------------------- */
    (function productInteractions(){
        const overlayRoot = document.getElementById('overlayRoot') || (() => { const d = document.createElement('div'); d.id = 'overlayRoot'; document.body.appendChild(d); return d; })();
        const bookingRoot = document.getElementById('bookingRoot') || (() => { const d = document.createElement('div'); d.id = 'bookingRoot'; document.body.appendChild(d); return d; })();

        // Quick view from collection cards
        $('.quick-view').forEach(btn => {
            btn.addEventListener('click', (e) => {
                const card = e.currentTarget.closest('.card');
                if (!card) return;
                const img = one('img', card);
                const title = card.dataset.title || one('.card-title', card)?.textContent || 'Tác phẩm';
                const meta = one('.card-meta', card)?.textContent || '';
                const price = card.dataset.price ? formatVND(card.dataset.price) : one('.price', card)?.textContent || '';
                const popup = document.createElement('div');
                popup.className = 'popup';
                popup.innerHTML = `
          <div class="popup-content" role="dialog" aria-modal="true" aria-label="Xem nhanh ${escapeHtml(title)}">
            <button class="popup-close" aria-label="Đóng">✕</button>
            <img src="${escapeHtml(img?.src || '')}" alt="${escapeHtml(img?.alt || '')}">
            <div class="popup-text">
              <h3>${escapeHtml(title)}</h3>
              <div class="meta"><span>${escapeHtml(meta)}</span><span>${escapeHtml(price)}</span></div>
              <p>Chi tiết ngắn gọn về tác phẩm. Bạn có thể xem trang chi tiết để biết thêm thông tin hoặc đặt lịch thăm xưởng.</p>
              <div style="margin-top:12px;display:flex;gap:10px;">
                <a href="detail.html?id=${encodeURIComponent(card.dataset.id || '')}" class="btn small">Xem chi tiết</a>
                <button class="btn small outline schedule-btn" data-id="${escapeHtml(card.dataset.id || '')}">Đặt lịch ngay</button>
              </div>
            </div>
          </div>
        `;
                overlayRoot.appendChild(popup);
                popup.querySelector('.popup-close')?.focus();
                const remove = () => popup.remove();
                popup.querySelector('.popup-close')?.addEventListener('click', remove);
                popup.addEventListener('click', (ev) => { if (ev.target === popup) remove(); });
                document.addEventListener('keydown', function esc(e){ if (e.key === 'Escape'){ remove(); document.removeEventListener('keydown', esc); }});
                // attach schedule handler inside popup
                popup.querySelectorAll('.schedule-btn').forEach(b => b.addEventListener('click', openBookingModal));
            });
        });

        // Schedule buttons (cards and popups)
        $('.schedule-btn').forEach(b => b.addEventListener('click', openBookingModal));

        function openBookingModal(e){
            const id = e.currentTarget?.dataset?.id || e.target?.dataset?.id || '';
            const card = one(`.card[data-id="${id}"]`);
            const title = card ? (card.dataset.title || one('.card-title', card)?.textContent) : document.title || 'Tác phẩm';
            const booking = document.createElement('div');
            booking.className = 'popup';
            booking.innerHTML = `
        <div class="booking-card" role="dialog" aria-modal="true" aria-label="Đặt lịch ${escapeHtml(title)}">
          <button class="booking-close" aria-label="Đóng">✕</button>
          <h3>Đặt lịch xem tác phẩm: ${escapeHtml(title)}</h3>
          <form class="booking-form">
            <input name="name" placeholder="Họ và tên" required>
            <input name="phone" placeholder="Số điện thoại" required>
            <input name="email" placeholder="Email (không bắt buộc)">
            <label style="display:block;margin-top:6px;">
              <span style="display:block;margin-bottom:6px;color:var(--muted)">Chọn ngày</span>
              <input type="date" name="date" required>
            </label>
            <label style="display:block;margin-top:6px;">
              <span style="display:block;margin-bottom:6px;color:var(--muted)">Ghi chú</span>
              <textarea name="note" rows="3" placeholder="Yêu cầu thêm (ví dụ: giờ, địa điểm)"></textarea>
            </label>
            <div class="booking-actions">
              <button type="button" class="btn outline booking-cancel">Hủy</button>
              <button type="submit" class="btn primary">Gửi yêu cầu</button>
            </div>
          </form>
        </div>
      `;
            bookingRoot.appendChild(booking);
            booking.querySelector('.booking-close')?.focus();
            const remove = () => booking.remove();
            booking.querySelector('.booking-close')?.addEventListener('click', remove);
            booking.querySelector('.booking-cancel')?.addEventListener('click', remove);
            booking.addEventListener('click', (ev) => { if (ev.target === booking) remove(); });
            booking.querySelector('.booking-form')?.addEventListener('submit', (ev) => {
                ev.preventDefault();
                const submitBtn = ev.currentTarget.querySelector('button[type="submit"]');
                submitBtn.textContent = 'Đang gửi...';
                submitBtn.disabled = true;
                setTimeout(() => {
                    submitBtn.textContent = 'Gửi yêu cầu';
                    submitBtn.disabled = false;
                    remove();
                    showToast('Yêu cầu đặt lịch đã được gửi. Chúng tôi sẽ liên hệ để xác nhận.');
                }, 900);
            });
        }

        // Detail buttons: optional interception (left as navigation by default)
        $('.detail-btn').forEach(link => {
            // If you want modal detail instead of navigation, intercept here.
            // link.addEventListener('click', (e) => { e.preventDefault(); /* open modal */ });
        });

        // Search filter integration (if search input exists)
        const searchInput = one('#search') || one('.search-input');
        if (searchInput) {
            searchInput.addEventListener('input', (e) => {
                const q = e.target.value.trim().toLowerCase();
                $('#productGrid .card').forEach(card => {
                    const title = (card.dataset.title || one('.card-title', card)?.textContent || '').toLowerCase();
                    const price = (card.dataset.price || '').toString();
                    const match = !q || title.includes(q) || price.includes(q);
                    card.style.display = match ? '' : 'none';
                });
            });
        }
    })();

    /* ---------------------------
       Product detail page interactions
       --------------------------- */
    (function detailPage(){
        const mainImage = one('#mainImage');
        const overlayRoot = document.getElementById('overlayRoot') || (() => { const d = document.createElement('div'); d.id = 'overlayRoot'; document.body.appendChild(d); return d; })();
        const bookingRoot = document.getElementById('bookingRoot') || (() => { const d = document.createElement('div'); d.id = 'bookingRoot'; document.body.appendChild(d); return d; })();

        // Thumbnails -> main image
        $('.thumb').forEach(btn => {
            btn.addEventListener('click', () => {
                $('.thumb').forEach(t => t.classList.remove('active'));
                btn.classList.add('active');
                const src = btn.dataset.src;
                if (mainImage && src) mainImage.src = src;
            });
        });

        // Click main image to open lightbox
        if (mainImage) {
            mainImage.addEventListener('click', () => {
                const src = mainImage.src;
                const popup = document.createElement('div');
                popup.className = 'popup';
                popup.innerHTML = `
          <div class="popup-content" role="dialog" aria-modal="true" aria-label="Xem ảnh lớn">
            <button class="popup-close" aria-label="Đóng">✕</button>
            <img src="${escapeHtml(src)}" alt="">
          </div>
        `;
                overlayRoot.appendChild(popup);
                popup.querySelector('.popup-close')?.focus();
                const remove = () => popup.remove();
                popup.querySelector('.popup-close')?.addEventListener('click', remove);
                popup.addEventListener('click', (ev) => { if (ev.target === popup) remove(); });
                document.addEventListener('keydown', function esc(e){ if (e.key === 'Escape'){ remove(); document.removeEventListener('keydown', esc); }});
            });
        }

        // Gallery items open lightbox
        $('.gallery-item').forEach(item => {
            item.addEventListener('click', () => {
                const src = item.dataset.src || one('img', item)?.src;
                if (!src) return;
                const popup = document.createElement('div');
                popup.className = 'popup';
                popup.innerHTML = `
          <div class="popup-content" role="dialog" aria-modal="true" aria-label="Xem ảnh chi tiết">
            <button class="popup-close" aria-label="Đóng">✕</button>
            <img src="${escapeHtml(src)}" alt="">
          </div>
        `;
                overlayRoot.appendChild(popup);
                popup.querySelector('.popup-close')?.focus();
                const remove = () => popup.remove();
                popup.querySelector('.popup-close')?.addEventListener('click', remove);
                popup.addEventListener('click', (ev) => { if (ev.target === popup) remove(); });
                document.addEventListener('keydown', function esc(e){ if (e.key === 'Escape'){ remove(); document.removeEventListener('keydown', esc); }});
            });
        });

        // Schedule & consult buttons on detail page
        $('.schedule-btn').forEach(btn => btn.addEventListener('click', (e) => {
            const title = one('.product-title')?.textContent || 'Tác phẩm';
            // reuse booking modal from productInteractions
            const evt = { currentTarget: { dataset: { id: btn.dataset.id || 'p1' } }, target: btn };
            // call openBookingModal indirectly by dispatching click on schedule-btn handled earlier
            btn.dispatchEvent(new Event('click', { bubbles: true }));
        }));
        $('.consult-btn').forEach(btn => btn.addEventListener('click', (e) => {
            const title = one('.product-title')?.textContent || 'Tác phẩm';
            // open consult modal (same booking modal but without date)
            const id = btn.dataset.id || 'p1';
            const booking = document.createElement('div');
            booking.className = 'popup';
            booking.innerHTML = `
        <div class="booking-card" role="dialog" aria-modal="true" aria-label="Yêu cầu tư vấn ${escapeHtml(title)}">
          <button class="booking-close" aria-label="Đóng">✕</button>
          <h3>Yêu cầu tư vấn: ${escapeHtml(title)}</h3>
          <form class="booking-form">
            <input name="name" placeholder="Họ và tên" required>
            <input name="phone" placeholder="Số điện thoại" required>
            <input name="email" placeholder="Email (không bắt buộc)">
            <textarea name="note" rows="3" placeholder="Ghi chú / Yêu cầu thêm"></textarea>
            <div class="booking-actions">
              <button type="button" class="btn outline booking-cancel">Hủy</button>
              <button type="submit" class="btn primary">Gửi yêu cầu</button>
            </div>
          </form>
        </div>
      `;
            bookingRoot.appendChild(booking);
            booking.querySelector('.booking-close')?.focus();
            const remove = () => booking.remove();
            booking.querySelector('.booking-close')?.addEventListener('click', remove);
            booking.querySelector('.booking-cancel')?.addEventListener('click', remove);
            booking.addEventListener('click', (ev) => { if (ev.target === booking) remove(); });
            booking.querySelector('.booking-form')?.addEventListener('submit', (ev) => {
                ev.preventDefault();
                const submitBtn = ev.currentTarget.querySelector('button[type="submit"]');
                submitBtn.textContent = 'Đang gửi...';
                submitBtn.disabled = true;
                setTimeout(() => {
                    submitBtn.textContent = 'Gửi yêu cầu';
                    submitBtn.disabled = false;
                    remove();
                    showToast('Yêu cầu tư vấn đã được gửi. Chúng tôi sẽ liên hệ để xác nhận.');
                }, 900);
            });
        }));
    })();

    /* ---------------------------
       Accessibility: keyboard focus outlines
       --------------------------- */
    document.body.addEventListener('keydown', (e) => { if (e.key === 'Tab') document.documentElement.classList.add('show-focus'); });

});
