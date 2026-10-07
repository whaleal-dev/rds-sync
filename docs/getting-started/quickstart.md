# 快速开始

> ⚠️ 本仓当前是 **P0 架构契约**：契约与装配点已定稿并有单测保护，但**还没有端到端可跑的同步**。本文如实区分「现在就能跑」与「还不能跑」。

## 环境要求

| 项 | 要求 |
|----|------|
| JDK | **1.8**（`maven.compiler.source/target = 8`） |
| 构建 | Maven 3.6+ |
| 数据库 | MySQL / Oracle / PostgreSQL（全量源，代码在仓库内） |
| 可选 | Kafka（Kafka Sink 形态） |

---

## 现在就能跑

### 1. 构建

```bash
cd rds-sync
mvn -DskipTests package
```

### 2. 契约单测（推荐每次都跑）

只跑契约相关模块，不涉及全量 JDBC 模块：

```bash
mvn -pl rds-transfer-model,rds-kafka-sink,rds-sync-client -am test
```

覆盖的内容：`SyncMode` 语义、`SinkType` 解析、`MemoryOffsetStore` 读写、`RdsSyncConfig` 校验与 Kafka 配置归一化、Kafka topic 命名、行级 envelope 编码。

### 3. 配置装配（真实可用）

`RdsSyncConfig` 与 Builder 是**已可用**的代码，可直接用来校验你的目标配置：

```java
RdsSyncConfig cfg = RdsSyncConfig.builder()
        .sourceUri("jdbc:mysql://127.0.0.1:3306/src")
        .sinkType(SinkType.KAFKA)
        .sinkUri("kafka://127.0.0.1:9092")      // 会归一化为 127.0.0.1:9092
        .syncMode(SyncMode.FULL_AND_INCREMENTAL)
        .kafkaTopicPrefix("rds")
        .bootstrapTable(true)                   // Kafka 下会被强制改回 false
        .build();

cfg.getSinkUri();                 // "127.0.0.1:9092"
cfg.isBootstrapTable();           // false
cfg.toKafkaSinkConfig().getTopicPrefix();  // "rds"
```

约束（构造时即校验，不合法直接抛异常）：

| 情况 | 结果 |
|------|------|
| 缺 `sourceUri` | `IllegalArgumentException` |
| `sinkType=MYSQL` 缺 `sinkUri` | `IllegalArgumentException` |
| `sinkType=KAFKA` 缺 `sinkUri` | `IllegalArgumentException` |
| `sinkType=KAFKA` 时 `bootstrapTable=true` | 被强制改回 `false`（Kafka 没有 JDBC 会话） |
| 对 `sinkType=MYSQL` 调 `toKafkaSinkConfig()` | `IllegalStateException` |

### 4. 事件构造（真实可用）

```java
RowChange row = RowChange.builder()
        .op("u")
        .schema("demo").table("orders")
        .before(java.util.Collections.singletonMap("id", 1))
        .after(java.util.Collections.singletonMap("id", 1))
        .pkColumns(java.util.Collections.singletonList("id"))
        .build();

DdlEvent ddl = DdlEvent.builder()
        .type(DdlEvent.Type.CREATE_INDEX)
        .schema("demo").table("orders")
        .sql("CREATE INDEX idx_x ON orders(x)")
        .build();
```

字段说明见 [概念总览 · 事件契约](../concepts/overview.md#2-事件契约)。

---

## 现在还不能跑

| 想做的事 | 现状 |
|----------|------|
| `RdsSyncClient.start()` 跑一次真实同步 | ❌ `RdsSyncClient` **只有接口，没有实现类** |
| 用 `SinkFactory.create(cfg)` 拿到 Sink | ❌ 当前一律抛 `UnsupportedOperationException`（MYSQL 指向 P1、KAFKA 指向 P1b） |
| 用配置文件驱动同步 | ❌ `rds-sync-client` **尚未实现 properties 加载器**；`docs/examples/*.properties` 目前是**架构约定键**，P1 起才由 Client 读取 |
| 直接拿 `rds-mysql-source` 当 SDK 用 | ❌ 仍是内部链路（`BatchDataEntity` + `MemoryCache`），未适配 `SnapshotSource` SPI |
| 增量同步 | ❌ `IncrementalSource` 只有 SPI；解析借 Debezium / Canal，适配器在 P2 |
| Kafka 真实投递 | ❌ 已有 `KafkaSinkConfig` / `KafkaTopicMapper` / `RowChangeEnvelope`，**没有 `kafka-clients` Producer**（P1b） |

> 上面的「❌」不是配置或用法问题，是**功能尚未实现**。请不要按未实现的路径去排障。落地顺序见 [落地状态与路线](../reference/roadmap.md)。

---

## 模块清单与状态

以根 `pom.xml` 的 `<modules>` 为准（9 个）：

| 模块 | 职责 | 状态 |
|------|------|------|
| `rds-transfer-model` | 事件、`SinkType`、`SyncMode`、全部 SPI | **P0 契约已交付**（有单测） |
| `rds-kafka-sink` | Topic 命名 / 行级 envelope 契约 | **契约已定，无 Producer**（有单测） |
| `rds-sync-client` | `RdsSyncConfig` + `SinkFactory` + 控制面 API | **装配点已定**；`RdsSyncClient` 仅接口，`SinkFactory` 抛 `UnsupportedOperationException` |
| `rds-common` / `rds-core` | 列类型、Range 切分、JDBC、线程池 | 保留，**对内** |
| `rds-mysql-source` / `rds-oracle-source` / `rds-pg-source` | 全量 JDBC（`BatchDataEntity` → `MemoryCache`） | **未接 SPI**，P1/P3 适配为 `SnapshotSource` |
| `rds-mysql-sink` | JDBC 写入 | **未接 SPI**，P1 适配为 `RowChangeSink` |

> 架构文档 §8 提到的 `realTimeOfMysql`（增量适配层）与 `execute`（可选 CLI）**当前仓库中没有对应目录**，属规划项。

---

## 配置示例

`docs/examples/` 下有两份模板：

```properties
# rds-sync.example.properties（MySQL Sink）
source.uri=jdbc:mysql://127.0.0.1:3306/demo?useSSL=false
sink.type=mysql
sink.uri=jdbc:mysql://127.0.0.1:3306/demo_dst?useSSL=false
sync.mode=FULL
bootstrap.table=true
```

```properties
# rds-sync-kafka.example.properties（Kafka Sink，行级 envelope）
source.uri=jdbc:mysql://127.0.0.1:3306/demo?useSSL=false
sink.type=kafka
sink.uri=127.0.0.1:9092
sync.mode=FULL
kafka.topic.prefix=rds
kafka.publish.ddl=true
```

再次强调：这些键目前是**架构约定**，不是「填了就能跑」。全部键与 Builder 方法的对照见 [配置项全解](../guide/configuration.md)。

---

## 下一步

- 了解契约 → [概念总览](../concepts/overview.md) · [SDK 与 SPI](../guide/api.md)
- 了解配置全貌 → [配置项全解](../guide/configuration.md)
- 确认什么时候能用 → [落地状态与路线](../reference/roadmap.md)
- 需要关系库以外的同步 → [mongo-sync](https://github.com/whaleal-dev/mongo-sync)
