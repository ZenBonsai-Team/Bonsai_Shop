package com.example.bonsai_shop.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * Thực thể quản lý Cơ hội chốt đơn / Khách hàng tiềm năng (Leads) phát sinh trong phiên Live Stream.
 * Được hệ thống tự động trích lọc từ bình luận chat khi khớp SĐT hoặc mã sản phẩm.
 */
@Entity
@Table(name = "live_lead")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class LiveLead {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "LeadID")
    private Integer leadId;

    // Liên kết với phiên Live Stream đang diễn ra
    @ManyToOne
    @JoinColumn(name = "SessionID", nullable = false)
    private LiveSession liveSession;

    // Liên kết với tác phẩm Bonsai khách hàng đang quan tâm (nếu bắt được mã BON-xxx trong chat)
    @ManyToOne
    @JoinColumn(name = "ProductID")
    private Product product;

    // Tên hiển thị của người bình luận (Tên khách hàng)
    @Column(name = "ViewerName", nullable = false, length = 150)
    private String viewerName;

    // Số điện thoại của khách hàng (Tự động lọc được bằng Regex)
    @Column(name = "PhoneNumber", length = 20)
    private String phoneNumber;

    // Nguyên văn nội dung bình luận của khách hàng trên live chat
    @Column(name = "RawComment", nullable = false, columnDefinition = "TEXT")
    private String rawComment;

    /** 
     * Phân loại ý định mua hàng/tương tác:
     * - CHOT_DON: Khách hàng muốn chốt đơn mua cây (Ví dụ: "chốt", "mua")
     * - TU_VAN: Khách hàng cần tư vấn thêm dáng, thế, giá cả (Ví dụ: "tư vấn", "xin giá")
     * - GOI_LAI: Khách để lại thông tin hẹn gọi lại sau
     */
    @Column(name = "IntentType", nullable = false, length = 50)
    private String intentType;

    /** 
     * Tình trạng xử lý cơ hội chốt đơn:
     * - PENDING: Chờ xử lý liên hệ
     * - CONTACTED: Đang liên hệ / Đã gọi điện
     * - DONE: Hoàn thành chốt giao dịch
     */
    @Column(name = "LeadStatus", nullable = false, length = 50)
    private String leadStatus = "PENDING";

    // Ghi chú của nhân viên hỗ trợ / điều phối viên khi gọi chốt đơn
    @Column(name = "Notes", length = 500)
    private String notes;

    @Column(name = "CreatedAt")
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "UpdatedAt")
    private LocalDateTime updatedAt = LocalDateTime.now();
}
