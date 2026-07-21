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
