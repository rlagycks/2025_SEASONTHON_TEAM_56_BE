package com.manil.manil.dto;

public class ProductDescriptionResponse {

    /** 최종 생성된 상세 설명 텍스트 */
    private String description;

    /** 사용한 모델, 토큰 등 메타(원하면 노출) */
    private String model;
    private Integer maxOutputTokens;

    public ProductDescriptionResponse() {}

    public ProductDescriptionResponse(String description, String model, Integer maxOutputTokens) {
        this.description = description;
        this.model = model;
        this.maxOutputTokens = maxOutputTokens;
    }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }

    public Integer getMaxOutputTokens() { return maxOutputTokens; }
    public void setMaxOutputTokens(Integer maxOutputTokens) { this.maxOutputTokens = maxOutputTokens; }
}
