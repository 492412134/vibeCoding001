package com.example.vibecoding001.mapper;

import com.example.vibecoding001.entity.ExternalApiConfigEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 外部API配置数据访问接口
 */
@Mapper
public interface ExternalApiConfigMapper {

    /**
     * 插入API配置
     * @param config API配置对象
     * @return 影响行数
     */
    int insert(ExternalApiConfigEntity config);

    /**
     * 更新API配置
     * @param config API配置对象
     * @return 影响行数
     */
    int update(ExternalApiConfigEntity config);

    /**
     * 根据ID删除API配置
     * @param apiId API ID
     * @return 影响行数
     */
    int deleteById(String apiId);

    /**
     * 根据ID查询API配置
     * @param apiId API ID
     * @return API配置对象
     */
    ExternalApiConfigEntity selectById(String apiId);

    /**
     * 查询所有API配置
     * @return API配置列表
     */
    List<ExternalApiConfigEntity> selectAll();

    /**
     * 查询所有启用的API配置
     * @return API配置列表
     */
    List<ExternalApiConfigEntity> selectAllEnabled();

    /**
     * 切换API启用/停用状态
     * @param apiId API ID
     * @param enabled 是否启用
     * @return 影响行数
     */
    int updateEnabledStatus(@Param("apiId") String apiId, @Param("enabled") Boolean enabled);
}
