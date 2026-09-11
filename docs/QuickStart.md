## QuickStart

本仓库正在从 PhotonT 关系型资产演进为 **rds-sync** SDK，启动方式以架构文档为准。

当前请先阅读：

- [ARCHITECTURE.md](ARCHITECTURE.md)
- 根目录 [README.md](../README.md)

### 构建

```bash
mvn -DskipTests package
```

### 说明

历史 PhotonT 平台版数据源配置示例（含 Mongo、HDFS 等）已废弃。  
本产品仅面向关系型数据库：MySQL / Oracle / PostgreSQL。
