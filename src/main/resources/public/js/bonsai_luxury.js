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
        const activeFilterChips = $("#activeFilterChips");
        const pagination = $("#luxuryPagination");
        const prevPageBtn = $("#luxuryPagePrev");
        const nextPageBtn = $("#luxuryPageNext");
        const pageStatus = $("#luxuryPageStatus");
        const tabs = $$(".luxury-tab");

        const normalize = (value) => (value || "").toString().trim().toLowerCase();
        const optionControls = [varietyFilter, segmentFilter].filter(Boolean);
        const rangeControls = [ageFilter, heightFilter].filter(Boolean);
        const filterControls = [...optionControls, ...rangeControls];
        const pageSize = Number.parseInt(productGrid.dataset.pageSize, 10) || 8;
        let currentPage = 1;

        const getRangeConfig = (control) => {
            const input = $("input[type='range']", control);
            return {
                input,
                values: (input?.dataset.values || "").split("|"),
                labels: (input?.dataset.labels || "").split("|")
            };
        };

        const updateRangeDisplay = (control) => {
            const { input, values, labels } = getRangeConfig(control);
            if (!input) return;

            const index = Number.parseInt(input.value, 10) || 0;
            const value = values[index] || "";
            const label = labels[index] || control.dataset.display || value;
            const max = Number.parseInt(input.max, 10) || values.length - 1 || 1;
            const progress = max > 0 ? (index / max) * 100 : 0;

            control.dataset.value = value;
            control.dataset.display = label;
            control.style.setProperty("--range-progress", `${progress}%`);
            $(".range-current", control)?.replaceChildren(document.createTextNode(label));
        };

        const getControlValue = (control) => {
            if (!control) return "";
            if ("value" in control) return control.value || "";
            return control.dataset.value || "";
        };

        const setControlValue = (control, value = "") => {
            if (!control) return;

            if ("value" in control) {
                control.value = value;
                return;
            }

            if (control.classList.contains("filter-range-control")) {
                const { input, values } = getRangeConfig(control);
                if (input) {
                    const selectedIndex = values.findIndex((rangeValue) => normalize(rangeValue) === normalize(value));
                    input.value = selectedIndex >= 0 ? selectedIndex.toString() : "0";
                }
                updateRangeDisplay(control);
                return;
            }

            control.dataset.value = value;
            $$(".filter-option", control).forEach((option) => {
                const isActive = normalize(option.dataset.value) === normalize(value);
                option.classList.toggle("active", isActive);
                option.setAttribute("aria-selected", isActive ? "true" : "false");
            });
        };

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

            const existingValues = new Set($$(".filter-option", varietyFilter).map((option) => option.dataset.value));
            const varieties = [...new Set(cards.map((card) => card.dataset.variety).filter(Boolean))]
                .sort((first, second) => first.localeCompare(second, "vi"));

            varieties.forEach((variety) => {
                if (existingValues.has(variety)) return;

                const option = document.createElement("button");
                option.type = "button";
                option.className = "filter-option";
                option.dataset.value = variety;
                option.setAttribute("aria-selected", "false");
                option.textContent = variety;
                varietyFilter.appendChild(option);
            });
        };

        const setSummary = (visibleCount) => {
            if (!summary) return;
            summary.textContent = `Hiển thị ${visibleCount} tác phẩm`;
        };

        const updatePagination = (totalItems) => {
            const totalPages = totalItems > 0 ? Math.ceil(totalItems / pageSize) : 0;
            const hasMultiplePages = totalPages > 1;

            if (currentPage > totalPages) {
                currentPage = totalPages || 1;
            }

            if (pageStatus) {
                pageStatus.textContent = `Trang ${totalItems > 0 ? currentPage : 0}/${totalPages || 0}`;
            }

            if (prevPageBtn) {
                prevPageBtn.disabled = !hasMultiplePages || currentPage <= 1;
            }

            if (nextPageBtn) {
                nextPageBtn.disabled = !hasMultiplePages || currentPage >= totalPages;
            }

            pagination?.classList.toggle("is-ready", hasMultiplePages);
        };

        const getSelectedText = (select) => {
            const value = getControlValue(select);
            if (!select || !value) return "";

            if ("options" in select) {
                return select.options[select.selectedIndex]?.textContent?.trim() || value;
            }

            if (select.classList.contains("filter-range-control")) {
                return select.dataset.display || value;
            }

            const activeOption = $$(".filter-option", select).find((option) => normalize(option.dataset.value) === normalize(value));
            return activeOption?.textContent?.trim() || value;
        };

        const setActiveState = () => {
            filterControls.forEach((control) => {
                control.classList.toggle("is-active", Boolean(getControlValue(control)));
            });
            searchInput?.classList.toggle("is-active", Boolean(searchInput.value.trim()));
        };

        const renderActiveFilters = () => {
            if (!activeFilterChips) return;

            const activeFilters = [
                searchInput?.value.trim() ? { icon: "fa-magnifying-glass", label: `Tìm: ${searchInput.value.trim()}` } : null,
                getControlValue(varietyFilter) ? { icon: "fa-seedling", label: getSelectedText(varietyFilter) } : null,
                getControlValue(segmentFilter) ? { icon: "fa-gem", label: getSelectedText(segmentFilter) } : null,
                getControlValue(ageFilter) ? { icon: "fa-hourglass-half", label: getSelectedText(ageFilter) } : null,
                getControlValue(heightFilter) ? { icon: "fa-ruler-vertical", label: getSelectedText(heightFilter) } : null
            ].filter(Boolean);

            activeFilterChips.replaceChildren();

            if (!activeFilters.length) {
                const defaultChip = document.createElement("span");
                defaultChip.className = "luxury-active-chip is-muted";
                defaultChip.innerHTML = '<i class="fa-solid fa-layer-group" aria-hidden="true"></i><span>Toàn bộ bộ sưu tập</span>';
                activeFilterChips.appendChild(defaultChip);
                return;
            }

            activeFilters.forEach((filter) => {
                const chip = document.createElement("span");
                chip.className = "luxury-active-chip";
                chip.innerHTML = `<i class="fa-solid ${filter.icon}" aria-hidden="true"></i><span></span>`;
                chip.querySelector("span").textContent = filter.label;
                activeFilterChips.appendChild(chip);
            });
        };

        const syncTabs = (segment) => {
            tabs.forEach((tab) => {
                tab.classList.toggle("active", normalize(tab.dataset.segment) === segment);
            });
        };

        const applyFilters = ({ resetPage = false } = {}) => {
            const query = normalize(searchInput?.value);
            const variety = normalize(getControlValue(varietyFilter));
            const segment = normalize(getControlValue(segmentFilter));
            const ageRange = getControlValue(ageFilter);
            const heightRange = getControlValue(heightFilter);
            const matchedCards = [];

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

                card.hidden = true;
                if (isVisible) matchedCards.push(card);
            });

            const visibleCount = matchedCards.length;
            const totalPages = visibleCount > 0 ? Math.ceil(visibleCount / pageSize) : 0;

            if (resetPage) {
                currentPage = 1;
            }

            if (currentPage > totalPages) {
                currentPage = totalPages || 1;
            }

            const pageStart = (currentPage - 1) * pageSize;
            const pageEnd = pageStart + pageSize;
            matchedCards.slice(pageStart, pageEnd).forEach((card) => {
                card.hidden = false;
            });

            if (emptyState) {
                emptyState.hidden = visibleCount > 0;
            }

            setSummary(visibleCount);
            updatePagination(visibleCount);
            syncTabs(segment);
            setActiveState();
            renderActiveFilters();
        };

        const resetFilters = () => {
            if (searchInput) searchInput.value = "";
            filterControls.forEach((control) => setControlValue(control, ""));
            applyFilters({ resetPage: true });
            showToast("Đã đặt lại bộ lọc");
        };

        populateVarieties();
        optionControls.forEach((control) => {
            setControlValue(control, getControlValue(control));
            control.addEventListener("click", (event) => {
                const option = event.target.closest(".filter-option");
                if (!option || !control.contains(option)) return;

                setControlValue(control, option.dataset.value || "");
                applyFilters({ resetPage: true });
            });
        });
        rangeControls.forEach((control) => {
            setControlValue(control, getControlValue(control));
            $("input[type='range']", control)?.addEventListener("input", () => {
                updateRangeDisplay(control);
                applyFilters({ resetPage: true });
            });
        });
        searchInput?.addEventListener("input", () => applyFilters({ resetPage: true }));

        tabs.forEach((tab) => {
            tab.addEventListener("click", () => {
                setControlValue(segmentFilter, tab.dataset.segment || "");
                applyFilters({ resetPage: true });
            });
        });

        prevPageBtn?.addEventListener("click", () => {
            if (currentPage <= 1) return;
            currentPage -= 1;
            applyFilters();
            productGrid.scrollIntoView({ behavior: prefersReducedMotion ? "auto" : "smooth", block: "start" });
        });

        nextPageBtn?.addEventListener("click", () => {
            currentPage += 1;
            applyFilters();
            productGrid.scrollIntoView({ behavior: prefersReducedMotion ? "auto" : "smooth", block: "start" });
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
