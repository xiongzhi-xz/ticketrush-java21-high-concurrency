# TicketRush 最终一致性与补偿说明

## 抢票到下单链路

```text
抢票请求
  -> Redis/MySQL 库存预占
  -> 发送 OrderCreateMessage
  -> RocketMQ 异步削峰
  -> 消费端创建 PENDING 订单
  -> 用户支付或订单超时关闭
```

## 一致性目标

- 不超卖：库存预占阶段必须保证同一票档不会扣出负数。
- 不重复下单：抢票请求和 RocketMQ 消费都使用同一个 `idempotentKey`。
- 不长期占库存：订单超时后关闭订单并释放锁定库存。
- 可重试：消息消费失败抛出异常，由 RocketMQ/Spring Cloud Stream 重试。

## 当前补偿策略

### 消息发送失败

库存预占成功后，如果订单创建消息发送失败：

1. 抢票入口释放已预占库存。
2. 返回服务繁忙错误。
3. 用户可以重新发起抢票请求。

### 消息重复消费

消费者创建订单前先检查 `idempotentKey`：

1. 已存在订单：直接跳过。
2. 不存在订单：创建 `PENDING` 订单。

### 订单超时关闭

定时任务批量扫描已过期的 `PENDING` 订单：

1. 原子关闭订单：`PENDING -> CLOSED`。
2. 关闭成功后释放锁定库存。
3. 如果订单已被其他线程关闭或支付，不释放库存。

## 当前边界

- 当前没有引入事务消息，订单创建消息和库存预占之间采用补偿式最终一致性。
- 如果订单关闭成功但释放库存失败，需要后续引入补偿日志或人工修复任务增强兜底。
- Seata AT 示例已补充，见 [seata-transaction-demo.md](./seata-transaction-demo.md)。该示例只包裹 MySQL 库存和订单写入，不用于回滚 Redis Lua 或 RocketMQ 消息。

## Seata 示例取舍

`SeataOrderTransactionDemoService` 演示的是同步强一致链路：

```text
@GlobalTransactional
  -> MySQL 乐观锁预占库存
  -> MySQL 创建 PENDING 订单
```

它适合解释 Seata AT 的事务边界和数据库回滚能力。主抢票链路没有直接改为 Seata，是因为热点票档更依赖 Redis 原子扣减、入口快返回和 RocketMQ 削峰；这类链路更适合通过幂等、补偿任务和监控告警做最终一致性闭环。
