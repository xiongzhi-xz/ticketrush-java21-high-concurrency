# TicketRush Seata 分布式事务示例

## 定位

TicketRush 主抢票链路采用 Redis 库存预占 + RocketMQ 异步下单 + 超时补偿的最终一致性方案。Seata 示例用于补充说明另一种强一致建模方式：把 MySQL 库存预占和订单落库放进一个 Seata AT 全局事务。

示例代码：

```text
src/main/java/com/ticketrush/infrastructure/seata/SeataOrderTransactionDemoService.java
```

## 示例链路

```text
reserveMysqlInventoryAndCreatePendingOrder
  -> @GlobalTransactional
  -> 校验只允许 MYSQL_OPTIMISTIC_LOCK
  -> 按 idempotentKey 查询订单，重复请求直接返回已有订单
  -> ticket_inventory 乐观锁预占库存
  -> ticket_order 创建 PENDING 订单
  -> 任一步抛出异常，由 Seata 全局事务回滚 MySQL 写入
```

核心注解：

```java
@GlobalTransactional(
        name = "ticketrush-seata-mysql-rush-order",
        rollbackFor = Exception.class
)
```

## 为什么只演示 MySQL

Seata AT 模式通过代理 JDBC 连接、记录 before image / after image 和 `undo_log` 来回滚数据库资源。它适合演示 MySQL 库存和 MySQL 订单之间的强一致事务边界。

本项目没有把 Redis Lua 和 RocketMQ 放进 Seata 示例中，原因是：

- Redis 库存扣减不是 Seata AT 的数据库资源，不能靠 AT 自动回滚。
- RocketMQ 异步消息和订单消费更适合用事务消息或补偿任务建模。
- 抢票热点链路追求入口快返回，主方案采用最终一致性比同步全局事务更适合高并发场景。

## 和主链路的关系

| 方案 | 使用位置 | 优点 | 代价 |
| --- | --- | --- | --- |
| Redis/RocketMQ 最终一致性 | 主抢票链路 | 吞吐高，入口快返回，适合热点票档 | 需要补偿任务和幂等设计 |
| Seata AT 示例 | MySQL 库存 + 订单同步事务演示 | 事务边界直观，失败自动回滚数据库写入 | 同步链路更重，热点行冲突更明显 |

## 验证

单元测试：

```powershell
$env:JAVA_HOME='<your-jdk-21-home>'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
mvn -Dtest=SeataOrderTransactionDemoServiceTest test
```

已覆盖：

- 方法存在 `@GlobalTransactional`，事务名为 `ticketrush-seata-mysql-rush-order`。
- MySQL 乐观锁预占成功后创建 `PENDING` 订单。
- 重复 `idempotentKey` 直接返回已有订单，不重复扣库存。
- 非 MySQL 策略会被拒绝，避免误以为 Seata AT 能回滚 Redis/MQ。
- 订单写入异常会继续向外抛出，交给 Seata 全局事务执行回滚。

## 运行注意

真实运行 Seata AT 时，数据库需要准备 Seata `undo_log` 表，并确保应用连接到可用的 Seata Server。当前 Docker Compose 已预留 `seata` 服务和应用配置；本示例不改动业务 schema，避免把演示表和核心票务表混在一起。
