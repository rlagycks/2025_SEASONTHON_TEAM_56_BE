package com.manil.manil.config;

import com.google.genai.Client;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GeminiConfig {

    @Value("${gemini.api-key:}")
    private String apiKey;

    @Bean
    public Client genAiClient() {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("GEMINI_API_KEY가 설정되지 않았습니다. application-dev.yml과 환경변수를 확인하세요.");
        }
        return new Client.Builder()
                .apiKey(apiKey)
                .build();
    }
}
