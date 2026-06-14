# TicketRush 数据库落地说明

## 文件

数据库 schema 位于：

```text
src/main/resources/schema.sql
```

MyBatis XML 位于：

```text
src/main/resources/mapper/TicketEventMapper.xml
src/main/resources/mapper/TicketSkuMapper.xml
src/main/resources/mapper/TicketInventoryMapper.xml
src/main/resources/mapper/TicketOrderMapper.xml
```

当前 schema 不配置自动执行，建议在本地 MySQL 初始化后手动执行：

```bash
mysql -h127.0.0.1 -P3306 -uticketrush -pticketrush ticketrush < src/main/resources/schema.sql
```

如果本机 `3306` 已被占用，可以用项目 Compose 的可配置端口启动 MySQL：

```powershell
$env:TICKETRUSH_MYSQL_PORT='13306'
docker compose up -d mysql
```

集成测试连接该端口：

```powershell
mvn -q `
  -Denforcer.skip=true `
  -Djava.version=22 `
  -Dticketrush.test.mysql.port=13306 `
  test
```

## 表

| 表 | 说明 |
| --- | --- |
| `ticket_event` | 活动/场次，控制整体售卖窗口 |
| `ticket_sku` | 票档，控制价格、票档售卖窗口和总库存 |
| `ticket_inventory` | 票档库存，维护可售、锁定、已售三段库存 |
| `ticket_order` | 订单，维护幂等键、订单状态和超时关闭字段 |

## 关键约束

- `ticket_inventory.sku_id` 为主键，和票档一一对应。
- `ticket_inventory` 使用 `CHECK (total_stock = available_stock + locked_stock + sold_stock)` 保证库存守恒。
- `ticket_order.order_no` 唯一。
- `ticket_order.idempotent_key` 唯一，用于消费幂等和重复请求保护。
- 状态字段使用枚举名字符串，便于 MyBatis 映射和线上排查。

## 关键索引

| 索引 | 用途 |
| --- | --- |
| `idx_ticket_event_sale_status` | 按状态和售卖窗口筛活动 |
| `idx_ticket_sku_event_id` | 查询活动下所有票档 |
| `idx_ticket_sku_sale_status` | 按状态和售卖窗口筛票档 |
| `idx_ticket_order_status_expire_id` | 扫描过期 `PENDING` 订单 |
| `idx_ticket_order_user_created` | 用户订单列表 |
| `idx_ticket_order_sku_created` | 票档维度订单排查 |

## MySQL 乐观锁库存扣减

`TicketInventoryMapper.reserveByOptimisticLock` 使用条件更新：

```sql
WHERE sku_id = ?
  AND version = ?
  AND available_stock >= ?
```

受影响行数为 `1` 才代表扣减成功；否则视为版本冲突或库存不足。

## 后续验证

- 使用真实 MySQL 执行 `schema.sql`。
- MySQL 乐观锁库存扣减集成测试已覆盖。
- 订单创建、过期订单扫描和超时关闭 SQL 集成测试已覆盖。
- 后续继续跑抢票入口端到端验证：预热库存 -> 抢票 -> RocketMQ 消费创建订单 -> 超时关闭释放库存。
