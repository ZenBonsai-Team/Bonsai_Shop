let currentDate = new Date();
let selectedDate = null;

const appointments = [
    {id:"AP001", client:"Nguyễn Văn A", bonsai:"Tùng La Hán", date:"2026-07-08", time:"09:00", status:"PENDING"},
    {id:"AP002", client:"Trần Thị B", bonsai:"Mai Chiếu Thủy", date:"2026-07-08", time:"11:00", status:"APPROVED"},
    {id:"AP003", client:"Lê Văn C", bonsai:"Samurai Pine", date:"2026-07-08", time:"14:00", status:"PENDING"},
    {id:"AP004", client:"Phạm Văn D", bonsai:"Kim Quýt", date:"2026-07-09", time:"10:00", status:"CANCELLED"},
];

function renderCalendar(date) {
    const calendarGrid = document.querySelector(".calendar-grid");
    const currentMonthSpan = document.querySelector(".current-month");

    const year = date.getFullYear();
    const month = date.getMonth();

    const monthNames = ["January","February","March","April","May","June",
        "July","August","September","October","November","December"];
    currentMonthSpan.textContent = `${monthNames[month]} ${year}`;

    const daysInMonth = new Date(year, month+1, 0).getDate();
    const startDay = new Date(year, month, 1).getDay();

    calendarGrid.innerHTML = "";

    let blanks = (startDay === 0 ? 6 : startDay - 1);
    for (let i=0;i<blanks;i++) calendarGrid.appendChild(document.createElement("div"));

    const today = new Date();
    for (let d=1; d<=daysInMonth; d++) {
        const dayCell = document.createElement("div");
        dayCell.classList.add("day");
        dayCell.textContent = d;

        // ngày hôm nay: border cam + auto selected
        if (d===today.getDate() && month===today.getMonth() && year===today.getFullYear()) {
            dayCell.classList.add("today", "selected");
            selectedDate = `${year}-${String(month+1).padStart(2,"0")}-${String(d).padStart(2,"0")}`;
        }


        // click chọn ngày
        dayCell.addEventListener("click", () => {
            selectedDate = `${year}-${String(month+1).padStart(2,"0")}-${String(d).padStart(2,"0")}`;
            // reset selected cho tất cả
            document.querySelectorAll(".day").forEach(el=>el.classList.remove("selected"));
            // chỉ thêm selected cho ngày vừa click
            dayCell.classList.add("selected");
            renderAppointmentsForDate(selectedDate);
        });

        calendarGrid.appendChild(dayCell);
    }
}

function renderAppointmentsForDate(dateStr) {
    const list = document.getElementById("todayAppointments");
    list.innerHTML = "";
    const filtered = appointments.filter(a=>a.date===dateStr);
    if (filtered.length===0) {
        const li = document.createElement("li");
        li.classList.add("empty");
        li.textContent = "Không có lịch hẹn";
        list.appendChild(li);
        return;
    }
    filtered.forEach(a=>{
        const li = document.createElement("li");
        li.innerHTML = `
      <span class="time">${a.time}</span>
      <span class="client">${a.client}</span>
      <span class="bonsai">${a.bonsai}</span>
      <span class="status ${a.status.toLowerCase()}">${a.status}</span>
    `;
        list.appendChild(li);
    });
}


document.getElementById("prevMonth").addEventListener("click", () => {
    currentDate.setMonth(currentDate.getMonth() - 1);
    renderCalendar(currentDate);
});

document.getElementById("nextMonth").addEventListener("click", () => {
    currentDate.setMonth(currentDate.getMonth() + 1);
    renderCalendar(currentDate);
});

document.addEventListener("DOMContentLoaded", () => {
    renderCalendar(currentDate);
    const todayStr = new Date().toISOString().split("T")[0];
    renderAppointmentsForDate(todayStr);
});

// mở modal khi bấm edit
document.addEventListener("DOMContentLoaded", () => {
    document.querySelectorAll(".edit-btn").forEach(btn=>{
        btn.addEventListener("click", ()=>{
            document.getElementById("editModal").style.display = "flex";
            // lưu tr id đang edit
            const row = btn.closest("tr");
            row.dataset.editing = "true";
        });
    });

    // đóng modal
    document.getElementById("closeModal").addEventListener("click", ()=>{
        document.getElementById("editModal").style.display = "none";
        document.querySelectorAll("tr[data-editing]").forEach(r=>r.removeAttribute("data-editing"));
    });

    // lưu status
    document.getElementById("saveStatus").addEventListener("click", ()=>{
        const newStatus = document.getElementById("statusSelect").value;
        const row = document.querySelector("tr[data-editing='true']");
        if(row){
            const statusCell = row.querySelector("td:nth-child(6)");
            statusCell.innerHTML = `<span class="status ${newStatus.toLowerCase()}">${newStatus}</span>`;
            row.removeAttribute("data-editing");
        }
        document.getElementById("editModal").style.display = "none";
    });
});

