// src/main/java/com/manil/manil/image/config/ImageStorageProperties.java
package com.manil.manil.image.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter @Setter
@ConfigurationProperties(prefix = "image")
public class ImageStorageProperties {
    private String rootDir;           // /var/www/manil/uploads
    private String publicBaseUrl;     // http://43.201.10.255/images/
    private long   maxSizeBytes;      // 10MB
    private String allowedExtensions; // "jpg,jpeg,png,webp,gif"
}