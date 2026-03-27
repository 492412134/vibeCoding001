package com.example.order.feign.fallback;

import com.example.common.result.Result;
import com.example.order.feign.PaymentFeignClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 支付服务 Feign 降级处理
 */
@Slf4j
@Component
public class PaymentFeignClientFallback implements PaymentFeignClient {
    
    @Override
    public Result<Map<String, Object>> processPayment(Map<String, Object> request) {
        log.error("支付服务调用失败，触发降级，请求：{}", request);
        return Result.error("支付服务暂时不可用，请稍后重试");
    }
}
