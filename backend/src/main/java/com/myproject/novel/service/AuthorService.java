package com.myproject.novel.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.myproject.novel.common.constant.CommonConsts;
import com.myproject.novel.common.exception.BusinessException;
import com.myproject.novel.common.exception.ErrorCode;
import com.myproject.novel.dao.entity.AuthorInfo;
import com.myproject.novel.dao.mapper.AuthorInfoMapper;
import com.myproject.novel.dto.req.AuthorApplyReqDto;
import com.myproject.novel.dto.resp.AuthorInfoRespDto;
import com.myproject.novel.security.UserContext;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 作者服务：入驻申请 / 申请状态 / 信息修改
 */
@Service
@RequiredArgsConstructor
public class AuthorService {

    private final AuthorInfoMapper authorInfoMapper;

    // ======================== 入驻申请 ========================

    /**
     * 提交作者入驻申请：
     * - 已有「待审核」记录 → 拦截
     * - 已「通过」→ 拦截（已是作家）
     * - 已「驳回」→ 复用原记录重新提交，状态回到待审核
     */
    @Transactional
    public void apply(AuthorApplyReqDto dto) {
        Long userId = UserContext.userId();

        AuthorInfo existing = authorInfoMapper.selectOne(
                new LambdaQueryWrapper<AuthorInfo>()
                        .eq(AuthorInfo::getUserId, userId));

        if (existing != null) {
            if (existing.getStatus() == CommonConsts.AuthorStatus.PENDING) {
                throw new BusinessException(ErrorCode.AUTHOR_APPLY_PENDING);
            }
            if (existing.getStatus() == CommonConsts.AuthorStatus.APPROVED) {
                throw new BusinessException(ErrorCode.AUTHOR_ALREADY_EXISTS);
            }
            // 被驳回：允许重新申请，复用原记录
            existing.setPenName(dto.getPenName());
            existing.setIntro(dto.getIntro());
            existing.setStatus(CommonConsts.AuthorStatus.PENDING);
            existing.setReviewComment(null);
            authorInfoMapper.updateById(existing);
            return;
        }

        AuthorInfo authorInfo = new AuthorInfo();
        authorInfo.setUserId(userId);
        authorInfo.setPenName(dto.getPenName());
        authorInfo.setIntro(dto.getIntro());
        authorInfo.setStatus(CommonConsts.AuthorStatus.PENDING);
        authorInfoMapper.insert(authorInfo);
    }

    // ======================== 申请状态 ========================

    /**
     * 获取当前用户的作者申请状态（未提交过返回 null，不抛异常）
     */
    public AuthorInfoRespDto getMyAuthorInfo() {
        Long userId = UserContext.userId();

        AuthorInfo authorInfo = authorInfoMapper.selectOne(
                new LambdaQueryWrapper<AuthorInfo>()
                        .eq(AuthorInfo::getUserId, userId));
        if (authorInfo == null) {
            return null;
        }

        AuthorInfoRespDto resp = new AuthorInfoRespDto();
        BeanUtils.copyProperties(authorInfo, resp);
        resp.setStatusText(statusText(authorInfo.getStatus()));
        return resp;
    }

    private String statusText(Integer status) {
        if (status == null) return "未提交";
        return switch (status) {
            case CommonConsts.AuthorStatus.PENDING -> "待审核";
            case CommonConsts.AuthorStatus.APPROVED -> "已通过";
            case CommonConsts.AuthorStatus.REJECTED -> "已驳回";
            default -> "未知";
        };
    }

    // ======================== 信息修改 ========================

    /**
     * 更新笔名和简介（仅已通过的作者可修改）
     */
    @Transactional
    public void updateAuthorInfo(AuthorApplyReqDto dto) {
        Long userId = UserContext.userId();

        AuthorInfo authorInfo = authorInfoMapper.selectOne(
                new LambdaQueryWrapper<AuthorInfo>()
                        .eq(AuthorInfo::getUserId, userId));
        if (authorInfo == null) {
            throw new BusinessException(ErrorCode.AUTHOR_NOT_APPROVED, "您尚未提交作者认证");
        }
        if (authorInfo.getStatus() != CommonConsts.AuthorStatus.APPROVED) {
            throw new BusinessException(ErrorCode.AUTHOR_NOT_APPROVED);
        }

        if (dto.getPenName() != null) {
            authorInfo.setPenName(dto.getPenName());
        }
        if (dto.getIntro() != null) {
            authorInfo.setIntro(dto.getIntro());
        }
        authorInfoMapper.updateById(authorInfo);
    }
}
