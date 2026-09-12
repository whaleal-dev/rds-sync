# QuickStart

**rds-sync**：关系型数据库同步 SDK（MySQL / Oracle / PostgreSQL）。  
文档库请用 [mongo-sync](https://github.com/whaleal-dev/mongo-sync)。

本仓库正在从 PhotonT 关系型资产演进为嵌入式 SDK，启动方式以架构文档为准。当前是 **P0 架构契约**。

## 阅读顺序

1. 根目录 [README.md](../README.md) — 定位、怎么选、模块  
2. [ARCHITECTURE.md](ARCHITECTURE.md) — 事件契约、控制面、落地顺序  
3. [examples](examples/) — `target.type=mysql|kafka` 配置键  

## 构建

```bash
cd rds-sync
mvn -DskipTests package
```

契约单测（不含 PhotonT 全量模块）：

```bash
mvn -pl rds-transfer-model,rds-kafka-sink,rds-sync-client -am test
```

## 范围

| 在本仓 | 不在本仓 |
|--------|----------|
| MySQL / Oracle / PostgreSQL 全量源（PhotonT 代码在，**未接**产品 SPI） | MongoDB / DocumentDB → 见 [mongo-sync](https://github.com/whaleal-dev/mongo-sync) |
| MySQL JDBC 目标、**Kafka 目标**（行级 envelope；Producer 待 P1b） | Hadoop / HDFS（已移除） |
| `RowChange` / SPI / `RdsSyncConfig` | 自研 binlog/redo/WAL；Kafka Connect 发行版 |

`mysqlSource` 等不能直接当 SDK 用。详见 [ARCHITECTURE.md](ARCHITECTURE.md) §8.1 / §10。

两边的 `pauseIncremental` / `canCommit` 语义一致，运维习惯可共用；没有官方 Mongo ↔ MySQL 直连。
