package com.example.bonsai_shop.seller.service;

import com.example.bonsai_shop.data.common.CloudinaryFolder;
import com.example.bonsai_shop.data.dto.CloudinaryUploadResponse;
import com.example.bonsai_shop.data.service.CloudinaryStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class SellerMediaStorageService {

    private final CloudinaryStorageService cloudinaryStorageService;

    public String storeProductMedia(MultipartFile file) {
        String contentType = file.getContentType();
        boolean isVideo = contentType != null && contentType.startsWith("video/");

        CloudinaryUploadResponse response = isVideo
                ? cloudinaryStorageService.uploadVideo(file, CloudinaryFolder.PRODUCT_VIDEO)
                : cloudinaryStorageService.uploadImage(file, CloudinaryFolder.PRODUCT_IMAGE);

        return response.getUrl();
    }

    public void deleteProductMedia(String mediaUrl) {
        CloudinaryFileReference fileReference = parseCloudinaryUrl(mediaUrl);
        if (fileReference == null) {
            return;
        }

        cloudinaryStorageService.deleteFile(fileReference.publicId(), fileReference.resourceType());
    }

    private CloudinaryFileReference parseCloudinaryUrl(String mediaUrl) {
        if (mediaUrl == null || mediaUrl.isBlank() || !mediaUrl.contains("/upload/")) {
            return null;
        }

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

    private record CloudinaryFileReference(String publicId, String resourceType) {
    }
}
