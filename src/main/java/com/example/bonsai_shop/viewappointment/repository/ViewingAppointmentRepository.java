package com.example.bonsai_shop.viewappointment.repository;

import com.example.bonsai_shop.entity.User;
import com.example.bonsai_shop.entity.ViewingAppointment;
import com.example.bonsai_shop.artisan.dto.ArtisanAppointmentDTO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;



import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ViewingAppointmentRepository extends JpaRepository<ViewingAppointment,Integer>{

    @Query("""
SELECT COUNT(v) > 0
FROM ViewingAppointment v
WHERE v.appointmentDate = :appointmentDate
AND v.status IN ('PENDING','APPROVED')
""")
    boolean existsActiveAppointment(LocalDateTime appointmentDate);

    List<ViewingAppointment> findByCustomer(User customer);

    Optional<ViewingAppointment> findByAppointmentIdAndCustomer(Integer appointmentId, User customer);

    boolean existsViewingAppointmentByAppointmentDateAndAppointmentIdNot(LocalDateTime appointmentDate, Integer appointmentId);

    @Query("""
SELECT new com.example.bonsai_shop.artisan.dto.ArtisanAppointmentDTO(
    a.appointmentId,
    a.appointmentDate,
    a.status,
    a.note,
    p.productCode,
    p.productName,
    c.fullName,
    c.phone,
    c.email
)
FROM ViewingAppointment a
JOIN a.product p
JOIN a.customer c
WHERE p.createdBy = :user
ORDER BY a.appointmentDate DESC
""")
    List<ArtisanAppointmentDTO> findAllAppointmentsByArtisan(
            @Param("user") User user);

    @Query("""
SELECT a
FROM ViewingAppointment a
JOIN a.product p
WHERE a.appointmentId = :appointmentId
AND p.createdBy = :user
""")
    Optional<ViewingAppointment> findByAppointmentIdAndArtisan(
            @Param("appointmentId") Integer appointmentId,
            @Param("user") User user);
}
