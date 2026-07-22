-- V4: Thêm cột ModerationReason vào bảng community_comment
-- Được thêm bởi nhánh KhaBN để hỗ trợ tính năng kiểm duyệt bình luận

ALTER TABLE community_comment
    ADD COLUMN ModerationReason VARCHAR(500) NULL;
