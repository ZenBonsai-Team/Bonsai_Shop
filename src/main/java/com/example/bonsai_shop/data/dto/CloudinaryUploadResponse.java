package com.example.bonsai_shop.data.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;


@Getter
@AllArgsConstructor

public class CloudinaryUploadResponse {

    private String url;

    private String publicId;

    private String resourceType;


}
