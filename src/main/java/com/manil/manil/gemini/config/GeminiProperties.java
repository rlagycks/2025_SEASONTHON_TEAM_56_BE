// src/main/java/com/manil/manil/gemini/config/GeminiProperties.java
package com.manil.manil.gemini.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "manil.gemini")
public class GeminiProperties {
    private String apiKey;
    private String baseUrl = "https://generativelanguage.googleapis.com";

    // ✅ 임베딩 모델/차원
    private String embedModel = "gemini-embedding-001";
    private int embedDimension = 768;

    // ✅ 텍스트 생성 모델
    private String generateModel = "gemini-2.5-flash-lite";

    private int connectTimeoutMs = 3000;
    private int readTimeoutMs = 8000;

    // getters/setters
    public String getApiKey() { return apiKey; }
    public void setApiKey(String apiKey) { this.apiKey = apiKey; }
    public String getBaseUrl() { return baseUrl; }
    public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }

    public String getEmbedModel() { return embedModel; }
    public void setEmbedModel(String embedModel) { this.embedModel = embedModel; }

    public int getEmbedDimension() { return embedDimension; }
    public void setEmbedDimension(int embedDimension) { this.embedDimension = embedDimension; }

    public String getGenerateModel() { return generateModel; }
    public void setGenerateModel(String generateModel) { this.generateModel = generateModel; }

    public int getConnectTimeoutMs() { return connectTimeoutMs; }
    public void setConnectTimeoutMs(int connectTimeoutMs) { this.connectTimeoutMs = connectTimeoutMs; }
    public int getReadTimeoutMs() { return readTimeoutMs; }
    public void setReadTimeoutMs(int readTimeoutMs) { this.readTimeoutMs = readTimeoutMs; }
}