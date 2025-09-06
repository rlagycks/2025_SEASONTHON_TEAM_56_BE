// src/main/java/com/manil/manil/image/dto/ImageUploadResponse.java
package com.manil.manil.image.dto;

import lombok.*;

@Getter @Setter @AllArgsConstructor @NoArgsConstructor @Builder
public class ImageUploadResponse {
    private String url;
    private String filename;
    private long size;
    private String contentType;
    private Integer width;
    private Integer height;
}