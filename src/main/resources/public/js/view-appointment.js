document.addEventListener("DOMContentLoaded", () => {
    "use strict";

    const prefersReducedMotion = window.matchMedia("(prefers-reduced-motion: reduce)").matches;
    const $ = (selector, scope = document) => scope.querySelector(selector);
    const $$ = (selector, scope = document) => Array.from(scope.querySelectorAll(selector));
    const body = document.body;

    const createElement = (tagName, className, textContent) => {
        const element = document.createElement(tagName);
        if (className) element.className = className;
        if (textContent !== undefined) element.textContent = textContent;
        return element;
    };

    const showToast = (message, type = "info") => {
        const oldToast = $(".luxury-toast");
        if (oldToast) oldToast.remove();

        const toast = createElement("div", `luxury-toast ${type}`, message);
        document.body.appendChild(toast);
        window.setTimeout(() => toast.remove(), 4200);
    };

    const initFlashMessages = () => {
        const success = $("#carrier-success")?.textContent?.trim();
        const error = $("#carrier-error")?.textContent?.trim();
        if (success) showToast(success, "success");
        if (error) showToast(error, "danger");
    };

    const initReveal = () => {
        const items = $$(".appointment-hero-copy, .appointment-summary-card, .section-heading-row, .appointment-card, .empty-appointment-state");
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

        $$(".appointment-summary-card, .appointment-card").forEach((item) => {
            item.addEventListener("pointermove", (event) => {
                const rect = item.getBoundingClientRect();
                const x = ((event.clientX - rect.left) / rect.width) * 100;
                const y = ((event.clientY - rect.top) / rect.height) * 100;
                item.style.setProperty("--pointer-x", `${x.toFixed(2)}%`);
                item.style.setProperty("--pointer-y", `${y.toFixed(2)}%`);
            });
        });
    };

    const openModal = (modal) => {
        if (!modal) return;
        modal.hidden = false;
        body.classList.add("modal-open");
        requestAnimationFrame(() => modal.classList.add("is-open"));
        $(".modal-close", modal)?.focus();
    };

    const closeModal = (modal) => {
        if (!modal || modal.hidden) return;
        modal.classList.remove("is-open");
        body.classList.remove("modal-open");
        window.setTimeout(() => {
            modal.hidden = true;
        }, 180);
    };

    const formatDateTime = (value) => {
        if (!value) return { date: "", time: "" };

        const dateObject = new Date(value);
        if (Number.isNaN(dateObject.getTime())) {
            return { date: value, time: "" };
        }

        const day = String(dateObject.getDate()).padStart(2, "0");
        const month = String(dateObject.getMonth() + 1).padStart(2, "0");
        const year = dateObject.getFullYear();
        const hours = String(dateObject.getHours()).padStart(2, "0");
        const minutes = String(dateObject.getMinutes()).padStart(2, "0");

        return {
            date: `${day}/${month}/${year}`,
            dateValue: `${year}-${month}-${day}`,
            time: `${hours}:${minutes}`
        };
    };

    const toLocalDateValue = (date) => {
        const year = date.getFullYear();
        const month = String(date.getMonth() + 1).padStart(2, "0");
        const day = String(date.getDate()).padStart(2, "0");
        return `${year}-${month}-${day}`;
    };

    const getTomorrowValue = () => {
        const tomorrow = new Date();
        tomorrow.setDate(tomorrow.getDate() + 1);
        return toLocalDateValue(tomorrow);
    };

    const initDetailModal = () => {
        const modal = $("#appointmentModal");
        if (!modal) return { close: () => {} };

        const close = () => closeModal(modal);

        $$(".view-detail-btn").forEach((button) => {
            button.addEventListener("click", async () => {
                const id = button.dataset.id;
                if (!id) return;

                showToast("Đang tải chi tiết lịch hẹn...", "info");
                try {
                    const response = await fetch(`/appointments/detail/${id}`);
                    if (!response.ok) throw new Error("Không thể tải thông tin lịch hẹn.");
                    const data = await response.json();
                    const dateTime = formatDateTime(data.appointmentDate);

                    $("#detailName").textContent = "Lịch thăm vườn Bonsai Luxury";
                    $("#detailCode").textContent = `APT-${data.appointmentId || id}`;
                    $("#detailDate").textContent = dateTime.date;
                    $("#detailTime").textContent = dateTime.time || data.appointmentTime || "";
                    $("#detailStatus").textContent = data.status || "PENDING";
                    $("#detailNote").textContent = data.note || "Không có ghi chú riêng";

                    openModal(modal);
                } catch (error) {
                    showToast(error.message, "danger");
                }
            });
        });

        $$(".close-modal-btn, [data-close-detail]", modal).forEach((element) => {
            element.addEventListener("click", close);
        });

        return { close };
    };

    const initUpdateModal = () => {
        const modal = $("#updateAppointmentModal");
        const form = $("#updateAppointmentForm");
        const dateInput = $("#updateDate");
        const timeInput = $("#updateTime");
        if (!modal || !form) return { close: () => {} };

        const close = () => closeModal(modal);
        const tomorrowValue = getTomorrowValue();
        if (dateInput) dateInput.min = tomorrowValue;

        $$(".update-btn").forEach((button) => {
            button.addEventListener("click", async () => {
                const card = button.closest(".appointment-card");
                const status = $(".appointment-status-tag", card)?.dataset.status || "";
                if (status !== "PENDING") {
                    showToast("Chỉ lịch hẹn PENDING mới được cập nhật.", "danger");
                    return;
                }

                const id = button.dataset.id;
                if (!id) return;

                showToast("Đang tải dữ liệu chỉnh sửa...", "info");
                try {
                    const response = await fetch(`/appointments/detail/${id}`);
                    if (!response.ok) throw new Error("Không thể tải thông tin chỉnh sửa.");
                    const data = await response.json();
                    const dateTime = formatDateTime(data.appointmentDate);

                    $("#updateName").textContent = "Lịch thăm vườn Bonsai Luxury";
                    $("#updateCode").textContent = `APT-${data.appointmentId || id}`;
                    $("#updateNote").value = data.note || "";
                    form.setAttribute("action", `/appointments/update/${id}`);

                    if (dateInput) {
                        dateInput.value = dateTime.dateValue && dateTime.dateValue >= tomorrowValue ? dateTime.dateValue : tomorrowValue;
                    }

                    if (timeInput) {
                        const safeTime = dateTime.time && dateTime.time >= "08:00" && dateTime.time <= "17:00" ? dateTime.time : "09:00";
                        timeInput.value = safeTime;
                    }

                    openModal(modal);
                } catch (error) {
                    showToast(error.message, "danger");
                }
            });
        });

        $$(".id-close-update, .id-close-update-btn, [data-close-update]", modal).forEach((element) => {
            element.addEventListener("click", close);
        });

        form.addEventListener("submit", (event) => {
            if (!dateInput?.value || dateInput.value < tomorrowValue) {
                event.preventDefault();
                showToast("Vui lòng chọn ngày xem từ ngày mai trở đi.", "danger");
                dateInput?.focus();
                return;
            }

            if (!timeInput?.value || timeInput.value < "08:00" || timeInput.value > "17:00") {
                event.preventDefault();
                showToast("Vui lòng chọn giờ xem trong khung 08:00 - 17:00.", "danger");
                timeInput?.focus();
            }
        });

        return { close };
    };

    const initCancelModal = () => {
        const modal = $("#cancelAppointmentModal");
        const form = $("#cancelAppointmentForm");
        if (!modal || !form) return { close: () => {} };

        const close = () => closeModal(modal);

        $$(".trigger-cancel-modal").forEach((button) => {
            button.addEventListener("click", () => {
                const card = button.closest(".appointment-card");
                const status = $(".appointment-status-tag", card)?.dataset.status || "";
                if (status !== "PENDING") {
                    showToast("Chỉ lịch hẹn PENDING mới được hủy.", "danger");
                    return;
                }

                const id = button.dataset.id;
                if (!id) return;

                form.setAttribute("action", `/appointments/cancel/${id}`);
                openModal(modal);
            });
        });

        $$(".id-close-cancel, .id-close-cancel-btn, [data-close-cancel]", modal).forEach((element) => {
            element.addEventListener("click", close);
        });

        form.addEventListener("submit", () => {
            showToast("Đang xử lý yêu cầu hủy...", "info");
        });

        return { close };
    };

    const detailModal = initDetailModal();
    const updateModal = initUpdateModal();
    const cancelModal = initCancelModal();

    document.addEventListener("keydown", (event) => {
        if (event.key === "Tab") {
            document.documentElement.classList.add("show-focus");
        }
        if (event.key === "Escape") {
            detailModal.close();
            updateModal.close();
            cancelModal.close();
        }
    });

    const year = $("#year");
    if (year) year.textContent = String(new Date().getFullYear());

    initFlashMessages();
    initReveal();
    initPointerGlow();
});
