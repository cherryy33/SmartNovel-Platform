package com.myproject.novel.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.myproject.novel.common.constant.CommonConsts;
import com.myproject.novel.common.exception.BusinessException;
import com.myproject.novel.common.exception.ErrorCode;
import com.myproject.novel.dao.entity.SysUser;
import com.myproject.novel.dao.entity.Tag;
import com.myproject.novel.dao.entity.UserInfo;
import com.myproject.novel.dao.entity.UserTag;
import com.myproject.novel.dao.mapper.SysUserMapper;
import com.myproject.novel.dao.mapper.TagMapper;
import com.myproject.novel.dao.mapper.UserInfoMapper;
import com.myproject.novel.dao.mapper.UserTagMapper;
import com.myproject.novel.dto.req.UpdatePasswordReqDto;
import com.myproject.novel.dto.req.UpdateUserReqDto;
import com.myproject.novel.dto.resp.TagRespDto;
import com.myproject.novel.dto.resp.UserInfoRespDto;
import com.myproject.novel.security.LoginUser;
import com.myproject.novel.security.UserContext;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 用户服务：个人资料 / 密码修改 / VIP / 兴趣标签
 */
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserInfoMapper userInfoMapper;
    private final SysUserMapper sysUserMapper;
    private final UserTagMapper userTagMapper;
    private final TagMapper tagMapper;
    private final PasswordEncoder passwordEncoder;

    // ======================== 个人资料 ========================

    /**
     * 获取当前登录用户的完整资料（含真实角色）
     */
    public UserInfoRespDto getProfile() {
        LoginUser loginUser = UserContext.require();
        UserInfoRespDto resp = new UserInfoRespDto();

        // 管理员使用 sys_user 表
        if (CommonConsts.Role.ADMIN.equals(loginUser.getRole())) {
            SysUser sysUser = sysUserMapper.selectById(loginUser.getUserId());
            if (sysUser == null) {
                throw new BusinessException(ErrorCode.USER_NOT_FOUND);
            }
            resp.setId(sysUser.getId());
            resp.setUsername(sysUser.getUsername());
            resp.setNickName(sysUser.getName());
            resp.setUserPhoto(sysUser.getAvatar());
            resp.setStatus(sysUser.getStatus());
            resp.setCreateTime(sysUser.getCreateTime());
            resp.setRole(CommonConsts.Role.ADMIN);
            return resp;
        }

        UserInfo userInfo = userInfoMapper.selectById(loginUser.getUserId());
        if (userInfo == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        BeanUtils.copyProperties(userInfo, resp);
        // 以 JWT 中解析出的角色为准（作者审核通过后 token 可能尚未刷新，这里以 author_info 校验为准）
        if (CommonConsts.Role.AUTHOR.equals(loginUser.getRole())) {
            resp.setRole(CommonConsts.Role.AUTHOR);
        } else {
            resp.setRole(CommonConsts.Role.normalize(userInfo.getRole()));
        }
        return resp;
    }

    /**
     * 修改个人资料（nickName / email / phone / userPhoto / userSex）
     */
    @Transactional
    public void updateProfile(UpdateUserReqDto dto) {
        Long userId = UserContext.userId();
        UserInfo userInfo = userInfoMapper.selectById(userId);
        if (userInfo == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }

        if (dto.getNickName() != null) {
            userInfo.setNickName(dto.getNickName());
        }
        if (dto.getEmail() != null) {
            userInfo.setEmail(dto.getEmail());
        }
        if (dto.getPhone() != null) {
            userInfo.setPhone(dto.getPhone());
        }
        if (dto.getUserPhoto() != null) {
            userInfo.setUserPhoto(dto.getUserPhoto());
        }
        if (dto.getUserSex() != null) {
            userInfo.setUserSex(dto.getUserSex());
        }

        userInfoMapper.updateById(userInfo);
    }

    // ======================== 密码 ========================

    /**
     * 修改密码：先验证旧密码，再更新为新密码
     */
    @Transactional
    public void updatePassword(UpdatePasswordReqDto dto) {
        Long userId = UserContext.userId();
        UserInfo userInfo = userInfoMapper.selectById(userId);
        if (userInfo == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }

        if (!passwordEncoder.matches(dto.getOldPassword(), userInfo.getPassword())) {
            throw new BusinessException(ErrorCode.USER_ACCOUNT_OR_PASSWORD_ERROR, "旧密码错误");
        }

        userInfo.setPassword(passwordEncoder.encode(dto.getNewPassword()));
        userInfoMapper.updateById(userInfo);
    }

    // ======================== VIP ========================

    /**
     * 升级 VIP（模拟，直接设置 isVip = 1）
     */
    @Transactional
    public void upgradeVip() {
        Long userId = UserContext.userId();
        UserInfo userInfo = userInfoMapper.selectById(userId);
        if (userInfo == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        userInfo.setIsVip(CommonConsts.YES);
        userInfoMapper.updateById(userInfo);
    }

    // ======================== 兴趣标签 ========================

    /**
     * 保存用户兴趣标签（全量替换）
     */
    @Transactional
    public void selectTags(List<Long> tagIds) {
        Long userId = UserContext.userId();

        // 删除旧标签
        userTagMapper.delete(
                new LambdaQueryWrapper<UserTag>()
                        .eq(UserTag::getUserId, userId));

        // 插入新标签
        if (tagIds != null && !tagIds.isEmpty()) {
            for (Long tagId : tagIds) {
                UserTag userTag = new UserTag();
                userTag.setUserId(userId);
                userTag.setTagId(tagId);
                userTagMapper.insert(userTag);
            }
        }
    }

    /**
     * 获取当前用户已选的兴趣标签
     */
    public List<TagRespDto> getUserTags() {
        Long userId = UserContext.userId();

        // 查询用户关联的标签 ID
        List<Long> tagIds = userTagMapper.selectList(
                        new LambdaQueryWrapper<UserTag>()
                                .eq(UserTag::getUserId, userId)
                                .select(UserTag::getTagId))
                .stream()
                .map(UserTag::getTagId)
                .collect(Collectors.toList());

        if (tagIds.isEmpty()) {
            return List.of();
        }

        // 查询标签详情
        return tagMapper.selectList(
                        new LambdaQueryWrapper<Tag>()
                                .in(Tag::getId, tagIds)
                                .orderByAsc(Tag::getSort))
                .stream()
                .map(tag -> {
                    TagRespDto dto = new TagRespDto();
                    dto.setId(tag.getId());
                    dto.setName(tag.getName());
                    dto.setSort(tag.getSort());
                    return dto;
                })
                .collect(Collectors.toList());
    }
}
