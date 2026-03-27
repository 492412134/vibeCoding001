/**
 * 支付服务本地配置
 * 用于支付服务独立部署时，直接访问本地 API
 */

const PaymentConfig = {
    // 本地 API 路径前缀
    apiPrefix: {
        payment: '/api/payment',
        rules: '/api/rules',
        snowflake: '/api/snowflake',
        external: '/api/external',
        mock: '/api/mock'
    },

    /**
     * 获取完整的API URL
     * @param {string} service - 服务名
     * @param {string} path - API路径
     * @returns {string} 完整的URL
     */
    getApiUrl(service, path) {
        const prefix = this.apiPrefix[service] || '';
        return `${prefix}${path}`;
    }
};

// 导出配置
if (typeof module !== 'undefined' && module.exports) {
    module.exports = PaymentConfig;
}
