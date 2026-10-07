# 配置项全解

> ⚠️ **配置来源现状**：`rds-sync-client` 目前**只有 Builder**，还没有 properties 加载器。因此：
>
> - **配置文件（`.properties`）中的键目前是「架构约定」** —— 写法已定稿，P1 起由 `RdsSyncClient` 读取；现在把它们写进文件不会有任何效果。
> - **Builder 调用是当前唯一真实生效的配置方式**。
>
> 下表把两者并列，便于「先按约定写文件、P1 后直接翻译成代码」。

---

## 一、架构约定键（可写成 `.properties`）

| 键 | 默认 | 说明 |
|----|------|------|
| `source.uri` | — | 源 JDBC URL。**必填** |
| `sink.type` | `mysql` | Sink 形态：`mysql` \| `kafka`。`jdbc` / `rds` 均被解析为 `mysql` |
| `sink.uri` | — | `mysql`：JDBC URL；`kafka`：bootstrap servers（允许 `kafka://` 前缀）。**必填** |
| `sync.mode` | `FULL` | `FULL` \| `INCREMENTAL` \| `FULL_AND_INCREMENTAL` \| `FULL_THEN_CATCH_UP` |
| `bootstrap.table` | `true` | MYSQL 可预建表；**KAFKA 强制为 false**（无 JDBC 会话） |
| `kafka.topic` | 空 | 固定 topic，非空时**覆盖**按表拼接 |
| `kafka.topic.prefix` | 空 | topic 前缀 |
| `kafka.topic.separator` | `.` | 分隔符 |
| `kafka.topic.suffix` | 空 | topic 后缀 |
| `kafka.ddl.topic` | 空 | DDL 专用 topic；为空则与行事件同规则 |
| `kafka.publish.ddl` | `true` | 是否把 DDL 作为消息投递 |
| `kafka.acks` | `all` | Producer `acks` |
| `kafka.linger.ms` | `5` | Producer `linger.ms` |
| `kafka.batch.size` | `16384` | Producer `batch.size`（字节） |
| `kafka.compression.type` | `lz4` | Producer `compression.type` |
| `kafka.client.id` | `rds-sync` | Producer `client.id` |
| `kafka.producer.*` | — | **透传** Producer 配置，写入时去掉 `kafka.producer.` 前缀。如 `kafka.producer.security.protocol=SASL_SSL` |

### Kafka topic 命名规则

```text
[prefix·sep] schema [·sep·table] [·sep·suffix]
```

- `prefix` / `suffix` 为空时**不会**多出多余分隔符
- `table` 为空时只输出 `schema` 段
- `kafka.topic` 非空 ⇒ 全部事件走该固定 topic
- DDL 优先走 `kafka.ddl.topic`，未配置则与行事件同规则

示例：`prefix=rds`、`separator=.`、`schema=demo`、`table=orders` ⇒ `rds.demo.orders`

---

## 二、仅 Builder 可用的字段

这些字段已在 `RdsSyncConfig` 中实现（含默认值与兜底），但**尚未分配 `.properties` 键名**，命名待 P1 定稿。

| Builder 方法 | 默认 | 说明 |
|--------------|------|------|
| `sourceUser(...)` / `sourcePassword(...)` | 空 | 源端凭证（也可直接写在 JDBC URL 里） |
| `sinkUser(...)` / `sinkPassword(...)` | 空 | 目标端凭证 |
| `tableWhite(...)` | `.*` | 库表过滤，匹配 `schema.table`（正则）。`getTableWhite()` 对空值兜底为 `.*`（即全部） |
| `sinkSchema(...)` | 空 | Sink schema；为空则与源 schema 相同 |
| `bucketNum(...)` | `4` | 分桶数（`<=0` 时兜底为 4） |
| `batchSize(...)` | `200` | 批量写入大小（`<=0` 时兜底为 200） |
| `splitSlices(...)` | `4` | 全量切分段数（`<=0` 时兜底为 4） |

> 这三个数值默认值（4 / 200 / 4）明显偏保守，是为「先跑通再调优」预留的；P1 接入 JDBC 时会按实测调整。

---

## 三、配置装配与校验

```java
RdsSyncConfig cfg = RdsSyncConfig.builder()
        .sourceUri("jdbc:mysql://127.0.0.1:3306/src")
        .sinkType(SinkType.MYSQL)
        .sinkUri("jdbc:mysql://127.0.0.1:3306/dst")
        .syncMode(SyncMode.FULL)
        .bootstrapTable(true)
        .tableWhite("demo.*")
        .bucketNum(8)
        .batchSize(500)
        .splitSlices(8)
        .build();
```

构造期的校验（不合法直接抛异常，不会「跑起来再说」）：

| 校验 | 异常 |
|------|------|
| `sourceUri` 非空 | `IllegalArgumentException` |
| `sinkType=MYSQL` 时 `sinkUri` 非空 | `IllegalArgumentException` |
| `sinkType=KAFKA` 时 `sinkUri` 非空 | `IllegalArgumentException` |
| `sinkType=KAFKA` ⇒ 归一化 `sinkUri`（去 `kafka://`）并强制 `bootstrapTable=false` | —（自动修正） |
| `toKafkaSinkConfig()` 仅在 `sinkType=KAFKA` 时可用 | `IllegalStateException` |

`RdsSyncConfig.toKafkaSinkConfig()` 会把配置转成 `rds-kafka-sink` 的 `KafkaSinkConfig`（P1b 装配 Producer 时使用）：

```java
KafkaSinkConfig kafka = cfg.toKafkaSinkConfig();   // 要求 cfg.getSinkType() == KAFKA
```

---

## 四、与 mongo-sync 的配置键对照

两边**故意对齐**，便于同一套运维习惯（差异只在数据面）：

| 语义 | rds-sync | mongo-sync |
|------|----------|-----------|
| 源 | `source.uri`（JDBC） | `source.uri`（Mongo 连接串） |
| Sink 形态 | `sink.type=mysql\|kafka` | `sink.type=mongodb\|kafka` |
| Sink 地址 | `sink.uri` | `sink.uri` |
| 同步模式 | `sync.mode` | `sync.mode` |
| 预建目标结构 | `bootstrap.table` | `bootstrap.collection` / `bootstrap.indexes` |
| Kafka topic | `kafka.topic` / `prefix` / `separator` / `suffix` / `ddl.topic` | `kafka.topic` / `prefix` / `separator` / `suffix` |
| Kafka Producer | `kafka.acks` / `linger.ms` / `batch.size` / `compression.type` / `client.id` / `producer.*` | 同名 |

> Kafka 消息格式**不同**：本仓是行级 envelope（`op` / `before` / `after`），mongo-sync 是 mongo-kafka Change Stream。下游消费者不能混用。

---

下一步：[同步模式](sync-modes.md) · [SDK 与 SPI](api.md) · [落地状态与路线](../reference/roadmap.md)
