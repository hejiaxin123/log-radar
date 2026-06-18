package com.example.logradar.service;

import com.example.logradar.entity.LogRecord;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.rocketmq.client.consumer.DefaultMQPushConsumer;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyStatus;
import org.apache.rocketmq.client.consumer.listener.MessageListenerConcurrently;
import org.apache.rocketmq.common.message.MessageExt;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.List;

@Service
public class MqService {

    private final RocketMQTemplate rocketMQTemplate;
    private final ObjectMapper objectMapper;

    public MqService(RocketMQTemplate rocketMQTemplate) {
        this.rocketMQTemplate = rocketMQTemplate;
        this.objectMapper = new ObjectMapper();
    }

    public int retryDlqMessages() throws Exception {
        DefaultMQPushConsumer consumer = new DefaultMQPushConsumer("log-radar-dlq-retry-group");
        consumer.setNamesrvAddr("127.0.0.1:9876");
        consumer.subscribe("log-topic-dlq", "*");
        final List<LogRecord> deadMessages = new ArrayList<>();
        consumer.registerMessageListener((MessageListenerConcurrently) (msgs, context) -> {
            for (MessageExt msg : msgs) {
                String body = new String(msg.getBody());
                try {
                    LogRecord log = objectMapper.readValue(body, LogRecord.class);
                    deadMessages.add(log);
                    rocketMQTemplate.convertAndSend("log-topic", log);
                    System.out.println("死信消息已回放: " + log.getId());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
        });
        consumer.start();
        Thread.sleep(5000);
        consumer.shutdown();
        return deadMessages.size();
    }
}