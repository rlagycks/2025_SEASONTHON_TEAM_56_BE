// src/main/java/com/manil/manil/gemini/service/GeminiEmbeddingService.java
package com.manil.manil.gemini.service;

import com.manil.manil.gemini.client.EmbeddingClient;
import com.manil.manil.gemini.config.GeminiProperties;
import com.manil.manil.gemini.dto.response.EmbedContentResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class GeminiEmbeddingService implements EmbeddingClient {

    private final RestClient geminiRestClient;
    private final GeminiProperties props;

    @Override
    public float[] embed(String text) {
        final int dim = props.getEmbedDimension();

        // 공백 입력 → DB 제약(vector(768))을 만족시키기 위해 0-벡터 반환
        if (text == null || text.isBlank()) {
            return new float[dim];
        }

        // ✅ 올바른 바디 (content + outputDimensionality)
        Map<String, Object> body = Map.of(
                "content", Map.of(
                        "parts", List.of(Map.of("text", text))
                ),
                "outputDimensionality", dim
                // 필요하면: "taskType", "title" 등 추가 가능
        );

        final EmbedContentResponse res = geminiRestClient.post()
                .uri(u -> u.path("/v1beta/models/{model}:embedContent")
                        .queryParam("key", props.getApiKey())
                        .build(props.getEmbedModel())) // ex) gemini-embedding-001
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(EmbedContentResponse.class);

        final List<Double> values = Optional.ofNullable(res)
                .map(EmbedContentResponse::getEmbedding)
                .map(e -> e.getValues())
                .orElseThrow(() -> new IllegalStateException("Empty embedding response"));

        if (values.size() != dim) {
            throw new IllegalArgumentException("Embedding dimension must be " + dim + ", got " + values.size());
        }

        float[] out = new float[dim];
        for (int i = 0; i < dim; i++) out[i] = values.get(i).floatValue();
        return out;
    }

    @Override public int dimension() { return props.getEmbedDimension(); }
    @Override public String modelName() { return props.getEmbedModel(); }
}