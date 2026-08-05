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

    @Builder.Default
    @Column(name = "AutoApprove", nullable = false)
    private Boolean autoApprove = true;

    @Builder.Default
    @Column(name = "AutoApproveAfter", nullable = false)
    private Integer autoApproveAfter = 5;

    @Builder.Default
    @Column(name = "AutoComplete", nullable = false)
    private Boolean autoComplete = true;

    @Builder.Default
    @Column(name = "AutoCompleteAfter", nullable = false)
    private Integer autoCompleteAfter = 60;

    @Column(name = "PauseFrom")
    private LocalDateTime pauseFrom;

    @Column(name = "PauseTo")
    private LocalDateTime pauseTo;

    @Column(name = "PauseReason", columnDefinition = "TEXT")
    private String pauseReason;

    @ManyToOne
    @JoinColumn(name = "UpdatedBy")
    private User updatedBy;

    @Column(name = "UpdatedAt")
    private LocalDateTime updatedAt;
}
