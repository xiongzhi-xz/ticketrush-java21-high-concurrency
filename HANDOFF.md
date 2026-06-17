# HANDOFF - TicketRush

## 当前目标

把 TicketRush 收口为可本地运行、可压测、可面试讲解的 Java 21 高并发票务秒杀系统。

它在求职叙事中的定位：

- 证明 8 年 Java 后端基本盘：高并发、Redis、RocketMQ、MySQL、Sentinel、幂等、一致性、监控和部署。
- 作为 SmartKB/Agent 工程平台的真实 Java 项目样本，用于后续验证项目接管、代码上下文检索、任务规划和 eval 能力。

## 当前阶段

Docker Compose 全链路启动已完成，10 个容器一键运行。下一步是 k6 压测和真实数据报告。

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

1. 用 k6 对三种库存策略跑压测，产出对比报告。
2. 用 k6 跑稳定性压测（限流前后对比），产出治理效果报告。
3. 跑 Virtual Threads vs 传统线程池基准测试，出线程模型对比报告。
4. 补 Seata 分布式事务示例。
5. ES 集成（优先级最低）。

## 未验证

- k6 对三种库存策略的真实压测数据。
- 限流前后稳定性压测记录。
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
