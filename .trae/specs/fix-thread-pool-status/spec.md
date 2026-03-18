# 修复线程池状态显示问题 Spec

## Why
支付测试页面的线程池状态区域无法显示数据，也看不到正在执行的线程信息。这导致用户无法监控线程池的运行状态，影响系统性能观测和调试。

## What Changes
- 修复前端线程池状态数据获取和显示逻辑
- 确保后端正确返回线程池状态数据
- 修复线程可视化组件的数据绑定
- **BREAKING**: 可能需要调整前后端数据格式对齐

## Impact
- Affected specs: 支付服务测试页面实时指标监控
- Affected code: 
  - `payment-demo.html` 前端展示逻辑
  - `PaymentController.java` 后端接口
  - `PaymentAggregator.java` 线程池状态收集

## ADDED Requirements
### Requirement: 线程池状态实时显示
The system SHALL 在支付测试页面实时显示线程池状态，包括：
- 处理中线程数量
- 空闲线程数量
- 总线程数量
- 线程池队列容量
- 每个线程的状态（处理中/空闲）

#### Scenario: 成功获取线程池状态
- **WHEN** 用户打开支付测试页面并启动测试
- **THEN** 页面应每秒更新一次线程池状态
- **AND** 显示处理中线程的实时数量
- **AND** 显示空闲线程的实时数量
- **AND** 可视化展示活跃和空闲线程

#### Scenario: 线程状态可视化
- **WHEN** 线程正在处理支付请求
- **THEN** 该线程应在"处理中"区域显示
- **AND** 显示线程名称和处理的数据量

#### Scenario: 无数据处理时
- **WHEN** 线程处于空闲状态
- **THEN** 该线程应在"空闲"区域显示
- **AND** 以不同颜色区分活跃和空闲线程

## MODIFIED Requirements
### Requirement: 线程池状态接口
The system SHALL 提供 `/api/payment/threads` 接口返回：
- 线程列表（包含线程名称、状态、处理数量）
- 活跃线程数
- 总线程数
- 线程池队列信息

## REMOVED Requirements
无
