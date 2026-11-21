package com.myproject.novel.controller.front;

import com.myproject.novel.common.result.RestResp;
import com.myproject.novel.dto.resp.CategoryRespDto;
import com.myproject.novel.service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/front/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    @GetMapping
    public RestResp<List<CategoryRespDto>> listAll() {
        return RestResp.ok(categoryService.listAll());
    }

    @GetMapping("/{workDirection}")
    public RestResp<List<CategoryRespDto>> listByDirection(@PathVariable Integer workDirection) {
        return RestResp.ok(categoryService.listByWorkDirection(workDirection));
    }
}