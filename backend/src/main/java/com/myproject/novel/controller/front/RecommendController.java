package com.myproject.novel.controller.front;

import com.myproject.novel.common.result.RestResp;
import com.myproject.novel.dto.resp.BookInfoRespDto;
import com.myproject.novel.dto.resp.RankingRespDto;
import com.myproject.novel.dto.resp.RecommendRespDto;
import com.myproject.novel.service.BookService;
import com.myproject.novel.service.RecommendationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/front/recommend")
@RequiredArgsConstructor
public class RecommendController {

    private final BookService bookService;
    private final RecommendationService recommendationService;

    @GetMapping("/hot")
    public RestResp<List<BookInfoRespDto>> hotRecommend(@RequestParam(defaultValue = "12") int limit) {
        return RestResp.ok(bookService.getHotBooks(limit));
    }

    @GetMapping("/personal")
    public RestResp<List<BookInfoRespDto>> personalRecommend() {
        return RestResp.ok(recommendationService.recommendForUser().getPersonalRecommend());
    }

    @GetMapping("/tag")
    public RestResp<List<BookInfoRespDto>> tagRecommend() {
        return RestResp.ok(recommendationService.recommendForUser().getTagRecommend());
    }

    @GetMapping("/guess")
    public RestResp<List<BookInfoRespDto>> guessYouLike() {
        return RestResp.ok(recommendationService.recommendForUser().getGuessYouLike());
    }

    @GetMapping("/new")
    public RestResp<List<BookInfoRespDto>> newBooks(@RequestParam(defaultValue = "10") int limit) {
        return RestResp.ok(bookService.getNewBooks(limit));
    }

    @GetMapping("/score")
    public RestResp<List<BookInfoRespDto>> scoreBooks(@RequestParam(defaultValue = "10") int limit) {
        return RestResp.ok(bookService.getScoreBooks(limit));
    }

    @GetMapping("/favorited")
    public RestResp<List<BookInfoRespDto>> favoritedBooks(@RequestParam(defaultValue = "10") int limit) {
        return RestResp.ok(bookService.getFavoritedBooks(limit));
    }

    @GetMapping("/all")
    public RestResp<RecommendRespDto> allRecommend() {
        return RestResp.ok(recommendationService.recommendForUser());
    }
}