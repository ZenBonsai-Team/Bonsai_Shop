document.addEventListener("DOMContentLoaded", () => {
    "use strict";

    const $ = (selector, scope = document) => scope.querySelector(selector);
    const $$ = (selector, scope = document) => Array.from(scope.querySelectorAll(selector));
    const body = document.body;

    const createElement = (tagName, className, textContent) => {
        const element = document.createElement(tagName);
        if (className) element.className = className;
        if (textContent !== undefined) element.textContent = textContent;
        return element;
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
        const icon = createElement("i", toastType === "success" ? "fa-solid fa-check" : "fa-solid fa-xmark");
        icon.setAttribute("aria-hidden", "true");
        toast.appendChild(icon);
        toast.appendChild(createElement("span", "", message));
        container.appendChild(toast);
        window.setTimeout(() => toast.remove(), 5000);
    };

    const bookingModal = $("#bookingModal");
    const bookingForm = $("#actualBookingForm");
    const closeBookingButton = $("#closeBookingBtn");
    const cancelBookingButton = $("#cancelBookingBtn");
    const dateInput = $("#appointmentDate");
    const timeInput = $("#appointmentTime");

    if (!bookingModal) return;

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
        if (bookingModal.hidden) return;
        bookingModal.classList.remove("is-open");
        body.classList.remove("modal-open");
        window.setTimeout(() => {
            bookingModal.hidden = true;
        }, 180);
    };

    const open = (trigger) => {
        if (trigger?.disabled || trigger?.dataset.available === "false") {
            showToast("Hiện chưa mở lịch thăm vườn.", "error");
            return;
        }

        if (body?.dataset.authenticated === "false") {
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

    $$(".js-booking-trigger, .schedule-btn").forEach((button) => {
        button.addEventListener("click", () => open(button));
    });

    closeBookingButton?.addEventListener("click", close);
    cancelBookingButton?.addEventListener("click", close);
    $("[data-close-modal]", bookingModal)?.addEventListener("click", close);

    document.addEventListener("keydown", (event) => {
        if (event.key === "Escape") close();
    });

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
});
