package com.example.logradar.service;

import com.example.logradar.entity.LogRecord;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.stereotype.Service;

@Service
public class LogProducer {

    private final RocketMQTemplate rocketMQTemplate;
    public LogProducer(RocketMQTemplate rocketMQTemplate) {
        this.rocketMQTemplate = rocketMQTemplate;
    }

    // 发送日志消息到 RocketMQ
    public void send(String topic, LogRecord log) {
        rocketMQTemplate.convertAndSend(topic, log);
    }
}