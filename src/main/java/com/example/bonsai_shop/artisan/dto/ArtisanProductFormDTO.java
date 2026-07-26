package com.example.bonsai_shop.artisan.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ArtisanProductFormDTO {

    @NotNull(message = "Vui lòng chọn variety.")
    private Integer varietyId;

    @NotNull(message = "Vui lòng chọn segment.")
    private Integer segmentId;

    @NotBlank(message = "Vui lòng nhập tên cây.")
    @Size(max = 255, message = "Tên cây không được vượt quá 255 ký tự.")
    private String productName;

    private String description;

    @NotNull(message = "Vui lòng nhập tuổi cây.")
    @Min(value = 0, message = "Tuổi cây không được âm.")
    private Integer age;

    @NotNull(message = "Vui lòng nhập chiều cao cây.")
    @PositiveOrZero(message = "Chiều cao cây không được âm.")
    private Float height;

    @NotNull(message = "Vui lòng nhập đường kính thân cây.")
    @PositiveOrZero(message = "Đường kính thân cây không được âm.")
    private Float trunkDiameter;

    @NotBlank(message = "Vui lòng nhập style cây.")
    @Size(max = 255, message = "Style không được vượt quá 255 ký tự.")
    private String style;

    @NotNull(message = "Vui lòng nhập giá sản phẩm.")
    @DecimalMin(value = "0.01", message = "Giá sản phẩm phải lớn hơn 0.")
    private BigDecimal price;

    private String productStatus;

    private List<Integer> tagIds;
}
