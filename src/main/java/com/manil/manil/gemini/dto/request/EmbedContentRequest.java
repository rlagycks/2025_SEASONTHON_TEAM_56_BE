// src/main/java/com/manil/manil/gemini/dto/request/EmbedContentRequest.java
package com.manil.manil.gemini.dto.request;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.manil.manil.gemini.dto.embed.Content; // ✅ embed.Content 임포트
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@JsonInclude(Include.NON_NULL)
public class EmbedContentRequest {
    private Content content;

    public static EmbedContentRequest ofText(String text) {
        return EmbedContentRequest.builder()
                .content(Content.ofText(text))
                .build();
    }
}