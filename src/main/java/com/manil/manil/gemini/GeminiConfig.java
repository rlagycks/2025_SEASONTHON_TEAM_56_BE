package com.manil.manil.gemini;

import com.google.genai.Client;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GeminiConfig {

    @Bean
    public Client genaiClient(@Value("${GEMINI_API_KEY}") String apiKey) {
        return new Client.Builder()
                .apiKey(apiKey)
                .build();
    }
}
