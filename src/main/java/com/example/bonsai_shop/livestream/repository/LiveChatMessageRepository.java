package com.example.bonsai_shop.livestream.repository;

import com.example.bonsai_shop.entity.LiveChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LiveChatMessageRepository extends JpaRepository<LiveChatMessage, Integer> {

    /** Load all messages for a session in chronological order (for history on page open) */
    List<LiveChatMessage> findByLiveSessionSessionIdOrderBySentAtAsc(Integer sessionId);
}
