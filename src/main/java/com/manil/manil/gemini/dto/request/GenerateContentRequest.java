package com.manil.manil.gemini.dto.request;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.manil.manil.gemini.config.GenerationConfig;
import com.manil.manil.gemini.dto.generate.Contents;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@JsonInclude(Include.NON_NULL)
public class GenerateContentRequest {
    private Contents[] contents;
    private GenerationConfig generationConfig;
}