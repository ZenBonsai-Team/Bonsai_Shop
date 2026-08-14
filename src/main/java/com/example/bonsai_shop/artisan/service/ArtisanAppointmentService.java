package com.example.bonsai_shop.artisan.service;

import com.example.bonsai_shop.appointmentSetting.reponsitory.AppointmentSettingRepository;
import com.example.bonsai_shop.artisan.dto.ArtisanAppointmentDTO;
import com.example.bonsai_shop.artisan.repository.ArtisanAppointmentRepository;
import com.example.bonsai_shop.entity.AppointmentSetting;
import com.example.bonsai_shop.entity.User;
import com.example.bonsai_shop.entity.ViewingAppointment;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ArtisanAppointmentService {

  private final ArtisanAppointmentRepository artisanAppointmentRepository;
  private final AppointmentSettingRepository appointmentSettingRepository;


  public List<ArtisanAppointmentDTO> findAllByAppointmentDateBetween(LocalDate appointmentDate) {

   LocalDateTime start = appointmentDate.atStartOfDay();
   LocalDateTime end = start.plusDays(1);

      List<ViewingAppointment> appointments =
              artisanAppointmentRepository.findByAppointmentDateBetween(start, end);

      List<ArtisanAppointmentDTO> result = new ArrayList<>();

      for (ViewingAppointment appointment : appointments) {
          result.add(toDto(appointment));
      }

      return result;

  }

  public ArtisanAppointmentDTO findById(int id) {
      ViewingAppointment appointment =
              artisanAppointmentRepository.findByAppointmentId(id)
                      .orElseThrow(() ->
                              new RuntimeException("Không tìm thấy lịch hẹn."));
      return toDto(appointment);
  }


    private ArtisanAppointmentDTO toDto(ViewingAppointment appointment) {

        ArtisanAppointmentDTO dto = new ArtisanAppointmentDTO();

        dto.setAppointmentId(appointment.getAppointmentId());
        dto.setAppointmentDate(appointment.getAppointmentDate());
        dto.setCreatedAt(appointment.getCreatedAt());
        dto.setStatus(appointment.getStatus());
        dto.setNote(appointment.getNote());

        dto.setCustomerName(appointment.getCustomer().getFullName());
        dto.setCustomerPhone(appointment.getCustomer().getPhone());
        dto.setCustomerEmail(appointment.getCustomer().getEmail());

        return dto;
    }

    @Transactional
    public void handUpdateStatus(int id, String status) {
        ViewingAppointment appointment =
                artisanAppointmentRepository.findByAppointmentId(id)
                        .orElseThrow(() ->
                                new RuntimeException("Không tìm thấy lịch hẹn."));

        if(!appointment.getStatus().equals("PENDING")) {
            throw new RuntimeException("Chỉ lịch đang chờ duyệt mới được cập nhật.");
        }

        if (appointment.getAppointmentDate().isBefore(LocalDateTime.now())) {
            throw new RuntimeException(
                    "Không thể cập nhật lịch đã quá thời gian hẹn."
            );
        }

        status = status.toUpperCase();

        if (!List.of("APPROVED", "REJECTED").contains(status)) {
            throw new RuntimeException("Trạng thái không hợp lệ.");
        }

        appointment.setStatus(status);
        artisanAppointmentRepository.save(appointment);
    }


    @Transactional
    public void handMarkComplete(int id, String status) {
        ViewingAppointment appointment =
                artisanAppointmentRepository.findByAppointmentId(id)
                        .orElseThrow(() ->
                                new RuntimeException("Không tìm thấy lịch hẹn."));

        if(!appointment.getStatus().equals("APPROVED")) {
            throw new RuntimeException("Chỉ lịch được duyệt mới được cập nhật.");
        }

        if (appointment.getAppointmentDate()
                .isAfter(LocalDateTime.now())) {

            throw new RuntimeException(
                    "Chưa đến thời gian lịch hẹn, không thể cập nhật."
            );
        }


        status = status.toUpperCase();

        if (!List.of("COMPLETED", "ABSENT").contains(status)) {
            throw new RuntimeException("Trạng thái không hợp lệ.");
        }

        appointment.setStatus(status);

        artisanAppointmentRepository.save(appointment);
    }

    @Transactional
    public void updateSetting(Boolean autoApprove,
                              Integer autoApproveAfter,
                              Boolean autoComplete,
                              Integer autoCompleteAfter,
                              LocalDateTime pauseFrom,
                              LocalDateTime pauseTo,
                              String pauseReason,
                              User user
    ) throws RuntimeException {
        AppointmentSetting setting = appointmentSettingRepository
                .findFirstByOrderBySettingIdAsc()
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy cấu hình."));

        if (Boolean.TRUE.equals(autoApprove)) {
            validateMinute(autoApproveAfter, "Thời gian tự động duyệt");
        }

        if (Boolean.TRUE.equals(autoComplete)) {
            validateMinute(autoCompleteAfter, "Thời gian tự động hoàn thành");
        }

        boolean hasPause =
                pauseFrom != null &&
                        pauseTo != null &&
                        pauseReason != null &&
                        !pauseReason.trim().isEmpty();
        if (!hasPause) {
            if (pauseFrom != null ||
                    pauseTo != null ||
                    (pauseReason != null && !pauseReason.trim().isEmpty())) {
                throw new RuntimeException(
                        "Thiết lập tạm dừng phải nhập đầy đủ thời gian bắt đầu, kết thúc và lý do.");
            }
        }

        if(hasPause && pauseFrom.isAfter(pauseTo)){
            throw new RuntimeException("Khoảng thời gian tạm dừng nhận lịch không hợp lệ.");
        }

        if (hasPause && pauseFrom.isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Thời gian bắt đầu phải lớn hơn thời điểm hiện tại.");
        }


        if(pauseReason != null && pauseReason.length() > 500){
            throw new RuntimeException("Lý do vắng không được vượt quá 500 ký tự.");
        }

        if (!autoApprove) {
            autoApproveAfter = setting.getAutoApproveAfter();
        }

        if (!autoComplete) {
            autoCompleteAfter = setting.getAutoCompleteAfter();
        }

        setting.setAutoApprove(autoApprove);
        setting.setAutoApproveAfter(autoApproveAfter);
        setting.setAutoComplete(autoComplete);
        setting.setAutoCompleteAfter(autoCompleteAfter);
        setting.setPauseFrom(pauseFrom);
        setting.setPauseTo(pauseTo);
        setting.setPauseReason(pauseReason);
        setting.setUpdatedBy(user);
        setting.setUpdatedAt(LocalDateTime.now());

        appointmentSettingRepository.save(setting);
  }

    private void validateMinute(Integer value, String fieldName) {
        if (value == null || value < 1 || value > 120) {
            throw new RuntimeException(fieldName + " phải từ 1 đến 120 phút.");
        }
    }

    @Transactional
    public int processAutoApprove(){
        AppointmentSetting setting = appointmentSettingRepository
                .findFirstByOrderBySettingIdAsc()
                .orElseThrow(() -> new RuntimeException("Không tìm thấy cấu hình."));

        if(setting.getAutoApprove() == false){
            return 0;
        }

        LocalDateTime now = LocalDateTime.now();
        List<ViewingAppointment> appointments = artisanAppointmentRepository.findByStatus("PENDING");

        int countPending = 0;

        for (ViewingAppointment appointment : appointments) {
            LocalDateTime approveTime = appointment.getCreatedAt().plusMinutes(setting.getAutoApproveAfter());
            if (!approveTime.isAfter(now)) {
                appointment.setStatus("APPROVED");
                artisanAppointmentRepository.save(appointment);
                countPending++;
            }
        }
        return countPending;
    }
  @Transactional
    public int processAutoComplete(){
      AppointmentSetting setting = appointmentSettingRepository
              .findFirstByOrderBySettingIdAsc()
              .orElseThrow(() -> new RuntimeException("Không tìm thấy cấu hình."));

      if(setting.getAutoComplete() == false){
          return 0;
      }

      LocalDateTime now = LocalDateTime.now();
      List<ViewingAppointment> appointments = artisanAppointmentRepository.findByStatus("APPROVED");

      int countApprove = 0;

      for (ViewingAppointment appointment : appointments) {
          LocalDateTime completedTime = appointment.getAppointmentDate().plusMinutes(setting.getAutoCompleteAfter());
          if (!completedTime.isAfter(now)) {
          appointment.setStatus("COMPLETED");
              artisanAppointmentRepository.save(appointment);
              countApprove++;
          }
      }
      return countApprove;
  }


}
