package com.myproject.novel.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.myproject.novel.common.constant.CommonConsts;
import com.myproject.novel.common.exception.BusinessException;
import com.myproject.novel.common.exception.ErrorCode;
import com.myproject.novel.common.util.JwtUtils;
import com.myproject.novel.dao.entity.AuthorInfo;
import com.myproject.novel.dao.entity.SysUser;
import com.myproject.novel.dao.entity.UserInfo;
import com.myproject.novel.dao.entity.UserOauth;
import com.myproject.novel.dao.mapper.AuthorInfoMapper;
import com.myproject.novel.dao.mapper.SysUserMapper;
import com.myproject.novel.dao.mapper.UserInfoMapper;
import com.myproject.novel.dao.mapper.UserOauthMapper;
import com.myproject.novel.dto.req.LoginReqDto;
import com.myproject.novel.dto.req.OauthLoginReqDto;
import com.myproject.novel.dto.req.RegisterReqDto;
import com.myproject.novel.dto.req.ResetPasswordReqDto;
import com.myproject.novel.dto.resp.CaptionRespDto;
import com.myproject.novel.dto.resp.LoginRespDto;
import com.myproject.novel.security.LoginUser;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 认证服务：登录 / 三方登录 / 注册 / 验证码 / 密码重置
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private static final String CAPTCHA_PREFIX = "captcha:";
    private static final long CAPTCHA_TTL_MINUTES = 5;

    private final UserInfoMapper userInfoMapper;
    private final SysUserMapper sysUserMapper;
    private final UserOauthMapper userOauthMapper;
    private final AuthorInfoMapper authorInfoMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;
    private final StringRedisTemplate stringRedisTemplate;

    // ======================== 登录 ========================

    /**
     * 用户名密码登录：支持按身份（READER / AUTHOR / ADMIN）校验并分流
     */
    public LoginRespDto login(LoginReqDto dto) {
        // 验证码校验（如果提供了则必须正确）
        if (dto.getCaptchaKey() != null && !dto.getCaptchaKey().isEmpty()) {
            validateCaptcha(dto.getCaptchaKey(), dto.getCaptchaCode());
        }

        String expectRole = CommonConsts.Role.normalize(dto.getRole());

        // 先查普通用户表（包含 READER / AUTHOR）
        UserInfo userInfo = userInfoMapper.selectOne(
                new LambdaQueryWrapper<UserInfo>()
                        .eq(UserInfo::getUsername, dto.getUsername()));

        if (userInfo != null) {
            if (userInfo.getStatus() != null && userInfo.getStatus() == CommonConsts.NO) {
                throw new BusinessException(ErrorCode.USER_DISABLED);
            }
            if (!passwordEncoder.matches(dto.getPassword(), userInfo.getPassword())) {
                throw new BusinessException(ErrorCode.USER_ACCOUNT_OR_PASSWORD_ERROR);
            }

            String role = resolveRole(userInfo);
            // 「读者」入口不做任何身份拦截：账号密码正确即登录成功（作家/管理员账号也可走读者入口）
            if (CommonConsts.Role.AUTHOR.equals(expectRole)
                    && !CommonConsts.Role.AUTHOR.equals(role)) {
                throw new BusinessException(ErrorCode.ROLE_MISMATCH);
            }
            // 选择「管理者」身份但账号不是管理员
            if (CommonConsts.Role.ADMIN.equals(expectRole)) {
                throw new BusinessException(ErrorCode.ROLE_MISMATCH);
            }

            AuthorInfo authorInfo = CommonConsts.Role.AUTHOR.equals(role)
                    ? authorInfoMapper.selectOne(new LambdaQueryWrapper<AuthorInfo>()
                        .eq(AuthorInfo::getUserId, userInfo.getId())) : null;

            LoginUser loginUser = new LoginUser();
            loginUser.setUserId(userInfo.getId());
            loginUser.setUsername(userInfo.getUsername());
            loginUser.setRole(role);
            loginUser.setAuthorId(authorInfo != null ? authorInfo.getId() : null);
            loginUser.setIsVip(userInfo.getIsVip());
            String token = jwtUtils.generateToken(loginUser);
            return buildLoginResp(token, userInfo, role);
        }

        // 再查管理员表
        SysUser sysUser = sysUserMapper.selectOne(
                new LambdaQueryWrapper<SysUser>()
                        .eq(SysUser::getUsername, dto.getUsername()));

        if (sysUser != null) {
            if (sysUser.getStatus() != null && sysUser.getStatus() == CommonConsts.NO) {
                throw new BusinessException(ErrorCode.USER_DISABLED);
            }
            if (!passwordEncoder.matches(dto.getPassword(), sysUser.getPassword())) {
                throw new BusinessException(ErrorCode.USER_ACCOUNT_OR_PASSWORD_ERROR);
            }
            // 管理员账号可走「管理者」或「读者」入口；选择「作家」入口则拦截
            if (CommonConsts.Role.AUTHOR.equals(expectRole)) {
                throw new BusinessException(ErrorCode.ROLE_MISMATCH);
            }
            LoginUser loginUser = new LoginUser();
            loginUser.setUserId(sysUser.getId());
            loginUser.setUsername(sysUser.getUsername());
            loginUser.setRole(CommonConsts.Role.ADMIN);
            String token = jwtUtils.generateToken(loginUser);
            return new LoginRespDto(token, sysUser.getId(), sysUser.getUsername(),
                    sysUser.getName(), sysUser.getAvatar(), CommonConsts.Role.ADMIN, null);
        }

        throw new BusinessException(ErrorCode.USER_ACCOUNT_OR_PASSWORD_ERROR);
    }

    /**
     * 计算用户真实角色：优先读 user_info.role，缺失时回退到作者审核表
     */
    private String resolveRole(UserInfo userInfo) {
        String role = CommonConsts.Role.normalize(userInfo.getRole());
        if (CommonConsts.Role.AUTHOR.equals(role)) {
            return role;
        }
        // role 列为 READER 时，再确认是否存在已通过的作者档案（兼容历史数据）
        Long approved = authorInfoMapper.selectCount(
                new LambdaQueryWrapper<AuthorInfo>()
                        .eq(AuthorInfo::getUserId, userInfo.getId())
                        .eq(AuthorInfo::getStatus, CommonConsts.AuthorStatus.APPROVED));
        return (approved != null && approved > 0) ? CommonConsts.Role.AUTHOR : CommonConsts.Role.READER;
    }

    // ======================== 三方登录（模拟 OAuth） ========================

    /**
     * 模拟 GitHub / 微信 OAuth 登录：如果 oauthId 已绑定则直接登录，否则自动创建用户并绑定
     */
    @Transactional
    public LoginRespDto oauthLogin(OauthLoginReqDto dto) {
        String oauthType = dto.getOauthType();

        // 查询已有绑定
        UserOauth existingOauth = userOauthMapper.selectOne(
                new LambdaQueryWrapper<UserOauth>()
                        .eq(UserOauth::getOauthType, oauthType)
                        .eq(UserOauth::getOauthId, dto.getOauthId()));

        UserInfo userInfo;
        if (existingOauth != null) {
            // 已绑定，直接用已有用户登录
            userInfo = userInfoMapper.selectById(existingOauth.getUserId());
            if (userInfo == null || (userInfo.getStatus() != null && userInfo.getStatus() == CommonConsts.NO)) {
                throw new BusinessException(ErrorCode.USER_DISABLED);
            }
        } else {
            // 未绑定，自动注册新用户并绑定
            userInfo = new UserInfo();
            String nickName = dto.getNickName() != null && !dto.getNickName().isEmpty()
                    ? dto.getNickName()
                    : ("user_" + oauthType + "_" + dto.getOauthId().substring(0, Math.min(dto.getOauthId().length(), 8)));
            userInfo.setUsername(UUID.randomUUID().toString().replace("-", "").substring(0, 16));
            userInfo.setPassword(passwordEncoder.encode(UUID.randomUUID().toString()));
            userInfo.setNickName(nickName);
            userInfo.setLevel(0);
            userInfo.setIsVip(CommonConsts.NO);
            userInfo.setStatus(CommonConsts.YES);
            userInfo.setRole(CommonConsts.Role.READER);
            userInfoMapper.insert(userInfo);

            // 创建绑定
            UserOauth oauth = new UserOauth();
            oauth.setUserId(userInfo.getId());
            oauth.setOauthType(oauthType);
            oauth.setOauthId(dto.getOauthId());
            userOauthMapper.insert(oauth);
        }

        String role = resolveRole(userInfo);
        LoginUser loginUser = new LoginUser();
        loginUser.setUserId(userInfo.getId());
        loginUser.setUsername(userInfo.getUsername());
        loginUser.setRole(role);
        loginUser.setIsVip(userInfo.getIsVip());
        String token = jwtUtils.generateToken(loginUser);
        return buildLoginResp(token, userInfo, role);
    }

    // ======================== 注册 ========================

    /**
     * 按身份注册（READER / AUTHOR / ADMIN），注册成功后直接返回登录态
     */
    @Transactional
    public LoginRespDto register(RegisterReqDto dto) {
        String role = CommonConsts.Role.normalize(dto.getRole());

        // 用户名在全站（普通用户 + 管理员）唯一
        if (userInfoMapper.selectCount(new LambdaQueryWrapper<UserInfo>()
                .eq(UserInfo::getUsername, dto.getUsername())) > 0
            || sysUserMapper.selectCount(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, dto.getUsername())) > 0) {
            throw new BusinessException(ErrorCode.USER_ALREADY_EXISTS);
        }

        LoginRespDto resp;
        if (CommonConsts.Role.ADMIN.equals(role)) {
            resp = registerAdmin(dto);
        } else {
            resp = registerUser(dto, role);
        }

        // 注册成功后清除验证码（如有）
        if (dto.getCaptchaKey() != null && !dto.getCaptchaKey().isEmpty()) {
            stringRedisTemplate.delete(CAPTCHA_PREFIX + dto.getCaptchaKey());
        }
        return resp;
    }

    /** 注册普通用户（读者 / 作家） */
    private LoginRespDto registerUser(RegisterReqDto dto, String role) {
        UserInfo userInfo = new UserInfo();
        userInfo.setUsername(dto.getUsername());
        userInfo.setPassword(passwordEncoder.encode(dto.getPassword()));
        userInfo.setRole(CommonConsts.Role.AUTHOR.equals(role)
                ? CommonConsts.Role.AUTHOR : CommonConsts.Role.READER);
        userInfo.setNickName(dto.getUsername());
        userInfo.setLevel(0);
        userInfo.setIsVip(CommonConsts.NO);
        userInfo.setStatus(CommonConsts.YES);
        userInfoMapper.insert(userInfo);

        Long authorId = null;
        if (CommonConsts.Role.AUTHOR.equals(role)) {
            // 直接以作家身份注册：初始化一条已审核通过的作者档案，注册完即可创作
            AuthorInfo authorInfo = new AuthorInfo();
            authorInfo.setUserId(userInfo.getId());
            authorInfo.setPenName(dto.getPenName() != null && !dto.getPenName().isBlank()
                    ? dto.getPenName().trim() : dto.getUsername());
            authorInfo.setIntro("通过注册通道直接入驻的作家");
            authorInfo.setStatus(CommonConsts.AuthorStatus.APPROVED);
            authorInfoMapper.insert(authorInfo);
            authorId = authorInfo.getId();
        }

        LoginUser loginUser = new LoginUser();
        loginUser.setUserId(userInfo.getId());
        loginUser.setUsername(userInfo.getUsername());
        loginUser.setRole(userInfo.getRole());
        loginUser.setAuthorId(authorId);
        loginUser.setIsVip(userInfo.getIsVip());
        String token = jwtUtils.generateToken(loginUser);
        return buildLoginResp(token, userInfo, userInfo.getRole());
    }

    /** 注册管理员（写入 sys_user） */
    private LoginRespDto registerAdmin(RegisterReqDto dto) {
        SysUser sysUser = new SysUser();
        sysUser.setUsername(dto.getUsername());
        sysUser.setPassword(passwordEncoder.encode(dto.getPassword()));
        sysUser.setName(dto.getUsername());
        sysUser.setStatus(CommonConsts.YES);
        sysUserMapper.insert(sysUser);

        LoginUser loginUser = new LoginUser();
        loginUser.setUserId(sysUser.getId());
        loginUser.setUsername(sysUser.getUsername());
        loginUser.setRole(CommonConsts.Role.ADMIN);
        String token = jwtUtils.generateToken(loginUser);
        return new LoginRespDto(token, sysUser.getId(), sysUser.getUsername(),
                sysUser.getName(), sysUser.getAvatar(), CommonConsts.Role.ADMIN, null);
    }

    // ======================== 验证码 ========================

    /**
     * 生成数学算式图形验证码（算术题）
     */
    public CaptionRespDto generateCaptcha() {
        int a = ThreadLocalRandom.current().nextInt(1, 50);
        int b = ThreadLocalRandom.current().nextInt(1, 50);
        int op = ThreadLocalRandom.current().nextInt(0, 3);
        String expression;
        int answer;
        switch (op) {
            case 0 -> { expression = a + " + " + b + " = ?"; answer = a + b; }
            case 1 -> { expression = a + " - " + b + " = ?"; answer = a - b; }
            default -> { expression = a + " * " + b + " = ?"; answer = a * b; }
        }

        String captchaKey = UUID.randomUUID().toString().replace("-", "");
        stringRedisTemplate.opsForValue().set(
                CAPTCHA_PREFIX + captchaKey,
                String.valueOf(answer),
                CAPTCHA_TTL_MINUTES, TimeUnit.MINUTES);

        CaptionRespDto resp = new CaptionRespDto();
        resp.setCaptchaKey(captchaKey);
        resp.setCaptchaImage(expression);
        return resp;
    }

    // ======================== 忘记密码 ========================

    /**
     * 邮箱验证重置密码
     */
    @Transactional
    public void resetPassword(ResetPasswordReqDto dto) {
        UserInfo userInfo = userInfoMapper.selectOne(
                new LambdaQueryWrapper<UserInfo>()
                        .eq(UserInfo::getEmail, dto.getEmail()));
        if (userInfo == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }

        // 校验邮箱验证码（模拟：直接检查 Redis 中的验证码）
        if (dto.getCode() == null || dto.getCode().isEmpty()) {
            throw new BusinessException(ErrorCode.CAPTCHA_ERROR, "邮箱验证码不能为空");
        }
        validateCaptcha("email:" + dto.getEmail(), dto.getCode());

        userInfo.setPassword(passwordEncoder.encode(dto.getNewPassword()));
        userInfoMapper.updateById(userInfo);

        // 清除验证码
        stringRedisTemplate.delete(CAPTCHA_PREFIX + "email:" + dto.getEmail());
    }

    // ======================== 内部工具方法 ========================

    private void validateCaptcha(String captchaKey, String captchaCode) {
        if (captchaCode == null || captchaCode.isEmpty()) {
            throw new BusinessException(ErrorCode.CAPTCHA_ERROR);
        }
        String stored = stringRedisTemplate.opsForValue().get(CAPTCHA_PREFIX + captchaKey);
        if (stored == null) {
            throw new BusinessException(ErrorCode.CAPTCHA_ERROR);
        }
        if (!stored.equals(captchaCode.trim())) {
            throw new BusinessException(ErrorCode.CAPTCHA_ERROR);
        }
    }

    private LoginRespDto buildLoginResp(String token, UserInfo userInfo, String role) {
        return new LoginRespDto(
                token,
                userInfo.getId(),
                userInfo.getUsername(),
                userInfo.getNickName(),
                userInfo.getUserPhoto(),
                role,
                userInfo.getIsVip());
    }
}
