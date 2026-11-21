package com.myproject.novel.security;

import com.myproject.novel.common.exception.BusinessException;
import com.myproject.novel.common.exception.ErrorCode;

/**
 * 当前登录用户上下文（ThreadLocal）
 */
public final class UserContext {

    private static final ThreadLocal<LoginUser> HOLDER = new ThreadLocal<>();

    private UserContext() {
    }

    public static void set(LoginUser loginUser) {
        HOLDER.set(loginUser);
    }

    public static LoginUser get() {
        return HOLDER.get();
    }

    /**
     * 获取当前登录用户，未登录则抛异常
     */
    public static LoginUser require() {
        LoginUser user = HOLDER.get();
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_UN_AUTH);
        }
        return user;
    }

    public static Long userId() {
        return require().getUserId();
    }

    public static void clear() {
        HOLDER.remove();
    }
}
