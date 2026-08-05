-- V13: Create live_chat_message table to persist chat history across page refreshes.
CREATE TABLE IF NOT EXISTS `live_chat_message` (
    `MessageID`   INT AUTO_INCREMENT PRIMARY KEY,
    `SessionID`   INT NOT NULL,
    `Author`      VARCHAR(100) NOT NULL,
    `Message`     TEXT NOT NULL,
    `Source`      VARCHAR(20) DEFAULT 'WEB',
    `SentAt`      DATETIME DEFAULT CURRENT_TIMESTAMP,
    KEY `idx_chat_msg_session_time` (`SessionID`, `SentAt` DESC),
    CONSTRAINT `fk_chat_msg_session` FOREIGN KEY (`SessionID`) REFERENCES `live_session` (`SessionID`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
