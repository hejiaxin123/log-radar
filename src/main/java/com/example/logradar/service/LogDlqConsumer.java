package com.example.logradar.service;

import com.example.logradar.entity.LogRecord;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

@Component
@RocketMQMessageListener(topic = "log-topic-dlq", consumerGroup = "log-radar-dlq-consumer")
public class LogDlqConsumer implements RocketMQListener<LogRecord> {

    @Override
    public void onMessage(LogRecord log) {
        // 死信队列中的消息记录到数据库或日志，等待人工处理
        System.err.println("死信消息：" + log.getId() + " - " + log.getMessage());
    }
}