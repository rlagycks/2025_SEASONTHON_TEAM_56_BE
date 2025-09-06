package com.manil.manil.global.exception;

import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.ErrorResponseException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingPathVariableException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;

import java.net.SocketTimeoutException;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class ExceptionHandlerAdvice {

    /* ========= 비즈니스 예외 ========= */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> business(BusinessException ex, HttpServletRequest req) {
        var msg = """
            Business Error
            - URI: {}
            - Status: {}
            - Message: {}
            """;
        switch (ex.getHttpStatus().series()) {
            case CLIENT_ERROR -> log.warn(msg, req.getRequestURI(), ex.getHttpStatus(), ex.getMessage());
            case SERVER_ERROR -> log.error(msg, req.getRequestURI(), ex.getHttpStatus(), ex.getMessage(), ex);
            default -> log.info(msg, req.getRequestURI(), ex.getHttpStatus(), ex.getMessage());
        }
        return build(ex.getHttpStatus(), ex.getMessage(), req);
    }

    /* ========= 요청 바디/JSON 문제 -> 400 ========= */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> badJson(HttpMessageNotReadableException ex, HttpServletRequest req) {
        // Jackson의 알 수 없는 필드 에러를 사람이 읽기 쉽게 가공
        String msg = "요청 본문을 읽을 수 없습니다.";
        Throwable cause = ex.getMostSpecificCause();
        if (cause instanceof UnrecognizedPropertyException u) {
            // 예: Unrecognized field "productName"
            msg = "지원하지 않는 필드: \"" + u.getPropertyName() + "\". 필드명을 확인하세요.";
        }
        log.warn("Bad JSON at {}: {}", req.getRequestURI(), ex.getMessage());
        return build(HttpStatus.BAD_REQUEST, msg, req);
    }

    /* ========= @Valid 바디 필드 에러 -> 400 ========= */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> invalidBody(MethodArgumentNotValidException ex, HttpServletRequest req) {
        String msg = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getDefaultMessage() != null ? fe.getDefaultMessage() : (fe.getField() + " is invalid"))
                .collect(Collectors.joining(", "));
        log.warn("Validation Error at {}: {}", req.getRequestURI(), msg);
        return build(HttpStatus.BAD_REQUEST, msg.isBlank() ? "잘못된 요청입니다." : msg, req);
    }

    @ExceptionHandler({
            ConstraintViolationException.class,
            MissingServletRequestParameterException.class,
            MissingPathVariableException.class,
            MethodArgumentTypeMismatchException.class,
            IllegalArgumentException.class
    })
    public ResponseEntity<ErrorResponse> badRequest(Exception ex, HttpServletRequest req) {

        String msg;

        if (ex instanceof ConstraintViolationException cve) {
            msg = cve.getConstraintViolations().stream()
                    .map(v -> v.getMessage())
                    .collect(Collectors.joining(", "));
        } else if (ex instanceof MissingServletRequestParameterException msrp) {
            msg = "필수 파라미터 누락: " + msrp.getParameterName();
        } else if (ex instanceof MissingPathVariableException mpv) {
            msg = "필수 경로변수 누락: " + mpv.getVariableName();
        } else if (ex instanceof MethodArgumentTypeMismatchException matm) {
            msg = "파라미터 타입 오류: " + matm.getName() + " 는 " +
                    (matm.getRequiredType() != null ? matm.getRequiredType().getSimpleName() : "올바른 타입") + " 이어야 합니다.";
        } else if (ex instanceof IllegalArgumentException iae) {
            msg = iae.getMessage();
        } else {
            msg = "잘못된 요청입니다.";
        }

        if (msg == null || msg.isBlank()) msg = "잘못된 요청입니다.";
        log.warn("Bad Request at {}: {}", req.getRequestURI(), msg);

        var body = ErrorResponse.of(HttpStatus.BAD_REQUEST, msg, req.getRequestURI());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    /* ========= DB 무결성/중복 -> 409 ========= */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> conflict(DataIntegrityViolationException ex, HttpServletRequest req) {
        log.warn("Data conflict at {}: {}", req.getRequestURI(), ex.getMostSpecificCause().getMessage());
        return build(HttpStatus.CONFLICT, "데이터 무결성 위반(중복 또는 제약조건 위반)입니다.", req);
    }

    /* ========= 외부 연동 4xx/5xx -> 502 ========= */
    @ExceptionHandler({
            WebClientResponseException.class, // WebClient
            HttpClientErrorException.class,   // RestClient (Spring 6)
            ErrorResponseException.class      // 일부 스프링 에러 래퍼
    })
    public ResponseEntity<ErrorResponse> upstreamError(RuntimeException ex, HttpServletRequest req) {
        String detail = ex.getMessage();
        if (ex instanceof WebClientResponseException w) {
            detail = safeBodySnippet(w.getResponseBodyAsString());
        } else if (ex instanceof HttpClientErrorException h) {
            detail = safeBodySnippet(h.getResponseBodyAsString());
        }
        String msg = "외부 서비스 오류(게이트웨이). " + (detail != null ? detail : "");
        log.error("Upstream error at {}: {}", req.getRequestURI(), ex.getMessage(), ex);
        return build(HttpStatus.BAD_GATEWAY, msg, req);
    }

    /* ========= 외부 연동 타임아웃/네트워크 -> 504 ========= */
    @ExceptionHandler({
            WebClientRequestException.class,
            SocketTimeoutException.class,
            TimeoutException.class
    })
    public ResponseEntity<ErrorResponse> upstreamTimeout(Exception ex, HttpServletRequest req) {
        log.error("Upstream timeout at {}: {}", req.getRequestURI(), ex.getMessage());
        return build(HttpStatus.GATEWAY_TIMEOUT, "외부 서비스 응답 지연(타임아웃)입니다.", req);
    }

    /* ========= 컨트롤러에서 직접 던진 상태코드 그대로 ========= */
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ErrorResponse> responseStatus(ResponseStatusException ex, HttpServletRequest req) {
        log.warn("ResponseStatusException at {}: {}", req.getRequestURI(), ex.getReason());
        HttpStatus status = HttpStatus.resolve(ex.getStatusCode().value());
        return build(status != null ? status : HttpStatus.INTERNAL_SERVER_ERROR,
                ex.getReason() != null ? ex.getReason() : "오류가 발생했습니다.", req);
    }

    /* ========= 최종 Fallback -> 500 ========= */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> fallback(Exception ex, HttpServletRequest req) {
        String msg = (ex instanceof NullPointerException) ? "필수 값이 누락되었습니다."
                : (ex instanceof IllegalStateException) ? "잘못된 상태입니다."
                : "서버 내부 오류가 발생했습니다.";
        log.error("Unexpected Error at {}: ", req.getRequestURI(), ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, msg, req);
    }

    /* ========= helpers ========= */
    private ResponseEntity<ErrorResponse> build(HttpStatus status, String message, HttpServletRequest req) {
        return ResponseEntity.status(status)
                .body(ErrorResponse.of(status, message, req.getRequestURI()));
    }

    private String safeBodySnippet(String body) {
        if (body == null) return null;
        var trimmed = body.replaceAll("\\s+", " ").trim();
        return trimmed.length() > 300 ? trimmed.substring(0, 300) + "..." : trimmed;
    }
}