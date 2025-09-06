package com.manil.manil.gemini.service;

import com.manil.manil.gemini.client.LlmAnalyzeClient;
import com.manil.manil.gemini.config.GeminiProperties;
import com.manil.manil.gemini.config.GenerationConfig;
import com.manil.manil.gemini.dto.generate.Contents;
import com.manil.manil.gemini.dto.request.GenerateContentRequest;
import com.manil.manil.gemini.dto.response.GenerateContentResponse;
import com.manil.manil.gemini.util.JsonSafe;
import com.manil.manil.gemini.util.PromptBuilder;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class GeminiAnalyzeService implements LlmAnalyzeClient {

    private final RestClient geminiRestClient;
    private final GeminiProperties props;

    @Override
    public AnalyzeOutput extractAll(AnalyzeInput in) {
        // 1) 프롬프트 합치기 (role 사용 안 함 → 오류 회피)
        String system = PromptBuilder.systemPrompt();
        String user   = PromptBuilder.userPrompt(in);
        String merged = system + "\n" + user;

        // 2) 요청 조립 (JSON만, thinking 끄기)
        var req = GenerateContentRequest.builder()
                .contents(new Contents[]{ Contents.ofText(merged) })
                .generationConfig(GenerationConfig.jsonWithThinkingOff())
                .build();

        // 3) 호출
        var res = geminiRestClient.post()
                .uri(uri -> uri.path("/v1beta/models/{model}:generateContent")
                        .build(props.getGenerateModel()))
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .body(req)
                .retrieve()
                .body(GenerateContentResponse.class);

        // 4) 텍스트 추출
        String text = JsonSafe.firstText(res)
                .orElseThrow(() -> new RuntimeException("Empty LLM response"));

        // 5) JSON 파싱
        Map<String,Object> map = JsonSafe.readMap(text);
        String detailed       = JsonSafe.getString(map, "detailed_description");
        var ingredients       = JsonSafe.getStringList(map, "ingredients");
        var regions           = JsonSafe.getStringList(map, "regions");
        var ageGroups         = JsonSafe.getStringList(map, "age_groups");
        var abstractTags      = JsonSafe.getStringList(map, "abstract_tags");

        // 임베딩은 ProductService에서 처리/비교
        return new AnalyzeOutput(detailed, null, ingredients, regions, ageGroups, abstractTags);
    }
}