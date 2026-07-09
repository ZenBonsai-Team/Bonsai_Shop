/**
 * Reusable Confirmation Dialog Modal Component
 * Under: src/main/resources/public/js/components/confirm-dialog.js
 */

const ConfirmDialog = {
    modalEl: null,
    titleEl: null,
    messageEl: null,
    summaryEl: null,
    btnCancel: null,
    btnConfirm: null,
    onConfirmCallback: null,

    /**
     * Initializes elements mapping
     */
    init() {
        this.modalEl = document.getElementById('confirmDialog');
        this.titleEl = this.modalEl.querySelector('.modal-title');
        this.messageEl = this.modalEl.querySelector('.modal-message');
        this.summaryEl = document.getElementById('confirmSummary');
        this.btnCancel = document.getElementById('btnConfirmCancel');
        this.btnConfirm = document.getElementById('btnConfirmOk');

        // Bind events
        this.btnCancel.addEventListener('click', () => this.hide());
        this.modalEl.addEventListener('click', (e) => {
            if (e.target === this.modalEl) this.hide();
        });
        
        this.btnConfirm.addEventListener('click', () => {
            if (this.onConfirmCallback) {
                this.onConfirmCallback();
            }
            this.hide();
        });
    },

    /**
     * Triggers the confirmation dialog modal
     * 
     * @param {Object} options Parameters for the dialog
     * @param {string} options.title Heading of the modal
     * @param {string} options.message Sub-text detail message
     * @param {Array<{label: string, value: string}>} options.summary List of label-value pairs for the summary card
     * @param {Function} options.onConfirm Callback on positive confirmation click
     */
    show({ title, message, summary = [], onConfirm }) {
        if (!this.modalEl) this.init();

        this.titleEl.textContent = title;
        this.messageEl.textContent = message;
        this.onConfirmCallback = onConfirm;

        // Render summary items
        this.summaryEl.innerHTML = '';
        if (summary.length > 0) {
            this.summaryEl.style.display = 'flex';
            summary.forEach(item => {
                const row = document.createElement('div');
                row.className = 'summary-row';
                
                const lbl = document.createElement('span');
                lbl.className = 'summary-label';
                lbl.textContent = item.label;
                
                const val = document.createElement('span');
                val.className = 'summary-val';
                val.textContent = item.value;
                
                row.appendChild(lbl);
                row.appendChild(val);
                this.summaryEl.appendChild(row);
            });
        } else {
            this.summaryEl.style.display = 'none';
        }

        // Show backdrop
        this.modalEl.classList.add('show');
    },

    /**
     * Hides the confirmation dialog
     */
    hide() {
        if (this.modalEl) {
            this.modalEl.classList.remove('show');
        }
        this.onConfirmCallback = null;
    }
};
