/**
 * API 配置
 * Vite 开发服务器会将 /api 代理到 Gateway (8081)
 * 生产环境需要配置 Nginx 或其他反向代理
 */
const ApiConfig = {
    // API 基础路径（Vite 代理会将 /api 转发到 Gateway）
    baseUrl: '/api',
    
    // 各服务路径
    payment: '/api/payment',
    order: '/api/order',
    rules: '/api/rule',
    snowflake: '/api/snowflake',
    external: '/api/external',
    mock: '/api/mock'
};
