package com.example.payment.payment.filter;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;

/**
 * 灰度节点访问校验过滤器
 * 确保只有灰度用户能访问灰度节点
 */
@Component
@Order(1) // 最先执行
public class GrayValidationFilter implements Filter {

    private static final Logger logger = LoggerFactory.getLogger(GrayValidationFilter.class);

    @Value("${server.port:unknown}")
    private String serverPort;

    @Value("${gray.node.version:v1}")
    private String nodeVersion;

    @Value("${gray.node.enabled:false}")
    private boolean grayNodeEnabled;

    // 白名单用户ID列表（从配置读取，实际应该从配置中心获取）
    @Value("${gray.user.white-list:}")
    private List<String> grayUserWhiteList;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        String requestUri = httpRequest.getRequestURI();
        String userId = httpRequest.getHeader("X-User-Id");
        String clientIp = getClientIp(httpRequest);

        // 只拦截支付相关接口
        if (!requestUri.startsWith("/api/payment/")) {
            chain.doFilter(request, response);
            return;
        }

        logger.debug("[Port:{}] GrayValidation - URI: {}, UserId: {}, NodeVersion: {}",
                serverPort, requestUri, userId, nodeVersion);

        // v1 节点（正常节点）：允许所有请求
        if ("v1".equals(nodeVersion)) {
            chain.doFilter(request, response);
            return;
        }

        // 灰度节点（v2+）：需要校验
        if (grayNodeEnabled && !"v1".equals(nodeVersion)) {
            // 检查是否是灰度用户
            boolean isGrayUser = isGrayUser(userId, clientIp);

            if (!isGrayUser) {
                logger.warn("[Port:{}] 非灰度用户访问灰度节点被拒绝 - UserId: {}, IP: {}",
                        serverPort, userId, clientIp);
                httpResponse.setStatus(HttpServletResponse.SC_FORBIDDEN);
                httpResponse.setContentType("application/json;charset=UTF-8");
                httpResponse.getWriter().write(
                        "{\"code\":403,\"message\":\"非灰度用户禁止访问灰度节点\"}");
                return;
            }

            logger.info("[Port:{}] 灰度用户访问通过 - UserId: {}", serverPort, userId);
        }

        chain.doFilter(request, response);
    }

    /**
     * 判断是否是灰度用户
     */
    private boolean isGrayUser(String userId, String clientIp) {
        // 1. 白名单用户
        if (userId != null && grayUserWhiteList != null && grayUserWhiteList.contains(userId)) {
            return true;
        }

        // 2. 本地IP（开发测试）
        if ("127.0.0.1".equals(clientIp) || "0:0:0:0:0:0:0:1".equals(clientIp)) {
            return true;
        }

        // 3. 内网IP
        if (clientIp != null && (clientIp.startsWith("192.168.") || clientIp.startsWith("10."))) {
            return true;
        }

        return false;
    }

    /**
     * 获取客户端IP
     */
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }
}
