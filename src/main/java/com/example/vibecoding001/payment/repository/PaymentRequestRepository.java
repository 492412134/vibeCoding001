package com.example.vibecoding001.payment.repository;

import com.example.vibecoding001.payment.model.PaymentRequest;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * 支付请求数据访问层
 */
@Mapper
public interface PaymentRequestRepository {

    /**
     * 插入支付请求
     */
    @Insert("INSERT INTO payment_request (request_id, name, idcard, bankcard, amount, status, create_time, retry_count, order_type) " +
            "VALUES (#{requestId}, #{name}, #{idcard}, #{bankcard}, #{amount}, #{status}, #{createTime}, #{retryCount}, #{orderType})")
    int insert(PaymentRequest request);

    /**
     * 根据ID查询
     */
    @Select("SELECT * FROM payment_request WHERE request_id = #{requestId}")
    PaymentRequest findById(String requestId);

    /**
     * 查询待处理的请求（PENDING状态）
     */
    @Select("SELECT * FROM payment_request WHERE status = 'PENDING' ORDER BY create_time LIMIT #{limit}")
    List<PaymentRequest> findPendingRequests(@Param("limit") int limit);

    /**
     * 分页查询待处理的请求（PENDING状态）
     */
    @Select("SELECT * FROM payment_request WHERE status = 'PENDING' ORDER BY create_time LIMIT #{limit} OFFSET #{offset}")
    List<PaymentRequest> findPendingRequestsWithOffset(@Param("limit") int limit, @Param("offset") int offset);

    /**
     * 查询处理中的请求（PROCESSING状态）
     */
    @Select("SELECT * FROM payment_request WHERE status = 'PROCESSING' ORDER BY submit_time LIMIT #{limit}")
    List<PaymentRequest> findProcessingRequests(@Param("limit") int limit);

    /**
     * 更新为处理中状态（提交第三方时）
     */
    @Update("UPDATE payment_request SET status = 'PROCESSING', batch_id = #{batchId}, submit_time = NOW() " +
            "WHERE request_id = #{requestId} AND status = 'PENDING'")
    int updateToProcessing(@Param("requestId") String requestId, @Param("batchId") String batchId);

    /**
     * 更新为成功状态
     */
    @Update("UPDATE payment_request SET status = 'SUCCESS', process_time = NOW() " +
            "WHERE request_id = #{requestId}")
    int updateToSuccess(String requestId);

    /**
     * 更新为失败状态
     */
    @Update("UPDATE payment_request SET status = 'FAILED', process_time = NOW(), error_msg = #{errorMsg} " +
            "WHERE request_id = #{requestId}")
    int updateToFailed(@Param("requestId") String requestId, @Param("errorMsg") String errorMsg);

    /**
     * 更新为超时状态
     */
    @Update("UPDATE payment_request SET status = 'TIMEOUT', process_time = NOW(), error_msg = #{errorMsg}, retry_count = retry_count + 1 " +
            "WHERE request_id = #{requestId}")
    int updateToTimeout(@Param("requestId") String requestId, @Param("errorMsg") String errorMsg);

    /**
     * 更新状态（通用）
     */
    @Update("UPDATE payment_request SET status = #{status}, process_time = #{processTime}, error_msg = #{errorMsg} " +
            "WHERE request_id = #{requestId}")
    int updateStatus(PaymentRequest request);
}
