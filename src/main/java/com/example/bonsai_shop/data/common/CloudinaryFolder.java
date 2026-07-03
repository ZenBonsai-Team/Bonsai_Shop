package com.example.bonsai_shop.data.common;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum CloudinaryFolder {

    AVATAR("bonsai_shop/avatars"),
    PRODUCT_IMAGE("bonsai_shop/products/images"),
    PRODUCT_VIDEO("bonsai_shop/products/videos"),
    BANNER("bonsai_shop/banners");

    private final String path;
}
