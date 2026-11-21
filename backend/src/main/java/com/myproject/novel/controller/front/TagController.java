package com.myproject.novel.controller.front;

import com.myproject.novel.common.result.RestResp;
import com.myproject.novel.dto.resp.TagRespDto;
import com.myproject.novel.service.TagService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/front/tags")
@RequiredArgsConstructor
public class TagController {

    private final TagService tagService;

    @GetMapping
    public RestResp<List<TagRespDto>> listAll() {
        return RestResp.ok(tagService.listAll());
    }
}