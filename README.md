# LogRadar - 企业级日志分析与智能告警平台

## 📌 项目简介
LogRadar 是一个基于 Spring Boot 的企业级日志分析平台，支持海量日志的采集、解析、存储、全文检索和智能告警。后端用 Java 处理日志数据，前端用简单的页面展示分析结果。

## 🛠️ 技术栈
- **核心框架**：Spring Boot 3.2.5、MyBatis-Plus 3.5.5
- **数据库**：MySQL 8.0
- **缓存**：Redis 7.x
- **搜索引擎**：Elasticsearch 7.17.28
- **消息队列**：RocketMQ 5.3.0
- **数据同步**：Canal 1.1.7（Binlog 监听）
- **网络编程**：Netty 4.x（TCP/UDP 双协议支持）
- **容器化**：Docker + Docker Compose
- **压测工具**：JMeter 5.6.3
- **限流组件**：Guava RateLimiter
- **前端**：Vue 3 + ECharts（简易仪表盘）

## 🏗️ 系统架构

```bash
┌──────────────────────────────────────────────┐
│ 日志接入层 (Ingestion) │
│ - HTTP REST API (Spring Boot) │
│ - TCP/UDP Syslog (Netty, 端口 5140) │
│ - Guava RateLimiter 接口限流 │
└────────────────────┬─────────────────────────┘
│
┌────────────────────▼─────────────────────────┐
│ 消息队列 (RocketMQ) │
│ - 异步削峰填谷 │
│ - 死信队列兜底 │
└────────────────────┬─────────────────────────┘
│
┌────────────────────▼─────────────────────────┐
│ 日志处理层 (Processor) │
│ - 责任链模式自动适配解析器 │
│ - JSON / Regex / Syslog (RFC 3164 & 5424) │
└──────────┬──────────────────────────────────┘
│
┌──────────▼──────────┐ ┌──────────────────────┐
│ MySQL (结构化存储) │ │ Elasticsearch (全文检索) │
│ - log_record │ │ - 倒排索引 │
│ - log_message │ │ - 聚合分析 │
│ - alert_rule │ │ - 幂等写入 (_id) │
│ - slow_query_log │ │ │
└──────────────────────┘ └──────────────────────┘
│ │
└────────┬───────────────┘
│
┌───────────────────▼──────────────────────────┐
│ Canal Binlog 同步 (兜底) │
│ - 伪装 MySQL 从库 │
│ - 代码零侵入 │
│ - 最终一致性终极保障 │
└───────────────────┬──────────────────────────┘
│
┌───────────────────▼──────────────────────────┐
│ 智能告警引擎 (Alert Engine) │
│ - Redis ZSET 滑动窗口 │
│ - 指数退避 + 冷却期 │
│ - 告警分级（钉钉/邮件/日志） │
│ - 规则热加载 │
└──────────────────────────────────────────────┘
```


## 🚀 核心功能模块

### 1. 日志采集与解析
- 支持 HTTP REST API 接收 JSON 格式日志
- 支持 TCP/UDP 协议解析 Syslog 设备日志（Netty 实现，标准 5140 端口）
- **责任链模式自动适配**：遍历解析器链，自动识别 JSON / Regex / Syslog 格式
- Syslog 兼容 RFC 3164（传统格式）和 RFC 5424（新格式）
- 自定义正则表达式解析日志字段（时间戳、级别、来源IP、消息）

### 2. 日志存储与检索
- MySQL 结构化存储日志核心字段（log_record）
- Elasticsearch 全文检索，支持 keyword + level + 时间范围任意组合搜索
- 百万级日志数据 P99 查询延迟 < 200ms
- ES 文档 `_id` 幂等写入，防止重复数据

### 3. 消息队列异步处理
- RocketMQ 异步缓冲，削峰填谷
- 消费者批量写入 ES（缓冲区攒够 100 条或每 5 秒刷新）
- 消费幂等性校验（`existsById`）
- 消费失败自动转入死信队列（log-topic-dlq）
- 死信消费者兜底写入，写入失败落本地消息表标记 FAILED

### 4. 数据一致性三层保障
- **第一层：应用层双写**（实时性，秒级可见）
- **第二层：本地消息表 + 定时补偿**（每 10 秒扫描 PENDING 消息重试，最多 5 次，超限标记 FAILED）
- **第三层：Canal Binlog 监听**（最终一致性兜底，代码零侵入）
- 健康检查接口 `GET /api/health/consistency` 实时对比 MySQL 与 ES 数据量

### 5. 智能告警引擎
- Redis ZSET 实现真正的滑动窗口（动态统计最近 N 秒内日志数量）
- 指数退避算法防抖，冷却期内不重复告警
- 告警分级通知（钉钉 / 邮件 / 日志打印）
- 规则热加载：`@Scheduled` 每 60 秒刷新内存缓存，无需重启

### 6. 聚合分析
- Elasticsearch 聚合分析，按日志级别分组统计
- MySQL 聚合查询作为兜底方案
- 双路对比验证数据一致性

### 7. 流量控制
- Guava RateLimiter 令牌桶限流，保护下游 MySQL 和 ES
- 限流阈值支持运行时动态调整（`PUT /api/admin/rate-limit`）
- 超出阈值返回 HTTP 429，提示稍后重试

### 8. 运维与管理
- 死信队列手动回放接口（`POST /api/mq/dlq/retry`），LitePullConsumer 主动拉取，秒级响应
- SENT 状态消息定时清理（每天凌晨 3 点清理 7 天前数据）
- 慢查询 AOP 监控（阈值 500ms，自动记录方法名和耗时）
- Knife4j 在线 API 文档

## 📊 压测数据

### JMeter 10万次并发压测
| 指标 | 数值 |
|------|------|
| 总请求数 | 100,000 |
| 并发线程数 | 1,000 |
| 平均响应时间 | 3427ms |
| QPS | 273.4/sec |
| 错误率 | 0.00% |

### 压测后数据一致性恢复
- 压测后 MySQL 与 ES 数据量差异：88,338 条
- 消费者大量消息堆积，触发死信队列保护机制
- 通过定时任务补偿 + 死信队列回放，3 分钟内将差异缩小至 9 条
- 最终数据一致性恢复时间：约 5 分钟

## 📡 API 接口文档

| URL | Method | 说明 |
|------|--------|------|
| `/api/logs` | POST | 日志上报（自动识别格式） |
| `/api/logs/search` | GET | 日志搜索（keyword/level/时间组合） |
| `/api/logs/aggregate` | GET | 聚合分析（MySQL） |
| `/api/logs/aggregate/es` | GET | 聚合分析（Elasticsearch） |
| `/api/health/consistency` | GET | 数据一致性检查 |
| `/api/mq/dlq/retry` | POST | 死信队列手动回放 |
| `/api/admin/rate-limit` | PUT | 动态调整限流阈值 |

### 日志上报

```bash
POST /api/logs
Content-Type: application/json

// JSON 格式
{
"timestamp": "2026-06-19T10:00:00",
"level": "ERROR",
"sourceIp": "192.168.1.1",
"message": "Connection timeout"
}

// Regex 格式
[2026-06-19T10:00:00] [ERROR] [192.168.1.1] Connection timeout

// Syslog RFC 3164
<3>Jun 19 10:30:45 webserver nginx[1234]: 502 Bad Gateway

// Syslog RFC 5424
<3>1 2026-06-19T10:30:45.000Z webserver nginx 1234 ID1 - 502 Bad Gateway
```

### 日志搜索

```bash
GET /api/logs/search?keyword=Connection&level=ERROR&startTime=2026-06-19T00:00:00&endTime=2026-06-19T23:59:59
```

### 数据一致性检查
```bash
GET /api/health/consistency

//返回示例：
{
"code": 200,
"data": {
"mysqlCount": 100000,
"esCount": 99997,
"diff": 3,
"status": "不一致",
"lastSyncTime": "2026-06-19T10:30:45"
}
}
```

### 动态调整限流阈值
```bash
PUT /api/admin/rate-limit?qps=1000
```


## 🔧 如何运行

### 1. 克隆项目
```bash
git clone https://github.com/hejiaxin123/log-radar.git
cd log-radar
```

### 2. 启动中间件

```bash
# MySQL
# 创建数据库 log_radar_db，执行项目 SQL 建表语句

# Redis
redis-server

# Elasticsearch
docker run -d --name es -p 9200:9200 -e "discovery.type=single-node" elasticsearch:7.17.28

# RocketMQ
docker compose -f docker-compose.yml up -d

# Canal（可选）
docker start canal-server
```

### 3. 启动应用
```bash
mvn spring-boot:run
```

### 4. 访问
```bash
应用端口：8084
Knife4j API 文档：http://localhost:8084/doc.html
```

## 🔧 重构记录
### 2026.02.07 — 本地消息表 + 事务保障
 - 问题：MQ 发送失败时数据丢失，无法保证最终一致性
 - 方案：引入 log_message 本地消息表，save() 加 @Transactional 保证 MySQL 写入与消息记录原子性，定时任务补偿重试
 - 效果：不丢消息，最终一致性得到保障

### 2026.02.09 — 责任链模式解析器
 - 问题：日志解析逻辑用 if-else 散落在 Controller 中，新增格式需改原有代码
 - 方案：定义 LogParser 接口，注入 List<LogParser> 遍历责任链，自动适配解析器
 - 效果：新增解析器只需加 @Component 实现接口，零改动原有代码

### 2026.02.16 — Syslog 双协议兼容
 - 问题：Syslog 解析器仅支持单一格式
- 方案：区分 RFC 3164 和 RFC 5424，入口通过版本号判断，分别解析
- 效果：兼容主流 Syslog 协议，Netty 接入层统一走责任链

### 2026.02.19 — 搜索条件全覆盖
- 问题：日志检索仅支持部分参数组合，keyword + level 同时传会丢失条件
- 方案：在 LogDocumentRepository 补齐方法命名查询，覆盖 7 种参数组合
- 效果：keyword / level / 时间范围任意组合均可正确检索

### 2026.02.22 — 死信回放优化
- 问题：回放接口用 Thread.sleep(5000) 硬等，响应慢
- 方案：改用 DefaultLitePullConsumer 主动拉取，拉完即停
- 效果：响应时间从固定 5 秒降到毫秒级

### 2026.02.25 — 接口限流
- 问题：高峰期日志上报无保护，下游有被打垮风险
- 方案：引入 Guava RateLimiter 令牌桶限流，支持运行时动态调整 QPS
- 效果：超出阈值返回 429，保护 MySQL / ES / MQ

## 📝 TODO

 - 本地消息表 + 定时补偿
- 责任链模式解析器
- Syslog RFC 3164 / 5424 双协议兼容
- 死信队列回放优化
- 接口限流 + 动态调整
- log_message 表定期清理
- 前端仪表盘完善
- 告警规则可视化配置
- 日志模板管理
- 多租户支持

## 👤 作者
[何家鑫] - [GitHub: https://github.com/hejiaxin123/log-radar]