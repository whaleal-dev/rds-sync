# rds-sync

**关系型数据库同步 SDK**（Java 8+）

**主路径：MySQL / Oracle / PostgreSQL → MySQL 或 Kafka**。  
面向异构搬迁、灾备、变更投递：控制面与 [mongo-sync](https://github.com/whaleal-dev/mongo-sync) 对齐；数据面只认行级 `RowChange` / `DdlEvent`；**Pipeline 只认 `RowChangeSink`**，MYSQL 与 KAFKA 并列。

本仓**不**包含 MongoDB / Hadoop / HDFS。文档库同步请用 [mongo-sync](https://github.com/whaleal-dev/mongo-sync)。

```bash
mvn -DskipTests package
```

**QQ 交流群：`983986505`**（使用问题、需求反馈、经验交流欢迎加群）

---

## 为什么选 rds-sync

| 诉求 | rds-sync 怎么做 |
|------|-----------------|
| 关系库主路径 | MySQL / Oracle / PostgreSQL 全量源 + MySQL JDBC **或 Kafka** Sink |
| 异构搬迁 | 列类型转换与主键 Range 切分，按主键分桶有序写 |
| 投递 Kafka | `sink.type=kafka`，行级 envelope（`op`/`before`/`after`），与 mongo-kafka Change Stream 格式区分 |
| 不停服切换 | 规划全量∥增量（`FULL_AND_INCREMENTAL`），`canCommit` / `commit` 最小 cutover |
| 增量不自研协议 | binlog / redo / WAL 借 Debezium 或 Canal，适配进 `RowChange` |
| 控制面统一 | `start` / `pauseIncremental` / `progress` / `canCommit`，与 mongo-sync 同构 |
| 可嵌入 | `RdsSyncClient` 骨架，嵌入 Java 业务进程 |

Sink **不感知** 上游是 JDBC 快照还是 CDC——统一变成 `RowChange` / `DdlEvent`，再交给 MYSQL 或 KAFKA Sink。

---

## 和 mongo-sync 怎么选

二者是姊妹产品，控制面对齐，**数据面互不替代**，也没有官方 Mongo ↔ MySQL 直连适配。

| | [rds-sync](https://github.com/whaleal-dev/rds-sync)（本仓） | [mongo-sync](https://github.com/whaleal-dev/mongo-sync) |
|--|------|-----------|
| 源 | MySQL / Oracle / PostgreSQL | MongoDB（Oplog / ChangeStream） |
| Sink | MySQL JDBC、Kafka（行级 envelope） | MongoDB、DocumentDB / DDS、Kafka（Change Stream） |
| 事件契约 | `RowChange` / `DdlEvent` | `TransferEvent` / `DdlEvent` |
| 增量解析 | 借 Debezium / Canal，适配进本仓契约 | 自研 Oplog + ChangeStream |
| 形态 | 嵌入式 SDK + 可选 CLI | 已可脚本启动（`mongosync.sh` / `verify.sh`） |

文档库（MongoDB → MongoDB / mongo-kafka 格式 Kafka）请用 **[mongo-sync](https://github.com/whaleal-dev/mongo-sync)**。关系库投递 Kafka 走本仓。

---

## 核心能力

- **四种同步模式**（契约已定）：仅全量、全量∥持续增量、全量后追平再停、仅增量  
- **双 Sink 形态**：MYSQL（JDBC，默认）/ KAFKA（行级 envelope；架构已定，实现见 P1b）  
- **全量源**：MySQL / Oracle / PostgreSQL（JDBC + Range 切分）  
- **事件契约**：`RowChange` / `DdlEvent`；Pipeline 只依赖 `RowChangeSink`  
- **控制面骨架**：`RdsSyncClient` 对齐 mongo-sync 状态机  
- **增量（规划）**：三方解析 → 适配器 → `RowChange`；换 Canal / Debezium 不改 Sink  

当前落地进度见 [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) §10：**P0 架构契约**；全量 Source / MYSQL Sink **尚未**接到产品 SPI。

---

## 同步模式（`SyncMode`）

| 枚举 | 行为 |
|------|------|
| `FULL` | 仅全量 |
| `FULL_AND_INCREMENTAL` | 全量∥增量并行，全量结束后持续增量 |
| `FULL_THEN_CATCH_UP` | 先全量，再追增量窗口，追平后停止（串行） |
| `INCREMENTAL` | 仅增量 |

命名与 [mongo-sync](https://github.com/whaleal-dev/mongo-sync) 一致：`AND` 表示全量与增量并行并持续；`THEN` 表示先全量、再追平、再停。

---

## 模块

| 模块 | 说明 |
|------|------|
| `rds-transfer-model` | `RowChange` / `DdlEvent` / SPI（`SnapshotSource` / `RowChangePipeline` / `RowChangeSink`） |
| `rds-common` / `rds-core` | 列类型、切分、JDBC（对内，未接产品链路） |
| `rds-mysql-source` / `rds-oracle-source` / `rds-pg-source` | 全量 Source；**尚未**适配 SPI |
| `rds-mysql-sink` | MySQL JDBC Sink；**尚未**适配 SPI |
| `rds-kafka-sink` | Topic / 行级 envelope 契约；Producer 待 P1b |
| `realTimeOfMysql` | MySQL 增量适配层（空壳） |
| `rds-sync-client` | `RdsSyncConfig` + `SinkFactory` + 控制面骨架 |

---

## 构建

```bash
cd rds-sync
mvn -DskipTests package
```

上手说明：[docs/QuickStart.md](docs/QuickStart.md)。

---

## 常见问题（FAQ）

### 支持哪些数据库？

源端规划 MySQL / Oracle / PostgreSQL。Sink：**MySQL JDBC** 或 **Kafka**。MongoDB / DocumentDB 请用 [mongo-sync](https://github.com/whaleal-dev/mongo-sync)（其 Kafka 消息是 Change Stream，与本仓行级 envelope 不同）。

### 现有 Source / Sink 模块能直接当 SDK 用吗？

不能当产品链路用。全量 JDBC 仍走内部 `BatchDataEntity` + `MemoryCache`，还没适配 `SnapshotSource` / `RowChangeSink`。见 [架构 §8.1](docs/ARCHITECTURE.md)。

### 增量自己解析 binlog 吗？

不自研协议栈。MySQL 增量借 Debezium Embedded 或 Canal，经适配器输出 `RowChange`。

### 支持 Kafka 吗？

架构已定：`sink.type=kafka`，消息为行级 envelope。Producer 在 P1b；配置示例见 [docs/examples](docs/examples/)。文档库 Change Stream 格式请用 [mongo-sync](https://github.com/whaleal-dev/mongo-sync)。

---

## 文档

| 文档 | 说明 |
|------|------|
| [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) | 架构契约、事件模型、控制面、落地顺序 |
| [docs/QuickStart.md](docs/QuickStart.md) | 构建与范围说明 |
| [docs/examples](docs/examples/) | MYSQL / KAFKA 配置键示例 |
| [docs/类型转换.md](docs/类型转换.md) | MySQL / Oracle / PG 列类型转换 |
| [docs/RDS-Sync介绍文档.md](docs/RDS-Sync介绍文档.md) | 产品背景与历史架构 |
| [mongo-sync](https://github.com/whaleal-dev/mongo-sync) | 文档库同步（MongoDB / Kafka） |

---

## 适用场景

- **MySQL → MySQL**：迁库、扩容、逻辑迁移  
- **MySQL / Oracle / PG → Kafka**：行级变更投递，供下游消费  
- **Oracle / PG → MySQL**：异构搬迁  
- **国产化转型**：关系库迁到达梦 / TiDB / 人大金仓等前的结构化同步（按 Sink JDBC 能力验证）  
- **与 mongo-sync 并列部署**：同一套运维习惯，分别处理关系库与文档库  

> 生产切换前请自行校验数据。更细限制与里程碑见 [架构说明](docs/ARCHITECTURE.md)。

---

## 主要贡献者

- [LHP](https://github.com/GitHubLhp123)

---

## 交流与支持

- QQ 交流群：**983986505**
