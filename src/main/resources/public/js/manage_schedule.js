document.addEventListener("DOMContentLoaded", () => {
    const state = {
        appointments: [],
        currentMonth: new Date().getMonth(),
        currentYear: new Date().getFullYear(),
        selectedDate: "",
        editingAppointmentId: null
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
        totalPercent: document.getElementById("totalPercent"),
        approvedPercent: document.getElementById("approvedPercent"),
        pendingPercent: document.getElementById("pendingPercent"),
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
        statusSelect: document.getElementById("statusSelect"),
        rejectReason: document.getElementById("rejectReason"),
        rejectReasonGroup: document.getElementById("rejectReasonGroup"),
        statusMessage: document.getElementById("statusMessage"),
        updateStatusForm: document.getElementById("updateStatusForm"),
        completeForm: document.getElementById("globalCompleteForm"),
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
        const badge = createElement("span", `status-badge ${status.toLowerCase()}`, status);
        return badge;
    }

    function setStatusMessage(message) {
        elements.statusMessage.textContent = message;
        elements.statusMessage.classList.toggle("visible", Boolean(message));
    }

    function toggleRejectReason(show) {
        elements.rejectReasonGroup.classList.toggle("sch-hidden", !show);
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
        const approved = state.appointments.filter(appointment => appointment.status === "APPROVED").length;
        const pending = state.appointments.filter(appointment => appointment.status === "PENDING").length;
        const activeAppointments = approved + pending;

        elements.statTotal.textContent = total;
        elements.statToday.textContent = today;
        elements.statApproved.textContent = approved;
        elements.statPending.textContent = pending;
        elements.totalPercent.textContent = total > 0 ? `+${total}` : "0";
        elements.approvedPercent.textContent = activeAppointments > 0
                ? `${Math.round((approved / activeAppointments) * 100)}%`
                : "0%";
        elements.pendingPercent.textContent = activeAppointments > 0
                ? `${Math.round((pending / activeAppointments) * 100)}%`
                : "0%";
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
            right.appendChild(createStatusBadge(appointment.status));
            item.append(left, right);
            elements.todayAppointments.appendChild(item);
        });
    }

    function renderTable() {
        const searchText = elements.searchInput.value.toLowerCase().trim();
        const filterValue = elements.statusFilter.value;
        const filteredAppointments = state.appointments.filter(appointment => {
            const matchesSearch = appointment.id.toLowerCase().includes(searchText)
                    || appointment.client.toLowerCase().includes(searchText)
                    || appointment.bonsai.toLowerCase().includes(searchText);
            const matchesStatus = filterValue === "ALL" || appointment.status === filterValue;
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

            appendTextCell(row, appointment.id, true);
            appendTextCell(row, appointment.client);
            appendTextCell(row, appointment.bonsai);
            appendTextCell(row, formatDateDisplay(appointment.date));
            appendTextCell(row, appointment.time);

            const statusCell = document.createElement("td");
            statusCell.appendChild(createStatusBadge(appointment.status));
            row.appendChild(statusCell);

            const actionsCell = createElement("td", "table-actions");
            actionsCell.appendChild(createActionElement(appointment));
            row.appendChild(actionsCell);
            elements.appointmentTableBody.appendChild(row);
        });
    }

    function createActionElement(appointment) {
        if (appointment.status === "PENDING") {
            const button = createElement("button", "sch-edit-btn edit-btn");
            button.type = "button";
            button.dataset.id = appointment.id;
            button.setAttribute("aria-label", "Cập nhật lịch hẹn");
            button.appendChild(createElement("i", "fa-solid fa-pen-to-square"));
            return button;
        }

        if (appointment.status === "APPROVED") {
            const button = createElement("button", "btn btn-success btn-complete", "Complete");
            button.type = "button";
            button.dataset.id = appointment.id;
            return button;
        }

        const badgeClass = appointment.status === "COMPLETED"
                ? "badge bg-success"
                : appointment.status === "REJECTED"
                        ? "badge bg-danger"
                        : "badge bg-secondary";
        const text = appointment.status.charAt(0) + appointment.status.slice(1).toLowerCase();
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

        if (appointment.status === "PENDING") {
            elements.statusSelect.disabled = false;
            elements.saveStatus.disabled = false;
            elements.statusSelect.value = "APPROVED";
            setStatusMessage("");
        } else {
            elements.statusSelect.disabled = true;
            elements.saveStatus.disabled = true;
            elements.statusSelect.value = appointment.status === "REJECTED" ? "REJECTED" : "APPROVED";
            setStatusMessage(`Lịch hẹn này đã được xử lý (${appointment.status}).`);
        }

        toggleRejectReason(elements.statusSelect.value === "REJECTED");
        elements.editModal.classList.add("show");
    }

    function closeEditModal() {
        elements.editModal.classList.remove("show");
        state.editingAppointmentId = null;
        setStatusMessage("");
    }

    function saveStatusChange() {
        if (!state.editingAppointmentId) return;

        elements.updateStatusForm.setAttribute(
                "action",
                `/artisan/appointments/update/${state.editingAppointmentId}/status`
        );
        elements.statusInput.value = elements.statusSelect.value;
        elements.messageInput.value = elements.statusSelect.value === "REJECTED"
                ? elements.rejectReason.value
                : "";
        elements.updateStatusForm.submit();
    }

    function completeAppointment(id) {
        if (!id) return;
        if (!confirm(`Bạn có chắc chắn muốn hoàn thành (Complete) lịch hẹn mã #${id} này không?`)) return;

        elements.completeForm.setAttribute("action", `/artisan/appointments/check/${id}`);
        elements.completeForm.submit();
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

        elements.appointmentTableBody.addEventListener("click", event => {
            const editButton = event.target.closest(".edit-btn");
            const completeButton = event.target.closest(".btn-complete");

            if (editButton) {
                openEditModal(editButton.dataset.id);
            } else if (completeButton) {
                completeAppointment(completeButton.dataset.id);
            }
        });

        elements.statusSelect.addEventListener("change", () => {
            toggleRejectReason(elements.statusSelect.value === "REJECTED");
        });
        elements.searchInput.addEventListener("input", renderTable);
        elements.statusFilter.addEventListener("change", renderTable);
        elements.saveStatus.addEventListener("click", saveStatusChange);
        elements.closeModal.addEventListener("click", closeEditModal);
        elements.closeModalFooter.addEventListener("click", closeEditModal);
        elements.editModal.addEventListener("click", event => {
            if (event.target === elements.editModal) closeEditModal();
        });
        document.addEventListener("keydown", event => {
            if (event.key === "Escape" && elements.editModal.classList.contains("show")) {
                closeEditModal();
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
        renderCalendar();
        renderDayPanel();
        renderTable();
    }

    init();
});
