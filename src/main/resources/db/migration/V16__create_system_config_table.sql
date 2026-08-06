CREATE TABLE `system_config` (
  `config_key` varchar(100) NOT NULL,
  `config_value` text DEFAULT NULL,
  `description` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`config_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO `system_config` (`config_key`, `config_value`, `description`) VALUES
('home_banner_title', 'Minh Kỷ Garden', 'Trang chủ - Tiêu đề Banner'),
('home_banner_subtitle', 'Nâng tầm không gian sống bằng nghệ thuật tạo hình cây cảnh độc bản', 'Trang chủ - Phụ đề Banner'),
('home_banner_image', '/images/hero-bonsai.png', 'Trang chủ - URL Ảnh Banner'),

('marketplace_banner_title', 'Tác Phẩm Bonsai Di Sản & Nghệ Thuật Độc Bản', 'Chợ cây - Tiêu đề Banner'),
('marketplace_banner_subtitle', 'Nơi lưu giữ hồn xưa đất Việt qua từng thế cây, dáng đá. Mỗi tác phẩm đều đi kèm hồ sơ nghệ nhân, phả hệ minh bạch và chứng nhận thẩm định từ nhà vườn.', 'Chợ cây - Phụ đề Banner'),
('marketplace_banner_image', '/images/hero-banner-1.jpg', 'Chợ cây - URL Ảnh Banner'),

('community_banner_title', 'Cộng Đồng Bonsai Việt Nam', 'Cộng đồng - Tiêu đề Banner'),
('community_banner_subtitle', 'Nơi giao lưu, chia sẻ kinh nghiệm tạo tác và chăm sóc cây cảnh cùng các nghệ nhân hàng đầu.', 'Cộng đồng - Phụ đề Banner'),
('community_banner_image', '/images/hero-banner-1.jpg', 'Cộng đồng - URL Ảnh Banner'),

('luxury_banner_title', 'Hàng Tuyển Tác Phẩm Độc Bản', 'Hàng tuyển - Tiêu đề Banner'),
('luxury_banner_subtitle', 'Bộ sưu tập siêu phẩm Bonsai quý hiếm, đỉnh cao nghệ thuật dành cho các nhà sưu tầm thượng lưu.', 'Hàng tuyển - Phụ đề Banner'),
('luxury_banner_image', '/images/hero-banner-1.jpg', 'Hàng tuyển - URL Ảnh Banner');
