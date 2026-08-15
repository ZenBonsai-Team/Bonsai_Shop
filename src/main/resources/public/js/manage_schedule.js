document.addEventListener("DOMContentLoaded", () => {
    const state = {
        appointments: [],
        pendingAppointments: [],
        currentMonth: new Date().getMonth(),
        currentYear: new Date().getFullYear(),
        selectedDate: "",
        requestToken: 0,
        appointmentsByDate: new Map(),
        appointmentDatesByMonth: new Map(),
        loadedAppointmentDateMonths: new Set(),
        loadingAppointmentDateMonths: new Set()
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
        COMPLETED: "HOÀN THÀNH",
        ABSENT: "KHÁCH VẮNG"
    };

    const elements = {
        reminderCount: document.getElementById("reminderCount"),
        pendingReminders: document.getElementById("pendingReminders"),
        currentMonth: document.querySelector(".current-month"),
        prevMonth: document.getElementById("prevMonth"),
        nextMonth: document.getElementById("nextMonth"),
        calendarGrid: document.querySelector(".calendar-grid"),
        todayAppointments: document.getElementById("todayAppointments"),
        todayPanelTitle: document.getElementById("todayPanelTitle"),
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
        approveAppointmentForm: document.getElementById("approveAppointmentForm"),
        rejectAppointmentForm: document.getElementById("rejectAppointmentForm"),
        completeAppointmentForm: document.getElementById("completeAppointmentForm"),
        noShowAppointmentForm: document.getElementById("noShowAppointmentForm"),
        notificationBtn: document.getElementById("notificationBtn"),
        notificationPopup: document.getElementById("notificationPopup"),
        appointmentSettingForm: document.getElementById("appointmentSettingForm"),
        autoApprove: document.getElementById("autoApprove"),
        autoApproveAfter: document.getElementById("autoApproveAfter"),
        autoComplete: document.getElementById("autoComplete"),
        autoCompleteAfter: document.getElementById("autoCompleteAfter"),
        pauseFrom: document.getElementById("pauseFrom"),
        pauseFromDate: document.getElementById("pauseFromDate"),
        pauseFromTime: document.getElementById("pauseFromTime"),
        pauseTo: document.getElementById("pauseTo"),
        pauseToDate: document.getElementById("pauseToDate"),
        pauseToTime: document.getElementById("pauseToTime"),
        pauseReason: document.getElementById("pauseReason"),
        pauseReasonCount: document.getElementById("pauseReasonCount"),
        clearPauseSetting: document.getElementById("clearPauseSetting")
    };

    function padZero(value) {
        return value < 10 ? `0${value}` : String(value);
    }

    function getLocalDateString(date = new Date()) {
        return `${date.getFullYear()}-${padZero(date.getMonth() + 1)}-${padZero(date.getDate())}`;
    }

    function getSelectedDateFromPage() {
        const selectedDate = elements.calendarGrid?.dataset.selectedDate || "";
        if (selectedDate) return selectedDate;

        const queryDate = new URLSearchParams(window.location.search).get("date");
        return parseDateInput(queryDate || "") || getLocalDateString();
    }

    function updateDateParam(dateString) {
        const url = new URL(window.location.href);
        url.pathname = "/artisan/appointments";
        url.searchParams.set("date", dateString);
        url.searchParams.delete("id");
        window.history.replaceState({}, "", url.toString());
    }

    function getMonthKey(year, monthIndex) {
        return `${year}-${padZero(monthIndex + 1)}`;
    }

    function getDateMonthKey(dateString) {
        const parsedDate = parseDateInput(dateString);
        return parsedDate ? parsedDate.substring(0, 7) : "";
    }

    function rememberAppointmentDate(dateString) {
        const parsedDate = parseDateInput(dateString);
        const monthKey = getDateMonthKey(parsedDate);
        if (!parsedDate || !monthKey) return;

        if (!state.appointmentDatesByMonth.has(monthKey)) {
            state.appointmentDatesByMonth.set(monthKey, new Set());
        }

        state.appointmentDatesByMonth.get(monthKey).add(parsedDate);
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

    function getAutoApproveAfterMinutes() {
        if (elements.autoApprove && !elements.autoApprove.checked) return null;

        const configuredMinutes = Number(elements.autoApproveAfter?.value || elements.appointmentSettingForm?.dataset.autoApproveAfter || 5);
        return Number.isFinite(configuredMinutes) && configuredMinutes > 0 ? configuredMinutes : 5;
    }

    function parseAppointmentProcessingDeadline(appointment) {
        const autoApproveAfterMinutes = getAutoApproveAfterMinutes();
        if (autoApproveAfterMinutes === null) return null;
        if (!appointment.createdAt) return null;
        const createdAt = new Date(appointment.createdAt);
        if (Number.isNaN(createdAt.getTime())) return null;
        return new Date(createdAt.getTime() + autoApproveAfterMinutes * 60000);
    }

    function getMinutesUntilProcessingDeadline(appointment) {
        const processingDeadline = parseAppointmentProcessingDeadline(appointment);
        return processingDeadline ? Math.ceil((processingDeadline.getTime() - Date.now()) / 60000) : null;
    }

    function getReminderLevel(minutesUntil) {
        if (minutesUntil === null) {
            return { className: "neutral", label: "C\u1ea7n ki\u1ec3m tra", priority: 5 };
        }
        if (minutesUntil <= 0) {
            return { className: "urgent", label: "\u0110\u1ebfn m\u1ed1c auto", priority: 1 };
        }
        if (minutesUntil <= 30) {
            return { className: "urgent", label: "S\u1eafp auto", priority: 1 };
        }
        if (minutesUntil <= 120) {
            return { className: "soon", label: "G\u1ea7n m\u1ed1c auto", priority: 2 };
        }
        if (minutesUntil <= 480) {
            return { className: "high", label: "S\u1eafp t\u1edbi m\u1ed1c", priority: 3 };
        }
        if (minutesUntil <= 1440) {
            return { className: "today", label: "Trong ngày", priority: 4 };
        }
        return { className: "neutral", label: "Ch\u1edd duy\u1ec7t", priority: 5 };
    }

    function formatTimeUntil(minutesUntil) {
        if (minutesUntil === null) return "Ch\u01b0a x\u00e1c \u0111\u1ecbnh m\u1ed1c auto";
        if (minutesUntil <= 0) return "\u0110\u00e3 t\u1edbi m\u1ed1c auto";
        if (minutesUntil < 60) return `C\u00f2n ${minutesUntil} ph\u00fat`;

        const hours = Math.floor(minutesUntil / 60);
        const minutes = minutesUntil % 60;
        if (minutesUntil < 1440) {
            return minutes > 0 ? `C\u00f2n ${hours} gi\u1edd ${minutes} ph\u00fat` : `C\u00f2n ${hours} gi\u1edd`;
        }

        const days = Math.floor(hours / 24);
        const remainingHours = hours % 24;
        return remainingHours > 0 ? `C\u00f2n ${days} ng\u00e0y ${remainingHours} gi\u1edd` : `C\u00f2n ${days} ng\u00e0y`;
    }

    function formatAppointmentLabel(appointment) {
        return `${formatDateDisplay(appointment.date)} lúc ${appointment.time}`;
    }

    function getReminderNote(reminderLevel) {
        if (getAutoApproveAfterMinutes() === null) {
            return "Tự động duyệt đang tắt. Cần xử lý lịch hẹn thủ công.";
        }

        if (reminderLevel.className === "urgent") {
            return "H\u1ec7 th\u1ed1ng t\u1ef1 duy\u1ec7t ho\u1eb7c t\u1eeb ch\u1ed1i theo l\u1ecbch b\u1eadn.";
        }

        return `Auto xử lý sau ${getAutoApproveAfterMinutes()} phút kể từ khi khách đặt lịch.`;
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

    function mapAppointmentRow(row) {
        const dateText = row.dataset.date || row.dataset.appointmentAt || "";
        const status = row.dataset.status ? row.dataset.status.trim().toUpperCase() : "PENDING";

        return {
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
        };
    }

    function mapAppointmentDto(appointment) {
        const appointmentAt = appointment.appointmentDate || "";
        const status = appointment.status ? appointment.status.trim().toUpperCase() : "PENDING";

        return {
            id: appointment.appointmentId || "",
            phone: appointment.customerPhone || "",
            email: appointment.customerEmail || "",
            note: appointment.note || "",
            appointmentAt,
            createdAt: appointment.createdAt || "",
            client: appointment.customerName || "",
            appointmentType: "L\u1ecbch tham quan v\u01b0\u1eddn",
            date: parseDateInput(appointmentAt),
            time: appointmentAt.substring(11, 16) || "",
            status
        };
    }

    function extractAppointmentsFromDOM() {
        state.appointments = [];
        elements.appointmentData.querySelectorAll(".appointment-data-row").forEach(row => {
            state.appointments.push(mapAppointmentRow(row));
        });
        if (state.selectedDate) {
            state.appointmentsByDate.set(state.selectedDate, state.appointments);
        }
        state.appointments.forEach(appointment => rememberAppointmentDate(appointment.date));
        syncPendingAppointmentsFromCache();
    }

    function syncPendingAppointmentsFromCache() {
        const pendingAppointmentsById = new Map();

        state.appointmentsByDate.forEach(appointments => {
            appointments.forEach(appointment => {
                if (appointment.status === "PENDING") {
                    pendingAppointmentsById.set(String(appointment.id), appointment);
                }
            });
        });

        state.pendingAppointments = Array.from(pendingAppointmentsById.values());
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

        state.appointments.forEach(appointment => rememberAppointmentDate(appointment.date));
        const monthKey = getMonthKey(state.currentYear, state.currentMonth);
        const appointmentDates = state.appointmentDatesByMonth.get(monthKey) || new Set();

        for (let day = 1; day <= totalDays; day += 1) {
            const dayButton = createElement("button", "calendar-day current-month-day", String(day));
            const dateString = `${state.currentYear}-${padZero(state.currentMonth + 1)}-${padZero(day)}`;
            dayButton.type = "button";
            dayButton.dataset.date = dateString;
            dayButton.setAttribute("aria-label", `Xem l\u1ecbch h\u1eb9n ng\u00e0y ${formatDateDisplay(dateString)}`);

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
        loadAppointmentsForDate(dateString);
    }

    async function fetchAppointmentsForDate(dateString) {
        const url = new URL("/artisan/appointments/data", window.location.origin);
        url.searchParams.set("date", dateString);

        const response = await fetch(url.toString(), {
            headers: { Accept: "application/json" }
        });

        if (!response.ok) {
            throw new Error("Kh\u00f4ng t\u1ea3i \u0111\u01b0\u1ee3c l\u1ecbch h\u1eb9n cho ng\u00e0y \u0111\u00e3 ch\u1ecdn.");
        }

        return (await response.json()).map(mapAppointmentDto);
    }

    async function loadAppointmentsForDate(dateString) {
        if (!dateString || dateString === state.selectedDate) return;

        if (state.appointmentsByDate.has(dateString)) {
            state.selectedDate = dateString;
            state.appointments = state.appointmentsByDate.get(dateString);
            state.appointments.forEach(appointment => rememberAppointmentDate(appointment.date));
            syncPendingAppointmentsFromCache();
            updateDateParam(dateString);
            renderAll();
            return;
        }

        const requestToken = state.requestToken + 1;
        state.requestToken = requestToken;
        state.selectedDate = dateString;
        updateDateParam(dateString);
        renderCalendar();
        renderDayLoading(dateString);

        try {
            const appointments = await fetchAppointmentsForDate(dateString);
            if (requestToken !== state.requestToken) return;

            state.appointments = appointments;
            state.appointmentsByDate.set(dateString, state.appointments);
            state.appointments.forEach(appointment => rememberAppointmentDate(appointment.date));
            syncPendingAppointmentsFromCache();
            renderAll();
        } catch (error) {
            alert(error.message);
        }
    }

    function getChangedAppointments(previousAppointments, nextAppointments) {
        const previousStatusById = new Map(
                previousAppointments.map(appointment => [String(appointment.id), appointment.status])
        );

        return nextAppointments.filter(appointment => {
            const previousStatus = previousStatusById.get(String(appointment.id));
            return previousStatus && previousStatus !== appointment.status;
        });
    }

    function notifyAppointmentStatusChanges(changedAppointments) {
        if (!changedAppointments.length || !window.BSMSToast) return;

        changedAppointments.forEach(appointment => {
            BSMSToast.info(
                    `Lịch #${appointment.id} đã chuyển sang ${statusLabels[appointment.status] || appointment.status}.`
            );
        });
    }

    async function refreshSelectedDateAppointments() {
        const dateString = state.selectedDate || getLocalDateString();
        if (!dateString) return;

        try {
            const previousAppointments = state.appointments;
            const nextAppointments = await fetchAppointmentsForDate(dateString);
            const changedAppointments = getChangedAppointments(previousAppointments, nextAppointments);

            state.appointments = nextAppointments;
            state.appointmentsByDate.set(dateString, nextAppointments);
            nextAppointments.forEach(appointment => rememberAppointmentDate(appointment.date));
            syncPendingAppointmentsFromCache();

            renderAll();
            notifyAppointmentStatusChanges(changedAppointments);
        } catch (error) {
            console.warn("Kh\u00f4ng th\u1ec3 t\u1ef1 t\u1ea3i l\u1ea1i l\u1ecbch h\u1eb9n.", error);
        }
    }

    async function refreshPendingAppointmentDates() {
        const pendingDates = Array.from(new Set(
                state.pendingAppointments
                        .map(appointment => appointment.date)
                        .filter(Boolean)
        ));

        if (pendingDates.length === 0) {
            syncPendingAppointmentsFromCache();
            renderReminderPanel();
            return;
        }

        const changedAppointments = [];
        let shouldRenderSelectedDay = false;

        await Promise.all(pendingDates.map(async dateString => {
            try {
                const previousAppointments = state.appointmentsByDate.get(dateString) || [];
                const nextAppointments = await fetchAppointmentsForDate(dateString);

                state.appointmentsByDate.set(dateString, nextAppointments);
                nextAppointments.forEach(appointment => rememberAppointmentDate(appointment.date));
                changedAppointments.push(...getChangedAppointments(previousAppointments, nextAppointments));

                if (dateString === state.selectedDate) {
                    state.appointments = nextAppointments;
                    shouldRenderSelectedDay = true;
                }
            } catch (error) {
                console.warn(`Kh\u00f4ng th\u1ec3 t\u1ef1 t\u1ea3i l\u1ea1i l\u1ecbch ch\u1edd ng\u00e0y ${dateString}.`, error);
            }
        }));

        syncPendingAppointmentsFromCache();
        renderReminderPanel();
        if (shouldRenderSelectedDay) {
            renderCalendar();
            renderDayPanel();
        }
        notifyAppointmentStatusChanges(changedAppointments);
    }

    async function loadAppointmentDotsForCurrentMonth(forceRefresh = false) {
        const monthKey = getMonthKey(state.currentYear, state.currentMonth);
        if ((!forceRefresh && state.loadedAppointmentDateMonths.has(monthKey)) || state.loadingAppointmentDateMonths.has(monthKey)) return;

        state.loadingAppointmentDateMonths.add(monthKey);
        if (!state.appointmentDatesByMonth.has(monthKey)) {
            state.appointmentDatesByMonth.set(monthKey, new Set());
        }

        const totalDays = new Date(state.currentYear, state.currentMonth + 1, 0).getDate();
        const dateStrings = Array.from({ length: totalDays }, (_, index) =>
                `${state.currentYear}-${padZero(state.currentMonth + 1)}-${padZero(index + 1)}`
        );
        const changedAppointments = [];
        let shouldRenderSelectedDay = false;

        await Promise.all(dateStrings.map(async dateString => {
            if (!forceRefresh && state.appointmentsByDate.has(dateString)) {
                if (state.appointmentsByDate.get(dateString).length > 0) {
                    rememberAppointmentDate(dateString);
                }
                return;
            }

            try {
                const previousAppointments = state.appointmentsByDate.get(dateString) || [];
                const appointments = await fetchAppointmentsForDate(dateString);
                state.appointmentsByDate.set(dateString, appointments);

                if (appointments.length > 0) {
                    rememberAppointmentDate(dateString);
                }

                if (forceRefresh) {
                    changedAppointments.push(...getChangedAppointments(previousAppointments, appointments));
                }

                if (dateString === state.selectedDate) {
                    state.appointments = appointments;
                    shouldRenderSelectedDay = true;
                }
            } catch (error) {
                console.warn(`Kh\u00f4ng t\u1ea3i \u0111\u01b0\u1ee3c d\u1ea5u ch\u1ea5m l\u1ecbch h\u1eb9n ng\u00e0y ${dateString}.`, error);
            }
        }));

        state.loadingAppointmentDateMonths.delete(monthKey);
        state.loadedAppointmentDateMonths.add(monthKey);
        syncPendingAppointmentsFromCache();
        renderCalendar();
        renderReminderPanel();
        if (shouldRenderSelectedDay) {
            renderDayPanel();
        }
        if (forceRefresh) {
            notifyAppointmentStatusChanges(changedAppointments);
        }
    }

    function renderDayLoading(dateString) {
        elements.todayPanelTitle.textContent = `L\u1ecbch h\u1eb9n ng\u00e0y ${formatDateDisplay(dateString)}`;
        elements.todayAppointments.innerHTML = "";
        elements.todayAppointments.appendChild(createElement("li", "no-appointments", "\u0110ang t\u1ea3i l\u1ecbch h\u1eb9n..."));
    }

    function renderDayPanel() {
        elements.todayAppointments.innerHTML = "";
        const targetDate = state.selectedDate || getLocalDateString();
        const dayAppointments = state.appointments
                .filter(appointment => appointment.date === targetDate)
                .sort((first, second) => first.time.localeCompare(second.time));
        elements.todayPanelTitle.textContent = `L\u1ecbch h\u1eb9n ng\u00e0y ${formatDateDisplay(targetDate)}`;

        if (dayAppointments.length === 0) {
            elements.todayAppointments.appendChild(createElement("li", "no-appointments", "Kh\u00f4ng c\u00f3 l\u1ecbch h\u1eb9n."));
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

    function renderReminderPanel() {
        if (!elements.pendingReminders || !elements.reminderCount) return;

        const pendingAppointments = state.pendingAppointments
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

        elements.reminderCount.textContent = `${pendingAppointments.length} l\u1ecbch ch\u1edd`;
        elements.pendingReminders.innerHTML = "";

        if (pendingAppointments.length === 0) {
            elements.pendingReminders.appendChild(createElement("li", "sch-reminder-empty", "Kh\u00f4ng c\u00f3 l\u1ecbch ch\u1edd x\u1eed l\u00fd."));
            return;
        }

        pendingAppointments.slice(0, 4).forEach((appointment, index) => {
            const reminderLevel = getReminderLevel(appointment.minutesUntil);
            const item = createElement("li", `sch-reminder-item ${reminderLevel.className}`);
            const card = createElement("div", "sch-reminder-card");
            const priority = createElement("span", "sch-reminder-priority", `#${index + 1}`);
            const content = createElement("div", "sch-reminder-content");
            const title = createElement("strong", "", appointment.client || "Khách hàng");
            const due = createElement("span", "sch-reminder-due", formatTimeUntil(appointment.minutesUntil));
            const autoApproveAfterMinutes = getAutoApproveAfterMinutes();
            const autoLabel = autoApproveAfterMinutes === null
                    ? "Tự động duyệt đang tắt"
                    : `Auto sau ${autoApproveAfterMinutes} phút đặt lịch`;
            const meta = createElement("span", "sch-reminder-meta", `${appointment.appointmentType} • Hẹn ${formatAppointmentLabel(appointment)} • ${autoLabel}`);
            const note = createElement("small", "sch-reminder-note", getReminderNote(reminderLevel));
            const badge = createElement("span", `sch-reminder-badge ${reminderLevel.className}`, reminderLevel.label);

            content.append(title, due, meta, note);
            card.append(priority, content, badge);
            item.appendChild(card);
            elements.pendingReminders.appendChild(item);
        });
    }

    function findAppointmentById(id) {
        return state.appointments.find(appointment => String(appointment.id) === String(id));
    }

    function showDetailModal(appointment) {
        if (!appointment) {
            alert("Kh\u00f4ng t\u00ecm th\u1ea5y l\u1ecbch h\u1eb9n trong ng\u00e0y \u0111ang ch\u1ecdn.");
            return;
        }

        elements.infoId.textContent = appointment.id || "-";
        elements.infoClient.textContent = appointment.client || "-";
        elements.infoPhone.textContent = appointment.phone || "-";
        elements.infoEmail.textContent = appointment.email || "-";
        elements.infoDate.textContent = formatDateDisplay(appointment.date);
        elements.infoTime.textContent = appointment.time || "-";
        elements.infoStatus.textContent = statusLabels[appointment.status] || appointment.status || "-";
        elements.infoAppointmentType.textContent = appointment.appointmentType || "-";
        elements.infoNote.textContent = appointment.note || "-";
        updateAppointmentActionForms(appointment);
        elements.detailModal.classList.add("show");
    }

    function updateAppointmentActionForms(appointment) {
        const actionForms = [
            elements.approveAppointmentForm,
            elements.rejectAppointmentForm,
            elements.completeAppointmentForm,
            elements.noShowAppointmentForm
        ].filter(Boolean);

        actionForms.forEach(form => {
            form.classList.add("sch-hidden");
            const idInput = form.querySelector('input[name="id"]');
            const dateInput = form.querySelector('input[name="date"]');
            if (idInput) idInput.value = appointment.id || "";
            if (dateInput) dateInput.value = state.selectedDate || appointment.date || "";
        });

        if (appointment.status === "PENDING") {
            elements.approveAppointmentForm?.classList.remove("sch-hidden");
            elements.rejectAppointmentForm?.classList.remove("sch-hidden");
            return;
        }

        if (appointment.status === "APPROVED") {
            elements.completeAppointmentForm?.classList.remove("sch-hidden");
            elements.noShowAppointmentForm?.classList.remove("sch-hidden");
        }
    }

    function openDetailModal(id) {
        const appointment = findAppointmentById(id);
        showDetailModal(appointment);
    }

    function closeDetailModal() {
        elements.detailModal.classList.remove("show");
    }

    function updateAppointmentSettingUi() {
        if (elements.autoApprove && elements.autoApproveAfter) {
            elements.autoApproveAfter.disabled = !elements.autoApprove.checked;
        }

        if (elements.autoComplete && elements.autoCompleteAfter) {
            elements.autoCompleteAfter.disabled = !elements.autoComplete.checked;
        }

        const autoApproveFallback = elements.appointmentSettingForm?.querySelector('input[type="hidden"][name="autoApprove"]');
        const autoCompleteFallback = elements.appointmentSettingForm?.querySelector('input[type="hidden"][name="autoComplete"]');

        if (autoApproveFallback && elements.autoApprove) {
            autoApproveFallback.disabled = elements.autoApprove.checked;
        }

        if (autoCompleteFallback && elements.autoComplete) {
            autoCompleteFallback.disabled = elements.autoComplete.checked;
        }

        if (elements.pauseReason && elements.pauseReasonCount) {
            elements.pauseReasonCount.textContent = `${elements.pauseReason.value.length}/500`;
        }
    }

    function getAppointmentSettingDraft() {
        return {
            autoApprove: Boolean(elements.autoApprove?.checked),
            autoApproveAfter: elements.autoApproveAfter?.value || "",
            autoComplete: Boolean(elements.autoComplete?.checked),
            autoCompleteAfter: elements.autoCompleteAfter?.value || "",
            pauseFrom: elements.pauseFrom?.value || "",
            pauseFromDate: elements.pauseFromDate?.value || "",
            pauseFromTime: elements.pauseFromTime?.value || "",
            pauseTo: elements.pauseTo?.value || "",
            pauseToDate: elements.pauseToDate?.value || "",
            pauseToTime: elements.pauseToTime?.value || "",
            pauseReason: elements.pauseReason?.value || ""
        };
    }

    function saveAppointmentSettingDraft() {
        if (!elements.appointmentSettingForm) return;
        localStorage.setItem("artisanAppointmentSettingDraft", JSON.stringify(getAppointmentSettingDraft()));
    }

    function restoreAppointmentSettingDraftIfNeeded() {
        if (!elements.appointmentSettingForm || elements.appointmentSettingForm.dataset.hasSetting === "true") return;

        const rawDraft = localStorage.getItem("artisanAppointmentSettingDraft");
        if (!rawDraft) return;

        try {
            const draft = JSON.parse(rawDraft);
            if (elements.autoApprove) elements.autoApprove.checked = Boolean(draft.autoApprove);
            if (elements.autoApproveAfter && draft.autoApproveAfter) elements.autoApproveAfter.value = draft.autoApproveAfter;
            if (elements.autoComplete) elements.autoComplete.checked = Boolean(draft.autoComplete);
            if (elements.autoCompleteAfter && draft.autoCompleteAfter) elements.autoCompleteAfter.value = draft.autoCompleteAfter;
            if (elements.pauseFrom) elements.pauseFrom.value = draft.pauseFrom || "";
            if (elements.pauseFromDate) elements.pauseFromDate.value = draft.pauseFromDate || "";
            if (elements.pauseFromTime) elements.pauseFromTime.dataset.value = draft.pauseFromTime || "";
            if (elements.pauseTo) elements.pauseTo.value = draft.pauseTo || "";
            if (elements.pauseToDate) elements.pauseToDate.value = draft.pauseToDate || "";
            if (elements.pauseToTime) elements.pauseToTime.dataset.value = draft.pauseToTime || "";
            if (elements.pauseReason) elements.pauseReason.value = draft.pauseReason || "";
        } catch (error) {
            localStorage.removeItem("artisanAppointmentSettingDraft");
        }
    }

    function hydrateAppointmentSettingFromDataset() {
        const dataset = elements.appointmentSettingForm?.dataset;
        if (!dataset || dataset.hasSetting !== "true") return;

        if (elements.autoApprove && dataset.autoApprove) {
            elements.autoApprove.checked = dataset.autoApprove === "true";
        }

        if (elements.autoApproveAfter && dataset.autoApproveAfter) {
            elements.autoApproveAfter.value = dataset.autoApproveAfter;
        }

        if (elements.autoComplete && dataset.autoComplete) {
            elements.autoComplete.checked = dataset.autoComplete === "true";
        }

        if (elements.autoCompleteAfter && dataset.autoCompleteAfter) {
            elements.autoCompleteAfter.value = dataset.autoCompleteAfter;
        }

        if (elements.pauseFrom) elements.pauseFrom.value = dataset.pauseFrom || "";
        if (elements.pauseFromDate) elements.pauseFromDate.value = dataset.pauseFromDate || "";
        if (elements.pauseFromTime) elements.pauseFromTime.dataset.value = dataset.pauseFromTime || "";
        if (elements.pauseTo) elements.pauseTo.value = dataset.pauseTo || "";
        if (elements.pauseToDate) elements.pauseToDate.value = dataset.pauseToDate || "";
        if (elements.pauseToTime) elements.pauseToTime.dataset.value = dataset.pauseToTime || "";
        if (elements.pauseReason) elements.pauseReason.value = dataset.pauseReason || "";
    }

    function applyInitialPauseTime(selectElement) {
        if (!selectElement || !selectElement.dataset.value) return;
        selectElement.value = selectElement.dataset.value;
    }

    function populatePauseTimeSelect(selectElement) {
        if (!selectElement) return;

        const selectedValue = selectElement.dataset.value || selectElement.value || "";
        selectElement.innerHTML = "";
        selectElement.appendChild(new Option("Ch\u1ecdn gi\u1edd", ""));

        for (let hour = 0; hour < 24; hour += 1) {
            [0, 30].forEach(minute => {
                const timeValue = `${padZero(hour)}:${padZero(minute)}`;
                selectElement.appendChild(new Option(timeValue, timeValue));
            });
        }

        selectElement.value = selectedValue;
    }

    function syncPauseHiddenInput(hiddenInput, dateInput, timeInput) {
        if (!hiddenInput || !dateInput || !timeInput) return;
        hiddenInput.value = dateInput.value && timeInput.value ? `${dateInput.value}T${timeInput.value}` : "";
    }

    function syncPauseHiddenInputs() {
        syncPauseHiddenInput(elements.pauseFrom, elements.pauseFromDate, elements.pauseFromTime);
        syncPauseHiddenInput(elements.pauseTo, elements.pauseToDate, elements.pauseToTime);
    }

    function applyPauseDateLimits() {
        const today = getLocalDateString();
        if (elements.pauseFromDate) elements.pauseFromDate.min = today;
        if (elements.pauseToDate) elements.pauseToDate.min = today;
    }

    function showAppointmentSettingError(event, message) {
        event.preventDefault();
        if (window.BSMSToast) {
            BSMSToast.error(message);
        } else {
            alert(message);
        }
    }

    function clearPauseSetting() {
        if (elements.pauseFrom) elements.pauseFrom.value = "";
        if (elements.pauseFromDate) elements.pauseFromDate.value = "";
        if (elements.pauseFromTime) elements.pauseFromTime.value = "";
        if (elements.pauseTo) elements.pauseTo.value = "";
        if (elements.pauseToDate) elements.pauseToDate.value = "";
        if (elements.pauseToTime) elements.pauseToTime.value = "";
        if (elements.pauseReason) elements.pauseReason.value = "";

        updateAppointmentSettingUi();
        saveAppointmentSettingDraft();

        if (window.BSMSToast) {
            BSMSToast.success("\u0110\u00e3 x\u00f3a th\u00f4ng tin t\u1ea1m d\u1eebng. B\u1ea5m L\u01b0u c\u1ea5u h\u00ecnh \u0111\u1ec3 \u00e1p d\u1ee5ng.");
        }
    }

    function validateAppointmentSettingForm(event) {
        updateAppointmentSettingUi();
        syncPauseHiddenInputs();
        saveAppointmentSettingDraft();
        if (!elements.pauseFrom || !elements.pauseTo) return;

        const pauseFrom = elements.pauseFrom.value ? new Date(elements.pauseFrom.value) : null;
        const pauseTo = elements.pauseTo.value ? new Date(elements.pauseTo.value) : null;

        if ((elements.pauseFromDate?.value && !elements.pauseFromTime?.value)
                || (!elements.pauseFromDate?.value && elements.pauseFromTime?.value)
                || (elements.pauseToDate?.value && !elements.pauseToTime?.value)
                || (!elements.pauseToDate?.value && elements.pauseToTime?.value)) {
            showAppointmentSettingError(event, "Vui l\u00f2ng ch\u1ecdn \u0111\u1ee7 ng\u00e0y v\u00e0 gi\u1edd khi t\u1ea1m d\u1eebng nh\u1eadn l\u1ecbch.");
            return;
        }

        const today = getLocalDateString();
        if ((elements.pauseFromDate?.value && elements.pauseFromDate.value < today)
                || (elements.pauseToDate?.value && elements.pauseToDate.value < today)) {
            showAppointmentSettingError(event, "Kh\u00f4ng th\u1ec3 ch\u1ecdn ng\u00e0y qu\u00e1 kh\u1ee9 khi t\u1ea1m d\u1eebng nh\u1eadn l\u1ecbch.");
            return;
        }

        if (pauseFrom && pauseTo && pauseTo <= pauseFrom) {
            showAppointmentSettingError(event, "Th\u1eddi \u0111i\u1ec3m k\u1ebft th\u00fac ph\u1ea3i sau th\u1eddi \u0111i\u1ec3m b\u1eaft \u0111\u1ea7u.");
        }
    }

    function bindEvents() {
        elements.prevMonth.addEventListener("click", () => {
            state.currentMonth -= 1;
            if (state.currentMonth < 0) {
                state.currentMonth = 11;
                state.currentYear -= 1;
            }
            renderCalendar();
            loadAppointmentDotsForCurrentMonth();
        });

        elements.nextMonth.addEventListener("click", () => {
            state.currentMonth += 1;
            if (state.currentMonth > 11) {
                state.currentMonth = 0;
                state.currentYear += 1;
            }
            renderCalendar();
            loadAppointmentDotsForCurrentMonth();
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

        elements.autoApprove?.addEventListener("change", () => {
            updateAppointmentSettingUi();
            renderReminderPanel();
        });
        elements.autoApproveAfter?.addEventListener("input", renderReminderPanel);
        elements.autoComplete?.addEventListener("change", updateAppointmentSettingUi);
        elements.pauseFromDate?.addEventListener("change", syncPauseHiddenInputs);
        elements.pauseFromTime?.addEventListener("change", syncPauseHiddenInputs);
        elements.pauseToDate?.addEventListener("change", syncPauseHiddenInputs);
        elements.pauseToTime?.addEventListener("change", syncPauseHiddenInputs);
        elements.pauseReason?.addEventListener("input", updateAppointmentSettingUi);
        elements.clearPauseSetting?.addEventListener("click", clearPauseSetting);
        elements.appointmentSettingForm?.addEventListener("submit", validateAppointmentSettingForm);

    }

    function renderAll() {
        renderReminderPanel();
        renderCalendar();
        renderDayPanel();
    }


    function init() {
        state.selectedDate = getSelectedDateFromPage();
        extractAppointmentsFromDOM();
        const selectedDate = new Date(`${state.selectedDate}T00:00:00`);
        if (!Number.isNaN(selectedDate.getTime())) {
            state.currentMonth = selectedDate.getMonth();
            state.currentYear = selectedDate.getFullYear();
        }
        hydrateAppointmentSettingFromDataset();
        restoreAppointmentSettingDraftIfNeeded();
        populatePauseTimeSelect(elements.pauseFromTime);
        populatePauseTimeSelect(elements.pauseToTime);
        applyInitialPauseTime(elements.pauseFromTime);
        applyInitialPauseTime(elements.pauseToTime);
        applyPauseDateLimits();
        syncPauseHiddenInputs();
        bindEvents();
        updateAppointmentSettingUi();
        renderAll();
        loadAppointmentDotsForCurrentMonth();
        setInterval(refreshSelectedDateAppointments, 5000);
        setInterval(refreshPendingAppointmentDates, 15000);
        setInterval(() => loadAppointmentDotsForCurrentMonth(true), 60000);
    }

    init();
});
