# SDK 与 SPI

> ⚠️ 本页绝大多数内容是**契约**。`RdsSyncClient` 目前只有接口、没有实现类；`SinkFactory.create()` 一律抛 `UnsupportedOperationException`。可直接运行的片段见 [快速开始](../getting-started/quickstart.md)。

---

## 编排层：`RdsSyncClient`

```java
public interface RdsSyncClient extends AutoCloseable {
    void start();
    void resume();
    MigrationProgress pause();
    MigrationProgress pauseIncremental();
    MigrationProgress resumeIncremental();
    MigrationProgress progress();
    boolean canCommit();
    MigrationProgress commit();
    void stop();
}
```

按 `sink.type` 装配 `RowChangeSink`；Pipeline 只认 SPI，不感知 JDBC / Kafka。

| 方法 | 语义 |
|------|------|
| `start()` | 启动；处于 `PAUSED` 时从暂停处续跑 |
| `resume()` | 恢复（**不**重跑已完成的全量） |
| `pause()` | 全量 + 增量都停 |
| `pauseIncremental()` / `resumeIncremental()` | 只冻增量，全量可继续 |
| `progress()` | 返回 `MigrationProgress` 快照 |
| `canCommit()` | 全量完成 + 管道排空 + lag ≤ 阈值 |
| `commit()` | 停捕获、排空、标记 `COMMITTED` |
| `stop()` / `close()` | 优雅停止并释放资源 |

状态机：

```text
IDLE → RUNNING ⇄ CAN_COMMIT → COMMITTING → COMMITTED
         ↓
       PAUSED
         ↓
       STOPPED / ERROR
```

`MigrationState` 枚举即上表状态；`MigrationProgress` 暴露 `state` / `canCommit` / `fullSyncComplete` / `incrementalPaused` / `detail` 等字段。

---

## Sink 装配点：`SinkFactory`

```java
public static RowChangeSink create(RdsSyncConfig config)
```

| `sink.type` | 当前行为 |
|-------------|----------|
| `KAFKA` | 抛 `UnsupportedOperationException`（"Kafka RowChangeSink is P1b"） |
| `MYSQL` | 抛 `UnsupportedOperationException`（"MYSQL RowChangeSink is P1"） |

> 这是**刻意的**：装配点先锁定，避免后续把 JDBC / Kafka 直接写进 Pipeline。单测 `RdsSyncConfigTest.factoryLocksAssemblyPoint` 就在保护这个行为。

---

## RowChangeSink SPI

Pipeline **只**依赖本接口（对齐 mongo-sync 的 `TransferSink`）：

| 方法 | 语义 |
|------|------|
| `long write(RowChange event)` | 写入一条行事件；返回写入序号，`0` 表示本次未产生写入 |
| `long landedThrough()` | **已确认落地**的最大写入序号：所有 `seq <= landedThrough()` 的写入都已在 Sink 端生效（JDBC 落库 / Kafka produce ack） |
| `void applyDdl(DdlEvent event)` | 先排空在途 CRUD，再落地 DDL（JDBC 执行 SQL；Kafka 按配置投递消息） |
| `void setOrdered(boolean ordered)` | JDBC 可调 statement 有序写；Kafka 实现可忽略 |
| `void flushAndWait()` | 刷缓冲并等待在途完成（**不关闭**资源） |
| `void close()` | 最终 flush 后关闭资源 |

`landedThrough()` 是「真实落地」而非「已提交」的判据 —— Pipeline 靠它决定何时可以执行 DDL / 何时算排空。

---

## 全部 SPI

`rds-transfer-model` 定义的方向契约：

| SPI | 方向 / 职责 | 关键方法 |
|-----|-------------|----------|
| `SnapshotSource` | 全量 → `RowChange(op=r)` | `start(RowChangeListener)` / `stop()` / `close()` |
| `IncrementalSource` | CDC → `RowChange` / `DdlEvent` | `start(RowChangeListener, DdlEventListener)` / `pause()` / `resume()` / `stop()` |
| `RowChangeListener` | Source → Pipeline | `onEvent(RowChange)` |
| `DdlEventListener` | Source → Pipeline | `onDdl(DdlEvent)` |
| `RowChangePipeline` | 分桶有序 + DDL barrier + 背压 | `attach(RowChangeSink)` / `waitDrained()` / `inflight()` / `close()`，并继承两个 Listener |
| `RowChangeSink` | Pipeline → MYSQL / KAFKA | 见上节 |
| `OffsetStore` | 位点存取 | `save(namespace, offset)` / `load(namespace)` / `clear(...)`；默认实现 `MemoryOffsetStore` |

实现状态：**以上接口目前均无实现类**（`grep "implements RowChangeSink"` 等均无结果）。P1 起由 `rds-mysql-sink` 实现 `RowChangeSink`，P1b 由 `rds-kafka-sink` 实现，P2 实现 `IncrementalSource`。

---

## 事件模型

### `RowChange`

| 字段 | 含义 |
|------|------|
| `op` | `c` / `u` / `d` / `r`（snapshot） |
| `schema` / `table` | 库表；`qualifiedTable()` 返回 `schema.table` |
| `before` / `after` | 列名 → 类型化值（不可变 Map） |
| `pkColumns` | 主键列名（分桶路由 / Kafka key）；缺省为空列表 |
| `tsMs` | 源端事件时间（可空） |
| `source` | `RowChangeSource`：捕获来源元数据 |

```java
RowChange row = RowChange.builder()
        .op("r")
        .schema("demo").table("orders")
        .after(java.util.Collections.singletonMap("id", 1))
        .pkColumns(java.util.Collections.singletonList("id"))
        .build();
```

### `DdlEvent`

| 字段 | 含义 |
|------|------|
| `type` | `CREATE_TABLE` / `ALTER_TABLE` / `DROP_TABLE` / `RENAME_TABLE` / `TRUNCATE_TABLE` / `CREATE_INDEX` / `DROP_INDEX` / `OTHER`（Builder 默认 `OTHER`） |
| `schema` / `table` | 对象定位 |
| `sql` | MYSQL Sink 执行；KAFKA Sink 作为消息发出 |
| `tsMs` | 源端时间 |

---

## Kafka 消息契约

`rds-kafka-sink` 已锁定 topic 命名与消息格式（**不含** `kafka-clients` Producer）。

### Topic

`KafkaTopicMapper`：

```text
topic(schema, table)      = [prefix·sep] schema [·sep·table] [·sep·suffix]
ddlTopic(schema, table)   = kafka.ddl.topic 非空 ? 它 : topic(schema, table)
```

- `kafka.topic` 非空 ⇒ 全部走固定 topic（`isFixedTopic()` 为 true）
- `prefix` / `suffix` 为空时不产生多余分隔符

### 消息

`RowChangeEnvelope` 常量定义了字段名：

| Kafka | 内容 |
|-------|------|
| key | 主键列对象（`pkColumns` 为空 ⇒ 空对象）；值优先取 `after`，缺失时回退 `before` |
| value | `op` / `schema` / `table` / `ts_ms` / `source` / `before` / `after` |
| DDL value | `op=ddl`，另含 `sql` / `ddl_type` |

`op` 常量：`OP_CREATE="c"` / `OP_UPDATE="u"` / `OP_DELETE="d"` / `OP_READ="r"` / `OP_DDL="ddl"`。

> P1b 才把 envelope 序列化为 JSON 字节（`RowChangeEnvelope` 目前只提供字段映射）。序列化前的字节编码方式随 P1b 定稿。

---

## 异常与失败语义

| 场景 | 行为 |
|------|------|
| 配置不合法（缺 `sourceUri` / `sinkUri`） | `IllegalArgumentException` |
| 对 `sinkType=MYSQL` 调 `toKafkaSinkConfig()` | `IllegalStateException` |
| 调用 `SinkFactory.create(...)`（任何类型） | `UnsupportedOperationException`（P1 / P1b 未实现） |
| 写失败（P1 后） | 由 Sink 实现决定；**建议**与 mongo-sync 一致：可注入写失败回调，且不静默丢事件 |

> 与 mongo-sync 不同，本仓**尚未定义**错误码枚举。P1 接入执行链路时应补上一套（对齐 mongo-sync 的 `MongoSyncErrorCode` 风格），便于运维按码排障。

---

下一步：[运行与排障](operations.md) · [落地状态与路线](../reference/roadmap.md)
