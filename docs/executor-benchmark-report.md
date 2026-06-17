# TicketRush Virtual Threads 执行器基准报告

## 目标

本报告用于验证 TicketRush 中 Java 21 Virtual Threads 的工程价值：在大量 I/O 等待型任务下，虚拟线程能显著降低等待任务排队时间；但它不是 CPU 加速器，CPU 密集任务仍受机器核心数和计算量限制。

测试入口：

```http
POST /api/benchmark/executors
```

接口会构造同样数量的任务，每个任务执行：

```text
Thread.sleep(blockingMillis)
consumeCpu(cpuTokens)
```

然后分别交给：

- `VIRTUAL_THREAD`：`Executors.newThreadPerTaskExecutor(Thread.ofVirtual())`
- `TRADITIONAL_THREAD_POOL`：固定平台线程池，默认 200 线程

## 环境

| 项 | 值 |
| --- | --- |
| Date | 2026-06-17 |
| Commit | `1d49f19` 后的工作区 |
| App | Docker Compose `ticketrush-app` |
| Java | `21.0.11+10-LTS` |
| Spring profile | `docker` |
| Traditional pool size | 200 |
| Base URL | `http://localhost:8080` |

测试前健康检查：

```text
GET /api/system/health
status=UP
virtualThreadsEnabled=true
currentThreadVirtual=true
```

原始响应 JSON 生成在 `target/executor-benchmark/`，该目录属于本地构建产物，不提交到 git。

## 预热

正式测试前先用 100 个 10ms 阻塞任务分别跑一次预热：

| Mode | Task count | Blocking | Elapsed | Throughput |
| --- | ---: | ---: | ---: | ---: |
| `VIRTUAL_THREAD` | 100 | 10ms | 15ms | 6,666.67 tasks/s |
| `TRADITIONAL_THREAD_POOL` | 100 | 10ms | 24ms | 4,166.67 tasks/s |

## 场景一：大量 I/O 等待任务

### 参数

```json
{
  "taskCount": 5000,
  "blockingMillis": 50,
  "cpuTokens": 0,
  "timeoutSeconds": 60
}
```

### 结果

| Mode | Elapsed | Throughput | Distinct threads | Virtual task count | Sample thread |
| --- | ---: | ---: | ---: | ---: | --- |
| `VIRTUAL_THREAD` | 56ms | 89,285.71 tasks/s | 5,000 | 5,000 | `ticketrush-vt-*` |
| `TRADITIONAL_THREAD_POOL` | 1,263ms | 3,958.83 tasks/s | 200 | 0 | `ticketrush-pt-*` |

### 结论

- 虚拟线程总耗时约为传统线程池的 `4.43%`。
- 虚拟线程吞吐约为传统线程池的 `22.55x`。
- 固定 200 平台线程池会把 5,000 个 50ms 等待任务分批执行，理论上至少需要约 25 个批次。
- 虚拟线程可以为每个任务创建轻量线程，等待期间不长期占用平台线程，因此排队时间明显减少。

## 场景二：I/O + 少量 CPU 混合任务

### 参数

```json
{
  "taskCount": 2000,
  "blockingMillis": 20,
  "cpuTokens": 50,
  "timeoutSeconds": 120
}
```

### 结果

| Mode | Elapsed | Throughput | Distinct threads | Virtual task count | Sample thread |
| --- | ---: | ---: | ---: | ---: | --- |
| `VIRTUAL_THREAD` | 83ms | 24,096.39 tasks/s | 2,000 | 2,000 | `ticketrush-vt-*` |
| `TRADITIONAL_THREAD_POOL` | 215ms | 9,302.33 tasks/s | 200 | 0 | `ticketrush-pt-*` |

### 结论

- 虚拟线程吞吐约为传统线程池的 `2.59x`。
- 加入 CPU 计算后，差距明显小于纯 I/O 等待场景。
- 这符合 Java 21 Virtual Threads 的定位：它主要改善阻塞等待和高并发连接场景的线程占用，不会让 CPU 计算本身变快。

## 面试讲法

30 秒版：

```text
我在 TicketRush 里专门做了一个执行器对比接口，同样的任务分别交给 Java 21 Virtual Threads 和固定 200 线程的平台线程池。5000 个 50ms I/O 等待任务下，虚拟线程 56ms 完成，传统线程池 1263ms 完成，吞吐约 22.55 倍；但加上 CPU token 后差距收敛到约 2.59 倍。这个结果说明虚拟线程非常适合抢票链路中的 Redis、MySQL、MQ、远程调用这类 I/O 等待，不是用来加速 CPU 密集计算的。
```

关键追问点：

- 为什么固定线程池在大量阻塞任务下会排队？
- 为什么虚拟线程等待时不会长期占用平台线程？
- 为什么 CPU 密集场景下虚拟线程优势会缩小？
- 业务里哪些环节适合用虚拟线程，哪些环节仍然需要限流、池化和背压？

## 边界

- 本报告是本地 Docker Compose 环境下的功能性 benchmark，不等价于生产压测。
- 当前接口统计的是总耗时、吞吐和线程数量，没有采集 CPU、GC、上下文切换等系统指标。
- 后续可结合 Prometheus/Grafana 或 JFR 进一步补充运行时视角。
