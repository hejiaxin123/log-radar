package com.example.logradar.service;

import com.example.logradar.entity.LogDocument;
import com.example.logradar.entity.LogRecord;
import com.example.logradar.repository.LogDocumentRepository;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Component
// RocketMQ 默认单线程消费，后续可通过配置扩消费者实例数
// 比如部署多个实例，或设置 consumeThreadMax = 10
@RocketMQMessageListener(topic = "log-topic", consumerGroup = "log-radar-consumer")
public class LogConsumer implements RocketMQListener<LogRecord> {

    private final LogDocumentRepository logDocumentRepository;
    private final RocketMQTemplate rocketMQTemplate;
    // 批量写入缓冲区
    private final List<LogDocument> buffer = Collections.synchronizedList(new ArrayList<>());
    private static final int BATCH_SIZE = 100;

    public LogConsumer(LogDocumentRepository logDocumentRepository, RocketMQTemplate rocketMQTemplate) {
        this.logDocumentRepository = logDocumentRepository;
        this.rocketMQTemplate = rocketMQTemplate;
    }

    @Override
    public void onMessage(LogRecord log) {
        try {
            // 幂等性检查
            if (logDocumentRepository.existsById(log.getId())) {
                return;
            }
            // 加入缓冲区
            buffer.add(new LogDocument(log));

            // 攒够 100 条就批量写入
            if (buffer.size() >= BATCH_SIZE) {
                flushBuffer();
            }
        } catch (Exception e) {
            System.err.println("消费失败，转入死信队列：" + e.getMessage());
            rocketMQTemplate.convertAndSend("log-topic-dlq", log);
        }
    }

    // 批量写入 ES
    private void flushBuffer() {
        if (!buffer.isEmpty()) {
            List<LogDocument> batch = new ArrayList<>(buffer);
            buffer.clear();
            logDocumentRepository.saveAll(batch);
            System.out.println("批量写入 ES：" + batch.size() + " 条");
        }
    }

    // 定时刷新：每 5 秒刷一次，避免消息不足 100 条时一直堆积
    @Scheduled(fixedDelay = 5000)
    public void scheduledFlush() {
        flushBuffer();
    }
}
