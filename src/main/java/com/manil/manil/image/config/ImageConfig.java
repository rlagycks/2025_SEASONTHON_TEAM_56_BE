package com.manil.manil.image.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(ImageStorageProperties.class)
public class ImageConfig { }