/**
 * mock-data/orders.js
 * 
 * This file serves as the client-side data store and "Service Layer" for orders.
 * It encapsulates mock data and provides asynchronous simulated API endpoints.
 * 
 * TODO: In a production environment, all of these functions should be replaced
 * with fetch/axios requests to the backend Spring Boot REST API (e.g., /api/orders/...).
 */

// Initial mock orders dataset representing various states in the business flow
const MOCK_ORDERS = [
    {
        orderId: 1,
        orderCode: "BSMS-A389F1",
        customer: {
            name: "Nguyễn Văn A",
            phone: "0912345678",
            email: "vana@gmail.com",
            address: "123 Đường Láng, Láng Thượng, Đống Đa, Hà Nội"
        },
        product: {
            id: 101,
            name: "Bonsai Tùng La Hán Dáng Thác Đổ",
            image: "/images/bonsai-tung.jpg",
            price: 15000000 // 15,000,000 VND
        },
        quantity: 1,
        totalAmount: 15000000,
        depositAmount: 3000000,
        orderDate: "2026-07-09T10:30:00",
        orderStatus: "PENDING",
        craneFee: null,
        shippingFee: null,
        notes: "Cần vận chuyển bằng xe cẩu chuyên dụng do cây lớn."
    },
    {
        orderId: 2,
        orderCode: "BSMS-B920C8",
        customer: {
            name: "Trần Thị B",
            phone: "0987654321",
            email: "thib@yahoo.com",
            address: "456 Nguyễn Hữu Thọ, Tân Phong, Quận 7, TP. Hồ Chí Minh"
        },
        product: {
            id: 102,
            name: "Bonsai Mai Chiếu Thủy Cổ Thụ",
            image: "/images/bonsai-mai.jpg",
            price: 28000000
        },
        quantity: 1,
        totalAmount: 28000000,
        depositAmount: 5000000,
        orderDate: "2026-07-08T15:45:00",
        orderStatus: "APPROVED",
        craneFee: 500000,
        shippingFee: 1200000,
        notes: "Giao vào ngày cuối tuần."
    },
    {
        orderId: 3,
        orderCode: "BSMS-C481A2",
        customer: {
            name: "Phạm Minh C",
            phone: "0933334444",
            email: "minhc@outlook.com",
            address: "78 Lê Lợi, Bến Nghé, Quận 1, TP. Hồ Chí Minh"
        },
        product: {
            id: 103,
            name: "Bonsai Sam Hương Dáng Văn Nhân",
            image: "/images/bonsai-sam.jpg",
            price: 8500000
        },
        quantity: 1,
        totalAmount: 8500000,
        depositAmount: 8500000,
        orderDate: "2026-07-07T09:15:00",
        orderStatus: "PAID",
        craneFee: 0,
        shippingFee: 250000,
        notes: "Đã thanh toán đủ qua VNPay."
    },
    {
        orderId: 4,
        orderCode: "BSMS-D572D9",
        customer: {
            name: "Lê Hoàng D",
            phone: "0905556666",
            email: "hoangd@gmail.com",
            address: "99 Trịnh Công Sơn, Hòa Cường Nam, Hải Châu, Đà Nẵng"
        },
        product: {
            id: 104,
            name: "Bonsai Linh Sam Bonsai Mini để bàn",
            image: "/images/bonsai-linhsam.jpg",
            price: 3200000
        },
        quantity: 2,
        totalAmount: 6400000,
        depositAmount: 0,
        orderDate: "2026-07-06T14:20:00",
        orderStatus: "REJECTED",
        craneFee: null,
        shippingFee: null,
        notes: "Khách hủy do không liên lạc được để xác nhận thông tin."
    },
    {
        orderId: 5,
        orderCode: "BSMS-E628E3",
        customer: {
            name: "Hoàng Văn E",
            phone: "0944445555",
            email: "vane@gmail.com",
            address: "12 Trần Hưng Đạo, Lộc Thọ, Nha Trang, Khánh Hòa"
        },
        product: {
            id: 105,
            name: "Bonsai Sanh Nam Điền Dáng Ôm Đá",
            image: "/images/bonsai-sanh.jpg",
            price: 45000000
        },
        quantity: 1,
        totalAmount: 45000000,
        depositAmount: 10000000,
        orderDate: "2026-07-05T17:00:00",
        orderStatus: "CANCELLED",
        craneFee: 1000000,
        shippingFee: 3000000,
        notes: "Đơn hàng đã được duyệt nhưng khách đổi ý không lấy nữa."
    },
    {
        orderId: 6,
        orderCode: "BSMS-F730F4",
        customer: {
            name: "Nguyễn Thị F",
            phone: "0977778888",
            email: "thif@gmail.com",
            address: "34 Quang Trung, Nguyễn Du, Hai Bà Trưng, Hà Nội"
        },
        product: {
            id: 106,
            name: "Bonsai Nguyệt Quế Dáng Huyền",
            image: "/images/bonsai-nguyetquai.jpg",
            price: 12500000
        },
        quantity: 1,
        totalAmount: 12500000,
        depositAmount: 0,
        orderDate: "2026-07-09T08:10:00",
        orderStatus: "PENDING",
        craneFee: null,
        shippingFee: null,
        notes: "Giao gấp trong ngày nếu được."
    }
];

// In-memory active database simulation
let ordersDb = [...MOCK_ORDERS];

/**
 * Simulates fetching order list with pagination, search, sort, and status filter
 * 
 * @param {Object} params Filter and pagination criteria
 * @returns {Promise<{orders: Array, totalCount: number, pages: number}>}
 */
function apiFetchOrders({ search = '', status = 'ALL', sort = 'date_desc', page = 1, limit = 5 } = {}) {
    return new Promise((resolve) => {
        // TODO: Replace with AJAX call: GET /api/orders?search=...&status=...&sort=...&page=...&limit=...
        
        setTimeout(() => {
            let filtered = [...ordersDb];

            // 1. Search filter (ID, Customer Name, Product Name)
            if (search.trim() !== '') {
                const query = search.toLowerCase();
                filtered = filtered.filter(o => 
                    o.orderCode.toLowerCase().includes(query) ||
                    o.customer.name.toLowerCase().includes(query) ||
                    o.product.name.toLowerCase().includes(query)
                );
            }

            // 2. Status filter
            if (status !== 'ALL') {
                filtered = filtered.filter(o => o.orderStatus === status);
            }

            // 3. Sorting logic
            if (sort === 'date_desc') {
                filtered.sort((a, b) => new Date(b.orderDate) - new Date(a.orderDate));
            } else if (sort === 'date_asc') {
                filtered.sort((a, b) => new Date(a.orderDate) - new Date(b.orderDate));
            } else if (sort === 'price_desc') {
                filtered.sort((a, b) => b.totalAmount - a.totalAmount);
            } else if (sort === 'price_asc') {
                filtered.sort((a, b) => a.totalAmount - b.totalAmount);
            }

            // 4. Pagination
            const totalCount = filtered.length;
            const pages = Math.ceil(totalCount / limit);
            const startIndex = (page - 1) * limit;
            const paginatedOrders = filtered.slice(startIndex, startIndex + limit);

            resolve({
                orders: paginatedOrders,
                totalCount,
                pages,
                currentPage: page
            });
        }, 300); // simulated network latency
    });
}

/**
 * Simulates fetching a single order detail
 * 
 * @param {string} orderCode The code of the order to retrieve
 * @returns {Promise<Object|null>}
 */
function apiFetchOrderDetail(orderCode) {
    return new Promise((resolve) => {
        // TODO: Replace with AJAX call: GET /api/orders/{orderCode}
        
        setTimeout(() => {
            const order = ordersDb.find(o => o.orderCode === orderCode);
            resolve(order ? JSON.parse(JSON.stringify(order)) : null); // Deep copy to prevent side effects
        }, 200);
    });
}

/**
 * Simulates verifying/approving a PENDING order with Crane and Shipping fees
 * 
 * @param {string} orderCode The code of the order to verify
 * @param {number} craneFee The cost of utilizing a crane
 * @param {number} shippingFee The cost of transport/freight
 * @returns {Promise<{success: boolean, order: Object|null, message: string}>}
 */
function apiVerifyOrder(orderCode, craneFee, shippingFee) {
    return new Promise((resolve) => {
        // TODO: Replace with AJAX call: POST /api/orders/{orderCode}/verify
        // Request Body: { craneFee, shippingFee }
        
        setTimeout(() => {
            const idx = ordersDb.findIndex(o => o.orderCode === orderCode);
            if (idx === -1) {
                resolve({ success: false, order: null, message: "Đơn hàng không tồn tại." });
                return;
            }

            if (ordersDb[idx].orderStatus !== 'PENDING') {
                resolve({ success: false, order: null, message: "Trạng thái đơn hàng không hợp lệ để duyệt." });
                return;
            }

            // Transition: PENDING -> APPROVED
            ordersDb[idx].orderStatus = 'APPROVED';
            ordersDb[idx].craneFee = Number(craneFee) || 0;
            ordersDb[idx].shippingFee = Number(shippingFee) || 0;
            // Update total price to include fees
            ordersDb[idx].totalAmount = ordersDb[idx].product.price + ordersDb[idx].craneFee + ordersDb[idx].shippingFee;

            resolve({
                success: true,
                order: JSON.parse(JSON.stringify(ordersDb[idx])),
                message: "Duyệt đơn hàng thành công."
            });
        }, 400);
    });
}

/**
 * Simulates rejecting a PENDING order
 * 
 * @param {string} orderCode The code of the order to reject
 * @param {string} reason Optional reason for rejection (saved in notes)
 * @returns {Promise<{success: boolean, order: Object|null, message: string}>}
 */
function apiRejectOrder(orderCode, reason) {
    return new Promise((resolve) => {
        // TODO: Replace with AJAX call: POST /api/orders/{orderCode}/reject
        // Request Body: { reason }
        
        setTimeout(() => {
            const idx = ordersDb.findIndex(o => o.orderCode === orderCode);
            if (idx === -1) {
                resolve({ success: false, order: null, message: "Đơn hàng không tồn tại." });
                return;
            }

            if (ordersDb[idx].orderStatus !== 'PENDING') {
                resolve({ success: false, order: null, message: "Chỉ đơn hàng PENDING mới có thể từ chối." });
                return;
            }

            // Transition: PENDING -> REJECTED
            ordersDb[idx].orderStatus = 'REJECTED';
            ordersDb[idx].notes = `Bị từ chối duyệt. Lý do: ${reason || 'Không có lý do cụ thể'}`;

            resolve({
                success: true,
                order: JSON.parse(JSON.stringify(ordersDb[idx])),
                message: "Từ chối duyệt đơn hàng thành công."
            });
        }, 400);
    });
}

/**
 * Simulates paying an APPROVED order
 * 
 * @param {string} orderCode The code of the order
 * @returns {Promise<{success: boolean, order: Object|null, message: string}>}
 */
function apiSimulatePayment(orderCode) {
    return new Promise((resolve) => {
        // TODO: Replace with AJAX call to simulate VNPay/Bank success callback
        
        setTimeout(() => {
            const idx = ordersDb.findIndex(o => o.orderCode === orderCode);
            if (idx === -1) {
                resolve({ success: false, order: null, message: "Đơn hàng không tồn tại." });
                return;
            }

            if (ordersDb[idx].orderStatus !== 'APPROVED') {
                resolve({ success: false, order: null, message: "Đơn hàng phải ở trạng thái APPROVED mới có thể thanh toán." });
                return;
            }

            // Transition: APPROVED -> PAID
            ordersDb[idx].orderStatus = 'PAID';

            resolve({
                success: true,
                order: JSON.parse(JSON.stringify(ordersDb[idx])),
                message: "Thanh toán thành công."
            });
        }, 300);
    });
}

/**
 * Simulates cancelling an APPROVED order
 * 
 * @param {string} orderCode The code of the order
 * @returns {Promise<{success: boolean, order: Object|null, message: string}>}
 */
function apiSimulateCancellation(orderCode) {
    return new Promise((resolve) => {
        // TODO: Replace with AJAX call
        
        setTimeout(() => {
            const idx = ordersDb.findIndex(o => o.orderCode === orderCode);
            if (idx === -1) {
                resolve({ success: false, order: null, message: "Đơn hàng không tồn tại." });
                return;
            }

            if (ordersDb[idx].orderStatus !== 'APPROVED') {
                resolve({ success: false, order: null, message: "Chỉ đơn hàng APPROVED mới có thể hủy bỏ." });
                return;
            }

            // Transition: APPROVED -> CANCELLED
            ordersDb[idx].orderStatus = 'CANCELLED';

            resolve({
                success: true,
                order: JSON.parse(JSON.stringify(ordersDb[idx])),
                message: "Hủy đơn hàng thành công."
            });
        }, 300);
    });
}
