package com.example.order.feign;

import com.example.common.result.Result;
import com.example.order.feign.fallback.PaymentFeignClientFallback;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Map;

/**
 * 支付服务 Feign 客户端
 */
@FeignClient(
    name = "vibe-payment-service",
    fallback = PaymentFeignClientFallback.class
)
public interface PaymentFeignClient {
    
    /**
     * 单笔支付
     */
    @PostMapping("/api/payment/single")
    Result<Map<String, Object>> processPayment(@RequestBody Map<String, Object> request);
}
