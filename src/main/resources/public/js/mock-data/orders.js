/**
 * mock-data/orders.js -> Đã chuyển đổi thành API Connector thật
 * 
 * Quản lý giao tiếp HTTP Client kết nối tới Spring Boot RestController (/api/orders)
 */

/**
 * Lấy danh sách đơn hàng có tìm kiếm, lọc trạng thái, sắp xếp và phân trang từ Backend
 */
function apiFetchOrders({ search = '', status = 'ALL', sort = 'date_desc', page = 1, limit = 5 } = {}) {
    const params = new URLSearchParams({
        search: search,
        status: status,
        sort: sort,
        page: page,
        limit: limit
    });
    
    return fetch(`/api/orders?${params.toString()}`)
        .then(response => {
            if (!response.ok) {
                throw new Error("Không thể kết nối đến máy chủ lấy danh sách đơn hàng.");
            }
            return response.json();
        })
        .catch(error => {
            console.error("Lỗi fetch danh sách đơn hàng:", error);
            return { orders: [], totalCount: 0, pages: 0, currentPage: page };
        });
}

/**
 * Lấy thông tin chi tiết của một đơn hàng dựa theo mã Code
 */
function apiFetchOrderDetail(orderCode) {
    return fetch(`/api/orders/${encodeURIComponent(orderCode)}`)
        .then(response => {
            if (!response.ok) {
                throw new Error("Không thể lấy chi tiết đơn hàng " + orderCode);
            }
            return response.json();
        })
        .catch(error => {
            console.error("Lỗi fetch chi tiết đơn hàng:", error);
            return null;
        });
}

/**
 * Gửi yêu cầu Phê duyệt đơn hàng (APPROVED) kèm theo phụ phí xe cẩu, phí vận chuyển
 */
function apiVerifyOrder(orderCode, craneFee, shippingFee, depositAmount) {
    // Đọc CSRF token từ các thẻ meta đã được nhúng trong orders.html
    const csrfToken = document.querySelector('meta[name="_csrf"]')?.getAttribute('content');
    const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.getAttribute('content');
    
    const headers = {
        'Content-Type': 'application/json'
    };
    if (csrfToken && csrfHeader) {
        headers[csrfHeader] = csrfToken; // Gửi token lên đầu để vượt qua filter bảo mật
    }
    
    return fetch(`/api/orders/${encodeURIComponent(orderCode)}/verify`, {
        method: 'POST',
        headers: headers,
        body: JSON.stringify({
            craneFee: craneFee,
            shippingFee: shippingFee,
            depositAmount: depositAmount
        })
    })
    .then(response => response.json())
    .catch(error => {
        console.error("Lỗi phê duyệt đơn hàng:", error);
        return { success: false, message: "Lỗi kết nối máy chủ khi phê duyệt." };
    });
}

/**
 * Gửi yêu cầu Từ chối đơn hàng (REJECTED) kèm lý do từ chối cụ thể
 */
function apiRejectOrder(orderCode, reason) {
    const csrfToken = document.querySelector('meta[name="_csrf"]')?.getAttribute('content');
    const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.getAttribute('content');
    
    const headers = {
        'Content-Type': 'application/json'
    };
    if (csrfToken && csrfHeader) {
        headers[csrfHeader] = csrfToken;
    }
    
    return fetch(`/api/orders/${encodeURIComponent(orderCode)}/reject`, {
        method: 'POST',
        headers: headers,
        body: JSON.stringify({
            reason: reason
        })
    })
    .then(response => response.json())
    .catch(error => {
        console.error("Lỗi từ chối duyệt đơn hàng:", error);
        return { success: false, message: "Lỗi kết nối máy chủ khi từ chối duyệt." };
    });
}

// Giữ lại các hàm giả lập không dùng đến trên UI để tránh lỗi tham chiếu nếu có
function apiCustomerNoShow(orderCode, notes) {
    const csrfToken = document.querySelector('meta[name="_csrf"]')?.getAttribute('content');
    const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.getAttribute('content');

    const headers = {
        'Content-Type': 'application/json'
    };
    if (csrfToken && csrfHeader) {
        headers[csrfHeader] = csrfToken;
    }

    return fetch(`/api/orders/${encodeURIComponent(orderCode)}/customer-no-show`, {
        method: 'POST',
        headers: headers,
        body: JSON.stringify({
            notes: notes
        })
    })
    .then(response => response.json())
    .catch(error => {
        console.error("Lỗi hủy đơn vì khách không nhận:", error);
        return { success: false, message: "Lỗi kết nối máy chủ khi hủy đơn vì khách không nhận." };
    });
}

function apiCompletePaidOrder(orderCode) {
    const csrfToken = document.querySelector('meta[name="_csrf"]')?.getAttribute('content');
    const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.getAttribute('content');

    const headers = {
        'Content-Type': 'application/json'
    };
    if (csrfToken && csrfHeader) {
        headers[csrfHeader] = csrfToken;
    }

    return fetch(`/api/orders/${encodeURIComponent(orderCode)}/complete`, {
        method: 'POST',
        headers: headers
    })
    .then(response => response.json())
    .catch(error => {
        console.error("Lỗi hoàn thành đơn:", error);
        return { success: false, message: "Lỗi kết nối máy chủ khi hoàn thành đơn." };
    });
}

function apiSimulatePayment(orderCode) {
    return Promise.resolve({ success: true });
}
function apiSimulateCancellation(orderCode) {
    return Promise.resolve({ success: true });
}
