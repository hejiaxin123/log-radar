# LogRadar - 企业级日志分析与智能告警平台

## 📌 项目简介
LogRadar 是一个基于 Spring Boot 的企业级日志分析平台，支持海量日志的采集、解析、存储、全文检索和智能告警。后端用 Java 处理日志数据，前端用简单的页面展示分析结果。

## 🛠️ 技术栈
- **核心框架**：Spring Boot 3.2.5、MyBatis-Plus 3.5.5
- **数据库**：MySQL 8.0
- **缓存**：Redis 7.x
- **搜索引擎**：Elasticsearch 7.17.28
- **消息队列**：RocketMQ 5.3.0
- **数据同步**：Canal 1.1.7(Binlog 监听)
- **网络编程**：Netty 4.x(TCP/UDP 协议解析)
- **容器化**：Docker + Docker Compose
- **压测工具**：JMeter 5.6.3
- **前端**：Vue 3 + ECharts(简易仪表盘)

## 🏗️ 系统架构

```
┌─────────────────────────────────────────┐
│ 日志接入层 (Ingestion) │
│ - HTTP REST API (Spring Boot) │
│ - TCP/UDP Syslog (Netty) │
└──────────────────┬──────────────────────┘
│
┌──────────────────▼──────────────────────┐
│ 消息队列 (RocketMQ) │
└──────────────────┬──────────────────────┘
│
┌──────────────────▼──────────────────────┐
│ 日志处理层 (Processor) │
│ - 格式解析 (正则表达式) │
│ - 自定义正则模板 │
└──────────┬───────────────────────────────┘
│
┌──────────▼──────────┐ ┌──────────────────┐
│ MySQL (结构化存储) │ │ Elasticsearch │
│ │ │ (全文检索) │
└──────────────────────┘ └──────────────────┘
│ │
┌──────────▼───────────────────────▼─────────┐
│ 智能告警引擎 (Alert Engine) │
│ - Redis 滑动窗口 (ZSET) │
│ - 指数退避防抖 │
│ - 告警收敛与静默 │
│ - 规则热加载 │
└─────────────────────────────────────────────┘
```

## 🚀 核心功能模块
### 1. 日志采集与解析
- 支持 HTTP REST API 接收 JSON 格式日志
- 支持 TCP/UDP 协议解析 Syslog 设备日志（Netty 实现）
- 自定义正则表达式解析日志字段（时间戳、级别、来源IP、消息）

### 2. 日志存储与检索
- MySQL 结构化存储日志核心字段
- Elasticsearch 全文检索，支持关键字、级别、时间范围搜索
- 百万级日志数据 P99 查询延迟 < 200ms

### 3. 消息队列异步处理
- RocketMQ 异步缓冲，削峰填谷
- 本地消息表 + 定时任务补偿，保证数据最终一致性

### 4. Canal Binlog 同步
- 监听 MySQL Binlog，自动同步数据到 Elasticsearch
- 代码零侵入，全表同步

### 5. 智能告警引擎
- Redis ZSET 实现真正的滑动窗口（动态统计最近 N 秒内日志数量）
- 指数退避算法防抖，冷却期内不重复告警
- 告警规则支持热加载，无需重启服务

### 6. 聚合分析
- Elasticsearch 聚合分析，按日志级别分组统计
- MySQL 聚合查询作为兜底方案

## 📊 压测数据 (JMeter)
| 指标 | 数值 |
|------|------|
| 并发线程数 | 100 |
| 循环次数 | 10 |
| 总请求数 | 10000 |
| 平均响应时间 | 2771ms |
| 错误率 | 0.00% |
| 吞吐量 (QPS) | 16.2/sec |
| 最小/最大响应时间 | 15ms / 32916ms |

## 📡 API 接口文档
| URL | Method | 说明 |
|------|--------|------|
| `/api/logs` | POST | 日志上报（JSON 格式） |
| `/api/logs/parse` | POST | 日志解析（正则格式） |
| `/api/logs/search` | GET | 日志搜索（关键字/级别/时间） |
| `/api/logs/aggregate` | GET | 聚合分析（MySQL） |
| `/api/logs/aggregate/es` | GET | 聚合分析（Elasticsearch） |

## 🔧 如何运行
### 1. 克隆项目
```bash
git clone [https://github.com/hejiaxin123/log-radar]

# Redis (Windows)
redis-server

# Elasticsearch
docker run -d --name es -p 9200:9200 -e "discovery.type=single-node" elasticsearch:7.17.28

# RocketMQ (docker-compose)
cd E:\rocketmq
docker compose up -d

# Canal
docker start canal-server

CREATE DATABASE log_radar_db;
-- 执行项目中的 SQL 建表语句

mvn spring-boot:run
```

## 📝 TODO
 - 前端仪表盘完善
 - 告警规则可视化配置
 - 日志模板管理
 - 多租户支持

## 👤 作者
**[何家鑫] - [我的GitHub链接:https://github.com/hejiaxin123/log-radar]**

