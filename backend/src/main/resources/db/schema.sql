-- ============================================================================
-- 智能小说阅读与创作平台（Smart Novel Platform）数据库结构定义
-- 目标数据库：MySQL 8.0  /  数据库名：smart_novel_pro
-- 设计说明：
--   * 原创前后端分离全栈项目，已彻底移除原开源项目(io.github.xxyopen)的支付、
--     稿费、邀请码、XXL-JOB 等与「轻量级平替」无关的历史表。
--   * 封面零图片存储：book_info.pic_url 允许为空，由前端 <BookCover /> 组件
--     以纯 CSS/SVG 动态渲染精装拟物书封。
--   * 轻量级 VIP：用户/小说/章节均以 is_vip 标识，无订单、无金额、无真实支付。
--   * 推荐：基于用户行为(浏览1/点赞3/收藏5/评分)的 Item-Based 协同过滤，
--     行为数据落在 user_* 系列表。
-- ============================================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ============================ 用户与权限 ====================================

-- 普通用户（读者/作者）表
DROP TABLE IF EXISTS `user_info`;
CREATE TABLE `user_info` (
    `id`          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    `username`    VARCHAR(50)     NOT NULL COMMENT '登录名',
    `password`    VARCHAR(100)    NOT NULL COMMENT '登录密码(BCrypt 加密)',
    `nick_name`   VARCHAR(50)     DEFAULT NULL COMMENT '昵称',
    `user_photo`  VARCHAR(200)    DEFAULT NULL COMMENT '用户头像',
    `user_sex`    TINYINT UNSIGNED DEFAULT NULL COMMENT '性别;0-男 1-女',
    `email`       VARCHAR(100)    DEFAULT NULL COMMENT '邮箱',
    `phone`       VARCHAR(20)     DEFAULT NULL COMMENT '手机号',
    `level`       INT UNSIGNED    NOT NULL DEFAULT 1 COMMENT '用户等级',
    `is_vip`      TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '是否VIP;0-普通 1-VIP',
    `status`      TINYINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '状态;0-禁用 1-正常',
    `create_time` DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_username` (`username`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '普通用户';

-- 管理员账号表
DROP TABLE IF EXISTS `sys_user`;
CREATE TABLE `sys_user` (
    `id`          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    `username`    VARCHAR(50)     NOT NULL COMMENT '登录名',
    `password`    VARCHAR(100)    NOT NULL COMMENT '登录密码(BCrypt 加密)',
    `name`        VARCHAR(100)    DEFAULT NULL COMMENT '管理员姓名',
    `avatar`      VARCHAR(200)    DEFAULT NULL COMMENT '头像',
    `status`      TINYINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '状态;0-禁用 1-正常',
    `create_time` DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_username` (`username`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '管理员账号';

-- 第三方登录绑定表（模拟 OAuth，仅记录 GitHub / 微信 openid）
DROP TABLE IF EXISTS `user_oauth`;
CREATE TABLE `user_oauth` (
    `id`          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    `user_id`     BIGINT UNSIGNED NOT NULL COMMENT '用户ID',
    `oauth_type`  VARCHAR(20)     NOT NULL COMMENT '第三方类型;github-微信 wechat',
    `oauth_id`    VARCHAR(100)    NOT NULL COMMENT '第三方唯一标识(模拟 openid)',
    `create_time` DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_type_oauth` (`oauth_type`, `oauth_id`),
    KEY `idx_user_id` (`user_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '第三方登录绑定';

-- 用户兴趣标签表（用于兴趣标签推荐）
DROP TABLE IF EXISTS `user_tag`;
CREATE TABLE `user_tag` (
    `id`          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    `user_id`     BIGINT UNSIGNED NOT NULL COMMENT '用户ID',
    `tag_id`      BIGINT UNSIGNED NOT NULL COMMENT '标签ID',
    `create_time` DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_tag` (`user_id`, `tag_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '用户兴趣标签';

-- ============================ 作者 ==========================================

-- 作者信息 / 入驻审核表
DROP TABLE IF EXISTS `author_info`;
CREATE TABLE `author_info` (
    `id`             BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    `user_id`        BIGINT UNSIGNED NOT NULL COMMENT '用户ID',
    `pen_name`       VARCHAR(50)     NOT NULL COMMENT '笔名',
    `intro`          VARCHAR(500)    DEFAULT NULL COMMENT '作者简介',
    `status`         TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '审核状态;0-待审核 1-审核通过 2-审核拒绝',
    `review_comment` VARCHAR(500)    DEFAULT NULL COMMENT '审核意见',
    `create_time`    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_id` (`user_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '作者信息';

-- ============================ 小说 ==========================================

-- 小说分类表
DROP TABLE IF EXISTS `book_category`;
CREATE TABLE `book_category` (
    `id`             BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    `work_direction` TINYINT UNSIGNED NOT NULL COMMENT '作品方向;0-男频 1-女频',
    `name`           VARCHAR(20)     NOT NULL COMMENT '类别名',
    `sort`           TINYINT UNSIGNED NOT NULL DEFAULT 10 COMMENT '排序',
    `create_time`    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '小说分类';

-- 兴趣标签表（用于小说打标与兴趣标签推荐）
DROP TABLE IF EXISTS `tag`;
CREATE TABLE `tag` (
    `id`          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    `name`        VARCHAR(50)     NOT NULL COMMENT '标签名',
    `sort`        INT             NOT NULL DEFAULT 0 COMMENT '排序',
    `create_time` DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_name` (`name`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '兴趣标签';

-- 小说信息表
DROP TABLE IF EXISTS `book_info`;
CREATE TABLE `book_info` (
    `id`                      BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    `work_direction`          TINYINT UNSIGNED DEFAULT NULL COMMENT '作品方向;0-男频 1-女频',
    `category_id`             BIGINT UNSIGNED DEFAULT NULL COMMENT '分类ID',
    `category_name`           VARCHAR(50)     DEFAULT NULL COMMENT '分类名',
    `pic_url`                 VARCHAR(200)    NOT NULL DEFAULT '' COMMENT '封面地址;为空则前端动态渲染书封',
    `book_name`               VARCHAR(50)     NOT NULL COMMENT '小说名',
    `author_id`               BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '作者ID;0-未关联作者',
    `author_name`             VARCHAR(50)     NOT NULL COMMENT '作者名',
    `book_desc`               VARCHAR(2000)   NOT NULL COMMENT '书籍描述',
    `score`                   TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '评分;总分10, 真实评分=score/10',
    `book_status`             TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '书籍状态;0-连载中 1-已完结',
    `visit_count`             BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '点击量/阅读量',
    `word_count`              INT UNSIGNED    NOT NULL DEFAULT 0 COMMENT '总字数',
    `comment_count`           INT UNSIGNED    NOT NULL DEFAULT 0 COMMENT '评论数',
    `last_chapter_id`         BIGINT UNSIGNED DEFAULT NULL COMMENT '最新章节ID',
    `last_chapter_name`       VARCHAR(50)     DEFAULT NULL COMMENT '最新章节名',
    `last_chapter_update_time` DATETIME       DEFAULT NULL COMMENT '最新章节更新时间',
    `is_vip`                  TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '是否VIP;0-免费 1-VIP',
    `audit_status`            TINYINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '上架状态;0-待审核 1-已上架 2-已下架 3-审核拒绝',
    `create_time`             DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`             DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_book_name_author` (`book_name`, `author_name`),
    KEY `idx_category_id` (`category_id`),
    KEY `idx_create_time` (`create_time`),
    KEY `idx_last_chapter_update_time` (`last_chapter_update_time`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '小说信息';

-- 小说-标签关联表
DROP TABLE IF EXISTS `book_tag`;
CREATE TABLE `book_tag` (
    `id`      BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    `book_id` BIGINT UNSIGNED NOT NULL COMMENT '小说ID',
    `tag_id`  BIGINT UNSIGNED NOT NULL COMMENT '标签ID',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_book_tag` (`book_id`, `tag_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '小说标签关联';

-- 小说章节表
DROP TABLE IF EXISTS `book_chapter`;
CREATE TABLE `book_chapter` (
    `id`           BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    `book_id`      BIGINT UNSIGNED NOT NULL COMMENT '小说ID',
    `chapter_num`  SMALLINT UNSIGNED NOT NULL COMMENT '章节号',
    `chapter_name` VARCHAR(100)    NOT NULL COMMENT '章节名',
    `word_count`   INT UNSIGNED    NOT NULL COMMENT '章节字数',
    `is_vip`       TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '是否VIP;0-免费 1-VIP',
    `create_time`  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_book_id_chapter_num` (`book_id`, `chapter_num`),
    KEY `idx_book_id` (`book_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '小说章节';

-- 章节内容表
DROP TABLE IF EXISTS `book_content`;
CREATE TABLE `book_content` (
    `id`          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    `chapter_id`  BIGINT UNSIGNED NOT NULL COMMENT '章节ID',
    `content`     MEDIUMTEXT      NOT NULL COMMENT '章节正文',
    `create_time` DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_chapter_id` (`chapter_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '章节内容';

-- ============================ 用户行为（互动） ===============================

-- 收藏/书架表
DROP TABLE IF EXISTS `user_bookshelf`;
CREATE TABLE `user_bookshelf` (
    `id`          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    `user_id`     BIGINT UNSIGNED NOT NULL COMMENT '用户ID',
    `book_id`     BIGINT UNSIGNED NOT NULL COMMENT '小说ID',
    `create_time` DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_book` (`user_id`, `book_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '用户书架(收藏)';

-- 浏览历史 / 阅读进度表
DROP TABLE IF EXISTS `user_read_history`;
CREATE TABLE `user_read_history` (
    `id`              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    `user_id`         BIGINT UNSIGNED NOT NULL COMMENT '用户ID',
    `book_id`         BIGINT UNSIGNED NOT NULL COMMENT '小说ID',
    `last_chapter_id` BIGINT UNSIGNED DEFAULT NULL COMMENT '上次阅读章节ID',
    `last_read_time`  DATETIME        DEFAULT NULL COMMENT '上次阅读时间',
    `create_time`     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_book` (`user_id`, `book_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '浏览历史与阅读进度';

-- 点赞表
DROP TABLE IF EXISTS `user_book_like`;
CREATE TABLE `user_book_like` (
    `id`          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    `user_id`     BIGINT UNSIGNED NOT NULL COMMENT '用户ID',
    `book_id`     BIGINT UNSIGNED NOT NULL COMMENT '小说ID',
    `create_time` DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_book` (`user_id`, `book_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '小说点赞';

-- 评分表（1-5 星）
DROP TABLE IF EXISTS `user_book_score`;
CREATE TABLE `user_book_score` (
    `id`          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    `user_id`     BIGINT UNSIGNED NOT NULL COMMENT '用户ID',
    `book_id`     BIGINT UNSIGNED NOT NULL COMMENT '小说ID',
    `score`       TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '星级评分;1-5',
    `create_time` DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_book` (`user_id`, `book_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '小说评分';

-- ============================ 评论 ==========================================

-- 书评 / 章节评论表
DROP TABLE IF EXISTS `book_comment`;
CREATE TABLE `book_comment` (
    `id`            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    `book_id`       BIGINT UNSIGNED NOT NULL COMMENT '小说ID',
    `chapter_id`    BIGINT UNSIGNED DEFAULT NULL COMMENT '章节ID;为空表示书评',
    `user_id`       BIGINT UNSIGNED NOT NULL COMMENT '评论用户ID',
    `content`       VARCHAR(1000)   NOT NULL COMMENT '评论内容',
    `reply_count`   INT UNSIGNED    NOT NULL DEFAULT 0 COMMENT '回复数量',
    `audit_status`  TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '审核状态;0-待审核 1-通过 2-拒绝',
    `create_time`   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_book_id` (`book_id`),
    KEY `idx_user_id` (`user_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '书评与章节评论';

-- 评论回复表
DROP TABLE IF EXISTS `comment_reply`;
CREATE TABLE `comment_reply` (
    `id`            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    `comment_id`    BIGINT UNSIGNED NOT NULL COMMENT '评论ID',
    `user_id`       BIGINT UNSIGNED NOT NULL COMMENT '回复用户ID',
    `reply_content` VARCHAR(1000)   NOT NULL COMMENT '回复内容',
    `audit_status`  TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '审核状态;0-待审核 1-通过 2-拒绝',
    `create_time`   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_comment_id` (`comment_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '评论回复';

-- ============================ 社区与私信 ======================================

-- 读者社区帖子表
DROP TABLE IF EXISTS `community_post`;
CREATE TABLE `community_post` (
    `id`          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    `user_id`     BIGINT UNSIGNED NOT NULL COMMENT '发帖用户ID',
    `title`       VARCHAR(200)    NOT NULL COMMENT '帖子标题',
    `content`     TEXT            NOT NULL COMMENT '帖子内容',
    `like_count`  INT UNSIGNED    NOT NULL DEFAULT 0 COMMENT '点赞数',
    `reply_count` INT UNSIGNED    NOT NULL DEFAULT 0 COMMENT '回复数',
    `status`      TINYINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '状态;0-待审核 1-已发布 2-已删除',
    `create_time` DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '读者社区帖子';

-- 社区回帖表
DROP TABLE IF EXISTS `community_reply`;
CREATE TABLE `community_reply` (
    `id`            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    `post_id`       BIGINT UNSIGNED NOT NULL COMMENT '帖子ID',
    `user_id`       BIGINT UNSIGNED NOT NULL COMMENT '回复用户ID',
    `reply_content` VARCHAR(1000)   NOT NULL COMMENT '回复内容',
    `status`        TINYINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '状态;0-待审核 1-已发布 2-已删除',
    `create_time`   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_post_id` (`post_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '社区回帖';

-- 站内私信表（HTTP 轮询，非 WebSocket）
DROP TABLE IF EXISTS `private_message`;
CREATE TABLE `private_message` (
    `id`          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    `sender_id`   BIGINT UNSIGNED NOT NULL COMMENT '发送者用户ID',
    `receiver_id` BIGINT UNSIGNED NOT NULL COMMENT '接收者用户ID',
    `content`     VARCHAR(1000)   NOT NULL COMMENT '私信内容',
    `is_read`     TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '是否已读;0-未读 1-已读',
    `create_time` DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '发送时间',
    PRIMARY KEY (`id`),
    KEY `idx_sender_receiver` (`sender_id`, `receiver_id`),
    KEY `idx_receiver` (`receiver_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '站内私信';

-- ============================ 留言与反馈 ======================================

-- 平台留言板表
DROP TABLE IF EXISTS `message_board`;
CREATE TABLE `message_board` (
    `id`          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    `user_id`     BIGINT UNSIGNED NOT NULL COMMENT '留言用户ID',
    `content`     VARCHAR(1000)   NOT NULL COMMENT '留言内容',
    `reply`       VARCHAR(1000)   DEFAULT NULL COMMENT '官方回复',
    `status`      TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '状态;0-待处理 1-已回复 2-已删除',
    `create_time` DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '平台留言板';

-- 用户反馈表
DROP TABLE IF EXISTS `user_feedback`;
CREATE TABLE `user_feedback` (
    `id`          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    `user_id`     BIGINT UNSIGNED NOT NULL COMMENT '反馈用户ID',
    `content`     VARCHAR(1000)   NOT NULL COMMENT '反馈内容',
    `reply`       VARCHAR(1000)   DEFAULT NULL COMMENT '官方回复',
    `status`      TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '状态;0-待处理 1-已回复 2-已删除',
    `create_time` DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '用户反馈';

-- ============================ 系统配置 =======================================

-- 首页轮播图表
DROP TABLE IF EXISTS `banner`;
CREATE TABLE `banner` (
    `id`          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    `title`       VARCHAR(100)    DEFAULT NULL COMMENT '轮播图标题',
    `image_url`   VARCHAR(200)    DEFAULT NULL COMMENT '轮播图地址',
    `book_id`     BIGINT UNSIGNED DEFAULT NULL COMMENT '关联小说ID(可选)',
    `sort`        INT             NOT NULL DEFAULT 0 COMMENT '排序',
    `status`      TINYINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '状态;0-下架 1-上架',
    `create_time` DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '首页轮播图';

-- 系统公告表
DROP TABLE IF EXISTS `notice`;
CREATE TABLE `notice` (
    `id`          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    `title`       VARCHAR(200)    NOT NULL COMMENT '公告标题',
    `content`     TEXT            NOT NULL COMMENT '公告内容',
    `status`      TINYINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '状态;0-下线 1-上线',
    `create_time` DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '系统公告';

-- ============================ AI 知识库（RAG） ================================
-- 向量本身存放于 SimpleVectorStore 本地 JSON 文件，
-- 以下两表仅负责「索引进度」与「分块原文/引用溯源」，便于增量构建与前端展示引用来源。

DROP TABLE IF EXISTS `book_ai_index`;
CREATE TABLE `book_ai_index` (
    `id`                  BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    `book_id`             BIGINT UNSIGNED NOT NULL COMMENT '小说ID',
    `indexed_chapter_num` INT             NOT NULL DEFAULT 0 COMMENT '已向量化的最大章节号;0-尚未构建',
    `indexed_chapter_id`  BIGINT UNSIGNED DEFAULT NULL COMMENT '已向量化的最大章节ID',
    `chunk_count`         INT             NOT NULL DEFAULT 0 COMMENT '向量块总数',
    `embedding_model`     VARCHAR(64)     DEFAULT NULL COMMENT '向量模型',
    `status`              TINYINT         NOT NULL DEFAULT 0 COMMENT '状态;0-未构建 1-构建中 2-已就绪 3-构建失败',
    `error_msg`           VARCHAR(500)    DEFAULT NULL COMMENT '最近失败原因',
    `create_time`         DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`         DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_book_id` (`book_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '小说AI知识库索引状态';

DROP TABLE IF EXISTS `book_ai_chunk`;
CREATE TABLE `book_ai_chunk` (
    `id`           BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    `book_id`      BIGINT UNSIGNED NOT NULL COMMENT '小说ID',
    `chapter_id`   BIGINT UNSIGNED NOT NULL COMMENT '章节ID',
    `chapter_num`  INT             NOT NULL COMMENT '章节号',
    `chapter_name` VARCHAR(100)    NOT NULL COMMENT '章节名',
    `chunk_index`  INT             NOT NULL COMMENT '章节内分块序号;从0开始',
    `content`      TEXT            NOT NULL COMMENT '分块文本(章节正文片段)',
    `char_count`   INT             NOT NULL DEFAULT 0 COMMENT '分块字符数',
    `vector_id`    VARCHAR(80)     NOT NULL COMMENT '向量库文档ID',
    `create_time`  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_chapter_chunk` (`chapter_id`, `chunk_index`),
    KEY `idx_book_chapter_num` (`book_id`, `chapter_num`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '小说AI知识库分块';

SET FOREIGN_KEY_CHECKS = 1;

-- ============================ 初始化种子数据 ==================================

-- 默认兴趣标签（用于小说打标与「兴趣标签推荐」）
INSERT INTO `tag` (`name`, `sort`) VALUES
('热血', 1),
('甜宠', 2),
('悬疑', 3),
('爽文', 4),
('穿越', 5),
('重生', 6),
('系统流', 7),
('无敌流', 8),
('轻松日常', 9),
('虐恋', 10);
