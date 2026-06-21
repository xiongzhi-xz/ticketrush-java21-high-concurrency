# PROJECT.md - TicketRush 项目规则与目标（GPT专用）

**项目名称**：TicketRush - 基于 Java 21 的高并发票务秒杀系统

**这是我Gap期间的第二个核心项目**，用于衔接我之前在浙江深大智能做的景区票务、剧院票务、年卡等业务经验。

### 项目要达到的标准（必须严格做到）：
- 必须体现**生产级高并发实战能力**，能作为项目材料和技术交流的核心亮点
- 必须**大规模、正确地使用 Java 21 Virtual Threads**（尤其在高并发抢票、库存扣减、订单处理等IO密集和CPU密集场景）
- 必须解决真实生产问题：防超卖、幂等、限流、分布式事务、异步削峰、热点库存
- 代码必须结构清晰、分层合理、注释详细（中文）、可维护、可技术讲解
- 最终必须产出高质量文档：专业README、架构图、真实压测报告、踩坑记录

### 技术栈要求（2026主流）：
- Java 21 + Virtual Threads + Structured Concurrency（必须重点使用）
- Spring Boot 3 + Spring Cloud Alibaba（Nacos、Sentinel、Seata、RocketMQ）
- Redis（分布式锁 + Lua脚本防超卖）
- Elasticsearch + MySQL
- Docker + Kubernetes (K3s)
- Arthas + Prometheus + Grafana（至少基础监控）

### 必须完成的核心模块（要做到什么程度）：
1. 高并发抢票核心接口（Virtual Threads 大规模应用）
2. 多方案防超卖（Redis锁 + Lua脚本 + 乐观锁对比）
3. Sentinel限流、幂等设计、RocketMQ异步下单、Seata分布式事务
4. 完整性能压测报告（Virtual Threads vs 传统线程的QPS、CPU、内存、GC数据）
5. Kubernetes部署配置（至少能本地部署）
6. Arthas诊断示例 + 基础Grafana监控
7. 专业README（含架构图、技术亮点、学习总结、踩坑记录）

**开发原则**：
- 生成代码前必须先说明技术方案、选型理由和潜在风险
- 按模块分步推进，不要一次性生成整个项目
- 代码质量要达到“可直接放到GitHub作为核心作品”的水平

此文件为本项目最高优先级规则，后续所有开发必须严格遵守。
```

---

### **第2步：启动 GPT 的 Prompt（直接复制给 GPT 使用）**

在 GPT（Cursor 或 ChatGPT）中，**先输入下面这段话**作为对话开始：

```text
你现在已加载 PROJECT.md 中的所有规则和项目目标。

我们正式开始开发 **TicketRush** 项目。

请先不要直接写代码，按以下顺序回复：

1. 确认你已经完全理解本项目的目标、质量标准、必须完成的核心模块，以及要达到的水平。
2. 给出推荐的项目包结构（分层建议）。
3. 给出详细的分阶段开发计划（建议分成6-7个阶段，每个阶段明确要完成什么、产出什么）。
4. 第一阶段请先生成基础文件：pom.xml、application.yml、docker-compose.yml。

开始吧。