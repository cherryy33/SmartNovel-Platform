package com.myproject.novel.config;

import com.myproject.novel.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Spring Security 配置：基于 JWT 实现 用户端 / 作者端 / 管理员端 三端权限隔离
 */
@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // 仅在初始请求上做鉴权：流式/异步响应（StreamingResponseBody）的
                // ASYNC 分派没有原始 SecurityContext，重复鉴权会误判为 Access Denied
                .shouldFilterAllDispatcherTypes(false)
                // 健康检查、静态资源
                .requestMatchers("/actuator/**", "/error").permitAll()
                // 公开接口：登录注册、三方登录回调、小说浏览/详情/目录/章节试读、分类、搜索、推荐热门等
                .requestMatchers("/api/front/auth/**").permitAll()
                // 用户端 GET 请求全部公开（浏览小说、榜单、分类、搜索、社区等无需登录）
                .requestMatchers(HttpMethod.GET, "/api/front/**").permitAll()
                // 用户端 POST/PUT/DELETE：登录即可
                .requestMatchers("/api/front/**").authenticated()
                // 作家入驻申请：任何已登录用户（读者）均可提交与查询
                .requestMatchers("/api/author/apply", "/api/author/apply/status").authenticated()
                // 作者端：作者或管理员
                .requestMatchers("/api/author/**").hasAnyRole("AUTHOR", "ADMIN")
                // 管理员端：仅管理员
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOriginPatterns(List.of("*"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
