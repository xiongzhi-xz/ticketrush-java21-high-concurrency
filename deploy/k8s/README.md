# Kubernetes/K3s 部署说明

## 镜像构建

```bash
docker build -t ghcr.io/xiongzhi-xz/ticketrush:latest .
```

本地 K3s 可以使用本地镜像名：

```bash
docker build -t ticketrush:local .
kubectl -n ticketrush set image deployment/ticketrush ticketrush=ticketrush:local
```

## 部署

```bash
kubectl apply -k deploy/k8s
```

端口转发：

```bash
kubectl -n ticketrush port-forward svc/ticketrush 8080:8080
```

健康检查：

```bash
curl http://localhost:8080/actuator/health
```

## 外部依赖

当前清单只部署 TicketRush 应用本身。以下中间件需要提前在集群内准备好，并使用对应 Service 名称：

| 组件 | Service 名 |
| --- | --- |
| MySQL | `mysql:3306` |
| Redis | `redis:6379` |
| Nacos | `nacos:8848` |
| Sentinel Dashboard | `sentinel-dashboard:8858` |
| RocketMQ NameServer | `rocketmq-namesrv:9876` |
| Seata | `seata:8091` |
| Elasticsearch | `elasticsearch:9200` |

如使用云厂商托管中间件，修改 `configmap.yaml` 和 `secret.yaml` 即可。`secret.yaml` 只保留占位值，部署前必须替换为真实数据库账号，不要把真实密码提交到仓库。
