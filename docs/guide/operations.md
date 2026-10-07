# 运行与排障

> ⚠️ 本仓**当前没有可运行的生产链路**（P0 契约阶段）。本页的重点不是「怎么调度、怎么调优」，而是**怎么区分「功能还没做」和「配置写错了」**，以及现在能做哪些验证。

---

## 现状：现在有哪些「运行期」的东西

| 项 | 状态 |
|----|------|
| 端到端同步 | ❌ 无 |
| 进程入口 / CLI | ❌ 无（架构文档提到的 `execute` 模块目录尚未建立） |
| 配置文件加载 | ❌ 无（`rds-sync-client` 没有 properties 加载器） |
| 日志 | ⚠️ 仓库根有 `logs/log.log`、`logs/error.log`，当前均为 0 字节（早期遗留，无写入方） |
| 契约单测 | ✅ 可跑，见下 |

### 现在能做的验证

```bash
cd rds-sync && mvn -pl rds-transfer-model,rds-kafka-sink,rds-sync-client -am test
```

这组测试保护的就是「契约不被悄悄改坏」：`SyncMode` 语义、`SinkType` 解析（`jdbc` / `rds` → `MYSQL`）、`MemoryOffsetStore` 存取、`RdsSyncConfig` 校验与 Kafka 归一化、`KafkaTopicMapper` 命名、`RowChangeEnvelope` 字段映射。

**改动契约（枚举、SPI 签名、配置语义、Kafka 消息格式）前后都必须跑通它** —— 因为现在没有端到端链路，这组单测是唯一的回归防线。

---

## 状态机与可用操作

设计语义（与 mongo-sync 对齐，实现待 P1）：

| API | 语义 | 前置条件 |
|-----|------|----------|
| `start()` | 启动；`PAUSED` 时续跑，**不**重跑已完成的全量 | 未 `STOPPED` |
| `resume()` | 恢复 | 非初始全量进行中 |
| `pause()` | 全量 + 增量都停 | 已启动；非初始全量进行中；非 `COMMITTING` / `COMMITTED` |
| `pauseIncremental()` | 只冻增量 | 已启动；非 `PAUSED` / `COMMITTING` / `COMMITTED`；模式含增量 |
| `resumeIncremental()` | 恢复增量 | 非整体 `PAUSED` |
| `progress()` | 进度快照 | 随时 |
| `canCommit()` | 全量完成 + 管道排空 + lag ≤ 阈值 | — |
| `commit()` | 停捕获 + 排空 + `COMMITTED` | 处于 `CAN_COMMIT` |
| `stop()` / `close()` | 优雅停止 | 随时 |

约束的**理由**（与 mongo-sync 相同）：

- 初始全量进行中**不允许 `pause()`** —— 否则快照重放会导致重复写入；此时应使用 `pauseIncremental()`
- `resume()` **不重跑**已完成的全量 —— 保证「暂停/恢复」是安全操作

---

## 当前缺失与限制

| 类别 | 缺失 | 影响 |
|------|------|------|
| 编排 | `RdsSyncClient` 只有接口，**无实现类** | 无法启动任务 |
| 装配 | `SinkFactory.create()` 一律抛 `UnsupportedOperationException` | 无法拿到 Sink |
| 配置 | 无 properties 加载器 | `.properties` 键目前**不生效**，只能用 Builder |
| Source | `*Source` 模块未适配 `SnapshotSource` SPI（仍走内部 `BatchDataEntity` + `MemoryCache`） | 不能当 SDK 用 |
| Sink | `rds-mysql-sink` 未实现 `RowChangeSink` | 不能写目标库 |
| Kafka | 有 topic / envelope 契约，**无 `kafka-clients` Producer** | 不能真实投递 |
| 增量 | 无 `IncrementalSource` 实现；Debezium / Canal 适配器未开始 | 无增量 |
| 位点 | 只有 `MemoryOffsetStore` | 进程重启不续传 |
| 错误码 | 未定义（mongo-sync 有 `MSYNC_*` 一套） | 排障时缺少按码定位 |
| CLI | 无 | 只能嵌入（且当前无可嵌入的实现） |
| 遗留 API | 全局整数 `ProStatus` 已**废弃**为产品 API | 新代码不得依赖 |

---

## 与 mongo-sync 的能力对照（判断现在该用哪个）

| 能力 | rds-sync | mongo-sync |
|------|----------|------------|
| 全量同步（可运行） | ❌ P1 | ✅ |
| 增量 / ChangeStream / Oplog | ❌ P2 | ✅ |
| Kafka Sink（真实投递） | ❌ P1b | ✅ |
| 数据校验工具 | ❌ | ✅ `verify.sh` |
| CLI / 脚本启动 | ❌ | ✅ `bin/mongosync.sh` |
| HTTP 控制面 | ❌ | ✅（可选） |
| 位点持久化 | ❌（仅内存） | ✅（`offset.store.dir`） |
| 契约与配置装配点 | ✅ P0 | ✅ |

**结论**：需要**立刻可用**的关系库同步能力时，本仓目前给不了；文档库场景请直接用 [mongo-sync](https://github.com/whaleal-dev/mongo-sync)。本仓现在适合的是**对齐契约、写适配器、评审架构**。

---

## 排障

现在的「故障」基本只有两类，区分清楚即可：

| 现象 | 判断 |
|------|------|
| 调 `SinkFactory.create(cfg)` 抛 `UnsupportedOperationException` | **不是配错**，是 P1 / P1b 未实现。异常信息里已写明指向哪个阶段 |
| 把 `.properties` 填好却发现没任何效果 | **不是配错**，是还没有加载器。改用 Builder（见 [配置项全解](configuration.md)） |
| 写 `RdsSyncClient client = new ...` 编译不过 | 它是**接口**，无实现类；P1 起才可实例化 |
| `toKafkaSinkConfig()` 抛 `IllegalStateException` | 配置问题：该方法要求 `sink.type=kafka` |
| `build()` 抛 `IllegalArgumentException` | 配置问题：缺 `sourceUri`，或按 `sink.type` 缺 `sinkUri` |
| 契约单测变红 | 契约被改动且未同步更新测试 —— 先确认是有意变更，再改测试 |

> 有疑问时先跑一遍契约单测：它能快速告诉你是「代码/契约问题」还是「你的用法问题」。

---

## 常见问题（FAQ）

### 现在能用吗？

不能用于生产同步。当前是 P0 契约阶段：**事件模型、SPI、配置装配点、Kafka 消息契约已定稿并有单测保护**，但执行链路（全量 Source / MySQL Sink / Kafka Producer / 增量）尚未实现。落地顺序见 [落地状态与路线](../reference/roadmap.md)。

### 什么时候能用？先能做什么？

- **P1**：MySQL 全量（Range 切分 → 流水线 → MySQL `RowChangeSink`）—— 第一个可用里程碑
- **P1b**：Kafka Sink 接 `kafka-clients`，实现 `RowChangeSink`
- **P2**：MySQL 增量（`IncrementalSource`，借 Debezium / Canal）
- **P3**：Oracle / PG 全量接入同一 Pipeline

### 我可以先把变更投到 Kafka 吗？

现在不行 —— topic 与 envelope 契约已定，但没有 Producer（P1b）。若下游只需要文档库变更，可先用 [mongo-sync](https://github.com/whaleal-dev/mongo-sync) 的 Kafka Sink（注意：那是 Change Stream 格式，与本仓行级 envelope **不同**）。

### 能直接依赖 `rds-mysql-source` 当 SDK 吗？

不能。它是内部链路（`BatchDataEntity` + `MemoryCache`），未适配 `SnapshotSource`；对外契约只有 `RowChange` / `DdlEvent`。

### 能和 mongo-sync 串成一条链路吗？

不能。两者数据面不同（`RowChange` vs `TransferEvent`，行级 envelope vs Change Stream），**没有**官方 Mongo ↔ 关系库适配器。正确做法是各自独立部署。

### 国产化（达梦 / TiDB / 人大金仓）支持吗？

方向上是「按 Sink JDBC 能力验证」：架构把 Sink 抽象成 `RowChangeSink`，新增一个 JDBC Sink 不需改动 Source / Pipeline。但目前**尚未实现任何非 MySQL 的 Sink**，请以 P3 之后的实际交付为准。

### 增量会自研 binlog 解析吗？

不会。原则是「能借就不造」：MySQL 增量借 Debezium Embedded（首选）或 Canal，经适配器输出 `RowChange`；Oracle / PG 同理。三方包**不得泄露到对外 SDK**。

---

## 变更契约时的检查清单

改动下面任何一项，都属于**破坏性变更**，必须同步更新测试与文档：

- [ ] `RowChange` / `DdlEvent` 字段
- [ ] SPI 接口签名（`RowChangeSink` / `SnapshotSource` / `IncrementalSource` / `RowChangePipeline` / `OffsetStore`）
- [ ] `SyncMode` / `SinkType` 枚举值及其解析规则
- [ ] `RdsSyncConfig` 的键语义与默认值
- [ ] Kafka topic 命名规则与 envelope 字段
- [ ] 已跑通 `mvn -pl rds-transfer-model,rds-kafka-sink,rds-sync-client -am test`

---

返回：[手册首页](../README.md) · QQ 交流群：**983986505**
