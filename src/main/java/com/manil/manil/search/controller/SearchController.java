// com.manil.manil.search.api.SearchController.java
package com.manil.manil.search.controller;

import com.manil.manil.global.payload.ResponseDto;
import com.manil.manil.search.service.SearchService;
import com.manil.manil.search.dto.SearchResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("api/products") // 최종: /api/products/search
@RequiredArgsConstructor
public class SearchController {

    private final SearchService searchService;

    @GetMapping("/search")
    public ResponseEntity<ResponseDto<SearchResponse>> search(
            @RequestParam String query,
            @RequestParam(required = false) String keywords
    ) {
        var res = searchService.search(query, keywords);
        return ResponseEntity.ok(ResponseDto.of(res));
    }
}