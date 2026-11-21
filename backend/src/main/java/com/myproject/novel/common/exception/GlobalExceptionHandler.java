package com.myproject.novel.common.exception;

import com.myproject.novel.common.result.RestResp;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.async.AsyncRequestNotUsableException;

/**
 * 全局异常处理器
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public RestResp<Void> handleBusinessException(BusinessException e) {
        return RestResp.fail(e.getErrorCode());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public RestResp<Void> handleValidException(MethodArgumentNotValidException e) {
        FieldError fieldError = e.getBindingResult().getFieldError();
        String message = fieldError == null ? ErrorCode.PARAM_ERROR.getMessage() : fieldError.getDefaultMessage();
        return RestResp.fail(ErrorCode.PARAM_ERROR, message);
    }

    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public RestResp<Void> handleAccessDenied(AccessDeniedException e) {
        return RestResp.fail(ErrorCode.USER_NO_PERMISSION);
    }

    /**
     * 客户端在流式响应（如 AI 边生成边推送）过程中断开连接，属于正常操作，仅记录不告警。
     */
    @ExceptionHandler(AsyncRequestNotUsableException.class)
    public void handleClientDisconnected(AsyncRequestNotUsableException e) {
        log.debug("客户端已断开流式连接: {}", e.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public RestResp<Void> handleException(Exception e) {
        log.error("系统异常", e);
        return RestResp.fail(ErrorCode.SYSTEM_ERROR);
    }
}
