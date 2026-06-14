# TicketRush 架构说明

## 总体架构

```mermaid
flowchart LR
    client[压测脚本 / 用户请求] --> api[Spring Boot API]
    api --> sentinel[Sentinel 全局与热点限流]
    sentinel --> admission[Redis 准入令牌]
    admission --> app[抢票应用服务]
    app --> vt[Java 21 Virtual Threads]
    vt --> inventory[库存扣减策略]
    inventory --> redis[(Redis Hash + Lua)]
    inventory --> mysql[(MySQL 乐观锁)]
    app --> mq[RocketMQ]
    mq --> order[订单消费服务]
    order --> orderdb[(MySQL 订单)]
    order --> compensation[超时关闭与库存补偿]
    compensation --> redis
    compensation --> orderdb
    api --> actuator[Actuator / Prometheus]
    actuator --> prometheus[Prometheus]
    prometheus --> grafana[Grafana]
    nacos[Nacos] -.配置 / 注册发现.-> api
    dashboard[Sentinel Dashboard] -.动态规则演示.-> sentinel
```

## 抢票主链路

```mermaid
sequenceDiagram
    participant U as User/k6
    participant C as RushTicketController
    participant S as RushTicketApplicationService
    participant SG as RushTrafficGuard
    participant AG as RushAdmissionGuard
    participant VT as VirtualThreadExecutor
    participant INV as InventoryDeductionRepository
    participant R as Redis/MySQL
    participant MQ as RocketMQ

    U->>C: POST /api/rush/tickets
    C->>S: rush(command)
    S->>SG: enter(skuId)
    SG-->>S: Sentinel permit / C0429
    S->>AG: acquire(skuId)
    AG-->>S: Redis token / C0429
    S->>VT: submit reserve task
    VT->>INV: reserve(command)
    INV->>R: atomic reserve stock
    R-->>INV: result
    INV-->>VT: InventoryDeductionResult
    VT-->>S: reserve result
    S->>MQ: publish OrderCreateMessage
    MQ-->>S: publish accepted
    S-->>C: accepted result
    C-->>U: unified ApiResponse
```

## 异步下单与补偿

```mermaid
flowchart TD
    rush[抢票成功: 锁定库存] --> publish[发送 OrderCreateMessage]
    publish --> consumer[RocketMQ Consumer]
    consumer --> idem[消费幂等检查]
    idem --> create[创建 PENDING 订单]
    create --> wait[等待支付 / 业务确认]
    wait --> timeout[OrderTimeoutCloseJob]
    timeout --> close[关闭过期 PENDING 订单]
    close --> release[释放锁定库存]
    publish -.发送失败.-> rollback[入口释放已预占库存]
    consumer -.消费失败.-> retry[RocketMQ 重试]
```

## 稳定性治理顺序

```mermaid
flowchart LR
    request[请求进入] --> global[Sentinel 全局 QPS]
    global --> hotspot[Sentinel 热点 skuId]
    hotspot --> admission[Redis 准入令牌]
    admission --> reserve[库存预占]
    reserve --> async[异步下单]
    global -.C0429.-> reject[统一限流响应]
    hotspot -.C0429.-> reject
    admission -.C0429.-> reject
```

## 分层结构

```mermaid
flowchart TB
    interfaces[interfaces<br/>Controller / Request / Response]
    application[application<br/>Use Case / Command / DTO]
    domain[domain<br/>Model / Domain Service / Repository Port]
    infrastructure[infrastructure<br/>Redis / MySQL / MQ / Sentinel Adapter]
    job[job<br/>Timeout Close / Preload / Compensation]
    common[common<br/>ApiResponse / ErrorCode / Exception / ID]
    config[config<br/>Virtual Threads / Redis Script / Scheduling]

    interfaces --> application
    application --> domain
    application --> infrastructure
    job --> application
    infrastructure --> domain
    interfaces --> common
    application --> common
    infrastructure --> config
```

## 部署视图

```mermaid
flowchart LR
    subgraph k8s[Kubernetes / K3s]
        svc[Service: ticketrush]
        pod1[Pod: ticketrush-1]
        pod2[Pod: ticketrush-2]
        cm[ConfigMap / Secret]
        svc --> pod1
        svc --> pod2
        cm --> pod1
        cm --> pod2
    end

    pod1 --> redis[(Redis)]
    pod2 --> redis
    pod1 --> mysql[(MySQL)]
    pod2 --> mysql
    pod1 --> rocketmq[RocketMQ]
    pod2 --> rocketmq
    pod1 --> nacos[Nacos]
    pod2 --> nacos
    pod1 --> sentinel[Sentinel Dashboard]
    prometheus[Prometheus] --> pod1
    prometheus --> pod2
    grafana[Grafana] --> prometheus
```

## 设计取舍

- 抢票入口优先返回“已受理”，订单创建交给 RocketMQ 异步削峰。
- 库存扣减提供 Redis Lua、Redis 锁、MySQL 乐观锁三条路径，便于压测对比。
- Sentinel 负责流量入口保护，Redis 准入令牌负责限制进入核心库存链路的并发。
- 订单超时关闭负责释放锁定库存，作为最终一致性补偿路径。
- Prometheus/Grafana 负责趋势观测，Arthas 负责现场链路诊断。
