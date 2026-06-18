# TicketRush 可观测性说明

## Prometheus

应用通过 Spring Boot Actuator 暴露 Prometheus 指标：

```text
GET /actuator/prometheus
```

Docker Compose 中的 Prometheus 会读取：

```text
docker/prometheus/prometheus.yml
```

默认抓取目标：

```yaml
job_name: ticketrush-app
metrics_path: /actuator/prometheus
targets:
  - app:8080
```

Docker Compose 全链路模式下，Spring Boot 应用同样运行在 Compose 网络中，所以 Prometheus 通过服务名 `app:8080` 抓取指标。

如果只在宿主机启动 Spring Boot、Prometheus/Grafana 仍在容器内运行，可以临时把 target 改成 `host.docker.internal:8080`。

## Grafana

Grafana 默认地址：

```text
http://localhost:3000
```

默认账号密码：

```text
admin / admin
```

项目已经提供 Grafana provisioning：

```text
docker/grafana/provisioning/datasources/prometheus.yml
docker/grafana/provisioning/dashboards/ticketrush.yml
docker/grafana/dashboards/ticketrush-overview.json
```

启动 Grafana 后会自动创建：

- Prometheus 数据源，UID 为 `prometheus`。
- `TicketRush` 文件夹。
- `TicketRush Overview` 面板。

## 当前面板

| 面板 | PromQL |
| --- | --- |
| HTTP RPS | `sum(rate(http_server_requests_seconds_count{application="ticketrush"}[1m]))` |
| HTTP p95 | `histogram_quantile(0.95, sum(rate(http_server_requests_seconds_bucket{application="ticketrush"}[5m])) by (le))` |
| Process CPU | `process_cpu_usage{application="ticketrush"}` |
| JVM Heap Used | `sum(jvm_memory_used_bytes{application="ticketrush",area="heap"})` |
| JVM Live Threads | `jvm_threads_live_threads{application="ticketrush"}` |

压测期间的 Prometheus API 导出记录见 [observability-benchmark-report.md](./observability-benchmark-report.md)。

## 启动顺序

```bash
docker compose up -d prometheus grafana
```

再启动 Spring Boot 应用，访问一次业务接口后，Grafana 面板会逐步出现数据。Docker Compose 全链路模式下可以直接使用：

```bash
docker compose up -d
```

## 注意事项

- 当前配置适合本地开发和作品演示。
- 如果应用不运行在 Docker Compose 网络内，应按实际网络位置调整 Prometheus target。
- `application.yml` 已打开 `http.server.requests` 直方图，用于支持 p95 面板。
