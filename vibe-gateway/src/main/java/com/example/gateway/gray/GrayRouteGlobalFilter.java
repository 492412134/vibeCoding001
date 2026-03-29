// package com.example.gateway.gray;

// import lombok.extern.slf4j.Slf4j;
// import org.springframework.cloud.client.ServiceInstance;
// import org.springframework.cloud.gateway.filter.GatewayFilterChain;
// import org.springframework.cloud.gateway.filter.GlobalFilter;
// import org.springframework.cloud.gateway.route.Route;
// import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
// import org.springframework.core.Ordered;
// import org.springframework.stereotype.Component;
// import org.springframework.web.server.ServerWebExchange;
// import reactor.core.publisher.Mono;

// import java.net.URI;
// import java.time.LocalDateTime;
// import java.time.format.DateTimeFormatter;
// import java.util.HashMap;
// import java.util.Map;
// import java.util.concurrent.ConcurrentHashMap;

// import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.GATEWAY_REQUEST_URL_ATTR;

// /**
//  * 灰度路由全局过滤器
//  * 在 LoadBalancerClientFilter 之前执行，确保灰度路由生效
//  * @deprecated 请使用 GrayRouteFilter
//  */
// @Slf4j
// @Deprecated
// public class GrayRouteGlobalFilter implements GlobalFilter, Ordered {

//     private final GrayRuleManager grayRuleManager;
//     private final ServiceInstanceSelector serviceInstanceSelector;

//     private static final Map<String, StringBuilder> routeLogs = new ConcurrentHashMap<>();
//     private static final RouteStats routeStats = new RouteStats();

//     public GrayRouteGlobalFilter(GrayRuleManager grayRuleManager, ServiceInstanceSelector serviceInstanceSelector) {
//         this.grayRuleManager = grayRuleManager;
//         this.serviceInstanceSelector = serviceInstanceSelector;
//     }

//     @Override
//     public int getOrder() {
//         // 在 LoadBalancerClientFilter(10150) 之前执行
//         return 10000;
//     }

//     @Override
//     public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
//         Route route = exchange.getAttribute(ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR);
//         if (route == null) {
//             return chain.filter(exchange);
//         }

//         String serviceId = route.getUri().getHost();
//         if (serviceId == null) {
//             return chain.filter(exchange);
//         }

//         // 只处理 vibe-payment-service
//         if (!"vibe-payment-service".equals(serviceId)) {
//             return chain.filter(exchange);
//         }

//         // 调试日志：打印请求头信息
//         String userId = exchange.getRequest().getHeaders().getFirst("X-User-Id");
//         String clientIp = exchange.getRequest().getRemoteAddress() != null
//                 ? exchange.getRequest().getRemoteAddress().getAddress().getHostAddress()
//                 : "unknown";
//         log.debug("[GrayRouteGlobal] Service: {}, X-User-Id: {}, ClientIP: {}", serviceId, userId, clientIp);

//         String routeVersion = "v1";
//         String routeType = "normal";
//         String matchedCondition = "none";

//         routeStats.incrementTotalRequests();

//         if (grayRuleManager.shouldRouteToGray(exchange, serviceId)) {
//             GrayRuleConfig.GrayRule rule = grayRuleManager.getRule(serviceId);
//             log.debug("[GrayRouteGlobal] Gray rule found: version={}, enabled={}",
//                     rule != null ? rule.getVersion() : "null",
//                     rule != null ? rule.isEnabled() : "null");

//             if (rule != null) {
//                 ServiceInstance instance = serviceInstanceSelector.selectInstance(serviceId, rule.getVersion());
//                 log.debug("[GrayRouteGlobal] Selected instance: {}", instance != null ?
//                         instance.getHost() + ":" + instance.getPort() : "null");

//                 if (instance != null) {
//                     // 使用请求的 path 构建灰度 URI
//                     String path = exchange.getRequest().getURI().getPath();
//                     String query = exchange.getRequest().getURI().getQuery();
//                     String fullPath = path + (query != null ? "?" + query : "");

//                     // 直接路由到选中的灰度实例，使用 http://host:port 格式
//                     URI grayUri = URI.create(String.format("http://%s:%d%s",
//                             instance.getHost(), instance.getPort(), fullPath));
//                     log.debug("[GrayRouteGlobal] 修改 URI: {} -> {}",
//                             exchange.getAttribute(GATEWAY_REQUEST_URL_ATTR), grayUri);
//                     // 重写请求URI，路由到灰度实例的URI
//                     exchange.getAttributes().put(GATEWAY_REQUEST_URL_ATTR, grayUri);
//                     //记录日志等信息
//                     routeVersion = rule.getVersion();
//                     routeType = "gray";
//                     matchedCondition = getMatchedCondition(exchange, rule);

//                     routeStats.incrementGrayRequests();
//                     routeStats.incrementVersionCount(routeVersion);

//                     String logMessage = String.format(
//                             "[%s] [GRAY ROUTE] Service: %s, Version: %s, Condition: %s, URI: %s, Instance: %s:%d",
//                             LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
//                             serviceId, routeVersion, matchedCondition, path,
//                             instance.getHost(), instance.getPort()
//                     );
//                     log.info(logMessage);
//                     addRouteLog(serviceId, logMessage);
//                 } else {
//                     log.warn("[GrayRouteGlobal] No instance found for version: {}, fallback to normal route", rule.getVersion());
//                 }
//             } else {
//                 log.warn("[GrayRouteGlobal] Rule is null after shouldRouteToGray returned true");
//             }
//         } else {
//             routeStats.incrementNormalRequests();
//             routeStats.incrementVersionCount(routeVersion);

//             String logMessage = String.format(
//                     "[%s] [NORMAL ROUTE] Service: %s, Version: %s, URI: %s",
//                     LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
//                     serviceId, routeVersion, exchange.getRequest().getURI().getPath()
//             );
//             log.debug(logMessage);
//             addRouteLog(serviceId, logMessage);
//         }

//         exchange.getAttributes().put("grayRouteVersion", routeVersion);
//         exchange.getAttributes().put("grayRouteType", routeType);
//         exchange.getAttributes().put("grayMatchedCondition", matchedCondition);

//         return chain.filter(exchange);
//     }

//     private String getMatchedCondition(ServerWebExchange exchange, GrayRuleConfig.GrayRule rule) {
//         if (rule.getConditions() == null || rule.getConditions().isEmpty()) {
//             return "weight-based";
//         }

//         for (GrayRuleConfig.GrayCondition condition : rule.getConditions()) {
//             String type = condition.getType();
//             String value = null;

//             switch (type) {
//                 case "user_id":
//                     value = exchange.getRequest().getHeaders().getFirst("X-User-Id");
//                     if (value == null) {
//                         value = exchange.getRequest().getQueryParams().getFirst("userId");
//                     }
//                     break;
//                 case "ip":
//                     value = exchange.getRequest().getRemoteAddress().getAddress().getHostAddress();
//                     break;
//                 case "header":
//                     if (condition.getValues() != null && !condition.getValues().isEmpty()) {
//                         value = exchange.getRequest().getHeaders().getFirst(condition.getValues().get(0));
//                     }
//                     break;
//                 case "param":
//                     if (condition.getValues() != null && condition.getValues().size() > 1) {
//                         value = exchange.getRequest().getQueryParams().getFirst(condition.getValues().get(0));
//                     }
//                     break;
//             }

//             if (value != null) {
//                 return String.format("%s=%s", type, value);
//             }
//         }

//         return "weight-based";
//     }

//     private void addRouteLog(String serviceId, String logMessage) {
//         routeLogs.computeIfAbsent(serviceId, k -> new StringBuilder());
//         StringBuilder sb = routeLogs.get(serviceId);

//         if (sb.length() > 10000) {
//             sb.delete(0, 2000);
//         }

//         sb.append(logMessage).append("\n");
//     }

//     public static Map<String, String> getRouteLogs() {
//         Map<String, String> logs = new HashMap<>();
//         routeLogs.forEach((key, value) -> logs.put(key, value.toString()));
//         return logs;
//     }

//     public static String getRouteLogs(String serviceId) {
//         StringBuilder sb = routeLogs.get(serviceId);
//         return sb != null ? sb.toString() : "";
//     }

//     public static void clearRouteLogs() {
//         routeLogs.clear();
//     }

//     public static RouteStats getRouteStats() {
//         return routeStats;
//     }

//     public static void resetRouteStats() {
//         routeStats.reset();
//     }
// }
