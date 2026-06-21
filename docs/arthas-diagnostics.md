# Arthas 诊断案例

## 目标

在压测或演示时，用 Arthas 快速定位抢票链路的耗时点和异常点，重点关注：

- `/api/rush/tickets` 入口耗时是否集中在应用编排、Redis 扣减或消息发送。
- Redis Lua、Redis 锁、MySQL 乐观锁三种策略的耗时差异。
- Sentinel/准入门拦截后是否没有继续进入库存扣减链路。
- 虚拟线程执行器是否正常承接库存预占任务。

## 启动 Arthas

```bash
java -jar arthas-boot.jar
```

选择 TicketRush 进程后进入 Arthas Console。

常用基础命令：

```text
dashboard
jvm
thread -n 20
sysprop java.version
logger
```

## 观察抢票入口

统计抢票应用服务调用频率和平均耗时：

```text
monitor -c 5 com.ticketrush.application.service.RushTicketApplicationService rush
```

查看慢请求调用栈：

```text
trace com.ticketrush.application.service.RushTicketApplicationService rush '#cost > 100'
```

观察返回值、异常和入参：

```text
watch com.ticketrush.application.service.RushTicketApplicationService rush '{params, returnObj, throwExp}' -x 2
```

预期：

- Sentinel 或 Redis 准入门触发时，`throwExp` 为 `BusinessException`，错误码是 `C0429`。
- 库存不足时，错误码是 `B0401`。
- 正常受理时，返回值中 `processedByVirtualThread=true`。

## 观察 Redis Lua 扣减

追踪 Redis Lua 策略耗时：

```text
trace com.ticketrush.infrastructure.redis.RedisLuaTicketInventoryRepository reserve '#cost > 50'
```

观察 Redis Lua 返回结果：

```text
watch com.ticketrush.infrastructure.redis.RedisLuaTicketInventoryRepository reserve '{params, returnObj, throwExp}' -x 2
```

预期：

- 正常扣减返回 `InventoryDeductionResult.success=true`。
- 重复请求返回 `A0429` 映射前的失败结果，消息为“重复请求”。
- 未预热库存或扣减失败会被应用层映射为 `B0402`。

## 观察 Redis 准入门

确认准入门是否在库存扣减前生效：

```text
trace com.ticketrush.infrastructure.redis.RedisRushAdmissionGuard acquire '#cost > 20'
```

```text
watch com.ticketrush.infrastructure.redis.RedisRushAdmissionGuard acquire '{params, returnObj, throwExp}' -x 2
```

压测时如果 `RushAdmissionGuard.acquire` 抛出 `C0429`，同时 `RedisLuaTicketInventoryRepository.reserve` 调用次数没有同步上涨，说明准入门成功挡住了核心链路压力。

## 观察消息发送

抢票成功后会发送 RocketMQ 创建订单消息：

```text
trace com.ticketrush.infrastructure.mq.RocketMqOrderCreateMessagePublisher publish '#cost > 100'
```

```text
watch com.ticketrush.infrastructure.mq.RocketMqOrderCreateMessagePublisher publish '{params, returnObj, throwExp}' -x 2
```

预期：

- 返回 `true` 表示消息已交给 Spring Cloud Stream。
- 返回 `false` 或抛异常时，应用服务会释放已预占库存并返回 `C0503`。

## 观察订单消费

```text
monitor -c 5 com.ticketrush.application.service.OrderApplicationService createOrder
```

```text
trace com.ticketrush.application.service.OrderApplicationService createOrder '#cost > 100'
```

预期：

- 重复消息不会重复创建订单。
- 消费失败会抛出异常交给 RocketMQ/Spring Cloud Stream 重试。

## 记录模板

| 日期 | Commit | 场景 | 命令 | 现象 | 判断 | 后续动作 |
| --- | --- | --- | --- | --- | --- | --- |
| 待补充 | 待补充 | 待补充 | 待补充 | 待补充 | 待补充 | 待补充 |

## 技术讲解点

- 先用 Prometheus/Grafana 判断系统是否有异常趋势，再用 Arthas 对单点链路做现场诊断。
- `trace` 用于看链路耗时分布，`watch` 用于看参数、返回值和异常。
- 对高并发抢票系统，优先确认限流和准入是否挡在库存扣减前，再继续看 Redis、MQ、数据库。
