package com.myproject.novel.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.myproject.novel.common.constant.CommonConsts;
import com.myproject.novel.common.exception.BusinessException;
import com.myproject.novel.common.exception.ErrorCode;
import com.myproject.novel.common.req.PageReqDto;
import com.myproject.novel.common.result.PageRespDto;
import com.myproject.novel.dao.entity.*;
import com.myproject.novel.dao.mapper.*;
import com.myproject.novel.dto.req.*;
import com.myproject.novel.dto.resp.*;
import com.myproject.novel.security.UserContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 管理员服务：用户管理、作者审核、Banner管理、公告管理、标签管理、分类管理、留言板管理、反馈管理
 */
@Service
@RequiredArgsConstructor
public class AdminService {

    private final UserInfoMapper userInfoMapper;
    private final AuthorInfoMapper authorInfoMapper;
    private final BannerMapper bannerMapper;
    private final NoticeMapper noticeMapper;
    private final TagMapper tagMapper;
    private final BookCategoryMapper bookCategoryMapper;
    private final BookInfoMapper bookInfoMapper;
    private final BookCommentMapper bookCommentMapper;
    private final CommentReplyMapper commentReplyMapper;
    private final CommunityPostMapper communityPostMapper;
    private final CommunityReplyMapper communityReplyMapper;
    private final MessageBoardMapper messageBoardMapper;
    private final UserFeedbackMapper userFeedbackMapper;

    // ==================== 用户管理 ====================
    public PageRespDto<UserInfoRespDto> listUsers(PageReqDto page, String keyword) {
        LambdaQueryWrapper<UserInfo> wrapper = new LambdaQueryWrapper<UserInfo>();
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.like(UserInfo::getUsername, keyword).or().like(UserInfo::getNickName, keyword);
        }
        wrapper.orderByDesc(UserInfo::getCreateTime);
        IPage<UserInfo> result = userInfoMapper.selectPage(
            new Page<>(page.getPageNum(), page.getPageSize()), wrapper);
        List<UserInfoRespDto> list = result.getRecords().stream().map(u -> {
            UserInfoRespDto dto = new UserInfoRespDto();
            dto.setId(u.getId()); dto.setUsername(u.getUsername());
            dto.setNickName(u.getNickName()); dto.setUserPhoto(u.getUserPhoto());
            dto.setUserSex(u.getUserSex()); dto.setEmail(u.getEmail());
            dto.setPhone(u.getPhone()); dto.setLevel(u.getLevel());
            dto.setIsVip(u.getIsVip()); dto.setStatus(u.getStatus());
            dto.setCreateTime(u.getCreateTime());
            return dto;
        }).collect(Collectors.toList());
        return PageRespDto.of(result.getCurrent(), result.getSize(), result.getTotal(), list);
    }

    public UserInfoRespDto getUser(Long userId) {
        UserInfo u = userInfoMapper.selectById(userId);
        if (u == null) throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        UserInfoRespDto dto = new UserInfoRespDto();
        dto.setId(u.getId()); dto.setUsername(u.getUsername());
        dto.setNickName(u.getNickName()); dto.setUserPhoto(u.getUserPhoto());
        dto.setUserSex(u.getUserSex()); dto.setEmail(u.getEmail());
        dto.setPhone(u.getPhone()); dto.setLevel(u.getLevel());
        dto.setIsVip(u.getIsVip()); dto.setStatus(u.getStatus());
        dto.setCreateTime(u.getCreateTime());
        return dto;
    }

    @Transactional
    public void updateUserStatus(Long userId, Integer status) {
        UserInfo u = userInfoMapper.selectById(userId);
        if (u == null) throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        u.setStatus(status); userInfoMapper.updateById(u);
    }

    @Transactional
    public void updateUserVip(Long userId, Integer isVip) {
        UserInfo u = userInfoMapper.selectById(userId);
        if (u == null) throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        u.setIsVip(isVip); userInfoMapper.updateById(u);
    }

    @Transactional
    public void updateUserLevel(Long userId, Integer level) {
        UserInfo u = userInfoMapper.selectById(userId);
        if (u == null) throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        u.setLevel(level); userInfoMapper.updateById(u);
    }

    // ==================== 作者审核 ====================
    public PageRespDto<AuthorInfoRespDto> listAuthors(PageReqDto page, Integer status) {
        LambdaQueryWrapper<AuthorInfo> wrapper = new LambdaQueryWrapper<AuthorInfo>();
        if (status != null) wrapper.eq(AuthorInfo::getStatus, status);
        // 待审核优先展示，其次按申请时间倒序
        wrapper.last("ORDER BY CASE WHEN status = 0 THEN 0 ELSE 1 END, create_time DESC");
        IPage<AuthorInfo> result = authorInfoMapper.selectPage(
            new Page<>(page.getPageNum(), page.getPageSize()), wrapper);

        List<Long> userIds = result.getRecords().stream()
            .map(AuthorInfo::getUserId).filter(Objects::nonNull).distinct()
            .collect(Collectors.toList());
        Map<Long, UserInfo> userMap = userIds.isEmpty() ? Map.of()
            : userInfoMapper.selectBatchIds(userIds).stream()
                .collect(Collectors.toMap(UserInfo::getId, u -> u));

        List<AuthorInfoRespDto> list = result.getRecords().stream().map(a -> {
            AuthorInfoRespDto dto = new AuthorInfoRespDto();
            dto.setId(a.getId()); dto.setUserId(a.getUserId());
            dto.setPenName(a.getPenName()); dto.setIntro(a.getIntro());
            dto.setStatus(a.getStatus()); dto.setReviewComment(a.getReviewComment());
            dto.setStatusText(authorStatusText(a.getStatus()));
            dto.setCreateTime(a.getCreateTime());
            UserInfo u = userMap.get(a.getUserId());
            if (u != null) dto.setUsername(u.getUsername());
            return dto;
        }).collect(Collectors.toList());
        return PageRespDto.of(result.getCurrent(), result.getSize(), result.getTotal(), list);
    }

    /**
     * 审核作者申请：通过则把用户角色升级为 AUTHOR；驳回则记录原因并允许重新申请
     */
    @Transactional
    public void auditAuthor(Long authorId, AuditReqDto dto) {
        AuthorInfo a = authorInfoMapper.selectById(authorId);
        if (a == null) throw new BusinessException(ErrorCode.PARAM_ERROR, "作者申请不存在");
        if (dto.getStatus() != CommonConsts.AuthorStatus.APPROVED
            && dto.getStatus() != CommonConsts.AuthorStatus.REJECTED) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "审核状态只能是 1（通过）或 2（驳回）");
        }

        a.setStatus(dto.getStatus());
        a.setReviewComment(dto.getReviewComment());
        authorInfoMapper.updateById(a);

        if (dto.getStatus() == CommonConsts.AuthorStatus.APPROVED) {
            UserInfo user = userInfoMapper.selectById(a.getUserId());
            if (user != null && !CommonConsts.Role.AUTHOR.equals(user.getRole())) {
                user.setRole(CommonConsts.Role.AUTHOR);
                userInfoMapper.updateById(user);
            }
        }
    }

    private String authorStatusText(Integer status) {
        if (status == null) return "未知";
        return switch (status) {
            case CommonConsts.AuthorStatus.PENDING -> "待审核";
            case CommonConsts.AuthorStatus.APPROVED -> "已通过";
            case CommonConsts.AuthorStatus.REJECTED -> "已驳回";
            default -> "未知";
        };
    }

    // ==================== Banner管理 ====================
    public PageRespDto<BannerRespDto> listBanners(PageReqDto page) {
        IPage<Banner> result = bannerMapper.selectPage(
            new Page<>(page.getPageNum(), page.getPageSize()),
            new LambdaQueryWrapper<Banner>().orderByAsc(Banner::getSort));
        List<BannerRespDto> list = result.getRecords().stream().map(b -> {
            BannerRespDto dto = new BannerRespDto();
            dto.setId(b.getId()); dto.setTitle(b.getTitle()); dto.setImageUrl(b.getImageUrl());
            dto.setBookId(b.getBookId()); dto.setSort(b.getSort()); dto.setStatus(b.getStatus());
            return dto;
        }).collect(Collectors.toList());
        return PageRespDto.of(result.getCurrent(), result.getSize(), result.getTotal(), list);
    }

    public List<BannerRespDto> listActiveBanners() {
        return bannerMapper.selectList(
            new LambdaQueryWrapper<Banner>().eq(Banner::getStatus, CommonConsts.YES).orderByAsc(Banner::getSort))
            .stream().map(b -> {
                BannerRespDto dto = new BannerRespDto();
                dto.setId(b.getId()); dto.setTitle(b.getTitle()); dto.setImageUrl(b.getImageUrl());
                dto.setBookId(b.getBookId()); dto.setSort(b.getSort()); dto.setStatus(b.getStatus());
                return dto;
            }).collect(Collectors.toList());
    }

    @Transactional
    public void addBanner(BannerReqDto dto) {
        Banner b = new Banner();
        b.setTitle(dto.getTitle()); b.setImageUrl(dto.getImageUrl());
        b.setBookId(dto.getBookId()); b.setSort(dto.getSort() != null ? dto.getSort() : 0);
        b.setStatus(dto.getStatus() != null ? dto.getStatus() : CommonConsts.YES);
        b.setCreateTime(LocalDateTime.now()); b.setUpdateTime(LocalDateTime.now());
        bannerMapper.insert(b);
    }

    @Transactional
    public void updateBanner(Long bannerId, BannerReqDto dto) {
        Banner b = bannerMapper.selectById(bannerId);
        if (b == null) throw new BusinessException(ErrorCode.PARAM_ERROR, "Banner不存在");
        if (dto.getTitle() != null) b.setTitle(dto.getTitle());
        if (dto.getImageUrl() != null) b.setImageUrl(dto.getImageUrl());
        if (dto.getBookId() != null) b.setBookId(dto.getBookId());
        if (dto.getSort() != null) b.setSort(dto.getSort());
        if (dto.getStatus() != null) b.setStatus(dto.getStatus());
        b.setUpdateTime(LocalDateTime.now());
        bannerMapper.updateById(b);
    }

    @Transactional
    public void deleteBanner(Long bannerId) {
        bannerMapper.deleteById(bannerId);
    }

    // ==================== 公告管理 ====================
    public PageRespDto<NoticeRespDto> listNotices(PageReqDto page) {
        IPage<Notice> result = noticeMapper.selectPage(
            new Page<>(page.getPageNum(), page.getPageSize()),
            new LambdaQueryWrapper<Notice>().orderByDesc(Notice::getCreateTime));
        List<NoticeRespDto> list = result.getRecords().stream().map(n -> {
            NoticeRespDto dto = new NoticeRespDto();
            dto.setId(n.getId()); dto.setTitle(n.getTitle()); dto.setContent(n.getContent());
            dto.setStatus(n.getStatus()); dto.setCreateTime(n.getCreateTime());
            return dto;
        }).collect(Collectors.toList());
        return PageRespDto.of(result.getCurrent(), result.getSize(), result.getTotal(), list);
    }

    public List<NoticeRespDto> listActiveNotices() {
        return noticeMapper.selectList(
            new LambdaQueryWrapper<Notice>().eq(Notice::getStatus, CommonConsts.YES).orderByDesc(Notice::getCreateTime))
            .stream().map(n -> {
                NoticeRespDto dto = new NoticeRespDto();
                dto.setId(n.getId()); dto.setTitle(n.getTitle()); dto.setContent(n.getContent());
                dto.setStatus(n.getStatus()); dto.setCreateTime(n.getCreateTime());
                return dto;
            }).collect(Collectors.toList());
    }

    @Transactional
    public void addNotice(NoticeReqDto dto) {
        Notice n = new Notice();
        n.setTitle(dto.getTitle()); n.setContent(dto.getContent());
        n.setStatus(dto.getStatus() != null ? dto.getStatus() : CommonConsts.YES);
        n.setCreateTime(LocalDateTime.now()); n.setUpdateTime(LocalDateTime.now());
        noticeMapper.insert(n);
    }

    @Transactional
    public void updateNotice(Long noticeId, NoticeReqDto dto) {
        Notice n = noticeMapper.selectById(noticeId);
        if (n == null) throw new BusinessException(ErrorCode.PARAM_ERROR, "公告不存在");
        if (dto.getTitle() != null) n.setTitle(dto.getTitle());
        if (dto.getContent() != null) n.setContent(dto.getContent());
        if (dto.getStatus() != null) n.setStatus(dto.getStatus());
        n.setUpdateTime(LocalDateTime.now());
        noticeMapper.updateById(n);
    }

    @Transactional
    public void deleteNotice(Long noticeId) {
        noticeMapper.deleteById(noticeId);
    }

    // ==================== 标签管理 ====================
    public List<TagRespDto> listTags() {
        return tagMapper.selectList(new LambdaQueryWrapper<Tag>().orderByAsc(Tag::getSort))
            .stream().map(t -> { TagRespDto d = new TagRespDto(); d.setId(t.getId()); d.setName(t.getName()); d.setSort(t.getSort()); return d; })
            .collect(Collectors.toList());
    }

    @Transactional
    public void addTag(TagReqDto dto) {
        Tag t = new Tag();
        t.setName(dto.getName()); t.setSort(dto.getSort() != null ? dto.getSort() : 0);
        t.setCreateTime(LocalDateTime.now()); t.setUpdateTime(LocalDateTime.now());
        tagMapper.insert(t);
    }

    @Transactional
    public void updateTag(Long tagId, TagReqDto dto) {
        Tag t = tagMapper.selectById(tagId);
        if (t == null) throw new BusinessException(ErrorCode.PARAM_ERROR, "标签不存在");
        if (dto.getName() != null) t.setName(dto.getName());
        if (dto.getSort() != null) t.setSort(dto.getSort());
        t.setUpdateTime(LocalDateTime.now());
        tagMapper.updateById(t);
    }

    @Transactional
    public void deleteTag(Long tagId) { tagMapper.deleteById(tagId); }

    // ==================== 分类管理 ====================
    public List<CategoryRespDto> listCategories() {
        return bookCategoryMapper.selectList(new LambdaQueryWrapper<BookCategory>().orderByAsc(BookCategory::getSort))
            .stream().map(c -> { CategoryRespDto d = new CategoryRespDto(); d.setId(c.getId()); d.setName(c.getName()); d.setWorkDirection(c.getWorkDirection()); d.setSort(c.getSort()); return d; })
            .collect(Collectors.toList());
    }

    @Transactional
    public void addCategory(CategoryReqDto dto) {
        BookCategory c = new BookCategory();
        c.setName(dto.getName()); c.setWorkDirection(dto.getWorkDirection());
        c.setSort(dto.getSort() != null ? dto.getSort() : 10);
        c.setCreateTime(LocalDateTime.now()); c.setUpdateTime(LocalDateTime.now());
        bookCategoryMapper.insert(c);
    }

    @Transactional
    public void updateCategory(Long categoryId, CategoryReqDto dto) {
        BookCategory c = bookCategoryMapper.selectById(categoryId);
        if (c == null) throw new BusinessException(ErrorCode.PARAM_ERROR, "分类不存在");
        if (dto.getName() != null) c.setName(dto.getName());
        if (dto.getWorkDirection() != null) c.setWorkDirection(dto.getWorkDirection());
        if (dto.getSort() != null) c.setSort(dto.getSort());
        c.setUpdateTime(LocalDateTime.now());
        bookCategoryMapper.updateById(c);
    }

    @Transactional
    public void deleteCategory(Long categoryId) { bookCategoryMapper.deleteById(categoryId); }

    // ==================== 留言板管理 ====================
    public PageRespDto<MessageBoardRespDto> listMessageBoard(PageReqDto page) {
        IPage<MessageBoard> result = messageBoardMapper.selectPage(
            new Page<>(page.getPageNum(), page.getPageSize()),
            new LambdaQueryWrapper<MessageBoard>().orderByDesc(MessageBoard::getCreateTime));
        List<MessageBoardRespDto> list = result.getRecords().stream().map(m -> {
            MessageBoardRespDto dto = new MessageBoardRespDto();
            dto.setId(m.getId()); dto.setUserId(m.getUserId());
            dto.setContent(m.getContent()); dto.setReply(m.getReply());
            dto.setStatus(m.getStatus()); dto.setCreateTime(m.getCreateTime());
            UserInfo u = userInfoMapper.selectById(m.getUserId());
            if (u != null) { dto.setUsername(u.getUsername()); dto.setNickName(u.getNickName()); dto.setUserPhoto(u.getUserPhoto()); }
            return dto;
        }).collect(Collectors.toList());
        return PageRespDto.of(result.getCurrent(), result.getSize(), result.getTotal(), list);
    }

    @Transactional
    public void replyMessageBoard(Long messageId, ReplyOfficialReqDto dto) {
        MessageBoard m = messageBoardMapper.selectById(messageId);
        if (m == null) throw new BusinessException(ErrorCode.PARAM_ERROR, "留言不存在");
        m.setReply(dto.getReply()); m.setStatus(1);
        m.setUpdateTime(LocalDateTime.now());
        messageBoardMapper.updateById(m);
    }

    @Transactional
    public void deleteMessageBoard(Long messageId) { messageBoardMapper.deleteById(messageId); }

    // ==================== 评论管理 ====================
    public PageRespDto<CommentRespDto> listComments(PageReqDto page, Integer auditStatus) {
        LambdaQueryWrapper<BookComment> wrapper = new LambdaQueryWrapper<BookComment>();
        if (auditStatus != null) wrapper.eq(BookComment::getAuditStatus, auditStatus);
        wrapper.orderByDesc(BookComment::getCreateTime);
        IPage<BookComment> result = bookCommentMapper.selectPage(
            new Page<>(page.getPageNum(), page.getPageSize()), wrapper);
        List<CommentRespDto> list = result.getRecords().stream().map(c -> {
            CommentRespDto dto = new CommentRespDto();
            dto.setId(c.getId()); dto.setBookId(c.getBookId()); dto.setChapterId(c.getChapterId());
            dto.setUserId(c.getUserId()); dto.setContent(c.getContent());
            dto.setReplyCount(c.getReplyCount()); dto.setAuditStatus(c.getAuditStatus());
            dto.setCreateTime(c.getCreateTime());
            UserInfo u = userInfoMapper.selectById(c.getUserId());
            if (u != null) { dto.setUsername(u.getUsername()); dto.setNickName(u.getNickName()); dto.setUserPhoto(u.getUserPhoto()); }
            return dto;
        }).collect(Collectors.toList());
        return PageRespDto.of(result.getCurrent(), result.getSize(), result.getTotal(), list);
    }

    @Transactional
    public void auditComment(Long commentId, AuditReqDto dto) {
        BookComment c = bookCommentMapper.selectById(commentId);
        if (c == null) throw new BusinessException(ErrorCode.PARAM_ERROR, "评论不存在");
        c.setAuditStatus(dto.getStatus());
        bookCommentMapper.updateById(c);
    }

    @Transactional
    public void deleteComment(Long commentId) { bookCommentMapper.deleteById(commentId); }

    // ==================== 社区管理 ====================
    public PageRespDto<CommunityPostRespDto> listCommunityPosts(PageReqDto page) {
        IPage<CommunityPost> result = communityPostMapper.selectPage(
            new Page<>(page.getPageNum(), page.getPageSize()),
            new LambdaQueryWrapper<CommunityPost>().orderByDesc(CommunityPost::getCreateTime));
        List<CommunityPostRespDto> list = result.getRecords().stream().map(p -> {
            CommunityPostRespDto dto = new CommunityPostRespDto();
            dto.setId(p.getId()); dto.setUserId(p.getUserId()); dto.setTitle(p.getTitle());
            dto.setContent(p.getContent()); dto.setLikeCount(p.getLikeCount());
            dto.setReplyCount(p.getReplyCount()); dto.setStatus(p.getStatus());
            dto.setCreateTime(p.getCreateTime());
            UserInfo u = userInfoMapper.selectById(p.getUserId());
            if (u != null) { dto.setUsername(u.getUsername()); dto.setNickName(u.getNickName()); dto.setUserPhoto(u.getUserPhoto()); }
            return dto;
        }).collect(Collectors.toList());
        return PageRespDto.of(result.getCurrent(), result.getSize(), result.getTotal(), list);
    }

    @Transactional
    public void deleteCommunityPost(Long postId) { communityPostMapper.deleteById(postId); }

    @Transactional
    public void deleteCommunityReply(Long replyId) { communityReplyMapper.deleteById(replyId); }

    // ==================== 反馈管理 ====================
    public void addFeedback(FeedbackReqDto dto) {
        Long userId = UserContext.userId();
        UserFeedback f = new UserFeedback();
        f.setUserId(userId); f.setContent(dto.getContent());
        f.setStatus(0); f.setCreateTime(LocalDateTime.now()); f.setUpdateTime(LocalDateTime.now());
        userFeedbackMapper.insert(f);
    }

    // ==================== 小说管理（管理员侧）====================
    public PageRespDto<BookInfoRespDto> listAllBooks(PageReqDto page, String keyword, Integer auditStatus) {
        LambdaQueryWrapper<BookInfo> wrapper = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.like(BookInfo::getBookName, keyword);
        }
        if (auditStatus != null) {
            wrapper.eq(BookInfo::getAuditStatus, auditStatus);
        }
        wrapper.orderByDesc(BookInfo::getUpdateTime);
        IPage<BookInfo> result = bookInfoMapper.selectPage(
            new Page<>(page.getPageNum(), page.getPageSize()), wrapper);
        List<BookInfoRespDto> list = result.getRecords().stream().map(b -> {
            BookInfoRespDto dto = new BookInfoRespDto();
            dto.setId(b.getId()); dto.setBookName(b.getBookName());
            dto.setAuthorId(b.getAuthorId()); dto.setAuthorName(b.getAuthorName());
            dto.setCategoryId(b.getCategoryId()); dto.setCategoryName(b.getCategoryName());
            dto.setPicUrl(b.getPicUrl()); dto.setBookDesc(b.getBookDesc());
            dto.setScore(b.getScore()); dto.setBookStatus(b.getBookStatus());
            dto.setVisitCount(b.getVisitCount()); dto.setWordCount(b.getWordCount());
            dto.setCommentCount(b.getCommentCount()); dto.setIsVip(b.getIsVip());
            dto.setAuditStatus(b.getAuditStatus()); dto.setCreateTime(b.getCreateTime());
            return dto;
        }).collect(Collectors.toList());
        return PageRespDto.of(result.getCurrent(), result.getSize(), result.getTotal(), list);
    }

    @Transactional
    public void updateBookStatus(Long bookId, Integer auditStatus) {
        BookInfo b = bookInfoMapper.selectById(bookId);
        if (b == null) throw new BusinessException(ErrorCode.BOOK_NOT_FOUND);
        b.setAuditStatus(auditStatus); b.setUpdateTime(LocalDateTime.now());
        bookInfoMapper.updateById(b);
    }

    @Transactional
    public void deleteBook(Long bookId) {
        bookInfoMapper.deleteById(bookId);
    }

    // ==================== 留言板（用户侧）====================
    public void addMessageBoard(MessageBoardAddReqDto dto) {
        Long userId = UserContext.userId();
        MessageBoard m = new MessageBoard();
        m.setUserId(userId); m.setContent(dto.getContent());
        m.setStatus(0); m.setCreateTime(LocalDateTime.now()); m.setUpdateTime(LocalDateTime.now());
        messageBoardMapper.insert(m);
    }

    public PageRespDto<MessageBoardRespDto> listMessageBoardPublic(PageReqDto page) {
        return listMessageBoard(page);
    }
}
