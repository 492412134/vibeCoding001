package com.example.vibecoding001.mapper;

import com.example.vibecoding001.entity.RuleEntity;
import com.example.vibecoding001.entity.RuleHistoryEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 规则数据访问接口
 */
@Mapper
public interface RuleMapper {
    int insert(RuleEntity rule);
    int update(RuleEntity rule);
    int deleteById(String id);
    RuleEntity selectById(String id);
    List<RuleEntity> selectAll();
    List<RuleEntity> selectAllEnabled();

    /**
     * 更新规则启用状态
     * @param ruleId 规则ID
     * @param enabled 是否启用
     * @return 影响行数
     */
    int updateEnabledStatus(@Param("ruleId") String ruleId, @Param("enabled") boolean enabled);

    int insertHistory(RuleHistoryEntity history);
    List<RuleHistoryEntity> selectHistoryByRuleId(String ruleId);
    RuleHistoryEntity selectHistoryByVersion(String ruleId, Integer version);
    int rollbackToVersion(String ruleId, Integer version);
}
