package com.example.bonsai_shop.artisan.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
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
// DTO nhận dữ liệu form tạo/cập nhật sản phẩm của artisan.
public class ArtisanProductFormDTO {

    @NotNull(message = "Vui lòng chọn loại cây.")
    // Giống cây được chọn cho sản phẩm.
    private Integer varietyId;

    @NotNull(message = "Vui lòng chọn phân khúc.")
    // Phân khúc sản phẩm dùng để xác định cách hiển thị giá.
    private Integer segmentId;

    @NotBlank(message = "Vui lòng nhập tên cây.")
    @Size(max = 100, message = "Tên cây không được vượt quá 100 ký tự.")
    @Pattern(regexp = "^[\\p{L}\\p{N}\\s.,'()\\-]+$", message = "Tên cây chỉ được nhập chữ, số, khoảng trắng và các dấu . , ' - ( ).")
    // Tên sản phẩm hiển thị trên marketplace.
    private String productName;

    @Size(max = 1000, message = "Mô tả không được vượt quá 1000 ký tự.")
    // Mô tả chi tiết sản phẩm.
    private String description;

    @Size(max = 1500, message = "Câu chuyện cây không được vượt quá 1500 ký tự.")
    // Câu chuyện hoặc nguồn gốc của cây.
    private String treeStory;

    @NotNull(message = "Vui lòng nhập tuổi cây.")
    @Min(value = 1, message = "Tuổi cây phải lớn hơn 0.")
    @Max(value = 1000, message = "Tuổi cây không được vượt quá 1000 năm.")
    // Tuổi cây, bắt buộc khi publish.
    private Integer age;

    @NotNull(message = "Vui lòng nhập chiều cao cây.")
    @DecimalMin(value = "0.01", message = "Chiều cao cây phải lớn hơn 0.")
    @DecimalMax(value = "1000.00", message = "Chiều cao cây không được vượt quá 1000 cm.")
    // Chiều cao cây, bắt buộc khi publish.
    private Float height;

    @NotNull(message = "Vui lòng nhập đường kính thân cây.")
    @DecimalMin(value = "0.01", message = "Đường kính thân cây phải lớn hơn 0.")
    @DecimalMax(value = "500.00", message = "Đường kính thân cây không được vượt quá 500 cm.")
    // Đường kính thân, bắt buộc khi publish.
    private Float trunkDiameter;

    @NotBlank(message = "Vui lòng nhập dáng cây.")
    @Size(max = 100, message = "Dáng cây không được vượt quá 100 ký tự.")
    @Pattern(regexp = "^[\\p{L}\\s'\\-]+$", message = "Dáng cây chỉ được nhập chữ, khoảng trắng và dấu ' -.")
    // Dáng/thế bonsai, bắt buộc khi publish.
    private String style;

    @NotNull(message = "Vui lòng nhập giá sản phẩm.")
    @DecimalMin(value = "0.01", message = "Giá sản phẩm phải lớn hơn 0.")
    @DecimalMax(value = "999999999999.00", message = "Giá sản phẩm không được vượt quá 999.999.999.999 VNĐ.")
    @Digits(integer = 12, fraction = 0, message = "Giá sản phẩm chỉ được nhập số nguyên VNĐ, tối đa 12 chữ số.")
    // Giá bán sản phẩm.
    private BigDecimal price;

    // Trạng thái sản phẩm khi submit form.
    private String productStatus;

    // Danh sách tag được chọn cho sản phẩm.
    private List<Integer> tagIds;
}
