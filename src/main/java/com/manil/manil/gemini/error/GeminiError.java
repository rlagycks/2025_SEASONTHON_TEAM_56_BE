package com.manil.manil.gemini.error;

import com.manil.manil.global.exception.error.BaseError;
import org.springframework.http.HttpStatus;

public enum GeminiError implements BaseError {
    INVALID_INPUT(HttpStatus.BAD_REQUEST, "요청 값이 유효하지 않습니다."),
    LLM_API_ERROR(HttpStatus.BAD_GATEWAY, "Gemini API 호출에 실패했습니다.");

    private final HttpStatus status;
    private final String message;

    GeminiError(HttpStatus status, String message) {
        this.status = status;
        this.message = message;
    }

    @Override public HttpStatus getHttpStatus() { return status; }
    @Override public String getMessage() { return message; }
}
