-- V5: Thêm cột Status vào bảng community_comment
-- Hỗ trợ tính năng kiểm duyệt bình luận (APPROVED, PENDING, REJECTED)

ALTER TABLE community_comment
    ADD COLUMN Status VARCHAR(50) NULL DEFAULT 'APPROVED';
