// ==========================================================================
// STATE MANAGEMENT & MOCK DATA GENERATOR
// ==========================================================================
// Current date state
let currentYear = 2026;
let currentMonth = 6; // July (0-indexed, so 6 is July)
let selectedDateStr = ""; // YYYY-MM-DD format
let editingAppointmentId = null;
// Mock list of appointments
const appointments = [];
// Helper functions for random items
const randomItem = (arr) => arr[Math.floor(Math.random() * arr.length)];
const padZero = (num) => String(num).padStart(2, '0');
function generateMockData() {
    const firstNames = ["Nguyễn", "Trần", "Lê", "Phạm", "Hoàng", "Huỳnh", "Phan", "Vũ", "Võ", "Đặng"];
    const middleNames = ["Văn", "Thị", "Minh", "Hữu", "Khánh", "Anh", "Đức", "Hồng", "Tuấn", "Thanh"];
    const lastNames = ["Anh", "Bình", "Cường", "Duy", "Hải", "Giang", "Hương", "Khánh", "Linh", "Minh", "Nam", "Phong", "Quỳnh", "Sơn", "Trang", "Vy"];
    const bonsais = ["Tùng La Hán", "Mai Chiếu Thủy", "Sanh Nam Điền", "Đỗ Quyên Nhật Bản", "Nguyệt Quế Cổ Thụ", "Khế Kiểng Dáng Huyền", "Lộc Vừng Bonsai"];

    // Generate exactly 128 appointments to match the dashboard statistics.
    // We want 6 on "2026-07-08" (Today).
    // Total: 128. Approved: 102. Pending: 26.
    let approvedCount = 0;
    let pendingCount = 0;

    // 1. Generate 6 appointments for Today (July 8, 2026)
    const todayStr = "2026-07-08";
    const todayTimes = ["08:30", "09:00", "10:30", "14:00", "15:30", "16:00"];

    for (let i = 0; i < 6; i++) {
        // Let's make 4 approved and 2 pending on today
        const status = i < 4 ? "APPROVED" : "PENDING";
        if (status === "APPROVED") approvedCount++;
        else pendingCount++;

        appointments.push({
            id: `AP${padZero(i + 1)}`,
            client: `${randomItem(firstNames)} ${randomItem(middleNames)} ${randomItem(lastNames)}`,
            bonsai: bonsais[i % bonsais.length],
            date: todayStr,
            time: todayTimes[i],
            status: status
        });
    }

    // 2. Generate remaining 122 appointments spread across June, July, and August 2026
    let idCounter = 7;
    const startNum = 128;

    // Generate dates helper
    const getRandDate = () => {
        // Generate dates between June 1st (2026-06-01) and Aug 31st (2026-08-31)
        const months = [5, 6, 7]; // June, July, August
        const m = randomItem(months);
        let d = Math.floor(Math.random() * 30) + 1;
        if (m === 6 && d > 31) d = 31; // July has 31 days
        if (m === 7 && d > 31) d = 31; // August has 31 days

        const dateStr = `2026-${padZero(m + 1)}-${padZero(d)}`;
        // Avoid adding more on July 8 to keep the "6 today" statistic clean
        if (dateStr === todayStr) {
            return "2026-07-09";
        }
        return dateStr;
    };

    while (idCounter <= startNum) {
        // Distribute statuses to hit exactly 102 Approved and 26 Pending
        let status = "APPROVED";
        if (pendingCount < 26 && (approvedCount >= 102 || Math.random() < 0.2)) {
            status = "PENDING";
            pendingCount++;
        } else {
            approvedCount++;
        }

        const h = padZero(Math.floor(Math.random() * 9) + 8); // 08:00 - 16:00
        const min = randomItem(["00", "15", "30", "45"]);

        appointments.push({
            id: `AP${String(idCounter).padStart(3, '0')}`,
            client: `${randomItem(firstNames)} ${randomItem(middleNames)} ${randomItem(lastNames)}`,
            bonsai: randomItem(bonsais),
            date: getRandDate(),
            time: `${h}:${min}`,
            status: status
        });
        idCounter++;
    }
    // Sort appointments: Date descending, Time ascending
    sortAppointments();
}
function sortAppointments() {
    appointments.sort((a, b) => {
        if (a.date !== b.date) {
            return b.date.localeCompare(a.date); // Latest date first
        }
        return a.time.localeCompare(b.time); // Earliest time first
    });
}
// Convert YYYY-MM-DD -> DD/MM/YYYY
function formatDateDisplay(dateStr) {
    const [year, month, day] = dateStr.split('-');
    return `${day}/${month}/${year}`;
}
// Convert DD/MM/YYYY -> YYYY-MM-DD
function parseDateInput(displayStr) {
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
// COMPONENT RENDERERS
// ==========================================================================
// 1. Update Statistics
function renderStats() {
    const total = appointments.length;
    const todayStr = "2026-07-08";
    const today = appointments.filter(a => a.date === todayStr).length;
    const approved = appointments.filter(a => a.status === "APPROVED").length;
    const pending = appointments.filter(a => a.status === "PENDING").length;
    const cancelled = appointments.filter(a => a.status === "CANCELLED").length;

    statTotal.textContent = total;
    statToday.textContent = today;
    statApproved.textContent = approved;
    statPending.textContent = pending;

    // Percentage comparisons (pretend last week's baseline was 114)
    totalPercent.textContent = `+${Math.round(((total - 114) / 114) * 100)}%`;

    // Percentage ratios
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

    // First day of the month
    const firstDay = new Date(currentYear, currentMonth, 1);
    // getDay() returns 0 for Sunday, 1 for Monday...
    // We want Monday to be 0, Tuesday to be 1... Sunday to be 6
    let startDayIdx = firstDay.getDay() - 1;
    if (startDayIdx < 0) startDayIdx = 6; // Sunday becomes 6

    // Total days in the month
    const totalDays = new Date(currentYear, currentMonth + 1, 0).getDate();

    // Empty cells before the 1st
    for (let i = 0; i < startDayIdx; i++) {
        const emptyCell = document.createElement("div");
        emptyCell.className = "calendar-day empty";
        calendarGrid.appendChild(emptyCell);
    }

    // Create calendar days
    for (let day = 1; day <= totalDays; day++) {
        const dayCell = document.createElement("div");
        dayCell.className = "calendar-day current-month-day";
        dayCell.textContent = day;

        const dateStr = `${currentYear}-${padZero(currentMonth + 1)}-${padZero(day)}`;

        // Mark today
        if (dateStr === "2026-07-08") {
            dayCell.classList.add("today");
        }

        // Highlight selected day
        if (dateStr === selectedDateStr) {
            dayCell.classList.add("selected");
        }

        // Check if there are appointments on this date
        const hasAppointments = appointments.some(a => a.date === dateStr);
        if (hasAppointments) {
            const dot = document.createElement("span");
            dot.className = "appointment-dot";
            dayCell.appendChild(dot);
        }

        // Add click event
        dayCell.addEventListener("click", () => {
            if (selectedDateStr === dateStr) {
                // Deselect if clicked again
                selectedDateStr = "";
                dayCell.classList.remove("selected");
                todayPanelTitle.textContent = "Today's Appointments";
            } else {
                // Select new date
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
// 3. Render Today's / Selected Date's Appointments List
function renderDayPanel() {
    todayAppointmentsUl.innerHTML = "";

    // Default to today (July 8, 2026) if no date is explicitly selected in state
    const targetDate = selectedDateStr || "2026-07-08";
    const dayAppointments = appointments.filter(a => a.date === targetDate);

    if (dayAppointments.length === 0) {
        const li = document.createElement("li");
        li.className = "no-appointments";
        li.textContent = "No appointments scheduled for this day.";
        todayAppointmentsUl.appendChild(li);
        return;
    }

    // Sort chronological for display
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

        // Double click item to edit status
        li.addEventListener("dblclick", () => {
            openEditModal(a.id);
        });

        todayAppointmentsUl.appendChild(li);
    });
}
// 4. Render Table
function renderTable() {
    appointmentTableBody.innerHTML = "";

    const searchText = searchInput.value.toLowerCase().trim();
    const filterVal = statusFilter.value;

    // Filter the items
    const filtered = appointments.filter(a => {
        // Search filter (ID, Client, Bonsai)
        const matchesSearch = a.id.toLowerCase().includes(searchText) ||
            a.client.toLowerCase().includes(searchText) ||
            a.bonsai.toLowerCase().includes(searchText);

        // Status filter
        const matchesStatus = filterVal === "ALL" || a.status === filterVal;

        // Calendar date selection filter
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
                <button class="edit-btn" aria-label="Edit status of ${a.id}"><i class="fa-solid fa-pen"></i></button>
            </td>
        `;

        // Open modal on clicking edit icon
        const editBtn = tr.querySelector(".edit-btn");
        editBtn.addEventListener("click", (e) => {
            e.stopPropagation();
            openEditModal(a.id);
        });

        // Support double-clicking row to edit too
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
    const appointment = appointments.find(a => a.id === id);
    if (!appointment) return;

    editingAppointmentId = id;

    infoId.textContent = appointment.id;
    infoClient.textContent = appointment.client;
    infoBonsai.textContent = appointment.bonsai;
    infoDate.textContent = formatDateDisplay(appointment.date);
    infoTime.textContent = appointment.time;

    // Status can be APPROVED, PENDING, or CANCELLED.
    // Note: Option value in select is APPROVED, PENDING, CANCELLED
    statusSelect.value = appointment.status;

    editModal.classList.add("show");

    // Accessible focus management
    statusSelect.focus();
}
function closeEditModal() {
    editModal.classList.remove("show");
    editingAppointmentId = null;
}
function saveStatusChange() {
    if (!editingAppointmentId) return;

    const appointment = appointments.find(a => a.id === editingAppointmentId);
    if (appointment) {
        appointment.status = statusSelect.value;

        // Re-sort if status change triggers statistics updates
        sortAppointments();

        // Refresh UI components
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
// Calendar month buttons
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
// Search and filters
searchInput.addEventListener("input", renderTable);
statusFilter.addEventListener("change", renderTable);
// Modal action buttons
saveStatusBtn.addEventListener("click", saveStatusChange);
closeModalBtn.addEventListener("click", closeEditModal);
// Close modal when clicking outside of modal-content
editModal.addEventListener("click", (e) => {
    if (e.target === editModal) {
        closeEditModal();
    }
});
// Escape key to close modal
document.addEventListener("keydown", (e) => {
    if (e.key === "Escape" && editModal.classList.contains("show")) {
        closeEditModal();
    }
});
// App Initialization
function initApp() {
    generateMockData();
    renderStats();
    renderCalendar();
    renderDayPanel();
    renderTable();
}
// Start the application
initApp();