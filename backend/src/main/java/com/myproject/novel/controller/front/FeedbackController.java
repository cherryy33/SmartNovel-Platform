package com.myproject.novel.controller.front;

import com.myproject.novel.common.result.RestResp;
import com.myproject.novel.dto.req.FeedbackReqDto;
import com.myproject.novel.service.AdminService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/front/feedback")
@RequiredArgsConstructor
public class FeedbackController {

    private final AdminService adminService;

    @PostMapping
    public RestResp<Void> addFeedback(@Valid @RequestBody FeedbackReqDto dto) {
        adminService.addFeedback(dto);
        return RestResp.ok();
    }
}