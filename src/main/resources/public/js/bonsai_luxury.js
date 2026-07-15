/* ==========================================================================
   Bonsai Luxury — Premium Interactions (Production Ready)
   ========================================================================== */
document.addEventListener('DOMContentLoaded', () => {

    /* ----------------------------------------------------------------------
       1. TIỆN ÍCH & CẤU HÌNH CƠ BẢN (Utilities)
       ---------------------------------------------------------------------- */
    // Tự động cập nhật năm ở Footer
    const yearEl = document.getElementById('year');
    if (yearEl) yearEl.textContent = new Date().getFullYear();

    // Mã hóa HTML để chống XSS
    const escapeHtml = (str) => {
        return String(str).replace(/[&<>"']/g, function (m) {
            return ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' })[m];
        });
    };

    // Hàm hiển thị thông báo góc màn hình (Toast)
    const showToast = (text) => {
        const existing = document.querySelector('.toast');
        if (existing) existing.remove();

        const t = document.createElement('div');
        t.className = 'toast';
        t.textContent = text;
        document.body.appendChild(t);

        // Hiệu ứng mờ dần trước khi xóa
        setTimeout(() => {
            t.style.opacity = '0';
            t.style.transition = 'opacity 0.4s ease';
            setTimeout(() => t.remove(), 400);
        }, 3500);
    };

    /* ----------------------------------------------------------------------
       2. HIỆU ỨNG GIAO DIỆN CHUNG (UI/UX)
       ---------------------------------------------------------------------- */
    // Đã loại bỏ logic đổi màu Navbar (.scrolled) để giữ nguyên trạng thái một màu cố định.

    // Cuộn mượt (Smooth Scroll) cho các liên kết mỏ neo (#)
    document.querySelectorAll('.nav-link').forEach(link => {
        const href = link.getAttribute('href');
        if (href && href.startsWith('#') && href.length > 1) {
            link.addEventListener('click', (e) => {
                e.preventDefault();
                const target = document.querySelector(href);
                if (target) {
                    target.scrollIntoView({ behavior: 'smooth', block: 'start' });
                }
            });
        }
    });

    // Hiệu ứng Fade-in các thẻ sản phẩm khi cuộn đến
    const revealOnScroll = () => {
        document.querySelectorAll('.card').forEach(card => {
            const rect = card.getBoundingClientRect();
            if (rect.top < window.innerHeight - 50) {
                card.classList.add('visible');
            }
        });
    };
    revealOnScroll();
    window.addEventListener('scroll', revealOnScroll, { passive: true });

    /* ----------------------------------------------------------------------
       3. HỆ THỐNG SLIDER (Hero & Studio)
       ---------------------------------------------------------------------- */
    // Hàm khởi tạo Slider dùng chung
    const initSlider = (sliderId, slideClass, intervalTime) => {
        const slider = document.getElementById(sliderId);
        if (!slider) return;

        const slides = Array.from(slider.querySelectorAll(slideClass));
        if (!slides.length) return;

        let idx = 0;
        let timer;

        const showSlide = (i) => slides.forEach((s, j) => s.classList.toggle('active', j === i));

        const next = () => { idx = (idx + 1) % slides.length; showSlide(idx); };
        const prev = () => { idx = (idx - 1 + slides.length) % slides.length; showSlide(idx); };

        const resetTimer = () => {
            clearInterval(timer);
            timer = setInterval(next, intervalTime);
        };

        // Khởi tạo
        showSlide(idx);
        timer = setInterval(next, intervalTime);

        // Nút điều hướng (nếu có)
        const parent = slider.parentElement;
        const prevBtn = parent.querySelector('.prev') || parent.querySelector('.prev-studio');
        const nextBtn = parent.querySelector('.next') || parent.querySelector('.next-studio');

        if (prevBtn) prevBtn.addEventListener('click', () => { prev(); resetTimer(); });
        if (nextBtn) nextBtn.addEventListener('click', () => { next(); resetTimer(); });

        // Tạm dừng khi hover
        slider.addEventListener('mouseenter', () => clearInterval(timer));
        slider.addEventListener('mouseleave', () => resetTimer());
    };

    initSlider('heroSlider', '.slide', 5000);
    initSlider('studioSlider', '.studio-slide', 6000);

    /* ----------------------------------------------------------------------
         4. BỘ LỌC TÌM KIẾM & HIỂN THỊ SẢN PHẨM (Search & Filter)
         ---------------------------------------------------------------------- */
    const searchInput = document.getElementById('search');
    const varietyFilter = document.getElementById('varietyFilter');
    const segmentFilter = document.getElementById('segmentFilter');
    const ageFilter = document.getElementById('ageFilter');       // THÊM MỚI
    const heightFilter = document.getElementById('heightFilter'); // THÊM MỚI
    const resetFiltersBtn = document.getElementById('resetFilters');
    const filterSummary = document.getElementById('filterSummary');
    const emptyState = document.querySelector('#productGrid .empty-state');
    const productCards = Array.from(document.querySelectorAll('#productGrid .card'));

    // Tự động trích xuất các Option lọc dựa trên dữ liệu thật trên DOM
    const populateFilterOptions = () => {
        if (!productCards.length) return;

        const varietySet = new Set();

        productCards.forEach(card => {
            const variety = card.dataset.variety?.trim();
            if (variety) varietySet.add(variety);
        });

        Array.from(varietySet).sort().forEach(value => {
            if(varietyFilter) {
                const option = document.createElement('option');
                option.value = value;
                option.textContent = value;
                varietyFilter.appendChild(option);
            }
        });
    };

    // Hàm bổ trợ kiểm tra giá trị số có nằm trong khoảng lọc (min-max) không
    const checkRangeMatch = (filterValue, cardValueStr) => {
        if (!filterValue) return true; // Không chọn lọc khoảng này -> Mặc định khớp
        if (!cardValueStr) return false; // Có lọc nhưng sản phẩm thiếu dữ liệu số -> Không khớp

        const cardValue = parseFloat(cardValueStr);
        if (isNaN(cardValue)) return false;

        // Xử lý trường hợp đặc biệt dấu "+" (Ví dụ: 50+, 100+)
        if (filterValue.endsWith('+')) {
            const min = parseFloat(filterValue);
            return cardValue >= min;
        }

        // Xử lý khoảng bình thường phân tách bằng dấu "-" (Ví dụ: 0-5, 5-15)
        const parts = filterValue.split('-');
        const min = parseFloat(parts[0]);
        const max = parseFloat(parts[1]);
        return cardValue >= min && cardValue <= max;
    };

    const applyFilters = () => {
        const q = searchInput?.value.trim().toLowerCase() || '';
        const variety = varietyFilter?.value.toLowerCase() || '';
        const segment = segmentFilter?.value.toLowerCase() || '';
        const ageRange = ageFilter?.value || '';       // THÊM MỚI
        const heightRange = heightFilter?.value || ''; // THÊM MỚI
        let visibleCount = 0;

        productCards.forEach(card => {
            const title = (card.dataset.title || card.querySelector('.card-title')?.textContent || '').toLowerCase();
            const meta = (card.querySelector('.card-meta')?.textContent || '').toLowerCase();
            const cardVariety = (card.dataset.variety || '').toLowerCase();
            const cardSegment = (card.dataset.segment || '').toLowerCase();

            // Lấy giá trị tuổi và chiều cao từ thuộc tính data của thẻ card
            const cardAge = card.dataset.age || '';       // THÊM MỚI
            const cardHeight = card.dataset.height || ''; // THÊM MỚI

            const textMatch = !q || title.includes(q) || meta.includes(q);
            const varietyMatch = !variety || cardVariety === variety;
            const segmentMatch = !segment || cardSegment === segment;

            // Thực hiện tính toán so khớp khoảng số cho tuổi và chiều cao
            const ageMatch = checkRangeMatch(ageRange, cardAge);         // THÊM MỚI
            const heightMatch = checkRangeMatch(heightRange, cardHeight); // THÊM MỚI

            // Tổng hợp tất cả điều kiện lọc
            const shouldShow = textMatch && varietyMatch && segmentMatch && ageMatch && heightMatch;

            card.style.display = shouldShow ? '' : 'none';
            if (shouldShow) visibleCount += 1;
        });

        if (filterSummary) {
            filterSummary.textContent = `Hiển thị ${visibleCount} tác phẩm`;
        }
        if (emptyState) {
            emptyState.style.display = visibleCount ? 'none' : 'block';
        }
    };

    // Gắn sự kiện cho bộ lọc
    populateFilterOptions();
    if (searchInput) searchInput.addEventListener('input', applyFilters);
    if (varietyFilter) varietyFilter.addEventListener('change', applyFilters);
    if (segmentFilter) segmentFilter.addEventListener('change', applyFilters);
    if (ageFilter) ageFilter.addEventListener('change', applyFilters);       // THÊM MỚI
    if (heightFilter) heightFilter.addEventListener('change', applyFilters); // THÊM MỚI

    if (resetFiltersBtn) {
        resetFiltersBtn.addEventListener('click', () => {
            if (varietyFilter) varietyFilter.value = '';
            if (segmentFilter) segmentFilter.value = '';
            if (ageFilter) ageFilter.value = '';         // THÊM MỚI
            if (heightFilter) heightFilter.value = '';   // THÊM MỚI
            if (searchInput) searchInput.value = '';
            applyFilters();
        });
    }

    // Chạy mặc định lần đầu để quản lý hiển thị sản phẩm chính xác
    applyFilters();


    /* ----------------------------------------------------------------------
       5. KHẢ NĂNG TRUY CẬP (Accessibility)
       ---------------------------------------------------------------------- */
    document.body.addEventListener('keydown', (e) => {
        if (e.key === 'Tab') document.documentElement.classList.add('show-focus');
    });

    /* ----------------------------------------------------------------------
       6. ĐIỀU KHIỂN CLICK DROPDOWN ACCOUNT (Đồng bộ Premium)
       ---------------------------------------------------------------------- */
    // Khối bao bọc cha chứa toàn bộ menu user
    const userMenuContainer = document.querySelector('.user-menu-premium');
    const userBtn = document.querySelector('.user-btn-premium');

    if (userMenuContainer && userBtn) {
        // Sự kiện Click vào nút User
        userBtn.addEventListener('click', (e) => {
            e.stopPropagation(); // Ngăn sự kiện nổi bọt gây đóng menu ngay lập tức

            // Toggle class 'active' tại THẺ CHA để kích hoạt cả menu và xoay chevron từ CSS
            userMenuContainer.classList.toggle('active');

            // Cập nhật thuộc tính hỗ trợ tiếp cận ARIA
            const isExpanded = userMenuContainer.classList.contains('active');
            userBtn.setAttribute('aria-expanded', isExpanded);
        });

        // Click ra bất cứ đâu ngoài vùng Menu thì tự động đóng lại
        document.addEventListener('click', (e) => {
            if (!userMenuContainer.contains(e.target)) {
                userMenuContainer.classList.remove('active');
                userBtn.setAttribute('aria-expanded', 'false');
            }
        });
    }
});