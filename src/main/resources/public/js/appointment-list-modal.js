document.addEventListener("DOMContentLoaded", () => {
    "use strict";

    const $ = (selector, scope = document) => scope.querySelector(selector);
    const $$ = (selector, scope = document) => Array.from(scope.querySelectorAll(selector));
    const body = document.body;

    const listModal = $("#appointmentListModal");
    const detailModal = $("#appointmentDetailModal");
    const listContent = $("#appointmentListContent");
    const listLoading = $("#appointmentListLoading");
    const listEmpty = $("#appointmentListEmpty");
    const updateForm = $("#appointmentUpdateForm");
    const cancelForm = $("#appointmentCancelForm");
    const showEditButton = $("#aptShowEditBtn");
    const cancelAppointmentButton = $("#aptCancelAppointmentBtn");
    const cancelEditButton = $("#aptCancelEditBtn");
    const lockedMessage = $("#appointmentLockedMessage");
    const detailActions = $("#appointmentDetailActions");
    const updateDateInput = $("#aptUpdateDate");
    const updateTimeInput = $("#aptUpdateTime");
    const updateNoteInput = $("#aptUpdateNote");

    if (!listModal || !detailModal || !listContent) return;

    let activeAppointment = null;

    const statusLabels = {
        PENDING: "Chờ duyệt",
        APPROVED: "Đã duyệt",
        REJECTED: "Đã từ chối",
        COMPLETED: "Hoàn thành",
        ABSENT: "Không đến",
        CANCELLED: "Đã hủy"
    };

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

        const toastType = type === "error" || type === "danger" ? "error" : "success";
        const toast = createElement("div", `flash-message flash-${toastType}`);
        const icon = createElement("i", toastType === "success" ? "fa-solid fa-check" : "fa-solid fa-xmark");
        icon.setAttribute("aria-hidden", "true");
        toast.appendChild(icon);
        toast.appendChild(createElement("span", "", message));
        container.appendChild(toast);
        window.setTimeout(() => toast.remove(), 5000);
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

    const formatDateTime = (value) => {
        if (!value) return { date: "", dateValue: "", day: "--", monthYear: "", time: "" };

        const date = new Date(value);
        if (Number.isNaN(date.getTime())) {
            return { date: value, dateValue: "", day: "--", monthYear: "", time: "" };
        }

        const day = String(date.getDate()).padStart(2, "0");
        const month = String(date.getMonth() + 1).padStart(2, "0");
        const year = date.getFullYear();
        const hours = String(date.getHours()).padStart(2, "0");
        const minutes = String(date.getMinutes()).padStart(2, "0");

        return {
            date: `${day}/${month}/${year}`,
            dateValue: `${year}-${month}-${day}`,
            day,
            monthYear: `${month}/${year}`,
            time: `${hours}:${minutes}`
        };
    };

    const openModal = (modal) => {
        modal.hidden = false;
        body.classList.add("modal-open");
        requestAnimationFrame(() => modal.classList.add("is-open"));
        $(".apt-list-close", modal)?.focus();
    };

    const closeModal = (modal) => {
        if (!modal || modal.hidden) return;
        modal.classList.remove("is-open");
        window.setTimeout(() => {
            modal.hidden = true;
            if (listModal.hidden && detailModal.hidden) {
                body.classList.remove("modal-open");
            }
        }, 180);
    };

    const setListState = (state) => {
        listLoading.hidden = state !== "loading";
        listEmpty.hidden = state !== "empty";
        listContent.hidden = state !== "content";
    };

    const renderAppointmentCard = (appointment) => {
        const dateTime = formatDateTime(appointment.appointmentDate);
        const item = createElement("article", "apt-list-item");

        item.innerHTML = `
            <div class="apt-list-item-main">
                <div>
                    <span class="apt-status" data-status="${appointment.status || "PENDING"}">${statusLabels[appointment.status] || appointment.status || "Chờ duyệt"}</span>
                    <h3>Lịch thăm vườn Bonsai Luxury</h3>
                    <div class="apt-list-meta">
                        <span><i class="fa-regular fa-calendar" aria-hidden="true"></i> ${dateTime.date}</span>
                        <span><i class="fa-regular fa-clock" aria-hidden="true"></i> ${dateTime.time}</span>
                    </div>
                </div>
            </div>
            <div class="apt-detail-actions">
                <span class="apt-list-code">APT-${appointment.appointmentId}</span>
                <button type="button" class="apt-btn apt-btn-primary" data-view-appointment="${appointment.appointmentId}">Xem chi tiết</button>
            </div>
        `;

        return item;
    };

    const loadAppointments = async () => {
        setListState("loading");
        try {
            const response = await fetch("/appointments/list", { headers: { Accept: "application/json" } });
            if (response.status === 401 || response.redirected) {
                window.location.href = "/login";
                return;
            }
            if (!response.ok) throw new Error("Không thể tải danh sách lịch hẹn.");

            const appointments = await response.json();
            listContent.replaceChildren();

            if (!appointments.length) {
                setListState("empty");
                return;
            }

            appointments.forEach((appointment) => listContent.appendChild(renderAppointmentCard(appointment)));
            setListState("content");
        } catch (error) {
            setListState("empty");
            showToast(error.message, "error");
        }
    };

    const fillDetail = (appointment) => {
        activeAppointment = appointment;
        const dateTime = formatDateTime(appointment.appointmentDate);
        const isPending = appointment.status === "PENDING";

        $("#aptDetailCode").textContent = `APT-${appointment.appointmentId}`;
        $("#aptDetailStatus").textContent = statusLabels[appointment.status] || appointment.status || "Chờ duyệt";
        $("#aptDetailDate").textContent = dateTime.date;
        $("#aptDetailTime").textContent = dateTime.time;
        $("#aptDetailNote").textContent = appointment.note || "Không có ghi chú";

        showEditButton.hidden = !isPending;
        cancelAppointmentButton.hidden = !isPending;
        lockedMessage.hidden = isPending;
        detailActions.hidden = false;
        updateForm.hidden = true;

        const tomorrowValue = getTomorrowValue();
        updateDateInput.min = tomorrowValue;
        updateDateInput.value = dateTime.dateValue && dateTime.dateValue >= tomorrowValue ? dateTime.dateValue : tomorrowValue;
        updateTimeInput.value = dateTime.time && dateTime.time >= "08:00" && dateTime.time <= "17:00" ? dateTime.time : "09:00";
        updateNoteInput.value = appointment.note || "";

        updateForm.setAttribute("action", `/appointments/update/${appointment.appointmentId}`);
        cancelForm.setAttribute("action", `/appointments/cancel/${appointment.appointmentId}`);
    };

    listContent.addEventListener("click", async (event) => {
        const button = event.target.closest("[data-view-appointment]");
        if (!button) return;

        try {
            const response = await fetch(`/appointments/detail/${button.dataset.viewAppointment}`, { headers: { Accept: "application/json" } });
            if (!response.ok) throw new Error("Không thể tải chi tiết lịch hẹn.");
            fillDetail(await response.json());
            openModal(detailModal);
        } catch (error) {
            showToast(error.message, "error");
        }
    });

    $$(".js-appointment-list-trigger").forEach((button) => {
        button.addEventListener("click", async () => {
            if (body?.dataset.authenticated === "false") {
                window.location.href = "/login";
                return;
            }

            openModal(listModal);
            await loadAppointments();
        });
    });

    $$("[data-close-appointment-list]").forEach((element) => {
        element.addEventListener("click", () => closeModal(listModal));
    });

    $$("[data-close-appointment-detail]").forEach((element) => {
        element.addEventListener("click", () => closeModal(detailModal));
    });

    showEditButton?.addEventListener("click", () => {
        if (!activeAppointment || activeAppointment.status !== "PENDING") return;
        detailActions.hidden = true;
        updateForm.hidden = false;
    });

    cancelEditButton?.addEventListener("click", () => {
        updateForm.hidden = true;
        detailActions.hidden = false;
    });

    updateForm?.addEventListener("submit", async (event) => {
        event.preventDefault();
        const tomorrowValue = getTomorrowValue();
        if (!updateDateInput.value || updateDateInput.value < tomorrowValue) {
            showToast("Vui lòng chọn ngày xem từ ngày mai trở đi.", "error");
            updateDateInput.focus();
            return;
        }
        if (!updateTimeInput.value || updateTimeInput.value < "08:00" || updateTimeInput.value > "17:00") {
            showToast("Vui lòng chọn giờ xem trong khung 08:00 - 17:00.", "error");
            updateTimeInput.focus();
            return;
        }

        updateForm.submit();
    });

    cancelAppointmentButton?.addEventListener("click", async () => {
        if (!activeAppointment || activeAppointment.status !== "PENDING") {
            showToast("Lịch đã qua trạng thái chờ duyệt nên không thể hủy.", "error");
            return;
        }

        const confirmed = window.BSMSConfirm
            ? await window.BSMSConfirm({
                title: "Xác nhận hủy lịch",
                message: "Bạn có chắc chắn muốn hủy lịch hẹn này?",
                type: "danger",
                confirmText: "Xác nhận hủy",
                cancelText: "Quay lại"
            })
            : true;

        if (!confirmed) return;

        cancelForm.submit();
    });

    document.addEventListener("keydown", (event) => {
        if (event.key !== "Escape") return;
        closeModal(detailModal);
        closeModal(listModal);
    });
});
