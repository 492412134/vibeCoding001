package com.example.order.service;

import com.example.common.result.Result;
import com.example.order.entity.Order;
import com.example.order.feign.PaymentFeignClient;
import com.example.order.mapper.OrderMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 订单服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderMapper orderMapper;
    private final PaymentFeignClient paymentFeignClient;

    /**
     * 创建订单
     */
    @Transactional
    public Order createOrder(Long userId, String productName, BigDecimal amount, 
                             Boolean vip, String idCard, String policyType) {
        Order order = new Order();
        order.setOrderNo(generateOrderNo());
        order.setUserId(userId);
        order.setProductName(productName);
        order.setAmount(amount);
        order.setStatus("PENDING");
        order.setVip(vip != null && vip);
        order.setIdCard(idCard);
        order.setPolicyType(policyType);
        // 生成 policyCode：身份证后4位或随机数
        if (idCard != null && idCard.length() >= 4) {
            try {
                order.setPolicyCode(Long.parseLong(idCard.substring(idCard.length() - 4)));
            } catch (NumberFormatException e) {
                order.setPolicyCode(ThreadLocalRandom.current().nextLong(1, 10000));
            }
        } else {
            order.setPolicyCode(ThreadLocalRandom.current().nextLong(1, 10000));
        }
        order.setCreateTime(LocalDateTime.now());
        order.setUpdateTime(LocalDateTime.now());

        orderMapper.insert(order);
        log.info("订单创建成功：orderNo={}, userId={}", order.getOrderNo(), userId);
        return order;
    }

    /**
     * 订单支付
     */
    @Transactional
    public Result<Map<String, Object>> payOrder(Long orderId) {
        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            return Result.error("订单不存在");
        }
        if (!"PENDING".equals(order.getStatus())) {
            return Result.error("订单状态不正确，当前状态：" + order.getStatus());
        }

        // 构建支付请求
        Map<String, Object> paymentRequest = new HashMap<>();
        paymentRequest.put("orderId", order.getOrderNo());
        paymentRequest.put("amount", order.getAmount());
        paymentRequest.put("idCard", order.getIdCard());
        paymentRequest.put("policyType", order.getPolicyType());
        paymentRequest.put("policyCode", order.getPolicyCode());
        paymentRequest.put("vip", order.getVip());

        log.info("调用支付服务，订单号：{}，金额：{}", order.getOrderNo(), order.getAmount());

        // 调用支付服务
        Result<Map<String, Object>> paymentResult = paymentFeignClient.processPayment(paymentRequest);

        if (paymentResult.isSuccess()) {
            // 支付成功，更新订单状态
            Map<String, Object> data = paymentResult.getData();
            String requestId = (String) data.get("requestId");
            orderMapper.updateStatus(orderId, "PAID", requestId);
            log.info("订单支付成功：orderNo={}, requestId={}", order.getOrderNo(), requestId);
        } else {
            log.error("订单支付失败：orderNo={}, error={}", order.getOrderNo(), paymentResult.getMessage());
        }

        return paymentResult;
    }

    /**
     * 查询订单
     */
    public Order getOrder(Long orderId) {
        return orderMapper.selectById(orderId);
    }

    /**
     * 生成订单号
     */
    private String generateOrderNo() {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String random = UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        return "ORD" + timestamp + random;
    }
}
