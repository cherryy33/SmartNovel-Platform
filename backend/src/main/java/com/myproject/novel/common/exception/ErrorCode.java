package com.myproject.novel.common.exception;

import lombok.Getter;

/**
 * 统一错误码
 */
@Getter
public enum ErrorCode {

    OK("00000", "成功"),

    // ---------- 通用 / 系统 ----------
    SYSTEM_ERROR("B0001", "系统异常，请稍后重试"),
    PARAM_ERROR("B0002", "请求参数错误"),
    THIRD_SERVICE_ERROR("B0003", "第三方服务调用失败"),

    // ---------- 认证 / 用户 ----------
    USER_UN_AUTH("A0001", "未登录或登录已过期"),
    USER_NOT_FOUND("A0002", "用户不存在"),
    USER_ACCOUNT_OR_PASSWORD_ERROR("A0003", "账号或密码错误"),
    USER_DISABLED("A0004", "账号已被禁用"),
    USER_ALREADY_EXISTS("A0005", "该账号已存在"),
    USER_NO_PERMISSION("A0006", "无权执行该操作"),
    CAPTCHA_ERROR("A0007", "验证码错误或已过期"),

    // ---------- 小说 / 章节 ----------
    BOOK_NOT_FOUND("A0011", "小说不存在"),
    CHAPTER_NOT_FOUND("A0012", "章节不存在"),
    CATEGORY_NOT_FOUND("A0013", "分类不存在"),

    // ---------- VIP ----------
    VIP_REQUIRED("A0021", "该章节为 VIP 章节，请先开通 VIP"),

    // ---------- 作者 ----------
    AUTHOR_NOT_APPROVED("A0031", "作者认证未通过审核"),
    AUTHOR_ALREADY_EXISTS("A0032", "您已提交过作者认证"),
    AUTHOR_APPLY_PENDING("A0033", "您的作者申请正在审核中，请耐心等待"),
    ROLE_MISMATCH("A0034", "该账号无对应身份权限"),

    // ---------- 管理员 ----------
    ADMIN_NOT_FOUND("A0041", "管理员不存在");

    private final String code;
    private final String message;

    ErrorCode(String code, String message) {
        this.code = code;
        this.message = message;
    }
}
