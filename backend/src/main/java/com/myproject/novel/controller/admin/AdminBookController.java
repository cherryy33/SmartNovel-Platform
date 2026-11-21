package com.myproject.novel.controller.admin;

import com.myproject.novel.common.req.PageReqDto;
import com.myproject.novel.common.result.PageRespDto;
import com.myproject.novel.common.result.RestResp;
import com.myproject.novel.dto.resp.BookInfoRespDto;
import com.myproject.novel.service.AdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/books")
@RequiredArgsConstructor
public class AdminBookController {

    private final AdminService adminService;

    @GetMapping
    public RestResp<PageRespDto<BookInfoRespDto>> list(
            @ModelAttribute PageReqDto page,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer auditStatus) {
        return RestResp.ok(adminService.listAllBooks(page, keyword, auditStatus));
    }

    @PutMapping("/{bookId}/status")
    public RestResp<Void> updateStatus(@PathVariable Long bookId,
            @RequestBody Map<String, Integer> body) {
        adminService.updateBookStatus(bookId, body.get("auditStatus"));
        return RestResp.ok();
    }

    @DeleteMapping("/{bookId}")
    public RestResp<Void> delete(@PathVariable Long bookId) {
        adminService.deleteBook(bookId);
        return RestResp.ok();
    }
}