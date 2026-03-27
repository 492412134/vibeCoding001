package com.example.order.mapper;

import com.example.order.entity.Order;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * 订单 Mapper
 */
@Mapper
public interface OrderMapper {
    
    /**
     * 插入订单
     */
    @Insert("INSERT INTO t_order (order_no, user_id, product_name, amount, status, vip, " +
            "payment_request_id, id_card, policy_type, policy_code, create_time, update_time) " +
            "VALUES (#{orderNo}, #{userId}, #{productName}, #{amount}, #{status}, #{vip}, " +
            "#{paymentRequestId}, #{idCard}, #{policyType}, #{policyCode}, NOW(), NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Order order);
    
    /**
     * 根据ID查询订单
     */
    @Select("SELECT * FROM t_order WHERE id = #{id}")
    Order selectById(Long id);
    
    /**
     * 根据订单号查询
     */
    @Select("SELECT * FROM t_order WHERE order_no = #{orderNo}")
    Order selectByOrderNo(String orderNo);
    
    /**
     * 更新订单状态
     */
    @Update("UPDATE t_order SET status = #{status}, payment_request_id = #{paymentRequestId}, " +
            "update_time = NOW() WHERE id = #{id}")
    int updateStatus(@Param("id") Long id, @Param("status") String status, 
                     @Param("paymentRequestId") String paymentRequestId);
    
    /**
     * 查询用户订单列表
     */
    @Select("SELECT * FROM t_order WHERE user_id = #{userId} ORDER BY create_time DESC")
    List<Order> selectByUserId(Long userId);
}
