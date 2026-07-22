document.addEventListener("DOMContentLoaded", () => {
    const prefersReducedMotion = window.matchMedia("(prefers-reduced-motion: reduce)").matches;
    const $ = (selector, scope = document) => scope.querySelector(selector);
    const $$ = (selector, scope = document) => Array.from(scope.querySelectorAll(selector));

    const updateYear = () => {
        const yearEl = $("#year");
        if (yearEl) {
            yearEl.textContent = new Date().getFullYear().toString();
        }
    };

    const showToast = (message) => {
        const oldToast = $(".toast");
        if (oldToast) oldToast.remove();

        const toast = document.createElement("div");
        toast.className = "toast";
        toast.textContent = message;
        document.body.appendChild(toast);

        window.setTimeout(() => {
            toast.style.opacity = "0";
            window.setTimeout(() => toast.remove(), 280);
        }, 2600);
    };

    const initAnchorScroll = () => {
        $$('a[href^="#"]').forEach((anchor) => {
            const href = anchor.getAttribute("href");
            if (!href || href === "#") return;

            anchor.addEventListener("click", (event) => {
                const target = $(href);
                if (!target) return;

                event.preventDefault();
                target.scrollIntoView({
                    behavior: prefersReducedMotion ? "auto" : "smooth",
                    block: "start"
                });
            });
        });
    };

    const initReveal = () => {
        const items = $$(".luxury-product-card, .service, .atelier-showcase");
        if (!items.length || prefersReducedMotion) {
            items.forEach((item) => item.classList.add("visible"));
            return;
        }

        items.forEach((item) => item.classList.add("reveal-pending"));

        const observer = new IntersectionObserver((entries) => {
            entries.forEach((entry) => {
                if (!entry.isIntersecting) return;
                entry.target.classList.add("visible");
                observer.unobserve(entry.target);
            });
        }, { threshold: 0.16, rootMargin: "0px 0px -40px 0px" });

        items.forEach((item) => observer.observe(item));
    };

    const initSlider = ({ sliderSelector, slideSelector, prevSelector, nextSelector, interval = 5200 }) => {
        const slider = $(sliderSelector);
        if (!slider) return;

        const slides = $$(slideSelector, slider);
        if (slides.length <= 1) return;

        let activeIndex = Math.max(0, slides.findIndex((slide) => slide.classList.contains("active")));
        let timerId = null;

        const showSlide = (index) => {
            activeIndex = (index + slides.length) % slides.length;
            slides.forEach((slide, slideIndex) => {
                slide.classList.toggle("active", slideIndex === activeIndex);
            });
        };

        const next = () => showSlide(activeIndex + 1);
        const prev = () => showSlide(activeIndex - 1);
        const stop = () => {
            if (timerId) window.clearInterval(timerId);
            timerId = null;
        };
        const start = () => {
            if (prefersReducedMotion) return;
            stop();
            timerId = window.setInterval(next, interval);
        };

        $(prevSelector)?.addEventListener("click", () => {
            prev();
            start();
        });

        $(nextSelector)?.addEventListener("click", () => {
            next();
            start();
        });

        slider.addEventListener("mouseenter", stop);
        slider.addEventListener("mouseleave", start);
        slider.addEventListener("focusin", stop);
        slider.addEventListener("focusout", start);

        showSlide(activeIndex);
        start();
    };

    const initFilters = () => {
        const productGrid = $("#productGrid");
        if (!productGrid) return;

        const cards = $$(".luxury-product-card", productGrid);
        const emptyState = $(".empty-state", productGrid);
        const searchInput = $("#search");
        const varietyFilter = $("#varietyFilter");
        const segmentFilter = $("#segmentFilter");
        const ageFilter = $("#ageFilter");
        const heightFilter = $("#heightFilter");
        const resetBtn = $("#resetFilters");
        const summary = $("#filterSummary");
        const tabs = $$(".luxury-tab");

        const normalize = (value) => (value || "").toString().trim().toLowerCase();

        const inRange = (range, value) => {
            if (!range) return true;

            const numericValue = Number.parseFloat(value);
            if (Number.isNaN(numericValue)) return false;

            if (range.endsWith("+")) {
                return numericValue >= Number.parseFloat(range);
            }

            const [min, max] = range.split("-").map(Number.parseFloat);
            return numericValue >= min && numericValue <= max;
        };

        const populateVarieties = () => {
            if (!varietyFilter) return;

            const existingValues = new Set($$("option", varietyFilter).map((option) => option.value));
            const varieties = [...new Set(cards.map((card) => card.dataset.variety).filter(Boolean))]
                .sort((first, second) => first.localeCompare(second, "vi"));

            varieties.forEach((variety) => {
                if (existingValues.has(variety)) return;

                const option = document.createElement("option");
                option.value = variety;
                option.textContent = variety;
                varietyFilter.appendChild(option);
            });
        };

        const setSummary = (visibleCount) => {
            if (!summary) return;
            summary.textContent = `Hiển thị ${visibleCount} tác phẩm`;
        };

        const syncTabs = (segment) => {
            tabs.forEach((tab) => {
                tab.classList.toggle("active", normalize(tab.dataset.segment) === segment);
            });
        };

        const applyFilters = () => {
            const query = normalize(searchInput?.value);
            const variety = normalize(varietyFilter?.value);
            const segment = normalize(segmentFilter?.value);
            const ageRange = ageFilter?.value || "";
            const heightRange = heightFilter?.value || "";
            let visibleCount = 0;

            cards.forEach((card) => {
                const title = normalize(card.dataset.title || $(".card-title", card)?.textContent);
                const code = normalize(card.dataset.id || $(".card-eyebrow", card)?.textContent);
                const meta = normalize($(".card-meta", card)?.textContent);
                const cardVariety = normalize(card.dataset.variety);
                const cardSegment = normalize(card.dataset.segment);

                const matchesText = !query || title.includes(query) || code.includes(query) || meta.includes(query) || cardVariety.includes(query);
                const matchesVariety = !variety || cardVariety === variety;
                const matchesSegment = !segment || cardSegment === segment;
                const matchesAge = inRange(ageRange, card.dataset.age);
                const matchesHeight = inRange(heightRange, card.dataset.height);
                const isVisible = matchesText && matchesVariety && matchesSegment && matchesAge && matchesHeight;

                card.hidden = !isVisible;
                if (isVisible) visibleCount += 1;
            });

            if (emptyState) {
                emptyState.hidden = visibleCount > 0;
            }

            setSummary(visibleCount);
            syncTabs(segment);
        };

        const resetFilters = () => {
            [searchInput, varietyFilter, segmentFilter, ageFilter, heightFilter].forEach((control) => {
                if (control) control.value = "";
            });
            applyFilters();
            showToast("Đã đặt lại bộ lọc");
        };

        populateVarieties();
        [searchInput, varietyFilter, segmentFilter, ageFilter, heightFilter].forEach((control) => {
            control?.addEventListener(control === searchInput ? "input" : "change", applyFilters);
        });

        tabs.forEach((tab) => {
            tab.addEventListener("click", () => {
                if (segmentFilter) {
                    segmentFilter.value = tab.dataset.segment || "";
                }
                applyFilters();
            });
        });

        resetBtn?.addEventListener("click", resetFilters);
        applyFilters();
    };

    const initPointerGlow = () => {
        if (prefersReducedMotion) return;

        const glowItems = $$(".luxury-product-card, .service, .filter-panel, .hero-right");
        glowItems.forEach((item) => {
            item.addEventListener("pointermove", (event) => {
                const rect = item.getBoundingClientRect();
                const x = ((event.clientX - rect.left) / rect.width) * 100;
                const y = ((event.clientY - rect.top) / rect.height) * 100;

                item.style.setProperty("--pointer-x", `${x.toFixed(2)}%`);
                item.style.setProperty("--pointer-y", `${y.toFixed(2)}%`);
            });
        });
    };

    document.body.addEventListener("keydown", (event) => {
        if (event.key === "Tab") {
            document.documentElement.classList.add("show-focus");
        }
    });

    updateYear();
    initAnchorScroll();
    initReveal();
    initSlider({
        sliderSelector: "#heroSlider",
        slideSelector: ".slide",
        prevSelector: ".prev",
        nextSelector: ".next",
        interval: 5200
    });
    initSlider({
        sliderSelector: "#studioSlider",
        slideSelector: ".studio-slide",
        prevSelector: ".prev-studio",
        nextSelector: ".next-studio",
        interval: 6400
    });
    initFilters();
    initPointerGlow();
});
