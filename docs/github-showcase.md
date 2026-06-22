# TicketRush GitHub 展示说明

这份说明用于技术读者快速判断 TicketRush 的展示重点。它不是新功能清单，也不替代 README。

## 快速看点

- Java 21 + Spring Boot 3 高并发票务秒杀主链路。
- Sentinel 全局限流、热点参数限流和 Redis 准入令牌保护入口流量。
- Redis Lua、Redis 分布式锁、MySQL 乐观锁三种防超卖策略可对比。
- Virtual Threads 用于库存预占和执行器 benchmark，能展示 IO 等待场景收益。
- RocketMQ 异步下单、消费幂等、订单超时关闭和库存释放补偿形成最终一致性闭环。
- k6、Prometheus/Grafana、Arthas、Docker Compose 和 K3s 文档已形成可验证材料。

## 推荐浏览顺序

1. [README.md](../README.md)：先看项目定位、架构图、界面截图、核心链路、快速启动和验证状态。
2. [docs/demo-runbook.md](demo-runbook.md)：看 5 分钟演示路径、CLI 替代演示和设计取舍。
3. [docs/architecture.md](architecture.md)：看抢票主链路、补偿链路和部署视图。
4. [docs/rush-benchmark-report.md](rush-benchmark-report.md)：看三种库存策略 baseline。
5. [docs/executor-benchmark-report.md](executor-benchmark-report.md)：看 Virtual Threads vs 固定线程池对比。
6. [SPEC.md](../SPEC.md)：看阶段进度、验收标准和不继续扩展的边界。

## 本地演示入口

本地演示台：

```text
http://localhost:8080/
```

推荐演示顺序：

1. 初始化库存：把热门票档库存写入 Redis。
2. 发起抢票：观察右侧结论卡片中的抢票结果、库存变化和处理线程。
3. 重复提交验证幂等：复用同一个幂等 Key，确认不会重复扣库存。
4. 运行压测：对比虚拟线程和传统固定线程池。
5. 重建活动索引 / 查询票档：演示 Elasticsearch 是读模型，不参与抢票写链路。

## 已验证证据

- `mvn test`：52 tests，0 failures，0 errors。
- Docker Compose 全链路启动：应用和核心中间件容器已验证。
- 本地演示页已在运行态访问并串联初始化库存、抢票、重复提交幂等、检索和 benchmark。
- k6 压测报告覆盖库存策略 baseline、稳定性治理 before/after 和热点分摊对比。
- Prometheus API 导出过压测指标证据。
- Seata AT 示例、Elasticsearch 查询、Redis Lua/Lock、MySQL 乐观锁、RocketMQ Stream binder、MyBatis XML/schema 均有测试或报告覆盖。

## 项目摘要

```text
TicketRush：基于 Java 21 + Spring Boot 3 的高并发票务秒杀系统，围绕抢票主链路实现 Sentinel 限流、Redis 准入令牌、Virtual Threads 库存预占、Redis Lua/分布式锁/MySQL 乐观锁防超卖、RocketMQ 异步下单、消费幂等、订单超时补偿，并补充 k6 压测、Prometheus/Grafana 指标证据和 Docker/K3s 部署材料。
```

## 边界说明

- 本地演示页用于证明核心链路，不是完整后台管理系统。
- 项目不接入真实支付、短信、实名制、多租户 SaaS 或生产订单系统。
- Seata 是 MySQL 库存预占 + 订单落库示例，主抢票链路仍采用 Redis/RocketMQ 最终一致性方案。
- Docker Compose 中的账号密码仅用于本地演示；不要提交 `.env`、真实账号、token、cookie、私钥或本地运行数据。
