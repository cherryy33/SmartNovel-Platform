package com.myproject.novel.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.myproject.novel.common.constant.CommonConsts;
import com.myproject.novel.common.exception.BusinessException;
import com.myproject.novel.common.exception.ErrorCode;
import com.myproject.novel.common.req.PageReqDto;
import com.myproject.novel.common.result.PageRespDto;
import com.myproject.novel.dao.entity.BookChapter;
import com.myproject.novel.dao.entity.BookComment;
import com.myproject.novel.dao.entity.CommentReply;
import com.myproject.novel.dao.entity.UserInfo;
import com.myproject.novel.dao.mapper.BookChapterMapper;
import com.myproject.novel.dao.mapper.BookCommentMapper;
import com.myproject.novel.dao.mapper.CommentReplyMapper;
import com.myproject.novel.dao.mapper.UserInfoMapper;
import com.myproject.novel.dto.req.CommentAddReqDto;
import com.myproject.novel.dto.req.ReplyAddReqDto;
import com.myproject.novel.dto.resp.CommentRespDto;
import com.myproject.novel.dto.resp.ReplyRespDto;
import com.myproject.novel.security.UserContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 评论服务
 */
@Service
@RequiredArgsConstructor
public class CommentService {

    private final BookCommentMapper bookCommentMapper;
    private final CommentReplyMapper commentReplyMapper;
    private final UserInfoMapper userInfoMapper;
    private final BookChapterMapper bookChapterMapper;

    /**
     * 分页查询评论列表（带用户信息）
     */
    public PageRespDto<CommentRespDto> listComments(Long bookId, Long chapterId, PageReqDto page) {
        LambdaQueryWrapper<BookComment> wrapper = new LambdaQueryWrapper<BookComment>()
                .eq(BookComment::getBookId, bookId)
                .eq(chapterId != null, BookComment::getChapterId, chapterId)
                .isNull(chapterId == null, BookComment::getChapterId)
                .orderByDesc(BookComment::getCreateTime);

        IPage<BookComment> resultPage = bookCommentMapper.selectPage(
                new Page<>(page.getPageNum(), page.getPageSize()), wrapper);

        List<CommentRespDto> list = resultPage.getRecords().stream().map(comment -> {
            CommentRespDto dto = new CommentRespDto();
            dto.setId(comment.getId());
            dto.setBookId(comment.getBookId());
            dto.setChapterId(comment.getChapterId());
            dto.setUserId(comment.getUserId());
            dto.setContent(comment.getContent());
            dto.setReplyCount(comment.getReplyCount());
            dto.setAuditStatus(comment.getAuditStatus());
            dto.setCreateTime(comment.getCreateTime());

            // 填充章节名
            if (comment.getChapterId() != null) {
                BookChapter chapter = bookChapterMapper.selectById(comment.getChapterId());
                if (chapter != null) {
                    dto.setChapterName(chapter.getChapterName());
                }
            }

            // 填充用户信息
            UserInfo user = userInfoMapper.selectById(comment.getUserId());
            if (user != null) {
                dto.setUsername(user.getUsername());
                dto.setNickName(user.getNickName());
                dto.setUserPhoto(user.getUserPhoto());
            }
            return dto;
        }).collect(Collectors.toList());

        return PageRespDto.of(resultPage.getCurrent(), resultPage.getSize(),
                resultPage.getTotal(), list);
    }

    /**
     * 添加书评或章节评论
     */
    public void addComment(Long bookId, CommentAddReqDto dto) {
        Long userId = UserContext.userId();

        BookComment comment = new BookComment();
        comment.setBookId(bookId);
        comment.setChapterId(dto.getChapterId());
        comment.setUserId(userId);
        comment.setContent(dto.getContent());
        comment.setReplyCount(0);
        comment.setAuditStatus(CommonConsts.YES);
        comment.setCreateTime(LocalDateTime.now());
        comment.setUpdateTime(LocalDateTime.now());
        bookCommentMapper.insert(comment);
    }

    /**
     * 删除评论（仅作者或管理员）
     */
    public void deleteComment(Long commentId) {
        BookComment comment = bookCommentMapper.selectById(commentId);
        if (comment == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "评论不存在");
        }

        Long currentUserId = UserContext.userId();
        String currentRole = UserContext.require().getRole();
        if (!comment.getUserId().equals(currentUserId) && !CommonConsts.Role.ADMIN.equals(currentRole)) {
            throw new BusinessException(ErrorCode.USER_NO_PERMISSION);
        }

        bookCommentMapper.deleteById(commentId);
    }

    /**
     * 分页查询评论回复列表（带用户信息）
     */
    public PageRespDto<ReplyRespDto> listReplies(Long commentId, PageReqDto page) {
        LambdaQueryWrapper<CommentReply> wrapper = new LambdaQueryWrapper<CommentReply>()
                .eq(CommentReply::getCommentId, commentId)
                .orderByAsc(CommentReply::getCreateTime);

        IPage<CommentReply> resultPage = commentReplyMapper.selectPage(
                new Page<>(page.getPageNum(), page.getPageSize()), wrapper);

        // 批量加载用户信息
        List<Long> userIds = resultPage.getRecords().stream()
                .map(CommentReply::getUserId)
                .distinct()
                .collect(Collectors.toList());
        Map<Long, UserInfo> userMap = userIds.isEmpty() ? Map.of() :
                userInfoMapper.selectBatchIds(userIds).stream()
                        .collect(Collectors.toMap(UserInfo::getId, u -> u));

        List<ReplyRespDto> list = resultPage.getRecords().stream().map(reply -> {
            ReplyRespDto dto = new ReplyRespDto();
            dto.setId(reply.getId());
            dto.setCommentId(reply.getCommentId());
            dto.setUserId(reply.getUserId());
            dto.setReplyContent(reply.getReplyContent());
            dto.setAuditStatus(reply.getAuditStatus());
            dto.setCreateTime(reply.getCreateTime());

            UserInfo user = userMap.get(reply.getUserId());
            if (user != null) {
                dto.setUsername(user.getUsername());
                dto.setNickName(user.getNickName());
                dto.setUserPhoto(user.getUserPhoto());
            }
            return dto;
        }).collect(Collectors.toList());

        return PageRespDto.of(resultPage.getCurrent(), resultPage.getSize(),
                resultPage.getTotal(), list);
    }

    /**
     * 添加回复，并自增评论的回复数
     */
    public void addReply(Long commentId, ReplyAddReqDto dto) {
        Long userId = UserContext.userId();

        // 校验评论存在
        BookComment comment = bookCommentMapper.selectById(commentId);
        if (comment == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "评论不存在");
        }

        CommentReply reply = new CommentReply();
        reply.setCommentId(commentId);
        reply.setUserId(userId);
        reply.setReplyContent(dto.getReplyContent());
        reply.setAuditStatus(CommonConsts.YES);
        reply.setCreateTime(LocalDateTime.now());
        reply.setUpdateTime(LocalDateTime.now());
        commentReplyMapper.insert(reply);

        // 自增评论回复数
        comment.setReplyCount(comment.getReplyCount() == null ? 1 : comment.getReplyCount() + 1);
        bookCommentMapper.updateById(comment);
    }

    /**
     * 删除回复（仅回复作者或管理员）
     */
    public void deleteReply(Long replyId) {
        CommentReply reply = commentReplyMapper.selectById(replyId);
        if (reply == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "回复不存在");
        }

        Long currentUserId = UserContext.userId();
        String currentRole = UserContext.require().getRole();
        if (!reply.getUserId().equals(currentUserId) && !CommonConsts.Role.ADMIN.equals(currentRole)) {
            throw new BusinessException(ErrorCode.USER_NO_PERMISSION);
        }

        commentReplyMapper.deleteById(replyId);
    }
}