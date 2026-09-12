# rds-sync 架构契约（方案 C：Hybrid）

> 关系型数据库同步产品。文档库由姊妹仓 [mongo-sync](https://github.com/whaleal-dev/mongo-sync) 负责；本仓**不**包含 Mongo / Hadoop / HDFS。  
> 两边控制面同构、事件模型独立：这里是 `RowChange`，那边是 `TransferEvent`。  
> **Sink 形态与 mongo-sync 对齐**：关系库 JDBC **和 Kafka 并列**；Pipeline 只认 Sink SPI，不感知 JDBC 还是 Kafka。

## 1. 定位

| 项 | 约定 |
|----|------|
| 源 | MySQL / Oracle / PostgreSQL（插件式 Source） |
| Sink | **MYSQL**（JDBC，首发）\| **KAFKA**（行级变更投递）；后续可扩 Oracle/PG JDBC Sink |
| 模式 | 全量、增量、全量∥增量、全量后追平再停（命名与 mongo-sync 的 `SyncMode` 对齐） |
| 形态 | 嵌入式 Java SDK + 可选 CLI（对齐 mongo-sync） |
| 姊妹产品 | [mongo-sync](https://github.com/whaleal-dev/mongo-sync)：MongoDB → MongoDB / Kafka |
| 非目标 | 做成 Kafka Connect / Flink CDC **产品**；自研 binlog/redo/WAL 协议栈；Mongo / Hadoop 生态；Mongo ↔ 关系库异构直连 |

Kafka 在本仓的角色是 **Sink 形态**（本进程 `Producer` 写出），不是再做一个 Connect 插件发行版。

## 2. 分层

```text
┌─────────────────────────────────────────────────────────┐
│  rds-sync-client                                        │
│  编排：start / pause / pauseIncremental / progress /    │
│        canCommit / commit                               │
│  sink.type = MYSQL | KAFKA                            │
│  SinkFactory 按类型挂 RowChangeSink               │
└───────────────────────────┬─────────────────────────────┘
                            │
     SnapshotSource / IncrementalSource
                            │ RowChange / DdlEvent
┌───────────────────────────▼─────────────────────────────┐
│  RowChangePipeline（有序分桶 + DDL barrier + 背压）     │
│  只依赖 RowChangeSink，不感知 JDBC / Kafka              │
└───────────────────────────┬─────────────────────────────┘
                            │
     ┌──────────────────────┼──────────────────────┐
     ▼                      ▼                      ▼
 OffsetStore            RowChangeSink
 （默认内存）            ├─ MYSQL：rds-mysql-sink 适配 SPI（P1）
                         └─ KAFKA：rds-kafka-sink Producer（P1b）
```

**分层职责：**

| 层 | 本仓 | 对齐 mongo-sync |
|----|------|-----------------|
| 列类型 / 全量 Range 切分 / JDBC 连接与批写 | ✅ `rds-common` + `rds-*-source` + `rds-mysql-sink` + `rds-core` | — |
| 事件模型 / 状态机 / Pipeline / 控制 API / **Sink SPI** | — | ✅ 同构语义（对应 `TransferSink`） |
| 增量 / 复杂解析 | **三方包**（见 §3） | 本仓只做适配到 `RowChange` |
| Kafka Sink | — | ✅ 同构：`sink.type=kafka`，独立 sink 模块 |

## 3. Sink 形态（MYSQL / KAFKA）

本仓配置键为 `sink.type` / `sink.uri`（Source / Sink）；mongo-sync 同样使用 `sink.type` / `sink.uri`。

| `sink.type` | `sink.uri` | 行为 |
|---------------|--------------|------|
| `MYSQL`（默认） | JDBC URL | `rds-mysql-sink` 批写；可预建表 / 索引 |
| `KAFKA` | bootstrap servers（允许 `kafka://` 前缀） | `Producer` 写出；**不**预建 JDBC 表 |

Client 按类型装配 Sink；Kafka 路径不创建 JDBC `DataSource`。

### 3.1 `RowChangeSink` SPI

Pipeline **只**依赖本接口（对齐 mongo-sync `TransferSink`）：

| 方法 | 语义 |
|------|------|
| `write(RowChange)` | 返回写入序号；`0` 表示未产生写入 |
| `landedThrough()` | 已确认落地的最大序号（JDBC 落库 / Kafka produce ack） |
| `applyDdl(DdlEvent)` | 先排空在途 CRUD，再落地 DDL |
| `setOrdered(boolean)` | JDBC 可调 statement 有序；Kafka 可忽略 |
| `flushAndWait()` | 刷缓冲并等待在途完成 |
| `close()` | 最终 flush 后关资源 |

MYSQL / KAFKA 各自实现，**换 Sink 不改 Source / Pipeline**。

### 3.2 Kafka 消息契约

与 mongo-sync 的 **mongo-kafka Change Stream** 格式区分开：本仓投递的是 **行级 envelope**（字段已锁在 `RowChangeEnvelope`）。

| Kafka | 内容 |
|-------|------|
| key | 主键列对象（无主键则空对象；P1b 可改为整行 hash） |
| value | `op` / `schema` / `table` / `ts_ms` / `source` / `before` / `after` |
| DDL value | `op=ddl`，另含 `sql` / `ddl_type` |
| topic | `{prefix}{sep}{schema}{sep}{table}{sep}{suffix}`；可配固定 `kafka.topic` |

`op` 与 `RowChange` 一致：`c` / `u` / `d` / `r`。

DDL：`kafka.publish.ddl=true`（默认）时，barrier 后发消息（同 topic 或 `kafka.ddl.topic`）；Kafka Sink **不** 在对端执行 DDL。

Topic 命名对齐 mongo-sync Kafka Sink，仅把 `db.coll` 换成 `schema.table`。当前 `rds-kafka-sink` **已有** config / mapper / envelope，**不含** `kafka-clients` Producer（P1b）。

### 3.3 配置键

| 键 | 含义 |
|----|------|
| `source.uri` | 源 JDBC URL |
| `sink.type` | `mysql`（默认）\| `kafka`（`jdbc`/`rds` 视为 mysql） |
| `sink.uri` | MYSQL：JDBC URL；KAFKA：bootstrap（允许 `kafka://`） |
| `sync.mode` | 与 mongo-sync 同名四模式 |
| `bootstrap.table` | MYSQL 可预建表；KAFKA **强制 false** |
| `kafka.topic` / `prefix` / `separator` / `suffix` / `ddl.topic` | Topic |
| `kafka.publish.ddl` | 默认 true |
| `kafka.producer.*` | 透传 Producer（P1b） |

示例：[docs/examples](examples/)。

**硬约束：**

1. Pipeline 禁止直接依赖 `kafka-clients` 或 JDBC Driver。  
2. Kafka 模块不得把 Connect API 泄漏到 `RdsSyncClient`。  
3. 同一条同步任务同一时刻只有一种 `sink.type`（不做双写）。  
4. Kafka Sink 关闭表结构 bootstrap（无 JDBC 会话）。

## 4. 解析策略：优先三方包

**原则：能借就不造。** 尤其是增量日志（binlog / redo / WAL）。rds-sync 负责编排、切分、类型、写入与控制面；**解析器实现选成熟开源，经适配器输出本仓事件契约。**

| 场景 | 推荐方向（可替换，SPI 隔离） | 本仓职责 |
|------|------------------------------|----------|
| MySQL 增量 | Debezium Embedded（首选）或 Canal parse | `BinlogEvent` → `RowChange` / `DdlEvent` + Offset |
| Oracle 增量 | Debezium Oracle / LogMiner 类方案 | 同上 |
| PostgreSQL 增量 | Debezium PG（逻辑复制） | 同上 |
| 全量 ResultSet → 列 | JDBC 解析 | 映射到 Column / `RowChange(op=r)` |
| DDL 文本 | 可借 Druid / JSqlParser（可选） | 落入 `DdlEvent` |

**硬约束：**

1. 三方包**不得**泄漏到对外 SDK：调用方只看见 `RowChange` / `DdlEvent` / Client API。  
2. 解析器与 Pipeline 之间只有适配层；换 Canal ↔ Debezium 不改 Sink / Client。  
3. 不做「又一个 Canal/Flink CDC 产品」；嵌入式嵌入本进程即可。  
4. 许可证与依赖体积在引入时评审。

## 5. 事件模型（契约）

中间传输**不以**内部 `BatchDataEntity` 为对外契约；对内可先适配，对外固定为：

### `RowChange`

| 字段 | 含义 |
|------|------|
| `op` | `c` insert / `u` update / `d` delete / `r` snapshot（全量） |
| `schema` / `table` | 库表 |
| `before` / `after` | 列名 → 类型化值 |
| `pkColumns` | 主键列名（分桶路由；Kafka key） |
| `tsMs` | 源端事件时间（可空） |
| `source` | 捕获来源元数据（实例、位点摘要） |

### `DdlEvent`

| 字段 | 含义 |
|------|------|
| `type` | CREATE/ALTER/DROP/RENAME/TRUNCATE … |
| `schema` / `table` | 对象定位 |
| `sql` / 结构化摘要 | MYSQL Sink 执行；KAFKA Sink 作为消息发出 |
| `tsMs` | 源端时间 |

### SPI（`rds-transfer-model`）

| SPI | 方向 |
|-----|------|
| `SnapshotSource` | 全量 → `RowChange(op=r)` |
| `IncrementalSource` | CDC → `RowChange` / `DdlEvent` |
| `RowChangeListener` / `DdlEventListener` | Source → Pipeline |
| `RowChangePipeline` | 分桶有序；只认 `RowChangeSink` |
| `RowChangeSink` | Pipeline → MYSQL / KAFKA |
| `OffsetStore` | 位点；默认 `MemoryOffsetStore` |

Source 只产出事件；Sink 只消费事件；Client 按 `SinkType` 挂 Sink。内部 `BatchDataEntity` **不是**对外契约。

## 6. 状态机与控制面

对齐 mongo-sync 语义（实现可逐步补齐）：

```text
IDLE → RUNNING ⇄ CAN_COMMIT → COMMITTING → COMMITTED
         ↓
       PAUSED
         ↓
       STOPPED / ERROR
```

| API | 语义 |
|-----|------|
| `start` / `resume` | 启动或从 PAUSED 续跑；resume **不**重跑已完成全量 |
| `pause` | 全量+增量都停（全量进行中可限制，与 mongo-sync 一致） |
| `pauseIncremental` / `resumeIncremental` | 仅冻增量，全量可继续 |
| `progress` | 快照：相位、全量进度、增量计数、inflight、lag、detail |
| `canCommit` | 全量完成 + 管道排空 + lag ≤ 阈值（Kafka 以 produce ack 计入排空） |
| `commit` | 停捕获、排空、标记 COMMITTED（最小 cutover） |

**废弃对外使用：** 全局整数 `ProStatus` 作为产品 API（内部过渡期可暂存，新代码不依赖）。

## 7. Pipeline 约定

1. **同主键有序**：`hash(pk) % bucketNum` → 单桶单写者  
2. **DDL barrier**：执行 / 投递 DDL 前 `waitDrained` 该表（或全局）在途 CRUD  
3. **背压**：有界队列；禁止静默丢事件（至少计数 + 告警）  
4. **关闭**：超时仍须 shutdown / 最终 `flushAndWait`  
5. **同 PK 未落地再来一条**：先 `flushAndWait`（对齐 mongo-sync `landedThrough`）

全量阶段可与增量并行（事件 `op=r` vs CDC）；MYSQL Sink 靠 PK 有序 + UPSERT；KAFKA Sink 靠 key 分区 + 同 PK flush。

## 8. 模块边界

| 模块 | 职责 | 状态 |
|------|------|------|
| `rds-transfer-model` | 事件、`SinkType`、`SyncMode`、全部 SPI | **P0 契约** |
| `rds-kafka-sink` | Topic / envelope；P1b 再接 Producer | **契约已定，无 Producer** |
| `rds-sync-client` | `RdsSyncConfig` + `SinkFactory` + 控制面 API | **装配点已定**；Sink 实现待 P1/P1b |
| `rds-common` / `rds-core` | 列类型、切分、JDBC、线程池 | 保留，对内 |
| `rds-mysql-source` / `rds-oracle-source` / `rds-pg-source` | 全量 JDBC（`BatchDataEntity` → `MemoryCache`） | **未接 SPI**；P1/P3 适配为 `SnapshotSource` |
| `rds-mysql-sink` | JDBC `RowChangeSink` | **未接 SPI**；P1 适配 |
| `realTimeOfMysql` | MySQL 增量适配层 | 空壳；P2 实现 `IncrementalSource` |
| `execute` | 可选 CLI | 空壳 |
| ~~mongodb*~~ / ~~hdfsTarget~~ | — | **已删除** |

### 8.1 旧全量链路 vs 产品 SPI

现有全量模块**有 JDBC 读写代码，但不是 rds-sync 产品链路**：

```text
旧：*SourceExecute → MemoryCache(BatchDataEntity) → MysqlSinkTask
新：SnapshotSource → RowChangePipeline → RowChangeSink
```

约束：

1. `RdsSyncClient` / Pipeline **禁止**直接依赖 `MemoryCache` / `ProStatus` / `ProgramInfo`。  
2. P1 用适配器把 Range 切分与 ResultSet 映射成 `RowChange`；不要把 `BatchDataEntity` 做成对外 API。  
3. 换 Sink 只换 `RowChangeSink` 实现，不改 Source。

## 9. Offset

| 模式 | 位点 |
|------|------|
| 全量 | 表级切段进度（可选持久化） |
| MySQL 增量 | binlog filename + position（或 GTID）；由三方解析器提供，本仓持久化 |
| Oracle / PG | 跟所选 CDC 方案的位点模型走 |

持久化：内存默认；文件/DB 可插拔（对齐 mongo-sync `offsetStoreDir`）。  
位点推进与 Sink 形态无关：仍按 Source 回调；严格场景后续可改为「Sink 落地后再记」（与 mongo-sync 同一限制）。

## 10. 落地顺序

1. **P0** 契约 + Client 装配点 + Kafka topic/envelope（无 Producer）— **本阶段**  
2. **P1** MySQL 全量：Range 切分 + JDBC 扫表适配 `SnapshotSource` → Pipeline → **MYSQL** `RowChangeSink`  
3. **P1b** Kafka：`rds-kafka-sink` 接 `kafka-clients`，实现 `RowChangeSink`  
4. **P2** MySQL 增量：`IncrementalSource`（Debezium / Canal）→ 两 Sink 共用  
5. **P3** Oracle / PG `SnapshotSource` 接入同一 Pipeline  

---

**一句话：** 关系型全量切分与 JDBC「芯」 + mongo-sync 的编排「壳」；解析借三方；Sink = **MYSQL 或 KAFKA**；Mongo / Hadoop 出局。

选型入口：关系库（含投递 Kafka）用本仓；文档库（含 mongo-kafka 格式）用 [mongo-sync](https://github.com/whaleal-dev/mongo-sync)。
