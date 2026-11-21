package com.myproject.novel.controller.front;

import com.myproject.novel.common.result.RestResp;
import com.myproject.novel.dto.resp.RankingRespDto;
import com.myproject.novel.service.RecommendationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/front/rankings")
@RequiredArgsConstructor
public class RankingController {

    private final RecommendationService recommendationService;

    @GetMapping
    public RestResp<RankingRespDto> getRankings() {
        return RestResp.ok(recommendationService.getRankings());
    }
}