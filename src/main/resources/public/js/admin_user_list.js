document.addEventListener('DOMContentLoaded', function () {
    const searchInput = document.getElementById('searchEmail');
    const tableRows = document.querySelectorAll('#userTableBody tr[data-email]');
    const noResultRow = document.getElementById('noResultRow');

    searchInput.addEventListener('input', function () {
        const keyword = this.value.trim().toLowerCase();
        let visibleCount = 0;

        tableRows.forEach(row => {
            const email = row.getAttribute('data-email').toLowerCase();

            if (email.includes(keyword)) {
                row.style.display = '';
                visibleCount++;
            } else {
                row.style.display = 'none';
            }
        });

        // Hiện thông báo "không tìm thấy" nếu không có kết quả nào
        if (visibleCount === 0) {
            noResultRow.style.display = '';
        } else {
            noResultRow.style.display = 'none';
        }
    });

    // Bấm nút X để xóa ô tìm kiếm
    const clearBtn = document.getElementById('clearSearchBtn');
    clearBtn.addEventListener('click', function () {
        searchInput.value = '';
        searchInput.dispatchEvent(new Event('input'));
        searchInput.focus();
    });
});

const rows = document.querySelectorAll("#userTableBody tr");

const rowsPerPage = 10;
let currentPage = 1;

function displayTable(page) {

    rows.forEach(row => row.style.display = "none");

    const start = (page - 1) * rowsPerPage;
    const end = start + rowsPerPage;

    for (let i = start; i < end && i < rows.length; i++) {
        rows[i].style.display = "";
    }

    document.getElementById("pageInfo").innerText =
        `Trang ${page}/${Math.ceil(rows.length / rowsPerPage)}`;
}

document.getElementById("prevBtn").onclick = () => {
    if (currentPage > 1) {
        currentPage--;
        displayTable(currentPage);
    }
};

document.getElementById("nextBtn").onclick = () => {
    if (currentPage < Math.ceil(rows.length / rowsPerPage)) {
        currentPage++;
        displayTable(currentPage);
    }
};

displayTable(currentPage);
