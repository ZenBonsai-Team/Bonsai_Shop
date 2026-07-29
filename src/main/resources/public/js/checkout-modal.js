/* src/main/resources/public/js/checkout-modal.js */

document.addEventListener('DOMContentLoaded', () => {
    initRealtimeValidation();
});

// Đối tượng lưu trạng thái focus hiện tại phục vụ cho Focus Trapping
let lastFocusedElement = null;

/**
 * Hàm kích hoạt hiển thị Modal và gán động dữ liệu cây cảnh
 * Được gọi từ nút "Gửi yêu cầu mua" ở trang chi tiết sản phẩm
 */
function openCheckoutModal(productId, productName, productCode, priceStr, imgUrl, segmentName) {
    // Lưu lại phần tử cuối cùng được chọn trước khi mở modal
    lastFocusedElement = document.activeElement;
    
    // Gán dữ liệu sang các trường thông tin trong Modal
    document.getElementById('submitProductId').value = productId;
    document.getElementById('modalProductName').innerText = productName;
    document.getElementById('modalProductCode').innerText = `Mã: ${productCode}`;
    document.getElementById('modalProductPrice').innerText = priceStr;
    document.getElementById('modalProductImg').src = imgUrl || 'https://images.unsplash.com/photo-1599599810769-bcde5a160d32?auto=format&fit=crop&q=80&w=800';
    
    const segmentEl = document.getElementById('modalProductSegment');
    segmentEl.innerText = segmentName || 'Popular';
    if(segmentName === 'Premium' || segmentName === 'VIP') {
        segmentEl.className = 'badge-premium mb-1';
    } else {
        segmentEl.className = 'badge-status-pill avail mb-1';
    }
    
    // Reset Form về trạng thái sạch sẽ ban đầu
    resetCheckoutForm();
    
    // Hiển thị Modal thông qua Bootstrap API
    const modalElement = document.getElementById('checkoutModal');
    const modalInstance = new bootstrap.Modal(modalElement);
    modalInstance.show();
    
    // Bẫy phím Tab bên trong modal (Accessibility)
    setupFocusTrap(modalElement);
}

/**
 * Đặt lại trạng thái ban đầu cho Form và ẩn các overlay thông báo
 */
function resetCheckoutForm() {
    const form = document.getElementById('checkoutForm');
    form.reset();
    form.classList.remove('was-validated');
    
    // Loại bỏ class lỗi thủ công trên từng ô input
    const inputs = form.querySelectorAll('.form-control');
    inputs.forEach(input => {
        input.classList.remove('is-invalid', 'is-valid');
    });
    
    // Đảm bảo phương thức COD được chọn mặc định
    selectPaymentMethod('COD');
    
    // Ẩn các Overlay thông báo thành công / lỗi
    document.getElementById('modalSuccessOverlay').classList.add('d-none');
    document.getElementById('modalErrorOverlay').classList.add('d-none');
    
    // Khôi phục trạng thái nút submit
    enableSubmitButton();
}

/**
 * Chuyển đổi trạng thái hiển thị của các thẻ Phương thức thanh toán
 */
function selectPaymentMethod(method) {
    const cards = document.querySelectorAll('.payment-method-card');
    cards.forEach(card => card.classList.remove('active'));
    
    if (method === 'COD') {
        document.getElementById('payCOD').checked = true;
        document.querySelector('[onclick="selectPaymentMethod(\'COD\')"]').classList.add('active');
    } else {
        document.getElementById('payVNPAY').checked = true;
        document.querySelector('[onclick="selectPaymentMethod(\'VNPAY\')"]').classList.add('active');
    }
}

/**
 * Validate phía Client theo thời gian thực (realtime) khi người dùng gõ phím
 */
function initRealtimeValidation() {
    const rules = {
        customerName: (val) => val.trim().length >= 2,
        customerPhone: (val) => /^(0|\+84)(\d{9})$/.test(val.trim()),
        customerEmail: (val) => /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(val.trim()),
        shippingAddress: (val) => val.trim().length >= 10
    };
    
    Object.keys(rules).forEach(fieldId => {
        const input = document.getElementById(fieldId);
        if (!input) return;
        
        const validateField = () => {
            const isValid = rules[fieldId](input.value);
            if (isValid) {
                input.classList.remove('is-invalid');
                input.classList.add('is-valid');
            } else {
                input.classList.remove('is-valid');
                input.classList.add('is-invalid');
            }
            return isValid;
        };
        
        input.addEventListener('input', validateField);
        input.addEventListener('blur', validateField);
    });
}

/**
 * Kiểm tra toàn bộ form trước khi gửi submit
 */
function validateWholeForm() {
    const rules = {
        customerName: (val) => val.trim().length >= 2,
        customerPhone: (val) => /^(0|\+84)(\d{9})$/.test(val.trim()),
        customerEmail: (val) => /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(val.trim()),
        shippingAddress: (val) => val.trim().length >= 10
    };
    
    let isAllValid = true;
    
    Object.keys(rules).forEach(fieldId => {
        const input = document.getElementById(fieldId);
        const isValid = rules[fieldId](input.value);
        if (!isValid) {
            input.classList.add('is-invalid');
            isAllValid = false;
        } else {
            input.classList.remove('is-invalid');
            input.classList.add('is-valid');
        }
    });
    
    return isAllValid;
}

/**
 * Vô hiệu hóa nút Submit và hiển thị Spinner (Chặn double-submit)
 */
function disableSubmitButton() {
    const btn = document.getElementById('btnSubmitOrder');
    btn.disabled = true;
    btn.querySelector('.normal-state').classList.add('d-none');
    btn.querySelector('.loading-state').classList.remove('d-none');
}

/**
 * Kích hoạt lại nút Submit
 */
function enableSubmitButton() {
    const btn = document.getElementById('btnSubmitOrder');
    btn.disabled = false;
    btn.querySelector('.normal-state').classList.remove('d-none');
    btn.querySelector('.loading-state').classList.add('d-none');
}

/**
 * Xử lý sự kiện gửi đơn hàng (AJAX Request)
 */
function processOrderSubmit() {
    if (!validateWholeForm()) {
        return; // Dừng xử lý nếu có lỗi validate phía client
    }
    
    disableSubmitButton();
    
    // Thu thập dữ liệu form để đóng gói DTO
    const productId = parseInt(document.getElementById('submitProductId').value, 10);
    const customerName = document.getElementById('customerName').value.trim();
    const customerPhone = document.getElementById('customerPhone').value.trim();
    const customerEmail = document.getElementById('customerEmail').value.trim();
    const shippingAddress = document.getElementById('shippingAddress').value.trim();
    const paymentMethod = document.querySelector('input[name="paymentMethod"]:checked').value;
    
    const requestData = {
        productId,
        customerName,
        customerPhone,
        customerEmail,
        shippingAddress,
        paymentMethod
    };
    
    // Lấy CSRF tokens từ Meta tags đã được nhúng sẵn trên trang product-detail
    const csrfToken = document.querySelector('meta[name="_csrf"]')?.getAttribute('content');
    const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.getAttribute('content');
    
    const headers = {
        'Content-Type': 'application/json'
    };
    if (csrfToken && csrfHeader) {
        headers[csrfHeader] = csrfToken; // Gửi kèm token để vượt qua bộ lọc Spring Security
    }
    
    // Gửi yêu cầu AJAX lên Backend
    fetch('/api/orders/checkout', {
        method: 'POST',
        headers: headers,
        body: JSON.stringify(requestData)
    })
    .then(async response => {
        const data = await response.json();
        if (response.ok && data.success) {
            handleOrderSuccess(data);
        } else {
            handleOrderError(data.message || 'Lỗi xử lý đơn hàng từ hệ thống.');
        }
    })
    .catch(error => {
        console.error('AJAX Error:', error);
        handleOrderError('Không thể kết nối đến máy chủ. Vui lòng kiểm tra lại mạng internet của bạn.');
    });
}

/**
 * Xử lý khi Backend phản hồi thành công
 */
function handleOrderSuccess(data) {
    // Tất cả các đơn hàng đều tạo ở trạng thái PENDING và hiển thị màn hình thành công chờ Moderator kiểm duyệt
    document.getElementById('successOrderCode').innerText = data.orderCode;
    document.getElementById('modalSuccessOverlay').classList.remove('d-none');
}

/**
 * Xử lý khi Backend phản hồi thất bại hoặc mất kết nối mạng
 */
function handleOrderError(errorMessage) {
    enableSubmitButton();
    document.getElementById('errorOverlayMessage').innerText = errorMessage;
    document.getElementById('modalErrorOverlay').classList.remove('d-none');
}

function hideErrorOverlay() {
    document.getElementById('modalErrorOverlay').classList.add('d-none');
}

/**
 * Thiết lập Focus Trapping đảm bảo bàn phím không thoát khỏi Modal khi đang mở (Accessibility)
 */
function setupFocusTrap(modalEl) {
    const focusableElements = modalEl.querySelectorAll('a[href], area[href], input:not([disabled]), select:not([disabled]), textarea:not([disabled]), button:not([disabled]), iframe, object, embed, [tabindex="0"], [contenteditable]');
    const firstFocusableEl = focusableElements[0];
    const lastFocusableEl = focusableElements[focusableElements.length - 1];
    
    modalEl.addEventListener('keydown', function(e) {
        const isTabPressed = (e.key === 'Tab' || e.keyCode === 9);
        if (!isTabPressed) return;
        
        if (e.shiftKey) { /* Shift + Tab: Di chuyển ngược chiều */
            if (document.activeElement === firstFocusableEl) {
                lastFocusableEl.focus();
                e.preventDefault();
            }
        } else { /* Tab: Di chuyển xuôi chiều */
            if (document.activeElement === lastFocusableEl) {
                firstFocusableEl.focus();
                e.preventDefault();
            }
        }
    });
    
    // Tự động focus vào ô đầu tiên sau khi modal mở hoàn tất
    modalEl.addEventListener('shown.bs.modal', () => {
        document.getElementById('customerName').focus();
    });
    
    // Khôi phục tiêu điểm của phần tử bấm trước khi mở modal khi đóng modal
    modalEl.addEventListener('hidden.bs.modal', () => {
        if (lastFocusedElement) {
            lastFocusedElement.focus();
        }
    });
}