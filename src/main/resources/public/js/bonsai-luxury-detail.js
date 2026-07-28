document.addEventListener("DOMContentLoaded", () => {
    "use strict";

    const prefersReducedMotion = window.matchMedia("(prefers-reduced-motion: reduce)").matches;
    const $ = (selector, scope = document) => scope.querySelector(selector);
    const $$ = (selector, scope = document) => Array.from(scope.querySelectorAll(selector));
    const body = document.body;
    const isAuthenticated = body?.dataset.authenticated === "true";

    const createElement = (tagName, className, textContent) => {
        const element = document.createElement(tagName);
        if (className) element.className = className;
        if (textContent !== undefined) element.textContent = textContent;
        return element;
    };

    const createIcon = (className) => {
        const icon = createElement("i", className);
        icon.setAttribute("aria-hidden", "true");
        return icon;
    };

    const showToast = (message, type = "success") => {
        let container = $(".flash-container");
        if (!container) {
            container = createElement("div", "flash-container");
            container.setAttribute("aria-live", "polite");
            document.body.appendChild(container);
        }

        const toastType = type === "error" ? "error" : "success";
        const toast = createElement("div", `flash-message flash-${toastType}`);
        toast.appendChild(createIcon(toastType === "success" ? "fa-solid fa-check" : "fa-solid fa-xmark"));
        toast.appendChild(createElement("span", "", message));
        container.appendChild(toast);
        window.setTimeout(() => toast.remove(), 5400);
    };

    const initReveal = () => {
        const items = $$(".summary-card, .main-image-wrap, .media-note, .detail-trust-strip, .lux-section, .specs-table-card, .profile-grid article, .artisan-profile-banner, .ownership-panel, .ownership-step");
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
        }, { threshold: 0.14, rootMargin: "0px 0px -40px 0px" });

        items.forEach((item) => observer.observe(item));
    };

    const initPointerGlow = () => {
        if (prefersReducedMotion) return;

        $$(".main-image-wrap, .summary-card, .specs-table-card, .profile-grid article, .artisan-profile-banner, .ownership-panel").forEach((item) => {
            item.addEventListener("pointermove", (event) => {
                const rect = item.getBoundingClientRect();
                const x = ((event.clientX - rect.left) / rect.width) * 100;
                const y = ((event.clientY - rect.top) / rect.height) * 100;
                item.style.setProperty("--pointer-x", `${x.toFixed(2)}%`);
                item.style.setProperty("--pointer-y", `${y.toFixed(2)}%`);
            });
        });
    };

    const initGallery = (lightbox) => {
        const mainImage = $("#mainImage");
        const mainVideo = $("#mainVideo");
        const thumbnailButtons = $$(".thumb-item");
        const thumbnailRows = $$(".thumb-row");
        const mainPreviousButton = $(".main-media-prev");
        const mainNextButton = $(".main-media-next");
        const expandMediaButton = $("#expandMediaBtn");
        const toggleSoundButton = $("#toggleSoundBtn");
        const galleryItems = [
            ...(mainVideo?.src ? [{
                src: mainVideo.src,
                type: "VIDEO",
                alt: mainImage?.alt || "Bonsai Luxury",
                button: null
            }] : []),
            ...thumbnailButtons.map((button) => ({
                src: button.dataset.src,
                type: (button.dataset.type || "IMAGE").toUpperCase(),
                alt: button.dataset.alt || mainImage?.alt || "Bonsai Luxury",
                button
            })).filter((item) => item.src)
        ];
        let activeMediaIndex = galleryItems.findIndex((item) => item.type === "VIDEO" && !mainVideo?.classList.contains("is-hidden"));
        if (activeMediaIndex < 0) {
            activeMediaIndex = Math.max(0, galleryItems.findIndex((item) => item.button?.classList.contains("is-active")));
        }

        const syncExpandButton = (mediaUrl, mediaType, mediaAlt) => {
            if (!expandMediaButton || !mediaUrl) return;
            expandMediaButton.dataset.src = mediaUrl;
            expandMediaButton.dataset.type = mediaType || "IMAGE";
            expandMediaButton.dataset.alt = mediaAlt || "Bonsai Luxury";
        };

        const showImage = (imageUrl, imageAlt) => {
            if (!mainImage || !imageUrl) return;
            mainVideo?.pause();
            if (mainVideo) {
                mainVideo.classList.add("is-hidden");
                mainVideo.removeAttribute("src");
                mainVideo.load();
            }
            mainImage.alt = imageAlt || mainImage.alt || "Bonsai Luxury";
            mainImage.classList.remove("is-hidden");
            mainImage.classList.add("is-switching");
            const clearState = () => mainImage.classList.remove("is-switching");
            mainImage.addEventListener("load", clearState, { once: true });
            mainImage.src = imageUrl;
            syncExpandButton(imageUrl, "IMAGE", mainImage.alt);
            window.setTimeout(clearState, 600);
        };

        const showVideo = (videoUrl) => {
            if (!mainVideo || !videoUrl) return;
            mainImage?.classList.add("is-hidden");
            mainVideo.src = videoUrl;
            mainVideo.classList.remove("is-hidden");
            mainVideo.load();
            syncExpandButton(videoUrl, "VIDEO", mainImage?.alt || "Bonsai Luxury");
        };

        const activateMedia = (index) => {
            if (!galleryItems.length) return;
            activeMediaIndex = (index + galleryItems.length) % galleryItems.length;
            const item = galleryItems[activeMediaIndex];

            thumbnailButtons.forEach((button) => button.classList.remove("is-active"));
            item.button?.classList.add("is-active");
            item.button?.scrollIntoView({ behavior: "smooth", block: "nearest", inline: "center" });

            if (item.type === "VIDEO") {
                showVideo(item.src);
                return;
            }
            showImage(item.src, item.alt);
        };

        thumbnailButtons.forEach((button) => {
            button.addEventListener("click", () => {
                const nextMediaUrl = button.dataset.src;
                const nextMediaType = (button.dataset.type || "IMAGE").toUpperCase();
                if (!nextMediaUrl) return;

                const nextIndex = galleryItems.findIndex((item) => item.button === button);
                if (nextIndex >= 0) {
                    activateMedia(nextIndex);
                    return;
                }
                if (nextMediaType === "VIDEO") showVideo(nextMediaUrl);
                else showImage(nextMediaUrl, button.dataset.alt);
            });
        });

        mainPreviousButton?.addEventListener("click", () => {
            activateMedia(activeMediaIndex - 1);
        });

        mainNextButton?.addEventListener("click", () => {
            activateMedia(activeMediaIndex + 1);
        });

        thumbnailRows.forEach((row) => {
            const carousel = row.closest(".thumb-carousel");
            if (!carousel) return;
            const previousButton = $(".thumb-nav-prev", carousel);
            const nextButton = $(".thumb-nav-next", carousel);
            const getScrollDistance = () => row.querySelector(".thumb-item")?.offsetWidth
                    ? row.querySelector(".thumb-item").offsetWidth + 12
                    : row.clientWidth;

            previousButton?.addEventListener("click", () => {
                row.scrollBy({ left: -getScrollDistance(), behavior: "smooth" });
            });

            nextButton?.addEventListener("click", () => {
                row.scrollBy({ left: getScrollDistance(), behavior: "smooth" });
            });
        });

        expandMediaButton?.addEventListener("click", () => {
            lightbox?.open(expandMediaButton.dataset.src, expandMediaButton.dataset.alt, expandMediaButton.dataset.type);
        });

        toggleSoundButton?.addEventListener("click", () => {
            if (!mainVideo) return;
            mainVideo.muted = !mainVideo.muted;
            const icon = $("i", toggleSoundButton);
            if (icon) {
                icon.className = mainVideo.muted ? "fa-solid fa-volume-xmark" : "fa-solid fa-volume-high";
            }
            toggleSoundButton.setAttribute("aria-label", mainVideo.muted ? "Bật tiếng video" : "Tắt tiếng video");
        });
    };

    const initLightbox = () => {
        const overlayRoot = $("#overlayRoot");
        if (!overlayRoot) return { close: () => {}, open: () => {} };

        let activeLightbox = null;

        const close = () => {
            if (!activeLightbox) return;

            activeLightbox.classList.remove("is-open");
            const currentLightbox = activeLightbox;
            activeLightbox = null;

            window.setTimeout(() => {
                currentLightbox.remove();
                overlayRoot.setAttribute("aria-hidden", "true");
            }, 180);
        };

        const open = (mediaUrl, mediaAlt, mediaType = "IMAGE") => {
            if (!mediaUrl) return;
            close();

            const normalizedMediaType = (mediaType || "IMAGE").toUpperCase();
            const lightbox = createElement("div", "lightbox");
            lightbox.setAttribute("role", "dialog");
            lightbox.setAttribute("aria-modal", "true");
            lightbox.setAttribute("aria-label", normalizedMediaType === "VIDEO" ? "Video chi tiết Bonsai" : "Ảnh chi tiết Bonsai");

            const panel = createElement("div", "lightbox-panel");
            const closeButton = createElement("button", "lightbox-close");
            closeButton.type = "button";
            closeButton.setAttribute("aria-label", "Đóng media lớn");
            closeButton.appendChild(createIcon("fa-solid fa-xmark"));

            const mediaElement = normalizedMediaType === "VIDEO"
                    ? createElement("video", "lightbox-image lightbox-video")
                    : createElement("img", "lightbox-image");
            mediaElement.src = mediaUrl;
            if (normalizedMediaType === "VIDEO") {
                mediaElement.controls = true;
                mediaElement.autoplay = true;
                mediaElement.playsInline = true;
            } else {
                mediaElement.alt = mediaAlt || "Ảnh chi tiết Bonsai Luxury";
            }

            panel.append(closeButton, mediaElement);
            lightbox.appendChild(panel);
            overlayRoot.appendChild(lightbox);
            overlayRoot.removeAttribute("aria-hidden");
            activeLightbox = lightbox;

            requestAnimationFrame(() => lightbox.classList.add("is-open"));
            closeButton.addEventListener("click", close);
            lightbox.addEventListener("click", (event) => {
                if (event.target === lightbox) close();
            });
            closeButton.focus();
        };

        $$(".gallery-item").forEach((item) => {
            item.addEventListener("click", () => {
                const image = $("img", item);
                open(item.dataset.src || image?.src, image?.alt, "IMAGE");
            });
        });

        return { close, open };
    };

    const initBooking = () => {
        const bookingModal = $("#bookingModal");
        const bookingForm = $("#actualBookingForm");
        const closeBookingButton = $("#closeBookingBtn");
        const cancelBookingButton = $("#cancelBookingBtn");
        const dateInput = $("#appointmentDate");
        const timeInput = $("#appointmentTime");

        const toLocalDateValue = (date) => {
            const year = date.getFullYear();
            const month = String(date.getMonth() + 1).padStart(2, "0");
            const day = String(date.getDate()).padStart(2, "0");
            return `${year}-${month}-${day}`;
        };

        const parseDateValue = (value) => {
            const [year, month, day] = value.split("-").map(Number);
            return new Date(year, month - 1, day);
        };

        const getTomorrowValue = () => {
            const tomorrow = new Date();
            tomorrow.setDate(tomorrow.getDate() + 1);
            return toLocalDateValue(tomorrow);
        };

        const close = () => {
            if (!bookingModal || bookingModal.hidden) return;
            bookingModal.classList.remove("is-open");
            body.classList.remove("modal-open");
            window.setTimeout(() => {
                bookingModal.hidden = true;
            }, 180);
        };

        const open = (trigger) => {
            if (!bookingModal) return;
            if (trigger.disabled || trigger.dataset.available !== "true") {
                showToast("Tác phẩm hiện chưa mở lịch xem riêng.", "error");
                return;
            }
            if (!isAuthenticated) {
                window.location.href = "/login";
                return;
            }

            const tomorrowValue = getTomorrowValue();
            if (dateInput) {
                dateInput.min = tomorrowValue;
                if (!dateInput.value || dateInput.value < tomorrowValue) {
                    dateInput.value = tomorrowValue;
                }
            }
            if (timeInput && (!timeInput.value || timeInput.value < "08:00" || timeInput.value > "17:00")) {
                timeInput.value = "09:00";
            }

            bookingModal.hidden = false;
            requestAnimationFrame(() => bookingModal.classList.add("is-open"));
            body.classList.add("modal-open");
            closeBookingButton?.focus();
        };

        $$(".schedule-btn").forEach((button) => {
            button.addEventListener("click", () => open(button));
        });

        closeBookingButton?.addEventListener("click", close);
        cancelBookingButton?.addEventListener("click", close);
        $("[data-close-modal]", bookingModal || document)?.addEventListener("click", close);

        timeInput?.addEventListener("change", () => {
            if (timeInput.value && (timeInput.value < "08:00" || timeInput.value > "17:00")) {
                showToast("Vui lòng chọn giờ xem trong khung 08:00 - 17:00.", "error");
                timeInput.value = "09:00";
            }
        });

        bookingForm?.addEventListener("submit", (event) => {
            const tomorrow = parseDateValue(getTomorrowValue());
            const selectedDate = dateInput?.value ? parseDateValue(dateInput.value) : null;

            if (!selectedDate || selectedDate < tomorrow) {
                event.preventDefault();
                showToast("Vui lòng chọn ngày xem từ ngày mai trở đi.", "error");
                dateInput?.focus();
                return;
            }

            if (!timeInput?.value || timeInput.value < "08:00" || timeInput.value > "17:00") {
                event.preventDefault();
                showToast("Vui lòng chọn giờ xem trong khung 08:00 - 17:00.", "error");
                timeInput?.focus();
                return;
            }

            const submitButton = $(".submit-booking-btn", bookingForm);
            if (submitButton) {
                submitButton.disabled = true;
                submitButton.classList.add("is-submitting");
                submitButton.textContent = "Đang xử lý...";
            }
        });

        return { close };
    };

    const updateYear = () => {
        const year = $("#year");
        if (year) year.textContent = String(new Date().getFullYear());
    };

    document.body.addEventListener("keydown", (event) => {
        if (event.key === "Tab") {
            document.documentElement.classList.add("show-focus");
        }
    });

    initReveal();
    initPointerGlow();
    const lightbox = initLightbox();
    initGallery(lightbox);
    const booking = initBooking();
    updateYear();

    document.addEventListener("keydown", (event) => {
        if (event.key !== "Escape") return;
        lightbox.close();
        booking.close();
    });
});
