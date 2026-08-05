package com.example.bonsai_shop.customer.repository;

import com.example.bonsai_shop.entity.User;
import com.example.bonsai_shop.entity.ViewingAppointment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ViewingAppointmentRepository extends JpaRepository<ViewingAppointment, Integer> {

    List<ViewingAppointment> findByCustomerOrderByCreatedAtDesc(User customer);

    Optional<ViewingAppointment> findByAppointmentIdAndCustomer(Integer appointmentId, User customer);

    boolean existsByCustomerAndStatusIn(User customer, List<String> status);

}
