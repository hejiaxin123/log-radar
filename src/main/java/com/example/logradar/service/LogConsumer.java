package com.example.logradar.service;

import com.example.logradar.entity.LogDocument;
import com.example.logradar.entity.LogRecord;
import com.example.logradar.repository.LogDocumentRepository;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

@Component
@RocketMQMessageListener(topic = "log-topic", consumerGroup = "log-radar-consumer")
public class LogConsumer implements RocketMQListener<LogRecord> {
    private final LogDocumentRepository logDocumentRepository;
    public LogConsumer(LogDocumentRepository logDocumentRepository) {
        this.logDocumentRepository = logDocumentRepository;
    }

    @Override
    public void onMessage(LogRecord log) {
        // 消费消息，写入 ES
        logDocumentRepository.save(new LogDocument(log));
        System.out.println("ES 写入成功: " + log.getMessage());
    }
}