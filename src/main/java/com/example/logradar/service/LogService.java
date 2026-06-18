package com.example.logradar.service;



import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.logradar.entity.LogDocument;
import com.example.logradar.entity.LogMessage;
import com.example.logradar.entity.LogRecord;
import com.example.logradar.mapper.LogMapper;
import com.example.logradar.mapper.LogMessageMapper;
import com.example.logradar.repository.LogDocumentRepository;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
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

    public LogService(LogDocumentRepository logDocumentRepository, LogProducer logProducer,
                      LogMessageMapper logMessageMapper, RestTemplate restTemplate) {
        this.logDocumentRepository = logDocumentRepository;
        this.logProducer = logProducer;
        this.logMessageMapper=logMessageMapper;
        this.restTemplate = restTemplate;
    }

    //日志上报：写 MySQL 的同时同步写 ES,添加本地消息表逻辑
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

        // 3. 尝试发送 MQ
        try {
            logProducer.send("log-topic", log);
            msg.setStatus("SENT");
            logMessageMapper.updateById(msg);
        } catch (Exception e) {
            // 发送失败，由定时任务补偿重试
            System.err.println("MQ 发送失败，进入补偿队列：" + e.getMessage());
        }
        return true;
    }

    // 日志搜索：从 ES 查询
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

    // 日志解析
    public LogRecord parseLog(String rawLog) {
        // 正则匹配：时间戳 级别 IP 消息
        String regex = "\\[(.*?)\\]\\s*\\[(.*?)\\]\\s*\\[(.*?)\\]\\s*(.*)";
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(regex);
        java.util.regex.Matcher matcher = pattern.matcher(rawLog);

        if (matcher.find()) {
            LogRecord log = new LogRecord();
            log.setTimestamp(LocalDateTime.parse(matcher.group(1).replace(" ", "T")));
            log.setLevel(matcher.group(2));
            log.setSourceIp(matcher.group(3));
            log.setMessage(matcher.group(4));
            return log;
        }
        return null;
    }

    //Sys格式日志解析
    public LogRecord parseSyslog(String rawLog) {
        // Syslog 格式：<优先级>时间戳 主机名 消息
        try {
            String[] parts = rawLog.split(" ", 4);
            if (parts.length < 4) return null;
            LogRecord log = new LogRecord();
            log.setTimestamp(LocalDateTime.now());
            log.setLevel("INFO"); // Syslog 默认级别
            log.setSourceIp(parts[2]); // 主机名作为来源IP
            log.setMessage(parts[3]); // 消息内容
            return log;
        } catch (Exception e) {
            return null;
        }
    }

    // 聚合分析
    public Map<String, Long> aggregateByLevel() {
        Map<String, Long> result = new HashMap<>();
        for (String level : new String[]{"INFO", "WARN", "ERROR", "DEBUG"}) {
            LambdaQueryWrapper<LogRecord> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(LogRecord::getLevel, level);
            result.put(level, count(wrapper));
        }
        return result;
    }

    //直接用 HTTP 调用 ES 的聚合 API
    public Map<String, Long> aggregateByLevelES() {
        String url = "http://localhost:9200/log_radar/_search";
        String query = "{\"size\":0,\"aggs\":{\"by_level\":{\"terms\":{\"field\":\"level\"}}}}";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> request = new HttpEntity<>(query, headers);
        ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);
        // 解析返回结果
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
}