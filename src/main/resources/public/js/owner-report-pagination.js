(function () {
    const DEFAULT_PAGE_SIZE = 10;

    function createPageButton(label, page, currentPage, totalPages, onClick) {
        const button = document.createElement('button');
        button.type = 'button';
        button.className = 'page-btn';
        button.textContent = label;

        const isDisabled = page < 1 || page > totalPages || page === currentPage;
        if (page === currentPage && /^\d+$/.test(String(label))) {
            button.classList.add('active');
            button.setAttribute('aria-current', 'page');
        }
        if (isDisabled) {
            button.disabled = true;
        }

        button.addEventListener('click', () => onClick(page));
        return button;
    }

    function setupPagination(tableBody) {
        const entries = Array.from(tableBody.querySelectorAll('[data-page-entry]'));
        const pageSize = Number.parseInt(tableBody.dataset.pageSize, 10) || DEFAULT_PAGE_SIZE;
        const totalPages = Math.ceil(entries.length / pageSize);
        const reportLabel = tableBody.dataset.reportLabel || 'entries';
        const panel = tableBody.closest('.owner-report-panel');
        const pagination = panel ? panel.querySelector('[data-owner-pagination]') : null;
        const info = pagination ? pagination.querySelector('[data-owner-pagination-info]') : null;
        const controls = pagination ? pagination.querySelector('[data-owner-pagination-controls]') : null;

        if (!pagination || !info || !controls || totalPages <= 1) {
            if (pagination) {
                pagination.hidden = true;
            }
            return;
        }

        let currentPage = 1;

        function renderPage(page) {
            currentPage = Math.min(Math.max(page, 1), totalPages);
            const startIndex = (currentPage - 1) * pageSize;
            const endIndex = Math.min(startIndex + pageSize, entries.length);

            entries.forEach((entry, index) => {
                entry.hidden = index < startIndex || index >= endIndex;
            });

            info.textContent = `Showing ${startIndex + 1} - ${endIndex} of ${entries.length} ${reportLabel}`;
            controls.innerHTML = '';
            controls.appendChild(createPageButton('Previous', currentPage - 1, currentPage, totalPages, renderPage));

            for (let pageNumber = 1; pageNumber <= totalPages; pageNumber += 1) {
                controls.appendChild(createPageButton(String(pageNumber), pageNumber, currentPage, totalPages, renderPage));
            }

            controls.appendChild(createPageButton('Next', currentPage + 1, currentPage, totalPages, renderPage));
            pagination.hidden = false;
        }

        renderPage(1);
    }

    document.addEventListener('DOMContentLoaded', () => {
        document.querySelectorAll('[data-owner-paginated-list]').forEach(setupPagination);
    });
}());
