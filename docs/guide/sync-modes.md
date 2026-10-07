# 同步模式

`sync.mode` 的语义与命名**与 [mongo-sync](https://github.com/whaleal-dev/mongo-sync) 完全对齐**，便于同一套运维习惯。

> 语义已定稿并被单测保护；**执行侧**（Source / Pipeline / Sink）尚在 P1–P3 实现中。见 [落地状态与路线](../reference/roadmap.md)。

---

## 四种模式

| 枚举 | 行为 | 全量与增量 | 何时结束 |
|------|------|------------|----------|
| `FULL`（**默认**） | 仅全量 | 只有全量 | 全量扫完即结束 |
| `INCREMENTAL` | 仅增量 | 只有增量 | 不自动结束 |
| `FULL_AND_INCREMENTAL` | 全量 ∥ 增量**并行**，全量结束后持续增量 | 并行 | 不自动结束（直到 `commit` / 停止） |
| `FULL_THEN_CATCH_UP` | 先全量，再追增量窗口，追平后停止 | **串行** | 追平上界后停止 |

### 命名约定（务必记住）

- **`AND`** = 全量与增量**并行**，并且增量**持续**
- **`THEN`** = **先**全量，**再**追平，**再**停

`SyncMode` 还提供几个语义判断方法，实现侧依赖它们而非硬编码枚举：

| 方法 | 含义 |
|------|------|
| `includesFull()` | `FULL` / `FULL_AND_INCREMENTAL` / `FULL_THEN_CATCH_UP` |
| `includesIncremental()` | `INCREMENTAL` / `FULL_AND_INCREMENTAL` / `FULL_THEN_CATCH_UP` |
| `isCatchUpThenStop()` | 仅 `FULL_THEN_CATCH_UP` |
| `parallelFullAndIncremental()` | 仅 `FULL_AND_INCREMENTAL` |

`SyncMode.parse(String)` 对空值兜底为 `FULL`，并且**大小写不敏感**（内部会转大写）。

---

## 各模式详解

### `FULL` —— 仅全量（当前默认）

- 只做 JDBC 扫表 + Range 切分 + 批写入，不消费变更日志
- 适合：一次性搬迁、冷数据同步、做全量基线

### `INCREMENTAL` —— 仅增量

- 不做全量，直接消费 binlog / redo / WAL（经 Debezium / Canal 适配为 `RowChange`）
- **前提：目标端已有与源端一致的全量基线**
- 适合：备端已同步完成，现在只需持续追增量

### `FULL_AND_INCREMENTAL` —— 全量 ∥ 增量（不停服主路径）

```text
时间轴 ──────────────────────────────────────────────►
        │◄──── 全量扫表（Range 切分，可能数小时）────►│
        │◄──────── 增量持续消费 ───────────────────────►│
                            ▲
                    全量结束：排空/刷写后持续增量
```

- 全量与增量**并行**，全量结束后增量继续
- 增量的起始位点取自全量**开始之前** ⇒ 与全量必然存在**重叠窗口**
- 重叠部分靠 **主键有序 + UPSERT** 兜底，不会产生重复行
- 适合：**不停服迁库 / 灾备**

> ⚠️ 并行模式的风险在**捕获窗口**：全量耗时越长，源端 binlog 越可能被清理，位点一旦被覆盖就断链。上线前请确认源端日志保留时长足够覆盖全量耗时。

### `FULL_THEN_CATCH_UP` —— 先全量，再追平，再停（串行）

- 全量与增量**串行**：先扫完全量，再开启增量把窗口内的变更追平，追平后**停止**
- 适合：需要明确「切换时点」的一次性迁移

---

## 幂等与有序

| Sink 形态 | 重叠/重复如何处理 |
|-----------|-------------------|
| `MYSQL` | 按主键分桶保证有序；写侧用 UPSERT 兜底重复 |
| `KAFKA` | 按主键做 key 分区；同主键先 `flushAndWait`，下游按 key 有序消费 |

Pipeline 的完整约定见 [概念总览 · Pipeline 约定](../concepts/overview.md#3-pipeline-约定)。

---

## 怎么选

| 你的情况 | 选它 |
|----------|------|
| 不停服迁库 / 灾备（源持续写） | `FULL_AND_INCREMENTAL` |
| 一次性搬迁，要明确的结束点 | `FULL_THEN_CATCH_UP` |
| 目标端已有全量基线，只需追增量 | `INCREMENTAL` |
| 只搬冷数据 / 只做一次性全量 | `FULL` |
| 要把变更投递 Kafka 给下游 | 通常 `FULL_AND_INCREMENTAL`（先给基线，再持续投递） |

---

## 停止与提交

| 动作 | 语义 |
|------|------|
| `stop()` / 进程退出 | 停捕获、排空、关资源 |
| `pause()` | 全量 + 增量都停（初始全量进行中受限） |
| `pauseIncremental()` / `resumeIncremental()` | 只冻增量，全量继续 |
| `canCommit()` → `commit()` | 最小 cutover：全量完成 + 管道排空 + lag 达标后停捕获并标记 `COMMITTED` |

状态机与各 API 的完整语义见 [概念总览 · 状态机与控制面](../concepts/overview.md#7-状态机与控制面)。

---

下一步：[SDK 与 SPI](api.md) · [运行与排障](operations.md)
