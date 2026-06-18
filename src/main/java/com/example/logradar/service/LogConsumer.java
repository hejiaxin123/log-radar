package com.example.logradar.service;

import com.example.logradar.entity.LogDocument;
import com.example.logradar.entity.LogRecord;
import com.example.logradar.repository.LogDocumentRepository;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.stereotype.Component;

@Component
@RocketMQMessageListener(topic = "log-topic", consumerGroup = "log-radar-consumer")
public class LogConsumer implements RocketMQListener<LogRecord> {

    private final LogDocumentRepository logDocumentRepository;
    private final RocketMQTemplate rocketMQTemplate;

    public LogConsumer(LogDocumentRepository logDocumentRepository, RocketMQTemplate rocketMQTemplate) {
        this.logDocumentRepository = logDocumentRepository;
        this.rocketMQTemplate = rocketMQTemplate;
    }

    @Override
    public void onMessage(LogRecord log) {
        try {
            // 幂等性检查：如果 ES 中已存在该 log_id，跳过
            if (logDocumentRepository.existsById(log.getId())) {
                System.out.println("消息已处理，跳过：" + log.getId());
                return;
            }
            logDocumentRepository.save(new LogDocument(log));
            System.out.println("ES 写入成功: " + log.getMessage());
        } catch (Exception e) {
            // 消费失败，发送到死信队列
            System.err.println("消费失败，转入死信队列：" + e.getMessage());
            rocketMQTemplate.convertAndSend("log-topic-dlq", log);
        }
    }
}