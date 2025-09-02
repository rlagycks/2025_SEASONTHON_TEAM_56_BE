package com.manil.manil.describe;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.manil.manil.describe.DescribeDto.DescribeRequest;
import com.manil.manil.describe.DescribeDto.DescribeResponse;
import com.manil.manil.product.Product;
import com.manil.manil.product.ProductRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.Map;

@Service
public class ProductDescribeService {

    private final ProductRepository repo;
    private final WebClient webClient;
    private final ObjectMapper om = new ObjectMapper();
    private final String model;

    public ProductDescribeService(
            ProductRepository repo,
            @Value("${gemini.base-url}") String baseUrl,
            @Value("${gemini.api-key}") String apiKey,
            @Value("${gemini.model}") String model
    ) {
        this.repo = repo;
        this.model = model;
        this.webClient = WebClient.builder()
                .baseUrl(baseUrl)
                .defaultHeaders(h -> h.setContentType(MediaType.APPLICATION_JSON))
                .defaultUriVariables(Map.of("apiKey", apiKey))
                .build();
    }

    public DescribeResponse describe(Long productId, DescribeRequest req) {
        Product p = repo.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("product not found: " + productId));

        String prompt = PromptTemplates.describeTemplate(
                p,
                (req.getHint() == null || req.getHint().isBlank())
                        ? "구매 전 알아야 할 장단점과 지역 특산성 맥락을 자세히"
                        : req.getHint(),
                (req.getLocale() == null) ? "ko" : req.getLocale()
        );

        // ==== curl 바디와 동일한 구조로 JSON 생성 ====
        ObjectNode part = om.createObjectNode().put("text", prompt);
        ObjectNode content = om.createObjectNode().set("parts", om.createArrayNode().add(part));
        ObjectNode body = om.createObjectNode().set("contents", om.createArrayNode().add(content));

        // 응답 받기
        String json = webClient.post()
                .uri(uriBuilder -> uriBuilder
                        .path("/models/{model}:generateContent")
                        .queryParam("key", "{apiKey}")
                        .build(this.model))
                .bodyValue(body)
                .retrieve()
                .bodyToMono(String.class)
                .timeout(Duration.ofSeconds(15))
                .block();

        if (json == null || json.isBlank()) {
            throw new IllegalStateException("Empty response from Gemini");
        }

        // candidates[0].content.parts[0].text 추출
        try {
            var root = om.readTree(json);
            var firstText = root.path("candidates").path(0)
                    .path("content").path("parts").path(0).path("text").asText("");
            if (firstText.isBlank()) throw new IllegalStateException("No text in response: " + json);

            String cleaned = stripFence(firstText);
            return om.readValue(cleaned, DescribeResponse.class);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to parse response: " + json, e);
        }
    }

    private static String stripFence(String s) {
        return s.replace("```json", "").replace("```", "").trim();
    }
}
