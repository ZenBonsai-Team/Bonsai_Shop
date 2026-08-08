package com.example.bonsai_shop.data.service;

import com.cloudinary.Cloudinary;
import com.example.bonsai_shop.data.common.CloudinaryFolder;
import com.example.bonsai_shop.data.dto.CloudinaryUploadResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CloudinaryStorageService {

    private final Cloudinary cloudinary;

    public CloudinaryUploadResponse uploadImage(MultipartFile file, CloudinaryFolder folder) {
        validateFile(file, "image");
        return upload(file, folder.getPath(), "image");
    }

    public CloudinaryUploadResponse uploadVideo(MultipartFile file, CloudinaryFolder folder) {
        validateFile(file, "video");
        return upload(file, folder.getPath(), "video");
    }

    public void deleteFile(String publicId, String resourceType) {
        if (publicId == null || publicId.isBlank()) {
            return;
        }

        try {
            cloudinary.uploader().destroy(
                    publicId,
                    Map.of("resource_type", resourceType)
            );
        } catch (IOException e) {
            throw new RuntimeException("Xóa file trên Cloudinary thất bại!");
        }
    }

    private CloudinaryUploadResponse upload(MultipartFile file, String folder, String resourceType) {
        try {
            Map uploadResult = cloudinary.uploader().upload(
                    file.getBytes(),
                    Map.of(
                            "folder", folder,
                            "resource_type", resourceType
                    )
            );

            String url = uploadResult.get("secure_url").toString();
            String publicId = uploadResult.get("public_id").toString();

            // Tự động nén chất lượng (q_auto) và chuyển định dạng tối ưu (f_auto) khi hiển thị
            if ("image".equals(resourceType) && url.contains("/upload/")) {
                url = url.replace("/upload/", "/upload/q_auto,f_auto/");
            }

            return new CloudinaryUploadResponse(url, publicId, resourceType);
        } catch (IOException e) {
            throw new RuntimeException("Upload file lên Cloudinary thất bại!");
        }
    }

    private void validateFile(MultipartFile file, String expectedType) {
        if (file == null || file.isEmpty()) {
            throw new RuntimeException("File không được để trống!");
        }

        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith(expectedType + "/")) {
            throw new RuntimeException("File không đúng định dạng " + expectedType + "!");
        }

        long maxSize = expectedType.equals("video")
                ? 100L * 1024 * 1024
                : 7L * 1024 * 1024;

        if (file.getSize() > maxSize) {
            throw new RuntimeException(
                    expectedType.equals("video")
                            ? "Video không được vượt quá 100MB!"
                            : "Ảnh không được vượt quá 7MB!"
            );
        }
    }
}
