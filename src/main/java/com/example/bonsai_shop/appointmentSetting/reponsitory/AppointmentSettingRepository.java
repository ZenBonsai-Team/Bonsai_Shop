package com.example.bonsai_shop.appointmentSetting.reponsitory;

import com.example.bonsai_shop.entity.AppointmentSetting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AppointmentSettingRepository extends JpaRepository<AppointmentSetting, Integer> {

    Optional<AppointmentSetting> findFirstByOrderBySettingIdAsc();

}
