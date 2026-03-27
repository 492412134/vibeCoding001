package com.example.order.controller;

import com.example.common.result.Result;
import com.example.order.entity.Order;
import com.example.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;

/**
 * 订单控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/order")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    /**
     * 创建订单
     */
    @PostMapping("/create")
    public Result<Order> createOrder(@RequestBody Map<String, Object> request) {
        log.info("创建订单请求：{}", request);
        
        Long userId = Long.valueOf(request.get("userId").toString());
        String productName = (String) request.get("productName");
        BigDecimal amount = new BigDecimal(request.get("amount").toString());
        Boolean vip = (Boolean) request.get("vip");
        String idCard = (String) request.get("idCard");
        String policyType = (String) request.get("policyType");

        Order order = orderService.createOrder(userId, productName, amount, vip, idCard, policyType);
        return Result.success(order);
    }

    /**
     * 订单支付
     */
    @PostMapping("/{orderId}/pay")
    public Result<Map<String, Object>> payOrder(@PathVariable Long orderId) {
        log.info("订单支付请求：orderId={}", orderId);
        return orderService.payOrder(orderId);
    }

    /**
     * 查询订单
     */
    @GetMapping("/{orderId}")
    public Result<Order> getOrder(@PathVariable Long orderId) {
        log.info("查询订单：orderId={}", orderId);
        Order order = orderService.getOrder(orderId);
        if (order == null) {
            return Result.error("订单不存在");
        }
        return Result.success(order);
    }
}
