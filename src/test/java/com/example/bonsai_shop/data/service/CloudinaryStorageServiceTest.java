package com.example.bonsai_shop.data.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.Uploader;
import com.example.bonsai_shop.data.common.CloudinaryFolder;
import com.example.bonsai_shop.data.dto.CloudinaryUploadResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class CloudinaryStorageServiceTest {

    private Cloudinary cloudinary;
    private Uploader uploader;
    private CloudinaryStorageService cloudinaryStorageService;

    @BeforeEach
    void setUp() {
        // Khoi tao Cloudinary gia lap de co config truong public ma khong can mock phuc tap.
        cloudinary = spy(new Cloudinary(Map.of(
                "cloud_name", "test-cloud",
                "api_key", "test-key",
                "api_secret", "test-secret"
        )));
        uploader = mock(Uploader.class);
        doReturn(uploader).when(cloudinary).uploader();

        cloudinaryStorageService = new CloudinaryStorageService(cloudinary);
    }

    @Test
    void generateUploadSignature_ShouldReturnSignatureData() {
        Map<String, Object> signatureData = cloudinaryStorageService.generateUploadSignature(CloudinaryFolder.PRODUCT_IMAGE);

        assertNotNull(signatureData);
        assertEquals("test-key", signatureData.get("apiKey"));
        assertEquals("test-cloud", signatureData.get("cloudName"));
        assertEquals(CloudinaryFolder.PRODUCT_IMAGE.getPath(), signatureData.get("folder"));
        assertTrue(signatureData.containsKey("signature"));
        assertTrue(signatureData.containsKey("timestamp"));
    }

    @Test
    void uploadImage_WhenValidFile_ShouldUploadAndReturnResponse() throws IOException {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "bonsai.jpg",
                "image/jpeg",
                "fake image content".getBytes()
        );

        Map<String, Object> uploadResult = Map.of(
                "secure_url", "https://res.cloudinary.com/test-cloud/image/upload/v12345/test.jpg",
                "public_id", "test-public-id"
        );

        when(uploader.upload(any(byte[].class), any(Map.class))).thenReturn(uploadResult);

        CloudinaryUploadResponse response = cloudinaryStorageService.uploadImage(file, CloudinaryFolder.PRODUCT_IMAGE);

        assertNotNull(response);
        assertEquals("https://res.cloudinary.com/test-cloud/image/upload/q_auto,f_auto/v12345/test.jpg", response.getUrl());
        assertEquals("test-public-id", response.getPublicId());
        assertEquals("image", response.getResourceType());
    }

    @Test
    void uploadImage_WhenEmptyFile_ShouldThrowException() {
        MockMultipartFile emptyFile = new MockMultipartFile(
                "file",
                "empty.jpg",
                "image/jpeg",
                new byte[0]
        );

        Exception exception = assertThrows(RuntimeException.class, () ->
                cloudinaryStorageService.uploadImage(emptyFile, CloudinaryFolder.PRODUCT_IMAGE)
        );

        assertTrue(exception.getMessage().contains("File không được để trống"));
    }

    @Test
    void uploadImage_WhenInvalidMimeType_ShouldThrowException() {
        MockMultipartFile txtFile = new MockMultipartFile(
                "file",
                "doc.txt",
                "text/plain",
                "some text".getBytes()
        );

        Exception exception = assertThrows(RuntimeException.class, () ->
                cloudinaryStorageService.uploadImage(txtFile, CloudinaryFolder.PRODUCT_IMAGE)
        );

        assertTrue(exception.getMessage().contains("File không đúng định dạng"));
    }

    @Test
    void uploadImage_WhenFileTooLarge_ShouldThrowException() {
        byte[] largeBytes = new byte[8 * 1024 * 1024]; // 8MB (image limit is 7MB)
        MockMultipartFile largeFile = new MockMultipartFile(
                "file",
                "huge.jpg",
                "image/jpeg",
                largeBytes
        );

        Exception exception = assertThrows(RuntimeException.class, () ->
                cloudinaryStorageService.uploadImage(largeFile, CloudinaryFolder.PRODUCT_IMAGE)
        );

        assertTrue(exception.getMessage().contains("Ảnh không được vượt quá 7MB"));
    }

    @Test
    void uploadVideo_WhenValidFile_ShouldUploadAndReturnResponse() throws IOException {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "clip.mp4",
                "video/mp4",
                "fake video content".getBytes()
        );

        Map<String, Object> uploadResult = Map.of(
                "secure_url", "https://res.cloudinary.com/test-cloud/video/upload/v12345/clip.mp4",
                "public_id", "video-public-id"
        );

        when(uploader.upload(any(byte[].class), any(Map.class))).thenReturn(uploadResult);

        CloudinaryUploadResponse response = cloudinaryStorageService.uploadVideo(file, CloudinaryFolder.PRODUCT_VIDEO);

        assertNotNull(response);
        assertEquals("https://res.cloudinary.com/test-cloud/video/upload/v12345/clip.mp4", response.getUrl());
        assertEquals("video-public-id", response.getPublicId());
        assertEquals("video", response.getResourceType());
    }

    @Test
    void uploadVideo_WhenFileTooLarge_ShouldThrowException() {
        byte[] largeBytes = new byte[101 * 1024 * 1024]; // 101MB (video limit is 100MB)
        MockMultipartFile largeFile = new MockMultipartFile(
                "file",
                "huge.mp4",
                "video/mp4",
                largeBytes
        );

        Exception exception = assertThrows(RuntimeException.class, () ->
                cloudinaryStorageService.uploadVideo(largeFile, CloudinaryFolder.PRODUCT_VIDEO)
        );

        assertTrue(exception.getMessage().contains("Video không được vượt quá 100MB"));
    }

    @Test
    void deleteFile_WhenValidPublicId_ShouldCallDestroy() throws IOException {
        cloudinaryStorageService.deleteFile("test-public-id", "image");

        verify(uploader, times(1)).destroy(eq("test-public-id"), eq(Map.of("resource_type", "image")));
    }

    @Test
    void deleteFile_WhenNullOrBlankPublicId_ShouldNotCallDestroy() throws IOException {
        cloudinaryStorageService.deleteFile(null, "image");
        cloudinaryStorageService.deleteFile("", "image");

        verify(uploader, never()).destroy(anyString(), anyMap());
    }

    @Test
    void deleteFile_WhenDestroyThrowsIOException_ShouldThrowRuntimeException() throws IOException {
        when(uploader.destroy(anyString(), anyMap())).thenThrow(new IOException("Connection failed"));

        assertThrows(RuntimeException.class, () ->
                cloudinaryStorageService.deleteFile("test-public-id", "image")
        );
    }
}
