# 配置示例

当前为架构约定键，P1 起由 `RdsSyncClient` 读取。

- MySQL Sink：[rds-sync.example.properties](./rds-sync.example.properties)
- Kafka Sink：[rds-sync-kafka.example.properties](./rds-sync-kafka.example.properties)

Kafka 消息是行级 envelope（`op`/`before`/`after`）。文档库 Change Stream 格式见 [mongo-sync](https://github.com/whaleal-dev/mongo-sync)。
