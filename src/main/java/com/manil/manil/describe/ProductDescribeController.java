package com.manil.manil.describe;

import com.manil.manil.describe.DescribeDto.DescribeRequest;
import com.manil.manil.describe.DescribeDto.DescribeResponse;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/products")
public class ProductDescribeController {

    private final ProductDescribeService service;

    public ProductDescribeController(ProductDescribeService service) {
        this.service = service;
    }

    // 예: POST /api/products/123/describe  { "hint": "선물용", "locale": "ko" }
    @PostMapping("/{id}/describe")
    public DescribeResponse describe(@PathVariable("id") Long id,
                                     @RequestBody DescribeRequest req) {
        return service.describe(id, req);
    }
}

