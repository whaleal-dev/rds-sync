# rds-kafka-sink

rds-sync 的 **Kafka 目标**：把 `RowChange` / `DdlEvent` 写成 **行级 envelope**。

当前模块锁定 topic 与消息契约，**不含** `kafka-clients` Producer（P1b 再接）。Pipeline 只认 `RowChangeSink`，本模块是 KAFKA 侧实现位。

```text
Source → RowChange / DdlEvent → Pipeline → RowChangeSink
                                           └─ P1b：本模块 Producer
```

下游按表消费，或再经 JDBC Sink 落库。消息格式 **不是** [mongo-kafka](https://www.mongodb.com/docs/kafka-connector/current/) Change Stream；文档库投递请用 [mongo-sync](https://github.com/whaleal-dev/mongo-sync)。

## 消息格式

| Kafka | 内容 |
|-------|------|
| key | 主键列对象（无主键则为空对象；P1b 可改为整行 hash） |
| value | JSON envelope：`op` / `schema` / `table` / `ts_ms` / `source` / `before` / `after` |
| DDL | `op=ddl`，另含 `sql` / `ddl_type`；默认发到同 topic，可配 `kafka.ddl.topic` |

`op` 与 `RowChange` 一致：`c` / `u` / `d` / `r`。

Topic 默认 `{prefix}{sep}{schema}{sep}{table}{sep}{suffix}`；`kafka.topic` 非空时全部走固定 topic。

## 状态

| 已定 | 待 P1b |
|------|--------|
| `KafkaSinkConfig` / `KafkaTopicMapper` / `RowChangeEnvelope` | `KafkaSinkClient` 实现 `RowChangeSink` |
| 配置键与 mongo-sync Kafka 目标同构 | `kafka-clients` Producer |

通常不必直接使用本模块，由 `RdsSyncClient` 在 `target.type=kafka` 时装配。
