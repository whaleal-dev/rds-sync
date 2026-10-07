# 概念总览

理解下面几件事，就能读懂 rds-sync 的配置、SPI 与后续实现。

> 本文描述的是**契约**（已经定稿、有单测保护），不是「已经跑起来的实现」。各模块当前状态见 [落地状态与路线](../reference/roadmap.md)。

---

## 1. 分层

```text
┌─────────────────────────────────────────────────────────┐
│  rds-sync-client                                        │
│  编排：start / pause / pauseIncremental / progress /    │
│        canCommit / commit                               │
│  sink.type = MYSQL | KAFKA                              │
│  SinkFactory 按类型挂 RowChangeSink                      │
└───────────────────────────┬─────────────────────────────┘
                            │
     SnapshotSource / IncrementalSource
                            │ RowChange / DdlEvent
┌───────────────────────────▼─────────────────────────────┐
│  RowChangePipeline（有序分桶 + DDL barrier + 背压）       │
│  只依赖 RowChangeSink，不感知 JDBC / Kafka               │
└───────────────────────────┬─────────────────────────────┘
                            │
     ┌──────────────────────┼──────────────────────┐
     ▼                      ▼                      ▼
 OffsetStore            RowChangeSink
 （默认内存）            ├─ MYSQL：rds-mysql-sink 适配 SPI
                         └─ KAFKA：rds-kafka-sink Producer
```

三层职责边界（**硬约束**）：

1. **Pipeline 禁止**直接依赖 `kafka-clients` 或 JDBC Driver
2. **Kafka 模块禁止**把 Connect API 泄漏到 `RdsSyncClient`
3. 同一条同步任务**同一时刻只有一种** `sink.type`，不做双写
4. Kafka Sink **不做表结构 bootstrap**（没有 JDBC 会话）
5. `RdsSyncClient` / Pipeline **禁止**直接依赖内部 `MemoryCache` / `ProStatus` / `ProgramInfo`

---

## 2. 事件契约

中间传输**不以**内部 `BatchDataEntity` 为对外契约。对外固定为两类事件：

### `RowChange`（行级变更，全量与 CDC 共用）

| 字段 | 含义 |
|------|------|
| `op` | `c` insert / `u` update / `d` delete / `r` snapshot（全量） |
| `schema` / `table` | 库表 |
| `before` / `after` | 列名 → 类型化值 |
| `pkColumns` | 主键列名（分桶路由；Kafka key） |
| `tsMs` | 源端事件时间（可空） |
| `source` | 捕获来源元数据（实例、位点摘要） |

### `DdlEvent`（结构变更）

| 字段 | 含义 |
|------|------|
| `type` | `CREATE_TABLE` / `ALTER_TABLE` / `DROP_TABLE` / `RENAME_TABLE` / `TRUNCATE_TABLE` / `CREATE_INDEX` / `DROP_INDEX` / `OTHER` |
| `schema` / `table` | 对象定位 |
| `sql` | DDL 语句：MYSQL Sink 执行，KAFKA Sink 作为消息发出 |
| `tsMs` | 源端时间 |

**Sink 不感知上游是 JDBC 快照还是 CDC** —— 统一变成事件后再写入，所以换捕获方式不影响 Sink，换 Sink 不影响 Source。

---

## 3. Pipeline 约定

`RowChangePipeline` 只依赖 `RowChangeSink`，其行为约定：

| # | 约定 | 说明 |
|---|------|------|
| 1 | **同主键有序** | `hash(pk) % bucketNum` → 单桶单写者 |
| 2 | **DDL barrier** | 执行 / 投递 DDL 前，先 `waitDrained` 该表（或全局）在途 CRUD |
| 3 | **背压** | 有界队列；**禁止静默丢事件**（至少计数 + 告警） |
| 4 | **关闭** | 超时仍须 shutdown / 最终 `flushAndWait` |
| 5 | **同 PK 未落地又来一条** | 先 `flushAndWait`（对齐 mongo-sync 的 `landedThrough`） |

全量阶段可与增量并行（`op=r` vs CDC 事件）；MYSQL Sink 靠 PK 有序 + UPSERT 兜底重叠，KAFKA Sink 靠 key 分区 + 同 PK flush。

---

## 4. 双 Sink 形态

配置键为 `sink.type` / `sink.uri`（与 mongo-sync 同名）：

| `sink.type` | `sink.uri` | 行为 |
|-------------|------------|------|
| `MYSQL`（默认） | JDBC URL | `rds-mysql-sink` 批写；可预建表 / 索引 |
| `KAFKA` | bootstrap servers（允许 `kafka://` 前缀） | Producer 写出；**不**预建 JDBC 表 |

### Kafka 消息契约（行级 envelope）

与 mongo-sync 的 **mongo-kafka Change Stream** 格式**明确区分**：

| Kafka | 内容 |
|-------|------|
| key | 主键列对象（无主键则空对象） |
| value | `op` / `schema` / `table` / `ts_ms` / `source` / `before` / `after` |
| DDL value | `op=ddl`，另含 `sql` / `ddl_type` |
| topic | `[prefix·sep]schema[·sep·table][·sep·suffix]`；`kafka.topic` 非空时全部走固定 topic；DDL 可用 `kafka.ddl.topic` 单独指定 |

`op` 取值与 `RowChange` 一致：`c` / `u` / `d` / `r`；DDL 为 `ddl`。

> Kafka Sink **不会在对端执行 DDL**，只是把 DDL 作为消息发出去，由下游决定如何处理。

---

## 5. 全量切分

全量走 JDBC 扫表，按**主键 Range 切段**并行：

| 主键类型 | 切分方式 |
|----------|----------|
| `long`（含 `int`） | 数值区间切分 |
| `String` | 字符串区间切分 |
| 复合（`long` + `String` 等） | 组合区间切分 |

切段后可生成多个查询任务（例如 `select * from t where pk > ? and pk < ?`），并发扫、按主键分桶有序写。

相关模块：`rds-common`（`RangeSplitUtil` / 列类型）、`rds-core`（JDBC 连接与线程池）、`rds-*-source`（各库全量源）。

> ⚠️ 现有全量模块**有 JDBC 读写代码，但还不是 rds-sync 产品链路**：它们走内部 `BatchDataEntity` + `MemoryCache`，尚未适配 `SnapshotSource` / `RowChangeSink`。详见 [落地状态与路线](../reference/roadmap.md)。

---

## 6. 类型转换

异构搬迁的核心是列类型映射（MySQL / Oracle / PG ↔ 公共数据类）。转换矩阵见 [类型转换.md](../类型转换.md)，代码在：

- 列类型定义：`rds-common` 的 `columntype` / `column` 包
- 源端解析：`rds-*-source` 的 `parse`
- 目标端映射：`rds-mysql-sink` 的 `parse`（`ColumnDataToMysqlData` / `ParseTypeFromColumnType`）

> 后续计划支持用户自定义转换规则。

---

## 7. 状态机与控制面

```text
IDLE → RUNNING ⇄ CAN_COMMIT → COMMITTING → COMMITTED
         ↓
       PAUSED
         ↓
       STOPPED / ERROR
```

| API | 语义 |
|-----|------|
| `start` / `resume` | 启动或从 `PAUSED` 续跑；resume **不**重跑已完成全量 |
| `pause` | 全量 + 增量都停（初始全量进行中受限，与 mongo-sync 一致） |
| `pauseIncremental` / `resumeIncremental` | 仅冻增量，全量可继续 |
| `progress` | 快照：相位、全量进度、增量计数、inflight、lag、detail |
| `canCommit` | 全量完成 + 管道排空 + lag ≤ 阈值（Kafka 以 produce ack 计入排空） |
| `commit` | 停捕获、排空、标记 `COMMITTED`（最小 cutover） |

**已废弃**：全局整数 `ProStatus` 作为产品 API（过渡期内部可暂存，新代码不得依赖）。

---

## 8. 位点（Offset）

| 模式 | 位点 |
|------|------|
| 全量 | 表级切段进度（可选持久化） |
| MySQL 增量 | binlog filename + position（或 GTID），由三方解析器提供，本仓持久化 |
| Oracle / PG | 跟所选 CDC 方案的位点模型走 |

持久化：默认内存（`MemoryOffsetStore`），文件 / DB 可插拔（对齐 mongo-sync 的 `offsetStoreDir`）。

> 位点推进与 Sink 形态无关 —— 仍按 **Source 回调**推进。严格场景后续可改为「Sink 落地后再记位点」，这与 mongo-sync 是同一限制。

---

下一步：[快速开始](../getting-started/quickstart.md) · [SDK 与 SPI](../guide/api.md) · [落地状态与路线](../reference/roadmap.md)
