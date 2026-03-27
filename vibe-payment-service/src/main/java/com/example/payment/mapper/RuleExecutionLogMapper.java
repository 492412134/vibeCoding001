package com.example.payment.mapper;

import com.example.payment.entity.RuleExecutionLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 规则执行日志数据访问接口
 */
@Mapper
public interface RuleExecutionLogMapper {

    /**
     * 插入执行日志
     * @param log 执行日志对象
     * @return 影响行数
     */
    int insert(RuleExecutionLog log);

    /**
     * 根据ID查询执行日志
     * @param id 日志ID
     * @return 执行日志对象
     */
    RuleExecutionLog selectById(Long id);

    /**
     * 查询所有执行日志
     * @return 执行日志列表
     */
    List<RuleExecutionLog> selectAll();

    /**
     * 根据规则ID查询执行日志
     * @param ruleId 规则ID
     * @return 执行日志列表
     */
    List<RuleExecutionLog> selectByRuleId(String ruleId);

    /**
     * 根据时间范围查询执行日志
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 执行日志列表
     */
    List<RuleExecutionLog> selectByTimeRange(@Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime);

    /**
     * 查询最近的执行日志
     * @param limit 限制数量
     * @return 执行日志列表
     */
    List<RuleExecutionLog> selectRecent(@Param("limit") int limit);

    /**
     * 统计规则执行次数
     * @param ruleId 规则ID
     * @return 执行次数
     */
    long countByRuleId(String ruleId);

    /**
     * 统计规则执行成功率
     * @param ruleId 规则ID
     * @return 成功率（0-100）
     */
    double calculateSuccessRate(String ruleId);

    /**
     * 删除指定时间之前的日志
     * @param beforeTime 时间
     * @return 删除行数
     */
    int deleteBefore(@Param("beforeTime") LocalDateTime beforeTime);
}
