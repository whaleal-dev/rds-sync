# rds-sync

关系型数据库同步 SDK（方案 C：Hybrid）。专注 MySQL / Oracle / PostgreSQL，不包含 Mongo、Hadoop/HDFS。

- **架构契约**：[docs/ARCHITECTURE.md](docs/ARCHITECTURE.md)
- **Mongo 同步**：请使用 [mongo-sync](https://github.com/whaleal-dev/mongo-sync)

## 模块（目标）

| 模块 | 说明 |
|------|------|
| `rds-transfer-model` | `RowChange` / `DdlEvent` 契约 |
| `common` / `core` | 列类型、切分、JDBC、线程池（PhotonT 资产） |
| `mysqlSource` / `mysqlTarget` | MySQL 全量源 / 目标 |
| `oracleSource` / `pgSource` | Oracle / PG 全量源 |
| `realTimeOfMysql` | MySQL 增量（待接） |
| `rds-sync-client` | 编排 SDK（骨架） |

## 构建

```bash
mvn -DskipTests package
```
