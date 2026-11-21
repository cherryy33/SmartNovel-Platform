package com.myproject.novel.controller.admin;

import com.myproject.novel.common.result.RestResp;
import com.myproject.novel.dto.req.AdminAiAskReqDto;
import com.myproject.novel.dto.resp.AiToolRespDto;
import com.myproject.novel.dto.resp.DashboardRespDto;
import com.myproject.novel.service.AIService;
import com.myproject.novel.service.StatisticsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminDashboardController {

    private final StatisticsService statisticsService;
    private final AIService aiService;

    @GetMapping("/dashboard")
    public RestResp<DashboardRespDto> dashboard() {
        return RestResp.ok(statisticsService.getDashboard());
    }

    /**
     * 管理员 AI 数据分析问答助手（Tool Calling）
     */
    @PostMapping("/dashboard/ai-ask")
    public RestResp<AiToolRespDto> aiAsk(@Valid @RequestBody AdminAiAskReqDto dto) {
        return RestResp.ok(aiService.adminAiAsk(dto));
    }
}