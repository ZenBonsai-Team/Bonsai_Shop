package com.example.bonsai_shop.data.dto;

public class CloudinaryUploadResponse {

    private String url;
    private String publicId;
    private String resourceType;

    public CloudinaryUploadResponse() {
    }

    public CloudinaryUploadResponse(String url, String publicId, String resourceType) {
        this.url = url;
        this.publicId = publicId;
        this.resourceType = resourceType;
    }

    public String getUrl() {
        return this.url;
    }

    public String getPublicId() {
        return this.publicId;
    }

    public String getResourceType() {
        return this.resourceType;
    }
}
