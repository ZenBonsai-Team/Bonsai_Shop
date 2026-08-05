-- V13: Create live_chat_message table to persist chat history across page refreshes.
-- Design notes:
--   - Messages are only kept while a session is ONGOING.
--   - When a session ends, messages are deleted via LiveStreamService.endSession().
--   - On page open, only the last 200 messages are loaded (findTop200...OrderBySentAtDesc).
--   - Index on (SessionID, SentAt DESC) makes the "last 200" query fast even with many rows.
CREATE TABLE IF NOT EXISTS live_chat_message (
    MessageID   INT AUTO_INCREMENT PRIMARY KEY,
    SessionID   INT NOT NULL,
    Author      VARCHAR(100) NOT NULL,
    Message     TEXT NOT NULL,
    Source      VARCHAR(20) DEFAULT 'WEB',
    SentAt      DATETIME DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_chat_msg_session FOREIGN KEY (SessionID) REFERENCES live_session(SessionID) ON DELETE CASCADE
);

-- Index for fast "last N messages per session" queries
DROP PROCEDURE IF EXISTS EnsureLiveChatMessageIndexes;

DELIMITER //

CREATE PROCEDURE EnsureLiveChatMessageIndexes()
BEGIN
  IF NOT EXISTS (
    SELECT 1
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'live_chat_message'
      AND index_name = 'idx_chat_msg_session_time'
  ) THEN
    CREATE INDEX idx_chat_msg_session_time ON live_chat_message (SessionID, SentAt DESC);
  END IF;
END //

DELIMITER ;

CALL EnsureLiveChatMessageIndexes();
DROP PROCEDURE EnsureLiveChatMessageIndexes;
