package com.manil.manil.service;

import com.manil.manil.config.GeminiProperties;
import com.manil.manil.dto.ProductAnalyzeRequest;
import com.manil.manil.dto.ProductAnalyzeResponse;
import com.manil.manil.gemini.external.GeminiApiSchemas.*;
import com.manil.manil.global.exception.BusinessException;

// ▼▼ 전역 에러 enum이 있다면 이 줄을 네 프로젝트의 실제 경로로 교체하세요.
// import com.manil.manil.global.error.ProductError;
// ▲▲ 전역 enum이 없다면, 아래 임시용 GeminiError를 사용:
import com.manil.manil.gemini.error.GeminiError;

import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GeminiProductService {

    private final WebClient geminiWebClient;   // bean: geminiWebClient
    private final GeminiProperties props;

    public ProductAnalyzeResponse generateDetailedDescription(ProductAnalyzeRequest req) {
        try {
            String prompt = buildPrompt(req);

            GenerateContentRequest body = new GenerateContentRequest();
            Content user = new Content();
            TextPart part = new TextPart();
            part.setText(prompt);
            user.setParts(List.of(part));
            body.setContents(List.of(user));

            GenerationConfig cfg = new GenerationConfig();
            cfg.setTemperature(0.7);
            cfg.setMaxOutputTokens(512);
            body.setGenerationConfig(cfg);

            String path = String.format("/v1beta/models/%s:generateContent", props.getModel());

            GenerateContentResponse res = geminiWebClient.post()
                    .uri(uriBuilder -> uriBuilder
                            .path(path)
                            .queryParam("key", props.getApiKey())
                            .build())
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .bodyValue(body)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, resp ->
                            resp.bodyToMono(String.class)
                                    .defaultIfEmpty("")
                                    .flatMap(b -> Mono.error(new BusinessException(
                                            // 전역 enum이 있다면 여기 ProductError.LLM_API_ERROR 로 변경
                                            GeminiError.LLM_API_ERROR,
                                            new RuntimeException("Gemini API " + resp.statusCode().value() + " " + b)
                                    )))
                    )
                    .bodyToMono(GenerateContentResponse.class)
                    .timeout(java.time.Duration.ofMillis(props.getTimeoutMs()))
                    .block();

            String output = extractFirstText(res);
            if (!StringUtils.hasText(output)) {
                // 전역 enum이 있다면 ProductError.LLM_API_ERROR 로
                throw new BusinessException(GeminiError.LLM_API_ERROR);
            }

            return new ProductAnalyzeResponse(output.trim(), java.util.UUID.randomUUID().toString());
        } catch (BusinessException be) {
            throw be;
        } catch (Exception e) {
            // 전역 enum이 있다면 ProductError.LLM_API_ERROR 로
            throw new BusinessException(GeminiError.LLM_API_ERROR, e);
        }
    }

    private String extractFirstText(GenerateContentResponse res) {
        if (res == null || res.getCandidates() == null) return null;
        return res.getCandidates().stream()
                .filter(Objects::nonNull)
                .map(Candidate::getContent)
                .filter(Objects::nonNull)
                .map(Content::getParts)
                .filter(Objects::nonNull)
                .flatMap(List::stream)
                .filter(Objects::nonNull)
                .map(TextPart::getText)
                .filter(StringUtils::hasText)
                .findFirst()
                .orElse(null);
    }

    // 이미지(images)는 상세설명 생성에 사용하지 않음(요구사항)
    private String buildPrompt(ProductAnalyzeRequest req) {
        List<String> lines = new ArrayList<>();
        lines.add("역할: 전자상거래 카피라이터");
        lines.add("목표: 아래 제공된 정보만을 활용해 한국어 상품 상세설명을 작성한다.");
        lines.add("제약:");
        lines.add("- 3~6문장, 정보 중심, 과장/확정적 표현 지양");
        lines.add("- 사실 불명은 '~일 수 있습니다'처럼 책임 있는 어투");
        lines.add("- 이모지/특수문자/해시태그 금지");

        List<String> facts = new ArrayList<>();
        if (StringUtils.hasText(req.getName()))              facts.add("상품명: " + req.getName());
        if (StringUtils.hasText(req.getSimpleDescription())) facts.add("간단한 설명: " + req.getSimpleDescription());
        if (req.getKeywords() != null && !req.getKeywords().isEmpty()) {
            String kws = req.getKeywords().stream()
                    .filter(StringUtils::hasText)
                    .limit(5)
                    .collect(Collectors.joining(", "));
            if (StringUtils.hasText(kws)) facts.add("키워드: " + kws);
        }
        if (StringUtils.hasText(req.getCategory()))          facts.add("카테고리: " + req.getCategory());
        if (req.getPrice() != null)                          facts.add("가격: " + req.getPrice());

        lines.add("");
        lines.add("입력 정보(사실):");
        if (facts.isEmpty()) {
            lines.add("- (제공된 사실 없음)");
        } else {
            facts.forEach(f -> lines.add("- " + f));
        }

        lines.add("");
        lines.add("출력 형식: 상세설명 본문만 한 단락으로 작성.");

        return String.join("\n", lines);
    }
}
