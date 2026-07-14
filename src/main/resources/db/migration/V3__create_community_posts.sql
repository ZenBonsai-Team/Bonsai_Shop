USE `bonsai_shop`;

CREATE TABLE IF NOT EXISTS `community_post` (
  `PostID` int NOT NULL AUTO_INCREMENT,
  `Title` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `Content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `Summary` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `AuthorName` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `AuthorAvatar` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `Category` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `ImageUrl` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `CreatedAt` datetime DEFAULT CURRENT_TIMESTAMP,
  `ReadTime` int DEFAULT 5,
  `LikesCount` int DEFAULT 0,
  `CommentsCount` int DEFAULT 0,
  PRIMARY KEY (`PostID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO `community_post` (`Title`, `Content`, `Summary`, `AuthorName`, `AuthorAvatar`, `Category`, `ImageUrl`, `ReadTime`, `LikesCount`, `CommentsCount`) VALUES 
(
  'Nghệ thuật Bonsai - Triết lý sống trong từng nhành cây',
  'Bonsai không chỉ là chơi cây, mà là hành trình rèn luyện sự kiên nhẫn, tỉ mỉ và cảm nhận vẻ đẹp của thiên nhiên thu nhỏ. Triết lý Zen của người Nhật Bản cũng như lối sống thanh bạch, tự do của ông cha ta được phản ánh rất rõ qua nghệ thuật tạo dáng cây cảnh. Mỗi tác phẩm nghệ thuật Bonsai đều ẩn chứa một câu chuyện, một lời nhắn nhủ của thiên nhiên và bàn tay nghệ nhân gửi gắm qua năm tháng.',
  'Bonsai không chỉ là chơi cây, mà là hành trình rèn luyện sự kiên nhẫn, tỉ mỉ và cảm nhận vẻ đẹp của thiên nhiên thu nhỏ...',
  'Nghệ nhân Nguyễn Văn Thành',
  'https://images.unsplash.com/photo-1534528741775-53994a69daeb?auto=format&fit=crop&q=80&w=150',
  'Nghệ thuật',
  'https://images.unsplash.com/photo-1512428559087-560fa5ceab42?auto=format&fit=crop&q=80&w=800',
  8,
  128,
  24
),
(
  'Kỹ thuật bấm tỉa đọt non và tạo tán cho cây Tùng La Hán',
  'Đối với dòng Tùng La Hán (Vạn Niên Tùng), kỹ thuật bấm tỉa đọt non quyết định trực tiếp tới mật độ lá và độ dày của bông tán. Thời điểm vàng để tiến hành bấm tỉa đọt non là vào mùa xuân hoặc đầu mùa thu khi thời tiết ấm áp, tránh bấm tỉa khi nắng quá gắt hoặc giữa mùa đông buốt giá. Dùng kéo sắc bén cắt tỉa sát nách lá để kích thích các chồi phụ phát triển...',
  'Hướng dẫn chi tiết kỹ thuật bấm tỉa đọt non, tỉa lá già và kéo kẽm tạo bông tán tròn đều cho cây Tùng La Hán nghệ thuật.',
  'Lão nông Trần Duy',
  'https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?auto=format&fit=crop&q=80&w=150',
  'Kỹ thuật',
  'https://images.unsplash.com/photo-1502082553048-f009c37129b9?auto=format&fit=crop&q=80&w=800',
  12,
  95,
  18
),
(
  'Kinh nghiệm chăm sóc Bonsai mini văn phòng luôn xanh tốt',
  'Chơi Bonsai mini đặt bàn làm việc đang là xu hướng rất hot. Tuy nhiên, do điều kiện trong phòng máy lạnh thiếu ánh sáng tự nhiên và gió trời, cây rất dễ bị héo úa hoặc úng rễ. Kinh nghiệm là bạn cần chọn các dòng cây chịu bóng tốt như Kim Tiền, Vạn Niên Thanh hoặc Nhất Chi Mai. Mỗi tuần nên mang cây ra ban công hứng nắng sáng khoảng 2 lần, mỗi lần 2-3 tiếng...',
  'Chia sẻ mẹo tưới nước, bón phân vi lượng và cách phơi nắng phục hồi cây cảnh mini khi trồng trong phòng điều hòa.',
  'Lê Minh Anh',
  'https://images.unsplash.com/photo-1494790108377-be9c29b29330?auto=format&fit=crop&q=80&w=150',
  'Chăm sóc',
  'https://images.unsplash.com/photo-1583847268964-b28dc8f51f92?auto=format&fit=crop&q=80&w=800',
  6,
  82,
  11
),
(
  'Hành trình tìm kiếm gốc Sanh cổ Nam Điền của đời tôi',
  'Nam Điền (Nam Trực, Nam Định) được ví như cái nôi của phôi Sanh cổ thụ. Tôi đã dành hơn 6 tháng ròng rã, rong ruổi khắp các ngõ ngách vườn tược vùng quê này để tìm kiếm một gốc phôi đáp ứng đúng tiêu chuẩn "Cổ - Kỳ - Mỹ". Và cơ duyên đã dẫn lối tôi đến một căn nhà cổ của một cụ lão nghệ nhân đã ngoài 80 tuổi, nơi đang gìn giữ một báu vật tuyệt mỹ...',
  'Câu chuyện đầy cảm xúc về hành trình lặn lội tìm kiếm, thuyết phục giao dịch và vận chuyển gốc Sanh cổ Nam Điền hơn 80 năm tuổi.',
  'Nghệ nhân Phạm Hoàng',
  'https://images.unsplash.com/photo-1628157582853-a796fa650a6a?auto=format&fit=crop&q=80&w=150',
  'Câu chuyện',
  'https://images.unsplash.com/photo-1599599810769-bcde5a160d32?auto=format&fit=crop&q=80&w=800',
  15,
  210,
  42
);
