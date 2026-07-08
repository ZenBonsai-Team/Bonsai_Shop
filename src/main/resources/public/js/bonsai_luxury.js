/* bonsai_luxury.js - Premium interactions (full) */
document.addEventListener('DOMContentLoaded', () => {
    /* Footer year */
    const yearEl = document.getElementById('year');
    if (yearEl) yearEl.textContent = new Date().getFullYear();

    /* Smooth scroll for nav links */
    document.querySelectorAll('.nav-link').forEach(link => {
        const href = link.getAttribute('href');
        if (href && href.startsWith('#')) {
            link.addEventListener('click', (e) => {
                e.preventDefault();
                const target = document.querySelector(href);
                if (target) target.scrollIntoView({ behavior: 'smooth', block: 'start' });
            });
        }
    });

    /* HERO SLIDER (auto + manual) */
    (function heroSlider() {
        const slider = document.getElementById('heroSlider');
        if (!slider) return;
        const slides = Array.from(slider.querySelectorAll('.slide'));
        let idx = 0;
        const show = (i) => slides.forEach((s, j) => s.classList.toggle('active', j === i));
        show(idx);
        const next = () => { idx = (idx + 1) % slides.length; show(idx); };
        const prev = () => { idx = (idx - 1 + slides.length) % slides.length; show(idx); };
        let timer = setInterval(next, 5000);
        slider.addEventListener('mouseenter', () => clearInterval(timer));
        slider.addEventListener('mouseleave', () => timer = setInterval(next, 5000));
        const prevBtn = document.querySelector('.slider-controls .prev');
        const nextBtn = document.querySelector('.slider-controls .next');
        if (prevBtn) prevBtn.addEventListener('click', () => { prev(); resetTimer(); });
        if (nextBtn) nextBtn.addEventListener('click', () => { next(); resetTimer(); });
        function resetTimer(){ clearInterval(timer); timer = setInterval(next, 5000); }
    })();

    /* STUDIO SLIDER (About replacement) */
    (function studioSlider() {
        const slides = Array.from(document.querySelectorAll('.studio-slide'));
        if (!slides.length) return;
        const prevBtn = document.querySelector('.prev-studio');
        const nextBtn = document.querySelector('.next-studio');
        let idx = 0;
        const showSlide = i => {
            slides.forEach((s, j) => s.classList.toggle('active', j === i));
            const current = slides[i];
            const studioName = current?.dataset?.name || '';
            document.querySelectorAll('.contact-studio').forEach(b => b.dataset.studio = studioName);
        };
        showSlide(idx);
        const next = () => { idx = (idx + 1) % slides.length; showSlide(idx); };
        const prev = () => { idx = (idx - 1 + slides.length) % slides.length; showSlide(idx); };
        let timer = setInterval(next, 6000);
        if (nextBtn) nextBtn.addEventListener('click', () => { next(); resetTimer(); });
        if (prevBtn) prevBtn.addEventListener('click', () => { prev(); resetTimer(); });
        document.querySelector('.studio-slider')?.addEventListener('mouseenter', () => clearInterval(timer));
        document.querySelector('.studio-slider')?.addEventListener('mouseleave', () => timer = setInterval(next, 6000));
        function resetTimer(){ clearInterval(timer); timer = setInterval(next, 6000); }
    })();

    /* SCROLL REVEAL for product cards */
    const revealOnScroll = () => {
        document.querySelectorAll('.card').forEach(card => {
            const rect = card.getBoundingClientRect();
            if (rect.top < window.innerHeight - 80) card.classList.add('visible');
        });
    };
    revealOnScroll();
    window.addEventListener('scroll', revealOnScroll, { passive: true });

    /* QUICK VIEW POPUP for images (detail preview) */
    const overlayRoot = document.getElementById('overlayRoot') || (function(){
        const d = document.createElement('div'); d.id = 'overlayRoot'; document.body.appendChild(d); return d;
    })();

    function escapeHtml(str) {
        return String(str).replace(/[&<>"']/g, function(m){ return ({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'})[m]; });
    }
    function formatVND(value) {
        const n = Number(value);
        if (Number.isNaN(n)) return value;
        return n.toLocaleString('vi-VN') + '₫';
    }
    function showToast(text) {
        const t = document.createElement('div');
        t.className = 'toast';
        t.textContent = text;
        document.body.appendChild(t);
        setTimeout(()=> t.remove(), 3500);
    }

    document.querySelectorAll('.quick-view').forEach(btn => {
        btn.addEventListener('click', (e) => {
            const card = e.currentTarget.closest('.card');
            const img = card.querySelector('img');
            const title = card.dataset.title || card.querySelector('.card-title').textContent;
            const meta = card.querySelector('.card-meta')?.textContent || '';
            const price = card.dataset.price ? formatVND(card.dataset.price) : card.querySelector('.price')?.textContent || '';

            const popup = document.createElement('div');
            popup.className = 'popup';
            popup.innerHTML = `
        <div class="popup-content" role="dialog" aria-modal="true" aria-label="Xem nhanh ${escapeHtml(title)}">
          <button class="popup-close" aria-label="Đóng">✕</button>
          <div class="popup-body">
            <img src="${img.src}" alt="${escapeHtml(img.alt)}">
            <div class="popup-text">
              <h3>${escapeHtml(title)}</h3>
              <div class="meta"><span>${escapeHtml(meta)}</span><span>${price}</span></div>
              <p>Chi tiết ngắn gọn về tác phẩm. Bạn có thể xem trang chi tiết để biết thêm thông tin hoặc đặt lịch thăm xưởng.</p>
              <div style="margin-top:12px;display:flex;gap:10px;">
                <a href="detail.html?id=${card.dataset.id}" class="btn small">Xem chi tiết</a>
                <button class="btn small outline schedule-btn" data-id="${card.dataset.id}">Đặt lịch ngay</button>
              </div>
            </div>
          </div>
        </div>
      `;
            overlayRoot.appendChild(popup);
            // close handlers
            popup.querySelector('.popup-close').addEventListener('click', () => popup.remove());
            popup.addEventListener('click', (ev) => { if (ev.target === popup) popup.remove(); });
            document.addEventListener('keydown', function esc(e){ if (e.key === 'Escape'){ popup.remove(); document.removeEventListener('keydown', esc); }});
            // attach schedule handler inside popup
            popup.querySelectorAll('.schedule-btn').forEach(b => b.addEventListener('click', openBookingModal));
        });
    });

    /* SCHEDULE / BOOKING modal (for schedule-btn and card buttons) */
    const bookingRoot = document.getElementById('bookingRoot') || (function(){
        const d = document.createElement('div'); d.id = 'bookingRoot'; document.body.appendChild(d); return d;
    })();

    document.querySelectorAll('.schedule-btn').forEach(btn => btn.addEventListener('click', openBookingModal));

    function openBookingModal(e) {
        const id = e.currentTarget.dataset.id || e.target.dataset.id;
        const card = document.querySelector(`.card[data-id="${id}"]`);
        const title = card ? (card.dataset.title || card.querySelector('.card-title').textContent) : 'Tác phẩm';
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
        // handlers
        booking.querySelector('.booking-close').addEventListener('click', () => booking.remove());
        booking.querySelector('.booking-cancel').addEventListener('click', () => booking.remove());
        booking.addEventListener('click', (ev) => { if (ev.target === booking) booking.remove(); });
        booking.querySelector('.booking-form').addEventListener('submit', (ev) => {
            ev.preventDefault();
            const submitBtn = ev.currentTarget.querySelector('button[type="submit"]');
            submitBtn.textContent = 'Đang gửi...';
            submitBtn.disabled = true;
            // simulate send
            setTimeout(() => {
                submitBtn.textContent = 'Gửi yêu cầu';
                submitBtn.disabled = false;
                booking.remove();
                showToast('Yêu cầu đặt lịch đã được gửi. Chúng tôi sẽ liên hệ để xác nhận.');
            }, 900);
        });
    }

    /* DETAIL buttons: if you prefer modal instead of navigation, intercept here (optional) */
    document.querySelectorAll('.detail-btn').forEach(link => {
        // If you want to open modal instead of navigating, uncomment below and implement modal content.
        // link.addEventListener('click', (e) => { e.preventDefault(); /* open modal */ });
    });

    /* SEARCH FILTER (client-side) */
    const searchInput = document.getElementById('search');
    if (searchInput) {
        searchInput.addEventListener('input', (e) => {
            const q = e.target.value.trim().toLowerCase();
            document.querySelectorAll('#productGrid .card').forEach(card => {
                const title = (card.dataset.title || card.querySelector('.card-title').textContent).toLowerCase();
                const price = (card.dataset.price || '').toString();
                const match = !q || title.includes(q) || price.includes(q);
                card.style.display = match ? '' : 'none';
            });
        });
    }

    /* Accessibility: show focus outlines for keyboard users */
    document.body.addEventListener('keydown', (e) => { if (e.key === 'Tab') document.documentElement.classList.add('show-focus'); });
});
