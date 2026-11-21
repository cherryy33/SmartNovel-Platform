package com.myproject.novel.common.result;

import com.myproject.novel.common.exception.ErrorCode;
import lombok.Getter;

import java.util.Objects;

/**
 * 统一响应体
 */
@Getter
public class RestResp<T> {

    private final String code;
    private final String message;
    private final T data;

    private RestResp(String code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    public static <T> RestResp<T> ok() {
        return new RestResp<>(ErrorCode.OK.getCode(), ErrorCode.OK.getMessage(), null);
    }

    public static <T> RestResp<T> ok(T data) {
        return new RestResp<>(ErrorCode.OK.getCode(), ErrorCode.OK.getMessage(), data);
    }

    public static <T> RestResp<T> fail(ErrorCode errorCode) {
        return new RestResp<>(errorCode.getCode(), errorCode.getMessage(), null);
    }

    public static <T> RestResp<T> fail(ErrorCode errorCode, String message) {
        return new RestResp<>(errorCode.getCode(), message, null);
    }

    public boolean isOk() {
        return Objects.equals(this.code, ErrorCode.OK.getCode());
    }
}
