package com.example.bonsai_shop.viewappointment.repository;

import com.example.bonsai_shop.artisan.dto.ArtisanAppointmentDTO;
import com.example.bonsai_shop.entity.User;
import com.example.bonsai_shop.entity.ViewingAppointment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ViewingAppointmentRepository extends JpaRepository<ViewingAppointment, Integer> {

    @Query("""
SELECT COUNT(a)
FROM ViewingAppointment a
WHERE a.customer = :customer
AND a.status IN ('PENDING','APPROVED')
""")
    long countActiveAppointmentByCustomer(@Param("customer") User customer);

    List<ViewingAppointment> findByCustomer(User customer);

    Optional<ViewingAppointment> findByAppointmentIdAndCustomer(Integer appointmentId, User customer);

    @Query("""
SELECT new com.example.bonsai_shop.artisan.dto.ArtisanAppointmentDTO(
    a.appointmentId,
    a.appointmentDate,
    a.status,
    a.note,
    c.fullName,
    c.phone,
    c.email
)
FROM ViewingAppointment a
JOIN a.customer c
ORDER BY a.appointmentDate DESC
""")
    List<ArtisanAppointmentDTO> findAllAppointmentsByArtisan();
}
