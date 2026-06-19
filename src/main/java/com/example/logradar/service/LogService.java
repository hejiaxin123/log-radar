package com.example.logradar.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.logradar.entity.LogDocument;
import com.example.logradar.entity.LogMessage;
import com.example.logradar.entity.LogRecord;
import com.example.logradar.mapper.LogMapper;
import com.example.logradar.mapper.LogMessageMapper;
import com.example.logradar.parser.LogParser;
import com.example.logradar.repository.LogDocumentRepository;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;  // ⬅️ 新增导入
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class LogService extends ServiceImpl<LogMapper, LogRecord> {

    private final LogProducer logProducer;
    private final LogDocumentRepository logDocumentRepository;
    private final LogMessageMapper logMessageMapper;
    private final RestTemplate restTemplate;
    private final AlertService alertService;
    private final List<LogParser> parsers;

    public LogService(LogDocumentRepository logDocumentRepository, LogProducer logProducer,
                      LogMessageMapper logMessageMapper, RestTemplate restTemplate,
                      AlertService alertService, List<LogParser> parsers) {
        this.logDocumentRepository = logDocumentRepository;
        this.logProducer = logProducer;
        this.logMessageMapper = logMessageMapper;
        this.restTemplate = restTemplate;
        this.alertService = alertService;
        this.parsers = parsers;
    }

    // 日志上报：写 MySQL 的同时记录本地消息表，保证原子性
    @Transactional  //  新增：保证步骤1和2原子执行
    @Override
    public boolean save(LogRecord log) {
        // 1. 先写 MySQL
        super.save(log);

        // 2. 记录本地消息表（状态：待发送）
        LogMessage msg = new LogMessage();
        msg.setLogId(log.getId());
        msg.setStatus("PENDING");
        msg.setRetryCount(0);
        msg.setCreateTime(LocalDateTime.now());
        logMessageMapper.insert(msg);

        // 3. 异步告警检查（不阻塞上报接口，也不在事务内回滚）
        alertService.checkAlert(log.getLevel());

        // 4. 尝试发送 MQ（发送失败不回滚MySQL，由定时任务补偿）
        try {
            logProducer.send("log-topic", log);
            msg.setStatus("SENT");
            logMessageMapper.updateById(msg);
        } catch (Exception e) {
            // 发送失败，由 MessageRetryTask 定时补偿重试，不修改状态
            System.err.println("MQ 发送失败，进入补偿队列：" + e.getMessage());
        }
        return true;
    }

    // 日志解析
    public LogRecord parseLog(String raw) {
        for (LogParser parser : parsers) {
            LogRecord log = parser.parse(raw);
            if (log != null) return log;
        }
        return null;
    }

    // 日志检索：从 ES 查询
    public List<LogDocument> search(String keyword, String level, LocalDateTime startTime, LocalDateTime endTime) {
        if (keyword != null && !keyword.isEmpty() && startTime != null && endTime != null) {
            return logDocumentRepository.findByMessageContainingAndTimestampBetween(keyword, startTime, endTime);
        }
        if (level != null && !level.isEmpty() && startTime != null && endTime != null) {
            return logDocumentRepository.findByLevelAndTimestampBetween(level, startTime, endTime);
        }
        if (keyword != null && !keyword.isEmpty()) {
            return logDocumentRepository.findByMessageContaining(keyword);
        }
        if (level != null && !level.isEmpty()) {
            return logDocumentRepository.findByLevel(level);
        }
        return (List<LogDocument>) logDocumentRepository.findAll();
    }

    // MySQL 聚合分析
    public Map<String, Long> aggregateByLevel() {
        Map<String, Long> result = new HashMap<>();
        for (String level : new String[]{"INFO", "WARN", "ERROR", "DEBUG"}) {
            LambdaQueryWrapper<LogRecord> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(LogRecord::getLevel, level);
            result.put(level, count(wrapper));
        }
        return result;
    }

    // ES 聚合分析
    public Map<String, Long> aggregateByLevelES() {
        String url = "http://localhost:9200/log_radar/_search";
        String query = "{\"size\":0,\"aggs\":{\"by_level\":{\"terms\":{\"field\":\"level\"}}}}";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> request = new HttpEntity<>(query, headers);
        ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);
        Map<String, Object> body = response.getBody();
        Map<String, Object> aggs = (Map<String, Object>) body.get("aggregations");
        Map<String, Object> byLevel = (Map<String, Object>) aggs.get("by_level");
        List<Map<String, Object>> buckets = (List<Map<String, Object>>) byLevel.get("buckets");
        Map<String, Long> result = new HashMap<>();
        for (Map<String, Object> bucket : buckets) {
            result.put((String) bucket.get("key"), ((Number) bucket.get("doc_count")).longValue());
        }
        return result;
    }

    public LocalDateTime getLastSyncTime() {
        return LocalDateTime.now();
    }
}