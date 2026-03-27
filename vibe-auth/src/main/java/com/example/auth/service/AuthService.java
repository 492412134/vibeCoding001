package com.example.auth.service;

import com.example.auth.dto.*;
import com.example.auth.entity.User;
import com.example.auth.mapper.UserMapper;
import com.example.auth.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserMapper userMapper;
    private final JwtUtil jwtUtil;
    private final TokenService tokenService;
    private final PasswordEncoder passwordEncoder;

    @Value("${jwt.expiration}")
    private Long expiration;

    @Value("${jwt.refresh-expiration}")
    private Long refreshExpiration;

    /**
     * 用户登录
     */
    public TokenResponse login(LoginRequest request) {
        // 查找用户
        User user = userMapper.findByUsername(request.getUsername());
        if (user == null) {
            throw new RuntimeException("用户名或密码错误");
        }

        // 检查状态
        if (user.getStatus() == 0) {
            throw new RuntimeException("账号已被禁用");
        }

        // 验证密码
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new RuntimeException("用户名或密码错误");
        }

        // 生成Token
        return generateTokenResponse(user);
    }

    /**
     * 用户注册
     */
    @Transactional
    public TokenResponse register(RegisterRequest request) {
        // 检查用户名是否已存在
        if (userMapper.findByUsername(request.getUsername()) != null) {
            throw new RuntimeException("用户名已存在");
        }

        // 检查邮箱是否已存在
        if (request.getEmail() != null && userMapper.findByEmail(request.getEmail()) != null) {
            throw new RuntimeException("邮箱已被注册");
        }

        // 创建用户
        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setEmail(request.getEmail());
        user.setPhone(request.getPhone());
        user.setRole(request.getRole());
        user.setStatus(1);

        userMapper.insert(user);

        log.info("用户注册成功: {}", request.getUsername());

        // 生成Token
        return generateTokenResponse(user);
    }

    /**
     * 刷新Token
     */
    public TokenResponse refreshToken(String refreshToken) {
        // 验证刷新令牌
        Long userId = tokenService.validateRefreshToken(refreshToken);
        if (userId == null) {
            throw new RuntimeException("刷新令牌无效或已过期");
        }

        // 查找用户
        User user = userMapper.findById(userId);
        if (user == null || user.getStatus() == 0) {
            throw new RuntimeException("用户不存在或已被禁用");
        }

        // 删除旧的刷新令牌
        tokenService.deleteRefreshToken(refreshToken);

        // 生成新Token
        return generateTokenResponse(user);
    }

    /**
     * 用户登出
     */
    public void logout(String accessToken, String refreshToken) {
        // 将访问令牌加入黑名单
        long expirationTime = jwtUtil.getExpirationTime(accessToken);
        tokenService.addToBlacklist(accessToken, expirationTime);

        // 删除刷新令牌
        if (refreshToken != null) {
            tokenService.deleteRefreshToken(refreshToken);
        }

        log.info("用户登出成功");
    }

    /**
     * 生成Token响应
     */
    private TokenResponse generateTokenResponse(User user) {
        // 生成访问令牌
        String accessToken = jwtUtil.generateAccessToken(user.getId(), user.getUsername(), user.getRole());

        // 生成刷新令牌
        String refreshToken = jwtUtil.generateRefreshToken(user.getId(), user.getUsername());

        // 存储刷新令牌
        tokenService.storeRefreshToken(refreshToken, user.getId(), refreshExpiration);

        // 存储用户Token（用于单点登录控制，可选）
        tokenService.storeUserToken(user.getId(), accessToken, expiration);

        return TokenResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .expiresIn(expiration / 1000)
                .tokenType("Bearer")
                .userInfo(TokenResponse.UserInfo.builder()
                        .userId(user.getId())
                        .username(user.getUsername())
                        .role(user.getRole())
                        .build())
                .build();
    }
}
