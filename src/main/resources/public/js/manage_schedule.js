// ==========================================================================
// STATE MANAGEMENT & INITIALIZATION
// ==========================================================================
let appointments = []; // Sẽ được lấy thực tế từ DOM (Thymeleaf)
let currentMonth = new Date().getMonth();
let currentYear = new Date().getFullYear();
let selectedDateStr = "";
let editingAppointmentId = null;

// Helper to pad zero
function padZero(num) {
    return num < 10 ? `0${num}` : num;
}

// Convert YYYY-MM-DD -> DD/MM/YYYY
function formatDateDisplay(dateStr) {
    if (!dateStr) return "";
    const [year, month, day] = dateStr.split('-');
    return `${day}/${month}/${year}`;
}

// Convert DD/MM/YYYY -> YYYY-MM-DD
function parseDateInput(displayStr) {
    if (!displayStr) return "";
    const [day, month, year] = displayStr.split('/');
    return `${year}-${month}-${day}`;
}

// ==========================================================================
// DOM ELEMENTS
// ==========================================================================
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

// ==========================================================================
// DATA EXTRACTION FROM DOM (THYMELEAF)
// ==========================================================================
function extractAppointmentsFromDOM() {
    appointments = [];
    const rows = appointmentTableBody.querySelectorAll("tr");

    rows.forEach(row => {
        const editBtn = row.querySelector(".edit-btn");
        if (!editBtn) return; // Bỏ qua dòng trống / header phụ nếu có

        const id = editBtn.getAttribute("data-id") || "";
        const client = row.cells[1] ? row.cells[1].textContent.trim() : "";
        const bonsai = row.cells[2] ? row.cells[2].textContent.trim() : "";

        // Date text format: DD/MM/YYYY -> Convert to YYYY-MM-DD
        const dateText = row.cells[3] ? row.cells[3].textContent.trim() : "";
        const date = parseDateInput(dateText);

        const time = row.cells[4] ? row.cells[4].textContent.trim() : "";
        const status = row.cells[5] ? row.cells[5].textContent.trim().toUpperCase() : "PENDING";

        appointments.push({ id, client, bonsai, date, time, status });
    });
}

// ==========================================================================
// COMPONENT RENDERERS
// ==========================================================================

// 1. Update Statistics
function renderStats() {
    const total = appointments.length;
    // Lấy ngày hôm nay theo chuẩn YYYY-MM-DD của hệ thống
    const todayStr = new Date().toISOString().split("T")[0];

    const today = appointments.filter(a => a.date === todayStr).length;
    const approved = appointments.filter(a => a.status === "APPROVED").length;
    const pending = appointments.filter(a => a.status === "PENDING").length;

    statTotal.textContent = total;
    statToday.textContent = today;
    statApproved.textContent = approved;
    statPending.textContent = pending;

    // Giả lập tỉ lệ tăng trưởng tổng quan
    totalPercent.textContent = total > 0 ? `+${Math.round((total / 100) * 10)}%` : "0%";

    // Tính toán phần trăm tỉ lệ
    const activeAppointments = approved + pending;
    if (activeAppointments > 0) {
        approvedPercent.textContent = `${Math.round((approved / activeAppointments) * 100)}%`;
        pendingPercent.textContent = `${Math.round((pending / activeAppointments) * 100)}%`;
    } else {
        approvedPercent.textContent = "0%";
        pendingPercent.textContent = "0%";
    }
}

// 2. Render Calendar
function renderCalendar() {
    const monthNames = [
        "January", "February", "March", "April", "May", "June",
        "July", "August", "September", "October", "November", "December"
    ];

    currentMonthSpan.textContent = `${monthNames[currentMonth]} ${currentYear}`;
    calendarGrid.innerHTML = "";

    // Ngày đầu tiên của tháng
    const firstDay = new Date(currentYear, currentMonth, 1);
    // Chuyển thứ sang định dạng Thứ 2 là 0 -> Chủ nhật là 6
    let startDayIdx = firstDay.getDay() - 1;
    if (startDayIdx < 0) startDayIdx = 6;

    // Tổng số ngày trong tháng
    const totalDays = new Date(currentYear, currentMonth + 1, 0).getDate();

    // Render các ô trống trước ngày mùng 1
    for (let i = 0; i < startDayIdx; i++) {
        const emptyCell = document.createElement("div");
        emptyCell.className = "calendar-day empty";
        calendarGrid.appendChild(emptyCell);
    }

    // Render các ngày trong tháng
    for (let day = 1; day <= totalDays; day++) {
        const dayCell = document.createElement("div");
        dayCell.className = "calendar-day current-month-day";
        dayCell.textContent = day;

        const dateStr = `${currentYear}-${padZero(currentMonth + 1)}-${padZero(day)}`;

        // Đánh dấu ngày hôm nay
        const today = new Date().toISOString().split("T")[0];
        if (dateStr === today) {
            dayCell.classList.add("today");
        }

        // Highlight ngày đang được chọn
        if (dateStr === selectedDateStr) {
            dayCell.classList.add("selected");
        }

        // Tạo dấu chấm xanh nếu có lịch hẹn vào ngày này
        const hasAppointments = appointments.some(a => a.date === dateStr);
        if (hasAppointments) {
            const dot = document.createElement("span");
            dot.className = "appointment-dot";
            dayCell.appendChild(dot);
        }

        // Sự kiện click chọn ngày
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

// 3. Render Panel Lịch Hẹn bên cạnh Calendar
function renderDayPanel() {
    todayAppointmentsUl.innerHTML = "";

    // Mặc định hiển thị lịch hôm nay nếu chưa chọn ngày cụ thể
    const todayStr = new Date().toISOString().split("T")[0];
    const targetDate = selectedDateStr || todayStr;

    const dayAppointments = appointments.filter(a => a.date === targetDate);

    if (dayAppointments.length === 0) {
        const li = document.createElement("li");
        li.className = "no-appointments";
        li.textContent = "No appointments scheduled for this day.";
        todayAppointmentsUl.appendChild(li);
        return;
    }

    // Sắp xếp thời gian tăng dần
    const sortedDayList = [...dayAppointments].sort((a, b) => a.time.localeCompare(b.time));

    sortedDayList.forEach(a => {
        const li = document.createElement("li");
        li.className = "appointment-item";

        let badgeClass = "pending";
        if (a.status === "APPROVED") badgeClass = "approved";
        if (a.status === "CANCELLED") badgeClass = "cancelled";

        li.innerHTML = `
            <div class="item-left">
                <span class="item-title">${a.client}</span>
                <span class="item-subtitle">${a.bonsai} (${a.id})</span>
            </div>
            <div class="item-right">
                <span class="item-time"><i class="fa-regular fa-clock"></i> ${a.time}</span>
                <span class="status-badge ${badgeClass}">${a.status}</span>
            </div>
        `;

        // Double click vào danh sách để đổi nhanh trạng thái
        li.addEventListener("dblclick", () => {
            openEditModal(a.id);
        });

        todayAppointmentsUl.appendChild(li);
    });
}

// 4. Render & Đồng bộ lại Bảng Danh Sách dưới
function renderTable() {
    appointmentTableBody.innerHTML = "";

    const searchText = searchInput.value.toLowerCase().trim();
    const filterVal = statusFilter.value;

    const filtered = appointments.filter(a => {
        const matchesSearch = a.id.toLowerCase().includes(searchText) ||
            a.client.toLowerCase().includes(searchText) ||
            a.bonsai.toLowerCase().includes(searchText);

        const matchesStatus = filterVal === "ALL" || a.status === filterVal;
        const matchesDate = !selectedDateStr || a.date === selectedDateStr;

        return matchesSearch && matchesStatus && matchesDate;
    });

    if (filtered.length === 0) {
        appointmentTableBody.innerHTML = `
            <tr>
                <td colspan="7" style="text-align: center; color: var(--text-secondary); font-style: italic; padding: 3rem 0;">
                    No appointments found matching your filters.
                </td>
            </tr>
        `;
        return;
    }

    filtered.forEach(a => {
        const tr = document.createElement("tr");

        let badgeClass = "pending";
        if (a.status === "APPROVED") badgeClass = "approved";
        if (a.status === "CANCELLED") badgeClass = "cancelled";

        tr.innerHTML = `
            <td><strong>${a.id}</strong></td>
            <td>${a.client}</td>
            <td>${a.bonsai}</td>
            <td>${formatDateDisplay(a.date)}</td>
            <td>${a.time}</td>
            <td><span class="status-badge ${badgeClass}">${a.status}</span></td>
            <td class="table-actions">
                <button class="edit-btn" data-id="${a.id}" aria-label="Edit status of ${a.id}"><i class="fa-solid fa-pen"></i></button>
            </td>
        `;

        const editBtn = tr.querySelector(".edit-btn");
        editBtn.addEventListener("click", (e) => {
            e.stopPropagation();
            openEditModal(a.id);
        });

        tr.addEventListener("dblclick", () => {
            openEditModal(a.id);
        });

        appointmentTableBody.appendChild(tr);
    });
}

// ==========================================================================
// MODAL INTERACTIONS
// ==========================================================================
function openEditModal(id) {
    const appointment = appointments.find(a => a.id == id);
    if (!appointment) return;

    editingAppointmentId = id;

    infoId.textContent = appointment.id;
    infoClient.textContent = appointment.client;
    infoBonsai.textContent = appointment.bonsai;
    infoDate.textContent = formatDateDisplay(appointment.date);
    infoTime.textContent = appointment.time;

    statusSelect.value = appointment.status;
    editModal.classList.add("show");
    statusSelect.focus();
}

function closeEditModal() {
    editModal.classList.remove("show");
    editingAppointmentId = null;
}

function saveStatusChange() {
    if (!editingAppointmentId) return;

    const appointment = appointments.find(a => a.id == editingAppointmentId);
    if (appointment) {
        appointment.status = statusSelect.value;

        // Gửi API Cập nhật trạng thái lên Server tại đây nếu cần thiết:
        // fetch(`/api/appointments/${editingAppointmentId}/status`, { ... })

        // Làm mới UI
        renderStats();
        renderCalendar();
        renderDayPanel();
        renderTable();
    }

    closeEditModal();
}

// ==========================================================================
// EVENT LISTENERS & INITIALIZATION
// ==========================================================================
prevMonthBtn.addEventListener("click", () => {
    currentMonth--;
    if (currentMonth < 0) {
        currentMonth = 11;
        currentYear--;
    }
    renderCalendar();
});

nextMonthBtn.addEventListener("click", () => {
    currentMonth++;
    if (currentMonth > 11) {
        currentMonth = 0;
        currentYear++;
    }
    renderCalendar();
});

searchInput.addEventListener("input", renderTable);
statusFilter.addEventListener("change", renderTable);
saveStatusBtn.addEventListener("click", saveStatusChange);
closeModalBtn.addEventListener("click", closeEditModal);

editModal.addEventListener("click", (e) => {
    if (e.target === editModal) closeEditModal();
});

document.addEventListener("keydown", (e) => {
    if (e.key === "Escape" && editModal.classList.contains("show")) {
        closeEditModal();
    }
});

// App Initialization
function initApp() {
    extractAppointmentsFromDOM(); // Đọc dữ liệu thực tế từ Thymeleaf
    renderStats();
    renderCalendar();
    renderDayPanel();
    renderTable(); // Vẽ lại bảng để gắn đúng Event Listener động
}

// Chạy ứng dụng khi DOM tải xong
document.addEventListener("DOMContentLoaded", initApp);