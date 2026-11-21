package com.myproject.novel.controller.front;

import com.myproject.novel.common.req.PageReqDto;
import com.myproject.novel.common.result.PageRespDto;
import com.myproject.novel.common.result.RestResp;
import com.myproject.novel.dto.req.ScoreReqDto;
import com.myproject.novel.dto.req.UpdatePasswordReqDto;
import com.myproject.novel.dto.req.UpdateUserReqDto;
import com.myproject.novel.dto.resp.*;
import com.myproject.novel.service.InteractionService;
import com.myproject.novel.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/front/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final InteractionService interactionService;

    @GetMapping("/profile")
    public RestResp<UserInfoRespDto> profile() {
        return RestResp.ok(userService.getProfile());
    }

    @PutMapping("/profile")
    public RestResp<Void> updateProfile(@Valid @RequestBody UpdateUserReqDto dto) {
        userService.updateProfile(dto);
        return RestResp.ok();
    }

    @PutMapping("/password")
    public RestResp<Void> updatePassword(@Valid @RequestBody UpdatePasswordReqDto dto) {
        userService.updatePassword(dto);
        return RestResp.ok();
    }

    @PostMapping("/upgrade-vip")
    public RestResp<Void> upgradeVip() {
        userService.upgradeVip();
        return RestResp.ok();
    }

    @PostMapping("/tags")
    public RestResp<Void> selectTags(@RequestBody List<Long> tagIds) {
        userService.selectTags(tagIds);
        return RestResp.ok();
    }

    @GetMapping("/tags")
    public RestResp<List<TagRespDto>> getUserTags() {
        return RestResp.ok(userService.getUserTags());
    }

    @GetMapping("/bookshelf")
    public RestResp<PageRespDto<BookInfoRespDto>> bookshelf(@ModelAttribute PageReqDto page) {
        return RestResp.ok(interactionService.getBookshelf(page));
    }

    @PostMapping("/bookshelf/{bookId}")
    public RestResp<Void> addToBookshelf(@PathVariable Long bookId) {
        interactionService.addToBookshelf(bookId);
        return RestResp.ok();
    }

    @DeleteMapping("/bookshelf/{bookId}")
    public RestResp<Void> removeFromBookshelf(@PathVariable Long bookId) {
        interactionService.removeFromBookshelf(bookId);
        return RestResp.ok();
    }

    @GetMapping("/read-history")
    public RestResp<PageRespDto<ReadHistoryRespDto>> readHistory(@ModelAttribute PageReqDto page) {
        return RestResp.ok(interactionService.getReadHistory(page));
    }

    @DeleteMapping("/read-history")
    public RestResp<Void> clearHistory() {
        interactionService.clearReadHistory();
        return RestResp.ok();
    }

    @PostMapping("/like/{bookId}")
    public RestResp<Void> likeBook(@PathVariable Long bookId) {
        interactionService.likeBook(bookId);
        return RestResp.ok();
    }

    @PostMapping("/score/{bookId}")
    public RestResp<Void> scoreBook(@PathVariable Long bookId, @Valid @RequestBody ScoreReqDto dto) {
        interactionService.scoreBook(bookId, dto);
        return RestResp.ok();
    }
}