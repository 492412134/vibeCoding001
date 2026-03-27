package com.example.gateway.filter;

import com.example.gateway.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class AuthFilter implements GlobalFilter, Ordered {

    private final JwtUtil jwtUtil;
    private final ReactiveStringRedisTemplate redisTemplate;

    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    // 从配置文件读取白名单，默认值为常用配置
    @Value("${gateway.auth.white-list:/api/auth/**,/api/rule/**,/actuator/**}")
    private List<String> whiteList;

    // 需要特定角色的路径
    private static final List<PathRole> PATH_ROLES = List.of(
            new PathRole("/api/payment/**", List.of("ADMIN", "USER", "MERCHANT")),
            new PathRole("/api/order/**", List.of("ADMIN", "USER"))
    );

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();

        log.debug("请求路径: {}", path);

        // 1. 检查白名单
        if (isWhiteList(path)) {
            log.debug("白名单路径，放行: {}", path);
            return chain.filter(exchange);
        }

        // 2. 获取Token
        String token = extractToken(request);
        if (token == null) {
            log.warn("请求未携带Token: {}", path);
            return unauthorized(exchange.getResponse(), "请先登录");
        }

        // 3. 验证Token
        if (!jwtUtil.validateToken(token)) {
            log.warn("Token验证失败: {}", path);
            return unauthorized(exchange.getResponse(), "登录已过期，请重新登录");
        }

        // 4. 检查Token黑名单
        return isTokenBlacklisted(token)
                .flatMap(isBlacklisted -> {
                    if (isBlacklisted) {
                        log.warn("Token已在黑名单: {}", path);
                        return unauthorized(exchange.getResponse(), "登录已失效，请重新登录");
                    }

                    // 5. 获取用户信息
                    Long userId = jwtUtil.getUserIdFromToken(token);
                    String username = jwtUtil.getUsernameFromToken(token);
                    String role = jwtUtil.getRoleFromToken(token);

                    log.debug("用户认证成功: {}, role: {}", username, role);

                    // 6. 权限校验
                    if (!hasPermission(path, role)) {
                        log.warn("权限不足: {}, user: {}, role: {}", path, username, role);
                        return forbidden(exchange.getResponse(), "权限不足");
                    }

                    // 7. 将用户信息传递给下游服务
                    ServerHttpRequest mutatedRequest = request.mutate()
                            .header("X-User-Id", userId.toString())
                            .header("X-Username", username)
                            .header("X-Role", role)
                            .build();

                    return chain.filter(exchange.mutate().request(mutatedRequest).build());
                });
    }

    /**
     * 检查是否在白名单
     */
    private boolean isWhiteList(String path) {
        return whiteList.stream().anyMatch(pattern -> pathMatcher.match(pattern, path));
    }

    /**
     * 提取Token
     */
    private String extractToken(ServerHttpRequest request) {
        String bearerToken = request.getHeaders().getFirst("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }

    /**
     * 检查Token是否在黑名单
     */
    private Mono<Boolean> isTokenBlacklisted(String token) {
        String key = "auth:blacklist:" + token;
        return redisTemplate.hasKey(key);
    }

    /**
     * 检查权限
     */
    private boolean hasPermission(String path, String userRole) {
        for (PathRole pathRole : PATH_ROLES) {
            if (pathMatcher.match(pathRole.pattern, path)) {
                return pathRole.allowedRoles.contains(userRole);
            }
        }
        // 没有配置的路径默认允许访问
        return true;
    }

    /**
     * 返回401未授权
     */
    private Mono<Void> unauthorized(ServerHttpResponse response, String message) {
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        String body = String.format("{\"code\":401,\"message\":\"%s\"}", message);
        DataBuffer buffer = response.bufferFactory().wrap(body.getBytes(StandardCharsets.UTF_8));
        return response.writeWith(Mono.just(buffer));
    }

    /**
     * 返回403禁止访问
     */
    private Mono<Void> forbidden(ServerHttpResponse response, String message) {
        response.setStatusCode(HttpStatus.FORBIDDEN);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        String body = String.format("{\"code\":403,\"message\":\"%s\"}", message);
        DataBuffer buffer = response.bufferFactory().wrap(body.getBytes(StandardCharsets.UTF_8));
        return response.writeWith(Mono.just(buffer));
    }

    @Override
    public int getOrder() {
        // 在路由过滤器之前执行
        return -100;
    }

    /**
     * 路径角色配置
     */
    private record PathRole(String pattern, List<String> allowedRoles) {}
}
