/* ==========================================================================
   Bonsai Luxury — Premium Appointment Interactions (Production Ready)
   ========================================================================== */
document.addEventListener('DOMContentLoaded', () => {

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
    const showToast = (text, type = 'info') => {
        const existing = document.querySelector('.luxury-toast');
        if (existing) existing.remove();

        const toast = document.createElement('div');
        toast.className = `luxury-toast ${type}`;
        toast.textContent = text;

        Object.assign(toast.style, {
            position: 'fixed',
            bottom: '30px',
            right: '30px',
            background: type === 'danger' ? '#C0392B' : '#113425',
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
    };

    /* 4. SỰ KIỆN HỦY LỊCH HẸN VỚI HOẠT ẢNH MỀM (SOFT ERASE ANIMATION) */
    const cancelButtons = document.querySelectorAll('.btn-apt-luxury.type-cancel');

    cancelButtons.forEach(button => {
        button.addEventListener('click', function (e) {
            e.preventDefault();

            const appointmentId = this.getAttribute('data-id');
            const cardElement = this.closest('.appointment-card');
            const productName = cardElement ? cardElement.querySelector('.appointment-product-name').textContent : 'tác phẩm';

            if (confirm(`Quý khách có chắc chắn muốn hủy yêu cầu thưởng lãm tác phẩm "${productName}"?`)) {
                showToast(`Đang xử lý yêu cầu hủy...`, 'info');

                if (cardElement) {
                    cardElement.style.transform = 'scale(0.96) translateY(10px)';
                    cardElement.style.opacity = '0';
                    cardElement.style.transition = 'all 0.6s cubic-bezier(0.16, 1, 0.3, 1)';

                    setTimeout(() => {
                        cardElement.remove();
                        showToast(`Đã hủy lịch hẹn xem: ${productName}`, 'danger');

                        const remainingCards = document.querySelectorAll('.appointment-card');
                        if (remainingCards.length === 0) {
                            window.location.reload();
                        }
                    }, 600);
                }
            }
        });
    });

    /* 5. VIEW APPOINTMENT DETAIL (Sửa lỗi xử lý và định dạng Ngày/Giờ hiển thị) */
    const modal = document.getElementById("appointmentModal");
    const closeBtn = document.querySelector(".close-btn");
    const detailButtons = document.querySelectorAll(".view-detail-btn");

    detailButtons.forEach(button => {
        button.addEventListener("click", function () {
            const id = this.dataset.id;
            showToast("Đang tải chi tiết...", "info");

            fetch(`/appointment/detail/${id}`)
                .then(response => response.json())
                .then(data => {
                    // Cập nhật tên, mã, trạng thái, ghi chú[cite: 1]
                    document.getElementById("detailName").textContent = data.productName;
                    document.getElementById("detailCode").textContent = data.productCode;
                    document.getElementById("detailStatus").textContent = data.status;
                    document.getElementById("detailNote").textContent = data.note ?? "Không có ghi chú riêng";

                    // --- XỬ LÝ ĐỊNH DẠNG NGÀY GIỜ CHUẨN LUXURY ---
                    // Nếu data.appointmentDate trả về chuỗi ISO (VD: "2026-05-12T14:30:00" hoặc định dạng tương tự)
                    if (data.appointmentDate) {
                        const dateObj = new Date(data.appointmentDate);

                        if (!isNaN(dateObj.getTime())) {
                            // Định dạng Ngày: dd/MM/yyyy
                            const day = String(dateObj.getDate()).padStart(2, '0');
                            const month = String(dateObj.getMonth() + 1).padStart(2, '0');
                            const year = dateObj.getFullYear();

                            // Định dạng Giờ: HH:mm
                            const hours = String(dateObj.getHours()).padStart(2, '0');
                            const minutes = String(dateObj.getMinutes()).padStart(2, '0');

                            document.getElementById("detailDate").textContent = `${day}/${month}/${year}`;
                            document.getElementById("detailTime").textContent = `${hours}:${minutes}`;
                        } else {
                            // Backup trường hợp chuỗi không parse được bằng Date object, gán thô tự động
                            document.getElementById("detailDate").textContent = data.appointmentDate;
                            document.getElementById("detailTime").textContent = data.appointmentTime ?? "";
                        }
                    } else {
                        document.getElementById("detailDate").textContent = data.appointmentDate ?? "";
                        document.getElementById("detailTime").textContent = data.appointmentTime ?? "";
                    }

                    // Kích hoạt hiển thị modal mượt mà qua class CSS[cite: 1]
                    if (modal) {
                        modal.classList.add('active');
                    }
                })
                .catch(error => {
                    console.error(error);
                    showToast("Không thể tải thông tin lịch hẹn!", "danger");
                });
        });
    });

    // Hàm đóng modal chuẩn hóa bằng cách loại bỏ class hoạt ảnh[cite: 1]
    const closeModal = () => {
        if (modal) {
            modal.classList.remove('active');
        }
    };

    // Đóng khi click nút Close (X)[cite: 1]
    if (closeBtn) {
        closeBtn.addEventListener("click", closeModal);
    }

    // Đóng khi click ra vùng phông nền tối bên ngoài popup[cite: 1]
    window.addEventListener("click", function (e) {
        if (e.target === modal) {
            closeModal();
        }
    });

    // Hỗ trợ đóng nhanh bằng phím bấm ESC[cite: 1]
    document.addEventListener('keydown', (e) => {
        if (e.key === 'Escape' && modal && modal.classList.contains('active')) {
            closeModal();
        }
    });
});