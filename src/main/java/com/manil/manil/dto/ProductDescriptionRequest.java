package com.manil.manil.dto;

import jakarta.validation.constraints.NotBlank;

public class ProductDescriptionRequest {

    /** 한 줄 요약(예: "선물용 500ml 텀블러, 보온/보냉") */
    @NotBlank
    private String oneLine;

    /** 선택: "ko" | "en" | "ja" | "zh" 등, 기본 ko */
    private String locale = "ko";

    /** 선택: 톤(예: "중립", "친절", "전문가") */
    private String tone;

    /** 선택: 추가 키워드(쉼표 구분: "선물용, 스테인리스, 500ml") */
    private String keywords;

    public String getOneLine() { return oneLine; }
    public void setOneLine(String oneLine) { this.oneLine = oneLine; }

    public String getLocale() { return locale; }
    public void setLocale(String locale) { this.locale = locale; }

    public String getTone() { return tone; }
    public void setTone(String tone) { this.tone = tone; }

    public String getKeywords() { return keywords; }
    public void setKeywords(String keywords) { this.keywords = keywords; }
}
