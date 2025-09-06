package com.manil.manil.gemini.dto.generate;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Contents {
    private Part[] parts;

    public static Contents ofText(String s) {
        return new Contents(new Part[]{ new Part(s) });
    }
}