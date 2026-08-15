package com.example.bonsai_shop.moderator.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * [DTO TIẾP NHẬN YÊU CẦU THỰC HIỆN HÀNH ĐỘNG CỦA MODERATOR - ORDER ACTION REQUEST DTO]
 *
 * Mục đích:
 * - Đóng gói dữ liệu AJAX từ giao diện Moderator khi thực hiện các hành động: claim, approve, reject, return_inventory, complete, customer_no_show, record_fault_refund.
 */
@Data
public class OrderActionRequestDTO {
    /** Tên hành động: claim, approve, reject, return_inventory, complete, customer_no_show, record_fault_refund. */
    private String action;

    /** Lý do từ chối/hủy/bùng cọc/hoàn tiền. */
    private String reason;

    /** Phí cẩu cây nặng (dùng khi approve). */
    private BigDecimal craneFee;

    /** Phí vận chuyển (dùng khi approve). */
    private BigDecimal shippingFee;

    /** Số tiền đặt cọc cần thu (dùng khi approve flow DEPOSIT). */
    private BigDecimal depositAmount;

    /** Bên chịu trách nhiệm lỗi: NURSERY (nhà vườn) hoặc DELIVERY (vận chuyển). */
    private String faultParty;

    /** Số tiền hoàn trả (tự động tính 100% số tiền khách đã trả). */
    private BigDecimal refundAmount;

    /** Ghi chú minh chứng (ảnh hỏng, biên bản vỡ chậu...). */
    private String evidenceNote;

    /** Mã tham chiếu ủy nhiệm chi hoàn tiền ngân hàng. */
    private String externalReference;

    /** Khách hàng có giữ lại cây không (mặc định false). */
    private Boolean customerKeepsTree;

    /** Cách thức xử lý cây: RETURNED_AND_RESELLABLE, RETURNED_AND_DAMAGED, NOT_RETURNED, UNDER_INSPECTION. */
    private String productResolution;
}
