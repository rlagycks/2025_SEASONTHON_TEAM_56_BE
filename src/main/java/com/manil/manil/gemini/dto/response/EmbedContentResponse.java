package com.manil.manil.gemini.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.manil.manil.gemini.dto.embed.Embedding;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class EmbedContentResponse {
    private Embedding embedding;
}