package com.example.bonsai_shop.livestream.repository;

import com.example.bonsai_shop.entity.LiveSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface LiveSessionRepository extends JpaRepository<LiveSession, Integer> {
    Optional<LiveSession> findFirstByStatusOrderByStartTimeDesc(String status);
    List<LiveSession> findAllByOrderByStartTimeDesc();
}
