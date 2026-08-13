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

    const initPointerGlow = () => {
        if (prefersReducedMotion) return;

        const glowItems = $$(".luxury-product-card, .service, .hero-right");
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
    initPointerGlow();
});
