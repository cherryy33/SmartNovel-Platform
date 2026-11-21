package com.myproject.novel.common.util;

import com.myproject.novel.common.constant.CommonConsts;
import com.myproject.novel.security.LoginUser;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * JWT 工具类
 */
@Component
public class JwtUtils {

    @Value("${smart-novel.jwt.secret}")
    private String secret;

    @Value("${smart-novel.jwt.expire-ms:86400000}")
    private long expireMs;

    public String generateToken(LoginUser user) {
        var builder = Jwts.builder()
            .setSubject(String.valueOf(user.getUserId()))
            .claim(CommonConsts.Jwt.ROLE, user.getRole())
            .claim(CommonConsts.Jwt.USERNAME, user.getUsername())
            .setIssuedAt(new Date())
            .setExpiration(new Date(System.currentTimeMillis() + expireMs))
            .signWith(Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8)), SignatureAlgorithm.HS256);
        if (user.getAuthorId() != null) {
            builder.claim(CommonConsts.Jwt.AUTHOR_ID, user.getAuthorId());
        }
        return builder.compact();
    }

    public LoginUser parseToken(String token) {
        try {
            Claims claims = Jwts.parserBuilder()
                .setSigningKey(Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8)))
                .build()
                .parseClaimsJws(token)
                .getBody();
            LoginUser user = new LoginUser();
            user.setUserId(Long.parseLong(claims.getSubject()));
            user.setRole(claims.get(CommonConsts.Jwt.ROLE, String.class));
            user.setUsername(claims.get(CommonConsts.Jwt.USERNAME, String.class));
            Object authorId = claims.get(CommonConsts.Jwt.AUTHOR_ID);
            user.setAuthorId(authorId == null ? null : Long.parseLong(String.valueOf(authorId)));
            return user;
        } catch (Exception e) {
            return null;
        }
    }
}
