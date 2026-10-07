# 落地状态与路线

> 本文的作用只有一个：**让你不用读代码就能知道现在到底做完了什么**。所有状态都给出可自行复现的判据。

---

## 总览

| 阶段 | 目标 | 关键交付物 | 状态 |
|------|------|------------|------|
| **P0** | 架构契约 + 装配点 + Kafka 契约 | `rds-transfer-model`（事件 / SPI / `SyncMode` / `SinkType`）、`RdsSyncConfig`、`SinkFactory`、`KafkaSinkConfig` / `KafkaTopicMapper` / `RowChangeEnvelope`、契约单测 | ✅ **已交付** |
| **P1** | MySQL 全量跑通 | Range 切分 + JDBC 扫表适配 `SnapshotSource` → Pipeline → **MYSQL** `RowChangeSink` | ⬜ 未开始 |
| **P1b** | Kafka Sink 真实投递 | `rds-kafka-sink` 接 `kafka-clients`，实现 `RowChangeSink` | ⬜ 未开始 |
| **P2** | MySQL 增量 | `IncrementalSource`（Debezium / Canal 适配）→ 两种 Sink 共用 | ⬜ 未开始 |
| **P3** | Oracle / PG 全量 | `*SnapshotSource` 接入同一 Pipeline | ⬜ 未开始 |

> 「P0 已交付」的含义是：**契约定稿且有测试保护**，不包含任何可运行的同步能力。

---

## P0 已交付（可以现在就依赖的部分）

| 交付物 | 位置 | 判据 |
|--------|------|------|
| 事件模型 | `rds-transfer-model` 的 `RowChange` / `DdlEvent` | 有字段与 Builder，可构造 |
| 同步模式语义 | `SyncMode`（含 `includesFull` / `includesIncremental` / `isCatchUpThenStop` / `parallelFullAndIncremental`） | `SyncModeTest` 全绿 |
| Sink 类型解析 | `SinkType`（`jdbc` / `rds` / `mysql` → `MYSQL`） | `SinkTypeTest` 全绿 |
| 全部 SPI | `SnapshotSource` / `IncrementalSource` / `RowChangeListener` / `DdlEventListener` / `RowChangePipeline` / `RowChangeSink` / `OffsetStore` + `MemoryOffsetStore` | `MemoryOffsetStoreTest` 全绿 |
| 配置与校验 | `RdsSyncConfig` + Builder + `toKafkaSinkConfig()` | `RdsSyncConfigTest` 全绿 |
| 锁定的装配点 | `SinkFactory.create()` 对 MYSQL / KAFKA 均抛 `UnsupportedOperationException` | `RdsSyncConfigTest.factoryLocksAssemblyPoint` |
| Kafka topic 命名 | `KafkaTopicMapper` | `KafkaTopicMapperTest` 全绿 |
| Kafka 行级 envelope | `RowChangeEnvelope`（字段与 key 映射） | `RowChangeEnvelopeTest` 全绿 |
| Kafka Producer 配置 | `KafkaSinkConfig`（归一化 bootstrap、默认值与兜底） | `KafkaSinkConfigTest` 全绿 |

### 自行核实现状

```bash
cd rds-sync

# 1) 实际有哪些模块（以 pom 为准，而不是文档）
grep -n "<module>" pom.xml

# 2) 有没有任何 SPI 实现类（预期：无输出）
grep -rn "implements RowChangeSink\|implements SnapshotSource\|implements IncrementalSource\|implements RowChangePipeline\|implements RdsSyncClient" \
  --include="*.java" . | grep -v "/target/"

# 3) 契约单测是否全绿
mvn -pl rds-transfer-model,rds-kafka-sink,rds-sync-client -am test
```

上面第 2 条**无输出**即为「尚无实现类」的判据。

---

## P1 起各阶段的验收判据

| 阶段 | 完成判据（可观测） |
|------|--------------------|
| P1 | `SinkFactory.create(mysqlCfg)` 不再抛异常；能把一张 MySQL 表经 Range 切分扫出并经 `RowChangeSink` 写入目标表；`progress()` 能给出全量进度；`pause` / `resume` 可用 |
| P1b | `sink.type=kafka` 时事件真实投递到 Kafka；topic 符合 `KafkaTopicMapper` 规则；`landedThrough()` 以 produce ack 推进；`kafka.publish.ddl=true` 时 DDL 消息可见 |
| P2 | `IncrementalSource` 接入后 `FULL_AND_INCREMENTAL` 可长时间运行；`lagMs` 稳定；`canCommit()` 在滞后达标后转 true；`commit()` 能完成最小 cutover |
| P3 | Oracle / PG 全量接入同一 Pipeline，**不改动** Pipeline / Sink 代码 |

---

## 文档与现实的已知不一致

阅读 `ARCHITECTURE.md` 等既有文档时，请注意以下几处与现实不符的地方（本文以**实际代码与 `pom.xml`** 为准）：

| 位置 | 文档写法 | 实际情况 |
|------|----------|----------|
| [ARCHITECTURE.md](../ARCHITECTURE.md) §8 模块表 | 列出 `realTimeOfMysql`（MySQL 增量适配层，空壳）与 `execute`（可选 CLI，空壳） | 仓库中**没有**这两个目录；根 `pom.xml` 只声明 9 个模块 |
| [ARCHITECTURE.md](../ARCHITECTURE.md) §3.2 topic 格式 | `{prefix}{sep}{schema}{sep}{table}{sep}{suffix}` | `KafkaTopicMapper` 在 `prefix` / `suffix` 为空时**不会**输出多余分隔符；`table` 为空时只输出 `schema` 段 |
| [ARCHITECTURE.md](../ARCHITECTURE.md) §3.3 配置键 | 只列 `source.uri` / `sink.type` / `sink.uri` / `sync.mode` / `bootstrap.table` / `kafka.*` | `RdsSyncConfig` 还实现了 `tableWhite` / `sinkSchema` / `bucketNum` / `batchSize` / `splitSlices` 与两端 user/password，但这些**尚未分配 `.properties` 键名** |
| [examples/README.md](../examples/README.md) | "当前为架构约定键，P1 起由 `RdsSyncClient` 读取" | 一致（无误）；但容易误读成「现在填文件就生效」 |
| [测试文档.md](../测试文档.md) | 文件存在 | **内容为空**（0 字节）；测试现状请以 `mvn ... test` 的实际结果为准 |

> 建议后续修订 `ARCHITECTURE.md` 时一并对齐上述几点，避免读者按不存在的模块或键去排查。

---

## 与 mongo-sync 的进度差距

mongo-sync 已在生产路径上具备全量 / 增量 / DDL / 校验 / CLI / 位点持久化 / HTTP 控制面；rds-sync 目前只有契约层。能力对照见 [运行与排障 · 与 mongo-sync 的能力对照](../guide/operations.md#与-mongo-sync-的能力对照判断现在该用哪个)。

需要立即可用的同步能力时，文档库请直接用 mongo-sync；关系库能力请等待 P1 起的交付。

---

返回：[手册首页](../README.md) · [ARCHITECTURE.md](../ARCHITECTURE.md) · QQ 交流群：**983986505**
