// src/main/java/com/manil/manil/product/controller/AnalyzeController.java
package com.manil.manil.product.controller;

import com.manil.manil.global.payload.ResponseDto;
import com.manil.manil.product.dto.request.AnalyzeRequest;
import com.manil.manil.product.dto.response.AnalyzeResponse;
import com.manil.manil.product.service.AnalyzeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class AnalyzeController {

    private final AnalyzeService analyzeService;

    /** JSON 버전 */
    @PostMapping(
            value = "/analyze",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<ResponseDto<AnalyzeResponse>> analyzeJson(@RequestBody AnalyzeRequest request) {
        var result = analyzeService.analyze(request, null, false);
        return ResponseEntity.ok(ResponseDto.of(result));
    }

    /** multipart/form-data 버전 (payload + images[]) */
    @PostMapping(
            value = "/analyze",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<ResponseDto<AnalyzeResponse>> analyzeMultipart(
            @RequestPart("payload") AnalyzeRequest payload,
            @RequestPart(name = "images", required = false) List<MultipartFile> images
    ) {
        var result = analyzeService.analyze(payload, images, true);
        return ResponseEntity.ok(ResponseDto.of(result));
    }
}