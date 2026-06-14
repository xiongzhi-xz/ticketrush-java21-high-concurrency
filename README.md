# TicketRush

TicketRush 是一个基于 Java 21 的高并发票务秒杀系统，用于模拟演出、景区、剧院、年卡等票务场景中的高并发抢票链路。

项目目标不是做一个简单 CRUD，而是完整展示生产级高并发系统的关键能力：抢票入口、库存防超卖、幂等控制、限流降级、异步削峰、分布式事务、可观测性、压测和本地部署。

## 技术栈

- Java 21
- Spring Boot 3
- Spring Cloud Alibaba
- Virtual Threads
- Structured Concurrency
- MySQL
- Redis
- RocketMQ
- Nacos
- Sentinel
- Seata
- Elasticsearch
- Docker Compose
- Kubernetes/K3s
- Prometheus
- Grafana
- Arthas

## 核心目标

- 高并发抢票核心接口：在真实业务链路中使用 Java 21 Virtual Threads。
- 防超卖方案对比：Redis Lua、Redis 分布式锁、MySQL 乐观锁。
- 流量治理：Sentinel 限流、热点参数保护、降级兜底。
- 异步削峰：RocketMQ 解耦抢票入口和订单创建。
- 一致性保障：幂等、补偿、订单超时关闭、Seata 示例。
- 可观测性：Actuator、Prometheus、Grafana、Arthas。
- 压测报告：Virtual Threads 与传统线程池的 QPS、CPU、内存、GC 对比。

## 架构分层

```text
com.ticketrush
├─ common          # 通用响应、异常、错误码、工具、ID
├─ config          # Web、虚拟线程、Redis、MQ、Sentinel、Seata、监控配置
├─ interfaces      # Controller、Request、Response
├─ application     # 应用服务、用例编排、命令对象、DTO
├─ domain          # 领域模型、领域服务、仓储接口
├─ infrastructure  # MySQL、Redis、MQ、ES、Seata、Sentinel 适配
└─ job             # 定时任务、补偿任务、库存预热、订单关闭
```

## 本地环境

### 基础要求

- JDK 21
- Maven 3.9+
- Docker Desktop 或兼容 Docker Compose 的本地环境

当前项目使用 Java 21，并为 Structured Concurrency 预览能力保留 `--enable-preview` 编译参数。请使用 JDK 21 进行完整编译和测试。

### 启动中间件

```bash
docker compose up -d
```

本地服务端口：

| 组件 | 地址 |
| --- | --- |
| MySQL | `localhost:3306` |
| Redis | `localhost:6379` |
| Nacos | `http://localhost:8848` |
| Sentinel Dashboard | `http://localhost:8858` |
| RocketMQ NameServer | `localhost:9876` |
| Seata | `localhost:8091` |
| Elasticsearch | `http://localhost:9200` |
| Prometheus | `http://localhost:9090` |
| Grafana | `http://localhost:3000` |

### Maven 校验

```bash
mvn clean verify
```

如果当前机器不是 JDK 21，项目中的 Java 版本门禁会阻止构建。这是有意设计，用于保证项目最终围绕 Java 21 能力交付。

## 当前进度

详见 [SPEC.md](./SPEC.md)。

已完成：

- Maven 基础配置
- Spring Boot 配置文件
- Docker Compose 本地中间件环境
- Git 仓库初始化
- 基础包结构和启动类
- README 初稿
- 统一响应、错误码和全局异常处理
- 虚拟线程基础配置
- 系统健康检查接口
- 参数校验示例接口

下一步：

- 使用 JDK 21 完整验证应用启动
- 建立票务活动、票档、库存和订单领域模型
- 设计库存扣减仓储接口

## 基础接口

### 健康检查

```http
GET /api/system/health
```

返回应用名、Java 版本、虚拟线程开关、当前请求线程是否为虚拟线程等信息。

### 参数校验示例

```http
POST /api/system/validation-check
Content-Type: application/json

{
  "requestId": "bench-001",
  "scenario": "virtual-thread-health-check",
  "concurrency": 1000
}
```

该接口用于验证统一参数校验和全局异常响应格式，后续业务接口沿用同样的校验方式。

## 文档规划

后续会逐步补齐：

- 架构图
- 压测报告
- Virtual Threads 使用说明
- 防超卖方案对比
- Sentinel 限流示例
- RocketMQ 异步下单说明
- Seata 与最终一致性说明
- Arthas 诊断案例
- Kubernetes/K3s 部署说明
- 踩坑记录
