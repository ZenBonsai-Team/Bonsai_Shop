let appointments = [];
let currentMonth = new Date().getMonth();
let currentYear = new Date().getFullYear();
let selectedDateStr = "";
let editingAppointmentId = null;

function padZero(num) { return num < 10 ? `0${num}` : num; }
function formatDateDisplay(dateStr) {
    if (!dateStr) return "";
    const [year, month, day] = dateStr.split('-');
    return `${day}/${month}/${year}`;
}
function parseDateInput(displayStr) {
    if (!displayStr) return "";
    const [day, month, year] = displayStr.split('/');
    return `${year}-${month}-${day}`;
}

// DOM ELEMENTS
const statTotal = document.getElementById("statTotal");
const statToday = document.getElementById("statToday");
const statApproved = document.getElementById("statApproved");
const statPending = document.getElementById("statPending");
const totalPercent = document.getElementById("totalPercent");
const approvedPercent = document.getElementById("approvedPercent");
const pendingPercent = document.getElementById("pendingPercent");
const currentMonthSpan = document.querySelector(".current-month");
const prevMonthBtn = document.getElementById("prevMonth");
const nextMonthBtn = document.getElementById("nextMonth");
const calendarGrid = document.querySelector(".calendar-grid");
const todayAppointmentsUl = document.getElementById("todayAppointments");
const todayPanelTitle = document.getElementById("todayPanelTitle");
const searchInput = document.getElementById("searchInput");
const statusFilter = document.getElementById("statusFilter");
const appointmentTableBody = document.getElementById("appointmentTableBody");
const editModal = document.getElementById("editModal");
const infoId = document.getElementById("infoId");
const infoClient = document.getElementById("infoClient");
const infoBonsai = document.getElementById("infoBonsai");
const infoDate = document.getElementById("infoDate");
const infoTime = document.getElementById("infoTime");
const statusSelect = document.getElementById("statusSelect");
const saveStatusBtn = document.getElementById("saveStatus");
const closeModalBtn = document.getElementById("closeModal");
const infoPhone = document.getElementById("infoPhone");
const infoEmail = document.getElementById("infoEmail");
const infoNote = document.getElementById("infoNote");
const notificationBtn = document.getElementById("notificationBtn");
const notificationPopup = document.getElementById("notificationPopup");

function extractAppointmentsFromDOM() {
    appointments = [];
    const rows = appointmentTableBody.querySelectorAll("tr");
    rows.forEach(row => {
        const editBtn = row.querySelector(".edit-btn");
        if (!editBtn) return;

        const id = editBtn.getAttribute("data-id") || "";
        const phone = editBtn.getAttribute("data-phone") || "";
        const email = editBtn.getAttribute("data-email") || "";
        const note = editBtn.getAttribute("data-note") || "";
        const client = row.cells[1] ? row.cells[1].textContent.trim() : "";
        const bonsai = row.cells[2] ? row.cells[2].textContent.trim() : "";
        const dateText = row.cells[3] ? row.cells[3].textContent.trim() : "";
        const date = parseDateInput(dateText);
        const time = row.cells[4] ? row.cells[4].textContent.trim() : "";
        const status = row.cells[5] ? row.cells[5].textContent.trim().toUpperCase() : "PENDING";

        appointments.push({ id, client, phone, email, bonsai, date, time, status, note });
    });
}

function renderStats() {
    const total = appointments.length;
    const todayStr = new Date().toISOString().split("T")[0];
    const today = appointments.filter(a => a.date === todayStr).length;
    const approved = appointments.filter(a => a.status === "APPROVED").length;
    const pending = appointments.filter(a => a.status === "PENDING").length;

    statTotal.textContent = total;
    statToday.textContent = today;
    statApproved.textContent = approved;
    statPending.textContent = pending;

    totalPercent.textContent = total > 0 ? `+${Math.round((total / 100) * 10)}%` : "0%";
    const activeAppointments = approved + pending;
    if (activeAppointments > 0) {
        approvedPercent.textContent = `${Math.round((approved / activeAppointments) * 100)}%`;
        pendingPercent.textContent = `${Math.round((pending / activeAppointments) * 100)}%`;
    } else {
        approvedPercent.textContent = "0%";
        pendingPercent.textContent = "0%";
    }
}

function renderCalendar() {
    const monthNames = ["January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December"];
    currentMonthSpan.textContent = `${monthNames[currentMonth]} ${currentYear}`;
    calendarGrid.innerHTML = "";

    const firstDay = new Date(currentYear, currentMonth, 1);
    let startDayIdx = firstDay.getDay() - 1;
    if (startDayIdx < 0) startDayIdx = 6;
    const totalDays = new Date(currentYear, currentMonth + 1, 0).getDate();

    for (let i = 0; i < startDayIdx; i++) {
        const emptyCell = document.createElement("div");
        emptyCell.className = "calendar-day empty";
        calendarGrid.appendChild(emptyCell);
    }

    for (let day = 1; day <= totalDays; day++) {
        const dayCell = document.createElement("div");
        dayCell.className = "calendar-day current-month-day";
        dayCell.textContent = day;
        const dateStr = `${currentYear}-${padZero(currentMonth + 1)}-${padZero(day)}`;

        if (dateStr === new Date().toISOString().split("T")[0]) dayCell.classList.add("today");
        if (dateStr === selectedDateStr) dayCell.classList.add("selected");

        if (appointments.some(a => a.date === dateStr)) {
            const dot = document.createElement("span");
            dot.className = "appointment-dot";
            dayCell.appendChild(dot);
        }

        dayCell.addEventListener("click", () => {
            if (selectedDateStr === dateStr) {
                selectedDateStr = "";
                dayCell.classList.remove("selected");
                todayPanelTitle.textContent = "Today's Appointments";
            } else {
                document.querySelectorAll(".calendar-day").forEach(c => c.classList.remove("selected"));
                selectedDateStr = dateStr;
                dayCell.classList.add("selected");
                todayPanelTitle.textContent = `Appointments on ${formatDateDisplay(dateStr)}`;
            }
            renderDayPanel();
            renderTable();
        });
        calendarGrid.appendChild(dayCell);
    }
}

function renderDayPanel() {
    todayAppointmentsUl.innerHTML = "";
    const todayStr = new Date().toISOString().split("T")[0];
    const targetDate = selectedDateStr || todayStr;
    const dayAppointments = appointments.filter(a => a.date === targetDate);

    if (dayAppointments.length === 0) {
        const li = document.createElement("li");
        li.className = "no-appointments";
        li.textContent = "No appointments scheduled.";
        todayAppointmentsUl.appendChild(li);
        return;
    }

    [...dayAppointments].sort((a, b) => a.time.localeCompare(b.time)).forEach(a => {
        const li = document.createElement("li");
        li.className = "appointment-item";
        li.innerHTML = `
            <div class="item-left">
                <span class="item-title">${a.client}</span>
                <span class="item-subtitle">${a.bonsai} (${a.id})</span>
            </div>
            <div class="item-right">
                <span class="item-time"><i class="fa-regular fa-clock"></i> ${a.time}</span>
                <span class="status-badge ${a.status.toLowerCase()}">${a.status}</span>
            </div>
        `;
        li.addEventListener("dblclick", () => openEditModal(a.id));
        todayAppointmentsUl.appendChild(li);
    });
}

function renderTable() {
    appointmentTableBody.innerHTML = "";
    const searchText = searchInput.value.toLowerCase().trim();
    const filterVal = statusFilter.value;

    const filtered = appointments.filter(a => {
        const matchesSearch = a.id.toLowerCase().includes(searchText) || a.client.toLowerCase().includes(searchText) || a.bonsai.toLowerCase().includes(searchText);
        const matchesStatus = filterVal === "ALL" || a.status === filterVal;
        const matchesDate = !selectedDateStr || a.date === selectedDateStr;
        return matchesSearch && matchesStatus && matchesDate;
    });

    if (filtered.length === 0) {
        appointmentTableBody.innerHTML = `<tr><td colspan="7" style="text-align: center; color: var(--text-secondary); padding: 3rem 0;">No matching records found.</td></tr>`;
        return;
    }

    filtered.forEach(a => {
        const tr = document.createElement("tr");
        tr.innerHTML = `
            <td><strong>${a.id}</strong></td>
            <td>${a.client}</td>
            <td>${a.bonsai}</td>
            <td>${formatDateDisplay(a.date)}</td>
            <td>${a.time}</td>
            <td><span class="status-badge ${a.status.toLowerCase()}">${a.status}</span></td>
            <td class="table-actions">
                <button class="edit-btn" data-id="${a.id}"><i class="fa-solid fa-pen"></i></button>
            </td>
        `;
        tr.querySelector(".edit-btn").addEventListener("click", (e) => { e.stopPropagation(); openEditModal(a.id); });
        tr.addEventListener("dblclick", () => openEditModal(a.id));
        appointmentTableBody.appendChild(tr);
    });
}

function openEditModal(id) {
    const appointment = appointments.find(a => a.id == id);
    if (!appointment) return;
    const statusMessage = document.getElementById("statusMessage");
    const rejectReasonGroup = document.getElementById("rejectReasonGroup");
    document.getElementById("rejectReason").value = "";

    if (appointment.status !== "PENDING") {
        statusSelect.disabled = true;
        saveStatusBtn.disabled = true;
        statusMessage.textContent = `This appointment has already been processed (${appointment.status}).`;
    } else {
        statusSelect.disabled = false;
        saveStatusBtn.disabled = false;
        statusMessage.textContent = "";
    }

    editingAppointmentId = id;
    infoId.textContent = appointment.id;
    infoClient.textContent = appointment.client;
    infoBonsai.textContent = appointment.bonsai;
    infoDate.textContent = formatDateDisplay(appointment.date);
    infoTime.textContent = appointment.time;
    infoPhone.textContent = appointment.phone;
    infoEmail.textContent = appointment.email;
    infoNote.textContent = appointment.note;
    statusSelect.value = appointment.status;

    rejectReasonGroup.style.display = statusSelect.value === "REJECTED" ? "block" : "none";
    editModal.classList.add("show");
}

function closeEditModal() { editModal.classList.remove("show"); editingAppointmentId = null; }

function saveStatusChange() {
    if (!editingAppointmentId) return;
    const form = document.getElementById("updateStatusForm");
    form.action = `/artisan/appointments/update/${editingAppointmentId}/status`;
    document.getElementById("statusInput").value = statusSelect.value;
    document.getElementById("messageInput").value = statusSelect.value === "REJECTED" ? document.getElementById("rejectReason").value : "";
    form.submit();
}

// BỔ SUNG: Xử lý bật tắt Popup thông báo toàn hệ thống
if(notificationBtn) {
    notificationBtn.addEventListener("click", (e) => {
        e.stopPropagation();
        notificationPopup.classList.toggle("show");
    });
    document.addEventListener("click", (e) => {
        if (!notificationPopup.contains(e.target) && e.target !== notificationBtn) {
            notificationPopup.classList.remove("show");
        }
    });
}

prevMonthBtn.addEventListener("click", () => { currentMonth--; if (currentMonth < 0) { currentMonth = 11; currentYear--; } renderCalendar(); });
nextMonthBtn.addEventListener("click", () => { currentMonth++; if (currentMonth > 11) { currentMonth = 0; currentYear++; } renderCalendar(); });
statusSelect.addEventListener("change", () => {
    document.getElementById("rejectReasonGroup").style.display = statusSelect.value === "REJECTED" ? "block" : "none";
});
searchInput.addEventListener("input", renderTable);
statusFilter.addEventListener("change", renderTable);
saveStatusBtn.addEventListener("click", saveStatusChange);
closeModalBtn.addEventListener("click", closeEditModal);
editModal.addEventListener("click", (e) => { if (e.target === editModal) closeEditModal(); });
document.addEventListener("keydown", (e) => { if (e.key === "Escape" && editModal.classList.contains("show")) closeEditModal(); });

function initApp() {
    extractAppointmentsFromDOM();
    renderStats();
    renderCalendar();
    renderDayPanel();
    renderTable();
}
document.addEventListener("DOMContentLoaded", initApp);