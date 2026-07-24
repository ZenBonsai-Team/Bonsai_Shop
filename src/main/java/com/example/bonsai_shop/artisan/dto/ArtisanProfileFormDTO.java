package com.example.bonsai_shop.artisan.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ArtisanProfileFormDTO {

    @NotBlank(message = "Vui lòng nhập tên nghệ nhân.")
    @Size(max = 255, message = "Tên nghệ nhân không được vượt quá 255 ký tự.")
    private String fullName;

    @Size(max = 5000, message = "Giới thiệu nghệ nhân quá dài.")
    private String bio;

    @Min(value = 0, message = "Năm kinh nghiệm không được âm.")
    private Integer yearsOfExperience;

    @Size(max = 255, message = "Chuyên môn không được vượt quá 255 ký tự.")
    private String specialty;
}
