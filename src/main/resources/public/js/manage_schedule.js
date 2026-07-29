﻿﻿document.addEventListener("DOMContentLoaded", () => {
    const state = {
        appointments: [],
        currentMonth: new Date().getMonth(),
        currentYear: new Date().getFullYear(),
        selectedDate: ""
    };

    const monthNames = [
        "Tháng 1", "Tháng 2", "Tháng 3", "Tháng 4", "Tháng 5", "Tháng 6",
        "Tháng 7", "Tháng 8", "Tháng 9", "Tháng 10", "Tháng 11", "Tháng 12"
    ];

    const statusLabels = {
        PENDING: "CHỜ DUYỆT",
        APPROVED: "ĐÃ DUYỆT",
        REJECTED: "ĐÃ TỪ CHỐI",
        CANCELLED: "ĐÃ HỦY",
        COMPLETED: "HOÀN THÀNH"
    };

    const elements = {
        statTotal: document.getElementById("statTotal"),
        statToday: document.getElementById("statToday"),
        statApproved: document.getElementById("statApproved"),
        statPending: document.getElementById("statPending"),
        totalPercent: document.getElementById("totalPercent"),
        approvedPercent: document.getElementById("approvedPercent"),
        pendingPercent: document.getElementById("pendingPercent"),
        reminderCount: document.getElementById("reminderCount"),
        pendingReminders: document.getElementById("pendingReminders"),
        currentMonth: document.querySelector(".current-month"),
        prevMonth: document.getElementById("prevMonth"),
        nextMonth: document.getElementById("nextMonth"),
        calendarGrid: document.querySelector(".calendar-grid"),
        todayAppointments: document.getElementById("todayAppointments"),
        todayPanelTitle: document.getElementById("todayPanelTitle"),
        appointmentListCount: document.getElementById("appointmentListCount"),
        appointmentRows: document.getElementById("appointmentRows"),
        appointmentData: document.getElementById("appointmentData"),
        detailModal: document.getElementById("editModal"),
        closeModal: document.getElementById("closeModal"),
        closeModalFooter: document.getElementById("closeModalFooter"),
        infoId: document.getElementById("infoId"),
        infoClient: document.getElementById("infoClient"),
        infoAppointmentType: document.getElementById("infoAppointmentType"),
        infoStatus: document.getElementById("infoStatus"),
        infoDate: document.getElementById("infoDate"),
        infoTime: document.getElementById("infoTime"),
        infoPhone: document.getElementById("infoPhone"),
        infoEmail: document.getElementById("infoEmail"),
        infoNote: document.getElementById("infoNote"),
        notificationBtn: document.getElementById("notificationBtn"),
        notificationPopup: document.getElementById("notificationPopup"),
        settingForm: document.querySelector(".sch-setting-form"),
        pauseFromInput: document.getElementById("pauseFromInput"),
        pauseFromDate: document.getElementById("pauseFromDate"),
        pauseFromTime: document.getElementById("pauseFromTime"),
        pauseToInput: document.getElementById("pauseToInput"),
        pauseToDate: document.getElementById("pauseToDate"),
        pauseToTime: document.getElementById("pauseToTime"),
        pauseReasonInput: document.getElementById("pauseReasonInput"),
        pauseReasonCount: document.getElementById("pauseReasonCount")
    };

    function padZero(value) {
        return value < 10 ? `0${value}` : String(value);
    }

    function getLocalDateString(date = new Date()) {
        return `${date.getFullYear()}-${padZero(date.getMonth() + 1)}-${padZero(date.getDate())}`;
    }

    function formatDateDisplay(dateString) {
        if (!dateString) return "";
        const [year, month, day] = dateString.split("-");
        return `${day}/${month}/${year}`;
    }

    function parseDateInput(value) {
        if (!value) return "";

        const normalizedValue = value.trim();
        const isoDateMatch = normalizedValue.match(/^(\d{4})-(\d{2})-(\d{2})/);
        if (isoDateMatch) {
            return `${isoDateMatch[1]}-${isoDateMatch[2]}-${isoDateMatch[3]}`;
        }

        const displayDateMatch = normalizedValue.match(/^(\d{2})\/(\d{2})\/(\d{4})/);
        if (displayDateMatch) {
            return `${displayDateMatch[3]}-${displayDateMatch[2]}-${displayDateMatch[1]}`;
        }

        const parsedDate = new Date(normalizedValue);
        return Number.isNaN(parsedDate.getTime()) ? "" : getLocalDateString(parsedDate);
    }

    function parseAppointmentDateTime(appointment) {
        const dateTimeValue = appointment.appointmentAt || `${appointment.date}T${appointment.time}`;
        const dateTimeMatch = dateTimeValue.trim().match(/^(\d{4})-(\d{2})-(\d{2})(?:T|\s)(\d{2}):(\d{2})/);
        const parsedDate = dateTimeMatch
                ? new Date(
                        Number(dateTimeMatch[1]),
                        Number(dateTimeMatch[2]) - 1,
                        Number(dateTimeMatch[3]),
                        Number(dateTimeMatch[4]),
                        Number(dateTimeMatch[5])
                )
                : new Date(dateTimeValue);
        return Number.isNaN(parsedDate.getTime()) ? null : parsedDate;
    }

    function parseAppointmentProcessingDeadline(appointment) {
        if (!appointment.createdAt) return null;
        const createdAt = new Date(appointment.createdAt);
        if (Number.isNaN(createdAt.getTime())) return null;
        return new Date(createdAt.getTime() + 5 * 60000);
    }

    function getMinutesUntilProcessingDeadline(appointment) {
        const processingDeadline = parseAppointmentProcessingDeadline(appointment);
        return processingDeadline ? Math.ceil((processingDeadline.getTime() - Date.now()) / 60000) : null;
    }

    function getReminderLevel(minutesUntil) {
        if (minutesUntil === null) {
            return { className: "neutral", label: "Cần kiểm tra", priority: 5 };
        }
        if (minutesUntil <= 0) {
            return { className: "urgent", label: "Đến mốc auto", priority: 1 };
        }
        if (minutesUntil <= 30) {
            return { className: "urgent", label: "Sắp auto", priority: 1 };
        }
        if (minutesUntil <= 120) {
            return { className: "soon", label: "Gần mốc auto", priority: 2 };
        }
        if (minutesUntil <= 480) {
            return { className: "high", label: "Sắp tới mốc", priority: 3 };
        }
        if (minutesUntil <= 1440) {
            return { className: "today", label: "Trong ngày", priority: 4 };
        }
        return { className: "neutral", label: "Chờ duyệt", priority: 5 };
    }

    function formatTimeUntil(minutesUntil) {
        if (minutesUntil === null) return "Chưa xác định mốc auto";
        if (minutesUntil <= 0) return "Đã tới mốc auto";
        if (minutesUntil < 60) return `Còn ${minutesUntil} phút`;

        const hours = Math.floor(minutesUntil / 60);
        const minutes = minutesUntil % 60;
        if (minutesUntil < 1440) {
            return minutes > 0 ? `Còn ${hours} giờ ${minutes} phút` : `Còn ${hours} giờ`;
        }

        const days = Math.floor(hours / 24);
        const remainingHours = hours % 24;
        return remainingHours > 0 ? `Còn ${days} ngày ${remainingHours} giờ` : `Còn ${days} ngày`;
    }

    function formatAppointmentLabel(appointment) {
        return `${formatDateDisplay(appointment.date)} lúc ${appointment.time}`;
    }

    function getReminderActionLabel(reminderLevel) {
        return reminderLevel.priority <= 2 ? "Kiểm tra" : "Xem chi tiết";
    }

    function getReminderNote(reminderLevel) {
        if (reminderLevel.className === "urgent") {
            return "Hệ thống tự duyệt hoặc từ chối theo lịch bận.";
        }
        return "Auto xử lý sau 5 phút kể từ khi khách đặt lịch.";
    }

    function updatePauseReasonCount() {
        if (!elements.pauseReasonInput || !elements.pauseReasonCount) return;
        elements.pauseReasonCount.textContent = `${elements.pauseReasonInput.value.length}/500`;
    }

    function splitDateTimeLocal(value) {
        const dateTimeMatch = value ? value.match(/^(\d{4}-\d{2}-\d{2})T(\d{2}:\d{2})/) : null;
        return dateTimeMatch ? { date: dateTimeMatch[1], time: dateTimeMatch[2] } : { date: "", time: "" };
    }

    function syncPausePickerFromHidden(hiddenInput, dateInput, timeSelect) {
        if (!hiddenInput || !dateInput || !timeSelect) return;
        const dateTime = splitDateTimeLocal(hiddenInput.value);
        if (!dateInput.value) dateInput.value = dateTime.date;
        if (!timeSelect.value) timeSelect.value = dateTime.time;
    }

    function syncPauseHiddenInput(hiddenInput, dateInput, timeSelect) {
        if (!hiddenInput || !dateInput || !timeSelect) return;
        hiddenInput.value = dateInput.value && timeSelect.value ? `${dateInput.value}T${timeSelect.value}` : "";
    }

    function syncPauseHiddenInputs() {
        syncPauseHiddenInput(elements.pauseFromInput, elements.pauseFromDate, elements.pauseFromTime);
        syncPauseHiddenInput(elements.pauseToInput, elements.pauseToDate, elements.pauseToTime);
    }

    function clearPausePickerValidity() {
        [
            elements.pauseFromDate,
            elements.pauseFromTime,
            elements.pauseToDate,
            elements.pauseToTime
        ].forEach(input => input?.setCustomValidity(""));
    }

    function updatePauseInputBounds() {
        const minDate = getLocalDateString();
        [elements.pauseFromDate, elements.pauseToDate].forEach(input => {
            if (!input) return;
            input.min = minDate;
        });
    }

    function isBusinessHourDateTime(value) {
        if (!value) return true;
        const dateTime = new Date(value);
        if (Number.isNaN(dateTime.getTime())) return false;

        const hours = dateTime.getHours();
        const minutes = dateTime.getMinutes();
        return (hours > 8 || (hours === 8 && minutes >= 0))
                && (hours < 17 || (hours === 17 && minutes === 0));
    }

    function validatePauseSettingForm(event) {
        updatePauseInputBounds();
        syncPauseHiddenInputs();
        clearPausePickerValidity();

        const pausePickers = [
            {
                hiddenInput: elements.pauseFromInput,
                dateInput: elements.pauseFromDate,
                timeSelect: elements.pauseFromTime
            },
            {
                hiddenInput: elements.pauseToInput,
                dateInput: elements.pauseToDate,
                timeSelect: elements.pauseToTime
            }
        ].filter(picker => picker.hiddenInput && picker.dateInput && picker.timeSelect);

        for (const picker of pausePickers) {
            const hasDate = Boolean(picker.dateInput.value);
            const hasTime = Boolean(picker.timeSelect.value);
            if (!hasDate && !hasTime) continue;
            if (hasDate !== hasTime) {
                const invalidInput = hasDate ? picker.timeSelect : picker.dateInput;
                invalidInput.setCustomValidity("Vui lòng chọn đủ ngày và giờ.");
                invalidInput.reportValidity();
                event.preventDefault();
                return;
            }
            if (new Date(picker.hiddenInput.value).getTime() < Date.now()) {
                picker.dateInput.setCustomValidity("Chỉ chọn thời gian từ hiện tại hoặc tương lai.");
                picker.dateInput.reportValidity();
                event.preventDefault();
                return;
            }
            if (!isBusinessHourDateTime(picker.hiddenInput.value)) {
                picker.timeSelect.setCustomValidity("Chỉ chọn giờ hành chính từ 08:00 đến 17:00.");
                picker.timeSelect.reportValidity();
                event.preventDefault();
                return;
            }
        }
    }

    function createElement(tagName, className, text) {
        const element = document.createElement(tagName);
        if (className) element.className = className;
        if (text !== undefined) element.textContent = text;
        return element;
    }

    function createStatusBadge(status) {
        return createElement("span", `status-badge ${status.toLowerCase()}`, statusLabels[status] || status);
    }

    function extractAppointmentsFromDOM() {
        state.appointments = [];
        elements.appointmentData.querySelectorAll(".appointment-data-row").forEach(row => {
            const dateText = row.dataset.date || row.dataset.appointmentAt || "";
            const status = row.dataset.status ? row.dataset.status.trim().toUpperCase() : "PENDING";

            state.appointments.push({
                id: row.dataset.id || "",
                phone: row.dataset.phone || "",
                email: row.dataset.email || "",
                note: row.dataset.note || "",
                appointmentAt: row.dataset.appointmentAt || "",
                createdAt: row.dataset.createdAt || "",
                client: row.dataset.client || "",
                appointmentType: row.dataset.appointmentType || "",
                date: parseDateInput(dateText),
                time: row.dataset.time || "",
                status
            });
        });
    }

    function renderStats() {
        const total = state.appointments.length;
        const todayString = getLocalDateString();
        const today = state.appointments.filter(appointment => appointment.date === todayString).length;
        const approved = state.appointments.filter(appointment => appointment.status === "APPROVED").length;
        const pending = state.appointments.filter(appointment => appointment.status === "PENDING").length;

        elements.statTotal.textContent = total;
        elements.statToday.textContent = today;
        elements.statApproved.textContent = approved;
        elements.statPending.textContent = pending;
        elements.totalPercent.textContent = total > 0 ? `+${total}` : "0";
        elements.approvedPercent.textContent = approved;
        elements.pendingPercent.textContent = pending;
    }

    function renderCalendar() {
        elements.currentMonth.textContent = `${monthNames[state.currentMonth]} ${state.currentYear}`;
        elements.calendarGrid.innerHTML = "";

        const firstDay = new Date(state.currentYear, state.currentMonth, 1);
        let startDayIndex = firstDay.getDay() - 1;
        if (startDayIndex < 0) startDayIndex = 6;

        const totalDays = new Date(state.currentYear, state.currentMonth + 1, 0).getDate();
        for (let index = 0; index < startDayIndex; index += 1) {
            elements.calendarGrid.appendChild(createElement("div", "calendar-day empty"));
        }

        const appointmentDates = new Set(state.appointments
                .map(appointment => appointment.date)
                .filter(Boolean));

        for (let day = 1; day <= totalDays; day += 1) {
            const dayButton = createElement("button", "calendar-day current-month-day", String(day));
            const dateString = `${state.currentYear}-${padZero(state.currentMonth + 1)}-${padZero(day)}`;
            dayButton.type = "button";
            dayButton.dataset.date = dateString;
            dayButton.setAttribute("aria-label", `Xem lịch hẹn ngày ${formatDateDisplay(dateString)}`);

            if (dateString === getLocalDateString()) dayButton.classList.add("today");
            if (dateString === state.selectedDate) dayButton.classList.add("selected");
            if (appointmentDates.has(dateString)) {
                dayButton.classList.add("has-appointment");
                dayButton.appendChild(createElement("span", "appointment-dot"));
            }

            dayButton.addEventListener("click", () => selectCalendarDate(dateString));
            elements.calendarGrid.appendChild(dayButton);
        }
    }

    function selectCalendarDate(dateString) {
        state.selectedDate = dateString;
        renderCalendar();
        renderDayPanel();
    }

    function renderDayPanel() {
        elements.todayAppointments.innerHTML = "";
        const targetDate = state.selectedDate || getLocalDateString();
        const dayAppointments = state.appointments
                .filter(appointment => appointment.date === targetDate)
                .sort((first, second) => first.time.localeCompare(second.time));
        elements.todayPanelTitle.textContent = `Lịch hẹn ngày ${formatDateDisplay(targetDate)}`;

        if (dayAppointments.length === 0) {
            elements.todayAppointments.appendChild(createElement("li", "no-appointments", "Không có lịch hẹn."));
            return;
        }

        dayAppointments.forEach(appointment => {
            const item = createElement("li", "appointment-item");
            const left = createElement("div", "item-left");
            const right = createElement("div", "item-right");
            const time = createElement("span", "item-time");

            item.dataset.id = appointment.id;
            item.setAttribute("role", "button");
            item.tabIndex = 0;
            left.appendChild(createElement("span", "item-title", appointment.client || "Khách hàng"));
            left.appendChild(createElement("span", "item-subtitle", `${appointment.appointmentType} (${appointment.id})`));
            time.appendChild(createElement("i", "fa-regular fa-clock"));
            time.append(` ${appointment.time}`);
            right.appendChild(time);
            right.appendChild(createStatusBadge(appointment.status));
            item.append(left, right);
            elements.todayAppointments.appendChild(item);
        });
    }

    function renderAppointmentList() {
        if (!elements.appointmentRows) return;

        const sortedAppointments = [...state.appointments].sort((first, second) => {
            const firstDate = parseAppointmentDateTime(first);
            const secondDate = parseAppointmentDateTime(second);
            const firstTime = firstDate ? firstDate.getTime() : 0;
            const secondTime = secondDate ? secondDate.getTime() : 0;
            return secondTime - firstTime;
        });

        if (elements.appointmentListCount) {
            elements.appointmentListCount.textContent = `${sortedAppointments.length} lịch`;
        }

        elements.appointmentRows.innerHTML = "";

        if (sortedAppointments.length === 0) {
            const emptyRow = document.createElement("tr");
            const emptyCell = createElement("td", "sch-empty-table", "Chưa có lịch tham quan vườn.");
            emptyCell.colSpan = 5;
            emptyRow.appendChild(emptyCell);
            elements.appointmentRows.appendChild(emptyRow);
            return;
        }

        sortedAppointments.forEach(appointment => {
            const row = document.createElement("tr");
            const contact = appointment.phone || appointment.email || "-";

            row.dataset.id = appointment.id;
            row.tabIndex = 0;
            row.setAttribute("role", "button");
            row.setAttribute("aria-label", `Xem chi tiết lịch ${appointment.id}`);
            row.append(
                    createElement("td", "", `#${appointment.id}`),
                    createElement("td", "", appointment.client || "Khách hàng"),
                    createElement("td", "", `${formatDateDisplay(appointment.date)} ${appointment.time}`),
                    createElement("td", "", contact)
            );

            const statusCell = document.createElement("td");
            statusCell.appendChild(createStatusBadge(appointment.status));
            row.appendChild(statusCell);
            elements.appointmentRows.appendChild(row);
        });
    }

    function renderReminderPanel() {
        if (!elements.pendingReminders || !elements.reminderCount) return;

        const pendingAppointments = state.appointments
                .filter(appointment => appointment.status === "PENDING")
                .map(appointment => ({
                    ...appointment,
                    minutesUntil: getMinutesUntilProcessingDeadline(appointment)
                }))
                .sort((first, second) => {
                    const firstLevel = getReminderLevel(first.minutesUntil);
                    const secondLevel = getReminderLevel(second.minutesUntil);
                    if (firstLevel.priority !== secondLevel.priority) return firstLevel.priority - secondLevel.priority;
                    if (first.minutesUntil === null) return 1;
                    if (second.minutesUntil === null) return -1;
                    return first.minutesUntil - second.minutesUntil;
                });

        elements.reminderCount.textContent = `${pendingAppointments.length} lịch chờ`;
        elements.pendingReminders.innerHTML = "";

        if (pendingAppointments.length === 0) {
            elements.pendingReminders.appendChild(createElement("li", "sch-reminder-empty", "Không có lịch chờ xử lý."));
            return;
        }

        pendingAppointments.slice(0, 4).forEach((appointment, index) => {
            const reminderLevel = getReminderLevel(appointment.minutesUntil);
            const item = createElement("li", `sch-reminder-item ${reminderLevel.className}`);
            const button = createElement("button", "sch-reminder-btn");
            const priority = createElement("span", "sch-reminder-priority", `#${index + 1}`);
            const content = createElement("div", "sch-reminder-content");
            const title = createElement("strong", "", appointment.client || "Khách hàng");
            const due = createElement("span", "sch-reminder-due", formatTimeUntil(appointment.minutesUntil));
            const meta = createElement("span", "sch-reminder-meta", `${appointment.appointmentType} • Hẹn ${formatAppointmentLabel(appointment)} • Auto sau 5 phút đặt lịch`);
            const note = createElement("small", "sch-reminder-note", getReminderNote(reminderLevel));
            const badge = createElement("span", `sch-reminder-badge ${reminderLevel.className}`, reminderLevel.label);
            const action = createElement("span", "sch-reminder-action", getReminderActionLabel(reminderLevel));

            button.type = "button";
            button.dataset.id = appointment.id;
            content.append(title, due, meta, note);
            button.append(priority, content, badge, action);
            item.appendChild(button);
            elements.pendingReminders.appendChild(item);
        });
    }

    function openDetailModal(id) {
        const appointment = state.appointments.find(item => item.id === String(id));
        if (!appointment) return;

        elements.infoId.textContent = appointment.id;
        elements.infoClient.textContent = appointment.client || "-";
        elements.infoAppointmentType.textContent = appointment.appointmentType || "-";
        elements.infoStatus.textContent = statusLabels[appointment.status] || appointment.status || "-";
        elements.infoDate.textContent = formatDateDisplay(appointment.date);
        elements.infoTime.textContent = appointment.time || "-";
        elements.infoPhone.textContent = appointment.phone || "-";
        elements.infoEmail.textContent = appointment.email || "-";
        elements.infoNote.textContent = appointment.note || "-";
        elements.detailModal.classList.add("show");
    }

    function closeDetailModal() {
        elements.detailModal.classList.remove("show");
    }

    function bindEvents() {
        elements.prevMonth.addEventListener("click", () => {
            state.currentMonth -= 1;
            if (state.currentMonth < 0) {
                state.currentMonth = 11;
                state.currentYear -= 1;
            }
            renderCalendar();
        });

        elements.nextMonth.addEventListener("click", () => {
            state.currentMonth += 1;
            if (state.currentMonth > 11) {
                state.currentMonth = 0;
                state.currentYear += 1;
            }
            renderCalendar();
        });

        elements.todayAppointments.addEventListener("click", event => {
            const item = event.target.closest(".appointment-item");
            if (item) openDetailModal(item.dataset.id);
        });

        elements.todayAppointments.addEventListener("keydown", event => {
            if (event.key !== "Enter" && event.key !== " ") return;
            const item = event.target.closest(".appointment-item");
            if (!item) return;
            event.preventDefault();
            openDetailModal(item.dataset.id);
        });

        elements.pendingReminders?.addEventListener("click", event => {
            const button = event.target.closest(".sch-reminder-btn");
            if (button) openDetailModal(button.dataset.id);
        });

        elements.appointmentRows?.addEventListener("click", event => {
            const row = event.target.closest("tr[data-id]");
            if (row) openDetailModal(row.dataset.id);
        });

        elements.appointmentRows?.addEventListener("keydown", event => {
            if (event.key !== "Enter" && event.key !== " ") return;
            const row = event.target.closest("tr[data-id]");
            if (!row) return;
            event.preventDefault();
            openDetailModal(row.dataset.id);
        });

        elements.closeModal?.addEventListener("click", closeDetailModal);
        elements.closeModalFooter?.addEventListener("click", closeDetailModal);
        elements.detailModal?.addEventListener("click", event => {
            if (event.target === elements.detailModal) closeDetailModal();
        });

        document.addEventListener("keydown", event => {
            if (event.key === "Escape" && elements.detailModal.classList.contains("show")) {
                closeDetailModal();
            }
        });

        if (elements.notificationBtn && elements.notificationPopup) {
            elements.notificationBtn.addEventListener("click", event => {
                event.stopPropagation();
                elements.notificationPopup.classList.toggle("show");
            });
            document.addEventListener("click", event => {
                if (!elements.notificationPopup.contains(event.target) && event.target !== elements.notificationBtn) {
                    elements.notificationPopup.classList.remove("show");
                }
            });
        }

        elements.pauseReasonInput?.addEventListener("input", updatePauseReasonCount);
        elements.settingForm?.addEventListener("submit", validatePauseSettingForm);
        [
            elements.pauseFromDate,
            elements.pauseFromTime,
            elements.pauseToDate,
            elements.pauseToTime
        ].forEach(input => {
            const syncPicker = () => {
                clearPausePickerValidity();
                syncPauseHiddenInputs();
            };
            input?.addEventListener("input", syncPicker);
            input?.addEventListener("change", syncPicker);
        });
    }

    function renderAll() {
        renderStats();
        renderReminderPanel();
        renderCalendar();
        renderDayPanel();
        renderAppointmentList();
    }

    function init() {
        extractAppointmentsFromDOM();
        state.selectedDate = getLocalDateString();
        syncPausePickerFromHidden(elements.pauseFromInput, elements.pauseFromDate, elements.pauseFromTime);
        syncPausePickerFromHidden(elements.pauseToInput, elements.pauseToDate, elements.pauseToTime);
        syncPauseHiddenInputs();
        updatePauseReasonCount();
        updatePauseInputBounds();
        bindEvents();
        renderAll();
        setInterval(renderAll, 60000);
    }

    init();
});
