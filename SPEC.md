# SPEC.md - TicketRush 项目执行规格

## 1. 项目目标

TicketRush 是一个基于 Java 21 的高并发票务秒杀系统，用于展示生产级高并发架构能力，并衔接景区票务、剧院票务、年卡等真实业务经验。

最终项目必须达到：

- 可本地运行核心链路
- 可解释高并发抢票完整架构
- 可验证防超卖、幂等、限流、异步削峰、分布式事务
- 可输出 Virtual Threads 与传统线程模型的压测对比
- 可提供 Docker/Kubernetes 部署方案
- 可作为 GitHub 作品和面试项目讲解

## 2. 技术栈

- JDK：Java 21
- 并发模型：Virtual Threads、Structured Concurrency
- Web 框架：Spring Boot 3
- 微服务组件：Spring Cloud Alibaba、Nacos、Sentinel、Seata、RocketMQ
- 存储：MySQL、Redis、Elasticsearch
- 观测：Spring Boot Actuator、Prometheus、Grafana、Arthas
- 部署：Docker Compose、Kubernetes/K3s
- 构建：Maven

## 3. 推荐包结构

```text
src/main/java/com/ticketrush
├─ TicketRushApplication.java
├─ common
│  ├─ api
│  ├─ exception
│  ├─ id
│  └─ utils
├─ config
├─ interfaces
│  ├─ controller
│  ├─ request
│  └─ response
├─ application
│  ├─ service
│  ├─ command
│  └─ dto
├─ domain
│  ├─ model
│  ├─ service
│  └─ repository
├─ infrastructure
│  ├─ mysql
│  ├─ redis
│  ├─ mq
│  ├─ elasticsearch
│  ├─ seata
│  └─ sentinel
└─ job
```

## 4. 核心业务模块

- 活动与票档管理：演出/景区/活动、票档、库存、售卖时间。
- 抢票入口：请求校验、限流、幂等、库存预扣、下单请求入队。
- 库存系统：Redis Lua 原子扣减、Redis 分布式锁、MySQL 乐观锁对比。
- 订单系统：异步创建订单、订单状态流转、超时关闭、库存回滚。
- 消息系统：RocketMQ 削峰、消费幂等、失败重试、死信处理。
- 分布式事务：Seata 示例和最终一致性方案说明。
- 检索系统：Elasticsearch 查询活动和票档。
- 稳定性治理：Sentinel 限流、热点参数保护、降级策略。
- 观测诊断：Prometheus 指标、Grafana 面板、Arthas 诊断案例。
- 压测体系：Virtual Threads vs 传统线程池对比报告。

## 5. 暂不做内容

- 暂不做完整前端后台管理系统。
- 暂不接入真实支付渠道。
- 暂不实现真实短信、实名制、身份证校验接口。
- 暂不做多租户 SaaS 化。
- 暂不拆成多个独立微服务仓库，先以单体工程完成高质量核心链路。

## 6. 分阶段计划

### 阶段 1：工程初始化与本地基础设施

目标：建立 Java 21 + Spring Boot 3 项目基础，准备本地中间件环境。

产出：

- [x] `pom.xml`
- [x] `src/main/resources/application.yml`
- [x] `docker-compose.yml`
- [x] Git 仓库初始化
- [x] `.gitignore`
- [x] 基础启动类
- [x] README 初稿

验收标准：

- Maven 依赖可解析
- Docker Compose 配置可解析
- 项目明确 Java 21 约束
- 配置中预留 Virtual Threads、MySQL、Redis、Nacos、Sentinel、RocketMQ、Seata、ES、Prometheus、Grafana

当前状态：

- 已完成基础配置文件、Git 初始化、基础启动类和 README 初稿。
- 当前机器使用 JDK 22，严格 `mvn validate` 会被 Java 21 门禁拦截；切换 JDK 21 后再执行完整校验。

### 阶段 2：基础骨架与通用能力

目标：建立可启动的 Spring Boot 工程骨架。

产出：

- [x] `TicketRushApplication`
- [x] 统一响应对象
- [x] 统一错误码
- [x] 全局异常处理
- [x] 参数校验示例
- [x] 基础健康检查接口
- [x] 虚拟线程基础配置类

验收标准：

- 应用可启动
- `/actuator/health` 可访问
- 通用异常和响应格式统一
- 虚拟线程配置有明确代码入口

当前状态：

- 已完成统一响应、错误码、业务异常和全局异常处理。
- 已提供 `/api/system/health` 用于检查应用、Java 版本和当前请求线程类型。
- 已提供 `/api/system/validation-check` 用于验证参数校验错误响应。
- 当前机器使用 JDK 22，完整编译和启动验证需要切换 JDK 21 后执行。

### 阶段 3：领域模型与库存基础链路

目标：建立票务核心领域模型和库存扣减基础能力。

产出：

- [x] 活动模型
- [x] 票档模型
- [x] 库存模型
- [x] 订单模型
- [x] 仓储接口
- [x] MyBatis Mapper
- [x] Redis 库存缓存结构
- [x] 库存领域服务
- [x] 库存领域单元测试

验收标准：

- 核心实体边界清晰
- 库存扣减接口可测试
- 支持后续 Redis Lua、分布式锁、乐观锁三种方案对比

当前状态：

- 已完成 `TicketEvent`、`TicketSku`、`TicketInventory`、`TicketOrder` 等核心领域模型。
- `TicketInventory` 已内置库存守恒校验：总库存 = 可售库存 + 锁定库存 + 已售库存。
- 已定义 `InventoryDeductionStrategy`，后续用于 Redis Lua、Redis 锁、MySQL 乐观锁压测对比。
- 已完成领域仓储接口、MyBatis Mapper 边界和 Redis 库存 Hash/Lua 脚本结构。
- 已添加库存预占领域测试，验证成功预占、库存不足和库存守恒异常。
- 暂未创建数据库 schema/migration 文件，后续需要单独确认表结构后再落地。

### 阶段 4：高并发抢票核心接口

目标：完成抢票主链路，并真实使用 Virtual Threads。

产出：

- [x] 抢票 API
- [x] 请求幂等校验
- [x] Redis Lua 原子扣减
- [x] Redis 分布式锁扣减方案
- [x] MySQL 乐观锁扣减方案
- [x] Virtual Threads 执行器
- [x] Virtual Threads 对比入口
- [x] 单元测试
- [x] Redis 库存预热接口
- [x] 第一版抢票 k6 压测脚本

验收标准：

- 高并发下不超卖
- 重复请求不会重复下单
- 能解释三种库存方案优缺点
- Virtual Threads 使用点明确，不只是配置开关

当前状态：

- 已完成 `/api/rush/tickets` 抢票接口。
- 已完成 `/api/rush/inventory/preload` 本地库存预热接口。
- 抢票链路当前为：Controller 参数校验 -> Application Service -> Virtual Thread -> Redis Lua -> Redis Hash 库存预占。
- Redis Lua 原子完成库存检查、库存预占、版本递增和幂等 Key 写入。
- Redis 分布式锁方案通过票档粒度 `SET NX` 锁保护库存 Hash 读写，并使用 Lua 校验 token 后释放锁。
- MySQL 乐观锁方案通过 `version` 和 `available_stock >= quantity` 条件更新防止并发超卖。
- 抢票请求支持通过 `strategy` 选择 `REDIS_LUA`、`REDIS_LOCK`、`MYSQL_OPTIMISTIC_LOCK`，默认使用 `REDIS_LUA`。
- 应用服务会将重复请求映射为 `A0429`，库存不足映射为 `B0401`，未预热库存映射为 `B0402`。
- 已添加应用服务和策略适配器单元测试，验证虚拟线程执行、策略选择、幂等冲突、库存预热、Redis 锁扣减和 MySQL 乐观锁扣减。
- MySQL 乐观锁方案目前完成代码路径和 Mapper 边界，真实集成测试需要先确认 schema 和 SQL。
- 已完成 `/api/benchmark/executors`，用于对比虚拟线程和传统固定线程池在同样阻塞任务下的耗时、吞吐和线程数量。
- 已完成第一版 k6 抢票压测脚本：`scripts/k6/rush-ticket.js`。
- 当前机器未安装 k6，脚本尚未执行真实压测。

### 阶段 5：异步削峰与订单最终一致性

目标：通过 RocketMQ 把抢票入口和订单创建解耦。

产出：

- [x] 抢票成功消息发送
- [x] RocketMQ 订单消费者
- [x] 消费幂等
- [x] 失败重试
- [ ] 订单超时关闭任务
- [ ] 库存回滚补偿
- [ ] Seata 示例或最终一致性说明

验收标准：

- 抢票入口快速返回
- 订单异步创建
- 消息重复消费不产生重复订单
- 失败场景有补偿路径

当前状态：

- 抢票成功后会发布 `OrderCreateMessage` 到 RocketMQ，抢票入口快速返回。
- 已通过 Spring Cloud Stream `StreamBridge` 发送订单创建消息。
- 已提供 `orderCreateConsumer` 函数式消费者，消费端调用 `OrderApplicationService` 创建订单。
- 消费幂等基于 `idempotentKey`，重复消息不会重复创建订单。
- 消费失败会抛出异常交给 RocketMQ/Spring Cloud Stream 重试，当前配置 `max-attempts=3`。
- 如果抢票入口库存预占成功但消息发送失败，会释放已预占库存。
- 订单当前创建为 `PENDING`，库存仍保持锁定；订单超时关闭释放库存尚未实现。
- MyBatis 订单仓储实现已完成，真实落库还需要确认 schema 和 SQL。

### 阶段 6：限流、热点保护与稳定性治理

目标：补齐生产级流量治理能力。

产出：

- [ ] Sentinel 限流规则
- [ ] 热点参数限流
- [ ] 降级和兜底响应
- [ ] Redis 热点库存预热
- [ ] 请求令牌或排队策略
- [ ] 稳定性测试记录

验收标准：

- 高并发流量下接口不会被直接打穿
- 热门票档有单独保护策略
- 限流和降级行为可演示

### 阶段 7：压测、监控、部署与文档

目标：形成作品级闭环。

产出：

- [ ] JMeter 或 k6 压测脚本
- [ ] Virtual Threads vs 传统线程池压测报告
- [ ] Prometheus 配置
- [ ] Grafana 面板说明
- [ ] Arthas 诊断案例
- [ ] Kubernetes/K3s 部署配置
- [ ] 专业 README
- [ ] 架构图
- [ ] 踩坑记录

验收标准：

- 有真实压测数据
- 有可复现部署步骤
- 有清晰架构说明
- 面试时能围绕项目讲清楚技术选择、性能收益和风险取舍

## 7. 下一步任务

跨阶段遗留验证：

- [ ] 使用 JDK 21 跑一次完整 Maven 校验

阶段 2 后续验证：

- [ ] 使用 JDK 21 执行 `mvn clean verify`
- [ ] 启动应用并访问 `/api/system/health`
- [ ] 调用 `/api/system/validation-check` 验证参数错误响应

阶段 3 后续任务：

- [ ] 确认数据库表结构后创建 schema
- [ ] 实现 MyBatis XML 或注解 SQL
- [x] 实现 Redis Lua 库存扣减适配器
- [x] 实现 MySQL 乐观锁库存扣减适配器

阶段 4 后续任务：

- [ ] 使用 Redis 运行集成测试验证 Lua 原子扣减
- [ ] 使用 Redis 运行集成测试验证分布式锁扣减
- [ ] 确认 schema 后运行 MySQL 乐观锁集成测试
- [x] 增加传统线程池与虚拟线程对比入口
- [x] 编写第一版抢票压测脚本
- [ ] 使用 k6 对三种库存策略跑第一轮本地压测

阶段 5 后续任务：

- [ ] 确认订单表结构并补充 MyBatis SQL
- [ ] 实现订单超时关闭任务
- [ ] 超时关闭时释放锁定库存
- [ ] 补充 RocketMQ 集成测试
- [ ] 编写最终一致性和失败补偿说明
