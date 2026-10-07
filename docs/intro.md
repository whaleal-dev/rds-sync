# 前言

> 阅读前请先了解：本仓处于 **P0 架构契约** 阶段，**尚无端到端可跑的同步链路**。产品定位与规划见本文，真实进度见 [落地状态与路线](reference/roadmap.md)。

## 为什么需要 rds-sync

关系型数据库的搬迁与容灾，长期靠「自己写个程序 select 出来再 insert 进去」，问题在于：

- **异构搬迁**：MySQL → MySQL、Oracle / PG → MySQL，列类型映射要一家一家对
- **大表切分**：单表几亿行，靠主键 Range 切段并行扫，切分策略与边界（`long` / `String` / 复合主键）都要处理
- **不停服**：全量拷几天，源端还在写，必须有增量与「全量 ∥ 增量」的能力
- **增量解析难**：binlog / redo / WAL 协议栈自研成本极高，还容易出错
- **切换要可判断**：什么时候算追平了？能不能切？需要明确的 `canCommit` 语义
- **投递下游**：很多场景不是搬到另一个库，而是把**行级变更**投到 Kafka 给下游消费
- **未来要国产化**：达梦 / TiDB / 人大金仓等，需要一个能换 Sink 的架构

rds-sync 的目标是把这些收敛成 **关系型全量切分与 JDBC「芯」 + 与 mongo-sync 同构的编排「壳」**。

| 你关心的事 | rds-sync 怎么做 |
|------------|-----------------|
| 关系库主路径 | MySQL / Oracle / PostgreSQL 全量源 + MySQL JDBC **或 Kafka** Sink |
| 异构搬迁 | 列类型转换 + 主键 Range 切分，按主键分桶有序写 |
| 投递 Kafka | `sink.type=kafka`，**行级 envelope**（`op` / `before` / `after`） |
| 不停服切换 | 规划全量 ∥ 增量（`FULL_AND_INCREMENTAL`）+ `canCommit` / `commit` 最小 cutover |
| 增量不自研协议 | binlog / redo / WAL 借 Debezium 或 Canal，适配进 `RowChange` |
| 控制面统一 | `start` / `pauseIncremental` / `progress` / `canCommit`，与 mongo-sync 同构 |
| 可嵌入 | `RdsSyncClient` 骨架，嵌入 Java 业务进程 |
| 换 Sink 不改链路 | Pipeline 只认 `RowChangeSink`，MYSQL 与 KAFKA 并列 |

如果你觉得它有价值，欢迎给仓库点 Star，也欢迎进 QQ 群交流：`983986505`。

## 核心设计取舍

| 取舍 | 选择 | 理由 |
|------|------|------|
| Sink 形态 | **MYSQL 与 KAFKA 并列**，不做双写 | 同一条任务同一时刻只有一种 `sink.type` |
| 增量解析 | **借三方**（Debezium / Canal），本仓只做适配 | 「能借就不造」；不自研 binlog/redo/WAL 协议栈 |
| 对外契约 | 只暴露 `RowChange` / `DdlEvent` | 内部 `BatchDataEntity` **不是**对外 API |
| 解析器可替换 | SPI 隔离 | 换 Canal ↔ Debezium 不改 Sink / Client |
| Kafka 的角色 | 本进程 `Producer` 写出 | **不是**再做一个 Kafka Connect 插件发行版 |
| 是否与 mongo-sync 合流 | 不做 Mongo ↔ 关系库异构直连 | 数据面本质不同，强做只会两头都不好 |

## 适用场景

- **MySQL → MySQL**：迁库、扩容、逻辑迁移
- **MySQL / Oracle / PG → Kafka**：行级变更投递，供下游消费
- **Oracle / PG → MySQL**：异构搬迁
- **国产化转型**：关系库迁到达梦 / TiDB / 人大金仓等前的结构化同步（按 Sink JDBC 能力验证）
- **与 mongo-sync 并列部署**：同一套运维习惯，分别处理关系库与文档库

## 明确的非目标

| 不做 | 说明 |
|------|------|
| 做成 Kafka Connect / Flink CDC 之类的**产品** | 本仓是嵌入式 SDK + 可选 CLI |
| 自研 binlog / redo / WAL 协议栈 | 借三方解析器 |
| MongoDB / Hadoop / HDFS 生态 | 已删除（文档库请用 [mongo-sync](https://github.com/whaleal-dev/mongo-sync)） |
| Mongo ↔ 关系库异构直连 | 无官方适配器，两个 SDK 不要串成一条链路 |
| 双写（同任务同时写 JDBC 和 Kafka） | 明确不支持 |

## 下一步

1. [落地状态与路线](reference/roadmap.md) —— 先确认现在到底能做什么
2. [概念总览](concepts/overview.md) —— 事件契约、Pipeline、双 Sink
3. [快速开始](getting-started/quickstart.md) —— 构建与当前可运行的部分
