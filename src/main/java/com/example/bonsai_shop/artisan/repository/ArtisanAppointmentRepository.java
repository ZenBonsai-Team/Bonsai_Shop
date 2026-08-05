package com.example.bonsai_shop.artisan.repository;

import com.example.bonsai_shop.entity.ViewingAppointment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ArtisanAppointmentRepository extends JpaRepository<ViewingAppointment, Integer> {

    Optional<ViewingAppointment> findByAppointmentId(Integer appointmentId);

    List<ViewingAppointment> findByAppointmentDateBetween(LocalDateTime appointmentDateStart, LocalDateTime appointmentDateEnd);

    List<ViewingAppointment> findByStatus(String status);
}
