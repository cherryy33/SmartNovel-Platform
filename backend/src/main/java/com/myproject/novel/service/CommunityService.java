package com.myproject.novel.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.myproject.novel.common.constant.CommonConsts;
import com.myproject.novel.common.exception.BusinessException;
import com.myproject.novel.common.exception.ErrorCode;
import com.myproject.novel.common.req.PageReqDto;
import com.myproject.novel.common.result.PageRespDto;
import com.myproject.novel.dao.entity.CommunityPost;
import com.myproject.novel.dao.entity.CommunityReply;
import com.myproject.novel.dao.entity.UserInfo;
import com.myproject.novel.dao.mapper.CommunityPostMapper;
import com.myproject.novel.dao.mapper.CommunityReplyMapper;
import com.myproject.novel.dao.mapper.UserInfoMapper;
import com.myproject.novel.dto.req.PostAddReqDto;
import com.myproject.novel.dto.req.ReplyAddReqDto;
import com.myproject.novel.dto.resp.CommunityPostRespDto;
import com.myproject.novel.dto.resp.CommunityReplyRespDto;
import com.myproject.novel.security.UserContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 社区服务
 */
@Service
@RequiredArgsConstructor
public class CommunityService {

    private final CommunityPostMapper communityPostMapper;
    private final CommunityReplyMapper communityReplyMapper;
    private final UserInfoMapper userInfoMapper;

    /**
     * 分页查询帖子列表（仅 status=1 的已发布帖子，带用户信息）
     */
    public PageRespDto<CommunityPostRespDto> listPosts(PageReqDto page) {
        LambdaQueryWrapper<CommunityPost> wrapper = new LambdaQueryWrapper<CommunityPost>()
                .eq(CommunityPost::getStatus, CommonConsts.YES)
                .orderByDesc(CommunityPost::getCreateTime);

        IPage<CommunityPost> resultPage = communityPostMapper.selectPage(
                new Page<>(page.getPageNum(), page.getPageSize()), wrapper);

        List<Long> userIds = resultPage.getRecords().stream()
                .map(CommunityPost::getUserId)
                .distinct()
                .collect(Collectors.toList());
        Map<Long, UserInfo> userMap = userIds.isEmpty() ? Map.of() :
                userInfoMapper.selectBatchIds(userIds).stream()
                        .collect(Collectors.toMap(UserInfo::getId, u -> u));

        List<CommunityPostRespDto> list = resultPage.getRecords().stream().map(post -> {
            CommunityPostRespDto dto = new CommunityPostRespDto();
            dto.setId(post.getId());
            dto.setUserId(post.getUserId());
            dto.setTitle(post.getTitle());
            dto.setContent(post.getContent());
            dto.setLikeCount(post.getLikeCount());
            dto.setReplyCount(post.getReplyCount());
            dto.setStatus(post.getStatus());
            dto.setCreateTime(post.getCreateTime());

            UserInfo user = userMap.get(post.getUserId());
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
     * 查看帖子详情（带用户信息）
     */
    public CommunityPostRespDto getPostDetail(Long postId) {
        CommunityPost post = communityPostMapper.selectById(postId);
        if (post == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "帖子不存在");
        }

        CommunityPostRespDto dto = new CommunityPostRespDto();
        dto.setId(post.getId());
        dto.setUserId(post.getUserId());
        dto.setTitle(post.getTitle());
        dto.setContent(post.getContent());
        dto.setLikeCount(post.getLikeCount());
        dto.setReplyCount(post.getReplyCount());
        dto.setStatus(post.getStatus());
        dto.setCreateTime(post.getCreateTime());

        UserInfo user = userInfoMapper.selectById(post.getUserId());
        if (user != null) {
            dto.setUsername(user.getUsername());
            dto.setNickName(user.getNickName());
            dto.setUserPhoto(user.getUserPhoto());
        }
        return dto;
    }

    /**
     * 发布新帖子
     */
    public void addPost(PostAddReqDto dto) {
        Long userId = UserContext.userId();

        CommunityPost post = new CommunityPost();
        post.setUserId(userId);
        post.setTitle(dto.getTitle());
        post.setContent(dto.getContent());
        post.setLikeCount(0);
        post.setReplyCount(0);
        post.setStatus(CommonConsts.YES);
        post.setCreateTime(LocalDateTime.now());
        post.setUpdateTime(LocalDateTime.now());
        communityPostMapper.insert(post);
    }

    /**
     * 删除帖子（仅作者或管理员）
     */
    public void deletePost(Long postId) {
        CommunityPost post = communityPostMapper.selectById(postId);
        if (post == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "帖子不存在");
        }

        Long currentUserId = UserContext.userId();
        String currentRole = UserContext.require().getRole();
        if (!post.getUserId().equals(currentUserId) && !CommonConsts.Role.ADMIN.equals(currentRole)) {
            throw new BusinessException(ErrorCode.USER_NO_PERMISSION);
        }

        communityPostMapper.deleteById(postId);
    }

    /**
     * 分页查询帖子回复列表（带用户信息）
     */
    public PageRespDto<CommunityReplyRespDto> listPostReplies(Long postId, PageReqDto page) {
        LambdaQueryWrapper<CommunityReply> wrapper = new LambdaQueryWrapper<CommunityReply>()
                .eq(CommunityReply::getPostId, postId)
                .orderByAsc(CommunityReply::getCreateTime);

        IPage<CommunityReply> resultPage = communityReplyMapper.selectPage(
                new Page<>(page.getPageNum(), page.getPageSize()), wrapper);

        List<Long> userIds = resultPage.getRecords().stream()
                .map(CommunityReply::getUserId)
                .distinct()
                .collect(Collectors.toList());
        Map<Long, UserInfo> userMap = userIds.isEmpty() ? Map.of() :
                userInfoMapper.selectBatchIds(userIds).stream()
                        .collect(Collectors.toMap(UserInfo::getId, u -> u));

        List<CommunityReplyRespDto> list = resultPage.getRecords().stream().map(reply -> {
            CommunityReplyRespDto dto = new CommunityReplyRespDto();
            dto.setId(reply.getId());
            dto.setPostId(reply.getPostId());
            dto.setUserId(reply.getUserId());
            dto.setReplyContent(reply.getReplyContent());
            dto.setStatus(reply.getStatus());
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
     * 添加帖子回复，并自增帖子的回复数
     */
    public void addPostReply(Long postId, ReplyAddReqDto dto) {
        Long userId = UserContext.userId();

        CommunityPost post = communityPostMapper.selectById(postId);
        if (post == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "帖子不存在");
        }

        CommunityReply reply = new CommunityReply();
        reply.setPostId(postId);
        reply.setUserId(userId);
        reply.setReplyContent(dto.getReplyContent());
        reply.setStatus(CommonConsts.YES);
        reply.setCreateTime(LocalDateTime.now());
        reply.setUpdateTime(LocalDateTime.now());
        communityReplyMapper.insert(reply);

        // 自增帖子回复数
        post.setReplyCount(post.getReplyCount() == null ? 1 : post.getReplyCount() + 1);
        communityPostMapper.updateById(post);
    }

    /**
     * 删除帖子回复（仅回复作者或管理员）
     */
    public void deletePostReply(Long replyId) {
        CommunityReply reply = communityReplyMapper.selectById(replyId);
        if (reply == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "回复不存在");
        }

        Long currentUserId = UserContext.userId();
        String currentRole = UserContext.require().getRole();
        if (!reply.getUserId().equals(currentUserId) && !CommonConsts.Role.ADMIN.equals(currentRole)) {
            throw new BusinessException(ErrorCode.USER_NO_PERMISSION);
        }

        communityReplyMapper.deleteById(replyId);
    }
}