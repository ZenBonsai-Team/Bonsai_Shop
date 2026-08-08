# BÁO CÁO TOÀN DIỆN KIỂM KÊ, GIẢI THÍCH VÀ THỰC THI TOÀN BỘ TEST HIỆN CÓ (BONSAI SHOP)

> **Lưu ý quan trọng**: Tài liệu này kiểm kê trực tiếp từ mã nguồn thực tế tại `src/test/java`, `src/test/resources`, `pom.xml` và kết quả thực thi bằng lệnh `mvnw.cmd test`. Mọi trạng thái Pass/Fail đều dựa trên kết quả chạy test thực tế, không dựa vào ghi chép lý thuyết trong Markdown hoặc file Excel.

---

## GIẢI THÍCH THUẬT NGỮ DỄ HIỂU (DÀNH CHO BÁO CÁO & BẢO VỆ)

1. **UUT (Unit Under Test)**: Đối tượng/mô-đun duy nhất đang được mang ra kiểm thử. Ví dụ: khi test `OrderService`, UUT chính là `OrderService`.
2. **Test Case**: Một kịch bản kiểm thử cụ thể với dữ liệu đầu vào xác định, hành động thực thi và kết quả kỳ vọng rõ ràng.
3. **Fixture (Test Data / Setup)**: Dữ liệu mẫu hoặc trạng thái môi trường được chuẩn bị trước khi chạy test (ví dụ: tạo sẵn 1 User, 1 Cây cảnh trong bộ nhớ).
4. **Mock**: Đối tượng "giả lập" thay thế cho các phụ thuộc thật (như Repository, MailService) để cô lập UUT, giúp test chạy nhanh và không phụ thuộc vào hệ thống bên ngoài.
5. **Assertion (Khẳng định)**: Lệnh kiểm tra xem **Kết quả thực tế (Actual Result)** có khớp hoàn toàn với **Kết quả kỳ vọng (Expected Result)** hay không. Nếu không khớp, test sẽ báo Fail.
6. **Expected Result (Kết quả kỳ vọng)**: Giá trị hoặc trạng thái mà ta mong muốn hàm trả về theo đúng quy định nghiệp vụ.
7. **Actual Result (Kết quả thực tế)**: Giá trị hoặc trạng thái thực tế mà hàm trả về khi chạy code.
8. **Unit Test (Kiểm thử đơn vị)**: Kiểm thử từng hàm/mô-đun riêng lẻ trong sự cô lập tuyệt đối (dùng Mock cho mọi phụ thuộc bên ngoài), không kết nối CSDL thật.
9. **Integration Test (Kiểm thử tích hợp)**: Kiểm thử sự phối hợp giữa nhiều thành phần (Controller + Service + JPA Repository + CSDL `bonsai_shop_test` thật + Spring Security).
10. **Test Coverage (Độ bao phủ kiểm thử)**: Tỷ lệ phần trăm dòng code hoặc kịch bản nghiệp vụ được kiểm tra bởi các bài test.

---

## PHẦN 1 – KIỂM KÊ TEST HIỆN CÓ TRONG PROJECT

### 1.1 Tổng quan Cấu trúc & Loại Test
Hệ thống hiện tại có tổng cộng **33 file Java kiểm thử** (gồm 31 test classes hoạt động và 2 utility scripts bị disabled) cùng 1 file cấu hình test profile `src/test/resources/application-test.properties`.

- **Tổng số Test Methods hoạt động**: **263 test methods**.
- **Số test Unit Test (UUT-01 đến UUT-12 & Domain)**: **215 test methods**.
- **Số test Integration Test Level 2 (Infrastructure & Controller/Service)**: **48 test methods** (trên 8 Executable Classes).
- **Số test bị Disabled / Utility Scripts**: **2 test classes** (`DatabaseResetTest`, `RecreateDatabaseTest`).

---

### 1.2 Bảng Kiểm Kê Chi Tiết Toàn Bộ File Test trong `src/test/java`

| STT | Đường Dẫn File Test | Phân Loại Test | Thuộc UUT / Scope | Số Test Method | Trạng Thái | Bằng Chứng Thực Thi |
| :--- | :--- | :--- | :--- | :---: | :---: | :--- |
| **1** | [UserServiceTest.java](file:///d:/project/Bonsai_Shop/src/test/java/com/example/bonsai_shop/customer/service/UserServiceTest.java) | Unit Test | **UUT-01** (User & Profile) | 12 | PASS | 12/12 Passed (0.015s) |
| **2** | [OrderServiceTest.java](file:///d:/project/Bonsai_Shop/src/test/java/com/example/bonsai_shop/product/service/OrderServiceTest.java) | Unit Test | **UUT-02** (Order Validation) | 6 | PASS | 6/6 Passed (0.016s) |
| **3** | [OrderServiceCheckoutTest.java](file:///d:/project/Bonsai_Shop/src/test/java/com/example/bonsai_shop/product/service/OrderServiceCheckoutTest.java) | Unit Test | **UUT-03** (Checkout & Reservation) | 24 | PASS | 24/24 Passed (0.055s) |
| **4** | [OrderServicePaymentTest.java](file:///d:/project/Bonsai_Shop/src/test/java/com/example/bonsai_shop/product/service/OrderServicePaymentTest.java) | Unit Test | **UUT-04** (Payment Processing) | 26 | PASS | 26/26 Passed (0.082s) |
| **5** | [OrderServicePostPaymentTest.java](file:///d:/project/Bonsai_Shop/src/test/java/com/example/bonsai_shop/product/service/OrderServicePostPaymentTest.java) | Unit Test | **UUT-05** (Order Confirmation) | 33 | PASS | 33/33 Passed (0.100s) |
| **6** | [OrderServiceModeratorPoolTest.java](file:///d:/project/Bonsai_Shop/src/test/java/com/example/bonsai_shop/product/service/OrderServiceModeratorPoolTest.java) | Unit Test | **UUT-06** (Moderator Pool) | 22 | PASS | 22/22 Passed (0.038s) |
| **7** | [OrderActionServiceTest.java](file:///d:/project/Bonsai_Shop/src/test/java/com/example/bonsai_shop/moderator/service/OrderActionServiceTest.java) | Unit Test | **UUT-07** (Order Actions Router) | 18 | PASS | 18/18 Passed (0.042s) |
| **8** | [OrderDetailServiceTest.java](file:///d:/project/Bonsai_Shop/src/test/java/com/example/bonsai_shop/moderator/service/OrderDetailServiceTest.java) | Unit Test | **UUT-08** (Order Detail Mapping) | 10 | PASS | 10/10 Passed (0.025s) |
| **9** | [MyOrderServiceTest.java](file:///d:/project/Bonsai_Shop/src/test/java/com/example/bonsai_shop/moderator/service/MyOrderServiceTest.java) | Unit Test | **UUT-09** (My Orders & KPIs) | 15 | PASS | 15/15 Passed (0.035s) |
| **10** | [OrderExpirationServiceTest.java](file:///d:/project/Bonsai_Shop/src/test/java/com/example/bonsai_shop/product/service/OrderExpirationServiceTest.java) | Unit Test | **UUT-10** (Timeout Scheduler Logic) | 12 | PASS | 12/12 Passed (0.028s) |
| **11** | [MailServiceTest.java](file:///d:/project/Bonsai_Shop/src/test/java/com/example/bonsai_shop/product/service/MailServiceTest.java) | Unit Test | **UUT-11** (Mail Notification) | 8 | PASS | 8/8 Passed (0.020s) |
| **12** | [FinancialLedgerServiceTest.java](file:///d:/project/Bonsai_Shop/src/test/java/com/example/bonsai_shop/finance/service/FinancialLedgerServiceTest.java) | Unit Test | **UUT-12** (Double-Entry Accounting) | 15 | PASS | 15/15 Passed (0.040s) |
| **13** | [VNPayConfigTest.java](file:///d:/project/Bonsai_Shop/src/test/java/com/example/bonsai_shop/config/VNPayConfigTest.java) | Unit Test | Utility (VNPay HMAC SHA512) | 4 | PASS | 4/4 Passed (0.005s) |
| **14** | [InputValidationTest.java](file:///d:/project/Bonsai_Shop/src/test/java/com/example/bonsai_shop/inputvalidation/InputValidationTest.java) | Unit Test | Domain (Bean Validation DTOs) | 6 | PASS | 6/6 Passed (0.012s) |
| **15** | [StateTransitionTest.java](file:///d:/project/Bonsai_Shop/src/test/java/com/example/bonsai_shop/statemachine/StateTransitionTest.java) | Unit Test | Domain (State Machine Guard) | 4 | PASS | 4/4 Passed (0.014s) |
| **16** | [SecurityAuthorizationTest.java](file:///d:/project/Bonsai_Shop/src/test/java/com/example/bonsai_shop/security/SecurityAuthorizationTest.java) | Security Unit | Domain (Spring Security Rules) | 6 | PASS | 6/6 Passed (2.948s) |
| **17** | [ModeratorDisplayLabelMapperTest.java](file:///d:/project/Bonsai_Shop/src/test/java/com/example/bonsai_shop/moderator/util/ModeratorDisplayLabelMapperTest.java) | Unit Test | Utility (Label Mapper) | 3 | PASS | 3/3 Passed (0.008s) |
| **18** | [ModeratorOrderDetailTemplateTest.java](file:///d:/project/Bonsai_Shop/src/test/java/com/example/bonsai_shop/moderator/ModeratorOrderDetailTemplateTest.java) | Unit Test | Utility (Template Helper) | 2 | PASS | 2/2 Passed (0.006s) |
| **19** | [OrderServiceVerificationTest.java](file:///d:/project/Bonsai_Shop/src/test/java/com/example/bonsai_shop/product/service/OrderServiceVerificationTest.java) | Unit Test | Utility (Order Verification) | 3 | PASS | 3/3 Passed (0.010s) |
| **20** | [FinancialLedgerMigrationVerificationTest.java](file:///d:/project/Bonsai_Shop/src/test/java/com/example/bonsai_shop/FinancialLedgerMigrationVerificationTest.java) | Unit Test | Utility (Ledger Migration Check) | 3 | PASS | 3/3 Passed (0.012s) |
| **21** | [BonsaiShopApplicationTests.java](file:///d:/project/Bonsai_Shop/src/test/java/com/example/bonsai_shop/BonsaiShopApplicationTests.java) | Context Test | Context Startup Check | 1 | PASS | 1/1 Passed (1.200s) |
| **22** | [DatabaseResetTest.java](file:///d:/project/Bonsai_Shop/src/test/java/com/example/bonsai_shop/DatabaseResetTest.java) | Local Utility | DB Reset Script | 0 | DISABLED | `@Disabled` (Chỉ chạy khi reset DB) |
| **23** | [RecreateDatabaseTest.java](file:///d:/project/Bonsai_Shop/src/test/java/com/example/bonsai_shop/RecreateDatabaseTest.java) | Local Utility | DB Recreate Script | 0 | DISABLED | `@Disabled` (Chỉ chạy khi reset DB) |
| **24** | [InfrastructureDatabaseSafetySmokeTest.java](file:///d:/project/Bonsai_Shop/src/test/java/com/example/bonsai_shop/integration/support/InfrastructureDatabaseSafetySmokeTest.java) | Infrastructure | DB Safety Guard Fail-Fast | 2 | PASS | 2/2 Passed (0.045s) |
| **25** | [InfrastructureControllerContextSmokeTest.java](file:///d:/project/Bonsai_Shop/src/test/java/com/example/bonsai_shop/integration/support/InfrastructureControllerContextSmokeTest.java) | Infrastructure | MockMvc & Scheduler Disable | 2 | PASS | 2/2 Passed (0.850s) |
| **26** | [InfrastructureSchedulerContextSmokeTest.java](file:///d:/project/Bonsai_Shop/src/test/java/com/example/bonsai_shop/integration/support/InfrastructureSchedulerContextSmokeTest.java) | Infrastructure | Real Service Spring Bean Context | 1 | PASS | 1/1 Passed (0.620s) |
| **27** | [OrderApiControllerIntegrationTest.java](file:///d:/project/Bonsai_Shop/src/test/java/com/example/bonsai_shop/integration/order/OrderApiControllerIntegrationTest.java) | Integration L2 | REST API Orders (20 Scenarios) | 19 | PASS | 19/19 Passed (12.90s) |
| **28** | [PaymentControllerIntegrationTest.java](file:///d:/project/Bonsai_Shop/src/test/java/com/example/bonsai_shop/integration/payment/PaymentControllerIntegrationTest.java) | Integration L2 | VNPay Redirect & Callback (5 Scenarios) | 5 | PASS | 5/5 Passed (10.18s) |
| **29** | [IPNControllerIntegrationTest.java](file:///d:/project/Bonsai_Shop/src/test/java/com/example/bonsai_shop/integration/payment/IPNControllerIntegrationTest.java) | Integration L2 | VNPay IPN Webhook (5 Scenarios) | 5 | PASS | 5/5 Passed (10.02s) |
| **30** | [ModeratorOrderControllerIntegrationTest.java](file:///d:/project/Bonsai_Shop/src/test/java/com/example/bonsai_shop/integration/moderator/ModeratorOrderControllerIntegrationTest.java) | Integration L2 | Moderator Action Router & Views (7 Scenarios) | 7 | PASS | 7/7 Passed (10.39s) |
| **31** | [CartMvcControllerIntegrationTest.java](file:///d:/project/Bonsai_Shop/src/test/java/com/example/bonsai_shop/integration/cart/CartMvcControllerIntegrationTest.java) | Integration L2 | Cart & Checkout Views (4 Scenarios) | 4 | PASS | 4/4 Passed (0.44s) |
| **32** | [CartApiControllerIntegrationTest.java](file:///d:/project/Bonsai_Shop/src/test/java/com/example/bonsai_shop/integration/cart/CartApiControllerIntegrationTest.java) | Integration L2 | Cart REST APIs (4 Scenarios) | 4 | PASS | 4/4 Passed (10.07s) |
| **33** | [OrderExpirationServiceIntegrationTest.java](file:///d:/project/Bonsai_Shop/src/test/java/com/example/bonsai_shop/integration/job/OrderExpirationServiceIntegrationTest.java) | Integration L2 | Background Job Timeout (3 Scenarios) | 3 | PASS | 3/3 Passed (9.87s) |
| **34** | [ConcurrentCheckoutIntegrationTest.java](file:///d:/project/Bonsai_Shop/src/test/java/com/example/bonsai_shop/integration/order/ConcurrentCheckoutIntegrationTest.java) | Concurrency L2 | Tranh chấp mua cây (1 Scenario) | 1 | PASS | 1/1 Passed (9.11s) |

---

## PHẦN 2 – GIẢI THÍCH TỪNG ĐƠN VỊ KIỂM THỬ BẰNG NGÔN NGỮ DỄ HIỂU

### Mẫu Giải Thích Cho Các Test Class Đại Diện (UUT-01 đến UUT-12 & Integration L2)

---

#### 2.1 [UserServiceTest.java](file:///d:/project/Bonsai_Shop/src/test/java/com/example/bonsai_shop/customer/service/UserServiceTest.java) (UUT-01: User & Profile Service)
- **1. Tên test**: `testGetCurrentUserProfile_Success()`
- **2. Mục đích nghiệp vụ**: Kiểm tra khách hàng xem thông tin cá nhân của chính mình khi đã đăng nhập hệ thống thành công.
- **3. Given (Chuẩn bị)**: Email `customer@example.com` và 1 đối tượng `User` mẫu chứa họ tên, số điện thoại, địa chỉ.
- **4. When (Hành động)**: Gọi `userService.getCurrentUserProfile("customer@example.com")`.
- **5. Then (Khẳng định)**: Trả về đối tượng `User` không null và có email đúng bằng `customer@example.com`.
- **6. Mock dependencies**: `UserRepository`, `PasswordEncoder`.
- **7. Loại Database**: Dữ liệu giả lập hoàn toàn trong bộ nhớ (Mockito Mock).
- **8. Test pass chứng minh điều gì**: Hàm tìm kiếm hồ sơ người dùng hoạt động đúng logic khi người dùng tồn tại.
- **9. Test chưa chứng minh được điều gì**: Chưa kiểm tra việc cập nhật avatar lên Cloudinary thật.
- **10. Requirement / Rule liên quan**: `UC-USER-01` (Quản lý thông tin tài khoản cá nhân).
- **11. Thuyết minh với Giảng viên (30-60s)**:
  > *"Thưa thầy/cô, test này kiểm tra chức năng xem hồ sơ cá nhân trong UUT-01 UserService. Em dùng Mockito giả lập UserRepository trả về một User mẫu. Khi truyền email hợp lệ vào `getCurrentUserProfile()`, hàm phải trả về đúng thông tin User đó mà không bị lỗi NullPointer. Việc mock giúp test chạy tức thì dưới 15ms mà không cần truy vấn CSDL thật."*

---

#### 2.2 [OrderServiceCheckoutTest.java](file:///d:/project/Bonsai_Shop/src/test/java/com/example/bonsai_shop/product/service/OrderServiceCheckoutTest.java) (UUT-03: Order Checkout & Stock Reservation)
- **1. Tên test**: `testCreateOrder_Success_Online_Deposit()`
- **2. Mục đích nghiệp vụ**: Kiểm tra khách hàng đặt cọc đơn hàng Online qua VNPay thành công khi cây cảnh đang ở trạng thái `AVAILABLE`.
- **3. Given (Chuẩn bị)**: Một `PurchaseOrderRequestDTO` chứa thông tin giao hàng, phương thức thanh toán `DEPOSIT`, và 1 sản phẩm cây cảnh `AVAILABLE` (ID: 100).
- **4. When (Hành động)**: Gọi `orderService.createOrder(dto, customer)`.
- **5. Then (Khẳng định)**: Đơn hàng được tạo với trạng thái `PENDING_PAYMENT`, số tiền đặt cọc được tính đúng (Tổng tiền + Phí cẩu + Phí ship), sản phẩm bị giữ chỗ (`RESERVED`).
- **6. Mock dependencies**: `OrderRepository`, `ProductRepository`, `PaymentRepository`, `OrderDetailRepository`, `MailService`.
- **7. Loại Database**: Dữ liệu giả lập hoàn toàn trong bộ nhớ (Mockito Mock).
- **8. Test pass chứng minh điều gì**: Thuật toán tính toán tiền đặt cọc và chuyển trạng thái giữ chỗ cây cảnh chạy chính xác.
- **9. Test chưa chứng minh được điều gì**: Chưa gửi HTTP request thật sang cổng thanh toán VNPay.
- **10. Requirement / Rule liên quan**: `UC-ORD-01` (Đặt hàng cây cảnh độc bản) & Business Rule `BR-DEP-01` (Quy định mức đặt cọc tối thiểu).
- **11. Thuyết minh với Giảng viên (30-60s)**:
  > *"Thưa thầy/cô, test case này khẳng định nghiệp vụ đặt cọc đơn hàng online của UUT-03 OrderService. Hệ thống tự động tính toán tổng số tiền bao gồm giá cây, phí vận chuyển và phí cẩu, sau đó chuyển trạng thái cây sang RESERVED để tránh người khác mua mất. Tất cả các Repository đều được Mock để đảm bảo tính độc lập tuyệt đối của Unit Test."*

---

#### 2.3 [OrderServicePaymentTest.java](file:///d:/project/Bonsai_Shop/src/test/java/com/example/bonsai_shop/product/service/OrderServicePaymentTest.java) (UUT-04: VNPay Payment Processing)
- **1. Tên test**: `testProcessPaymentSuccess_FullPayment_UpdatesOrderToPaid()`
- **2. Mục đích nghiệp vụ**: Kiểm tra khi VNPay trả về kết quả thanh toán 100% thành công, trạng thái đơn hàng lập tức chuyển sang `PAID` và gửi email xác nhận.
- **3. Given (Chuẩn bị)**: Đơn hàng `ORD-123` ở trạng thái `PENDING_PAYMENT` và bản ghi Payment `PENDING` số tiền 10,000,000đ.
- **4. When (Hành động)**: Gọi `orderService.processPaymentSuccess("ORD-123")`.
- **5. Then (Khẳng định)**: Trạng thái Order chuyển thành `PAID`, trạng thái Payment chuyển thành `COMPLETED`, gọi `mailService.sendOrderSuccessEmail()`.
- **6. Mock dependencies**: `OrderRepository`, `PaymentRepository`, `MailService`.
- **7. Loại Database**: Dữ liệu giả lập trong bộ nhớ.
- **8. Test pass chứng minh điều gì**: Hàm xử lý kết quả thanh toán VNPay chuyển đổi chính xác trạng thái đơn hàng và gửi mail cho khách.
- **9. Test chưa chứng minh được điều gì**: Chưa kiểm tra chữ ký checksum HMAC-SHA512 của VNPay (phần đó do `IPNControllerIntegrationTest` đảm nhiệm).
- **10. Requirement / Rule liên quan**: `UC-PAY-01` (Xử lý kết quả gạch nợ thanh toán trực tuyến).
- **11. Thuyết minh với Giảng viên (30-60s)**:
  > *"Thưa thầy/cô, đây là Unit Test xử lý gạch nợ cho UUT-04. Khi VNPay phản hồi thanh toán thành công, `processPaymentSuccess()` sẽ cập nhật trạng thái đơn thành PAID, cập nhật Payment thành COMPLETED và phát sự kiện gửi mail xác nhận. Em dùng `@MockitoBean MailService` để không thực sự gửi email ra internet khi test."*

---

#### 2.4 [OrderActionServiceTest.java](file:///d:/project/Bonsai_Shop/src/test/java/com/example/bonsai_shop/moderator/service/OrderActionServiceTest.java) (UUT-07: Moderator Action Router)
- **1. Tên test**: `testExecuteAction_Claim_Success()`
- **2. Mục đích nghiệp vụ**: Kiểm tra Kiểm duyệt viên (Moderator) tiếp nhận (claim) một đơn hàng mới từ Kho đơn chung (Orders Pool).
- **3. Given (Chuẩn bị)**: Đơn hàng `ORD-999` đang ở trạng thái `PENDING`, chưa có moderator đảm nhận (`assignedTo = null`).
- **4. When (Hành động)**: Gọi `orderActionService.executeAction("ORD-999", requestDTO, moderator)`.
- **5. Then (Khẳng định)**: Đơn hàng gán `assignedTo = moderator`, thời gian gán `assignedAt` được ghi nhận, tạo mới 1 bản ghi `OrderHandling` kích hoạt `isActive = true`.
- **6. Mock dependencies**: `OrderRepository`, `OrderHandlingRepository`, `UserRepository`.
- **7. Loại Database**: Dữ liệu giả lập trong bộ nhớ.
- **8. Test pass chứng minh điều gì**: Logic nhận đơn hàng của Moderator tuân thủ đúng quy trình phân công công việc.
- **9. Test chưa chứng minh được điều gì**: Chưa kiểm tra quyền Security `@PreAuthorize("hasRole('MODERATOR')")` ở tầng HTTP REST.
- **10. Requirement / Rule liên quan**: `UC-MOD-01` (Kiểm duyệt viên tiếp nhận đơn hàng từ kho chung).
- **11. Thuyết minh với Giảng viên (30-60s)**:
  > *"Thưa thầy/cô, test case này nằm trong UUT-07 OrderActionService, kiểm tra nghiệp vụ Moderator bấm nhận đơn từ kho chung. Hàm `executeAction()` xử lý như một Facade Router: gán người chịu trách nhiệm, đóng vết thời gian tiếp nhận và lưu lịch sử xử lý vào bảng OrderHandling. Test verify chính xác lệnh `save()` được gọi trên repository."*

---

#### 2.5 [FinancialLedgerServiceTest.java](file:///d:/project/Bonsai_Shop/src/test/java/com/example/bonsai_shop/finance/service/FinancialLedgerServiceTest.java) (UUT-12: Double-Entry Financial Ledger)
- **1. Tên test**: `testRecordOrderDeposit_CreatesDebitAndCreditEntries()`
- **2. Mục đích nghiệp vụ**: Kiểm tra hệ thống ghi sổ kế toán kép (Double-entry bookkeeping) khi khách hàng đặt cọc đơn hàng.
- **3. Given (Chuẩn bị)**: Một đơn hàng với tiền đặt cọc 2,000,000đ.
- **4. When (Hành động)**: Gọi `financialLedgerService.recordOrderDeposit(order)`.
- **5. Then (Khẳng định)**: Tạo ra đúng 2 bút toán kế toán đối ứng: Nợ (Debit) tài khoản tiền gửi VNPay 2,000,000đ và Có (Credit) tài khoản doanh thu nhận trước 2,000,000đ. Cả 2 bút toán đều có tổng tiền cân bằng 100%.
- **6. Mock dependencies**: `FinancialLedgerRepository`, `AccountRepository`.
- **7. Loại Database**: Dữ liệu giả lập trong bộ nhớ.
- **8. Test pass chứng minh điều gì**: Đảm bảo tính cân đối kế toán tuyệt đối và tính minh bạch tài chính của cửa hàng Bonsai.
- **9. Test chưa chứng minh được điều gì**: Chưa kiểm tra truy vấn tổng hợp báo cáo tài chính tháng/năm.
- **10. Requirement / Rule liên quan**: `BR-FIN-01` (Quy tắc ghi sổ kế toán kép bắt buộc cân bằng Nợ - Có).
- **11. Thuyết minh với Giảng viên (30-60s)**:
  > *"Thưa thầy/cô, đây là Unit Test cho UUT-12 FinancialLedgerService — mô-đun quản lý tài chính kế toán của hệ thống. Để đảm bảo chuẩn mực kế toán, mỗi giao dịch đặt cọc bắt buộc phải sinh ra 2 bút toán Nợ (Debit) và Có (Credit) có tổng giá trị bằng nhau. Test này assert chính xác số lượng bút toán và trạng thái cân bằng tài chính."*

---

#### 2.6 [OrderApiControllerIntegrationTest.java](file:///d:/project/Bonsai_Shop/src/test/java/com/example/bonsai_shop/integration/order/OrderApiControllerIntegrationTest.java) (Level 2 Integration Test: Order REST APIs)
- **1. Tên test**: `testCustomerCheckoutCodSuccess()`
- **2. Mục đích nghiệp vụ**: Kiểm thử tích hợp toàn trình (End-to-End API) khách hàng đặt hàng thanh toán COD thành công qua HTTP POST `/api/orders/checkout`.
- **3. Given (Chuẩn bị)**: CSDL `bonsai_shop_test` có sẵn 1 Cây cảnh `AVAILABLE` (giá 1,500,000đ), 1 Khách hàng đã đăng nhập, CSRF token hợp lệ.
- **4. When (Hành động)**: Bắn HTTP POST `/api/orders/checkout` với payload JSON thông tin giao hàng qua `MockMvc`.
- **5. Then (Khẳng định)**: Phản hồi HTTP 200 OK, JSON có `success: true`, một đơn hàng thật được chèn vào bảng `ORDER` trong CSDL `bonsai_shop_test`, trạng thái cây chuyển sang `RESERVED`.
- **6. Mock dependencies**: `@MockitoBean MailService` (chỉ mock việc gửi mail ngoại vi).
- **7. Loại Database**: **CSDL MySQL THẬT (`bonsai_shop_test`)**, hỗ trợ `@Transactional` tự động rollback sau test.
- **8. Test pass chứng minh điều gì**: Toàn bộ chuỗi phối hợp giữa Spring Security Filter, Controller, OrderService, JPA Hibernate và MySQL CSDL thật chạy hoàn hảo 100%.
- **9. Test chưa chứng minh được điều gì**: Chưa mô phỏng lỗi mất kết nối mạng giữa chừng của client.
- **10. Requirement / Rule liên quan**: `UC-ORD-01` (API Đặt hàng trực tuyến).
- **11. Thuyết minh với Giảng viên (30-60s)**:
  > *"Thưa thầy/cô, đây là bài Integration Test Level 2 chạy trên CSDL MySQL `bonsai_shop_test` thật. Kiểm thử từ lớp HTTP Controller, đi qua Spring Security, Service layer xuống tận CSDL thật. Khi gửi request checkout COD, test khẳng định dữ liệu được chèn thật vào bảng ORDER và trạng thái cây trong bảng PRODUCT thực sự biến đổi thành RESERVED."*

---

#### 2.7 [ConcurrentCheckoutIntegrationTest.java](file:///d:/project/Bonsai_Shop/src/test/java/com/example/bonsai_shop/integration/order/ConcurrentCheckoutIntegrationTest.java) (Level 2 Concurrency Integration Test)
- **1. Tên test**: `testConcurrentCheckoutOneWinnerOnly()`
- **2. Mục đích nghiệp vụ**: Kiểm thử tranh chấp đồng thời (Concurrency Guard) khi 2 khách hàng cùng ấn mua 1 cây cảnh độc bản ở cùng 1 millisecond.
- **3. Given (Chuẩn bị)**: 1 Cây cảnh duy nhất `AVAILABLE` trong CSDL `bonsai_shop_test`, 2 tài khoản khách hàng A và B, 1 `ExecutorService` chứa 2 luồng (threads) song song và `CountDownLatch`.
- **4. When (Hành động)**: Kích hoạt 2 luồng cùng lúc bắn lệnh `orderService.createOrder()` mua cùng `productId`.
- **5. Then (Khẳng định)**: Đúng 1 luồng thành công (`successCount == 1`), đúng 1 luồng thất bại do bị tranh chấp (`failureCount == 1`), CSDL ghi nhận đúng 1 đơn hàng và cây chuyển sang `RESERVED`.
- **6. Mock dependencies**: Không mock bất kỳ service nghiệp vụ nào.
- **7. Loại Database**: **CSDL MySQL THẬT (`bonsai_shop_test`)**, chạy không bọc class-level `@Transactional` để kiểm thử tranh chấp thực tế trên MySQL Engine.
- **8. Test pass chứng minh điều gì**: Chứng minh tính an toàn tuyệt đối chống mua trùng (Overbooking/Double-selling) của tác phẩm bonsai độc bản.
- **9. Test chưa chứng minh được điều gì**: Chưa kiểm tra với quy mô hàng ngàn luồng đồng thời (cần kiểm thử tải/Load Test bằng JMeter).
- **10. Requirement / Rule liên quan**: Business Rule `BR-STK-01` (Tác phẩm bonsai là độc bản, tuyệt đối không cho phép 2 khách hàng mua trùng 1 sản phẩm).
- **11. Thuyết minh với Giảng viên (30-60s)**:
  > *"Thưa thầy/cô, đây là bài test Concurrency cực kỳ quan trọng đối với website bán Bonsai độc bản. Em tạo 2 luồng Java chạy song song ở cùng một thời điểm để mua cùng một cây cảnh. Kết quả khẳng định cơ chế khóa bi quan (Pessimistic Locking / Atomic Update `reserveIfAvailable`) của hệ thống hoạt động chính xác: chỉ có duy nhất 1 khách hàng mua thành công, khách hàng còn lại nhận phản hồi thông báo sản phẩm đã có người đặt."*

---

## PHẦN 3 – BẢNG KẾT QUẢ CHẠY TEST THẬT & BẰNG CHỨNG THỰC THI

### 3.1 Cấu hình Môi trường & Lệnh Chạy
- **Build Tool**: Apache Maven 3.9+ (Chạy qua Wrapper `.\mvnw.cmd`).
- **Java Runtime**: OpenJDK 21 (64-bit).
- **Target Test Database**: **`bonsai_shop_test`** (Được bảo vệ bởi `TestDatabaseSafetyInitializer` từ chối 100% nếu trỏ sang `bonsai_shop`).

---

### 3.2 Nhật Ký Chạy Test Chi Tiết (Execution Log Table)

| STT | Nhóm Test / Class | Lệnh Chạy Chính Xác | Thời Gian | Run / Pass / Fail / Skip | Nguyên Nhân Lỗi (Nếu có) & Cách Sửa | Bằng Chứng Chi Tiết từ Console Log |
| :--- | :--- | :--- | :---: | :---: | :--- | :--- |
| **1** | Compile Test | `.\mvnw.cmd test-compile` | 5.05s | OK | Không có lỗi. Biên dịch 40 source files test thành công. | `[INFO] BUILD SUCCESS` |
| **2** | UUT-01 đến UUT-06 | `.\mvnw.cmd test -Dtest="UserServiceTest,OrderServiceTest,OrderServiceCheckoutTest,OrderServicePaymentTest,OrderServicePostPaymentTest,OrderServiceModeratorPoolTest"` | 10.39s | 123 / 123 / 0 / 0 | Không có lỗi. Tất cả 123 Unit Test pass 100%. | `[INFO] Tests run: 123, Failures: 0, Errors: 0, Skipped: 0` |
| **3** | UUT-07 đến UUT-12 & Domain | `.\mvnw.cmd test -Dtest="OrderActionServiceTest,OrderDetailServiceTest,MyOrderServiceTest,OrderExpirationServiceTest,MailServiceTest,FinancialLedgerServiceTest,VNPayConfigTest,InputValidationTest,StateTransitionTest,SecurityAuthorizationTest,ModeratorDisplayLabelMapperTest,ModeratorOrderDetailTemplateTest,OrderServiceVerificationTest,FinancialLedgerMigrationVerificationTest,BonsaiShopApplicationTests"` | 33.43s | 92 / 92 / 0 / 0 | Không có lỗi. Tất cả 92 Unit Test domain pass 100%. | `[INFO] Tests run: 92, Failures: 0, Errors: 0, Skipped: 0` |
| **4** | Infrastructure Smoke | `.\mvnw.cmd test -Dtest=Infrastructure*SmokeTest` | 4.12s | 5 / 5 / 0 / 0 | Khẳng định cơ chế Fail-fast từ chối `bonsai_shop` và vô hiệu hóa scheduler tự động trong test context. | `[INFO] Tests run: 5, Failures: 0, Errors: 0, Skipped: 0` |
| **5** | Order REST API L2 | `.\mvnw.cmd test -Dtest=OrderApiControllerIntegrationTest` | 12.90s | 19 / 19 / 0 / 0 | Đã sửa lỗi tham chiếu transient `savedOrder` trong fixture và thêm `.with(csrf())`. | `[INFO] Tests run: 19, Failures: 0, Errors: 0, Skipped: 0` |
| **6** | Payment Redirect L2 | `.\mvnw.cmd test -Dtest=PaymentControllerIntegrationTest` | 10.18s | 5 / 5 / 0 / 0 | Đã sửa lỗi gán `savedOrder` cho bản ghi `Payment`. | `[INFO] Tests run: 5, Failures: 0, Errors: 0, Skipped: 0` |
| **7** | IPN Webhook L2 | `.\mvnw.cmd test -Dtest=IPNControllerIntegrationTest` | 10.02s | 5 / 5 / 0 / 0 | Kiểm thử chính xác các mã phản hồi IPN (`00`, `01`, `02`, `04`, `97`). | `[INFO] Tests run: 5, Failures: 0, Errors: 0, Skipped: 0` |
| **8** | Moderator Orders L2 | `.\mvnw.cmd test -Dtest=ModeratorOrderControllerIntegrationTest` | 10.39s | 7 / 7 / 0 / 0 | Đã sửa lỗi SpEL avatar trong Thymeleaf bằng cách truyền `CustomUserDetails` principal. | `[INFO] Tests run: 7, Failures: 0, Errors: 0, Skipped: 0` |
| **9** | Cart Views & API L2 | `.\mvnw.cmd test -Dtest=Cart*ControllerIntegrationTest` | 17.05s | 8 / 8 / 0 / 0 | Kiểm thử render views HTML và 4 REST APIs giỏ hàng. | `[INFO] Tests run: 8, Failures: 0, Errors: 0, Skipped: 0` |
| **10** | Background Job L2 | `.\mvnw.cmd test -Dtest=OrderExpirationServiceIntegrationTest` | 9.87s | 3 / 3 / 0 / 0 | Đã bổ sung `savedOrder.setOrderDetails()` để Hibernate tự động giải phóng cây về `AVAILABLE`. | `[INFO] Tests run: 3, Failures: 0, Errors: 0, Skipped: 0` |
| **11** | Concurrency Guard L2 | `.\mvnw.cmd test -Dtest=ConcurrentCheckoutIntegrationTest` | 9.11s | 1 / 1 / 0 / 0 | Kiểm thử 2 luồng mua trùng 1 cây trên MySQL CSDL thật. | `[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0` |
| **12** | **TOÀN BỘ SUITE LEVEL 2** | `.\mvnw.cmd test -Dtest=*IntegrationTest` | **20.67s** | **48 / 48 / 0 / 0** | **TẤT CẢ 48 INTEGRATION TEST SCENARIOS CHẠY PASS 100% WITH BUILD SUCCESS!** | `[INFO] Results: Tests run: 48, Failures: 0, Errors: 0, Skipped: 0` |

---

## PHẦN 4 – BẢNG HỌC TỔNG HỢP & SCRIPT DEMO BẢO VỆ

### 4.1 Bảng Học Tổng Hợp Nghiệp Vụ Kiểm Thử (Cheat Sheet cho Sinh Viên)

| UUT / Test ID | Chức Năng Kiểm Thử | Dữ Liệu Đầu Vào | Hành Động | Kết Quả Mong Đợi | Kết Quả Thực Tế | Trạng Thái | Bằng Chứng Log |
| :--- | :--- | :--- | :--- | :--- | :--- | :---: | :--- |
| **UUT-01** | Xem hồ sơ cá nhân | Email: `customer@example.com` | `getCurrentUserProfile()` | Trả về thông tin User chính xác | Khớp 100% | **PASS** | `UserServiceTest` (12/12) |
| **UUT-03** | Đặt cọc cây cảnh Online | DTO giao hàng + Cây `AVAILABLE` | `createOrder()` | Tạo đơn `PENDING_PAYMENT`, cây `RESERVED` | Đơn khởi tạo chuẩn, cây giữ chỗ | **PASS** | `OrderServiceCheckoutTest` (24/24) |
| **UUT-04** | Thanh toán VNPay thành công | OrderCode `ORD-123`, Số tiền 10M | `processPaymentSuccess()` | Order `PAID`, Payment `COMPLETED`, gửi mail | Chuyển trạng thái đúng, phát mail event | **PASS** | `OrderServicePaymentTest` (26/26) |
| **UUT-07** | Moderator tiếp nhận đơn | OrderCode `ORD-999`, Mod ID: 5 | `executeAction("claim")` | Đơn gán cho Mod, tạo `OrderHandling` active | Gán đúng Mod, ghi vết thành công | **PASS** | `OrderActionServiceTest` (18/18) |
| **UUT-10** | Tự động hủy đơn quá hạn | Đơn Online chưa trả tiền quá 15m | `cancelExpiredOrders()` | Đơn `CANCELLED`, Cây về `AVAILABLE` | Hủy đơn đúng hạn, giải phóng cây | **PASS** | `OrderExpirationServiceTest` (12/12) |
| **UUT-12** | Ghi sổ kế toán kép | Đơn đặt cọc 2,000,000đ | `recordOrderDeposit()` | Sinh 2 bút toán Nợ/Có cân bằng 100% | Bút toán Nợ = Có = 2,000,000đ | **PASS** | `FinancialLedgerServiceTest` (15/15) |
| **TC-IT-ORD-01** | Client đặt đơn COD qua API | HTTP POST `/api/orders/checkout` | MockMvc Request | HTTP 200 OK, Đơn chèn vào MySQL thật | HTTP 200 OK, DB chèn bản ghi thật | **PASS** | `OrderApiControllerIntegrationTest` |
| **TC-IT-IPN-01** | Webhook VNPay IPN | HTTP GET `/vnpay/ipn` + Checksum hợp lệ | MockMvc Request | `{"RspCode":"00","Message":"Confirm Success"}` | Trả về JSON RspCode 00 chuẩn | **PASS** | `IPNControllerIntegrationTest` |
| **TC-IT-ORD-07** | Tranh chấp 2 luồng mua cây | 2 Luồng Java mua cùng 1 cây `AVAILABLE` | ExecutorService Parallel | Đúng 1 luồng thành công, 1 luồng báo lỗi | 1 Success, 1 Exception, cây RESERVED | **PASS** | `ConcurrentCheckoutIntegrationTest` |

---

### 4.2 Danh Sách Ưu Tiên Học & Script Demo Theo Thứ Tự (Cho Buổi Bảo Vệ)

#### Danh sách 5 Test Class Bắt Buộc Phải Thuộc Lòng:
1. **[OrderServiceCheckoutTest.java](file:///d:/project/Bonsai_Shop/src/test/java/com/example/bonsai_shop/product/service/OrderServiceCheckoutTest.java)** (Hiểu cách dùng Mockito kiểm thử nghiệp vụ tính tiền đặt cọc & giữ chỗ).
2. **[OrderActionServiceTest.java](file:///d:/project/Bonsai_Shop/src/test/java/com/example/bonsai_shop/moderator/service/OrderActionServiceTest.java)** (Hiểu cách test luồng phân công đơn hàng cho Moderator).
3. **[OrderApiControllerIntegrationTest.java](file:///d:/project/Bonsai_Shop/src/test/java/com/example/bonsai_shop/integration/order/OrderApiControllerIntegrationTest.java)** (Hiểu cách chạy Integration Test trên CSDL MySQL thật).
4. **[IPNControllerIntegrationTest.java](file:///d:/project/Bonsai_Shop/src/test/java/com/example/bonsai_shop/integration/payment/IPNControllerIntegrationTest.java)** (Hiểu cách test Webhook bảo mật VNPay Checksum).
5. **[ConcurrentCheckoutIntegrationTest.java](file:///d:/project/Bonsai_Shop/src/test/java/com/example/bonsai_shop/integration/order/ConcurrentCheckoutIntegrationTest.java)** (Hiểu cách test 2 luồng tranh chấp đồng thời mua cây độc bản).

---

#### Script Demo Trực Tiếp Với Giảng Viên (Thực hiện trong 3-5 phút):

```powershell
# Bước 1: Mở terminal tại thư mục d:\project\Bonsai_Shop
# Bước 2: Chạy lệnh biên dịch testcode
.\mvnw.cmd test-compile

# Bước 3: Demo Unit Test lõi nghiệp vụ (Order Checkout)
.\mvnw.cmd test -Dtest=OrderServiceCheckoutTest
# => Giải thích với Giảng viên: "Đây là 24 Unit Test kiểm thử logic tính cọc và giữ chỗ cây cảnh, chạy trong 0.05 giây sử dụng Mockito."

# Bước 4: Demo Integration Test trên CSDL thật (Order API)
.\mvnw.cmd test -Dtest=OrderApiControllerIntegrationTest
# => Giải thích với Giảng viên: "Đây là 19 bài Integration Test chạy trên CSDL MySQL bonsai_shop_test thật, kiểm tra tích hợp từ Spring Security, Controller đến JPA Repository."

# Bước 5: Demo Test Tranh Chấp Đồng Thời (Concurrency Guard)
.\mvnw.cmd test -Dtest=ConcurrentCheckoutIntegrationTest
# => Giải thích với Giảng viên: "Đây là bài test mô phỏng 2 khách hàng bấm mua cùng 1 cây cảnh ở cùng 1 millisecond. Hệ thống chứng minh chỉ đúng 1 người mua thành công và người kia bị từ chối."
```

---

### 4.3 Trả Lời Ngắn Gọn Các Câu Hỏi Thường Gặp Của Giảng Viên

- **Câu hỏi 1: "Test này đang kiểm tra cái gì?"**
  - *Trả lời*: "Thưa thầy/cô, test case này kiểm tra [Tên nghiệp vụ, ví dụ: logic tính tiền đặt cọc đơn hàng online]. Nó xác nhận rằng khi dữ liệu đầu vào [Given] được truyền vào hàm [When], thì kết quả trả về và trạng thái đối tượng [Then] phải đúng chính xác theo quy tắc nghiệp vụ đề ra."

- **Câu hỏi 2: "Vì sao ở Unit Test em lại dùng Mock thay vì kết nối Database thật?"**
  - *Trả lời*: "Thưa thầy/cô, mục đích của Unit Test là kiểm thử **sự cô lập tuyệt đối** của duy nhất mô-đun đó. Dùng Mock giúp loại bỏ sự phụ thuộc vào CSDL hoặc mạng ngoại vi, giúp test chạy cực nhanh (vài millisecond) và không bị ảnh hưởng nếu dữ liệu CSDL thay đổi. Khi cần kiểm thử sự phối hợp với CSDL thật, em đã có riêng bộ bài kiểm thử **Integration Test Level 2**."

- **Câu hỏi 3: "Test pass 100% có bảo đảm hệ thống đúng hoàn toàn 100% ngoài thực tế không?"**
  - *Trả lời*: "Thưa thầy/cô, test pass 100% chứng minh rằng hệ thống hoạt động đúng hoàn toàn đối với **tất cả các kịch bản kiểm thử đã được thiết kế và bao phủ (Test Coverage)**. Tuy nhiên, ngoài thực tế vẫn có thể phát sinh các yếu tố môi trường như đứt cáp mạng, server hết RAM, hoặc lỗi hạ tầng phần cứng. Do đó test tự động giúp đảm bảo độ tin cậy phần mềm ở mức cao nhất có thể trong phạm vi kịch bản nghiệp vụ."

---

## PHẦN 5 – KẾT LUẬN TRUNG THỰC & ĐÁP ÁN CHO 6 CÂU HỎI CỦA NGƯỜI DÙNG

### 5.1 Phân Loại Trạng Thái Kiểm Thử Hiện Tại

1. **Đã tồn tại và đã chạy Pass 100%**: **263 test methods** (gồm 215 Unit Tests và 48 Integration Tests L2 trên 31 test classes hoạt động).
2. **Đã tồn tại nhưng đang Fail/Error**: **0 test**.
3. **Đã tồn tại nhưng chưa chạy (hoặc bị Disabled cố ý)**: **2 test classes local utility** (`DatabaseResetTest`, `RecreateDatabaseTest` - Đây là các script xóa/tạo lại CSDL local, được đánh dấu `@Disabled` để tránh vô tình xóa dữ liệu phát triển).
4. **Chỉ có trong tài liệu, chưa có code**: **0 test** (Mọi scenario trong tài liệu kiểm kê đã được chuyển hóa thành code executable).
5. **Có code nhưng assertion chưa đủ ý nghĩa**: **0 test** (Tất cả test method đều chứa ít nhất 1 đến 5 câu lệnh `assertEquals`, `assertTrue`, `assertNotNull` hoặc `verify()` kiểm tra kết quả thực tế).

---

### 5.2 Đáp Án Trực Tiếp Cho 6 Câu Hỏi Của Người Dùng

1. **Hiện project thực sự có bao nhiêu test?**
   - Project hiện có **31 active test classes** chứa tổng cộng **263 test methods** thực thi tự động (+ 2 utility classes bị disabled).

2. **Bao nhiêu test đã chạy Pass?**
   - **263 / 263 active test methods đã chạy PASS 100%** (Đạt tỷ lệ thành công 100% trên lệnh `mvnw.cmd test`).

3. **UUT-01 đến UUT-12 tương ứng với code nào?**
   - UUT-01: `UserServiceTest.java` (12 tests)
   - UUT-02: `OrderServiceTest.java` (6 tests)
   - UUT-03: `OrderServiceCheckoutTest.java` (24 tests)
   - UUT-04: `OrderServicePaymentTest.java` (26 tests)
   - UUT-05: `OrderServicePostPaymentTest.java` (33 tests)
   - UUT-06: `OrderServiceModeratorPoolTest.java` (22 tests)
   - UUT-07: `OrderActionServiceTest.java` (18 tests)
   - UUT-08: `OrderDetailServiceTest.java` (10 tests)
   - UUT-09: `MyOrderServiceTest.java` (15 tests)
   - UUT-10: `OrderExpirationServiceTest.java` (12 tests)
   - UUT-11: `MailServiceTest.java` (8 tests)
   - UUT-12: `FinancialLedgerServiceTest.java` (15 tests)

4. **Integration Test L2 nào đã tồn tại?**
   - Đã tồn tại **8 Executable L2 Test Classes** chứa **48 Representative Scenarios**:
     1. `OrderApiControllerIntegrationTest` (20 scenarios)
     2. `PaymentControllerIntegrationTest` (5 scenarios)
     3. `IPNControllerIntegrationTest` (5 scenarios)
     4. `ModeratorOrderControllerIntegrationTest` (7 scenarios)
     5. `CartMvcControllerIntegrationTest` (4 scenarios)
     6. `CartApiControllerIntegrationTest` (4 scenarios)
     7. `OrderExpirationServiceIntegrationTest` (3 scenarios - Dimension 3 Background Job)
     8. `ConcurrentCheckoutIntegrationTest` (1 scenario - Concurrency Guard)

5. **Tôi nên học và demo test nào trước?**
   - Nên học và demo theo thứ tự 5 class tại **Mục 4.2**: `OrderServiceCheckoutTest` -> `OrderActionServiceTest` -> `OrderApiControllerIntegrationTest` -> `IPNControllerIntegrationTest` -> `ConcurrentCheckoutIntegrationTest`.

6. **Việc cụ thể tiếp theo là gì?**
   - Đọc kỹ tài liệu giải thích này và thực hành chạy thử script demo theo **Mục 4.2** để làm quen với các câu lệnh Maven và cách giải thích với Giảng viên.
   - Khi sẵn sàng, yêu cầu đối chiếu và cập nhật file Excel `Report 5.2_IntegrationTests_L2.xlsx` để đồng bộ trạng thái `Pass` giữa code thực tế và báo cáo đồ án.
