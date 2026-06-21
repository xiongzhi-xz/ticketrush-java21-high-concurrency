# AGENTS.md - TicketRush 开发协作规则

本文件约束 TicketRush 项目中的 AI 协作方式。任何开发动作开始前，必须先读取并遵守 `PROJECT.md`、`SPEC.md` 和本文件。

## 必读顺序

1. 先读 `PROJECT.md`，确认项目目标、技术栈、质量标准和核心模块。
2. 再读 `SPEC.md`，确认当前阶段、任务清单、验收标准和已完成进度。
3. 如果项目已初始化 Git，继续任务前读取最近提交记录：`git log --oneline -5`。

## 项目定位

TicketRush 是基于 Java 21 的高并发票务秒杀系统，必须达到可作为 GitHub 核心作品、项目亮点和技术说明材料的质量。

重点不是简单实现接口，而是完整展示：

- Java 21 Virtual Threads 的真实高并发落地
- 抢票、防超卖、幂等、限流、异步削峰、分布式事务
- Redis Lua、RocketMQ、Sentinel、Seata、MySQL、Elasticsearch 的工程化整合
- 压测、监控、诊断、部署、文档的完整闭环

## 开发原则

- 生成代码前必须说明技术方案、选型理由和潜在风险。
- 按阶段推进，每次只完成一个明确任务，不一次性生成整个项目。
- 优先保持结构清晰、边界明确、可测试、可讲解。
- 中文注释用于解释关键业务规则、并发控制和中间件交互，不写空洞注释。
- 不为了炫技引入无关复杂度，所有技术点都必须服务于高并发票务场景。

## 分层约定

推荐基础包名：`com.ticketrush`

```text
com.ticketrush
├─ common          # 通用响应、异常、错误码、工具、ID
├─ config          # Web、虚拟线程、Redis、MQ、Sentinel、Seata、监控配置
├─ interfaces      # Controller、Request、Response
├─ application     # 应用服务、用例编排、命令对象、DTO
├─ domain          # 领域模型、领域服务、仓储接口
├─ infrastructure  # MySQL、Redis、MQ、ES、Seata、Sentinel 适配
└─ job             # 定时任务、补偿任务、库存预热、订单关闭
```

分层规则：

- Controller 只做参数校验、认证上下文提取和响应转换。
- Application Service 负责编排流程，不直接写复杂中间件细节。
- Domain 层表达核心业务规则，如库存扣减、抢票资格、订单状态流转。
- Infrastructure 层负责具体技术实现，如 Redis Lua、MyBatis、RocketMQ、ES。
- 禁止把抢票核心逻辑全部堆进 Controller 或单个 Service。

## 质量要求

- Java 版本固定为 21。
- Spring Boot 使用 3.x。
- Virtual Threads 必须有明确配置、业务使用点和压测对比。
- 高并发核心代码必须配套测试或压测脚本。
- 每个关键技术点必须能在 README 或专题文档中说明清楚。
- 改动完成后必须运行可行的验证命令；如果环境限制导致无法运行，必须说明原因。

## 安全边界

- 不擅自删除用户文件、数据目录或配置文件。
- 不擅自修改数据库迁移或 schema 文件；如确需新增，先说明方案。
- 不擅自替换技术栈。
- 不把密钥、真实账号、个人隐私写入仓库。
- 不把本地临时产物、日志、压测大文件提交到项目。

## Git 规则

- 每个阶段或独立任务完成后单独提交。
- commit message 使用 `type: 简短描述` 格式。
- 常用类型：`feat`、`fix`、`refactor`、`docs`、`test`、`style`、`chore`。
- 不把多个无关变更混在一个 commit。
- 如果当前目录还不是 Git 仓库，先提醒用户初始化，再执行提交。

## 沟通规则

- 用户问“怎么做”时，只给方案步骤，不直接改代码。
- 用户说“帮我做”“开始”“继续”时，直接推进当前明确任务。
- 不确定就说“不确定”，先查项目文件和官方文档，不编造。
- 每次完成后说明改了什么、验证了什么、还有什么风险或下一步。
