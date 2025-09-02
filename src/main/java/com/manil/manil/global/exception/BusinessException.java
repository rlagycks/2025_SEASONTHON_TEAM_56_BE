package com.manil.manil.global.exception;

import com.manil.manil.global.exception.error.BaseError;
import org.springframework.http.HttpStatus;

public class BusinessException extends RuntimeException {
    private final BaseError baseError;

    public BusinessException(BaseError baseError) {
        super(baseError.getMessage());
        this.baseError = baseError;
    }

    public BusinessException(BaseError baseError, Throwable cause) {
        super(baseError.getMessage(), cause);
        this.baseError = baseError;
    }

    public HttpStatus getHttpStatus() {
        return baseError.getHttpStatus();
    }

    @Override
    public String getMessage() {
        return baseError.getMessage();
    }
}
