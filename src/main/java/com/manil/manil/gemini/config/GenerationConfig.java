package com.manil.manil.gemini.config;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record GenerationConfig(
        @JsonProperty("response_mime_type") String responseMimeType,
        @JsonProperty("thinkingConfig") ThinkingConfig thinkingConfig
) {
    public static GenerationConfig jsonOnly() {
        return new GenerationConfig("application/json", null);
    }
    public static GenerationConfig jsonWithThinkingOff() {
        return new GenerationConfig("application/json", new ThinkingConfig(0));
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static record ThinkingConfig(
            @JsonProperty("thinkingBudget") Integer thinkingBudget
    ) {}
}