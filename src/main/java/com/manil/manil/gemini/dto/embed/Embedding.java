package com.manil.manil.gemini.dto.embed;

import lombok.*;

import java.util.List;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Embedding {
    private List<Double> values;
}