<p align="center">
  <img src="assets/banner.svg" alt="rds-sync — 关系型数据库同步 SDK" />
</p>

# rds-sync 使用手册

> **关系型数据库同步 SDK（Java 8+）** —— MySQL / Oracle / PostgreSQL 全量源，Sink 为 MySQL JDBC 或 Kafka。

本目录是面向**接入方与运维**的使用手册。仓库根目录的 [README.md](../README.md) 做产品概览与选型，深入细节以本手册为准。

> ⚠️ **先读这一句**：本仓当前处于 **P0 架构契约** 阶段 —— 事件模型、SPI、配置装配点、Kafka 消息契约已定稿并有单测保护，但**全量 Source / MySQL Sink 尚未接到产品 SPI，`SinkFactory` 目前会抛 `UnsupportedOperationException`，还没有可跑的端到端同步**。本手册据此如实描述「已可用」与「规划中」，请勿把规划当现状。落地顺序见 [落地状态与路线](reference/roadmap.md)。

---

## 按角色选择阅读路径

| 我是谁 | 建议顺序 |
|--------|----------|
| 先搞清楚这是个什么、能不能用 | [前言](intro.md) → [落地状态与路线](reference/roadmap.md) |
| 要对齐事件契约 / 写适配器 | [概念总览](concepts/overview.md) → [SDK 与 SPI](guide/api.md) |
| 要接 Kafka 投递 | [概念总览](concepts/overview.md) → [配置项全解](guide/configuration.md) → [SDK 与 SPI](guide/api.md) 的 Kafka 契约 |
| 要评估迁移方案 / 选模式 | [前言](intro.md) → [同步模式](guide/sync-modes.md) |

---

## 章节

| 章节 | 说明 |
|------|------|
| [前言](intro.md) | 为什么做、解决什么问题、适用场景、与 mongo-sync 的分工 |
| [概念总览](concepts/overview.md) | 全量切分、事件契约、Pipeline 有序写、双 Sink、状态机 |
| [快速开始](getting-started/quickstart.md) | 构建、契约单测、当前**可运行**的部分（配置装配 / 事件构造） |
| [配置项全解](guide/configuration.md) | 全部配置键与 Builder 方法、默认值、必填规则、当前落地状态 |
| [同步模式](guide/sync-modes.md) | 四种模式与全量 / 增量衔接语义 |
| [SDK 与 SPI](guide/api.md) | `RdsSyncClient`、`RowChangeSink`、事件模型、Kafka 消息契约 |
| [运行与排障](guide/operations.md) | 状态机、进度、已知缺失、FAQ |
| [落地状态与路线](reference/roadmap.md) | P0 → P3 各阶段交付物与当前真实进度 |

---

## 本目录其他文档

| 文档 | 说明 |
|------|------|
| [ARCHITECTURE.md](ARCHITECTURE.md) | 架构契约（方案 C：Hybrid）—— 分层、Sink SPI、事件模型、控制面、落地顺序 |
| [QuickStart.md](QuickStart.md) | 早期速查（阅读顺序 / 构建 / 范围） |
| [DevDesign.md](DevDesign.md) | 早期实时迁移与全量/增量线程模型设计 |
| [RDS-Sync介绍文档.md](RDS-Sync介绍文档.md) | 产品背景与历史架构 |
| [类型转换.md](类型转换.md) | MySQL / Oracle / PG 列类型转换图 |
| [examples/](examples/) | `sink.type=mysql|kafka` 配置键示例 |
| [../README.md](../README.md) | 产品概览、与 mongo-sync 的选型对照 |

> 说明：[ARCHITECTURE.md](ARCHITECTURE.md) §8 的模块表里还列着 `realTimeOfMysql`（MySQL 增量适配层）与 `execute`（可选 CLI）—— 这两个模块**当前仓库中尚未建立目录**，属规划项；实际模块清单以根 `pom.xml` 的 `<modules>` 为准。

---

## 与 mongo-sync 的分工

两者是**姊妹产品**，控制面（`start` / `pauseIncremental` / `progress` / `canCommit` / `commit`）与 `SyncMode` 命名同构，**数据面互不替代**：

| | rds-sync（本仓） | [mongo-sync](https://github.com/whaleal-dev/mongo-sync) |
|--|------------------|--------------------------------------------------------|
| 源 | MySQL / Oracle / PostgreSQL | MongoDB（Oplog / ChangeStream） |
| Sink | MySQL JDBC、Kafka（**行级 envelope**） | MongoDB、DocumentDB / DDS、Kafka（Change Stream） |
| 事件契约 | `RowChange` / `DdlEvent` | `TransferEvent` / `DdlEvent` |
| 增量解析 | 借 Debezium / Canal，适配进本仓契约 | 自研 Oplog + ChangeStream |

两边都可以把 Kafka 当 Sink，但**消息格式不同**（行级 envelope ≠ mongo-kafka Change Stream），下游消费者不能混用。文档库同步请用 mongo-sync。

---

## 关于文档发布

本手册为纯 Markdown，链接均为仓库内相对路径。本仓文档目录即 `docs/`，可直接作为 GitHub Pages 的「Deploy from a branch → `/docs`」来源。

若后续接入 Jekyll 之类的静态站生成器，注意 `.md` 相对链接会被渲染为 `.html`，发布前需要做一次链接重写（Docusaurus 可直接消费 `.md` 链接）。

---

返回仓库首页：[README](../README.md) · QQ 交流群：**983986505**
