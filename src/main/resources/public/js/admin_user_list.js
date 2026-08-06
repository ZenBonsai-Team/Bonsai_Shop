document.addEventListener('DOMContentLoaded', function () {
    const searchInput = document.getElementById('searchEmail');
    const clearBtn = document.getElementById('clearSearchBtn');
    const tableRows = Array.from(document.querySelectorAll('#userTableBody tr[data-email]'));
    const noResultRow = document.getElementById('noResultRow');
    
    const rowsPerPage = 10;
    let currentPage = 1;
    let filteredRows = [...tableRows];

    // Function to display table rows for the current page
    function displayTable() {
        // Hide all rows first
        tableRows.forEach(row => row.style.display = 'none');
        noResultRow.style.display = 'none';

        if (filteredRows.length === 0) {
            noResultRow.style.display = '';
            document.getElementById("pageInfo").innerText = "Trang 0/0";
            document.getElementById("prevBtn").classList.add("disabled");
            document.getElementById("nextBtn").classList.add("disabled");
            return;
        }

        // Calculate paging indices
        const totalPages = Math.ceil(filteredRows.length / rowsPerPage);
        if (currentPage > totalPages) {
            currentPage = totalPages || 1;
        }

        const start = (currentPage - 1) * rowsPerPage;
        const end = start + rowsPerPage;

        // Show only rows for current page
        for (let i = start; i < end && i < filteredRows.length; i++) {
            filteredRows[i].style.display = '';
        }

        // Update page info
        document.getElementById("pageInfo").innerText = `Trang ${currentPage}/${totalPages}`;

        // Toggle disabled states of buttons
        if (currentPage <= 1) {
            document.getElementById("prevBtn").classList.add("disabled");
            document.getElementById("prevBtn").setAttribute("disabled", "true");
        } else {
            document.getElementById("prevBtn").classList.remove("disabled");
            document.getElementById("prevBtn").removeAttribute("disabled");
        }

        if (currentPage >= totalPages) {
            document.getElementById("nextBtn").classList.add("disabled");
            document.getElementById("nextBtn").setAttribute("disabled", "true");
        } else {
            document.getElementById("nextBtn").classList.remove("disabled");
            document.getElementById("nextBtn").removeAttribute("disabled");
        }
    }

    // Input event listener for search box
    searchInput.addEventListener('input', function () {
        const keyword = this.value.trim().toLowerCase();
        
        // Show/hide X clear button
        if (keyword.length > 0) {
            clearBtn.style.display = 'block';
        } else {
            clearBtn.style.display = 'none';
        }

        // Filter rows matching keyword
        filteredRows = tableRows.filter(row => {
            const email = row.getAttribute('data-email').toLowerCase();
            return email.includes(keyword);
        });

        // Reset page to 1 on new filter and display
        currentPage = 1;
        displayTable();
    });

    // Clear search button handler
    clearBtn.addEventListener('click', function () {
        searchInput.value = '';
        clearBtn.style.display = 'none';
        filteredRows = [...tableRows];
        currentPage = 1;
        displayTable();
        searchInput.focus();
    });

    // Pager controls
    document.getElementById("prevBtn").onclick = () => {
        if (currentPage > 1) {
            currentPage--;
            displayTable();
        }
    };

    document.getElementById("nextBtn").onclick = () => {
        const totalPages = Math.ceil(filteredRows.length / rowsPerPage);
        if (currentPage < totalPages) {
            currentPage++;
            displayTable();
        }
    };

    // Initial display
    displayTable();
});
