package com.example.bonsai_shop.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "appointment_setting")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AppointmentSetting {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "SettingID")
    private Integer settingId;

    @Column(name = "PauseFrom")
    private LocalDateTime pauseFrom;

    @Column(name = "PauseTo")
    private LocalDateTime pauseTo;

    @Column(name = "AutoComplete", nullable = false)
    @Builder.Default
    private Boolean autoComplete = true;

    @ManyToOne
    @JoinColumn(name = "UpdatedBy")
    private User updatedBy;

    @Column(name = "UpdatedAt")
    private LocalDateTime updatedAt;
}
