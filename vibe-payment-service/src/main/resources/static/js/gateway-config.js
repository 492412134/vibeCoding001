/**
 * 分布式网关配置
 * 所有前端页面通过此配置访问后端API
 * 
 * 架构说明：
 * - 前端页面通过 Gateway (端口8081) 访问所有服务
 * - Gateway 负责负载均衡，将请求分发到多个服务实例
 * - 支付服务可以启动多个实例（如 10991, 10993），Gateway 自动轮询
 */

const GatewayConfig = {
    // Gateway 地址 - 所有请求都通过这里访问
    // 开发环境使用 localhost:8081
    // 生产环境应改为域名或负载均衡器地址
    baseUrl: '',
    
    // API 路径前缀
    apiPrefix: {
        payment: '/api/payment',
        rules: '/api/rules',
        snowflake: '/api/snowflake',
        external: '/api/external',
        mock: '/api/mock',
        order: '/api/order'
    },
    
    /**
     * 获取完整的API URL
     * @param {string} service - 服务名: payment|rules|snowflake|external|mock|order
     * @param {string} path - API路径，如 '/single', '/batch'
     * @returns {string} 完整的URL
     */
    getApiUrl(service, path) {
        const prefix = this.apiPrefix[service] || '';
        return `${this.baseUrl}${prefix}${path}`;
    },
    
    /**
     * 获取负载均衡测试URL
     * 用于测试Gateway的负载均衡效果
     */
    getLoadBalanceUrl(path) {
        return `${this.baseUrl}/lb${path}`;
    },
    
    /**
     * 显示当前服务实例信息
     * 用于验证负载均衡是否生效
     */
    async showServiceInstance() {
        try {
            const response = await fetch(`${this.baseUrl}/api/payment/instance`);
            const data = await response.json();
            console.log('当前服务实例:', data);
            return data;
        } catch (error) {
            console.error('获取服务实例信息失败:', error);
            return null;
        }
    }
};

// 导出配置（兼容不同模块方式）
if (typeof module !== 'undefined' && module.exports) {
    module.exports = GatewayConfig;
}
