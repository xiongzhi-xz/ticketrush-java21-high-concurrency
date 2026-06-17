# HANDOFF - TicketRush

## 当前目标

把 TicketRush 收口为可本地运行、可压测、可面试讲解的 Java 21 高并发票务秒杀系统。

它在求职叙事中的定位：

- 证明 8 年 Java 后端基本盘：高并发、Redis、RocketMQ、MySQL、Sentinel、幂等、一致性、监控和部署。
- 作为 SmartKB/Agent 工程平台的真实 Java 项目样本，用于后续验证项目接管、代码上下文检索、任务规划和 eval 能力。

## 当前阶段

Docker Compose 全链路启动、第一轮 Dockerized k6 本地压测、Virtual Threads vs 传统线程池报告、稳定性治理 before/after 对照已完成。下一步是 Prometheus/Grafana 指标视角或 Seata 示例。

## 已完成

- Java 21 + Spring Boot 3 项目骨架。
- 统一响应、错误码、全局异常处理。
- Virtual Threads 配置和执行器对比接口。
- 票务活动、票档、库存、订单领域模型。
- Redis Lua 原子扣减、防超卖和幂等。
- Redis 分布式锁扣减方案。
- MySQL 乐观锁扣减方案。
- 抢票核心接口 `/api/rush/tickets`。
- 本地库存预热接口 `/api/rush/inventory/preload`。
- RocketMQ 异步创建订单链路。
- 订单消费幂等、超时关闭、库存释放补偿。
- Sentinel 全局限流、热点参数限流、限流兜底响应。
- Redis 准入令牌和热点库存自动预热。
- k6 压测脚本和稳定性治理文档模板。
- Dockerized k6 三种库存策略低负载 baseline 报告。
- Dockerized k6 单热点票档默认治理观察记录。
- Virtual Threads vs 传统线程池执行器 benchmark 报告。
- Dockerized k6 稳定性治理 before/after 对照报告。
- Prometheus 配置、Grafana 说明、Arthas 诊断案例、Kubernetes/K3s 部署清单。
- README、架构图、数据库 schema、踩坑记录等求职展示文档。
- **Docker Compose 全链路一键启动**（app + 9 中间件）。
- **RocketMQ Broker 容器启动修复**（NPE 根因：Docker named volume 权限 → bind mount + entrypoint 脚本）。
- **application-docker.yml** profile（Docker 内部服务名覆盖）。
- **JDK 21 下 mvn clean verify 通过**（35 tests, 0 failures）。
- **Docker 全链路验证通过**：health UP、抢票成功、虚拟线程生效、Prometheus 指标可抓取。

## 本次已修改文件

- `Dockerfile` — 简化为单阶段（本地 mvn package 后挂载 JAR）
- `docker-compose.yml` — 添加 app 服务、修复 broker、清理 named volume
- `docker/prometheus/prometheus.yml` — scrape 目标改为 `app:8080`
- `docker/rocketmq/broker.conf` — 补充 storePathRootDir 等路径（参考用）
- `docker/rocketmq/broker-entrypoint.sh` — **新建**，修复 Broker NPE
- `src/main/resources/application-docker.yml` — **新建**，Docker 内部服务名
- `.gitignore` — 添加 `docker/rocketmq/store/`
- `SPEC.md` — 更新验证勾选
- `HANDOFF.md` — 更新本文件

## 本次已验证

- `mvn clean verify`：35 tests, 0 failures, 0 errors, BUILD SUCCESS (JDK 21.0.7 JBR)
- `docker compose up -d`：10/10 容器运行正常
- `/api/system/health`：UP, virtualThreadsEnabled=true, JDK 21.0.11
- `/api/rush/inventory/preload`：库存预热成功 (skuId=1001, totalStock=1000)
- `/api/rush/tickets`：抢票成功 (accepted=true, remainingStock=999, virtualThread=true)
- `/actuator/prometheus`：指标正常输出

## 下一步

1. 用 Prometheus/Grafana 补压测期间指标视角。
2. 补 Seata 分布式事务示例。
3. 做多票档 `SKU_SPREAD > 1` 的热点分摊对比。
4. ES 集成（优先级最低）。

## 未验证

- Seata 示例仍未完成。
- Elasticsearch 集成未实现。

## 风险和注意事项

- 不要提交 RocketMQ、MySQL、Redis、Elasticsearch 等本地运行数据目录。
- `docker/rocketmq/store/` 已加入 `.gitignore`，不要提交。
- 不要修改数据库 schema 或迁移文件，除非用户明确要求。
- 不要把密钥、真实账号、token、cookie、私钥或 `.env` 内容写入仓库。
- 不要为了补齐技术点继续无限扩展功能，TicketRush 当前优先级是收口验证和真实压测。

## 2026-06-17 Work Log - GitHub Safety Cleanup

Current goal:
- Make the pushed GitHub version safer as a private backup and easier to turn into a public showcase later.

Completed:
- Confirmed the working tree was clean before the safety pass.
- Confirmed `.env` is ignored.
- Reviewed tracked public-facing files for Secret/env/token/local-path indicators without printing secret values.
- Changed `deploy/k8s/secret.yaml` from demo-looking database values to explicit `CHANGE_ME_*` placeholders.
- Updated `deploy/k8s/README.md` to state that real database credentials must be filled locally and must not be committed.
- Checked unauthenticated GitHub API visibility; the repository returned 404, so it is not visible as a public repository from that view.

Modified files:
- `deploy/k8s/secret.yaml`
- `deploy/k8s/README.md`
- `HANDOFF.md`

Verified:
- `git diff --check`: only LF/CRLF warnings, no whitespace errors.
- `docker compose config --quiet`: passed.
- `npx --yes js-yaml deploy/k8s/secret.yaml deploy/k8s/kustomization.yaml deploy/k8s/deployment.yaml`: passed.

Not verified:
- Maven tests were not rerun for TicketRush because this step only changed K8s/documentation files.

Next step:
- Commit and push the safety cleanup, then continue README/showcase polishing or k6 pressure-test reporting.

## 2026-06-17 Work Log - README Showcase Polish

Current goal:
- Turn TicketRush `README.md` into a GitHub/project-showcase entry page that pairs cleanly with SmartKB.

Completed:
- Rewrote the README around the high-concurrency ticket-rush main path:
  - Sentinel guard -> Redis admission token -> Java 21 Virtual Threads -> inventory strategy -> RocketMQ async order -> timeout compensation.
- Added a Mermaid architecture diagram, project highlights, technology table, feature checklist, Docker Compose startup path, manual smoke commands, k6 entry points, API overview, verification status, project structure, interview talking points, documentation navigation, and safety notes.
- Kept Seata and Elasticsearch described as reserved/pending integration rather than completed business features.
- Marked README showcase polish complete in `SPEC.md`.

Modified files:
- `README.md`
- `SPEC.md`
- `HANDOFF.md`

Verified:
- README local link scan via PowerShell: passed (`README links ok: 21`).
- Public-safety scan for private gateways, token patterns, private keys, and local absolute paths: no matches.
- `git diff --check`: passed with no whitespace errors.
- Maven tests were not rerun because this step only changed documentation.

Next step:
- Continue with Prometheus/Grafana metric evidence or Seata example.

## 2026-06-17 Work Log - k6 Benchmark Report

Current goal:
- Produce the first real local k6 benchmark record for TicketRush.

Completed:
- Confirmed the working tree was clean before the benchmark task.
- Confirmed Docker Compose stack was already running.
- Confirmed `/api/system/health` returned `UP`, Java 21.0.11, `virtualThreadsEnabled=true`, and `currentThreadVirtual=true`.
- Used Dockerized k6 (`grafana/k6`) because k6 was not installed on the host.
- Fixed k6 scripts so expected business statuses `409/429/503` are not counted as transport failures.
- Ran low-load baseline for `REDIS_LUA`, `REDIS_LOCK`, and `MYSQL_OPTIMISTIC_LOCK`.
- Seeded local Docker MySQL with benchmark event/sku/inventory rows for the MySQL optimistic-lock strategy.
- Ran guarded hot-sku stability observation with `VUS=30`, `DURATION=20s`, `SKU_SPREAD=1`.
- Added `docs/rush-benchmark-report.md` and updated `docs/stability-benchmark.md`.
- Updated README and SPEC to reflect the new benchmark report.

Key results:
- `REDIS_LUA`: 45.40 req/s, p95 4.09ms, 0.00% HTTP failed.
- `REDIS_LOCK`: 44.26 req/s, p95 5.72ms, 0.00% HTTP failed.
- `MYSQL_OPTIMISTIC_LOCK`: 39.15 req/s, p95 7.98ms, 0.00% HTTP failed, one 3.9s max-latency outlier.
- Guarded hot-sku run: 833,629 requests, 35,765.45 req/s, 2,023 accepted, 831,605 rate-limited, 0.00% unexpected responses.

Modified files:
- `README.md`
- `SPEC.md`
- `HANDOFF.md`
- `docs/rush-benchmark-report.md`
- `docs/stability-benchmark.md`
- `scripts/k6/rush-ticket.js`
- `scripts/k6/stability-governance.js`

Verified:
- Dockerized k6 baseline runs passed thresholds for all three inventory strategies.
- Dockerized k6 guarded hot-sku run passed thresholds with 0.00% unexpected responses.
- Raw k6 summary JSON was generated under `target/k6/` and was not intended for git.

Not verified:
- Stability before/after comparison with governance disabled or threshold-tuned.
- Virtual Threads vs traditional thread pool benchmark report.
- Prometheus/Grafana metric screenshots or exports during the k6 runs.

Next step:
- Continue with Prometheus/Grafana metric evidence or Seata example.

## 2026-06-17 Work Log - Governance Comparison Report

Current goal:
- Produce a stability-governance before/after comparison report.

Completed:
- Confirmed the working tree was clean before the task.
- Read Sentinel and Redis admission guard implementations.
- Confirmed Sentinel disabled mode means rules are not loaded, while the guard still allows traffic.
- Confirmed Redis admission can be disabled via `TICKETRUSH_RUSH_ADMISSION_ENABLED=false`.
- Started a temporary `ticketrush-app-noguard` container on the same Docker network with:
  - `TICKETRUSH_SENTINEL_ENABLED=false`
  - `TICKETRUSH_RUSH_ADMISSION_ENABLED=false`
- Ran default protected hot-sku k6 comparison against `http://app:8080`.
- Ran no-guard hot-sku k6 comparison against `http://ticketrush-app-noguard:8080`.
- Removed the temporary `ticketrush-app-noguard` container after the run.
- Added `docs/governance-comparison-report.md`.
- Updated README, SPEC, HANDOFF, and `docs/stability-benchmark.md`.

Key results:
- Protected: 8,708 requests, 741.53 req/s, 1,197 accepted, 7,510 rate-limited, 86.25% rate-limit ratio, p95 3.21ms, max 16.52ms.
- No Guard: 7,502 requests, 724.48 req/s, 7,501 accepted, 0 rate-limited, p95 4.94ms, max 350.89ms.
- Both runs had 0.00% unexpected responses and 0.00% HTTP failed.

Modified files:
- `README.md`
- `SPEC.md`
- `HANDOFF.md`
- `docs/governance-comparison-report.md`
- `docs/stability-benchmark.md`

Verified:
- Dockerized k6 protected and no-guard runs passed thresholds.
- Main `ticketrush-app` health remained `UP` after the comparison.
- Raw k6 summary JSON was generated under `target/governance-comparison/` and was not intended for git.

Not verified:
- Prometheus/Grafana metric screenshots or exports during the comparison.
- Multi-SKU spread comparison with `SKU_SPREAD > 1`.

Next step:
- Add Prometheus/Grafana metric evidence or move to the Seata example.

## 2026-06-17 Work Log - Executor Benchmark Report

Current goal:
- Produce the Virtual Threads vs traditional thread pool benchmark report for TicketRush.

Completed:
- Confirmed the working tree was clean before the benchmark task.
- Read the benchmark controller/service/request/response implementation.
- Confirmed traditional benchmark executor is a fixed 200-platform-thread pool.
- Confirmed `/api/system/health` returned `UP`, Java 21.0.11, `virtualThreadsEnabled=true`, and `currentThreadVirtual=true`.
- Ran warm-up calls for both executor modes.
- Ran pure I/O waiting benchmark: 5,000 tasks, 50ms blocking, 0 CPU tokens.
- Ran mixed benchmark: 2,000 tasks, 20ms blocking, 50 CPU tokens.
- Added `docs/executor-benchmark-report.md`.
- Updated README, SPEC, HANDOFF, and `docs/rush-benchmark-report.md`.

Key results:
- Pure I/O waiting: `VIRTUAL_THREAD` finished in 56ms at 89,285.71 tasks/s; `TRADITIONAL_THREAD_POOL` finished in 1,263ms at 3,958.83 tasks/s. Virtual-thread throughput was about 22.55x.
- Mixed I/O + CPU: `VIRTUAL_THREAD` finished in 83ms at 24,096.39 tasks/s; `TRADITIONAL_THREAD_POOL` finished in 215ms at 9,302.33 tasks/s. Virtual-thread throughput was about 2.59x.
- Virtual-thread runs used one virtual thread per task; traditional runs used 200 platform threads and queued the rest.

Modified files:
- `README.md`
- `SPEC.md`
- `HANDOFF.md`
- `docs/executor-benchmark-report.md`
- `docs/rush-benchmark-report.md`

Verified:
- `/api/benchmark/executors` returned successful responses for all four official benchmark runs.
- Raw benchmark JSON was generated under `target/executor-benchmark/` and was not intended for git.

Not verified:
- Prometheus/Grafana metric screenshots or exports during the executor benchmark.
- Stability multi-SKU spread comparison with `SKU_SPREAD > 1`.

Next step:
- Continue with Prometheus/Grafana metric evidence or Seata example.

## 接管开场模板

新窗口或换模型时，先执行：

```powershell
Get-Content -Raw HANDOFF.md
Get-Content -Raw PROJECT.md
Get-Content -Raw SPEC.md
git status --short
git log --oneline -5
```

然后先输出：

```text
当前目标：
当前阶段：
已完成：
未完成：
工作区是否有未提交改动：
我下一步只做：
```
