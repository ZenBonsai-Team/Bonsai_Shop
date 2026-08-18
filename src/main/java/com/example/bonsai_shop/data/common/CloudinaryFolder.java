package com.example.bonsai_shop.data.common;

public enum CloudinaryFolder {

    AVATAR("bonsai_shop/avatars"),
    ARTISAN_COVER("bonsai_shop/artisans/covers"),
    // Tach rieng image va video de quan ly resource tren Cloudinary de hon.
    PRODUCT_IMAGE("bonsai_shop/products/images"),
    PRODUCT_VIDEO("bonsai_shop/products/videos"),
    BANNER("bonsai_shop/banners"),
    COMMUNITY("bonsai_shop/community");

    private final String path;

    CloudinaryFolder(String path) {
        this.path = path;
    }

    public String getPath() {
        return this.path;
    }
}
