function toggleUserMenu() {
    const menu = document.getElementById("userDropdownMenu");
    menu.classList.toggle("show");
}

document.addEventListener("click", function (event) {

    const dropdown = document.querySelector(".user-dropdown");
    const menu = document.getElementById("userDropdownMenu");

    if (!dropdown.contains(event.target)) {
        menu.classList.remove("show");
    }

});

const heroImages = [
    "../images/hero-bonsai.png",
    "../images/hero-bonsai-2.png",
    "../images/hero-bonsai-3.png"
];

let currentHeroIndex = 0;

function changeHeroImage() {
    const hero = document.getElementById("heroSlider");

    hero.style.backgroundImage =
        `linear-gradient(rgba(0, 0, 0, 0.25), rgba(0, 0, 0, 0.45)), url('${heroImages[currentHeroIndex]}')`;
}

function nextHeroImage() {
    currentHeroIndex++;

    if (currentHeroIndex >= heroImages.length) {
        currentHeroIndex = 0;
    }

    changeHeroImage();
}

function prevHeroImage() {
    currentHeroIndex--;

    if (currentHeroIndex < 0) {
        currentHeroIndex = heroImages.length - 1;
    }

    changeHeroImage();
}

// ================================================
// FILM SLIDER - 1 ẢNH GIỮA PHÓNG TO, 4 ẢNH NỀN PHÍA SAU
// Tự động đổi ảnh trung tâm sau mỗi 30 giây
// ================================================

(function () {
    const INTERVAL_MS = 30000; // 30 giây

    let currentIndex = 0;
    let autoTimer = null;

    const track  = document.getElementById('filmTrack');
    const dotsEl = document.getElementById('filmDots');

    if (!track) return;

    const cards = Array.from(track.querySelectorAll('.film-card'));
    const dots  = dotsEl ? Array.from(dotsEl.querySelectorAll('.film-dot')) : [];
    const total = cards.length;

    if (total === 0) return;

    // Tính khoảng cách vòng tròn giữa card i và card đang active
    // Kết quả: 0 = giữa, -1/-2 = lệch trái, 1/2 = lệch phải
    function getOffset(i, current, total) {
        let diff = i - current;
        if (diff > total / 2)  diff -= total;
        if (diff < -total / 2) diff += total;
        return diff;
    }

    // ===== CẬP NHẬT GIAO DIỆN =====
    function updateSlider(index) {
        cards.forEach((card, i) => {
            card.classList.remove('active');
            card.removeAttribute('data-offset');

            const diff = getOffset(i, index, total);

            if (diff === 0) {
                card.classList.add('active');
            } else if (Math.abs(diff) <= 2) {
                card.setAttribute('data-offset', diff);
            } else {
                // Nếu có nhiều hơn 5 sản phẩm, các ảnh dư sẽ ẩn đi
                card.setAttribute('data-offset', 'hidden');
            }
        });

        dots.forEach((dot, i) => {
            dot.classList.toggle('active', i === index);
        });

        startProgress(index);
    }

    // ===== PROGRESS BAR (chỉ chạy trên ảnh giữa) =====
    function startProgress(index) {
        cards.forEach(card => {
            const b = card.querySelector('.film-progress-bar');
            if (b) {
                b.classList.remove('running');
                b.style.width = '0%';
                void b.offsetWidth; // reset animation
            }
        });

        const activeCard = cards[index];
        const bar = activeCard ? activeCard.querySelector('.film-progress-bar') : null;
        if (bar) bar.classList.add('running');
    }

    // ===== TỰ ĐỘNG CHUYỂN =====
    function startAuto() {
        stopAuto();
        autoTimer = setInterval(() => {
            filmNext();
        }, INTERVAL_MS);
    }

    function stopAuto() {
        if (autoTimer) {
            clearInterval(autoTimer);
            autoTimer = null;
        }
    }

    // ===== ĐIỀU HƯỚNG =====
    window.filmNext = function () {
        currentIndex = (currentIndex + 1) % total;
        updateSlider(currentIndex);
        startAuto();
    };

    window.filmPrev = function () {
        currentIndex = (currentIndex - 1 + total) % total;
        updateSlider(currentIndex);
        startAuto();
    };

    window.filmGoTo = function (index) {
        currentIndex = index;
        updateSlider(currentIndex);
        startAuto();
    };

    // ===== BẤM VÀO ẢNH NỀN → ĐƯA RA GIỮA (thay vì điều hướng luôn) =====
    cards.forEach((card, i) => {
        const link = card.querySelector('.film-card-link');
        if (!link) return;
        link.addEventListener('click', (e) => {
            if (i !== currentIndex) {
                e.preventDefault();
                filmGoTo(i);
            }
            // Nếu đang là ảnh giữa thì cho phép điều hướng bình thường
        });
    });

    // ===== SWIPE TRÊN MOBILE =====
    let touchStartX = 0;

    track.addEventListener('touchstart', (e) => {
        touchStartX = e.touches[0].clientX;
    }, { passive: true });

    track.addEventListener('touchend', (e) => {
        const diff = touchStartX - e.changedTouches[0].clientX;
        if (Math.abs(diff) > 50) {
            diff > 0 ? filmNext() : filmPrev();
        }
    }, { passive: true });

    // ===== PAUSE KHI HOVER =====
    const wrapper = document.getElementById('filmSlider');
    if (wrapper) {
        wrapper.addEventListener('mouseenter', stopAuto);
        wrapper.addEventListener('mouseleave', startAuto);
    }

    // ===== KHỞI ĐỘNG =====
    updateSlider(0);
    startAuto();

})();

const navbar = document.querySelector(".navbar");
const btn = document.getElementById("toggleNavbarBtn");

let isHidden = false;

btn.addEventListener("click", function () {

    if (isHidden) {

        navbar.style.transform = "translateY(0)";
        btn.innerHTML = "⬆";

    } else {

        navbar.style.transform = "translateY(-100%)";
        btn.innerHTML = "⬇";
    }

    isHidden = !isHidden;
});