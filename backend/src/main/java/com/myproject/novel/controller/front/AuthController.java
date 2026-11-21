package com.myproject.novel.controller.front;

import com.myproject.novel.common.result.RestResp;
import com.myproject.novel.dto.req.LoginReqDto;
import com.myproject.novel.dto.req.OauthLoginReqDto;
import com.myproject.novel.dto.req.RegisterReqDto;
import com.myproject.novel.dto.req.ResetPasswordReqDto;
import com.myproject.novel.dto.resp.CaptionRespDto;
import com.myproject.novel.dto.resp.LoginRespDto;
import com.myproject.novel.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/front/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public RestResp<LoginRespDto> login(@Valid @RequestBody LoginReqDto dto) {
        return RestResp.ok(authService.login(dto));
    }

    @PostMapping("/register")
    public RestResp<LoginRespDto> register(@Valid @RequestBody RegisterReqDto dto) {
        return RestResp.ok(authService.register(dto));
    }

    @GetMapping("/captcha")
    public RestResp<CaptionRespDto> captcha() {
        return RestResp.ok(authService.generateCaptcha());
    }

    @PostMapping("/oauth")
    public RestResp<LoginRespDto> oauthLogin(@Valid @RequestBody OauthLoginReqDto dto) {
        return RestResp.ok(authService.oauthLogin(dto));
    }

    @PostMapping("/reset-password")
    public RestResp<Void> resetPassword(@Valid @RequestBody ResetPasswordReqDto dto) {
        authService.resetPassword(dto);
        return RestResp.ok();
    }
}