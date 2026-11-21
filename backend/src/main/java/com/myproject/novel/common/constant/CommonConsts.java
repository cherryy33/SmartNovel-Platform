package com.myproject.novel.common.constant;

/**
 * 通用常量
 */
public final class CommonConsts {

    private CommonConsts() {
        throw new IllegalStateException("常量类不允许实例化");
    }

    /** 角色 */
    public static final class Role {
        /** 普通读者 */
        public static final String READER = "READER";
        /** @deprecated 旧数据使用 USER，等同于 READER */
        @Deprecated
        public static final String USER = "USER";
        public static final String AUTHOR = "AUTHOR";
        public static final String ADMIN = "ADMIN";

        /** 归一化角色字符串：兼容 ROLE_ 前缀与大小写 */
        public static String normalize(String raw) {
            if (raw == null || raw.isBlank()) return READER;
            String r = raw.trim().toUpperCase();
            if (r.startsWith("ROLE_")) r = r.substring(5);
            return switch (r) {
                case "ADMIN" -> ADMIN;
                case "AUTHOR", "WRITER" -> AUTHOR;
                default -> READER;
            };
        }
    }

    /** JWT 声明键 */
    public static final class Jwt {
        public static final String ROLE = "role";
        public static final String AUTHOR_ID = "authorId";
        public static final String USERNAME = "username";
    }

    /** 通用状态：0-否/禁用 1-是/正常 */
    public static final int NO = 0;
    public static final int YES = 1;

    /** 小说上架状态 */
    public static final class AuditStatus {
        public static final int PENDING = 0;
        public static final int PUBLISHED = 1;
        public static final int OFF_SHELF = 2;
        public static final int REJECTED = 3;
    }

    /** 作者审核状态 */
    public static final class AuthorStatus {
        public static final int PENDING = 0;
        public static final int APPROVED = 1;
        public static final int REJECTED = 2;
    }

    /** 第三方登录类型 */
    public static final class OauthType {
        public static final String GITHUB = "github";
        public static final String WECHAT = "wechat";
    }
}
