# rds-sync 架构契约（方案 C：Hybrid）

> 关系型数据库同步产品。Mongo 由 [mongo-sync](https://github.com/whaleal-dev/mongo-sync) 负责；本仓**不**包含 Mongo / Hadoop / HDFS。

## 1. 定位

| 项 | 约定 |
|----|------|
| 源 | MySQL / Oracle / PostgreSQL（插件式 Source） |
| 目标 | 以 MySQL 为首发；后续可扩 Oracle/PG Sink |
| 模式 | 全量、增量、全量∥增量、追平后可停 |
| 形态 | 嵌入式 Java SDK + 可选 CLI（对齐 mongo-sync） |
| 非目标 | Flink/Kafka Connect 产品化；自研 binlog/redo/WAL 协议栈；Mongo / Hadoop 生态 |

## 2. 分层

```text
┌─────────────────────────────────────────────────────────┐
│  rds-sync-client                                        │
│  编排：start / pause / pauseIncremental / progress /    │
│        canCommit / commit                               │
└───────────────────────────┬─────────────────────────────┘
                            │ RowChange / DdlEvent
┌───────────────────────────▼─────────────────────────────┐
│  Pipeline（有序分桶 + DDL barrier + 背压）               │
└───────────────────────────┬─────────────────────────────┘
                            │
     ┌──────────────────────┼──────────────────────┐
     ▼                      ▼                      ▼
 Source (full / CDC)    Offset Store           Sink (JDBC)
 mysql / oracle / pg                           mysqlTarget …
```

**资产来源：**

| 层 | 继承 PhotonT | 对齐 mongo-sync |
|----|--------------|-----------------|
| 列类型 / 全量 Range 切分 / JDBC 连接与批写 | ✅ `common` + `*Source` + `*Target` + `core` | — |
| 事件模型 / 状态机 / Pipeline / 控制 API | — | ✅ 同构语义 |
| 增量 / 复杂解析 | **三方包**（见 §3） | 本仓只做适配到 `RowChange` |

## 3. 解析策略：优先三方包

**原则：能借就不造。** 尤其是增量日志（binlog / redo / WAL），协议细节多、版本碎片多，自研成本高且易踩坑。rds-sync 负责编排、切分、类型、写入与控制面；**解析器实现选成熟开源，经适配器输出本仓事件契约。**

| 场景 | 推荐方向（可替换，SPI 隔离） | 本仓职责 |
|------|------------------------------|----------|
| MySQL 增量 | Debezium Embedded（首选）或 Canal parse | `BinlogEvent` → `RowChange` / `DdlEvent` + Offset |
| Oracle 增量 | Debezium Oracle / LogMiner 类方案 | 同上 |
| PostgreSQL 增量 | Debezium PG（逻辑复制） | 同上 |
| 全量 ResultSet → 列 | 可继续用 PhotonT JDBC 解析；复杂类型可借 JDBC/驱动官方类型 | 映射到 Column / `RowChange(op=r)` |
| DDL 文本 | 可借 Druid / JSqlParser 等做结构化（可选） | 落入 `DdlEvent` |

**硬约束：**

1. 三方包**不得**泄漏到对外 SDK：调用方只看见 `RowChange` / `DdlEvent` / Client API。  
2. 解析器与 Pipeline 之间只有适配层；换 Canal ↔ Debezium 不改 Sink / Client。  
3. 不做「又一个 Canal/Flink CDC 产品」；嵌入式嵌入本进程即可。  
4. 许可证与依赖体积在引入时评审（P2 选型时定稿）。

## 4. 事件模型（契约）

中间传输**不以** PhotonT `BatchDataEntity` 为对外契约；对内可先适配，对外固定为：

### `RowChange`

| 字段 | 含义 |
|------|------|
| `op` | `c` insert / `u` update / `d` delete / `r` snapshot（全量） |
| `schema` / `table` | 库表 |
| `before` / `after` | 列名 → 类型化值（复用 PhotonT Column 语义，可装箱为 `Object`/`AbstractColumn`） |
| `pkColumns` | 主键列名（分桶路由） |
| `tsMs` | 源端事件时间（可空） |
| `source` | 捕获来源元数据（实例、位点摘要） |

### `DdlEvent`

| 字段 | 含义 |
|------|------|
| `type` | CREATE/ALTER/DROP/RENAME/TRUNCATE … |
| `schema` / `table` | 对象定位 |
| `sql` / 结构化摘要 | 供 Sink 执行或记录 |
| `tsMs` | 源端时间 |

### SPI

- `RowChangeListener#onEvent(RowChange)`
- `DdlEventListener#onDdl(DdlEvent)`

Source 只产出事件；Sink 只消费事件；Client 连接二者并挂 Pipeline。

## 5. 状态机与控制面

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
| `canCommit` | 全量完成 + 管道排空 + lag ≤ 阈值 |
| `commit` | 停捕获、排空、标记 COMMITTED（最小 cutover） |

**废弃对外使用：** 全局整数 `ProStatus` 作为产品 API（内部过渡期可暂存，新代码不依赖）。

## 6. Pipeline 约定

1. **同主键有序**：`hash(pk) % bucketNum` → 单桶单写者  
2. **DDL barrier**：执行 DDL 前 `waitDrained` 该表（或全局）在途 CRUD  
3. **背压**：有界队列；禁止静默丢事件（至少计数 + 告警）  
4. **关闭**：超时仍须 shutdown / 最终 flush  

全量阶段可与增量并行（事件 `op=r` vs CDC）；冲突靠 PK 有序 + UPSERT/按 op 语义解决。

## 7. 模块边界

| 模块 | 职责 | 状态 |
|------|------|------|
| `rds-transfer-model` | `RowChange` / `DdlEvent` / Listener SPI | 新建（契约） |
| `common` | Column 类型、切分工具、旧实体（过渡） | 保留，逐步瘦身 |
| `core` | JDBC 连接、线程池 | 保留（仅关系型） |
| `mysqlSource` / `oracleSource` / `pgSource` | 全量 Source；后续接 CDC | 保留 |
| `mysqlTarget` | JDBC Sink | 保留 |
| `realTimeOfMysql` | MySQL 增量适配层（三方解析 → `RowChange`） | 空壳待填 |
| `rds-sync-client` | 编排 SDK | 新建（骨架） |
| `execute` | 可选 CLI/样例入口 | 保留为空壳或样例 |
| ~~mongodb*~~ / ~~realTimeOfMongoDb~~ | — | **已删除** |
| ~~hdfsTarget~~ / Hadoop 连接与依赖 | — | **已删除** |

## 8. Offset

| 模式 | 位点 |
|------|------|
| 全量 | 表级切段进度（可选持久化） |
| MySQL 增量 | binlog filename + position（或 GTID）；由三方解析器提供，本仓持久化 |
| Oracle / PG | 跟所选 CDC 方案的位点模型走 |

持久化：内存默认；文件/DB 可插拔（对齐 mongo-sync `offsetStoreDir` 思路）。

## 9. 落地顺序（确认后再写业务）

1. **P0** 契约模块 + Client 空壳 + 根工程可编译（无 Mongo / Hadoop）  
2. **P1** MySQL 全量：切分修复 → `RowChange(op=r)` → Pipeline → `mysqlTarget`  
3. **P2** MySQL 增量：选定 Debezium Embedded（或 Canal）→ 适配器 → `pauseIncremental` / `canCommit`  
4. **P3** Oracle / PG Source 接入同一 SPI（增量同样外借三方）  

---

**一句话：** PhotonT 的关系型「芯」 + mongo-sync 的编排「壳」；解析（尤其增量日志）借三方；Mongo / Hadoop 出局。
