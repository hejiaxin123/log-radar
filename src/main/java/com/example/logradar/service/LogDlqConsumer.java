package com.example.logradar.service;

import com.example.logradar.entity.LogDocument;
import com.example.logradar.entity.LogMessage;
import com.example.logradar.entity.LogRecord;
import com.example.logradar.mapper.LogMessageMapper;
import com.example.logradar.repository.LogDocumentRepository;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@RocketMQMessageListener(topic = "log-topic-dlq", consumerGroup = "log-radar-dlq-consumer")
public class LogDlqConsumer implements RocketMQListener<LogRecord> {

    private final LogMessageMapper logMessageMapper;
    private final LogDocumentRepository logDocumentRepository;

    public LogDlqConsumer(LogMessageMapper logMessageMapper, LogDocumentRepository logDocumentRepository) {
        this.logMessageMapper = logMessageMapper;
        this.logDocumentRepository = logDocumentRepository;
    }

    @Override
    public void onMessage(LogRecord log) {
        // 1. 尝试写入 ES
        try {
            logDocumentRepository.save(new LogDocument(log));
            System.out.println("死信消息写入 ES 成功：" + log.getId());
        } catch (Exception e) {
            // 2. ES 写入失败，记录到本地消息表
            LogMessage msg = new LogMessage();
            msg.setLogId(log.getId());
            msg.setStatus("FAILED");
            msg.setRetryCount(0);
            msg.setCreateTime(LocalDateTime.now());
            logMessageMapper.insert(msg);
            System.err.println("死信消息记录到数据库：" + log.getId());
        }
    }
}