package com.myproject.novel.controller.author;

import com.myproject.novel.common.result.RestResp;
import com.myproject.novel.dto.req.AuthorApplyReqDto;
import com.myproject.novel.dto.resp.AuthorInfoRespDto;
import com.myproject.novel.service.AuthorService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/author")
@RequiredArgsConstructor
public class AuthorController {

    private final AuthorService authorService;

    @GetMapping("/info")
    public RestResp<AuthorInfoRespDto> getInfo() {
        return RestResp.ok(authorService.getMyAuthorInfo());
    }

    @PostMapping("/apply")
    public RestResp<Void> apply(@Valid @RequestBody AuthorApplyReqDto dto) {
        authorService.apply(dto);
        return RestResp.ok();
    }

    /** 查询本人最近的作者申请状态（未提交返回 null） */
    @GetMapping("/apply/status")
    public RestResp<AuthorInfoRespDto> applyStatus() {
        return RestResp.ok(authorService.getMyAuthorInfo());
    }

    @PutMapping("/info")
    public RestResp<Void> updateInfo(@Valid @RequestBody AuthorApplyReqDto dto) {
        authorService.updateAuthorInfo(dto);
        return RestResp.ok();
    }
}