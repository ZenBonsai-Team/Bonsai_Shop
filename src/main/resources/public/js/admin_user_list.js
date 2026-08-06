document.addEventListener('DOMContentLoaded', function () {
    const searchInput = document.getElementById('searchEmail');
    const clearBtn = document.getElementById('clearSearchBtn');
    const filterRole = document.getElementById('filterRole');
    const filterStatus = document.getElementById('filterStatus');
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

    // Main filter function triggered on any input change
    function runFilters() {
        const keyword = searchInput.value.trim().toLowerCase();
        const selectedRole = filterRole.value;
        const selectedStatus = filterStatus.value;

        // Show/hide clear button
        if (keyword.length > 0) {
            clearBtn.style.display = 'block';
        } else {
            clearBtn.style.display = 'none';
        }

        // Filter rows based on all constraints
        filteredRows = tableRows.filter(row => {
            const email = row.getAttribute('data-email').toLowerCase();
            const role = row.getAttribute('data-role');
            const status = row.getAttribute('data-status');

            // Email matching
            const matchesEmail = email.includes(keyword);

            // Role matching (handle both OWNER and ROLE_OWNER formats)
            let matchesRole = true;
            if (selectedRole !== 'ALL') {
                const normRole = role.replace('ROLE_', '');
                const normSelRole = selectedRole.replace('ROLE_', '');
                matchesRole = (normRole === normSelRole);
            }

            // Status matching
            const matchesStatus = (selectedStatus === 'ALL' || status === selectedStatus);

            return matchesEmail && matchesRole && matchesStatus;
        });

        // Reset page to 1 on filter run and refresh view
        currentPage = 1;
        displayTable();
    }

    // Add change listeners
    searchInput.addEventListener('input', runFilters);
    filterRole.addEventListener('change', runFilters);
    filterStatus.addEventListener('change', runFilters);

    // Clear search handler
    clearBtn.addEventListener('click', function () {
        searchInput.value = '';
        clearBtn.style.display = 'none';
        runFilters();
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
