/* ==========================================================================
   Bonsai Luxury — Premium Appointment Interactions (Production Ready)
   ========================================================================== */
document.addEventListener('DOMContentLoaded', () => {

    /* ==========================================================================
       BẮT THÔNG BÁO TỪ CONTROLLER TRẢ VỀ VÀ HIỂN THỊ QUA TOAST LUXURY
       ========================================================================== */
    const carrierSuccess = document.getElementById('carrier-success');
    const carrierError = document.getElementById('carrier-error');

    if (carrierSuccess && carrierSuccess.textContent.trim() !== "") {
        showToast(carrierSuccess.textContent.trim(), 'success');
    }
    if (carrierError && carrierError.textContent.trim() !== "") {
        showToast(carrierError.textContent.trim(), 'danger');
    }

    /* 1. TỰ ĐỘNG CẬP NHẬT NĂM FOOTER */
    const yearEl = document.getElementById('year');
    if (yearEl) yearEl.textContent = new Date().getFullYear();

    /* 2. ĐIỀU KHIỂN DROPDOWN ACCOUNT PREMIUM */
    const userMenuContainer = document.querySelector('.user-menu-premium');
    const userBtn = document.querySelector('.user-btn-premium');

    if (userMenuContainer && userBtn) {
        userBtn.addEventListener('click', (e) => {
            e.stopPropagation();
            userMenuContainer.classList.toggle('active');
            const isExpanded = userMenuContainer.classList.contains('active');
            userBtn.setAttribute('aria-expanded', isExpanded);
        });

        document.addEventListener('click', (e) => {
            if (!userMenuContainer.contains(e.target)) {
                userMenuContainer.classList.remove('active');
                userBtn.setAttribute('aria-expanded', 'false');
            }
        });
    }

    /* 3. HIỆU ỨNG TOAST NOTIFICATION CAO CẤP */
    function showToast(text, type = 'info') {
        const existing = document.querySelector('.luxury-toast');
        if (existing) existing.remove();

        const toast = document.createElement('div');
        toast.className = `luxury-toast ${type}`;
        toast.textContent = text;

        Object.assign(toast.style, {
            position: 'fixed',
            bottom: '30px',
            right: '30px',
            background: type === 'danger' ? '#C0392B' : (type === 'success' ? '#113425' : '#1e221f'),
            color: '#FFFFFF',
            padding: '16px 32px',
            fontSize: '0.82rem',
            fontFamily: 'var(--font-sans)',
            letterSpacing: '1px',
            textTransform: 'uppercase',
            boxShadow: '0 20px 50px rgba(0,0,0,0.15)',
            zIndex: '9999',
            opacity: '0',
            transform: 'translateY(20px)',
            transition: 'all 0.5s cubic-bezier(0.16, 1, 0.3, 1)'
        });

        document.body.appendChild(toast);

        setTimeout(() => {
            toast.style.opacity = '1';
            toast.style.transform = 'translateY(0)';
        }, 50);

        setTimeout(() => {
            toast.style.opacity = '0';
            toast.style.transform = 'translateY(20px)';
            setTimeout(() => toast.remove(), 500);
        }, 4000);
    }

    /* 4. ĐIỀU KHIỂN MODAL HỦY LỊCH HẸN CAO CẤP */
    const cancelModal = document.getElementById('cancelAppointmentModal');
    const cancelForm = document.getElementById('cancelAppointmentForm');
    const cancelProductNameSpan = document.getElementById('cancelProductName');
    const triggerCancelButtons = document.querySelectorAll('.trigger-cancel-modal');
    const closeCancelElements = document.querySelectorAll('.id-close-cancel, .id-close-cancel-btn');

    const closeCancelModal = () => {
        if (cancelModal) {
            cancelModal.classList.remove('active');
        }
    };

    triggerCancelButtons.forEach(button => {
        button.addEventListener('click', function () {
            const id = this.getAttribute('data-id');
            const productName = this.getAttribute('data-product');

            if (cancelProductNameSpan) {
                cancelProductNameSpan.textContent = productName;
            }
            if (cancelForm) {
                cancelForm.setAttribute('action', `/appointments/cancel/${id}`);
            }
            if (cancelModal) {
                cancelModal.classList.add('active');
            }
        });
    });

    closeCancelElements.forEach(el => {
        el.addEventListener('click', closeCancelModal);
    });

    // Xử lý gửi yêu cầu hủy lịch hẹn
    if (cancelForm) {
        cancelForm.addEventListener('submit', function (e) {
            e.preventDefault();
            closeCancelModal();
            showToast("Đang xử lý yêu cầu hủy...", "info");

            const actionUrl = this.getAttribute('action');
            const formData = new FormData(this);

            fetch(actionUrl, {
                method: 'POST',
                body: formData
            })
                .then(response => {
                    if (response.ok) {
                        // Nếu Controller chuyển hướng (redirect), di chuyển trình duyệt đến URL đó
                        // Việc này giúp Spring Boot mang theo FlashAttribute hiển thị Toast thông báo thành công
                        if (response.redirected) {
                            window.location.href = response.url;
                        } else {
                            window.location.reload();
                        }
                    } else {
                        showToast("Có lỗi xảy ra khi hủy lịch hẹn!", "danger");
                    }
                })
                .catch(error => {
                    console.error(error);
                    showToast("Lỗi kết nối hệ thống!", "danger");
                });
        });
    }

    /* 5. VIEW APPOINTMENT DETAIL[cite: 1] */
    const modal = document.getElementById("appointmentModal");
    const closeBtnElements = document.querySelectorAll(".close-btn, .close-modal-btn");
    const detailButtons = document.querySelectorAll(".view-detail-btn");

    detailButtons.forEach(button => {
        button.addEventListener("click", function () {
            const id = this.dataset.id;
            showToast("Đang tải chi tiết...", "info");

            fetch(`/appointments/detail/${id}`)
                .then(response => response.json())
                .then(data => {
                    // Cập nhật thông tin chi tiết[cite: 1]
                    document.getElementById("detailName").textContent = data.productName;
                    document.getElementById("detailCode").textContent = data.productCode;
                    document.getElementById("detailStatus").textContent = data.status;
                    document.getElementById("detailNote").textContent = data.note ?? "Không có ghi chú riêng";

                    if (data.appointmentDate) {
                        const dateObj = new Date(data.appointmentDate);
                        if (!isNaN(dateObj.getTime())) {
                            const day = String(dateObj.getDate()).padStart(2, '0');
                            const month = String(dateObj.getMonth() + 1).padStart(2, '0');
                            const year = dateObj.getFullYear();
                            const hours = String(dateObj.getHours()).padStart(2, '0');
                            const minutes = String(dateObj.getMinutes()).padStart(2, '0');

                            document.getElementById("detailDate").textContent = `${day}/${month}/${year}`;
                            document.getElementById("detailTime").textContent = `${hours}:${minutes}`;
                        } else {
                            document.getElementById("detailDate").textContent = data.appointmentDate;
                            document.getElementById("detailTime").textContent = data.appointmentTime ?? "";
                        }
                    } else {
                        document.getElementById("detailDate").textContent = "";
                        document.getElementById("detailTime").textContent = "";
                    }

                    if (modal) {
                        modal.classList.add('active'); // Kích hoạt hiển thị[cite: 1]
                    }
                })
                .catch(error => {
                    console.error(error);
                    showToast("Không thể tải thông tin lịch hẹn!", "danger");
                });
        });
    });

    const closeModal = () => {
        if (modal) {
            modal.classList.remove('active');
        }
    };

    closeBtnElements.forEach(btn => {
        btn.addEventListener("click", closeModal);
    });

    /* ==========================================================================
       6. UPDATE APPOINTMENT MODAL INTERACTION
       ========================================================================== */
    const updateModal = document.getElementById("updateAppointmentModal");
    const updateForm = document.getElementById("updateAppointmentForm");
    const updateButtons = document.querySelectorAll(".update-btn");
    const closeUpdateElements = document.querySelectorAll(".id-close-update, .id-close-update-btn");

    updateButtons.forEach(button => {
        button.addEventListener("click", function () {
            const id = this.dataset.id;
            showToast("Đang tải dữ liệu chỉnh sửa...", "info");

            fetch(`/appointments/detail/${id}`)
                .then(response => {
                    if (!response.ok) throw new Error("Không tìm thấy lịch hẹn");
                    return response.json();
                })
                .then(data => {
                    document.getElementById("updateName").textContent = data.productName;
                    document.getElementById("updateCode").textContent = data.productCode;
                    document.getElementById("updateNote").value = data.note ?? "";

                    if (updateForm) {
                        updateForm.setAttribute("action", `/appointments/update/${id}`);
                    }

                    if (data.appointmentDate) {
                        const dateObj = new Date(data.appointmentDate);
                        if (!isNaN(dateObj.getTime())) {
                            const year = dateObj.getFullYear();
                            const month = String(dateObj.getMonth() + 1).padStart(2, '0');
                            const day = String(dateObj.getDate()).padStart(2, '0');
                            const hours = String(dateObj.getHours()).padStart(2, '0');
                            const minutes = String(dateObj.getMinutes()).padStart(2, '0');

                            document.getElementById("updateDate").value = `${year}-${month}-${day}`;
                            document.getElementById("updateTime").value = `${hours}:${minutes}`;
                        }
                    }

                    if (updateModal) {
                        updateModal.classList.add('active');
                    }
                })
                .catch(error => {
                    console.error(error);
                    showToast("Không thể tải thông tin chỉnh sửa!", "danger");
                });
        });
    });

    const closeUpdateModal = () => {
        if (updateModal) {
            updateModal.classList.remove('active');
        }
    };

    closeUpdateElements.forEach(el => {
        el.addEventListener("click", closeUpdateModal);
    });

    // Đóng modal khi click ra ngoài vùng phông nền tối[cite: 1]
    window.addEventListener("click", function (e) {
        if (e.target === modal) closeModal();
        if (e.target === updateModal) closeUpdateModal();
        if (e.target === cancelModal) closeCancelModal();
    });

    // Phím ESC đóng tất cả modal[cite: 1]
    document.addEventListener('keydown', (e) => {
        if (e.key === 'Escape') {
            closeModal();
            closeUpdateModal();
            closeCancelModal();
        }
    });
});