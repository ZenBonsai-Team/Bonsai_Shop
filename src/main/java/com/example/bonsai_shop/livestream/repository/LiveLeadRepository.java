package com.example.bonsai_shop.livestream.repository;

import com.example.bonsai_shop.entity.LiveLead;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface LiveLeadRepository extends JpaRepository<LiveLead, Integer> {
    List<LiveLead> findByLiveSessionSessionIdOrderByCreatedAtDesc(Integer sessionId);
    List<LiveLead> findByLiveSessionSessionIdAndLeadStatus(Integer sessionId, String leadStatus);
    long countByLiveSessionSessionId(Integer sessionId);
}
