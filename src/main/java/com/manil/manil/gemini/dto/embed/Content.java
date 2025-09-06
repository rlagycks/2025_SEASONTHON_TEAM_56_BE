// src/main/java/com/manil/manil/gemini/dto/embed/Content.java
package com.manil.manil.gemini.dto.embed;

import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Content {
    private Part[] parts;

    public static Content ofText(String text) {
        return new Content(new Part[]{ new Part(text) });
    }
}