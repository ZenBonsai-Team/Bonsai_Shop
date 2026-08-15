package com.example.bonsai_shop.artisan.service;

import com.example.bonsai_shop.data.common.CloudinaryFolder;
import com.example.bonsai_shop.data.dto.CloudinaryUploadResponse;
import com.example.bonsai_shop.data.service.CloudinaryStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
// Service bọc Cloudinary để lưu và xóa media của artisan.
public class ArtisanMediaStorageService {

    private final CloudinaryStorageService cloudinaryStorageService;

    // Upload file media sản phẩm lên Cloudinary.
    public String storeProductMedia(MultipartFile file) {
        String contentType = file.getContentType();
        boolean isVideo = contentType != null && contentType.startsWith("video/");

        // Chon folder va API upload theo Content-Type de Cloudinary tao dung resource image/video.
        CloudinaryUploadResponse response = isVideo
                ? cloudinaryStorageService.uploadVideo(file, CloudinaryFolder.PRODUCT_VIDEO)
                : cloudinaryStorageService.uploadImage(file, CloudinaryFolder.PRODUCT_IMAGE);

        return response.getUrl();
    }

    // Xóa media trên Cloudinary nếu URL hợp lệ.
    public void deleteProductMedia(String mediaUrl) {
        CloudinaryFileReference fileReference = parseCloudinaryUrl(mediaUrl);
        if (fileReference == null) {
            return;
        }

        cloudinaryStorageService.deleteFile(fileReference.publicId(), fileReference.resourceType());
    }

    // Tách publicId và resourceType từ URL Cloudinary đã lưu.
    private CloudinaryFileReference parseCloudinaryUrl(String mediaUrl) {
        if (mediaUrl == null || mediaUrl.isBlank() || !mediaUrl.contains("/upload/")) {
            return null;
        }

        // Can resourceType khi xoa vi Cloudinary tach namespace image va video.
        String resourceType = mediaUrl.contains("/video/upload/") ? "video" : "image";
        String uploadMarker = "/upload/";
        int uploadIndex = mediaUrl.indexOf(uploadMarker);
        String pathAfterUpload = mediaUrl.substring(uploadIndex + uploadMarker.length());
        String pathWithoutVersion = pathAfterUpload.replaceFirst("^v\\d+/", "");
        int extensionIndex = pathWithoutVersion.lastIndexOf('.');
        String publicId = extensionIndex > 0
                ? pathWithoutVersion.substring(0, extensionIndex)
                : pathWithoutVersion;

        return new CloudinaryFileReference(publicId, resourceType);
    }

    // Dữ liệu tham chiếu tối thiểu để thao tác với Cloudinary.
    private record CloudinaryFileReference(String publicId, String resourceType) {
    }
}
