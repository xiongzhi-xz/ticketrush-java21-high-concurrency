# TicketRush 踩坑记录

## JDK 版本门禁

现象：

- 当前开发机是 JDK 22。
- 项目通过 Maven Enforcer 约束 Java 版本为 `[21,22)`。
- 直接执行 `mvn validate` 会失败。

原因：

- 项目目标是 Java 21 作品，必须保证最终交付不漂移到 JDK 22。

处理方式：

```powershell
mvn -q '-Denforcer.skip=true' '-Djava.version=22' test
```

后续动作：

- 在 JDK 21 环境执行 `mvn clean verify` 作为最终验收。

## schema 单独落地

现象：

- 当前已创建 `src/main/resources/schema.sql`。
- 当前已补充 MyBatis XML 和 MySQL 仓储实现。

原因：

- 数据库迁移属于安全红线文件，需要在表结构确认后单独提交，避免混入其他功能改动。

处理方式：

- schema 使用 `CREATE TABLE IF NOT EXISTS`，不包含 `DROP TABLE`。
- Spring Boot 不默认强制初始化 MySQL schema，建议本地手动执行。

后续动作：

- 使用真实 MySQL 执行 schema。
- MySQL 乐观锁、订单创建和超时关闭 SQL 集成测试已补充。

## k6 未安装

现象：

- 当前机器执行 `k6 version` 失败。
- k6 脚本已经编写，但尚未产生真实压测数据。

处理方式：

- 先用脚本语法检查和文档模板固定压测方法。
- 等安装 k6 后运行：

```powershell
k6 run scripts/k6/rush-ticket.js
k6 run scripts/k6/stability-governance.js
```

后续动作：

- 补充三种库存策略压测结果。
- 补充限流/准入开启前后的稳定性对比记录。

## MySQL 端口冲突

现象：

- 本机已有 `mysqld` 占用 `3306`。
- `docker compose up -d mysql` 会因为端口冲突启动失败。

处理方式：

```powershell
$env:TICKETRUSH_MYSQL_PORT='13306'
docker compose up -d mysql
```

验证方式：

```powershell
mvn -q `
  -Denforcer.skip=true `
  -Djava.version=22 `
  -Dticketrush.test.mysql.port=13306 `
  test
```

## Sentinel Dashboard 规则不持久化

现象：

- Dashboard 可以运行时修改规则。
- 应用重启后会恢复为 `application.yml` 中的本地规则。

原因：

- 当前演示使用 Sentinel Dashboard 的内存规则能力，尚未接入 Nacos 动态数据源持久化。

处理方式：

- 本地演示用 Dashboard 验证动态效果。
- 生产方案应迁移到 Nacos/Apollo 等动态规则源。

后续动作：

- 如需要完整生产演示，再补 Sentinel Nacos datasource 配置。

## Prometheus 容器访问宿主机应用

现象：

- Spring Boot 应用运行在宿主机。
- Prometheus 运行在 Docker Compose 容器中。

原因：

- 容器内访问 `localhost:8080` 指向的是 Prometheus 容器自身，不是宿主机应用。

处理方式：

- Prometheus target 使用 `host.docker.internal:8080`。
- Compose 中为 Linux Docker 添加：

```yaml
extra_hosts:
  - "host.docker.internal:host-gateway"
```

后续动作：

- 如果应用也放进 Compose 或 K8s，把 target 改为应用 Service 名。

## Grafana p95 面板需要直方图

现象：

- `histogram_quantile` 需要 `http_server_requests_seconds_bucket` 指标。

原因：

- Micrometer 默认不一定为 HTTP 请求开启 percentile histogram。

处理方式：

```yaml
management:
  metrics:
    distribution:
      percentiles-histogram:
        http.server.requests: true
      percentiles:
        http.server.requests: 0.5,0.95,0.99
```

后续动作：

- 压测时重点观察 p95/p99 与错误码分布的关系。

## K8s 清单只部署应用

现象：

- `deploy/k8s` 只包含 TicketRush Deployment/Service/ConfigMap/Secret。
- MySQL、Redis、RocketMQ、Nacos 等没有在同一套清单中创建。

原因：

- 有状态中间件部署方式差异大，本项目先聚焦应用交付。

处理方式：

- 本地 K3s 可复用已有中间件或用 Helm 单独部署。
- 云环境可以改 `configmap.yaml` 指向托管中间件。

后续动作：

- 如果要做完整一键环境，再单独补 `deploy/k8s-infra` 或 Helm values。
