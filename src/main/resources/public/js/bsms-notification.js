/**
 * ========================================================
 * BSMS GLOBAL NOTIFICATION SERVICE (Toast & Confirm Modal)
 * ========================================================
 */

(function () {
    'use strict';

    // Ensure Toast Container exists
    function getOrCreateToastContainer() {
        let container = document.getElementById('bsms-toast-container');
        if (!container) {
            container = document.createElement('div');
            container.id = 'bsms-toast-container';
            document.body.appendChild(container);
        }
        return container;
    }

    /**
     * BSMSToast Service
     */
    const BSMSToast = {
        show: function (options) {
            const type = options.type || 'info'; // 'success' | 'error' | 'warning' | 'info'
            const title = options.title || this.getDefaultTitle(type);
            const message = options.message || '';
            const duration = options.duration || 3500; // ms

            const container = getOrCreateToastContainer();

            // Create toast element
            const toast = document.createElement('div');
            toast.className = `bsms-toast bsms-toast-${type}`;

            // Icons mapping
            const iconMap = {
                success: 'fa-circle-check',
                error: 'fa-circle-xmark',
                warning: 'fa-triangle-exclamation',
                info: 'fa-circle-info'
            };
            const iconClass = iconMap[type] || 'fa-bell';

            toast.innerHTML = `
                <div class="bsms-toast-icon">
                    <i class="fa-solid ${iconClass}"></i>
                </div>
                <div class="bsms-toast-body">
                    <div class="bsms-toast-title">${title}</div>
                    <div class="bsms-toast-message">${message}</div>
                </div>
                <button class="bsms-toast-close" aria-label="Close">&times;</button>
                <div class="bsms-toast-progress">
                    <div class="bsms-toast-progress-fill"></div>
                </div>
            `;

            container.appendChild(toast);

            // Animate progress bar
            const progressFill = toast.querySelector('.bsms-toast-progress-fill');
            if (progressFill) {
                progressFill.style.transitionDuration = `${duration}ms`;
                setTimeout(() => {
                    progressFill.style.width = '0%';
                }, 10);
            }

            // Close handler
            const closeBtn = toast.querySelector('.bsms-toast-close');
            let dismissTimer = setTimeout(() => {
                this.dismiss(toast);
            }, duration);

            if (closeBtn) {
                closeBtn.addEventListener('click', () => {
                    clearTimeout(dismissTimer);
                    this.dismiss(toast);
                });
            }
        },

        dismiss: function (toast) {
            if (!toast || toast.classList.contains('bsms-toast-hiding')) return;
            toast.classList.add('bsms-toast-hiding');
            toast.addEventListener('animationend', () => {
                if (toast.parentNode) {
                    toast.parentNode.removeChild(toast);
                }
            });
        },

        getDefaultTitle: function (type) {
            switch (type) {
                case 'success': return 'Thành công';
                case 'error': return 'Báo lỗi';
                case 'warning': return 'Cảnh báo';
                case 'info': default: return 'Thông báo';
            }
        },

        success: function (message, title) {
            this.show({ type: 'success', title: title || 'Thành công', message });
        },
        error: function (message, title) {
            this.show({ type: 'error', title: title || 'Lỗi hệ thống', message });
        },
        warning: function (message, title) {
            this.show({ type: 'warning', title: title || 'Lưu ý', message });
        },
        info: function (message, title) {
            this.show({ type: 'info', title: title || 'Thông báo', message });
        }
    };

    /**
     * BSMSConfirm Modal Service
     */
    function BSMSConfirm(options) {
        return new Promise((resolve) => {
            const title = options.title || 'Xác nhận thao tác';
            const message = options.message || 'Bạn có chắc chắn muốn thực hiện thao tác này?';
            const type = options.type || 'warning'; // 'warning' | 'danger' | 'primary'
            const confirmText = options.confirmText || 'Xác nhận';
            const cancelText = options.cancelText || 'Hủy bỏ';

            // Create backdrop & modal HTML
            const backdrop = document.createElement('div');
            backdrop.className = 'bsms-modal-backdrop';

            const iconMap = {
                warning: 'fa-triangle-exclamation',
                danger: 'fa-circle-exclamation',
                primary: 'fa-circle-question'
            };
            const iconClass = iconMap[type] || 'fa-triangle-exclamation';

            backdrop.innerHTML = `
                <div class="bsms-modal">
                    <div class="bsms-modal-header">
                        <div class="bsms-modal-icon-wrapper ${type}">
                            <i class="fa-solid ${iconClass}"></i>
                        </div>
                        <h3 class="bsms-modal-title">${title}</h3>
                        <p class="bsms-modal-message">${message}</p>
                    </div>
                    <div class="bsms-modal-footer">
                        <button class="bsms-modal-btn bsms-modal-btn-cancel">${cancelText}</button>
                        <button class="bsms-modal-btn bsms-modal-btn-confirm ${type === 'danger' ? 'danger' : ''}">${confirmText}</button>
                    </div>
                </div>
            `;

            document.body.appendChild(backdrop);

            // Force reflow for fade in
            setTimeout(() => {
                backdrop.classList.add('bsms-modal-show');
            }, 10);

            const btnCancel = backdrop.querySelector('.bsms-modal-btn-cancel');
            const btnConfirm = backdrop.querySelector('.bsms-modal-btn-confirm');

            const closeModal = (result) => {
                backdrop.classList.remove('bsms-modal-show');
                setTimeout(() => {
                    if (backdrop.parentNode) {
                        backdrop.parentNode.removeChild(backdrop);
                    }
                    resolve(result);
                    if (result && typeof options.onConfirm === 'function') {
                        options.onConfirm();
                    } else if (!result && typeof options.onCancel === 'function') {
                        options.onCancel();
                    }
                }, 250);
            };

            btnCancel.addEventListener('click', () => closeModal(false));
            btnConfirm.addEventListener('click', () => closeModal(true));
            backdrop.addEventListener('click', (e) => {
                if (e.target === backdrop) closeModal(false);
            });
        });
    }

    // Override Global alert() with BSMSToast to safely catch any legacy alert() calls
    window.alert = function (message) {
        if (!message) return;
        const msgStr = String(message);
        if (msgStr.toLowerCase().includes('lỗi') || msgStr.toLowerCase().includes('error') || msgStr.toLowerCase().includes('thất bại')) {
            BSMSToast.error(msgStr);
        } else if (msgStr.toLowerCase().includes('thành công') || msgStr.toLowerCase().includes('success')) {
            BSMSToast.success(msgStr);
        } else if (msgStr.toLowerCase().includes('vui lòng') || msgStr.toLowerCase().includes('chưa') || msgStr.toLowerCase().includes('trống')) {
            BSMSToast.warning(msgStr);
        } else {
            BSMSToast.info(msgStr);
        }
    };

    // Expose to window
    window.BSMSToast = BSMSToast;
    window.BSMSConfirm = BSMSConfirm;
})();
