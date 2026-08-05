package com.example.bonsai_shop.livestream.repository;

import com.example.bonsai_shop.entity.LiveChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LiveChatMessageRepository extends JpaRepository<LiveChatMessage, Integer> {

    /**
     * Load ALL messages for a session in chronological order.
     * Only used internally (e.g., cleanup). Prefer the limited version below for API calls.
     */
    List<LiveChatMessage> findByLiveSessionSessionIdOrderBySentAtAsc(Integer sessionId);

    /**
     * Load the LAST 200 messages for history reload (newest-first, then reversed in service).
     * This prevents loading thousands of messages on page open.
     */
    List<LiveChatMessage> findTop200ByLiveSessionSessionIdOrderBySentAtDesc(Integer sessionId);

    /**
     * Delete all chat messages for a session (called when session ends to free DB space).
     * Chat history is only useful within an active session; leads capture the valuable data.
     */
    void deleteByLiveSessionSessionId(Integer sessionId);
}
