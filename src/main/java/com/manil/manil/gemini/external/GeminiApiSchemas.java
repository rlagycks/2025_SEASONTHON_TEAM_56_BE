package com.manil.manil.gemini.external;

import lombok.Getter;
import lombok.Setter;
import java.util.List;

@Getter @Setter
public class GeminiApiSchemas {

    @Getter @Setter
    public static class TextPart { private String text; }

    @Getter @Setter
    public static class Content {
        private String role; // 요청 시 생략 가능
        private List<TextPart> parts;
    }

    @Getter @Setter
    public static class GenerationConfig {
        private Double temperature;
        private Integer maxOutputTokens;
    }

    @Getter @Setter
    public static class GenerateContentRequest {
        private List<Content> contents;
        private GenerationConfig generationConfig;
    }

    @Getter @Setter
    public static class Candidate {
        private Content content;     // parts[].text
        private String finishReason;
    }

    @Getter @Setter
    public static class GenerateContentResponse {
        private List<Candidate> candidates;
    }
}
