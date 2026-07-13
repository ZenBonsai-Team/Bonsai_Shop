package com.example.bonsai_shop.customer.repository;
import com.example.bonsai_shop.entity.ViewingAppointment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;

public interface ViewingAppointmentRepository extends JpaRepository<ViewingAppointment,Integer>{

    boolean existsViewingAppointmentByAppointmentDate(LocalDateTime appointmentDate);

}
