package com.example.bonsai_shop.customer.repository;
import com.example.bonsai_shop.entity.User;
import com.example.bonsai_shop.entity.ViewingAppointment;
import com.example.bonsai_shop.seller.dto.SellerAppointmentDTO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;



import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ViewingAppointmentRepository extends JpaRepository<ViewingAppointment,Integer>{

    boolean existsViewingAppointmentByAppointmentDate(LocalDateTime appointmentDate);

    List<ViewingAppointment> findByCustomer(User customer);

    Optional<ViewingAppointment> findByAppointmentIdAndCustomer(Integer appointmentId, User customer);

    boolean existsViewingAppointmentByAppointmentDateAndAppointmentIdNot(LocalDateTime appointmentDate, Integer appointmentId);

    @Query("""
SELECT new com.example.bonsai_shop.seller.dto.SellerAppointmentDTO(
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
WHERE p.seller = :seller
ORDER BY a.appointmentDate DESC
""")
    List<SellerAppointmentDTO> findAllAppointmentsBySeller(
            @Param("seller") User seller);
}
