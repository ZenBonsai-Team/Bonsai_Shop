document.addEventListener("DOMContentLoaded", () => {
    const state = {
        appointments: [],
        currentMonth: new Date().getMonth(),
        currentYear: new Date().getFullYear(),
        selectedDate: "",
        editingAppointmentId: null,
        completingAppointmentId: null,
        completeSubmitMode: ""
    };

    const monthNames = [
        "Tháng 1", "Tháng 2", "Tháng 3", "Tháng 4", "Tháng 5", "Tháng 6",
        "Tháng 7", "Tháng 8", "Tháng 9", "Tháng 10", "Tháng 11", "Tháng 12"
    ];

    const elements = {
        statTotal: document.getElementById("statTotal"),
        statToday: document.getElementById("statToday"),
        statApproved: document.getElementById("statApproved"),
        statPending: document.getElementById("statPending"),
        statOverdue: document.getElementById("statOverdue"),
        totalPercent: document.getElementById("totalPercent"),
        approvedPercent: document.getElementById("approvedPercent"),
        pendingPercent: document.getElementById("pendingPercent"),
        overduePercent: document.getElementById("overduePercent"),
        reminderCount: document.getElementById("reminderCount"),
        pendingReminders: document.getElementById("pendingReminders"),
        currentMonth: document.querySelector(".current-month"),
        prevMonth: document.getElementById("prevMonth"),
        nextMonth: document.getElementById("nextMonth"),
        calendarGrid: document.querySelector(".calendar-grid"),
        todayAppointments: document.getElementById("todayAppointments"),
        todayPanelTitle: document.getElementById("todayPanelTitle"),
        searchInput: document.getElementById("searchInput"),
        statusFilter: document.getElementById("statusFilter"),
        appointmentTableBody: document.getElementById("appointmentTableBody"),
        editModal: document.getElementById("editModal"),
        closeModal: document.getElementById("closeModal"),
        closeModalFooter: document.getElementById("closeModalFooter"),
        saveStatus: document.getElementById("saveStatus"),
        statusArea: document.querySelector(".sch-status-area"),
        statusSelectLabel: document.getElementById("statusSelectLabel"),
        statusSelect: document.getElementById("statusSelect"),
        rejectReason: document.getElementById("rejectReason"),
        rejectReasonGroup: document.getElementById("rejectReasonGroup"),
        statusMessage: document.getElementById("statusMessage"),
        updateStatusForm: document.getElementById("updateStatusForm"),
        completeForm: document.getElementById("globalCompleteForm"),
        overdueForm: document.getElementById("globalOverdueForm"),
        confirmCompleteModal: document.getElementById("confirmCompleteModal"),
        confirmCompleteMessage: document.getElementById("confirmCompleteMessage"),
        cancelCompleteConfirm: document.getElementById("cancelCompleteConfirm"),
        acceptCompleteConfirm: document.getElementById("acceptCompleteConfirm"),
        statusInput: document.getElementById("statusInput"),
        messageInput: document.getElementById("messageInput"),
        infoId: document.getElementById("infoId"),
        infoClient: document.getElementById("infoClient"),
        infoBonsai: document.getElementById("infoBonsai"),
        infoDate: document.getElementById("infoDate"),
        infoTime: document.getElementById("infoTime"),
        infoPhone: document.getElementById("infoPhone"),
        infoEmail: document.getElementById("infoEmail"),
        infoNote: document.getElementById("infoNote"),
        notificationBtn: document.getElementById("notificationBtn"),
        notificationPopup: document.getElementById("notificationPopup")
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

    function parseDateInput(displayString) {
        if (!displayString) return "";
        const [day, month, year] = displayString.split("/");
        return `${year}-${month}-${day}`;
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

    function parseAppointmentDecisionDeadline(appointment) {
        if (!appointment.date) return null;
        const [year, month, day] = appointment.date.split("-").map(Number);
        const deadline = new Date(year, month - 1, day, 0, 0, 0, 0);
        return Number.isNaN(deadline.getTime()) ? null : deadline;
    }

    function isOverduePending(appointment) {
        if (appointment.status !== "PENDING") return false;
        const decisionDeadline = parseAppointmentDecisionDeadline(appointment);
        return decisionDeadline ? decisionDeadline.getTime() <= Date.now() : false;
    }

    function getEffectiveStatus(appointment) {
        return isOverduePending(appointment) ? "OVERDUE" : appointment.status;
    }

    function getMinutesUntilAppointment(appointment) {
        const decisionDeadline = parseAppointmentDecisionDeadline(appointment);
        return decisionDeadline ? Math.ceil((decisionDeadline.getTime() - Date.now()) / 60000) : null;
    }

    function getReminderLevel(minutesUntil) {
        if (minutesUntil === null) {
            return {
                className: "neutral",
                label: "Cần kiểm tra",
                priority: 4
            };
        }

        if (minutesUntil <= 30) {
            return {
                className: "urgent",
                label: "Cần xử lý ngay",
                priority: 1
            };
        }

        if (minutesUntil <= 120) {
            return {
                className: "soon",
                label: "Sắp đến giờ hẹn",
                priority: 2
            };
        }

        if (minutesUntil <= 1440) {
            return {
                className: "today",
                label: "Trong 24 giờ",
                priority: 3
            };
        }

        return {
            className: "neutral",
            label: "Chờ duyệt",
            priority: 4
        };
    }

    function formatTimeUntil(minutesUntil) {
        if (minutesUntil === null) return "Chưa xác định thời gian";
        if (minutesUntil <= 0) return "Đã tới giờ hẹn";
        if (minutesUntil < 60) return `Còn ${minutesUntil} phút`;

        const hours = Math.floor(minutesUntil / 60);
        const minutes = minutesUntil % 60;
        if (hours < 24) {
            return minutes > 0 ? `Còn ${hours} giờ ${minutes} phút` : `Còn ${hours} giờ`;
        }

        const days = Math.floor(hours / 24);
        const remainingHours = hours % 24;
        return remainingHours > 0 ? `Còn ${days} ngày ${remainingHours} giờ` : `Còn ${days} ngày`;
    }

    function getReminderLevelV2(minutesUntil) {
        if (minutesUntil === null) {
            return {
                className: "neutral",
                label: "Cần kiểm tra",
                priority: 5
            };
        }

        if (minutesUntil <= 30) {
            return {
                className: "urgent",
                label: "Hết hạn sớm",
                priority: 1
            };
        }

        if (minutesUntil <= 120) {
            return {
                className: "soon",
                label: "Rất gần hạn",
                priority: 2
            };
        }

        if (minutesUntil <= 480) {
            return {
                className: "high",
                label: "Ưu tiên cao",
                priority: 3
            };
        }

        if (minutesUntil <= 1440) {
            return {
                className: "today",
                label: "Trong 24 giờ",
                priority: 4
            };
        }

        return {
            className: "neutral",
                label: "Chờ duyệt",
            priority: 5
        };
    }

    function formatTimeUntilV2(minutesUntil) {
        if (minutesUntil === null) return "Chưa xác định thời gian";
        if (minutesUntil <= 0) return "Đã hết hạn xử lý";
        if (minutesUntil < 60) return `Còn ${minutesUntil} phút để xử lý`;

        const hours = Math.floor(minutesUntil / 60);
        const minutes = minutesUntil % 60;
        if (minutesUntil < 1440) {
            return minutes > 0 ? `Còn ${hours} giờ ${minutes} phút để xử lý` : `Còn ${hours} giờ để xử lý`;
        }

        const days = Math.floor(hours / 24);
        const remainingHours = hours % 24;
        return remainingHours > 0 ? `Còn ${days} ngày ${remainingHours} giờ để xử lý` : `Còn ${days} ngày để xử lý`;
    }

    function formatTimeUntilExact(minutesUntil) {
        return "";
    }

    function formatAppointmentLabel(appointment) {
        return `${formatDateDisplay(appointment.date)} lúc ${appointment.time}`;
    }

    function formatDecisionDeadlineLabel(appointment) {
        return `Hạn xử lý: trước 00:00 ngày ${formatDateDisplay(appointment.date)}`;
    }

    function getReminderActionLabel(reminderLevel) {
        return reminderLevel.priority <= 2 ? "Xử lý ngay" : "Xem và xử lý";
    }

    function getReminderNote(reminderLevel) {
        if (reminderLevel.className === "urgent") return "Sắp hết hạn chỉnh, cần duyệt hoặc từ chối ngay.";
        if (reminderLevel.className === "soon") return "Nên xử lý trước khi sang ngày xem lịch.";
        if (reminderLevel.className === "high") return "Hạn xử lý còn trong 8 giờ, ưu tiên kiểm tra trước.";
        if (reminderLevel.className === "today") return "Hạn xử lý nằm trong 24 giờ tới.";
        return "Lịch đang chờ duyệt.";
    }

    function createElement(tagName, className, text) {
        const element = document.createElement(tagName);
        if (className) element.className = className;
        if (text !== undefined) element.textContent = text;
        return element;
    }

    function appendTextCell(row, text, strong = false) {
        const cell = document.createElement("td");
        if (strong) {
            const strongElement = document.createElement("strong");
            strongElement.textContent = text;
            cell.appendChild(strongElement);
        } else {
            cell.textContent = text;
        }
        row.appendChild(cell);
        return cell;
    }

    function createStatusBadge(status) {
        const statusLabels = {
            OVERDUE: "QUÁ HẠN"
        };
        const badge = createElement("span", `status-badge ${status.toLowerCase()}`, statusLabels[status] || status);
        return badge;
    }

    function setStatusMessage(message) {
        elements.statusMessage.textContent = message;
        elements.statusMessage.classList.toggle("visible", Boolean(message));
    }

    function toggleRejectReason(show) {
        elements.rejectReasonGroup.classList.toggle("sch-hidden", !show);
        elements.rejectReason.required = show;
        if (!show) {
            elements.rejectReason.classList.remove("sch-field-error");
        }
    }

    function validateRejectReason() {
        if (elements.statusSelect.value !== "REJECTED") return true;

        const reason = elements.rejectReason.value.trim();
        if (reason.length >= 5) {
            elements.rejectReason.classList.remove("sch-field-error");
            return true;
        }

        elements.rejectReason.classList.add("sch-field-error");
        setStatusMessage("Vui lòng nhập lý do từ chối tối thiểu 5 ký tự trước khi lưu.");
        elements.rejectReason.focus();
        return false;
    }

    function setModalEditMode(canEdit) {
        elements.statusArea?.classList.toggle("readonly", !canEdit);
        elements.statusSelect.classList.toggle("sch-hidden", !canEdit);
        elements.saveStatus.classList.toggle("sch-hidden", !canEdit);
        elements.closeModalFooter.textContent = canEdit ? "Hủy" : "Đóng";

        if (elements.statusSelectLabel) {
            elements.statusSelectLabel.textContent = canEdit ? "Cập nhật trạng thái" : "Trạng thái hiện tại";
        }
    }

    function setStatusOptions(options) {
        elements.statusSelect.innerHTML = "";
        options.forEach(option => {
            const optionElement = document.createElement("option");
            optionElement.value = option.value;
            optionElement.textContent = option.label;
            elements.statusSelect.appendChild(optionElement);
        });
    }

    function extractAppointmentsFromDOM() {
        state.appointments = [];
        elements.appointmentTableBody.querySelectorAll("tr").forEach(row => {
            if (row.cells.length < 6) return;

            const dateText = row.cells[3] ? row.cells[3].textContent.trim() : "";
            const status = row.cells[5] ? row.cells[5].textContent.trim().toUpperCase() : "PENDING";

            state.appointments.push({
                id: row.dataset.id || "",
                phone: row.dataset.phone || "",
                email: row.dataset.email || "",
                note: row.dataset.note || "",
                appointmentAt: row.dataset.appointmentAt || "",
                client: row.cells[1] ? row.cells[1].textContent.trim() : "",
                bonsai: row.cells[2] ? row.cells[2].textContent.trim() : "",
                date: parseDateInput(dateText),
                time: row.cells[4] ? row.cells[4].textContent.trim() : "",
                status
            });
        });
    }

    function renderStats() {
        const total = state.appointments.length;
        const todayString = getLocalDateString();
        const today = state.appointments.filter(appointment => appointment.date === todayString).length;
        const approved = state.appointments.filter(appointment => getEffectiveStatus(appointment) === "APPROVED").length;
        const pending = state.appointments.filter(appointment => getEffectiveStatus(appointment) === "PENDING").length;
        const overdue = state.appointments.filter(appointment => getEffectiveStatus(appointment) === "OVERDUE").length;
        const activeAppointments = approved + pending + overdue;

        elements.statTotal.textContent = total;
        elements.statToday.textContent = today;
        elements.statApproved.textContent = approved;
        elements.statPending.textContent = pending;
        if (elements.statOverdue) elements.statOverdue.textContent = overdue;
        elements.totalPercent.textContent = total > 0 ? `+${total}` : "0";
        elements.approvedPercent.textContent = activeAppointments > 0
                ? `${Math.round((approved / activeAppointments) * 100)}%`
                : "0%";
        elements.pendingPercent.textContent = activeAppointments > 0
                ? `${Math.round((pending / activeAppointments) * 100)}%`
                : "0%";
        if (elements.overduePercent) {
            elements.overduePercent.textContent = activeAppointments > 0
                    ? `${Math.round((overdue / activeAppointments) * 100)}%`
                    : "0%";
        }
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

        for (let day = 1; day <= totalDays; day += 1) {
            const dayButton = createElement("button", "calendar-day current-month-day", String(day));
            const dateString = `${state.currentYear}-${padZero(state.currentMonth + 1)}-${padZero(day)}`;
            dayButton.type = "button";
            dayButton.dataset.date = dateString;
            dayButton.setAttribute("aria-label", `Xem lịch hẹn ngày ${formatDateDisplay(dateString)}`);

            if (dateString === getLocalDateString()) dayButton.classList.add("today");
            if (dateString === state.selectedDate) dayButton.classList.add("selected");

            if (state.appointments.some(appointment => appointment.date === dateString)) {
                dayButton.appendChild(createElement("span", "appointment-dot"));
            }

            dayButton.addEventListener("click", () => selectCalendarDate(dateString));
            elements.calendarGrid.appendChild(dayButton);
        }
    }

    function selectCalendarDate(dateString) {
        if (state.selectedDate === dateString) {
            state.selectedDate = "";
            elements.todayPanelTitle.textContent = "Lịch hẹn hôm nay";
        } else {
            state.selectedDate = dateString;
            elements.todayPanelTitle.textContent = `Lịch hẹn ngày ${formatDateDisplay(dateString)}`;
        }

        renderCalendar();
        renderDayPanel();
        renderTable();
    }

    function renderDayPanel() {
        elements.todayAppointments.innerHTML = "";
        const targetDate = state.selectedDate || getLocalDateString();
        const dayAppointments = state.appointments
                .filter(appointment => appointment.date === targetDate)
                .sort((first, second) => first.time.localeCompare(second.time));

        if (dayAppointments.length === 0) {
            elements.todayAppointments.appendChild(
                    createElement("li", "no-appointments", "Không có lịch hẹn.")
            );
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
            left.appendChild(createElement("span", "item-title", appointment.client));
            left.appendChild(createElement("span", "item-subtitle", `${appointment.bonsai} (${appointment.id})`));
            time.appendChild(createElement("i", "fa-regular fa-clock"));
            time.append(` ${appointment.time}`);
            right.appendChild(time);
            right.appendChild(createStatusBadge(getEffectiveStatus(appointment)));
            item.append(left, right);
            elements.todayAppointments.appendChild(item);
        });
    }

    function renderReminderPanel() {
        if (!elements.pendingReminders || !elements.reminderCount) return;

        const pendingAppointments = state.appointments
                .filter(appointment => getEffectiveStatus(appointment) === "PENDING")
                .map(appointment => ({
                    ...appointment,
                    minutesUntil: getMinutesUntilAppointment(appointment)
                }))
                .sort((first, second) => {
                    const firstLevel = getReminderLevelV2(first.minutesUntil);
                    const secondLevel = getReminderLevelV2(second.minutesUntil);
                    if (firstLevel.priority !== secondLevel.priority) {
                        return firstLevel.priority - secondLevel.priority;
                    }
                    if (first.minutesUntil === null) return 1;
                    if (second.minutesUntil === null) return -1;
                    return first.minutesUntil - second.minutesUntil;
                });

        elements.reminderCount.textContent = `${pendingAppointments.length} lịch chờ`;
        elements.pendingReminders.innerHTML = "";

        if (pendingAppointments.length === 0) {
            const emptyItem = createElement("li", "sch-reminder-empty", "Không có lịch chờ xử lý.");
            elements.pendingReminders.appendChild(emptyItem);
            return;
        }

        pendingAppointments.slice(0, 4).forEach((appointment, index) => {
            const reminderLevel = getReminderLevelV2(appointment.minutesUntil);
            const item = createElement("li", `sch-reminder-item ${reminderLevel.className}`);
            const button = createElement("button", "sch-reminder-btn");
            const priority = createElement("span", "sch-reminder-priority", `#${index + 1}`);
            const content = createElement("div", "sch-reminder-content");
            const title = createElement("strong", "", appointment.client);
            const due = createElement("span", "sch-reminder-due", formatTimeUntilV2(appointment.minutesUntil));
            const exactDue = createElement("small", "sch-reminder-exact", formatTimeUntilExact(appointment.minutesUntil));
            const deadline = createElement("small", "sch-reminder-deadline", formatDecisionDeadlineLabel(appointment));
            const meta = createElement(
                    "span",
                    "sch-reminder-meta",
                    `${appointment.bonsai} • ${formatAppointmentLabel(appointment)}`
            );
            const note = createElement("small", "sch-reminder-note", getReminderNote(reminderLevel));
            const badge = createElement("span", `sch-reminder-badge ${reminderLevel.className}`, reminderLevel.label);
            const action = createElement("span", "sch-reminder-action", getReminderActionLabel(reminderLevel));

            button.type = "button";
            button.dataset.id = appointment.id;
            content.append(title, due);
            if (exactDue.textContent) content.appendChild(exactDue);
            content.append(deadline, meta, note);
            button.append(priority, content, badge, action);
            item.appendChild(button);
            elements.pendingReminders.appendChild(item);
        });
    }

    function renderTable() {
        const searchText = elements.searchInput.value.toLowerCase().trim();
        const filterValue = elements.statusFilter.value;
        const filteredAppointments = state.appointments.filter(appointment => {
            const matchesSearch = appointment.id.toLowerCase().includes(searchText)
                    || appointment.client.toLowerCase().includes(searchText)
                    || appointment.bonsai.toLowerCase().includes(searchText);
            const effectiveStatus = getEffectiveStatus(appointment);
            const matchesStatus = filterValue === "ALL" || effectiveStatus === filterValue;
            const matchesDate = !state.selectedDate || appointment.date === state.selectedDate;
            return matchesSearch && matchesStatus && matchesDate;
        });

        elements.appointmentTableBody.innerHTML = "";

        if (filteredAppointments.length === 0) {
            const emptyRow = document.createElement("tr");
            const emptyCell = createElement("td", "sch-empty-table", "Không tìm thấy lịch hẹn phù hợp.");
            emptyCell.colSpan = 7;
            emptyRow.appendChild(emptyCell);
            elements.appointmentTableBody.appendChild(emptyRow);
            return;
        }

        filteredAppointments.forEach(appointment => {
            const row = document.createElement("tr");
            row.dataset.id = appointment.id;
            row.dataset.phone = appointment.phone;
            row.dataset.email = appointment.email;
            row.dataset.note = appointment.note;
            row.dataset.appointmentAt = appointment.appointmentAt;

            appendTextCell(row, appointment.id, true);
            appendTextCell(row, appointment.client);
            appendTextCell(row, appointment.bonsai);
            appendTextCell(row, formatDateDisplay(appointment.date));
            appendTextCell(row, appointment.time);

            const statusCell = document.createElement("td");
            statusCell.appendChild(createStatusBadge(getEffectiveStatus(appointment)));
            row.appendChild(statusCell);

            const actionsCell = createElement("td", "table-actions");
            actionsCell.appendChild(createActionElement(appointment));
            row.appendChild(actionsCell);
            elements.appointmentTableBody.appendChild(row);
        });
    }

    function createActionElement(appointment) {
        const effectiveStatus = getEffectiveStatus(appointment);

        if (appointment.status === "PENDING" && effectiveStatus === "OVERDUE") {
            const button = createElement("button", "btn-overdue", "Cập nhật quá hạn");
            button.type = "button";
            button.dataset.id = appointment.id;
            return button;
        }

        if (effectiveStatus === "OVERDUE") {
            return createElement("span", "sch-overdue-action", "Đã quá hạn");
        }

        if (effectiveStatus === "PENDING" || effectiveStatus === "APPROVED") {
            const button = createElement("button", "sch-edit-btn edit-btn");
            button.type = "button";
            button.dataset.id = appointment.id;
            button.setAttribute("aria-label", "Cập nhật lịch hẹn");
            button.appendChild(createElement("i", "fa-solid fa-pen-to-square"));
            return button;
        }

        const badgeClass = effectiveStatus === "COMPLETED"
                ? "badge bg-success"
                : effectiveStatus === "REJECTED"
                        ? "badge bg-danger"
                        : "badge bg-secondary";
        const text = effectiveStatus.charAt(0) + effectiveStatus.slice(1).toLowerCase();
        return createElement("span", badgeClass, text);
    }

    function openEditModal(id) {
        const appointment = state.appointments.find(item => item.id === String(id));
        if (!appointment) return;

        state.editingAppointmentId = appointment.id;
        elements.infoId.textContent = appointment.id;
        elements.infoClient.textContent = appointment.client;
        elements.infoBonsai.textContent = appointment.bonsai;
        elements.infoDate.textContent = formatDateDisplay(appointment.date);
        elements.infoTime.textContent = appointment.time;
        elements.infoPhone.textContent = appointment.phone || "-";
        elements.infoEmail.textContent = appointment.email || "-";
        elements.infoNote.textContent = appointment.note || "-";
        elements.rejectReason.value = "";
        const effectiveStatus = getEffectiveStatus(appointment);
        const canEdit = effectiveStatus === "PENDING" || effectiveStatus === "APPROVED";
        setModalEditMode(canEdit);

        if (effectiveStatus === "OVERDUE") {
            elements.statusSelect.disabled = true;
            elements.saveStatus.disabled = true;
            elements.statusSelect.value = "APPROVED";
            setStatusMessage("Lịch PENDING này đã qua hạn xử lý trước 00:00 ngày xem lịch, không thể duyệt từ giao diện.");
        } else if (effectiveStatus === "PENDING") {
            setStatusOptions([
                { value: "APPROVED", label: "APPROVED" },
                { value: "REJECTED", label: "REJECTED" }
            ]);
            elements.statusSelect.disabled = false;
            elements.saveStatus.disabled = false;
            elements.statusSelect.value = "APPROVED";
            setStatusMessage("");
        } else if (effectiveStatus === "APPROVED") {
            setStatusOptions([
                { value: "COMPLETED", label: "COMPLETED" }
            ]);
            elements.statusSelect.disabled = false;
            elements.saveStatus.disabled = false;
            elements.statusSelect.value = "COMPLETED";
            setStatusMessage("Lịch đã duyệt. Cập nhật COMPLETED khi khách đã xem cây xong.");
        } else {
            elements.statusSelect.disabled = true;
            elements.saveStatus.disabled = true;
            elements.statusSelect.value = effectiveStatus === "REJECTED" ? "REJECTED" : "APPROVED";
            setStatusMessage(`Lịch hẹn này đã được xử lý (${effectiveStatus}).`);
        }

        toggleRejectReason(canEdit && elements.statusSelect.value === "REJECTED");
        elements.editModal.classList.add("show");
    }

    function closeEditModal() {
        elements.editModal.classList.remove("show");
        state.editingAppointmentId = null;
        setStatusMessage("");
        setModalEditMode(true);
    }

    function saveStatusChange() {
        if (!state.editingAppointmentId) return;
        const appointment = state.appointments.find(item => item.id === String(state.editingAppointmentId));
        const effectiveStatus = appointment ? getEffectiveStatus(appointment) : "";

        if (!appointment || (effectiveStatus !== "PENDING" && effectiveStatus !== "APPROVED")) {
            setStatusMessage("Chỉ lịch PENDING hợp lệ hoặc APPROVED mới được cập nhật.");
            return;
        }

        if (effectiveStatus === "PENDING" && !validateRejectReason()) return;

        if (elements.statusSelect.value === "COMPLETED") {
            state.completeSubmitMode = "status-update";
            state.completingAppointmentId = state.editingAppointmentId;

            if (elements.confirmCompleteMessage) {
                elements.confirmCompleteMessage.textContent =
                        `Lịch hẹn mã #${state.editingAppointmentId} sẽ được chuyển sang trạng thái COMPLETED. Khách hàng sẽ nhận thông báo hoàn thành.`;
            }

            if (elements.confirmCompleteModal) {
                elements.confirmCompleteModal.classList.add("show");
                elements.confirmCompleteModal.setAttribute("aria-hidden", "false");
                elements.acceptCompleteConfirm?.focus();
                return;
            }
        }

        submitStatusChangeForm();
    }

    function submitStatusChangeForm() {
        if (!state.editingAppointmentId) return;

        elements.updateStatusForm.setAttribute(
                "action",
                `/artisan/appointments/update/${state.editingAppointmentId}/status`
        );
        elements.statusInput.value = elements.statusSelect.value;
        elements.messageInput.value = elements.statusSelect.value === "REJECTED"
                ? elements.rejectReason.value.trim()
                : "";
        elements.updateStatusForm.submit();
    }

    function completeAppointment(id) {
        if (!id) return;
        state.completingAppointmentId = id;
        state.completeSubmitMode = "legacy-check";

        if (elements.confirmCompleteMessage) {
            elements.confirmCompleteMessage.textContent =
                    `Lịch hẹn mã #${id} sẽ được chuyển sang trạng thái COMPLETED. Thao tác này không thể chỉnh lại từ màn hình này.`;
        }

        if (elements.confirmCompleteModal) {
            elements.confirmCompleteModal.classList.add("show");
            elements.confirmCompleteModal.setAttribute("aria-hidden", "false");
            elements.acceptCompleteConfirm?.focus();
            return;
        }

        submitCompleteAppointment();
    }

    function closeCompleteConfirm() {
        if (!elements.confirmCompleteModal) return;
        elements.confirmCompleteModal.classList.remove("show");
        elements.confirmCompleteModal.setAttribute("aria-hidden", "true");
        state.completingAppointmentId = null;
        state.completeSubmitMode = "";
    }

    function submitCompleteAppointment() {
        if (state.completeSubmitMode === "status-update") {
            submitStatusChangeForm();
            return;
        }

        const id = state.completingAppointmentId;
        if (!id) return;

        elements.completeForm.setAttribute("action", `/artisan/appointments/check/${id}`);
        elements.completeForm.submit();
    }

    function markAppointmentOverdue(id) {
        if (!id || !elements.overdueForm) return;
        if (!confirm(`Cập nhật lịch hẹn mã #${id} sang trạng thái QUÁ HẠN trong database?`)) return;

        elements.overdueForm.setAttribute("action", `/artisan/appointments/overdue/${id}`);
        elements.overdueForm.submit();
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
            if (item) openEditModal(item.dataset.id);
        });

        elements.todayAppointments.addEventListener("keydown", event => {
            if (event.key !== "Enter" && event.key !== " ") return;
            const item = event.target.closest(".appointment-item");
            if (!item) return;
            event.preventDefault();
            openEditModal(item.dataset.id);
        });

        if (elements.pendingReminders) {
            elements.pendingReminders.addEventListener("click", event => {
                const button = event.target.closest(".sch-reminder-btn");
                if (button) openEditModal(button.dataset.id);
            });
        }

        elements.appointmentTableBody.addEventListener("click", event => {
            const editButton = event.target.closest(".edit-btn");
            const completeButton = event.target.closest(".btn-complete");
            const overdueButton = event.target.closest(".btn-overdue");

            if (editButton) {
                openEditModal(editButton.dataset.id);
            } else if (completeButton) {
                completeAppointment(completeButton.dataset.id);
            } else if (overdueButton) {
                markAppointmentOverdue(overdueButton.dataset.id);
            }
        });

        elements.statusSelect.addEventListener("change", () => {
            toggleRejectReason(elements.statusSelect.value === "REJECTED");
            setStatusMessage("");
        });
        elements.rejectReason.addEventListener("input", () => {
            if (elements.rejectReason.value.trim().length >= 5) {
                elements.rejectReason.classList.remove("sch-field-error");
                setStatusMessage("");
            }
        });
        elements.searchInput.addEventListener("input", renderTable);
        elements.statusFilter.addEventListener("change", renderTable);
        elements.saveStatus.addEventListener("click", saveStatusChange);
        elements.closeModal.addEventListener("click", closeEditModal);
        elements.closeModalFooter.addEventListener("click", closeEditModal);
        elements.editModal.addEventListener("click", event => {
            if (event.target === elements.editModal) closeEditModal();
        });
        if (elements.cancelCompleteConfirm) {
            elements.cancelCompleteConfirm.addEventListener("click", closeCompleteConfirm);
        }
        if (elements.acceptCompleteConfirm) {
            elements.acceptCompleteConfirm.addEventListener("click", submitCompleteAppointment);
        }
        if (elements.confirmCompleteModal) {
            elements.confirmCompleteModal.addEventListener("click", event => {
                if (event.target === elements.confirmCompleteModal) closeCompleteConfirm();
            });
        }
        document.addEventListener("keydown", event => {
            if (event.key === "Escape" && elements.editModal.classList.contains("show")) {
                closeEditModal();
            }
            if (event.key === "Escape" && elements.confirmCompleteModal?.classList.contains("show")) {
                closeCompleteConfirm();
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
    }

    function init() {
        extractAppointmentsFromDOM();
        bindEvents();
        renderStats();
        renderReminderPanel();
        renderCalendar();
        renderDayPanel();
        renderTable();
        setInterval(() => {
            renderStats();
            renderReminderPanel();
            renderCalendar();
            renderDayPanel();
            renderTable();
        }, 60000);
    }

    init();
});
