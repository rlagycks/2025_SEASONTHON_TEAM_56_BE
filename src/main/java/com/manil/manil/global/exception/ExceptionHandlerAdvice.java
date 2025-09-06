package com.manil.manil.global.exception;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class ExceptionHandlerAdvice {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> businessExceptionHandler(
            BusinessException exception,
            HttpServletRequest request
    ) {
        // Text Blocks 사용 (Java 15+)
        var logMessage = """
            Business Error Details:
            - URI: {}
            - Error: {}
            - Status: {}
            """;

        // Enhanced switch expression (Java 14+) - 올바른 사용법
        switch (exception.getHttpStatus().series()) {
            case CLIENT_ERROR -> log.warn(logMessage,
                    request.getRequestURI(),
                    exception.getMessage(),
                    exception.getHttpStatus());
            case SERVER_ERROR -> log.error(logMessage,
                    request.getRequestURI(),
                    exception.getMessage(),
                    exception.getHttpStatus(),
                    exception);
            default -> log.info(logMessage,
                    request.getRequestURI(),
                    exception.getMessage(),
                    exception.getHttpStatus());
        }

        var errorResponse = ErrorResponse.of(
                exception.getHttpStatus(),
                exception.getMessage(),
                request.getRequestURI()
        );

        return ResponseEntity.status(exception.getHttpStatus()).body(errorResponse);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> validationExceptionHandler(
            MethodArgumentNotValidException exception,
            HttpServletRequest request
    ) {
        log.warn("Validation Error at {}: ", request.getRequestURI(), exception);

        var errorMessage = exception.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(FieldError::getDefaultMessage)
                .filter(message -> message != null && !message.isBlank())
                .collect(Collectors.joining(", "));

        var errorResponse = ErrorResponse.of(
                HttpStatus.BAD_REQUEST,
                errorMessage.isBlank() ? "잘못된 요청입니다." : errorMessage,
                request.getRequestURI()
        );

        return ResponseEntity.badRequest().body(errorResponse);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> illegalArgumentExceptionHandler(
            IllegalArgumentException exception,
            HttpServletRequest request
    ) {
        log.warn("Illegal Argument at {}: {}", request.getRequestURI(), exception.getMessage());

        var errorResponse = ErrorResponse.of(
                HttpStatus.BAD_REQUEST,
                exception.getMessage(),
                request.getRequestURI()
        );

        return ResponseEntity.badRequest().body(errorResponse);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> generalExceptionHandler(
            Exception exception,
            HttpServletRequest request
    ) {
        // Java 17에서 사용 가능한 instanceof pattern matching
        String errorMessage;
        if (exception instanceof NullPointerException) {
            errorMessage = "필수 값이 누락되었습니다.";
        } else if (exception instanceof IllegalStateException) {
            errorMessage = "잘못된 상태입니다.";
        } else if (exception instanceof RuntimeException) {
            errorMessage = "처리 중 오류가 발생했습니다.";
        } else {
            errorMessage = "서버 내부 오류가 발생했습니다.";
        }

        log.error("Unexpected Error at {}: ", request.getRequestURI(), exception);

        var errorResponse = ErrorResponse.of(
                HttpStatus.INTERNAL_SERVER_ERROR,
                errorMessage,
                request.getRequestURI()
        );

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }
}
